package Collections;
/*
 * Copyright (c) 1998, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * SortedMap 是一个额外提供对 key 排序的 Map。根据 key 的自然顺序或者
 * 指定的 Comparator 进行排序（Comparator 通常在创建 Map 的时候指定）
 * 对 Map 的集合视图（由 entrySet, keySet 和 values 方法返回）迭代
 * 时会使用到这一排序结果。除此之外还提供了集中额外的方法来利用排序。
 *
 * 插入到 SortedMap 中的所有 key 必须实现 Comparable 接口（或者存在
 * 一个指定的比较器）。除此之外，所有的键必须是相互可比较的：对于任意
 * 两个键 k1, k2，保证k1.compareTo(k2)（或者 comparator.compare(k1, k2)）
 * 不会抛出 ClassCastException 异常。
 *
 * 注意 SortedMap 所维护的顺序（无论是否提供显式比较器）必须与 equals
 * 一致。这是因为 Map 接口是根据 equals 操作定义的，SortedMap 使用
 * compareTo（或compare方法）执行所有的键比较，所以从 SortedMap 的
 * 角度看，比较的结果必须与 equals 操作的行为一致。
 *
 * 所有通用的 SortedMap 都应该提供四个“标准”构造函数。由于接口中无法
 * 指定构造函数，所以这一建议无法强制执行。所有 SortedMap 的实现都
 * 应该有以下四个构造函数：
 *   一个 void 构造函数，根据 key 的自然顺序创建一个空的 SortedMap。
 *   一个包含参数 Comparator 的构造函数，根据指定的比较器创建一个空的 SortedMap。
 *   一个包含参数 Map 的构造函数。创建一个和参数 Map 中所有键值对相同的 Map，根据键的自然顺序排序。
 *   一个包含参数 SortedMap 的构造函数，创建一个和参数中所有键值对相同的 SortedMap。
 *
 *   此接口是  Java Collections Framework 的成员。
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Josh Bloch
 * @see java.util.Map
 * @see TreeMap
 * @see SortedSet
 * @see Comparator
 * @see Comparable
 * @see java.util.Collection
 * @see ClassCastException
 * @since 1.2
 */

public interface SortedMap<K,V> extends java.util.Map<K,V> {
    /**
     * 返回 Map 中对键排序的比较器，如果使用自然顺序排序，返回 null
     *
     * @return the comparator used to order the keys in this map,
     *         or {@code null} if this map uses the natural ordering
     *         of its keys
     */
    Comparator<? super K> comparator();

    /**
     * 返回包含该 Map 部分键值对的视图，范围从 fromKey 到 toKey。（如果
     * fromKey 和 toKey 相等，返回 null）。返回的 Map 由此 Map 支撑，任何
     * 对返回 Map 的修改都会影响此 Map，反之亦然。返回的 Map 支持此 Map
     * 的所有操作。
     *
     * 如果试图在返回 Map 的范围之外插入 key，将会抛出 IllegalArgumentException 异常。
     *
     * @param fromKey low endpoint (inclusive) of the keys in the returned map
     * @param toKey high endpoint (exclusive) of the keys in the returned map
     * @return a view of the portion of this map whose keys range from
     *         {@code fromKey}, inclusive, to {@code toKey}, exclusive
     * @throws ClassCastException if {@code fromKey} and {@code toKey}
     *         cannot be compared to one another using this map's comparator
     *         (or, if the map has no comparator, using natural ordering).
     *         Implementations may, but are not required to, throw this
     *         exception if {@code fromKey} or {@code toKey}
     *         cannot be compared to keys currently in the map.
     * @throws NullPointerException if {@code fromKey} or {@code toKey}
     *         is null and this map does not permit null keys
     * @throws IllegalArgumentException if {@code fromKey} is greater than
     *         {@code toKey}; or if this map itself has a restricted
     *         range, and {@code fromKey} or {@code toKey} lies
     *         outside the bounds of the range
     */
    java.util.SortedMap<K,V> subMap(K fromKey, K toKey);

    /**
     * 返回该映射中 key 严格小于 toKey 的部分。返回的 Map 由此 Map 支撑，
     * 任何对返回 Map 的修改都会影响此 Map，反之亦然。返回的 Map 支持
     * 此 Map 的所有操作。
     *
     * 如果试图在返回 Map 的范围之外插入 key，将会抛出 IllegalArgumentException 异常。
     *
     * @param toKey high endpoint (exclusive) of the keys in the returned map
     * @return a view of the portion of this map whose keys are strictly
     *         less than {@code toKey}
     * @throws ClassCastException if {@code toKey} is not compatible
     *         with this map's comparator (or, if the map has no comparator,
     *         if {@code toKey} does not implement {@link Comparable}).
     *         Implementations may, but are not required to, throw this
     *         exception if {@code toKey} cannot be compared to keys
     *         currently in the map.
     * @throws NullPointerException if {@code toKey} is null and
     *         this map does not permit null keys
     * @throws IllegalArgumentException if this map itself has a
     *         restricted range, and {@code toKey} lies outside the
     *         bounds of the range
     */
    java.util.SortedMap<K,V> headMap(K toKey);

    /**
     * 返回该映射中 key 大于等于 fromKey 的部分。返回的 Map 由此 Map 支撑，
     * 任何对返回 Map 的修改都会影响此 Map，反之亦然。返回的 Map 支持
     * 此 Map 的所有操作。
     *
     * 如果试图在返回 Map 的范围之外插入 key，将会抛出 IllegalArgumentException 异常。
     *
     * @param fromKey low endpoint (inclusive) of the keys in the returned map
     * @return a view of the portion of this map whose keys are greater
     *         than or equal to {@code fromKey}
     * @throws ClassCastException if {@code fromKey} is not compatible
     *         with this map's comparator (or, if the map has no comparator,
     *         if {@code fromKey} does not implement {@link Comparable}).
     *         Implementations may, but are not required to, throw this
     *         exception if {@code fromKey} cannot be compared to keys
     *         currently in the map.
     * @throws NullPointerException if {@code fromKey} is null and
     *         this map does not permit null keys
     * @throws IllegalArgumentException if this map itself has a
     *         restricted range, and {@code fromKey} lies outside the
     *         bounds of the range
     */
    java.util.SortedMap<K,V> tailMap(K fromKey);

    /**
     * 返回此时 Map 中第一个（最小的）key。
     *
     * @return the first (lowest) key currently in this map
     * @throws NoSuchElementException if this map is empty
     */
    K firstKey();

    /**
     * 返回此时 Map 中最后一个（最大的）key。
     *
     * @return the last (highest) key currently in this map
     * @throws NoSuchElementException if this map is empty
     */
    K lastKey();

    /**
     * 返回包含 Map 中所有 key 的 Set 集合视图。
     * 集合的迭代器按升序返回。
     * 返回的集合由此 Map 支撑，任何对此 Map 的修改都会影响到集合，
     * 反之亦然。对集合的迭代过程中如果此 Map 被修改
     * （除了通过迭代器自身的 remove 方法），迭代的结果未定义。此集合
     * 支持元素的删除，通过 Iterator.remove, Set.remove, removeAll,
     * retainAll, clear 方法删除 Map 中对应的映射。集合不支持 add 或者 addAll
     * 操作。
     *
     * @return a set view of the keys contained in this map, sorted in
     *         ascending order
     */
    java.util.Set<K> keySet();

    /**
     * 返回包含 Map 中所有 value 的 Collection 集合视图。
     * 集合的迭代器按对应 key 的升序返回 value 值。
     * 返回的集合由此 Map 支撑，任何对此 Map 的修改都会影响到集合，
     * 反之亦然。对集合的迭代过程中如果此 Map 被修改
     * （除了通过迭代器自身的 remove 方法），迭代的结果未定义。此集合
     * 支持元素的删除，通过 Iterator.remove, Set.remove, removeAll,
     * retainAll, clear 方法删除 Map 中对应的映射。集合不支持 add 或者 addAll
     * 操作。
     *
     * @return a collection view of the values contained in this map,
     *         sorted in ascending key order
     */
    Collection<V> values();

    /**
     * 返回包含 Map 中所有映射的 Set 集合视图。
     * 集合的迭代器按对应 key 的升序返回 entry。
     * 返回的集合由此 Map 支撑，任何对此 Map 的修改都会影响到集合，
     * 反之亦然。对集合的迭代过程中如果此 Map 被修改
     * （除了通过迭代器自身的 remove 方法或者 setValue 方法），迭代的
     * 结果未定义。此集合支持元素的删除，通过
     * Iterator.remove, Set.remove, removeAll, retainAll, clear 方法删除
     * Map 中对应的映射。集合不支持 add 或者 addAll 操作。
     *
     * @return a set view of the mappings contained in this map,
     *         sorted in ascending key order
     */
    Set<Entry<K, V>> entrySet();
}