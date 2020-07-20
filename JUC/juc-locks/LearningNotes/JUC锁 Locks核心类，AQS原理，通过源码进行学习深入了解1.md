# 概述
AbstractQueuedSynchronizer抽象类（以下简称AQS）是整个java.util.concurrent包的核心。在JDK1.5时，Doug Lea引入了J.U.C包，该包中的大多数同步器都是基于AQS来构建的。AQS框架提供了一套通用的机制来管理同步状态（synchronization state）、阻塞/唤醒线程、管理等待队列。

## AQS核心思想
AQS核心思想是，如果被请求的共享资源空闲，则将当前请求资源的线程设置为有效的工作线程，并且将共享资源设置为锁定状态。如果被请求的共享资源被占用，那么就需要一套线程阻塞等待以及被唤醒时锁分配的机制，这个机制AQS是用CLH队列锁实现的，即将暂时获取不到锁的线程加入到队列中。

AQS框架，分离了构建同步器时的一系列关注点，它的所有操作都围绕着资源——同步状态（synchronization state）来展开，并替用户解决了如下问题：

* 资源是可以被同时访问？还是在同一时间只能被一个线程访问？（共享/独占功能）
* 访问资源的线程如何进行并发管理？（等待队列）
* 如果线程等不及资源了，如何从等待队列退出？（超时/中断）
***
一种常用的方式是实现 AQS 的抽象方法，并将其实例作为某个同步器的属性。AQS 中常用锁的概念有自旋锁（自身循环），重入锁，独占锁，共享锁，读锁，写锁，乐观锁和悲观锁等，这些在后续内容中将会多次出现。
AQS定义两种资源共享方式
*  Exclusive(独占)：只有一个线程能执行，如ReentrantLock。又可分为公平锁和非公平锁：
* Share(共享)：多个线程可同时执行，如Semaphore/CountDownLatch。Semaphore、CountDownLatCh、 CyclicBarrier、ReadWriteLock
***
AQS内部还实现了ConditionObject的内部类，用于子类可以实现条件等待的功能。而它的实现原理也是维护一个队列，只不过这个队列是等待队列，并且可以有1个或多个这样的队列，因为队列和condition是一一对应的，即一个AQS子类同步器可以有创建多个condition，内部维护多个condition队列。
***
概括来讲：AQS内部包含了两种队列，同步队列，它是双向队列，一旦资源出现争抢，那么队列肯定不为空，肯定存在并且唯一，而condition队列，是可有可无的，它主要取决于用户是否使用了condition条件等待，且condition等待队列是单向队列，它可以根据condition个数内部存在零条或多条队列。
在同步器运行过程中，队列情况可能如下：
同步队列：
![同步队列](https://img-blog.csdnimg.cn/20200720102240252.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
condition等待队列：![等待队列](https://img-blog.csdnimg.cn/20200720102803690.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
***
# LockSupport方法和Unsafe的CAS方法
AQS之所以能实现线程阻塞和线程运行，这离不开LockSupport类中的相关park方法和unpark方法。而多线程中为了保证修改的原子性，特别是设置头尾节点时要防止新节点加入、设置节点等待状态时，也要反正多个线程进行修改，这些操作都是使用Unsafe类中的CAS方法。
## LockSupport方法
方法名 | 作用
--- | ----
park() | 无限阻塞当前线程，不需要捕获异常，中断效果和unpark都会使线程继续运行
park(Object blocker) | 与上面方法相比，只是多指定了一个阻塞块，调用了setBlocker(currentThread,object)
parkNanos(Object blocker, long nanos)| 与上面方法相比，多指定了纳秒级时间，在指定时间内park线程，时间结束，线程自动唤醒，期间可以被unpark或中断唤醒
parkNanos(long nanos)| 与上面方法相比，不指定阻塞块
parkUntil(Object blocker, long deadline)| 与parkNanos相比，指定时间方式不同，该方法指定截止时间，到截止时间就自动唤醒，期间也可以被unpark或中断唤醒
parkUntil(long deadline)|  与上面方法相比，不指定阻塞块
unpark(Thread thread) |唤醒指定线程，在park之前先调用unpark线程也会正常运行，即不会出现提前调用而导致线程阻塞

## CAS方法
CAS的全称为Compare-And-Swap，直译就是对比交换。是一条CPU的原子指令，其作用是让CPU先进行比较两个值是否相等，然后原子地更新某个位置的值，经过调查发现，其实现方式是基于硬件平台的汇编指令，就是说CAS是靠硬件实现的，JVM只是封装了汇编调用，那些AtomicInteger类便是使用了这些封装后的接口。
 
简单解释：CAS操作需要输入两个数值，一个旧值(期望操作前的值)和一个新值，在操作期间先比较下在旧值有没有发生变化，如果没有发生变化，才交换成新值，发生了变化则不交换。 CAS操作是原子性的，所以多线程并发使用CAS更新数据时，可以不使用锁。JDK中大量使用了CAS来更新数据而防止加锁(synchronized 重量级锁)来保持原子更新。
在AQS类中有以下一段代码：
```java
 // Unsafe类实例
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long stateOffset; // state内存偏移地址
    private static final long headOffset; // head内存偏移地址
    private static final long tailOffset;  // state内存偏移地址
    private static final long waitStatusOffset;  // tail内存偏移地址
    private static final long nextOffset;  // next内存偏移地址
    // 静态初始化块
    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                    (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                    (Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * CAS head field. Used only by enq.
     * 原子地设置队列的头节点
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS tail field. Used only by enq.
     * 原子地设置队列的尾节点
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS waitStatus field of a node.
     */
    private static final boolean compareAndSetWaitStatus(Node node,
                                                         int expect,
                                                         int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                expect, update);
    }

    /**
     * CAS next field of a node.
     */
    private static final boolean compareAndSetNext(Node node,
                                                   Node expect,
                                                   Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
```
上面定义的CAS方法设置头头节点、尾节点、next指针，已经节点的等待状态，都会在AQS其他方法中经常使用。

# Node节点类
```java
  /**
     * Wait queue node class.
     * 队列的节点类。
     */
    static final class Node {
        /** Marker to indicate a node is waiting in shared mode */
        /** 节点在共享模式下等待的标记 */
        static final Node SHARED = new Node();
        /** Marker to indicate a node is waiting in exclusive mode */
        /** 节点在独占模式下等待的标记 */
        static final Node EXCLUSIVE = null;

        /** waitStatus value to indicate thread has cancelled */
        // 等待状态的值为 0 表示当前节点在 sync 队列中，等待着获取锁
        // 表示等待状态的值，为 1 表示当前节点已被取消调度，进入这个状态
        //的节点不会再变化
        static final int CANCELLED =  1;

        /** waitStatus value to indicate successor's thread needs unparking */
        /** 表示等待状态的值，为 -1 表示当前节点的后继节点线程被阻塞，正在
         * 等待当前节点唤醒。后继节点入队列的时候，会将前一个节点的状态
         * 更新为 SIGNAL ，以便当前节点取消或释放时将后续节点唤醒*/
        static final int SIGNAL    = -1;

        /** waitStatus value to indicate thread is waiting on condition */
        /**  表示等待状态的值，为 -2 表示当前节点等待在 condition 上，当其他
         * 线程调用了 Condition 的 signal 方法后，condition 状态的节点将从
         * 等待队列转移到同步队列中，等到获取同步锁。
         * CONDITION 在同步队列里不会用到*/
        static final int CONDITION = -2;

        /**
         * waitStatus value to indicate the next acquireShared should
         * unconditionally propagate
         */
        /**
         * 表示等待状态的值，为 -3 ，在共享锁里使用，表示下一个 acquireShared
         * 操作应该被无条件传播。保证后续节点可以获取共享资源。
         * 共享模式下，前一个节点不仅会唤醒其后继节点，同时也可能会唤醒
         * 后继的后继节点。
         */
        static final int PROPAGATE = -3;

        /**
         * waitStatus 表示节点状态，有如下几个值（就是上面的那几个）：
         * SIGNAL: 当前节点的后继节点被（或者即将被）阻塞（通过 park），
         * 因此当前节点在释放或者取消时必须接触对后继节点的阻塞。为了避免
         * 竞争，acquire 方法必须首先表明它们需要一个信号，然后然后尝试原子
         * 获取，如果失败则阻塞。
         * CANCELLED：此节点由于超时或中断而取消。节点永远不会离开此状态。
         * 特别是，具有已取消节点的线程不会再阻塞。
         * CONDITION：此节点此时位于条件队列上。在转移之前它不会被用作
         * 同步队列节点，此时状态被设为 0。（这里此值的使用与此字段的其他
         * 用法无关，但是简化了机制）。
         * PROPAGATE：releaseShared 应该被传播到其它节点。这是在 doReleaseShared
         * 中设置的（仅针对头结点），以确保传播能够继续，即使其它操作已经介入了。
         * 0：以上情况都不是，表示当前节点在sync队列中，等待着获取锁
         *
         * 此值以数字形式组织以简化使用。非负值意味着节点不需要发出信号。
         * 因此，大多数代码不需要检查特定的值，只需要检查符号。
         *
         * 对于正常的同步节点此字段初始化为 0，对于条件节点初始化为
         * CONDITION。可以使用 CAS （或者可能的话，使用无条件的 volatile 写）
         * 修改它。
         */
        volatile int waitStatus;

        /**
         * 与当前节点锁依赖的用于检查等待状态的前辈节点建立的连接。在进入
         * 队列时分配，在退出队列时设置为 null（便于垃圾回收）。此外，在查找
         * 一个未取消的前驱节点时短路，这个前驱节点总是存在，因为头结点
         * 绝不会被取消：一个节点只有在成功 acquire 之后才成为头结点。被取消
         * 的线程 acquire 绝不会成功，而且线程只取消自己，不会取消其他节点。
         */
        volatile Node prev;

        /**
         * 与当前节点 unpark 之后的后续节点建立的连接。在入队时分配，在绕过
         * 已取消的前一个节点时调整，退出队列时设置为 null（方便 GC）。入队
         * 操作直到 attachment 之后才会分配其后继节点，所以看到此字段为 null
         * 并不一定意味着节点在队列尾部。但是，如果 next 字段看起来为 null，
         * 我们可以从 tail 往前以进行双重检查。被取消节点的 next 字段设置成指向
         * 其自身而不是 null，以使 isOnSyncQueue 的工作更简单。
         */
        volatile Node next;

        /**
         * The thread that enqueued this node.  Initialized on
         * construction and nulled out after use.
         *  此节点代表的线程。
         */
        volatile Thread thread;

        /**
         * 连接到在 condition 等待的下一个节点。由于条件队列只在独占模式下被
         * 访问，我们只需要一个简单的链式队列在保存在 condition 中等待的节点。
         * 然后他们被转移到队列中重新执行 acquire。由于 condition 只能是排它的，
         * 我们可以通过使用一个字段，保存特殊值SHARED来表示共享模式。
         */
        Node nextWaiter;

        /**
         * Returns true if node is waiting in shared mode.
         * 如果节点在共享模式中处于等待状态，返回 true
         */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * Returns previous node, or throws NullPointerException if null.
         * Use when predecessor cannot be null.  The null check could
         * be elided, but is present to help the VM.
         *返回前一个节点，如果为 null 抛出 NullPointerException 异常。
         *  当前一个节点不为 null 时才能使用。非空检查可以省略，此处是为了辅助
         *   虚拟机
         * @return the predecessor of this node
         */
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }
        // 用来创建初始节点或者共享标记
        Node() {    // Used to establish initial head or SHARED marker
        }
        // addWaiter 使用
        Node(Thread thread, Node mode) {     // Used by addWaiter
            this.nextWaiter = mode;
            this.thread = thread;
        }
        //Condition 使用
        Node(Thread thread, int waitStatus) { // Used by Condition
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }
```
* 同步队列是双向队列，所以prev和next指针，用于存放前后节点的引用。
* nextWaiter则是condition队列中使用的，指向下一个等待队列中的节点，而由于 condition 只能是排它的，我们也可以通过使用一个字段，保存特殊值SHARED来表示共享模式。
* waiterStatus表示当前节点的状态:

节点状态 | 值 | 描述
--- | --- | ---
CANCELLED| 1| 表示当前节点已经被取消调度，有可能是因为超时或中断导致，这个状态下的节点永远不会离开此状态，特别是已取消的线程不会再阻塞，并且这样的节点会离开同步队列
SIGNAL | -1 | 表示当前节点的后继节点线程被阻塞，正在等待当前节点唤醒。后继节点入队列的时候，会将前一个节点的状态 更新为 SIGNAL ，以便当前节点取消或释放时将后续节点唤醒
CONDITION |-2| condition等待队列专用，当等待队列的节点接收到signal，signalALL或者中断、超时退出等待，这个节点的状态值都会更改为0，加入到同步队列的末尾。
PROPAGATE|-3|在共享锁里使用，表示下一个acquireShared操作应该被无条件传播。保证后续节点可以获取共享资源。共享模式下，前一个节点不仅会唤醒其后继节点，也可能会唤醒后继的后继节点
初始化时|0| 默认的状态值，同步队列的新节点会处于这个状态。表示当前线程正在阻塞，等待获取锁
# 成员变量
```java
 /**
     * Head of the wait queue, lazily initialized.  Except for
     * initialization, it is modified only via method setHead.  Note:
     * If head exists, its waitStatus is guaranteed not to be
     * CANCELLED.
     * 同步队列的头结点，延迟初始化。除了初始化之外，只能通过 setHead
     *修改。注意：如果 head 存在的话，其状态必须保证不是 CANCELLED。
     */
    private transient volatile Node head;

    /**
     * Tail of the wait queue, lazily initialized.  Modified only via
     * method enq to add new wait node.
     *  同步队列的尾节点，延迟初始化。只能通过入队方法添加新的节点。
     */
    private transient volatile Node tail;

    /**
     * The synchronization state.
     *  同步状态。
     */
    private volatile int state;

    /**
     * Returns the current value of synchronization state.
     * This operation has memory semantics of a {@code volatile} read.
     *  返回同步状态的当前值。
     *   此操作具有 volatile read 的内存语义。
     * @return current state value
     */
    protected final int getState() {//返回同步状态的当前值。
        return state;
    }

    /**
     * Sets the value of synchronization state.
     * This operation has memory semantics of a {@code volatile} write.
     * 设置同步状态的值。
     * 返回同步状态的当前值。此操作具有 volatile write 的内存语义。
     * @param newState the new state value
     */
    protected final void setState(int newState) {//设置同步状态的值。
        state = newState;
    }

    /**
     * Atomically sets synchronization state to the given updated
     * value if the current state value equals the expected value.
     * This operation has memory semantics of a {@code volatile} read
     * and write.
     * //原子地(CAS操作)将同步状态值设置为给定值update
     * 如果当前同步状态的值等于expect(期望值)
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that the actual
     *         value was not equal to the expected value.
     */
    protected final boolean compareAndSetState(int expect, int update) {
        // See below for intrinsics setup to support this
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

```
head和tail分别记录同步队列的头尾节点，volatile保证了属性在多线程中的可见性。
state代表着同步状态，用于判断什么时候资源可用，它在不同的同步器实现中有着不同的含义
![资源的定义](https://img-blog.csdnimg.cn/20200720120956995.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
***
# 关键方法
## 工具方法
### enq 和 addWaiter
```java
 /**
     * Inserts node into queue, initializing if necessary. See picture above.
     * @param node the node to insert
     * @return node's predecessor
     *  把节点添加到队列中，必要时初始化。
     */
    //返回的是之前的tail。而不是新传入的node
    private Node enq(final Node node) {
        for (;;) {    //在循环内。只有CAS操作成功的将node插入队列尾后，才会退出循环结束该方法。
            Node t = tail;   //获取尾节点
            //如果尾节点为null，则需要初始化并设置新的节点为头节点和尾节点
            if (t == null) { // Must initialize
                // 以 CAS 方式添加，防止多线程添加产生节点覆盖
                if (compareAndSetHead(new Node()))  //使用的是new Node()说明，初始第一个节点应该是空节点。
                    tail = head;       //同时更新尾结点
            } else {     //如果尾节点不为空，
                node.prev = t;        //当前节点的prev赋值为tail，
                // 以 CAS 方式设置尾节点，防止多线程添加产生节点覆盖
                if (compareAndSetTail(t, node)) {
                    t.next = node;    //前一个节点的next指针，指向当前节点。
                    return t;
                }
            }
        }
    }

    /**
     * Creates and enqueues node for current thread and given mode.
     *为当前线程和给定模式创建节点并添加到同步队列尾部，并返回当前线程
     * 所在节点。
     * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
     * @return the new node
     */
    //如果 tail 不为 null，即同步队列已经存在，则以 CAS 的方式将当前线程节点
     //加入到同步队列的末尾。否则，通过 enq 方法初始化一个同步队列，并返回当前节点。
    private Node addWaiter(Node mode) {
        //使用Thread.currentThread()获得当前线程，
        //mode为给定的模式Node.EXCLUSIVE 独占模式， Node.SHARED为共享模式。
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        // 尝试快速入队，失败时载调用 enq 函数的方式入队
        Node pred = tail;   //存储尾节点
        if (pred != null) {   //如果尾节点不为null，
            node.prev = pred;       //插入节点的prev指向记录的pred尾节点
            if (compareAndSetTail(pred, node)) {  //CAS方式设置尾节点，如果成功
                pred.next = node;      //将之前保存的前一节点pred的next赋值为新节点。
                return node;
            }
        }
        //如果以上compareAndSetTail失败了，或者当前队列尾节点tail为null。
        //则需要调用enq方法自旋的将节点插入队列末尾，并且如果尾节点为null，还会进行初始化
        enq(node);
        return node;   //返回新建的节点。
    }
```
***
### setHead 和 setHeadAndPropagate
```java
/**
     * Sets head of queue to be node, thus dequeuing. Called only by
     * acquire methods.  Also nulls out unused fields for sake of GC
     * and to suppress unnecessary signals and traversals.
     *将队列的头节点设置为指定节点，从而退出队列。仅仅在 acquire 方法中
     *   调用。为了进行 GC 和防止不必要的信号和遍历，将不使用的字段设置为 null。
     * @param node the node
     */
    private void setHead(Node node) {  //可以看到head的thread和prev加入时都是null。
        head = node;
        node.thread = null;
        node.prev = null;
    }
 /**
     * Sets head of queue, and checks if successor may be waiting
     * in shared mode, if so propagating if either propagate > 0 or
     * PROPAGATE status was set.
     *指定队列的 head，并检查后继节点是否在共享模式下等待，如果是且
     * （propagate > 0 或等待状态为 PROPAGATE），则传播。
     * @param node the node
     * @param propagate the return value from a tryAcquireShared
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // Record old head for check below//保存头节点
        setHead(node);   //将传入的node，作为队列的头节点，
        /*
         * Try to signal next queued node if:
         *   Propagation was indicated by caller,
         *     or was recorded (as h.waitStatus either before
         *     or after setHead) by a previous operation
         *     (note: this uses sign-check of waitStatus because
         *      PROPAGATE status may transition to SIGNAL.)
         * and
         *   The next node is waiting in shared mode,
         *     or we don't know, because it appears null
         *
         * The conservatism in both of these checks may cause
         * unnecessary wake-ups, but only when there are multiple
         * racing acquires/releases, so most need signals now or soon
         * anyway.
         */
        //propagate>0说明还有资源可以获取。
        //就算没有资源可拿，但是之前的头节点，状态不为0，因为
        //它可能是由其他线程release时设置的PROPAGATE，则还是需要做一次
        //release传播。
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
                (h = head) == null || h.waitStatus < 0) {
            Node s = node.next;
            if (s == null || s.isShared())
                doReleaseShared();
        }
    }
```
***
###  unparkSuccessor 和 doReleaseShared
```java
 /**
     * Wakes up node's successor, if one exists.
     * 唤醒指定节点的后继节点，如果其存在的话。
     * @param node the node
     */
    //unpark - 唤醒
    //成功获取到资源之后，调用这个方法唤醒 head 的下一个节点。由于当前
     //节点已经释放掉资源，下一个等待的线程可以被唤醒继续获取资源。
    private void unparkSuccessor(Node node) {
        /*
         * If status is negative (i.e., possibly needing signal) try
         * to clear in anticipation of signalling.  It is OK if this
         * fails or if status is changed by waiting thread.
         */
        // 如果当前节点没有被取消，更新 waitStatus 为 0。
        int ws = node.waitStatus;   //得到当前节点的等待状态，
        //如果等待状态为负数，即-1 -2 -3 中任意一个状态值
        //为1则此节点由于超时或中断而取消。
        if (ws < 0)    //如果非取消状态，
            compareAndSetWaitStatus(node, ws, 0); //CAS方法设置waitStatus为0。

        /*
         * Thread to unpark is held in successor, which is normally
         * just the next node.  But if cancelled or apparently null,
         * traverse backwards from tail to find the actual
         * non-cancelled successor.
         *  待唤醒的线程保存在后继节点中，通常是下一个节点。但是如果已经被
         * 取消或者显然为 null，则从 tail 向前遍历，以找到实际的未取消后继节点。
         */
        Node s = node.next;  //存储当前节点的next指针
        if (s == null || s.waitStatus > 0) {  //如果node的next指向了null，或者下一个节点是被取消。
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev) //则需要从tail向前遍历，
                if (t.waitStatus <= 0)    //找到一个离node最近的未取消的后继节点，
                    s = t;
        }
        if (s != null)    //如果找到了这个未取消的后继节点，就进行唤醒，
            LockSupport.unpark(s.thread);    //唤醒节点中thread存储的线程
    }

    /**
     * Release action for shared mode -- signals successor and ensures
     * propagation. (Note: For exclusive mode, release just amounts
     * to calling unparkSuccessor of head if it needs signal.)
     * 共享模式下的释放（资源）操作 -- 信号发送给后继者并确保资源传播。
     * （注意：对于独占模式，如果释放之前需要信号，直接调用 head 的
     *     unparkSuccessor。）
     *  在 tryReleaseShared 成功释放资源后，调用此方法唤醒后继线程并保证
     *  后继节点的 release 传播（通过设置 head 的 waitStatus 为 PROPAGATE。
     */
    private void doReleaseShared() {
        /*
         *确保 release 传播，即使有其它的正在 acquire 或者 release。这是试图
         * 调用 head 唤醒后继者的正常方式，如果需要唤醒的话。但如果没有，
         * 则将状态设置为 PROPAGATE，以确保 release 之后传播继续进行。
         * 此外，我们必须在无限循环下进行，防止新节点插入到里面。另外，与
         * unparkSuccessor 的其他用法不同，我们需要知道是否 CAS 的重置操作
         * 失败，并重新检查。
         */
     // 自旋（无限循环）确保释放后唤醒后继节点
        for (;;) {
            Node h = head;   //保存头节点
            if (h != null && h != tail) { //头节点不为空，即队列不为空，
                int ws = h.waitStatus;      //得到头节点的等待状态
                if (ws == Node.SIGNAL) {   //如果等待状态为-1，则说明需要唤醒后一个节点。
                    //因为共享模式可能存在多个线程同时release资源，但是唤醒时只能唤醒一个，
                    //第一个先进这个方法的进程，会尝试设置waitStatus为0，这里需要CAS，
                    //因为其他线程也可能会修改这个状态，修改成功的线程，会执行unparkSuccessor
                    //唤醒后继节点
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // loop to recheck cases 如果失败重新开始循环，
                    // 如果成功，则将头节点等待状态设置为0,然后唤醒后继节点
                    unparkSuccessor(h);
                }
                //其他线程如果进入到这个方法，发现后继节点已经被唤醒，则设置状态为PROPAGATE。
                //以确保 release 之后传播继续进行
                else if (ws == 0 &&
                        !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                // loop on failed CAS
            }
            if (h == head)          //如果头结点改变，继续循环         // loop if head changed
                break;
        }
    }
```
***
### cancelAcquire 
```java
 /**
     * Cancels an ongoing attempt to acquire.
     *取消正在进行的 acquire 尝试。
     *  使 node 不再关联任何线程，并将 node 的状态设置为 CANCELLED。
     * @param node the node
     */
    private void cancelAcquire(Node node) {
        // Ignore if node doesn't exist
        if (node == null)   //如果节点不存在，直接忽略
            return;
        //node节点不再关联线程，即node.thread=null。
        node.thread = null;

        // Skip cancelled predecessors
        //跳过已经 cancel 的前驱节点，找到一个有效的前驱节点 pred
        Node pred = node.prev;   //保存取消节点pred上一个节点，
        while (pred.waitStatus > 0)       //节点的等待状态还是取消，那么继续循环
            node.prev = pred = pred.prev; //继续向前遍历，一直到找到一个有效的前驱节点 pred赋值给node的pred。

        // predNext is the apparent node to unsplice. CASes below will
        // fail if not, in which case, we lost race vs another cancel
        // or signal, so no further action is necessary.
        Node predNext = pred.next;//为了下一步的CAS操作，

        // Can use unconditional write instead of CAS here.
        // After this atomic step, other Nodes can skip past us.
        // Before, we are free of interference from other threads.
        // 这里可以使用无条件写代替 CAS。在这个原子步骤之后，其他节点可以
        // 跳过。在此之前，我们不受其它线程的干扰。
        node.waitStatus = Node.CANCELLED; //节点的等待状态更改为1，代表取消。

        // If we are the tail, remove ourselves.
        // 如果当前节点是 tail，删除自身（更新 tail 为 pred，并使 predNext
        // 指向 null）。
        if (node == tail && compareAndSetTail(node, pred)) {//CAS操作将找到的有效前驱节点pred设为tail。
            compareAndSetNext(pred, predNext, null);
        } else { //如果当前节点不是尾节点，
            // If successor needs signal, try to set pred's next-link
            // so it will get one. Otherwise wake it up to propagate.
            // 如果 node 不是 tail 也不是 head 的后继节点，将 node 的前驱节点
            // 设置为 SIGNAL，然后将 node 前驱节点的 next 设置为 node 的
            // 后继节点。
            int ws;
            if (pred != head &&    //node 不是 head 的后继节点
                    ((ws = pred.waitStatus) == Node.SIGNAL || //CAS将node 的前驱节点设置为 SIGNAL
                            (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                    pred.thread != null) { //并且前驱节点不是头节点
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    compareAndSetNext(pred, predNext, next);//将 node 前驱节点的 next 设置为 node 的后继节点。即跳过当前node。
            } else {
                // // 如果 node 是 head 的后继节点，直接唤醒 node 的后继节点
                unparkSuccessor(node);
            }

            node.next = node; // help GC  被取消的节点，next指向自身
        }
    }
```
***
### shouldParkAfterFailedAcquire、selfInterrupt 和 parkAndCheckInterrupt
```java
 /**
     * Checks and updates status for a node that failed to acquire.
     * Returns true if thread should block. This is the main signal
     * control in all acquire loops.  Requires that pred == node.prev.
     *检查和更新未能成功 acquire 的节点状态。如果线程应该阻塞，返回 true。
     * 这是所有 acquire 循环的主要信号控制。需要 pred == node.prev。
     * @param pred node's predecessor holding status
     * @param node the node
     * @return {@code true} if thread should block
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;  //获得前驱节点的等待状态
        if (ws == Node.SIGNAL)       //如果等待状态为唤醒
            /*
             * This node has already set status asking a release
             * to signal it, so it can safely park.
             * pred 节点已经将状态设置为 SIGNAL，即 node 已告诉前驱节点自己正在
             * 等到唤醒。此时可以安心进入等待状态。
             */
            return true;     //直接返回
        if (ws > 0) {  //如果前驱节点取消状态
            /*
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             * 如果前驱节点取消，跳过前驱节点一直往前找，直到找到一个非
             * CANCEL 的节点，将前驱节点设置为此节点。（中途经过的 CANCEL
             * 节点会被垃圾回收。）
             */
            do {
                node.prev = pred = pred.prev;    //找到一个不是取消状态的前驱结点，赋值为node的prev。
            } while (pred.waitStatus > 0);  //遇到还是取消状态的节点，还是继续跳过
            pred.next = node;  //该前驱节点，的next赋值为当前 node。
        } else { //如果前驱节点非取消状态
            /*
             * waitStatus must be 0 or PROPAGATE.  Indicate that we
             * need a signal, but don't park yet.  Caller will need to
             * retry to make sure it cannot acquire before parking.
             * 如果进行到这里 waitStatus 应该是 0 或者 PROPAGATE。说明我们
             * 需要一个信号，但是不要立即 park。在 park 前调用者需要重试。
             * 使用 CAS 的方式将 pred 的状态设置成 SIGNAL。（例如如果 pred
             * 刚刚 CANCEL 就不能设置成 SIGNAL。）
             */
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);  //就将前驱节点等待状态设置为SIGNAL。
        }
        // 只有当前驱节点是 SIGNAL 才直接返回 true，否则只能返回 false，
        // 并重新尝试。
        return false;
    }

    /**
     * Convenience method to interrupt current thread.
     * 中断当前线程
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * Convenience method to park and then check if interrupted
     * 让线程进入等待状态。park 会让线程进入 waiting 状态。在此状态下有
     *两种途径可以唤醒该线程：被 unpark 或者被 interrupt。Thread 会清除
     * 当前线程的中断标记位。
     * @return {@code true} if interrupted
     */
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted(); //如果当前线程是中断状态而退出的阻塞，返回true。
    }
```
## 不同类型acquire 和 release
AQS实现了独占模式和共享模式，而对于每个模式的acquire，又分为了不可中断，可中断，限时获取这三种方式，所以一共定义了6种不同的获取方法，每个方法都调用自己的执行方法，所以关于acquire列表一共12种方法：
方法 | 功能
--- | ----
acquire|	独占模式获取锁，忽略中断
acquireQueued | 执行独占模式下获取锁，不可中断
acquireInterruptibly|	独占中断模式获取锁
doAcquireInterruptibly|	执行独占中断模式下获取锁
tryAcquireNanos	|独占模式限时获取锁
doAcquireNanos|	执行独占限时模式下获取锁
acquireShared	|共享模式获取锁，忽略中断
doAcquireShared|	执行共享模式下获取锁，不可中断
acquireSharedInterruptibly |	共享中断模式获取锁
doAcquireSharedInterruptibly |	执行共享中断模式下释放锁
tryAcquireSharedNanos |	共享模式限时获取锁
doAcquireSharedNanos |	执行共享限时模式下获取锁
***
### acquire 使用 acquireQueued
```java
 /**
     * Acquires in exclusive mode, ignoring interrupts.  Implemented
     * by invoking at least once {@link #tryAcquire},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquire} until success.  This method can be used
     * to implement method {@link Lock#lock}.
     * 以独占模式 acquire，忽略中断。通过调用一次或多次 tryAcquire 来实现，
     * 成功后返回。否则线程将入队列，可能会重复阻塞或者取消阻塞，直到
     * tryAcquire 成功。此方法可以用来实现 Lock.lock 方法。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     */
    /*此方法流程如下：
            * tryAcquire 尝试获取资源，如果成功直接返回；
            * addWaiter 将该线程加入到同步队列尾部，并且标记为独占模式；
            * acquireQueued 使线程在同步队列中获取资源，直到取到为止。在整个等待
     * 过程中存在被中断过返回 true，否则返回 false；
            * 如果线程在等待过程中被中断，它不会响应。直到获取到资源后才进行自我
     * 中断（selfInterrupt），将中断补上。
     * */
    public final void acquire(int arg) {
        if (!tryAcquire(arg) &&  //如果tryAcquire失败，
                acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) //
            selfInterrupt();  //如果获取到资源，而且还有过中断，需要将中断补上
    }
 /**
     * Acquires in exclusive uninterruptible mode for thread already in
     * queue. Used by condition wait methods as well as acquire.
     *同步队列中的线程自旋时，以独占且不可中断的方式 acquire。
     * 用于 condition 等待方式中的 acquire。
     * @param node the node
     * @param arg the acquire argument  获取的参数
     * @return {@code true} if interrupted while waiting  //如果在等待期间中断
     */
    //注意就算中断，也会继续自旋，知道获得资源为止，才会退出自旋，
    //且如果这个过程中存在因为线程中断而退出park的情况，都要返回true。
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true; // 标记是否成功拿到资源
        try {
            boolean interrupted = false;       //标记等待过程中是否被中断过
            for (;;) {    //线程自旋
                final Node p = node.predecessor();// 获取 node 的前一个节点
                if (p == head && tryAcquire(arg)) {//如果前一节点是头节点，就尝试获取资源，
                    //获取成功继续执行
                    setHead(node);    //将传入的node设为新的头节点。
                    p.next = null; // help GC
                    failed = false;    //成功拿到锁，即改变failed标志位，
                    return interrupted;       //返回中断状态，用于辨别是正常unpark还是中断退出的。
                }
                //如果上面没有返回。说明获取失败或者前驱节点不是头节点，就需要维护节点的状态
                if (shouldParkAfterFailedAcquire(p, node) && //将节点前驱节点设置为signal。
                        parkAndCheckInterrupt())  //park线程，并且如果线程是因为中断结束park。，
                    interrupted = true;    //则，中断标志设置为true。
            }
        } finally {  //如果最后
            if (failed)       //也没有拿到资源。
                cancelAcquire(node);     //则取消正在进行的acquire。
        }
    }
```
***
### acquireInterruptibly使用doAcquireInterruptibly
```java
  /**
     * Acquires in exclusive mode, aborting if interrupted.
     * Implemented by first checking interrupt status, then invoking
     * at least once {@link #tryAcquire}, returning on
     * success.  Otherwise the thread is queued, possibly repeatedly
     * blocking and unblocking, invoking {@link #tryAcquire}
     * until success or the thread is interrupted.  This method can be
     * used to implement method {@link Lock#lockInterruptibly}.
     *以独占模式 acquire，如果中断则中止。
     * 首先检查中断状态，然后至少调用一次 tryAcquire，成功直接返回。否则线程
     * 进入队列等待，可能重复阻塞或者取消阻塞，调用 tryAcquire 直到成功或线程
     * 被中断。此方法可用来实现 lockInterruptibly。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    //过程如下：1.首先先判断当前尝试获得资源的线程中断状态，如果是已经中断状态，抛出异常，
    // 2.如果不是中断状态，就调用tryAcquire尝试获取资源，成功直接返回
    //3.失败的话要调用doAcquireInterruptibly，创建一个当前线程的node独占模式节点加入同步队列尾部
    //4.线程在当前同步队列中获取资源，直到取到资源，正常退出队列，
    //5.或者在等待过程中当被其他线程中断，则会抛出中断异常，并取消对资源的获取操作。

    public final void acquireInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())     //先检查中断状态，如果当前线程还没进入尝试回去前就是中断状态
            throw new InterruptedException();  //直接抛出异常。
        if (!tryAcquire(arg))      //尝试获取资源，失败调用doAcquireInterruptibly
            doAcquireInterruptibly(arg);
    }
     /**
     * Acquires in exclusive interruptible mode.
     * @param arg the acquire argument
     *             以独占中断模式 acquire。
     */
    private void doAcquireInterruptibly(int arg)
            throws InterruptedException {
        // 将当前线程以独占模式创建节点加入同步队列尾部
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {   //线程自旋
                final Node p = node.predecessor();   //获得前驱节点
                if (p == head && tryAcquire(arg)) {   //如果前驱节点是头节点，就尝试获取锁
                    //如果获取成功就继续执行
                    setHead(node);     //设置新的头节点
                    p.next = null; // help GC  方便GC
                    failed = false; //说明成功获得资源
                    return;
                }
                //如果获取失败，维护节点状态，将前驱节点状态设置成 SIGNAL 之后才能安心进入休眠状态。
                // 并且调用park阻塞线程。并检查中断。
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                    throw new InterruptedException();  //如果中断而使线程继续执行，则抛出中断异常
            }
        } finally {           //如果异常，退出自旋
            if (failed)              //未能成功获得资源
                cancelAcquire(node);    //取消当前节点的acquire
        }
    }
```
***
### tryAcquireNanos 使用 doAcquireNanos
```java
 /**
     *尝试以独占模式 acquire，如果中断将中止，如果超出给定时间将失败。首先检查
     *中断状态，然后至少调用一次 tryAcquire，成功立即返回。否则线程进入同步队列，
     * 可能会重复阻塞或取消阻塞，调用 tryAcquire 直到成功或线程中断或超时。
     *  此方法可用于实现 tryLock(long, TimeUnit)。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    //在同步队列中，自旋时间为nanosTimeout，即等待过程中如果中断，就会抛出异常
    //如果等待过程中没有获取资源，就返回false，说明获取资源失败了。
    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())  //因为此方法也可响应中断，所以首先要判断当前线程状态。
            throw new InterruptedException();
        return tryAcquire(arg) ||     //尝试获取资源，成功直接返回，
                doAcquireNanos(arg, nanosTimeout);   //失败调用doAcquireNanos
    }
      /**
     * Acquires in exclusive timed mode.
     *以独占限时模式 acquire
     * @param arg the acquire argument
     * @param nanosTimeout max wait time
     * @return {@code true} if acquired
     */
    //一开始能获取到就直接执行，
    //如果不能获取，就调用park进行限时等待，等待结束再次循环尝试获取
    //等待过程如果是被中断而唤醒的，直接抛出中断异常
    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)    //如果参数小于等于0，直接false。
            return false;
        //计算截止时间。
        final long deadline = System.nanoTime() + nanosTimeout;
        //新建独占模式的线程节点。
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;    //标志位，是否成功获取资源
        try {
            for (;;) {  //线程自旋
                final Node p = node.predecessor();  //获得前驱节点
                if (p == head && tryAcquire(arg)) {  //如果前驱节点为头节点，尝试获得锁，
                    //成功获得锁，继续执行，
                    setHead(node);  //设置新的头节点
                    p.next = null; // help GC  方便GC
                    failed = false;
                    return true;   //返回true。
                }
                //查看剩余多少等待时间
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)  //如果达到截止时间
                    return false;    //表示等待过程中并没有获得，则直接返回false，退出自旋
                if (shouldParkAfterFailedAcquire(p, node) &&   //维护节点没有获得资源时的状态，
                        nanosTimeout > spinForTimeoutThreshold)  //如果还未到等待时限
                    //该方法只会在第二次自旋循环，已经将node的前驱节点的waitStatus设置为SIGNAL时才会执行
                    LockSupport.parkNanos(this, nanosTimeout); //就停止线程nanosTimeout秒，
                //时限过程可以被unpark提前唤醒，也可以时间到了自己唤醒，
                // 或者通过线程中断，也会退出park
                if (Thread.interrupted())          //如果等待过程，是被中断唤醒的
                    throw new InterruptedException();
            }
        } finally {   //如果是因为中断，退出自旋
            if (failed) //并且未获得资源
                cancelAcquire(node);   //则同样取消获取资源的请求
        }
    }
```
***
### acquireShared 使用 doAcquireShared
```java
  /**
     * Acquires in shared mode, ignoring interrupts.  Implemented by
     * first invoking at least once {@link #tryAcquireShared},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquireShared} until success.
     *忽略中断，以共享模式 acquire。首先调用至少一次 tryAcquireShared，
     * 成功后返回。否则线程将排队，可能会重复阻塞和取消阻塞，不断调用
     *  tryAcquiredShared 直到成功。
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     */
    public final void acquireShared(int arg) {  //共享模式获取资源
        if (tryAcquireShared(arg) < 0)       //尝试获取资源。如果返回小于0，获取失败
            doAcquireShared(arg);  //进入同步队列，直到成功获取资源。
    }
      /**
     * Acquires in shared uninterruptible mode.
     *  共享  不中断模式下执行 acquire。
     * @param arg the acquire argument
     */
    private void doAcquireShared(int arg) {
        //新增共享模式节点。
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;    //标志是否成功获取资源
        try {
            boolean interrupted = false;     //标记是否中断
            for (;;) {     //线程自旋
                final Node p = node.predecessor(); //获得前驱节点
                if (p == head) {      //如果某个时刻前驱节点是头结点
                    int r = tryAcquireShared(arg); //尝试获取资源，
                    if (r >= 0) {    //如果大于等于0，说明获取成功，
                        //设置队列的 head为node，并检查后继节点是否在共享模式下等待
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted)    //// 如果在等待过程中被中断过，那么此时将中断补上
                            selfInterrupt();
                        failed = false;// 改变 failed 标志位，表示获取成功，然后返回（退出此函数）
                        return;
                    }
                }
                //维护节点状态，并且如果设置好了状态，那就调用parkAndCheckInterrupt进行休眠
                //进入park状态。等待中断，或unpark。
                //休眠过程如果被中断，则设置中断标志为true。
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)       //退出自旋，如果没成功获取，则取消线程的请求。
                cancelAcquire(node);
        }
    }
```
### acquireSharedInterruptibly 使用 doAcquireSharedInterruptibly
```java
  /**
     *  以共享模式 acquire，如果中断将中止。首先检查中断状态，然后调用至少一次
     *   tryAcquireShared，成功立即返回。否则，将进入同步队列，可能会重复阻塞
     *   和取消阻塞，调用 tryAcquireShared 直到成功或者线程中断。
     *
     * @param arg the acquire argument.
     * This value is conveyed to {@link #tryAcquireShared} but is
     * otherwise uninterpreted and can represent anything
     * you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {  //同样不支持中断的，都要事先检查当前线程的中断状态，
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0)   //获取资源失败
            doAcquireSharedInterruptibly(arg); //进入同步队列，如果过程中被中断抛出异常
    }
 /**
     * Acquires in shared interruptible mode.
     * 中断模式的 共享acquire
     * @param arg the acquire argument
     */
    private void doAcquireSharedInterruptibly(int arg)
            throws InterruptedException {
        final Node node = addWaiter(Node.SHARED); //新建共享模式节点
        boolean failed = true;
        try {
            for (;;) {    //线程自旋
                final Node p = node.predecessor();
                if (p == head) {    //如果某一时刻，前驱节点为头节点
                    int r = tryAcquireShared(arg);   //尝试获取资源
                    if (r >= 0) {//如果成功
                        setHeadAndPropagate(node, r); //设置头节点。
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                //如果没有获得资源，则设置好前置节点以后进入休眠
                //休眠条件设置好后，调用parkAndCheckInterrupt，进入park阻塞。
                //再等待过程如果线程被中断，则parkAndCheckInterrupt返回true。
                //这时抛出中断异常。结束自旋，跳到finally代码段
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)      //如果最终没有获得锁而结束自旋
                cancelAcquire(node);    //那么就取消线程争取资源的请求。
        }
    }
```
### tryAcquireSharedNanos 使用 doAcquireSharedNanos
```java 
   /**
     *尝试以共享模式 acquire，如果中断将停止，如果超过时限将失败。首先检查
     * 中断状态，然后至少调用一次 tryAcquireShared，成功立即返回。否则线程将
     * 进入同步队列，可能会重复阻塞和取消阻塞，调用 tryAcquireShared 直到成功
     * 或线程中断或超时。
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    //在同步队列中，自旋时间为nanosTimeout，即等待过程中如果中断，就会抛出异常
    //如果等待过程中没有获取资源，就返回false，说明获取资源失败了。
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquireShared(arg) >= 0 ||   //如果直接获取成功，返回true，否则为false
                doAcquireSharedNanos(arg, nanosTimeout);//上一个添加为false，则需要进入同步队列nanosTimeout秒
    }
/**
     * Acquires in shared timed mode.
     *以共享限时模式 acquire。
     * @param arg the acquire argument
     * @param nanosTimeout max wait time
     * @return {@code true} if acquired
     */
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)  //传入为负数直接返回false。
            return false;
        final long deadline = System.nanoTime() + nanosTimeout; //计算截止时间
        final Node node = addWaiter(Node.SHARED);//创建共享模式节点，放到队列后。
        boolean failed = true;
        try {
            for (;;) {   //线程自旋争用资源，主要是通过判断前驱节点是否是头节点
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg); //尝试获取资源
                    if (r >= 0) {          //如果成功
                        setHeadAndPropagate(node, r);    //设置新的头节点，并且判断下一个节点是否是在共享模式下等待。
                        p.next = null; // help GC
                        failed = false;    //成功获取
                        return true;
                    }
                }
                nanosTimeout = deadline - System.nanoTime(); //得到现在还剩多久，时限结束
                if (nanosTimeout <= 0L)     //如果指定时限结束了。也
                    return false;
                //将节点的前驱节点设置为signal，然后第二次循环检查才会返回true。
                if (shouldParkAfterFailedAcquire(p, node) &&//设置休眠条件后
                        nanosTimeout > spinForTimeoutThreshold)  //如果这时还没有到时限
                    LockSupport.parkNanos(this, nanosTimeout);   //那么它就调用parkNanos等待剩余的时限。
                if (Thread.interrupted())   //如果等待过程被中断，就抛出异常
                    throw new InterruptedException();
            }
        } finally {
            if (failed)   //如果退出自旋时，没有获得当前资源，则取消当前节点的acquire请求。
                cancelAcquire(node);
        }
    }
```
### 独占模式的release 和 共享模式的releaseShared
```java
   /**
     * Releases in exclusive mode.  Implemented by unblocking one or
     * more threads if {@link #tryRelease} returns true.
     * This method can be used to implement method {@link Lock#unlock}.
     *独占模式下 release。如果 tryRelease 返回 true，则通过解除一个或多个
     *  线程的阻塞来实现。此方法可以用来实现 Lock 的 unlock 方法。
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryRelease} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @return the value returned from {@link #tryRelease}
     */
    public final boolean release(int arg) {
        if (tryRelease(arg)) {// 通过 tryRelease 的返回值来判断是否已经完成释放资源
            Node h = head;      //如果已经释放了，则资源可用，就先取得同步队列头节点。
            if (h != null && h.waitStatus != 0)     //如果头节点不为空，且头节点的等待状态不为0。
                unparkSuccessor(h);// 唤醒同步队列里的下一个线程
            return true;
        }
        return false;   //失败释放，返回false
    }
      /**
     * Releases in shared mode.  Implemented by unblocking one or more
     * threads if {@link #tryReleaseShared} returns true.
     *共享模式下的 release 操作。如果 tryReleaseShared 返回 true，唤醒一个
     *  或多个线程。
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryReleaseShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @return the value returned from {@link #tryReleaseShared}
     */
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {// 尝试释放资源
            doReleaseShared(); // 唤醒后继节点
            return true;
        }
        return false;
    }
```
***
以下部分，编写在JUC锁 Locks核心类，AQS原理，通过源码进行学习深入了解2中

#  ConditionObject内部类
## Condition的内部支撑方法
## ConditionObject内部方法
# 队列检查和监控方法
# 参考
[AQS原理](https://www.pdai.tech/md/java/thread/java-thread-x-lock-AbstractQueuedSynchronizer.html)
[J.U.C之locks框架：AQS综述(1)](https://segmentfault.com/a/1190000015562787)
