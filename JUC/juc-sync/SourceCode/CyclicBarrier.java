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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 允许一组线程到达一个共同的屏障点时等待的同步辅助工具。CyclicBarrier
 * 在包含固定数量线程的程序中非常有用，这些线程必须彼此等待。这个屏障
 * 被称为 cyclic，因为它可以再等待线程被释放后被重新使用。
 *
 * CyclicBarrier 支持一个可选的 Runnable 命令，该命令在每一个屏障点上运行
 * 一次，在最后一个线程到达之后，在任何线程被 release 之后。
 * 这个 barrier 行为是有用的，可用于在任何一方继续之前更新共享状态。
 *
 * 示例：下面是在一个并行分解设计中使用屏障的示例
 * class Solver {
 *   final int N;
 *   final float[][] data;
 *   final CyclicBarrier barrier;
 *
 *   class Worker implements Runnable {
 *     int myRow;
 *     Worker(int row) { myRow = row; }
 *     public void run() {
 *       while (!done()) {
 *         processRow(myRow);
 *
 *         try {
 *           barrier.await();
 *         } catch (InterruptedException ex) {
 *           return;
 *         } catch (BrokenBarrierException ex) {
 *           return;
 *         }
 *       }
 *     }
 *   }
 *
 *   public Solver(float[][] matrix) {
 *     data = matrix;
 *     N = matrix.length;
 *     Runnable barrierAction =
 *       new Runnable() { public void run() { mergeRows(...); }};
 *     barrier = new CyclicBarrier(N, barrierAction);
 *
 *     List<Thread> threads = new ArrayList<Thread>(N);
 *     for (int i = 0; i < N; i++) {
 *       Thread thread = new Thread(new Worker(i));
 *       threads.add(thread);
 *       thread.start();
 *     }
 *
 *     // wait until done
 *     for (Thread thread : threads)
 *       thread.join();
 *   }
 * }
 *
 * 上述程序中，每个工作线程处理矩阵的一行，然后在屏障处等待，直到处理完
 * 所有的行。当所有的行处理完之后，执行提供的 Runnable 屏障行为并合并行。
 * 如果合并确定找到了解决方案，那么 done 将返回 true，每个 worker 将中止。
 *
 * 如果 barrier 行为执行时不依赖被挂起的参与方，那么参与方中的任何线程都
 * 可以在释放该操作时执行该操作。为了方便，每次调用 await 都会返回到达屏障
 * 处的线程索引。可以选择那个线程应该执行 barrier 行为。例如：
 * if (barrier.await() == 0) {
 *   // log the completion of this iteration
 * }}
 *
 * CyclicBarrier 在同步失败时使用 all-or-none 模式：如果一个线程因为中断，
 * 失败，或者超时过早地离开屏障点，所有在屏障点等待的其他线程也将通过
 * BrokenBarrierException 异常（或通过 InterruptedException 异常，如果
 * 他们在同一时间也被中断的话）离开。
 *
 * @since 1.5
 * @see CountDownLatch
 *
 * @author Doug Lea
 */
public class CyclicBarrier {
    /**
     * 屏障的每一次使用都表现为一个 generation 实例。无论何时触发屏障或者
     * 重置屏障，generation 都会改变。
     * Each use of the barrier is represented as a generation instance.
     * The generation changes whenever the barrier is tripped, or
     * is reset. There can be many generations associated with threads
     * using the barrier - due to the non-deterministic way the lock
     * may be allocated to waiting threads - but only one of these
     * can be active at a time (the one to which {@code count} applies)
     * and all the rest are either broken or tripped.
     * There need not be an active generation if there has been a break
     * but no subsequent reset.
     */
    private static class Generation {
        boolean broken = false;
    }

    /** 守护 barrier 入口的锁 */
    private final ReentrantLock lock = new ReentrantLock();
    /** 在此 Condition 上等待直到所有线程到达 barrier */
    private final Condition trip = lock.newCondition();
    /** 要屏障的线程数 */
    private final int parties;
    /** 当线程都到达 barrier，运行的 barrierCommand */
    private final Runnable barrierCommand;
    /** 当前的 generation */
    private Generation generation = new Generation();

    /**
     * 仍然需要等待的线程数量。每一个 generation 从 parties 降到 0。创建新的
     * generation 或者 broken 的时候重置为需要拦截的线程数。
     * parties 表示需要拦截的线程数。
     */
    private int count;

    /**
     * 更新 barrier 上 trip 队列的状态，唤醒所有线程。
     * 只有在持有锁时才调用。
     */
    private void nextGeneration() {
        // 唤醒所有线程
        trip.signalAll();
        // 重置 count 为需要拦截的线程数
        count = parties;
        generation = new Generation();
    }

    /**
     * 打破屏障，唤醒所有线程。
     * 只有在持有锁时才能调用。
     */
    private void breakBarrier() {
        generation.broken = true;  //标志说明该CyclicBarrier的当前世代已经被破坏，
        count = parties;
        // 唤醒所有线程
        trip.signalAll();
    }

    /**
     * barrier 的主要代码，包括多种策略。
     */
    private int dowait(boolean timed, long nanos)
            throws InterruptedException, BrokenBarrierException,
            TimeoutException {
        final ReentrantLock lock = this.lock; //获得锁
        lock.lock();  //上锁
        try {
            //首先第一部分得到当前CyclicBarrier的当前轮次的运行状态。
            //如果其中类的打破标志为true，说明被打破了，要抛出异常。
            //再判断当前线程的中断标志是否为true，如果已经中断，调用方法将此循环屏障打破，唤醒所有线程，然后返回中断异常。
            // 获取当前 generation
            final Generation g = generation;

            // 判断当前 generation 是否是打破状态，如果是抛出异常
            if (g.broken)
                throw new BrokenBarrierException();

            // 如果线程已经有了中断状态，则打破屏障，唤醒所有线程，并抛出中断异常
            if (Thread.interrupted()) {
                breakBarrier();
                throw new InterruptedException();
            }

            //检查完时代信息后，要维护屏障状态。
            int index = --count;    //count自减，count表示的是还有多少线程到达才会打破屏障。
            //为0，说明打破了。
            if (index == 0) {  // tripped
                boolean ranAction = false;
                try {
                    // 执行屏障打破后应该做的事情，
                    final Runnable command = barrierCommand;
                    if (command != null)//如果command不为null，即事先指定了事件的构造函数创建的屏障。
                        command.run();
                    ranAction = true;   //执行了就设置为true。
                    nextGeneration();    //开启下一世代。重置count和唤醒所有等待的线程。
                    return 0;   //返回0.
                } finally {
                    if (!ranAction)    //如果事情过程中抛出了异常，那么直接打破屏障。
                        breakBarrier();  //唤醒所有线程并重置count为parties，设置broken标志。
                }
            }
            //
            // 自旋，直到满足 tripped, broken, interrupted, timed out 之一。
            //下面是到达的线程，不是最后一个到达的，需要加入到condition等待队列的执行代码。
            for (;;) {
                try {
                    if (!timed) //如果没有限时，单纯await（）。
                        // 线程释放当前lock,并进入 Condition 队列中等待,唤醒后会转移到同步队列中，尝试获取锁，
                        trip.await();
                    else if (nanos > 0L)   //还在限时内，
                        nanos = trip.awaitNanos(nanos);   //调用awaitNanos方法，等待nanos秒。过程中可以被中断，signal。
                } catch (InterruptedException ie) {       //如果是在signal之前被中断，则会捕获异常，执行以下代码
                    if (g == generation && ! g.broken) {     //如果还是当前世代，且屏障没有被打破，那么因为中断所以要打破屏障。
                        breakBarrier();  //打破屏障，重置count为parties，唤醒所有线程，设置标志位。
                        throw ie;     //抛出异常。
                    } else {    //如果有其他线程执行了breakBarrier,或者更换了世代了，那么需要补一次自我中断。
                        // We're about to finish waiting even if we had not
                        // been interrupted, so this interrupt is deemed to
                        // "belong" to subsequent execution.
                        Thread.currentThread().interrupt();
                    }
                }

                if (g.broken) //如果其他线程设置了标志位，那么这一步抛出屏障打破的异常。
                    throw new BrokenBarrierException();

                if (g != generation)    //如果此时已经是新世代了，那么返回上一世代的index。
                    return index;

                if (timed && nanos <= 0L) {
                    // 超时，打破当前屏障
                    breakBarrier();
                    throw new TimeoutException();  //抛出超时异常。
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 构造函数，指定 parties（线程数），count（计数），barrierCommand（屏障行为）
     *
     * @param parties the number of threads that must invoke {@link #await}
     *        before the barrier is tripped
     * @param barrierAction the command to execute when the barrier is
     *        tripped, or {@code null} if there is no action
     * @throws IllegalArgumentException if {@code parties} is less than 1
     */
    public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) throw new IllegalArgumentException();
        this.parties = parties;
        this.count = parties;   //count初始=parties。
        this.barrierCommand = barrierAction;
    }

    /**
     * 构造函数，执行 parties
     *
     * @param parties the number of threads that must invoke {@link #await}
     *        before the barrier is tripped
     * @throws IllegalArgumentException if {@code parties} is less than 1
     */
    public CyclicBarrier(int parties) {
        this(parties, null);
    }

    /**
     * 获取想要越过屏障的线程数 parties。
     *
     * @return the number of parties required to trip this barrier
     */
    public int getParties() {    //得到屏障打破需要达到的线程数。
        return parties;
    }

    /**
     * 在屏障前等待，直到所有的线程到达屏障。
     *
     * 如果当前线程是不会最后到达的，那么出于线程调度的目的，它将会休眠，
     * 直到发生以下情况之一：
     * 最后一个线程到达；
     * 其他线程中断了当前线程；
     * 其他线程中断了某一个线程；
     * 等待屏障过程中其他线程等待时间到了；
     * 其他线程调用了 reset。
     *
     * 如果当前线程：
     * 在进入此函数之前有中断状态；或者等待时被中断
     * 那么抛出 InterruptedException 异常并清除当前线程的中断状态。
     *
     * 如果在任何线程等待的时候屏障被 reset 了，或者如果 await 被调用时
     * 屏障被打破了，或者线程等待时发生了上面的事情，则抛出
     * BrokenBarrierException 异常。
     *
     * 如果等待时任何线程被中断了，所有等待的线程都会抛出
     * BrokenBarrierException 异常，同时屏障被置为打破状态。
     *
     * 如果当前线程是最后一个到达的线程，且构造此对象时制定了屏障行为，
     * 那么在允许其他线程继续执行之前，将会执行此屏障操作。
     * 如果执行此操作过程中发生了异常，则异常将会被传递到当前线程中，屏障
     * 被置为打破状态。
     *
     * @return the arrival index of the current thread, where index
     *         {@code getParties() - 1} indicates the first
     *         to arrive and zero indicates the last to arrive
     * @throws InterruptedException if the current thread was interrupted
     *         while waiting
     * @throws BrokenBarrierException if <em>another</em> thread was
     *         interrupted or timed out while the current thread was
     *         waiting, or the barrier was reset, or the barrier was
     *         broken when {@code await} was called, or the barrier
     *         action (if present) failed due to an exception
     */
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe); // cannot happen
        }
    }

    /**
     * 在屏障前等待一段时间，直到所有的线程到达屏障，或等待时间到期。
     * 到期后会打破屏障
     *
     * 如果当前线程是不会最后到达的，那么出于线程调度的目的，它将会休眠，
     * 直到发生以下情况之一：
     * 最后一个线程到达；
     * 等待时间到期；
     * 其他线程中断了当前线程；
     * 其他线程中断了某一个线程；
     * 等待屏障过程中其他线程等待时间到了；
     * 其他线程调用了 reset。
     *
     * 如果当前线程：
     * 在进入此函数之前有中断状态；或者等待时被中断
     * 那么抛出 InterruptedException 异常并清除当前线程的中断状态。
     *
     * 如果指定等待时间到期，将会抛出 TimeoutException 异常，并不再等待。
     *
     * 如果在任何线程等待的时候屏障被 reset 了，或者如果 await 被调用时
     * 屏障被打破了，或者线程等待时发生了上面的事情，则抛出
     * BrokenBarrierException 异常。
     *
     * 如果等待时任何线程被中断了，所有等待的线程都会抛出
     * BrokenBarrierException 异常，同时屏障被置为打破状态。
     *
     * 如果当前线程是最后一个到达的线程，且构造此对象时制定了屏障行为，
     * 那么在允许其他线程继续执行之前，将会执行此屏障操作。
     * 如果执行此操作过程中发生了异常，则异常将会被传递到当前线程中，屏障
     * 被置为打破状态。
     *
     * @param timeout the time to wait for the barrier
     * @param unit the time unit of the timeout parameter
     * @return the arrival index of the current thread, where index
     *         {@code getParties() - 1} indicates the first
     *         to arrive and zero indicates the last to arrive
     * @throws InterruptedException if the current thread was interrupted
     *         while waiting
     * @throws TimeoutException if the specified timeout elapses.
     *         In this case the barrier will be broken.
     * @throws BrokenBarrierException if <em>another</em> thread was
     *         interrupted or timed out while the current thread was
     *         waiting, or the barrier was reset, or the barrier was broken
     *         when {@code await} was called, or the barrier action (if
     *         present) failed due to an exception
     */
    public int await(long timeout, TimeUnit unit)
            throws InterruptedException,
            BrokenBarrierException,
            TimeoutException {
        return dowait(true, unit.toNanos(timeout));
    }

    /**
     * 查询屏障是否是打破状态（当前 generation）
     *
     * @return {@code true} if one or more parties broke out of this
     *         barrier due to interruption or timeout since
     *         construction or the last reset, or a barrier action
     *         failed due to an exception; {@code false} otherwise.
     */
    public boolean isBroken() {     //判断当前世代的屏障是否被打破。
        final ReentrantLock lock = this.lock;
        // 执行过程中锁定
        lock.lock();
        try {
            return generation.broken;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将屏障重置到初始状态。如果有线程在屏障点等待，会抛出
     * BrokenBarrierException 异常。
     */
    public void reset() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 打破旧时代，会唤醒所有线程，并且标志位设为true，所有被唤醒的线程，都会抛出这个异常。
            breakBarrier();
            // 开启新时代
            nextGeneration();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 返回此刻在屏障处等待的线程数量。
     *
     * @return the number of parties currently blocked in {@link #await}
     */
    public int getNumberWaiting() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return parties - count;    //和count是相对的概念，count为，还需要多少线程就能打破屏障。
        } finally {
            lock.unlock();
        }
    }
}
