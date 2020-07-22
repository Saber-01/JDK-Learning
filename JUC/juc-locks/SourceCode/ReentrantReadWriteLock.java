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
import java.util.Collection;
import java.util.concurrent.locks.Condition;

/**
 * 此类是ReadWriteLock 接口的一个实现，支持 ReentrantLock 相同的语义。
 * 此类有如下特性：
 * <b>Acquisition order</b>
 *此类不强制对锁访问进行读写线程优先级排序，但是它支持一个可选的公平性策略。
 * <b>非公平模式（默认）</b>
 * 若构造时指定为非公平模式（默认），读写锁的进入顺序是未指定的，这
 * 取决于可重入性约束。一个被持续竞争的非公平锁可能无限期地延迟一个或
 * 多个读写线程，但通常比公平锁有更高的吞吐量。
 * <b>公平模式</b>
 * 若构造时指定为公平模式，线程使用类似于到达顺序的策略竞争进入。当当前
 * 持有的锁被释放时，要么等待时间最长的单个写线程被分配写锁，要么为一组
 * 等待时间比所有写线程等待时间都长的读线程分配读锁。
 * 如果写锁被持有，或者有正在等待锁的线程，那么试图公平获取读锁（非重入）
 * 的线程将阻塞。在当前最老的等待写锁线程获得并释放写锁之前，线程不会获取
 * 读锁。当然，如果一个正在等待的写线程放弃了它的等待，留下一个或多个读
 * 线程作为队列中最长的等待线程，而写锁是空闲的，那么这些读线程将被分配读锁。
 * 除非读锁和写锁都是空闲的（这意味着没有等待的线程），否则试图公平获取
 * 一个写锁的线程将阻塞。（注意，非阻塞的 ReadLock.tryLock 和 WriteLock.tryLock
 * 方法不支持这种公平设定，如果可能，他们将立即获取锁，不考虑正在等待的线程。）
 * <b>可重入</b>
 * 此锁允许读线程和写线程再次获取读锁或写锁，以 ReentrantLock 一样的方式。
 * 不可重入的读线程不允许重入，直到被写线程持有的写锁全部释放。
 * 除此之外，写线程可获取读锁，但是反过来不行。在其他应用中，可重入性在
 * 对读锁下执行读的方法的调用或毁掉期间保持写锁时非常有用。如果一个读线程
 * 试图获取写锁，它将永远不会成功。
 * <b>锁降级</b>
 * 重入还允许通过获取写锁，然后获取读锁，然后释放写锁，从写锁降级为读锁。
 * 但是无法从读锁升级到写锁。
 * <b>锁获取过程中的中断</b>
 * 读锁和写锁都支持锁获取过程中的中断。
 * <b>支持Condition</b>
 * 写锁提供了一个 Condition 实现，和 ReentrantLock.newCondition 的行为
 * 方式相同。当然，这个 Condition 只能和写锁一起使用。
 * 读锁不支持 Condition，调用 readLock().newCondition() 会抛出
 * UnsupportedOperationException 异常。
 * <b>监控</b>
 * 此类支持确定锁是否被持有和被竞争的方法。这些方法用来监控系统状态，
 * 不用于同步控制。
 *
 * 示例用法。下面的代码演示了如何在更新缓存后执行锁降级（异常处理在以
 * 非嵌套的方式处理多个锁时特别棘手）：
 * class CachedData {
 *   Object data;
 *   volatile boolean cacheValid;
 *   final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *
 *   void processCachedData() {
 *     rwl.readLock().lock();
 *     if (!cacheValid) {
 *       // Must release read lock before acquiring write lock
 *       // 在获取写锁之前必须释放读锁
 *       rwl.readLock().unlock();
 *       rwl.writeLock().lock();
 *       try {
 *         // Recheck state because another thread might have
 *         // acquired write lock and changed state before we did.
 *         // 重新检查状态，因为另一个线程可能已经获取写锁并在之前更新了状态
 *         if (!cacheValid) {
 *           data = ...
 *           cacheValid = true;
 *         }
 *         // Downgrade by acquiring read lock before releasing write lock
 *         // 在释放写锁之前通过获取读锁进行降级
 *         rwl.readLock().lock();
 *       } finally {
 *         rwl.writeLock().unlock(); // Unlock write, still hold read
 *         // 释放写锁，但此时依然持有读锁
 *       }
 *     }
 *
 *     try {
 *       use(data);
 *     } finally {
 *       rwl.readLock().unlock();
 *     }
 *   }
 * }}
 *
 * ReentrantReadWriteLocks 可用于某些集合中提高并发性。通常，在集合
 * 预期会很大，读线程比写线程更多地访问集合，且操作的开销超过同步开销时，
 * 才值得这样做。例如，有一个使用 TreeMap 的类，预期容量很大，且需要
 * 并发访问。
 * class RWDictionary {
 *   private final Map<String, Data> m = new TreeMap<String, Data>();
 *   private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *   private final Lock r = rwl.readLock();
 *   private final Lock w = rwl.writeLock();
 *
 *   // 分别对所有的读操作添加读锁，对所有的写操作添加写锁。
 *   public Data get(String key) {
 *     r.lock();
 *     try { return m.get(key); }
 *     finally { r.unlock(); }
 *   }
 *   public String[] allKeys() {
 *     r.lock();
 *     try { return m.keySet().toArray(); }
 *     finally { r.unlock(); }
 *   }
 *   public Data put(String key, Data value) {
 *     w.lock();
 *     try { return m.put(key, value); }
 *     finally { w.unlock(); }
 *   }
 *   public void clear() {
 *     w.lock();
 *     try { m.clear(); }
 *     finally { w.unlock(); }
 *   }
 * }}
 *
 * 实现时需要注意：此锁支持最多 65535 个递归写锁和 65535 个读锁。试图
 * 超过这个限制将导致从 lock 方法中抛出 Error。
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ReentrantReadWriteLock
        implements ReadWriteLock, java.io.Serializable {
    private static final long serialVersionUID = -6992448646407690164L;
    /** Inner class providing readlock */
    // 提供读锁的内部类
    private final ReentrantReadWriteLock.ReadLock readerLock;
    /** Inner class providing writelock */
    // 提供写锁的内部类
    private final ReentrantReadWriteLock.WriteLock writerLock;
    /** Performs all synchronization mechanics */
    // 实现所有的同步机制
    final Sync sync;

    /**
     * ReentrantReadWriteLock 构造函数，默认使用非公平排序属性。
     */
    public ReentrantReadWriteLock() {
        this(false);
    }

    /**
     * 使用给定的公平性策略创造一个新的 ReentrantReadWriteLock。
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

    //提供给ReentrantReadWriteLock实例的获取写锁和读锁的方法
    public WriteLock writeLock() { return writerLock; }
    public ReadLock  readLock()  { return readerLock; }

    /**
     * ReentrantReadWriteLock 的同步器的实现。
     * 子类分为公平版本和非公平版本。
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 6317671515068378041L;

        /*
         * Read vs write count extraction constants and functions.
         * Lock state is logically divided into two unsigned shorts:
         * The lower one representing the exclusive (writer) lock hold count,
         * and the upper the shared (reader) hold count.
         */
        // 最多支持 65535(1<<16 - 1) 个写锁和 65535 个读锁
        // int 值的低十六位表示写锁计数，高十六位表示持有读锁的线程数
        static final int SHARED_SHIFT   = 16;
        // 增加一个线程获取读锁，则持有数加 SHARED_UNIT，因为只有高十六位
        // 才表示读锁数量
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
        // 锁的最大数量
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
        // 写锁计数掩码（低十六位的二进制全部为 1）
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

        /** Returns the number of shared holds represented in count  */
        // 返回当前持有读锁的线程数（高十六位表示读锁计数，所以无符号右移 16 位）
        static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
        /** Returns the number of exclusive holds represented in count  */
        // 返回写锁的重入次数（获取低十六位）
        static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }

        /**
         * 持有读锁的线程计数器。记录单个线程持有的读锁数量。
         */
        //线程持有几个读锁的计时器类，
            //2个成员函数，int类型的读锁个数，long类型的线程ID。
        static final class HoldCounter {
            int count = 0;
            // Use id, not reference, to avoid garbage retention
            // 当前线程的 id
            final long tid = getThreadId(Thread.currentThread());
        }

        /**
         * ThreadLocal 的子类。
         */
        //继承ThreadLocal的子类，
        static final class ThreadLocalHoldCounter
                extends ThreadLocal<HoldCounter> {
            //重写了initialValue方法，则初始化thread中的Map中的key时，不为null，而是HoldCounter的默认实例
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }

        /**
         * 当前线程持有的可重入读锁的数量。
         * 仅在构造函数和 readObject 中初始化。
         * 当读线程的持有计数下降到 0 时删除。
         */
        private transient ThreadLocalHoldCounter readHolds;

        /**
         * 成功获取 readLock 的最后一个线程的持有计数。此变量在通常情况下
         * 节省了 ThreadLocal 的查找，因为下一个要 release 的线程是最后一个
         * acquire 的。这是 non-volatile 的，只作为一种启发使用，而且对于线程
         * 缓存来说非常好。
         *
         * 可以比存储读持有计数的线程活的更久，但是通过不保留线程的引用来
         * 避免垃圾保留。
         *
         * 通过良性呃数据竞争访问；依赖于内存模型的 final 字段和非空保证。
         */
        private transient HoldCounter cachedHoldCounter;

        /**
         * firstReader 是获得读锁的第一个线程。
         * firstReaderHoldCount 是 firstReader 的持有计数。
         *
         * 更准确地说，firstReader 是最后一次将共享计数从 0 更改为 1 的唯一
         * 线程，并且从那时起就没有释放读锁；如果没有这样的线程则为 null。
         */
        private transient Thread firstReader = null;
        private transient int firstReaderHoldCount;

        // 构造函数
        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            setState(getState()); // 确保readHolds的可见性。ensures visibility of readHolds
        }

        /*
         * Acquires and releases use the same code for fair and
         * nonfair locks, but differ in whether/how they allow barging
         * when queues are non-empty.
         */

        /**
         * 如果当前线程在尝试获取读锁时应该阻塞，则返回 true。
         */
        abstract boolean readerShouldBlock();

        /**
         * 如果当前线程在尝试获取读锁时应该阻塞，则返回 true。
         */
        abstract boolean writerShouldBlock();

        /*
         * Note that tryRelease and tryAcquire can be called by
         * Conditions. So it is possible that their arguments contain
         * both read and write holds that are all released during a
         * condition wait and re-established in tryAcquire.
         */

        // 释放独占锁，基本和 ReentrantLock 一样
        protected final boolean tryRelease(int releases) {
            // 当前线程不是持有独占锁的线程，抛出异常
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            // 持有计数减少
            int nextc = getState() - releases;
            //独占锁的state状态是否为0，为0 才是true，不为0 为false
            boolean free = exclusiveCount(nextc) == 0;
            // 只有持有计数为 0 的时候才会完全释放
            if (free)
                setExclusiveOwnerThread(null); //当独占资源状态为0时，释放当前线程锁定
            setState(nextc);  //更新state。
            return free;   //成功释放返回true。失败返回false
        }

        // 获取独占锁，基本和 ReentrantLock 一样
        protected final boolean tryAcquire(int acquires) {
            /*
             * Walkthrough:
             * 1. If read count nonzero or write count nonzero
             *    and owner is a different thread, fail.
             * 2. If count would saturate, fail. (This can only
             *    happen if count is already nonzero.)
             * 3. Otherwise, this thread is eligible for lock if
             *    it is either a reentrant acquire or
             *    queue policy allows it. If so, update state
             *    and set owner.
             */
            Thread current = Thread.currentThread();
            int c = getState();    //得到同步器的state
            int w = exclusiveCount(c);  //得到state中记录写锁的计数值，它在state中的后低6位中

            // 写锁或读锁已经被获取,只有当前线程独占了写锁，即重入才可以。
            //@注意：如果已经先获取了写锁，不管之后该线程有没有获得读锁，再次重入写锁也是可以的。
            if (c != 0) {
                // (Note: if c != 0 and w == 0 then shared count != 0)
                // 写锁为 0 （即读锁已被获取）或者得到锁的不是当前线程，则获取失败
                // 说明：其他线程持有读锁时，当前线程写锁不能获取到资源
                //当前线程持有读锁时，如果线程未持有独占锁，即独占锁的持有者不是当前线程，也不能获取到资源。
                //即读锁不能升级成写锁。
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                // 写锁重入持有计数已达到限制，则获取失败，最大为2的16次方-1
                if (w + exclusiveCount(acquires) > MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                // Reentrant acquire
                // 重入获取成功，当且仅当，持有w!=0&&current==getExclusiveOwnerThread()
                setState(c + acquires);
                return true;
            }
            //到这一步说明state为0，则读锁写锁都没有线程拥有。
            // 当前线程应该阻塞，或者 CAS 设置状态失败，则获取锁失败
            // 1.如果是公平锁，那么writerShouldBlock只允许队列头的线程获取锁
            // 2.如果是非公平锁，不做限制，writerShouldBlock直接返回false
            if (writerShouldBlock() ||
                    !compareAndSetState(c, c + acquires))
                return false;
            // 否则获取成功,设置独占写锁。
            setExclusiveOwnerThread(current);
            return true;
        }

        // 共享模式释放锁
        protected final boolean tryReleaseShared(int unused) {
            Thread current = Thread.currentThread();
            // 如果当前为第一个获取读锁的线程，即最后一个使读锁的state从0变为1的线程
            if (firstReader == current) {
                // assert firstReaderHoldCount > 0;
                // 如果只持有一次，则可以释放，将 firstReader 设为空，否则计数
                // 减 1
                if (firstReaderHoldCount == 1)   //只持有1次，就释放 并重置firstReader
                    firstReader = null;
                else    //不止1次，则次数减1
                    firstReaderHoldCount--;
            } else {    //如果当前线程不是第一个获得读锁的
                // 获取当前线程的计数器
                HoldCounter rh = cachedHoldCounter;
                //如果当前线程不是最后一个获取所得线程，即无法从cachedHoldCounter中直接获取
                if (rh == null || rh.tid != getThreadId(current))
                    rh = readHolds.get(); //则获得当前线程map中存储的HoldCounter
                // 得到当前线程持有读锁的次数count。
                int count = rh.count;
                // 持有数小于等于1，
                if (count <= 1) {
                    //注意以下remove只是将线程的map中的底层table的entry引用删除了，如果cachedHoldCounter此时引用entry，它的值不会为null。
                    readHolds.remove();//不管是count为1，还是为0，将threadLocals中的映射直接删除，避免内存泄漏，
                    if (count <= 0)   //如果小于等于0，说明没有lock就进行unlock，抛出异常。
                        throw unmatchedUnlockException();
                }
                // 线程持有锁数减1
                --rh.count;
            }
            // 更新完持有计数之后，自旋更新同步器状态，把读锁的数量减 1
            //自旋的原因是因为，同一时间有可能有多个读锁正在进行释放锁
            for (;;) {
                int c = getState();
                int nextc = c - SHARED_UNIT;   //释放资源，同步器状态需要减SHARED_UNIT。
                if (compareAndSetState(c, nextc)) //只有成功设置同步器状态后，才会返回
                    // Releasing the read lock has no effect on readers,
                    // but it may allow waiting writers to proceed if
                    // both read and write locks are now free.
                    // 表示是否完全释放读锁资源。
                    return nextc == 0;  //说明只有读锁完全释放，才会唤醒后继线程，写锁才有机会争取到。
            }
        }

        private IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException(
                    "attempt to unlock read lock, not locked by current thread");
        }

        // 共享模式获取锁（获取读锁）
        protected final int tryAcquireShared(int unused) {
            /*
             * Walkthrough:
             * 1. If write lock held by another thread, fail.
             * 2. Otherwise, this thread is eligible for
             *    lock wrt state, so ask if it should block
             *    because of queue policy. If not, try
             *    to grant by CASing state and updating count.
             *    Note that step does not check for reentrant
             *    acquires, which is postponed to full version
             *    to avoid having to check hold count in
             *    the more typical non-reentrant case.
             * 3. If step 2 fails either because thread
             *    apparently not eligible or CAS fails or count
             *    saturated, chain to version with full retry loop.
             */
            Thread current = Thread.currentThread();
            int c = getState();
            // exclusiveCount(c) != 0 独占计数不等于 0，说明有线程持有写锁
            // 持有写锁的线程如果不是当前线程，则当前线程不能获取读锁。
            //返回-1。负数表示获取失败
            if (exclusiveCount(c) != 0 &&
                    getExclusiveOwnerThread() != current)
                return -1;

            //如果没有返回，说明要么写锁没有被其他线程拥有，要么写锁被当前线程拥有
            //获取写锁后，可以再获取读锁，这时锁降级的原理。
            // 获取持有读锁的同步器状态值，
            int r = sharedCount(c);//r记录进行CAS前的读锁的状态值

            // 1.readerShouldBlock(),在非公平锁中：如果exclusiveCount(c)不为0，就有可能导致同步队列中
            //存在竞争写锁而阻塞的线程，如果head.next是个独占模式节点，则发返回true。
            //在公平锁中：如果同步队列中有正在等待的线程，不管是acquire读锁还是写锁，都会返回true。
            // 2.没有达到最大数量
            // 3.满足前面条件就进行CAS 更新，将c变为c+SHARED_UNIT。
            //成功将进行firstReader、firstReaderHoldCount、cachedHoldCounter的状态更新。
            if (!readerShouldBlock() &&
                    r < MAX_COUNT &&
                    compareAndSetState(c, c + SHARED_UNIT)) {

                // 如果未CAS之前的读锁状态为0，说明当前线程是第一个获得读锁的线程
                //第一次获得读锁的线程，没有调用readHolds.get，说明线程map中没有存储该映射。
                if (r == 0) {
                    firstReader = current;// 初始化 firstReader 和 firstReaderHoldCount
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) {//读锁不是第一次被获取。
                    // 但是当前线程就是首次获取的线程，即读锁重入，
                    // 计数加 1
                    firstReaderHoldCount++;
                } else {//不是第一次获取读锁，且当前线程也不是firstReader。则写锁状态必定为0，
                    //cachedHoldCounter为记录的最后一次获得读锁的线程。
                    HoldCounter rh = cachedHoldCounter;
                    //如果最后一次获取读锁的线程为空，或者不是当前线程，则更新cachedHoldCounter。
                    if (rh == null || rh.tid != getThreadId(current))
                        //readHolds的get会以当前线程为key，到当前线程的threadLocals映射中找到记录的holdCount
                        //如果线程之前没有拥有过该读锁，那么它会新建一个ThreadLocal.ThreadLocalMap，
                        //并且以key=当前线程，value=new HoldCounter()的entry放入Map中，
                        //这个Map最后会赋值给当前线程的threadLocals。
                        cachedHoldCounter = rh = readHolds.get();
                    else if (rh.count == 0)     //如果当前线程是最后一次获得读锁的线程，但记数为0。
                        readHolds.set(rh);  //在count为1时，释放过读锁，则一定调用过readHold.remove，所以要重新set。
                    //不等于null，并且当前线程是cachedHoldCounter缓存中的最后一个线程，
                    //则直接从变量rh中拿，不从map中获取，性能快那么一点点。
                    rh.count++;
                }
                // 返回 1 表示获取成功
                return 1;
            }
            // 一次获取读锁失败后，尝试循环获取
            //失败原因可能有，CAS过程中失败，公平锁中，同步队列中有节点等待。
            //非公平锁中，同步队列中head.next是一个独占模式节点在等待
            //或者锁超过了2的16次方-1
            return fullTryAcquireShared(current);
        }

        /**
         * 读锁 acquire 的完整版本，包含 CAS 失败或者 tryAcquireShared 中
         * 没有获取可重入读锁的情况
         * tryAcquireShared 是一次快速获取的情况，且里面用到的 CAS 只允许一个
         * 线程获取成功，但读锁是共享的，所以需要此函数来循环获取，直到成功。
         */
        //该方法处理CAS失败，或者重入失败的情况。
        final int fullTryAcquireShared(Thread current) {
            /*
             * This code is in part redundant with that in
             * tryAcquireShared but is simpler overall by not
             * complicating tryAcquireShared with interactions between
             * retries and lazily reading hold counts.
             */
            HoldCounter rh = null;
            for (;;) {
                int c = getState();
                // 如果有线程获取到了写锁
                if (exclusiveCount(c) != 0) {
                    // 判断获取到写锁的是不是当前线程，如果不是则返回获取失败
                    //但是如果是当前线程持有写锁，那么可以尝试获取多次读锁，这个在低下会进行处理。
                    if (getExclusiveOwnerThread() != current)
                        return -1;
                }
                //没有线程获得了写锁，但是因为公平锁队列存在等待节点，
                //或者非公平锁中，下一个是独占模式节点，即争取写锁。
                else if (readerShouldBlock()) {
                    // Make sure we're not acquiring read lock reentrantly
                    // 如果当前线程是 firstReader，一定是重入，
                    //因为如果firstReaderHoldCount为0，firstReader应该为null。
                    if (firstReader == current) {
                        //重入在下面代码判断。
                        // assert firstReaderHoldCount > 0;
                    } else {    //如果不是第一个获取读锁的线程，有可能在重入，也有可能是首次获取读锁
                        if (rh == null) {
                            rh = cachedHoldCounter;
                            // 如果当前线程不是缓存的最后一个线程，从 readHolds 里读取
                            if (rh == null || rh.tid != getThreadId(current)) {
                                rh = readHolds.get();
                                if (rh.count == 0)   //如果发现当前线程是首次获取锁，那么它确实应该被阻塞。
                                    readHolds.remove();  //将get创建的映射删除掉，反正内存泄漏，
                            }
                        }
                        // 注意：
                        // 已经获取了读锁的线程重入时，不能阻塞，阻塞会导致死锁。
                        // 所以如果得到count为0，说明该线程不是重入，那么它就应该被阻塞。
                        if (rh.count == 0)
                            return -1;    //失败返回-1.
                    }
                }
                //到这一步，只可能是重入，或者锁降级的情况、或者是锁过多了。
                // 读锁的数量达到限制
                if (sharedCount(c) == MAX_COUNT)  //先排除锁过多情况
                    throw new Error("Maximum lock count exceeded");
                // 此线程不应该被阻塞，且读锁的数量没有达到限制，那么可以获取读锁
                // 获取读锁成功，下面的处理和 tryAcquireShared 类似
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (sharedCount(c) == 0) {   //再考虑锁降级的情况，
                        firstReader = current;   //持有写锁的线程一定是第一个获得读锁的线程。
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {   //如果是锁降级中的读锁重入。
                        firstReaderHoldCount++;   //那么记数自加
                    } else {    //写锁未被拥有的情况下的读锁重入。
                        if (rh == null)
                            rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                        cachedHoldCounter = rh; // 更新cachedHoldCounter
                    }
                    return 1;
                }
            }
        }

        /**
         * 实现独占版本的 tryLock。除了缺少对 writerShouldBlock 的调用外，
         * 这与 tryAcquire 在效果上是相同的。
         * 尝试获取写锁一次，成功即返回 true，失败返回 false。
         */
        //没有公平锁判断，即都是非公平版本
        final boolean tryWriteLock() {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c != 0) {
                int w = exclusiveCount(c);
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                if (w == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
            }
            if (!compareAndSetState(c, c + 1))
                return false;
            setExclusiveOwnerThread(current);
            return true;
        }

        /**
         * 实现共享版本的 tryLock。除了缺少对 readShouldBlock 的调用外，
         * 这与 tryAcquireShared 在效果上是相同的。
         */
        //直接自旋，因为不需要做公平性判定，不在乎队列是否有
        final boolean tryReadLock() {
            Thread current = Thread.currentThread();
            // 自旋获取
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0 &&
                        getExclusiveOwnerThread() != current)
                    return false;
                int r = sharedCount(c);
                if (r == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                // CAS 方式设置线程数加 1
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (r == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        HoldCounter rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            cachedHoldCounter = rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                    }
                    return true;
                }
            }
        }

        // 是否是当前线程持有
        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        // Methods relayed to outer class

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // 获取持有写锁的线程
        final Thread getOwner() {
            // Must read state before owner to ensure memory consistency
            return ((exclusiveCount(getState()) == 0) ?
                    null :
                    getExclusiveOwnerThread());
        }

        // 持有读锁的线程数
        final int getReadLockCount() {
            return sharedCount(getState());
        }

        // 是否持有写锁
        final boolean isWriteLocked() {
            return exclusiveCount(getState()) != 0;
        }

        // 写锁的持有计数（重入次数加 1）
        final int getWriteHoldCount() {
            return isHeldExclusively() ? exclusiveCount(getState()) : 0;
        }

        // 获取当前线程读锁的持有计数
        final int getReadHoldCount() {
            // 没有线程持有读锁
            if (getReadLockCount() == 0)
                return 0;

            // 当前线程
            Thread current = Thread.currentThread();
            // 当前线程是第一个获取读锁的线程
            if (firstReader == current)
                return firstReaderHoldCount;

            // 当前线程是缓存的线程
            HoldCounter rh = cachedHoldCounter;
            if (rh != null && rh.tid == getThreadId(current))
                return rh.count;

            // 从 ThreadLocal 里获取当前线程
            int count = readHolds.get().count;
            if (count == 0) readHolds.remove();
            return count;
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
                throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            readHolds = new ThreadLocalHoldCounter();
            setState(0); // reset to unlocked state
        }

        // 得到同步器状态
        final int getCount() { return getState(); }
    }

    /**
     * 同步器的非公平版本
     * 实现了 writerShouldBlock 和 readerShouldBlock
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -8159625535654395037L;
        final boolean writerShouldBlock() {
            return false; // writers can always barge
        }
        final boolean readerShouldBlock() {
            /* As a heuristic to avoid indefinite writer starvation,
             * block if the thread that momentarily appears to be head
             * of queue, if one exists, is a waiting writer.  This is
             * only a probabilistic effect since a new reader will not
             * block if there is a waiting writer behind other enabled
             * readers that have not yet drained from the queue.
             */
            return apparentlyFirstQueuedIsExclusive();
        }
    }

    /**
     * 同步器的非公平版本
     * 实现了 writerShouldBlock 和 readerShouldBlock（有后继者则必须等待）
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -2274990926593161451L;
        final boolean writerShouldBlock() {
            return hasQueuedPredecessors();
        }
        final boolean readerShouldBlock() {
            return hasQueuedPredecessors();
        }
    }

    /**
     * 读锁的实现
     */
    public static class ReadLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -5992448646407690164L;
        private final Sync sync;  //读锁类的内部同步器

        /**
         * 构造函数
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;   //在构造函数中同步器会使用读写锁中同步器
        }

        /**
         * 获取读锁（共享模式获取，调用 AQS 中的 acquireShared。
         *
         * 如果其他线程没有持有写锁，则获取读锁，并立即返回
         *
         * 如果写锁已经被其他线程持有，当前线程进入休眠直到读锁被 acquire。
         */
        public void lock() {
            sync.acquireShared(1);
        }

        /**
         * 以响应中断的方式获取读锁。
         *
         * 如果其他线程没有持有写锁，则获取读锁，并立即返回
         *
         * 如果写锁已经被其他线程持有，当前线程进入休眠直到发生其一：
         * 当前线程获取到读锁或者其他线程中断了当前线程。
         *
         * 如果当前线程：
         * 在进入此方法前已经设置了中断状态；或者在 acquire 的过程中被中断，
         * 抛出 InterruptedException 异常并清除中断状态。
         *
         * 此实现中，由于这个方法是一个显式的终端店，所以优先响应中断而不是
         * 正常进行或者获取可重入锁。
         *
         * @throws InterruptedException if the current thread is interrupted
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        /**
         * 如果在调用时写锁没有被其他线程持有，则 acquire 读锁。
         *
         * 如果写锁不被其他线程持有，则 acquire 读锁，并立即返回 true。
         * 当这个锁被设置成公平锁策略时，如果锁可用，调用 tryLock会立即获得
         * 读锁，不管其它线程是否正在等待读锁。这种行为在某些情况下是
         * 有用的，即使它破坏了公平性。如果想为这个锁设置公平性，使用
         * tryLock(0, TimeUnit.SECONDS)即可（他会检测中断）。
         *
         * 如果写锁被其他线程持有，此方法会立刻返回 false。
         *
         * @return {@code true} if the read lock was acquired
         */
        public boolean tryLock() {
            return sync.tryReadLock();
        }

        /**
         * 如果在给定的等待时间内没有其他线程持有写锁，且当前线程没有中断，
         * 则 acquire 读锁。
         *
         * 如果写锁不被其他线程持有，则 acquire 读锁，并立即返回 true。
         * 当这个锁被设置成使用公平策略时，如果有其他线程正在等待锁，那么
         * 锁不会被 acquire 到。这与 tryLock 形成对比。如果想要一个定时的
         * tryLock，它允许对一个公平的锁进行操作，那么把定时的和不定时的
         * 结合在一起：
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}
         *
         * 如果写锁被其他线程持有，那么当前线程进入睡眠。直到发生以下三种
         * 情况之一：
         * 此读锁被当前线程 acquire；或者其他线程中断了此线程；或者时间到期。
         *
         * 如果读锁被当前线程 acquire 到，返回 true。
         *
         * 如果当前线程：
         * 在进入此方法时其中断状态已设置，或者在获取锁时被中断了，那么抛出
         * InterruptedException 异常，并清除当前线程的中断状态。
         *
         * 如果指定的等待时间到期了，返回 false。如果时间小于等于 0，此方法
         * 不会再等待。
         *
         * 在此实现中，由于这个方法是显式中断点，所以优先相应中断而不是正常的
         * 或者可重入的锁获取，也不是等待时间的流逝。
         *
         * @param timeout the time to wait for the read lock
         * @param unit the time unit of the timeout argument
         * @return {@code true} if the read lock was acquired
         * @throws InterruptedException if the current thread is interrupted
         * @throws NullPointerException if the time unit is null
         */
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

        /**
         * 尝试释放锁。
         *
         * 如果读线程的数量此刻为 0，则释放锁给写线程。
         */
        public void unlock() {
            sync.releaseShared(1);
        }

        /**
         * 直接抛出异常，因为读锁不支持 Condition。
         *
         * @throws UnsupportedOperationException always
         */
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        /**
         * @return a string identifying this lock, as well as its lock state
         */
        //会输出读锁的state状态值。即被获取了多少次
        public String toString() {
            int r = sync.getReadLockCount();
            return super.toString() +
                    "[Read locks = " + r + "]";
        }
    }

    /**
     * 写锁的实现
     */
    public static class WriteLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -4992448646407690164L;
        private final Sync sync;

        /**
         * 构造函数
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * acquire 写锁。
         *
         * 如果锁没有被其他读线程或写线程持有则 acquire 写锁，并立即返回，
         * 设置锁的持有数为 1。
         *
         * 如果当前线程已经持有写锁，那么持有计数加 1，然后立刻返回。
         *
         * 如果锁被其他线程持有，那么当前线程进入睡眠。直到锁被 acquire 到，
         * 此时锁持有的计数被设置成 1。
         */
        public void lock() {
            sync.acquire(1);
        }

        /**
         * 以响应中断的方式获取写锁。
         *
         * 如果写锁不被其他读线程或写线程持有，则获取锁并立即返回，将持有
         * 计数设置为 1。
         *
         * 如果当前线程已经持有此锁，那么持有计数加 1，然后立即返回。
         *
         * 如果锁被其他线程持有，那么当前线程进入睡眠。直到发生以下两种情况之一：
         * 此锁被当前线程 acquire；或者其他线程中断了此线程。
         *
         * 如果锁被当前线程 acquire 到，持有计数设置为 1。
         *
         * 如果当前线程：
         * 在进入此方法时其中断状态已设置，或者在获取锁时被中断了，那么抛出
         * InterruptedException 异常，并清除当前线程的中断状态。
         *
         * 在此实现中，由于这个方法是显式中断点，所以优先相应中断而不是正常的
         * 或者可重入的锁获取。
         *
         * @throws InterruptedException if the current thread is interrupted
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        /**
         * 如果在调用时写锁没有被其他线程持有，则 acquire 锁。
         *
         * 如果锁不被其他读线程或写线程持有，则 acquire 锁，并立即返回 true，
         * 设置锁的持有计数为 1。当这个锁被设置成公平锁策略时，如果锁可用，
         * 调用 tryLock 会立即获得锁，不管其它线程是否正在等待锁。这种行为
         * 在某些情况下是有用的，即使它破坏了公平性。如果想为这个锁设置
         * 公平性，使用 tryLock(0, TimeUnit.SECONDS)即可（他会检测中断）。
         *
         * 如果当前线程已经持有该锁，那么持有计数将增加 1，然后此方法返回 true。
         *
         * 如果锁被其他线程持有，此方法立即返回 false。
         *
         * @return {@code true} if the lock was free and was acquired
         * by the current thread, or the write lock was already held
         * by the current thread; and {@code false} otherwise.
         */
        public boolean tryLock( ) {
            return sync.tryWriteLock();
        }

        /**
         * 如果在给定的等待时间内没有其他线程持有锁，且当前线程没有中断，
         * 则 acquire 锁。
         *
         * 如果锁不被其他线程持有，则 acquire 锁，并立即返回 true，设置持有计数
         * 为 1。当这个锁被设置成使用公平策略时，如果有其他线程正在等待锁，
         * 那么锁不会被 acquire 到。这与 tryLock 形成对比。如果想要一个定时的
         * tryLock，它允许对一个公平的锁进行操作，那么把定时的和不定时的
         * 结合在一起：
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}
         *
         * 如果当前线程已经持有锁，那么持有计数加 1，然后方法返回 true。
         *
         * 如果锁被其他线程持有，那么当前线程进入睡眠。直到发生以下三种情况之一：
         * 此锁被当前线程 acquire；或者其他线程中断了此线程；或者时间到期。
         *
         * 如果锁被当前线程 acquire，返回 true，持有计数设置为 1。
         *
         * 如果当前线程：
         * 在进入此方法时其中断状态已设置，或者在获取锁时被中断了，那么抛出
         * InterruptedException 异常，并清除当前线程的中断状态。
         *
         * 如果指定的等待时间到期了，返回 false。如果时间小于等于 0，此方法
         * 不会再等待。
         *
         * 在此实现中，由于这个方法是显式中断点，所以优先相应中断而不是正常的
         * 或者可重入的锁获取，也不是等待时间的流逝。
         *
         * @param timeout the time to wait for the write lock
         * @param unit the time unit of the timeout argument
         *
         * @return {@code true} if the lock was free and was acquired
         * by the current thread, or the write lock was already held by the
         * current thread; and {@code false} if the waiting time
         * elapsed before the lock could be acquired.
         *
         * @throws InterruptedException if the current thread is interrupted
         * @throws NullPointerException if the time unit is null
         */
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }

        /**
         * 尝试释放锁。
         *
         * 如果当前线程是这个锁的持有者，那么持有计数将递减。如果持有计数现在
         * 为 0，则释放锁。如果当前线程不是这个锁的持有者，则抛出
         * IllegalMonitorStateException 异常。
         *
         * @throws IllegalMonitorStateException if the current thread does not
         * hold this lock
         */
        public void unlock() {
            sync.release(1);
        }

        /**
         * 返回与此 Lock 实例一起使用的 Condition 实例。
         *
         * 返回的 Condition 实例支持 Object 监视器同样的以下方法：wait，notify，
         * notifyAll。
         *
         * 如果 Condition 方法调用时写锁没有被持有，抛出 IllegalMonitorStateException
         * 异常。（读锁的持有独立于写锁，因此不会被检查或影响。然而，当
         * 当前线程也获取到了读锁时，调用此方法通常是错误的，因为其他可以
         * 解除阻塞的线程将无法获取写锁。）
         *
         * 当 Condition 的 await 方法被调用时，如果锁被释放，在返回之前，会再次
         * 获取锁，并将持有计数恢复到方法被调用时的值。
         *
         * 如果一个线程在等待时被中断，那么等待将会停止，抛出 InterruptedException
         * 异常，线程的中断状态将被清除。
         *
         * 等待的线程按 FIFO 的顺序唤醒。
         *
         * 从等待方法返回的线程的锁重新 acquire 的顺序与最初获取锁的线程相同
         * （在默认情况下未指定），但对于公平锁，优先使用那些等待时间最长的线程。
         *
         * </ul>
         *
         * @return the Condition object
         */
        public Condition newCondition() {
            return sync.newCondition();
        }

        /**
         * @return a string identifying this lock, as well as its lock state
         */
        public String toString() {
            Thread o = sync.getOwner();
            return super.toString() + ((o == null) ?
                    "[Unlocked]" :
                    "[Locked by thread " + o.getName() + "]");
        }

        /**
         * 检查写锁是否被当前线程持有，和
         * ReentrantReadWriteLock.isWriteLockedByCurrentThread 方法相同。
         *
         * @return {@code true} if the current thread holds this lock and
         *         {@code false} otherwise
         * @since 1.6
         */
        public boolean isHeldByCurrentThread() {
            return sync.isHeldExclusively();
        }

        /**
         * 检查当前线程持有写锁的次数。和
         * ReentrantReadWriteLock.getWriteHoldCount 方法相同。
         *
         * @return the number of holds on this lock by the current thread,
         *         or zero if this lock is not held by the current thread
         * @since 1.6
         */
        public int getHoldCount() {
            return sync.getWriteHoldCount();
        }
    }

    // 监控和状态

    /**
     * 如果锁被设置为公平锁则返回 true。
     *
     * @return {@code true} if this lock has fairness set true
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 返回当前持有写锁的线程，如果没有返回 null。
     *
     * @return the owner, or {@code null} if not owned
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * 查询读锁被持有的次数。此方法用于监控系统状态，而不是同步控制。
     * @return the number of read locks held
     */
    public int getReadLockCount() {
        return sync.getReadLockCount();
    }

    /**
     * 检查写锁是否被某个线程持有。此方法用于监控系统状态，而不是同步控制。
     *
     * @return {@code true} if any thread holds the write lock and
     *         {@code false} otherwise
     */
    public boolean isWriteLocked() {
        return sync.isWriteLocked();
    }

    /**
     * 检查写锁是否被当前线程持有。
     *
     * @return {@code true} if the current thread holds the write lock and
     *         {@code false} otherwise
     */
    public boolean isWriteLockedByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * 查询当前线程的可重入写锁持有计数。
     *
     * @return the number of holds on the write lock by the current thread,
     *         or zero if the write lock is not held by the current thread
     */
    public int getWriteHoldCount() {
        return sync.getWriteHoldCount();
    }

    /**
     * 查询当前线程的可重入读锁持有计数。
     *
     * @return the number of holds on the read lock by the current thread,
     *         or zero if the read lock is not held by the current thread
     * @since 1.6
     */
    public int getReadHoldCount() {
        return sync.getReadHoldCount();
    }

    /**
     * 返回包含正在等待写锁的所有线程的集合。由于在构造这个结果时，实际的
     * 线程集可能会动态变化，返回的集合只是最佳估计。返回的集合中的元素
     * 没有特定顺序。此方法的目的是为了方便构造更多的监视器子类。
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedWriterThreads() {
        return sync.getExclusiveQueuedThreads();
    }

    /**
     * 返回包含正在等待读锁的所有线程的集合。由于在构造这个结果时，实际的
     * 线程集可能会动态变化，返回的集合只是最佳估计。返回的集合中的元素
     * 没有特定顺序。此方法的目的是为了方便构造更多的监视器子类。
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedReaderThreads() {
        return sync.getSharedQueuedThreads();
    }

    /**
     * 检查是否有任何线程正在等待读锁或者写锁。注意，因为取消可能随时发生，
     * 返回 true 并不保证任何其他线程将获得此锁。该方法主要用于监控系统状态。
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 检查指定线程是否正在等待读锁或者写锁。注意，因为取消可能随时发生，
     * 返回 true 并不保证任何其他线程将获得此锁。该方法主要用于监控系统状态。
     *
     * @param thread the thread
     * @return {@code true} if the given thread is queued waiting for this lock
     * @throws NullPointerException if the thread is null
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * 返回等待获取读锁或写锁的线程数量的估计值。这个值只是一个估计值，
     * 因为当这个方法遍历内部数据结构的时候线程可能会动态变化。此方法用来
     * 监视系统状态，而不是用于同步控制。
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 返回一个包含可能正在等待获取读锁或写锁的线程的集合。由于在构造这个结果时，
     * 实际的线程集可能会动态变化，返回的集合只是最佳估计。返回的集合中的
     * 元素没有特定顺序。此方法的目的是为了方便构造更多的监视器子类。
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * 查询是否有线程正在写锁的 condition 队列上等待。注意，由于时限到期或者中断
     * 可能随时发生，返回 true 并不保证未来的 signal 会唤醒任何线程。此方法
     * 主要用来监视系统状态。
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 返回与写锁关联的指定 condition 上等待的线程数量。注意，由于超时和中断
     * 可能随时发生，因此估计值仅用于实际等待者数量的上限。此方法用来监视
     * 系统状态，而不是用于同步控制。
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 返回一个集合，其中包含可能在与写锁关联的指定 condition 上等待的线程。
     * 由于在构造这个结果时，实际的线程集可能会动态变化，返回的集合只是
     * 最佳估计。返回的集合中的元素没有特定顺序。此方法的目的是为了方便
     * 构造更多的监视器子类。
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        int c = sync.getCount();
        int w = Sync.exclusiveCount(c);
        int r = Sync.sharedCount(c);

        return super.toString() +
                "[Write locks = " + w + ", Read locks = " + r + "]";
    }

    /**
     * 返回给定线程的线程 id。
     */
    static final long getThreadId(Thread thread) {
        return UNSAFE.getLongVolatile(thread, TID_OFFSET);
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long TID_OFFSET;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            TID_OFFSET = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("tid"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
