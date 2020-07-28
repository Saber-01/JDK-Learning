## PriorityBlockingQueue

PriorityBlockingQueue 是支持多线程的无界优先级阻塞队列，基于堆实现，底层数据结构依然是数组。实现思想和 PriorityQueue 相似。


### 类属性

和 PriorityQueue 相同的属性不再赘述，在 PriorityQueue 基础上，增加了一个可重入锁，一个 condition 队列，一个只允许 CAS 操作的变量（实际上也作为锁，非阻塞锁）用于并发控制。

lock：可重入锁，用于入队列、出队列操作；

notEmpty：与 lock 相关联的 condition 队列，在 take 和 poll 方法中用于容纳因为队列为空而无法获取到元素的线程（队列为无界队列，put 相关的方法不需要 condition）；

allocationSpinLock：自旋锁，扩容时控制只有一个线程真正执行扩容操作。（其实没有自旋，只是一个判断而已。）

```java
    /**
     * 保证所有 public 方法线程安全的锁。
     */
    private final ReentrantLock lock;

    /**
     * 当队列为空时用来阻塞线程的 condition。
     */
    private final Condition notEmpty;

    /**
     * 自旋锁
     */
    private transient volatile int allocationSpinLock;
```

### 成员函数

优先级队列中最常用的是对整型元素进行优先级排序，所以后面方法中的注释皆以比较器为整型元素自然顺序（构建最小堆）为例。

#### 入队列

入队列的核心方法在 offer 中实现。实际上，入队操作的全过程都在 lock 的锁定范围内（tryGrow 会释放锁，然后重新获取），所以此方法基本上是完全串行的方法。

使用 siftUp 最初将指定元素添加到数组最后一个槽里，然后再调整（上浮）直到找到其正确的位置（siftUp 的具体内容和思路请参考堆的调整方法）。

插入完成后，队列必定不是空队列了，最后调用 signal 唤醒 condition 上等待 take 的线程。

```java
    /**
     * 将指定元素插入到优先队列。
     * 由于队列是无界队列，此方法不会返回 false。
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws ClassCastException if the specified element cannot be compared
     *         with elements currently in the priority queue according to the
     *         priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        if (e == null)
            throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        // 获取锁
        lock.lock();
        int n, cap;
        Object[] array;
        // 扩容（size 表示下一个可插入的位置）
        while ((n = size) >= (cap = (array = queue).length))
            tryGrow(array, cap);
        try {
            Comparator<? super E> cmp = comparator;
            // 没有比较器的插入方式（整型默认自然顺序为最小堆）
            // 在 n 位置插入元素
            if (cmp == null)
                siftUpComparable(n, e, array);
            // 指定了比较器的插入方式
            else
                siftUpUsingComparator(n, e, array, cmp);
            size = n + 1;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
        return true;
    }
    
    /**
     * 将元素插入到 k 位置，然后调整元素位置使其满足堆的性质。
     * 将其往上层移动直到大于等于其父节点，或者其已经是根节点了。
     *
     * @param k the position to fill
     * @param x the item to insert
     * @param array the heap array
     */
    private static <T> void siftUpComparable(int k, T x, Object[] array) {
        Comparable<? super T> key = (Comparable<? super T>) x;
        while (k > 0) {
            // 父节点位置为 (k - 1) / 2
            int parent = (k - 1) >>> 1;
            Object e = array[parent];
            // 如果 key 大于等于父节点，跳出循环
            if (key.compareTo((T) e) >= 0)
                break;
            // 否则父节点下移，继续往上
            array[k] = e;
            k = parent;
        }
        // 找到 key 的位置了
        array[k] = key;
    }
```

#### 扩容

此方法设置扩容由单线程完成，由 allocationSpinLock 进行控制。

在扩容之前释放锁，尝试 CAS 修改 allocationSpinLock 的值，只有一个线程能修改成功，开始创建新的数组（避免内存占用过多），其他修改失败的线程将时间片让给该线程。

当旧容量小于 64 时，新容量设置为 2 * oldCap + 2；旧容量大于等于 64 时，新容量设置为 1.5 * oldCap。

新数组创建完成后（newArray 不为 null），线程再次竞争锁。竞争成功的第一个线程将旧数组的所有元素复制到新数组里。随后返回到 offer 函数中，继续完成后面的操作。

```java
    /**
     * 扩容。
     *
     * @param array the heap array
     * @param oldCap the length of the array
     */
    private void tryGrow(Object[] array, int oldCap) {
        // 此类是私有方法，只在 offer 函数中调用。
        // 调用时已经获取了锁，进行扩容操作前释放
        lock.unlock();
        Object[] newArray = null;
        // 只有一条线程能进入
        if (allocationSpinLock == 0 &&
                UNSAFE.compareAndSwapInt(this, allocationSpinLockOffset,
                        0, 1)) {
            try {
                // 新的容量
                // 旧容量小于 64 时，新容量为 2 * cap + 2
                // 旧容量大于等于 64 时，新容量为 1.5 * cap
                int newCap = oldCap + ((oldCap < 64) ?
                        (oldCap + 2) : // 容量较小时扩容较快
                        (oldCap >> 1));
                // 计算出来的新容量超出最大限制
                if (newCap - MAX_ARRAY_SIZE > 0) {    // possible overflow
                    int minCap = oldCap + 1;
                    // 溢出
                    if (minCap < 0 || minCap > MAX_ARRAY_SIZE)
                        throw new OutOfMemoryError();
                    // 或者容量直接变成最大容量
                    newCap = MAX_ARRAY_SIZE;
                }
                // 如果数组还没有被更改，创建新的数组
                if (newCap > oldCap && queue == array)
                    newArray = new Object[newCap];
            } finally {
                // 释放 CAS 锁
                allocationSpinLock = 0;
            }
        }
        // newArray 是局部变量，线程独有
        // newArray 为 null 说明竞争 allocationSpinLock 失败，让出时间片
        if (newArray == null)
            Thread.yield();
        // 竞争锁
        lock.lock();
        // 上锁，如果数组还没有被更改，将旧数组中的元素全部复制到新数组里
        if (newArray != null && queue == array) {
            queue = newArray;
            System.arraycopy(array, 0, newArray, 0, oldCap);
        }
    }
```

#### 出队列

实行出队列之前同样要先获取锁。在 take 函数中，如果队列为空，线程进入 condition 等待，直到队列中有元素为止。

堆顶元素出队列之后，将数组中最后一个元素移动到堆顶，然后使用 siftDown 判断是否需要下沉，调整该元素位置直到符合堆的性质。

siftDown 的具体内容和思路请参考堆的调整方法

```java
    // 堆顶元素出队列。没有元素进入 condition 等到，直到有元素为止。
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        E result;
        try {
            // 没有元素进入 condition 等待。
            while ( (result = dequeue()) == null)
                notEmpty.await();
        } finally {
            lock.unlock();
        }
        return result;
    }
    
    /**
     * poll 的具体实现。在持有锁时才能调用。
     */
    private E dequeue() {
        int n = size - 1;
        // 没有元素，返回 null
        if (n < 0)
            return null;
        else {
            Object[] array = queue;
            // 堆顶元素出列
            E result = (E) array[0];
            // 最后一个元素插入堆顶，然后调整其位置
            E x = (E) array[n];
            array[n] = null;
            Comparator<? super E> cmp = comparator;
            if (cmp == null)
                siftDownComparable(0, x, array, n);
            else
                siftDownUsingComparator(0, x, array, n, cmp);
            size = n;
            return result;
        }
    }
    
    /**
     * siftUp 用于插入，因为插入操作是新元素插入到最底层
     * siftDown 用于删除，删除是将最后一个元素提到最上层
     * 将元素 x 插入到 k 位置，调整使其满足堆的性质。将 x 元素下移直到
     * 小于等于其子节点或者成为叶节点。
     *
     * @param k the position to fill
     * @param x the item to insert
     * @param array the heap array
     * @param n heap size
     */
    private static <T> void siftDownComparable(int k, T x, Object[] array,
                                               int n) {
        if (n > 0) {
            Comparable<? super T> key = (Comparable<? super T>)x;
            int half = n >>> 1;
            // k 大于等于 half 时已经是叶节点，只需要
            while (k < half) {
                // 左子节点索引
                int child = (k << 1) + 1;
                Object c = array[child];
                // 右子节点索引
                int right = child + 1;
                // 获取值较小的元素，和其索引
                if (right < n &&
                        ((Comparable<? super T>) c).compareTo((T) array[right]) > 0)
                    c = array[child = right];
                // 当 key 小于等于 c 时，跳出循环
                if (key.compareTo((T) c) <= 0)
                    break;
                // k 位置赋值为较小的元素，然后 k 继续往下
                array[k] = c;
                k = child;
            }
            // 找到位置了
            array[k] = key;
        }
    }
```

#### 建堆

和 PriorityQueue 一样。

```java
    /**
     * 对初始的完全无序的数组执行建堆操作。
     */
    private void heapify() {
        Object[] array = queue;
        int n = size;
        // 从 half 开始往前处理每一个节点，找到它正确的位置
        int half = (n >>> 1) - 1;
        Comparator<? super E> cmp = comparator;
        if (cmp == null) {
            for (int i = half; i >= 0; i--)
                siftDownComparable(i, (E) array[i], array, n);
        }
        else {
            for (int i = half; i >= 0; i--)
                siftDownUsingComparator(i, (E) array[i], array, n, cmp);
        }
    }
```

### 参考

[JUC源码分析-集合篇（七）：PriorityBlockingQueue](https://www.jianshu.com/p/fd26c91cd2a0)
