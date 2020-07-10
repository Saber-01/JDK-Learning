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
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Resizable-array implementation of the <tt>List</tt> interface.  Implements
 * all optional list operations, and permits all elements, including
 * <tt>null</tt>.  In addition to implementing the <tt>List</tt> interface,
 * this class provides methods to manipulate the size of the array that is
 * used internally to store the list.  (This class is roughly equivalent to
 * <tt>Vector</tt>, except that it is unsynchronized.)
 *译文：List接口的大小可变数组的实现.  实现了所有可选列表操作，允许所有元素类型，
 *  包括null。除了实现List接口外，这个类还提供了方法用来操作内部存储列表的
 *   数组的大小。 这个类大致等同于Vector，除了此类是不同步的。
 *
 * <p>The <tt>size</tt>, <tt>isEmpty</tt>, <tt>get</tt>, <tt>set</tt>,
 * <tt>iterator</tt>, and <tt>listIterator</tt> operations run in constant
 * time.  The <tt>add</tt> operation runs in <i>amortized constant time</i>,
 * that is, adding n elements requires O(n) time.  All of the other operations
 * run in linear time (roughly speaking).  The constant factor is low compared
 * to that for the <tt>LinkedList</tt> implementation.
 *译文：size, isEmpty, get, set, iterator和listIterator操作在常数时间内完成，add
 *  操作在分摊的常数时间内完成，即添加n个元素需要O(n)时间。除此之外，
 *  其他操作在线性时间内完成。该类的常数因子比实现LinkedList的常数因子要低。
 *
 * <p>Each <tt>ArrayList</tt> instance has a <i>capacity</i>.  The capacity is
 * the size of the array used to store the elements in the list.  It is always
 * at least as large as the list size.  As elements are added to an ArrayList,
 * its capacity grows automatically.  The details of the growth policy are not
 * specified beyond the fact that adding an element has constant amortized
 * time cost.
 *译文：每一个ArrayList实例都有一个容量。这个容量是用来储存元素的数组大小。它
 *   至少等于列表的大小。当元素被添加到ArrayList之后，它的容量自动增长。
 *   没有固定的增长策略，因为这不仅仅只是添加元素会带来分摊固定时间开销那样
 *   简单。
 *
 * <p>An application can increase the capacity of an <tt>ArrayList</tt> instance
 * before adding a large number of elements using the <tt>ensureCapacity</tt>
 * operation.  This may reduce the amount of incremental reallocation.
 *译文：在添加大量元素之前，应用程序可以使用ensureCapacity操作增加ArrayList
 * 实例的容量。这可以减少递增式再分配的量。
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access an <tt>ArrayList</tt> instance concurrently,
 * and at least one of the threads modifies the list structurally, it
 * <i>must</i> be synchronized externally.  (A structural modification is
 * any operation that adds or deletes one or more elements, or explicitly
 * resizes the backing array; merely setting the value of an element is not
 * a structural modification.)  This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the list.
 *译文：这一实现并不是同步的。如果多个线程同时访问ArrayList实例，且至少有一个
 *   线程从结构上修改了ArrayList，那么必须从外部同步。（从结构上修改指的是
 *   添加，删除一个或多个元素，或者显式地改变底层数组的大小，仅仅设置元素
 *   的值不算从结构上修改。）这一般通过对自然封装该列表的对象进行同步操作
 *   来完成。
 *
 * If no such object exists, the list should be "wrapped" using the
 * {@link Collections#synchronizedList Collections.synchronizedList}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the list:<pre>
 *   List list = Collections.synchronizedList(new ArrayList(...));</pre>
 *译文：如果没有这样的对象则应该使用Collections.synchronizedList方法将该列表
 *   “包装”起来。这最好在创建时完成，以防止意外对列表进行不同步的访问：
 *   List list = Collections.synchronizedList(new ArrayList(…));
 *
 * <p><a name="fail-fast">
 * The iterators returned by this class's {@link #iterator() iterator} and
 * {@link #listIterator(int) listIterator} methods are <em>fail-fast</em>:</a>
 * if the list is structurally modified at any time after the iterator is
 * created, in any way except through the iterator's own
 * {@link ListIterator#remove() remove} or
 * {@link ListIterator#add(Object) add} methods, the iterator will throw a
 * {@link ConcurrentModificationException}.  Thus, in the face of
 * concurrent modification, the iterator fails quickly and cleanly, rather
 * than risking arbitrary, non-deterministic behavior at an undetermined
 * time in the future.
 *译文：此类的iterator和listIterator方法返回的迭代器是快速失败的：在创建迭代器
 *   之后，除非通过迭代器自身的remove或add方法从结构上对列表进行修改，否则
 *   在任何时间以任何方式对列表进行修改，迭代器都会抛出
 *   ConcurrentModificationException。因此，面对并发的修改，迭代器很快就会
 *   完全失败，而不是冒着在将来某个不确定时间发生任意不确定行为的风险。
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:  <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *译文： 注意，迭代器的 fast-fail 行为无法得到保证，因为一般来说，不可能对是否出现
 *  不同步并发修改做出任何硬性保证。fast-fail 迭代器会尽最大努力抛出
 *   ConcurrentModificationException。因此，为提高这类迭代器的正确性而编写
 *  一个依赖于此异常的程序是错误的做法：迭代器的 fast-fail 行为应该仅用于
 *   检测bug。
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *此类是Java Collections Framework的成员。
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see     Collection
 * @see     List
 * @see     LinkedList
 * @see     Vector
 * @since   1.2
 *
 * 注：ArrayList 是变长集合类，基于定长数组实现。非线性安全类。
 *  ArrayList 的核心是扩容
 */

/* RandomAccess接口：
        * 标记性接口，用来快速随机存取，实现了该接口之后，使用普通的for循环来
        * 遍历，性能更高，例如ArrayList。而没有实现该接口的话，使用Iterator来
        * 迭代，这样性能更高，例如linkedList。所以这个标记性只是为了让我们知道
        * 用什么样的方式去获取数据性能更好。
  *Cloneable接口：
 * 实现了该接口，就可以使用Object.Clone()方法了，列表能被克隆。
 * Serializable接口：
 * 实现该序列化接口，表明该类可以被序列化，能够从类变成节流传输，然后还能
 * 从字节流变成原来的类。
*/
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable
{
    private static final long serialVersionUID = 8683452581122892189L;
//序列化用，用于比较版本，对类功能没有意义。
    /**
     * Default initial capacity.
     */
    private static final int DEFAULT_CAPACITY = 10;
   //默认的初始容器的容量为10。
    /**
     * Shared empty array instance used for empty instances.
     */
    //所有的空的arraylist实例共享此数组，为了防止一个应用中创建多个空数组，导致空间浪费。
            //注意：此实例只被指定初始容量为0的含参构造函数创建的空数组 共享。
    private static final Object[] EMPTY_ELEMENTDATA = {};

    /**
     * Shared empty array instance used for default sized empty instances. We
     * distinguish this from EMPTY_ELEMENTDATA to know how much to inflate when
     * first element is added.
     */
    //没有指定初始容量的无参构造函数创建的空数组共享此空数组。
            //注意：和 EMPTY_ELEMENTDATA 区分开是
    //  为了知道当第一个元素被添加时需要扩容多少。
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

    /**
     * The array buffer into which the elements of the ArrayList are stored.
     * The capacity of the ArrayList is the length of this array buffer. Any
     * empty ArrayList with elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA
     * will be expanded to DEFAULT_CAPACITY when the first element is added.
     */
    //这一个数组用来存储 ArrayList 元素。ArrayList 的容量是这个数组的长度。
    //  添加第一个元素的时候任何满足
    //  elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA
    // 的空 ArrayList 会将容量扩展到 DEFAULT_CAPACITY。
    //
    //注意：transient关键字是主要用于序列化操作中（网络传输）。如果希望信息不想被学序列化传输，
     //加上此关键字后，就不会被序列化，即这个字段的生命周期仅存于调用者内存中而不会写到磁盘里持久化。
    transient Object[] elementData; // non-private to simplify nested class access

    /**
     * The size of the ArrayList (the number of elements it contains).
     *
     * @serial
     */
    private int size;  //ArrayList容量大小。(包含元素的数量)

    /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param  initialCapacity  the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity
     *         is negative
     */
    //指定了初始容量的构造函数
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {          //如果指定的容量大于0
            this.elementData = new Object[initialCapacity];    //则创建一个Object数组，大小为指定的容量。
        } else if (initialCapacity == 0) {          //如果指定的容量为0。
            this.elementData = EMPTY_ELEMENTDATA;     //含参构造函数构造的空数组将共享EMPTY_ELEMENTDATA空数组。
        } else {     //小于0,则抛出异常
            throw new IllegalArgumentException("Illegal Capacity: "+
                    initialCapacity);
        }
    }

    /**
     * Constructs an empty list with an initial capacity of ten.
     */
    //没有参数的构造函数
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA; //没有指定初始容量的无参构造函数创建的空数组共享此空数组。
    }

    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param c the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     */
    //使用指定的集合，构造一个包含了该集合所有元素的ArrayList，顺序是按照集合使用
    //Iterator迭代器放回的顺序。
    public ArrayList(Collection<? extends E> c) {
        elementData = c.toArray();    //返回一个包含了集合C所有元素的数组Object[]。
        if ((size = elementData.length) != 0) {       //如果集合c不为空
            // c.toArray might (incorrectly) not return Object[] (see 6260652)
            //集合c可能不会正确的返回一个object数组。所以需要判断。
            if (elementData.getClass() != Object[].class)    //如果集合数组c.toArray返回的不是Object数组类型。
                elementData = Arrays.copyOf(elementData, size, Object[].class);//使用Arrays.copyOf将数组转化成size长度的Object数组。
        } else {  //如果集合为空
            // replace with empty array.
            this.elementData = EMPTY_ELEMENTDATA;    //参构造函数构造的空数组将共享EMPTY_ELEMENTDATA空数组。
        }
    }

    /**
     * Trims the capacity of this <tt>ArrayList</tt> instance to be the
     * list's current size.  An application can use this operation to minimize
     * the storage of an <tt>ArrayList</tt> instance.
     */
    // 调整 ArrayList 实例的容量为列表的当前大小。程序可以用这个方法最小化
    //   ArrayList 实例占用的空间。
    // Arrays的copyOf()方法传回的数组是新的数组对象，改变传回
    // 数组中的元素值，不会影响原来的数组。copyOf()的第二个
    // 自变量指定要建立的新数组长度，如果新数组的长度超过原数组的
    // 长度，则超过的位数上的元素为默认值。
    public void trimToSize() {
        modCount++;  // 数组修改结构的次数加1。
        if (size < elementData.length) {     //如果包含的元素数量小于数组的指定长度。
            elementData = (size == 0)        //如果size为0，则将数组赋值为含参构造函数共享的空数组。
                    ? EMPTY_ELEMENTDATA
                    : Arrays.copyOf(elementData, size);     //如果size不为0,则将数组赋值为长度为size的数组。即实现最小化ArrayList的长度(占用空间)。
        }
    }

    /**
     * Increases the capacity of this <tt>ArrayList</tt> instance, if
     * necessary, to ensure that it can hold at least the number of elements
     * specified by the minimum capacity argument.
     *如果有需要，增加 ArrayList 实例的容量，来确保它可以容纳至少指定
     *  最小容量的元素。
     * @param   minCapacity   the desired minimum capacity
     */
    public void ensureCapacity(int minCapacity) {   //扩容的外部方法，可以在要传入大量的元素前，事先调用扩容，减少递增式再分配
        int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
                // any size if not default element table  //如果不是默认元素表，则为任意大小
                ? 0                //如果数组没有共享无参构造函数构建的空列表共享的数组。即当前数组不为空，或者使用了指定大小的构造函数创建的空数组。
                // larger than default for default empty table. It's already
                // supposed to be at default size.
                : DEFAULT_CAPACITY;        //如果数组共享了上面描述的数组，则说明没有指定初始容量，则使用默认初始容量。

        if (minCapacity > minExpand) {    //无参构造函数构造的空数组，最小容量默认为10，如果指定的最小容量小于等于10，就没有必要扩容。
            ensureExplicitCapacity(minCapacity);
        }
    }

    private void ensureCapacityInternal(int minCapacity) {      //内部的确保具有最小容量的方法，它是扩容的入口方法
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {  //如果没有指定构造函数创建的列表
            minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);  //取默认容量10和传入参数中最大的那个值。
        }

        ensureExplicitCapacity(minCapacity);
    }

    private void ensureExplicitCapacity(int minCapacity) {
        modCount++;   //数组修改次数加1，不管是否调用了grow函数进行扩容。

        // overflow-conscious code
        if (minCapacity - elementData.length > 0)      //如果这个指定的容量比当前数组的长度大。即当前数组长度不足以存放要求的最小数量时才进行扩充。
            grow(minCapacity);           //调用grow函数
    }

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    //能分配的最大数组的大小。
    //  些虚拟机会保留一些头消息，占用部分空间。
    // 尝试分配比这个值更大的空间可能会抛出 OutOfMemoryError 错误：请求的
    // 数组大小超过了虚拟机的限制。
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;  //能分配的最大的数组大小。

    /**
     * Increases the capacity to ensure that it can hold at least the
     * number of elements specified by the minimum capacity argument.
     *增大容量确保可以容纳指定最小容量的元素。
     * @param minCapacity the desired minimum capacity
     */
    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = elementData.length;  //老容量为当前数组的长度。
        int newCapacity = oldCapacity + (oldCapacity >> 1);  //新容量为老容量加上半个老容量，即扩容为老容量的1.5倍。
        if (newCapacity - minCapacity < 0)       // 如果扩容后的新容量还是比指定的最小容量小，或者上一步可能存在溢出，
            newCapacity = minCapacity;        //则直接使用指定的最小容量作为新的容量。即继续扩容到指定的最小的容量。
        if (newCapacity - MAX_ARRAY_SIZE > 0)               //如果扩容后的新容量大于了最大的能分配的数组的大小
            newCapacity = hugeCapacity(minCapacity);            //如果 newCapacity 比最大容量还大，调用 hugeCapacity 函数进行判断
        // minCapacity is usually close to size, so this is a win:
        // elementData 最终指向 newCapacity 大小的新数组空间
        elementData = Arrays.copyOf(elementData, newCapacity);
    }
    //判断扩容的新容量是通过minCapacity赋值，还是通过老容量扩容1.5倍得到的。两者在新容量超出最大可分配数组大小时返回的值不同。
    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow  //如果小于0，说明该整数值溢出了
            throw new OutOfMemoryError(); //抛出溢出异常
        return (minCapacity > MAX_ARRAY_SIZE) ? //如果指定最小容量大于最大可以分配的数组的大小，
                Integer.MAX_VALUE :             //则返回最大的整数值。
                MAX_ARRAY_SIZE;                   //如果没有大于，则返回最大的可分配的数组大小
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    public int size() {    //返回的是列表中元素的个数。而不是数组的长度。
        return size;
    }

    /**
     * Returns <tt>true</tt> if this list contains no elements.
     *
     * @return <tt>true</tt> if this list contains no elements
     */
    public boolean isEmpty() {      //用于判断列表是否为空，即判断列表是否不含元素。
        return size == 0;        //不含元素，返回true,代表为空，反之，返回false，代表列表含有元素。
    }

    /**
     * Returns <tt>true</tt> if this list contains the specified element.
     * More formally, returns <tt>true</tt> if and only if this list contains
     * at least one element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *如果列表包含指定元素返回 true。
     *     更正式地，当且仅当列表包含一个元素 e 且满足
     *    (o==null ? e==null : o.equals(e)) 时返回true。
     *
     * @param o element whose presence in this list is to be tested
     * @return <tt>true</tt> if this list contains the specified element
     */
    public boolean contains(Object o) {
        return indexOf(o) >= 0;      //如果indexOf返回的值不是-1，即大于等于0.说明找的到该元素。返回true。反正，返回false。
    }

    /**
     * Returns the index of the first occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the lowest index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     *  返回列表中第一次出现指定元素的索引位置，如果列表不包含该元素返回 -1。
     *   更正式地说，如果满足
     *   (o==null ? get(i)==null : o.equals(get(i))) 则返回最小的索引值 i，否则认为
     *    索引不存在则返回-1。
     */
    public int indexOf(Object o) {
        if (o == null) {      //如果传入的是null
            for (int i = 0; i < size; i++)       //遍历数组中各个元素。size代表元素数量。
                if (elementData[i]==null)       //找到第一个为null的元素，立即返回索引i
                    return i;
        } else {                            //如果传入的不为null
            for (int i = 0; i < size; i++)       //遍历数组各个元素
                if (o.equals(elementData[i]))      //调用对象的equals方法判断，找到第一个相等的元素后立即返回索引i
                    return i;
        }
        return -1;          //如果上面判断中都没有返回，说明没有找到，则返回-1；
    }

    /**
     * Returns the index of the last occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the highest index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     * 返回列表中最后一次出现指定元素的索引位置，如果列表不包含该元素返回 -1。
     *  更正式地说，如果满足
     *   (o==null ? get(i)==null : o.equals(get(i))) 则返回最大的索引值 i，否则认为
     *  索引不存在则返回-1。
     */
    public int lastIndexOf(Object o) {
        if (o == null) {      //如果传入的是null。
            for (int i = size-1; i >= 0; i--)       //从最后一个元素开始遍历，(最后一个元素的索引位置为size-1)
                if (elementData[i]==null)       //一旦找到第一个为null元素，就返回索引i，
                    return i;
        } else {              //如果传入不为null。
            for (int i = size-1; i >= 0; i--)          //从最后一个元素开始遍历
                if (o.equals(elementData[i]))          //调用对象的equals方法，找到第一个相等的值就返回对应的索引i
                    return i;
        }
        return -1;       //如果上述过程都没返回，则说明数组中不含有该元素，返回-1；
    }

    /**
     * Returns a shallow copy of this <tt>ArrayList</tt> instance.  (The
     * elements themselves are not copied.)
     *返回 ArrayList 实例的拷贝。
     * @saber 对于基本数据类型来说clone()方法实现数组拷贝也属于深拷贝。
     * 对于引用类型来说，clone都是浅拷贝，拷贝的都是对象的引用。即修改clone得到的副本
     * 同样也会影响原来的ArrayList。
     * @return a clone of this <tt>ArrayList</tt> instance
     */
    public Object clone() {
        try {
            ArrayList<?> v = (ArrayList<?>) super.clone();  //浅拷贝，创建了一个当前对象的副本。(ArrayList<?>为显示强制转换)
            //这里副本对象中的数组还是当前数组，所以需要用Arrays.copyOf创建一个新的数组。
            v.elementData = Arrays.copyOf(elementData, size);  //对副本中的成员elementData数组使用copyOf进行重新赋值。
            // 注意：这里新建的数组地址虽然不同了(即数组中各元素都存放在新地址)，但是因为java中对象的赋值都是引用传递，所以副本和原来数组中存放的
            //对象引用指向了同一个对象的实际位置，通过副本数组中存放的引用改变了对象，也会反映到原来数组通过引用得到的对象的值。
            v.modCount = 0;      //副本的初始修改值设置为0。
            return v;      //返回这个副本
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
    }

    /**
     * Returns an array containing all of the elements in this list
     * in proper sequence (from first to last element).
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this list.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this list in
     *         proper sequence
     */
    //以适当的顺序（从第一个元素到最后一个元素）返回包含列表所有元素的数组。
    //
    //  返回的数组是安全的，因为原列表中不包含它的引用。（换句话说，返回的
    //  数组是新分配的内存空间）。调用者可以任意修改返回的数组。
    public Object[] toArray() {
        return Arrays.copyOf(elementData, size);      //实际上原理和clone是一样的。
    }

    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence (from first to last element); the runtime type of the returned
     * array is that of the specified array.  If the list fits in the
     * specified array, it is returned therein.  Otherwise, a new array is
     * allocated with the runtime type of the specified array and the size of
     * this list.
     *
     * <p>If the list fits in the specified array with room to spare
     * (i.e., the array has more elements than the list), the element in
     * the array immediately following the end of the collection is set to
     * <tt>null</tt>.  (This is useful in determining the length of the
     * list <i>only</i> if the caller knows that the list does not contain
     * any null elements.)
     *
     * 以适当的顺序（从第一个元素到最后一个元素）返回包含列表所有元素的数组。
     *  返回数组的运行时类型是指定数组的类型。如果指定数组能完全容纳列表所有
     *  元素，那么将所有元素存入指定数组内。否则，在内存中分配足以容纳所有
     * 元素的新的数组空间。
     *
     *  如果指定的数组还有多余的空间（即指定数组比列表有更多的元素），在数组
     *  中紧跟集合末尾的元素被设置为 null。
     * @param a the array into which the elements of the list are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing the elements of the list
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this list
     * @throws NullPointerException if the specified array is null
     */
    //@saber-01
   // System.arraycopy(Object src,  int  srcPos,Object dest, int destPos,int length);
    //src：源对象
    //srcPos：源数组中的起始位置
    //dest：目标数组对象
    //destPos：目标数据中的起始位置
    //length：要拷贝的数组元素的数量
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size)     //如果目标数组的长度比当前列表中元素个数小，
            // Make a new array of a's runtime type, but my contents:
            //创建一个新的数组，使用copyOf函数，且指定了运行时类型为参数a的类型T，返回一个新的T[]类型数组。
            return (T[]) Arrays.copyOf(elementData, size, a.getClass());
        //如果目标数组长度大于等于当前数组元素个数size。则调用System.arraycopy。
        System.arraycopy(elementData, 0, a, 0, size);     //将size个元素全部复制到a中。
        if (a.length > size)             //如果目标数组长度大于当前数组元素个数size，
            a[size] = null;                 //则在目标数组a的紧跟着当前数组的末尾元素的位置上赋值为null。
        return a;
    }

    // Positional Access Operations
    // 位置访问相关操作
    @SuppressWarnings("unchecked")
    E elementData(int index) {        //默认访问权限只能当前包当前类使用。  传入一个索引，返回数组索引位置上的值,值类型为传入的模板E类型
        return (E) elementData[index];
    }

    /**
     * Returns the element at the specified position in this list.
     *返回列表中指定位置的元素
     * @param  index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E get(int index) {            //传入一个索引
        rangeCheck(index);            //检测索引合法性。检测索引是否越界，越界抛出异常

        return elementData(index);    //   返回数组中对应索引位置的值。类型为E。
    }

    /**
     * Replaces the element at the specified position in this list with
     * the specified element.
     *用指定元素替换列表中某一位置的元素。返回值是该位置的旧值。
     * @param index index of the element to replace     要替换的元素在数组中的位置
     * @param element element to be stored at the specified position  要替换成的值，即用这个参数值作为新的对应位置上的值。
     * @return the element previously at the specified position   //返回被替换的值，即原来索引上的旧值
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E set(int index, E element) {
        rangeCheck(index);      //检测索引是否越界。

        E oldValue = elementData(index);      //得到数组中对应位置原来的值,类型为E.
        elementData[index] = element;         //将新值赋值到数组指定位置。
        return oldValue;             //返回旧值。
    }

    /**
     * Appends the specified element to the end of this list.
     *    在列表的末尾添加指定的值。
     * @param e element to be appended to this list
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     */
    public boolean add(E e) {
        //确保容量充足。
        ensureCapacityInternal(size + 1);  // Increments modCount!!
        elementData[size++] = e;        //size自加，并且在末尾位置后放入新值。
        return true;           //成功返回true
    }

    /**
     * Inserts the specified element at the specified position in this
     * list. Shifts the element currently at that position (if any) and
     * any subsequent elements to the right (adds one to their indices).
     *在列表指定位置添加元素。把该位置及之后的所有元素向后移动（索引加一）。
     *
     * @param index index at which the specified element is to be inserted  需要放入的索引位置
     * @param element element to be inserted           //插入的新值
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public void add(int index, E element) {
        rangeCheckForAdd(index);     //ADD版本的数组边界监测
        //确保列表能够容纳size+1个元素。即确保容量充足
        ensureCapacityInternal(size + 1);  // Increments modCount!!
        //调用了System.arraycopy方法，从index开始移动到index+1。即将index之后的元素全部向后移动一位。
        System.arraycopy(elementData, index, elementData, index + 1,
                size - index);
        elementData[index] = element;   //移动后，将新值插入到index位置。
        size++;        //列表包含的元素个数加1.
    }

    /**
     * Removes the element at the specified position in this list.
     * Shifts any subsequent elements to the left (subtracts one from their
     * indices).
     *删除列表指定位置的元素。
     *   把后续元素向左移动（索引减一）。
     * @param index the index of the element to be removed  要删除元素的位置。
     * @return the element that was removed from the list    返回被删除的元素的值。
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E remove(int index) {
        rangeCheck(index);     //检测索引是否越界

        modCount++;             //列表结构修改次数加1
        E oldValue = elementData(index);            //先取出索引位置的旧值，用于返回

        int numMoved = size - index - 1;     //计算需要向左移动的位数。
        if (numMoved > 0)      //如果删除的不是最后一个元素。
            //调用System.arraycopy函数从将数组index+1开始的numMoved个元素移动到从index开始，即index后面的值全部向左移动一位
            System.arraycopy(elementData, index+1, elementData, index,
                    numMoved);
        elementData[--size] = null; // clear to let GC do its work //列表元素个数减1，并且末尾位置设置为null,方便GC回收。

        return oldValue;     //返回被删除的值
    }

    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present.  If the list does not contain the element, it is
     * unchanged.  More formally, removes the element with the lowest index
     * <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * (if such an element exists).  Returns <tt>true</tt> if this list
     * contained the specified element (or equivalently, if this list
     * changed as a result of the call).
     * 删除列表中第一次出现的指定元素，如果它存在的话。如果该元素不存在，
     * 不作任何变化。更正式地说，删除满足条件
     *  (o==null ? get(i)==null : o.equals(get(i))) 的索引值最小的元素。如果操作
     *成功则返回 true。
     *
     * @param o element to be removed from this list, if present
     * @return <tt>true</tt> if this list contained the specified element
     */
    public boolean remove(Object o) {  //删除于指定元素相等的第一次出现在数组中的元素。没找到就什么也没发生。
        if (o == null) {     //如果传入对象为null
            for (int index = 0; index < size; index++)     //遍历数组，
                if (elementData[index] == null) {      //找到第一个为null 的元素，
                    fastRemove(index);              //快速删除找到的索引上的元素
                    return true;                 //删除成功返回true
                }
        } else {                      //如果不为空
            for (int index = 0; index < size; index++)        //遍历数组
                if (o.equals(elementData[index])) {               //使用对象的equals方法判断相等。
                    fastRemove(index);                 //找到第一个相等的值后，直接快速删除
                    return true;                      //删除成功返回true
                }
        }
        return false;            //如果以上步骤没返回，说明没找到要删除的对象o，返回false
    }

    /*
     * Private remove method that skips bounds checking and does not
     * return the value removed.
     * 跳过边界检查并且不返回旧值的私有快速删除函数。效果和remove类似。
     */
    private void fastRemove(int index) {
        modCount++;
        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index,
                    numMoved);
        elementData[--size] = null; // clear to let GC do its work
    }

    /**
     * Removes all of the elements from this list.  The list will
     * be empty after this call returns.
     * 删除列表中的所有元素。此方法调用后列表为空。
     */
    public void clear() {
        modCount++;

        // clear to let GC do its work
        for (int i = 0; i < size; i++)        //遍历数组，
            elementData[i] = null;        //每个数组位置全部设为null,方便GC

        size = 0;      //列表元素个数设为0
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the
     * specified collection's Iterator.  The behavior of this operation is
     * undefined if the specified collection is modified while the operation
     * is in progress.  (This implies that the behavior of this call is
     * undefined if the specified collection is this list, and this
     * list is nonempty.)
     * 把指定集合的所有元素，按照迭代器指定集合迭代器返回的顺序，添加到列表
     *  末尾。如果指定集合在操作过程中被修改，则这个操作的行为是不确定的。
     *
     *
     * @param c collection containing elements to be added to this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     */
    //@saber-01 注意：结构性修改操作，一般步骤：检查边界，确保容量(添加元素时)，添加或删除，维护数组连续。
    public boolean addAll(Collection<? extends E> c) {
        Object[] a = c.toArray();   //得到集合c的object数组a
        int numNew = a.length;    //得到数组a的长度。
        ensureCapacityInternal(size + numNew);  // Increments modCount    //确保容量充足
        //从原数组a的0开始复制到目标数组(即当前列表中的elementData数组)的size索引开始。复制numNew(集合c中元素个数)个元素。
        System.arraycopy(a, 0, elementData, size, numNew);
        size += numNew;     //复制过后，列表元素个数size增加numNew个。
        return numNew != 0;                //如果集合含有元素，则返回true，为空返回false。
    }

    /**
     * Inserts all of the elements in the specified collection into this
     * list, starting at the specified position.  Shifts the element
     * currently at that position (if any) and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in the list in the order that they are returned by the
     * specified collection's iterator.
     *从指定位置开始，将指定集合的所有元素插入列表中。把当前位置及之后的
     *  元素向右移动（增加索引）。新增加的元素将按照指定集合迭代器的返回顺序
     * 出现在列表中。
     *
     * @param index index at which to insert the first element from the  插入的位置
     *              specified collection
     * @param c collection containing elements to be added to this list    //指定的集合
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException if the specified collection is null
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);  //检测边界

        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityInternal(size + numNew);  // Increments modCount

        int numMoved = size - index;        //计算需要移动的元素个数。
        if (numMoved > 0)    //如果不是在最后一个元素后面加入，就需要移动元素。
            //将数组从index位置开始的numMoved数向右移动numNew个位置，腾出位置存放集合c的元素。
            System.arraycopy(elementData, index, elementData, index + numNew,
                    numMoved);
        //腾出位置后，进行复制。
        System.arraycopy(a, 0, elementData, index, numNew);
        size += numNew;           //复制过后，列表元素个数size增加numNew个。
        return numNew != 0;      //如果集合含有元素，说明改变了列表，返回true，反之，返回false。
    }

    /**
     * Removes from this list all of the elements whose index is between
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
     * Shifts any succeeding elements to the left (reduces their index).
     * This call shortens the list by {@code (toIndex - fromIndex)} elements.
     * (If {@code toIndex==fromIndex}, this operation has no effect.)
     *
     *  删除列表中从 fromIndex（包含），到 toIndex（不包含）索引之间的元素。
     * 将所有后续元素向左移动（减小索引）。（如果 toIndex == fromIndex，此
     * 操作无影响。）
     *
     * @throws IndexOutOfBoundsException if {@code fromIndex} or
     *         {@code toIndex} is out of range
     *         ({@code fromIndex < 0 ||
     *          fromIndex >= size() ||
     *          toIndex > size() ||
     *          toIndex < fromIndex})
     */
    protected void removeRange(int fromIndex, int toIndex) {  //删除指定范围的值，包含起始索引，不包含结束索引。
        modCount++;
        int numMoved = size - toIndex;              //计算需要移动的个数。因为不包括结束索引，索引没有减1
        System.arraycopy(elementData, toIndex, elementData, fromIndex,//直接将从toIndex开始的numMoved数移动到从fromIndex。(覆盖掉了fromIndex上的值)
                numMoved);

        // clear to let GC do its work     //
        int newSize = size - (toIndex-fromIndex);//新的列表长度。
        for (int i = newSize; i < size; i++) {       //将列表长度后的位置上赋值null,方便GC
            elementData[i] = null;
        }
        size = newSize;              //设置新的列表长度。
    }

    /**
     * Checks if the given index is in range.  If not, throws an appropriate
     * runtime exception.  This method does *not* check if the index is
     * negative: It is always used immediately prior to an array access,
     * which throws an ArrayIndexOutOfBoundsException if index is negative.
     * 检查给定的索引是否在范围内。如果不在，抛出适当的运行时异常。这个方法
     * 不会检查索引是否是负数：这个方法总是在访问数组之前使用，如果索引为
     *  负数，访问数组时会抛出 ArrayIndexOutOfBoundsException 异常。
     */
    private void rangeCheck(int index) {       //传入索引，对索引的合法性进行检测。
        if (index >= size)          //如果索引大于等于size。抛出索引越界异常。 小于0的话会在elementData(int index)抛出异常。
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /**
     * A version of rangeCheck used by add and addAll.
     * 被add和addAll使用的索引检查版本。
     */
    private void rangeCheckForAdd(int index) {
        if (index > size || index < 0)           //add版本，所以允许index=size。且，显然index不能为负
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /**
     * Constructs an IndexOutOfBoundsException detail message.
     * Of the many possible refactorings of the error handling code,
     * this "outlining" performs best with both server and client VMs.
     * 构造一个 IndexOutOfBoundsException 详细消息。
     *  在错误处理代码许多可能的构建中，这种“描述”对服务器和客户端虚拟机都
     * 表现最好。
     */
    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size;
    }

    /**
     * Removes from this list all of its elements that are contained in the
     * specified collection.
     *移除列表中和指定集合相同的元素。
     * 注：即是remove方法的集合版
     * @param c collection containing elements to be removed from this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the class of an element of this list
     *         is incompatible with the specified collection
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this list contains a null element and the
     *         specified collection does not permit null elements
     * (<a href="Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see Collection#contains(Object)
     */
    public boolean removeAll(Collection<?> c) {
        // 使用 Objects 工具类检查集合 c 是否指向 null。为null，会抛出NullPointerException空指针异常
        Objects.requireNonNull(c);
        // 根据第二个参数判断是删除还是保留。为false，说明是删除数组中与集合c中元素相同的元素。
        return batchRemove(c, false);
    }

    /**
     * Retains only the elements in this list that are contained in the
     * specified collection.  In other words, removes from this list all
     * of its elements that are not contained in the specified collection.
     *保留列表中和指定集合相同的元素（求交集）。换句话说，移除列表中有的而
     *指定集合中没有的那些元素。
     *
     * @param c collection containing elements to be retained in this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the class of an element of this list
     *         is incompatible with the specified collection
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this list contains a null element and the
     *         specified collection does not permit null elements
     * (<a href="Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see Collection#contains(Object)
     */
    public boolean retainAll(Collection<?> c) {
        // 使用 Objects 工具类检查集合 c 是否指向 null，为null，会抛出NullPointerException空指针异常
        Objects.requireNonNull(c);
        //根据第二个参数判断是删除还是保留。为true，说明是保留数组中与集合c中元素相同的元素，删除其他元素。
        return batchRemove(c, true);
    }
    // complement 为 true，保留集合中存在的元素；complement 为 false 时删除。
    private boolean batchRemove(Collection<?> c, boolean complement) {
        // 对于一个final变量，如果是基本数据类型的变量，则其数值一旦在初始化
        //  之后便不能更改；如果是引用类型的变量，则在对其初始化之后便不能
        //再让其指向另一个对象。
        final Object[] elementData = this.elementData;
        int r = 0, w = 0;
        boolean modified = false;
        try {
            for (; r < size; r++)       //遍历数组。
                // complement 为 false：若 c 不包含 elementData[r]，if成立，则保留该 elementData[r]。
                // complement 为 true：若 c 包含 elementData[r]，if成立，保留该值 elementData[r]。
                //循环结果就是complement为true，数组剩下的为包含在c中的元素，若为false，则剩下的是不包含在c中的元素
                if (c.contains(elementData[r]) == complement)
                    elementData[w++] = elementData[r];
        } finally {
            // Preserve behavioral compatibility with AbstractCollection,
            // even if c.contains() throws.
            //保持与AbstractCollection的行为兼容性，
            //即使c.contains（）抛出异常，也要执行finally代码块。
            if (r != size) {   //如果因为异常退出循环，r还没遍历到size。
                System.arraycopy(elementData, r,            //将r后面的元素直接复制到w位置后，
                        elementData, w,
                        size - r);
                w += size - r;           //w向右移动size-r个位置。
            }
            if (w != size) {                 //如果w不等于size，即列表发生了变化。
                // clear to let GC do its work
                for (int i = w; i < size; i++)       //将后面多余位置上原来的值赋值为null。方便GC。
                    elementData[i] = null;
                modCount += size - w;
                size = w;                //设置新长度为w。
                modified = true;      //因为列表发生了变化，所以为true。
            }
        }
        return modified;
    }

    /**
     * Save the state of the <tt>ArrayList</tt> instance to a stream (that
     * is, serialize it).
     *保留 ArrayList 实例的状态到 stream 里（也就是序列化它）。
     * @serialData The length of the array backing the <tt>ArrayList</tt>
     *             instance is emitted (int), followed by all of its elements
     *             (each an <tt>Object</tt>) in the proper order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException{
        // Write out element count, and any hidden stuff
        // 写出元素计数和所有隐藏的东西。在写之前保存此刻的 modCount。
        int expectedModCount = modCount;
        s.defaultWriteObject();

        // Write out size as capacity for behavioural compatibility with clone()
        //将大小写为与clone（）的行为兼容性的容量
        s.writeInt(size);

        // Write out all elements in the proper order.
        // 遍历 elementData。以正确的顺序写出所有元素，
        for (int i=0; i<size; i++) {
            s.writeObject(elementData[i]);
        }
       //判断写过程中，是否发生了列表的结构性修改操作。如果发生了，抛出异常
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Reconstitute the <tt>ArrayList</tt> instance from a stream (that is,
     * deserialize it).
     *  从 stream 流里复原 ArrayList 实例（也就是说反序列化它）。
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        elementData = EMPTY_ELEMENTDATA;

        // Read in size, and any hidden stuff
        s.defaultReadObject();

        // Read in capacity 读入容量
        s.readInt(); // ignored

        if (size > 0) {
            // be like clone(), allocate array based upon size not capacity
            // 就像 clone() 一样，存储数组是基于 size 而不是 capacity
            ensureCapacityInternal(size);  //确保足够多的容量可以存放size个元素

            Object[] a = elementData;        //创建一个object数组用于存放读出的元素。
            // Read in all elements in the proper order.
            //遍历数组。以正确的顺序读入所有元素
            for (int i=0; i<size; i++) {
                a[i] = s.readObject();
            }
        }
    }

    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence), starting at the specified position in the list.
     * The specified index indicates the first element that would be
     * returned by an initial call to {@link ListIterator#next next}.
     * An initial call to {@link ListIterator#previous previous} would
     * return the element with the specified index minus one.
     *
     * 从列表的指定位置开始，返回列表中元素的列表迭代器（按正确顺序）。
     *  指定的索引表示第一次调用 next 方法返回的第一个元素。第一次调用
     *  previous 方法将返回指定索引减 1 代表的元素。
     *
     * 返回的迭代器是支持 fast-fail 的。
     * <p>The returned list iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size)    //如果索引为负，或者超过size，则抛出异常
            throw new IndexOutOfBoundsException("Index: "+index);
        return new ListItr(index);      //返回从当前index开始的迭代器。
    }

    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence).
     *返回列表中元素的列表迭代器（按正确顺序）。
     *
     *  返回的迭代器是支持 fast-fail 的。
     *
     * <p>The returned list iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
     *
     * @see #listIterator(int)
     */
    public ListIterator<E> listIterator() {      //从0开始的迭代器。
        return new ListItr(0);
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * <p>The returned iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
     *按正确顺序返回列表中元素的迭代器。
     * @return an iterator over the elements in this list in proper sequence
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * An optimized version of AbstractList.Itr
     * AbstractList.Itr的优化版本
     */
    private class Itr implements Iterator<E> {
        int cursor;       // index of next element to return    将要返回的下一个元素的索引
        int lastRet = -1; // index of last element returned; -1 if no such 最后一个迭代遍历到的元素的索引，如果没有则为 -1
        int expectedModCount = modCount;   //将迭代前 列表修改次数保存，便于检查迭代过程是否发生修改。

        public boolean hasNext() {
            return cursor != size;
        }      //是否还有下一个元素，即下一个元素如果为size，说明没有下一个了。

        @SuppressWarnings("unchecked")
        public E next() {
            checkForComodification();    //是否并发修改一致
            int i = cursor;           //i取下一个元素的索引
            //对下一个索引进行判断是否合法
            if (i >= size)
                throw new NoSuchElementException();
            Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            cursor = i + 1;       // 下一个元素的索引已经取出，所以要更新为下一个。
            return (E) elementData[lastRet = i];       //取出原数中索引i上的数进行返回。最后遍历的索引更新为i
        }

        public void remove() {   //迭代器的删除方法
            if (lastRet < 0)     //如果还未进行遍历，或者执行了remove以后还未继续遍历。则抛出异常
                throw new IllegalStateException();
            checkForComodification();     //判断并发修改一致

            try {
                ArrayList.this.remove(lastRet);       //调用的是ArrayList的remove(index)方法
                cursor = lastRet;    //下一个元素更新为当前遍历的索引，因为删除了一个索引
                lastRet = -1;      //最后遍历到的位置设置成初始值-1
                expectedModCount = modCount;  //因为调用remove(index)会更新modCount所以需要更新expectedModCount
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        //ArrayList支持lambda表达式
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            final int size = ArrayList.this.size;
            int i = cursor;
            if (i >= size) {
                return;
            }
            final Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length) {
                throw new ConcurrentModificationException();
            }
            while (i != size && modCount == expectedModCount) {
                consumer.accept((E) elementData[i++]);
            }
            // update once at end of iteration to reduce heap write traffic
            cursor = i;
            lastRet = i - 1;
            checkForComodification();
        }
        //检查并发修改异常
        final void checkForComodification() {
            if (modCount != expectedModCount)    //如果迭代过程中发生了列表结构性操作(不是通过迭代器方法),则抛出异常
                throw new ConcurrentModificationException();
        }
    }

    /**
     * An optimized version of AbstractList.ListItr
     * AbstractList.ListItr的优化版本
     */
    private class ListItr extends Itr implements ListIterator<E> {
        ListItr(int index) {   // 从指定索引开始的构造方法
            super();
            cursor = index;
        }
            //是否有上一个元素。
        public boolean hasPrevious() {
            return cursor != 0;
        }
        // 返回下一个元素的索引
        public int nextIndex() {
            return cursor;
        }
        // 返回上一个元素的索引
        public int previousIndex() {
            return cursor - 1;
        }

        @SuppressWarnings("unchecked")
        // 返回上一个元素,将当前光标向前移动一位。
        public E previous() {
            checkForComodification();
            int i = cursor - 1;
            //对索引进行判断合法性
            if (i < 0)
                throw new NoSuchElementException();
            Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            cursor = i;  //更新cursor向前移动一位，指向当前元素，
            return (E) elementData[lastRet = i];   //最后遍历的位置更新为i。
        }
        //设置最后一个遍历到的索引上的值为e。
        public void set(E e) {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                ArrayList.this.set(lastRet, e);
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
        //在光标cursor处调用ArrayList.add(index,e)方法添加一个数。
        public void add(E e) {
            checkForComodification();

            try {
                int i = cursor;
                ArrayList.this.add(i, e);
                cursor = i + 1;
                lastRet = -1;     //初始化lastRet。
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * Returns a view of the portion of this list between the specified
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.  (If
     * {@code fromIndex} and {@code toIndex} are equal, the returned list is
     * empty.)  The returned list is backed by this list, so non-structural
     * changes in the returned list are reflected in this list, and vice-versa.
     * The returned list supports all of the optional list operations.
     *返回 fromIndex（包含该位置）到 toIndex（不包含该位置）之间此列表的
     * 视图 。（如果 fromIndex 等于 toIndex，返回的列表为空）返回的列表是
     *  依赖于此列表的，所以返回列表的非结构化更改会反映到此列表中，反之亦然。
     *  返回的列表支持列表所有操作。
     * <p>This method eliminates the need for explicit range operations (of
     * the sort that commonly exist for arrays).  Any operation that expects
     * a list can be used as a range operation by passing a subList view
     * instead of a whole list.  For example, the following idiom
     * removes a range of elements from a list:
     * <pre>
     *      list.subList(from, to).clear();
     * </pre>
     * Similar idioms may be constructed for {@link #indexOf(Object)} and
     * {@link #lastIndexOf(Object)}, and all of the algorithms in the
     * {@link Collections} class can be applied to a subList.
     *此方法消除了对显式范围方法的需要（通常还需要对数组排序）。任何需要
     *  对整个列表部分的操作都可以传递 subList，而不是对整个列表进行操作
     * （提高性能）。例如，下面的语句从列表中删除范围内的元素：
     *      * list.subList(from, to).clear();
     *      * 可以为 indexOf(Object) 和 lastIndexOf(Object) 构造类似的语句。
     *      * Collection 中的所有算法都可以应用 subList 的思想。
     *
     * <p>The semantics of the list returned by this method become undefined if
     * the backing list (i.e., this list) is <i>structurally modified</i> in
     * any way other than via the returned list.  (Structural modifications are
     * those that change the size of this list, or otherwise perturb it in such
     * a fashion that iterations in progress may yield incorrect results.)
     *如果原列表通过子列表以外的方式进行了结构性修改，那么通过这个方法返回
     * 的列表就变得不确定。（结构化的修改是指改变了这个列表的大小，或者其他
     *  扰乱列表的方式，使得正在进行的迭代可能产生不正确的结果）
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public List<E> subList(int fromIndex, int toIndex) {
        // 检查索引是否超出边界范围
        subListRangeCheck(fromIndex, toIndex, size);
        return new SubList(this, 0, fromIndex, toIndex);
    }
   // 子列表边界检查，如果过程中出现越界，则抛出异常
    static void subListRangeCheck(int fromIndex, int toIndex, int size) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        if (toIndex > size)
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        if (fromIndex > toIndex)
            throw new IllegalArgumentException("fromIndex(" + fromIndex +
                    ") > toIndex(" + toIndex + ")");
    }
     //子列表的内部类
    private class SubList extends AbstractList<E> implements RandomAccess {
        private final AbstractList<E> parent;   //父列表
         // 相对于父列表的偏移值，子列表调用结构性修改方法时，要用到这个偏移修改父列表中的数组。
         // 因为结构化修改会调用父类方法，只要传当前子列的parentOffset+索引给父类即可
        private final int parentOffset;
         //相对子列表自己的偏移值， 在set,get，遍历时，用到。因为子列表的读取值类的方法都是自己实现的，
         // 所以当创建了子列表的子列表时候，在子子列表要访问最初的ArrayList时，每一次的新建都需要加入offset上一个子列的偏移，才能正确取到值。
         private final int offset;
        int size;       //子列表的大小，包含元素的个数
       //构造函数，(可以发现并没有new 一块新区域存放列表，所以子列表的操作直接反应在父列表上)
        SubList(AbstractList<E> parent,
                int offset, int fromIndex, int toIndex) {
            this.parent = parent;      //设置父列表
            this.parentOffset = fromIndex;     //设置相对于父列表(索引0位置)的偏移值，因为是截取其中的一段，所以所有操作都需要一定的偏移才能正确修改父列表。
            this.offset = offset + fromIndex;   //子列表自己的偏移值，即相对于父列表的偏移值再加上传入的偏移值。
            this.size = toIndex - fromIndex;
            this.modCount = ArrayList.this.modCount;
        }
         // 修改索引 index 位置的值， 返回修改前的值
         // 注意：子列表和原列表保存同一段元素，修改子列表元素值，父列表里面
         // 也改变了
        public E set(int index, E e) {
            rangeCheck(index);    //检查索引合法性
            checkForComodification();   //检查并发合法性。
            E oldValue = ArrayList.this.elementData(offset + index);  //子列表中index相当于父列表中index+offset的位置
            ArrayList.this.elementData[offset + index] = e;
            return oldValue;
        }
       //  获取某一索引位置的值
        public E get(int index) {
            rangeCheck(index);//检查索引合法性
            checkForComodification();//检查并发合法性。
            return ArrayList.this.elementData(offset + index);//子列表中index相当于父列表中index+offset的位置
        }
        //得到子列表中含有的元素个数。
        public int size() {
            checkForComodification();//检查并发合法性。
            return this.size;
        }
         // 以下操作的实现均调用父列表相应的函数，由于父列表和子列表共用一个
         // elementData 数组，所以父列表同样受到影响
         // 添加元素操作
         // 参数 index 是相对于子列表而言
        public void add(int index, E e) {
            rangeCheckForAdd(index);
            checkForComodification();
            parent.add(parentOffset + index, e);    //调用父类方法，所以，传入的索引需要加上父类偏移。
            this.modCount = parent.modCount;   //结构性修改，所以子列表的结构性修改次数更新。
            this.size++;          //子列表包含元素个数加1。
        }
         // 根据索引移除元素，并返回移除的元素。父列表和子列表都会受影响
        public E remove(int index) {
            rangeCheck(index);
            checkForComodification();
            E result = parent.remove(parentOffset + index);//调用父类方法，所以，传入的索引需要加上父类偏移。
            this.modCount = parent.modCount;
            this.size--;
            return result;
        }
       // 移除范围内的元素
        protected void removeRange(int fromIndex, int toIndex) {
            checkForComodification();     //检查修改的并发一致性
            parent.removeRange(parentOffset + fromIndex,
                    parentOffset + toIndex);
            this.modCount = parent.modCount;
            this.size -= toIndex - fromIndex;
        }
         // 添加指定集合的所有元素到子列表的末尾
        public boolean addAll(Collection<? extends E> c) {
            return addAll(this.size, c);
        }
         // 添加指定集合的所有元素到子列表的末尾
        public boolean addAll(int index, Collection<? extends E> c) {
            rangeCheckForAdd(index);  //检查索引合法性
            int cSize = c.size();
            if (cSize==0)
                return false;

            checkForComodification();  //检查并发一致性
            parent.addAll(parentOffset + index, c);
            this.modCount = parent.modCount;
            this.size += cSize;
            return true;
        }
       //子列表的迭代器。
        public Iterator<E> iterator() {
            return listIterator();
        }
       //子列表的ListIterator迭代器。
        public ListIterator<E> listIterator(final int index) {
            checkForComodification();
            rangeCheckForAdd(index);
            final int offset = this.offset;

            return new ListIterator<E>() {
                int cursor = index;
                int lastRet = -1;
                int expectedModCount = ArrayList.this.modCount;

                public boolean hasNext() {
                    return cursor != SubList.this.size;
                }

                @SuppressWarnings("unchecked")
                public E next() {
                    checkForComodification();
                    int i = cursor;
                    if (i >= SubList.this.size)
                        throw new NoSuchElementException();
                    Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length)
                        throw new ConcurrentModificationException();
                    cursor = i + 1;
                    return (E) elementData[offset + (lastRet = i)];
                }

                public boolean hasPrevious() {
                    return cursor != 0;
                }

                @SuppressWarnings("unchecked")
                public E previous() {
                    checkForComodification();
                    int i = cursor - 1;
                    if (i < 0)
                        throw new NoSuchElementException();
                    Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length)
                        throw new ConcurrentModificationException();
                    cursor = i;
                    return (E) elementData[offset + (lastRet = i)];
                }

                @SuppressWarnings("unchecked")
                public void forEachRemaining(Consumer<? super E> consumer) {
                    Objects.requireNonNull(consumer);
                    final int size = SubList.this.size;
                    int i = cursor;
                    if (i >= size) {
                        return;
                    }
                    final Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length) {
                        throw new ConcurrentModificationException();
                    }
                    while (i != size && modCount == expectedModCount) {
                        consumer.accept((E) elementData[offset + (i++)]);
                    }
                    // update once at end of iteration to reduce heap write traffic
                    lastRet = cursor = i;
                    checkForComodification();
                }

                public int nextIndex() {
                    return cursor;
                }

                public int previousIndex() {
                    return cursor - 1;
                }

                public void remove() {
                    if (lastRet < 0)
                        throw new IllegalStateException();
                    checkForComodification();

                    try {
                        SubList.this.remove(lastRet);
                        cursor = lastRet;
                        lastRet = -1;
                        expectedModCount = ArrayList.this.modCount;
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                public void set(E e) {
                    if (lastRet < 0)
                        throw new IllegalStateException();
                    checkForComodification();

                    try {
                        ArrayList.this.set(offset + lastRet, e);
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                public void add(E e) {
                    checkForComodification();

                    try {
                        int i = cursor;
                        SubList.this.add(i, e);
                        cursor = i + 1;
                        lastRet = -1;
                        expectedModCount = ArrayList.this.modCount;
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                final void checkForComodification() {
                    if (expectedModCount != ArrayList.this.modCount)
                        throw new ConcurrentModificationException();
                }
            };
        }
         // SubList 的 subList 方法，其中传入的 offset 参数是 SubList 的 offset
         // 属性的值
        public List<E> subList(int fromIndex, int toIndex) {
            subListRangeCheck(fromIndex, toIndex, size);
            return new SubList(this, offset, fromIndex, toIndex);
        }

        private void rangeCheck(int index) {  //子列表的索引合法检查。
            if (index < 0 || index >= this.size)
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }

        private void rangeCheckForAdd(int index) { //子列表的ADD方法版本的索引合法检查
            if (index < 0 || index > this.size)
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }
        //抛出的异常输出形式方法。
        private String outOfBoundsMsg(int index) {
            return "Index: "+index+", Size: "+this.size;
        }

        private void checkForComodification() {     //检查是否发生并发不一致。
            if (ArrayList.this.modCount != this.modCount)
                throw new ConcurrentModificationException();
        }
         // 返回分裂迭代器
        public Spliterator<E> spliterator() {
            checkForComodification();
            return new ArrayListSpliterator<E>(ArrayList.this, offset,
                    offset + this.size, this.modCount);
        }
    }

    @Override
    // Java 8 Lambda 表达式遍历列表元素的方式
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        final int expectedModCount = modCount;
        @SuppressWarnings("unchecked")
        final E[] elementData = (E[]) this.elementData;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            action.accept(elementData[i]);
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * list.
     * 在此列表中的元素上创建一个支持 late-binding 和 fail-fast 的 Spliterator。
     * @saber-01 可分割迭代器（splitable iterator），增强并行处理能力。用来
     *  多线程并行迭代的迭代器。主要作用是把集合分成几段，每个线程执行一段。
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, and {@link Spliterator#ORDERED}.
     * Overriding implementations should document the reporting of additional
     * characteristic values.
     *
     * @return a {@code Spliterator} over the elements in this list
     * @since 1.8
     */
    @Override
    public Spliterator<E> spliterator() {
        return new ArrayListSpliterator<>(this, 0, -1, 0);
    }

    /** Index-based split-by-two, lazily initialized Spliterator */
    /** 基于索引的，二分的，懒加载器 */
    static final class ArrayListSpliterator<E> implements Spliterator<E> {

        /*
        如果 ArrayList 是不可变的，或者在结构上是不可变的（没有添加，删除
         * 等操作），我们可以用 ArrayList.spliterator 实现它们的 spliterator。
         * 相反，我们在遍历过程中检测尽可能多的干扰，同时又不会牺牲太多性能。
         * If ArrayLists were immutable, or structurally immutable (no
         * adds, removes, etc), we could implement their spliterators
         * with Arrays.spliterator. Instead we detect as much
         * interference during traversal as practical without
         * sacrificing much performance. We rely primarily on
         * modCounts. These are not guaranteed to detect concurrency
         * violations, and are sometimes overly conservative about
         * within-thread interference, but detect enough problems to
         * be worthwhile in practice. To carry this out, we (1) lazily
         * initialize fence and expectedModCount until the latest
         * point that we need to commit to the state we are checking
         * against; thus improving precision.  (This doesn't apply to
         * SubLists, that create spliterators with current non-lazy
         * values).  (2) We perform only a single
         * ConcurrentModificationException check at the end of forEach
         * (the most performance-sensitive method). When using forEach
         * (as opposed to iterators), we can normally only detect
         * interference after actions, not before. Further
         * CME-triggering checks apply to all other possible
         * violations of assumptions for example null or too-small
         * elementData array given its size(), that could only have
         * occurred due to interference.  This allows the inner loop
         * of forEach to run without any further checks, and
         * simplifies lambda-resolution. While this does entail a
         * number of checks, note that in the common case of
         * list.stream().forEach(a), no checks or other computation
         * occur anywhere other than inside forEach itself.  The other
         * less-often-used methods cannot take advantage of most of
         * these streamlinings.
         */

        private final ArrayList<E> list; // 用于存放 ArrayList 对象
        private int index; // current index, modified on advance/split 当前索引（包含），advance/split 操作时会被修改
        private int fence; // -1 until used; then one past last index  结束位置（不包含），-1 表示到最后一个元素
        private int expectedModCount; // initialized when fence set   用于存放 list 的 modCount

        /** Create new spliterator covering the given  range */
        // 构造函数
        ArrayListSpliterator(ArrayList<E> list, int origin, int fence,
                             int expectedModCount) {
            this.list = list; // OK if null unless traversed
            this.index = origin;
            this.fence = fence;
            this.expectedModCount = expectedModCount;
        }
        // 获取结束位置（首次使用需要对 fence 赋值）
        private int getFence() { // initialize fence to size on first use
            int hi; // (a specialized variant appears in method forEach)
            ArrayList<E> lst;
            // 第一次初始化时 fence 才会小于 0,因为初始传入了-1作为参数。
            if ((hi = fence) < 0) {
                // list 为 null 时，fence = 0
                if ((lst = list) == null)
                    hi = fence = 0;
                    // 否则，fence 等于 list 的长度
                else {
                    expectedModCount = lst.modCount;
                    hi = fence = lst.size;
                }
            }
            return hi;
        }
        // 分割 list，返回一个新分割出的 spliterator 实例
        public ArrayListSpliterator<E> trySplit() {
            //取得index为初始位置，fence为结尾位置，mid则是取得他们的中间位置。
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;//>>>：无符号右移。无论是正数还是负数，高位通通补0。
            return (lo >= mid) ? null : // divide range in half unless too small    //如果传入的长度太小，则无法分割，返回null
                    new ArrayListSpliterator<E>(list, lo, index = mid,   //否则返回从lo到mid的一般长度作为新参数创建的子迭代器。
                            expectedModCount);
        }
        // 返回 true 表示可能还有元素未处理
        // 返回 false 表示没有剩余的元素了
        public boolean tryAdvance(Consumer<? super E> action) {
            if (action == null)   //如果action处理为null，则抛出异常
                throw new NullPointerException();
            int hi = getFence(), i = index;
            // hi 为当前的结束位置
            // i 为起始位置
            if (i < hi) {       // 还有剩余的元素没有处理
                index = i + 1;  //取出后，index向后遍历一位。
                @SuppressWarnings("unchecked") E e = (E)list.elementData[i];  //取出索引对应的值
                action.accept(e);   //对值进行action处理。
                if (list.modCount != expectedModCount)   //判断并发修改的一致性
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }
        // 顺序遍历处理所有剩下的元素，并对元素进行action处理。
        public void forEachRemaining(Consumer<? super E> action) {
            int i, hi, mc; // hoist accesses and checks from loop
            ArrayList<E> lst; Object[] a;
            if (action == null)//如果action处理为null，则抛出异常
                throw new NullPointerException();
            if ((lst = list) != null && (a = lst.elementData) != null) {
                if ((hi = fence) < 0) { //初始化时。
                    mc = lst.modCount;
                    hi = lst.size;
                }
                else
                    mc = expectedModCount;
                if ((i = index) >= 0 && (index = hi) <= a.length) {  //传入的索引满足边界条件
                    for (; i < hi; ++i) {          //遍历数组，对数组中从index到fence的元素进行action处理。
                        @SuppressWarnings("unchecked") E e = (E) a[i];
                        action.accept(e);
                    }
                    if (lst.modCount == mc)
                        return;
                }
            }
            throw new ConcurrentModificationException();
        }
        // 估算大小
        public long estimateSize() {
            return (long) (getFence() - index);
        }
        // 返回特征值
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

    @Override
    // 过滤器删除元素
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        // 找出要删除的元素，抛出的任何异常都会使集合不发生变化
        // figure out which elements are to be removed
        // any exception thrown from the filter predicate at this stage
        // will leave the collection unmodified
        int removeCount = 0;
        final BitSet removeSet = new BitSet(size);
        final int expectedModCount = modCount;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            @SuppressWarnings("unchecked")
            final E element = (E) elementData[i];
            if (filter.test(element)) {
                removeSet.set(i);
                removeCount++;
            }
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }

        // shift surviving elements left over the spaces left by removed elements
        // 删除元素的实现：向左移动存活的元素。
        final boolean anyToRemove = removeCount > 0;
        if (anyToRemove) {
            final int newSize = size - removeCount;
            for (int i=0, j=0; (i < size) && (j < newSize); i++, j++) {
                i = removeSet.nextClearBit(i);
                elementData[j] = elementData[i];
            }
            for (int k=newSize; k < size; k++) {
                elementData[k] = null;  // Let gc do its work
            }
            this.size = newSize;
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            modCount++;
        }

        return anyToRemove;
    }

    @Override
    @SuppressWarnings("unchecked")
    // 根据操作符替换列表中所有元素
    public void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final int expectedModCount = modCount;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            elementData[i] = operator.apply((E) elementData[i]);
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }

    @Override
    @SuppressWarnings("unchecked")
    // 根据 Comparator 排序
    public void sort(Comparator<? super E> c) {
        final int expectedModCount = modCount;
        Arrays.sort((E[]) elementData, 0, size, c);
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }
}
