## LinkedBlockingQueue

LinkedBlockingQueue 是单向有界阻塞队列。元素顺序为 FIFO，使用锁来保证线程安全，实现方法比较简单。


### 类属性

此队列为有界队列，可以指定容量 capacity，默认最大容量为 Integer.MAX_VALUE。

head 总是指向队列中第一个节点（第一个节点不保存有效值），相应的，last 总是指向最后一个节点。

使用显式锁 takeLock 和 putLock 分别保护 take 操作和 put 操作的线程安全；使用与之对应的 notEmpty 和 notFull 两个 condition 分别保存对应操作中阻塞的线程。 

```java
    /** 队列容量，默认为 Integer.MAX_VALUE */
    private final int capacity;

    /** 当前元素数量 */
    private final AtomicInteger count = new AtomicInteger();

    /**
     * 链表头部
     * Invariant: head.item == null
     */
    transient Node<E> head;

    /**
     * 链表尾部
     * Invariant: last.next == null
     */
    private transient Node<E> last;

    /** take 和 poll 等持有的锁 */
    private final ReentrantLock takeLock = new ReentrantLock();

    /** 等待执行 take 操作的 condition */
    private final Condition notEmpty = takeLock.newCondition();

    /** put 和 offer 等持有的锁 */
    private final ReentrantLock putLock = new ReentrantLock();

    /** 等待执行 put 操作的 condition */
    private final Condition notFull = putLock.newCondition();
```

### 成员函数

take 和 put 操作使用完全互斥的资源访问控制方式，对更新操作均使用互斥的可重入锁访问。

**put**

尝试将节点添加到队列尾部时，首先需要获取 putLock 锁。获取到锁之后，判断队列是否已满，如果队列已满，当前线程进入 condition 等待，直到被唤醒。将节点添加到队列尾部之后，再一次根据元素数量，判断是否要进行入队操作（因为此时可能执行了 take 操作，队列有了多余的空间）。

值得注意的是，虽然 count 没有受到锁的保护（put 和 take 是不同的锁），但是依然可以使用 count 和容量进行比较，用于判断是否需要进入 condition 队列等待。详细解释见源码注释。

```java
    /**
     * 将指定元素添加到队列尾部，如果没有空间了，等待直到有空余的空间为止。
     * 响应中断。
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        int c = -1;
        // 创造一个新节点
        Node<E> node = new Node<E>(e);
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        // 获取响应中断锁 putLock
        putLock.lockInterruptibly();
        try {
            /*
             * 注意就算 count 没有受到锁的保护，仍然用于 wait guard。
             * 这样做事可行的，因为此时计数只可能减少（因为 put 操作
             * 已经被锁定了，只能执行 take 操作），而且如果容量发生变化
             * 当前线程（或者其它等待 put 的线程）会被唤醒。
             */
            while (count.get() == capacity) {
                // 队列已满，在 condition 中等待
                notFull.await();
            }
            // 节点进入队列
            enqueue(node);
            // 计数加一
            c = count.getAndIncrement();
            // 如果此时计数小于容量（可能有其他线程执行了 take 操作，唤醒等待
            // put 的线程
            if (c + 1 < capacity)
                notFull.signal();
        } finally {
            // 释放锁
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();
    }
```

**take**

```java
    // 响应中断的 take 操作
    public E take() throws InterruptedException {
        E x;
        int c = -1;
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            // 此处依然可以使用 count，理由同 put 函数中相应解释。
            // 如果计数等于 0，队列中已经没有元素了，阻塞当前线程
            while (count.get() == 0) {
                notEmpty.await();
            }
            // 出队列
            x = dequeue();
            // 计数减一
            c = count.getAndDecrement();
            // 唤醒其它等待 take 的线程
            if (c > 1)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        // 返回获取到的元素
        return x;
    }
```


