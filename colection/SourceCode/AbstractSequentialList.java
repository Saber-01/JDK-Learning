/*
 * Copyright (c) 1997, 2006, Oracle and/or its affiliates. All rights reserved.
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
 * This class provides a skeletal implementation of the <tt>List</tt>
 * interface to minimize the effort required to implement this interface
 * backed by a "sequential access" data store (such as a linked list).  For
 * random access data (such as an array), <tt>AbstractList</tt> should be used
 * in preference to this class.<p>
 *这个类提供了一个 List 接口的框架实现，以最小化实现由“顺序访问”
 *  数据存储（例如链表）支持的此接口所需工作。对于随机访问数据结构
 * （例如数组），应该优先使用 AbstractList。
 *
 * This class is the opposite of the <tt>AbstractList</tt> class in the sense
 * that it implements the "random access" methods (<tt>get(int index)</tt>,
 * <tt>set(int index, E element)</tt>, <tt>add(int index, E element)</tt> and
 * <tt>remove(int index)</tt>) on top of the list's list iterator, instead of
 * the other way around.<p>
 *这个类与 AbstractList 类相反，它实现了列表迭代器顶部的随机访问
 *方法 (get(int index), set(int index, E element),
 *add(int index, E element) and remove(int index))。
 *
 * To implement a list the programmer needs only to extend this class and
 * provide implementations for the <tt>listIterator</tt> and <tt>size</tt>
 * methods.  For an unmodifiable list, the programmer need only implement the
 * list iterator's <tt>hasNext</tt>, <tt>next</tt>, <tt>hasPrevious</tt>,
 * <tt>previous</tt> and <tt>index</tt> methods.<p>
 *要实现此列表接口，程序员只需要扩展这个类并为 listIterator 和 size
 * 方法提供实现。对于不可修改列表，程序员只需要实现列表 iterator 的
 * hasNext, next, hasPrevious, previous and index 方法。
 *
 * For a modifiable list the programmer should additionally implement the list
 * iterator's <tt>set</tt> method.  For a variable-size list the programmer
 * should additionally implement the list iterator's <tt>remove</tt> and
 * <tt>add</tt> methods.<p>
 *对于可修改的列表，程序员应该实现列表 iterator 的 set 方法。对于
 * 可变大小的列表，程序员应该额外实现列表 iterator 的 remove 和 add
 * 方法。
 *
 * The programmer should generally provide a void (no argument) and collection
 * constructor, as per the recommendation in the <tt>Collection</tt> interface
 * specification.<p>
 *按照 Collection 接口规范中的建议，程序员通常应该提供一个
 * void（无参数）构造函数。
 * This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 * 此类是 Java Collections Framework 的成员。
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see Collection
 * @see List
 * @see AbstractList
 * @see AbstractCollection
 * @since 1.2
 */
// Sequential 相继的，按次序的
public abstract class AbstractSequentialList<E> extends AbstractList<E> {
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     * *唯一的构造函数。（用于子类构造函数调用，通常是隐式的）
     */
    protected AbstractSequentialList() {
    }

    /**
     * Returns the element at the specified position in this list.
     * 返回列表中指定位置的元素。
     * <p>This implementation first gets a list iterator pointing to the
     * indexed element (with <tt>listIterator(index)</tt>).  Then, it gets
     * the element using <tt>ListIterator.next</tt> and returns it.
     *此实现首先用 listIterator(index) 得到从指定索引位置开始的迭代器，
     * 然后使用 ListIterator.next 方法得到该元素并返回。
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E get(int index) {
        try {
            return listIterator(index).next();       //返回从index开始的迭代器的next方法返回的元素。
        } catch (NoSuchElementException exc) {
            throw new IndexOutOfBoundsException("Index: "+index);
        }
    }

    /**
     * Replaces the element at the specified position in this list with the
     * specified element (optional operation).
     * 用指定元素替换列表中指定位置的元素（可选操作）。
     *
     * <p>This implementation first gets a list iterator pointing to the
     * indexed element (with <tt>listIterator(index)</tt>).  Then, it gets
     * the current element using <tt>ListIterator.next</tt> and replaces it
     * with <tt>ListIterator.set</tt>.
     *此实现首先用 listIterator(index) 得到从指定索引位置开始的迭代器，
     *  然后使用 ListIterator.next 方法得到该元素，使用 ListIterator.set
     * 设置该位置的新值并返回旧值。
     *
     * <p>Note that this implementation will throw an
     * <tt>UnsupportedOperationException</tt> if the list iterator does not
     * implement the <tt>set</tt> operation.
     *注意如果列表迭代器没有实现 set 方法，会抛出
     * UnsupportedOperationException 异常。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public E set(int index, E element) {
        try {
            ListIterator<E> e = listIterator(index);    //得到从指定位置index开始的迭代器
            E oldVal = e.next();     //使用得到的迭代器获得cursor的值，并设置为旧值，用于返回
            e.set(element);                //调用迭代器的set方法。set方法使用lastRet，即最后一次遍历的索引。
            return oldVal;
        } catch (NoSuchElementException exc) {
            throw new IndexOutOfBoundsException("Index: "+index);
        }
    }

    /**
     * Inserts the specified element at the specified position in this list
     * (optional operation).  Shifts the element currently at that position
     * (if any) and any subsequent elements to the right (adds one to their
     * indices).
     *在指定位置插入指定元素（可选操作）。把当前位置及其之后的元素
     * 向右移动一位（索引加一）。
     *
     * <p>This implementation first gets a list iterator pointing to the
     * indexed element (with <tt>listIterator(index)</tt>).  Then, it
     * inserts the specified element with <tt>ListIterator.add</tt>.
     *此实现首先用 listIterator(index) 得到从指定索引位置开始的迭代器，
     *然后使用 ListIterator.add 方法插入指定元素。
     *
     * <p>Note that this implementation will throw an
     * <tt>UnsupportedOperationException</tt> if the list iterator does not
     * implement the <tt>add</tt> operation.
     *注意如果列表迭代器没有实现 add 方法会抛出
     * UnsupportedOperationException 异常。
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public void add(int index, E element) {
        try {
            //得到从index开始的迭代器，调用迭代器add方法，
            //将在cursor位置插入新值。
            listIterator(index).add(element);
        } catch (NoSuchElementException exc) {
            throw new IndexOutOfBoundsException("Index: "+index);
        }
    }

    /**
     * Removes the element at the specified position in this list (optional
     * operation).  Shifts any subsequent elements to the left (subtracts one
     * from their indices).  Returns the element that was removed from the
     * list.
     * 移除列表指定位置的元素（可选操作）。把所有后续元素向左移动一位
     *  （索引减一）。返回从列表中移除的元素。
     *
     * <p>This implementation first gets a list iterator pointing to the
     * indexed element (with <tt>listIterator(index)</tt>).  Then, it removes
     * the element with <tt>ListIterator.remove</tt>.
     *此实现首先用 listIterator(index) 得到从指定索引位置开始的迭代器，
     * 然后使用 ListIterator.remove 方法删除指定元素。
     *
     * <p>Note that this implementation will throw an
     * <tt>UnsupportedOperationException</tt> if the list iterator does not
     * implement the <tt>remove</tt> operation.
     * 注意如果列表迭代器没有实现 remove 方法会抛出
     * UnsupportedOperationException 异常。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public E remove(int index) {
        try {
            ListIterator<E> e = listIterator(index);  //得到从index开始的列表迭代器
            E outCast = e.next();                //将当前旧值取出。
            e.remove();                    //注意remove操作的是lastRet上的值，即迭代器最后一次遍历的索引上的值。
            return outCast;                    //返回被删除的值
        } catch (NoSuchElementException exc) {
            throw new IndexOutOfBoundsException("Index: "+index);
        }
    }


    // Bulk Operations
  // 批量操作
    /**
     * Inserts all of the elements in the specified collection into this
     * list at the specified position (optional operation).  Shifts the
     * element currently at that position (if any) and any subsequent
     * elements to the right (increases their indices).  The new elements
     * will appear in this list in the order that they are returned by the
     * specified collection's iterator.  The behavior of this operation is
     * undefined if the specified collection is modified while the
     * operation is in progress.  (Note that this will occur if the specified
     * collection is this list, and it's nonempty.)
     *将指定集合中的所有元素插入到列表中的指定位置（可选操作）。把
     *当前位置及之后的元素向右移动（增加索引）。插入的顺序为指定
     *  集合迭代器返回的顺序。如果此操作进行的过程中指定集合被修改，
     *  那么此操作的行为未知。（注意如果执行集合是 this 列表，且不为空，
     *  就会发生这种情况。）
     *
     * <p>This implementation gets an iterator over the specified collection and
     * a list iterator over this list pointing to the indexed element (with
     * <tt>listIterator(index)</tt>).  Then, it iterates over the specified
     * collection, inserting the elements obtained from the iterator into this
     * list, one at a time, using <tt>ListIterator.add</tt> followed by
     * <tt>ListIterator.next</tt> (to skip over the added element).
     * 此实现首先得到指定集合的迭代器，以及使用 listIterator(index)
     * 得到列表从指定索引开始的列表迭代器。然后对指定集合进行迭代，
     *  使用 ListIterator.next（跳过刚插入的元素） 和 ListIterator.add
     *  将元素依次插入到列表集合中。
     *
     * <p>Note that this implementation will throw an
     * <tt>UnsupportedOperationException</tt> if the list iterator returned by
     * the <tt>listIterator</tt> method does not implement the <tt>add</tt>
     * operation.
     *如果返回的列表迭代器没有实现 add 方法，抛出
     * UnsupportedOperationException 异常。
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public boolean addAll(int index, Collection<? extends E> c) { //从指定位置插入一个集合中的所有元素
        try {
            boolean modified = false;   //用于判断添加的集合后列表是否发生改变
            ListIterator<E> e1 = listIterator(index);  //得到 从index开始的列表迭代器
            Iterator<? extends E> e2 = c.iterator();    //得到指定集合c的迭代器。
            while (e2.hasNext()) {          //遍历指定集合，将每个元素都调用列表迭代器的add方法，
                e1.add(e2.next());              //因为add从cursor开始操作，并且完成后，cursor自动+1，所以列表迭代器e1不需要next。
                modified = true;
            }
            return modified;
        } catch (NoSuchElementException exc) {
            throw new IndexOutOfBoundsException("Index: "+index);
        }
    }


    // Iterators
    // 迭代器
    /**
     * Returns an iterator over the elements in this list (in proper
     * sequence).<p>
     *返回列表迭代器（按正确的顺序）
     * This implementation merely returns a list iterator over the list.
     * //注意此类的迭代器也是只有一个列表迭代器，并没有iterator。只有ListIterator.
     * @return an iterator over the elements in this list (in proper sequence)
     */
    public Iterator<E> iterator() {
        return listIterator();
    }

    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence).
     *返回列表迭代器（按正确的顺序）
     * @param  index index of first element to be returned from the list
     *         iterator (by a call to the <code>next</code> method)
     * @return a list iterator over the elements in this list (in proper
     *         sequence)
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public abstract ListIterator<E> listIterator(int index);
}
