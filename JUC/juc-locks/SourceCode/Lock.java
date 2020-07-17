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

/**
 * {@code Lock} implementations provide more extensive locking
 * operations than can be obtained using {@code synchronized} methods
 * and statements.  They allow more flexible structuring, may have
 * quite different properties, and may support multiple associated
 * {@link Condition} objects.
 *与使用 synchronized 方法和 synchronized 代码块相比，Lock 的实现提供了
 * 更广泛的锁定操作。它们允许更灵活的结构，可能有更多的属性，支持多个
 *  关联的 Condition 对象。
 *
 * <p>A lock is a tool for controlling access to a shared resource by
 * multiple threads. Commonly, a lock provides exclusive access to a
 * shared resource: only one thread at a time can acquire the lock and
 * all access to the shared resource requires that the lock be
 * acquired first. However, some locks may allow concurrent access to
 * a shared resource, such as the read lock of a {@link ReadWriteLock}.
 * 锁是一种控制多个线程对共享资源访问的工具。通常锁提供对共享资源的独占
 *  访问：同一时间只有一个线程可以获得锁，对共享资源的所有访问都需要首先
 *  获取锁。但是，有些锁可能允许并发访问共享资源，比如 ReadWriteLock 锁。
 *
 * <p>The use of {@code synchronized} methods or statements provides
 * access to the implicit monitor lock associated with every object, but
 * forces all lock acquisition and release to occur in a block-structured way:
 * when multiple locks are acquired they must be released in the opposite
 * order, and all locks must be released in the same lexical scope in which
 * they were acquired.
 *使用 synchronized 方法或者 synchronized 代码块访问和对象关联的隐式
 *  监视器锁，但是所有的 acquire 和 release 都以 block-structure 的方式发生：
 * 多个锁 acquire 时必须以相反的顺序 release，所有的锁都必须以 acquire 对应
 * 的形式 release。
 *
 * <p>While the scoping mechanism for {@code synchronized} methods
 * and statements makes it much easier to program with monitor locks,
 * and helps avoid many common programming errors involving locks,
 * there are occasions where you need to work with locks in a more
 * flexible way. For example, some algorithms for traversing
 * concurrently accessed data structures require the use of
 * &quot;hand-over-hand&quot; or &quot;chain locking&quot;: you
 * acquire the lock of node A, then node B, then release A and acquire
 * C, then release B and acquire D and so on.  Implementations of the
 * {@code Lock} interface enable the use of such techniques by
 * allowing a lock to be acquired and released in different scopes,
 * and allowing multiple locks to be acquired and released in any
 * order.
 * 虽然 synchronized 方法和 synchronized 代码块的作用域机制使基于监视器锁
 *  变成的机制更加容易，并且帮助避免了许多涉及锁的常见编程错误，但是在
 *  某些情况下需要更灵活地使用锁。例如，一些并发访问的数据结构的遍历算法
 *  需要使用 hand-over-hand 或者链锁：先获取节点 A，然后是节点 B，然后释放
 *  A，然后获取 C，然后释放 B，然后获取 D 等。Lock 接口的实现允许在不同范围内
 *  获取和释放一个锁，并允许以任意顺序获取和释放多个锁。
 *
 * <p>With this increased flexibility comes additional
 * responsibility. The absence of block-structured locking removes the
 * automatic release of locks that occurs with {@code synchronized}
 * methods and statements. In most cases, the following idiom
 * should be used:
 *伴随着灵活性增加的是额外的责任。块结构锁的消失消除了使用 synchronized
 *  产生的锁的自动释放。在大多数情况下，应该使用如下习语：
 *  <pre> {@code
 * Lock l = ...;
 * l.lock();
 * try {
 *   // access the resource protected by this lock
 * } finally {
 *   l.unlock();
 * }}</pre>
 *
 * When locking and unlocking occur in different scopes, care must be
 * taken to ensure that all code that is executed while the lock is
 * held is protected by try-finally or try-catch to ensure that the
 * lock is released when necessary.
 * 当锁定和解锁发生在不同的作用域时，必须注意保持持有锁时执行的所有代码
 *  都收到 try-finally 或 try-catch 的保护，以确保在必要时释放锁。
 *
 * <p>{@code Lock} implementations provide additional functionality
 * over the use of {@code synchronized} methods and statements by
 * providing a non-blocking attempt to acquire a lock ({@link
 * #tryLock()}), an attempt to acquire the lock that can be
 * interrupted ({@link #lockInterruptibly}, and an attempt to acquire
 * the lock that can timeout ({@link #tryLock(long, TimeUnit)}).
 *Lock 实现相对于 synchronized 提供了额外的功能，它提供了 tryLock 用于
 *非阻塞尝试获取锁，提供 lockInterruptibly 用于可中断模式获取锁，提供了
 * tryLock(long, TimeUnit) 用于有限等待时间内获取锁。
 *
 * <p>A {@code Lock} class can also provide behavior and semantics
 * that is quite different from that of the implicit monitor lock,
 * such as guaranteed ordering, non-reentrant usage, or deadlock
 * detection. If an implementation provides such specialized semantics
 * then the implementation must document those semantics.
 *Lock 类还提供与隐式监视器锁完全不同的语义和行为，比如保证顺序、不可
 *重入使用或死锁检测。
 *
 * <p>Note that {@code Lock} instances are just normal objects and can
 * themselves be used as the target in a {@code synchronized} statement.
 * Acquiring the
 * monitor lock of a {@code Lock} instance has no specified relationship
 * with invoking any of the {@link #lock} methods of that instance.
 * It is recommended that to avoid confusion you never use {@code Lock}
 * instances in this way, except within their own implementation.
 *注意，Lock 实例只是普通对象，也可以用作 synchronized 语句持有对象。
 *  获取实例的监视器锁与调用该实例的 lock 方法没有具体关系。为了避免混淆，
 * 建议不要以这种方式实现 Lock 实例。
 *
 * <p>Except where noted, passing a {@code null} value for any
 * parameter will result in a {@link NullPointerException} being
 * thrown.
 *
 * <h3>Memory Synchronization</h3>
 *
 * <p>All {@code Lock} implementations <em>must</em> enforce the same
 * memory synchronization semantics as provided by the built-in monitor
 * lock, as described in
 * <a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4">
 * The Java Language Specification (17.4 Memory Model)</a>:
 * <ul>
 * <li>A successful {@code lock} operation has the same memory
 * synchronization effects as a successful <em>Lock</em> action.
 * <li>A successful {@code unlock} operation has the same
 * memory synchronization effects as a successful <em>Unlock</em> action.
 * </ul>
 *
 * Unsuccessful locking and unlocking operations, and reentrant
 * locking/unlocking operations, do not require any memory
 * synchronization effects.
 *
 * <h3>Implementation Considerations</h3>
 *
 * <p>The three forms of lock acquisition (interruptible,
 * non-interruptible, and timed) may differ in their performance
 * characteristics, ordering guarantees, or other implementation
 * qualities.  Further, the ability to interrupt the <em>ongoing</em>
 * acquisition of a lock may not be available in a given {@code Lock}
 * class.  Consequently, an implementation is not required to define
 * exactly the same guarantees or semantics for all three forms of
 * lock acquisition, nor is it required to support interruption of an
 * ongoing lock acquisition.  An implementation is required to clearly
 * document the semantics and guarantees provided by each of the
 * locking methods. It must also obey the interruption semantics as
 * defined in this interface, to the extent that interruption of lock
 * acquisition is supported: which is either totally, or only on
 * method entry.
 * 锁获取的三种形式（可中断、不可中断和定时）
 * 在性能特征、排序保证或其他实现质量方面可能有所不同。
 * 此外，在给定的{@code lock}类中，中断正在进行的锁获取的能力可能不可用。
 * 因此，实现不需要为所有三种形式的锁获取定义完全相同的保证或语义，
 * 也不需要支持正在进行的锁获取的中断。实现需要清楚地记录每
 * 个锁定方法提供的语义和保证。它还必须遵守此接口中定义的中断语义，
 * 以支持锁获取的中断程度：要么完全中断，要么只在方法入口中断。
 *
 * <p>As interruption generally implies cancellation, and checks for
 * interruption are often infrequent, an implementation can favor responding
 * to an interrupt over normal method return. This is true even if it can be
 * shown that the interrupt occurred after another action may have unblocked
 * the thread. An implementation should document this behavior.
 * 由于中断通常意味着取消，并且对中断的检查通常很少，
 * 所以实现可以优先响应中断而不是常规方法返回。
 * 即使可以显示在另一个操作之后发生的中断可能已经解除了线程的阻塞，
 * 这也是正确的。实现应该记录这种行为。
 *
 * @see ReentrantLock
 * @see Condition
 * @see ReadWriteLock
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Lock {

    /**
     * Acquires the lock.
     *获取锁。如果锁不可用一直等待。
     * <p>If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until the
     * lock has been acquired.
     *如果锁不可用，则当前线程将出于线程调度的目的而禁用，并处于休眠状态，
     *  直到获取到锁为止
     * <p><b>Implementation Considerations</b>
     * 实现提示：
     * <p>A {@code Lock} implementation may be able to detect erroneous use
     * of the lock, such as an invocation that would cause deadlock, and
     * may throw an (unchecked) exception in such circumstances.  The
     * circumstances and the exception type must be documented by that
     * {@code Lock} implementation.
     * Lock 的实现可能会检测锁的错误使用，比如彼此的调用会导致死锁，并且
     * 可能在这种情况下抛出未检查的异常。
     */
    void lock();

    /**
     * Acquires the lock unless the current thread is
     * {@linkplain Thread#interrupt interrupted}.
     * 获取锁除非当前线程被中断。获取锁的同时响应中断。
     * <p>Acquires the lock if it is available and returns immediately.
     *如果锁可用并能立即返回的话获取锁。
     * <p>If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     *如果锁不可用，则当前线程将出于线程调度的目的而禁用，并处于休眠状态，
     *  直到发生以下两种情况之一：
     * <ul>
     * <li>The lock is acquired by the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of lock acquisition is supported.
     * </ul>
     *当前线程获取锁；或者其他线程中断当前线程，且 lock 的 acquire 支持
     * 中断。
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring the
     * lock, and interruption of lock acquisition is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *如果当前线程:在进入此方法时设置了中断状态;
     * 或者在获取锁时是否{@linkplain Thread interrupt interrupted}，
     * 且支持锁获取中断，则抛出{@link InterruptedException}，
     * 清除当前线程的中断状态
     * <p><b>Implementation Considerations</b>
     *实现注意事项
     * <p>The ability to interrupt a lock acquisition in some
     * implementations may not be possible, and if possible may be an
     * expensive operation.  The programmer should be aware that this
     * may be the case. An implementation should document when this is
     * the case.
     *在某些实现中，中断获取锁的能力可能是不可能的，如果可能的话，
     * 可能是一个昂贵的操作。程序员应该意识到可能会出现这种情况。
     * 实现应该记录这种情况。
     *
     * <p>An implementation can favor responding to an interrupt over
     * normal method return.
     *一个实现可以倾向于响应一个中断而不是正常的方法返回。
     *
     * <p>A {@code Lock} implementation may be able to detect
     * erroneous use of the lock, such as an invocation that would
     * cause deadlock, and may throw an (unchecked) exception in such
     * circumstances.  The circumstances and the exception type must
     * be documented by that {@code Lock} implementation.
     *一个{@code Lock}实现可以检测锁的错误使用，
     * 比如会导致死锁的调用，并可能在这种情况下抛出(未检查的)异常。
     * 环境和异常类型必须由{@code Lock}实现记录。
     *
     * @throws InterruptedException if the current thread is
     *         interrupted while acquiring the lock (and interruption
     *         of lock acquisition is supported)
     *   如果当前线程在获取锁时被中断(并且支持锁获取的中断)
     *   会抛出InterruptedException异常
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * Acquires the lock only if it is free at the time of invocation.
     *调用时如果锁是空闲的，则尝试 acquire。获取失败直接返回。
     * <p>Acquires the lock if it is available and returns immediately
     * with the value {@code true}.
     * If the lock is not available then this method will return
     * immediately with the value {@code false}.
     *如果锁可用，则获取锁，并立即返回 true。如果锁不可用，返回 false。
     * <p>A typical usage idiom for this method would be:
     *  <pre> {@code
     * Lock lock = ...;
     * if (lock.tryLock()) {
     *   try {
     *     // manipulate protected state
     *   } finally {
     *     lock.unlock();
     *   }
     * } else {
     *   // perform alternative actions
     * }}</pre>
     *
     * This usage ensures that the lock is unlocked if it was acquired, and
     * doesn't try to unlock if the lock was not acquired.
     *这种用法保证了如果锁被 acquire，那么确保它之后会被释放，如果没有
     *  acquire 则不会被释放。
     * @return {@code true} if the lock was acquired and
     *         {@code false} otherwise
     */
    boolean tryLock();   //嗅探锁

    /**
     * Acquires the lock if it is free within the given waiting time and the
     * current thread has not been {@linkplain Thread#interrupt interrupted}.
     * 如果给定的等待时间内线程是空闲的，并且当前线程没有被中断，则
     *   获取 锁。
     * <p>If the lock is available this method returns immediately
     * with the value {@code true}.
     * If the lock is not available then
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * <ul>
     * <li>The lock is acquired by the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of lock acquisition is supported; or
     * <li>The specified waiting time elapses
     * </ul>
     * 如果 lock 可用的话此方法返回 true。
     *  如果锁不可用，则当前线程将出于线程调度的目的而禁用，并处于休眠状态，
     *  直到发生以下三种情况之一：
     *  当前线程获取到锁；或者其它线程中断当前线程，且 lock 的 acquire 支持
     *   中断；或者时间到期。
     *
     * <p>If the lock is acquired then the value {@code true} is returned.
     * 如果获取到锁则返回 true。
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring
     * the lock, and interruption of lock acquisition is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *如果当前线程在进入此方法前已经设置其中断状态；或者在 acquire 时被
     * 中断，而且 lock 的 acquire 支持中断，那么抛出 InterruptedException
     *  异常，并清除当前线程的中断状态。
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.
     * If the time is
     * less than or equal to zero, the method will not wait at all.
     *如果指定的等待时间过期，则返回 false。
     *  如果时间小于等于 0，则该方法不会再等待。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The ability to interrupt a lock acquisition in some implementations
     * may not be possible, and if possible may
     * be an expensive operation.
     * The programmer should be aware that this may be the case. An
     * implementation should document when this is the case.
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return, or reporting a timeout.
     *
     * <p>A {@code Lock} implementation may be able to detect
     * erroneous use of the lock, such as an invocation that would cause
     * deadlock, and may throw an (unchecked) exception in such circumstances.
     * The circumstances and the exception type must be documented by that
     * {@code Lock} implementation.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return {@code true} if the lock was acquired and {@code false}
     *         if the waiting time elapsed before the lock was acquired
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while acquiring the lock (and interruption of lock
     *         acquisition is supported)
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
       //即相当于在tryLock过程生效时间内都在尝试获取，出现中断，并且当前锁可中断的话，抛出异常
    //或者时间内获取到锁，返回true，如果时间结束还是没有获取到，则返回false。
    /**
     * Releases the lock.
     *释放锁。
     * <p><b>Implementation Considerations</b>
     *
     * <p>A {@code Lock} implementation will usually impose
     * restrictions on which thread can release a lock (typically only the
     * holder of the lock can release it) and may throw
     * an (unchecked) exception if the restriction is violated.
     * Any restrictions and the exception
     * type must be documented by that {@code Lock} implementation.
     * 一个{@code Lock}实现通常会对哪些线程可以释放锁施加限制
     * (通常只有锁的持有者才能释放它)，如果违反限制，
     * 可能会抛出(未检查)异常。任何限制和异常类型都必须由{@code Lock}实现进行记录。
     */
    void unlock();

    /**
     * Returns a new {@link Condition} instance that is bound to this
     * {@code Lock} instance.
     *返回一个新的 Condition 实例，该实例绑定到此 Lock 实例。
     * <p>Before waiting on the condition the lock must be held by the
     * current thread.
     * A call to {@link Condition#await()} will atomically release the lock
     * before waiting and re-acquire the lock before the wait returns.
     * 在 condition 队列上等待之前，锁必须由当前线程持有。
     * 调用 Condition.await 将自动在 wait 之前释放锁，在 wait 返回之前再次
     *  获取锁。
     * <p><b>Implementation Considerations</b>
     *
     * <p>The exact operation of the {@link Condition} instance depends on
     * the {@code Lock} implementation and must be documented by that
     * implementation.
     *
     * @return A new {@link Condition} instance for this {@code Lock} instance
     * @throws UnsupportedOperationException if this {@code Lock}
     *         implementation does not support conditions
     */
    Condition newCondition();
}
