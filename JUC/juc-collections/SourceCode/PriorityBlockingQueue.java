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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.Consumer;
import sun.misc.SharedSecrets;

/**
 * 一个使用和 PriorityQueue 同样排序规则的无界阻塞队列，支持阻塞查询操作。
 * 虽然队列在逻辑上是无界的，但是由于内存资源耗尽可能导致
 * OutOfMemoryError，从而添加失败。此类不允许 null 元素。插入的元素必须
 * 可比较。
 *
 * 此类及其迭代器实现了 Collection 和 Iterator 的所有方法。iterator 方法
 * 提供的迭代器不保证以任何特定的顺序遍历此类中的元素。如果需要有序遍历，
 * 可以考虑使用 Arrays.sort(pq.toArray())。此外，此类提供 drainTo 函数，
 * 按优先级的顺序删除一些或所有元素，并将它们放入到另一个集合中。
 *
 * 此类的操作不保证具有相同优先级的元素的特定顺序。如果需要强制执行任何
 * 排序，可以定义自定义类或比较器：
 * class FIFOEntry<E extends Comparable<? super E>>
 *     implements Comparable<FIFOEntry<E>> {
 *   static final AtomicLong seq = new AtomicLong(0);
 *   final long seqNum;
 *   final E entry;
 *   public FIFOEntry(E entry) {
 *     seqNum = seq.getAndIncrement();
 *     this.entry = entry;
 *   }
 *   public E getEntry() { return entry; }
 *   public int compareTo(FIFOEntry<E> other) {
 *     int res = entry.compareTo(other.entry);
 *     if (res == 0 && other.entry != this.entry)
 *       res = (seqNum < other.seqNum ? -1 : 1);
 *     return res;
 *   }
 * }
 *
 * 此类是 Java Collections Framework 的成员。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
@SuppressWarnings("unchecked")
public class PriorityBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = 5595510919245408276L;

    /*
     * The implementation uses an array-based binary heap, with public
     * operations protected with a single lock. However, allocation
     * during resizing uses a simple spinlock (used only while not
     * holding main lock) in order to allow takes to operate
     * concurrently with allocation.  This avoids repeated
     * postponement of waiting consumers and consequent element
     * build-up. The need to back away from lock during allocation
     * makes it impossible to simply wrap delegated
     * java.util.PriorityQueue operations within a lock, as was done
     * in a previous version of this class. To maintain
     * interoperability, a plain PriorityQueue is still used during
     * serialization, which maintains compatibility at the expense of
     * transiently doubling overhead.
     */

    /**
     * 默认的数组容量
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 11;

    /**
     * 最大数组容量。
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * 作为堆结构的数组：queue[n] 的两个子节点分别为 queue[2*n+1] 和
     * queue[2*(n+1)]。按指定的比较器或者自然顺序排序。
     */
    private transient Object[] queue;

    /**
     * 队列中的元素个数。
     */
    private transient int size;

    /**
     * 比较器。如果为 null 按照元素的自然顺序排序。
     */
    private transient Comparator<? super E> comparator;

    /**
     * 保证所有 public 方法线程安全的锁。
     */
    private final ReentrantLock lock;

    /**
     * 当队列为空时用来阻塞线程的 condition。
     */
    private final Condition notEmpty;

    /**
     * 自旋锁
     */
    private transient volatile int allocationSpinLock;

    /**
     * A plain PriorityQueue used only for serialization,
     * to maintain compatibility with previous versions
     * of this class. Non-null only during serialization/deserialization.
     */
    private PriorityQueue<E> q;

    /**
     * 构造默认容量、默认比较器的优先队列。
     */
    public PriorityBlockingQueue() {
        this(DEFAULT_INITIAL_CAPACITY, null);
    }

    /**
     * 构造指定容量、默认比较器的优先队列。
     *
     * @param initialCapacity the initial capacity for this priority queue
     * @throws IllegalArgumentException if {@code initialCapacity} is less
     *         than 1
     */
    public PriorityBlockingQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    /**
     * 构造指定容量，指定比较器的优先队列。
     *
     * @param initialCapacity the initial capacity for this priority queue
     * @param  comparator the comparator that will be used to order this
     *         priority queue.  If {@code null}, the {@linkplain Comparable
     *         natural ordering} of the elements will be used.
     * @throws IllegalArgumentException if {@code initialCapacity} is less
     *         than 1
     */
    public PriorityBlockingQueue(int initialCapacity,
                                 Comparator<? super E> comparator) {
        if (initialCapacity < 1)
            throw new IllegalArgumentException();
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
        this.comparator = comparator;
        this.queue = new Object[initialCapacity];
    }

    /**
     * 构造包含指定集合所有元素的优先队列。如果指定集合是 SortedSet 或者
     * PriorityQueue，此优先级队列会按照同样的顺序排序，否则按照元素自然
     * 顺序排序。
     *
     * @param  c the collection whose elements are to be placed
     *         into this priority queue
     * @throws ClassCastException if elements of the specified collection
     *         cannot be compared to one another according to the priority
     *         queue's ordering
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    public PriorityBlockingQueue(Collection<? extends E> c) {
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
        boolean heapify = true; // true if not known to be in heap order
        boolean screen = true;  // true if must screen for nulls
        if (c instanceof SortedSet<?>) {
            SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
            this.comparator = (Comparator<? super E>) ss.comparator();
            heapify = false;
        }
        else if (c instanceof PriorityBlockingQueue<?>) {
            PriorityBlockingQueue<? extends E> pq =
                    (PriorityBlockingQueue<? extends E>) c;
            this.comparator = (Comparator<? super E>) pq.comparator();
            screen = false;
            if (pq.getClass() == PriorityBlockingQueue.class) // exact match
                heapify = false;
        }
        Object[] a = c.toArray();
        int n = a.length;
        // If c.toArray incorrectly doesn't return Object[], copy it.
        if (a.getClass() != Object[].class)
            a = Arrays.copyOf(a, n, Object[].class);
        if (screen && (n == 1 || this.comparator != null)) {
            for (int i = 0; i < n; ++i)
                if (a[i] == null)
                    throw new NullPointerException();
        }
        this.queue = a;
        this.size = n;
        if (heapify)
            heapify();
    }

    /**
     * 扩容。
     *
     * @param array the heap array
     * @param oldCap the length of the array
     */
    private void tryGrow(Object[] array, int oldCap) {
        // 此类是私有方法，只在 offer 函数中调用。
        // 调用时已经获取了锁，进行扩容操作前释放
        lock.unlock();
        Object[] newArray = null;
        // 只有一条线程能进入
        if (allocationSpinLock == 0 &&
                UNSAFE.compareAndSwapInt(this, allocationSpinLockOffset,
                        0, 1)) {
            try {
                // 新的容量
                // 旧容量小于 64 时，新容量为 2 * cap + 2
                // 旧容量大于等于 64 时，新容量为 1.5 * cap
                int newCap = oldCap + ((oldCap < 64) ?
                        (oldCap + 2) : // 容量较小时扩容较快
                        (oldCap >> 1));
                // 计算出来的新容量超出最大限制
                if (newCap - MAX_ARRAY_SIZE > 0) {    // possible overflow
                    int minCap = oldCap + 1;
                    // 溢出
                    if (minCap < 0 || minCap > MAX_ARRAY_SIZE)
                        throw new OutOfMemoryError();
                    // 或者容量直接变成最大容量
                    newCap = MAX_ARRAY_SIZE;
                }
                // 如果数组还没有被更改，创建新的数组
                if (newCap > oldCap && queue == array)
                    newArray = new Object[newCap];
            } finally {
                // 释放 CAS 锁
                allocationSpinLock = 0;
            }
        }
        // newArray 是局部变量，线程独有
        // newArray 为 null 说明竞争 allocationSpinLock 失败，让出时间片
        if (newArray == null)
            Thread.yield();
        // 在新容量计算好了以后，对底层数组的更改和对原数组的复制等操作，是需要重新上锁的，
        lock.lock();
        // 上锁，如果数组还没有被更改，将旧数组中的元素全部复制到新数组里
        if (newArray != null && queue == array) {
            queue = newArray;
            System.arraycopy(array, 0, newArray, 0, oldCap);
        }
    }

    /**
     * poll 的具体实现。在持有锁时才能调用。
     */
    private E dequeue() {
        int n = size - 1;
        // 没有元素，返回 null
        if (n < 0)
            return null;
        else {
            Object[] array = queue;
            // 堆顶元素出列
            E result = (E) array[0];
            // 最后一个元素插入堆顶，然后调整其位置
            E x = (E) array[n];
            array[n] = null;
            Comparator<? super E> cmp = comparator;
            if (cmp == null)
                siftDownComparable(0, x, array, n);
            else
                siftDownUsingComparator(0, x, array, n, cmp);
            size = n;
            return result;
        }
    }

    /**
     * 将元素插入到 k 位置，然后调整元素位置使其满足堆的性质。
     * 将其往上层移动直到大于等于其父节点，或者其已经是根节点了。
     *
     * @param k the position to fill
     * @param x the item to insert
     * @param array the heap array
     */
    private static <T> void siftUpComparable(int k, T x, Object[] array) {
        Comparable<? super T> key = (Comparable<? super T>) x;
        while (k > 0) {
            // 父节点位置为 (k - 1) / 2
            int parent = (k - 1) >>> 1;
            Object e = array[parent];
            // 如果 key 大于等于父节点，跳出循环
            if (key.compareTo((T) e) >= 0)
                break;
            // 否则父节点下移，继续往上
            array[k] = e;
            k = parent;
        }
        // 找到 key 的位置了
        array[k] = key;
    }

    // 有比较器的 siftUp 操作
    private static <T> void siftUpUsingComparator(int k, T x, Object[] array,
                                                  Comparator<? super T> cmp) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            Object e = array[parent];
            if (cmp.compare(x, (T) e) >= 0)
                break;
            array[k] = e;
            k = parent;
        }
        array[k] = x;
    }

    /**
     * siftUp 用于插入，因为插入操作是新元素插入到最底层
     * siftDown 用于删除，删除是将最后一个元素提到最上层
     * 将元素 x 插入到 k 位置，调整使其满足堆的性质。将 x 元素下移直到
     * 小于等于其子节点或者成为叶节点。
     *
     * @param k the position to fill
     * @param x the item to insert
     * @param array the heap array
     * @param n heap size
     */
    private static <T> void siftDownComparable(int k, T x, Object[] array,
                                               int n) {
        if (n > 0) {
            Comparable<? super T> key = (Comparable<? super T>)x;
            int half = n >>> 1;
            // k 大于等于 half 时已经是叶节点，只需要
            while (k < half) {
                // 左子节点索引
                int child = (k << 1) + 1;
                Object c = array[child];
                // 右子节点索引
                int right = child + 1;
                // 获取值较小的元素，和其索引
                if (right < n &&
                        ((Comparable<? super T>) c).compareTo((T) array[right]) > 0)
                    c = array[child = right];
                // 当 key 小于等于 c 时，跳出循环
                if (key.compareTo((T) c) <= 0)
                    break;
                // k 位置赋值为较小的元素，然后 k 继续往下
                array[k] = c;
                k = child;
            }
            // 找到位置了
            array[k] = key;
        }
    }

    // 有比较器的 siftDown
    private static <T> void siftDownUsingComparator(int k, T x, Object[] array,
                                                    int n,
                                                    Comparator<? super T> cmp) {
        if (n > 0) {
            int half = n >>> 1;
            while (k < half) {
                int child = (k << 1) + 1;
                Object c = array[child];
                int right = child + 1;
                if (right < n && cmp.compare((T) c, (T) array[right]) > 0)
                    c = array[child = right];
                if (cmp.compare(x, (T) c) <= 0)
                    break;
                array[k] = c;
                k = child;
            }
            array[k] = x;
        }
    }

    /**
     * 对初始的完全无序的数组执行建堆操作。
     */
    private void heapify() {
        Object[] array = queue;
        int n = size;
        // 从 half 开始往前处理每一个节点，找到它正确的位置
        int half = (n >>> 1) - 1;
        Comparator<? super E> cmp = comparator;
        if (cmp == null) {
            for (int i = half; i >= 0; i--)
                siftDownComparable(i, (E) array[i], array, n);
        }
        else {
            for (int i = half; i >= 0; i--)
                siftDownUsingComparator(i, (E) array[i], array, n, cmp);
        }
    }

    /**
     * 将指定元素插入到队列中。
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws ClassCastException if the specified element cannot be compared
     *         with elements currently in the priority queue according to the
     *         priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        return offer(e);
    }

    /**
     * 将指定元素插入到优先队列。
     * 由于队列是无界队列，此方法不会返回 false。
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws ClassCastException if the specified element cannot be compared
     *         with elements currently in the priority queue according to the
     *         priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        if (e == null)
            throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        // 获取锁
        lock.lock();
        int n, cap;
        Object[] array;
        // 扩容（size 表示下一个可插入的位置）
        while ((n = size) >= (cap = (array = queue).length))
            tryGrow(array, cap);
        try {
            Comparator<? super E> cmp = comparator;
            // 没有比较器的插入方式（整型默认自然顺序为最小堆）
            // 在 n 位置插入元素
            if (cmp == null)
                siftUpComparable(n, e, array);
            // 指定了比较器的插入方式
            else
                siftUpUsingComparator(n, e, array, cmp);
            size = n + 1;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
        return true;
    }

    /**
     * 将指定元素插入到队列中。
     *
     * @param e the element to add
     * @throws ClassCastException if the specified element cannot be compared
     *         with elements currently in the priority queue according to the
     *         priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    public void put(E e) {
        offer(e);
    }

    /**
     * 将指定元素插入到队列中。
     *
     * @param e the element to add
     * @param timeout This parameter is ignored as the method never blocks
     * @param unit This parameter is ignored as the method never blocks
     * @return {@code true} (as specified by
     *  {@link BlockingQueue#offer(Object,long,TimeUnit) BlockingQueue.offer})
     * @throws ClassCastException if the specified element cannot be compared
     *         with elements currently in the priority queue according to the
     *         priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e);
    }

    // 堆顶元素出队列。没有元素返回 null。
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    // 堆顶元素出队列。没有元素进入 condition 等到，直到有元素为止。
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        E result;
        try {
            // 没有元素进入 condition 等待。
            while ( (result = dequeue()) == null)
                notEmpty.await();
        } finally {
            lock.unlock();
        }
        return result;
    }

    // 堆顶元素出队列。没有元素进入 condition 等到，直到有元素或者时间到期
    // 为止。时间到期还没有元素返回 null。
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        E result;
        try {
            while ( (result = dequeue()) == null && nanos > 0)
                nanos = notEmpty.awaitNanos(nanos);
        } finally {
            lock.unlock();
        }
        return result;
    }

    // 获取堆顶元素。没有返回 null。
    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (size == 0) ? null : (E) queue[0];
        } finally {
            lock.unlock();
        }
    }

    /**
     * 返回比较器。
     *
     * @return the comparator used to order the elements in this queue,
     *         or {@code null} if this queue uses the natural
     *         ordering of its elements
     */
    public Comparator<? super E> comparator() {
        return comparator;
    }

    // 返回 size。
    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return size;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 返回 Integer.MAX_VALUE。
     */
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    // 获取索引。
    private int indexOf(Object o) {
        if (o != null) {
            Object[] array = queue;
            int n = size;
            for (int i = 0; i < n; i++)
                if (o.equals(array[i]))
                    return i;
        }
        return -1;
    }

    /**
     * 删除第 i 个元素。
     * 删除之后还是将最后一个元素移到 i 为止，然后调整堆。
     */
    private void removeAt(int i) {
        Object[] array = queue;
        int n = size - 1;
        if (n == i) // removed last element
            array[i] = null;
        else {
            E moved = (E) array[n];
            array[n] = null;
            Comparator<? super E> cmp = comparator;
            if (cmp == null)
                siftDownComparable(i, moved, array, n);
            else
                siftDownUsingComparator(i, moved, array, n, cmp);
            if (array[i] == moved) {
                if (cmp == null)
                    siftUpComparable(i, moved, array);
                else
                    siftUpUsingComparator(i, moved, array, cmp);
            }
        }
        size = n;
    }

    /**
     * 删除指定元素。
     *
     * @param o element to be removed from this queue, if present
     * @return {@code true} if this queue changed as a result of the call
     */
    public boolean remove(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 首先找到索引
            int i = indexOf(o);
            if (i == -1)
                return false;
            // 删除
            removeAt(i);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Identity-based version for use in Itr.remove
     */
    void removeEQ(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] array = queue;
            for (int i = 0, n = size; i < n; i++) {
                if (o == array[i]) {
                    removeAt(i);
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 判断是否包含指定元素。
     *
     * @param o object to be checked for containment in this queue
     * @return {@code true} if this queue contains the specified element
     */
    public boolean contains(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return indexOf(o) != -1;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 返回数组。
     *
     * @return an array containing all of the elements in this queue
     */
    public Object[] toArray() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return Arrays.copyOf(queue, size);
        } finally {
            lock.unlock();
        }
    }

    // 返回字符串
    public String toString() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = size;
            if (n == 0)
                return "[]";
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = 0; i < n; ++i) {
                Object e = queue[i];
                sb.append(e == this ? "(this Collection)" : e);
                if (i != n - 1)
                    sb.append(',').append(' ');
            }
            return sb.append(']').toString();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
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
        if (maxElements <= 0)
            return 0;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = Math.min(size, maxElements);
            // 将堆顶元素添加到指定集合中。
            // 然后删除堆顶元素。
            for (int i = 0; i < n; i++) {
                c.add((E) queue[0]); // In this order, in case add() throws.
                dequeue();
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 删除所有元素。
     */
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] array = queue;
            int n = size;
            size = 0;
            for (int i = 0; i < n; i++)
                array[i] = null;
        } finally {
            lock.unlock();
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
    public <T> T[] toArray(T[] a) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = size;
            if (a.length < n)
                // Make a new array of a's runtime type, but my contents:
                return (T[]) Arrays.copyOf(queue, size, a.getClass());
            System.arraycopy(queue, 0, a, 0, n);
            if (a.length > n)
                a[n] = null;
            return a;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 返回迭代器。
     *
     * <p>The returned iterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * @return an iterator over the elements in this queue
     */
    public Iterator<E> iterator() {
        return new Itr(toArray());
    }

    /**
     * 获取创建时的快照。
     */
    final class Itr implements Iterator<E> {
        final Object[] array; // Array of all elements
        int cursor;           // index of next element to return
        int lastRet;          // index of last element, or -1 if no such

        Itr(Object[] array) {
            lastRet = -1;
            this.array = array;
        }

        public boolean hasNext() {
            return cursor < array.length;
        }

        public E next() {
            if (cursor >= array.length)
                throw new NoSuchElementException();
            lastRet = cursor;
            return (E)array[cursor++];
        }

        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            removeEQ(array[lastRet]);
            lastRet = -1;
        }
    }
}

