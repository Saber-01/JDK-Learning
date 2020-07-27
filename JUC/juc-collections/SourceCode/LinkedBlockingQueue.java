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
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * 基于链接节点的可选边界阻塞队列。
 * 队列中元素顺序为 FIFO。
 * 队列的头部元素是进队列最久的元素，尾部元素是进队列最短的元素。新元素
 * 插入到队列尾部，从队列头部获取元素。链式队列通常比基于数组的队列具有
 * 更高的吞吐量，但是在大多数并发应用程序中，其预期性能较差。
 *
 * 构造函数参数中指定可选的容量，防止过度的队列扩散。未指定时容量等于
 * Integer.MAX_VALUE。在每次插入时动态创建链接节点，除非超出容量。
 *
 * 此类和它的迭代器实现了 Collection 和 Iterator 的所有可选操作。
 *
 * 此类是 Java Collections Framework 的成员。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public class LinkedBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = -6903933977591709194L;

    /*
     * A variant of the "two lock queue" algorithm.  The putLock gates
     * entry to put (and offer), and has an associated condition for
     * waiting puts.  Similarly for the takeLock.  The "count" field
     * that they both rely on is maintained as an atomic to avoid
     * needing to get both locks in most cases. Also, to minimize need
     * for puts to get takeLock and vice-versa, cascading notifies are
     * used. When a put notices that it has enabled at least one take,
     * it signals taker. That taker in turn signals others if more
     * items have been entered since the signal. And symmetrically for
     * takes signalling puts. Operations such as remove(Object) and
     * iterators acquire both locks.
     *
     * Visibility between writers and readers is provided as follows:
     *
     * Whenever an element is enqueued, the putLock is acquired and
     * count updated.  A subsequent reader guarantees visibility to the
     * enqueued Node by either acquiring the putLock (via fullyLock)
     * or by acquiring the takeLock, and then reading n = count.get();
     * this gives visibility to the first n items.
     *
     * To implement weakly consistent iterators, it appears we need to
     * keep all Nodes GC-reachable from a predecessor dequeued Node.
     * That would cause two problems:
     * - allow a rogue Iterator to cause unbounded memory retention
     * - cause cross-generational linking of old Nodes to new Nodes if
     *   a Node was tenured while live, which generational GCs have a
     *   hard time dealing with, causing repeated major collections.
     * However, only non-deleted Nodes need to be reachable from
     * dequeued Nodes, and reachability does not necessarily have to
     * be of the kind understood by the GC.  We use the trick of
     * linking a Node that has just been dequeued to itself.  Such a
     * self-link implicitly means to advance to head.next.
     */

    /**
     * 节点类
     */
    static class Node<E> {
        E item;

        Node<E> next;

        Node(E x) { item = x; }
    }

    /** 队列容量，默认为 Integer.MAX_VALUE */
    private final int capacity;

    /** 当前元素数量 */
    private final AtomicInteger count = new AtomicInteger();

    /**
     * 链表头部
     * Invariant: head.item == null
     */
    transient Node<E> head;

    /**
     * 链表尾部
     * Invariant: last.next == null
     */
    private transient Node<E> last;

    /** take 和 poll 等持有的锁 */
    private final ReentrantLock takeLock = new ReentrantLock();

    /** 等待执行 take 操作的 condition */
    private final Condition notEmpty = takeLock.newCondition();

    /** put 和 offer 等持有的锁 */
    private final ReentrantLock putLock = new ReentrantLock();

    /** 等待执行 put 操作的 condition */
    private final Condition notFull = putLock.newCondition();

    /**
     * 唤醒一个等待 take 的线程。在 put/offer 里调用。
     */
    private void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * 唤醒一个等待 put 的线程。在 take/poll 里调用。
     */
    private void signalNotFull() {
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            notFull.signal();
        } finally {
            putLock.unlock();
        }
    }

    /**
     * 私有方法。
     * 在队列尾部添加指定节点。
     *
     * @param node the node
     */
    private void enqueue(Node<E> node) {
        last = last.next = node;
    }

    /**
     * 私有方法。
     * 从队列头部删除一个节点。
     * 删除的节点为队列第一个元素，返回的元素为队列第二个节点中的元素，
     * 将队列第二个节点的 item 置为 null，然后将 head 向后移一位。
     *
     * @return the node
     */
    private E dequeue() {
        Node<E> h = head;
        Node<E> first = h.next;
        h.next = h;
        head = first;
        E x = first.item;
        first.item = null;
        return x;
    }

    /**
     * 获取类中的两个锁。
     */
    void fullyLock() {
        putLock.lock();
        takeLock.lock();
    }

    /**
     * 释放类中的两个所。
     */
    void fullyUnlock() {
        takeLock.unlock();
        putLock.unlock();
    }

    /**
     * 默认容量的构造函数
     */
    public LinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    /**
     * 指定容量的构造函数
     *
     * @param capacity the capacity of this queue
     * @throws IllegalArgumentException if {@code capacity} is not greater
     *         than zero
     */
    public LinkedBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        last = head = new Node<E>(null);
    }

    /**
     * 构造默认容量的阻塞队列，将指定集合中所有元素加入到队列中，加入的
     * 顺序为集合迭代器返回的顺序。
     *
     * @param c the collection of elements to initially contain
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    public LinkedBlockingQueue(Collection<? extends E> c) {
        this(Integer.MAX_VALUE);
        final ReentrantLock putLock = this.putLock;
        // 加锁
        putLock.lock(); // Never contended, but necessary for visibility
        try {
            int n = 0;
            for (E e : c) {
                // 元素不允许为 null
                if (e == null)
                    throw new NullPointerException();
                // 检查容量
                if (n == capacity)
                    throw new IllegalStateException("Queue full");
                // 插入到队列中
                enqueue(new Node<E>(e));
                ++n;
            }
            // 设置元素个数
            count.set(n);
        } finally {
            putLock.unlock();
        }
    }

    /**
     * 返回队列中元素个数。
     *
     * @return the number of elements in this queue
     */
    public int size() {
        return count.get();
    }

    /**
     * 返回队列剩余容量（还能容纳多少个元素）。
     * 没有加锁，不准确。
     */
    public int remainingCapacity() {
        return capacity - count.get();
    }

    /**
     * 将指定元素添加到队列尾部，如果没有空间了，等待直到有空余的空间为止。
     * 响应中断。
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        int c = -1;
        // 创造一个新节点
        Node<E> node = new Node<E>(e);
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        // 获取响应中断锁 putLock
        putLock.lockInterruptibly();
        try {
            /*
             * 注意就算 count 没有受到锁的保护，仍然用于 wait guard。
             * 这样做事可行的，因为此时计数只可能减少（因为 put 操作
             * 已经被锁定了，只能执行 take 操作），而且如果容量发生变化
             * 当前线程（或者其它等待 put 的线程）会被唤醒。
             */
            while (count.get() == capacity) {
                // 队列已满，在 condition 中等待
                notFull.await();
            }
            // 节点进入队列
            enqueue(node);
            // 计数加一
            c = count.getAndIncrement();
            // 如果此时计数小于容量（可能有其他线程执行了 take 操作，唤醒等待
            // put 的线程
            if (c + 1 < capacity)
                notFull.signal();
        } finally {
            // 释放锁
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();
    }

    /**
     * 将指定元素添加到队列尾部，如果没有空间了，等待直到有空余的空间为止。
     * 响应中断，有等待时间限制。
     *
     * @return {@code true} if successful, or {@code false} if
     *         the specified waiting time elapses before space is available
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean offer(E e, long timeout, TimeUnit unit)
            throws InterruptedException {

        if (e == null) throw new NullPointerException();
        long nanos = unit.toNanos(timeout);
        int c = -1;
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            // 队列已满，在 condition 中等待，如果时间到了返回 false。
            while (count.get() == capacity) {
                if (nanos <= 0)
                    return false;
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(new Node<E>(e));
            c = count.getAndIncrement();
            if (c + 1 < capacity)
                notFull.signal();
        } finally {
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();
        // 添加成功返回 true
        return true;
    }

    /**
     * 将指定元素添加到队列尾部，如果队列还有剩余空间的话。成功返回 true，
     * 队列已满直接返回 false。
     * 使用有容量限制的队列时，此方法比 BlockingQueue 中的 add 方法更好，
     * add 方法在插入失败时只会抛出异常。
     *
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        final AtomicInteger count = this.count;
        // 容量达到限制，返回 false
        if (count.get() == capacity)
            return false;
        int c = -1;
        Node<E> node = new Node<E>(e);
        final ReentrantLock putLock = this.putLock;
        // 加锁
        putLock.lock();
        try {
            // 在此判断，防止加锁之前队列已满
            if (count.get() < capacity) {
                // 添加
                enqueue(node);
                c = count.getAndIncrement();
                // 还有剩余空间，唤醒等待 put 的线程
                if (c + 1 < capacity)
                    notFull.signal();
            }
        } finally {
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();
        return c >= 0;
    }

    // 响应中断的 take 操作
    public E take() throws InterruptedException {
        E x;
        int c = -1;
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            // 此处依然可以使用 count，理由同 put 函数中相应解释。
            // 如果计数等于 0，队列中已经没有元素了，阻塞当前线程
            while (count.get() == 0) {
                notEmpty.await();
            }
            // 出队列
            x = dequeue();
            // 计数减一
            c = count.getAndDecrement();
            // 唤醒其它等待 take 的线程
            if (c > 1)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        // 返回获取到的元素
        return x;
    }

    // 响应中断的 poll 操作，有等待时间限制
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E x = null;
        int c = -1;
        long nanos = unit.toNanos(timeout);
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            // 在队列中等待，超过时间直接返回 null
            while (count.get() == 0) {
                if (nanos <= 0)
                    return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    // 队列为空直接返回 null
    public E poll() {
        final AtomicInteger count = this.count;
        if (count.get() == 0)
            return null;
        E x = null;
        int c = -1;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            // 在此判断队列是否为空
            if (count.get() > 0) {
                x = dequeue();
                // 计数减一
                c = count.getAndDecrement();
                if (c > 1)
                    notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    // 获取头部元素
    public E peek() {
        // 队列为空
        if (count.get() == 0)
            return null;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            Node<E> first = head.next;
            if (first == null)
                return null;
            else
                return first.item;
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * 删除节点 p
     */
    void unlink(Node<E> p, Node<E> trail) {
        p.item = null;
        trail.next = p.next;
        if (last == p)
            last = trail;
        if (count.getAndDecrement() == capacity)
            notFull.signal();
    }

    /**
     * 删除队列中指定元素，如果其存在的话。
     * 删除成功返回 true。
     *
     * @param o element to be removed from this queue, if present
     * @return {@code true} if this queue changed as a result of the call
     */
    public boolean remove(Object o) {
        if (o == null) return false;
        fullyLock();
        try {
            // 扫描队列，搜索指定元素
            for (Node<E> trail = head, p = trail.next;
                 p != null;
                 trail = p, p = p.next) {
                if (o.equals(p.item)) {
                    unlink(p, trail);
                    return true;
                }
            }
            return false;
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 如果队列包含指定元素返回 true。
     *
     * @param o object to be checked for containment in this queue
     * @return {@code true} if this queue contains the specified element
     */
    public boolean contains(Object o) {
        if (o == null) return false;
        // 不允许修改
        fullyLock();
        try {
            for (Node<E> p = head.next; p != null; p = p.next)
                if (o.equals(p.item))
                    return true;
            return false;
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 返回数组。
     *
     * @return an array containing all of the elements in this queue
     */
    public Object[] toArray() {
        // 不允许修改
        fullyLock();
        try {
            int size = count.get();
            Object[] a = new Object[size];
            int k = 0;
            for (Node<E> p = head.next; p != null; p = p.next)
                a[k++] = p.item;
            return a;
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 返回数组。
     *
     * @param a the array into which the elements of the queue are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this queue
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this queue
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        fullyLock();
        try {
            int size = count.get();
            if (a.length < size)
                a = (T[])java.lang.reflect.Array.newInstance
                        (a.getClass().getComponentType(), size);

            int k = 0;
            for (Node<E> p = head.next; p != null; p = p.next)
                a[k++] = (T)p.item;
            if (a.length > k)
                a[k] = null;
            return a;
        } finally {
            fullyUnlock();
        }
    }

    // 返回字符串
    public String toString() {
        fullyLock();
        try {
            Node<E> p = head.next;
            if (p == null)
                return "[]";

            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (;;) {
                E e = p.item;
                sb.append(e == this ? "(this Collection)" : e);
                p = p.next;
                if (p == null)
                    return sb.append(']').toString();
                sb.append(',').append(' ');
            }
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 清除队列所有元素。
     * 执行完之后队列为空
     */
    public void clear() {
        // 全部上锁
        fullyLock();
        try {
            // 从 head 开始
            for (Node<E> p, h = head; (p = h.next) != null; h = p) {
                h.next = h;
                p.item = null;
            }
            head = last;
            // 设置容量为 0
            if (count.getAndSet(0) == capacity)
                notFull.signal();
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 将队列元素移动到指定集合中，删除队列中原来的元素。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    /**
     * 将队列元素移动到指定集合中，指定需要移动的元素个数，并且删除队列中原来的元素。
     *
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
        if (maxElements <= 0)
            return 0;
        boolean signalNotFull = false;
        // take 操作上锁
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            // 指定的 maxElements 和元素个数中较小的那个
            // 其实就是元素个数
            int n = Math.min(maxElements, count.get());
            Node<E> h = head;
            int i = 0;
            try {
                // 从 head 开始遍历
                while (i < n) {
                    Node<E> p = h.next;
                    // 删除原来的元素
                    c.add(p.item);
                    p.item = null;
                    h.next = h;
                    h = p;
                    ++i;
                }
                return n;
            } finally {
                if (i > 0) {
                    head = h;
                    signalNotFull = (count.getAndAdd(-i) == capacity);
                }
            }
        } finally {
            takeLock.unlock();
            if (signalNotFull)
                signalNotFull();
        }
    }

    /**
     * 迭代器
     *
     * @return an iterator over the elements in this queue in proper sequence
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        /*
         * Basic weakly-consistent iterator.  At all times hold the next
         * item to hand out so that if hasNext() reports true, we will
         * still have it to return even if lost race with a take etc.
         */

        private Node<E> current;
        private Node<E> lastRet;
        private E currentElement;

        Itr() {
            fullyLock();
            try {
                current = head.next;
                if (current != null)
                    currentElement = current.item;
            } finally {
                fullyUnlock();
            }
        }

        public boolean hasNext() {
            return current != null;
        }

        /**
         * Returns the next live successor of p, or null if no such.
         *
         * Unlike other traversal methods, iterators need to handle both:
         * - dequeued nodes (p.next == p)
         * - (possibly multiple) interior removed nodes (p.item == null)
         */
        private Node<E> nextNode(Node<E> p) {
            for (;;) {
                Node<E> s = p.next;
                if (s == p)
                    return head.next;
                if (s == null || s.item != null)
                    return s;
                p = s;
            }
        }

        public E next() {
            fullyLock();
            try {
                if (current == null)
                    throw new NoSuchElementException();
                E x = currentElement;
                lastRet = current;
                current = nextNode(current);
                currentElement = (current == null) ? null : current.item;
                return x;
            } finally {
                fullyUnlock();
            }
        }

        public void remove() {
            if (lastRet == null)
                throw new IllegalStateException();
            fullyLock();
            try {
                Node<E> node = lastRet;
                lastRet = null;
                for (Node<E> trail = head, p = trail.next;
                     p != null;
                     trail = p, p = p.next) {
                    if (p == node) {
                        unlink(p, trail);
                        break;
                    }
                }
            } finally {
                fullyUnlock();
            }
        }
    }
}

