## Collections in java.util

### List, Stack and Queue

* [ArrayList]( [https://github.com/Saber-01/JDK-Learning/blob/master/colection/LearningNotes/ArrayList%20%E5%8E%9F%E7%90%86%EF%BC%8C%E9%80%9A%E8%BF%87%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E8%BF%9B%E8%A1%8C%E6%B7%B1%E5%85%A5%E4%BA%86%E8%A7%A3.md](https://github.com/Saber-01/JDK-Learning/blob/master/colection/LearningNotes/ArrayList 原理，通过源码学习进行深入了解.md) )| [LinkedList]( [https://github.com/Saber-01/JDK-Learning/blob/master/colection/LearningNotes/LinkedList%E5%8E%9F%E7%90%86%EF%BC%8C%20%E9%80%9A%E8%BF%87%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E8%BF%9B%E8%A1%8C%E6%B7%B1%E5%85%A5%E4%BA%86%E8%A7%A3.md](https://github.com/Saber-01/JDK-Learning/blob/master/colection/LearningNotes/LinkedList原理， 通过源码学习进行深入了解.md) )
- ArrayList 是基于数组实现的线性表，没有最大容量限制（其实有，是 Integer.MAX_VALUE），可扩容。LinkedList 是基于节点实现的线性表（双向链表），没有最大容量限制。
  - LinkedList 还实现了 Deque 接口，可以作为单向和双向队列实例。
  
* [Stack]( https://github.com/Saber-01/JDK-Learning/blob/master/colection/SourceCode/Stack.java )
- Stack 继承自 Vector，提供基础的栈操作。其保障线程安全的手段是使用 synchronized 包装所有函数，和其它线程安全的集合比起来，在多线程环境中效率很低。
  
* [ArrayDeque]( [https://github.com/Saber-01/JDK-Learning/blob/master/colection/LearningNotes/ArrayDeque%E5%8E%9F%E7%90%86%EF%BC%8C%20%E9%80%9A%E8%BF%87%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E8%BF%9B%E8%A1%8C%E6%B7%B1%E5%85%A5%E4%BA%86%E8%A7%A3.md](https://github.com/Saber-01/JDK-Learning/blob/master/colection/LearningNotes/ArrayDeque原理， 通过源码学习进行深入了解.md) )
- ArrayDeque 是基于循环数组的双向队列，可扩容，可用作栈和队列。
  - 平均情况下，作为栈比 Stack 效率更高，作为队列比 LinkedList 效率更高。
  
* [PriorityQueue]( [https://github.com/Saber-01/JDK-Learning/blob/master/colection/LearningNotes/PriorityQueue%E5%8E%9F%E7%90%86%EF%BC%8C%20%E9%80%9A%E8%BF%87%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E8%BF%9B%E8%A1%8C%E6%B7%B1%E5%85%A5%E4%BA%86%E8%A7%A3.md](https://github.com/Saber-01/JDK-Learning/blob/master/colection/LearningNotes/PriorityQueue原理， 通过源码学习进行深入了解.md) )
- 基于堆（底层为数组）的优先队列，可指定比较器。对于整型元素而言，默认最小堆。

### Set

* [HashSet]( https://github.com/Saber-01/JDK-Learning/blob/master/colection/SourceCode/HashSet.java ) | TreeSet
- Set 中不允许出现重复元素。
  - HashSet 完全依赖 HashMap（将 HashMap 实例作为一个属性），Map 中的 key 用来存储元素。TreeSet 则依赖 TreeMap 实现。

### Map

* [HashMap]( [https://github.com/Saber-01/JDK-Learning/blob/master/colection/LearningNotes/HashMap%E5%8E%9F%E7%90%86%EF%BC%8C%E9%80%9A%E8%BF%87%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E8%BF%9B%E8%A1%8C%E6%B7%B1%E5%85%A5%E4%BA%86%E8%A7%A3.md](https://github.com/Saber-01/JDK-Learning/blob/master/colection/LearningNotes/HashMap原理，通过源码学习进行深入了解.md) ) | TreeMap | [LinkedHashMap]( [https://github.com/Saber-01/JDK-Learning/blob/master/colection/LearningNotes/LinkedHashMap%E5%8E%9F%E7%90%86%EF%BC%8C%20%E9%80%9A%E8%BF%87%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E8%BF%9B%E8%A1%8C%E6%B7%B1%E5%85%A5%E4%BA%86%E8%A7%A3.md](https://github.com/Saber-01/JDK-Learning/blob/master/colection/LearningNotes/LinkedHashMap原理， 通过源码学习进行深入了解.md) )
- HashMap 作为一种高效的 Map 实现，平均情况下检索的时间代价只需要 O(1)，其核心的数据结构为数组，解决哈希碰撞的时候还会用到链表和红黑树（JDK 1.8）。
  - TreeMap 使用红黑树存储每个键值对节点，平均检索时间为 O(log n)。相对于 HashMap 而言，红黑树的优势是节点有序（因为红黑树是相对平衡的二叉检索树）。
  - LinkedHashMap 继承自 HashMap，在 HashMap 的基础上把所有节点组织成双向链表结构，所以 LinkedHashMap 也是有序的。LinkedHashMap 的思想可以用来实现 LRU 算法。

&nbsp;

## Concurrency Tools in java.util.concurrent

### ThreadLocal

* [ThreadLocal]( [https://github.com/Saber-01/JDK-Learning/blob/master/JUC/JUC%20%20ThreadLocal%E5%8E%9F%E7%90%86%EF%BC%8C%E9%80%9A%E8%BF%87%E6%BA%90%E7%A0%81%E8%BF%9B%E8%A1%8C%E5%AD%A6%E4%B9%A0%E6%B7%B1%E5%85%A5%E4%BA%86%E8%A7%A3.md](https://github.com/Saber-01/JDK-Learning/blob/master/JUC/JUC  ThreadLocal原理，通过源码进行学习深入了解.md) )
- ThreadLocal 是属于 java.lang 包的类。它为每个使用该变量的线程提供独立的变量副本，所以每一个线程都可以独立地改变自己的副本，而不会影响其它线程所对应的副本。可以简单地理解为为指定线程存储数据，只有指定线程可以读取。

### Synchronizer

* [AbstractQueuedSynchronizer]( [https://github.com/Saber-01/JDK-Learning/blob/master/JUC/juc-locks/LearningNotes/JUC%E9%94%81%20Locks%E6%A0%B8%E5%BF%83%E7%B1%BB%EF%BC%8CAQS%E5%8E%9F%E7%90%86%EF%BC%8C%E9%80%9A%E8%BF%87%E6%BA%90%E7%A0%81%E8%BF%9B%E8%A1%8C%E5%AD%A6%E4%B9%A0%E6%B7%B1%E5%85%A5%E4%BA%86%E8%A7%A31.md](https://github.com/Saber-01/JDK-Learning/blob/master/JUC/juc-locks/LearningNotes/JUC锁 Locks核心类，AQS原理，通过源码进行学习深入了解1.md) )
- AQS（AbstractQueuedSynchronizer）抽象类，队列同步控制器，是 Java 并发用来控制锁和其他同步组件的基础框架。常用的 Lock、CountDownLatch、CyclicBarrier、Semaphore 等均基于 AQS 实现。
  
* [ReentrantLock](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/ReentrantLock.md) | [ReentrantReadWriteLock](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/ReentrantReadWriteLock.md)

  - ReentrantLock 是 Lock 接口的实现，翻译为可重入锁，支持线程无限制重入同一代码段，在获取和释放时记录重入次数。
  - ReentrantReadWriteLock 是 Lock 接口的实现，翻译为可重入读写锁，实现了可重入读锁和可重入写锁，也即共享锁和互斥锁（排它锁）。

* [CountDownLatch](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/CountDownLatch.md) | [CyclicBarrier](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/CyclicBarrier.md) | [Phaser](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/Phaser.md)

  - CountDownLatch 可称为倒数计数器，latch 的作用是控制计数器的值降到 0 时，让所有等待的线程继续执行。
  - CyclicBarrier 可称为循环栅栏，线程到达栅栏时相互等待，等到所有线程都到达时才继续执行。
  - Phaser 可称为多阶段栅栏，是最复杂且最灵活的控制器，兼具前两者的特性。

* [Semaphore](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/Semaphore.md)

  - 通过“令牌数”限制同一时间并发的线程数量，拿到令牌的线程可以继续运行，没拿到的线程需要等待，直到拿到为止。

* [Exchanger](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/Exchanger.md)

  - 用于两个线程之间的交换数据。

### Concurrency Collections

* [CAS](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/CAS.md)

  - CAS（Compare And Swap，比较和交换），是基于乐观锁的操作，不需要阻塞就可以实现原子操作的一种方式。

* [CopyOnWriteArrayList](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/JUCCollections/CopyOnWriteArrayList.md)

  - 对应于常用集合中的 ArrayList，使用 COW（Copy On Write，写时复制）保证线程安全。

* [ConcurrentHashMap](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/JUCCollections/ConcurrentHashMap.md) | [ConcurrentSkipListMap](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/JUCCollections/ConcurrentSkipListMap.md)

  - ConcurrentHashMap 对应于常用集合中的 HashMap，JDK 1.8 中不再使用分段锁，改用自旋 + CAS 保障线程安全。
  - ConcurrentSkipListMap 是基于跳跃表（SkipList）实现的 Map 集合，随机建立层级索引和增加层级。如果按照标准的跳跃表建立索引，跳跃表索引会无限接近接近平衡二叉树时，检索的时间复杂度能达到 O(log n)。

* [ArrayBlockingQueue](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/JUCCollections/ArrayBlockingQueue.md) | [LinkedBlockingQueue](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/JUCCollections/LinkedBlockingQueue.md) | [LinkedBlockingDeque](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/JUCCollections/LinkedBlockingDeque.md) | [PriorityBlockingQueue](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/JUCCollections/PriorityBlockingQueue.md)

  - BlockingQueue 最常见的四个具体实现，使用显式的锁来保证线程安全。
  - ArrayBlockingQueue 是基于数组的有界阻塞队列，不允许扩容，元素的排列顺序为 FIFO。
  - LinkedBlockingQueue 是基于链表的单向有界阻塞队列。
  - LinkedBlockingDeque 是基于链表的双向有界阻塞队列，支持 FIFO 和 LIFO 两种方式。
  - PriorityBlockingQueue 是基于堆（数组）的优先级阻塞队列（对应常用集合里的 PriorityQueue）。
  - 以上四种 BlockingQueue 都不允许 null 元素，试图插入 null 元素将会抛出 NullPointerException 异常。

* [ConcurrentLinkedQueue](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/JUCCollections/ConcurrentLinkedQueue.md) | [ConcurrentLinkedDeque](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/JUCCollections/ConcurrentLinkedDeque.md)
- 单向/双向无界非阻塞队列。抛弃显式锁，使用 CAS 构建，不需要阻塞线程就能实现线程安全。基础数据结构为链表。
  
* [LinkedTransferQueue](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/JUCCollections/LinkedTransferQueue.md) | [SynchronousQueue](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/JUCCollections/SynchronousQueue.md)
- LinkedTransferQueue 是无界阻塞队列。基于链表实现，通过 CAS 保证线程安全。其中还用到了 LockSupport 来阻塞线程。
  - SynchronousQueue 是没有容量的阻塞队列，每个插入操作都要等待其他线程的删除操作，每个删除操作都要等待插入操作，实际相当于将数据从一个线程传递到另一个线程。和 LinkedTransferQueue 类似，但包括了公平和非公平两种模式。
  
* [DelayQueue](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/JUCCollections/DelayQueue.md)

  - 无界延时阻塞队列。使用显式锁保证线程安全。使用优先队列对延迟时间排序。只有当队列头部元素延迟时间到期，才允许被取出，否则线程一直等待。

### Thread Pool

* [FutureTask](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/FutureTask.md)

  - 实现了 Runnable 和 Future 接口，是可取消的异步运算，支持的任务类型是 Callable。可以通过 get 方法获取结果，如果任务尚未完成，获取结果的线程将会被阻塞。
  - FutureTask 可以通过 submit 方法提交到线程池中执行。

* [CompletableFuture](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/CompletableFuture.md)

  - 实现了 Future 接口和 CompletionStage 接口，主要用于获取异步任务执行的结果和回调触发新的操作。

* [ThreadPoolExecutor](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/ThreadPoolExecutor.md) | [ScheduledThreadPoolExecutor](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/ScheduledThreadPoolExecutor.md)

  - 线程池用来控制一系列线程的创建、调度、监控和销毁等。
  - ThreadPoolExecutor 实现了 ExecutorService 接口，是创建线程池的核心类，可指定核心线程数，最大线程数，阻塞队列，拒绝策略等参数。工厂类 Executors 中一大半的常用线程池都是通过 ThreadPoolExecutor 创建。
  - ScheduledThreadPoolExecutor 是线程池的一种，用于延迟或周期性执行提交的任务。

* [~~ForkJoinPool~~](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/ForkJoinPool.md)

    > 太难了...
