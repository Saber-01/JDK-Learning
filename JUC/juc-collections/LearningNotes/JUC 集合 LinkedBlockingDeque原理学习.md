## LinkedBlockingDeque

LinkedBlockingDeque 是双向有界阻塞队列。元素顺序支持 FIFO 和 LIFO，使用锁来保证线程安全。



### 小结

此类在实现上，甚至比 LinkedBlockingQueue 还要简单，LinkedBlockingQueue 使用了两个锁，分别保护 take 和 put 操作，而此类仅仅使用一个可重入锁，用于所有并发操作的线程安全。几乎所有的操作都在开始前加锁，完成后释放。所有的操作都是同步操作，不可并行。

此类实现了 Stack, Queue, Deque 的所有方法。


