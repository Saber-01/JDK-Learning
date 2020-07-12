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
 * A collection designed for holding elements prior to processing.
 * Besides basic {@link java.util.Collection Collection} operations,
 * queues provide additional insertion, extraction, and inspection
 * operations.  Each of these methods exists in two forms: one throws
 * an exception if the operation fails, the other returns a special
 * value (either {@code null} or {@code false}, depending on the
 * operation).  The latter form of the insert operation is designed
 * specifically for use with capacity-restricted {@code Queue}
 * implementations; in most implementations, insert operations cannot
 * fail.
 *
 * 这是一个用于在进行处理之前保存元素的集合类。除了基本的 Collection 框架
 *  提供的操作外，队列额外提供插入，提取和检查操作。这些方法都以两种形式
 *  存在：一种在操作失败时抛出异常，另一种返回特殊值（null 或者 false，取决于
 *  操作）。后一种插入操作的形式是专门为受容量限制的 Queue 设计的，在大多数
 *  实现方式下，插入操作不能失败。
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 * <caption>Summary of Queue methods</caption>
 *  <tr>
 *    <td></td>
 *    <td ALIGN=CENTER><em>Throws exception</em></td>
 *    <td ALIGN=CENTER><em>Returns special value</em></td>
 *  </tr>
 *  <tr>
 *    <td><b>Insert</b></td>
 *    <td>{@link Queue#add add(e)}</td>
 *    <td>{@link Queue#offer offer(e)}</td>
 *  </tr>
 *  <tr>
 *    <td><b>Remove</b></td>
 *    <td>{@link Queue#remove remove()}</td>
 *    <td>{@link Queue#poll poll()}</td>
 *  </tr>
 *  <tr>
 *    <td><b>Examine</b></td>
 *    <td>{@link Queue#element element()}</td>
 *    <td>{@link Queue#peek peek()}</td>
 *  </tr>
 * </table>
 *
 * <p>Queues typically, but do not necessarily, order elements in a
 * FIFO (first-in-first-out) manner.  Among the exceptions are
 * priority queues, which order elements according to a supplied
 * comparator, or the elements' natural ordering, and LIFO queues (or
 * stacks) which order the elements LIFO (last-in-first-out).
 *  队列通常（但并不一定）以 FIFO（先进先出）的方式对元素排序。例外的情况
 *  包括优先队列（根据提供的比较器或者元素自然顺序对元素排序）和 先进后出的
 * LIFO 队列（或堆栈）。
 *
 * Whatever the ordering used, the <em>head</em> of the queue is that
 * element which would be removed by a call to {@link #remove() } or
 * {@link #poll()}.  In a FIFO queue, all new elements are inserted at
 * the <em>tail</em> of the queue. Other kinds of queues may use
 * different placement rules.  Every {@code Queue} implementation
 * must specify its ordering properties.
 *无论使用什么顺序，都是通过 remove 或 poll 来删除 head 元素的。在一个FIFO
 *  队列中，所有的新元素都插入队列尾部。其他类型的队列可能使用不同的放置
 * 规则。每一个 Queue 的实现都必须指定排序规则。
 *
 * <p>The {@link #offer offer} method inserts an element if possible,
 * otherwise returning {@code false}.  This differs from the {@link
 * java.util.Collection#add Collection.add} method, which can fail to
 * add an element only by throwing an unchecked exception.  The
 * {@code offer} method is designed for use when failure is a normal,
 * rather than exceptional occurrence, for example, in fixed-capacity
 * (or &quot;bounded&quot;) queues.
 *如果可能，offer方法插入一个元素，否则返回false。这和 Collection.add 方法
 *不同，Collection.add 方法只会在添加失败的时候抛出未检查的异常。offer 方法
 * 用于在故障是正常的情况下使用，而不是在异常情况（例如在固定容量，或有界
 * 队列）下使用。
 *
 * <p>The {@link #remove()} and {@link #poll()} methods remove and
 * return the head of the queue.
 * Exactly which element is removed from the queue is a
 * function of the queue's ordering policy, which differs from
 * implementation to implementation. The {@code remove()} and
 * {@code poll()} methods differ only in their behavior when the
 * queue is empty: the {@code remove()} method throws an exception,
 * while the {@code poll()} method returns {@code null}.
 *  remove 和 poll 方法从列表头部删除元素，并返回该元素。
 *  确切地说，从队列删除哪一个元素是队列的排序策略决定的，每一个实现都不一
 *  样。remove 和 poll 方法只在队列为空时行为不同：remove 抛出异常，而 poll
 * 方法返回 null。
 *
 * <p>The {@link #element()} and {@link #peek()} methods return, but do
 * not remove, the head of the queue.
 *element 和 peek 方法返回队列的头部元素，但不会删除。
 *
 * <p>The {@code Queue} interface does not define the <i>blocking queue
 * methods</i>, which are common in concurrent programming.  These methods,
 * which wait for elements to appear or for space to become available, are
 * defined in the {@link java.util.concurrent.BlockingQueue} interface, which
 * extends this interface.
 *Queue 接口不定义阻塞队列方法，尽管这些方法在并发编程中很常见。这些方法
 * 在 java.util.concurrent.BlockingQueue 接口中定义，它们等待元素出现或空间
 * 可用。
 *
 * <p>{@code Queue} implementations generally do not allow insertion
 * of {@code null} elements, although some implementations, such as
 * {@link LinkedList}, do not prohibit insertion of {@code null}.
 * Even in the implementations that permit it, {@code null} should
 * not be inserted into a {@code Queue}, as {@code null} is also
 * used as a special return value by the {@code poll} method to
 * indicate that the queue contains no elements.
 *Queue 的实现通常不允许插入 null 元素，尽管有的实现，例如 LinkedList 不禁止
 * 插入 null。即使在允许插入 null 的实现中，null 也不应该插入到 Queue 中，因为 poll 将
 *  null 用作特殊的返回值，用来表示队列不包含任何元素。
 *
 * <p>{@code Queue} implementations generally do not define
 * element-based versions of methods {@code equals} and
 * {@code hashCode} but instead inherit the identity based versions
 * from class {@code Object}, because element-based equality is not
 * always well-defined for queues with the same elements but different
 * ordering properties.
 *Queue 的实现通常不定义 element-based 版本的 equals 方法和 hashCode 方法，而是
 *从 Object 类继承基于标识的版本，因为 element-based 的相等在元素相同但
 * 排序属性不同的队列里的定义并不确定。
 *
 * <p>This interface is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *这个接口是 Java Collections Framework 的成员。
 *
 * @see java.util.Collection
 * @see LinkedList
 * @see PriorityQueue
 * @see java.util.concurrent.LinkedBlockingQueue
 * @see java.util.concurrent.BlockingQueue
 * @see java.util.concurrent.ArrayBlockingQueue
 * @see java.util.concurrent.LinkedBlockingQueue
 * @see java.util.concurrent.PriorityBlockingQueue
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public interface Queue<E> extends Collection<E> {
    /**
     * Inserts the specified element into this queue if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * {@code true} upon success and throwing an {@code IllegalStateException}
     * if no space is currently available.
     *如果插入操作不违反容量限制，那么将指定元素插入到队列中，成功后返回
     *  true，如果无空间可用抛出 IllegalStateException 异常。
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions   非法状态异常
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue  类型强制转化错误
     * @throws NullPointerException if the specified element is null and
     *         this queue does not permit null elements  空指针异常
     * @throws IllegalArgumentException if some property of this element
     *         prevents it from being added to this queue  非法参数
     */
    boolean add(E e);

    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions.
     * When using a capacity-restricted queue, this method is generally
     * preferable to {@link #add}, which can fail to insert an element only
     * by throwing an exception.
     *如果插入操作不违反容量限制，立即将指定元素插入到队列中。
     * 当使用有容量限制的队列时，此方法通常比 add 更可取，因为 add 插入失败
     * 只会抛出异常。
     * 注意：少了非法状态异常，因为offer插入失败时，会返回false。
     * @param e the element to add
     * @return {@code true} if the element was added to this queue, else
     *         {@code false}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null and
     *         this queue does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     *         prevents it from being added to this queue
     */
    boolean offer(E e);

    /**
     * Retrieves and removes the head of this queue.  This method differs
     * from {@link #poll poll} only in that it throws an exception if this
     * queue is empty.
     *检索并删除队列头。这一个方法与 poll 的不同之处在于如果队列为空，
     *  它会抛出异常。
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    E remove();

    /**
     * Retrieves and removes the head of this queue,
     * or returns {@code null} if this queue is empty.
     *检索并删除队列头，如果队列为空返回 null。
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    E poll();

    /**
     * Retrieves, but does not remove, the head of this queue.  This method
     * differs from {@link #peek peek} only in that it throws an exception
     * if this queue is empty.
     *检索但不删除队列的头部。这个方法和 peek 方法不同的是，他会在
     * 队列为空时抛出异常。
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    E element();

    /**
     * Retrieves, but does not remove, the head of this queue,
     * or returns {@code null} if this queue is empty.
     *检索但不删除队列的头部。如果队列为空返回 null。
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    E peek();
}
