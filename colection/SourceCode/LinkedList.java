/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * Doubly-linked list implementation of the {@code List} and {@code Deque}
 * interfaces.  Implements all optional list operations, and permits all
 * elements (including {@code null}).
 *实现了 list 和 Deque 接口的双链表。实现所有可选列表操作，并允许所有元素
 * （包括 null）。
 *
 * <p>All of the operations perform as could be expected for a doubly-linked
 * list.  Operations that index into the list will traverse the list from
 * the beginning or the end, whichever is closer to the specified index.
 *所有操作的执行都符合双链表的预期。列表中索引相关的操作将从头到尾遍历
 *  列表，从而不断靠近指定索引。
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a linked list concurrently, and at least
 * one of the threads modifies the list structurally, it <i>must</i> be
 * synchronized externally.  (A structural modification is any operation
 * that adds or deletes one or more elements; merely setting the value of
 * an element is not a structural modification.)  This is typically
 * accomplished by synchronizing on some object that naturally
 * encapsulates the list.
 *注意，这个实现不是同步（synchronized）的。如果多个线程同时访问一个链表，
 *并且其中至少有一个线程从结构上修改了链表，那么必须在外部同步。（结构
 * 修改是添加或删除一个或多个元素的操作；仅仅设置元素的值并不是从结构上的
 * 修改。）这通常是对一些能自然封装列表的对象进行同步来实现的。
 *
 * If no such object exists, the list should be "wrapped" using the
 * {@link Collections#synchronizedList Collections.synchronizedList}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the list:<pre>
 *   List list = Collections.synchronizedList(new LinkedList(...));</pre>
 *如果不存在这样的对象，应该使用 Collections.synchronizedList 方法。这一
 * 操作最好在创建时完成，以防止意外的异步列表访问：
 * List list = Collections.synchronizedList(new LinkedList(...));
 *
 * <p>The iterators returned by this class's {@code iterator} and
 * {@code listIterator} methods are <i>fail-fast</i>: if the list is
 * structurally modified at any time after the iterator is created, in
 * any way except through the Iterator's own {@code remove} or
 * {@code add} methods, the iterator will throw a {@link
 * ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than
 * risking arbitrary, non-deterministic behavior at an undetermined
 * time in the future.
 *此类的 iterator 和 listIterator 方法返回的迭代器支持 fast-fail：如果在迭代器
 *被创建之后，列表除了迭代器自身的 add 或 remove 方法之外的任何结构性修改，
 * 迭代器都会抛出 ConcurrentModificationException 异常。因此，在面对并发
 * 修改时，迭代器会快速干净地 fail，而不是在将来某个不确定的时间出现不确定
 * 的风险和行为。
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:   <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *注意不能确保迭代器的 fast-fail 行为，通常来说，在存在异步的并发修改情况
 * 下不可能做出任何严格的保证。fast-fail 迭代器以最大的努力抛出
 * ConcurrentModificationException 异常。因此，编写一个依赖此异常来检查
 * 正确性的程序是错误的：迭代器的 fail-fast 行为应该只用于监测 bug。
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *这个类是 Java Collections Framework 的成员。
 * @author  Josh Bloch
 * @see     List
 * @see     ArrayList
 * @since 1.2
 * @param <E> the type of elements held in this collection
 */
// 同时实现双向链表deque接口和List列表接口
    //@saber-01
    //注意链表的很多操作，都要考虑链表是否为空，对于节点的操作，
    //还要考虑到节点是否是头节点或者是尾结点。
    //注意添加和删除都要维护列表中的first和last指针的值。
    //并且要时刻维护链表的双向结构，即维护节点中的prev和next属性
public class LinkedList<E>
        extends AbstractSequentialList<E>
        implements List<E>, Deque<E>, Cloneable, java.io.Serializable
{
    transient int size = 0;// 链表中元素的个数，transient 表示序列化时不包括该变量

    /**
     * Pointer to first node.指向第一个节点的指针
     * Invariant: (first == null && last == null) ||
     *            (first.prev == null && first.item != null)
     */
    transient Node<E> first;

    /**
     * Pointer to last node.  指向链表的最后一个节点的 指针
     * Invariant: (first == null && last == null) ||
     *            (last.next == null && last.item != null)
     */
    transient Node<E> last;

    /**
     * Constructs an empty list.
     * 无参构造函数，创建一个空链表。
     */
    public LinkedList() {
    }

    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *构造函数：构造一个包括指定集合所有元素的列表，按照集合迭代器
     *  返回的顺序。
     *  使用指定集合创建列表，若集合为null，会抛出异常
     * @param  c the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     */
    public LinkedList(Collection<? extends E> c) {
        this();                //实际是调用无参构造函数构建列表后，再调用addAll。
        addAll(c);
    }

    /**
     * Links e as first element.
     *  在表头添加元素。
     */
    private void linkFirst(E e) {
        final Node<E> f = first;   //记录表头节点
        //创建一个新的节点，next赋值为存放的先前头结点f，prev指向null，因为是表头节点
        final Node<E> newNode = new Node<>(null, e, f);
        first = newNode;   //更新列表的头结点。
        if (f == null)      //如果刚开始列表头结点为null，说明列表为空，这时，还要更新last指向新建的头结点。
            //因为如果列表只有一个节点，那么first==last==newNode。
            last = newNode;
        else
            f.prev = newNode;      //创建新节点后，还要将原来头结点f(现在变成第二个节点)的prev指针指向新建的表头节点。
        size++;    //长度加1。
        modCount++;
    }

    /**
     * Links e as last element.
     * 在表尾添加元素
     */
    void linkLast(E e) {
        final Node<E> l = last;   //记录尾结点
        //创建一个新的节点，prev指向先前的尾结点l,next指向null，因为新节点要作为表尾节点。
        final Node<E> newNode = new Node<>(l, e, null);
        last = newNode;       //创建好后，更新列表的表尾节点。
        if (l == null)       //如果一开始表尾节点为null，说明列表为空，
            first = newNode;  //需要将first也赋值为新节点。因为如果列表只有一个节点，那么first==last==newNode。
        else
            l.next = newNode;    //如果不为空，那么先前的表尾节点l(现在变成倒数第二个节点)的next要更新为新建的表尾节点
        size++;   //长度加1。
        modCount++;
    }

    /**
     * Inserts element e before non-null Node succ.
     * 在指定的非空节点 succ 之前插入节点值为E的节点
     */
    void linkBefore(E e, Node<E> succ) {
        // assert succ != null;
        final Node<E> pred = succ.prev;  //先记录指定的非空节点的前一个节点的引用prev
        //新建一个节点值为e的节点，next指向指定的succ节点，pre指向事先记录的节点引用pred。
        final Node<E> newNode = new Node<>(pred, e, succ);
        succ.prev = newNode;             //指定节点的prev更新为插入的新节点
        if (pred == null)            //需要判断指定的节点是否是表头节点，
            first = newNode;      //如果是，则列表记录的表头节点要更新，
        else                    //如果不是表头节点，
            pred.next = newNode;     //则将pred指向的指定节点succ的前一个节点的next指向新插入的节点。
        size++;        //列表元素个数加1
        modCount++;
    }

    /**
     * Unlinks non-null first node f.
     *  移除头结点f，并返回 f 的值
     *
     */
    private E unlinkFirst(Node<E> f) {
        // assert f == first && f != null;
        final E element = f.item;    //先记录f节点的item值用于返回
        final Node<E> next = f.next;       //记录f节点的下一个节点
        f.item = null;        //设置f节点中的属性为null，方便GC
        f.next = null; // help GC
        //因为会删除头结点，所以直接头结点更新为f的下一个节点即可
        first = next;
        if (next == null)    //还要判断f是不是表尾节点，
            last = null;       //如果f是表尾节点， 那么next就为null，这时列表为空，表尾节点也要赋值为null。
        else              //如果f不是表尾节点。
            next.prev = null;       //需要将f的下一个节点的prev属性设置为null，因为它已经变成头结点。
        size--;           //删除头结点后 ，元素个数减一
        modCount++;
        return element;
    }

    /**
     * Unlinks non-null last node l.
     * 删除表尾节点。并返回表尾节点值。
     */
    private E unlinkLast(Node<E> l) {
        // assert l == last && l != null;
        final E element = l.item;  // 先记录表尾节点的item值，用于返回
        final Node<E> prev = l.prev;     //记录表尾节点的前一个节点
        l.item = null;         //将表尾节点中的属性设置为null，方便GC
        l.prev = null; // help GC
        last = prev;              //删除的是尾结点，所以列表的尾结点直接更新为l的前一个节点。
        if (prev == null)    // 要判断l是不是表头节点，即判断原先列表是不是只有一个节点
            first = null;        //如果原先列表只有一个节点，删除后，列表为空，表头节点也要设置为null。
        else
            prev.next = null;   //如果l不是尾结点，则需要将l的上一个节点的next属性更新为null，因为它变成了尾结点。
        size--;//删除尾结点后 ，元素个数减一
        modCount++;
        return element;
    }

    /**
     * Unlinks non-null node x.
     * 删除一个非空节点 x，并返回删除节点的值，
     * 考虑了 节点是 表头节点/表尾节点的情况。
     */
    E unlink(Node<E> x) {
        // assert x != null;   //确定节点不为null
        final E element = x.item;    // 记录节点item值用于返回
        final Node<E> next = x.next;   //记录节点的下一个节点
        final Node<E> prev = x.prev;   //记录节点的上一个节点

        if (prev == null) {        //如果上一个节点为null，说明x为表头节点，
            first = next;             //删除就是将表头节点直接定义成x的下一个节点。
        } else {             //如果x存在上一个节点，
            prev.next = next;      //则上一个节点的next要更新为x的下一个节点，
            x.prev = null;           //x.prev属性利用完成赋值为null，方便GC
        }

        if (next == null) {    //如果下一个节点为null，即x为表尾节点，
            last = prev;          //则删除会更新表尾节点，即表尾节点直接赋值为x的上一个节点
        } else {                 //如果x存在下一个节点，
            next.prev = prev;     //则下一个节点的prev属性更新成x的上一个节点。
            x.next = null;             //x.next属性利用完成赋值为null，方便GC
        }

        x.item = null;       //将item赋值为null，方便GC
        size--;     //删除一个节点，所以列表size--
        modCount++;
        return element;
    }

    /**
     * Returns the first element in this list.
     *返回列表的第一个元素。
     * 双端队列的实现
     * @return the first element in this list
     * @throws NoSuchElementException if this list is empty
     */
    public E getFirst() {
        final Node<E> f = first;  //直接获取列表的first头结点
        if (f == null)    //如果列表为空
            throw new NoSuchElementException();  //抛出异常
        return f.item; //正常则返回头结点的item
    }

    /**
     * Returns the last element in this list.
     *返回列表的最后一个元素
     *  双端队列的实现
     * @return the last element in this list
     * @throws NoSuchElementException if this list is empty
     */
    public E getLast() {
        final Node<E> l = last;  //直接获取列表的last表尾节点
        if (l == null)       //如果列表为空
            throw new NoSuchElementException();  //抛出异常
        return l.item;    //正常则返回表尾节点的item
    }

    /**
     * Removes and returns the first element from this list.
     *移除并返回列表的第一个元素
     * 双端队列的实现
     * @return the first element from this list
     * @throws NoSuchElementException if this list is empty
     */
    public E removeFirst() {
        final Node<E> f = first;
        if (f == null)  //判断头结点是否为空
            throw new NoSuchElementException(); //为空抛出异常
        return unlinkFirst(f);  //正常调用unlinkFirst
    }

    /**
     * Removes and returns the last element from this list.
     *移除并返回列表的最后一个元素
     * 双端队列的实现
     * @return the last element from this list
     * @throws NoSuchElementException if this list is empty
     */
    public E removeLast() {
        final Node<E> l = last;
        if (l == null)//判断尾节点是否为空
            throw new NoSuchElementException();//为空抛出异常
        return unlinkLast(l); //正常调用unlinkLast
    }

    /**
     * Inserts the specified element at the beginning of this list.
     * 在列表头添加指定元素
     * 双端队列的实现
     * @param e the element to add
     */
    public void addFirst(E e) {
        linkFirst(e);
    }

    /**
     * Appends the specified element to the end of this list.
     *在列表尾添加指定元素
     * 双端队列的实现
     * <p>This method is equivalent to {@link #add}.
     *此方法等价于 add。
     * @param e the element to add
     */
    public void addLast(E e) {
        linkLast(e);
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     * More formally, returns {@code true} if and only if this list contains
     * at least one element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *如果列表包含指定元素返回 true。
     *
     * @param o element whose presence in this list is to be tested
     * @return {@code true} if this list contains the specified element
     */
    public boolean contains(Object o) {  //调用的是indexOf判断，如果不存在返回索引就是-1,存在则！=-1。
        return indexOf(o) != -1;
    }

    /**
     * Returns the number of elements in this list.
     *返回列表元素个数。
     * @return the number of elements in this list
     */
    public int size() {
        return size;
    }

    /**
     * Appends the specified element to the end of this list.
     *在列表尾部添加元素
     *
     * <p>This method is equivalent to {@link #addLast}.
     * 此方法等同于 addLast
     * @param e element to be appended to this list
     * @return {@code true} (as specified by {@link Collection#add})
     */
    public boolean add(E e) {
        linkLast(e);
        return true;
    }

    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present.  If this list does not contain the element, it is
     * unchanged.  More formally, removes the element with the lowest index
     * {@code i} such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * (if such an element exists).  Returns {@code true} if this list
     * contained the specified element (or equivalently, if this list
     * changed as a result of the call).
     *如果指定元素存在，移除列表中和指定元素相等的第一个元素。如果
     *  列表不包括该元素，不做任何变化。
     *  如果列表发生了改变，即列表包含了指定元素，返回true。
     * @param o element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element
     */
    public boolean remove(Object o) {
        if (o == null) {             //如果传入对象为null
            for (Node<E> x = first; x != null; x = x.next) {     //从表头开始遍历链表，
                if (x.item == null) {           //遇到节点item==null，执行
                    unlink(x);                 //调用unlink方法删除节点
                    return true;
                }
            }
        } else {//如果传入对象不为null
            for (Node<E> x = first; x != null; x = x.next) {   //从表头开始遍历链表
                if (o.equals(x.item)) {//使用传入对象的equals方法判断相等
                    unlink(x);  //调用unlink方法删除节点
                    return true;
                }
            }
        }
        return false;   //如果以上过程没返回，则说明未找到，返回false。
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the specified
     * collection's iterator.  The behavior of this operation is undefined if
     * the specified collection is modified while the operation is in
     * progress.  (Note that this will occur if the specified collection is
     * this list, and it's nonempty.)
     *把指定集合中所有元素，按照集合迭代器返回的顺序，添加到列表的
     * 末尾。在这个操作（方法）进行时，指定集合被修改了，则此操作的
     * 行为是不确定的。（注意：如果指定的集合是当前列表，并且它是非空的，
     *  就会发生这种情况）
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     */
    public boolean addAll(Collection<? extends E> c) {
        return addAll(size, c);     //调用的是add(index,c)方法，在size处就是在列表尾巴添加
    }

    /**
     * Inserts all of the elements in the specified collection into this
     * list, starting at the specified position.  Shifts the element
     * currently at that position (if any) and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in the list in the order that they are returned by the
     * specified collection's iterator.
     *将参数集合中的所有元素插入到指定位置。将原来位置及其之后的
     * 元素后移（索引相应增加）。添加的顺序为参数集合迭代器返回的
     *  顺序。
     * @param index index at which to insert the first element
     *              from the specified collection
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException if the specified collection is null
     */
    //注意：在表头和表尾拆入，即index=0，index=size时，情况下要维护
    //first和last的值。
    public boolean addAll(int index, Collection<? extends E> c) {
        checkPositionIndex(index);     //  检查索引合法性。

        Object[] a = c.toArray();    //将集合c转化为数组。
        int numNew = a.length;     //取得集合c的长度。
        if (numNew == 0)                 //如果长度为0，即不会发生改变，返回false。
            return false;
          //需要保存节点的位置，方便插入以后链表的连接。
        Node<E> pred, succ;            //pred存储位置的前一个节点，succ存储插入位置的一个节点。
        if (index == size) {   //如果要插入的位置是size，即在表尾添加集合，需要改变尾结点。
            succ = null;         //插入位置size没有元素，所以为null
            pred = last;      //size-1位置是表尾，所以pred为尾节点last
        } else {         //如果不是尾部插入
            succ = node(index);     //  赋值为插入位置的节点。
            pred = succ.prev;     //赋值为插入位置的前一个节点。
        }

        for (Object o : a) {      //遍历数组a,也就是集合c转化来的数组
            @SuppressWarnings("unchecked") E e = (E) o;     //转化元素值类型为E
            //新建节点，pred保存了上一次循环中加入的新节点，
            //本次添加的节点的prev显然指向上一次次先添加的节点pred。
            //item为本次遍历到的值e。
            Node<E> newNode = new Node<>(pred, e, null);
            //如果pred为null,只会出现在addAll传入的index为0时，即从表头位置插入，则需要维护first
            //并且是插入时的首次循环，遇到。
            if (pred == null)
                first = newNode;      //那么表头节点更新为首次循环插入的新节点。
            else        //不是从0开始插入，或者非首次循环。
                pred.next = newNode;     //将上一个节点的next赋值为当前节点。
            pred = newNode;    //一次循环结束，需要更新pred的值，使得在每次循环中都记录着上次循环的节点。
        }
           //头节点维护完后，还要维护last尾节点
        if (succ == null) {   //如果插入位置值为null，即在表尾后size处插入，
            last = pred;       //则将尾节点，设置为上面循环记录的最后一个节点，即插入的最后一个节点。
        } else {      //如果非表尾后插入，则要实现双向连接。
            pred.next = succ;    //将最后一次插入的节点的next值赋值为原来记录的插入位置处的节点，
            succ.prev = pred;  //反过来，原来插入位置处的节点的prev指针也要指向插入的最后一个节点。
        }

        size += numNew;    //元素个数添加了集合所含数量numNew
        modCount++;
        return true;
    }

    /**
     * Removes all of the elements from this list.
     * The list will be empty after this call returns.
     * 删除列表中所有元素。
     *   调用此方法之后，列表将为空。
     */
    public void clear() {
        // Clearing all of the links between nodes is "unnecessary", but:
        // - helps a generational GC if the discarded nodes inhabit
        //   more than one generation
        // - is sure to free memory even if there is a reachable Iterator
        for (Node<E> x = first; x != null; ) {    //从头节点开始遍历。
            Node<E> next = x.next;
            x.item = null;     //方便GC。
            x.next = null;
            x.prev = null;
            x = next;   //x指向下一个节点。
        }
        first = last = null;     //删除完毕后，重置first和last值为null
        size = 0;    //size变为0
        modCount++;
    }


    // Positional Access Operations
    // 基于位置的访问操作，与list相关。
    /**
     * Returns the element at the specified position in this list.
     * 返回列表中指定位置的元素
     * @param index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E get(int index) {
        checkElementIndex(index);      //检查索引合法性。
        return node(index).item;      //通过node（index）获得节点，再返回节点的item值
    }

    /**
     * Replaces the element at the specified position in this list with the
     * specified element.
     *把列表中指定位置的元素值设置为特定的对象，并返回原来的值
     *  //注意并没有新建节点，只是修改节点上的值。
     * @param index index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E set(int index, E element) {
        checkElementIndex(index);  //检查节点的合法值。
        Node<E> x = node(index);   //保存位置的节点
        E oldVal = x.item;     // 将旧值存储起来
        x.item = element;     //将节点中的值更改为新的值element
        return oldVal;   //返回旧值
    }

    /**
     * Inserts the specified element at the specified position in this list.
     * Shifts the element currently at that position (if any) and any
     * subsequent elements to the right (adds one to their indices).
     *在列表中指定位置插入特定元素。把当前位置的元素和后续的所有
     *   元素右移一位（索引加一）。
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public void add(int index, E element) {
        checkPositionIndex(index);  //检查索引的合法性，允许在size处插入

        if (index == size)           //index等于size的话即在表尾插入，调用linkLast
            linkLast(element);
        else             //如果正常插入，先用node方法获得节点再调用linkBefore 在指定索引的节点前插入新节点。
            linkBefore(element, node(index));
    }

    /**
     * Removes the element at the specified position in this list.  Shifts any
     * subsequent elements to the left (subtracts one from their indices).
     * Returns the element that was removed from the list.
     *移除列表中指定位置的元素。把当前位置的元素和后续的所有
     *元素左移一位（索引减一）。最后返回从列表中移除的元素。
     * @param index the index of the element to be removed
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E remove(int index) {   //指定位置移除
        checkElementIndex(index);//检查索引合法性
        return unlink(node(index));     //先调用node方法获得节点，再用unlink删除
    }

    /**
     * Tells if the argument is the index of an existing element.
     * 判断索引是不是列表中元素的索引。
     */
    private boolean isElementIndex(int index) {
        return index >= 0 && index < size;
    }

    /**
     * Tells if the argument is the index of a valid position for an
     * iterator or an add operation.
     *判断索引对于迭代器和add操作是否是合法的。
     */
    private boolean isPositionIndex(int index) {
        return index >= 0 && index <= size;
    }

    /**
     * Constructs an IndexOutOfBoundsException detail message.
     * Of the many possible refactorings of the error handling code,
     * this "outlining" performs best with both server and client VMs.
     *  出现 IndexOutOfBoundsException 异常时的详细信息。
     *    在错误处理代码许多可能的重构中，这种“描述”对服务器和客户端
     *    虚拟机都表现最好。
     */
    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size;
    }

    private void checkElementIndex(int index) {  //检查索引的合法性，取元素值版本的
        if (!isElementIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));  //不满足抛出异常
    }

    private void checkPositionIndex(int index) {    //检查索引的合法性。add和迭代器版本的
        if (!isPositionIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));//不满足抛出异常
    }

    /**
     * Returns the (non-null) Node at the specified element index.
     * 返回指定索引处的节点
     */
    Node<E> node(int index) {
        // assert isElementIndex(index);
        //确定调用时，index是合法的
        //注意因为是双向链表，可以从表头或者表尾都可以进行遍历
        //为了更快的遍历，肯定是选择更靠近索引index的那一端开始遍历
        //即看index是落在链表的哪一半部分。
        if (index < (size >> 1)) {     //如果index落在前半部分，
            Node<E> x = first;    //则从头结点开始遍历。
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else {                        //如果index落在后半部分
            Node<E> x = last;             //则从尾结点开始遍历。
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }

    // Search Operations
    // 查询操作
    /**
     * Returns the index of the first occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the lowest index {@code i} such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     *返回列表中第一次出现指定元素的索引，如果不包含该元素返回 -1。
     *
     * @param o element to search for
     * @return the index of the first occurrence of the specified element in
     *         this list, or -1 if this list does not contain the element
     */
    public int indexOf(Object o) {
        int index = 0;      //用于返回
        //同样传入的是对象，就需要判断o是否为null，而使用不同方式判断相等。
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null)   //从first遍历第一次碰到就返回此处index。
                    return index;
                index++;         //index随着循环不断更新
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item))         //和上面过程类似，只是这里用对象的equals方法来判断相等
                    return index;
                index++;
            }
        }
        return -1;    //未找到返回-1；
    }

    /**
     * Returns the index of the last occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the highest index {@code i} such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     *返回列表中最后一次出现指定元素的索引，如果不包含该元素返回 -1。
     * @param o element to search for
     * @return the index of the last occurrence of the specified element in
     *         this list, or -1 if this list does not contain the element
     */
    //和上面的indexOf类似，只是遍历时候从尾结点开始使用prev遍历
    //并且index初始为size，不断递减，
    public int lastIndexOf(Object o) {
        int index = size;
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;    //index--在判断前，所以初始是size，而不是size-1；
                if (x.item == null)
                    return index;
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (o.equals(x.item))
                    return index;
            }
        }
        return -1;
    }

    // Queue operations.
    // Queue 相关操作
    /**
     * Retrieves, but does not remove, the head (first element) of this list.
     *检索但不删除列表的头元素（第一个元素）
     *  不会抛出任何异常
     *  区别element，其会抛出异常。
     *  方法等同于peekFirst
     * @return the head of this list, or {@code null} if this list is empty
     * @since 1.5
     */
    public E peek() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;//头结点为空返回null，不为空，返回头结点item值
    }

    /**
     * Retrieves, but does not remove, the head (first element) of this list.
     *检索但不删除链表的头部（第一个元素）
     *  列表为空抛出异常
     *  注意，会抛出异常
     * @return the head of this list
     * @throws NoSuchElementException if this list is empty
     * @since 1.5
     */
    public E element() {      //方法等同于getFirst
        return getFirst();
    }

    /**
     * Retrieves and removes the head (first element) of this list.
     *检索并移除链表的头元素（第一个元素）
     * 列表为空时返回 null，不抛出异常
     * 等同于pollFirst
     * @return the head of this list, or {@code null} if this list is empty
     * @since 1.5
     */
    public E poll() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }

    /**
     * Retrieves and removes the head (first element) of this list.
     *检索并删除列表的头元素（第一个元素）
     *  列表为空抛出异常
     *  方法等同于removeFirst
     * @return the head of this list
     * @throws NoSuchElementException if this list is empty
     * @since 1.5
     */
    public E remove() {
        return removeFirst();
    }

    /**
     * Adds the specified element as the tail (last element) of this list.
     * 把指定元素添加到列表末尾
     * 等同于offerLast 以及 add
     * @param e the element to add
     * @return {@code true} (as specified by {@link Queue#offer})
     * @since 1.5
     */
    public boolean offer(E e) {
        return add(e);
    }

    // Deque operations
    // Deque 相关操作
    /**
     * Inserts the specified element at the front of this list.
     * 把指定元素添加到列表头部
     *
     * @param e the element to insert
     * @return {@code true} (as specified by {@link Deque#offerFirst})
     * @since 1.6
     */
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    /**
     * Inserts the specified element at the end of this list.
     * 把指定元素添加到列表尾部
     * @param e the element to insert
     * @return {@code true} (as specified by {@link Deque#offerLast})
     * @since 1.6
     */
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    /**
     * Retrieves, but does not remove, the first element of this list,
     * or returns {@code null} if this list is empty.
     *检索但不删除列表的第一个元素。
     * 如果列表为空返回 null，不会抛出异常。
     * @return the first element of this list, or {@code null}
     *         if this list is empty
     * @since 1.6
     */
    public E peekFirst() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
    }

    /**
     * Retrieves, but does not remove, the last element of this list,
     * or returns {@code null} if this list is empty.
     *检索但不删除列表的最后一个元素，列表为空返回 null。
     *
     * @return the last element of this list, or {@code null}
     *         if this list is empty
     * @since 1.6
     */
    public E peekLast() {
        final Node<E> l = last;
        return (l == null) ? null : l.item;
    }

    /**
     * Retrieves and removes the first element of this list,
     * or returns {@code null} if this list is empty.
     *检索并删除列表的第一个元素，如果列表为空返回 null。
     * @return the first element of this list, or {@code null} if
     *     this list is empty
     * @since 1.6
     */
    public E pollFirst() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }

    /**
     * Retrieves and removes the last element of this list,
     * or returns {@code null} if this list is empty.
     *检索并删除列表的最后一个元素，如果列表为空返回 null。
     * @return the last element of this list, or {@code null} if
     *     this list is empty
     * @since 1.6
     */
    public E pollLast() {
        final Node<E> l = last;
        return (l == null) ? null : unlinkLast(l);
    }

    /**
     * Pushes an element onto the stack represented by this list.  In other
     * words, inserts the element at the front of this list.
     * 将指定元素 push 到列表所代表的的栈里，即插入到列表表头。
     *
     * <p>This method is equivalent to {@link #addFirst}.
     *
     * @param e the element to push
     * @since 1.6
     */
    public void push(E e) {
        addFirst(e);
    }

    /**
     * Pops an element from the stack represented by this list.  In other
     * words, removes and returns the first element of this list.
     *从列表所代表的栈里 pop 出一个元素，即移除并返回列表的第一个元素。
     * 如果列表为空抛出异常。
     * <p>This method is equivalent to {@link #removeFirst()}.
     *
     * @return the element at the front of this list (which is the top
     *         of the stack represented by this list)
     * @throws NoSuchElementException if this list is empty
     * @since 1.6
     */
    public E pop() {
        return removeFirst();
    }

    /**
     * Removes the first occurrence of the specified element in this
     * list (when traversing the list from head to tail).  If the list
     * does not contain the element, it is unchanged.
     *移除列表中和指定元素匹配的第一个元素（从头到尾遍历）。如果
     *  列表不包含该元素则不做出任何改变。
     * @param o element to be removed from this list, if present
     * @return {@code true} if the list contained the specified element
     * @since 1.6
     */
    public boolean removeFirstOccurrence(Object o) { //该方法显然和remove等价。都是从头到尾，删除匹配的第一个元素
        return remove(o);
    }

    /**
     * Removes the last occurrence of the specified element in this
     * list (when traversing the list from head to tail).  If the list
     * does not contain the element, it is unchanged.
     *移除列表中最后一个和指定元素匹配的元素（从头到尾遍历）。如果
     * 列表不包含该元素则不做任何改变。
     * @param o element to be removed from this list, if present
     * @return {@code true} if the list contained the specified element
     * @since 1.6
     */
    //和remove类似，只是，从尾部last开始遍历，
    // 同样分成两种情况，一种是指定元素为空，一种是不为空。
    // 如果不分，那么当指定元素为空时，访问 x.item 抛出异常
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;     //同样没找到就返回false。
    }

    /**
     * Returns a list-iterator of the elements in this list (in proper
     * sequence), starting at the specified position in the list.
     * Obeys the general contract of {@code List.listIterator(int)}.<p>
     *返回列表的按序迭代器，从指定元素开始。遵守 List.listIterator(int)
     *的基本规范。
     * The list-iterator is <i>fail-fast</i>: if the list is structurally
     * modified at any time after the Iterator is created, in any way except
     * through the list-iterator's own {@code remove} or {@code add}
     * methods, the list-iterator will throw a
     * {@code ConcurrentModificationException}.  Thus, in the face of
     * concurrent modification, the iterator fails quickly and cleanly, rather
     * than risking arbitrary, non-deterministic behavior at an undetermined
     * time in the future.
     *列表迭代器支持 fail-fast：在迭代器创建后任何时间列表被结构性修改，
     *  除非是列表自身的 remove 或者 add 方法，否则迭代器会抛出
     * ConcurrentModificationException 异常。因此，在面对并发修改的
     *  时候，迭代器会快速干净地失败，而不会在未来不确定的时间出现未知
     *  的风险和未定义的行为
     * @param index index of the first element to be returned from the
     *              list-iterator (by a call to {@code next})
     * @return a ListIterator of the elements in this list (in proper
     *         sequence), starting at the specified position in the list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @see List#listIterator(int)
     */
    public ListIterator<E> listIterator(int index) {
        checkPositionIndex(index);  //检查索引的合法性
        return new ListItr(index);              //返回内部类ListItr
    }

    private class ListItr implements ListIterator<E> {
        //因为没有了底层数组，需要使用节点引用来记录。
        private Node<E> lastReturned;  //最后一次遍历到的节点，类比lastRet
        private Node<E> next;       //类比cursor
        private int nextIndex;
        private int expectedModCount = modCount;
        //构造函数中含有index，即表示nextIndex初始值设为index
        ListItr(int index) {
            // assert isPositionIndex(index);
            next = (index == size) ? null : node(index);   //如果传入为size，直接为空，迭代器没有下一个节点。否则得到index当前节点。
            nextIndex = index;
        }

        public boolean hasNext() { // 后面是否还有元素
            return nextIndex < size;
        }

        public E next() {       //返回后一个元素的值，并更新next和nextIndex，以及lastReturned
            checkForComodification();
            if (!hasNext())               //没有元素了会抛出异常
                throw new NoSuchElementException();

            lastReturned = next;
            next = next.next;
            nextIndex++;
            return lastReturned.item;
        }

        public boolean hasPrevious() {         //前面是否还有元素
            return nextIndex > 0;
        }

        public E previous() {    //    返回next节点前一个元素值，并更新next和nextIndex，以及lastReturned
            checkForComodification();
            if (!hasPrevious())//没有元素了会抛出异常
                throw new NoSuchElementException();
            //next已到末尾，则previous为尾结点，否则为next.prev
            lastReturned = next = (next == null) ? last : next.prev;
            nextIndex--;  //向前移动一位
            return lastReturned.item;
        }

        public int nextIndex() {   //返回下一个元素的索引
            return nextIndex;
        }

        public int previousIndex() {   //返回前一个元素的索引
            return nextIndex - 1;
        }

        public void remove() {      //迭代器的remove方法
            checkForComodification();     //检查并发修改合法性
            if (lastReturned == null)       //注意删除时删除lastReturned，即最后一次遍历到的节点。
                throw new IllegalStateException();   //如果删除为空，则会抛出异常

            Node<E> lastNext = lastReturned.next;       //记录要删除的节点后的一个节点，方便删除后连接
            unlink(lastReturned);              //删除lastReturned,
            if (next == lastReturned)       //如果才调用了previous方法则，next会等于astReturned。
                next = lastNext;           //那么删除以后，需要next指向事先保存的下一个节点。
            else                 //大部分情况下，next.prev会等于lastReturned。
                nextIndex--;                  //删除了一个节点，索引减一。
            lastReturned = null;           //执行删除以后，lastReturned重置为null。
            expectedModCount++;
        }

        public void set(E e) {      //迭代器set方法  。注意还是操作的lastReturned上的值
            if (lastReturned == null)                  //lastReturned为null，则抛出异常
                throw new IllegalStateException();
            checkForComodification();   //检查并发修改一致性
            lastReturned.item = e;     //只修改最后一次遍历的节点中的item值
        }

        public void add(E e) {         //迭代器add方法。
            checkForComodification();   // 检查并发修改
            lastReturned = null;  //add和remove会初始化lastReturned的值为null
            if (next == null)     //如果当前next为null，说明遍历到尾，尾部添加直接调用linkLast
                linkLast(e);
            else
                linkBefore(e, next);  //正常情况，则调用，linkBefore在指定节点前添加。
            nextIndex++;     //next不变，但是nextIndex需要更新加1
            expectedModCount++;
        }

        public void forEachRemaining(Consumer<? super E> action) {  //  迭代器支持lambda表达式
            Objects.requireNonNull(action);
            while (modCount == expectedModCount && nextIndex < size) {
                action.accept(next.item);                //遍历，对每个item执行action操作
                lastReturned = next;
                next = next.next;
                nextIndex++;
            }
            checkForComodification();
        }

        final void checkForComodification() {    //检查是否发生并发修改
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    private static class Node<E> {      //链表的节点类
        E item;       //存储元素值
        Node<E> next;     //上一个节点的引用
        Node<E> prev;     //下一个节点的引用

        Node(Node<E> prev, E element, Node<E> next) {     //构造函数。
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }

    /**
     * @since 1.6
     * 返回按照降序排列的迭代器
     */
    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    /**
     * Adapter to provide descending iterators via ListItr.previous
     *  适配器通过 ListItr.previous 提供降序迭代器。
     */
    private class DescendingIterator implements Iterator<E> {
        private final ListItr itr = new ListItr(size());
        public boolean hasNext() {
            return itr.hasPrevious();
        }
        public E next() {
            return itr.previous();
        }
        public void remove() {
            itr.remove();
        }
    }

    @SuppressWarnings("unchecked")
    private LinkedList<E> superClone() {
        try {
            return (LinkedList<E>) super.clone();   //调用object的clone方法。
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Returns a shallow copy of this {@code LinkedList}. (The elements
     * themselves are not cloned.)
     *返回 LinkedList 的浅拷贝，元素的值没有拷贝，但是节点不是原来的
     * 节点。
     * @return a shallow copy of this {@code LinkedList} instance
     */
    public Object clone() {
        LinkedList<E> clone = superClone();  //object方法返回的副本，内部属性还是原来保存的值

        // Put clone into "virgin" state
        clone.first = clone.last = null;       //初始化内部属性。
        clone.size = 0;
        clone.modCount = 0;

        // Initialize clone with our elements
        for (Node<E> x = first; x != null; x = x.next)  //遍历当前链表，对每一个节点进行添加。
            clone.add(x.item);

        return clone;
    }

    /**
     * Returns an array containing all of the elements in this list
     * in proper sequence (from first to last element).
     *按序（从第一个到最后一个元素）返回一个包含列表中所有元素的数组。
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this list.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *返回的数组是安全的，列表不会保留对它的任何引用。（换句话说，
     * 此方法会重新分配一个数组空间）。因此调用者可以对返回的数组进行
     *  任意修改。
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this list
     *         in proper sequence
     */
    public Object[] toArray() {
        Object[] result = new Object[size];  //创建一个可以容纳size个元素的Object数组
        int i = 0;
        for (Node<E> x = first; x != null; x = x.next)    //循环遍历，将每个节点item取出赋值。
            result[i++] = x.item;
        return result;
    }

    /**
     * Returns an array containing all of the elements in this list in
     * proper sequence (from first to last element); the runtime type of
     * the returned array is that of the specified array.  If the list fits
     * in the specified array, it is returned therein.  Otherwise, a new
     * array is allocated with the runtime type of the specified array and
     * the size of this list.
     *按序（从第一个到最后一个元素）返回一个包含列表中所有元素的数组；
     * 返回数组的运行时类型是指定数组的类型。
     *  如果列表适合指定的数组，则返回到指定数组中（指定数组长度和
     *   列表 size 大小相等）否则一个以指定数组的类型为运行类型，大小为
     *   列表 size 的新数组将被分配新的空间。
     * <p>If the list fits in the specified array with room to spare (i.e.,
     * the array has more elements than the list), the element in the array
     * immediately following the end of the list is set to {@code null}.
     * (This is useful in determining the length of the list <i>only</i> if
     * the caller knows that the list does not contain any null elements.)
     * 如果列表适合指定的数组并且还有剩余空间（即指定数组比列表有更多
     *  的元素），在数组中紧跟集合末尾的元素被设置为 null。（仅当调用者
     *  知道列表中不包含任何 null 元素，在决定列表长度时才是有用的）
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *这个方法充当基于数组和基于集合API的桥梁（集合与数组的转换）。
     * 此外，该方法允许精确控制输出数组的运行时类型，并且在某些情况
     * 下可以用于节省分配成本。
     * <p>Suppose {@code x} is a list known to contain only strings.
     * The following code can be used to dump the list into a newly
     * allocated array of {@code String}:
     *
     * <pre>
     *     String[] y = x.toArray(new String[0]);</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the list are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing the elements of the list
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this list
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size)  //如果目标数组长度不够，
            a = (T[])java.lang.reflect.Array.newInstance( //创建一个以指定数组类型为运行类型的长度为size的新数组。
                    a.getClass().getComponentType(), size);
        int i = 0;
        Object[] result = a;
        for (Node<E> x = first; x != null; x = x.next)       //遍历链表，将各个节点的item取出赋值
            result[i++] = x.item;

        if (a.length > size)     //如果目标数组的长度大于size，则需要将紧跟集合末尾的位置赋值为null。
            a[size] = null;

        return a;
    }

    private static final long serialVersionUID = 876323262645176354L;

    /**
     * Saves the state of this {@code LinkedList} instance to a stream
     * (that is, serializes it).
     *保存链表实例的状态，输入到stream中，即序列化它
     * @serialData The size of the list (the number of elements it
     *             contains) is emitted (int), followed by all of its
     *             elements (each an Object) in the proper order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        // Write out any hidden serialization magic
        // 写出所有隐藏的东西。
        s.defaultWriteObject();

        // Write out size
        //写出size。
        s.writeInt(size);

        // Write out all elements in the proper order.
        //遍历写出每一个节点值。
        for (Node<E> x = first; x != null; x = x.next)
            s.writeObject(x.item);
    }

    /**
     * Reconstitutes this {@code LinkedList} instance from a stream
     * (that is, deserializes it).
     * 反序列化，从stream中读取一个链表实例
     */
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        // Read in any hidden serialization magic
        //读入隐藏的东西
        s.defaultReadObject();

        // Read in size
        //读入size
        int size = s.readInt();

        // Read in all elements in the proper order.
        //读入stream中的item值，并调用linkLast进行连接。
        for (int i = 0; i < size; i++)
            linkLast((E)s.readObject());
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * list.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED} and
     * {@link Spliterator#ORDERED}.  Overriding implementations should document
     * the reporting of additional characteristic values.
     *
     * @implNote
     * The {@code Spliterator} additionally reports {@link Spliterator#SUBSIZED}
     * and implements {@code trySplit} to permit limited parallelism..
     *
     * @return a {@code Spliterator} over the elements in this list
     * @since 1.8
     */
    @Override
    public Spliterator<E> spliterator() {
        return new LLSpliterator<E>(this, -1, 0);
    }

    /** A customized variant of Spliterators.IteratorSpliterator */
    //分裂迭代器。
    static final class LLSpliterator<E> implements Spliterator<E> {
        static final int BATCH_UNIT = 1 << 10;  // batch array size increment
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final LinkedList<E> list; // null OK unless traversed
        Node<E> current;      // current node; null until initialized
        int est;              // size estimate; -1 until first needed
        int expectedModCount; // initialized when est set
        int batch;            // batch size for splits

        LLSpliterator(LinkedList<E> list, int est, int expectedModCount) {
            this.list = list;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getEst() {
            int s; // force initialization
            final LinkedList<E> lst;
            if ((s = est) < 0) {
                if ((lst = list) == null)
                    s = est = 0;
                else {
                    expectedModCount = lst.modCount;
                    current = lst.first;
                    s = est = lst.size;
                }
            }
            return s;
        }

        public long estimateSize() { return (long) getEst(); }

        public Spliterator<E> trySplit() {
            Node<E> p;
            int s = getEst();
            if (s > 1 && (p = current) != null) {
                int n = batch + BATCH_UNIT;
                if (n > s)
                    n = s;
                if (n > MAX_BATCH)
                    n = MAX_BATCH;
                Object[] a = new Object[n];
                int j = 0;
                do { a[j++] = p.item; } while ((p = p.next) != null && j < n);
                current = p;
                batch = j;
                est = s - j;
                return Spliterators.spliterator(a, 0, j, Spliterator.ORDERED);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Node<E> p; int n;
            if (action == null) throw new NullPointerException();
            if ((n = getEst()) > 0 && (p = current) != null) {
                current = null;
                est = 0;
                do {
                    E e = p.item;
                    p = p.next;
                    action.accept(e);
                } while (p != null && --n > 0);
            }
            if (list.modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) throw new NullPointerException();
            if (getEst() > 0 && (p = current) != null) {
                --est;
                E e = p.item;
                current = p.next;
                action.accept(e);
                if (list.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

}
