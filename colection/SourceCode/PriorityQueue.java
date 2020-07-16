/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

import java.util.function.Consumer;

/**
 * An unbounded priority {@linkplain Queue queue} based on a priority heap.
 * The elements of the priority queue are ordered according to their
 * {@linkplain Comparable natural ordering}, or by a {@link Comparator}
 * provided at queue construction time, depending on which constructor is
 * used.  A priority queue does not permit {@code null} elements.
 * A priority queue relying on natural ordering also does not permit
 * insertion of non-comparable objects (doing so may result in
 * {@code ClassCastException}).
 *一个基于优先级堆的无界优先级队列。根据 Comparable 比较器的自然顺序
 *   确定优先级元素的排列顺序，或者根据构造队列时创建的 Comparator 比较器
 *  排列队列元素。优先级队列不允许 null 元素。依赖于自然顺序的优先级队列
 *  也不允许插入不可比较的对象（这样做可能抛出 ClassCastException 异常）。
 *
 * <p>The <em>head</em> of this queue is the <em>least</em> element
 * with respect to the specified ordering.  If multiple elements are
 * tied for least value, the head is one of those elements -- ties are
 * broken arbitrarily.  The queue retrieval operations {@code poll},
 * {@code remove}, {@code peek}, and {@code element} access the
 * element at the head of the queue.
 *队列的头部元素是于指定顺序相关的最小的元素。如果多个元素满足该条件，
 * 那么头部元素是其中任意一个。队列的检索操作 poll, remove, peek, element
 * 等会访问队列的头部元素。
 *
 * <p>A priority queue is unbounded, but has an internal
 * <i>capacity</i> governing the size of an array used to store the
 * elements on the queue.  It is always at least as large as the queue
 * size.  As elements are added to a priority queue, its capacity
 * grows automatically.  The details of the growth policy are not
 * specified.
 * 优先级队列是无界的，但是具有控制数组大小的内部容量。该容量应该大于等于
 * 队列的大小。当元素被添加到优先级队列中时，其容量自动增加。没有指定
 *  具体的增长策略。
 *
 * <p>This class and its iterator implement all of the
 * <em>optional</em> methods of the {@link Collection} and {@link
 * Iterator} interfaces.  The Iterator provided in method {@link
 * #iterator()} is <em>not</em> guaranteed to traverse the elements of
 * the priority queue in any particular order. If you need ordered
 * traversal, consider using {@code Arrays.sort(pq.toArray())}.
 * 这个类和它的迭代器实现了 Collection 和 Iterator 接口的所有可选方法。
 *  iterator 方法提供的迭代器不能保证以任何特定的顺序遍历优先级队列的元素。
 *  如果需要有序遍历，考虑使用 Arrays.sort(pq.toArray())。
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * Multiple threads should not access a {@code PriorityQueue}
 * instance concurrently if any of the threads modifies the queue.
 * Instead, use the thread-safe {@link
 * java.util.concurrent.PriorityBlockingQueue} class.
 *注意该实现不是同步的。多线程不应该同时修改优先级队列，而应该使用线程
 * 安全的 PriorityBlockingQueue 类。
 *
 * <p>Implementation note: this implementation provides
 * O(log(n)) time for the enqueuing and dequeuing methods
 * ({@code offer}, {@code poll}, {@code remove()} and {@code add});
 * linear time for the {@code remove(Object)} and {@code contains(Object)}
 * methods; and constant time for the retrieval methods
 * ({@code peek}, {@code element}, and {@code size}).
 *此实现提供了时间代价为 O(log(n)) 的入队和出队方法：offer, poll, remove,
 *   add；提供了线性时间代价的 remove 和 contains 方法；除此之外，还有常数
 *  时间代价的 peek, element, size 方法。
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *此类是 Java Collections Framework 的成员。
 * @since 1.5
 * @author Josh Bloch, Doug Lea
 * @param <E> the type of elements held in this collection
 */
public class PriorityQueue<E> extends AbstractQueue<E>
        implements java.io.Serializable {

    private static final long serialVersionUID = -7720805057305804111L;
   //序列化，比较版本用的，对类功能没有影响
    private static final int DEFAULT_INITIAL_CAPACITY = 11;
     //默认的初始容量
    /**
     * Priority queue represented as a balanced binary heap: the two
     * children of queue[n] are queue[2*n+1] and queue[2*(n+1)].  The
     * priority queue is ordered by comparator, or by the elements'
     * natural ordering, if comparator is null: For each node n in the
     * heap and each descendant d of n, n <= d.  The element with the
     * lowest value is in queue[0], assuming the queue is nonempty.
     *
     * 优先级队列表现为一个平衡的二进制堆：queue[n] 的两个子队列分别为
     * queue[2*n+1] 和 queue[2*(n+1)]。如果队列的顺序由比较器决定，或者按
     * 元素的自然顺序排序。对于堆中的每个节点 n 和 n 的每个后代 d，有 n <= d。
     * 假定队列非空，堆中最小值的元素为 queue[0]。
     */
    transient Object[] queue; // non-private to simplify nested class access
    //非私有以简化嵌套类访问
    /**
     * The number of elements in the priority queue.
     * 优先级队列中的元素个数。
     */
    private int size = 0;

    /**
     * The comparator, or null if priority queue uses elements'
     * natural ordering.
     * 比较器。如果使用元素的自然顺序排序的话此比较器为 null。
     */
    private final Comparator<? super E> comparator;

    /**
     * The number of times this priority queue has been
     * <i>structurally modified</i>.  See AbstractList for gory details.
     * 优先级队列被结构性修改的次数。
     */
    transient int modCount = 0; // non-private to simplify nested class access

    /**
     * Creates a {@code PriorityQueue} with the default initial
     * capacity (11) that orders its elements according to their
     * {@linkplain Comparable natural ordering}.
     * 创建一个容量为默认初始容量的优先级队列。其中元素的顺序为
     * Comparable 自然顺序。
     */
    public PriorityQueue() {
        this(DEFAULT_INITIAL_CAPACITY, null);
    }

    /**
     * Creates a {@code PriorityQueue} with the specified initial
     * capacity that orders its elements according to their
     * {@linkplain Comparable natural ordering}.
     * 创建一个特定初始容量的优先级队列，其中元素的顺序为 Comparable 的
     * 自然顺序。
     * @param initialCapacity the initial capacity for this priority queue
     * @throws IllegalArgumentException if {@code initialCapacity} is less
     *         than 1
     */
    public PriorityQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    /**
     * Creates a {@code PriorityQueue} with the default initial capacity and
     * whose elements are ordered according to the specified comparator.
     *创建一个容量为默认初始容量，元素排列顺序为比较器指定顺序的优先级
     *     队列。
     * @param  comparator the comparator that will be used to order this
     *         priority queue.  If {@code null}, the {@linkplain Comparable
     *         natural ordering} of the elements will be used.
     * @since 1.8
     */
    public PriorityQueue(Comparator<? super E> comparator) {
        this(DEFAULT_INITIAL_CAPACITY, comparator);
    }

    /**
     * Creates a {@code PriorityQueue} with the specified initial capacity
     * that orders its elements according to the specified comparator.
     *创建一个容量为默认初始容量，元素排列顺序为比较器指定顺序的优先级
     * 队列。
     * @param  initialCapacity the initial capacity for this priority queue
     * @param  comparator the comparator that will be used to order this
     *         priority queue.  If {@code null}, the {@linkplain Comparable
     *         natural ordering} of the elements will be used.
     * @throws IllegalArgumentException if {@code initialCapacity} is
     *         less than 1
     */
    public PriorityQueue(int initialCapacity,
                         Comparator<? super E> comparator) {
        // Note: This restriction of at least one is not actually needed,
        // but continues for 1.5 compatibility
        if (initialCapacity < 1)     //指定容量小于1，抛出异常，注意不允许指定容量为0
            throw new IllegalArgumentException();
        this.queue = new Object[initialCapacity];       //创建新数组
        this.comparator = comparator;       //比较器赋值
    }

    /**
     * Creates a {@code PriorityQueue} containing the elements in the
     * specified collection.  If the specified collection is an instance of
     * a {@link SortedSet} or is another {@code PriorityQueue}, this
     * priority queue will be ordered according to the same ordering.
     * Otherwise, this priority queue will be ordered according to the
     * {@linkplain Comparable natural ordering} of its elements.
     *创建一个包含指定集合所有元素的优先级队列。如果指定集合是一个
     *  SortedSet 的实例或者是另一个 PriorityQueue，这个优先级队列中的元素
     *  会以相同的顺序排列。否则，这个优先级队列将根据元素的自然顺序排列。
     * @param  c the collection whose elements are to be placed
     *         into this priority queue
     * @throws ClassCastException if elements of the specified collection
     *         cannot be compared to one another according to the priority
     *         queue's ordering
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    @SuppressWarnings("unchecked")
    public PriorityQueue(Collection<? extends E> c) {
        if (c instanceof SortedSet<?>) {      //判断是否是SortedSet
            SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
            this.comparator = (Comparator<? super E>) ss.comparator();   //赋值比较器
            initElementsFromCollection(ss);    //排序set版本的初始化值
        }
        else if (c instanceof PriorityQueue<?>) { //判断是否是PriorityQueue
            PriorityQueue<? extends E> pq = (PriorityQueue<? extends E>) c;
            this.comparator = (Comparator<? super E>) pq.comparator();//赋值比较器
            initFromPriorityQueue(pq);    //优先队列的初始化值
        }
        else {      //如果不是可排序的，则使用默认比较器
            this.comparator = null;   //   即比较器赋值为null
            initFromCollection(c);   //普通集合版本的初始化值
        }
    }

    /**
     * Creates a {@code PriorityQueue} containing the elements in the
     * specified priority queue.  This priority queue will be
     * ordered according to the same ordering as the given priority
     * queue.
     *创建一个包含指定集合所有元素的优先级队列。这个优先级队列中的元素
     *   会以指定队列相同的顺序排序。
     * @param  c the priority queue whose elements are to be placed
     *         into this priority queue
     * @throws ClassCastException if elements of {@code c} cannot be
     *         compared to one another according to {@code c}'s
     *         ordering
     * @throws NullPointerException if the specified priority queue or any
     *         of its elements are null
     */
    @SuppressWarnings("unchecked")
    public PriorityQueue(PriorityQueue<? extends E> c) {    //和上面传入集合是优先队列时候处理一样
        this.comparator = (Comparator<? super E>) c.comparator();
        initFromPriorityQueue(c);
    }

    /**
     * Creates a {@code PriorityQueue} containing the elements in the
     * specified sorted set.   This priority queue will be ordered
     * according to the same ordering as the given sorted set.
     *创建一个包含指定 SortedSet 的所有元素的优先级队列。这个优先级队列
     *  中的元素以给定 SortedSet 的顺序排列。
     * @param  c the sorted set whose elements are to be placed
     *         into this priority queue
     * @throws ClassCastException if elements of the specified sorted
     *         set cannot be compared to one another according to the
     *         sorted set's ordering
     * @throws NullPointerException if the specified sorted set or any
     *         of its elements are null
     */
    @SuppressWarnings("unchecked")
    public PriorityQueue(SortedSet<? extends E> c) {      //和上面传入集合是排序set时处理一样。
        this.comparator = (Comparator<? super E>) c.comparator();
        initElementsFromCollection(c);
    }

    // 根据给定的优先级队列初始化此优先级队列，将所有元素复制到此队列中
    // 直接将给定的优先级队列转化为数组，赋值给此队列
    private void initFromPriorityQueue(PriorityQueue<? extends E> c) {  //优先队列版本的初始化值
        if (c.getClass() == PriorityQueue.class) {   //先判断传入的是否是优先队列类
            this.queue = c.toArray();   //是的话，将指定优先队列转化为数组，赋值给当前队列底层数组queue
            this.size = c.size();
        } else {
            initFromCollection(c);      //如果发现不是优先队列，调用普通集合版本的初始化方法
        }
    }
    // 根据给定的 Collection 初始化此优先级队列，将所有元素复制到此队列中
    private void initElementsFromCollection(Collection<? extends E> c) {  //SortedSet版本，普通集合也会调用到
        Object[] a = c.toArray();    //得到集合的数组。
        // If c.toArray incorrectly doesn't return Object[], copy it.
        //如果不是返回Object数组，则使用Array.copyOf进行类型转化。
        if (a.getClass() != Object[].class)
            a = Arrays.copyOf(a, a.length, Object[].class);
        int len = a.length;     //得到元素个数
        if (len == 1 || this.comparator != null)  //有排序功能集合，或者只有一个元素的集合执行检查空值。
            for (int i = 0; i < len; i++)   //判断集合得到的数组是否全不为null，因为优先队列不允许元素为null，
                if (a[i] == null)  //只要有一个为null,就会抛出空指针异常
                    throw new NullPointerException();
        this.queue = a;
        this.size = a.length;     //排完序的集合初始化到这步就完成。
    }

    /**
     * Initializes queue array with elements from the given Collection.
     * 根据给定的集合中的元素初始化此队列。普通集合版本，
     * @param c the collection
     */
    private void initFromCollection(Collection<? extends E> c) {  //普通集合版本
        initElementsFromCollection(c);
        heapify();              //堆处理
    }

    /**
     * The maximum size of array to allocate.
     *  数组最多能容纳的元素个数。
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     * 些虚拟机会保留一些头消息，占用部分空间。
     *   尝试分配比这个值更大的空间可能会抛出 OutOfMemoryError 错误：请求的
     *  数组大小超过了虚拟机的限制。
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * Increases the capacity of the array.
     *扩大数组的容量
     * @param minCapacity the desired minimum capacity
     */
    private void grow(int minCapacity) {
        int oldCapacity = queue.length;    //记录旧容量
        // Double size if small; else grow by 50%
        //如果容量小于64，则扩容的新容量为2*老容量+2，
        //否则新容量为老容量的1.5倍。
        //因为容量都是+1然后判断，并且最小容量为1，则扩容以后的新容量肯定大于minCapacity
        //这和ArrayList不同，不需要判断扩容以后，新容量是否大于minCapacity
        int newCapacity = oldCapacity + ((oldCapacity < 64) ?
                (oldCapacity + 2) :
                (oldCapacity >> 1));
        // overflow-conscious code
        // 如果新容量比规定的最大容量还要大，那么将新容量设置为整型最大值
        if (newCapacity - MAX_ARRAY_SIZE > 0)     //如果扩容大于最大允许的数组长度
            newCapacity = hugeCapacity(minCapacity); //调用hugeCapacity进行判断。
        queue = Arrays.copyOf(queue, newCapacity);   //同样适用Array.copyOf进行复制。
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // 溢出
            throw new OutOfMemoryError();
        // 指定容量大于规定的最大容量，将新容量设置为整型最大值，否则设置
        // 为规定的最大容量。
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }

    /**
     * Inserts the specified element into this priority queue.
     *将指定元素插入到优先级队列中
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws ClassCastException if the specified element cannot be
     *         compared with elements currently in this priority queue
     *         according to the priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        return offer(e);
    }

    /**
     * Inserts the specified element into this priority queue.
     *将指定元素插入到优先级队列中
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws ClassCastException if the specified element cannot be
     *         compared with elements currently in this priority queue
     *         according to the priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        if (e == null)    //为null会抛出空指针异常
            throw new NullPointerException();
        modCount++;
        int i = size;
        if (i >= queue.length)   //判断是否需要扩容
            grow(i + 1);
        size = i + 1;  //更新size
        if (i == 0)     //如果是第一次添加元素，
            queue[0] = e;
        else       //不是第一次，需要调用siftUp进行添加，因为需要维护堆的结构
            siftUp(i, e);
        return true;
    }

    @SuppressWarnings("unchecked")
    public E peek() {   //取得队列头部元素，队列为空返回null
        return (size == 0) ? null : (E) queue[0];
    }

    private int indexOf(Object o) {     //返回指定对象o在队列中的第一次出现的索引。
        if (o != null) {                     //需要不为null
            for (int i = 0; i < size; i++)   //遍历数组，调用equals方法逐一判断。
                if (o.equals(queue[i]))
                    return i;
        }
        return -1;    //没找到返回-1
    }

    /**
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element {@code e} such
     * that {@code o.equals(e)}, if this queue contains one or more such
     * elements.  Returns {@code true} if and only if this queue contained
     * the specified element (or equivalently, if this queue changed as a
     * result of the call).
     * 从队列中删除指定元素的单个匹配实例。如果存在一个或多个删除任意一个。
     *  当队列包含指定元素时返回 true（表示操作成功）
     * @param o element to be removed from this queue, if present
     * @return {@code true} if this queue changed as a result of the call
     */
    public boolean remove(Object o) {
        int i = indexOf(o);    //获得元素的索引
        if (i == -1)
            return false;  //未找到返回false
        else {
            removeAt(i);  //removeAt方法删除
            return true;  //找到返回true
        }
    }

    /**
     * Version of remove using reference equality, not equals.
     * Needed by iterator.remove.
     * 和 remove 不同的是，判断相等时直接判断引用，不使用 equals 方法
     *   iterator.remove 需要此方法
     * @param o element to be removed from this queue, if present
     * @return {@code true} if removed
     */
    boolean removeEq(Object o) {
        for (int i = 0; i < size; i++) {
            if (o == queue[i]) {   //直接判断引用的对象地址是否相同。
                removeAt(i); //removeAt方法删除
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this queue contains the specified element.
     * More formally, returns {@code true} if and only if this queue contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *如果队列包含指定元素返回 true，判断方是否相等使用 equals
     * @param o object to be checked for containment in this queue
     * @return {@code true} if this queue contains the specified element
     */
    public boolean contains(Object o) {     //内部调用indexOf方法。
        return indexOf(o) != -1;
    }

    /**
     * Returns an array containing all of the elements in this queue.
     * The elements are in no particular order.
     * 返回包含队列中所有元素的数组
     * 这些元素没有特定的顺序
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this queue.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     * 返回的数组是“安全”的，因为队列不保留对它的引用。（换句话说，数组
     *  空间是新分配的）。调用者可以随意修改返回的数组。
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this queue
     */
    public Object[] toArray() {
        return Arrays.copyOf(queue, size);
    }

    /**
     * Returns an array containing all of the elements in this queue; the
     * runtime type of the returned array is that of the specified array.
     * The returned array elements are in no particular order.
     * If the queue fits in the specified array, it is returned therein.
     * Otherwise, a new array is allocated with the runtime type of the
     * specified array and the size of this queue.
     *返回一个包含队列中的所有元素的数组返回数组的运行时类型是指定数组的
     *  运行时类型。数组中元素的顺序没有规定。如果指定的数组能容纳队列的
     *  所有元素，则返回指定数组。否则，将按照指定数组的运行时类型和该队列
     *的大小分配一个新数组。
     * <p>If the queue fits in the specified array with room to spare
     * (i.e., the array has more elements than the queue), the element in
     * the array immediately following the end of the collection is set to
     * {@code null}.
     *如果指定数组还有空余的位置，则将其设置为 null。
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *和 toArray 方法一样，将此方法作为沟通基于数组和基于集合的 API 的桥梁。
     * 除此之外，此方法允许对输出数组的运行时类型进行控制，在精确的计算下，
     *可以用来节省空间。
     *
     * <p>Suppose {@code x} is a queue known to contain only strings.
     * The following code can be used to dump the queue into a newly
     * allocated array of {@code String}:
     *
     *  <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the queue are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing all of the elements in this queue
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this queue
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        final int size = this.size;
        if (a.length < size)    //如果指定数组无法容纳下全部元素
            // Make a new array of a's runtime type, but my contents:
            //创建一个新的数组，使用copyOf函数，且指定了运行时类型为参数a的类型T，返回一个新的T[]类型数组。
            return (T[]) Arrays.copyOf(queue, size, a.getClass());
        System.arraycopy(queue, 0, a, 0, size);
        if (a.length > size)  //如果目标数组还有空余位置。
            a[size] = null;   //则在目标数组a的紧跟着当前数组的末尾元素的位置上赋值为null。
        return a;
    }

    /**
     * Returns an iterator over the elements in this queue. The iterator
     * does not return the elements in any particular order.
     *返回队列元素的迭代器。迭代器中元素没有特定顺序。
     * @return an iterator over the elements in this queue
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    private final class Itr implements Iterator<E> {
        /**
         * Index (into queue array) of element to be returned by
         * subsequent call to next.
         * 下一个被队列next调用返回的值的索引。
         */
        private int cursor = 0;

        /**
         * Index of element returned by most recent call to next,
         * unless that element came from the forgetMeNot list.
         * Set to -1 if element is deleted by a call to remove.
         * 最后一次被遍历的元素的索引，
         * 除非这个元素是来自于forgetMeNot列表
         * 如果删除某个元素，初始化为-1
         */
        private int lastRet = -1;

        /**
         * A queue of elements that were moved from the unvisited portion of
         * the heap into the visited portion as a result of "unlucky" element
         * removals during the iteration.  (Unlucky element removals are those
         * that require a siftup instead of a siftdown.)  We must visit all of
         * the elements in this list to complete the iteration.  We do this
         * after we've completed the "normal" iteration.
         *从堆中未访问的部分移动到已访问的部分的元素，即作为迭代过程中
         *  删除的“不幸”元素。（不幸的元素是那些需要 siftUp 而不是
         *  siftDown 的元素）。我们必须在迭代过程中访问列表中所有的元素。
         *  这一步在我们完成普通的迭代之后进行。
         * We expect that most iterations, even those involving removals,
         * will not need to store elements in this field.
         * 我们希望大多数的迭代过程，甚至是包含删除操作的迭代，都不需要
         *  在这个部分存储元素。
         */
        private ArrayDeque<E> forgetMeNot = null;  //ArrayDeque,循环数组实现的双端队列。

        /**
         * Element returned by the most recent call to next iff that
         * element was drawn from the forgetMeNot list.
         * 如果该元素是从 forgetMeNot 列表中取出的，则由最近一次调用的
         * next 返回。
         */
        private E lastRetElt = null;

        /**
         * The modCount value that the iterator believes that the backing
         * Queue should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         * 迭代器认为正在遍历的队列应该具有的modCount值。
         * 如果违反了这个期望，迭代器就检测到并发修改。
         */
        private int expectedModCount = modCount;
  //判断是否有下一个元素
        public boolean hasNext() {
            return cursor < size ||     //当前光标未到末尾
                    (forgetMeNot != null && !forgetMeNot.isEmpty());  //或者到了末尾，但forgetMeNot队列不为空，
        }

        @SuppressWarnings("unchecked")
        public E next() {
            if (expectedModCount != modCount)
                throw new ConcurrentModificationException();
            if (cursor < size)    //还未到末尾
                //lastRet=cursor，更新
                //cursor++，更新
                //再返回queue[lastRet]
                return (E) queue[lastRet = cursor++];
            if (forgetMeNot != null) {    //只有cursor到了末尾，才会取出forgetMeNot队列中的数
                lastRet = -1;  //lastRet=-1，因为元素从forgetMeNot取出
                lastRetElt = forgetMeNot.poll();    //弹出队列头部元素。
                if (lastRetElt != null)
                    return lastRetElt;
            }
            throw new NoSuchElementException();
        }
        //迭代器删除方法。
        public void remove() {
            if (expectedModCount != modCount)    //检查并发修改
                throw new ConcurrentModificationException();
            if (lastRet != -1) {
                E moved = PriorityQueue.this.removeAt(lastRet);  //调用优先队列的removeAt方法删除lastRet索引上的值
                lastRet = -1;   //删除后，重置为-1
                if (moved == null)      //如果返回null，说明removeAt中末尾元素没有丢失。
                    cursor--;  //则 当前cursor向后退即可
                else {       //如果元素丢失
                    if (forgetMeNot == null)    //第一个丢失的元素存储前，需要对数组进行初始化。
                        forgetMeNot = new ArrayDeque<>();
                    //将丢失元素加入遗忘队列中，且这一步，出让cursor没有减，因为当前i位置是一个已经遍历过的元素。
                    forgetMeNot.add(moved);    //在丢失队列的末尾添加元素。
                }
            } else if (lastRetElt != null) {        //如果lastRet等于-1，但是lastRetElt不为null，即丢失队列中还有值，
                //调用removeEq方法删除丢失队列中的值。因为本身就是队列中取出的。所以直接用==判断引用。
                PriorityQueue.this.removeEq(lastRetElt);
                lastRetElt = null;           //删除后还是要重置lastRetElt，
            } else {
                throw new IllegalStateException();     //否则状态异常错误、
            }
            expectedModCount = modCount;
        }
    }

    public int size() {      //返回队列中的元素个数
        return size;
    }

    /**
     * Removes all of the elements from this priority queue.
     * The queue will be empty after this call returns.
     * 移除优先队列中的所有元素。
     * 调用后，队列为空
     */
    public void clear() {
        modCount++;
        for (int i = 0; i < size; i++)     //直接遍历底层数组
            queue[i] = null;    //每个数组的值全赋值为null
        size = 0;    //重置size
    }

    @SuppressWarnings("unchecked")
    //实际上就是取出队首元素，即堆顶元素，然后用最后一个队列元素填充
    //然后在进行下移操作，因为放到了队首，只可能进行siftDown操作。
    public E poll() {           //弹出队列头部元素，队列为空返回null。
        if (size == 0)
            return null;
        int s = --size;        //size自减，并取得最后一个元素的值
        modCount++;
        E result = (E) queue[0];         //保存队列头部元素值，用于返回
        E x = (E) queue[s];                //保存队尾元素值，用于比较
        queue[s] = null;      //队列为空赋值为null。
        if (s != 0)           //只要原先队列不止一个元素，
            siftDown(0, x);       //就从0队首位置开始进行下移操作。
        return result;     //返回堆顶值
    }

    /**
     * Removes the ith element from queue.
     *从队列中删除第 i 个元素。
     * Normally this method leaves the elements at up to i-1,
     * inclusive, untouched.  Under these circumstances, it returns
     * null.  Occasionally, in order to maintain the heap invariant,
     * it must swap a later element of the list with one earlier than
     * i.  Under these circumstances, this method returns the element
     * that was previously at the end of the list and is now at some
     * position before i. This fact is used by iterator.remove so as to
     * avoid missing traversing elements.
     * 即删除一个值，就是将队列最后一个元素，提到这个i位置上
     * 然后先进行下移操作，因为下移操作只会将这个元素移到i后面的索引，
     * 并且后面索引上的值也只会移到i包括i后面，这些元素在迭代器遍历是，都不回丢失
     * 索引如果只是下移操作，直接返回null。
     * 但是如果下移操作没有执行，即queue[i]==moved,则说明应该进行上移判断
     * 如果发生了一次上移操作，说明这个原来在索引i后的元素，移动到了索引i之前，
     * 那么迭代器遍历就会缺失这个元素，所以要返回moved进行额外保存。
     */
    @SuppressWarnings("unchecked")
    private E removeAt(int i) {
        // assert i >= 0 && i < size; 断言，i肯定满足合法性
        modCount++;
        int s = --size;   //size更新后，赋值给s
        if (s == i) // removed last element //如果i==s,说明是最后一个元素的索引
            queue[i] = null;     //直接令最后一个元素为null。
        else {
            E moved = (E) queue[s];             //先保存队列最后一个元素值。
            queue[s] = null;          //将最后一个位置赋值为null。
            siftDown(i, moved);  //将最后这个值插入到当前删除位置的地方，并进行堆下移操作。
            // 如果没有向下调整，说明可能它的位置在上面，接着向上调整
            if (queue[i] == moved) {   //当前i上的值还是下移前保存的值，说明没有进行下移
                siftUp(i, moved);      //则进行上移
                if (queue[i] != moved)      //如果当前i不等于上移前保存的值，说明至少上移了一次。
                    return moved;      //返回原队列末尾的值
            }
        }
        return null;  //否则返回null。
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by
     * promoting x up the tree until it is greater than or equal to
     * its parent, or is the root.
     *在 k 位置插入元素 x（从 k 位置开始调整堆，找到元素 x 的位置，忽略
     * 原先 k 位置的元素），通过向上提升 x 直到它大于等于它的父元素或者
     *   根元素，来保证满足堆成立的条件。
     * To simplify and speed up coercions and comparisons. the
     * Comparable and Comparator versions are separated into different
     * methods that are otherwise identical. (Similarly for siftDown.)
     *为了简化和加速强制转换和比较，Comparable（元素的默认比较器）
     * 和 Comparator（指定的比较器）被分成不同的方法，这两个方法基本
     * 等同。（siftDown 同理）
     * @param k the position to fill
     * @param x the item to insert
     */
    private void siftUp(int k, E x) {
        if (comparator != null)       //有指定的比较器。
            siftUpUsingComparator(k, x);
        else        //如果没有指定，即元素实现了Comparable接口，则使用元素默认比较器。
            siftUpComparable(k, x);
    }

    @SuppressWarnings("unchecked")
    //传入的参数是，出插入的x,和填充到队列中的初始位置k。
    private void siftUpComparable(int k, E x) {   //元素默认比较器的向上移动插入元素的版本。
        // <? extends E>，集合中元素类型上限为 E，即只能是 E 或者 E 的子类
        // <? super E>，集合中元素类型下限为 E，即只能是 E 或者 E 的父类
        //Comparable<? super E>表示实现了Comparable接口的类，且这个接口中T是E本身或父类。
        Comparable<? super E> key = (Comparable<? super E>) x;
        while (k > 0) {
            int parent = (k - 1) >>> 1;      //获取k的父节点。
            Object e = queue[parent];      //取得父节点
            if (key.compareTo((E) e) >= 0)    //如果父节点和插入值相比，插入值大于等于父节点的值。
                break;   //说明不需要上移了
            queue[k] = e;      //如果父节点值大于插入值，则父节点交换到当前k,即当前比较的父节点的子节点位置
            k = parent;    //更新k位置原先父节点位置，进行下一次判断。即代表插入的节点本次循环上移到该位置。
        }
        queue[k] = key;     //找到插入值需要上移到的位置后，将数组中对应位置，赋值为插入元素的值。
    }

    @SuppressWarnings("unchecked")
    private void siftUpUsingComparator(int k, E x) {//使用了比较器的上移插入元素的版本
        while (k > 0) {
            int parent = (k - 1) >>> 1;         //获取k的父节点。
            Object e = queue[parent];    //获取父节点值
            if (comparator.compare(x, (E) e) >= 0)  //如果插入值大于等于父节点的值。
                break;  //上移完毕
            queue[k] = e;     //否则，将父节点下移到当前的k位置，即当前比较父节点的子节点位置，
            k = parent;    //更新k位置原先父节点位置，进行下一次判断。即代表插入的节点本次循环上移到该位置。
        }
        queue[k] = x;//找到插入值需要上移到的位置后，将数组中对应位置，赋值为插入元素的值。
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by
     * demoting x down the tree repeatedly until it is less than or
     * equal to its children or is a leaf.
     * 在 k 位置插入元素 x，通过向下调整 x 直到它小于等于它的子节点或者
     *   叶节点，来保证满足堆成立的条件。
     * @param k the position to fill
     * @param x the item to insert
     */
    private void siftDown(int k, E x) {   //向下调整插入的元素
        if (comparator != null)
            siftDownUsingComparator(k, x);  //使用Comparator比较器方法
        else
            siftDownComparable(k, x);       //元素本身是实现了Comparable
    }

    @SuppressWarnings("unchecked")
    //传入的是，初始插入位置为k，值为x的元素
    private void siftDownComparable(int k, E x) {   //元素本身实现了Comparable
        Comparable<? super E> key = (Comparable<? super E>)x;    //转化为实现了Comparable接口的类，且接口是E及E的父类的
        int half = size >>> 1;        //    //得到队列中间一半的位置索引
        //如果k小于中间位置索引，则循环，因为叶堆的叶子节点索引大于中间的索引。
        //即只要k,为非叶子节点，就循环。
        while (k < half) {
            int child = (k << 1) + 1; // assume left child is least  //得到k位置的孩子左子节点索引，
            Object c = queue[child];   //取得左子节点值
            int right = child + 1;   //取得右子节点的索引，
            if (right < size &&         //如果有右子节点
                    ((Comparable<? super E>) c).compareTo((E) queue[right]) > 0) //比较，如果右子节点值小于左子节点
                c = queue[child = right];    //则c赋值为右子节点，即c会赋值为，左右子节点中较小的数，如果左右相等，取左子节点。
            //使用左右子节点最大的值和传入的元素值进行比较，
            //因为下移，所以直到元素值小于等于当前左右子节点中最小值，说明不需要下移了。
            if (key.compareTo((E) c) <= 0)
                break;
            queue[k] = c;       //如果不满足，则将较小的孩子节点，交换到其父节点位置，即当前的k位置。
            k = child;              //k更新为较小的孩子节点的位置，方便下次判断。
        }
        queue[k] = key;    //找到插入值应该放的位置后，放入元素值。
    }

    @SuppressWarnings("unchecked")
    //和上面类似
    private void siftDownUsingComparator(int k, E x) {
        int half = size >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            Object c = queue[child];
            int right = child + 1;
            if (right < size &&
                    comparator.compare((E) c, (E) queue[right]) > 0)  //比较时使用this.comparator.compare方法比较。
                c = queue[child = right];  //c赋值为较小的孩子节点值
            if (comparator.compare(x, (E) c) <= 0)    //直到插入的节点大于等于当前左右子节点最小值
                break;      //结束下移
            queue[k] = c;
            k = child;
        }
        queue[k] = x;
    }

    /**
     * Establishes the heap invariant (described above) in the entire tree,
     * assuming nothing about the order of the elements prior to the call.
     * 在整个树中建立堆不变量（如上所述），
     *
     * 不假设调用之前元素的顺序。
     */
    @SuppressWarnings("unchecked")
    //将整个堆进行最小堆调整，思想就是从非叶子节点开始进行下移操作
    //倒序操作，说明方法建立最小堆/最大堆是从下到上的。即先让最底层符合最小堆/最大堆结构
    //然后保证上层的下移操作交换的都是当前其子堆中最小/最大的值。
    //只要这样遍历到队列头，每一次都进行下移操作即可实现将整个队列变为优先队列
    //即建立起最小堆/最大堆。
    private void heapify() {
        for (int i = (size >>> 1) - 1; i >= 0; i--) //即从队列中间位置开始遍历
            siftDown(i, (E) queue[i]);       //逐个进行下移操作，
    }

    /**
     * Returns the comparator used to order the elements in this
     * queue, or {@code null} if this queue is sorted according to
     * the {@linkplain Comparable natural ordering} of its elements.
     *返回用来对堆元素进行排序的比较器 comparator，如果按照元素自身的
     * Comparable 排序的话，返回 null。
     * @return the comparator used to order this queue, or
     *         {@code null} if this queue is sorted according to the
     *         natural ordering of its elements
     */
    public Comparator<? super E> comparator() {
        return comparator;
    }

    /**
     * Saves this queue to a stream (that is, serializes it).
     *
     * @serialData The length of the array backing the instance is
     *             emitted (int), followed by all of its elements
     *             (each an {@code Object}) in the proper order.
     * @param s the stream
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        // Write out element count, and any hidden stuff
        s.defaultWriteObject();

        // Write out array length, for compatibility with 1.5 version
        s.writeInt(Math.max(2, size + 1));

        // Write out all elements in the "proper order".
        for (int i = 0; i < size; i++)
            s.writeObject(queue[i]);
    }

    /**
     * Reconstitutes the {@code PriorityQueue} instance from a stream
     * (that is, deserializes it).
     *    序列化操作。
     * @param s the stream
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        // Read in size, and any hidden stuff
        s.defaultReadObject();

        // Read in (and discard) array length
        s.readInt();

        queue = new Object[size];

        // Read in all elements.
        for (int i = 0; i < size; i++)
            queue[i] = s.readObject();

        // Elements are guaranteed to be in "proper order", but the
        // spec has never explained what that might be.
        heapify();
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * queue.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, and {@link Spliterator#NONNULL}.
     * Overriding implementations should document the reporting of additional
     * characteristic values.
     *   //分裂迭代器
     * @return a {@code Spliterator} over the elements in this queue
     * @since 1.8
     */
    public final Spliterator<E> spliterator() {
        return new PriorityQueueSpliterator<E>(this, 0, -1, 0);
    }

    static final class PriorityQueueSpliterator<E> implements Spliterator<E> {
        /*
         * This is very similar to ArrayList Spliterator, except for
         * extra null checks.
         */
        private final PriorityQueue<E> pq;
        private int index;            // current index, modified on advance/split
        private int fence;            // -1 until first use
        private int expectedModCount; // initialized when fence set

        /** Creates new spliterator covering the given range */
        PriorityQueueSpliterator(PriorityQueue<E> pq, int origin, int fence,
                                 int expectedModCount) {
            this.pq = pq;
            this.index = origin;
            this.fence = fence;
            this.expectedModCount = expectedModCount;
        }

        private int getFence() { // initialize fence to size on first use
            int hi;
            if ((hi = fence) < 0) {
                expectedModCount = pq.modCount;
                hi = fence = pq.size;
            }
            return hi;
        }

        public PriorityQueueSpliterator<E> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null :
                    new PriorityQueueSpliterator<E>(pq, lo, index = mid,
                            expectedModCount);
        }

        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super E> action) {
            int i, hi, mc; // hoist accesses and checks from loop
            PriorityQueue<E> q; Object[] a;
            if (action == null)
                throw new NullPointerException();
            if ((q = pq) != null && (a = q.queue) != null) {
                if ((hi = fence) < 0) {
                    mc = q.modCount;
                    hi = q.size;
                }
                else
                    mc = expectedModCount;
                if ((i = index) >= 0 && (index = hi) <= a.length) {
                    for (E e;; ++i) {
                        if (i < hi) {
                            if ((e = (E) a[i]) == null) // must be CME
                                break;
                            action.accept(e);
                        }
                        else if (q.modCount != mc)
                            break;
                        else
                            return;
                    }
                }
            }
            throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            if (action == null)
                throw new NullPointerException();
            int hi = getFence(), lo = index;
            if (lo >= 0 && lo < hi) {
                index = lo + 1;
                @SuppressWarnings("unchecked") E e = (E)pq.queue[lo];
                if (e == null)
                    throw new ConcurrentModificationException();
                action.accept(e);
                if (pq.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        public long estimateSize() {
            return (long) (getFence() - index);
        }

        public int characteristics() {
            return Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL;
        }
    }
}
