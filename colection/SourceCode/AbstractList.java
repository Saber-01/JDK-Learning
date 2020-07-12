/*
 * Copyright (c) 1997, 2012, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This class provides a skeletal implementation of the {@link List}
 * interface to minimize the effort required to implement this interface
 * backed by a "random access" data store (such as an array).  For sequential
 * access data (such as a linked list), {@link AbstractSequentialList} should
 * be used in preference to this class.
 *这个类提供了List接口的基本实现，以最小化实现由“随机访问”数据存储支持的
 *  * 接口所需工作。对于顺序访问数据（例如链表），应该优先使用
 *AbstractSequentialList。
 *
 * <p>To implement an unmodifiable list, the programmer needs only to extend
 * this class and provide implementations for the {@link #get(int)} and
 * {@link List#size() size()} methods.
 *想要实现一个不可修改列表，程序员只需要扩展这个类并实现get和size方法。
 *
 * <p>To implement a modifiable list, the programmer must additionally
 * override the {@link #set(int, Object) set(int, E)} method (which otherwise
 * throws an {@code UnsupportedOperationException}).  If the list is
 * variable-size the programmer must additionally override the
 * {@link #add(int, Object) add(int, E)} and {@link #remove(int)} methods.
 *想要实现可修改列表，程序员必须额外重写set方法（否则会抛出
 * UnsupportedOperationException异常）。如果列表大小允许变化，程序员必须
 * 重写add和remove方法。
 *
 * <p>The programmer should generally provide a void (no argument) and collection
 * constructor, as per the recommendation in the {@link Collection} interface
 * specification.
 *根据Collection接口规范中的建议，程序员通常应该提供一个void（无参数）
 * 构造函数。
 *
 * <p>Unlike the other abstract collection implementations, the programmer does
 * <i>not</i> have to provide an iterator implementation; the iterator and
 * list iterator are implemented by this class, on top of the "random access"
 * methods:
 * 与其它抽象集合的实现不同的是，程序员不必提供迭代器的实现；iterator和
 * list iterator在这一个类中已经实现，在这些“随机访问”方法之上：
 * {@link #get(int)},
 * {@link #set(int, Object) set(int, E)},
 * {@link #add(int, Object) add(int, E)} and
 * {@link #remove(int)}.
 *
 * <p>The documentation for each non-abstract method in this class describes its
 * implementation in detail.  Each of these methods may be overridden if the
 * collection being implemented admits a more efficient implementation.
 *该类中每个非抽象的方法在类文档中都详细描述了其实现。如果需要一个更高
 *性能的实现，可以重写这些方法。
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *这个类是Java Collections Framework的成员。
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @since 1.2
 */

public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     *  唯一的构造函数。（由于是protect类型，所以用于子类构造函数的调用，
     *  通常是隐式的）
     */
    protected AbstractList() {
    }

    /**
     * Appends the specified element to the end of this list (optional
     * operation).
     *将指定元素添加到列表末尾（可选操作）
     * <p>Lists that support this operation may place limitations on what
     * elements may be added to this list.  In particular, some
     * lists will refuse to add null elements, and others will impose
     * restrictions on the type of elements that may be added.  List
     * classes should clearly specify in their documentation any restrictions
     * on what elements may be added.
     *支持此操作的列表可能对添加到列表的元素设置限制。特别是，一些列表会
     * 拒绝添加 null 元素，其他的会对添加元素的类型施加限制。List 类应该在文档
     * 中清晰说明可以添加哪些元素。
     * <p>This implementation calls {@code add(size(), e)}.
     *这一实现会调用 add(size(), e) 方法。
     * <p>Note that this implementation throws an
     * {@code UnsupportedOperationException} unless
     * {@link #add(int, Object) add(int, E)} is overridden.
     *如果 add(int, Object) 和 add(int, E) 方法都没有被重写，将会抛出
     *  UnsupportedOperationException异常。
     *
     * @param e element to be appended to this list
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws UnsupportedOperationException if the {@code add} operation
     *         is not supported by this list
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this list
     * @throws NullPointerException if the specified element is null and this
     *         list does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     *         prevents it from being added to this list
     */
    public boolean add(E e) {
        add(size(), e);
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    abstract public E get(int index);    //通过索引获得列表中的值，有可能抛出索引越界的异常，为抽象方法，需要在子类中实现。

    /**
     * {@inheritDoc}
     *
     * <p>This implementation always throws an
     * {@code UnsupportedOperationException}.
     *这一实现总是抛出 UnsupportedOperationException 异常。
     * 所以如果子类不重写调用的话一直抛出异常
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation always throws an
     * {@code UnsupportedOperationException}.
     *这一实现总是抛出 UnsupportedOperationException 异常。
     *所以如果子类不重写调用的话一直抛出异常
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation always throws an
     * {@code UnsupportedOperationException}.
     *这一实现总是抛出 UnsupportedOperationException 异常。
     *  所以如果子类不重写调用的话一直抛出异常
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }


    // Search Operations
        //搜索操作
    /**
     * {@inheritDoc}
     *
     * <p>This implementation first gets a list iterator (with
     * {@code listIterator()}).  Then, it iterates over the list until the
     * specified element is found or the end of the list is reached.
     *首先获得列表的迭代器，然后根据迭代器向后遍历，直到找到指定元素或者到达列表末尾。
     * 未找到会返回-1，如果列表含有该元素，会返回迭代器顺序中第一个找到的元素的索引。
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public int indexOf(Object o) {
        ListIterator<E> it = listIterator();
        if (o==null) {
            while (it.hasNext())
                if (it.next()==null)
                    return it.previousIndex();
        } else {
            while (it.hasNext())
                if (o.equals(it.next()))
                    return it.previousIndex();
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation first gets a list iterator that points to the end
     * of the list (with {@code listIterator(size())}).  Then, it iterates
     * backwards over the list until the specified element is found, or the
     * beginning of the list is reached.
     *首先获得列表的迭代器，然后根据迭代器向前遍历，直到找到指定元素或者到达列表开头。
     * 未找到会返回-1，如果列表含有该元素，会返回迭代器向前顺序中第一个找到的元素的索引。
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public int lastIndexOf(Object o) {
        ListIterator<E> it = listIterator(size());
        if (o==null) {
            while (it.hasPrevious())
                if (it.previous()==null)
                    return it.nextIndex();
        } else {
            while (it.hasPrevious())
                if (o.equals(it.previous()))
                    return it.nextIndex();
        }
        return -1;
    }


    // Bulk Operations
// 批量操作
    /**
     * Removes all of the elements from this list (optional operation).
     * The list will be empty after this call returns.
     * 删除列表中所有元素（可选操作）。调用此方法后列表为空。
     * <p>This implementation calls {@code removeRange(0, size())}.
     *这一实现会调用 removeRange(0, size()) 方法。
     * <p>Note that this implementation throws an
     * {@code UnsupportedOperationException} unless {@code remove(int
     * index)} or {@code removeRange(int fromIndex, int toIndex)} is
     * overridden.
     *如果 remove(int index) 和 removeRange(int fromIndex, int toIndex) 都
     * 没有被重写，将会抛出 UnsupportedOperationException 异常。
     * @throws UnsupportedOperationException if the {@code clear} operation
     *         is not supported by this list
     */
    public void clear() {
        removeRange(0, size());
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation gets an iterator over the specified collection
     * and iterates over it, inserting the elements obtained from the
     * iterator into this list at the appropriate position, one at a time,
     * using {@code add(int, E)}.
     * Many implementations will override this method for efficiency.
     *这一实现获取指定集合的迭代器并进行迭代，使用 add(int, E) 方法，将从
     * 迭代器获取的元素插入到列表的合适位置，每次插入一个。
     * 为了提高效率，许多实现都会重写这一方法。
     * <p>Note that this implementation throws an
     * {@code UnsupportedOperationException} unless
     * {@link #add(int, Object) add(int, E)} is overridden.
     *如果 add 方法没有被重写，将会抛出 UnsupportedOperationException
     *  异常。
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);    // 检查索引是否越界
        boolean modified = false;    //用于判断是否改变
        for (E e : c) {            //实际上是使用指定集合的迭代器，每一次循环都调用add方法
            add(index++, e);
            modified = true;
        }
        return modified;
    }


    // Iterators
    // 迭代器
    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *按正确的顺序返回列表元素的迭代器。
     * <p>This implementation returns a straightforward implementation of the
     * iterator interface, relying on the backing list's {@code size()},
     * {@code get(int)}, and {@code remove(int)} methods.
     *这个实现返回 iterator 接口的一个简单实现，依赖于列表的 size(), get(int),
     * 和 remove(int) 方法。
     * <p>Note that the iterator returned by this method will throw an
     * {@link UnsupportedOperationException} in response to its
     * {@code remove} method unless the list's {@code remove(int)} method is
     * overridden.
     *如果列表的 remove(int) 方法没有被重写的话，迭代器会抛出
     * UnsupportedOperationException异常。
     *
     * <p>This implementation can be made to throw runtime exceptions in the
     * face of concurrent modification, as described in the specification
     * for the (protected) {@link #modCount} field.
     *并发修改中,这一实现会抛出运行时异常,抛出异常的根据是modeCount域的
     * 值
     * @return an iterator over the elements in this list in proper sequence
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns {@code listIterator(0)}.
     *这一实现返回 listIterator。
     * @see #listIterator(int)
     */
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns a straightforward implementation of the
     * {@code ListIterator} interface that extends the implementation of the
     * {@code Iterator} interface returned by the {@code iterator()} method.
     * The {@code ListIterator} implementation relies on the backing list's
     * {@code get(int)}, {@code set(int, E)}, {@code add(int, E)}
     * and {@code remove(int)} methods.
     *这一实现返回一个简单的 ListIterator 接口的实现，它扩展了 iterator()
     * 方法返回的 Iterator 接口的实现。ListIterator 的实现依赖于列表的 size(),
     * get(int), 和 remove(int) 方法。
     * <p>Note that the list iterator returned by this implementation will
     * throw an {@link UnsupportedOperationException} in response to its
     * {@code remove}, {@code set} and {@code add} methods unless the
     * list's {@code remove(int)}, {@code set(int, E)}, and
     * {@code add(int, E)} methods are overridden.
     *如果列表的 remove(int), set(int, E), 或者 add(int, E)方法没有被覆盖，
     *  这个实现返回的迭代器将会抛出 UnsupportedOperationException 异常。
     * <p>This implementation can be made to throw runtime exceptions in the
     * face of concurrent modification, as described in the specification for
     * the (protected) {@link #modCount} field.
     *并发修改中,这一实现会抛出运行时异常,抛出异常的根据是modeCount域的
     * 值。
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public ListIterator<E> listIterator(final int index) {
        rangeCheckForAdd(index);

        return new ListItr(index);
    }

    // 私有内部类，实现 Iterator 接口
    private class Itr implements Iterator<E> {
        /**
         * Index of element to be returned by subsequent call to next.
         * 要由next的后续调用返回的元素的索引。
         */
        int cursor = 0;

        /**
         * Index of element returned by most recent call to next or
         * previous.  Reset to -1 if this element is deleted by a call
         * to remove.
         * 最新调用 next 或 previous 方法的元素索引。如果该元素被删除则为-1。
         */
        int lastRet = -1;

        /**
         * The modCount value that the iterator believes that the backing
         * List should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         * 迭代器认为列表应该具有的 modCount 值，如果违背了这个期望，迭代器会
         * 检测到发生了并发修改。
         * @saber-01
         *  modCount 记录对象的修改次数。
         *         注意到 modCount 声明为 volatile，保证线程之间修改的可见性。
         *          fail-fast策略：如果在使用迭代器的过程中有其他线程修改了
         *          集合，那么将抛出ConcurrentModificationException
         */
        int expectedModCount = modCount;

        public boolean hasNext() {
            return cursor != size();
        }
        // 索引超出界限抛出 IndexOutOfBoundsException 异常
        // cursor向后移动一位，修改 lastRet 的值。
        //该方法会改变cursor和lastRet值。
        public E next() {
            checkForComodification();
            try {
                int i = cursor;
                E next = get(i);
                lastRet = i;  //本地调用next后，最后一个遍历的索引更新为cursor
                cursor = i + 1;  //下一次调用next会返回的值的索引cursor
                return next;       //返回还没有更新时候的cursor位置上的值。
            } catch (IndexOutOfBoundsException e) {
                checkForComodification();
                throw new NoSuchElementException();
            }
        }

        public void remove() {     //删除当前lastRet处的值。并改变cursor和lastRet值。
            if (lastRet < 0)
                throw new IllegalStateException();  //非法状态异常，当前状态不可以删除
            checkForComodification();   //检查并发修改

            try {
                AbstractList.this.remove(lastRet);   //调用的是本类中的remove(index)方法
                if (lastRet < cursor)
                    cursor--;       //更新cursor
                lastRet = -1;      //删除后，更新lastRet赋值为-1
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }
        //检查 modCount是否被修改，若不符合预期抛出异常。
        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }
    // 私有内部类,实现 ListIterator 接口
    //比Itr类多了向前遍历的功能，并且对列表的操作多了，add和set。
    private class ListItr extends Itr implements ListIterator<E> {
        ListItr(int index) {      //构造函数，实现了从哪里开始遍历的功能。
            cursor = index;
        }

        public boolean hasPrevious() {
            return cursor != 0;
        }

        public E previous() {  //得到cursor的前一个值。并改变cursor和lastRet值。
            checkForComodification();
            try {
                int i = cursor - 1;
                E previous = get(i);
                lastRet = cursor = i;
                return previous;
            } catch (IndexOutOfBoundsException e) {
                checkForComodification();
                throw new NoSuchElementException();
            }
        }

        public int nextIndex() {
            return cursor;
        } //得到cursor的值

        public int previousIndex() {
            return cursor-1;
        }  //得到cursor的前一个索引的值

        public void set(E e) {      //将lastRet处的值设置为e。
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                AbstractList.this.set(lastRet, e);  //调用列表的set方法设置lastRet处的值为 e。
                expectedModCount = modCount;     //修改后更新expectedModCount
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        public void add(E e) {     //同样会改变cursor和lastRet的值
            checkForComodification();
           // 调用 AbstractList 的 add 方法将指定元素添加到cursor位置。
            try {
                int i = cursor;
                AbstractList.this.add(i, e);   //注意添加是在cursor处添加。是下一个nextIndex返回的值处添加。
                lastRet = -1;                        //添加后，lastRet也会变为-1。
                cursor = i + 1;                     // cursor向后移动一位。
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns a list that subclasses
     * {@code AbstractList}.  The subclass stores, in private fields, the
     * offset of the subList within the backing list, the size of the subList
     * (which can change over its lifetime), and the expected
     * {@code modCount} value of the backing list.  There are two variants
     * of the subclass, one of which implements {@code RandomAccess}.
     * If this list implements {@code RandomAccess} the returned list will
     * be an instance of the subclass that implements {@code RandomAccess}.
     *这个实现返回作为 AbstractList 子类的列表。子类在私有字段中存储列表中
     * 子列表的偏移量，子列表的大小（可以再生命周期中更改），和预期的
     *  modCount值。子类中有两个变量，其中一个实现了 RandomAccess。如果
     *  列表实现了 RandomAccess，则返回的子列表将是实现了 RandomAccess 的
     *  子类实例。
     * <p>The subclass's {@code set(int, E)}, {@code get(int)},
     * {@code add(int, E)}, {@code remove(int)}, {@code addAll(int,
     * Collection)} and {@code removeRange(int, int)} methods all
     * delegate to the corresponding methods on the backing abstract list,
     * after bounds-checking the index and adjusting for the offset.  The
     * {@code addAll(Collection c)} method merely returns {@code addAll(size,
     * c)}.
     *子类的 set(int, E), get(int), add(int, E)}, remove(int)},
     *  addAll(int, Collection)} and removeRange(int, int) 方法在检查索引边界并
     *  调整偏移量之后，都将委托列表相应的方法。addAll(Collection c)方法仅仅
     *   返回 addAll(size, c)。
     * <p>The {@code listIterator(int)} method returns a "wrapper object"
     * over a list iterator on the backing list, which is created with the
     * corresponding method on the backing list.  The {@code iterator} method
     * merely returns {@code listIterator()}, and the {@code size} method
     * merely returns the subclass's {@code size} field.
     * listIterator(int) 方法在原列表的迭代器上返回一个“包装器对象“，该对象是
     *用原列表的相应方法创建的。iterator 方法只返回 listIterator() 方法， size()
     * 方法只返回子类的 size 字段。
     *
     * <p>All methods first check to see if the actual {@code modCount} of
     * the backing list is equal to its expected value, and throw a
     * {@code ConcurrentModificationException} if it is not.
     *所有方法首先检查原列表的实际 modCount 值是否等于期望值，如果不相等
     *  抛出 ConcurrentModificationException 异常
     *
     * @throws IndexOutOfBoundsException if an endpoint index value is out of range
     *         {@code (fromIndex < 0 || toIndex > size)}
     * @throws IllegalArgumentException if the endpoint indices are out of order
     *         {@code (fromIndex > toIndex)}
     */
    public List<E> subList(int fromIndex, int toIndex) {
        // 判断当前对象是否是 RandomAccess 的实例对象。
        return (this instanceof RandomAccess ?
                new RandomAccessSubList<>(this, fromIndex, toIndex) :
                new SubList<>(this, fromIndex, toIndex));
    }

    // Comparison and hashing
// 比较和hash操作
    /**
     * Compares the specified object with this list for equality.  Returns
     * {@code true} if and only if the specified object is also a list, both
     * lists have the same size, and all corresponding pairs of elements in
     * the two lists are <i>equal</i>.  (Two elements {@code e1} and
     * {@code e2} are <i>equal</i> if {@code (e1==null ? e2==null :
     * e1.equals(e2))}.)  In other words, two lists are defined to be
     * equal if they contain the same elements in the same order.<p>
     * 比较指定的对象和该列表是否相等。如果指定的对象也是列表，两个列表大小
     * 相等，所有对应元素相等，那么返回 true (Two elements e1 and e2 are
     *  equal if (e1==null ? e2==null : e1.equals(e2)).)。换句话说，如果两个列表
     *  包含相同元素且元素顺序相同，那么认为两个列表相等。
     *
     * This implementation first checks if the specified object is this
     * list. If so, it returns {@code true}; if not, it checks if the
     * specified object is a list. If not, it returns {@code false}; if so,
     * it iterates over both lists, comparing corresponding pairs of elements.
     * If any comparison returns {@code false}, this method returns
     * {@code false}.  If either iterator runs out of elements before the
     * other it returns {@code false} (as the lists are of unequal length);
     * otherwise it returns {@code true} when the iterations complete.
     *这个实现首先检查指定的对象是否是 this 列表，如果是返回 true；如果不是
     * 检查指定对象是否是列表对象，如果不是返回 false；然后遍历两个列表，
     *  比较对应元素是否相等，如果有任何一对不等则此函数返回 false（列表长度
     *   不等同样返回 false）；否则迭代完成后返回 true。
     *
     * @param o the object to be compared for equality with this list
     * @return {@code true} if the specified object is equal to this list
     */
    public boolean equals(Object o) {
        if (o == this)        //o.equeals(o) 肯定相等
            return true;
        if (!(o instanceof List))          //判断o是否是List实例。
            return false;

        ListIterator<E> e1 = listIterator();      //得到当前列表的迭代器
        ListIterator<?> e2 = ((List<?>) o).listIterator();     //得到指定对象的迭代器。
        while (e1.hasNext() && e2.hasNext()) {             //遍历每个迭代器元素，依次判断是否相等。
            E o1 = e1.next();
            Object o2 = e2.next();
            if (!(o1==null ? o2==null : o1.equals(o2)))            //判断相等的方法
                return false;
        }
        //如果两个迭代器同时到末尾，并且上面循环正常结束，说明相等，反之，如果任一列表先结束，说明不相等。
        return !(e1.hasNext() || e2.hasNext());
    }

    /**
     * Returns the hash code value for this list.
     * 返回列表的 hash 值。
     * <p>This implementation uses exactly the code that is used to define the
     * list hash function in the documentation for the {@link List#hashCode}
     * method.
     *此实现使用的是hashCode方法中定义 list hash function 的代码。
     * @return the hash code value for this list
     */
    public int hashCode() {
        int hashCode = 1;
        for (E e : this)
            hashCode = 31*hashCode + (e==null ? 0 : e.hashCode());
        return hashCode;
    }

    /**
     * Removes from this list all of the elements whose index is between
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
     * Shifts any succeeding elements to the left (reduces their index).
     * This call shortens the list by {@code (toIndex - fromIndex)} elements.
     * (If {@code toIndex==fromIndex}, this operation has no effect.)
     *从该列表中删除索引位于 fromIndex 和 toIndex 之间的元素。将所有后续
     * 元素向左移动（减小索引）。这个调用删除 (toIndex - fromIndex) 个元素。
     *  （如果 toIndex == fromIndex，此操作无效）。
     *
     * <p>This method is called by the {@code clear} operation on this list
     * and its subLists.  Overriding this method to take advantage of
     * the internals of the list implementation can <i>substantially</i>
     * improve the performance of the {@code clear} operation on this list
     * and its subLists.
     *这个方法由该列表和其子列表的 clear 操作调用。重写此方法，利用列表实现
     * 的内部机制，可以显著提升列表和子列表 clear 操作的性能。
     * <p>This implementation gets a list iterator positioned before
     * {@code fromIndex}, and repeatedly calls {@code ListIterator.next}
     * followed by {@code ListIterator.remove} until the entire range has
     * been removed.  <b>Note: if {@code ListIterator.remove} requires linear
     * time, this implementation requires quadratic time.</b>
     *这一实现获得位于 fromIndex 之前的列表迭代器，反复调用
     * ListIterator.next 和 ListIterator.remove，知道范围内所有元素被删除。
     * 注意：如果 ListIterator.remove 需要线性时间，那么这一实现需要二次
     * 时间。
     * @param fromIndex index of first element to be removed
     * @param toIndex index after last element to be removed
     */
    protected void removeRange(int fromIndex, int toIndex) {
        ListIterator<E> it = listIterator(fromIndex);  //获得从formIndex开始的迭代器。
        for (int i=0, n=toIndex-fromIndex; i<n; i++) {    //循环遍历此迭代器，逐个remove。
            it.next();
            it.remove();
        }
    }

    /**
     * The number of times this list has been <i>structurally modified</i>.
     * Structural modifications are those that change the size of the
     * list, or otherwise perturb it in such a fashion that iterations in
     * progress may yield incorrect results.
     * modCount 变量表示此列表在结构上被修改的次数。结构修改是指改变列表
     * 大小，或者以一种可能会产生错误结果的迭代扰乱列表内容。
     *
     * <p>This field is used by the iterator and list iterator implementation
     * returned by the {@code iterator} and {@code listIterator} methods.
     * If the value of this field changes unexpectedly, the iterator (or list
     * iterator) will throw a {@code ConcurrentModificationException} in
     * response to the {@code next}, {@code remove}, {@code previous},
     * {@code set} or {@code add} operations.  This provides
     * <i>fail-fast</i> behavior, rather than non-deterministic behavior in
     * the face of concurrent modification during iteration.
     *这个字段在 iterator 和 listIterator 中使用。如果该字段的值发生意外改变，
     *在 next, remove, previous, set, add 操作中， iterator （或者listIterator）
     * 会抛出ConcurrentModificationException异常。即提供了 fail-fast 行为，
     *而不是迭代过程中发生并发修改的非确定性行为。
     *
     * <p><b>Use of this field by subclasses is optional.</b> If a subclass
     * wishes to provide fail-fast iterators (and list iterators), then it
     * merely has to increment this field in its {@code add(int, E)} and
     * {@code remove(int)} methods (and any other methods that it overrides
     * that result in structural modifications to the list).  A single call to
     * {@code add(int, E)} or {@code remove(int)} must add no more than
     * one to this field, or the iterators (and list iterators) will throw
     * bogus {@code ConcurrentModificationExceptions}.  If an implementation
     * does not wish to provide fail-fast iterators, this field may be
     * ignored.
     *  子类可以选择使用此字段。如果子类希望提供 fail-fast iterators
     *  (and list iterators)，那么它只需要在 add(int, E) 和 remove(int)
     *  （以及任何会导致列表结构性更改的方法）中增加这个字段。对add(int, E)
     *  或者 remove(int) 的单次调用必须只增加该字段一次，否则会抛出
     *  ConcurrentModificationExceptions 异常。如果实现不希望提供 fail-fast
     *   行为，可以忽略该字段。
     */
    protected transient int modCount = 0;

    // 检查索引是否在数组范围内
    private void rangeCheckForAdd(int index) {
        if (index < 0 || index > size())
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    private String outOfBoundsMsg(int index) { //定义抛出异常的形式
        return "Index: "+index+", Size: "+size();
    }
}

//子列表类，未实现RandomAccess接口的子类。
/* 标记性接口，用来快速随机存取，实现了该接口之后，使用普通的for循环来
        * 遍历，性能更高，例如ArrayList。而没有实现该接口的话，使用Iterator来
        * 迭代，这样性能更高，例如linkedList。所以这个标记性只是为了让我们知道
        * 用什么样的方式去获取数据性能更好。*/
class SubList<E> extends AbstractList<E> {
    private final AbstractList<E> l;  //父亲列表。
    private final int offset;  //子类偏移。
    private int size;

    //从构造函数中可以知道子列表只是指向原列表某一位置，子列表并没有申请新的内存空间。
    SubList(AbstractList<E> list, int fromIndex, int toIndex) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        if (toIndex > list.size())
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        if (fromIndex > toIndex)
            throw new IllegalArgumentException("fromIndex(" + fromIndex +
                    ") > toIndex(" + toIndex + ")");
        l = list;
        offset = fromIndex;
        size = toIndex - fromIndex;
        this.modCount = l.modCount;
    }
   //子列表的增删改查操作都是调用的父类的相应方法实现的，
   // 有关index的操作只需要加入offset偏移即可。
    public E set(int index, E element) {
        rangeCheck(index);
        checkForComodification();
        return l.set(index+offset, element);
    }

    public E get(int index) {
        rangeCheck(index);
        checkForComodification();
        return l.get(index+offset);
    }

    public int size() {
        checkForComodification();
        return size;
    }

    public void add(int index, E element) {
        rangeCheckForAdd(index);
        checkForComodification();
        l.add(index+offset, element);
        this.modCount = l.modCount;
        size++;
    }

    public E remove(int index) {
        rangeCheck(index);
        checkForComodification();
        E result = l.remove(index+offset);
        this.modCount = l.modCount;
        size--;
        return result;
    }

    protected void removeRange(int fromIndex, int toIndex) {
        checkForComodification();
        l.removeRange(fromIndex+offset, toIndex+offset);
        this.modCount = l.modCount;
        size -= (toIndex-fromIndex);
    }

    public boolean addAll(Collection<? extends E> c) {
        return addAll(size, c);
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);
        int cSize = c.size();
        if (cSize==0)
            return false;

        checkForComodification();
        l.addAll(offset+index, c);
        this.modCount = l.modCount;
        size += cSize;
        return true;
    }
  //注意子列表的迭代器只有一个ListIterator迭代器。
    public Iterator<E> iterator() {
        return listIterator();
    }
    //同样迭代器方法方法也是调用的父类获得listIterator的方法。
    public ListIterator<E> listIterator(final int index) {
        checkForComodification();
        rangeCheckForAdd(index);

        return new ListIterator<E>() {
            private final ListIterator<E> i = l.listIterator(index+offset);

            public boolean hasNext() {
                return nextIndex() < size;
            }

            public E next() {
                if (hasNext())
                    return i.next();
                else
                    throw new NoSuchElementException();
            }

            public boolean hasPrevious() {
                return previousIndex() >= 0;
            }

            public E previous() {
                if (hasPrevious())
                    return i.previous();
                else
                    throw new NoSuchElementException();
            }

            public int nextIndex() {
                return i.nextIndex() - offset;
            }

            public int previousIndex() {
                return i.previousIndex() - offset;
            }

            public void remove() {
                i.remove();
                SubList.this.modCount = l.modCount;
                size--;
            }

            public void set(E e) {
                i.set(e);
            }

            public void add(E e) {
                i.add(e);
                SubList.this.modCount = l.modCount;
                size++;
            }
        };
    }

    public List<E> subList(int fromIndex, int toIndex) {
        return new SubList<>(this, fromIndex, toIndex);
    }

    private void rangeCheck(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    private void rangeCheckForAdd(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size;
    }

    private void checkForComodification() {
        if (this.modCount != l.modCount)
            throw new ConcurrentModificationException();
    }
}
// RandomAccess(since 1.4) 是一个标记接口，用于标明实现该接口的List支持快速随机
// 访问，主要目的是使算法能够在随机和顺序访问的list中表现的更加高效。
class RandomAccessSubList<E> extends SubList<E> implements RandomAccess {
    RandomAccessSubList(AbstractList<E> list, int fromIndex, int toIndex) {
        super(list, fromIndex, toIndex);
    }

    public List<E> subList(int fromIndex, int toIndex) {
        return new RandomAccessSubList<>(this, fromIndex, toIndex);
    }
}
