/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import sun.misc.Unsafe;

/**
 * Provides a framework for implementing blocking locks and related
 * synchronizers (semaphores, events, etc) that rely on
 * first-in-first-out (FIFO) wait queues.  This class is designed to
 * be a useful basis for most kinds of synchronizers that rely on a
 * single atomic {@code int} value to represent state. Subclasses
 * must define the protected methods that change this state, and which
 * define what that state means in terms of this object being acquired
 * or released.  Given these, the other methods in this class carry
 * out all queuing and blocking mechanics. Subclasses can maintain
 * other state fields, but only the atomically updated {@code int}
 * value manipulated using methods {@link #getState}, {@link
 * #setState} and {@link #compareAndSetState} is tracked with respect
 * to synchronization.
 *提供一个框架来实现基于先进先出（FIFO）等待队列的阻塞锁和相关同步器
 * （信号量，时间等）。此类被用来作为大多数同步器的重要基础，这些同步器
 *  依赖于单个原子 int 值来表示状态。子类必须定义 protected 方法来改变这个
 *  状态，这些方法定义了这个状态对于被获取或释放的对象意味着什么。在此
 *   基础上，这个类中其它的方法执行所有的排队和阻塞机制。子类可以维护其它
 *  的状态字段，但是只有使用方法 getState, setState, compareAndSetState
 * 自动更新的 int 值才会被同步跟踪。
 *
 * <p>Subclasses should be defined as non-public internal helper
 * classes that are used to implement the synchronization properties
 * of their enclosing class.  Class
 * {@code AbstractQueuedSynchronizer} does not implement any
 * synchronization interface.  Instead it defines methods such as
 * {@link #acquireInterruptibly} that can be invoked as
 * appropriate by concrete locks and related synchronizers to
 * implement their public methods.
 *子类应该定义为非 public 的内部辅助类，用于实现其封闭类的同步属性。
 *   AbstractQueuedSynchronizer 类不实现任何同步接口，相反，它定义了
 *   acquireInterruptibly 等方法，这些方法可以被具体的锁和相关的同步器适当地
 *   调用来实现它们的 public 方法。
 *
 * <p>This class supports either or both a default <em>exclusive</em>
 * mode and a <em>shared</em> mode. When acquired in exclusive mode,
 * attempted acquires by other threads cannot succeed. Shared mode
 * acquires by multiple threads may (but need not) succeed. This class
 * does not &quot;understand&quot; these differences except in the
 * mechanical sense that when a shared mode acquire succeeds, the next
 * waiting thread (if one exists) must also determine whether it can
 * acquire as well. Threads waiting in the different modes share the
 * same FIFO queue. Usually, implementation subclasses support only
 * one of these modes, but both can come into play for example in a
 * {@link ReadWriteLock}. Subclasses that support only exclusive or
 * only shared modes need not define the methods supporting the unused mode.
 *此类支持默认独占模式和共享模式中的一种或两种。当以独占模式获取时，
 *  其它线程尝试获取都不会成功。由多个线程获取的共享模式有可能会（但不一定）
 *  会成功。在不同的模式下等待的线程共享相同的 FIFO 队列。通常，实现的子类
 *  只支持其中一种模式，但是两种模式都可以发挥作用，例如在 ReadWriteLock
 *  中。只支持独占或共享模式的子类不需要定义和未使用模式相关的方法。
 *
 * <p>This class defines a nested {@link ConditionObject} class that
 * can be used as a {@link Condition} implementation by subclasses
 * supporting exclusive mode for which method {@link
 * #isHeldExclusively} reports whether synchronization is exclusively
 * held with respect to the current thread, method {@link #release}
 * invoked with the current {@link #getState} value fully releases
 * this object, and {@link #acquire}, given this saved state value,
 * eventually restores this object to its previous acquired state.  No
 * {@code AbstractQueuedSynchronizer} method otherwise creates such a
 * condition, so if this constraint cannot be met, do not use it.  The
 * behavior of {@link ConditionObject} depends of course on the
 * semantics of its synchronizer implementation.
 *这个类定义了一个嵌套的 ConditionObject 类，可由支持独占模式的子类作为
 * Condition 的实现，该子类的方法 isHeldExclusively 报告当前线程是否独占。
 * 使用 getState 方法调用 release 完全释放该对象，使用 acquire 获取给定的
 * 状态值。否则，没有 AbstractQueuedSynchronizer 的方法会创建这样的
 * condition，因此如果不能满足这样的限制，不要使用它。ConditionObject
 *  的行为取决于其同步器实现的语义。
 *
 * <p>This class provides inspection, instrumentation, and monitoring
 * methods for the internal queue, as well as similar methods for
 * condition objects. These can be exported as desired into classes
 * using an {@code AbstractQueuedSynchronizer} for their
 * synchronization mechanics.
 *此类提供了内部队列的检查，检测和检测方法，以及 condition 对象的类似方法。
 *  这些可以根据需要使用 AbstractQueuedSynchronizer 导出到类中以实现其
 * 同步机制。
 *
 * <p>Serialization of this class stores only the underlying atomic
 * integer maintaining state, so deserialized objects have empty
 * thread queues. Typical subclasses requiring serializability will
 * define a {@code readObject} method that restores this to a known
 * initial state upon deserialization.
 *此类的序列化仅存储基础原子 integer 的维护状态，因此反序列化的对象具有
 *  空线程队列。需要序列化的典型子类会定义一个 readObject 方法，在反序列
 *  化时，将其恢复到已知的初始状态。
 *
 * <h3>Usage</h3>
 *用法：
 * <p>To use this class as the basis of a synchronizer, redefine the
 * following methods, as applicable, by inspecting and/or modifying
 * the synchronization state using {@link #getState}, {@link
 * #setState} and/or {@link #compareAndSetState}:
 *想要使用这个类作为同步器的基础，需要重新定义以下方法，在调用 getState,
 *  setState, compareAndSetState 时检查或修改同步状态：
 *
 * <ul>
 * <li> {@link #tryAcquire}排它获取（资源数）
 * <li> {@link #tryRelease}排它释放（资源数）
 * <li> {@link #tryAcquireShared} 共享获取（资源数）
 * <li> {@link #tryReleaseShared}共享释放（资源数）
 * <li> {@link #isHeldExclusively}  是否排他状态。
 *
 * @Saber-01
 *  即不同同步器的state属性值对应的同步器状态是不同的。
 *  通过在这些方法中调用getState,  setState, compareAndSetState
 *  来修改state的状态值，从而定义资源是否可以被访问。
 *  AQS通过暴露以下API来让用户自己解决上面提到的“如何定义资源是否可以被访问”的问题
 * </ul>
 *
 * Each of these methods by default throws {@link
 * UnsupportedOperationException}.  Implementations of these methods
 * must be internally thread-safe, and should in general be short and
 * not block. Defining these methods is the <em>only</em> supported
 * means of using this class. All other methods are declared
 * {@code final} because they cannot be independently varied.
 * 默认情况下这些方法中每一个都会抛出 UnsupportedOperationException
 * 异常。这些方法的实现必须是在内部线程安全的，或者必须简短且不阻塞。
 * 要使用此类必须定义这些方法。所有其他的方法都声明为 final，因为它们
 * 不能独立变化。
 *
 * <p>You may also find the inherited methods from {@link
 * AbstractOwnableSynchronizer} useful to keep track of the thread
 * owning an exclusive synchronizer.  You are encouraged to use them
 * -- this enables monitoring and diagnostic tools to assist users in
 * determining which threads hold locks.
 *你可能还会发现从 AbstractOwnableSynchronizer 继承的方法对跟踪拥有
 * 独占同步器的线程很有用。鼓励用户使用它们，这将启用监视和诊断工具，以
 * 帮助用户确定那些线程持有锁
 *
 * <p>Even though this class is based on an internal FIFO queue, it
 * does not automatically enforce FIFO acquisition policies.  The core
 * of exclusive synchronization takes the form:
 *即使这些类基于内部 FIFO 队列，它也不会自动执行 FIFO 获取策略。独占
 * 同步操作的核心采取以下形式：
 * <pre>
 * Acquire:
 *     while (!tryAcquire(arg)) {
 *          // 如果没有在队列中则进入队列
 *          // 可能会阻塞当前线程
 *        <em>enqueue thread if it is not already queued</em>;
 *        <em>possibly block current thread</em>;
 *     }
 *
 * Release:
 *     if (tryRelease(arg))
 *      // 释放队列的第一个线程
 *        <em>unblock the first queued thread</em>;
 * </pre>
 *
 * (Shared mode is similar but may involve cascading signals.)
 *（共享锁类似，但可能涉及级联信号。）
 * <p id="barging">Because checks in acquire are invoked before
 * enqueuing, a newly acquiring thread may <em>barge</em> ahead of
 * others that are blocked and queued.  However, you can, if desired,
 * define {@code tryAcquire} and/or {@code tryAcquireShared} to
 * disable barging by internally invoking one or more of the inspection
 * methods, thereby providing a <em>fair</em> FIFO acquisition order.
 * In particular, most fair synchronizers can define {@code tryAcquire}
 * to return {@code false} if {@link #hasQueuedPredecessors} (a method
 * specifically designed to be used by fair synchronizers) returns
 * {@code true}.  Other variations are possible.
 *在入队列之前进行 acquire 检查，所以新的正在获取的线程可能会插入到被阻塞
 * 和排队的线程之前。但是，如果需要，你可以定义 tryAcquire 或 tryAcquireShared
 * 以通过内部调用一种或多种检查方法来禁用插入，从而提供一个公平的 FIFO
 * 顺序。特别是，如果 hasQueuedPredecessors（一种专门为公平的同步器设计
 * 的方法）返回 true，大多数公平的同步器可以定义 tryAcquire 以返回 true。
 *其它变化也是有可能的。
 *
 * <p>Throughput and scalability are generally highest for the
 * default barging (also known as <em>greedy</em>,
 * <em>renouncement</em>, and <em>convoy-avoidance</em>) strategy.
 * While this is not guaranteed to be fair or starvation-free, earlier
 * queued threads are allowed to recontend before later queued
 * threads, and each recontention has an unbiased chance to succeed
 * against incoming threads.  Also, while acquires do not
 * &quot;spin&quot; in the usual sense, they may perform multiple
 * invocations of {@code tryAcquire} interspersed with other
 * computations before blocking.  This gives most of the benefits of
 * spins when exclusive synchronization is only briefly held, without
 * most of the liabilities when it isn't. If so desired, you can
 * augment this by preceding calls to acquire methods with
 * "fast-path" checks, possibly prechecking {@link #hasContended}
 * and/or {@link #hasQueuedThreads} to only do so if the synchronizer
 * is likely not to be contended.
 * 对于默认插入（也被称为贪婪，放弃和避免拥护）策略，吞吐量和可伸缩性
 *  通常最高。尽管这不能保证公平，也不会出现饥饿现象，但是可以让较早入队
 *  的线程在较晚入队的线程之间重新竞争。同样，尽管 acquire 不是通常意义上
 *  的自旋，但它可能会在阻塞之前执行 tryAcquire 多次调用，并插入其它计算。
 *  仅仅短暂地保持排他同步的时候，这将给自旋提供大量好处，而不会带来过多
 *  负担。如果需要的话，你可以通过在调用之前对 acquire 进行 “fast-path” 检查
 *  来增强此功能，如果同步器不被竞争的话，可能会预先检查 hashContended
 *  或 hashQueuedThread。
 *
 * <p>This class provides an efficient and scalable basis for
 * synchronization in part by specializing its range of use to
 * synchronizers that can rely on {@code int} state, acquire, and
 * release parameters, and an internal FIFO wait queue. When this does
 * not suffice, you can build synchronizers from a lower level using
 * {@link java.util.concurrent.atomic atomic} classes, your own custom
 * {@link java.util.Queue} classes, and {@link LockSupport} blocking
 * support.
 *此类为同步提供了有效和可扩展的基础，部分是通过将其使用范围限定于依赖
 *  int 状态，acquire 和 release 参数，内部 FIFO 等待队列的同步器。如果这
 *  还不够，你可以使用原子类，你自定义的 Queue 类，和
 *  LockSupport 阻塞支持来从底层创建自己的同步器。
 * <h3>Usage Examples</h3>
 *使用案例：
 * <p>Here is a non-reentrant mutual exclusion lock class that uses
 * the value zero to represent the unlocked state, and one to
 * represent the locked state. While a non-reentrant lock
 * does not strictly require recording of the current owner
 * thread, this class does so anyway to make usage easier to monitor.
 * It also supports conditions and exposes
 * one of the instrumentation methods:
 *这是一个不可重入的互斥锁类，使用值 0 表示非锁定状态，1 表示锁定状态。
 *  尽管非重入锁并不严格记录当前的所有者线程，此类还是选择这样做来让监视
 *  更方便。她还支持 condition，和一种检测方法：
 *
 *  <pre> {@code
 * class Mutex implements Lock, java.io.Serializable {
 * // 我们的内部辅助类
 *   // Our internal helper class
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     // Reports whether in locked state
 *     // 描述是否是锁定状态
 *     protected boolean isHeldExclusively() {
 *       return getState() == 1;
 *     }
 *
 *     // Acquires the lock if state is zero
 *       // 如果状态值是 0 就获取锁
 *     public boolean tryAcquire(int acquires) {
 *       assert acquires == 1; // Otherwise unused
 *       if (compareAndSetState(0, 1)) {
 *         setExclusiveOwnerThread(Thread.currentThread());
 *         return true;
 *       }
 *       return false;
 *     }
 *
 *     // Releases the lock by setting state to zero
 *     // 设置状态值为 0，释放锁
 *     protected boolean tryRelease(int releases) {
 *       assert releases == 1; // Otherwise unused
 *       if (getState() == 0) throw new IllegalMonitorStateException();
 *       setExclusiveOwnerThread(null);
 *       setState(0);
 *       return true;
 *     }
 *
 *     // Provides a Condition
 *      // 提供一个 Condition
 *     Condition newCondition() { return new ConditionObject(); }
 *
 *     // Deserializes properly
 *     // 正确地反序列化
 *     private void readObject(ObjectInputStream s)
 *         throws IOException, ClassNotFoundException {
 *       s.defaultReadObject();
 *       setState(0); // reset to unlocked state
 *     }
 *   }
 *
 *   // The sync object does all the hard work. We just forward to it.
 *   private final Sync sync = new Sync();
 *
 *   public void lock()                { sync.acquire(1); }
 *   public boolean tryLock()          { return sync.tryAcquire(1); }
 *   public void unlock()              { sync.release(1); }
 *   public Condition newCondition()   { return sync.newCondition(); }
 *   public boolean isLocked()         { return sync.isHeldExclusively(); }
 *   public boolean hasQueuedThreads() { return sync.hasQueuedThreads(); }
 *   public void lockInterruptibly() throws InterruptedException {
 *     sync.acquireInterruptibly(1);
 *   }
 *   public boolean tryLock(long timeout, TimeUnit unit)
 *       throws InterruptedException {
 *     return sync.tryAcquireNanos(1, unit.toNanos(timeout));
 *   }
 * }}</pre>
 *
 * <p>Here is a latch class that is like a
 * {@link java.util.concurrent.CountDownLatch CountDownLatch}
 * except that it only requires a single {@code signal} to
 * fire. Because a latch is non-exclusive, it uses the {@code shared}
 * acquire and release methods.
 *这是一个类似 CountDownLatch 的类，但它只需要一个单独的 signal 来触发。
 *  如果一个 latch 是非独占的，它使用可共享的 acquire 和 release 方法。
 *
 *  <pre> {@code
 * class BooleanLatch {
 *
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     boolean isSignalled() { return getState() != 0; }
 *
 *     protected int tryAcquireShared(int ignore) {
 *       return isSignalled() ? 1 : -1;
 *     }
 *
 *     protected boolean tryReleaseShared(int ignore) {
 *       setState(1);
 *       return true;
 *     }
 *   }
 *
 *   private final Sync sync = new Sync();
 *   public boolean isSignalled() { return sync.isSignalled(); }
 *   public void signal()         { sync.releaseShared(1); }
 *   public void await() throws InterruptedException {
 *     sync.acquireSharedInterruptibly(1);
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public abstract class AbstractQueuedSynchronizer
        extends AbstractOwnableSynchronizer
        implements java.io.Serializable {

    private static final long serialVersionUID = 7373984972572414691L;

    /**
     * Creates a new {@code AbstractQueuedSynchronizer} instance
     * with initial synchronization state of zero.
     * 创建初始同步状态为 0 的 AbstractQueuedSynchronizer 实例。
     */
    protected AbstractQueuedSynchronizer() { }

    /**
     * Wait queue node class.
     * 等待队列的节点类。
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

    // Queuing utilities
    // 队列工具
    /**
     * The number of nanoseconds for which it is faster to spin
     * rather than to use timed park. A rough estimate suffices
     * to improve responsiveness with very short timeouts.
     */
    //自旋的 超时阈值
    static final long spinForTimeoutThreshold = 1000L;

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

    // Utilities for various versions of acquire
// 不同版本的 acquire 实现的辅助工具
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

    /*
     * Various flavors of acquire, varying in exclusive/shared and
     * control modes.  Each is mostly the same, but annoyingly
     * different.  Only a little bit of factoring is possible due to
     * interactions of exception mechanics (including ensuring that we
     * cancel if tryAcquire throws exception) and other control, at
     * least not without hurting performance too much.
     */
    //以下是各种各样执行 acquire 操作的方式，在独占/共享和控制模式下各不相同。
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

    // Main exported methods
  //主要方法
    /**
     * Attempts to acquire in exclusive mode. This method should query
     * if the state of the object permits it to be acquired in the
     * exclusive mode, and if so to acquire it.
     *尝试以独占方式 acquire。此方法应该查询对象的状态是否允许以独占模式
     *    acquire，如果允许，则继续进行。
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread. This can be used
     * to implement method {@link Lock#tryLock()}.
     *线程执行 acquire 操作时总是调用此方法。如果此方法提示失败，则线程进入
     * 同步队列，直到其他线程发出 release 的信号。这可以用来实现方法 tryLock。
     *
     * <p>The default
     * implementation throws {@link UnsupportedOperationException}.
     *默认的实现仅仅是抛出 UnsupportedOperationException 异常。需要由扩展了
     * AQS 的同步类来实现。
     * 独占模式下只需要实现 tryAcquire 和 tryRelease，共享模式下只需要实现
     *  tryAcquireShared 和 tryReleaseShared。
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * @return {@code true} if successful. Upon success, this object has
     *         been acquired.
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in exclusive
     * mode.
     * 释放资源。
     * 如果已经释放掉资源返回 true，否则返回 false。
     * <p>This method is always invoked by the thread performing release.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this object is now in a fully released
     *         state, so that any waiting threads may attempt to acquire;
     *         and {@code false} otherwise.
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to acquire in shared mode. This method should query if
     * the state of the object permits it to be acquired in the shared
     * mode, and if so to acquire it.
     *尝试以共享模式 acquire。此方法应该查询对象的状态是否允许在共享模式
     * 下获取它，如果允许的话，才 acquire。
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread.
     *执行 acquire 的线程总是调用此方法。如果此方法报告失败，acquire 方法
     *  可能将线程入队列（如果它没有入队的话），直到通过其他线程的释放发出
     *  信号（signal）。
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * @return a negative value on failure; zero if acquisition in shared
     *         mode succeeded but no subsequent shared-mode acquire can
     *         succeed; and a positive value if acquisition in shared
     *         mode succeeded and subsequent shared-mode acquires might
     *         also succeed, in which case a subsequent waiting thread
     *         must check availability. (Support for three different
     *         return values enables this method to be used in contexts
     *         where acquires only sometimes act exclusively.)  Upon
     *         success, this object has been acquired.
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    //失败返回负数，0代表获取成功，但是没有后续线程会继续成功，
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in shared mode.
     * 共享模式下释放资源。
     * <p>This method is always invoked by the thread performing release.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this release of shared mode may permit a
     *         waiting acquire (shared or exclusive) to succeed; and
     *         {@code false} otherwise
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code true} if synchronization is held exclusively with
     * respect to the current (calling) thread.  This method is invoked
     * upon each call to a non-waiting {@link ConditionObject} method.
     * (Waiting methods instead invoke {@link #release}.)
     * 如果同步状态由当前线程独占则返回 true。此方法在每次调用非等待的
     *  ConditionObject 方法时调用。（相反等待方法时调用 release）
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}. This method is invoked
     * internally only within {@link ConditionObject} methods, so need
     * not be defined if conditions are not used.
     *如果不使用 condition 则此方法不需要定义。
     *
     * @return {@code true} if synchronization is held exclusively;
     *         {@code false} otherwise
     * @throws UnsupportedOperationException if conditions are not supported
     */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

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
     * Attempts to acquire in exclusive mode, aborting if interrupted,
     * and failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquire}, returning on success.  Otherwise, the thread is
     * queued, possibly repeatedly blocking and unblocking, invoking
     * {@link #tryAcquire} until success or the thread is interrupted
     * or the timeout elapses.  This method can be used to implement
     * method {@link Lock#tryLock(long, TimeUnit)}.
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
     * Acquires in shared mode, aborting if interrupted.  Implemented
     * by first checking interrupt status, then invoking at least once
     * {@link #tryAcquireShared}, returning on success.  Otherwise the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted.
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
     * Attempts to acquire in shared mode, aborting if interrupted, and
     * failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquireShared}, returning on success.  Otherwise, the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted or the timeout elapses.
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

    // Queue inspection methods
    // 队列检查方法
    /**
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations due to interrupts and timeouts may occur
     * at any time, a {@code true} return does not guarantee that any
     * other thread will ever acquire.
     * 查询是否有线程等待 acquire。由于任何时间的中断和时限到期将导致线程
     *  取消，返回 true 并不能保证任何其他线程 acquire。
     * <p>In this implementation, this operation returns in
     * constant time.
     *此实现在常数时间内完成。
     * @return {@code true} if there may be other threads waiting to acquire
     */
    //即检查头尾节点是否相等，如果相等说明队列为空。返回false
    //如果不相等，说明同步队列中有线程在等待，返回true
    //因为线程可中断和限时acquire的原因，这个true只能保证那一刻的队列状态
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     * Queries whether any threads have ever contended to acquire this
     * synchronizer; that is if an acquire method has ever blocked.
     * 查询是否有任何线程竞争过此同步器；即是否有 acquire 方法阻塞了。
     * <p>In this implementation, this operation returns in
     * constant time.
     *此实现在常数时间内完成。
     * contend：竞争
     * @return {@code true} if there has ever been contention
     */
    public final boolean hasContended() {
        return head != null;   //head不为null，说明曾经有线程竞争过，即队列已经初始化过
    }

    /**
     * Returns the first (longest-waiting) thread in the queue, or
     * {@code null} if no threads are currently queued.
     *返回队列中第一个阻塞的线程（等待时间最久的线程），如果队列中没有线程
     *  返回 null。
     * <p>In this implementation, this operation normally returns in
     * constant time, but may iterate upon contention if other threads are
     * concurrently modifying the queue.
     *此实现通常在常数时间内返回，但是如果其他线程同时修改队列，会在竞争
     *  的基础上迭代。
     * @return the first (longest-waiting) thread in the queue, or
     *         {@code null} if no threads are currently queued
     */
    //得到队列中第一个阻塞的线程
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * Version of getFirstQueuedThread called when fastpath fails
     * fastpath失败时调用的getFirstQueuedThread版本
     */
    private Thread fullGetFirstQueuedThread() {
        /*
         *
         *  第一个节点通常是 head.next。尝试获取它的线程字段，确保一致性读取：
         * 如果线程字段为 null 或者 s.prev不再是 head，其他线程并发调用 setHead
         * 在两次读取之间。我们在遍历之前尝试两次。
         */
        Node h, s;
        Thread st;
        //遍历前判断两次，确保取到的确实是不为空的head的next指针中的thread。
        if (((h = head) != null && (s = h.next) != null &&
                s.prev == head && (st = s.thread) != null) ||
                ((h = head) != null && (s = h.next) != null &&
                        s.prev == head && (st = s.thread) != null))
            return st;

        /*
         *
         * 运行到这一步表示 head.next 还没有设置，或者在执行 setHead 之后仍然
         * 没有设置。所以我们必须检查是否 tail 是第一个节点。如果不是，从 tail
         * 节点向 head 遍历直到找到第一个有效节点。
         */

        Node t = tail;
        Thread firstThread = null;
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null)            //从tail开始，找到最后一个有效节点。即为从head后的第一个有效节点
                firstThread = tt;
            t = t.prev;
        }
        return firstThread;
    }

    /**
     * Returns true if the given thread is currently queued.
     * 如果指定的线程正在排队即返回 true。
     * <p>This implementation traverses the queue to determine
     * presence of the given thread.
     *此实现通过遍历队列来查找指定线程。（从 tail 到 head）
     *
     * @param thread the thread
     * @return {@code true} if the given thread is on the queue
     * @throws NullPointerException if the thread is null
     */
    //查看当前线程是否在图同步队列中
    public final boolean isQueued(Thread thread) {
        if (thread == null)     //指定线程为空，则抛出异常
            throw new NullPointerException();
        for (Node p = tail; p != null; p = p.prev)   //从队列尾部开始遍历，因为头部经常会被设置。
            if (p.thread == thread)     //找到一个节点中的thread等于指定参数，就返回true
                return true;
        return false;  //没找到返回false
    }

    /**
     * Returns {@code true} if the apparent first queued thread, if one
     * exists, is waiting in exclusive mode.  If this method returns
     * {@code true}, and the current thread is attempting to acquire in
     * shared mode (that is, this method is invoked from {@link
     * #tryAcquireShared}) then it is guaranteed that the current thread
     * is not the first queued thread.  Used only as a heuristic in
     * ReentrantReadWriteLock.
     * 如果第一个显式排队的线程正在以独占模式等待，返回 true。如果此方法
     * 返回 true，且当前线程正在试图以共享模式 acquire（即在
     *  tryAcquireShared 方法中调用），那么当前线程不是队列中第一个线程。
     *  在 ReetrantReadWriteLock 用于启发（？）
     */
    //是否队列的第一个等待获取的是以独占模式在请求资源
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null &&       //头节点不为空
                (s = h.next)  != null &&    //头节点的next也不为空
                !s.isShared()         &&           //如果head.next不是共享节点，
                s.thread != null;             //且这个节点的thread是有效的。那么返回true
    }

    /**
     * Queries whether any threads have been waiting to acquire longer
     * than the current thread.
     * 查询是否有任何线程比当前线程等待执行 acquire 的时间还长。
     *
     * <p>An invocation of this method is equivalent to (but may be
     * more efficient than):
     *  <pre> {@code
     * getFirstQueuedThread() != Thread.currentThread() &&
     * hasQueuedThreads()}</pre>
     *此方法的调用等价于（但是可能更有效）：
     * getFirstQueuedThread() != Thread.currentThread() && hasQueuedThreads()
     *
     * <p>Note that because cancellations due to interrupts and
     * timeouts may occur at any time, a {@code true} return does not
     * guarantee that some other thread will acquire before the current
     * thread.  Likewise, it is possible for another thread to win a
     * race to enqueue after this method has returned {@code false},
     * due to the queue being empty.
     *注意，由于中断和超时导致的取消可能随时发生，返回 true 并不能保证
     * 其他线程在此线程会 acuqire。同样，在此方法返回 false 之后，由于队列
     *  为空，其他线程也可能成功入队。
     *
     * <p>This method is designed to be used by a fair synchronizer to
     * avoid <a href="AbstractQueuedSynchronizer#barging">barging</a>.
     * Such a synchronizer's {@link #tryAcquire} method should return
     * {@code false}, and its {@link #tryAcquireShared} method should
     * return a negative value, if this method returns {@code true}
     * (unless this is a reentrant acquire).  For example, the {@code
     * tryAcquire} method for a fair, reentrant, exclusive mode
     * synchronizer might look like this:
     *此方法用来作为一个公平的同步器，以避免碰撞。这样的一个同步器的
     * tryAcquire 方法应该返回 false，如果此方法返回 true（除非这是一个
     * 可重入的 acquire），它的 tryAcquireShared 方法应该返回一个负值。
     * 例如，一个公平的，可重入的，独占模式同步器的 tryAcquire 方法应该
     *  看起来类似于：
     *  <pre> {@code
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) {   可重入
     *     // A reentrant acquire; increment hold count
     *     return true;
     *   } else if (hasQueuedPredecessors()) {  //如果等待队列中第一个等待acquire的线程不是自己，
     *     return false;    //说明无法获取资源
     *   } else {          //如果没有正在等待获取资源的线程。
     *     // try to acquire normally   就正常acquire。
     *   }
     * }}</pre>
     *
     * @return {@code true} if there is a queued thread preceding the
     *         current thread, and {@code false} if the current thread
     *         is at the head of the queue or the queue is empty
     * @since 1.7
     */
    //
    public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s;
        return h != t && //队列不为空， 且头节点的next不是当前线程
                ((s = h.next) == null || s.thread != Thread.currentThread());
    }


    // Instrumentation and monitoring methods
    //仪器仪表和监控方法
    /**
     * Returns an estimate of the number of threads waiting to
     * acquire.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring system state, not for synchronization
     * control.
     * 返回等待 acquire 线程数量的估计值。这个值只是一个估计值，因为当这个
     * 方法遍历内部数据结构的时候线程数可能会动态变化。此方法用于监控系统
     *  状态，不用于同步控制。
     * @return the estimated number of threads waiting to acquire
     */
    //返回等待队列中acquire的线程数量的估计。
    public final int getQueueLength() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) { //一样从tail向前遍历
            if (p.thread != null)   //当前队列中有效的线程节点才会被记录
                ++n;
        }
        return n;       //返回记录的线程个数n
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive monitoring facilities.
     *返回一个包含正在等待 acquire 的所有线程的集合。因为在构造这个结果的
     *时候，实际的线程集合可能会动态变化，返回的集合只是一个最佳效果的
     * 估计。返回集合的元素没有特定的顺序。此方法用于构建更广泛监视工具。
     * @return the collection of threads
     */
    //同样返回的值只是估计值，因为线程集合是动态变化的。
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();  //使用arraylist记录
        for (Node p = tail; p != null; p = p.prev) {    //同样从tail开始遍历
            Thread t = p.thread;
            if (t != null)          //如果节点线程不为null
                list.add(t);      //添加当前线程到ArrayList中
        }
        return list;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in exclusive mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to an exclusive acquire.
     * 返回一个包含以独占模式等待 acquire 的线程集合。它具有和 getQueuedThreads
     * 相同的属性，只是它只返回独占模式等待的线程。
     * @return the collection of threads
     */
    //和上面的getQueuedThreads不同，这个方法只返回以独占模式等待的线程集合
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) {     //多了一步判断。即如果不是共享模式，才进行下一步加入判断
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in shared mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to a shared acquire.
     *返回一个包含以共享模式等待 acquire 的线程集合。它具有和 getQueuedThreads
     *  相同的属性，只是它只返回独占模式等待的线程。
     * @return the collection of threads
     */
    //和上面2个方法类似，只是返回的是共享模式等待的线程集合
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) {      //只多了一个判断条件
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a string identifying this synchronizer, as well as its state.
     * The state, in brackets, includes the String {@code "State ="}
     * followed by the current value of {@link #getState}, and either
     * {@code "nonempty"} or {@code "empty"} depending on whether the
     * queue is empty.
     *返回能识别此同步器和其状态的字符串。
     * @return a string identifying this synchronizer, as well as its state
     */
    public String toString() {
        int s = getState();  //得到同步器状态
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() +
                "[State = " + s + ", " + q + "empty queue]";
    }


    // Internal support methods for Conditions
//Condition的内部支撑方法
    /**
     * Returns true if a node, always one that was initially placed on
     * a condition queue, is now waiting to reacquire on sync queue.
     *  如果一个节点现在正在同步队列上等待重新 acquire，则返回 true。
     * @param node the node
     * @return true if is reacquiring
     */
    final boolean isOnSyncQueue(Node node) {
        //如果waitStatus为-2，说明它在condition队列中，
        //node.prev说明为头节点，但是头节点是当前获得资源的线程节点，它不算在同步队列中
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false;
        //如果它有后继，那么肯定在队列中，
        //如果为null，肯定不会有后继，因为，取消节点的acquire，节点next赋值也是指向自身而已。
        if (node.next != null) // If has successor, it must be on queue
            return true;
        /*
         * node.prev can be non-null, but not yet on queue because
         * the CAS to place it on queue can fail. So we have to
         * traverse from tail to make sure it actually made it.  It
         * will always be near the tail in calls to this method, and
         * unless the CAS failed (which is unlikely), it will be
         * there, so we hardly ever traverse much.
         */
        /**
         * node.prev 可以是非空的，但还不能确定在队列上，因为将它放在队列上
         * 的 设置为尾节点的CAS操作可能会失败。所以我们必须从 tail 开始遍历确保它确实成功了。
         * 在对这个方法的调用中，它总是在尾部附近，除非 CAS 失败（这是不太
         * 可能的），否则它将永远在那里，因此我们几乎不会遍历太多。
         */
        return findNodeFromTail(node);
    }

    /**
     * Returns true if node is on sync queue by searching backwards from tail.
     * Called only when needed by isOnSyncQueue.
     * 从 tail 开始向前遍历，如果 node 在同步队列上返回 true。
     *   只在 isOnSyncQueue 方法中才会调用此方法。
     * @return true if present
     */
    private boolean findNodeFromTail(Node node) {
        Node t = tail;
        for (;;) {
            if (t == node)    //找到这个节点直接返回true。
                return true;
            if (t == null)   //到头节点还没找到
                return false;    //返回false。
            t = t.prev;
        }
    }

    /**
     * Transfers a node from a condition queue onto sync queue.
     * Returns true if successful.
     * 把 condition 队列中的节点移动到 sync 队列中。
     *  如果成功返回 true。
     * @param node the node
     * @return true if successfully transferred (else the node was
     * cancelled before signal)
     */
    final boolean transferForSignal(Node node) {
        /*
         * If cannot change waitStatus, the node has been cancelled.
         * 如果不能改变状态，说明节点已经被取消了。
         */
        //只有原来的值是, Node.CONDITION才可以改为0，
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;  //如果修改失败，说明节点被取消了，即返回false

        /*
         * Splice onto queue and try to set waitStatus of predecessor to
         * indicate that thread is (probably) waiting. If cancelled or
         * attempt to set waitStatus fails, wake up to resync (in which
         * case the waitStatus can be transiently and harmlessly wrong).
         * 移动到 sync 队列中，并设置前驱节点的状态来表明线程正在等待。
         * 如果取消或者尝试设置状态失败，则唤醒并重新同步（在这种情况下，
         * 等待状态可能暂时错误，但不会造成任何伤害）。
         */
        Node p = enq(node);   //将当前在condition队列中的节点加入到同步队列的尾部，并返回先前的tail
        int ws = p.waitStatus;       //得到当前节点前驱节点的等待状态
        //如果该节点被取消，或者该节点设置为signal失败
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            //则重新唤醒，让其尝试获取资源，重新进行同步状态维护，
            //如果其获取失败，它会在自旋方法中自己维护自己的前驱节点状态。并重新unpark等待acquire。
            LockSupport.unpark(node.thread);
        return true;
    }

    /**
     * Transfers node, if necessary, to sync queue after a cancelled wait.
     * Returns true if thread was cancelled before being signalled.
     *如有必要，在取消等待后将节点传输到同步队列。
     * 如果线程在发出信号之前被取消，则返回true。
     * @param node the node
     * @return true if cancelled before the node was signalled
     */
    final boolean transferAfterCancelledWait(Node node) {
        //如果修改成功，说明节点的等待状态还是-2，即还在等待队列中，
        //还没有被转移到同步队列中，则enq，让它加入到同步队列。
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            enq(node);  //将线程节点加入到同步队列中。
            return true;
        }
        /*
         * If we lost out to a signal(), then we can't proceed
         * until it finishes its enq().  Cancelling during an
         * incomplete transfer is both rare and transient, so just
         * spin.
         * 如果我们丢失了一个signal，则我们不能继续，
         * 直到它完成它的enq()。在不完全转移期间取消是罕见的和短暂的，所以只旋转。
         */
        //如果节点已经不在等待队列中了，
        //但是判断也不在同步队列中，说明发生不完全转移期间取消，则自旋
        while (!isOnSyncQueue(node))
            // 从运行状态转换到就绪状态（让出时间片）
            Thread.yield();
        return false;
    }

    /**
     * Invokes release with current state value; returns saved state.
     * Cancels node and throws exception on failure.
     *  使用当前状态值调用 release；返回保存的状态。
     *   失败则取消节点并抛出异常。
     * @param node the condition node for this wait
     * @return previous sync state
     */
    final int fullyRelease(Node node) {
        boolean failed = true;
        try {
            int savedState = getState();  //得到还没释放时的同步状态
            if (release(savedState)) {   //释放所有资源，如果成功
                failed = false;
                return savedState;  //返回未释放前的状态值
            } else {       //失败抛出异常
                throw new IllegalMonitorStateException();
            }
        } finally {
            if (failed)            //释放失败，
                node.waitStatus = Node.CANCELLED;     //就取消节点
        }
    }

    // Instrumentation methods for conditions
    // 监测 Condition 队列的方法
    /**
     * Queries whether the given ConditionObject
     * uses this synchronizer as its lock.
     * 检查给定 ConditionObject 是否使用此同步器作为其 lock。
     * @param condition the condition
     * @return {@code true} if owned
     * @throws NullPointerException if the condition is null
     */
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * Queries whether any threads are waiting on the given condition
     * associated with this synchronizer. Note that because timeouts
     * and interrupts may occur at any time, a {@code true} return
     * does not guarantee that a future {@code signal} will awaken
     * any threads.  This method is designed primarily for use in
     * monitoring of the system state.
     * 检查是否有线程等待在和此同步器关联的给定 condition 上。注意由于时间
     * 到期和线程中断会在任何时候发生，返回 true 并不保证未来的信号会唤醒
     * 任何线程。此方法用于监控系统状态。
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    //判断在指定condition的队列中是否有等待的线程
    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.hasWaiters();
    }

    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with this synchronizer. Note that
     * because timeouts and interrupts may occur at any time, the
     * estimate serves only as an upper bound on the actual number of
     * waiters.  This method is designed for use in monitoring of the
     * system state, not for synchronization control.
     * 返回与此同步器关联的 condition 上等待的线程数。注意由于时间到期和
     * 线程中断会在任何时候发生，因此估计值仅仅作为实际等待着数量的上限。
     *  此方法用于监视系统状态，而不是作为同步器控制。
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitQueueLength();
    }

    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with this
     * synchronizer.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate. The elements of the
     * returned collection are in no particular order.
     *返回一个包含与此同步器关联的 condition 上所有线程的集合。由于构建此
     *集合的时候，实际线程集合可能会动态变化，返回的集合只是一个最佳的
     *  估计。返回集合的元素没有特定顺序。
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitingThreads();
    }

    /**
     * Condition implementation for a {@link
     * AbstractQueuedSynchronizer} serving as the basis of a {@link
     * Lock} implementation.
     *Condition 的实现
     * <p>Method documentation for this class describes mechanics,
     * not behavioral specifications from the point of view of Lock
     * and Condition users. Exported versions of this class will in
     * general need to be accompanied by documentation describing
     * condition semantics that rely on those of the associated
     * {@code AbstractQueuedSynchronizer}.
     *
     * <p>This class is Serializable, but all fields are transient,
     * so deserialized conditions have no waiters.
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        /** First node of condition queue. */
        /** Condition 队列的第一个节点 */
        private transient Node firstWaiter;
        /** Last node of condition queue. */
        /** Condition 队列的最后一个节点 */
        private transient Node lastWaiter;

        /**
         * Creates a new {@code ConditionObject} instance.
         * //构造函数，
         */
        public ConditionObject() { }

        // Internal methods
     //内部方法
        /**
         * Adds a new waiter to wait queue.
         * @return its new wait node
         * 添加新的等待线程到等待队列中。
         */
        private Node addConditionWaiter() {
            Node t = lastWaiter;    //记录tail
            // If lastWaiter is cancelled, clean out.
            //如果lastWaiter的状态不是-2，说明被取消
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();   //维护队列的状态，该方法会删除等待队列所有的取消节点
                t = lastWaiter;       // t再更新为未取消的lastWaiter
            }
            //建立一个当前线程的等待状态为CONDITION的新节点。
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            if (t == null)    //如果此时队列为空
                firstWaiter = node;    //那么维护队列firstWaiter
            else     //如果不为空
                t.nextWaiter = node;     // 维护单向链表的连接操作
            lastWaiter = node;        //更新队列的尾节点lastWaiter
            return node;
        }

        /**
         * Removes and transfers nodes until hit non-cancelled one or
         * null. Split out from signal in part to encourage compilers
         * to inline the case of no waiters.
         * 删除和转变节点，直到命中一个并未取消或者为 null 的节点。
         * @param first (non-null) the first node on condition queue
         */
        private void doSignal(Node first) {
            do {
                if ( (firstWaiter = first.nextWaiter) == null) //如果下一个节点是null，
                    lastWaiter = null;   //说明队列为空了，即维护lastWaiter。
                first.nextWaiter = null;     //说明通过signal加入到同步队列中的节点，nextWaiter都是null
            } while (!transferForSignal(first) &&  //如果转移节点到同步队列成功就结束循环，如果失败了
                    (first = firstWaiter) != null);       //则更新first为下一节点，如果下一个节点为null，则结束循环
        }

        /**
         * Removes and transfers all nodes.
         * 移除和转移全部节点。
         * @param first (non-null) the first node on condition queue
         */
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;  //队列状态更新为空，维护首尾指针
            do {
                Node next = first.nextWaiter;   //保存first下一个节点
                first.nextWaiter = null;       //说明通过signalAll加入到同步队列中的节点，nextWaiter都是null
                transferForSignal(first);    //转移first
                first = next;      //first更新为它的下一个节点
            } while (first != null);  //一直遍历到队列末尾，停止
        }

        /**
         *  从 condition 队列中删除已取消（状态不是 CONDITION 即为已取消）
         *   的等待节点。
         *   只有在持有锁的时候才调用。在 condition 队列中等待时如果发生节点
         *   取消，且看到 lastWaiter 被取消然后插入新节点时调用。
         *  （addConditionWaiter 函数中调用）。需要使用此方法在没有 signal 的
         * 时候避免保留垃圾。因此即使它需要完整的遍历，也只有在没有信号的
         * 情况下发生超时或者取消时，它才会起作用。它将会遍历所有节点，而不是
         * 停在一个特定的目标上来取消垃圾节点的连接，且不需要在取消频繁发生时
        * 进行多次重复遍历。
         *
         */
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;        //从firstWaiter开始
            Node trail = null;
            while (t != null) {       //未到末尾，就继续遍历
                Node next = t.nextWaiter;    //next存储下一个等待的节点，
                if (t.waitStatus != Node.CONDITION) {  //如果当前节点已经被取消
                    t.nextWaiter = null;       //就将当前节点的指针赋值为null，方便GC
                    if (trail == null)      //如果这时候trail还没赋值。说明还未碰到正常节点，
                        firstWaiter = next;     //那么firstWaiter就更新为现在取消节点的下一个节点，
                    else   //如果trail已经赋值了，说明前面碰到了正常节点，说明firstWaiter已经维护完成
                        trail.nextWaiter = next;    //那么就将trail保存的前一个正常节点的下一个指针赋值给next，
                    if (next == null)       //如果发现，下一个指针是null，说明最后一个节点也是取消状态，所以要更新lastWaiter
                        lastWaiter = trail;
                }
                else  //碰到了正常节点，
                    trail = t;   //如果遇到未取消的正常节点，trail就赋值为当前节点。
                t = next;       //指针遍历
            }
        }

        // public methods

        /**
         * Moves the longest-waiting thread, if one exists, from the
         * wait queue for this condition to the wait queue for the
         * owning lock.
         *将最长等待的线程，（第一个节点）如果存在的话，从 condition 等待
         * 队列移动到拥有锁的等待队列。
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signal() {
            if (!isHeldExclusively()) //判断当前线程是否独占资源
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                (first);     //唤醒等待队列第一个线程，让它转移到同步队列中
        }

        /**
         * Moves all threads from the wait queue for this condition to
         * the wait queue for the owning lock.
         *将 condition 中等待的所有线程移动到拥有锁的等待队列。
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signalAll() {
            if (!isHeldExclusively())     //判断当前线程是否独占资源， 独占同步状态
                throw new IllegalMonitorStateException();   //非独占，抛出监视状态异常
            Node first = firstWaiter;   //从第一个节点开始
            if (first != null)
                doSignalAll(first);    //将所有的节点转移到同步对列中
        }

        /**
         * Implements uninterruptible condition wait.
         * 实现不中断的 condition 队列上等待。
         * <ol>
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * </ol>
         */
        public final void awaitUninterruptibly() {//无法中断的await
            Node node = addConditionWaiter();    //创建新节点加入到等待队列末尾，并得到这个新节点
            //将资源释放，释放失败会抛出异常，
            //所以await方法不在lock与unlock间使用，会抛出异常
            //释放资源成功，返回未释放时候的state。
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {    //如果当前线程节点不在同步队列中。就继续循环
                //将当前线程park，而当其他线程使用了同一个condition调用了signal或者signalAll方法
                //该线程将被转移到同步队列中去。
                //而在transferForSignal,有可能因为前驱节点取消状态后前驱节点设置signal失败，而unpark，
                //这种情况下，会在下面acquireQueued中重新进入不可中断的独占模式acquire。
                //另一种是设置成功，就不会unpark，等到释放资源，调用unparkSuccessor时唤醒，从当前位置开始继续执行
                //这种情况，同样会进入acquireQueued中。获取资源。
                //所以总结来说，await的线程，接收到signal和signalAll方法后只是到同步队列中。需要获取资源才可继续进行
                LockSupport.park(this);
                //因为park的线程运到线程中断，也会唤醒，所以需要判断
                //但是如果该线程没有被signal唤醒因为中断而继续，也无法继续运行，因为它会重新进入while循环，继续park掉。
                if (Thread.interrupted())// 如果线程中断，用标志位 interrupted 记录
                    interrupted = true;  //如果是因为中断而继续运行，就中断标志位记录为true。
            }
            //进入的是不可中断的获取资源方法acquireQueued，即只有获得到资源才会退出自旋方法。
            //如果acquireQueued过程中断过，它返回true，或者该线程自己在等待signal时也被中断过
            if (acquireQueued(node, savedState) || interrupted)   //就补充一次自我中断，
                selfInterrupt();
        }

        /*
         * For interruptible waits, we need to track whether to throw
         * InterruptedException, if interrupted while blocked on
         * condition, versus reinterrupt current thread, if
         * interrupted while blocked waiting to re-acquire.
         * 对于可中断的等待，我们需要跟踪如果阻塞在 condition 中的时候，
         * 是否抛出 InterruptedException，如果在阻塞中中断等待重新获取时，
         * 是否重新中断当前线程。
         */

        /** Mode meaning to reinterrupt on exit from wait */
        /** 退出等待状态时重新中断 */
        private static final int REINTERRUPT =  1;
        /** Mode meaning to throw InterruptedException on exit from wait */
        /** 退出等待状态时抛出 InterruptException */
        private static final int THROW_IE    = -1;

        /**
         * Checks for interrupt, returning THROW_IE if interrupted
         * before signalled, REINTERRUPT if after signalled, or
         * 0 if not interrupted.
         *  检查中断，如果中断发生在 signal 之前则返回 THROW_IE，如果在 signal
         *   之后则返回 REINTERRUPT，如果没有发生返回 0.
         */
        //判断中断发生在什么期间
        //如果未发生，返回0，
        //如果发生了，调用transferAfterCancelledWait(node)方法，
        //如果该方法，成功将当前线程的node转移到同步队列，返回了true，则返回THROW_IE，说明退出了等待状态要抛出异常
        //而如果该方法，发现node已经不在等待队列了，返回了false，则返回REINTERRUPT
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                    (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                    0;
        }

        /**
         * Throws InterruptedException, reinterrupts current thread, or
         * does nothing, depending on mode.
         * 抛出 InterruptedException，再次中断当前线程，或者不做任何操作，
         * 取决于具体的模式。
         */
        private void reportInterruptAfterWait(int interruptMode)
                throws InterruptedException {
            if (interruptMode == THROW_IE)  //如果是等待过程，signal之前中断的。
                throw new InterruptedException();   //则抛出中断异常
            else if (interruptMode == REINTERRUPT)     //如果是signal之后中断的，需要补充一次中断。
                selfInterrupt();
        }

        /**
         * Implements interruptible condition wait.
         * 实现可中断 condition 的 wait 方法。
         *
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled or interrupted.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted())   //可中断的方法，都要实现判断当前线程判断标志
                throw new InterruptedException();
            Node node = addConditionWaiter();       //创建当前线程节点加入到等待队列
            int savedState = fullyRelease(node);   //释放当前线程的资源，并且保存未释放前的同步状态
            int interruptMode = 0;
            // 如果节点不在 sync 中一直循环（阻塞）
            // 同时检查是否发生中断，如果发生则中止循环
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                 //和不可中断的await不同，这里要判断是在等待signal时候中断，还是已经在等待队列，发生中断。
                //如果在signal之前就中断，interruptMode记为-1，并且当前节点被移到同步队列
                //如果在signal之后才中断，interruptMode记为1.
                //而如果没有发生中断interruptMode记为0；
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;     //如果返回的是不为0，说明发生中断，因为await可中断，所以要退出循环。
            }
            //走到这一步，说明线程节点已经移动到了同步队列中
            //使用释放前的状态值去请求资源，如获取失败，则加入到同步队列中等待。
            //调用的是不可中断的acquire，所以如果返回true，说明获取过程中曾被中断唤醒过，
            //而且之前等待signal时，并没有被中断，则将interruptMode赋值为REINTERRUPT(1)。
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            //到这一步，说明已经node已经获取到资源了，这时发现node.nextWaiter不为null，
            //说明该节点在没有接收到signal或signalAll情况下，通过中断加入到了同步对列。
            if (node.nextWaiter != null) // clean up if cancelled
                unlinkCancelledWaiters();     //所以需要清理掉这样取消了的节点。
            if (interruptMode != 0)      //如果非正常退出await，或者非正常获取到acquire。都要进行维护中断状态。
                reportInterruptAfterWait(interruptMode);//signal之前中断抛出中断异常，否则其他情况补充一次线程中断
        }

        /**
         * Implements timed condition wait.
         * <ol>实现 condition 里有时间限制的 wait 方法。
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted())   //等待过程也是可中断的，所以需要提前判断一次
                throw new InterruptedException();
            Node node = addConditionWaiter();      //新建节点加入等待队列
            int savedState = fullyRelease(node);     //释放同步状态，并保存释放前状态
            final long deadline = System.nanoTime() + nanosTimeout; //得到截止时间
            int interruptMode = 0;   //和await方法中类似效果。
            while (!isOnSyncQueue(node)) {     //因为可中断，所以循环可以break。
                if (nanosTimeout <= 0L) {     //如果指定等待时间为负数，在等待队列中取消节点。
                    transferAfterCancelledWait(node); //将节点转移到同步队列。
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)   //如果还未到时限
                    LockSupport.parkNanos(this, nanosTimeout);  //park线程nanosTimeout
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)//如果等待过程中断，就退出。
                    break;
                nanosTimeout = deadline - System.nanoTime();   //剩余时间。
            }
            //不管是中断，还是限时到了，还是signal，都会进同步队列，
            //直到获取到资源才线程才会继续
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)//限时结束，或者中断而加入同步队列的都需要清楚取消的节点
                unlinkCancelledWaiters();
            if (interruptMode != 0) //如果是signal之前被中断抛出异常，之后则补充一次线程中断
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }

        /**
         * Implements absolute timed condition wait.
         * 实现指定截止时间的 condition 中 wait 方法。
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        //和上面方法基本类似
        public final boolean awaitUntil(Date deadline)
                throws InterruptedException {
            long abstime = deadline.getTime();//转化为毫秒
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {//如果截止时间小于当前系统时间
                    timedout = transferAfterCancelledWait(node);  //直接取消节点，并且将节点放入同步队列尾部，转移成功返回true， 表示超时
                    break;
                }
                LockSupport.parkUntil(this, abstime);  //如果没有则调用parkUntil参数为指定的时间
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)  //等待过程可能被中断，检查中断并处理
                    break;
            }
            //不管是时间到了，还是中断了，还是指定时间超时，或者正常接收到signal，
            //都会进入同步队列获取资源。
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)  //如果是取消状态的节点，即超时取消，或中断取消。
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);  //signal前中断会抛出异常。之后会补充中断
            return !timedout;
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        //该方法和awaitNanos(long)基本相同 ，只是参数时间不同，所以将指定时间转化为纳秒即可。
        public final boolean await(long time, TimeUnit unit)
                throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        //  support for instrumentation

        /**
         * Returns true if this condition was created by the given
         * synchronization object.
         *如果这个condition是被指定的同步器创建的，则返回true。
         * @return {@code true} if owned
         */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this; //和condition外部类的this指针比较
        }

        /**
         * Queries whether any threads are waiting on this condition.
         * Implements {@link AbstractQueuedSynchronizer#hasWaiters(ConditionObject)}.
         *查询是否有线程在 condition 中等待。
         * @return {@code true} if there are any waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final boolean hasWaiters() {
            if (!isHeldExclusively())  //判断当前线程是否独占同步状态
                throw new IllegalMonitorStateException();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {//从firstWaiter开始遍历
                if (w.waitStatus == Node.CONDITION)   //只要发现状态为-2的就返回true
                    return true;
            }
            return false;   //没发现返回false，说明没有condition队列没有节点
        }

        /**
         * Returns an estimate of the number of threads waiting on
         * this condition.
         * 返回在 condition 中等待的线程数估计值。
         * Implements {@link AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)}.
         *
         * @return the estimated number of waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final int getWaitQueueLength() {
            if (!isHeldExclusively())   //判断当前线程是否独占同步状态
                throw new IllegalMonitorStateException();
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)     //遍历，发现一个等待状态为-2，就是等待队列有效节点，就记数
                    ++n;
            }
            return n;
        }

        /**
         * Returns a collection containing those threads that may be
         * waiting on this Condition.
         * 返回在 condition 上等待的所有线程的集合。
         * Implements {@link AbstractQueuedSynchronizer#getWaitingThreads(ConditionObject)}.
         *
         * @return the collection of threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively())   //判断当前线程是否独占
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<Thread>();     //返回存储线程类型的arraylist
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }

    /**
     * Setup to support compareAndSet. We need to natively implement
     * this here: For the sake of permitting future enhancements, we
     * cannot explicitly subclass AtomicInteger, which would be
     * efficient and useful otherwise. So, as the lesser of evils, we
     * natively implement using hotspot intrinsics API. And while we
     * are at it, we do the same for other CASable fields (which could
     * otherwise be done with atomic field updaters).
     */
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
}
