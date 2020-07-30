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

package JUC;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * A cancellable asynchronous computation.  This class provides a base
 * implementation of {@link Future}, with methods to start and cancel
 * a computation, query to see if the computation is complete, and
 * retrieve the result of the computation.  The result can only be
 * retrieved when the computation has completed; the {@code get}
 * methods will block if the computation has not yet completed.  Once
 * the computation has completed, the computation cannot be restarted
 * or cancelled (unless the computation is invoked using
 * {@link #runAndReset}).
 *
 * <p>A {@code FutureTask} can be used to wrap a {@link Callable} or
 * {@link Runnable} object.  Because {@code FutureTask} implements
 * {@code Runnable}, a {@code FutureTask} can be submitted to an
 * {@link Executor} for execution.
 *
 * <p>In addition to serving as a standalone class, this class provides
 * {@code protected} functionality that may be useful when creating
 * customized task classes.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this FutureTask's {@code get} methods
 */
public class FutureTask<V> implements RunnableFuture<V> {

    /**
     * 此任务的状态存放在 state 中。
     *
     * 状态变化路径由以下四种:
     * NEW -> COMPLETING -> NORMAL
     * NEW -> COMPLETING -> EXCEPTIONAL
     * NEW -> CANCELLED
     * NEW -> INTERRUPTING -> INTERRUPTED
     */
    private volatile int state;
    private static final int NEW          = 0;
    private static final int COMPLETING   = 1;
    private static final int NORMAL       = 2;
    private static final int EXCEPTIONAL  = 3;
    private static final int CANCELLED    = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED  = 6;

    // 内部持有的 callable 任务，运行完毕后置为空
    private Callable<V> callable;
    // 从 get() 中返回的结果或抛出的异常
    private Object outcome;
    // 运行 Callable 的线程
    private volatile Thread runner;
    // Treiber 栈（无所并发栈）保存等待线程（等待获取结果的线程）
    private volatile WaitNode waiters;

    /**
     * 返回结果或抛出异常。
     *
     * @param s completed state value
     */
    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        // 获取结果
        Object x = outcome;
        // 如果是正常完成，直接返回结果
        if (s == NORMAL)
            return (V)x;
        // 如果取消或异常，抛出异常
        if (s >= CANCELLED)
            throw new CancellationException();
        throw new ExecutionException((Throwable)x);
    }

    /**
     * 构造函数
     *
     * @param  callable the callable task
     * @throws NullPointerException if the callable is null
     */
    public FutureTask(Callable<V> callable) {
        if (callable == null)
            throw new NullPointerException();
        this.callable = callable;
        this.state = NEW;       // ensure visibility of callable
    }

    /**
     * 构造函数。
     *
     * @param runnable the runnable task
     * @param result the result to return on successful completion. If
     * you don't need a particular result, consider using
     * constructions of the form:
     * {@code Future<?> f = new FutureTask<Void>(runnable, null)}
     * @throws NullPointerException if the runnable is null
     */
    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;       // ensure visibility of callable
    }

    // 是否已取消（异常也算）
    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    // 是否已经完成（只要不是 NEW 都算已完成）
    public boolean isDone() {
        return state != NEW;
    }

    // 通过中断的方式取消异步任务
    public boolean cancel(boolean mayInterruptIfRunning) {
        // 尝试把状态变成 INTERRUPTING 或 CANCELLED
        if (!(state == NEW &&
                UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
                        mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
            return false;
        try {
            // 如果可以在运行时中断（取消只能通过中断完成）
            if (mayInterruptIfRunning) {
                try {
                    // 中断线程
                    Thread t = runner;
                    if (t != null)
                        t.interrupt();
                } finally { // final state
                    // 把状态改为中断
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
                }
            }
        } finally {
            // 移除并唤醒所有等待线程
            finishCompletion();
        }
        return true;
    }

    /**
     * 获取结果。
     * @throws CancellationException {@inheritDoc}
     */
    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        // 如果未完成，调用 awaitDone 等待任务完成
        if (s <= COMPLETING)
            s = awaitDone(false, 0L);
        return report(s);
    }

    /**
     * 获取结果。
     * @throws CancellationException {@inheritDoc}
     */
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null)
            throw new NullPointerException();
        int s = state;
        if (s <= COMPLETING &&
                (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING)
            throw new TimeoutException();
        return report(s);
    }

    /**
     * Protected method invoked when this task transitions to state
     * {@code isDone} (whether normally or via cancellation). The
     * default implementation does nothing.  Subclasses may override
     * this method to invoke completion callbacks or perform
     * bookkeeping. Note that you can query status inside the
     * implementation of this method to determine whether this task
     * has been cancelled.
     */
    protected void done() { }

    /**
     * 保存结果。
     *
     * @param v the value
     */
    protected void set(V v) {
        // 把状态先改成 COMPLETING
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            // 保存结果
            outcome = v;
            // 再把状态改成 NORMAL
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state
            finishCompletion();
        }
    }

    /**
     * 处理异常。
     *
     * @param t the cause of failure
     */
    protected void setException(Throwable t) {
        // 把状态从 NEW 设置成 COMPLETING
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = t;
            // 设置状态为 EXCEPTIONAL
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state
            finishCompletion();
        }
    }

    // 运行
    public void run() {
        // 状态不是 NEW，说明任务已经执行过了，或者已经取消，直接返回。
        // 状态是 NEW，尝试把当前线程保存在 runner 中，表示当前线程即将执行任务。赋值失败直接返回
        if (state != NEW ||
                !UNSAFE.compareAndSwapObject(this, runnerOffset,
                        null, Thread.currentThread()))
            return;
        try {
            // 获取当前任务
            Callable<V> c = callable;
            // 任务不为空且状态为 NEW，开始运行
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    // 调用 Callable 的 call 方法运行（类似 Runnable 的 run）
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    // 设置异常
                    setException(ex);
                }
                // 如果成功运行，把结果保存下来
                if (ran)
                    set(result);
            }
        } finally {
            // 设置 runner 为 null
            runner = null;
            // 检查状态，如果状态是 INTERRUPTING 或者 INTERRUPTED
            // 处理中断逻辑
            int s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
    }

    /**
     * 不会设置结果，不会获取 call 的结果。
     * 任务执行完之后把状态还是 NEW，任务可以多次运行。
     * runAndReset 的典型应用是在 ScheduledThreadPoolExecutor 中，周期性的执行任务。
     *
     * @return {@code true} if successfully run and reset
     */
    protected boolean runAndReset() {
        // 判断是否是初始阶段
        if (state != NEW ||
                !UNSAFE.compareAndSwapObject(this, runnerOffset,
                        null, Thread.currentThread()))
            return false;
        boolean ran = false;
        int s = state;
        try {
            // 调用 call
            Callable<V> c = callable;
            if (c != null && s == NEW) {
                try {
                    c.call(); // don't set result
                    ran = true;
                } catch (Throwable ex) {
                    setException(ex);
                }
            }
        } finally {
            runner = null;
            s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
        // 是否运行成功而且状态是否还是 NEW（没有 cancel 和 exception）
        return ran && s == NEW;
    }

    /**
     * 处理中断逻辑，确保中断停留在当前 run 或 runAndReset
     */
    private void handlePossibleCancellationInterrupt(int s) {
        //在中断者中断线程之前可能会延迟，所以我们只需要让出时间片自旋等待
        if (s == INTERRUPTING)
            while (state == INTERRUPTING)
                Thread.yield();
    }

    /**
     * 等待节点。存储的是线程，初始化的时候把当前线程保存在里面。
     */
    static final class WaitNode {
        volatile Thread thread;
        volatile WaitNode next;
        WaitNode() { thread = Thread.currentThread(); }
    }

    /**
     * 唤醒所有的等待线程
     */
    private void finishCompletion() {
        for (WaitNode q; (q = waiters) != null;) {
            // q 保存 waiters，把 waiters 设置为 null
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                // 自旋遍历 waiters
                for (;;) {
                    Thread t = q.thread;
                    // 唤醒线程
                    if (t != null) {
                        q.thread = null;
                        LockSupport.unpark(t);
                    }
                    // 继续往后
                    WaitNode next = q.next;
                    if (next == null)
                        break;
                    q.next = null; // unlink to help gc
                    q = next;
                }
                break;
            }
        }

        done();

        callable = null;        // to reduce footprint
    }

    /**
     * 等待任务完成。如果超时或中断，则取消
     *
     * @param timed true if use timed waits
     * @param nanos time to wait, if timed
     * @return state upon completion
     */
    private int awaitDone(boolean timed, long nanos)
            throws InterruptedException {
        // 截止时间
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        WaitNode q = null;
        boolean queued = false;
        for (;;) {
            // 如果中断了，清除中断状态
            if (Thread.interrupted()) {
                // 移除等待的节点
                removeWaiter(q);
                // 抛出中断异常
                throw new InterruptedException();
            }

            int s = state;
            // 如果为结束状态（正常结束/异常/取消）
            if (s > COMPLETING) {
                if (q != null)
                    q.thread = null;
                return s;
            }
            // 如果正在完成，为任务让出时间片
            else if (s == COMPLETING) // cannot time out yet
                Thread.yield();
            // 如果节点等于  null，创建一个新节点
            else if (q == null)
                q = new WaitNode();
            // 如果还没有进入队列， CAS 把节点插入到队列头部
            else if (!queued)
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset,
                        q.next = waiters, q);
            // 有超时限制
            else if (timed) {
                // 计算等待时间
                nanos = deadline - System.nanoTime();
                // 已经超时了，删除节点，返回当前状态
                if (nanos <= 0L) {
                    removeWaiter(q);
                    return state;
                }
                // 还没超时，阻塞线程
                LockSupport.parkNanos(this, nanos);
            }
            // 其他情况，阻塞线程
            else
                LockSupport.park(this);
        }
    }

    /**
     * 删除节点。
     */
    private void removeWaiter(WaitNode node) {
        if (node != null) {
            node.thread = null;
            retry:
            for (;;) {          // restart on removeWaiter race
                // 从头遍历，pred 表示前一个节点，q 表示当前节点，s 表示下一个节点
                // 删除遍历过程中遇到的无效节点
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    s = q.next;
                    if (q.thread != null)
                        // q 是有效节点，往后移动一位，继续内层循环
                        pred = q;
                    else if (pred != null) {
                        // q 是无效节点，但 q 的前一个不为 null，删除 q
                        pred.next = s;
                        if (pred.thread == null)
                            // 如果 pred 的线程为 null，说明 pred 也是无效节点，pred 被错过了
                            // 又从头开始遍历
                            continue retry;
                    }
                    // pred == null 且 q.thread == null，说明 q 是头结点且头结点无效，把头结点删了
                    else if (!UNSAFE.compareAndSwapObject(this, waitersOffset,
                            q, s))
                        continue retry;
                }
                // 遍历完了，跳出自旋
                break;
            }
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}

