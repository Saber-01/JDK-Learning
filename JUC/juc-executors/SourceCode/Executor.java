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

/**
 * 一个执行已提交 Runnable 任务的对象。此接口提供了一种将任务提交与如何
 * 运行的机制（包括使用、调度细节）分离的方法。通常使用 Executor 而不是
 * 显式地创建线程。例如，预期 对每一个任务调用
 * new Thread(new(RunnableTask())).start()，不如使用：
 * Executor executor = <em>anExecutor</em>;
 * executor.execute(new RunnableTask1());
 * executor.execute(new RunnableTask2());
 * ...
 *
 * 但是，Executor 接口并不严格需要异步执行。在最简单的情况下，执行者
 * 可以在调用者线程中立即运行提交的任务：
 * class DirectExecutor implements Executor {
 *   public void execute(Runnable r) {
 *     r.run();
 *   }
 * }}
 *
 * 更典型地，任务运行在其他线程而不是调用者线程里。下面的 executor 为每个
 * 任务生成一个新线程。
 * class ThreadPerTaskExecutor implements Executor {
 *   public void execute(Runnable r) {
 *     new Thread(r).start();
 *   }
 * }}
 *
 * 许多 Executor 的实现对如何以及何时调度任务施加了某种限制。下面的
 * executor 将任务的提交序列化到第二个执行程序，演示了复合的 executor。
 * class SerialExecutor implements Executor {
 *   final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
 *   final Executor executor;
 *   Runnable active;
 *
 *   SerialExecutor(Executor executor) {
 *     this.executor = executor;
 *   }
 *
 *   public synchronized void execute(final Runnable r) {
 *     tasks.offer(new Runnable() {
 *       public void run() {
 *         try {
 *           r.run();
 *         } finally {
 *           scheduleNext();
 *         }
 *       }
 *     });
 *     if (active == null) {
 *       scheduleNext();
 *     }
 *   }
 *
 *   protected synchronized void scheduleNext() {
 *     if ((active = tasks.poll()) != null) {
 *       executor.execute(active);
 *     }
 *   }
 * }}
 *
 * 此包中提供了基于 Executor 的一个更广泛的接口 ExecutorService。
 * ThreadPoolExecutor 类提供了一个可扩展的线程池实现。类为这些 Executor
 * 提供了方便的工厂方法。
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Executor {

    /**
     * 在未来的某个时候执行给定的指令（Runnable）。这个指令可能会在
     * 新线程、线程池、调用的线程里执行，取决于 Executor 的实现。
     *
     * @param command the runnable task
     * @throws RejectedExecutionException if this task cannot be
     * accepted for execution
     * @throws NullPointerException if command is null
     */
    void execute(Runnable command);
}

