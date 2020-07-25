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
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group.  Adapted and released, under explicit permission,
 * from JDK ArrayList.java which carries the following copyright:
 *
 * Copyright 1997 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 */

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import sun.misc.SharedSecrets;

/**
 * ArrayList 的线程安全版本。其中所有的可变操作（add, set 等）都是通过
 * 创建底层数组的新副本来实现的。
 *
 * 这通常开销太大，但是当便利操作的数量远远超过突变时，它可能比替代方法
 * 更有效。如果不能或不想同步遍历，但又需要排除并发线程之间的干扰时，
 * 此类很有用。“快照”模式实现的迭代器方法使用迭代器创建时数组状态的引用。
 * 此数组在迭代器的生命周期内不会改变，因此不可能产生干扰，迭代器不会
 * 抛出 ConcurrentModificationException 异常。创建迭代器之后，迭代器不会对
 * 列表进行添加、删除或更改操作。不支持对迭代器本身的 remove, set, add
 * 进行元素更改操作。这些方法会抛出 UnsupportedOperationException 异常。
 *
 * 允许所有类型的元素，包括 null。
 *
 * 内存一致性：与其它并发集合一样，遵循 happen-before 原则。一个线程将
 * 对象插入到 CopyOnWriteArrayList 的操作发生在访问或删除该元素的操作之前。
 *
 * 此类是 Java Collections Framework 的成员。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public class CopyOnWriteArrayList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 8673264195747942595L;

    /** 保护所有数据更改操作的锁 */
    final transient ReentrantLock lock = new ReentrantLock();

    /** 数组。只能通过 getArray/setArray 访问此数组（不是数组中的元素）。*/
    private transient volatile Object[] array;

    /**
     * 获取数组。包访问权限。可以通过 CopyOnWriteArraySet 访问。
     */
    final Object[] getArray() {
        return array;
    }

    /**
     * 设置数组。
     */
    final void setArray(Object[] a) {
        array = a;
    }

    /**
     * 创建空列表。
     */
    public CopyOnWriteArrayList() {
        setArray(new Object[0]);
    }

    /**
     * 创建包括指定集合中所有元素的列表，排列顺序为迭代器返回的顺序。
     *
     * @param c the collection of initially held elements
     * @throws NullPointerException if the specified collection is null
     */
    public CopyOnWriteArrayList(Collection<? extends E> c) {
        Object[] elements;
        if (c.getClass() == CopyOnWriteArrayList.class)
            elements = ((CopyOnWriteArrayList<?>)c).getArray();
        else {
            elements = c.toArray();
            // c.toArray might (incorrectly) not return Object[] (see 6260652)
            if (elements.getClass() != Object[].class)
                elements = Arrays.copyOf(elements, elements.length, Object[].class);
        }
        setArray(elements);
    }

    /**
     * 创建一个包含指定数组所有元素的列表。
     *
     * @param toCopyIn the array (a copy of this array is used as the
     *        internal array)
     * @throws NullPointerException if the specified array is null
     */
    public CopyOnWriteArrayList(E[] toCopyIn) {
        setArray(Arrays.copyOf(toCopyIn, toCopyIn.length, Object[].class));
    }

    /**
     * 返回列表中元素个数。
     *
     * @return the number of elements in this list
     */
    public int size() {
        return getArray().length;
    }

    /**
     * 如果列表不包含任何元素返回 true。
     *
     * @return {@code true} if this list contains no elements
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * 测试是否相等，调用 equals 判断是否相等。
     */
    private static boolean eq(Object o1, Object o2) {
        return (o1 == null) ? o2 == null : o1.equals(o2);
    }

    /**
     * indexOf 的静态版本，允许重复调用而无需每次重新获取数组。查找范围为
     * index（包含）到 fence（不包含）。
     * 注意：没有加锁
     * @param o element to search for
     * @param elements the array
     * @param index first index to search
     * @param fence one past last index to search
     * @return index of element, or -1 if absent
     */
    private static int indexOf(Object o, Object[] elements,
                               int index, int fence) {
        if (o == null) {
            for (int i = index; i < fence; i++)
                if (elements[i] == null)
                    return i;
        } else {
            for (int i = index; i < fence; i++)
                if (o.equals(elements[i]))
                    return i;
        }
        return -1;
    }

    /**
     * lastIndexOf 的静态版本。从索引为 index 位置开始查找。
     * 没有加锁。
     * @param o element to search for
     * @param elements the array
     * @param index first index to search
     * @return index of element, or -1 if absent
     */
    private static int lastIndexOf(Object o, Object[] elements, int index) {
        if (o == null) {
            for (int i = index; i >= 0; i--)
                if (elements[i] == null)
                    return i;
        } else {
            for (int i = index; i >= 0; i--)
                if (o.equals(elements[i]))
                    return i;
        }
        return -1;
    }

    /**
     * 如果列表包含指定元素返回 true。
     *
     * @param o element whose presence in this list is to be tested
     * @return {@code true} if this list contains the specified element
     */
    public boolean contains(Object o) {
        Object[] elements = getArray();
        return indexOf(o, elements, 0, elements.length) >= 0;
    }

    /**
     * {@inheritDoc}
     */
    public int indexOf(Object o) {
        Object[] elements = getArray();
        return indexOf(o, elements, 0, elements.length);
    }

    /**
     * 返回列表中第一次出现指定元素的索引。从 index 位置开始往后找，如果
     * 没找到返回 -1。
     *
     * @param e element to search for
     * @param index index to start searching from
     * @return the index of the first occurrence of the element in
     *         this list at position {@code index} or later in the list;
     *         {@code -1} if the element is not found.
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    public int indexOf(E e, int index) {
        Object[] elements = getArray();
        return indexOf(e, elements, index, elements.length);
    }

    /**
     * {@inheritDoc}
     */
    public int lastIndexOf(Object o) {
        Object[] elements = getArray();
        return lastIndexOf(o, elements, elements.length - 1);
    }

    /**
     * 返回列表中指定元素出现的最后一个位置索引，从 index 往前找，没找到
     * 返回 -1。
     *
     * @param e element to search for
     * @param index index to start searching backwards from
     * @return the index of the last occurrence of the element at position
     *         less than or equal to {@code index} in this list;
     *         -1 if the element is not found.
     * @throws IndexOutOfBoundsException if the specified index is greater
     *         than or equal to the current size of this list
     */
    public int lastIndexOf(E e, int index) {
        Object[] elements = getArray();
        return lastIndexOf(e, elements, index);
    }

    /**
     * 返回列表的浅拷贝。（元素本身不拷贝）
     *
     * @return a clone of this list
     */
    public Object clone() {
        try {
            @SuppressWarnings("unchecked")
            CopyOnWriteArrayList<E> clone =
                    (CopyOnWriteArrayList<E>) super.clone();
            clone.resetLock();
            return clone;
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    /**
     * 返回包含列表所有元素拷贝的数组，以正确的顺序（从头到尾）。
     *
     * 返回的数组完全安全，列表不保留任何对它的引用（返回的完全是一个新的数组）。
     * 调用者可以任意修改。
     *
     * 此方法是基于数组的 API 和基于集合的 API 的桥梁。
     *
     * @return an array containing all the elements in this list
     */
    public Object[] toArray() {
        Object[] elements = getArray();
        return Arrays.copyOf(elements, elements.length);
    }

    /**
     * 如果指定数组能容纳列表所有元素，则放入指定数组，不能容纳时，分配
     * 新的数组空间。
     *
     * 多余的空间设置为 null。
     *
     * @param a the array into which the elements of the list are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing all the elements in this list
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this list
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T a[]) {
        Object[] elements = getArray();
        int len = elements.length;
        // 判断指定数组能否容纳所有的元素
        if (a.length < len)
            return (T[]) Arrays.copyOf(elements, len, a.getClass());
        else {
            System.arraycopy(elements, 0, a, 0, len);
            if (a.length > len)
                a[len] = null;
            return a;
        }
    }

    // 位置访问操作

    @SuppressWarnings("unchecked")
    private E get(Object[] a, int index) {
        return (E) a[index];
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E get(int index) {
        return get(getArray(), index);
    }

    /**
     * 将列表中指定位置元素替换成指定的值。
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E set(int index, E element) {
        final ReentrantLock lock = this.lock;
        // 替换之前上锁
        lock.lock();
        try {
            Object[] elements = getArray(); //得到底层数组array
            E oldValue = get(elements, index);  //得到替换前的旧值

            if (oldValue != element) {           //如果旧值不等于替换的值
                int len = elements.length;     //得到数组长度
                // Arrays.copyOf 创建了一个新数组，并指定新数组长度,写时复制的思想
                Object[] newElements = Arrays.copyOf(elements, len);
                newElements[index] = element; //修改新数组中对应位置的值
                // 将修改后的新数组设定为存储元素的数组
                setArray(newElements);
            } else {
                // Not quite a no-op; ensures volatile write semantics
                setArray(elements);    //如果相等，那么原数组不变化，
            }
            return oldValue;  //返回旧值
        } finally {  //解锁
            lock.unlock();
        }
    }

    /**
     * 将指定元素添加到列表末尾。
     *
     * @param e element to be appended to this list
     * @return {@code true} (as specified by {@link Collection#add})
     */
    public boolean add(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();//上锁
        try {
            Object[] elements = getArray();
            int len = elements.length;
            // 同样创建新数组，新数组长度为原数组+1，并将元素添加到新数组尾部，然后将新数组设置成
            // 支撑数组
            Object[] newElements = Arrays.copyOf(elements, len + 1);
            newElements[len] = e;  //将元素放入尾部
            setArray(newElements);  //新数组设置为底层的数组
            return true;  //插入成功返回true
        } finally {
            lock.unlock();  //解锁
        }
    }

    /**
     * 在列表中指定位置插入指定元素（插入后元素在 index 位置）。将 index
     * 位置及之后的元素往右移一位（索引加 1）。
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public void add(int index, E element) {
        final ReentrantLock lock = this.lock;
        lock.lock();//上锁
        try {
            Object[] elements = getArray();  //得到原数组
            int len = elements.length;
            if (index > len || index < 0)    //检查索引合法性
                throw new IndexOutOfBoundsException("Index: "+index+
                        ", Size: "+len);
            Object[] newElements;
            // 需要移动的元素个数
            int numMoved = len - index;
            // 如果不用移动任何元素，则和add(E)一样，创造一个比当前数组容量大 1 的数组。
            if (numMoved == 0)
                newElements = Arrays.copyOf(elements, len + 1);
            else {   //需要移动，
                // 初始化新数组的固定长度为之前长度+1
                newElements = new Object[len + 1];
                // 将 elements 里的元素从0开始放入到 newElements 从0开始的位置，复制个数为index，即将index前面元素先移动
                System.arraycopy(elements, 0, newElements, 0, index);
                //然后将elements中index之后的元素，移动到新数组的index+1开始的位置，移动个数为计算出来的numMoved。
                System.arraycopy(elements, index, newElements, index + 1,
                        numMoved);
            }
            //移动完毕后，在index处放入新值
            newElements[index] = element;
            setArray(newElements);  //设置新数组
        } finally {
            lock.unlock();  //解锁
        }
    }

    /**
     * 删除列表中指定位置的元素。
     * 将之后的元素往前移动一格。并返回删除的元素。
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E remove(int index) {
        final ReentrantLock lock = this.lock;
        lock.lock();  //上锁
        try {
            Object[] elements = getArray();
            int len = elements.length;
            E oldValue = get(elements, index);  //先获得对应位置旧值，如果index不合法，这一步会抛出异常
            int numMoved = len - index - 1; //需要移动的元素个数。
            // 删除的元素是最后一个元素，直接赋值少一个个数即可。
            if (numMoved == 0)
                setArray(Arrays.copyOf(elements, len - 1));
            else {
                // 否则将其后的元素向前移动一位
                Object[] newElements = new Object[len - 1]; //同样先固定长度为原始长度-1
                //这一步先赋值前index个值
                System.arraycopy(elements, 0, newElements, 0, index);
                //再赋值index+1后的这些元素值，放到新数组index处。
                System.arraycopy(elements, index + 1, newElements, index,
                        numMoved);
                setArray(newElements);   //赋值新数组给底层数组
            }
            return oldValue;//返回删除的旧值
        } finally {
            lock.unlock();  //解锁
        }
    }

    /**
     * 从列表中删除第一次出现的指定元素，如果其存在的话。如果不包含该元素，
     * 不做出任何改变。
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element
     */
    public boolean remove(Object o) {   //没有上锁，在调用的remove版本中上锁
        Object[] snapshot = getArray();
        // 要删除元素的索引
        int index = indexOf(o, snapshot, 0, snapshot.length); //如果存在，返回值不是-1.
        return (index < 0) ? false : remove(o, snapshot, index);   //在旧数组中没找到，就直接返回，否则调用加锁的remove。
    }

    /**
     * A version of remove(Object) using the strong hint that given
     * recent snapshot contains o at the given index.
     */
    private boolean remove(Object o, Object[] snapshot, int index) {
        final ReentrantLock lock = this.lock;
        lock.lock(); //上锁。
        try {
            Object[] current = getArray(); //得到当前数组引用。
            int len = current.length;
            // 如果快照已经不是当前的数组了
            if (snapshot != current) findIndex: {
                int prefix = Math.min(index, len);//更新遍历的结束点。
                // 遍历找到该元素现在的位置
                for (int i = 0; i < prefix; i++) {
                    // 如果 current[i] == snapshot[i]，数组当前位置没有被改变
                    // 如果不相等，而且 o 等于 curr[i]，说明snapshot与current之间修改了数组,已经找到现在所在位置
                    if (current[i] != snapshot[i] && eq(o, current[i])) {
                        index = i;  //index 赋值
                        break findIndex; //打断这个if语句块
                    }
                }
                // 运行到这一步的 index 大于等于 len，说明数组缩短了
                // 如果在 len 范围内没找到，说明元素在列表中已经不存在了
                if (index >= len)
                    return false;
                // index < len
                // 当前 index 的元素就是要找的元素则跳出if，否则从 index 开始，
                // 查找 index - len 范围内是否存在
                if (current[index] == o)
                    break findIndex;
                index = indexOf(o, current, index, len);
                if (index < 0)  //如果没有找到，返回false。
                    return false;
            }
            // 到这里，说明找到了索引，执行复制操作，和remove（index）方法一样原理。
            Object[] newElements = new Object[len - 1];
            System.arraycopy(current, 0, newElements, 0, index);
            System.arraycopy(current, index + 1,
                    newElements, index,
                    len - index - 1);
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();  //解锁
        }
    }

    /**
     * 删除索引范围为 fromIndex（包含）到 toIndex（不包含）的元素。
     * 将右边的元素向左移动。如果 fromIndex 等于 toIndex，列表不会做出任何改变。
     *
     * @param fromIndex index of first element to be removed
     * @param toIndex index after last element to be removed
     * @throws IndexOutOfBoundsException if fromIndex or toIndex out of range
     *         ({@code fromIndex < 0 || toIndex > size() || toIndex < fromIndex})
     */
    void removeRange(int fromIndex, int toIndex) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;

            // 判断边界是否符合要求
            if (fromIndex < 0 || toIndex > len || toIndex < fromIndex)
                throw new IndexOutOfBoundsException();
            // 新的长度
            int newlen = len - (toIndex - fromIndex);
            int numMoved = len - toIndex;
            if (numMoved == 0)
                setArray(Arrays.copyOf(elements, newlen));
            else {
                Object[] newElements = new Object[newlen];
                System.arraycopy(elements, 0, newElements, 0, fromIndex);
                System.arraycopy(elements, toIndex, newElements,
                        fromIndex, numMoved);
                setArray(newElements);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 添加元素，如果它不存在的话。
     *
     * @param e element to be added to this list, if absent
     * @return {@code true} if the element was added
     */
    public boolean addIfAbsent(E e) {
        // 快照是留下来的旧的数组，最新的数组和快照没有关系
        Object[] snapshot = getArray();
        // 调用 index 判断是否存在该元素，只有在快照数组中没找到，才会去方法中找。
        return indexOf(e, snapshot, 0, snapshot.length) >= 0 ? false :
                addIfAbsent(e, snapshot);
    }

    /**
     * A version of addIfAbsent using the strong hint that given
     * recent snapshot does not contain e.
     * 和 remove(Object o, Object[] snapshot, int index) 类似的操作。
     */
    private boolean addIfAbsent(E e, Object[] snapshot) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] current = getArray();
            int len = current.length;
            if (snapshot != current) {
                // Optimize for lost race to another addXXX operation
                // 首先在快照的长度和最新数组的长度值较小的范围内查找
                int common = Math.min(snapshot.length, len);
                for (int i = 0; i < common; i++)
                    // 如果已经存在该元素，返回 false.
                    // 如果 current[i] == snapshot[i]，说明该位置没有被其他线程修改过，
                    // 同时该位置是不可能存在的，因为之前已经检查过了
                    if (current[i] != snapshot[i] && eq(e, current[i]))
                        return false;
                // 如果在没查找的范围内找到了，也返回 false
                if (indexOf(e, current, common, len) >= 0)
                    return false;
            }
            // 复制
            Object[] newElements = Arrays.copyOf(current, len + 1);
            newElements[len] = e;
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 如果列表中包含指定集合所有元素则返回 true。
     * 没有加锁，也不存在快照和实际数组的区分。
     * CopyOnWriteArrayList 的读操作不需要加锁（读取快照）。
     * @param c collection to be checked for containment in this list
     * @return {@code true} if this list contains all of the elements of the
     *         specified collection
     * @throws NullPointerException if the specified collection is null
     * @see #contains(Object)
     */
    public boolean containsAll(Collection<?> c) {
        Object[] elements = getArray();
        int len = elements.length;
        for (Object e : c) {
            if (indexOf(e, elements, 0, len) < 0)
                return false;
        }
        return true;
    }

    /**
     * 从列表中删除指定集合中包含的元素。此操作代价昂贵，因为需要内部临时
     * 存储的数组。
     *
     * @param c collection containing elements to be removed from this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the class of an element of this list
     *         is incompatible with the specified collection
     *         (<a href="../Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this list contains a null element and the
     *         specified collection does not permit null elements
     *         (<a href="../Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see #remove(Object)
     */
    public boolean removeAll(Collection<?> c) {
        if (c == null) throw new NullPointerException();
        // 此类中所有的写操作都加锁，此时其他的写操作就不能再写入了
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                // temp array holds those elements we know we want to keep
                int newlen = 0;
                // 创建临时数组 temp，用于保存不需要删除的元素
                Object[] temp = new Object[len];
                for (int i = 0; i < len; ++i) {
                    Object element = elements[i];
                    if (!c.contains(element))
                        temp[newlen++] = element;
                }
                // 复制
                if (newlen != len) {
                    setArray(Arrays.copyOf(temp, newlen));
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 保留此列表中同时存在于指定集合中的元素。换句话说，删除列表在指定
     * 集合中不存在的元素。（求交集）
     *
     * @param c collection containing elements to be retained in this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the class of an element of this list
     *         is incompatible with the specified collection
     *         (<a href="../Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this list contains a null element and the
     *         specified collection does not permit null elements
     *         (<a href="../Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see #remove(Object)
     */
    public boolean retainAll(Collection<?> c) {
        if (c == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                // temp array holds those elements we know we want to keep
                int newlen = 0;
                Object[] temp = new Object[len];
                for (int i = 0; i < len; ++i) {
                    Object element = elements[i];
                    if (c.contains(element))
                        temp[newlen++] = element;
                }
                if (newlen != len) {
                    setArray(Arrays.copyOf(temp, newlen));
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将指定集合中所有不存在于列表中的元素加入到列表中（求并集），按
     * 指定集合迭代器返回的顺序加入。
     *
     * 如果加入列表的元素有重复，只加入一次。
     *
     * @param c collection containing elements to be added to this list
     * @return the number of elements added
     * @throws NullPointerException if the specified collection is null
     * @see #addIfAbsent(Object)
     */
    public int addAllAbsent(Collection<? extends E> c) {
        Object[] cs = c.toArray(); //cs既用来存放集合的元素，也用来存放需要转移到数组中去的元素。
        if (cs.length == 0)
            return 0;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            int added = 0;
            // uniquify and compact elements in cs
            // 从集合中找出不存在于列表中的元素（还需要判断它和已处理过的
            // 元素不相等，为了删除重复的元素）
            // 将找出的元素保存在cs 数组前面
            for (int i = 0; i < cs.length; ++i) {
                Object e = cs[i];
                if (indexOf(e, elements, 0, len) < 0 && //只有还未存在的旧表中，
                        indexOf(e, cs, 0, added) < 0)   //且也没存在在已经转移的数据中。
                    cs[added++] = e;
            }
            // 复制
            if (added > 0) { //如果添加了值。
                //先创建新数组，把原来的数组全部复制到新数组中去，并且新数组长度为原来数组加上添加到集合的数组。
                Object[] newElements = Arrays.copyOf(elements, len + added);
                System.arraycopy(cs, 0, newElements, len, added);  //将cs中可以添加到新数组中的，移到len后面。
                setArray(newElements);
            }
            return added;   //返回添加的有效个数
        } finally {
            lock.unlock();  //解锁
        }
    }

    /**
     * 从列表中删除所有元素。
     * 此方法调用后列表为空。
     */
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();   //上锁。
        try {
            setArray(new Object[0]); //初始化底层数组。
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将指定集合中的所有元素加入到列表末尾，添加顺序为指定集合迭代器
     * 返回的顺序。
     *
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     * @see #add(Object)
     */
    public boolean addAll(Collection<? extends E> c) {
        Object[] cs = (c.getClass() == CopyOnWriteArrayList.class) ?
                ((CopyOnWriteArrayList<?>)c).getArray() : c.toArray();
        if (cs.length == 0)
            return false;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len == 0 && cs.getClass() == Object[].class)
                setArray(cs);
            else {
                // 直接创建新的数组，而不是加在原来的数组后面
                Object[] newElements = Arrays.copyOf(elements, len + cs.length);
                //此时cs为底层数组。
                System.arraycopy(cs, 0, newElements, len, cs.length);
                setArray(newElements);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 在此列表中指定位置插入指定集合中所有元素。将当前位置和其之后的元素
     * 向右移动（索引增加）。插入的顺序和集合迭代器返回的顺序一致。
     *
     * @param index index at which to insert the first element
     *        from the specified collection
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException if the specified collection is null
     * @see #add(int,Object)
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        Object[] cs = c.toArray();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            // 判断边界
            if (index > len || index < 0)
                throw new IndexOutOfBoundsException("Index: "+index+
                        ", Size: "+len);
            if (cs.length == 0)
                return false;
            // 需要移动的元素个数
            int numMoved = len - index;
            Object[] newElements;
            if (numMoved == 0)
                // 创建可以容纳所有元素的数组，并将列表元素复制到该数组里
                newElements = Arrays.copyOf(elements, len + cs.length);
            else {
                // 创建可以容纳所有元素的数组，将分别将 index 之前，index 及
                // 之后的元素复制到对应的位子（中间空出 cs 的位置）
                newElements = new Object[len + cs.length];
                //分两段复制
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index,
                        newElements, index + cs.length,
                        numMoved);
            }
            // 将指定集合元素复制到 index 及之后
            System.arraycopy(cs, 0, newElements, index, cs.length);
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void forEach(Consumer<? super E> action) {
        if (action == null) throw new NullPointerException();
        Object[] elements = getArray();
        int len = elements.length;
        for (int i = 0; i < len; ++i) {
            @SuppressWarnings("unchecked") E e = (E) elements[i];
            action.accept(e);
        }
    }

    public boolean removeIf(Predicate<? super E> filter) {
        if (filter == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                int newlen = 0;
                Object[] temp = new Object[len];
                for (int i = 0; i < len; ++i) {
                    @SuppressWarnings("unchecked") E e = (E) elements[i];
                    if (!filter.test(e))
                        temp[newlen++] = e;
                }
                if (newlen != len) {
                    setArray(Arrays.copyOf(temp, newlen));
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void replaceAll(UnaryOperator<E> operator) {
        if (operator == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            Object[] newElements = Arrays.copyOf(elements, len);
            for (int i = 0; i < len; ++i) {
                @SuppressWarnings("unchecked") E e = (E) elements[i];
                newElements[i] = operator.apply(e);
            }
            setArray(newElements);
        } finally {
            lock.unlock();
        }
    }

    // 排序（调用 Arrays.sort 排序）
    public void sort(Comparator<? super E> c) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            // 生成新数组
            Object[] newElements = Arrays.copyOf(elements, elements.length);
            @SuppressWarnings("unchecked") E[] es = (E[])newElements;
            Arrays.sort(es, c);
            setArray(newElements);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 序列化
     *
     * @param s the stream
     * @throws java.io.IOException if an I/O error occurs
     * @serialData The length of the array backing the list is emitted
     *               (int), followed by all of its elements (each an Object)
     *               in the proper order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {

        s.defaultWriteObject();

        Object[] elements = getArray();
        // Write out array length
        s.writeInt(elements.length);

        // Write out all elements in the proper order.
        for (Object element : elements)
            s.writeObject(element);
    }

    /**
     * 反序列化
     * @param s the stream
     * @throws ClassNotFoundException if the class of a serialized object
     *         could not be found
     * @throws java.io.IOException if an I/O error occurs
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {

        s.defaultReadObject();

        // bind to new lock
        resetLock();

        // Read in array length and allocate array
        int len = s.readInt();
        SharedSecrets.getJavaOISAccess().checkArray(s, Object[].class, len);
        Object[] elements = new Object[len];

        // Read in all elements in the proper order.
        for (int i = 0; i < len; i++)
            elements[i] = s.readObject();
        setArray(elements);
    }

    /**
     * 返回字符串
     *
     * @return a string representation of this list
     */
    public String toString() {
        return Arrays.toString(getArray());
    }

    /**
     * 比较指定的对象和列表是否相等。如果指定集合也是 List 类型，指定集合
     * 迭代器返回的元素顺序和列表迭代器返回的元素顺序相同，且每个元素对应
     * 相等，则认为这两个对象相等。
     *
     * @param o the object to be compared for equality with this list
     * @return {@code true} if the specified object is equal to this list
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof List))
            return false;

        List<?> list = (List<?>)(o);
        Iterator<?> it = list.iterator();
        Object[] elements = getArray();
        int len = elements.length;
        for (int i = 0; i < len; ++i)
            if (!it.hasNext() || !eq(elements[i], it.next()))
                return false;
        if (it.hasNext())
            return false;
        return true;
    }

    /**
     * 返回列表的 hash 值
     *
     * 此实现使用了 List.hashCode 的定义。
     *
     * @return the hash code value for this list
     */
    public int hashCode() {
        int hashCode = 1;
        Object[] elements = getArray();
        int len = elements.length;
        for (int i = 0; i < len; ++i) {
            Object obj = elements[i];
            hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
        }
        return hashCode;
    }

    /**
     * 返回列表中元素正常顺序的迭代器。
     *
     * 遍历迭代器的时候不需要同步，因为迭代器是快照。迭代器不支持 remove 方法。
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    public Iterator<E> iterator() {
        return new COWIterator<E>(getArray(), 0);
    }

    /**
     * {@inheritDoc}
     *
     * 遍历迭代器的时候不需要同步，因为迭代器是快照。迭代器不支持 remove 方法。
     */
    public ListIterator<E> listIterator() {
        return new COWIterator<E>(getArray(), 0);
    }

    /**
     * {@inheritDoc}
     *
     * 遍历迭代器的时候不需要同步，因为迭代器是快照。迭代器不支持 remove 方法。
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public ListIterator<E> listIterator(int index) {
        Object[] elements = getArray();
        int len = elements.length;
        if (index < 0 || index > len)
            throw new IndexOutOfBoundsException("Index: "+index);

        return new COWIterator<E>(elements, index);
    }

    /**
     * Returns a {@link Spliterator} over the elements in this list.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#IMMUTABLE},
     * {@link Spliterator#ORDERED}, {@link Spliterator#SIZED}, and
     * {@link Spliterator#SUBSIZED}.
     *
     * <p>The spliterator provides a snapshot of the state of the list
     * when the spliterator was constructed. No synchronization is needed while
     * operating on the spliterator.
     *
     * @return a {@code Spliterator} over the elements in this list
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator
                (getArray(), Spliterator.IMMUTABLE | Spliterator.ORDERED);
    }

    static final class COWIterator<E> implements ListIterator<E> {
        /** 数组快照*/
        private final Object[] snapshot;
        /** 下次调用 next 时的元素索引  */
        private int cursor;

        private COWIterator(Object[] elements, int initialCursor) {
            cursor = initialCursor;
            snapshot = elements;
        }

        public boolean hasNext() {
            return cursor < snapshot.length;
        }

        public boolean hasPrevious() {
            return cursor > 0;
        }

        @SuppressWarnings("unchecked")
        public E next() {
            if (! hasNext())
                throw new NoSuchElementException();
            return (E) snapshot[cursor++];
        }

        @SuppressWarnings("unchecked")
        public E previous() {
            if (! hasPrevious())
                throw new NoSuchElementException();
            return (E) snapshot[--cursor];
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor-1;
        }

        /**
         * 不支持。调用时总是抛出 UnsupportedOperationException 异常。
         * @throws UnsupportedOperationException always; {@code remove}
         *         is not supported by this iterator.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * 不支持。总是抛出 UnsupportedOperationException 异常。
         * @throws UnsupportedOperationException always; {@code set}
         *         is not supported by this iterator.
         */
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        /**
         * 不支持。总是抛出 UnsupportedOperationException 异常。
         * @throws UnsupportedOperationException always; {@code add}
         *         is not supported by this iterator.
         */
        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            Object[] elements = snapshot;
            final int size = elements.length;
            for (int i = cursor; i < size; i++) {
                @SuppressWarnings("unchecked") E e = (E) elements[i];
                action.accept(e);
            }
            cursor = size;
        }
    }

    /**
     * 返回列表索引从 fromIndex（包括）到 toIndex（不包括）部分的视图。
     * 返回的列表由此列表支撑，所以返回列表的任何改变都会影响此列表。
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex high endpoint (exclusive) of the subList
     * @return a view of the specified range within this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public List<E> subList(int fromIndex, int toIndex) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            // 检查边界
            if (fromIndex < 0 || toIndex > len || fromIndex > toIndex)
                throw new IndexOutOfBoundsException();
            return new COWSubList<E>(this, fromIndex, toIndex);
        } finally {
            lock.unlock();
        }
    }

    /**
     * CopyOnWriteArrayList 的子列表。
     *
     * 函数实现调用父列表中相应的方法，根据 offset 偏移量计算当前索引。
     */
    private static class COWSubList<E>
            extends AbstractList<E>
            implements RandomAccess
    {
        private final CopyOnWriteArrayList<E> l;
        private final int offset;
        private int size;
        private Object[] expectedArray;

        // only call this holding l's lock
        COWSubList(CopyOnWriteArrayList<E> list,
                   int fromIndex, int toIndex) {
            // l 表示父数组
            l = list;
            expectedArray = l.getArray();
            // offset 用于计算偏移位置，表示从哪一个位置开始
            offset = fromIndex;
            size = toIndex - fromIndex;
        }

        // only call this holding l's lock
        private void checkForComodification() {
            if (l.getArray() != expectedArray)
                throw new ConcurrentModificationException();
        }

        // 检查 index 是否在数组范围内
        private void rangeCheck(int index) {
            if (index < 0 || index >= size)
                throw new IndexOutOfBoundsException("Index: "+index+
                        ",Size: "+size);
        }

        // 设置指定索引处的值
        public E set(int index, E element) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                rangeCheck(index);
                checkForComodification();
                // 设置指定位置（子数组的指定位置）处的值
                E x = l.set(index+offset, element);
                expectedArray = l.getArray();//更新底层数组的快照
                return x;
            } finally {
                lock.unlock();
            }
        }

        // 获取指定索引处的值
        public E get(int index) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                rangeCheck(index);
                checkForComodification();
                return l.get(index+offset);
            } finally {
                lock.unlock();
            }
        }

        // 子数组大小
        public int size() {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                return size;
            } finally {
                lock.unlock();
            }
        }

        // 指定索引处添加元素
        public void add(int index, E element) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                if (index < 0 || index > size)
                    throw new IndexOutOfBoundsException();
                l.add(index+offset, element);
                expectedArray = l.getArray();
                size++;
            } finally {
                lock.unlock();
            }
        }

        // 清空
        public void clear() {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                l.removeRange(offset, offset+size);
                expectedArray = l.getArray();
                size = 0;
            } finally {
                lock.unlock();
            }
        }

        // 删除指定索引处的值
        public E remove(int index) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                rangeCheck(index);
                checkForComodification();
                E result = l.remove(index+offset);
                expectedArray = l.getArray();
                size--;
                return result;
            } finally {
                lock.unlock();
            }
        }

        // 删除指定元素
        public boolean remove(Object o) {
            int index = indexOf(o);
            if (index == -1)
                return false;
            remove(index);
            return true;
        }

        public Iterator<E> iterator() {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                return new COWSubListIterator<E>(l, 0, offset, size);
            } finally {
                lock.unlock();
            }
        }

        public ListIterator<E> listIterator(int index) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                if (index < 0 || index > size)
                    throw new IndexOutOfBoundsException("Index: "+index+
                            ", Size: "+size);
                return new COWSubListIterator<E>(l, index, offset, size);
            } finally {
                lock.unlock();
            }
        }

        public List<E> subList(int fromIndex, int toIndex) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                if (fromIndex < 0 || toIndex > size || fromIndex > toIndex)
                    throw new IndexOutOfBoundsException();
                return new COWSubList<E>(l, fromIndex + offset,
                        toIndex + offset);
            } finally {
                lock.unlock();
            }
        }

        public void forEach(Consumer<? super E> action) {
            if (action == null) throw new NullPointerException();
            int lo = offset;
            int hi = offset + size;
            Object[] a = expectedArray;
            if (l.getArray() != a)
                throw new ConcurrentModificationException();
            if (lo < 0 || hi > a.length)
                throw new IndexOutOfBoundsException();
            for (int i = lo; i < hi; ++i) {
                @SuppressWarnings("unchecked") E e = (E) a[i];
                action.accept(e);
            }
        }

        public void replaceAll(UnaryOperator<E> operator) {
            if (operator == null) throw new NullPointerException();
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int lo = offset;
                int hi = offset + size;
                Object[] elements = expectedArray;
                if (l.getArray() != elements)
                    throw new ConcurrentModificationException();
                int len = elements.length;
                if (lo < 0 || hi > len)
                    throw new IndexOutOfBoundsException();
                Object[] newElements = Arrays.copyOf(elements, len);
                for (int i = lo; i < hi; ++i) {
                    @SuppressWarnings("unchecked") E e = (E) elements[i];
                    newElements[i] = operator.apply(e);
                }
                l.setArray(expectedArray = newElements);
            } finally {
                lock.unlock();
            }
        }

        public void sort(Comparator<? super E> c) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int lo = offset;
                int hi = offset + size;
                Object[] elements = expectedArray;
                if (l.getArray() != elements)
                    throw new ConcurrentModificationException();
                int len = elements.length;
                if (lo < 0 || hi > len)
                    throw new IndexOutOfBoundsException();
                Object[] newElements = Arrays.copyOf(elements, len);
                @SuppressWarnings("unchecked") E[] es = (E[])newElements;
                Arrays.sort(es, lo, hi, c);
                l.setArray(expectedArray = newElements);
            } finally {
                lock.unlock();
            }
        }

        public boolean removeAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean removed = false;
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int n = size;
                if (n > 0) {
                    int lo = offset;
                    int hi = offset + n;
                    Object[] elements = expectedArray;
                    if (l.getArray() != elements)
                        throw new ConcurrentModificationException();
                    int len = elements.length;
                    if (lo < 0 || hi > len)
                        throw new IndexOutOfBoundsException();
                    int newSize = 0;
                    Object[] temp = new Object[n];
                    for (int i = lo; i < hi; ++i) {
                        Object element = elements[i];
                        if (!c.contains(element))
                            temp[newSize++] = element;
                    }
                    if (newSize != n) {
                        Object[] newElements = new Object[len - n + newSize];
                        System.arraycopy(elements, 0, newElements, 0, lo);
                        System.arraycopy(temp, 0, newElements, lo, newSize);
                        System.arraycopy(elements, hi, newElements,
                                lo + newSize, len - hi);
                        size = newSize;
                        removed = true;
                        l.setArray(expectedArray = newElements);
                    }
                }
            } finally {
                lock.unlock();
            }
            return removed;
        }

        public boolean retainAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean removed = false;
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int n = size;
                if (n > 0) {
                    int lo = offset;
                    int hi = offset + n;
                    Object[] elements = expectedArray;
                    if (l.getArray() != elements)
                        throw new ConcurrentModificationException();
                    int len = elements.length;
                    if (lo < 0 || hi > len)
                        throw new IndexOutOfBoundsException();
                    int newSize = 0;
                    Object[] temp = new Object[n];
                    for (int i = lo; i < hi; ++i) {
                        Object element = elements[i];
                        if (c.contains(element))
                            temp[newSize++] = element;
                    }
                    if (newSize != n) {
                        Object[] newElements = new Object[len - n + newSize];
                        System.arraycopy(elements, 0, newElements, 0, lo);
                        System.arraycopy(temp, 0, newElements, lo, newSize);
                        System.arraycopy(elements, hi, newElements,
                                lo + newSize, len - hi);
                        size = newSize;
                        removed = true;
                        l.setArray(expectedArray = newElements);
                    }
                }
            } finally {
                lock.unlock();
            }
            return removed;
        }

        public boolean removeIf(Predicate<? super E> filter) {
            if (filter == null) throw new NullPointerException();
            boolean removed = false;
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int n = size;
                if (n > 0) {
                    int lo = offset;
                    int hi = offset + n;
                    Object[] elements = expectedArray;
                    if (l.getArray() != elements)
                        throw new ConcurrentModificationException();
                    int len = elements.length;
                    if (lo < 0 || hi > len)
                        throw new IndexOutOfBoundsException();
                    int newSize = 0;
                    Object[] temp = new Object[n];
                    for (int i = lo; i < hi; ++i) {
                        @SuppressWarnings("unchecked") E e = (E) elements[i];
                        if (!filter.test(e))
                            temp[newSize++] = e;
                    }
                    if (newSize != n) {
                        Object[] newElements = new Object[len - n + newSize];
                        System.arraycopy(elements, 0, newElements, 0, lo);
                        System.arraycopy(temp, 0, newElements, lo, newSize);
                        System.arraycopy(elements, hi, newElements,
                                lo + newSize, len - hi);
                        size = newSize;
                        removed = true;
                        l.setArray(expectedArray = newElements);
                    }
                }
            } finally {
                lock.unlock();
            }
            return removed;
        }

        public Spliterator<E> spliterator() {
            int lo = offset;
            int hi = offset + size;
            Object[] a = expectedArray;
            if (l.getArray() != a)
                throw new ConcurrentModificationException();
            if (lo < 0 || hi > a.length)
                throw new IndexOutOfBoundsException();
            return Spliterators.spliterator
                    (a, lo, hi, Spliterator.IMMUTABLE | Spliterator.ORDERED);
        }

    }

    private static class COWSubListIterator<E> implements ListIterator<E> {
        private final ListIterator<E> it;
        private final int offset;
        private final int size;

        COWSubListIterator(List<E> l, int index, int offset, int size) {
            this.offset = offset;
            this.size = size;
            it = l.listIterator(index+offset);
        }

        public boolean hasNext() {
            return nextIndex() < size;
        }

        public E next() {
            if (hasNext())
                return it.next();
            else
                throw new NoSuchElementException();
        }

        public boolean hasPrevious() {
            return previousIndex() >= 0;
        }

        public E previous() {
            if (hasPrevious())
                return it.previous();
            else
                throw new NoSuchElementException();
        }

        public int nextIndex() {
            return it.nextIndex() - offset;
        }

        public int previousIndex() {
            return it.previousIndex() - offset;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            int s = size;
            ListIterator<E> i = it;
            while (nextIndex() < s) {
                action.accept(i.next());
            }
        }
    }

    // Support for resetting lock while deserializing
    private void resetLock() {
        UNSAFE.putObjectVolatile(this, lockOffset, new ReentrantLock());
    }
    private static final sun.misc.Unsafe UNSAFE;
    private static final long lockOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = CopyOnWriteArrayList.class;
            lockOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("lock"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}