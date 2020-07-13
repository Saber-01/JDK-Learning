/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * The {@code Vector} class implements a growable array of
 * objects. Like an array, it contains components that can be
 * accessed using an integer index. However, the size of a
 * {@code Vector} can grow or shrink as needed to accommodate
 * adding and removing items after the {@code Vector} has been created.
 *Vector类实现了一个可增长的数组。和数组一样，它包含了可以直接使用
 * 整数索引访问的组件。但是，Vector的大小可以根据需要增长或收缩，以
 * 适应在创建之后的添加和删除操作。
 *
 * <p>Each vector tries to optimize storage management by maintaining a
 * {@code capacity} and a {@code capacityIncrement}. The
 * {@code capacity} is always at least as large as the vector
 * size; it is usually larger because as components are added to the
 * vector, the vector's storage increases in chunks the size of
 * {@code capacityIncrement}. An application can increase the
 * capacity of a vector before inserting a large number of
 * components; this reduces the amount of incremental reallocation.
 *每一个向量都希望通过一个 capacity 和一个 capacityIncrement 来优化
 * 存储管理。capacity 至少和向量的大小一样大，实际上会更大一点，因为
 *随着新的组件被添加到向量中，向量的存储以块的形式增加，块的大小
 * 为 capacityIncrement。应用程序可以在添加大量组件之前增加向量的
 *容量；这会减少空间再分配的次数。
 *
 * <p><a name="fail-fast">
 * The iterators returned by this class's {@link #iterator() iterator} and
 * {@link #listIterator(int) listIterator} methods are <em>fail-fast</em></a>:
 * if the vector is structurally modified at any time after the iterator is
 * created, in any way except through the iterator's own
 * {@link ListIterator#remove() remove} or
 * {@link ListIterator#add(Object) add} methods, the iterator will throw a
 * {@link ConcurrentModificationException}.  Thus, in the face of
 * concurrent modification, the iterator fails quickly and cleanly, rather
 * than risking arbitrary, non-deterministic behavior at an undetermined
 * time in the future.  The {@link Enumeration Enumerations} returned by
 * the {@link #elements() elements} method are <em>not</em> fail-fast.
 *该类的 iterator 方法和 listIterator 方法返回的迭代器都支持 fail-fast：
 * 如果在迭代器创建之后，除了其自身的 remove 和 add 方法之外，一旦
 * 向量任何时候被结构性修改，会抛出 ConcurrentModificationException
 * 异常。因此，在面对并发修改的时候，迭代器会干净利落地停止，而不是
 *  在未来某个时间承担任意的风险和出现未知的行为。
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:  <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *注意迭代器的 fail-fast 行为不能完全保证正确，因为通常来说，非同步的
 * 并发修改都不能做出任何严格的承诺。支持 fail-fast 的迭代器会尽最大
 *  努力抛出 ConcurrentModificationException 异常。因此，编写一个依赖
 *  此异常来判断其正确性的程序是错误的：迭代器的快速故障行为应该只
 *  用于检测 bug。
 *
 * <p>As of the Java 2 platform v1.2, this class was retrofitted to
 * implement the {@link List} interface, making it a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.  Unlike the new collection
 * implementations, {@code Vector} is synchronized.  If a thread-safe
 * implementation is not needed, it is recommended to use {@link
 * ArrayList} in place of {@code Vector}.
 *从 Java 1.2 开始，这个类被修改为实现 List 接口，使其成为了
 * Java Collections Framework 的成员。和新的集合类不同的时，Vector
 *  是同步的。如果不考虑线程安全，推荐使用 ArrayList。
 *
 * @author  Lee Boynton
 * @author  Jonathan Payne
 * @see Collection
 * @see LinkedList
 * @since   JDK1.0
 *  @Saber-01 Vector方法都加上了synchroized语句，在多线程环境下效率
 *  不高，现在大多不再使用了。
 *  ArrayList与Vector拥有相同的扩容机制
 *  不同点在于ArrayList没有设置增长率，默认扩容为1.5倍。Vector设置了
 *  增长率。
 */

public class Vector<E>
        extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable
{
    /**
     * The array buffer into which the components of the vector are
     * stored. The capacity of the vector is the length of this array buffer,
     * and is at least large enough to contain all the vector's elements.
     *存储向量组件的数组缓冲区。向量的大小就是数组的长度，其大小至少
     * 应该所有的向量元素。
     * <p>Any array elements following the last element in the Vector are null.
     *向量中最后一个元素后面的任何数组元素都为 null。
     * @serial
     */
    protected Object[] elementData;

    /**
     * The number of valid components in this {@code Vector} object.
     * Components {@code elementData[0]} through
     * {@code elementData[elementCount-1]} are the actual items.
     * 在 Vector 对象中有效部件的数量，elementData[0] 到
     *   elementData[elementCount - 1]是真实存在的部件。
     * @serial
     */
    protected int elementCount;

    /**
     * The amount by which the capacity of the vector is automatically
     * incremented when its size becomes greater than its capacity.  If
     * the capacity increment is less than or equal to zero, the capacity
     * of the vector is doubled each time it needs to grow.
     *向量的容量在 size 大于 capacity 时增加的量。如果容量的增量小于或
     * 等于 0，向量的 capacity 就增加一倍。
     * @serial
     */
    protected int capacityIncrement;

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = -2767605614048989439L;
    //序列化，跟版本相关。

    /**
     * Constructs an empty vector with the specified initial capacity and
     * capacity increment.
     * 根据指定的初始容量和容量增量构造空向量。
     * @param   initialCapacity     the initial capacity of the vector
     * @param   capacityIncrement   the amount by which the capacity is
     *                              increased when the vector overflows
     * @throws IllegalArgumentException if the specified initial capacity
     *         is negative
     */
    public Vector(int initialCapacity, int capacityIncrement) {
        super();
        if (initialCapacity < 0)  //指定初始容量小于0，抛出异常
            throw new IllegalArgumentException("Illegal Capacity: "+
                    initialCapacity);
        this.elementData = new Object[initialCapacity];
        this.capacityIncrement = capacityIncrement;
    }

    /**
     * Constructs an empty vector with the specified initial capacity and
     * with its capacity increment equal to zero.
     * 根据指定的初始容量构造空向量，容量增量为 0。
     * @param   initialCapacity   the initial capacity of the vector
     * @throws IllegalArgumentException if the specified initial capacity
     *         is negative
     */
    public Vector(int initialCapacity) {  //只指定初始容量，则容量增量默认为0.
        this(initialCapacity, 0);
    }

    /**
     * Constructs an empty vector so that its internal data array
     * has size {@code 10} and its standard capacity increment is
     * zero.
     * //不带参数的构造函数。
     * 则构造初始容量为 10，标准容量增量为 0 的空向量。
     */
    public Vector() {
        this(10);
    }

    /**
     * Constructs a vector containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *构造包含指定集合所有元素的向量，其存储的顺序为集合迭代器返回
     *的顺序。
     *
     * @param c the collection whose elements are to be placed into this
     *       vector
     * @throws NullPointerException if the specified collection is null
     * @since   1.2
     */
    public Vector(Collection<? extends E> c) {
        elementData = c.toArray();
        elementCount = elementData.length;
        // c.toArray might (incorrectly) not return Object[] (see 6260652)
        if (elementData.getClass() != Object[].class)
            elementData = Arrays.copyOf(elementData, elementCount, Object[].class);
    }

    /**
     * Copies the components of this vector into the specified array.
     * The item at index {@code k} in this vector is copied into
     * component {@code k} of {@code anArray}.
     *把向量的部件全部复制到指定数组里。向量中索引为 k 的元素将会
     * 复制到 anArray 中索引为 k 的位置。
     * @param  anArray the array into which the components get copied
     * @throws NullPointerException if the given array is null  如果参数为null，抛出空指针错误
     * @throws IndexOutOfBoundsException if the specified array is not
     *         large enough to hold all the components of this vector   索引错误，即指定数组容量太小
     * @throws ArrayStoreException if a component of this vector is not of
     *         a runtime type that can be stored in the specified array  类型错误
     * @see #toArray(Object[])
     */
    public synchronized void copyInto(Object[] anArray) {
        System.arraycopy(elementData, 0, anArray, 0, elementCount);
    }

    /**
     * Trims the capacity of this vector to be the vector's current
     * size. If the capacity of this vector is larger than its current
     * size, then the capacity is changed to equal the size by replacing
     * its internal data array, kept in the field {@code elementData},
     * with a smaller one. An application can use this operation to
     * minimize the storage of a vector.
     *  把向量的容量修剪为当前的 size。如果向量的容量大于当前大小，那么
     *   通过替换掉内部用来存储元素的 elementData 来将容量更改为等于其
     *  当前大小。可以应用此操作最小化向量的存储空间。
     */
    public synchronized void trimToSize() {
        modCount++;
        int oldCapacity = elementData.length;
        if (elementCount < oldCapacity) { // 有效部件的数量少于数组的大小，需要缩减数组。
            elementData = Arrays.copyOf(elementData, elementCount);
        }
    }

    /**
     * Increases the capacity of this vector, if necessary, to ensure
     * that it can hold at least the number of components specified by
     * the minimum capacity argument.
     * 如果必要，增加向量的容量，确保它至少可以容纳最小参数指定的部件
     *   数量。
     * <p>If the current capacity of this vector is less than
     * {@code minCapacity}, then its capacity is increased by replacing its
     * internal data array, kept in the field {@code elementData}, with a
     * larger one.  The size of the new data array will be the old size plus
     * {@code capacityIncrement}, unless the value of
     * {@code capacityIncrement} is less than or equal to zero, in which case
     * the new capacity will be twice the old capacity; but if this new size
     * is still smaller than {@code minCapacity}, then the new capacity will
     * be {@code minCapacity}.
     *如果这个向量的当前容量小于 minCapacity，那么通过把 elementData
     * 替换成一个更大的数组来增加容量。新数组的大小是原数组的大小加上
     * capacityIncrement 的大小，除非 capacityIncrement 小于或等于 0，
     *  这种情况下新数组的容量是原来的两倍。如果扩容后的大小还是比
     *  minCapacity 小，那么直接将容量变成 minCapacity 大小。
     * @param minCapacity the desired minimum capacity
     */
    public synchronized void ensureCapacity(int minCapacity) {
        if (minCapacity > 0) {
            modCount++;
            ensureCapacityHelper(minCapacity);
        }
    }

    /**
     * This implements the unsynchronized semantics of ensureCapacity.
     * Synchronized methods in this class can internally call this
     * method for ensuring capacity without incurring the cost of an
     * extra synchronization.
     *这一方法实现了 ensureCapacity 的非同步语义。该类中的同步方法
     * 可以在内部调用此方法，以确保容量，而不会产生额外同步的成本。
     * @see #ensureCapacity(int)
     */
    private void ensureCapacityHelper(int minCapacity) {
        // overflow-conscious code
        if (minCapacity - elementData.length > 0)
            grow(minCapacity);
    }

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     *  要分配的数组的最大大小。
     * 一些虚拟机在数组中保留一些头信息。
     *  尝试分配更大的数组可能会导致 OutOfMemoryError：请求的数组大
     *   小超过虚拟机限制。
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private void grow(int minCapacity) {
        // 计算新数组的空间大小 newCapacity
        // overflow-conscious code
        int oldCapacity = elementData.length;       //记录原来数组的容量，
        //如果Vector增量大于0，新容量为旧容量+增量，
        //如果Vector的增量小于等于0，则新容量为旧容量的2倍。
        int newCapacity = oldCapacity + ((capacityIncrement > 0) ?
                capacityIncrement : oldCapacity);
        if (newCapacity - minCapacity < 0)   //如果扩容后的新容量还是比指定的最小容量小，或者上一步可能存在溢出，
            newCapacity = minCapacity;       //则直接使用指定的最小容量作为新的容量，
        if (newCapacity - MAX_ARRAY_SIZE > 0)       //如果此时上述过程得到的新容量大于了最大的能分配的数组的大小
            newCapacity = hugeCapacity(minCapacity);      //还需要调用hugeCapacity方法判断。
        // elementData 最终指向 newCapacity 大小的新数组空间
        elementData = Arrays.copyOf(elementData, newCapacity);
    }
    //判断扩容的新容量是通过minCapacity赋值，还是通过老容量扩容得到的。两者在新容量超出最大可分配数组大小时返回的值不同。
    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow  //如果最小容量溢出，
            throw new OutOfMemoryError();    //抛出内存溢出异常
        return (minCapacity > MAX_ARRAY_SIZE) ?     //如果指定最小容量大于最大可以分配的数组的大小，
                Integer.MAX_VALUE :        //新容量为整数的最大值。Integer.MAX_VALUE
                MAX_ARRAY_SIZE;     //否则将新容量设置为最大可分配的数组的大小。
    }

    /**
     * Sets the size of this vector. If the new size is greater than the
     * current size, new {@code null} items are added to the end of
     * the vector. If the new size is less than the current size, all
     * components at index {@code newSize} and greater are discarded.
     * 设置向量的 size （不是 capacity）。如果新的 size 比当前的 size
     *  要大，将 null 元素添加到向量末尾。如果新的 size 比当前的 size
     *   要小，将 newSize 索引及之后的元素设置为 null。
     *
     * @param  newSize   the new size of this vector
     * @throws ArrayIndexOutOfBoundsException if the new size is negative
     */
    public synchronized void setSize(int newSize) {
        modCount++;
        if (newSize > elementCount) {     //不够要扩容，
            ensureCapacityHelper(newSize);
        } else {
            for (int i = newSize ; i < elementCount ; i++) {  //够的话，要将数组newsize后的值全部重置为null 。
                elementData[i] = null;
            }
        }
        elementCount = newSize;   //元素个数更新为newSize.
    }

    /**
     * Returns the current capacity of this vector.
     * 返回向量当前容量。
     * @return  the current capacity (the length of its internal
     *          data array, kept in the field {@code elementData}
     *          of this vector)
     */
    public synchronized int capacity() {
        return elementData.length;
    }

    /**
     * Returns the number of components in this vector.
     *返回向量中部件的数量（size）
     * @return  the number of components in this vector
     */
    public synchronized int size() {
        return elementCount;
    }

    /**
     * Tests if this vector has no components.
     *测试向量是否不包含任何部件（元素）。
     * @return  {@code true} if and only if this vector has
     *          no components, that is, its size is zero;
     *          {@code false} otherwise.
     */
    public synchronized boolean isEmpty() {
        return elementCount == 0;
    }

    /**
     * Returns an enumeration of the components of this vector. The
     * returned {@code Enumeration} object will generate all items in
     * this vector. The first item generated is the item at index {@code 0},
     * then the item at index {@code 1}, and so on.
     *返回向量部件的枚举类。返回的枚举类包含向量中的所有元素。第一个
     * 元素是索引为 0 的元素，第二个是索引为 1 的元素，以此类推。
     *
     * @return  an enumeration of the components of this vector
     * @see     Iterator
     */
    public Enumeration<E> elements() {
        return new Enumeration<E>() {
            int count = 0;

            public boolean hasMoreElements() {
                return count < elementCount;
            }

            public E nextElement() {
                synchronized (Vector.this) {
                    if (count < elementCount) {
                        return elementData(count++);
                    }
                }
                throw new NoSuchElementException("Vector Enumeration");
            }
        };
    }

    /**
     * Returns {@code true} if this vector contains the specified element.
     * More formally, returns {@code true} if and only if this vector
     * contains at least one element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *如果向量包含指定元素返回 true。
     *
     * @param o element whose presence in this vector is to be tested
     * @return {@code true} if this vector contains the specified element
     */
    public boolean contains(Object o) {
        return indexOf(o, 0) >= 0;
    }

    /**
     * Returns the index of the first occurrence of the specified element
     * in this vector, or -1 if this vector does not contain the element.
     * More formally, returns the lowest index {@code i} such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     *返回向量中第一次出现指定元素的索引位置，如果不包含该元素返回 -1。
     * @param o element to search for
     * @return the index of the first occurrence of the specified element in
     *         this vector, or -1 if this vector does not contain the element
     */
    public int indexOf(Object o) {
        return indexOf(o, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified element in
     * this vector, searching forwards from {@code index}, or returns -1 if
     * the element is not found.
     * More formally, returns the lowest index {@code i} such that
     * <tt>(i&nbsp;&gt;=&nbsp;index&nbsp;&amp;&amp;&nbsp;(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i))))</tt>,
     * or -1 if there is no such index.
     *从指定索引开始往后遍历，返回向量中第一次出现指定元素的索引
     * 位置，如果不包含该元素返回 -1。
     * @param o element to search for
     * @param index index to start searching from
     * @return the index of the first occurrence of the element in
     *         this vector at position {@code index} or later in the vector;
     *         {@code -1} if the element is not found.
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @see     Object#equals(Object)
     */
    public synchronized int indexOf(Object o, int index) {
        if (o == null) {
            for (int i = index ; i < elementCount ; i++)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = index ; i < elementCount ; i++)
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * Returns the index of the last occurrence of the specified element
     * in this vector, or -1 if this vector does not contain the element.
     * More formally, returns the highest index {@code i} such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     *返回向量中最后一次出现指定元素的索引，不包含该元素返回 -1。
     * @param o element to search for
     * @return the index of the last occurrence of the specified element in
     *         this vector, or -1 if this vector does not contain the element
     */
    public synchronized int lastIndexOf(Object o) {
        return lastIndexOf(o, elementCount-1);
    }

    /**
     * Returns the index of the last occurrence of the specified element in
     * this vector, searching backwards from {@code index}, or returns -1 if
     * the element is not found.
     * More formally, returns the highest index {@code i} such that
     * <tt>(i&nbsp;&lt;=&nbsp;index&nbsp;&amp;&amp;&nbsp;(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i))))</tt>,
     * or -1 if there is no such index.
     *从指定索引开始往前遍历，返回向量中第一次出现指定元素的索引
     *  位置（向量中最后一次出现的索引位置），如果不包含该元素返回 -1。
     * @param o element to search for
     * @param index index to start searching backwards from
     * @return the index of the last occurrence of the element at position
     *         less than or equal to {@code index} in this vector;
     *         -1 if the element is not found.
     * @throws IndexOutOfBoundsException if the specified index is greater
     *         than or equal to the current size of this vector
     */
    public synchronized int lastIndexOf(Object o, int index) {
        if (index >= elementCount)
            throw new IndexOutOfBoundsException(index + " >= "+ elementCount);

        if (o == null) {
            for (int i = index; i >= 0; i--)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = index; i >= 0; i--)
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * Returns the component at the specified index.
     *返回指定索引位置的部件（元素）。
     * <p>This method is identical in functionality to the {@link #get(int)}
     * method (which is part of the {@link List} interface).
     * 这个方法等价于 List 接口的 get 方法。
     * @param      index   an index into this vector
     * @return     the component at the specified index
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index >= size()})
     */
    public synchronized E elementAt(int index) {
        if (index >= elementCount) {
            throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
        }

        return elementData(index);
    }

    /**
     * Returns the first component (the item at index {@code 0}) of
     * this vector.
     *返回向量中的第一个元素（索引为 0 的元素）。
     * @return     the first component of this vector
     * @throws NoSuchElementException if this vector has no components
     */
    public synchronized E firstElement() {
        if (elementCount == 0) {
            throw new NoSuchElementException();
        }
        return elementData(0);
    }

    /**
     * Returns the last component of the vector.
     * 返回向量中的最后一个元素。
     * @return  the last component of the vector, i.e., the component at index
     *          <code>size()&nbsp;-&nbsp;1</code>.
     * @throws NoSuchElementException if this vector is empty
     */
    public synchronized E lastElement() {
        if (elementCount == 0) {
            throw new NoSuchElementException();
        }
        return elementData(elementCount - 1);
    }

    /**
     * Sets the component at the specified {@code index} of this
     * vector to be the specified object. The previous component at that
     * position is discarded.
     * 把向量中指定索引位置设定为指定元素。
     * <p>The index must be a value greater than or equal to {@code 0}
     * and less than the current size of the vector.
     *指定索引必须大于等于 0，小于向量的当前 size。
     * <p>This method is identical in functionality to the
     * {@link #set(int, Object) set(int, E)}
     * method (which is part of the {@link List} interface). Note that the
     * {@code set} method reverses the order of the parameters, to more closely
     * match array usage.  Note also that the {@code set} method returns the
     * old value that was stored at the specified position.
     *此方法等价于 List 接口的 set 方法。注意 set 方法为了更符合数组的
     *  使用，参数的顺序不同。注意 set 方法返回了原来储存在该位置的元素。
     * @param      obj     what the component is to be set to
     * @param      index   the specified index
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index >= size()})
     */
    public synchronized void setElementAt(E obj, int index) {
        if (index >= elementCount) {
            throw new ArrayIndexOutOfBoundsException(index + " >= " +
                    elementCount);
        }
        elementData[index] = obj;
    }

    /**
     * Deletes the component at the specified index. Each component in
     * this vector with an index greater or equal to the specified
     * {@code index} is shifted downward to have an index one
     * smaller than the value it had previously. The size of this vector
     * is decreased by {@code 1}.
     * 删除指定索引处的元素。向量中索引大于等于该指定索引的所有元素，
     *  向左移动一位，即索引减一。向量的大小也减一。
     * <p>The index must be a value greater than or equal to {@code 0}
     * and less than the current size of the vector.
     *指定索引必须大于等于 0，小于向量的当前大小。
     *
     * <p>This method is identical in functionality to the {@link #remove(int)}
     * method (which is part of the {@link List} interface).  Note that the
     * {@code remove} method returns the old value that was stored at the
     * specified position.
     *此方法等价于 List 接口的 remove 方法。注意 remove 返回了原来
     * 储存在该位置的元素，此方法没有。
     * @param      index   the index of the object to remove
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index >= size()})
     */
    public synchronized void removeElementAt(int index) {
        modCount++;
        if (index >= elementCount) {
            throw new ArrayIndexOutOfBoundsException(index + " >= " +
                    elementCount);
        }
        else if (index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        int j = elementCount - index - 1;
        if (j > 0) {
            // 将 elementData 数组里从索引为 index + 1 的元素开始，复制
            // 到 elementData 里索引为 index 的位置，复制元素的个数为 j 。
            System.arraycopy(elementData, index + 1, elementData, index, j);
        }
        elementCount--;
        elementData[elementCount] = null; /* to let gc do its work */
    }

    /**
     * Inserts the specified object as a component in this vector at the
     * specified {@code index}. Each component in this vector with
     * an index greater or equal to the specified {@code index} is
     * shifted upward to have an index one greater than the value it had
     * previously.
     *在指定索引处插入新元素。向量中每个大于等于当前索引的元素向后
     * 移动一位，即索引加一。
     * <p>The index must be a value greater than or equal to {@code 0}
     * and less than or equal to the current size of the vector. (If the
     * index is equal to the current size of the vector, the new element
     * is appended to the Vector.)
     *指定索引必须大于等于 0，小于等于向量当前大小。（如果指定索引
     * 等于向量的当前大小，新元素被添加到向量末尾。）
     * <p>This method is identical in functionality to the
     * {@link #add(int, Object) add(int, E)}
     * method (which is part of the {@link List} interface).  Note that the
     * {@code add} method reverses the order of the parameters, to more closely
     * match array usage.
     *此方法等同于 List 接口的 add 方法。
     * @param      obj     the component to insert
     * @param      index   where to insert the new component
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     */
    public synchronized void insertElementAt(E obj, int index) {
        modCount++;
        if (index > elementCount) {
            throw new ArrayIndexOutOfBoundsException(index
                    + " > " + elementCount);
        }
        // 插入元素的一般步骤是，首先检查向量的容量是否足够大，
        // 然后使用 System.arrayCopy 将索引之后的元素右移，最后设置索引处
        // 的元素为指定元素。
        ensureCapacityHelper(elementCount + 1);
        System.arraycopy(elementData, index, elementData, index + 1, elementCount - index);
        elementData[index] = obj;
        elementCount++;
    }

    /**
     * Adds the specified component to the end of this vector,
     * increasing its size by one. The capacity of this vector is
     * increased if its size becomes greater than its capacity.
     *把指定元素添加到向量的末尾，并在向量的大小上加一。若向量的 size
     * 大于 capacity，那么 capacity 相应增加。此方法等同于 List 接口的
     *   add 方法。
     * <p>This method is identical in functionality to the
     * {@link #add(Object) add(E)}
     * method (which is part of the {@link List} interface).
     *
     * @param   obj   the component to be added
     */
    public synchronized void addElement(E obj) {
        modCount++;
        ensureCapacityHelper(elementCount + 1);
        elementData[elementCount++] = obj;
    }

    /**
     * Removes the first (lowest-indexed) occurrence of the argument
     * from this vector. If the object is found in this vector, each
     * component in the vector with an index greater or equal to the
     * object's index is shifted downward to have an index one smaller
     * than the value it had previously.
     * 删除向量中第一次出现（索引最小）的和指定元素匹配的元素。如果
     * 该元素存在，将所有索引大于等于该索引的元素向前移动一位，即
     *  索引减一。
     * <p>This method is identical in functionality to the
     * {@link #remove(Object)} method (which is part of the
     * {@link List} interface).
     *此方法等同于 List 接口中的 remove 方法。
     * @param   obj   the component to be removed
     * @return  {@code true} if the argument was a component of this
     *          vector; {@code false} otherwise.
     */
    public synchronized boolean removeElement(Object obj) {
        modCount++;
        int i = indexOf(obj);   //得到要删除值的index
        if (i >= 0) {
            removeElementAt(i);    //调用参数为index版本的删除函数。
            return true;
        }
        return false;
    }

    /**
     * Removes all components from this vector and sets its size to zero.
     *删除向量中所有元素，并将向量的大小设为 0。
     * <p>This method is identical in functionality to the {@link #clear}
     * method (which is part of the {@link List} interface).
     * 等同于clear操作。
     */
    public synchronized void removeAllElements() {
        modCount++;
        // Let gc do its work
        for (int i = 0; i < elementCount; i++)
            elementData[i] = null;

        elementCount = 0;
    }

    /**
     * Returns a clone of this vector. The copy will contain a
     * reference to a clone of the internal data array, not a reference
     * to the original internal data array of this {@code Vector} object.
     *返回向量的克隆。此克隆会保留指向新的内部数组的引用，而不是指向
     * 原数组的引用。
     * @return  a clone of this vector
     */
    public synchronized Object clone() {
        try {
            @SuppressWarnings("unchecked")
            Vector<E> v = (Vector<E>) super.clone();
            v.elementData = Arrays.copyOf(elementData, elementCount);
            v.modCount = 0;
            return v;
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
    }

    /**
     * Returns an array containing all of the elements in this Vector
     * in the correct order.
     *按顺序返回一个包含所有元素的数组。
     * @since 1.2
     */
    public synchronized Object[] toArray() {
        return Arrays.copyOf(elementData, elementCount);
    }

    /**
     * Returns an array containing all of the elements in this Vector in the
     * correct order; the runtime type of the returned array is that of the
     * specified array.  If the Vector fits in the specified array, it is
     * returned therein.  Otherwise, a new array is allocated with the runtime
     * type of the specified array and the size of this Vector.
     *按顺序返回一个包含向量中所有元素的数组；返回的数组即为参数指定
     * 的数组。如果向量元素能全部保存在该数组中，那么返回该数组。否则，
     * 开辟一个新的数组空间用来保存向量中的元素，并返回该数组
     *
     * <p>If the Vector fits in the specified array with room to spare
     * (i.e., the array has more elements than the Vector),
     * the element in the array immediately following the end of the
     * Vector is set to null.  (This is useful in determining the length
     * of the Vector <em>only</em> if the caller knows that the Vector
     * does not contain any null elements.)
     * 如果指定的数组还有多余的空间（即数组长度大于向量元素个数），
     * 数组中的剩余元素设置为 null。
     *
     * @param a the array into which the elements of the Vector are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing the elements of the Vector
     * @throws ArrayStoreException if the runtime type of a is not a supertype
     * of the runtime type of every element in this Vector
     * @throws NullPointerException if the given array is null
     * @since 1.2
     */
    @SuppressWarnings("unchecked")
    public synchronized <T> T[] toArray(T[] a) {
        if (a.length < elementCount)
            return (T[]) Arrays.copyOf(elementData, elementCount, a.getClass());

        System.arraycopy(elementData, 0, a, 0, elementCount);

        if (a.length > elementCount)
            a[elementCount] = null;

        return a;
    }

    // Positional Access Operations
    // 位置访问操作
    @SuppressWarnings("unchecked")
    E elementData(int index) {
        return (E) elementData[index];
    }

    /**
     * Returns the element at the specified position in this Vector.
     * // 返回指定索引位置的元素
     *
     * @param index index of the element to return
     * @return object at the specified index
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *            ({@code index < 0 || index >= size()})
     * @since 1.2
     */
    public synchronized E get(int index) {
        if (index >= elementCount)
            throw new ArrayIndexOutOfBoundsException(index);

        return elementData(index);
    }

    /**
     * Replaces the element at the specified position in this Vector with the
     * specified element.
     *  用指定元素替换向量中指定位置的元素，并返回原来的元素。
     * @param index index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index >= size()})
     * @since 1.2
     */
    public synchronized E set(int index, E element) {
        if (index >= elementCount)
            throw new ArrayIndexOutOfBoundsException(index);

        E oldValue = elementData(index);
        elementData[index] = element;
        return oldValue;
    }

    /**
     * Appends the specified element to the end of this Vector.
     * 将指定的元素添加到向量末尾。
     * @param e element to be appended to this Vector
     * @return {@code true} (as specified by {@link Collection#add})
     * @since 1.2
     */
    public synchronized boolean add(E e) {
        modCount++;
        ensureCapacityHelper(elementCount + 1);
        elementData[elementCount++] = e;
        return true;
    }

    /**
     * Removes the first occurrence of the specified element in this Vector
     * If the Vector does not contain the element, it is unchanged.  More
     * formally, removes the element with the lowest index i such that
     * {@code (o==null ? get(i)==null : o.equals(get(i)))} (if such
     * an element exists).
     *删除向量中和指定元素匹配的第一个元素。如果向量不包含该元素，
     * 不做出任何改变。
     * @param o element to be removed from this Vector, if present
     * @return true if the Vector contained the specified element
     * @since 1.2
     */
    public boolean remove(Object o) {
        return removeElement(o);
    }

    /**
     * Inserts the specified element at the specified position in this Vector.
     * Shifts the element currently at that position (if any) and any
     * subsequent elements to the right (adds one to their indices).
     * 在向量中指定位置插入指定元素。将当前位置和之后的元素向右移动
     *      一位（索引加一）。
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     * @since 1.2
     */
    public void add(int index, E element) {
        insertElementAt(element, index);
    }

    /**
     * Removes the element at the specified position in this Vector.
     * Shifts any subsequent elements to the left (subtracts one from their
     * indices).  Returns the element that was removed from the Vector.
     *删除指定位置处的元素。将之后的元素向左移动一位（索引减一）。
     *并返回从向量中删除的元素。
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index >= size()})
     * @param index the index of the element to be removed
     * @return element that was removed
     * @since 1.2
     */
    public synchronized E remove(int index) {
        modCount++;
        if (index >= elementCount)
            throw new ArrayIndexOutOfBoundsException(index);
        E oldValue = elementData(index); //先取出索引位置的旧值，用于返回

        int numMoved = elementCount - index - 1;//计算需要向左移动的数量
        if (numMoved > 0)
            //调用System.arraycopy函数从将数组index+1开始的numMoved个元素移动到从index开始，即index后面的值全部向左移动一位
            System.arraycopy(elementData, index+1, elementData, index,
                    numMoved);
        elementData[--elementCount] = null; // Let gc do its work

        return oldValue;
    }

    /**
     * Removes all of the elements from this Vector.  The Vector will
     * be empty after this call returns (unless it throws an exception).
     *删除向量的所有元素。此方法调用后向量为空（除非抛出异常）。
     *
     * @since 1.2
     */
    public void clear() {
        removeAllElements();
    }

    // Bulk Operations
// 批量操作
    /**
     * Returns true if this Vector contains all of the elements in the
     * specified Collection.
     * 如果向量包含指定集合中的所有元素，返回 true。
     * @param   c a collection whose elements will be tested for containment
     *          in this Vector
     * @return true if this Vector contains all of the elements in the
     *         specified collection
     * @throws NullPointerException if the specified collection is null
     */
    public synchronized boolean containsAll(Collection<?> c) {
        return super.containsAll(c);
    }

    /**
     * Appends all of the elements in the specified Collection to the end of
     * this Vector, in the order that they are returned by the specified
     * Collection's Iterator.  The behavior of this operation is undefined if
     * the specified Collection is modified while the operation is in progress.
     * (This implies that the behavior of this call is undefined if the
     * specified Collection is this Vector, and this Vector is nonempty.)
     *把指定集合中的所有元素添加到向量的末尾，以指定集合迭代器返回
     * 的顺序。如果操作进行过程中，指定集合被修改，则此操作的行为
     *  未定义。
     * @param c elements to be inserted into this Vector
     * @return {@code true} if this Vector changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     * @since 1.2
     */
    public synchronized boolean addAll(Collection<? extends E> c) {
        modCount++;
        Object[] a = c.toArray();
        int numNew = a.length;
        // 先将向量扩容到足够大，然后把指定集合的数组复制到向量的数组里。
        ensureCapacityHelper(elementCount + numNew);
        System.arraycopy(a, 0, elementData, elementCount, numNew);
        elementCount += numNew;
        return numNew != 0;
    }

    /**
     * Removes from this Vector all of its elements that are contained in the
     * specified Collection.
     *删除向量中和指定集合相同的所有元素。
     * @param c a collection of elements to be removed from the Vector
     * @return true if this Vector changed as a result of the call
     * @throws ClassCastException if the types of one or more elements
     *         in this vector are incompatible with the specified
     *         collection
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this vector contains one or more null
     *         elements and the specified collection does not support null
     *         elements
     * (<a href="Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @since 1.2
     */
    public synchronized boolean removeAll(Collection<?> c) {
        return super.removeAll(c);
    }

    /**
     * Retains only the elements in this Vector that are contained in the
     * specified Collection.  In other words, removes from this Vector all
     * of its elements that are not contained in the specified Collection.
     *保留向量中和指定元素相同的部分。换句话说，删除向量中和指定
     * 集合元素不同的部分。
     * @param c a collection of elements to be retained in this Vector
     *          (all other elements are removed)
     * @return true if this Vector changed as a result of the call
     * @throws ClassCastException if the types of one or more elements
     *         in this vector are incompatible with the specified
     *         collection
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this vector contains one or more null
     *         elements and the specified collection does not support null
     *         elements
     *         (<a href="Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @since 1.2
     */
    public synchronized boolean retainAll(Collection<?> c) {
        return super.retainAll(c);
    }

    /**
     * Inserts all of the elements in the specified Collection into this
     * Vector at the specified position.  Shifts the element currently at
     * that position (if any) and any subsequent elements to the right
     * (increases their indices).  The new elements will appear in the Vector
     * in the order that they are returned by the specified Collection's
     * iterator.
     *把指定集合的所有元素插入到向量指定的位置。把当前位置和其之后
     * 的元素向后移动（索引减小）。插入的顺序为指定集合迭代器返回的
     * 顺序。
     * @param index index at which to insert the first element from the
     *              specified collection
     * @param c elements to be inserted into this Vector
     * @return {@code true} if this Vector changed as a result of the call
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     * @throws NullPointerException if the specified collection is null
     * @since 1.2
     */
    public synchronized boolean addAll(int index, Collection<? extends E> c) {
        modCount++;
        // 判断索引是否在范围内
        if (index < 0 || index > elementCount)
            throw new ArrayIndexOutOfBoundsException(index);

        Object[] a = c.toArray();    //得到集合数组。
        int numNew = a.length;      //取得要插入的集合的数量，即长度
        ensureCapacityHelper(elementCount + numNew); //确保Vector可以容纳所有集合中元素

        int numMoved = elementCount - index;//计算需要移动的元素个数。
        if (numMoved > 0)//如果不是在最后一个元素后面加入，就需要移动元素。
            //将数组从index位置开始的numMoved数向右移动numNew个位置，腾出位置存放集合c的元素。
            System.arraycopy(elementData, index, elementData, index + numNew,
                    numMoved);
        //腾出位置后，进行复制。
        System.arraycopy(a, 0, elementData, index, numNew);
        elementCount += numNew;
        return numNew != 0;
    }

    /**
     * Compares the specified Object with this Vector for equality.  Returns
     * true if and only if the specified Object is also a List, both Lists
     * have the same size, and all corresponding pairs of elements in the two
     * Lists are <em>equal</em>.  (Two elements {@code e1} and
     * {@code e2} are <em>equal</em> if {@code (e1==null ? e2==null :
     * e1.equals(e2))}.)  In other words, two Lists are defined to be
     * equal if they contain the same elements in the same order.
     *比较指定对象和此向量是否相等。如果指定对象也是 List，两个集合
     大小相同，且对应的元素均相等，那么返回 true。换句话说，如果两个
     * 集合对应元素对应相等，那么说两个集合相等。
     * @param o the Object to be compared for equality with this Vector
     * @return true if the specified Object is equal to this Vector
     */
    public synchronized boolean equals(Object o) {
        return super.equals(o);
    }

    /**
     * Returns the hash code value for this Vector.
     *  返回集合的 hash 值
     */
    public synchronized int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns a string representation of this Vector, containing
     * the String representation of each element.
     * 返回集合的字符串表示，包括集合所有元素的字符串表示。
     */
    public synchronized String toString() {
        return super.toString();
    }

    /**
     * Returns a view of the portion of this List between fromIndex,
     * inclusive, and toIndex, exclusive.  (If fromIndex and toIndex are
     * equal, the returned List is empty.)  The returned List is backed by this
     * List, so changes in the returned List are reflected in this List, and
     * vice-versa.  The returned List supports all of the optional List
     * operations supported by this List.
     *返回列表从 fromIndex（包含）到 toIndex（不包含）的视图。（如果
     *  fromIndex 等于 toIndex，那么返回的列表为空。）返回的列表由此
     *  列表支撑，所以返回列表的任何改变将会影响此列表，反之亦然。返回
     *   的列表支持所有此列表支持的操作。
     * <p>This method eliminates the need for explicit range operations (of
     * the sort that commonly exist for arrays).  Any operation that expects
     * a List can be used as a range operation by operating on a subList view
     * instead of a whole List.  For example, the following idiom
     * removes a range of elements from a List:
     *
     * <pre>
     *      list.subList(from, to).clear();
     * </pre>
     * Similar idioms may be constructed for indexOf and lastIndexOf,
     * and all of the algorithms in the Collections class can be applied to
     * a subList.
     *
     * <p>The semantics of the List returned by this method become undefined if
     * the backing list (i.e., this List) is <i>structurally modified</i> in
     * any way other than via the returned List.  (Structural modifications are
     * those that change the size of the List, or otherwise perturb it in such
     * a fashion that iterations in progress may yield incorrect results.)
     *如果支撑列表在任何情况下进行了结构上的修改，那么返回列表的语义
     *   未知。（结构性的修改指的时改变列表大小。
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex high endpoint (exclusive) of the subList
     * @return a view of the specified range within this List
     * @throws IndexOutOfBoundsException if an endpoint index value is out of range
     *         {@code (fromIndex < 0 || toIndex > size)}
     * @throws IllegalArgumentException if the endpoint indices are out of order
     *         {@code (fromIndex > toIndex)}
     */
    public synchronized List<E> subList(int fromIndex, int toIndex) {
        return Collections.synchronizedList(super.subList(fromIndex, toIndex),
                this);
    }

    /**
     * Removes from this list all of the elements whose index is between
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
     * Shifts any succeeding elements to the left (reduces their index).
     * This call shortens the list by {@code (toIndex - fromIndex)} elements.
     * (If {@code toIndex==fromIndex}, this operation has no effect.)
     * 删除 fromIndex（包含）到 toIndex（不包含）范围内的所有元素。
     *   将之后的元素向左移动（减小索引），如果 toIndex == fromIndex，
     *   此操作没有任何影响。
     */
    protected synchronized void removeRange(int fromIndex, int toIndex) {
        modCount++;
        int numMoved = elementCount - toIndex;
        System.arraycopy(elementData, toIndex, elementData, fromIndex,
                numMoved);

        // Let gc do its work
        int newElementCount = elementCount - (toIndex-fromIndex);
        while (elementCount != newElementCount)
            elementData[--elementCount] = null;
    }

    /**
     * Save the state of the {@code Vector} instance to a stream (that
     * is, serialize it).
     *  把向量实例的状态加载到流里面（即序列化）。
     *   此方法确保流数据的持久化。
     * This method performs synchronization to ensure the consistency
     * of the serialized data.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        final java.io.ObjectOutputStream.PutField fields = s.putFields();
        final Object[] data;
        synchronized (this) {
            fields.put("capacityIncrement", capacityIncrement);
            fields.put("elementCount", elementCount);
            data = elementData.clone();
        }
        fields.put("elementData", data);
        s.writeFields();
    }

    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence), starting at the specified position in the list.
     * The specified index indicates the first element that would be
     * returned by an initial call to {@link ListIterator#next next}.
     * An initial call to {@link ListIterator#previous previous} would
     * return the element with the specified index minus one.
     *返回列表的迭代器（以正确的顺序），从列表指定位置开始。
     *返回的迭代器支持 fast-fail。
     * <p>The returned list iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public synchronized ListIterator<E> listIterator(int index) {
        if (index < 0 || index > elementCount)
            throw new IndexOutOfBoundsException("Index: "+index);
        return new ListItr(index);
    }

    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence).
     * 返回列表的迭代器（以正确的顺序），从第一个元素开始。
     *      * 返回的迭代器支持 fast-fail。
     * <p>The returned list iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
     *
     * @see #listIterator(int)
     */
    public synchronized ListIterator<E> listIterator() {
        return new ListItr(0);
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * <p>The returned iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
     *按正确顺序返回列表中元素的迭代器。
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    public synchronized Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * An optimized version of AbstractList.Itr
     *  AbstractList.Itr 的优化版本
     */
    private class Itr implements Iterator<E> {
        int cursor;       // index of next element to return
        int lastRet = -1; // index of last element returned; -1 if no such
        int expectedModCount = modCount;
        // 是否存在下一个元素
        public boolean hasNext() {
            // Racy but within spec, since modifications are checked
            // within or after synchronization in next/previous
            return cursor != elementCount;
        }
        // 返回 cursor指向的元素，并将游标向前移动一位。
        public E next() {
            synchronized (Vector.this) {
                checkForComodification();
                int i = cursor;
                if (i >= elementCount)
                    throw new NoSuchElementException();
                cursor = i + 1;
                return elementData(lastRet = i);
            }
        }

        public void remove() {
            if (lastRet == -1)
                throw new IllegalStateException();
            synchronized (Vector.this) {
                checkForComodification();
                Vector.this.remove(lastRet);
                expectedModCount = modCount;
            }
            cursor = lastRet;
            lastRet = -1;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            synchronized (Vector.this) {
                final int size = elementCount;
                int i = cursor;
                if (i >= size) {
                    return;
                }
                @SuppressWarnings("unchecked")
                final E[] elementData = (E[]) Vector.this.elementData;
                if (i >= elementData.length) {
                    throw new ConcurrentModificationException();
                }
                while (i != size && modCount == expectedModCount) {
                    action.accept(elementData[i++]);
                }
                // update once at end of iteration to reduce heap write traffic
                cursor = i;
                lastRet = i - 1;
                checkForComodification();
            }
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    /**
     * An optimized version of AbstractList.ListItr
     * AbstractList.ListItr 的优化版本
     */
    final class ListItr extends Itr implements ListIterator<E> {
        ListItr(int index) {
            super();
            cursor = index;
        }

        public boolean hasPrevious() {
            return cursor != 0;
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor - 1;
        }
        // 返回 cursor - 1 指向的元素并向往回移动一位
        public E previous() {
            synchronized (Vector.this) {
                checkForComodification();
                int i = cursor - 1;
                if (i < 0)
                    throw new NoSuchElementException();
                cursor = i;
                return elementData(lastRet = i);
            }
        }
        // 把 lastRet 处的元素设置为指定元素
        public void set(E e) {
            if (lastRet == -1)
                throw new IllegalStateException();
            synchronized (Vector.this) {
                checkForComodification();
                Vector.this.set(lastRet, e);
            }
        }
        // 在 cursor 处添加指定元素
        public void add(E e) {
            int i = cursor;
            synchronized (Vector.this) {
                checkForComodification();
                Vector.this.add(i, e);
                expectedModCount = modCount;
            }
            cursor = i + 1;
            lastRet = -1;
        }
    }

    @Override
    public synchronized void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        final int expectedModCount = modCount;
        @SuppressWarnings("unchecked")
        final E[] elementData = (E[]) this.elementData;
        final int elementCount = this.elementCount;
        for (int i=0; modCount == expectedModCount && i < elementCount; i++) {
            action.accept(elementData[i]);
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    // 删除满足条件的元素
    @Override
    @SuppressWarnings("unchecked")
    public synchronized boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        // figure out which elements are to be removed
        // any exception thrown from the filter predicate at this stage
        // will leave the collection unmodified
        int removeCount = 0;
        final int size = elementCount;
        final BitSet removeSet = new BitSet(size);
        final int expectedModCount = modCount;
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
            elementCount = newSize;
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            modCount++;
        }

        return anyToRemove;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final int expectedModCount = modCount;
        final int size = elementCount;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            elementData[i] = operator.apply((E) elementData[i]);
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }


    // 按比较器的规则排序
    @SuppressWarnings("unchecked")
    @Override
    public synchronized void sort(Comparator<? super E> c) {
        final int expectedModCount = modCount;
        Arrays.sort((E[]) elementData, 0, elementCount, c);
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * list.
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
        return new VectorSpliterator<>(this, null, 0, -1, 0);
    }

    /** Similar to ArrayList Spliterator */
    static final class VectorSpliterator<E> implements Spliterator<E> {
        private final Vector<E> list;
        private Object[] array;
        private int index; // current index, modified on advance/split
        private int fence; // -1 until used; then one past last index
        private int expectedModCount; // initialized when fence set

        /** Create new spliterator covering the given  range */
        VectorSpliterator(Vector<E> list, Object[] array, int origin, int fence,
                          int expectedModCount) {
            this.list = list;
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.expectedModCount = expectedModCount;
        }

        private int getFence() { // initialize on first use
            int hi;
            if ((hi = fence) < 0) {
                synchronized(list) {
                    array = list.elementData;
                    expectedModCount = list.modCount;
                    hi = fence = list.elementCount;
                }
            }
            return hi;
        }

        public Spliterator<E> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null :
                    new VectorSpliterator<E>(list, array, lo, index = mid,
                            expectedModCount);
        }

        @SuppressWarnings("unchecked")
        public boolean tryAdvance(Consumer<? super E> action) {
            int i;
            if (action == null)
                throw new NullPointerException();
            if (getFence() > (i = index)) {
                index = i + 1;
                action.accept((E)array[i]);
                if (list.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super E> action) {
            int i, hi; // hoist accesses and checks from loop
            Vector<E> lst; Object[] a;
            if (action == null)
                throw new NullPointerException();
            if ((lst = list) != null) {
                if ((hi = fence) < 0) {
                    synchronized(lst) {
                        expectedModCount = lst.modCount;
                        a = array = lst.elementData;
                        hi = fence = lst.elementCount;
                    }
                }
                else
                    a = array;
                if (a != null && (i = index) >= 0 && (index = hi) <= a.length) {
                    while (i < hi)
                        action.accept((E) a[i++]);
                    if (lst.modCount == expectedModCount)
                        return;
                }
            }
            throw new ConcurrentModificationException();
        }

        public long estimateSize() {
            return (long) (getFence() - index);
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }
}
