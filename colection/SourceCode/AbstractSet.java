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

package Collections;

import java.util.Iterator;
import java.util.Objects;

/**
 * 此类提供 Set 接口的基本实现，以最小化实现该接口所需的工作。
 *
 * 通过扩展这个类来实现 Set 的过程等同于通过扩展 AbstractCollection
 * 来实现 Collection 的过程，除了此类的子类中的所有方法和构造函数都
 * 必须遵守 Set 接口施加的约束之外。（例如，add 方法不允许在集合中
 * 添加 object的多个实例）
 *
 * 注意这个类不会覆盖 AbstractCollection 类中的任何实现。它仅仅添加了
 * equals 和 hashCode 的实现。
 *
 * 此类是 Java Collections Framework 的成员。
 *
 * @param <E> the type of elements maintained by this set
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see Collection
 * @see AbstractCollection
 * @see Set
 * @since 1.2
 */

public abstract class AbstractSet<E> extends java.util.AbstractCollection<E> implements Set<E> {
    /**
     * 唯一的构造函数。（用于子类构造函数调用，通常是隐式的。）
     */
    protected AbstractSet() {
    }

    // Comparison and hashing
    // 比较和 hash 操作

    /**
     * 比较指定对象和此 set 是否相等。如果给定的对象也是一个 set，两个
     * 集合大小相同，且给定集合包含在此集合内，则返回 true。这一点确保
     * 了两个不同的 Set 接口的实现也能使用 equals 函数。
     *
     * 这个实现首先检查指定的对象是不是此集合，如果是返回 true。然后
     * 检查指定对象是否是一个和此集合大小相等的 set，如果不是返回 false。
     * 如果是的话，返回 containsAll((Collection)o) 的值。
     *
     * @param o object to be compared for equality with this set
     * @return true if the specified object is equal to this set
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Set))
            return false;
        java.util.Collection<?> c = (java.util.Collection<?>) o;
        if (c.size() != size())
            return false;
        try {
            return containsAll(c);
        } catch (ClassCastException unused)   {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
    }

    /**
     * 返回此集合的 hash 值。集合的 hash 值定义为集合元素的 hash 值之和，
     * 其中 null 元素的 hash 值定义为零。
     *
     * 此实现遍历集合，调用 hashCode 方法获取集合元素的 hash 值，然后
     * 相加得到结果。
     *
     * @return the hash code value for this set
     * @see Object#equals(Object)
     * @see Set#equals(Object)
     */
    public int hashCode() {
        int h = 0;
        Iterator<E> i = iterator();
        while (i.hasNext()) {
            E obj = i.next();
            if (obj != null)
                h += obj.hashCode();
        }
        return h;
    }

    /**
     * 从此 set 的所有元素中移除指定集合包含的元素（可选操作）。如果指定
     * 集合也是一个 set，此操作将有效地改变这个集合，从而让它的值成为两个
     * 集合的非对称集的差。
     *
     * 这个实现通过调用 size 方法来确定这个集合和指定集合哪个更小。如果此
     * 集合更小，那么对此集合进行迭代，检查迭代器返回的每一个元素是否包含
     * 在指定集合里面。如果包含，那么使用迭代器的 remove 方法将其从集合中
     * 删除。如果指定集合更小，那么此方法遍历指定集合，使用集合的 remove
     * 方法，从此集合中删除迭代器返回的元素。
     *
     * 注意如果迭代器不支持 remove 方法，此实现会抛出
     * UnsupportedOperationException 异常。
     *
     * @param  c collection containing elements to be removed from this set
     * @return <tt>true</tt> if this set changed as a result of the call
     * @throws UnsupportedOperationException if the <tt>removeAll</tt> operation
     *         is not supported by this set
     * @throws ClassCastException if the class of an element of this set
     *         is incompatible with the specified collection
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this set contains a null element and the
     *         specified collection does not permit null elements
     * (<a href="Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;

        // 此集合大于指定集合，使用此集合的 remove 方法删除
        if (size() > c.size()) {
            for (Iterator<?> i = c.iterator(); i.hasNext(); )
                modified |= remove(i.next());
        } else {
            // 此集合小于等于指定集合，使用指定集合迭代器的 remove 方法删除
            for (Iterator<?> i = iterator(); i.hasNext(); ) {
                if (c.contains(i.next())) {
                    i.remove();
                    modified = true;
                }
            }
        }
        return modified;
    }

}