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
 * Written by Doug Lea, Bill Scherer, and Michael Scott with
 * assistance from members of JCP JSR-166 Expert Group and released to
 * the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package JUC;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p><b>Sample Usage:</b>
 * Here are the highlights of a class that uses an {@code Exchanger}
 * to swap buffers between threads so that the thread filling the
 * buffer gets a freshly emptied one when it needs it, handing off the
 * filled one to the thread emptying the buffer.
 *  {@code
 * class FillAndEmpty {
 *   Exchanger<DataBuffer> exchanger = new Exchanger<DataBuffer>();
 *   DataBuffer initialEmptyBuffer = ... a made-up type
 *   DataBuffer initialFullBuffer = ...
 *
 *   class FillingLoop implements Runnable {
 *     public void run() {
 *       DataBuffer currentBuffer = initialEmptyBuffer;
 *       try {
 *         while (currentBuffer != null) {
 *           addToBuffer(currentBuffer);
 *           if (currentBuffer.isFull())
 *             currentBuffer = exchanger.exchange(currentBuffer);
 *         }
 *       } catch (InterruptedException ex) { ... handle ... }
 *     }
 *   }
 *
 *   class EmptyingLoop implements Runnable {
 *     public void run() {
 *       DataBuffer currentBuffer = initialFullBuffer;
 *       try {
 *         while (currentBuffer != null) {
 *           takeFromBuffer(currentBuffer);
 *           if (currentBuffer.isEmpty())
 *             currentBuffer = exchanger.exchange(currentBuffer);
 *         }
 *       } catch (InterruptedException ex) { ... handle ...}
 *     }
 *   }
 *
 *   void start() {
 *     new Thread(new FillingLoop()).start();
 *     new Thread(new EmptyingLoop()).start();
 *   }
 * }}
 *
 * @since 1.5
 * @author Doug Lea and Bill Scherer and Michael Scott
 * @param <V> The type of objects that may be exchanged
 */
public class Exchanger<V> {

    /**
     * The byte distance (as a shift value) between any two used slots
     * in the arena.  1 << ASHIFT should be at least cacheline size.
     */
    private static final int ASHIFT = 7;

    /**
     * 支持的 arena 索引的最大值。能够分噢诶的 arena 最大值是 MMASK + 1。
     * 二进制 0000...000011111111
     */
    private static final int MMASK = 0xff;

    /**
     * Unit for sequence/version bits of bound field. Each successful
     * change to the bound also adds SEQ.
     * 二进制 0000...000100000000
     */
    private static final int SEQ = MMASK + 1;

    /** CPU 个数 */
    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    /**
     * arena 数组的最大索引：能够容纳所有线程，让线程不需要竞争的槽的数量，
     * 或者是
     * The maximum slot index of the arena: The number of slots that
     * can in principle hold all threads without contention, or at
     * most the maximum indexable value.
     */
    static final int FULL = (NCPU >= (MMASK << 1)) ? MMASK : NCPU >>> 1;

    /**
     * 自旋次数。
     */
    private static final int SPINS = 1 << 10;

    /**
     * 空对象。
     */
    private static final Object NULL_ITEM = new Object();

    /**
     * Sentinel value returned by internal exchange methods upon
     * timeout, to avoid need for separate timed versions of these
     * methods.
     */
    private static final Object TIMED_OUT = new Object();

    @sun.misc.Contended static final class Node {
        // 以下字段多槽交换时使用
        // 在 arena 中的下标，多个槽位的时候使用
        int index;              // Arena index
        // 上一次记录的 Exchanger.bound
        int bound;              // Last recorded value of Exchanger.bound
        // 当前 bound 下 CAS 失败的次数
        int collides;           // Number of CAS failures at current bound
        // 以下字段单槽交换时使用
        // 用于自旋是计算随机数
        int hash;               // Pseudo-random for spins
        // 线程当前需要交换的数据
        Object item;            // This thread's current item
        //  配对线程会把自身携带的值存入 match
        volatile Object match;  // Item provided by releasing thread
        // 休眠的线程（先到达的线程）
        volatile Thread parked; // Set to this thread when parked, else null
    }

    // 继承自 ThreadLocal，为每个线程保留唯一的 Node 节点
    static final class Participant extends ThreadLocal<Node> {
        public Node initialValue() { return new Node(); }
    }

    /**
     * ThreadLocal。
     */
    private final Participant participant;

    /**
     * 多槽交换数组
     */
    private volatile Node[] arena;

    /**
     * 单槽交换节点
     */
    private volatile Node slot;

    /**
     * The index of the largest valid arena position, OR'ed with SEQ
     * number in high bits, incremented on each update.  The initial
     * update from 0 to SEQ is used to ensure that the arena array is
     * constructed only once.
     */
    private volatile int bound;    //arena数组的最大有效索引。确保数组只被创建一次

    /**
     * 多槽交换。
     *
     * @param item the (non-null) item to exchange
     * @param timed true if the wait is timed
     * @param ns if timed, the maximum wait time, else 0L
     * @return the other thread's item; or null if interrupted; or
     * TIMED_OUT if timed and timed out
     */
    private final Object arenaExchange(Object item, boolean timed, long ns) {
        Node[] a = arena;
        // 当前线程携带的节点
        Node p = participant.get();
        for (int i = p.index;;) {                      // 当前线程的 arena 索引
            int b, m, c; long j;
            // 从 arena 数组中找出偏移地址为 (i << ASHIFT) + ABASE 的元素, 即真正可用的 Node
            Node q = (Node)U.getObjectVolatile(a, j = (i << ASHIFT) + ABASE);
            // 如果槽中节点不为 null，说明已经有线程在等待了
            if (q != null && U.compareAndSwapObject(a, j, q, null)) {
                // 获取已到达线程的数据
                Object v = q.item;                     // release
                // 设置已到达线程的 match（先到达的线程感应到 match 已设置，就会继续往后直行）
                q.match = item;
                // 唤醒阻塞的线程
                Thread w = q.parked;
                if (w != null)
                    U.unpark(w);
                return v;
            }
            // 槽位可用而且节点为 null，说明当前线程是先到达的那一个。
            else if (i <= (m = (b = bound) & MMASK) && q == null) {
                p.item = item;                         // offer
                // 如果成功设置槽位为当前线程
                if (U.compareAndSwapObject(a, j, null, p)) {
                    long end = (timed && m == 0) ? System.nanoTime() + ns : 0L;
                    Thread t = Thread.currentThread(); // wait
                    // 开始自旋
                    for (int h = p.hash, spins = SPINS;;) {
                        Object v = p.match;
                        // 有线程到达了
                        if (v != null) {
                            U.putOrderedObject(p, MATCH, null);
                            p.item = null;             // clear for next use
                            p.hash = h;
                            return v;
                        }
                        // 继续自旋
                        else if (spins > 0) {
                            h ^= h << 1; h ^= h >>> 3; h ^= h << 10; // xorshift
                            if (h == 0)                // initialize hash
                                h = SPINS | (int)t.getId();
                            else if (h < 0 &&          // approx 50% true
                                    (--spins & ((SPINS >>> 1) - 1)) == 0)
                                Thread.yield();        // two yields per wait
                        }
                        // 配对线程已到达，但是还没设置 match，继续自旋
                        else if (U.getObjectVolatile(a, j) != p)
                            spins = SPINS;       // releaser hasn't set match yet
                        else if (!t.isInterrupted() && m == 0 &&
                                (!timed ||
                                        (ns = end - System.nanoTime()) > 0L)) {
                            // 没等待任何线程，阻塞
                            U.putObject(t, BLOCKER, this); // emulate LockSupport
                            p.parked = t;              // minimize window
                            if (U.getObjectVolatile(a, j) == p)
                                U.park(false, ns);
                            p.parked = null;
                            U.putObject(t, BLOCKER, null);
                        }
                        else if (U.getObjectVolatile(a, j) == p &&
                                U.compareAndSwapObject(a, j, p, null)) {
                            // 尝试缩减 arena 数组（修改 bound）
                            if (m != 0)                // try to shrink
                                U.compareAndSwapInt(this, BOUND, b, b + SEQ - 1);
                            p.item = null;
                            p.hash = h;
                            i = p.index >>>= 1;        // descend
                            if (Thread.interrupted())
                                return null;
                            if (timed && m == 0 && ns <= 0L)
                                return TIMED_OUT;
                            break;                     // expired; restart
                        }
                    }
                }
                // 占用槽位失败
                else
                    p.item = null;                     // clear offer
            }
            // 槽位不可用
            else {
                if (p.bound != b) {                    // stale; reset
                    p.bound = b;
                    p.collides = 0;
                    i = (i != m || m == 0) ? m : m - 1;
                }
                else if ((c = p.collides) < m || m == FULL ||
                        !U.compareAndSwapInt(this, BOUND, b, b + SEQ + 1)) {
                    p.collides = c + 1;
                    i = (i == 0) ? m : i - 1;          // cyclically traverse
                }
                else
                    i = m + 1;                         // grow
                p.index = i;
            }
        }
    }

    /**
     * 单槽交换。
     *
     * @param item the item to exchange
     * @param timed true if the wait is timed
     * @param ns if timed, the maximum wait time, else 0L
     * @return the other thread's item; or null if either the arena
     * was enabled or the thread was interrupted before completion; or
     * TIMED_OUT if timed and timed out
     */
    private final Object slotExchange(Object item, boolean timed, long ns) {
        // 当前线程携带的 Node 节点
        Node p = participant.get();
        // 当前线程
        Thread t = Thread.currentThread();
        if (t.isInterrupted()) // preserve interrupt status so caller can recheck
            return null;

        // 自旋
        for (Node q;;) {
            // q 不等于 null，slot上存放了其他线程放入的节点。
            if ((q = slot) != null) {
                // 把 slot 置为 null，让出空槽
                if (U.compareAndSwapObject(this, SLOT, q, null)) {
                    // 取出slot中存放节点的item赋值给v用于返回。
                    Object v = q.item;
                    q.match = item;     //slot中的match赋值为当前线程请求交换的item。
                    // 交换完成后，唤醒 slot 里的线程
                    Thread w = q.parked;
                    if (w != null)
                        U.unpark(w);
                    // 返回交换到的
                    return v;
                }
                // 把 slot 置为 null 的尝试失败了，说明多个线程竞争修改 slot。
               //  bound == 0 表示 arena 数组未初始化过，CAS操作bound将其增加SEQ
                // 创造一个 arena 数组
                if (NCPU > 1 && bound == 0 &&
                        U.compareAndSwapInt(this, BOUND, 0, SEQ))
                    arena = new Node[(FULL + 2) << ASHIFT];
            }
            // 如果 slot 为 null 但是 arena 不为 null，应该调用的是 arenaExchange，直接返回 null。
            else if (arena != null)
                return null; // caller must reroute to arenaExchange

              // slot 为 null 且 arena 为 null，说明之前没有线程到达。
            // 只有在这种情况下会 break，不会 return。
            else {
                // 设置 slot。
                // 这里只需要设置 item，不需要设置 thread 等，因为 thread 还没有休眠
                p.item = item;
                if (U.compareAndSwapObject(this, SLOT, null, p))
                    break;
                p.item = null;
            }
        }

        // 如果执行到这里说明当前线程是先到达的那一个，且已经设置好了 slot，
        // 等待另一个线程到达。
        int h = p.hash;
        long end = timed ? System.nanoTime() + ns : 0L;
        // 自旋次数
        int spins = (NCPU > 1) ? SPINS : 1;
        Object v;
        // 如果配对的线程还没来
        while ((v = p.match) == null) {
            if (spins > 0) {
                // 自旋过程中随机释放 CPU，让其他线程可以竞争
                h ^= h << 1; h ^= h >>> 3; h ^= h << 10;
                if (h == 0)
                    h = SPINS | (int)t.getId();
                else if (h < 0 && (--spins & ((SPINS >>> 1) - 1)) == 0)
                    Thread.yield();
            }
            // 配对线程已经来了，已经把 slot 改了，继续自旋
            else if (slot != p)
                spins = SPINS;
            // 配对线程还没来，spins <= 0 了
            else if (!t.isInterrupted() && arena == null &&
                    (!timed || (ns = end - System.nanoTime()) > 0L)) {
                // BLOCKER 保存当前线程
                U.putObject(t, BLOCKER, this);
                // 当前线程休眠
                p.parked = t;
                if (slot == p)
                    U.park(false, ns);
                // 当前线程被唤醒
                p.parked = null;
                U.putObject(t, BLOCKER, null);
            }
            // 超时或者其他原因，让出 slot
            else if (U.compareAndSwapObject(this, SLOT, p, null)) {
                v = timed && ns <= 0L && !t.isInterrupted() ? TIMED_OUT : null;
                break;
            }
        }
        // p.match 不为 null 了，先到的线程也可以返回了
        U.putOrderedObject(p, MATCH, null);
        p.item = null;
        p.hash = h;
        return v;
    }

    /**
     * 构造函数，初始化 Participant
     */
    public Exchanger() {
        participant = new Participant();  //创建ThreadLoal
    }

    /**
     * 等待其他线程到达交换点的时候（除非当前线程被中断），将指定数据传给该线程，
     * 并接受该线程的数据。
     *
     * @param x the object to exchange
     * @return the object provided by the other thread
     * @throws InterruptedException if the current thread was
     *         interrupted while waiting
     */
    @SuppressWarnings("unchecked")
    public V exchange(V x) throws InterruptedException {
        Object v;
        // 要交换的对象
        Object item = (x == null) ? NULL_ITEM : x; // translate null args
        // 如果 arena 为 null，执行 slotExchange，也就是单个槽的方法。slotExchange 传入参数为需要交换的对象
        // 如果 arena 不为 null，或者单槽交换失败时，执行 arenaExchange 多槽交换
        if ((arena != null ||
                (v = slotExchange(item, false, 0L)) == null) &&
                ((Thread.interrupted() || // disambiguates null return
                        (v = arenaExchange(item, false, 0L)) == null)))
            throw new InterruptedException();
        return (v == NULL_ITEM) ? null : (V)v;
    }

    /**
     * Waits for another thread to arrive at this exchange point (unless
     * the current thread is {@linkplain Thread#interrupt interrupted} or
     * the specified waiting time elapses), and then transfers the given
     * object to it, receiving its object in return.
     *
     * <p>If another thread is already waiting at the exchange point then
     * it is resumed for thread scheduling purposes and receives the object
     * passed in by the current thread.  The current thread returns immediately,
     * receiving the object passed to the exchange by that other thread.
     *
     * <p>If no other thread is already waiting at the exchange then the
     * current thread is disabled for thread scheduling purposes and lies
     * dormant until one of three things happens:
     * <ul>
     * <li>Some other thread enters the exchange; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for the exchange,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then {@link
     * TimeoutException} is thrown.  If the time is less than or equal
     * to zero, the method will not wait at all.
     *
     * @param x the object to exchange
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return the object provided by the other thread
     * @throws InterruptedException if the current thread was
     *         interrupted while waiting
     * @throws TimeoutException if the specified waiting time elapses
     *         before another thread enters the exchange
     */
    @SuppressWarnings("unchecked")
    public V exchange(V x, long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException {
        Object v;
        Object item = (x == null) ? NULL_ITEM : x;
        long ns = unit.toNanos(timeout);
        if ((arena != null ||
                (v = slotExchange(item, true, ns)) == null) &&
                ((Thread.interrupted() ||
                        (v = arenaExchange(item, true, ns)) == null)))
            throw new InterruptedException();
        if (v == TIMED_OUT)
            throw new TimeoutException();
        return (v == NULL_ITEM) ? null : (V)v;
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final long BOUND;
    private static final long SLOT;
    private static final long MATCH;
    private static final long BLOCKER;
    private static final int ABASE;
    static {
        int s;
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> ek = Exchanger.class;
            Class<?> nk = Node.class;
            Class<?> ak = Node[].class;
            Class<?> tk = Thread.class;
            BOUND = U.objectFieldOffset
                    (ek.getDeclaredField("bound"));
            SLOT = U.objectFieldOffset
                    (ek.getDeclaredField("slot"));
            MATCH = U.objectFieldOffset
                    (nk.getDeclaredField("match"));
            BLOCKER = U.objectFieldOffset
                    (tk.getDeclaredField("parkBlocker"));
            s = U.arrayIndexScale(ak);
            // ABASE absorbs padding in front of element 0
            ABASE = U.arrayBaseOffset(ak) + (1 << ASHIFT);

        } catch (Exception e) {
            throw new Error(e);
        }
        if ((s & (s-1)) != 0 || s > (1 << ASHIFT))
            throw new Error("Unsupported array scale");
    }

}
