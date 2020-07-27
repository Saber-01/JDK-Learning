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
 * Written by Doug Lea and Martin Buchholz with assistance from members of
 * JCP JSR-166 Expert Group and released to the public domain, as explained
 * at http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * 一个基于节点的无界的线程安全双向队列（Deque）。插入，删除和访问操作
 * 在并发环境下线程安全。
 * 和大多数并发集合的实现一样，此类不允许 null 元素。
 *
 * 与大多数集合不同，size 方法不是常量时间的操作。由于这些队列的异步性，确定
 * 当前元素的数量需要遍历，所以如果在遍历期间修改此集合，可能会返回不准确的
 * 结果。此外，批量操作函数 addAll, removeAll, retainAll, containsAll, equals,
 * 和 toArray 不能保证原子性。例如，与 addAll 操作并发执行的迭代器只能查看
 * 添加的元素中的某一部分。
 *
 * 此类和它的迭代器实现了 Deque 和 Iterator 接口的所有可选操作。
 *
 * 此类是 Java Collections Framework 的成员。
 *
 * @since 1.7
 * @author Doug Lea
 * @author Martin Buchholz
 * @param <E> the type of elements held in this collection
 */
public class ConcurrentLinkedDeque<E>
        extends AbstractCollection<E>
        implements Deque<E>, java.io.Serializable {

    /*
     * This is an implementation of a concurrent lock-free deque
     * supporting interior removes but not interior insertions, as
     * required to support the entire Deque interface.
     *
     * We extend the techniques developed for ConcurrentLinkedQueue and
     * LinkedTransferQueue (see the internal docs for those classes).
     * Understanding the ConcurrentLinkedQueue implementation is a
     * prerequisite for understanding the implementation of this class.
     *
     * The data structure is a symmetrical doubly-linked "GC-robust"
     * linked list of nodes.  We minimize the number of volatile writes
     * using two techniques: advancing multiple hops with a single CAS
     * and mixing volatile and non-volatile writes of the same memory
     * locations.
     *
     * A node contains the expected E ("item") and links to predecessor
     * ("prev") and successor ("next") nodes:
     *
     * class Node<E> { volatile Node<E> prev, next; volatile E item; }
     *
     * A node p is considered "live" if it contains a non-null item
     * (p.item != null).  When an item is CASed to null, the item is
     * atomically logically deleted from the collection.
     *
     * At any time, there is precisely one "first" node with a null
     * prev reference that terminates any chain of prev references
     * starting at a live node.  Similarly there is precisely one
     * "last" node terminating any chain of next references starting at
     * a live node.  The "first" and "last" nodes may or may not be live.
     * The "first" and "last" nodes are always mutually reachable.
     *
     * A new element is added atomically by CASing the null prev or
     * next reference in the first or last node to a fresh node
     * containing the element.  The element's node atomically becomes
     * "live" at that point.
     *
     * A node is considered "active" if it is a live node, or the
     * first or last node.  Active nodes cannot be unlinked.
     *
     * A "self-link" is a next or prev reference that is the same node:
     *   p.prev == p  or  p.next == p
     * Self-links are used in the node unlinking process.  Active nodes
     * never have self-links.
     *
     * A node p is active if and only if:
     *
     * p.item != null ||
     * (p.prev == null && p.next != p) ||
     * (p.next == null && p.prev != p)
     *
     * The deque object has two node references, "head" and "tail".
     * The head and tail are only approximations to the first and last
     * nodes of the deque.  The first node can always be found by
     * following prev pointers from head; likewise for tail.  However,
     * it is permissible for head and tail to be referring to deleted
     * nodes that have been unlinked and so may not be reachable from
     * any live node.
     *
     * There are 3 stages of node deletion;
     * "logical deletion", "unlinking", and "gc-unlinking".
     *
     * 1. "logical deletion" by CASing item to null atomically removes
     * the element from the collection, and makes the containing node
     * eligible for unlinking.
     *
     * 2. "unlinking" makes a deleted node unreachable from active
     * nodes, and thus eventually reclaimable by GC.  Unlinked nodes
     * may remain reachable indefinitely from an iterator.
     *
     * Physical node unlinking is merely an optimization (albeit a
     * critical one), and so can be performed at our convenience.  At
     * any time, the set of live nodes maintained by prev and next
     * links are identical, that is, the live nodes found via next
     * links from the first node is equal to the elements found via
     * prev links from the last node.  However, this is not true for
     * nodes that have already been logically deleted - such nodes may
     * be reachable in one direction only.
     *
     * 3. "gc-unlinking" takes unlinking further by making active
     * nodes unreachable from deleted nodes, making it easier for the
     * GC to reclaim future deleted nodes.  This step makes the data
     * structure "gc-robust", as first described in detail by Boehm
     * (http://portal.acm.org/citation.cfm?doid=503272.503282).
     *
     * GC-unlinked nodes may remain reachable indefinitely from an
     * iterator, but unlike unlinked nodes, are never reachable from
     * head or tail.
     *
     * Making the data structure GC-robust will eliminate the risk of
     * unbounded memory retention with conservative GCs and is likely
     * to improve performance with generational GCs.
     *
     * When a node is dequeued at either end, e.g. via poll(), we would
     * like to break any references from the node to active nodes.  We
     * develop further the use of self-links that was very effective in
     * other concurrent collection classes.  The idea is to replace
     * prev and next pointers with special values that are interpreted
     * to mean off-the-list-at-one-end.  These are approximations, but
     * good enough to preserve the properties we want in our
     * traversals, e.g. we guarantee that a traversal will never visit
     * the same element twice, but we don't guarantee whether a
     * traversal that runs out of elements will be able to see more
     * elements later after enqueues at that end.  Doing gc-unlinking
     * safely is particularly tricky, since any node can be in use
     * indefinitely (for example by an iterator).  We must ensure that
     * the nodes pointed at by head/tail never get gc-unlinked, since
     * head/tail are needed to get "back on track" by other nodes that
     * are gc-unlinked.  gc-unlinking accounts for much of the
     * implementation complexity.
     *
     * Since neither unlinking nor gc-unlinking are necessary for
     * correctness, there are many implementation choices regarding
     * frequency (eagerness) of these operations.  Since volatile
     * reads are likely to be much cheaper than CASes, saving CASes by
     * unlinking multiple adjacent nodes at a time may be a win.
     * gc-unlinking can be performed rarely and still be effective,
     * since it is most important that long chains of deleted nodes
     * are occasionally broken.
     *
     * The actual representation we use is that p.next == p means to
     * goto the first node (which in turn is reached by following prev
     * pointers from head), and p.next == null && p.prev == p means
     * that the iteration is at an end and that p is a (static final)
     * dummy node, NEXT_TERMINATOR, and not the last active node.
     * Finishing the iteration when encountering such a TERMINATOR is
     * good enough for read-only traversals, so such traversals can use
     * p.next == null as the termination condition.  When we need to
     * find the last (active) node, for enqueueing a new node, we need
     * to check whether we have reached a TERMINATOR node; if so,
     * restart traversal from tail.
     *
     * The implementation is completely directionally symmetrical,
     * except that most public methods that iterate through the list
     * follow next pointers ("forward" direction).
     *
     * We believe (without full proof) that all single-element deque
     * operations (e.g., addFirst, peekLast, pollLast) are linearizable
     * (see Herlihy and Shavit's book).  However, some combinations of
     * operations are known not to be linearizable.  In particular,
     * when an addFirst(A) is racing with pollFirst() removing B, it is
     * possible for an observer iterating over the elements to observe
     * A B C and subsequently observe A C, even though no interior
     * removes are ever performed.  Nevertheless, iterators behave
     * reasonably, providing the "weakly consistent" guarantees.
     *
     * Empirically, microbenchmarks suggest that this class adds about
     * 40% overhead relative to ConcurrentLinkedQueue, which feels as
     * good as we can hope for.
     */

    private static final long serialVersionUID = 876323262645176354L;

    /**
     * head 节点必须是从列表中第一个节点可以在 O(1) 时间内访问到的节点。
     * 不变性：
     * - 第一个节点总是可以从 head 通过 head.prev 在 O(1) 时间内到达
     * - 所有有效的节点都可以从第一个节点通过 succ() 到达
     * - head != null
     * - head 的 next 不能指向自身
     * - head 不会是 gc-unlinked 节点（但是可能是 unlinked 节点）
     * 可变性:
     * - head.item 可能为 null
     * - head 可能从第一个或者最后一个节点或者 tail 节点不可达
     */
    private transient volatile Node<E> head;

    /**
     * tail 节点必须是从列表中第一个节点可以在 O(1) 时间内访问到的节点。
     * 不变性：
     * - 最后一个节点总是可以从 tail 通过 head.next 在 O(1) 时间内到达
     * - 所有有效的节点都可以从最后一个节点通过 pred() 到达
     * - tail != null
     * - tail 不会是 gc-unlinked 节点（但是可能是 unlinked 节点）
     * 可变性：
     * - tail.item 可能为 null
     * - tail 可能从第一个或者最后一个节点或者 head 节点不可达
     */
    private transient volatile Node<E> tail;

    private static final Node<Object> PREV_TERMINATOR, NEXT_TERMINATOR;

    @SuppressWarnings("unchecked")
    Node<E> prevTerminator() {
        return (Node<E>) PREV_TERMINATOR;
    }

    @SuppressWarnings("unchecked")
    Node<E> nextTerminator() {
        return (Node<E>) NEXT_TERMINATOR;
    }

    static final class Node<E> {
        volatile Node<E> prev;
        volatile E item;
        volatile Node<E> next;

        Node() {  // default constructor for NEXT_TERMINATOR, PREV_TERMINATOR
        }

        /**
         * 构造函数
         */
        Node(E item) {
            UNSAFE.putObject(this, itemOffset, item);
        }

        // CAS 更新 item
        boolean casItem(E cmp, E val) {
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        // 设置 next 属性
        void lazySetNext(Node<E> val) {
            UNSAFE.putOrderedObject(this, nextOffset, val);
        }

        // CAS 设置 next 属性
        boolean casNext(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        // 设置 prev 属性
        void lazySetPrev(Node<E> val) {
            UNSAFE.putOrderedObject(this, prevOffset, val);
        }

        // CAS 设置 prev 属性
        boolean casPrev(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, prevOffset, cmp, val);
        }

        // Unsafe mechanics

        private static final sun.misc.Unsafe UNSAFE;
        private static final long prevOffset;
        private static final long itemOffset;
        private static final long nextOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Node.class;
                prevOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("prev"));
                itemOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /**
     * 将元素 e 包装成一个节点，添加到队列头部
     */
    private void linkFirst(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<E>(e);

        restartFromHead:
        for (;;)
            // 从 head 开始往前寻找第一个节点（并不一定是有效节点）
            for (Node<E> h = head, p = h, q;;) {
                // 如果 p.prev 等于 null，跳过此 if 块，p 可能是自链接节点或第一个节点
                // 如果 p.prev 不等于 null，把 p，q 都向前移动一位
                // 移动过后，如果 p.prev 等于 null，p 有可能是第一个节点，跳过此 if 块
                // 如果 p.prev 不等于 null，p 肯定不是第一个节点，进入此 if 块，继续往前扫描
                if ((q = p.prev) != null &&
                        (q = (p = q).prev) != null)
                    // 如果 head 被修改，返回 head 重新开始查找
                    p = (h != (h = head)) ? h : q;
                // 排除上一步中的自链节点如果是自链接节点，重新从 head 开始扫描
                else if (p.next == p) // PREV_TERMINATOR
                    continue restartFromHead;
                else {
                    // p 是第一个节点，将新创建节点的 next 设置为 p
                    newNode.lazySetNext(p);
                    // CAS将p的prev指向新节点。这一步可能失败，失败说明第一个节点已经更新，那么就自旋重新判断p.prev。
                    if (p.casPrev(null, newNode)) {
                        // CAS 将 head 设置为 p，允许 CAS 失败
                        //如果p至少更新过一次，那么p就有可能不等h。但是如果是第一次循环就到这个判断，
                        // 即一开始就是找到了第一个节点，那就不会更新头节点，也是延迟更新HOPS的策略。
                        if (p != h)
                            casHead(h, newNode);
                        return;
                    }
                }
            }
    }

    /**
     * 元素 e 包装成一个节点，添加到队列头部
     */
    //这个方法和linkFirst是一个原理。只是prev和next的区别，
    private void linkLast(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<E>(e);

        restartFromTail:
        for (;;)
            // 从队列尾部开始往后扫描
            for (Node<E> t = tail, p = t, q;;) {
                // 不是最后一个节点，继续往后
                if ((q = p.next) != null &&
                        (q = (p = q).next) != null)
                    p = (t != (t = tail)) ? t : q;
                    // 自链接节点，从新的 tail 开始重新扫描
                else if (p.prev == p) // NEXT_TERMINATOR
                    continue restartFromTail;
                    // p 是最后一个节点
                else {
                    newNode.lazySetPrev(p);
                    if (p.casNext(null, newNode)) {
                        if (p != t)
                            casTail(t, newNode);
                        return;
                    }
                }
            }
    }

    private static final int HOPS = 2;

    /**
     * 删除节点 x
     * 此方法会删除
     */
    void unlink(Node<E> x) {

        final Node<E> prev = x.prev;
        final Node<E> next = x.next;
        // 节点为第一个节点
        if (prev == null) {
            unlinkFirst(x, next);
            // 节点为最后一个节点
        } else if (next == null) {
            unlinkLast(x, prev);
        } else {
            Node<E> activePred, activeSucc;
            boolean isFirst, isLast;
            int hops = 1;

            // 从 prev 向前扫描，找到 item 不为 null 的前驱节点（有效节点）
            for (Node<E> p = prev; ; ++hops) {
                // 找到了
                if (p.item != null) {
                    activePred = p;
                    isFirst = false;
                    break;
                }
                Node<E> q = p.prev;
                if (q == null) {
                    // p 是自链接节点
                    if (p.next == p)
                        return;
                    // p 是第一个节点，且p.item=null
                    activePred = p;
                    isFirst = true;
                    break;
                }
                // p 已经是自链接节点了，直接返回
                else if (p == q)
                    return;
                    // 继续往前
                else
                    p = q;
            }

            // 从 next 向后扫描，找到 item 不为 null 的后继节点
            for (Node<E> p = next; ; ++hops) {
                // 找到了
                if (p.item != null) {
                    activeSucc = p;
                    isLast = false;
                    break;
                }
                Node<E> q = p.next;
                if (q == null) {
                    // p 是自链接节点
                    if (p.prev == p)
                        return;
                    // p 是最后一个节点
                    activeSucc = p;
                    isLast = true;
                    break;
                }
                // 自链接节点
                else if (p == q)
                    return;
                    // 继续往后
                else
                    p = q;
            }

            // 无节点跳跃并且操作的节点有 item为null的first 或 last 时，不更新链表
            if (hops < HOPS
                    // always squeeze out interior deleted nodes
                    && (isFirst | isLast))
                return;

            // 删除 activePred 之后的连续无效节点
            skipDeletedSuccessors(activePred);
            // 删除 activeSucc 之前的连续无效节点
            skipDeletedPredecessors(activeSucc);
            // 完成删除操作后，activePred会和activeSucc连接到一起，（除非第一个节点到 x
            // 而且/或者 x 到最后一个节点之间为无效节点）

            // 第一个或最后一个节点是无效节点，
            if ((isFirst | isLast) &&

                    // 再次检查是否连接上，且是否满足条件（已经删除 x 及前后无效节点）
                    (activePred.next == activeSucc) &&
                    (activeSucc.prev == activePred) &&
                    (isFirst ? activePred.prev == null : activePred.item != null) &&
                    (isLast  ? activeSucc.next == null : activeSucc.item != null)) {

                // 确保 x 不能从 head 到达
                updateHead();
                // 确保 x 不能从 tail 到达
                updateTail();

                //此时X已经为无效节点，所以需要让它变为自链接节点，提醒其他线程，该节点已经被移除队列了。
                // 设置 x 的 prev 指向自己
                x.lazySetPrev(isFirst ? prevTerminator() : x);
                // 设置 x 的 next 指向自己
                x.lazySetNext(isLast  ? nextTerminator() : x);
            }
        }
    }

    /**
     * 删除队列头部的无效节点（item == null）
     */
    //删除first之后的无效节点，直到遇到一个有效节点。
    private void unlinkFirst(Node<E> first, Node<E> next) {
        // 从 next 开始往后寻找有效节点
        for (Node<E> o = null, p = next, q;;) {
            // p 是有效节点，或者p是最后一个节点
            if (p.item != null || (q = p.next) == null) {//只有p.item!=null时，q=p.next才会赋值并判断。
                //o保存了上次自旋的p节点，如果p不是自链接节点，就将first的next赋值为p。此时o已经不能通过first遍历到了。
                if (o != null && p.prev != p && first.casNext(next, p)) {
                    // 跳过 p 之前的连续无效节点，即p.prev一定是个有效节点，或为null
                    skipDeletedPredecessors(p);
                    // 如果满足以下三个条件：
                    // 1. 检查现在的 first 的前一个节点是否为 null（即first仍然 是第一个节点）
                    // 2. p 可以是最后一个节点；如果不是，必须是有效节点
                    // 3. p 的 prev 是 first 节点（p 是 first 的后一个节点）
                    if (first.prev == null &&
                            (p.next == null || p.item != null) &&  //p是最后一个及item，或者p.item!=null,
                            p.prev == first) {

                        // 确保 o 不能从 head 到达
                        updateHead();
                        // 确保 o 不能从 tail 到达
                        updateTail();

                        // 设置 o 的 next 指向自身
                        o.lazySetNext(o);
                        // 设置 o 的 prev 指向 PREV_TERMINATOR
                        o.lazySetPrev(prevTerminator());
                    }
                }
                return;
            }
            //到这里p.item==null,且p.next!=null,说明p为无效节点，且p不是最后一个节点。
            // 如果p是自链接节点，该方法也直接返回
            else if (p == q)
                return;
            // 如果，p是无效节点，且p不是最后一个节点，而且也不是自链接节点。
            else {
                o = p;   //o赋值为循环的上一个节点。这个o的item肯定为null。
                p = q;    //p赋值p的下一个节点q,向右遍历。
            }
        }
    }

    /**
     * 删除队列尾部的无效节点，与 unlinkFirst 基本一样
     */
    private void unlinkLast(Node<E> last, Node<E> prev) {
        for (Node<E> o = null, p = prev, q;;) {
            if (p.item != null || (q = p.prev) == null) {
                if (o != null && p.next != p && last.casPrev(prev, p)) {
                    skipDeletedSuccessors(p);
                    if (last.next == null &&
                            (p.prev == null || p.item != null) &&
                            p.next == last) {

                        updateHead(); // Ensure o is not reachable from head
                        updateTail(); // Ensure o is not reachable from tail

                        // Finally, actually gc-unlink
                        o.lazySetPrev(o);
                        o.lazySetNext(nextTerminator());
                    }
                }
                return;
            }
            else if (p == q)
                return;
            else {
                o = p;
                p = q;
            }
        }
    }

    /**
     * 确保在调用此方法之前未链接的节点在返回后无法从 head 访问。不保证消除
     * 松弛，此方法运行期间，只有 head 会指向处于活动状态的节点。
     */
    private final void updateHead() {
        Node<E> h, p, q;
        restartFromHead:
        // h 的 item 为 null 而且 h 不是第一个节点时，只有当前head为无效节点，
        // 并且不是真正的头节点时才会进入循环。
        // 设置p为h.prev,从 head 往 prev 方向向前遍历。
        while ((h = head).item == null && (p = h.prev) != null) {
            for (;;) {
                // 如果 p 的前一个节点为 null，进入
                // 如果 p 往前移动一个之后，其前一个节点为 null，也进入
                if ((q = p.prev) == null ||
                        (q = (p = q).prev) == null) { //进入此方法的p的prev都是null。
                    // 将 head 设置为 p，然后返回
                    if (casHead(h, p))   //成功设置头结点直接返回。
                        return;
                    //如果CAS失败，说明头节点被更新，那么重新从外层while开始更新head信息后再循环
                    else
                        continue restartFromHead;
                }
                // head 被改变了，从外层重新获取 head 然后循环
                else if (h != head)
                    continue restartFromHead;
                //如果，p的前一节点不为null，并且p的前一节点的前一节点还是不为null，而且head也没有改变，
                    // 那么继续往前查找。
                else
                    p = q;
            }
        }
    }

    /**
     * 确保在调用此方法之前未链接的节点在返回后无法从 tail 访问。不保证消除
     * 松弛，此方法运行期间，只有 tail 会指向处于活动状态的节点。
     */
    //和上面的方法原理相同，
    private final void updateTail() {
        // Either tail already points to an active node, or we keep
        // trying to cas it to the last node until it does.
        Node<E> t, p, q;
        restartFromTail:
        while ((t = tail).item == null && (p = t.next) != null) {
            for (;;) {
                if ((q = p.next) == null ||
                        (q = (p = q).next) == null) {
                    // It is possible that p is NEXT_TERMINATOR,
                    // but if so, the CAS is guaranteed to fail.
                    if (casTail(t, p))
                        return;
                    else
                        continue restartFromTail;
                }
                else if (t != tail)
                    continue restartFromTail;
                else
                    p = q;
            }
        }
    }

    // 删除 x 之前的连续无效节点
    private void skipDeletedPredecessors(Node<E> x) {
        whileActive:
        do {
            // 从 x.prev 开始往前查找
            Node<E> prev = x.prev; //prev保存初始x.prev
            Node<E> p = prev; //p用来遍历
            findActive:
            for (;;) {
                // 遇到可用节点，直接退出自旋
                if (p.item != null)
                    break findActive;
                //以下情况 p.item==null.
                Node<E> q = p.prev;  //q赋值为p的前一个节点。
                if (q == null) {  //如果p.prev为null，p是第一个节点，
                    // 刚好该节点变为了自链接节点
                    if (p.next == p)
                        continue whileActive;//重新开始遍历时，下一次就不会到自链接节点了，应为prev会改变。
                    // 不是自链接节点说明已经到第一个节点了
                    break findActive;
                }
                else if (p == q)
                    continue whileActive;
                else
                    p = q;
            }

            // 到这里已经找到可用节点 p，
            // 如果prev没变。即p未曾赋值，直接返回，否则将 x 的 prev 设置为 p
            if (prev == p || x.casPrev(prev, p))
                return;

        } while (x.item != null || x.next == null); //如果这时x已经变成无效，并且x.next不等于null，才会退出循环
    }

    // 删除 x 之后的连续无效节点
    private void skipDeletedSuccessors(Node<E> x) {
        whileActive:
        do {
            // 从 next 开始往后查找
            Node<E> next = x.next;
            Node<E> p = next;
            findActive:
            for (;;) {
                // 找到可用的节点
                if (p.item != null)
                    break findActive;
                Node<E> q = p.next;
                if (q == null) {
                    // 自链接节点
                    if (p.prev == p)
                        continue whileActive;
                    // 不是自链接节点说明已经到最后一个节点了
                    break findActive;
                }
                // 自链接节点
                else if (p == q)
                    continue whileActive;
                    // 继续往后查找
                else
                    p = q;
            }

            // 找到可用的节点 p，将 x 的 next 设置为 p
            if (next == p || x.casNext(next, p))
                return;

        } while (x.item != null || x.prev == null);
    }

    /**
     * 返回 p 的后继节点，如果 p 是自链接节点，返回第一个节点。
     */
    final Node<E> succ(Node<E> p) {
        // TODO: should we skip deleted nodes here?
        Node<E> q = p.next;
        return (p == q) ? first() : q;
    }

    /**
     * 返回 p 的前驱节点，如果 p 是前驱自链接节点，返回最后一个节点。
     */
    final Node<E> pred(Node<E> p) {
        Node<E> q = p.prev;
        return (p == q) ? last() : q;
    }

    /**
     * 返回第一个节点，第一个节点 p 满足：p.prev == null && p.next != p
     * 返回的节点在逻辑上可能已经被删除，即可能item==null，确保 head 指向了返回的节点。
     */
    Node<E> first() {
        restartFromHead:
        for (;;)
            // 从 head 开始往前查找
            for (Node<E> h = head, p = h, q;;) {
                // p 的前一个节点和前前节点都不为 null
                // 如果 head 改变，从新的 head 开始循环，否则往前移两步即可
                if ((q = p.prev) != null &&
                        (q = (p = q).prev) != null)
                    p = (h != (h = head)) ? h : q;
                    // p 的前一个节点为 null 或者 p 的前前节点为 null
                    // p 为 head 返回 head
                    // p 不为 head，将 head 设置为 p
                    // 然后返回 p
                else if (p == h
                        || casHead(h, p))  //p!=h时，说明需要更新head了。
                    return p;
                 // head 改变，重新从 head 开始
                else
                    continue restartFromHead;
            }
    }

    /**
     * 返回最后一个节点，最后一个节点 p 满足：p.next == null && p.prev != p
     * 返回的节点在逻辑上可能已经被删除。确保 tail 指向了返回的节点。
     */
    Node<E> last() {
        restartFromTail:
        for (;;)
            for (Node<E> t = tail, p = t, q;;) {
                if ((q = p.next) != null &&
                        (q = (p = q).next) != null)
                    p = (t != (t = tail)) ? t : q;
                else if (p == t
                        || casTail(t, p))
                    return p;
                else
                    continue restartFromTail;
            }
    }

    // Minor convenience utilities

    /**
     * 如果参数为 null 抛出 NullPointerException 异常
     *
     * @param v the element
     */
    private static void checkNotNull(Object v) {
        if (v == null)
            throw new NullPointerException();
    }

    /**
     * 如果元素为 null 抛出 NoSuchElementException 异常，否则返回该元素
     *
     * @param v the element
     * @return the element
     */
    private E screenNullResult(E v) {
        if (v == null)
            throw new NoSuchElementException();
        return v;
    }

    /**
     * 创造一个顺序表，将此队列中的元素添加到该顺序表中
     * 使用 toArray。
     *
     * @return the array list
     */
    private ArrayList<E> toArrayList() {
        ArrayList<E> list = new ArrayList<E>();
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null)
                list.add(item);
        }
        return list;
    }

    /**
     * 构造一个空的队列。
     */
    public ConcurrentLinkedDeque() {
        head = tail = new Node<E>(null);
    }

    /**
     * 构造一个包含指定集合所有元素的双向队列，元素添加的顺序为集合迭代器
     * 返回的顺序。
     *
     * @param c the collection of elements to initially contain
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    public ConcurrentLinkedDeque(Collection<? extends E> c) {
        Node<E> h = null, t = null;
        for (E e : c) {
            checkNotNull(e);
            Node<E> newNode = new Node<E>(e);
            if (h == null)
                h = t = newNode;
            else {
                t.lazySetNext(newNode);
                newNode.lazySetPrev(t);
                t = newNode;
            }
        }
        initHeadTail(h, t);
    }

    /**
     * 初始化 head 和 tail。
     */
    private void initHeadTail(Node<E> h, Node<E> t) {
        if (h == t) {
            // 还没有初始化
            if (h == null)
                h = t = new Node<E>(null);
                // h == t != null，创造一个 item 为 null 的节点，t 指向该节点
            else {
                // Avoid edge case of a single Node with non-null item.
                Node<E> newNode = new Node<E>(null);
                t.lazySetNext(newNode);
                newNode.lazySetPrev(t);
                t = newNode;
            }
        }
        head = h;
        tail = t;
    }

    /**
     * 将指定元素插入到队列头部。
     * 由于队列是无界队列，不会抛出 IllegalStateException 异常。
     *
     * @throws NullPointerException if the specified element is null
     */
    public void addFirst(E e) {
        linkFirst(e);
    }

    /**
     * 将指定元素插入到队列尾部。
     * 由于队列是无界队列，不会抛出 IllegalStateException 异常。
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @throws NullPointerException if the specified element is null
     */
    public void addLast(E e) {
        linkLast(e);
    }

    /**
     * 将指定元素插入到队列头部。
     * 由于队列是无界队列，不会抛出 IllegalStateException 异常。
     *
     * @return {@code true} (as specified by {@link Deque#offerFirst})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offerFirst(E e) {
        linkFirst(e);
        return true;
    }

    /**
     * 将指定元素插入到队列尾部。
     * 由于队列是无界队列，不会抛出 IllegalStateException 异常。
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @return {@code true} (as specified by {@link Deque#offerLast})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offerLast(E e) {
        linkLast(e);
        return true;
    }

    // 获取第一个元素（有效节点的 item 值）
    public E peekFirst() {
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null)
                return item;
        }
        return null;
    }

    // 获取最后一个元素
    public E peekLast() {
        for (Node<E> p = last(); p != null; p = pred(p)) {
            E item = p.item;
            if (item != null)
                return item;
        }
        return null;
    }

    /**
     * 获取第一个元素
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E getFirst() {
        return screenNullResult(peekFirst());
    }

    /**
     * 获取最后一个元素
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E getLast() {
        return screenNullResult(peekLast());
    }

    // 获取并删除队列的首节点（有效节点）
    public E pollFirst() {
        // 从前往后找到链表中第一个有效节点
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            // 将 p 节点的 item 设置为 null
            if (item != null && p.casItem(item, null)) {
                // 删除 p 节点
                unlink(p);
                return item;
            }
        }
        return null;
    }

    // 获取并删除队列的尾节点（有效节点）
    public E pollLast() {
        // 从后往前找到链表中最后一个有效节点
        for (Node<E> p = last(); p != null; p = pred(p)) {
            E item = p.item;
            if (item != null && p.casItem(item, null)) {
                unlink(p);
                return item;
            }
        }
        return null;
    }

    /**
     * 删除第一个元素
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E removeFirst() {
        return screenNullResult(pollFirst());
    }

    /**
     * 删除最后一个元素
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E removeLast() {
        return screenNullResult(pollLast());
    }

    // *** Queue and stack methods ***

    /**
     * 将指定元素插入到队列尾部。
     *
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        return offerLast(e);
    }

    /**
     * 将指定元素插入到队列尾部。
     *
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        return offerLast(e);
    }

    public E poll()           { return pollFirst(); }
    public E peek()           { return peekFirst(); }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E remove()         { return removeFirst(); }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E pop()            { return removeFirst(); }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E element()        { return getFirst(); }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    public void push(E e)     { addFirst(e); }

    /**
     * 删除第一次出现的该元素。
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     * @throws NullPointerException if the specified element is null
     */
    public boolean removeFirstOccurrence(Object o) {
        checkNotNull(o);
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null && o.equals(item) && p.casItem(item, null)) {
                unlink(p);
                return true;
            }
        }
        return false;
    }

    /**
     * 删除最后一次出现的该元素。
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     * @throws NullPointerException if the specified element is null
     */
    public boolean removeLastOccurrence(Object o) {
        checkNotNull(o);
        for (Node<E> p = last(); p != null; p = pred(p)) {
            E item = p.item;
            if (item != null && o.equals(item) && p.casItem(item, null)) {
                unlink(p);
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否包含指定元素。
     *
     * @param o element whose presence in this deque is to be tested
     * @return {@code true} if this deque contains the specified element
     */
    public boolean contains(Object o) {
        if (o == null) return false;
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null && o.equals(item))
                return true;
        }
        return false;
    }

    /**
     * 判断集合是否为空。
     *
     * @return {@code true} if this collection contains no elements
     */
    public boolean isEmpty() {
        return peekFirst() == null;
    }

    /**
     * 返回队列中元素个数。从第一个节点开始遍历。超过 Integer.MAX_VALUE
     * 返回 Integer.MAX_VALUE。
     *
     * @return the number of elements in this deque
     */
    public int size() {
        int count = 0;
        for (Node<E> p = first(); p != null; p = succ(p))
            if (p.item != null)
                // Collection.size() spec says to max out
                if (++count == Integer.MAX_VALUE)
                    break;
        return count;
    }

    /**
     * 删除（第一次出现的）指定元素。
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     * @throws NullPointerException if the specified element is null
     */
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    /**
     * 将指定集合中的所有元素添加到队列尾部。
     *
     * @param c the elements to be inserted into this deque
     * @return {@code true} if this deque changed as a result of the call
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     * @throws IllegalArgumentException if the collection is this deque
     */
    public boolean addAll(Collection<? extends E> c) {
        if (c == this)
            // As historically specified in AbstractQueue#addAll
            throw new IllegalArgumentException();

        // Copy c into a private chain of Nodes
        Node<E> beginningOfTheEnd = null, last = null;
        for (E e : c) {
            checkNotNull(e);
            Node<E> newNode = new Node<E>(e);
            if (beginningOfTheEnd == null)
                beginningOfTheEnd = last = newNode;
            else {
                last.lazySetNext(newNode);
                newNode.lazySetPrev(last);
                last = newNode;
            }
        }
        if (beginningOfTheEnd == null)
            return false;

        // Atomically append the chain at the tail of this collection
        restartFromTail:
        for (;;)
            for (Node<E> t = tail, p = t, q;;) {
                if ((q = p.next) != null &&
                        (q = (p = q).next) != null)
                    // Check for tail updates every other hop.
                    // If p == q, we are sure to follow tail instead.
                    p = (t != (t = tail)) ? t : q;
                else if (p.prev == p) // NEXT_TERMINATOR
                    continue restartFromTail;
                else {
                    // p is last node
                    beginningOfTheEnd.lazySetPrev(p); // CAS piggyback
                    if (p.casNext(null, beginningOfTheEnd)) {
                        // Successful CAS is the linearization point
                        // for all elements to be added to this deque.
                        if (!casTail(t, last)) {
                            // Try a little harder to update tail,
                            // since we may be adding many elements.
                            t = tail;
                            if (last.next == null)
                                casTail(t, last);
                        }
                        return true;
                    }
                    // Lost CAS race to another thread; re-read next
                }
            }
    }

    /**
     * 删除所有元素。
     */
    public void clear() {
        while (pollFirst() != null)
            ;
    }

    /**
     * 返回数组。
     *
     * @return an array containing all of the elements in this deque
     */
    public Object[] toArray() {
        return toArrayList().toArray();
    }

    /**
     * 返回数组。
     *
     * @param a the array into which the elements of the deque are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this deque
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this deque
     * @throws NullPointerException if the specified array is null
     */
    public <T> T[] toArray(T[] a) {
        return toArrayList().toArray(a);
    }

    /**
     * 返回迭代器。
     *
     * <p>The returned iterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * @return an iterator over the elements in this deque in proper sequence
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * 返回逆序迭代器。从 last 到 first。
     *
     * <p>The returned iterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * @return an iterator over the elements in this deque in reverse order
     */
    public Iterator<E> descendingIterator() {
        return new DescendingItr();
    }

    private abstract class AbstractItr implements Iterator<E> {
        /**
         * 下一个节点
         */
        private Node<E> nextNode;

        /**
         * 下一个 item
         */
        private E nextItem;

        /**
         * 最近访问的节点
         */
        private Node<E> lastRet;

        abstract Node<E> startNode();
        abstract Node<E> nextNode(Node<E> p);

        AbstractItr() {
            advance();
        }

        /**
         * 将 nextNode 和 nextItem 设置成下一个有效的节点或 item，没有则为 null。
         */
        private void advance() {
            lastRet = nextNode;

            Node<E> p = (nextNode == null) ? startNode() : nextNode(nextNode);
            for (;; p = nextNode(p)) {
                if (p == null) {
                    // p might be active end or TERMINATOR node; both are OK
                    nextNode = null;
                    nextItem = null;
                    break;
                }
                E item = p.item;
                if (item != null) {
                    nextNode = p;
                    nextItem = item;
                    break;
                }
            }
        }

        public boolean hasNext() {
            return nextItem != null;
        }

        public E next() {
            E item = nextItem;
            if (item == null) throw new NoSuchElementException();
            advance();
            return item;
        }

        public void remove() {
            Node<E> l = lastRet;
            if (l == null) throw new IllegalStateException();
            l.item = null;
            unlink(l);
            lastRet = null;
        }
    }

    /** Forward iterator */
    private class Itr extends AbstractItr {
        Node<E> startNode() { return first(); }
        Node<E> nextNode(Node<E> p) { return succ(p); }
    }

    /** Descending iterator */
    private class DescendingItr extends AbstractItr {
        Node<E> startNode() { return last(); }
        Node<E> nextNode(Node<E> p) { return pred(p); }
    }

    private boolean casHead(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    private boolean casTail(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    // Unsafe mechanics

    private static final sun.misc.Unsafe UNSAFE;
    private static final long headOffset;
    private static final long tailOffset;
    static {
        PREV_TERMINATOR = new Node<Object>();
        PREV_TERMINATOR.next = PREV_TERMINATOR;
        NEXT_TERMINATOR = new Node<Object>();
        NEXT_TERMINATOR.prev = NEXT_TERMINATOR;
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ConcurrentLinkedDeque.class;
            headOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("tail"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}