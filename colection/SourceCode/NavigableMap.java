package Collections;

/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *

 *
 * Written by Doug Lea and Josh Bloch with assistance from members of JCP
 * JSR-166 Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedMap;

/**
 * 扩展了 SortedMap 的接口，包括为给性检索目标返回最接近的匹配的方法。
 * lowerEntry, floorEntry, ceilingEntry 和 higherEntry 分别返回小于，小于或等于，
 * 大于或等于，大于指定 key 的 entry，如果没有这样的 key 则返回 null。
 * 同样的， lowerKey, floorKey, ceilingKey 和 higherKey 只返回相关的 key。
 * 所有的这些方法都是为定位设计，而非遍历。
 *
 * 可按升序或者降序访问和遍历一个 NavigableMap。descendingMap 方法
 * 返回映射的一个视图，该视图包括所有反向的相关和定向方法。升序的操作
 * 和视图可能比降序的要快。subMap, headMap, 和 tailMap 方法和名称比较
 * 类似的 SortedMap 不同，它们接受额外的参数来描述上界和下界是包含的
 * 还是排他的。任何 NavigableMap 的子映射都必须实现 NavigableMap 接口。
 *
 * 这一接口定义了 firstEntry, pollFirstEntry, lastEntry 和 pollLastEntry，
 * 用来返回最小的和最大的映射，如果不存在则返回 null。
 *
 * 此接口是 Java Collections Framework 的成员。
 *
 * @author Doug Lea
 * @author Josh Bloch
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @since 1.6
 */
public interface NavigableMap<K,V> extends java.util.SortedMap<K,V> {
    /**
     * 返回小于给定 key 的最大的 key 所对应的的键值对映射，如果不存在返回 null。
     *
     * @param key the key
     * @return an entry with the greatest key less than {@code key},
     *         or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    java.util.Map.Entry<K,V> lowerEntry(K key);

    /**
     * 返回小于给定 key 的最大的 key，如果不存在返回 null。
     *
     * @param key the key
     * @return the greatest key less than {@code key},
     *         or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    K lowerKey(K key);

    /**
     * 返回小于等于给定 key 的最大的 key 所对应的的键值对映射，如果不存在返回 null。
     *
     * @param key the key
     * @return an entry with the greatest key less than or equal to
     *         {@code key}, or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    java.util.Map.Entry<K,V> floorEntry(K key);

    /**
     * 返回小于等于给定 key 的最大的 key，如果不存在返回 null。
     *
     * @param key the key
     * @return the greatest key less than or equal to {@code key},
     *         or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    K floorKey(K key);

    /**
     * 返回大于等于给定 key 的最大的 key 所对应的的键值对映射，如果不存在返回 null。
     *
     * @param key the key
     * @return an entry with the least key greater than or equal to
     *         {@code key}, or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    java.util.Map.Entry<K,V> ceilingEntry(K key);

    /**
     * 返回大于等于给定 key 的最大的 key，如果不存在返回 null。
     *
     * @param key the key
     * @return the least key greater than or equal to {@code key},
     *         or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    K ceilingKey(K key);

    /**
     * 返回大于给定 key 的最小的 key 所对应的的键值对映射，如果不存在返回 null。
     *
     * @param key the key
     * @return an entry with the least key greater than {@code key},
     *         or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    java.util.Map.Entry<K,V> higherEntry(K key);

    /**
     * 返回大于给定 key 的最大的 key，如果不存在返回 null。
     *
     * @param key the key
     * @return the least key greater than {@code key},
     *         or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    K higherKey(K key);

    /**
     * 返回此 Map 中最小的 key 对应的键值对，如果 Map 为空返回 null。
     *
     * @return an entry with the least key,
     *         or {@code null} if this map is empty
     */
    java.util.Map.Entry<K,V> firstEntry();

    /**
     * 返回此 Map 中最大的 key 对应的键值对，如果 Map 为空返回 null。
     *
     * @return an entry with the greatest key,
     *         or {@code null} if this map is empty
     */
    java.util.Map.Entry<K,V> lastEntry();

    /**
     * 删除并返回此 Map 中最小的 key 对应的键值对，如果 Map 为空返回 null。
     *
     * @return the removed first entry of this map,
     *         or {@code null} if this map is empty
     */
    java.util.Map.Entry<K,V> pollFirstEntry();

    /**
     * 删除并返回此 Map 中最大的 key 对应的键值对，如果 Map 为空返回 null。
     *
     * @return the removed last entry of this map,
     *         or {@code null} if this map is empty
     */
    Map.Entry<K,V> pollLastEntry();

    /**
     * 返回此映射中包含映射的逆序视图。返回的 Map 由此 Map 支撑，所以任何
     * 对此 Map 的改变都会反映到返回的 Map 中，反之亦然。如果这两者中的
     * 任何一个在迭代过程中被修改（除非是迭代器自己的 remove 操作），迭代
     * 的结果未定义。
     *
     * 返回 Map 中的顺序和 Collections.reverseOrder 中的顺序一样。
     * m.descendingMap().descendingMap() 表达式的结果和原来的 m 完全相等。
     *
     * @return a reverse order view of this map
     */
    java.util.NavigableMap<K,V> descendingMap();

    /**
     * 返回包含此 Map 中所有 key 的 NavigableSet 视图。返回集合的迭代器的
     * 顺序为升序。返回的集合由此 Map 支撑，所以对此 Map 的任何改变都会影响
     * 返回的集合，反之亦然。如果对返回集合的迭代过程中此 Map 被修改（除非
     * 通过迭代器自身的 remove 操作），迭代的结果不确定。返回的集合提供
     * 删除操作，同时也会删除 Map 中对应的映射，通过函数 Iterator.remove,
     * Set.remove, removeAll, retainAll, 和 clear 实现。不支持 add 和 addAll 操作。
     *
     * @return a navigable set view of the keys in this map
     */
    java.util.NavigableSet<K> navigableKeySet();

    /**
     * 返回包含此 Map 中所有 key 的逆序 NavigableSet 视图。返回集合的迭代器的
     * 顺序为逆序。返回的集合由此 Map 支撑，所以对此 Map 的任何改变都会影响
     * 返回的集合，反之亦然。如果对返回集合的迭代过程中此 Map 被修改（除非
     * 通过迭代器自身的 remove 操作），迭代的结果不确定。返回的集合提供
     * 删除操作，同时也会删除 Map 中对应的映射，通过函数 Iterator.remove,
     * Set.remove, removeAll, retainAll, 和 clear 实现。不支持 add 和 addAll 操作。
     *
     * @return a reverse order navigable set view of the keys in this map
     */
    NavigableSet<K> descendingKeySet();

    /**
     * 返回包括此 Map 部分键值对的子 Map 视图，key 的范围从 fromKey 到 toKey。
     * 如果 fromKey 和 toKey 相等，则返回的 Map 为空，除非 fromInclusive
     * 和 toInclusive 都为 true。返回的 Map 由此 Map 支撑，所以返回 Map 的
     * 任何改变都会影响此 Map，反之亦然。返回的 Map 支持此 Map 支持的所有操作。
     *
     * 如果试图在返回的 Map 包含的范围之外插入键值对，返回的 Map 将会抛出
     * IllegalArgumentException 异常。
     *
     * @param fromKey low endpoint of the keys in the returned map
     * @param fromInclusive {@code true} if the low endpoint
     *        is to be included in the returned view
     * @param toKey high endpoint of the keys in the returned map
     * @param toInclusive {@code true} if the high endpoint
     *        is to be included in the returned view
     * @return a view of the portion of this map whose keys range from
     *         {@code fromKey} to {@code toKey}
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
    java.util.NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                       K toKey, boolean toInclusive);

    /**
     * 返回包含此 Map 部分键值对的视图，这些键值对的 key 小于（如果 inclusive
     * 为 true 则为小于等于）toKey。返回的 Map 由此 Map 支撑，所以返回 Map
     * 的任何改变都会影响此 Map，反之亦然。返回的 Map 支持所有此 Map 支持
     * 的操作。
     *
     * 如果试图在范围之外插入新的键值对，返回的 Map 将会抛出
     * IllegalArgumentException 异常。
     *
     * @param toKey high endpoint of the keys in the returned map
     * @param inclusive {@code true} if the high endpoint
     *        is to be included in the returned view
     * @return a view of the portion of this map whose keys are less than
     *         (or equal to, if {@code inclusive} is true) {@code toKey}
     * @throws ClassCastException if {@code toKey} is not compatible
     *         with this map's comparator (or, if the map has no comparator,
     *         if {@code toKey} does not implement {@link Comparable}).
     *         Implementations may, but are not required to, throw this
     *         exception if {@code toKey} cannot be compared to keys
     *         currently in the map.
     * @throws NullPointerException if {@code toKey} is null
     *         and this map does not permit null keys
     * @throws IllegalArgumentException if this map itself has a
     *         restricted range, and {@code toKey} lies outside the
     *         bounds of the range
     */
    java.util.NavigableMap<K,V> headMap(K toKey, boolean inclusive);

    /**
     * 返回包含此 Map 部分键值对的视图，这些键值对的 key 大于（如果 inclusive
     * 为 true 则为大于等于）fromKey。返回的 Map 由此 Map 支撑，所以返回 Map
     * 的任何改变都会影响此 Map，反之亦然。返回的 Map 支持所有此 Map 支持
     * 的操作。
     *
     * 如果试图在范围之外插入新的键值对，返回的 Map 将会抛出
     * IllegalArgumentException 异常。
     *
     * @param fromKey low endpoint of the keys in the returned map
     * @param inclusive {@code true} if the low endpoint
     *        is to be included in the returned view
     * @return a view of the portion of this map whose keys are greater than
     *         (or equal to, if {@code inclusive} is true) {@code fromKey}
     * @throws ClassCastException if {@code fromKey} is not compatible
     *         with this map's comparator (or, if the map has no comparator,
     *         if {@code fromKey} does not implement {@link Comparable}).
     *         Implementations may, but are not required to, throw this
     *         exception if {@code fromKey} cannot be compared to keys
     *         currently in the map.
     * @throws NullPointerException if {@code fromKey} is null
     *         and this map does not permit null keys
     * @throws IllegalArgumentException if this map itself has a
     *         restricted range, and {@code fromKey} lies outside the
     *         bounds of the range
     */
    java.util.NavigableMap<K,V> tailMap(K fromKey, boolean inclusive);

    /**
     * {@inheritDoc}
     *
     * <p>Equivalent to {@code subMap(fromKey, true, toKey, false)}.
     *
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    java.util.SortedMap<K,V> subMap(K fromKey, K toKey);

    /**
     * {@inheritDoc}
     *
     * <p>Equivalent to {@code headMap(toKey, false)}.
     *
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    java.util.SortedMap<K,V> headMap(K toKey);

    /**
     * {@inheritDoc}
     *
     * <p>Equivalent to {@code tailMap(fromKey, true)}.
     *
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    SortedMap<K,V> tailMap(K fromKey);
}
