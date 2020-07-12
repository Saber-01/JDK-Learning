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
 * Written by Doug Lea and Josh Bloch with assistance from members of
 * JCP JSR-166 Expert Group and released to the public domain, as explained
 * at http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util;

/**
 * A linear collection that supports element insertion and removal at
 * both ends.  The name <i>deque</i> is short for "double ended queue"
 * and is usually pronounced "deck".  Most {@code Deque}
 * implementations place no fixed limits on the number of elements
 * they may contain, but this interface supports capacity-restricted
 * deques as well as those with no fixed size limit.
 *支持在两端插入和删除的线性集合。"deque" 是 "double ended queue"
 *的简写，通常读作 "deck"。大多数 Deque 的实现对它们可能包含的元素
 * 数量没有固定的限制。但是这个接口支持有容量限制的 Deque 以及没有
 *  固定大小限制的 Deque。
 *
 * <p>This interface defines methods to access the elements at both
 * ends of the deque.  Methods are provided to insert, remove, and
 * examine the element.  Each of these methods exists in two forms:
 * one throws an exception if the operation fails, the other returns a
 * special value (either {@code null} or {@code false}, depending on
 * the operation).  The latter form of the insert operation is
 * designed specifically for use with capacity-restricted
 * {@code Deque} implementations; in most implementations, insert
 * operations cannot fail.
 *这个接口定义了访问 deque 两端元素的方法。这些方法用于插入、删除
 *  和检查元素。每一个方法都存在两种形式：一种在操作失败时抛出异常，
 *  另一种返回特定的值（null 或者 false，具体取决于该操作的定义）。后
 * 一种插入操作的形式是专门为有容量限制的 Deque 实现而设计的；在大
 *  多数实现中，插入操作不能失败。
 * <p>The twelve methods described above are summarized in the
 * following table:
 *这十二个方法总结如下：
 * First Element (Head):
 * Throws exception:
 * Insert----{@link Deque#addFirst addFirst(e)}
* Remove----{@link Deque#removeFirst removeFirst()}
* Examine----{@link Deque#getFirst getFirst()}
* Special value:
* Insert----{@link Deque#offerFirst offerFirst(e)}
* Remove----{@link Deque#pollFirst pollFirst()}
* Examine----{@link Deque#peekFirst peekFirst()}
* Last Element (Tail):
* Throws exception:
* Insert----{@link Deque#addLast addLast(e)}
* Remove----{@link Deque#removeLast removeLast()}
* Examine----{@link Deque#getLast getLast()}
* Special value:
* Insert----{@link Deque#offerLast offerLast(e)}
* Remove----{@link Deque#pollLast pollLast()}
* Examine----{@link Deque#peekLast peekLast()}
*
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 * <caption>Summary of Deque methods</caption>
 *  <tr>
 *    <td></td>
 *    <td ALIGN=CENTER COLSPAN = 2> <b>First Element (Head)</b></td>
 *    <td ALIGN=CENTER COLSPAN = 2> <b>Last Element (Tail)</b></td>
 *  </tr>
 *  <tr>
 *    <td></td>
 *    <td ALIGN=CENTER><em>Throws exception</em></td>
 *    <td ALIGN=CENTER><em>Special value</em></td>
 *    <td ALIGN=CENTER><em>Throws exception</em></td>
 *    <td ALIGN=CENTER><em>Special value</em></td>
 *  </tr>
 *  <tr>
 *    <td><b>Insert</b></td>
 *    <td>{@link Deque#addFirst addFirst(e)}</td>
 *    <td>{@link Deque#offerFirst offerFirst(e)}</td>
 *    <td>{@link Deque#addLast addLast(e)}</td>
 *    <td>{@link Deque#offerLast offerLast(e)}</td>
 *  </tr>
 *  <tr>
 *    <td><b>Remove</b></td>
 *    <td>{@link Deque#removeFirst removeFirst()}</td>
 *    <td>{@link Deque#pollFirst pollFirst()}</td>
 *    <td>{@link Deque#removeLast removeLast()}</td>
 *    <td>{@link Deque#pollLast pollLast()}</td>
 *  </tr>
 *  <tr>
 *    <td><b>Examine</b></td>
 *    <td>{@link Deque#getFirst getFirst()}</td>
 *    <td>{@link Deque#peekFirst peekFirst()}</td>
 *    <td>{@link Deque#getLast getLast()}</td>
 *    <td>{@link Deque#peekLast peekLast()}</td>
 *  </tr>
 * </table>
 *
 * <p>This interface extends the {@link Queue} interface.  When a deque is
 * used as a queue, FIFO (First-In-First-Out) behavior results.  Elements are
 * added at the end of the deque and removed from the beginning.  The methods
 * inherited from the {@code Queue} interface are precisely equivalent to
 * {@code Deque} methods as indicated in the following table:
 *这个接口继承了 Queue 接口。当一个 deque 用作 queue 时，
 * 将会产生 FIFO(First-In-First-Out) 的行为。元素将会添加到队列末尾，
 * 并删除队列头部的元素。从 Queue 接口继承的方法和下列 Deque 中的
 * 方法完全等价：
 * Comparison of Queue and Deque methods
 *  * {@code Queue} Method                       {@code Deque} Method
 *  * {@link Queue#add add(e)}                  {@link #addLast addLast(e)}
 *  * {@link Queue#offer offer(e)}            {@link #offerLast offerLast(e)}
 *  * {@link Queue#remove remove()}        {@link #removeFirst removeFirst()}
 *  * {@link Queue#poll poll()}                     {@link #pollFirst pollFirst()}
 *  * {@link Queue#element element()}      {@link #getFirst getFirst()}
 *  * {@link Queue#peek peek()}                {@link #peek peekFirst()}
 *
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 * <caption>Comparison of Queue and Deque methods</caption>
 *  <tr>
 *    <td ALIGN=CENTER> <b>{@code Queue} Method</b></td>
 *    <td ALIGN=CENTER> <b>Equivalent {@code Deque} Method</b></td>
 *  </tr>
 *  <tr>
 *    <td>{@link java.util.Queue#add add(e)}</td>
 *    <td>{@link #addLast addLast(e)}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link java.util.Queue#offer offer(e)}</td>
 *    <td>{@link #offerLast offerLast(e)}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link java.util.Queue#remove remove()}</td>
 *    <td>{@link #removeFirst removeFirst()}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link java.util.Queue#poll poll()}</td>
 *    <td>{@link #pollFirst pollFirst()}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link java.util.Queue#element element()}</td>
 *    <td>{@link #getFirst getFirst()}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link java.util.Queue#peek peek()}</td>
 *    <td>{@link #peek peekFirst()}</td>
 *  </tr>
 * </table>
 *
 * <p>Deques can also be used as LIFO (Last-In-First-Out) stacks.  This
 * interface should be used in preference to the legacy {@link Stack} class.
 * When a deque is used as a stack, elements are pushed and popped from the
 * beginning of the deque.  Stack methods are precisely equivalent to
 * {@code Deque} methods as indicated in the table below:
 *Deques 也可以用作 LIFO(Last-In-First-Out) 的栈。这个接口应该优先
 * 使用遗留的 Stack 类。当一个 deque 用作栈时，元素被添加到队列头部，
 *并从队列头部删除。Stack 的方法和下列 Deque 中的方法完全等价：
 *
 * Comparison of Stack and Deque methods
 *   Stack Method                                 Deque Method
 *   {@link #push push(e)}                   {@link #addFirst addFirst(e)}
 *   {@link #pop pop()}                         {@link #removeFirst removeFirst()}
 *   {@link #peek peek()}                     {@link #peekFirst peekFirst()}
 *
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 * <caption>Comparison of Stack and Deque methods</caption>
 *  <tr>
 *    <td ALIGN=CENTER> <b>Stack Method</b></td>
 *    <td ALIGN=CENTER> <b>Equivalent {@code Deque} Method</b></td>
 *  </tr>
 *  <tr>
 *    <td>{@link #push push(e)}</td>
 *    <td>{@link #addFirst addFirst(e)}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link #pop pop()}</td>
 *    <td>{@link #removeFirst removeFirst()}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link #peek peek()}</td>
 *    <td>{@link #peekFirst peekFirst()}</td>
 *  </tr>
 * </table>
 *
 * <p>Note that the {@link #peek peek} method works equally well when
 * a deque is used as a queue or a stack; in either case, elements are
 * drawn from the beginning of the deque.
 *注意，当 deque 被用作 queue 或者 stack 时，peek 方法同样有效；在
 * 上述两种情况下，元素都是从 deque 头部抽取。
 *
 * <p>This interface provides two methods to remove interior
 * elements, {@link #removeFirstOccurrence removeFirstOccurrence} and
 * {@link #removeLastOccurrence removeLastOccurrence}.
 *这一接口提供了两个方法来删除内部元素，removeFirstOccurrence 和
 *  removeLastOccurrence。
 *
 * <p>Unlike the {@link List} interface, this interface does not
 * provide support for indexed access to elements.
 *和 List 接口不同的是，这个接口不支持根据索引访问任意元素。
 *
 * <p>While {@code Deque} implementations are not strictly required
 * to prohibit the insertion of null elements, they are strongly
 * encouraged to do so.  Users of any {@code Deque} implementations
 * that do allow null elements are strongly encouraged <i>not</i> to
 * take advantage of the ability to insert nulls.  This is so because
 * {@code null} is used as a special return value by various methods
 * to indicated that the deque is empty.
 *Deque 的实现并不严格禁止插入 null 元素，但是强烈建议这样做。任何
 *支持 null 元素的 Deque 实现都强烈建议不要插入 null 元素。这是因为
 *  许多方法都把 null 作为一个特殊返回值，以说明 deque 为空集合。
 *
 * <p>{@code Deque} implementations generally do not define
 * element-based versions of the {@code equals} and {@code hashCode}
 * methods, but instead inherit the identity-based versions from class
 * {@code Object}.
 * Deque 的实现通常不定义 element-based 版本的 equals 方法和
 *  hashCode 方法，而是从 Object 类继承基于标识的版本。
 *
 * <p>This interface is a member of the <a
 * href="{@docRoot}/../technotes/guides/collections/index.html"> Java Collections
 * Framework</a>.
 *这个接口是 Java Collections Framework 的成员。
 * @author Doug Lea
 * @author Josh Bloch
 * @since  1.6
 * @param <E> the type of elements held in this collection
 */
public interface Deque<E> extends Queue<E> {
    /**
     * Inserts the specified element at the front of this deque if it is
     * possible to do so immediately without violating capacity restrictions,
     * throwing an {@code IllegalStateException} if no space is currently
     * available.  When using a capacity-restricted deque, it is generally
     * preferable to use method {@link #offerFirst}.
     *如果插入操作不违反容量限制，那么将指定元素插入到队列头部，
     *如果无空间可用抛出 IllegalStateException 异常。当 deque 有容量
     * 限制时，使用 offerFirst 方法更好。
     * @param e the element to add
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    void addFirst(E e);

    /**
     * Inserts the specified element at the end of this deque if it is
     * possible to do so immediately without violating capacity restrictions,
     * throwing an {@code IllegalStateException} if no space is currently
     * available.  When using a capacity-restricted deque, it is generally
     * preferable to use method {@link #offerLast}.
     *如果插入操作不违反容量限制，那么将指定元素插入到队列尾部，
     * 如果无空间可用抛出 IllegalStateException 异常。当 deque 有容量
     * 限制时，使用 offerLast 方法更好。
     * <p>This method is equivalent to {@link #add}.
     *这个方法等价于 add 方法。
     * @param e the element to add
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    void addLast(E e);

    /**
     * Inserts the specified element at the front of this deque unless it would
     * violate capacity restrictions.  When using a capacity-restricted deque,
     * this method is generally preferable to the {@link #addFirst} method,
     * which can fail to insert an element only by throwing an exception.
     *如果插入操作不违反容量限制，将指定元素插入到队列头部。当使用
     * 有容量限制的队列时，此方法通常比 addFirst 更可取，因为 addFirst
     *  插入失败只会抛出异常。
     * @param e the element to add
     * @return {@code true} if the element was added to this deque, else
     *         {@code false}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    boolean offerFirst(E e);

    /**
     * Inserts the specified element at the end of this deque unless it would
     * violate capacity restrictions.  When using a capacity-restricted deque,
     * this method is generally preferable to the {@link #addLast} method,
     * which can fail to insert an element only by throwing an exception.
     *如果插入操作不违反容量限制，将指定元素插入到队列尾部。当使用
     *有容量限制的队列时，此方法通常比 addLast 更可取，因为 addLast
     * 插入失败只会抛出异常。
     * @param e the element to add
     * @return {@code true} if the element was added to this deque, else
     *         {@code false}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    boolean offerLast(E e);

    /**
     * Retrieves and removes the first element of this deque.  This method
     * differs from {@link #pollFirst pollFirst} only in that it throws an
     * exception if this deque is empty.
     *检索并删除队列第一个元素。这一个方法与 pollFirst 的不同之处在于
     * 如果队列为空，它会抛出异常。
     * @return the head of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    E removeFirst();

    /**
     * Retrieves and removes the last element of this deque.  This method
     * differs from {@link #pollLast pollLast} only in that it throws an
     * exception if this deque is empty.
     * 检索并删除队列最后一个元素。这一个方法与 pollLast 的不同之处在于
     *  如果队列为空，它会抛出异常。
     * @return the tail of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    E removeLast();

    /**
     * Retrieves and removes the first element of this deque,
     * or returns {@code null} if this deque is empty.
     *检索并删除队列头部元素。如果队列为空返回 null。
     * @return the head of this deque, or {@code null} if this deque is empty
     */
    E pollFirst();

    /**
     * Retrieves and removes the last element of this deque,
     * or returns {@code null} if this deque is empty.
     *检索并删除队列尾部元素。如果队列为空返回 null。
     * @return the tail of this deque, or {@code null} if this deque is empty
     */
    E pollLast();

    /**
     * Retrieves, but does not remove, the first element of this deque.
     *
     * This method differs from {@link #peekFirst peekFirst} only in that it
     * throws an exception if this deque is empty.
     *检索但不删除队列的第一个元素。这个方法和 peekFirst 方法不同的
     * 是，他会在队列为空时抛出异常。
     * @return the head of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    E getFirst();

    /**
     * Retrieves, but does not remove, the last element of this deque.
     * This method differs from {@link #peekLast peekLast} only in that it
     * throws an exception if this deque is empty.
     *检索但不删除队列的最后一个元素。这个方法和 peekLast 方法不同的
     * 是，他会在队列为空时抛出异常。
     * @return the tail of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    E getLast();

    /**
     * Retrieves, but does not remove, the first element of this deque,
     * or returns {@code null} if this deque is empty.
     *检索但不删除队列的头部元素。如果队列为空返回 null。
     * @return the head of this deque, or {@code null} if this deque is empty
     */
    E peekFirst();

    /**
     * Retrieves, but does not remove, the last element of this deque,
     * or returns {@code null} if this deque is empty.
     * 检索但不删除队列的尾部元素。如果队列为空返回 null。
     * @return the tail of this deque, or {@code null} if this deque is empty
     */
    E peekLast();

    /**
     * Removes the first occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>
     * (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *从该队列中删除第一个匹配的元素。如果 deque 不包含该元素，队列
     * 保持不变。更一般地，删除满足下列条件的
     *  第一个元素 e ：(o==null ? e==null : o.equals(e))，如果该元素存在的话。
     * 如果这个队列包含指定的元素返回 true。
     * @param o element to be removed from this deque, if present
     * @return {@code true} if an element was removed as a result of this call
     * @throws ClassCastException if the class of the specified element
     *         is incompatible with this deque
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    boolean removeFirstOccurrence(Object o);

    /**
     * Removes the last occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the last element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>
     * (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *从该队列中删除最后一个匹配的元素。如果 deque 不包含该元素，
     * 队列保持不变。更一般地，删除满足下列条件的最后
     * 一个元素 e ：(o==null ? e==null : o.equals(e))，如果该元素存在的话。
     * 如果这个队列包含指定的元素返回 true。
     * @param o element to be removed from this deque, if present
     * @return {@code true} if an element was removed as a result of this call
     * @throws ClassCastException if the class of the specified element
     *         is incompatible with this deque
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    boolean removeLastOccurrence(Object o);

    // *** Queue methods ***
    // Queue 相关的操作
    /**
     * Inserts the specified element into the queue represented by this deque
     * (in other words, at the tail of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * {@code true} upon success and throwing an
     * {@code IllegalStateException} if no space is currently available.
     * When using a capacity-restricted deque, it is generally preferable to
     * use {@link #offer(Object) offer}.
     *如果插入操作不违反容量限制，那么将指定元素插入到队列尾部，成功
     * 后返回 true，如果无空间可用抛出 IllegalStateException 异常。
     * 如果队列为容量限制的 deque，那么使用 offer 更好。
     *
     * <p>This method is equivalent to {@link #addLast}.
     * 此方法等同于 addLast。
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    boolean add(E e);

    /**
     * Inserts the specified element into the queue represented by this deque
     * (in other words, at the tail of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * {@code true} upon success and {@code false} if no space is currently
     * available.  When using a capacity-restricted deque, this method is
     * generally preferable to the {@link #add} method, which can fail to
     * insert an element only by throwing an exception.
     *如果插入操作不违反容量限制，立即将指定元素插入到队列中。成功
     * 返回 true，无空间可用时返回 false。当使用有容量限制的队列时，
     * 此方法通常比 add 更可取，因为 add 插入失败只会抛出异常。
     *
     * <p>This method is equivalent to {@link #offerLast}.
     *此方法等同于 offerLast。
     *
     * @param e the element to add
     * @return {@code true} if the element was added to this deque, else
     *         {@code false}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    boolean offer(E e);

    /**
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque).
     * This method differs from {@link #poll poll} only in that it throws an
     * exception if this deque is empty.
     *检索并删除队列头（deque 的第一个元素）。这一个方法与 poll 的
     * 不同之处在于如果队列为空，它会抛出异常。
     *
     * <p>This method is equivalent to {@link #removeFirst()}.
     * 此方法等同于 removeFirst。
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException if this deque is empty
     */
    E remove();

    /**
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque), or returns
     * {@code null} if this deque is empty.
     *检索并删除队列头（deque 的第一个元素）。如果队列为空返回 null。
     * <p>This method is equivalent to {@link #pollFirst()}.
     * 此方法等同于 pollFirst。
     * @return the first element of this deque, or {@code null} if
     *         this deque is empty
     */
    E poll();

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque (in other words, the first element of this deque).
     * This method differs from {@link #peek peek} only in that it throws an
     * exception if this deque is empty.
     *检索但不删除队列的头部（deque 的第一个元素）。这个方法和 peek
     *方法不同的是，他会在队列为空时抛出异常。
     * <p>This method is equivalent to {@link #getFirst()}.
     * 此方法等同于 getFirst。
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException if this deque is empty
     */
    E element();

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque (in other words, the first element of this deque), or
     * returns {@code null} if this deque is empty.
     * 检索但不删除队列的头部。如果队列为空返回 null。
     * <p>This method is equivalent to {@link #peekFirst()}.
     *此方法等同于 peekFirst。
     * @return the head of the queue represented by this deque, or
     *         {@code null} if this deque is empty
     */
    E peek();


    // *** Stack methods ***
    // Stack 相关的操作
    /**
     * Pushes an element onto the stack represented by this deque (in other
     * words, at the head of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, throwing an
     * {@code IllegalStateException} if no space is currently available.
     *如果插入操作不违反容量限制，立即将指定元素插入到栈中。如果没有
     * 多余空间抛出 IllegalStateException 异常。
     *
     * <p>This method is equivalent to {@link #addFirst}.
     *此方法等同于 addFirst。
     *
     * @param e the element to push
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    void push(E e);

    /**
     * Pops an element from the stack represented by this deque.  In other
     * words, removes and returns the first element of this deque.
     *从栈中弹出栈顶元素。换句话说，从 deque 中移除并返回第一个元素。
     * <p>This method is equivalent to {@link #removeFirst()}.
     *此方法等同于 removeFirst。
     *
     * @return the element at the front of this deque (which is the top
     *         of the stack represented by this deque)
     * @throws NoSuchElementException if this deque is empty
     */
    E pop();


    // *** Collection methods ***
    // Collection 相关操作
    /**
     * Removes the first occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>
     * (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *从队列中删除第一个匹配的元素。如果不包含该元素，不做任何改变。
     *更正式地说，删除第一个满足下列条件的
     * 元素 e： (o==null?e==null:o.equals(e))。如果此集合包含指定的元素，
     *  则返回 true（如果此集合由于调用而更改，则返回 true）。
     *
     * <p>This method is equivalent to {@link #removeFirstOccurrence(Object)}.
     *此方法等同于 removeFirstOccurrence(Object)。
     * @param o element to be removed from this deque, if present
     * @return {@code true} if an element was removed as a result of this call
     * @throws ClassCastException if the class of the specified element
     *         is incompatible with this deque
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    boolean remove(Object o);

    /**
     * Returns {@code true} if this deque contains the specified element.
     * More formally, returns {@code true} if and only if this deque contains
     * at least one element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *如果队列包含指定元素返回 true。
     *
     * @param o element whose presence in this deque is to be tested
     * @return {@code true} if this deque contains the specified element
     * @throws ClassCastException if the type of the specified element
     *         is incompatible with this deque
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    boolean contains(Object o);

    /**
     * Returns the number of elements in this deque.
     * 队列包含的元素个数。
     * @return the number of elements in this deque
     */
    public int size();

    /**
     * Returns an iterator over the elements in this deque in proper sequence.
     * The elements will be returned in order from first (head) to last (tail).
     *返回队列的按序迭代器。顺序为从第一个元素（head）到最后
     *一个元素（tail）。
     * @return an iterator over the elements in this deque in proper sequence
     */
    Iterator<E> iterator();

    /**
     * Returns an iterator over the elements in this deque in reverse
     * sequential order.  The elements will be returned in order from
     * last (tail) to first (head).
     *返回队列的反序迭代器。顺序为从最后一个元素（tail）到第一个
     * 元素（head）。
     * @return an iterator over the elements in this deque in reverse
     * sequence
     */
    Iterator<E> descendingIterator();

}
