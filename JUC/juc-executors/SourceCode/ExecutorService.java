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
import java.util.List;
import java.util.Collection;
import java.util.concurrent.*;

/**
 * 提供管理线程终止的各种方法的 Executor，同时提供可以生成用于跟踪一个
 * 或多个异步任务的进度的 Future 的方法。
 *
 * ExecutorService 可以关闭，这将导致它拒绝新任务。有两种方法用来关闭
 * ExecutorService，shutdown 方法允许在终止之前执行以前提交的任务，
 * shutdownNow 方法可以防止 waiting 的任务启动并尝试停止当前正在执行的
 * 任务。在终止时，executor 没有正在执行的任务，没有任务等待执行，也不能
 * 提交新的任务。应该关闭未使用的 ExecutorService 以允许其回收资源。
 *
 * 方法 submit 通过创建和返回一个 Future 来扩展基本方法 execute，该 Future
 * 可以用来取消执行和/或等待完成。
 * 方法 invokeAny 和 invokeAll 执行最常用的批量形式，执行一组任务，然后
 * 等待一个或全部完成。
 *
 * Executors 类为这个包中提供的 executor 服务提供工厂方法。
 *
 * 示例
 *
 * 下面是一个网络服务的示意图，其中线程池服务中的线程传入请求。它使用了
 * 预先配置的 Executors.newFixedThreadPool 工厂方法：
 * class NetworkService implements Runnable {
 *   private final ServerSocket serverSocket;
 *   private final ExecutorService pool;
 *
 *   public NetworkService(int port, int poolSize)
 *       throws IOException {
 *     serverSocket = new ServerSocket(port);
 *     pool = Executors.newFixedThreadPool(poolSize);
 *   }
 *
 *   public void run() { // run the service
 *     try {
 *       for (;;) {
 *         pool.execute(new Handler(serverSocket.accept()));
 *       }
 *     } catch (IOException ex) {
 *       pool.shutdown();
 *     }
 *   }
 * }
 *
 * class Handler implements Runnable {
 *   private final Socket socket;
 *   Handler(Socket socket) { this.socket = socket; }
 *   public void run() {
 *     // read and service request on socket
 *   }
 * }
 *
 * 下满的方法分两个阶段关闭 ExecutorService，首先调用 shutdown 来拒绝
 * 传入的任务，然后调用 shutdownNow（如果需要的话），来取消任何延迟任务：
 * void shutdownAndAwaitTermination(ExecutorService pool) {
 *   pool.shutdown(); // Disable new tasks from being submitted
 *   try {
 *     // Wait a while for existing tasks to terminate
 *     if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
 *       pool.shutdownNow(); // Cancel currently executing tasks
 *       // Wait a while for tasks to respond to being cancelled
 *       if (!pool.awaitTermination(60, TimeUnit.SECONDS))
 *           System.err.println("Pool did not terminate");
 *     }
 *   } catch (InterruptedException ie) {
 *     // (Re-)Cancel if current thread also interrupted
 *     pool.shutdownNow();
 *     // Preserve interrupt status
 *     Thread.currentThread().interrupt();
 *   }
 * }}
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface ExecutorService extends Executor {

    /**
     * 启动有序关闭，同时执行已经提交了的任务，不再接受新的任务。如果
     * 已经关闭，调用将没有其他效果。
     *
     * 此方法不等待以前提交的任务完成执行。使用 awaitTermination 来完成。
     *
     * @throws SecurityException if a security manager exists and
     *         shutting down this ExecutorService may manipulate
     *         threads that the caller is not permitted to modify
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")},
     *         or the security manager's {@code checkAccess} method
     *         denies access.
     */
    void shutdown();

    /**
     * 尝试停止所有正在执行的任务，停止正在等待的任务，并返回正在等待执行
     * 的任务列表。
     *
     * 此方法不会等待正在执行的任务终止。使用 awaitTermination 实现此目标。
     *
     * 除了仅最大努力停止处理正在执行的任务外，不保证任何其他事情。例如，
     * 典型的实现会通过 interrupt 取消，因此任何未能相应中断的任务都可能
     * 永远不会终止。
     *
     * @return list of tasks that never commenced execution
     * @throws SecurityException if a security manager exists and
     *         shutting down this ExecutorService may manipulate
     *         threads that the caller is not permitted to modify
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")},
     *         or the security manager's {@code checkAccess} method
     *         denies access.
     */
    List<Runnable> shutdownNow();

    /**
     * 如果 executor 被中断则返回 true。
     *
     * @return {@code true} if this executor has been shut down
     */
    boolean isShutdown();

    /**
     * 如果所有的线程在 shut down 之后都结束则返回 true。
     * 注意 isTerminated 不会是 true，除非 shutdown 或者 shutdownNow 先被
     * 调用。
     *
     * @return {@code true} if all tasks have completed following shut down
     */
    boolean isTerminated();

    /**
     * 在 shutdown 之后阻塞，直到所有任务执行完毕，或者时间到期，或者
     * 当前线程被中断。
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return {@code true} if this executor terminated and
     *         {@code false} if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * 提交一个需要返回值的任务以供执行，返回一个表示给任务未决结果的
     * Future。Future 的 get 方法将在任务成功完成后返回任务的结果。
     *
     * @param task the task to submit
     * @param <T> the type of the task's result
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    <T> Future<T> submit(Callable<T> task);

    /**
     * 提交一个需要返回值的任务以供执行，返回一个表示给任务未决结果的
     * Future。Future 的 get 方法将在任务成功完成后返回任务的结果。
     *
     * @param task the task to submit
     * @param result the result to return
     * @param <T> the type of the result
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    <T> Future<T> submit(Runnable task, T result);

    /**
     * 提交一个需要返回值的任务以供执行，返回一个表示给任务未决结果的
     * Future。Future 的 get 方法将在任务成功完成后返回 null。
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    Future<?> submit(Runnable task);

    /**
     * 执行给定的任务，返回一个 Future 的列表，其中包含所有任务完成时的
     * 状态和结果。
     *
     * @param tasks the collection of tasks
     * @param <T> the type of the values returned from the tasks
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list, each of which has completed
     * @throws InterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks or any of its elements are {@code null}
     * @throws RejectedExecutionException if any task cannot be
     *         scheduled for execution
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException;

    /**
     * 执行给定的任务，返回一个 Future 的列表，其中包含所有任务完成时的
     * 状态和结果。
     *
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param <T> the type of the values returned from the tasks
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list. If the operation did not time out,
     *         each task will have completed. If it did time out, some
     *         of these tasks will not have completed.
     * @throws InterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks, any of its elements, or
     *         unit are {@code null}
     * @throws RejectedExecutionException if any task cannot be scheduled
     *         for execution
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                  long timeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * 执行给定的任务，返回某个成功执行的任务的结果，如果有的话。
     * 执行过程中如果给定的集合被改变，则操作的结果未定义。
     *
     * @param tasks the collection of tasks
     * @param <T> the type of the values returned from the tasks
     * @return the result returned by one of the tasks
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if tasks or any element task
     *         subject to execution is {@code null}
     * @throws IllegalArgumentException if tasks is empty
     * @throws ExecutionException if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *         for execution
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException;

    /**
     * 执行给定的任务，返回某个在时间到期前成功执行的任务的结果，如果
     * 有的话。执行过程中如果给定的集合被改变，则操作的结果未定义。
     *
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param <T> the type of the values returned from the tasks
     * @return the result returned by one of the tasks
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if tasks, or unit, or any element
     *         task subject to execution is {@code null}
     * @throws TimeoutException if the given timeout elapses before
     *         any task successfully completes
     * @throws ExecutionException if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *         for execution
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                    long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException;
}

