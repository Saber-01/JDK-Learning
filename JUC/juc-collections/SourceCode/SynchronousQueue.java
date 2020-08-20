package JUC.JUCCollections;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.*;

/**
 * 一个实现 BlockingQueue 接口的阻塞队列，每个插入操作必须等待另一个线程
 * 相应的删除操作，反之亦然。SynchronousQueue 没有任何内部容量。不能使用
 * peek 方法，因为一个元素只有在试图删除它的时候才会出现；不能插入元素，
 * 除非有其他线程想要删除它；不能迭代，因为没有任何元素可以迭代。队列的
 * head 是第一个插入线程试图添加到队列的元素；如果没有这样的线程，那么也
 * 不存在删除，poll 操作会返回 null。对于其它 Collection 的方法，例如 contains，
 * SynchronousQueue 完全是空集合。此队列不允许 null 元素。
 *
 * 此类支持可选的公平性策略，用来对等待的生产者和消费者排序。默认情况下，
 * 使用公平的方式，遵循 FIFO 的顺序。
 *
 * 此类是 Java Collections Framework 的成员。
 *
 * @since 1.5
 * @author Doug Lea and Bill Scherer and Michael Scott
 * @param <E> the type of elements held in this collection
 */
public class SynchronousQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = -3223113410248163686L;

    /*
     * This class implements extensions of the dual stack and dual
     * queue algorithms described in "Nonblocking Concurrent Objects
     * with Condition Synchronization", by W. N. Scherer III and
     * M. L. Scott.  18th Annual Conf. on Distributed Computing,
     * Oct. 2004 (see also
     * http://www.cs.rochester.edu/u/scott/synchronization/pseudocode/duals.html).
     * The (Lifo) stack is used for non-fair mode, and the (Fifo)
     * queue for fair mode. The performance of the two is generally
     * similar. Fifo usually supports higher throughput under
     * contention but Lifo maintains higher thread locality in common
     * applications.
     *
     * A dual queue (and similarly stack) is one that at any given
     * time either holds "data" -- items provided by put operations,
     * or "requests" -- slots representing take operations, or is
     * empty. A call to "fulfill" (i.e., a call requesting an item
     * from a queue holding data or vice versa) dequeues a
     * complementary node.  The most interesting feature of these
     * queues is that any operation can figure out which mode the
     * queue is in, and act accordingly without needing locks.
     *
     * Both the queue and stack extend abstract class Transferer
     * defining the single method transfer that does a put or a
     * take. These are unified into a single method because in dual
     * data structures, the put and take operations are symmetrical,
     * so nearly all code can be combined. The resulting transfer
     * methods are on the long side, but are easier to follow than
     * they would be if broken up into nearly-duplicated parts.
     *
     * The queue and stack data structures share many conceptual
     * similarities but very few concrete details. For simplicity,
     * they are kept distinct so that they can later evolve
     * separately.
     *
     * The algorithms here differ from the versions in the above paper
     * in extending them for use in synchronous queues, as well as
     * dealing with cancellation. The main differences include:
     *
     *  1. The original algorithms used bit-marked pointers, but
     *     the ones here use mode bits in nodes, leading to a number
     *     of further adaptations.
     *  2. SynchronousQueues must block threads waiting to become
     *     fulfilled.
     *  3. Support for cancellation via timeout and interrupts,
     *     including cleaning out cancelled nodes/threads
     *     from lists to avoid garbage retention and memory depletion.
     *
     * Blocking is mainly accomplished using LockSupport park/unpark,
     * except that nodes that appear to be the next ones to become
     * fulfilled first spin a bit (on multiprocessors only). On very
     * busy synchronous queues, spinning can dramatically improve
     * throughput. And on less busy ones, the amount of spinning is
     * small enough not to be noticeable.
     *
     * Cleaning is done in different ways in queues vs stacks.  For
     * queues, we can almost always remove a node immediately in O(1)
     * time (modulo retries for consistency checks) when it is
     * cancelled. But if it may be pinned as the current tail, it must
     * wait until some subsequent cancellation. For stacks, we need a
     * potentially O(n) traversal to be sure that we can remove the
     * node, but this can run concurrently with other threads
     * accessing the stack.
     *
     * While garbage collection takes care of most node reclamation
     * issues that otherwise complicate nonblocking algorithms, care
     * is taken to "forget" references to data, other nodes, and
     * threads that might be held on to long-term by blocked
     * threads. In cases where setting to null would otherwise
     * conflict with main algorithms, this is done by changing a
     * node's link to now point to the node itself. This doesn't arise
     * much for Stack nodes (because blocked threads do not hang on to
     * old head pointers), but references in Queue nodes must be
     * aggressively forgotten to avoid reachability of everything any
     * node has ever referred to since arrival.
     */

    /**
     * 双重队列和双重栈共同的抽象类接口。
     */
    abstract static class Transferer<E> {
        /**
         * 用于 put 和 take
         *
         * @param e if non-null, the item to be handed to a consumer;
         *          if null, requests that transfer return an item
         *          offered by producer.
         * @param timed if this operation should timeout
         * @param nanos the timeout, in nanoseconds
         * @return if non-null, the item provided or received; if null,
         *         the operation failed due to timeout or interrupt --
         *         the caller can distinguish which of these occurred
         *         by checking Thread.interrupted.
         */
        abstract E transfer(E e, boolean timed, long nanos);
    }

    /** CPU 个数*/
    static final int NCPUS = Runtime.getRuntime().availableProcessors();

    /**
     * 超时等待的情况下，在阻塞之前自旋的次数。
     */
    static final int maxTimedSpins = (NCPUS < 2) ? 0 : 32;

    /**
     * 没有超时等待的情况下，在阻塞之前自旋的次数。
     */
    static final int maxUntimedSpins = maxTimedSpins * 16;

    /**
     * 针对有超时的情况，达到自旋次数后，如果剩余时间大于此阈值，就使用
     * LockSupport.parkNanos 阻塞。
     */
    static final long spinForTimeoutThreshold = 1000L;

    /** 双重栈 */
    static final class TransferStack<E> extends Transferer<E> {
        /*
         * This extends Scherer-Scott dual stack algorithm, differing,
         * among other ways, by using "covering" nodes rather than
         * bit-marked pointers: Fulfilling operations push on marker
         * nodes (with FULFILLING bit set in mode) to reserve a spot
         * to match a waiting node.
         */

        /* Modes for SNodes, ORed together in node fields */
        /** 消费者（请求数据）节点 */
        static final int REQUEST    = 0;
        /** 生产者（提供数据）节点 */
        static final int DATA       = 1;
        /** 二者正在匹配 */
        //fulfill：完成，履行，
        static final int FULFILLING = 2;

        /** 如果节点正在匹配就返回 true */
        static boolean isFulfilling(int m) { return (m & FULFILLING) != 0; }

        /** TransferStacks 的节点类 */
        static final class SNode {
            volatile SNode next;        // next node in stack
            // 此节点匹配的节点
            volatile SNode match;       // the node matched to this
            // 此节点上等待的线程
            volatile Thread waiter;     // to control park/unpark
            Object item;                // data; or null for REQUESTs
            // 节点类型
            int mode;
            // Note: item and mode fields don't need to be volatile
            // since they are always written before, and read after,
            // other volatile/atomic operations.

            SNode(Object item) {
                this.item = item;
            }

            boolean casNext(SNode cmp, SNode val) {
                return cmp == next &&
                        UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
            }

            /**
             * 尝试将节点 s 和此节点匹配。
             *
             * @param s the node to match
             * @return true if successfully matched to s
             */
            boolean tryMatch(SNode s) {
                // 此节点 m 还没有匹配者，将 s 作为其匹配着
                if (match == null &&
                        UNSAFE.compareAndSwapObject(this, matchOffset, null, s)) {
                    Thread w = waiter;
                    if (w != null) {    // waiters need at most one unpark
                        waiter = null;
                        // 唤醒 m 中的线程，匹配完毕
                        LockSupport.unpark(w);
                    }
                    return true;
                }
                // 返回 boolean，判断匹配到的线程是不是 s
                return match == s;
            }

            /**
             * 将 match 指向自己，表示取消了
             */
            void tryCancel() {
                UNSAFE.compareAndSwapObject(this, matchOffset, null, this);
            }

            // 是否取消了
            boolean isCancelled() {
                return match == this;
            }

            // Unsafe mechanics
            private static final sun.misc.Unsafe UNSAFE;
            private static final long matchOffset;
            private static final long nextOffset;

            static {
                try {
                    UNSAFE = sun.misc.Unsafe.getUnsafe();
                    Class<?> k = SNode.class;
                    matchOffset = UNSAFE.objectFieldOffset
                            (k.getDeclaredField("match"));
                    nextOffset = UNSAFE.objectFieldOffset
                            (k.getDeclaredField("next"));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }

        /** 栈的头节点（top）节点 */
        volatile SNode head;

        // head 是栈中最重要的属性，表示头结点，只有用 CAS 方式才能更新
        // 因为每一次操作在有多线程并发的时候，可能会处理同一个/不同的节点，
        // 随时可能发生不同步的情况，此时需要重新定位到栈顶（或栈底），
        // 而 head 就是每一次失败操作重新开始的地方（正是由于 head 的修改
        // 是原子操作，所以 head 可以作为并发条件下定位此队列的临界变量。）
        boolean casHead(SNode h, SNode nh) {
            return h == head &&
                    UNSAFE.compareAndSwapObject(this, headOffset, h, nh);
        }

        /**
         * 构造函数
         * 如果 s 为 null 就构造一个新的节点
         * s 不为 null，重新设置 s 的 mode 和 next，返回原来的 s
         */
        static SNode snode(SNode s, Object e, SNode next, int mode) {
            if (s == null) s = new SNode(e);
            s.mode = mode;
            s.next = next;
            return s;
        }

        /**
         * 栈内部 put 或者 take 操作的基础。
         */
        @SuppressWarnings("unchecked")
        E transfer(E e, boolean timed, long nanos) {
            /*
             * Basic algorithm is to loop trying one of three actions:
             *
             * 1. If apparently empty or already containing nodes of same
             *    mode, try to push node on stack and wait for a match,
             *    returning it, or null if cancelled.
             *
             * 2. If apparently containing node of complementary mode,
             *    try to push a fulfilling node on to stack, match
             *    with corresponding waiting node, pop both from
             *    stack, and return matched item. The matching or
             *    unlinking might not actually be necessary because of
             *    other threads performing action 3:
             *
             * 3. If top of stack already holds another fulfilling node,
             *    help it out by doing its match and/or pop
             *    operations, and then continue. The code for helping
             *    is essentially the same as for fulfilling, except
             *    that it doesn't return the item.
             */

            SNode s = null; // constructed/reused as needed
            // 如果传入的 e 为 null，说明是请求数据（消费者），e 不为 null，则是
            // 存入数据（生产者）
            int mode = (e == null) ? REQUEST : DATA;

            // 自旋 + CAS
            // 注意，如果没有执行到 return 的地方，会无限循环
            for (;;) {
                SNode h = head;
                // 栈顶没有元素，或者栈顶元素跟当前操作是同一个模式，
                // 都是生产者或者都是消费者。
                if (h == null || h.mode == mode) {  // empty or same-mode
                    // 等待时间已经到期
                    if (timed && nanos <= 0) {      // can't wait
                        // h 不为 null 而且已经是取消状态
                        if (h != null && h.isCancelled())
                            // CAS 将头结点设置成下一个节点，继续循环
                            casHead(h, h.next);     // pop cancelled node
                        // 否则返回 null
                        else
                            return null;
                        // CAS 尝试入栈
                    } else if (casHead(h, s = snode(s, e, h, mode))) {
                        // 入栈成功（模式相同可以入栈）
                        // 调用 awaitFulfill 方法自旋+阻塞当前线程，等待被匹配
                        SNode m = awaitFulfill(s, timed, nanos);
                        // 如果 m 等于 s，说明取消了，把它清除，并返回 null
                        if (m == s) {               // wait was cancelled
                            clean(s);
                            return null;
                        }
                        // 运行到这里说明匹配到元素了，因为从 awaitFulfill 出来要么
                        // 就是取消了，要么就是匹配到了。
                        // 如果头结点不为 null 而且头结点的下一个节点是 s
                        // CAS 尝试将头结点设置成 s.next，即弹出栈顶的两个元素
                        if ((h = head) != null && h.next == s)
                            casHead(h, s.next);     // help s's fulfiller
                        // 根据当前节点的模式判断返回 m 还是 s 中的值
                        return (E) ((mode == REQUEST) ? m.item : s.item);
                    }
                } else if (!isFulfilling(h.mode)) { // try to fulfill
                    // 栈顶有元素而且模式不一样（可匹配）
                    // 判断头结点是否正在匹配中，如果没有，进入到此代码块中
                    // h 已经被取消了，将头结点设置为 h 的下一个节点
                    if (h.isCancelled())            // already cancelled
                        casHead(h, h.next);         // pop and retry
                    // 先让节点进入队列，将其设置为头结点，状态为正在匹配
                    else if (casHead(h, s=snode(s, e, h, FULFILLING|mode))) {
                        for (;;) { // loop until matched or waiters disappear
                            SNode m = s.next;       // m is s's match
                            // 如果 m 为null，说明除了 s 节点外的节点都被其它线程先一步匹配掉了
                            // 就清空栈并跳出内部循环，到外部循环再重新入栈判断
                            if (m == null) {        // all waiters are gone
                                casHead(s, null);   // pop fulfill node
                                s = null;           // use new node next time
                                break;              // restart main loop
                            }
                            SNode mn = m.next;
                            // 如果 m 和 s 匹配成功，就弹出栈顶的两个元素 m 和 s
                            if (m.tryMatch(s)) {
                                casHead(s, mn);     // pop both s and m
                                return (E) ((mode == REQUEST) ? m.item : s.item);
                            } else                  // lost match
                            // 匹配失败说明其他线程已经匹配 m 了，协助清除它
                                s.casNext(m, mn);   // help unlink
                        }
                    }

                } else {                            // help a fulfiller
                    // 头结点和当前操作模式不一样，且头结点正在匹配中
                    // 帮助匹配
                    SNode m = h.next;               // m is h's match
                    // m 已经被其他线程匹配了
                    if (m == null)                  // waiter is gone
                        casHead(h, null);           // pop fulfilling node
                    else {
                        SNode mn = m.next;
                        // 协助匹配，如果 m 和 s 尝试匹配成功，就弹出栈顶的两个元素 m 和 s
                        if (m.tryMatch(h))          // help match
                            // 匹配成功，弹出栈顶的两个元素
                            casHead(h, mn);         // pop both h and m
                        else                        // lost match
                        // 匹配失败，说明 m 已经被其他线程匹配了，协助清除
                            h.casNext(m, mn);       // help unlink
                    }
                }
            }
        }

        /**
         * 自旋/阻塞直到节点 s 被匹配。
         *
         * @param s the waiting node
         * @param timed true if timed wait
         * @param nanos timeout value
         * @return matched node, or s if cancelled
         */
        SNode awaitFulfill(SNode s, boolean timed, long nanos) {
            /*
             * When a node/thread is about to block, it sets its waiter
             * field and then rechecks state at least one more time
             * before actually parking, thus covering race vs
             * fulfiller noticing that waiter is non-null so should be
             * woken.
             *
             * When invoked by nodes that appear at the point of call
             * to be at the head of the stack, calls to park are
             * preceded by spins to avoid blocking when producers and
             * consumers are arriving very close in time.  This can
             * happen enough to bother only on multiprocessors.
             *
             * The order of checks for returning out of main loop
             * reflects fact that interrupts have precedence over
             * normal returns, which have precedence over
             * timeouts. (So, on timeout, one last check for match is
             * done before giving up.) Except that calls from untimed
             * SynchronousQueue.{poll/offer} don't check interrupts
             * and don't wait at all, so are trapped in transfer
             * method rather than calling awaitFulfill.
             */
            // 到期时间
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            // 当前线程
            Thread w = Thread.currentThread();
            // 自旋次数
            int spins = (shouldSpin(s) ?
                    (timed ? maxTimedSpins : maxUntimedSpins) : 0);
            for (;;) {
                // 当前线程被设置中断，尝试清除 s
                if (w.isInterrupted())
                    s.tryCancel();
                SNode m = s.match;
                // 已经匹配到了，返回匹配到的节点
                if (m != null)
                    return m;
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    // 超时了，尝试清除 s
                    if (nanos <= 0L) {
                        s.tryCancel();
                        continue;
                    }
                }
                if (spins > 0)
                    // 还有自旋次数，自旋次数减一，然后进入下一次自旋
                    spins = shouldSpin(s) ? (spins-1) : 0;
                // 到这儿说明自旋次数没有了
                else if (s.waiter == null)
                    // s 中等待的线程为 null，设置为当前线程
                    s.waiter = w; // establish waiter so can park next iter
                else if (!timed)
                    // 不允许超时，直接阻塞
                    LockSupport.park(this);
                else if (nanos > spinForTimeoutThreshold)
                    // 阻塞相应的时间
                    LockSupport.parkNanos(this, nanos);
            }
        }

        /**
         * 如果节点是头结点或者是活跃的匹配者。
         */
        boolean shouldSpin(SNode s) {
            SNode h = head;
            return (h == s || h == null || isFulfilling(h.mode));
        }

        /**
         * 将 s 从栈中删除。
         */
        void clean(SNode s) {
            s.item = null;   // forget item
            s.waiter = null; // forget thread

            /*
             * At worst we may need to traverse entire stack to unlink
             * s. If there are multiple concurrent calls to clean, we
             * might not see s if another thread has already removed
             * it. But we can stop when we see any node known to
             * follow s. We use s.next unless it too is cancelled, in
             * which case we try the node one past. We don't check any
             * further because we don't want to doubly traverse just to
             * find sentinel.
             */

            // 后面的两步操作都最多遍历到 past
            SNode past = s.next;
            if (past != null && past.isCancelled())
                past = past.next;

            // 找到第一个有效的 head
            SNode p;
            while ((p = head) != null && p != past && p.isCancelled())
                casHead(p, p.next);

            // 清除 past 之前被取消的节点
            while (p != null && p != past) {
                SNode n = p.next;
                if (n != null && n.isCancelled())
                    p.casNext(n, n.next);
                else
                    p = n;
            }
        }

        // Unsafe mechanics
        private static final sun.misc.Unsafe UNSAFE;
        private static final long headOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TransferStack.class;
                headOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("head"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /** 双重队列 */
    static final class TransferQueue<E> extends Transferer<E> {
        /*
         * This extends Scherer-Scott dual queue algorithm, differing,
         * among other ways, by using modes within nodes rather than
         * marked pointers. The algorithm is a little simpler than
         * that for stacks because fulfillers do not need explicit
         * nodes, and matching is done by CAS'ing QNode.item field
         * from non-null to null (for put) or vice versa (for take).
         */

        /** Node class for TransferQueue. */
        static final class QNode {
            volatile QNode next;          // next node in queue
            volatile Object item;         // CAS'ed to or from null
            volatile Thread waiter;       // to control park/unpark
            // 节点类型，是数据节点还是请求节点
            final boolean isData;

            QNode(Object item, boolean isData) {
                this.item = item;
                this.isData = isData;
            }

            boolean casNext(QNode cmp, QNode val) {
                return next == cmp &&
                        UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
            }

            boolean casItem(Object cmp, Object val) {
                return item == cmp &&
                        UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
            }

            /**
             * item 指向自己，表示已删除
             */
            void tryCancel(Object cmp) {
                UNSAFE.compareAndSwapObject(this, itemOffset, cmp, this);
            }

            // 节点是否已经失效
            boolean isCancelled() {
                return item == this;
            }

            /**
             * 如果节点已经离开了队列。
             */
            boolean isOffList() {
                return next == this;
            }

            // Unsafe mechanics
            private static final sun.misc.Unsafe UNSAFE;
            private static final long itemOffset;
            private static final long nextOffset;

            static {
                try {
                    UNSAFE = sun.misc.Unsafe.getUnsafe();
                    Class<?> k = QNode.class;
                    itemOffset = UNSAFE.objectFieldOffset
                            (k.getDeclaredField("item"));
                    nextOffset = UNSAFE.objectFieldOffset
                            (k.getDeclaredField("next"));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }

        /** 队列头 */
        transient volatile QNode head;
        /** 队列尾 */
        transient volatile QNode tail;
        /**
         * Reference to a cancelled node that might not yet have been
         * unlinked from queue because it was the last inserted node
         * when it was cancelled.
         */
        transient volatile QNode cleanMe;

        TransferQueue() {
            QNode h = new QNode(null, false); // initialize to dummy node.
            head = h;
            tail = h;
        }

        /**
         * 将 nh 设置成新的头结点，将原来的节点删除（自链接）。
         */
        void advanceHead(QNode h, QNode nh) {
            if (h == head &&
                    UNSAFE.compareAndSwapObject(this, headOffset, h, nh))
                h.next = h; // forget old next
        }

        /**
         * 设置尾结点。
         */
        void advanceTail(QNode t, QNode nt) {
            if (tail == t)
                UNSAFE.compareAndSwapObject(this, tailOffset, t, nt);
        }

        /**
         * 设置 cleanMe
         */
        boolean casCleanMe(QNode cmp, QNode val) {
            return cleanMe == cmp &&
                    UNSAFE.compareAndSwapObject(this, cleanMeOffset, cmp, val);
        }

        /**
         * put 或者 take 的核心操作
         */
        @SuppressWarnings("unchecked")
        E transfer(E e, boolean timed, long nanos) {
            /* Basic algorithm is to loop trying to take either of
             * two actions:
             *
             * 1. If queue apparently empty or holding same-mode nodes,
             *    try to add node to queue of waiters, wait to be
             *    fulfilled (or cancelled) and return matching item.
             *
             * 2. If queue apparently contains waiting items, and this
             *    call is of complementary mode, try to fulfill by CAS'ing
             *    item field of waiting node and dequeuing it, and then
             *    returning matching item.
             *
             * In each case, along the way, check for and try to help
             * advance head and tail on behalf of other stalled/slow
             * threads.
             *
             * The loop starts off with a null check guarding against
             * seeing uninitialized head or tail values. This never
             * happens in current SynchronousQueue, but could if
             * callers held non-volatile/final ref to the
             * transferer. The check is here anyway because it places
             * null checks at top of loop, which is usually faster
             * than having them implicitly interspersed.
             */

            QNode s = null; // constructed/reused as needed
            boolean isData = (e != null);

            // 自旋
            for (;;) {
                QNode t = tail;
                QNode h = head;
                // 未初始化
                if (t == null || h == null)         // saw uninitialized value
                    continue;                       // spin

                // 队列为空或者模式相同
                if (h == t || t.isData == isData) {
                    QNode tn = t.next;
                    // 重新读取 tail
                    if (t != tail)
                        continue;
                    // 还没有到达尾结点，tail 向后移动（tail 节点需要 CAS）
                    if (tn != null) {
                        advanceTail(t, tn);
                        continue;
                    }
                    if (timed && nanos <= 0)        // can't wait
                        return null;
                    // 创造新节点
                    if (s == null)
                        s = new QNode(e, isData);
                    // 添加节点
                    if (!t.casNext(null, s))
                        continue;

                    // 将尾结点设置成新添加的节点
                    advanceTail(t, s);
                    // 自旋/等待
                    Object x = awaitFulfill(s, e, timed, nanos);
                    // 等待结束，删除节点
                    if (x == s) {
                        clean(t, s);
                        return null;
                    }

                    // s 没有离开队列
                    if (!s.isOffList()) {
                        advanceHead(t, s);
                        if (x != null)
                            s.item = s;
                        s.waiter = null;
                    }
                    return (x != null) ? (E)x : e;
                } else {
                    // 模式不同，进入此程序段
                    // 从 head 节点开始查找
                    QNode m = h.next;               // node to fulfill
                    // 重新获取 head/tail
                    if (t != tail || m == null || h != head)
                        continue;                   // inconsistent read

                    Object x = m.item;
                    // 如果模式相同（其他线程已经进行了处理）
                    // 或者节点被取消
                    // 或者匹配失败（CAS 失败，说明有其他线程已经处理了）
                    if (isData == (x != null) ||
                            x == m ||
                            !m.casItem(x, e)) {
                        // 继续往后查找，head 往后移动
                        advanceHead(h, m);
                        continue;
                    }

                    // 匹配成功，head 出列
                    advanceHead(h, m);
                    // 唤醒匹配成功节点里的线程
                    LockSupport.unpark(m.waiter);
                    return (x != null) ? (E)x : e;
                }
            }
        }

        /**
         * 自旋/阻塞直到节点被填满
         *
         * @param s the waiting node
         * @param e the comparison value for checking match
         * @param timed true if timed wait
         * @param nanos timeout value
         * @return matched item, or s if cancelled
         */
        Object awaitFulfill(QNode s, E e, boolean timed, long nanos) {
            /* Same idea as TransferStack.awaitFulfill */
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            Thread w = Thread.currentThread();
            int spins = ((head.next == s) ?
                    (timed ? maxTimedSpins : maxUntimedSpins) : 0);
            for (;;) {
                if (w.isInterrupted())
                    s.tryCancel(e);
                Object x = s.item;
                if (x != e)
                    return x;
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) {
                        s.tryCancel(e);
                        continue;
                    }
                }
                if (spins > 0)
                    --spins;
                else if (s.waiter == null)
                    s.waiter = w;
                else if (!timed)
                    LockSupport.park(this);
                else if (nanos > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanos);
            }
        }

        /**
         * 清除节点 s
         */
        void clean(QNode pred, QNode s) {
            s.waiter = null; // forget thread
            /*
             * At any given time, exactly one node on list cannot be
             * deleted -- the last inserted node. To accommodate this,
             * if we cannot delete s, we save its predecessor as
             * "cleanMe", deleting the previously saved version
             * first. At least one of node s or the node previously
             * saved can always be deleted, so this always terminates.
             */
            while (pred.next == s) { // Return early if already unlinked
                QNode h = head;
                QNode hn = h.next;
                // 找到有效的 head 节点
                if (hn != null && hn.isCancelled()) {
                    advanceHead(h, hn);
                    continue;
                }
                QNode t = tail;      // Ensure consistent read for tail
                // 队列为空，直接返回
                if (t == h)
                    return;
                QNode tn = t.next;
                // tail 被修改，重新获取
                if (t != tail)
                    continue;
                // 找到新的 tail 节点
                if (tn != null) {
                    advanceTail(t, tn);
                    continue;
                }
                // 如果要删除的节点不是 tail，直接修改 pred 的 next，然后返回
                if (s != t) {        // If not tail, try to unsplice
                    QNode sn = s.next;
                    if (sn == s || pred.casNext(s, sn))
                        return;
                }
                // s 是尾结点
                // 不能直接删除尾结点，此时可能有其它线程正在执行入队操作，
                // 而入队是放在队尾的，因此直接删除队尾，可能会导致其它线程
                // 入队操作不能正确的加入队列
               // cleanMe 表示需要删除节点的前驱
                QNode dp = cleanMe;
                // 有需要删除的节点
                if (dp != null) {    // Try unlinking previous cancelled node
                    QNode d = dp.next;
                    QNode dn;
                    // 删除 d
                    if (d == null ||               // d is gone or
                            d == dp ||                 // d is off list or
                            !d.isCancelled() ||        // d not cancelled or
                            (d != t &&                 // d not tail and
                                    (dn = d.next) != null &&  //   has successor
                                    dn != d &&                //   that is on list
                                    dp.casNext(d, dn)))       // d unspliced
                        // 将 cleanMe 设置为 null
                        casCleanMe(dp, null);
                    if (dp == pred)
                        return;      // s is already saved node
                    // 将 cleanMe 设置为 pred
                } else if (casCleanMe(null, pred))
                    return;          // Postpone cleaning s
            }
        }

        private static final sun.misc.Unsafe UNSAFE;
        private static final long headOffset;
        private static final long tailOffset;
        private static final long cleanMeOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TransferQueue.class;
                headOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("head"));
                tailOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("tail"));
                cleanMeOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("cleanMe"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /**
     * 只能在构造函数中设置。
     */
    private transient volatile Transferer<E> transferer;

    /**
     * 创建非公平模式的 SynchronousQueue。
     */
    public SynchronousQueue() {
        this(false);
    }

    /**
     * 创建给定公平策略的 SynchronousQueue。
     *
     * @param fair if true, waiting threads contend in FIFO order for
     *        access; otherwise the order is unspecified.
     */
    public SynchronousQueue(boolean fair) {
        transferer = fair ? new TransferQueue<E>() : new TransferStack<E>();
    }

    /**
     * 将指定元素放入队列中，如果有必要，等待直到另一个线程获取到该元素。
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        if (transferer.transfer(e, false, 0) == null) {
            Thread.interrupted();
            throw new InterruptedException();
        }
    }

    /**
     * 将指定元素放入队列中，如果有必要，等待指定时间直到另一个线程获取
     * 到该元素。
     *
     * @return {@code true} if successful, or {@code false} if the
     *         specified waiting time elapses before a consumer appears
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean offer(E e, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (e == null) throw new NullPointerException();
        if (transferer.transfer(e, true, unit.toNanos(timeout)) != null)
            return true;
        if (!Thread.interrupted())
            return false;
        throw new InterruptedException();
    }

    /**
     * 将指定元素插入到队列中，如果有另一个线程正在等待它的话。
     *
     * @param e the element to add
     * @return {@code true} if the element was added to this queue, else
     *         {@code false}
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        return transferer.transfer(e, true, 0) != null;
    }

    /**
     * 获取并删除队列的头元素，如果有必要的话，等待另一个线程插入。
     *
     * @return the head of this queue
     * @throws InterruptedException {@inheritDoc}
     */
    public E take() throws InterruptedException {
        E e = transferer.transfer(null, false, 0);
        if (e != null)
            return e;
        Thread.interrupted();
        throw new InterruptedException();
    }

    /**
     * 获取并删除队列的头元素，如果有必要的话，等待指定时间内另一个线程插入。
     *
     * @return the head of this queue, or {@code null} if the
     *         specified waiting time elapses before an element is present
     * @throws InterruptedException {@inheritDoc}
     */
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = transferer.transfer(null, true, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted())
            return e;
        throw new InterruptedException();
    }

    /**
     * 获取并删除队列头部元素，如果另一个线程已经插入了元素的话。
     *
     * @return the head of this queue, or {@code null} if no
     *         element is available
     */
    public E poll() {
        return transferer.transfer(null, true, 0);
    }

    /**
     * 返回 true。
     *
     * @return {@code true}
     */
    public boolean isEmpty() {
        return true;
    }

    /**
     * 返回 0。
     *
     * @return zero
     */
    public int size() {
        return 0;
    }

    /**
     * 返回 0。
     *
     * @return zero
     */
    public int remainingCapacity() {
        return 0;
    }

    /**
     * do nothing
     */
    public void clear() {
    }

    /**
     * 总是返回 false。
     *
     * @param o the element
     * @return {@code false}
     */
    public boolean contains(Object o) {
        return false;
    }

    /**
     * 返回 false。
     *
     * @param o the element to remove
     * @return {@code false}
     */
    public boolean remove(Object o) {
        return false;
    }

    /**
     * 返回 false。
     *
     * @param c the collection
     * @return {@code false} unless given collection is empty
     */
    public boolean containsAll(Collection<?> c) {
        return c.isEmpty();
    }

    /**
     * 返回 false。
     *
     * @param c the collection
     * @return {@code false}
     */
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    /**
     * 返回 false。
     *
     * @param c the collection
     * @return {@code false}
     */
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    /**
     * 返回 null。
     *
     * @return {@code null}
     */
    public E peek() {
        return null;
    }

    /**
     * 返回空数组。
     *
     * @return a zero-length array
     */
    public Object[] toArray() {
        return new Object[0];
    }

    /**
     * 返回空数组。
     *
     * @param a the array
     * @return the specified array
     * @throws NullPointerException if the specified array is null
     */
    public <T> T[] toArray(T[] a) {
        if (a.length > 0)
            a[0] = null;
        return a;
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        int n = 0;
        for (E e; (e = poll()) != null;) {
            c.add(e);
            ++n;
        }
        return n;
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        int n = 0;
        for (E e; n < maxElements && (e = poll()) != null;) {
            c.add(e);
            ++n;
        }
        return n;
    }

}

