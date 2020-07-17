/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

package Collections;

import java.io.InvalidObjectException;
import java.util.*;

import sun.misc.SharedSecrets;

/**
 * 此类实现了 Set 接口，由一个 hash 表（实际上是一个 HashMap 实例）
 * 提供支持。此类不保证集合迭代的顺序；特别是，他不保证顺序会永恒不变。
 * 此类允许 null 元素。
 *
 * 假定 hash 函数将元素均匀地分散到所有的桶里，此类保证基本操作
 * （add, remove, contains 和 size）在恒定时间内完成。
 * 遍历这个结合需要的时间和 HashSet 实例的大小（元素的数量）加上
 * HashMap 实例的容量（桶的数量）的总和成正比。因此如果想要提高遍历
 * 的性能的话，不要将初始容量设置得太高（或负载因子太低）。
 *
 * 注意，这个实现不是同步的。如果多个线程同时访问 HashSet，且至少一个
 * 线程修改了此 Set，那么必须从外部同步该集合。这通常是对一些自然封装
 * 了集合的对象进行同步来实现。
 *
 * 如果不存在这样的对象，则应该使用 Collections.synchronizedSet 方法
 * 包装该对象。这一步最好是在创建时完成，以防止对此 set 的意外异步访问：
 * Set s = Collections.synchronizedSet(new HashSet(...));
 * 此类的 iterator 方法返回的迭代器支持 fast-fail：如果在迭代器创建之后
 * 如果集合在任何时候被修改了，除了通过迭代器自身的 remove 方法，
 * 迭代器将抛出 ConcurrentModificationException 异常。因此，在面对并发
 * 修改的时候，迭代器会快速干净地失败，而不是在将来某个不确定的时间
 * 出现不确定的行为。
 *
 * 注意不能保证迭代器的 fast-fail 行为完全正确，通常来说，在存在异步并发
 * 的情况下，不可能做出任何严格的保证。支持 fast-fail 的迭代器会尽最大
 * 努力抛出 ConcurrentModificationException 异常。因此，编写一个依赖
 * 此异常来判断其正确性的程序是错误的：迭代器的 fast-fail 机制应该只用于
 * 检测 bug。
 *
 * 此类是 Java Collections Framework 的成员。
 *
 * @param <E> the type of elements maintained by this set
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see     Collection
 * @see     Set
 * @see     TreeSet
 * @see     HashMap
 * @since   1.2
 */

public class HashSet<E>
        extends AbstractSet<E>
        implements Set<E>, Cloneable, java.io.Serializable
{
    static final long serialVersionUID = -5024744406713321676L;

    // 基于 HashMap 存放数据，map 的 key 就是 HashSet 要存放的数据
    private transient HashMap<E,Object> map;

    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();

    /**
     * 构造一个空集合；HashMap 实例的容量为默认容量（16），加载因子为
     * 默认加载因子（0.75）
     */
    public HashSet() {
        map = new HashMap<>();
    }

    /**
     * 构造一个包含指定集合所有元素的新的 set。HashMap 实例的初始容量
     * 足以容纳指定集合的所有元素，加载因子为默认加载因子（0.75）
     *
     * @param c the collection whose elements are to be placed into this set
     * @throws NullPointerException if the specified collection is null
     */
    public HashSet(java.util.Collection<? extends E> c) {
        map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
        addAll(c);
    }

    /**
     * 构造一个空集合；HashMap 实例的初始容量为指定参数 initialCapacity，
     * 加载因子为指定参数 loadFactor
     *
     * @param      initialCapacity   the initial capacity of the hash map
     * @param      loadFactor        the load factor of the hash map
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero, or if the load factor is nonpositive
     */
    public HashSet(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    /**
     * 构造一个空集合；HashMap 实例的初始容量为指定参数 initialCapacity，
     * 加载因子为默认加载因子（0.75）
     *
     * @param      initialCapacity   the initial capacity of the hash table
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero
     */
    public HashSet(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

    /**
     * 构造一个空的 linkedHashSet。（这个只有包权限的构造方法只用于
     * LinkedHashSet。)支撑此集合的 HashMap 实例是有指定初始容量和
     * 指定加载因子的 LinkedHashMap。
     *
     * @August 包内的任何类都可以访问，包外的任何其他类都不能访问
     * （包括包外继承了此类的子类）
     *
     * @param      initialCapacity   the initial capacity of the hash map
     * @param      loadFactor        the load factor of the hash map
     * @param      dummy             ignored (distinguishes this
     *             constructor from other int, float constructor.)
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero, or if the load factor is nonpositive
     */
    HashSet(int initialCapacity, float loadFactor, boolean dummy) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    /**
     * 返回 set 的一个迭代器。迭代器返回的元素没有特定顺序。
     *
     * @return an Iterator over the elements in this set
     * @see ConcurrentModificationException
     */
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    /**
     * 返回集合中元素的数量
     *
     * @return the number of elements in this set (its cardinality)
     */
    public int size() {
        return map.size();
    }

    /**
     * 如果集合不包含任何元素返回 true
     *
     * @return true if this set contains no elements
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * 如果集合包含指定的元素则返回 true
     *
     * @param o element whose presence in this set is to be tested
     * @return true if this set contains the specified element
     */
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    /**
     * 将指定元素添加到集合里，如果它在集合中并不存在的话。
     * 如果集合已经包含该元素，那么集合不作出任何改变，并返回 false
     *
     * @param e element to be added to this set
     * @return true if this set did not already contain the specified
     * element
     */
    public boolean add(E e) {
        return map.put(e, PRESENT)==null;
    }

    /**
     * 从集合中删除指定元素，如果其存在的话。如果集合包含该元素，则返回
     * true，调用此方法后，集合不会再包含该元素。
     *
     * @param o object to be removed from this set, if present
     * @return true if the set contained the specified element
     */
    public boolean remove(Object o) {
        return map.remove(o)==PRESENT;
    }

    /**
     * 从集合中删除所有元素。
     * 此方法调用后集合为空。
     */
    public void clear() {
        map.clear();
    }

    /**
     * 返回 HashSet 的一个浅拷贝：集合里的元素本身没有拷贝。
     *
     * @return a shallow copy of this set
     */
    @SuppressWarnings("unchecked")
    public Object clone() {
        try {
            HashSet<E> newSet = (HashSet<E>) super.clone();
            newSet.map = (HashMap<E, Object>) map.clone();
            return newSet;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * 保存 HashSet 实例到流里（即序列化）
     *
     * @serialData The capacity of the backing <tt>HashMap</tt> instance
     *             (int), and its load factor (float) are emitted, followed by
     *             the size of the set (the number of elements it contains)
     *             (int), followed by all of its elements (each an Object) in
     *             no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        // Write out any hidden serialization magic
        s.defaultWriteObject();

        // Write out HashMap capacity and load factor
        s.writeInt(map.capacity());
        s.writeFloat(map.loadFactor());

        // Write out size
        s.writeInt(map.size());

        // Write out all elements in the proper order.
        for (E e : map.keySet())
            s.writeObject(e);
    }

    /**
     * 反序列化。
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        // Read in any hidden serialization magic
        s.defaultReadObject();

        // Read capacity and verify non-negative.
        int capacity = s.readInt();
        if (capacity < 0) {
            throw new InvalidObjectException("Illegal capacity: " +
                    capacity);
        }

        // Read load factor and verify positive and non NaN.
        float loadFactor = s.readFloat();
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new InvalidObjectException("Illegal load factor: " +
                    loadFactor);
        }

        // Read size and verify non-negative.
        int size = s.readInt();
        if (size < 0) {
            throw new InvalidObjectException("Illegal size: " +
                    size);
        }
        // Set the capacity according to the size and load factor ensuring that
        // the HashMap is at least 25% full but clamping to maximum capacity.
        capacity = (int) Math.min(size * Math.min(1 / loadFactor, 4.0f),
                HashMap.MAXIMUM_CAPACITY);

        // Constructing the backing map will lazily create an array when the first element is
        // added, so check it before construction. Call HashMap.tableSizeFor to compute the
        // actual allocation size. Check Map.Entry[].class since it's the nearest public type to
        // what is actually created.

        SharedSecrets.getJavaOISAccess()
                .checkArray(s, Map.Entry[].class, HashMap.tableSizeFor(capacity));

        // Create backing HashMap
        map = (((HashSet<?>)this) instanceof LinkedHashSet ?
                new LinkedHashMap<E,Object>(capacity, loadFactor) :
                new HashMap<E,Object>(capacity, loadFactor));

        // Read in all elements in the proper order.
        for (int i=0; i<size; i++) {
            @SuppressWarnings("unchecked")
            E e = (E) s.readObject();
            map.put(e, PRESENT);
        }
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * set.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED} and
     * {@link Spliterator#DISTINCT}.  Overriding implementations should document
     * the reporting of additional characteristic values.
     *
     * @return a {@code Spliterator} over the elements in this set
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return new HashMap.KeySpliterator<E,Object>(map, 0, -1, 0, 0);
    }
}