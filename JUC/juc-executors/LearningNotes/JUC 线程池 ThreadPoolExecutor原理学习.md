# 概述
ThreadPoolExecutor，从该类的命名也可以看出，这是一种线程池执行器。当有任务需要执行时，线程池会给该任务分配线程，如果当前没有可用线程，一般会将任务放进一个队列中，当有线程可用时，再从队列中取出任务并执行，如下图：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200730151145988.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
线程池的引入主要为了解决以下问题：
* 方便管理： 由线程池创建、调度、监控和销毁所有线程，能够控制线程数量，避免出现线程泄漏。
* 降低资源消耗：重复利用已经创建的线程执行任务，而不是持续不断地创建和销毁线程，减少了系统因为频繁创建和销毁线程所带来的性能开销。
* 提高响应速度： 直接从线程池空闲的线程中获取工作线程，立即执行任务，就不需要再次创建新线程。

# 线程池状态
线程池一共有五种状态分别是：RUNNING、SHOUTDOWN、STOP、TIDYING、TERMINATED。
它们之间满足不同条件会进行转化。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200730152258741.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
RUNNING：正常运行状态，接受新任务，处理阻塞队列中的任务。

SHUTDOWN：不接受新任务，但是会继续执行正在执行的任务，并且仍然处理阻塞队列中的任务。

STOP：不接受新任务，也不处理阻塞队列中的任务，并且会尝试中断正在进行的任务。

TIDYING：所有的任务都已经停止，workerCount工作线程为0，当池状态为TIDYING时会准备运行terminated()方法

TERMINATED：线程池运行完terminated方法，完全终止。

线程池状态保存在作为类属性的原子整型变量 ctl 的高 3 位 bits 中。
# 内部常量
ctl 为线程池状态，它是一个32位的原子类整型数据，它的高三位存储线程池的运行状态，即以上五种，
依次是，111，000，001， 010，011。它的低29位表示工作线程数量，理论上如果没有限制最大线程数量的话，最大线程数为2的29次方-1，
ctlOf方法，将线程池运行状态和线程池工作线程数，合为一个完整的表示线程池状态的值。
注意：因为高三位的第一位是符号位，所以，实际RUNNING的运行状态时，ctl得到的是小于0 的。
且。以上五种运行状态，它们对应的ctl显然是递增的。
```java
// ctl 整型变量共有 32 位，低 29 位保存有效线程数 workCount，使用
    // 高 3 位表示线程池运行状态 runState。
    // 线程池状态，是一个原子型整数32位，高3位保存线程池运行状态runState,
    // 低29位保存线程池线程数量。
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    private static final int COUNT_BITS = Integer.SIZE - 3; //整型数字32位-3，29位。
    // 最大有效线程数为 (2^29)-1
    // CAPACITY容量 ，即最大线程数量，2的29次方-1.
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;

    // runState is stored in the high-order bits
    //runState线程池工作状态，存储在ctl的高三位。
    //111 + 29 个 0
    private static final int RUNNING    = -1 << COUNT_BITS;  //初始化时为运行状态。
    // 000 + 29 个 0
    private static final int SHUTDOWN   =  0 << COUNT_BITS;  //不接受新任务，但是等待队列的任务还是会处理，正在运行的任务也会继续执行
    // 001 + 29 个 0
    private static final int STOP       =  1 << COUNT_BITS;   //  不接受新任务，等待队列的也不处理了，并且尝试中断所有正在工作的线程。
    // 010 + 29 个 0
    private static final int TIDYING    =  2 << COUNT_BITS;   //当线程池和队列都为空时，会运行钩子函数，terminated方法。
    // 011 + 29 个 0
    private static final int TERMINATED =  3 << COUNT_BITS;   //terminated方法运行完成

    // Packing and unpacking ctl
    // 获取 runState，
    private static int runStateOf(int c)     { return c & ~CAPACITY; }
    // 获取 workerCount
    private static int workerCountOf(int c)  { return c & CAPACITY; }
    // 如果 workerCount 和 runState 分别是两个整数，将指定2个变量，通过逻辑或，合成一个32位变量。
    private static int ctlOf(int rs, int wc) { return rs | wc; }

```
***
# 内部属性
* 最重要的类属性 ctl 是原子整型变量类型，保存了线程池的两个状态，高 3 位表示线程池的状态，低 29 位表示线程池有效线程数。

* workQueue 是用于保存待执行任务的阻塞队列。workers 是保存了所有工作线程的集合，在此线程池中，工作线程并不是 Thread，而是封装了 Thread 的 Worker 内部类。
* mainLock是一个重入锁ReentrantLock，在线程池运行过程中，如删除工作线程，中断任务，添加工作线程到hashset类型的works集合，更改线程池状态等等操作，都需要上锁，保证并发一致性。

构造一个 ThreadPoolExecutor 线程池主要用到类属性中以下六个参数：

* corePoolSize：核心线程数，表示通常情况下线程池中活跃的线程个数；初始必须指定

* maximumPoolSize：线程池中可容纳的最大线程数；初始必须指定

* keepAliveTime：此参数表示空闲线程，闲置多久会被销毁，默认在工作线程超出核心线程时回收超出的空闲线程，如果开启allowCoreThreadTimeOut那么核心线程也会参与空闲回收。初始必须指定

* workQueue：存放任务的阻塞队列，该队列可以使用不同的阻塞队列来实现不同类型的线程池，初始必须指定

* threadFactory：创建新线程的线程工厂；线程工厂主要好处是，一方面解耦解耦线程对象的创建与使用，二是可以批量配置线程池中工作线程的信息（优先级、线程名称、是否守护线程等）。 可以缺省。

* handler：线程池饱和（任务过多）时的拒绝策略。可以缺省。

allowCoreThreadTimeOut：为true时，keepAliveTime，清除空闲线程

每个变量的具体含义如下所示：
```java
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
 /**
     * 用于保存任务并且将任务交给工作线程的队列。不需要让 workQueue.poll 返回 null
     * 和队列为空划等号，仅仅依赖 workQueue.isEmpty 的结果来判断队列是否为
     * 空即可（例如在判断状态是否从 SHUTDOWN 转变到 TIDYING）。
     */
    private final BlockingQueue<Runnable> workQueue; //保存要执行的任务的阻塞队列，接收Runnable类型任务。

    /**
     * 访问 worker 集合和相关 bookkeeping 持有的锁。虽然可以使用某种类型
     * 的并发集合，但一般使用锁更好。其中一个原因是，它序列化了
     * interruptIdleWorkers，从而避免了不必要的中断风暴，特别是在 shutdown 期间。
     */
    private final ReentrantLock mainLock = new ReentrantLock();

    /**
     * 包括线程池中所有工作线程的集合。只有在持有 mainLock 时才能访问。
     */
    private final HashSet<Worker> workers = new HashSet<Worker>();

    /**
     * 用来支持 awaitTermination 的 condition 队列
     */
    private final Condition termination = mainLock.newCondition();

    /**
     * 最大池容量。只有在持有 mainLock 时才能访问。
     * 保存工作线程集合，最大时达到的线程数量
     */
    private int largestPoolSize;

    /**
     * 已完成任务的计数器。仅在工作线程终止时更新。只有在持有 mainLock 时才能访问。
     */
    private long completedTaskCount;

    /*
     * 所有的用户控制参数都被声明为 volatile，因此正在进行的操作基于最新的值，
     * 不需要锁定，因为没有内部的不变量依赖于它们的同步改变。
     */

    /**
     * 创建新线程的工厂。所有的线程都是使用这个工厂创建的（通过
     * addWorker 方法）。所有的调用者必须为 addWorker 失败做好准备，
     * 这可能是因为系统或用户的策略限制了线程的数量。即使它不被看成一个
     * 错误，创建线程失败可能会导致新的任务被拒绝或者现有任务留在队列中。
     */
    private volatile ThreadFactory threadFactory;

    /**
     * 线程池饱和或 shutdown 时调用。
     *用于处理饱和后还加入的新任务。
     */
    private volatile RejectedExecutionHandler handler;

    /**
     * 等待工作的空闲线程的超时时间。超过 corePoolSize 或 allowCoreThreadTimeOut
     * 时，线程使用。否则，它们将永远等待执行新的任务。
     */
    private volatile long keepAliveTime;

    /**
     * 如果为 false（默认），核心线程即使空闲也保持活动状态。
     * 如果为 true，空闲的核心线程由 keepAliveTime 确定存活时间。
     */
    private volatile boolean allowCoreThreadTimeOut;

    /**
     *
     * 核心线程数，只有在allowCoreThreadTimeOut设置为true情况下，
     * 核心线程有可能因为空闲时间超过了指定的keepAliveTime决定什么时候消亡。
     */
    private volatile int corePoolSize;

    /**
     * 线程池最多容纳线程数。注意实际的最大值受到 CAPACITY 的限制。
     */
    private volatile int maximumPoolSize;
```
***
# 内部类
## Worker 工作线程
可以看到工作线程类主要继承于AQS同步器框架基础类，并且实现了Runnable接口。
继承AQS主要是因为，工作线程实现了基本的锁功能，简化获取和执行任务过程的同步性保证。
Worker构造函数中接受一个Runnable类型的task任务，然后用这个task，初始化属性firstTask,然后使用自身this引用调用线程工厂的newThread方法。并将创建的线程赋值到Worker中的thread属性，所以当调用Worker中thread的start方法时，最终实际是调用了Worker.run()方法，该方法内部委托给runWorker方法执行任务，这个方法我们后面会详细介绍。
```java
 /**
     * Worker 类主要维护执行任务的线程的中断控制状态，以及其他的
     * bookkeeping 功能。该类扩展了 AQS，以简化获取和释放任务执行时的锁。
     * 这可以防止一些试图唤醒正在等待任务工作线程的中断，而不是防止中断
     * 正在运行的任务。我们实现了一个简单的不可重入独占锁，而不是使用
     * ReentrantLock，因为我们不希望工作线程在调用诸如 setCorePoolSize
     * 之类的线程池控制方法时能重入获取锁。另外，为了在线程真正开始运行
     * 任务之前禁止中断，我们将锁状态初始化为一个负值，并在启动时清除它（在
     * runWorker 中）。
     */
    private final class Worker
            extends AbstractQueuedSynchronizer
            implements Runnable
    {
        /**
         * This class will never be serialized, but we provide a
         * serialVersionUID to suppress a javac warning.
         */
        private static final long serialVersionUID = 6138294804551838833L;

        /** 此 worker 运行的线程，如果创建失败为 null */
        final Thread thread; //Worker工作线程，初始化失败为null。
        /** 第一个执行的任务。可能为 null */
        Runnable firstTask;
        /** 每一个工作线程完成的任务计数器 */
        volatile long completedTasks;

        /**
         * 构造函数
         * @param firstTask the first task (null if none)
         */
        Worker(Runnable firstTask) {   //指定初始任务创建工作线程。
            // 设置此 AQS 的状态。初始为-1
            setState(-1); // 在runWorker调用前禁止中断 inhibit interrupts until runWorker
            this.firstTask = firstTask;
            // 使用指定的线程工厂创建一个新的线程，使用传入 this，也就是新建线程会执行当前Worker类中的Run函数。
            this.thread = getThreadFactory().newThread(this);
        }

        /** Delegates main run loop to outer runWorker  */
        public void run() {  //线程执行该任务的入口函数。
            runWorker(this);    //将自身传给外层的runWorker方法
        }

        // Lock methods
        //
        // 0 表示未锁定状态
        // 1 表示锁定状态

        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public void lock()        { acquire(1); }
        public boolean tryLock()  { return tryAcquire(1); }
        public void unlock()      { release(1); }
        public boolean isLocked() { return isHeldExclusively(); }

        // 如果线程存在，则中断线程
        void interruptIfStarted() {
            Thread t;
            //如果同步状态大于等于0，即Worker被runWorker运行过。且当前线程不为null，线程非中断。
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();   //中断线程。
                } catch (SecurityException ignore) {   //安全问题
                }
            }
        }
    }

```
## 四种拒绝策略
### AbortPolicy
该策略是默认的拒绝策略，在rejectedExecution方法中，直接抛出异常。
```java
/**
     * 直接抛出 RejectedExecutionException 异常
     */
    public static class AbortPolicy implements RejectedExecutionHandler {
        /**
         * 创建 AbortPolicy。
         */
        public AbortPolicy() { }

        /**
         * 总是抛出 RejectedExecutionException 异常。
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         * @throws RejectedExecutionException always
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() +
                    " rejected from " +
                    e.toString());
        }
    }
```
***
### CallerRunsPolicy
该策略，判断线程池如果没有被shutdown的话，直接由当前线程运行传入的任务的 run方法。即立刻执行任务。
```java
  /**
     * 被拒绝任务的处理程序，直接在 execute 方法的调用线程中运行被拒绝的
     * 任务，除非线程池已经被 shutdown 了，在这种情况下被丢弃。
     */
    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        /**
         * 创建 CallerRunsPolicy
         */
        public CallerRunsPolicy() { }

        /**
         * 在调用者的线程中执行任务 r，除非执行者被 shutdown，这种情况下
         * 任务被忽略。
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            // 如果线程池没有被 shut down，则直接运行
            if (!e.isShutdown()) {
                r.run();  //直接用当前线程运行任务的run方法。
            }
        }
    }
```
***
### DiscardPolicy
这个策略直接忽略任务，也不会抛出异常，可以看到rejectedExecution方法体为空。
```java
 /**
     * 直接忽略，不会抛出任何异常
     */
    public static class DiscardPolicy implements RejectedExecutionHandler {
        /**
         * 创建 DiscardPolicy。
         */
        public DiscardPolicy() { }

        /**
         * 直接忽略。
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }
    }
```
***
### DiscardOldestPolicy
该策略在线程池没有被shutdown时，会移除任务队列中阻塞最久的队列头部的任务，然后调用线程池的execute方法重新执行一次刚任务。
```java
 /**
     * 将任务队列中最老的未处理请求删除，然后 execute 任务。除非线程池被
     * shut down，这种情况下任务被丢弃。
     */
    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        /**
         * 创建 DiscardOldestPolicy。
         */
        public DiscardOldestPolicy() { }

        /**
         * 获取并忽略线程池中下一个将执行的任务（任务队列中最老的任务），
         * 然后重新尝试执行任务 r。除非线程池关闭，在这种情况下，任务 r 将被
         * 丢弃。
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {    //乳沟线程池没有被shutdown
                e.getQueue().poll();  //移除任务队列等待最久的任务，
                e.execute(r);       //然后执行新加进来的任务。
            }
        }
    }
```
***
# 构造函数
实际上它们运行了同一段代码。因为缺省参数的构造函数最终会调用最完整参数的构造函数。
一共四个构造函数，其中：
corePoolSize 核心线程数、maximumPoolSize最大线程数、keepAliveTime允许空闲的时间数和TimeUnit类型的unit时间单位、已经任务队列应该存放的阻塞队列BlockingQueue < Runnable > 这五个参数是必须指定的。
剩下的ThreadFactory类型的线程工厂方法 和 RejectedExecutionHandler类型的拒绝策略这2个参数是缺省的，不指定的话会使用默认的设置。
```java
 /**
     * 使用给定的初始参数和默认的线程工厂以及拒绝策略执行程序构造一个
     * ThreadPoolExecutor 实例。使用 Executors 工厂方法而不是这个通用函数
     * 可能会更方便。
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue} is null
     */
    //线程池最少参数，为，核心线程数，最大线程数，允许空闲的时间数，空闲时间单位，阻塞队列。
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), defaultHandler);   //使用默认的线程工厂，和默认的拒绝策略。
    }

    /**
     * 根据给定的参数和默认的拒绝策略构造新的 ThreadPoolExecutor。
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} is null
     */
    //比上面多指定了一个线程工厂。
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory, defaultHandler);   //使用指定的线程工厂
    }

    /**
     * 根据给定的参数和默认的拒绝策略构造新的 ThreadPoolExecutor。
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code handler} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), handler);   //指定了拒绝策略
    }

    /**
     * 根据给定的参数构造新的 ThreadPoolExecutor。
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} or {@code handler} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {   //全部参数都指定了
        if (corePoolSize < 0 ||
                maximumPoolSize <= 0 ||
                maximumPoolSize < corePoolSize ||
                keepAliveTime < 0)                       //判断参数合法性，注意最大线程不能小于核心线程数，但是可以等于，
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)    //判断阻塞队列，线程工厂，拒绝策略是否非空
            throw new NullPointerException();
        this.acc = System.getSecurityManager() == null ?  //安全性
                null :
                AccessController.getContext();
        this.corePoolSize = corePoolSize;                      //赋值成员变量
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }
```
# 关键方法
## execute
* 首先得到线程池状态，如果当前工作线程数小于核心线程数，那么调用addWorker方法将core设置为true，尝试创建核心线程，如果创建成功直接返回。
* 上一步创建失败，那么重新获取线程池状态，如果此时线程池状态是RUNNING,说明上一步失败时核心线程数已满，那么尝试offer将任务添加到阻塞队列中，如果添加成功，再一次检查线程池状态，
	* 如果不为RUNNING了，那就remove将任务从队列中删除，进行回滚，删除的任务将使用拒绝策略执行。
	* 如果还是RUNNING状态，那么判断如果工作线程为0，就添加一个没有初始任务的空闲工作线程
* 以上都不满足，即工作线程超过核心线程，且任务队列应该阻塞，那么尝试创建个辅助工作线程来执行指定的任务，如果创建失败，有可能是因为超出最大线程数，那么就对任务执行拒绝策略。
```java
  /**
     * 在未来的某个时间执行给定的任务。此任务会由一个新的线程或存在于线程池
     * 中的某个线程执行。
     *
     * 如果任务不能提交，要么是因为线程池已经被 shut down，要么是因为
     * 达到了最大容量，由当前的 RejectedExecutionHandler 执行拒绝策略。
     *
     * @param command the task to execute
     * @throws RejectedExecutionException at discretion of
     *         {@code RejectedExecutionHandler}, if the task
     *         cannot be accepted for execution
     * @throws NullPointerException if {@code command} is null
     */
    public void execute(Runnable command) {
        if (command == null)     //任务为空，抛出异常
            throw new NullPointerException();
        /*
         * 按以下三个步骤执行：
         *
         * 1. 如果运行的线程数小于 corePoolSize，尝试创建一个新线程，将给定的
         * 任务作为其第一个执行的任务。调用 addWorker 会自动检查 runState
         * 和 workCount，从而判断是否可以添加工作线程，如果添加失败通过返回 false
         * 以便在不应该添加线程的时候发出错误警报。
         *
         * 2. 如果一个任务可以成功地进入队列，我们仍然需要检查是否应该添加
         * 一个新的线程（从任务入队列到入队完成可能有线程死掉，或者线程池
         * 被关闭）。重新检查线程池状态，如果有必要回滚入队操作。如果没有
         * 线程，则添加一个。
         *
         * 3. 如果任务不能入队，再次尝试增加一个新线程，如果添加失败，意味着
         * 池已关闭或已经饱和，此时执行任务拒绝策略。
         */
        int c = ctl.get();               //得到线程池状态
        if (workerCountOf(c) < corePoolSize) {        //如果工作线程数量小于核心线程数，那么新建Worker执行进来的任务。
            if (addWorker(command, true))       //core设置为true，因为创建核心线程。
                return;                   //添加成功返回。
            c = ctl.get(); //如果添加失败，重新获取线程池状态
        }
        // 如果线程是RUNNING状态，尝试将任务放入阻塞队列，如果放入成功。
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            if (! isRunning(recheck) && remove(command))   //再次检查线程池状态，如果不是RUNNING了，说明不接受新线程，那么移除任务，进行回滚
                reject(command);   //如果移除成功，调用reject 拒绝策略执行新加入的任务。
            else if (workerCountOf(recheck) == 0)    //如果工作线程为0了。
                addWorker(null, false);       //那就加入一个工作线程。
        }
        //如果不是RUNNING，或者阻塞队列已满。尝试不要求核心线程模式，创建一个工作线程。
        else if (!addWorker(command, false))
            reject(command);   //如果当前线程数都超过了最大线程数，或者线程池状态不允许添加工作线程了，那么就拒绝执行。同样调用拒绝策略执行。
    }
```
***
## addWorker
主要包括以下步骤
* 自旋检查线程池执行状态，如果线程池状态是SHUTDOWN后面的状态，即STOP、TIDYING、TERMINATED等，直接返回false，这些状态不允许执行任务，如果是SHUTDOWN，且参数firstTask为null，而任务队列又有线程，那么可以继续。否则其他情况状态是SHUTDOWN，也返回false
* 自旋检查并尝试修改线程池中工作线程数量。主要根据传入的core，判断当前工作线程是否已经饱和。
* 如果以上检查都成功，并成功增加了工作线程数，那么使用指定的firstTask创建新的Worker类代表工作线程，加锁，将创建的工作线程add到workers工作线程集合中，过程中检查是否线程池状态还是满足添加条件，成功添加后，更新largestPoolSize，然后解锁，将Worker中的thead进行start，开始运作工作线程。
* 如果start失败，那么调用addWorkerFailed。workers调用remove删除本次添加的工作线程，然后CAS更新线程池状态，将工作线程数减1。然后调用tryTerminate尝试终止线程池。
```java
  /**
     * 检查是否可以根据当前线程池的状态和给定的边界（核心线程数和最大线程数）
     * 添加新的 worker。如果允许添加，创建并启动一个新的 worker，运行
     * firstTask 作为其第一个任务。如果线程池停止或者即将被 shutdown，则
     * 此方法返回 false。如果线程工厂创建线程失败，也返回 false。如果线程
     * 创建失败，要么是由于线程工厂返回 null，要么是异常（特别是 Thread.start()
     * 的 OOM），将干净利落地回滚。
     *
     * @param firstTask the task the new thread should run first (or
     * null if none). Workers are created with an initial first task
     * (in method execute()) to bypass queuing when there are fewer
     * than corePoolSize threads (in which case we always start one),
     * or when the queue is full (in which case we must bypass queue).
     * Initially idle threads are usually created via
     * prestartCoreThread or to replace other dying workers.
     *
     * @param core if true use corePoolSize as bound, else
     * maximumPoolSize. (A boolean indicator is used here rather than a
     * value to ensure reads of fresh values after checking other pool
     * state).
     * @return true if successful
     */
    //首先判断线程池工作状态是否可以插入新工作线程，其次判断当前线程数是否满足插入，满足则CAS尝试添加工作线程数量，
    //最后使用指定的firstTask创建工作线程，加锁将新建工作线程加入到workers集合中，
    // 加入前还需要判断线程池状态，成功后解锁，并start启动该工作线程
    private boolean addWorker(Runnable firstTask, boolean core) {
        //这个循环，是判断是否可以添加工作线程，如果可以，添加后。CAS修改线程状态ctl。

        retry:
        for (;;) {
            // 获取线程池的运行状态。
            int c = ctl.get();
            int rs = runStateOf(c);

            // Check if queue empty only if necessary.
            /* 这一句话可以转化为
            if ((rs > SHUTDOWN) ||
                 (rs >= SHUTDOWN && firstTask == null) ||
                 (rs >= SHUTDOWN && workQueue.isEmpty()))
             若线程池状态大于 SHUTDOWN 或者
             （状态大于等于 SHUTDOWN 且 firstTask == null）或者
             （状态大于等于 SHUTDOWN 且 任务队列为空）
             则返回添加失败
             */
            if (rs >= SHUTDOWN &&
                    ! (rs == SHUTDOWN &&
                            firstTask == null &&
                            ! workQueue.isEmpty()))
                return false;

            //如果线程状态小于SHUTDOWN。或者为SHOUTDOWN且任务队列不为空。但是指定的firstTask为空。
            //以上也是新任务加进来的条件。

            // 自旋操作增加 线程池状态ctl 中线程数量
            for (;;) {
                int wc = workerCountOf(c); //得到工作线程数量
                // 线程数量已经不小于 CAPACITY 或者根据指定的 core 参数判断是否
                // 满足数量限制的要求
                // （core 为 true 时必须小于 corePoolSize；为 false 必须
                // 小于 maximumPoolSize）
                if (wc >= CAPACITY ||
                        wc >= (core ? corePoolSize : maximumPoolSize)) //当前工作线程是否可添加。
                    return false;
                // 使用 CAS 添加WorkerCount记数，然后退出自旋操作（break 打破外部循环）
                if (compareAndIncrementWorkerCount(c))
                    break retry;
                c = ctl.get();  // Re-read ctl   //如果更新失败，再一次读取线程池状态
                // 如果 runState 改变了，说明有线程操作了线程池，那么从外层循环重新开始（continue 继续外层循环）
                if (runStateOf(c) != rs)
                    continue retry;
                // else 继续内层循环
                // else CAS failed due to workerCount change; retry inner loop
            }
        }

        // 状态修改成功，可以开始创建新的 Worker 了
        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            w = new Worker(firstTask);   //使用指定的Runnable 任务创建工作线程。
            final Thread t = w.thread;   //得到线程工厂分配的线程t。
            if (t != null) {   //如果分配成功。
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();        //上锁。
                try {
                    // 加锁之后再次检查线程池的状态，防止加锁过程中状态被修改
                    int rs = runStateOf(ctl.get()); //得到线程运行状态。

                    // 如果还没有 SHUTDOWN （即 RUNNING）或者正在
                    // SHUTDOWN 且 firstTask 为空，才可以添加 Worker
                    // 第二种情况必须没有指定任务才行，因为SHOUTDOWN以后不接受新任务。
                    // 没有运行任务，那么只是添加了线程而已
                    if (rs < SHUTDOWN ||
                            (rs == SHUTDOWN && firstTask == null)) {
                        // 如果线程已经开启了，抛出 IllegalThreadStateException 异常
                        if (t.isAlive()) // precheck that t is startable
                            throw new IllegalThreadStateException();
                        // workers 类型为 HashSet，由于 HashSet 线程不安全，
                        // 所以需要加锁
                        workers.add(w);
                        int s = workers.size();  //获得HashSet的元素个数，即工作线程的数量。
                        // 如果最大的线程数量小于当前s.更新最大线程池大小 largestPoolSize
                        if (s > largestPoolSize)
                            largestPoolSize = s;
                        // 添加成功，修改标准位为true。
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();   //解锁。
                }
                if (workerAdded) {    //如果添加成功
                    // 添加成功，开启线程，让线程准备就绪。
                    t.start();
                    workerStarted = true;   //设置任务开启成功
                }
            }
        } finally {
            if (! workerStarted)   //如果开启失败。
                // 开启失败，调用addWorkerFailed 方法移除失败的 worker
                addWorkerFailed(w);
        }
        return workerStarted;    //返回是否开启任务成功。
    }
 /**
     * 回滚创建 Worker 的操作。
     * - 从 workers 中删除该 worker
     * - 减小 worker 计数
     * - 再次检查是否终止，防止它的存在阻止了 termination
     */
    private void addWorkerFailed(Worker w) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();  //加锁。
        try {
            // 如果指定的Worker对象。不为空。
            if (w != null)
                workers.remove(w);    //那么调用HashSet的remove方法。删除该工作线程
            // 工作线程计数减一
            decrementWorkerCount();
            // 添加工作线程失败了。导致回滚，尝试终止，防止之前因为这个工作线程的存在，导致没有termination。
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }
```
***
## runWorker
当execute将任务分配到addWorker中创建的新工作线程后，该worker初始化时，内部保存的firstTask存储了execute指定的task,thread保存着线程工厂建立的thread，该Thread实例的Runnable对象时this指针，即worker自身，那么在addWorker会start这个工作线程，就会调用worker中的run方法，而run方法主要就是运行runWorker(this)。
所以该方法就是工作线程具体的运行内容。
主要包括以下步骤：
* 第一次执行时，先得到Worker中firstTask存储的初始任务，并调用unlock，进行初始化同步器状态，以便它可以进行之后的lock操作。
* 进入while循环，这个循环是工作线程执行的主体部分。如果初始任务不为null，进入循环，否则通过getTask成功获得任务再进入循环。
* 加锁。判断线程池状态如果至少是STOP状态，那么进行一次线程自我中断，如果不满足条件就继续执行。
* 执行beforeExecute反复，如果过程抛出异常，那么直接让线程死亡。
* 执行task.run方法，即执行工作线程中的任务。
* 执行afterExecute方法
* 成功执行任务后task赋值为null，工作线程中的completedTasks,加1.然后解锁。
* 如果没有异常发生，继续getTask,但是没有返回任务，则退出while，代表工作线程可能需要销毁了，如果是因为异常退出while，会设置completedAbruptly为true。然后调用processWorkerExit。
```java
  /**
     * 主 worker 运行循环。重复从任务队列获取任务并执行，同时处理一些问题：
     *
     * 1. 我们可能是从第一个初始的任务开始的，在这种情况下，不需要获取
     * 第一个。否则，只要线程池状态是 RUNNING，则需要通过 getTask 获取
     * 任务。如果 getTask 返回 null，则工作线程将有线程池状态更改或配置参数
     * 而退出。
     *
     * 2. 在运行任何任务之前，锁被获取以防止任务执行过程中其他的线程池中断
     * 发生，然后确保除非线程池停止，否则线程不会有中断集。
     *
     * 3. 每一个任务运行之前都调用 beforeExecute，这可能会抛出异常，在
     * 这种情况下，不处理任何任务，让线程死亡（用 completedAbruptly 中止
     * 循环）
     *
     * 4. 假设 beforeExecute 正常完成，运行这个任务，收集它抛出的任何异常
     * 并发送给 afterExecute。我们分别处理 RuntimeException, Error 和任意
     * 可抛出的对象。因为我们不能在 Runnable.run 中重新跑出 Throwables，
     * 所以在抛出时将它们封装在 Error 中（到线程的 UncaughtExceptionHandler）
     * 任何抛出的异常也会导致线程死亡。
     *
     * 5. 在 task.run 完成后，调用 afterExecute，它也可能抛出一个异常，这会
     * 导致线程死亡。
     *
     * @param w the worker
     */
    //当一个工作线程addWorker方法中成功的创建并start后，它就会执行自己Worker类中的run方法，
    //run方法就是使用自身this指针调用runWorker方法，开始进行工作。
    final void runWorker(Worker w) {
        //获取执行run方法的线程，也就是记录在Worker.thread中的线程工厂创建的方法
        Thread wt = Thread.currentThread();
        // 获取当前工作线程执行的任务task。
        Runnable task = w.firstTask;
        w.firstTask = null;     //取出后，就重置为null。
        // Worker继承AQS，初始同步状态为 -1，unlock后就设置为 0，表示可以获取锁，并执行任务
        w.unlock(); // allow interrupts
        boolean completedAbruptly = true; //记录是否是中断。退出。
        try {
            // 当 task 不为 null 或者从 getTask 取出的任务不为 null 时，一直执行While，
            // 不断从任务队列中获取任务来执行，
            while (task != null || (task = getTask()) != null) {
                // 加锁，不是为了防止并发执行任务，因为此任务只会由当前Worker中的thread线程执行，
                // 所以加锁是为了在 shutdown后，终止所有线程时，w.tryLock得不到锁，也就说明worker在执行工作。
                // worker 本身就是一个锁，那么每个 worker 就是不同的锁
                w.lock();
                // 如果线程被停止，确保需要设置中断位的线程设置了中断位
                // 如果没有，确保线程没有被中断。清除中断位时需要再次检查以
                // 以应对 shutdownNow。
                // 如果线程池状态已经至少是 STOP，则中断当前线程。
                // Thread.interrupted 判断是否中断，并且将中断状态重置为未中断，
                // 所以 Thread.interrupted() && runStateAtLeast(ctl.get(), STOP)
                // 的作用是当状态低于 STOP 时，确保不设置中断位。
                // 最后再次检查 !wt.isInterrupted() 判断是否应该中断
                if ((runStateAtLeast(ctl.get(), STOP) ||   //如果当前线程池状态为STOP,那么终止执行任务
                        (Thread.interrupted() &&                  //如果当前状态不是STOP，那么不设置中断为。
                                runStateAtLeast(ctl.get(), STOP))) &&
                        !wt.isInterrupted())   //确保线程没有中断。
                    wt.interrupt();  //那么补充一次中断。
                try {
                    beforeExecute(wt, task);    //在任务执行前，执行该方法
                    Throwable thrown = null;
                    try {
                        // 执行 Runnable 任务。
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);    //在运行完，执行次方法。
                    }
                } finally {
                    // task 置为 null
                    // 记录完成任务数加一
                    // 解锁
                    task = null;
                    w.completedTasks++;   //记数加1.
                    w.unlock();    //解锁
                }
            }
            // 正常while 执行完毕后设置 completedAbruptly 标志位为 false，如果中断。就会是一开始的true。
            completedAbruptly = false;
        } finally {
            // 1. 将 worker 从数组 workers 里删除掉；
            // 2. 根据布尔值 allowCoreThreadTimeOut 来决定是否补充新的 Worker 进数组workers
            processWorkerExit(w, completedAbruptly);
        }
    }
```
***
### processWorkerExit
* 此方法，先移除死亡的worker，更新线程池完成任务总数并维护workers集合，
* 然后调用tryTerminate尝试终止线程池，如果成功，就不执行下一个if
* 如果失败，继续执行，根据work是如何退出的决定是否新建一个工作线程来替换它,具体为如果是异常退出的，如果是删除线程，导致工作的线程不够了，都需要补充替换。
```java
  /**
     * 为正在死亡的 worker 清理和登记。仅限工作线程调用。除非设置了 completedAbruptly，
     * 否则假定 workCount 已经被更改了。此方法从 worker 集合中移除线程，
     * 如果线程因任务异常而退出，或者运行的工作线程数小于 corePoolSize，
     * 或者队列非空但没有工作线程，则可能终止线程池或替换工作线程。
     *
     * @param w the worker
     * @param completedAbruptly if the worker died due to user exception
     */
  
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        // 如果此变量为 true，需要将 workerCount 减一
        if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted
            decrementWorkerCount();

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();   //上锁
        try {
            // 移除当前线程
            completedTaskCount += w.completedTasks;  //记录的完成任务数更新。
            workers.remove(w);   //移除当前worker。
        } finally {
            mainLock.unlock();
        }

        // 尝试终止线程池，
        tryTerminate();

        int c = ctl.get();
        // 线程池状态小于 STOP，说明上面终止失败
        if (runStateLessThan(c, STOP)) {
            // 如果原线程不是因为异常退出的，如果异常退出，标志位会设置为true。
            // 那么不是异常退出就进入此 if 块判断
            // 当 worker 没有任务可执行而退出循环时，completedAbruptly 的值为 false
            if (!completedAbruptly) {
                // 如果 allowCoreThreadTimeOut 为 true，就算是核心线程，只要空闲，
                // 都要移除
                // min 为正常情况下，可能维持的最小的线程数。
                //如果开启了allowCoreThreadTimeOut。允许清楚空闲的核心线程。那么min就有可能取0
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                // 如果得到的min为0，就要判断当前是否还有任务再等待执行，有的话，必须有一个工作线程，所以min为1.
                if (min == 0 && ! workQueue.isEmpty())
                    min = 1;
                // 如果当前工作线程数大于推测出的min.，那直接删除，不用补，因为没有影响
                if (workerCountOf(c) >= min)
                    return; // replacement not needed
            }
            // 如果，原线程是工作异常退出，或者经过上面判断推测出是不应该被删除，应该添加新的空闲线程替换它。
            addWorker(null, false);
        }
    }
```
***
### getTask
 * 首先判断线程池运行状态，是否可以继续执行队列中的任务。(具体为，大于SHOUTDOWN的不可以， 等于SHOUTDOWN的只有队列不为空才可以,RUNNING无条件可以)
 * 其次，判断工作线程是否有资格获取任务。包括超时和当前线程超出最大线程数。（超时首先需要开启Timed判断，具体为当前线程数大于核心线程数开启，或者allowCoreThreadTimeOut为true，那么无条件开启，然后在分配新任务时会调用poll指定时间获取任务，超出时间没有得到任务timeOut会变为true。)
* 判定需要删除该工作线程后，如果工作线程大于1，或者为1但是任务队列为空，那么就减少线程数，返回null。
* 最后以上没返回，那么就分配任务，根据是否开启超时判断，分别使用poll和take两种方案
	* poll如果超时，会设置timedOut为true，开始第二次自旋，
	* take则不会超时，直到得到任务队列的线程就返回该任务。期间如果因为中断退出，也没获得，那么自旋。
```java
 /**
     * 获取等待队列中的任务。基于当前线程池的配置来决定执行任务阻塞或等待
     * 或返回 null。在以下四种情况下会引起 worker 退出，并返回 null：
     * 1. 工作线程数超过 maximumPoolSize。
     * 2. 线程池已停止。
     * 3. 线程池已经 shutdown，且等待队列为空。
     * 4. 工作线程等待任务超时。
     *
     * 并不仅仅是简单地从队列中拿到任务就结束了。
     *
     * @return task, or null if the worker must exit, in which case
     *         workerCount is decremented
     */
   
    private Runnable getTask() {
        //是否超时的标志
        boolean timedOut = false; // Did the last poll() time out?

        for (;;) {
            // 先获取线程池的状态。
            int c = ctl.get();
            int rs = runStateOf(c);   //得到运行状态

            // 以下判断线程在当前线程池状态下，是否需要分配新任务。
            // 状态为以下两种情况时会 workerCount 减 1，并返回 null
            // 1. 状态为 SHUTDOWN，且任务队列为空，说明
            // （说明在 SHUTDOWN 状态线程池中的线程还是会继续取任务执行）
            // 2. 线程池状态为 STOP，那么就不会再进行任务执行了。
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
                // 返回 null，runWorker 中当前线程会退出 while 循环，然后执行
                // processWorkerExit
            }

            int wc = workerCountOf(c);   //得到线程池的工作线程数

            // 然后根据超时限制和核心线程数判断当前线程该不该存活

            // allowCoreThreadTimeOut 表示就算是核心线程也会超时
            //如果打开了allowCoreThreadTimeOut，那么需要限时控制，
            //如果没开始，但是当前线程数超过了指定的核心线程数，那么也要限时控制。
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

            //如果线程数已经到达了指定的maximumPoolSize最大线程数(在线程池工作时，有线程调用
            // setMaximumPoolSize方法修改了线程池最大指标),或者已经超时，
            //并且线程数大于1，或者任务队列为空。
            if ((wc > maximumPoolSize || (timed && timedOut))
                    && (wc > 1 || workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(c))   //那么就CAS减少线程数，不分配任务。
                    return null;     //直接返回。
                continue;  //如果删除失败，重新自旋。
            }

            //到这里，就可以分配新任务了。
            try {
                //workQueue.poll指定了时间，表示如果指定队列有任务直接弹出，如果没任务，等待指定时间，还是没等到就返回null。
                //如果timed开启限时，那么使用poll限时等待，否则使用take，阻塞等待。
                Runnable r = timed ?
                        workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                        workQueue.take();   //如果对列为空，将await，直到有任务进来，就返回任务。
                // 获取到不为空的任务，直接返回。
                if (r != null)
                    return r;
                // 如果 r 等于 null，说明poll超时退出，设置 timedOut 为 true，在下次
                // 自旋时回收
                timedOut = true;
            } catch (InterruptedException retry) {
                // 如果是等待过程发生中断，而导致获取到null，那就设置成没有超时，并继续执行
                timedOut = false;
            }
        }
    }
```
***
### tryTerminate
此方法用于尝试终止线程池，但只有当严格满足线程池终止的条件时，才会完全终止线程池。

* 首先仍然要判断线程池状态。只能是SHUTDOWN状态，且任务队列为空，或者线程运行状态为STOP。
说明当前要进行SHUTDOWN或STOP转移到TIDYING再到TERMINATED。其他情况，说明线程池不应该被终止或者已经有其它线程在终止了，立即返回，不执行后面的操作。

* tryTerminate 在所有可能导致终止线程池的行为（例如减少线程数、移除队列中的任务等）之后调用。每一个线程在消亡时都会调用 tryTerminate，如果还有空闲线程，tryTerminate 只会终止一个空闲线程，然后直接返回。线程池中只有最后一个线程能执行 tryTerminate 的后半部分，也就是把状态改成 TIDYING，最后改成 TERMINATED，表示线程池被停止。
* TERMINATED修改成功后，表示线程池被终止，那么将等待线程池终止的线程，全部唤醒。即调用termination.signalAll。
```java
 /**
     * 如果当前状态为（SHUTDOWN 且 线程池和队列为空）或者（STOP
     * 且线程池为空）,转换到 TERMINATED 状态，。如果有资格终止，但 workerCount
     * 不是零，中断空闲的线程，以确保 shutdown 的信号传播。此方法必须在
     * 任何可能导致终止的动作之后调用——以减少工作线程数量或在 shutdown
     * 期间从队列中删除任务。此方法是非私有的，ScheduledThreadPoolExecutor
     * 也可以访问。
     *
     * 如果线程池状态为 RUNNING 或 （TIDYING 或 TERMINATED）或
     * （SHUTDOWN 且任务队列不为空），不终止或执行任何操作，直接返回。
     *
     * tryTerminate 用于尝试终止线程池，在 shutdow、shutdownNow、remove
     * 中均是通过此方法来终止线程池。此方法必须在任何可能导致终止的行为
     * 之后被调用，例如减少工作线程数，移除队列中的任务，或者是在工作线程
     * 运行完毕后处理工作线程退出逻辑的方法 processWorkerExit。
     * 如果线程池可被终止（状态为 SHUTDOWN 并且等待队列和池任务都为空，
     * 或池状态为 STOP 且池任务为空），调用此方法转换线程池状态为 TERMINATED。
     * 如果线程池可以被终止，但是当前工作线程数大于 0，则调用
     * interruptIdleWorkers方法先中断一个空闲的工作线程，用来保证池
     * 关闭操作继续向下传递。
     */
    final void tryTerminate() {
        for (;;) {
            int c = ctl.get();
            //如果线程池运行状态为RUNNING,或TIDYING,TERMINATED.
            //或者当前状态为SHUTDOWN且工作队列不为空。那么直接返回
            if (isRunning(c) ||
                    runStateAtLeast(c, TIDYING) ||
                    (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))
                return;
            //到这里，只能是SHUTDOWN状态，且工作队列为空，或者线程运行状态为STOP。
            // 如果工作线程数 workCount 不为 0，调用函数关闭一个空闲线程，然后返回
            // (只关闭一个的原因我猜是遍历所有的 worker 消耗太大。)
            if (workerCountOf(c) != 0) { // Eligible to terminate
                interruptIdleWorkers(ONLY_ONE);
                return;
            }
           //如果工作线程数为0，且状态为SHUTDOWN,任务队列为空，或状态是STOP。
            // 此处为线程池状态转化图中满足 SHUTDOWN
            // 或 STOP 转化到 TIDYING 的情况。
            final ReentrantLock mainLock = this.mainLock; //获取主锁
            mainLock.lock();  //转化线程池的状态。需要加锁。
            try {

                //如果成功的将状态转化到TIDYING，工作线程为0，的ctl.那么就需要去调用terminated方法。完成最后到TERMINATED的转换
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        terminated();
                    } finally {
                        // 最后将状态设为 TERMINATED 即可
                        ctl.set(ctlOf(TERMINATED, 0));
                        termination.signalAll();  //将等待线程池终止的线程，全部唤醒。
                    }
                    return;
                }
            } finally {
                mainLock.unlock();
            }
            // else retry on failed CAS
        }
    }
```
***
## shutdown
设置线程池的状态为SHUTDOWN，然后调用interruptIdleWorkers()终止所有空闲的线程。然后调用tryTerminate产生终止线程池。
```java
 /**
     * 启动有序的 shutdown，在此过程中执行以前已经提交的任务，但不接受新
     * 的任务。如果已经 shutdown，调用将没有其它效果。
     *
     * 此方法不等待以前提交的任务完成执行。使用 awaitTermination 来完成。
     *
     * @throws SecurityException {@inheritDoc}
     */
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 检查有没有权限
            checkShutdownAccess();
            // 将状态转变为参数中指定的状态，此处为 SHUTDOWN
            advanceRunState(SHUTDOWN);
            // 终止所有空闲线程
            interruptIdleWorkers();
            onShutdown(); // hook for ScheduledThreadPoolExecutor
        } finally {
            mainLock.unlock();
        }
        tryTerminate();     //尝试终止线程池状态。
    }
     /**
     * interruptIdleWorker 的普通版本
     */
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);  //中断所有工作集合中的空闲线程
    }
    /**
     * 中断可能正在等到任务的线程（空闲线程），以便他们可以检查终止或
     * 配置更改。忽略 SecurityExceptions （防止一些线程没有被中断）
     *
     * @param onlyOne If true, interrupt at most one worker. This is
     * called only from tryTerminate when termination is otherwise
     * enabled but there are still other workers.  In this case, at
     * most one waiting worker is interrupted to propagate shutdown
     * signals in case all threads are currently waiting.
     * Interrupting any arbitrary thread ensures that newly arriving
     * workers since shutdown began will also eventually exit.
     * To guarantee eventual termination, it suffices to always
     * interrupt only one idle worker, but shutdown() interrupts all
     * idle workers so that redundant workers exit promptly, not
     * waiting for a straggler task to finish.
     */
    //
    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock; //获得锁，
        mainLock.lock();//修改线程池中的工作队列，也要加锁。
        try {
            for (Worker w : workers) {  //遍历HashSet中存储的工作线程。
                Thread t = w.thread;
                // 如果线程没有被中断且能获取到锁（能获取到说明它很闲，因为在
                // 正常执行任务的线程都已经获取到锁了），
                // 则尝试中断
                if (!t.isInterrupted() && w.tryLock()) { //未中断，且嗅探拿到了锁，说明空闲
                    try {
                        t.interrupt();    //中断
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                // 如果 onlyOne 为 true，仅中断一个线程
                if (onlyOne)
                    break;
            }
        } finally {
            mainLock.unlock();
        }
    }
```
### shutdownNow
将线程池状态设置为STOP，并且调用interruptWorkers，该方法会遍历工作线程集合workers中所有工作线程，并调用他们的自我中断方法，不管有没有在执行任务，直接中断它们。然后调用tryTerminate。
```java
 /**
     * 尝试停止所有正在执行的任务，停止等待线程，并返回等待执行的任务列表。
     * 从此方法返回时，将从任务队列中删除这些任务。
     *
     * 此方法不会等待正在活跃执行的任务终止。使用 awaitTermination 来完成。
     *
     * 除了尽最大努力停止处理正在执行的任务之外，没有任何其他承诺。此实现
     * 通过 Thread.interrupt 来取消任务，所以没有响应中断的任务可能永远不会
     * 终止。
     *
     * @throws SecurityException {@inheritDoc}
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 检查权限
            checkShutdownAccess();
            // 将状态变成 STOP
            advanceRunState(STOP);
            // 中断 Worker
            interruptWorkers();
            // 调用 drainQueue 将队列中未处理的任务移到 tasks 里
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }
 /**
     * 中断所有线程，即使线程仍然活跃。忽略 SecurityExceptions （防止一些
     * 线程没有被中断）
     */
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers)   //遍历工作线程的集合。
                w.interruptIfStarted();   //中断线程
        } finally {
            mainLock.unlock();
        }
    }
```
***
# 添加新任务的过程
常用的线程池添加一个新任务时，主要有以下步骤：

* 若当前线程数小于核心线程数，创建一个新的线程执行该任务。

* 若当前线程数大于等于核心线程数，且任务队列未满，将任务放入任务队列，等待空闲线程执行。

* 若当前线程数大于等于核心线程数，且任务队列已满

	*  若线程数小于最大线程数，创建一个新线程执行该任务

	* 若线程数等于最大线程数，执行拒绝策略

需要注意的两个地方是：当当前线程数达到核心线程数后，把新来的任务放入队列而不是创建新的线程；当线程数达到最大线程数且任务队列已满的时候，才会执行拒绝策略。
# Executors和常用线程池
Executors 类，提供了一系列工厂方法用于创建线程池，返回的线程池都实现了 ExecutorService 接口，常用以下四种线程池（不包括 ForkJoinPool）：

* newCachedThreadPool: 线程数量没有限制（核心线程数设置为 0，其实有，最大限制为 Integer.MAX_VALUE），如果线程等待时间过长（默认为超过 60 秒），该空闲线程会自动终止。

* newFixedThreadPool: 指定线程数量（核心线程数和最大线程数相同），在线程池没有任务可运行时，不会释放工作线程，还将占用一定的系统资源。

* newSingleThreadPool: 只包含一个线程的线程池（核心线程数和最大线程数均为 1），保证任务顺序执行。

* newScheduledThreadPool: 定长线程池，定时及周期性执行任务。
# 池化
统一管理资源，包括服务器、存储、和网络资源等等。通过共享资源，使用户在低投入中获益。

* 内存池(Memory Pooling)：预先申请内存，提升申请内存速度，减少内存碎片。
* 连接池(Connection Pooling)：预先申请数据库连接，提升申请连接的速度，降低系统的开销。
* 实例池(Object Pooling)：循环使用对象，减少资源在初始化和释放时的昂贵损耗。
