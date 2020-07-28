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
import java.util.concurrent.TimeUnit;

/**
 * 此类是一个同步辅助类，允许一个或多个线程等待，直到在其他线程中执行的
 * 一组操作完成。
 *
 * CountDownLatch 是用给定的 count 初始化。由于 countDown 方法的调用，
 * await 方法阻塞，直到当前计数为 0，释放所有的等待线程，然后立即返回任何
 * 后续的 await 调用。这是一次性现象——计数无法重置。如果需要重置计数，
 * 考虑使用 CyclicBarrier。
 *
 * CountDownLatch 是一种通用的同步工具，可以用于多种用途。初始化一个
 * count 为 1 的 CountDownLatch 作为简单的 on/off latch 或 gate：所有调用
 * await 的线程都在 gate 外等候，直到调用 countDown 的线程打开它。初始化
 * 为 N 的 CountDownLatch 可以用来让一个线程等待，直到 N 个线程完成了
 * 一下操作，或者完成一些操作 N 次。
 *
 * CountDownLatch 一个有用的属性是，它不需要调用 countDown 的线程
 * 等待计数为 0 才继续，它只是防止任何线程通过 await，直到所有线程都可以
 * 通过。
 *
 * 实例：这是两个类，其中一组工作线程使用两个 CountDownLatch：
 * 第一个是启动信号，用于在 driver 准备好继续执行之前，防止任何 worker
 * 继续执行。
 * 第二个是完成信号，允许 driver 等到所有 worker 执行完成。
 *
 * class Driver { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch startSignal = new CountDownLatch(1);
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       new Thread(new Worker(startSignal, doneSignal)).start();
 *
 *     doSomethingElse();            // don't let run yet
 *     startSignal.countDown();      // let all threads proceed
 *     doSomethingElse();
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class Worker implements Runnable {
 *   private final CountDownLatch startSignal;
 *   private final CountDownLatch doneSignal;
 *   Worker(CountDownLatch startSignal, CountDownLatch doneSignal) {
 *     this.startSignal = startSignal;
 *     this.doneSignal = doneSignal;
 *   }
 *   public void run() {
 *     try {
 *       startSignal.await();
 *       doWork();
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * }
 *
 * 另一个典型用法是将一个问题分成 N 个部分，用一个 Runnable 来描述每个
 * 部分，每个 Runnable 执行部分任务，并在 latch 上 count down，然后将所有
 * Runnable 排队给一个 Executor。当所有的子部件完成后，协调线程将能够
 * 通过 await 通过。（当所有线程必须以这样的方式重复 count down，使用 CyclicBarrier）
 *
 * class Driver2 { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *     Executor e = ...
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       e.execute(new WorkerRunnable(doneSignal, i));
 *
 *     // 等待所有线程执行完毕
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class WorkerRunnable implements Runnable {
 *   private final CountDownLatch doneSignal;
 *   private final int i;
 *   WorkerRunnable(CountDownLatch doneSignal, int i) {
 *     this.doneSignal = doneSignal;
 *     this.i = i;
 *   }
 *   public void run() {
 *     try {
 *       doWork(i);
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * }
 *
 * @since 1.5
 * @author Doug Lea
 */
public class CountDownLatch {
    /**
     * CountDownLatch 的同步控制器。
     */
    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4982264981922014374L;

        Sync(int count) {  //使用传入的count作为state的初始值。
            setState(count);
        }

        int getCount() {
            return getState();
        }

        // 调用此函数的是await方法中的 acquireSharedInterruptibly
        // 在 acquireSharedInterruptibly 中，返回值小于 0 时进入 AQS 队列等待
        //count==state不为0时，争取不到资源，必定阻塞。
        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;  //如果当前state状态值为0，返回1，说明可获取，如果state状态不为0，则返回-1.
        }

        // 释放共享锁
        //调用countDown时，会releaseShared会调用这个方法。
        //只有stata为0时，才显示释放成功。
        protected boolean tryReleaseShared(int releases) {
            // Decrement count; signal when transition to zero
            //自旋，因为可能同时有多个线程进行释放。
            for (;;) {
                int c = getState();  //得到当前同步器状态。
                if (c == 0)      //如果同步器状态已经为0，释放失败返回false。
                    return false;
                int nextc = c-1;
                // 通过 CAS 设置同步状态-1.
                if (compareAndSetState(c, nextc))
                    return nextc == 0;   //如果此时state变为0了。说明释放成功。
            }
        }
    }

    private final Sync sync;

    /**
     * 根据给定的 count 构造一个 CountDownLatch
     *
     * @param count the number of times {@link #countDown} must be invoked
     *        before threads can pass through {@link #await}
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public CountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }

    /**
     * 使当前线程等待，直到 latch 的计数降到 0，等待过程中响应中断。
     *
     * 如果当前计数为 0，则此方法立即返回（获取成功）。
     *
     * 如果当前计数大于 0，则出于线程调度的目的，当前线程将被禁用，并休眠
     * 直到发生以下两种情况之一：
     * 由于调用 countDown 方法当前计数降到 0；或者其他线程中断此线程。
     *
     * 如果当前线程：
     * 在进入此方法前设置了中断状态；或者在等待时被中断，
     * 那么抛出 InterruptedException 异常，并清除当前线程的中断状态。
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);  //调用AQS同步器中的可中断的共享模式获取。
    }

    /**
     * 使当前线程等待，直到 latch 的计数降到 0，等待过程中响应中断，并且
     * 有等待时间限制。
     *
     * 如果当前计数为 0，则此方法立即返回 true（获取成功）。
     *
     * 如果当前计数大于 0，则出于线程调度的目的，当前线程将被禁用，并休眠
     * 直到发生以下三种情况之一：
     * 由于调用 countDown 方法当前计数降到 0；或者其他线程中断此线程；
     * 或者等待时间到期。
     *
     * 如果计数降到 0，此方法返回 true。
     *
     * 如果当前线程：
     * 在进入此方法前设置了中断状态；或者在等待时被中断，
     * 那么抛出 InterruptedException 异常，并清除当前线程的中断状态。
     *
     * 如果指定等待时间到期，返回 false。如果时间小于等于 0，此方法不会再
     * 等待。
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if the count reached zero and {@code false}
     *         if the waiting time elapsed before the count reached zero
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public boolean await(long timeout, TimeUnit unit)  //中断。countDown以后记数为0，或到时间了。就直接退出等待。
            throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout)); //第一个参数是请求的资源数，第二个是等待时间纳秒
    }

    /**
     * latch 的计数递减，如果计数达到 0，则释放所有的等待线程。
     *
     * 如果当前计数大于 0，则递减。如果新的计数为 0，那么所有等待的线程都
     * 将重新启用，以便进行线程调度。
     *
     * 如果当前计数等于 0，则什么也不会发生。
     */
    public void countDown() {
        sync.releaseShared(1);
    }

    /**
     * 返回当前计数。
     *
     * <p>This method is typically used for debugging and testing purposes.
     *
     * @return the current count
     */
    public long getCount() {   //得到当前计数器
        return sync.getCount();
    }

    /**
     * @return a string identifying this latch, as well as its state
     */
    public String toString() {
        return super.toString() + "[Count = " + sync.getCount() + "]";
    }
}

