## DelayQueue

DelayQueue 是无界延时阻塞队列。元素必须实现 Delay 接口，并且在队列中按照延迟时间排序。队列头部的元素延迟时间到期之后，才能被取出。使用可重入锁保证线程安全。



### 类属性

使用可重入锁保证线程安全，未获取到元素的线程进入锁的 Condition 队列。

使用优先队列 PriorityQueue 对元素排序。

leader 指向等待队列头部元素的线程。

```java
    // 保证线程安全的锁
    private final transient ReentrantLock lock = new ReentrantLock();
    
    // 用于排序的优先队列
    private final PriorityQueue<E> q = new PriorityQueue<E>();

    /**
     * 等待队列头部元素的线程。Leader-Follower 模式的这种变体可以最小化
     * 不必要的等待时间。当一个线程成为 leader 时，它等待下一个 delayed，
     * 其他线程无限期等待。leader 线程在从 take/poll 返回之前，必须唤醒
     * 其他线程，除非其他线程在此期间成为 leader。每当队列头被一个具有
     * 较早过期时间的元素替换时，通过将 leader 设置为 null 使其无效，并且
     * 会通知一些正在等待的线程，但不一定是当前的 leader。因此等待线程
     * 必须准备在等待时获取和失去领导权。
     */
    private Thread leader = null;

    /**
     * 当一个新的元素到队列头部或者一个新的线程需要成为 leader 时唤醒
     * condition 队列的线程。
     */
    private final Condition available = lock.newCondition();
```

### 成员函数

**offer**

offer 用于将元素插入到优先队列中。如果队列之前为空，且成功插入一个元素，需要唤醒等待获取元素的线程。

```java
    /**
     * 指定元素插入到队列。
     *
     * @param e the element to add
     * @return {@code true}
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 插入优先队列
            q.offer(e);
            if (q.peek() == e) {
                leader = null;
                available.signal();
            }
            return true;
        } finally {
            lock.unlock();
        }
    }
```

**take**

take/poll 用于获取并删除头部元素。

如果头部元素的等待时间已经结束了，那么获取成功，直接返回。

如果还没有结束，且 leader 已经被其他线程占用了，那么进入 Condition 队列等待

在执行操作前要先加锁。

```java
    /**
     * 获取并删除队列头部元素，如果没有延迟到期的元素，等待直到获取成功。
     *
     * @return the head of this queue
     * @throws InterruptedException {@inheritDoc}
     */
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (;;) {
                E first = q.peek();
                // 如果没有元素，进入 available 队列等待
                if (first == null)
                    available.await();
                else {
                    // 获取剩余时间
                    long delay = first.getDelay(NANOSECONDS);
                    // 没有剩余时间，可以出队列了
                    if (delay <= 0)
                        return q.poll();
                    // 如果等待时间还没到期
                    // 释放 first 的引用，避免内存泄露
                    first = null;
                    // leader 不为 null，说明有其他线程已经获取到 leader，进入
                    // available 队列等待
                    if (leader != null)
                        available.await();
                    else {
                        // 获取当前线程，将 leader 设置为当前线程
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread;
                        try {
                            // 在 available 中等待 delay 时间
                            available.awaitNanos(delay);
                        } finally {
                            // 如果当前线程就是 leader，将 leader 置为 null，重新循环
                            // 再次尝试获取元素。
                            if (leader == thisThread)
                                leader = null;
                        }
                    }
                }
            }
        } finally {
            if (leader == null && q.peek() != null)
                available.signal();
            lock.unlock();
        }
    }
```

### 参考

[JUC源码分析-集合篇（十）：DelayQueue](https://www.jianshu.com/p/ae2e05fd638f)


