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
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 计数信号量。从概念上将，信号量维持一组许可证。每一个 acquire 都会被阻塞，
 * 直到获取到许可证。每一个 release 添加一个许可证，潜在地释放一个被阻塞的
 * 获取者。
 * 但是，没有使用实际的许可证对象， Semaphore 只是保持可用数量的一个计数，
 * 并相应地进行操作。
 *
 * Semaphore 通常用于限制可以同时访问（物理或逻辑的）资源的线程数量。例如，
 * 使用信号量来控制对资源池的访问：
 * class Pool {
 *   private static final int MAX_AVAILABLE = 100;
 *   private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);
 *
 *   public Object getItem() throws InterruptedException {
 *     available.acquire();
 *     return getNextAvailableItem();
 *   }
 *
 *   public void putItem(Object x) {
 *     if (markAsUnused(x))
 *       available.release();
 *   }
 *
 *   // Not a particularly efficient data structure; just for demo
 *
 * // items 的每一个槽表示可用的资源
 *   protected Object[] items = ... whatever kinds of items being managed
 *   protected boolean[] used = new boolean[MAX_AVAILABLE];
 *
 * //获取可用的资源，并将该资源标记为不可用
 *   protected synchronized Object getNextAvailableItem() {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (!used[i]) {
 *          used[i] = true;
 *          return items[i];
 *       }
 *     }
 *     return null; // not reached
 *   }
 *
 * // 将指定资源标记为可用
 *   protected synchronized boolean markAsUnused(Object item) {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (item == items[i]) {
 *          if (used[i]) {
 *            used[i] = false;
 *            return true;
 *          } else
 *            return false;
 *       }
 *     }
 *     return false;
 *   }
 * }
 *
 * 在获取每一个资源项之前，每个线程都要从 semaphore 获取许可，确保该资源项
 * 可以被获取。当线程使用完毕之后，该资源项被返回到资源池中，许可证被返回
 * 到 semaphore 中，确保下一个线程能获取到该许可证。注意 acquire 的时候不会
 * 持有同步锁，因为这将阻止资源项被返回到资源池中。semaphore 中等装了限制对
 * 资源池访问所需的同步，与维护资源池本身的一致性需要的同步是分开的。
 *
 * 一个 semaphore 初始化为 1，即只有一个许可证的时候，可以用作互斥独占锁。
 * 这通常被称为二进制信号量，因为它只有两种状态：一个可用许可证或者零个可用
 * 许可证。当以这种方式使用时，二进制信号量有很多有用的属性（和许多 lock 的
 * 实现不同），例如可以由所有者之外的线程释放（semaphore 没有是所有权的概念）。
 * 这在某些特定的上下文中很有用，比如死锁恢复。
 *
 * 该类的构造函数可以接受公平性参数。当设置为 false 的时候，该类不保证线程
 * 获取许可证的顺序。特别地，允许倒挂，就是说一个调用 acquire 的线程可以为
 * 已经在等待的线程分配一个许可证 - 逻辑上新的线程会将自己放在等待队列的最前面。
 * 当公平性设置为 true 时，semaphore 保证调用 acquire 的线程顺序遵循 FIFO 的
 * 原则。
 * 一个线程可以在另一个线程之前调用 acquire，但是可以在该线程之后到达排序点。
 * 不定时的 tryAcquire 方法不支持公平性设定，但是会获取可用的许可证。
 *
 * 通常用于控制资源访问的 semaphore 应该被初始化为公平的，以确保没有线程
 * 因为没有获取到资源而饿死。但是显然非公平的策略吞吐量将大于公平策略。
 *
 * 此类提供了同一时间多个线程访问统一资源的 acquire 和 release 控制。当使用
 * 这些方法而没有将公平性策略设置为 true 时，要注意无限期延迟（饥饿）的风险。
 *
 * @since 1.5
 * @author Doug Lea
 */
public class Semaphore implements java.io.Serializable {
    private static final long serialVersionUID = -3222578661600680210L;
    /** All mechanics via AbstractQueuedSynchronizer subclass */
    private final Sync sync;

    /**
     * semaphore 中使用的同步控制器的实现。使用 AQS 的状态值表示许可证。
     * 子类分为公平版本和非公平版本。
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;

        Sync(int permits) {  //构造函数中调用的方法， 指定初始许可证，将许可证设置为同步器的初始state。
            setState(permits);
        }

        // 获取许可证数量。即获得同步器状态
        final int getPermits() {
            return getState();
        }

        // 非公平 tryAcquireShared实现。
        final int nonfairTryAcquireShared(int acquires) {
            // 因为同一时间会有很多个同时争取资源，所以，使用自旋+CAS
            for (;;) {
                // 获得当前可用许可证数量
                int available = getState();
                // 如果当前线程得到了许可以后，剩余多少许可证
                int remaining = available - acquires;
                // 判断这个许可证，只要是许可证大于0等于0，说明都可以获取到，那么CAS设置同步器state,然后返回剩下了多少许可证。
                // 如果小于0，说明在获取前许可证已经是0了，那就不需要修改同步器状态。直接返回0.
                if (remaining < 0 ||
                        compareAndSetState(available, remaining))
                    return remaining;
            }
        }

        // 释放资源和许可证（公平版和非公平版相同）
        protected final boolean tryReleaseShared(int releases) {
            for (;;) {
                int current = getState();  //得到当前的许可证状态，也就是同步器状态
                // 计算如果释放之后，总的可用许可证数量
                int next = current + releases;
                if (next < current) //如果移除。说明到达了最大许可证数。
                    throw new Error("Maximum permit count exceeded");
                // 改变状态
                if (compareAndSetState(current, next))
                    return true;
            }
        }

        // 减少许可证
        final void reducePermits(int reductions) {
            for (;;) {
                int current = getState();
                int next = current - reductions;
                if (next > current) // underflow
                    throw new Error("Permit count underflow");
                if (compareAndSetState(current, next))
                    return;
            }
        }

        // 许可证数变为 0
        final int drainPermits() {
            for (;;) {
                int current = getState();
                if (current == 0 || compareAndSetState(current, 0))
                    return current;
            }
        }
    }

    /**
     * 非公平版本
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;

        NonfairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }

    /**
     * 公平版本
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = 2014338818796000944L;

        FairSync(int permits) {
            super(permits);
        }

        // 公平版 tryAcquire
        protected int tryAcquireShared(int acquires) {
            for (;;) {
                // 和公平版本不同的是，需要判断是否有前继节点，如果有，返回 -1，
                // 即不允许获取
                if (hasQueuedPredecessors())
                    return -1;
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                        compareAndSetState(available, remaining))
                    return remaining;
            }
        }
    }

    /**
     * 创造一个指定数量许可证和非公平的 Semaphore。
     *
     * @param permits the initial number of permits available.
     *        This value may be negative, in which case releases
     *        must occur before any acquires will be granted.
     */
    public Semaphore(int permits) {
        sync = new NonfairSync(permits);
    }

    /**
     * 创造一个指定数量许可证和指定公平策略的 Semaphore。
     *
     * @param permits the initial number of permits available.
     *        This value may be negative, in which case releases
     *        must occur before any acquires will be granted.
     * @param fair {@code true} if this semaphore will guarantee
     *        first-in first-out granting of permits under contention,
     *        else {@code false}
     */
    public Semaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }

    /**
     * 尝试从 semaphore 中获取一个许可证，如果没有获取到则阻塞，直到获取到
     * 可用的许可证或者线程被中断为止。
     *
     * 如果获取到许可证则返回，并将可用许可证的数量减 1。
     *
     * 如果没有许可证可用，当前线程休眠，直到发生以下两种情况之一：
     * 其他线程调用 release 且当前线程获取到释放的许可证，或者其他线程中断了
     * 此线程。
     *
     * 如果当前线程：
     * 在进入此方法之前被设置了中断状态，或者在等待许可证的时候被中断，
     * 那么抛出中断异常（InterruptedException）并清除当前线程的中断状态。
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * 尝试从 semaphore 中获取一个许可证，如果没有获取到则阻塞，直到获取到
     * 可用的许可证为止。
     *
     * 如果获取到许可证则返回，并将可用许可证的数量减 1。
     *
     * 如果没有许可证可用，当前线程休眠，直到其他线程调用 release 且
     * 当前线程获取到释放的许可证
     *
     * 如果当前线程在等待时被中断，它会继续等待，当线程从这个方法返回的时候，
     * 中断状态将被设置。
     */
    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }

    /**
     * 从 semaphore 中获取许可证，当且仅当调用时有许可证可用时。
     *
     * 获取一个许可证，获取成功返回 true，并将许可证数量减 1。获取失败返回 false。
     *
     * 即使 semaphore 被设置成使用公平策略排序，调用 tryAcquire 都会获取到
     * 一个许可证，如果有许可证可用的话，而不关心同时是否有其他线程正在等待
     * 获取。
     *
     * @return {@code true} if a permit was acquired and {@code false}
     *         otherwise
     */
    public boolean tryAcquire() {
        return sync.nonfairTryAcquireShared(1) >= 0;
    }

    /**
     * 从 semaphore 获取许可证，如果在给定时间内当前线程没有被中断，且有许可证
     * 可用，则获取成功。
     *
     * 获取一个许可证成功返回 true，并将许可证数量减 1。
     *
     * 如果没有许可证可以获取，当前线程休眠，直到发生以下三种情况之一：
     * 其他线程调用 release，且当前线程获取到其释放的许可证；
     * 或者其他线程中断了当前线程；
     * 或者指定的时间片到期。
     *
     * 如果获取到许可证则返回 true。
     *
     * 如果当前线程在进入方法前已经被设置了中断状态；或者在等待获取许可证时被
     * 中断，
     * 那么抛出 InterruptedException 异常，并清除当前线程的中断状态。
     *
     * 如果指定的时间片到期，返回 false。
     *
     * @param timeout the maximum time to wait for a permit
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if a permit was acquired and {@code false}
     *         if the waiting time elapsed before a permit was acquired
     * @throws InterruptedException if the current thread is interrupted
     */
    public boolean tryAcquire(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * 释放许可证，将其退还给 semaphore。
     *
     * 释放许可证，将可用许可证数量加 1。
     *
     * 没有 acquire 许可证的线程也可以释放一个许可证。
     */
    public void release() {
        sync.releaseShared(1);
    }

    /**
     * 尝试获取指定数量的许可证，如果数量不够则阻塞当前线程。响应中断。
     *
     * @param permits the number of permits to acquire
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireSharedInterruptibly(permits);  //指定获取多少个许可证
    }

    /**
     * 尝试获取指定数量的许可证，如果数量不够则阻塞当前线程。不响应中断。
     *
     * @param permits the number of permits to acquire
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void acquireUninterruptibly(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireShared(permits);
    }

    /**
     * 尝试获取指定数量的许可证，当且仅当调用时有足够数量的许可证才会获取成功。
     *
     * 获取成功返回 true，失败返回 false。
     *
     * @param permits the number of permits to acquire
     * @return {@code true} if the permits were acquired and
     *         {@code false} otherwise
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public boolean tryAcquire(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.nonfairTryAcquireShared(permits) >= 0;
    }

    /**
     * 尝试获取指定数量的许可证，当调用时有足够数量的许可证，且没有被中断时，
     * 获取成功。否则等待指定时间。
     *
     * 获取成功返回 true，失败返回 false。
     *
     * @param permits the number of permits to acquire
     * @param timeout the maximum time to wait for the permits
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if all permits were acquired and {@code false}
     *         if the waiting time elapsed before all permits were acquired
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
    }

    /**
     * 释放给定数量的许可证，并将它们返回给 semaphore。
     *
     * 没有 acquire 的线程也可以释放许可证。
     *
     * @param permits the number of permits to release
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    //指定释放多少个许可证
    public void release(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.releaseShared(permits);
    }

    /**
     * 返回当前可用的许可证数量。
     *
     * @return the number of permits available in this semaphore
     */
    public int availablePermits() {
        return sync.getPermits();
    }

    /**
     * 获取并返回所有可用的许可证数量，并将许可证的剩余数量变为 0。
     *
     * @return the number of permits acquired
     */
    public int drainPermits() {
        return sync.drainPermits();
    }

    /**
     * 减少许可证的数量。此方法和 acquire 不同的地方是不会阻塞等待获取许可的
     * 线程。
     *
     * @param reduction the number of permits to remove
     * @throws IllegalArgumentException if {@code reduction} is negative
     */
    protected void reducePermits(int reduction) {
        if (reduction < 0) throw new IllegalArgumentException();
        sync.reducePermits(reduction);
    }

    /**
     * 如果执行了公平策略，则返回 true。
     *
     * @return {@code true} if this semaphore has fairness set true
     */
    public boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 检查是否有线程在等待 acquire。此方法用于监控。
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 返回所有等待 acquire 的线程数量（估计值）。
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 返回包含所有等待 acquire 的线程集合（估计值）。
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * 转化成字符串
     *
     * @return a string identifying this semaphore, as well as its state
     */
    public String toString() {
        return super.toString() + "[Permits = " + sync.getPermits() + "]";
    }
}
