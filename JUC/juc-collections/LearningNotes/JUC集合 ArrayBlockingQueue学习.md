## ArrayBockingQueue


### 类属性

ArrayBlockingQueue 的原理非常简单。使用指定容量的循环数组（不可扩容）作为底层数据结构，存储阻塞队列的元素。使用两个整型变量 takeIndex、putIndex 作为索引分别记录队列头部和队列尾部。使用变量 count 记录队列中元素个数。

对于可能产生并发修改的操作，使用可重入锁进行控制。使用两个 Condition 队列分别在队列为空、队列已满的时候阻塞想要获取、想要插入元素的线程，并在适当的时候唤醒这些线程。

使用 Itrs 集合对象（每个迭代器都是一个节点）管理所有的迭代器，在任何影响迭代器的操作最后，相应地对迭代器也进行同样的操作，保证并发环境下迭代器的正确运行。

```java
    // 存储元素的数组
    final Object[] items;

    // 下一个 take, poll, peek 或者 remove 操作的位置索引
    int takeIndex;

    // 下一个 put, offer 或者 add 操作的位置索引
    int putIndex;

    // 队列中元素数量
    int count;

    /**
     * 使用经典的 two-condition 算法进行并发控制。
     */

    /** 用于所有访问控制的锁 */
    final ReentrantLock lock;

    // 等待 take 的 condition 队列
    private final Condition notEmpty;

    // 等待 put 的 condition 队列
    private final Condition notFull;

    /**
     * 当前可共享的活跃迭代器。如果没有此属性的值为 null。允许队列相关操作更新
     * 迭代器状态。
     */
    transient Itrs itrs = null;
```

### 成员函数

**入队**

将指定元素插入到 putIndex 位置，并唤醒因阻塞队列为空而在 notEmpty 队列中等待执行 take 操作的线程，提醒它们此时队列中已经有元素，可以执行 take 操作了。

```java
    /**
     * 在当前的 put 位置插入指定元素，并唤醒等待 take 的线程。
     * 只能在持有锁时调用。
     */
    private void enqueue(E x) {
        // assert lock.getHoldCount() == 1;
        // assert items[putIndex] == null;
        final Object[] items = this.items;
        items[putIndex] = x;
        // 在循环数组内索引增加方式（考虑到达边界）
        if (++putIndex == items.length)
            putIndex = 0;
        count++;
        notEmpty.signal();
    }
```

**出队**

返回 takeIndex 位置的元素并从队列中删除该元素（出队列），并唤醒因阻塞队列已满而在 notFull 队列中等待执行 put 操作的线程，提醒它们此时可以在队列尾部插入元素了。

```java
    /**
     * 在当前的 take 位置获取元素，并唤醒等待 put 的线程。
     * 只有在持有锁时调用。
     */
    private E dequeue() {
        // assert lock.getHoldCount() == 1;
        // assert items[takeIndex] != null;
        final Object[] items = this.items;
        @SuppressWarnings("unchecked")
        E x = (E) items[takeIndex];
        items[takeIndex] = null;
        // take 位置索引变化
        if (++takeIndex == items.length)
            takeIndex = 0;
        // 元素计数减一
        count--;
        // 如果当前有迭代器
        if (itrs != null)
            itrs.elementDequeued();
        // 唤醒等待 put 的线程
        notFull.signal();
        return x;
    }
```

以上两个方法为 private 方法，是 offer，add 等方法的基础。在外部的 public 方法中会检查数组是否满了，以及是否有超时限制。

此类中的操作基本和 Collections 中相应的集合操作一样，只是在操作上加上了可重入锁，保证同一时间只有单个线程进行修改操作。相对来说效率较低。

请参见 Collections 部分和完整源码解析。
