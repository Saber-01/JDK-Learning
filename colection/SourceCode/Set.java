package Collections;
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

import java.util.*;
import java.util.Collection;
import java.util.List;

/**
 * 不包含重复元素的集合。最多只包含一个空元素。顾名思义，该接口对数学中
 * 的 set 进行抽象。
 *
 * Set 接口在集合接口继承约定之外，在所有构造函数以及 add, equals, hashCode
 * 方法的约定上有额外的规范。为了方便起见，这里还包含了其他继承方法的声明。
 * （这些规范是针对 Set 接口定制的）
 *
 * 关于构造函数的额外规定是，所有构造函数都必须创建一个不包含重复元素的
 * 集合。
 *
 * 注意：如果使用可变的元素作为集合元素，必须格外小心。如果对象作为一个
 * 集合元素，而对象的值改变时，会影响 equals 的比较，从而集合的行为不确定。
 * 这一限制的一个典型例子是，不允许将集合本身作为集合的一个元素。
 *
 * 一些 set 的实现对其包含的元素有限制。例如，一些实现禁止 null 元素，一些
 * 对元素类型有限制。试图插入一个非法元素会抛出未检查的异常，特别是
 * NullPointerException 或者 ClassCastException。试图查询非法元素会抛出
 * 异常，或者只是返回 false；一些实现会出现前一种情况，一些会出现后一种
 * 情况。
 *
 * 此接口是 Java Collections Framework 的成员。
 *
 * @param <E> the type of elements maintained by this set
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see java.util.Collection
 * @see List
 * @see SortedSet
 * @see HashSet
 * @see TreeSet
 * @see AbstractSet
 * @see Collections#singleton(java.lang.Object)
 * @see Collections#EMPTY_SET
 * @since 1.2
 */

public interface Set<E> extends java.util.Collection<E> {
    // Query Operations
    // 查询操作

    /**
     * 返回集合集合元素的个数。如果此集合包含了超过 Integer.MAX_VALUE
     * 个元素，则返回 Integer.MAX_VALUE
     *
     * @return the number of elements in this set (its cardinality)
     */
    int size();

    /**
     * 如果集合不包含任何元素返回 true
     *
     * @return true if this set contains no elements
     */
    boolean isEmpty();

    /**
     * 如果集合包含指定元素则返回 true
     *
     * @param o element whose presence in this set is to be tested
     * @return true if this set contains the specified element
     * @throws ClassCastException if the type of the specified element
     *         is incompatible with this set
     * @throws NullPointerException if the specified element is null and this
     *         set does not permit null elements
     */
    boolean contains(Object o);

    /**
     * 返回集合元素的迭代器。返回的元素没有特定的顺序（除非此集合是一种
     * 提供这一保证的类）
     *
     * @return an iterator over the elements in this set
     */
    Iterator<E> iterator();

    /**
     * 返回包含集合所有元素的数组
     *
     * @return an array containing all the elements in this set
     */
    Object[] toArray();

    /**
     * 返回包含集合所有元素的数组
     *
     * @param a the array into which the elements of this set are to be
     *        stored, if it is big enough; otherwise, a new array of the same
     *        runtime type is allocated for this purpose.
     * @return an array containing all the elements in this set
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in this
     *         set
     * @throws NullPointerException if the specified array is null
     */
    <T> T[] toArray(T[] a);


    // Modification Operations
    // 修改操作

    /**
     * 将指定元素添加到集合中，如果集合中之前不存在该元素的话。如果集合
     * 已经包含该元素，则不作出任何改变，并返回 false。对构造 set 集合的
     * 限制，确保了 set 不包含重复的元素。
     *
     * 上述规定并不意味着集合必须接受所有元素；集合可能会拒绝添加特殊的
     * 元素，包括 null，然后像 Collection.add 规范中描述的那样，抛出异常。
     * 每一个 set 的实现应该清楚地说明对其可以包含元素的限制。
     *
     * @param e element to be added to this set
     * @return <tt>true</tt> if this set did not already contain the specified
     *         element
     * @throws UnsupportedOperationException if the <tt>add</tt> operation
     *         is not supported by this set
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this set
     * @throws NullPointerException if the specified element is null and this
     *         set does not permit null elements
     * @throws IllegalArgumentException if some property of the specified element
     *         prevents it from being added to this set
     */
    boolean add(E e);


    /**
     * 从集合中删除指定元素，如果它存在的话（可选操作）。如果存在该元素
     * 则返回 true
     *
     * @param o object to be removed from this set, if present
     * @return <tt>true</tt> if this set contained the specified element
     * @throws ClassCastException if the type of the specified element
     *         is incompatible with this set
     * @throws NullPointerException if the specified element is null and this
     *         set does not permit null elements
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation
     *         is not supported by this set
     */
    boolean remove(Object o);


    // Bulk Operations
    // 批量操作

    /**
     * 如果此集合包含指定集合的所有元素则返回 true。如果指定集合也是 set，
     * 且指定集合是此集合的子集，则返回 true。
     *
     * @param  c collection to be checked for containment in this set
     * @return true if this set contains all of the elements of the
     *         specified collection
     * @throws ClassCastException if the types of one or more elements
     *         in the specified collection are incompatible with this
     *         set
     * @throws NullPointerException if the specified collection contains one
     *         or more null elements and this set does not permit null
     *         elements, or if the specified collection is null
     * @see    #contains(Object)
     */
    boolean containsAll(java.util.Collection<?> c);

    /**
     * 将指定集合的所有元素添加到此集合中，如果这些元素在此集合中不存在
     * 的话。如果指定集合也是 set，addAll 操作会修改此集合，从而此集合的
     * 元素是两个集合的并集。如果操作进行过程中此集合被修改，那么此操作的
     * 行为未知。
     *
     * @param  c collection containing elements to be added to this set
     * @return true if this set changed as a result of the call
     *
     * @throws UnsupportedOperationException if the <tt>addAll</tt> operation
     *         is not supported by this set
     * @throws ClassCastException if the class of an element of the
     *         specified collection prevents it from being added to this set
     * @throws NullPointerException if the specified collection contains one
     *         or more null elements and this set does not permit null
     *         elements, or if the specified collection is null
     * @throws IllegalArgumentException if some property of an element of the
     *         specified collection prevents it from being added to this set
     * @see #add(Object)
     */
    boolean addAll(java.util.Collection<? extends E> c);

    /**
     * 只保留此集合中和指定集合相同的元素。换句话说，删除只存在于此集合而
     * 不存在于指定集合中的元素。如果指定集合也是 set，那么此方法会修改此
     * 集合，最终此集合是两个集合的交集。
     *
     * @param  c collection containing elements to be retained in this set
     * @return true if this set changed as a result of the call
     * @throws UnsupportedOperationException if the <tt>retainAll</tt> operation
     *         is not supported by this set
     * @throws ClassCastException if the class of an element of this set
     *         is incompatible with the specified collection
     * @throws NullPointerException if this set contains a null element and the
     *         specified collection does not permit null elements or if the
     *         specified collection is null
     * @see #remove(Object)
     */
    boolean retainAll(java.util.Collection<?> c);

    /**
     * 从此集合中删除和指定集合中相同的元素。
     *
     * @param  c collection containing elements to be removed from this set
     * @return <tt>true</tt> if this set changed as a result of the call
     * @throws UnsupportedOperationException if the <tt>removeAll</tt> operation
     *         is not supported by this set
     * @throws ClassCastException if the class of an element of this set
     *         is incompatible with the specified collection
     * @throws NullPointerException if this set contains a null element and the
     *         specified collection does not permit null elements, or if the
     *         specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    boolean removeAll(Collection<?> c);

    /**
     * 删除集合中所有元素。
     *
     * @throws UnsupportedOperationException if the <tt>clear</tt> method
     *         is not supported by this set
     */
    void clear();


    // Comparison and hashing
    // 比较和 hash 操作

    /**
     * 比较此集合和指定集合是否相等。如果指定集合也是 set，且两个集合大小
     * 相同，指定集合的每一个元素都包含在此集合中（或者说，此集合的每一个
     * 元素都包含在指定集合中），那么说两个集合相等，返回 true
     *
     * @param o object to be compared for equality with this set
     * @return <tt>true</tt> if the specified object is equal to this set
     */
    boolean equals(Object o);

    /**
     * 返回集合的 hash 值。集合的 hash 值定义为集合所有元素 hash 值的和，
     *  null 元素的 hash 值定义为 0
     *
     * @return the hash code value for this set
     * @see Object#equals(Object)
     * @see java.util.Set#equals(Object)
     */
    int hashCode();

    /**
     * Creates a {@code Spliterator} over the elements in this set.
     *
     * The {@code Spliterator} reports {@link Spliterator#DISTINCT}.
     * Implementations should document the reporting of additional
     * characteristic values.
     *
     * @implSpec
     * The default implementation creates a spliterator from the set's
     * {@code Iterator}.  The spliterator inherits the fail-fast properties
     * of the set's iterator.
     * The created {@code Spliterator} additionally reports
     * {@link Spliterator#SIZED}.
     *
     * @implNote
     * The created {@code Spliterator} additionally reports
     * {@link Spliterator#SUBSIZED}.
     *
     * @return a {@code Spliterator} over the elements in this set
     * @since 1.8
     */
    @Override
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT);
    }
}