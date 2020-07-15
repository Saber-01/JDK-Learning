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

package java.util;

/**
 * This class provides skeletal implementations of some {@link Queue}
 * operations. The implementations in this class are appropriate when
 * the base implementation does <em>not</em> allow <tt>null</tt>
 * elements.  Methods {@link #add add}, {@link #remove remove}, and
 * {@link #element element} are based on {@link #offer offer}, {@link
 * #poll poll}, and {@link #peek peek}, respectively, but throw
 * exceptions instead of indicating failure via <tt>false</tt> or
 * <tt>null</tt> returns.
 *这个类提供一些队列操作的实现。当此类的基本实现不允许 null 元素时，这些
 *   实现是可取的。方法 add、remove 和 element 分别基于 offer、poll 和 peek，
 *  但是其结果是抛出异常而不是通过返回 false 或者 null 宣告失败。
 *
 * <p>A <tt>Queue</tt> implementation that extends this class must
 * minimally define a method {@link Queue#offer} which does not permit
 * insertion of <tt>null</tt> elements, along with methods {@link
 * Queue#peek}, {@link Queue#poll}, {@link Collection#size}, and
 * {@link Collection#iterator}.  Typically, additional methods will be
 * overridden as well.  If these requirements cannot be met, consider
 * instead subclassing {@link AbstractCollection}.
 *继承这个类的队列实现必须至少定义一个不允许插入空元素的 offer 方法，
 * 以及 peek, poll, size, iterator 方法。通常还会覆盖其他方法。如果这些需求
 * 无法满足，考虑继承 AbstractCollection。
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *这个类是 Java Collections Framework 的成员。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public abstract class AbstractQueue<E>
        extends AbstractCollection<E>
        implements Queue<E> {

    /**
     * Constructor for use by subclasses.
     * //被子类使用的构造函数。
     */
    protected AbstractQueue() {
    }

    /**
     * Inserts the specified element into this queue if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * <tt>true</tt> upon success and throwing an <tt>IllegalStateException</tt>
     * if no space is currently available.
     * 在不违反容量限制的时候立即将指定元素插入到队列中，成功返回 true，
     *   如果没有空间可用，抛出 IllegalStateException。
     * <p>This implementation returns <tt>true</tt> if <tt>offer</tt> succeeds,
     * else throws an <tt>IllegalStateException</tt>.
     *如果 offer 操作成功返回 true，否则抛出 IllegalStateException 异常。
     * @param e the element to add
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null and
     *         this queue does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     *         prevents it from being added to this queue
     */
    public boolean add(E e) {
        if (offer(e))
            return true;
        else
            throw new IllegalStateException("Queue full");
    }

    /**
     * Retrieves and removes the head of this queue.  This method differs
     * from {@link #poll poll} only in that it throws an exception if this
     * queue is empty.
     *检索并删除队列头部元素。此方法和 poll 的区别是如果队列为空会抛出
     *    异常而没有返回值。
     * <p>This implementation returns the result of <tt>poll</tt>
     * unless the queue is empty.
     * 除非队列为空，其他情况下此方法会返回 poll 的结果。
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    public E remove() {
        E x = poll();
        if (x != null)
            return x;
        else
            throw new NoSuchElementException();
    }

    /**
     * Retrieves, but does not remove, the head of this queue.  This method
     * differs from {@link #peek peek} only in that it throws an exception if
     * this queue is empty.
     * 检索但不删除队列的头部元素。此方法和 peek 的区别是如果队列为空会
     * 抛出异常。
     * <p>This implementation returns the result of <tt>peek</tt>
     * unless the queue is empty.
     * 除非队列为空，其他情况下此方法会返回 peek 的结果。
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    public E element() {
        E x = peek();
        if (x != null)
            return x;
        else
            throw new NoSuchElementException();
    }

    /**
     * Removes all of the elements from this queue.
     * The queue will be empty after this call returns.
     *删除队列的所有元素。
     * 此方法调用后队列为空。
     * <p>This implementation repeatedly invokes {@link #poll poll} until it
     * returns <tt>null</tt>.
     *  此方法会反复调用 poll 方法直到其返回 null
     */
    public void clear() {
        while (poll() != null)
            ;
    }

    /**
     * Adds all of the elements in the specified collection to this
     * queue.  Attempts to addAll of a queue to itself result in
     * <tt>IllegalArgumentException</tt>. Further, the behavior of
     * this operation is undefined if the specified collection is
     * modified while the operation is in progress.
     *将指定集合的所有元素添加到此队列。将所有元素添加到自身会导致
     *  IllegalArgumentException 异常。此外，在操作进行过程中如果指定集合
     *   被修改，则操作的行为未知。
     * <p>This implementation iterates over the specified collection,
     * and adds each element returned by the iterator to this
     * queue, in turn.  A runtime exception encountered while
     * trying to add an element (including, in particular, a
     * <tt>null</tt> element) may result in only some of the elements
     * having been successfully added when the associated exception is
     * thrown.
     *这个方法迭代遍历指定集合，然后将迭代器返回的元素按顺序添加到队列中。
     *   若添加过程中遇到运行时异常，可能会导致只有一些元素成功添加到队列中。
     *
     * @param c collection containing elements to be added to this queue
     * @return <tt>true</tt> if this queue changed as a result of the call
     * @throws ClassCastException if the class of an element of the specified
     *         collection prevents it from being added to this queue
     * @throws NullPointerException if the specified collection contains a
     *         null element and this queue does not permit null elements,
     *         or if the specified collection is null
     * @throws IllegalArgumentException if some property of an element of the
     *         specified collection prevents it from being added to this
     *         queue, or if the specified collection is this queue
     * @throws IllegalStateException if not all the elements can be added at
     *         this time due to insertion restrictions
     * @see #add(Object)
     */
    public boolean addAll(Collection<? extends E> c) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        boolean modified = false;
        for (E e : c)
            if (add(e))
                modified = true;
        return modified;
    }

}
