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

package java.util;

import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The root interface in the <i>collection hierarchy</i>.  A collection
 * represents a group of objects, known as its <i>elements</i>.  Some
 * collections allow duplicate elements and others do not.  Some are ordered
 * and others unordered.  The JDK does not provide any <i>direct</i>
 * implementations of this interface: it provides implementations of more
 * specific subinterfaces like <tt>Set</tt> and <tt>List</tt>.  This interface
 * is typically used to pass collections around and manipulate them where
 * maximum generality is desired.
 * collection集合层次中的基础接口。一个collection表示一组对象，每个对象称为
 * 元素。一些collection允许重复元素，一些不允许。一些是有序的，一些是无序
 * 的。Java JDK不提供这个接口的直接实现：只提供具体的子接口的实现，例如
 *  Set和List。这个接口用于在需要最大通用性的地方传递集合并操作它们。
 *
 * <p><i>Bags</i> or <i>multisets</i> (unordered collections that may contain
 * duplicate elements) should implement this interface directly.
 * Bags或者Multisets（一种可能包含重复元素的无序集合）必须直接实现这个接口。
 *
 * <p>All general-purpose <tt>Collection</tt> implementation classes (which
 * typically implement <tt>Collection</tt> indirectly through one of its
 * subinterfaces) should provide two "standard" constructors: a void (no
 * arguments) constructor, which creates an empty collection, and a
 * constructor with a single argument of type <tt>Collection</tt>, which
 * creates a new collection with the same elements as its argument.  In
 * effect, the latter constructor allows the user to copy any collection,
 * producing an equivalent collection of the desired implementation type.
 * There is no way to enforce this convention (as interfaces cannot contain
 * constructors) but all of the general-purpose <tt>Collection</tt>
 * implementations in the Java platform libraries comply.
 *
 * 所有通用集合实现类（通过Collection的一个子接口间接实现该接口的类）应该
 * 至少提供两个标准构造函数：一个无参构造函数，和只有一个Collection类型参数的
 * 构造函数，其创造了一个和参数同类型的新的Collection。实际上，后一个构造
 * 函数允许用户复制任何集合，来生成所要实现类型的等效集合。没办法强制执行
 * 这种约定（因为接口不能包括构造函数），但是Java平台库中所有通用集合的
 * 实现都遵循这项原则。
 *
 * <p>The "destructive" methods contained in this interface, that is, the
 * methods that modify the collection on which they operate, are specified to
 * throw <tt>UnsupportedOperationException</tt> if this collection does not
 * support the operation.  If this is the case, these methods may, but are not
 * required to, throw an <tt>UnsupportedOperationException</tt> if the
 * invocation would have no effect on the collection.  For example, invoking
 * the {@link #addAll(Collection)} method on an unmodifiable collection may,
 * but is not required to, throw the exception if the collection to be added
 * is empty.
 * 这个接口包含破坏性的方法，即修改它们所操作的集合，若这个集合不支持这个
 * 操作，会抛出UnsupportedOperationException异常。在这种情况下，如果调用
 *  对集合没有影响，这些方法可以，但不是必须这样做。例如，在不可修改的集合
 * 里调用addAll(java.util.Collection)方法，如果要添加的集合为空，那么可以抛出
 *  异常但不一定必须这么做。
 *
 * <p><a name="optional-restrictions">
 * Some collection implementations have restrictions on the elements that
 * they may contain.</a>  For example, some implementations prohibit null elements,
 * and some have restrictions on the types of their elements.  Attempting to
 * add an ineligible element throws an unchecked exception, typically
 * <tt>NullPointerException</tt> or <tt>ClassCastException</tt>.  Attempting
 * to query the presence of an ineligible element may throw an exception,
 * or it may simply return false; some implementations will exhibit the former
 * behavior and some will exhibit the latter.  More generally, attempting an
 * operation on an ineligible element whose completion would not result in
 * the insertion of an ineligible element into the collection may throw an
 * exception or it may succeed, at the option of the implementation.
 * Such exceptions are marked as "optional" in the specification for this
 * interface.
 * 一些集合的实现对它们可能包含的元素有限制。比如一些实现禁止null元素，一些
 * 对元素类型有限制。试图添加非法元素会抛出未检查的异常，特别是
 * NullPointerException 或者 ClassCastException。尝试查找非法元素会抛出
 *异常，或者直接返回false；一些实现会显示前一种，一些会显示后一种。更一般
 * 的情况下，尝试对非法元素进行操作，其实现过程不会导致将非法元素插入到
 * 集合中，可以选择抛出异常或者操作成功，这取决于它具体的实现。在接口中，
 * 这些异常被标记为可选。
 *
 * <p>It is up to each collection to determine its own synchronization
 * policy.  In the absence of a stronger guarantee by the
 * implementation, undefined behavior may result from the invocation
 * of any method on a collection that is being mutated by another
 * thread; this includes direct invocations, passing the collection to
 * a method that might perform invocations, and using an existing
 * iterator to examine the collection.
 * 每一个集合决定它自己的同步策略。在集合的实现上，缺乏更强的保证时，未知
 * 的行为可能来自于另一线程对该集合某一方法的突然调用，其中包括直接调用，
 * 将集合传递给被调用的函数，或者是使用已有迭代器检查集合。
 *
 * <p>Many methods in Collections Framework interfaces are defined in
 * terms of the {@link Object#equals(Object) equals} method.  For example,
 * the specification for the {@link #contains(Object) contains(Object o)}
 * method says: "returns <tt>true</tt> if and only if this collection
 * contains at least one element <tt>e</tt> such that
 * <tt>(o==null ? e==null : o.equals(e))</tt>."  This specification should
 * <i>not</i> be construed to imply that invoking <tt>Collection.contains</tt>
 * with a non-null argument <tt>o</tt> will cause <tt>o.equals(e)</tt> to be
 * invoked for any element <tt>e</tt>.  Implementations are free to implement
 * optimizations whereby the <tt>equals</tt> invocation is avoided, for
 * example, by first comparing the hash codes of the two elements.  (The
 * {@link Object#hashCode()} specification guarantees that two objects with
 * unequal hash codes cannot be equal.)  More generally, implementations of
 * the various Collections Framework interfaces are free to take advantage of
 * the specified behavior of underlying {@link Object} methods wherever the
 * implementor deems it appropriate.
 *Collection Framework接口中的许多方法都是根据
 * Object.equals(Object) 方法定义的。比如 contains(Object o)方法的规范中
 * 说“当且仅当这个集合包括一个元素e，且(o==null ? e==null : o.equals(e))，
 * 返回true"。这一规范不应该被解释成对于非null元素o，使用
 * Collection.contains时都会调用o.equals(e)。该函数可以自由地实现和优化，
 * 从而避免调用equal，例如，首先比较两个元素的hash值。Object.hashCode()
 * 的规范保证了hash值不相等的两个元素不可能相等。更一般地说，各种
 *  Collections Framework接口的实现都可以在实现者认为合适的地方自由地利用
 *  底层 Object 的方法的合法行为。
 *
 * <p>Some collection operations which perform recursive traversal of the
 * collection may fail with an exception for self-referential instances where
 * the collection directly or indirectly contains itself. This includes the
 * {@code clone()}, {@code equals()}, {@code hashCode()} and {@code toString()}
 * methods. Implementations may optionally handle the self-referential scenario,
 * however most current implementations do not do so.
 *一些执行递归遍历的操作可能会失败，因为直接或间接包含了自身的引用。这
 *包括clone()，equals()，hashCode()， toString()。在实现中可以选择处理
 * 这些自引用场景，但是大多数实现方式不这样做。
 *
 * <p>This interface is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 * 本接口是Java Collections Framework的一个成员。
 *
 * @implSpec
 * The default method implementations (inherited or otherwise) do not apply any
 * synchronization protocol.  If a {@code Collection} implementation has a
 * specific synchronization protocol, then it must override default
 * implementations to apply that protocol.
 *默认方法实现（继承或其他）不应用任何同步协议。如果一个Collection的
 * 实现有特定的同步协议，那么它必须覆盖默认的实现来应用这项协议。
 * @param <E> the type of elements in this collection
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see     Set
 * @see     List
 * @see     Map
 * @see     SortedSet
 * @see     SortedMap
 * @see     HashSet
 * @see     TreeSet
 * @see     ArrayList
 * @see     LinkedList
 * @see     Vector
 * @see     Collections
 * @see     Arrays
 * @see     AbstractCollection
 * @since 1.2
 */

public interface Collection<E> extends Iterable<E> {
    // Query Operations

    /**
     * Returns the number of elements in this collection.  If this collection
     * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * 返回集合元素个数。如果这个集合元素个数超过Integer.MAX_VALUE，
     * 那么返回Integer.MAX_VALUE。
     * @return the number of elements in this collection
     */
    int size();    //返回集合元素的数量

    /**
     * Returns <tt>true</tt> if this collection contains no elements.
     *
     * @return <tt>true</tt> if this collection contains no elements
     */
    boolean isEmpty();//若集合为空返回true

    /**
     * Returns <tt>true</tt> if this collection contains the specified element.
     * More formally, returns <tt>true</tt> if and only if this collection
     * contains at least one element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *如果集合中包括元素o，返回true。更一般地说，如果集合包括至少一个
     * 元素e满足(o==null?e==null:o.equals(e))，返回true。
     *
     * @param o element whose presence in this collection is to be tested
     * @return <tt>true</tt> if this collection contains the specified
     *         element
     * @throws ClassCastException if the type of the specified element
     *         is incompatible with this collection          //抛出类型错误异常
     *         (<a href="#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *         collection does not permit null elements   //抛出空指针错误
     *         (<a href="#optional-restrictions">optional</a>)
     */
    boolean contains(Object o);

    /**
     * Returns an iterator over the elements in this collection.  There are no
     * guarantees concerning the order in which the elements are returned
     * (unless this collection is an instance of some class that provides a
     * guarantee).
     *返回集合元素的迭代器。不保证集合元素按顺序返回。（除非集合的实例
     * 提供了这项功能。
     * @return an <tt>Iterator</tt> over the elements in this collection
     */
    Iterator<E> iterator();

    /**
     * Returns an array containing all of the elements in this collection.
     * If this collection makes any guarantees as to what order its elements
     * are returned by its iterator, this method must return the elements in
     * the same order.
     *返回一个包括集合所有元素的数组。
     *  如果这个集合保证以迭代器的顺序返回的话，那么这个方法以该顺序返回。
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this collection.  (In other words, this method must
     * allocate a new array even if this collection is backed by an array).
     * The caller is thus free to modify the returned array.
     *返回的数组是安全的，因为这个集合不会维持对它的引用。（换句话说，
     * 即使集合有数组的支持，这个方法也必须分配一个新的数组空间。）因此，
     * 调用者可以自由地修改返回的数组。
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @Saber-01
     * 注意：只能返回Object类型的数组。且每一次返回的数组都是新建的，
     *只是内容一样而已。注意：如果list.toArray()返回的数组中存放的是
     * ist原始对象的引用，只是创建了一个新的数组来装这些引用，并没有
     * 对list中原始对象进行拷贝或复制。
     *
     * @return an array containing all of the elements in this collection
     */
    Object[] toArray();

    /**
     * Returns an array containing all of the elements in this collection;
     * the runtime type of the returned array is that of the specified array.
     * If the collection fits in the specified array, it is returned therein.
     * Otherwise, a new array is allocated with the runtime type of the
     * specified array and the size of this collection.
     * 返回一个包括集合所有元素的数组。
     * 返回数组的运行时类型是指定数组的运行时类型。如果集合和指定数组
     * 相符，则返回其中的集合。否则，根据指定数组的运行时类型和该集合的
     *  大小分配一个新数组。
     *
     * <p>If this collection fits in the specified array with room to spare
     * (i.e., the array has more elements than this collection), the element
     * in the array immediately following the end of the collection is set to
     * <tt>null</tt>.  (This is useful in determining the length of this
     * collection <i>only</i> if the caller knows that this collection does
     * not contain any <tt>null</tt> elements.)
     *如果指定数组能装下该集合，即数组中的元素个数多于此集合，那么数组
     * 跟在集合末尾的元素设置为null。（只有当调用者知道集合不包括任何
     * null元素时，才有助于确定该集合长度）
     *
     * <p>If this collection makes any guarantees as to what order its elements
     * are returned by its iterator, this method must return the elements in
     * the same order.
     *如果这个集合保证以迭代器的顺序返回的话，那么这个方法以该顺序返回。
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *和toArray()方法类似，这个方法充当了array-base和collection-base的
     *连接桥梁。此外，这种方法允许对输出数组的运行时类型精确控制（必须
     *是list中元素类型的父类或本身），在某些情况下，还可以用来节省分配
     *成本。
     *
     * <p>Suppose <tt>x</tt> is a collection known to contain only strings.
     * The following code can be used to dump the collection into a newly
     * allocated array of <tt>String</tt>:
     *
     * <pre>
     *     String[] y = x.toArray(new String[0]);</pre>
     *
     * Note that <tt>toArray(new Object[0])</tt> is identical in function to
     * <tt>toArray()</tt>.
     *
     * @param <T> the runtime type of the array to contain the collection
     * @param a the array into which the elements of this collection are to be
     *        stored, if it is big enough; otherwise, a new array of the same
     *        runtime type is allocated for this purpose.
     * @return an array containing all of the elements in this collection
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this collection
     * @throws NullPointerException if the specified array is null
     *
     * 注意：是否创建新的数组取决于a.length
     */
    <T> T[] toArray(T[] a);

    // Modification Operations

    /**
     * Ensures that this collection contains the specified element (optional
     * operation).  Returns <tt>true</tt> if this collection changed as a
     * result of the call.  (Returns <tt>false</tt> if this collection does
     * not permit duplicates and already contains the specified element.)<p>
     *确保此集合包含指定的元素（可选操作）。如果此集合因调用改变了，即调用
     *成功，则返回true。（如果这个集合不允许重复，并且已经包含指定的
     * 元素，则返回false。）
     * Collections that support this operation may place limitations on what
     * elements may be added to this collection.  In particular, some
     * collections will refuse to add <tt>null</tt> elements, and others will
     * impose restrictions on the type of elements that may be added.
     * Collection classes should clearly specify in their documentation any
     * restrictions on what elements may be added.<p>
     *支持此操作的集合可能会限制像集合中添加哪些元素。特别是，一些集合
     * 将拒绝添加null元素，而其他集合将对可能添加的元素类型施加限制。
     * 集合类应该在其文档中明确指定可以添加哪些元素的任何限制。
     * If a collection refuses to add a particular element for any reason
     * other than that it already contains the element, it <i>must</i> throw
     * an exception (rather than returning <tt>false</tt>).  This preserves
     * the invariant that a collection always contains the specified element
     * after this call returns.
     *如果集合拒绝添加一个特定的元素，除了它已经包含该元素这，其他情况下
     * 它必须抛出一个异常（而不是返回false）。
     *
     * @param e element whose presence in this collection is to be ensured
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call
     * @throws UnsupportedOperationException if the <tt>add</tt> operation
     *         is not supported by this collection   不支持添加操作的集合会抛出此异常。
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this collection   类型不兼容。
     * @throws NullPointerException if the specified element is null and this
     *         collection does not permit null elements
     *         空指针异常，被添加的元素为null，而当前集合却不允许值为null。
     * @throws IllegalArgumentException if some property of the element
     *         prevents it from being added to this collection
     *         非法元素。如果元素的某些属性阻止将其添加到此集合中
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to insertion restrictions
     *         不允许添加。如果此时由于插入限制而无法添加元素
     */
    boolean add(E e);

    /**
     * Removes a single instance of the specified element from this
     * collection, if it is present (optional operation).  More formally,
     * removes an element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>, if
     * this collection contains one or more such elements.  Returns
     * <tt>true</tt> if this collection contained the specified element (or
     * equivalently, if this collection changed as a result of the call).
     *如果指定元素存在，则从该集合中移除该元素的单个实例（可选操作）。
     * 更正式地说，如果这个集合包含一个或多个这样的元素，则删除一个
     *  元素e (o==null?e==null:o.equals(e))。如果此集合包含指定的元素，
     *  则返回true（如果此集合由于调用而更改，则返回true）。
     * @param o element to be removed from this collection, if present
     * @return <tt>true</tt> if an element was removed as a result of this call
     * @throws ClassCastException if the type of the specified element
     *         is incompatible with this collection
     *         (<a href="#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *         collection does not permit null elements
     *         (<a href="#optional-restrictions">optional</a>)
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation
     *         is not supported by this collection
     */
    boolean remove(Object o);


    // Bulk Operations
  //批量操作
    /**
     * Returns <tt>true</tt> if this collection contains all of the elements
     * in the specified collection.
     * 如果此集合包含指定集合中的所有元素，则返回true
     *
     * @param  c collection to be checked for containment in this collection
     * @return <tt>true</tt> if this collection contains all of the elements
     *         in the specified collection
     * @throws ClassCastException if the types of one or more elements
     *         in the specified collection are incompatible with this
     *         collection
     *         (<a href="#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified collection contains one
     *         or more null elements and this collection does not permit null
     *         elements
     *         (<a href="#optional-restrictions">optional</a>),
     *         or if the specified collection is null.
     * @see    #contains(Object)
     */
    boolean containsAll(Collection<?> c);

    /**
     * Adds all of the elements in the specified collection to this collection
     * (optional operation).  The behavior of this operation is undefined if
     * the specified collection is modified while the operation is in progress.
     * (This implies that the behavior of this call is undefined if the
     * specified collection is this collection, and this collection is
     * nonempty.)
     * 将指定集合中的所有元素添加到此集合（可选操作）。如果在操作进行期间
     * 修改了指定的集合，则此操作的行为未定义。（这意味着，如果指定的
     * 集合是这个集合，并且这个集合不是空的，则此调用的行为是未定义的。）
     *
     * @param c collection containing elements to be added to this collection
     * @return <tt>true</tt> if this collection changed as a result of the call
     * @throws UnsupportedOperationException if the <tt>addAll</tt> operation
     *         is not supported by this collection
     * @throws ClassCastException if the class of an element of the specified
     *         collection prevents it from being added to this collection
     * @throws NullPointerException if the specified collection contains a
     *         null element and this collection does not permit null elements,
     *         or if the specified collection is null
     * @throws IllegalArgumentException if some property of an element of the
     *         specified collection prevents it from being added to this
     *         collection
     * @throws IllegalStateException if not all the elements can be added at
     *         this time due to insertion restrictions
     * @see #add(Object)
     */
    boolean addAll(Collection<? extends E> c);

    /**
     * Removes all of this collection's elements that are also contained in the
     * specified collection (optional operation).  After this call returns,
     * this collection will contain no elements in common with the specified
     * collection.
     * 删除此集合中和指定集合相同的所有元素（可选操作）。此调用返回
     *  后，此集合将不包含与指定集合相同的元素。
     * @param c collection containing elements to be removed from this collection
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call
     * @throws UnsupportedOperationException if the <tt>removeAll</tt> method
     *         is not supported by this collection
     * @throws ClassCastException if the types of one or more elements
     *         in this collection are incompatible with the specified
     *         collection
     *         (<a href="#optional-restrictions">optional</a>)
     * @throws NullPointerException if this collection contains one or more
     *         null elements and the specified collection does not support
     *         null elements
     *         (<a href="#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    boolean removeAll(Collection<?> c);

    /**
     * Removes all of the elements of this collection that satisfy the given
     * predicate.  Errors or runtime exceptions thrown during iteration or by
     * the predicate are relayed to the caller.
     * 删除集合中所有满足给定谓词的元素。迭代过程错误或谓词抛出错误会
     *  传递给调用者。
     * @implSpec
     * The default implementation traverses all elements of the collection using
     * its {@link #iterator}.  Each matching element is removed using
     * {@link Iterator#remove()}.  If the collection's iterator does not
     * support removal then an {@code UnsupportedOperationException} will be
     * thrown on the first matching element.
     *默认实现使用集合的迭代器遍历集合的所有元素。使用remove()删除
     * 每个匹配的元素。如果集合的迭代器不支持删除，则将在第一个匹配的
     *元素上抛出UnsupportedOperationException。
     *
     * @param filter a predicate which returns {@code true} for elements to be
     *        removed
     * @return {@code true} if any elements were removed
     * @throws NullPointerException if the specified filter is null
     * @throws UnsupportedOperationException if elements cannot be removed
     *         from this collection.  Implementations may throw this exception if a
     *         matching element cannot be removed or if, in general, removal is not
     *         supported.
     * @since 1.8
     *  default方法是在java8中引入的关键字，也可称为
     *  Virtual extension methods——虚拟扩展方法。是指，在接口内部包含了
     * 一些默认的方法实现。（也就是接口中可以包含方法体，这打破了Java之
     *   前版本对接口的语法限制）
     *   此方法：移除满足条件的所有元素
     */
    default boolean removeIf(Predicate<? super E> filter) {
        //判断filter是否为空，如果为空抛出空指针错误
        Objects.requireNonNull(filter);
        boolean removed = false;
        final Iterator<E> each = iterator();  //得到集合的遍历器
        while (each.hasNext()) {     //循环遍历集合
            if (filter.test(each.next())) {      //满足条件则删除
                each.remove();
                removed = true;
            }
        }
        return removed;
    }

    /**
     * Retains only the elements in this collection that are contained in the
     * specified collection (optional operation).  In other words, removes from
     * this collection all of its elements that are not contained in the
     * specified collection.
     *只保留此集合中包含在指定集合中的元素（可选操作）。换句话说，
     *  从这个集合中删除指定集合中不包含的所有元素。
     * @param c collection containing elements to be retained in this collection
     * @return <tt>true</tt> if this collection changed as a result of the call
     * @throws UnsupportedOperationException if the <tt>retainAll</tt> operation
     *         is not supported by this collection
     * @throws ClassCastException if the types of one or more elements
     *         in this collection are incompatible with the specified
     *         collection
     *         (<a href="#optional-restrictions">optional</a>)
     * @throws NullPointerException if this collection contains one or more
     *         null elements and the specified collection does not permit null
     *         elements
     *         (<a href="#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    boolean retainAll(Collection<?> c);

    /**
     * Removes all of the elements from this collection (optional operation).
     * The collection will be empty after this method returns.
     *删除集合中所有元素（可选操作）
     * 方法返回之后集合为空
     * @throws UnsupportedOperationException if the <tt>clear</tt> operation
     *         is not supported by this collection
     */
    void clear();


    // Comparison and hashing
    //比较和散列
    /**
     * Compares the specified object with this collection for equality. <p>
     *比较指定的集合和此集合是否相等
     *
     * While the <tt>Collection</tt> interface adds no stipulations to the
     * general contract for the <tt>Object.equals</tt>, programmers who
     * implement the <tt>Collection</tt> interface "directly" (in other words,
     * create a class that is a <tt>Collection</tt> but is not a <tt>Set</tt>
     * or a <tt>List</tt>) must exercise care if they choose to override the
     * <tt>Object.equals</tt>.  It is not necessary to do so, and the simplest
     * course of action is to rely on <tt>Object</tt>'s implementation, but
     * the implementor may wish to implement a "value comparison" in place of
     * the default "reference comparison."  (The <tt>List</tt> and
     * <tt>Set</tt> interfaces mandate such value comparisons.)<p>
     *当集合接口没有为Object.equals的通用契约添加任何约定时，实现
     * 集合接口的程序员（换句话说，创建一个除Set和List之外的集合类）
     *  必须注意是否选择覆盖Object.equals。并不一定非要这样做，最简单
     *  的做法是依赖Object的实现，但实现者可能希望实现一个“值比较”来
     *   代替默认的“引用比较”。（List和Set接口要求进行这样的值比较）
     *
     * The general contract for the <tt>Object.equals</tt> method states that
     * equals must be symmetric (in other words, <tt>a.equals(b)</tt> if and
     * only if <tt>b.equals(a)</tt>).  The contracts for <tt>List.equals</tt>
     * and <tt>Set.equals</tt> state that lists are only equal to other lists,
     * and sets to other sets.  Thus, a custom <tt>equals</tt> method for a
     * collection class that implements neither the <tt>List</tt> nor
     * <tt>Set</tt> interface must return <tt>false</tt> when this collection
     * is compared to any list or set.  (By the same logic, it is not possible
     * to write a class that correctly implements both the <tt>Set</tt> and
     * <tt>List</tt> interfaces.)
     *
     * Object.equals方法的通用约定时等号必须是对称的（换句话说，
     * a.equals(b)当且仅当b.equals(a)）。List.equals和Set.equals的规范是
     * list只等于其他list，set只等于其它set。因此，一个非List和Set集合
     * 类中定制的equal方法在其与List或Set比较时，必须返回false。（根
     *  据这样的逻辑，不可能同时实现Set和List接口）。
     *
     * @param o object to be compared for equality with this collection
     * @return <tt>true</tt> if the specified object is equal to this
     * collection
     *
     * @see Object#equals(Object)
     * @see Set#equals(Object)
     * @see List#equals(Object)
     */
    boolean equals(Object o);

    /**
     * Returns the hash code value for this collection.  While the
     * <tt>Collection</tt> interface adds no stipulations to the general
     * contract for the <tt>Object.hashCode</tt> method, programmers should
     * take note that any class that overrides the <tt>Object.equals</tt>
     * method must also override the <tt>Object.hashCode</tt> method in order
     * to satisfy the general contract for the <tt>Object.hashCode</tt> method.
     * In particular, <tt>c1.equals(c2)</tt> implies that
     * <tt>c1.hashCode()==c2.hashCode()</tt>.
     * 返回此集合的hash值。当集合接口没有为Object.hasCode方法的通用
     * 规范添加任何规定时，程序员应该注意任何重写了Object.equals方法
     *  的类也应该重写Object.hashCode方法，才能满足Object.hashCode
     *  方法的通用规范。特别地，c1.equals(c2)即意味着
     *  c1.hashCode()==c2. hashcode()
     *
     * @return the hash code value for this collection
     *
     * @see Object#hashCode()
     * @see Object#equals(Object)
     */
    int hashCode();

    /**
     * Creates a {@link Spliterator} over the elements in this collection.
     *
     * Implementations should document characteristic values reported by the
     * spliterator.  Such characteristic values are not required to be reported
     * if the spliterator reports {@link Spliterator#SIZED} and this collection
     * contains no elements.
     *
     * <p>The default implementation should be overridden by subclasses that
     * can return a more efficient spliterator.  In order to
     * preserve expected laziness behavior for the {@link #stream()} and
     * {@link #parallelStream()}} methods, spliterators should either have the
     * characteristic of {@code IMMUTABLE} or {@code CONCURRENT}, or be
     * <em><a href="Spliterator.html#binding">late-binding</a></em>.
     * If none of these is practical, the overriding class should describe the
     * spliterator's documented policy of binding and structural interference,
     * and should override the {@link #stream()} and {@link #parallelStream()}
     * methods to create streams using a {@code Supplier} of the spliterator,
     * as in:
     * <pre>{@code
     *     Stream<E> s = StreamSupport.stream(() -> spliterator(), spliteratorCharacteristics)
     * }</pre>
     * <p>These requirements ensure that streams produced by the
     * {@link #stream()} and {@link #parallelStream()} methods will reflect the
     * contents of the collection as of initiation of the terminal stream
     * operation.
     *
     * @implSpec
     * The default implementation creates a
     * <em><a href="Spliterator.html#binding">late-binding</a></em> spliterator
     * from the collections's {@code Iterator}.  The spliterator inherits the
     * <em>fail-fast</em> properties of the collection's iterator.
     * <p>
     * The created {@code Spliterator} reports {@link Spliterator#SIZED}.
     *
     * @implNote
     * The created {@code Spliterator} additionally reports
     * {@link Spliterator#SUBSIZED}.
     *
     * <p>If a spliterator covers no elements then the reporting of additional
     * characteristic values, beyond that of {@code SIZED} and {@code SUBSIZED},
     * does not aid clients to control, specialize or simplify computation.
     * However, this does enable shared use of an immutable and empty
     * spliterator instance (see {@link Spliterators#emptySpliterator()}) for
     * empty collections, and enables clients to determine if such a spliterator
     * covers no elements.
     *
     * @return a {@code Spliterator} over the elements in this collection
     * @since 1.8
     */
    @Override
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, 0);
    }

    /**
     * Returns a sequential {@code Stream} with this collection as its source.
     *
     * <p>This method should be overridden when the {@link #spliterator()}
     * method cannot return a spliterator that is {@code IMMUTABLE},
     * {@code CONCURRENT}, or <em>late-binding</em>. (See {@link #spliterator()}
     * for details.)
     *
     * @implSpec
     * The default implementation creates a sequential {@code Stream} from the
     * collection's {@code Spliterator}.
     *
     * @return a sequential {@code Stream} over the elements in this collection
     * @since 1.8
     */
    default Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Returns a possibly parallel {@code Stream} with this collection as its
     * source.  It is allowable for this method to return a sequential stream.
     *
     * <p>This method should be overridden when the {@link #spliterator()}
     * method cannot return a spliterator that is {@code IMMUTABLE},
     * {@code CONCURRENT}, or <em>late-binding</em>. (See {@link #spliterator()}
     * for details.)
     *
     * @implSpec
     * The default implementation creates a parallel {@code Stream} from the
     * collection's {@code Spliterator}.
     *
     * @return a possibly parallel {@code Stream} over the elements in this
     * collection
     * @since 1.8
     */
    default Stream<E> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }
}
