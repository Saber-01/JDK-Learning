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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Hash table based implementation of the <tt>Map</tt> interface.  This
 * implementation provides all of the optional map operations, and permits
 * <tt>null</tt> values and the <tt>null</tt> key.  (The <tt>HashMap</tt>
 * class is roughly equivalent to <tt>Hashtable</tt>, except that it is
 * unsynchronized and permits nulls.)  This class makes no guarantees as to
 * the order of the map; in particular, it does not guarantee that the order
 * will remain constant over time.
 *(译文：Hashtable 是基于Map这个接口实现的，这个实现提供了所有的可选择的映射操作，
 * 但Hashtable的key和value均不能为空。而HashMap几乎和Hashtable相似，但是它允许
 * key和value相同，并且他不支持同步（即线程不安全）。且HashMap不保证顺序存储。)
 *
 * <p>This implementation provides constant-time performance for the basic
 * operations (<tt>get</tt> and <tt>put</tt>), assuming the hash function
 * disperses the elements properly among the buckets.  Iteration over
 * collection views requires time proportional to the "capacity" of the
 * <tt>HashMap</tt> instance (the number of buckets) plus its size (the number
 * of key-value mappings).  Thus, it's very important not to set the initial
 * capacity too high (or the load factor too low) if iteration performance is
 * important.
 *(译文：假设哈希函数可以在不同的桶之间将元素正确适当的分散开，那么这个HashMap的
 * 实现为基本操作put和get都提供了恒定的时间性能。也就是迭代整个集合视图所需要的时间和HashMap实例
 * 的容量（桶的数量）和它的大小（键值对的数量）成比例。因此，如果迭代性能很重要，
 * 那么不要将初始容量设的太高（或者负载因子设的太低）是非常重要的。）
 *
 * <p>An instance of <tt>HashMap</tt> has two parameters that affect its
 * performance: <i>initial capacity</i> and <i>load factor</i>.  The
 * <i>capacity</i> is the number of buckets in the hash table, and the initial
 * capacity is simply the capacity at the time the hash table is created.  The
 * <i>load factor</i> is a measure of how full the hash table is allowed to
 * get before its capacity is automatically increased.  When the number of
 * entries in the hash table exceeds the product of the load factor and the
 * current capacity, the hash table is <i>rehashed</i> (that is, internal data
 * structures are rebuilt) so that the hash table has approximately twice the
 * number of buckets.
 *（译文：HashMap实例有两个影响其性能的参数：初始容量和负载系数。容量（capacity）指的
 * 是在哈希表中桶数，初始容量就是在哈希表创建时的容量。负载因子（load factor）是在哈希表自动
 * 增加容量之前允许哈希表能达到的满度的度量，当哈希表的数目超过了当前容量和负载因子的乘积时，
 * 哈希表会被重新哈希（即重新构件内部数据结构），使得哈希表的桶数变为原来的2倍）
 *
 * <p>As a general rule, the default load factor (.75) offers a good
 * tradeoff between time and space costs.  Higher values decrease the
 * space overhead but increase the lookup cost (reflected in most of
 * the operations of the <tt>HashMap</tt> class, including
 * <tt>get</tt> and <tt>put</tt>).  The expected number of entries in
 * the map and its load factor should be taken into account when
 * setting its initial capacity, so as to minimize the number of
 * rehash operations.  If the initial capacity is greater than the
 * maximum number of entries divided by the load factor, no rehash
 * operations will ever occur.
 *（译文：作为一般规则，默认的负载因子设置为0.75，在时间和空间成本之间提供了一个很好的权衡。
 * 更高的值减少了空间开销，但是增加了查找成本（反应在HashMap类的大多数操作中，包括
 * put和get）。在设置初始容量时，应该考虑映射中的预期条目和它的负载因子，以便最小化
 * 重新散列操作的数量。如果初始容量大于最大条目数除以负载因子，则不会发生重新哈希操作。）
 *
 * <p>If many mappings are to be stored in a <tt>HashMap</tt>
 * instance, creating it with a sufficiently large capacity will allow
 * the mappings to be stored more efficiently than letting it perform
 * automatic rehashing as needed to grow the table.  Note that using
 * many keys with the same {@code hashCode()} is a sure way to slow
 * down performance of any hash table. To ameliorate impact, when keys
 * are {@link Comparable}, this class may use comparison order among
 * keys to help break ties.
 *（译文：如果在一个Hashmap实例中存储了很多的键值对映射，那么给予足够大的初始容量
 * 会比其根据需要自己重新哈希来扩容效率要高很多。注意，使用多个具有相同hashcode(这个码是
 * key通过方法hashCode()计算出来的)的key肯定会减慢任何哈希表的性能，为了改善
 * 这种情况，当键（key）是可以比较的，那么这个类可以使用键之间的比较顺序来
 * 降低这种影响）
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a hash map concurrently, and at least one of
 * the threads modifies the map structurally, it <i>must</i> be
 * synchronized externally.  (A structural modification is any operation
 * that adds or deletes one or more mappings; merely changing the value
 * associated with a key that an instance already contains is not a
 * structural modification.)  This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the map.
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedMap Collections.synchronizedMap}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the map:<pre>
 *   Map m = Collections.synchronizedMap(new HashMap(...));</pre>
 * *（译文：我们注意到HashMap的实现时不同步的，如果多个线程同时
 *  * 访问一个HashMap,并且至少有一个线程修改了映射的结构，那么它必须
 *  * 在外部进行同步。（结构修改是添加或者删除一个或多个映射的任何操作；
 *  * 仅仅更改实例中的key对应的value是不算做结构修改的）这通常是通过
 *  * 在自然封装映射的某个对象上进行同步。如果不存在这样的对象，则应该使用
 *  Collections工具类中的synchronizedMap方法进行包装，并且最好是在
 *  创建HashMap的时候就进行包装（或者使用ConcurrentHashMap,）。
 *  使用Map m = Collections.synchronizedMap(new HashMap(...));）
 *
 * <p>The iterators returned by all of this class's "collection view methods"
 * are <i>fail-fast</i>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * <tt>remove</tt> method, the iterator will throw a
 * {@link ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
 *  * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 *  * as it is, generally speaking, impossible to make any hard guarantees in the
 *  * presence of unsynchronized concurrent modification.  Fail-fast iterators
 *  * throw <tt>ConcurrentModificationException</tt> on a best-effort basis.
 *  * Therefore, it would be wrong to write a program that depended on this
 *  * exception for its correctness: <i>the fail-fast behavior of iterators
 *  * should be used only to detect bugs.</i>
 *(译文：Iterator迭代器的快速失败机制(fail-fast)，不仅对于HashMap，对于所有Collection
 * 集合的Iterator迭代器都是使用快速失败机制，一旦在迭代器创建后，任何时候除了用迭代器自带的
 * remove方法，其他方式只要是修改了映射的结构，迭代器都会抛出一个ConcurrentModificationException
 * 异常。因此，在遇到并发修改时，会快速的进行失败操作，避免在未来不确定的时间内发生
 * 不确定性的风险行为。
 * 并且，要注意的是，迭代器的快速失败机制并不能得到保证，因为通常来说，
 * 在存在非同步的并发修改时，不可能做出任何严格的保证，快速失败机制在尽最大努力
 * 地抛出ConcurrentModificationException异常。因此，编写一个依赖于此异常来
 * 判断正确性的程序是错误的。迭代器的快速失败机制只能用来检测BUG。
 * 比如在有些情况，对迭代器中元素进行删除可能不会引发异常，
 * 但一定不要以为可以在迭代时进行删除元素。对于HashMap、ArrayList等集合，
 * 迭代时进行结构化操作都会导致异常，只要在操作特定元素时才不会抛出异常，)

 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *(该类是JAVA集合中的一员)
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * （K代表映射包含的值类型，V代表映射的value的类型。）
 * @author  Doug Lea
 * @author  Josh Bloch
 * @author  Arthur van Hoff
 * @author  Neal Gafter
 * @see     Object#hashCode()
 * @see     Collection
 * @see     Map
 * @see     TreeMap
 * @see     Hashtable
 * @since   1.2
 */
public class HashMap<K,V> extends AbstractMap<K,V>
        implements Map<K,V>, Cloneable, Serializable {

    private static final long serialVersionUID = 362498820763181265L;
    //序列化用，用于比较版本，对类功能没有意义。
    /*
     * Implementation notes.
     *
     * This map usually acts as a binned (bucketed) hash table, but
     * when bins get too large, they are transformed into bins of
     * TreeNodes, each structured similarly to those in
     * java.util.TreeMap. Most methods try to use normal bins, but
     * relay to TreeNode methods when applicable (simply by checking
     * instanceof a node).  Bins of TreeNodes may be traversed and
     * used like any others, but additionally support faster lookup
     * when overpopulated. However, since the vast majority of bins in
     * normal use are not overpopulated, checking for existence of
     * tree bins may be delayed in the course of table methods.
     *(译文：这个映射通常充当一个带有很多桶的哈希表，但是当桶的内容变得太大，
     * 它们就会将桶转换为一个树状节点的容器，每个树的结构类似于java.util中TreeMap
     * 的结构（红黑树jdk8以后）。大多数方法尝试使用简单的容器bin,但是当需要时，
     * 桶中链表会转换为TreeNode(仅仅检查是不是node的实例)，红黑树节点组成的桶
     * 和其他容器一样被遍历和使用，但当元素密集时，它支持更快的查找效率。但是，
     * 因为大多数正常使用的桶是没有被过度填充的，所以在表方法过程中，可能会延迟
     * 检查以红黑树为结构的桶容器)
     *
     * Tree bins (i.e., bins whose elements are all TreeNodes) are
     * ordered primarily by hashCode, but in the case of ties, if two
     * elements are of the same "class C implements Comparable<C>",
     * type then their compareTo method is used for ordering. (We
     * conservatively check generic types via reflection to validate
     * this -- see method comparableClassFor).  The added complexity
     * of tree bins is worthwhile in providing worst-case O(log n)
     * operations when keys either have distinct hashes or are
     * orderable, Thus, performance degrades gracefully under
     * accidental or malicious usages in which hashCode() methods
     * return values that are poorly distributed, as well as those in
     * which many keys share a hashCode, so long as they are also
     * Comparable. (If neither of these apply, we may waste about a
     * factor of two in time and space compared to taking no
     * precautions. But the only known cases stem from poor user
     * programming practices that are already so slow that this makes
     * little difference.)
     *（总结：树结构的桶主要根据hashCode排序，但是如果两个元素
     * 都是实现了Comparable<C> ，那么它们可以用compareTo来进行排序。
     * 这些额外的树结构容器的复杂度是值得的，因为当遇到分配不均匀，导致
     * 很多不同key返回了相同的hashcode,性能降低就得到了平缓。）
     *
     * Because TreeNodes are about twice the size of regular nodes, we
     * use them only when bins contain enough nodes to warrant use
     * (see TREEIFY_THRESHOLD). And when they become too small (due to
     * removal or resizing) they are converted back to plain bins.  In
     * usages with well-distributed user hashCodes, tree bins are
     * rarely used.  Ideally, under random hashCodes, the frequency of
     * nodes in bins follows a Poisson distribution
     * (http://en.wikipedia.org/wiki/Poisson_distribution) with a
     * parameter of about 0.5 on average for the default resizing
     * threshold of 0.75, although with a large variance because of
     * resizing granularity. Ignoring variance, the expected
     * occurrences of list size k are (exp(-0.5) * pow(0.5, k) /
     * factorial(k)). The first values are:
     *
     * 0:    0.60653066
     * 1:    0.30326533
     * 2:    0.07581633
     * 3:    0.01263606
     * 4:    0.00157952
     * 5:    0.00015795
     * 6:    0.00001316
     * 7:    0.00000094
     * 8:    0.00000006
     * more: less than 1 in ten million
     *（总结：由于树结构节点大小相对于普通链表节点大小是两倍，所以我们只在容器中
     * 包含了足够多的链表节点以后，才对这个容器结构进行转化，参见（TREEIFY_THRESHOLD）
     * 并且当容器节点变得足够小的时候，（由于removal或resizing）它们会转换回原来的桶结构，
     * 在一个很好的散列开的哈希表中，红黑树桶是很少被用到的。
     * 接下来一段是数学的概率分析。说不清楚）
     *
     * The root of a tree bin is normally its first node.  However,
     * sometimes (currently only upon Iterator.remove), the root might
     * be elsewhere, but can be recovered following parent links
     * (method TreeNode.root()).
     *（树容器的节点一般是第一个节点，但是有些时候（目前仅可能Iterator.remove造成）
     * root的节点也许是其他的，但是它们可以通过指向父节点的链接被恢复）
     *
     * All applicable internal methods accept a hash code as an
     * argument (as normally supplied from a public method), allowing
     * them to call each other without recomputing user hashCodes.
     * Most internal methods also accept a "tab" argument, that is
     * normally the current table, but may be a new or old one when
     * resizing or converting.
     *（所有应用内部方法，都接收hashcode 作为一个参数（这通常是由一个公共方法提供的），
     * 允许它们可以互相调用而不用通过重新计算使用的hashcode。
     * 大多数的内部方法也接收一个tab参数，通常表示的是当前的哈希表，但也许
     * 是一个正在扩容或者转换的新或者老的表）
     *
     * When bin lists are treeified, split, or untreeified, we keep
     * them in the same relative access/traversal order (i.e., field
     * Node.next) to better preserve locality, and to slightly
     * simplify handling of splits and traversals that invoke
     * iterator.remove. When using comparators on insertion, to keep a
     * total ordering (or as close as is required here) across
     * rebalancings, we compare classes and identityHashCodes as
     * tie-breakers.
     *（当桶被转换成红黑树时，分离或者从树中拆开，我们要保证它们访问、
     * 遍历的次序一致（比如 变量 node.next），目的是更好的保留位置，并且
     * 稍微简化调用iterator.remove的拆分和遍历的处理。即红黑树的每一次插入
     * 和删除，都要维护红黑树的结构不被破坏，）
     *
     * The use and transitions among plain vs tree modes is
     * complicated by the existence of subclass LinkedHashMap. See
     * below for hook methods defined to be invoked upon insertion,
     * removal and access that allow LinkedHashMap internals to
     * otherwise remain independent of these mechanics. (This also
     * requires that a map instance be passed to some utility methods
     * that may create new nodes.)
     *
     * The concurrent-programming-like SSA-based coding style helps
     * avoid aliasing errors amid all of the twisty pointer operations.
     */

    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
     //默认的初始容器大小为2的4次方，初始容器一定是2的次方。
    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;//最大的容量为2的30次方，

    /**
     * The load factor used when none specified in constructor.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;//默认负载因子0.75

    /**
     * The bin count threshold for using a tree rather than list for a
     * bin.  Bins are converted to trees when adding an element to a
     * bin with at least this many nodes. The value must be greater
     * than 2 and should be at least 8 to mesh with assumptions in
     * tree removal about conversion back to plain bins upon
     * shrinkage.
     */
    //链表节点转换为红黑树的阈值为8个节点，
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * The bin count threshold for untreeifying a (split) bin during a
     * resize operation. Should be less than TREEIFY_THRESHOLD, and at
     * most 6 to mesh with shrinkage detection under removal.
     */
    //当红黑树转化为链表的阈值为6个节点，
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * The smallest table capacity for which bins may be treeified.
     * (Otherwise the table is resized if too many nodes in a bin.)
     * Should be at least 4 * TREEIFY_THRESHOLD to avoid conflicts
     * between resizing and treeification thresholds.
     */
    //存在红黑树的必要条件是，table的最小长度为64，
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * Basic hash bin node, used for most entries.  (See below for
     * TreeNode subclass, and in LinkedHashMap for its Entry subclass.)
     */
    static class Node<K,V> implements Map.Entry<K,V> {  //hash链表节点，实现了Map中的Entry接口类。
        final int hash;    //hashcode值
        final K key;        // 键
        V value;             //值
        Node<K,V> next;    //存放下一个链表节点

        Node(int hash, K key, V value, Node<K,V> next) {    //节点构造函数
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey()        { return key; }               //得到节点key值
        public final V getValue()      { return value; }               //得到节点value值
        public final String toString() { return key + "=" + value; }          //toString方法 返回 key=value形式

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }    //节点类自己的哈希方法。得到哈希值

        public final V setValue(V newValue) {    //设置值，成员value赋值为新值，将旧值返回。
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {         //节点类的equals 方法，只有当两个节点的key和value用== 判断都相等的情况下，才为true
            if (o == this)   //如果o就是调用这个方法的对象，那么肯定相等
                return true;
            if (o instanceof Map.Entry) {           //如果o是实现了Map.Entry类的派生类对象，
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;      //转化为Entry，用于比较
                if (Objects.equals(key, e.getKey()) &&      //调用了Object类的equal方法，即简单的用==比较 key和value存储地址是否相同。
                        Objects.equals(value, e.getValue()))
                    return true;                                        //当key 和value分别用== 比较都相同，返回true。
            }
            return false;
        }
    }

    /* ---------------- Static utilities -------------- */

    /**
     * Computes key.hashCode() and spreads (XORs) higher bits of hash
     * to lower.  Because the table uses power-of-two masking, sets of
     * hashes that vary only in bits above the current mask will
     * always collide. (Among known examples are sets of Float keys
     * holding consecutive whole numbers in small tables.)  So we
     * apply a transform that spreads the impact of higher bits
     * downward. There is a tradeoff between speed, utility, and
     * quality of bit-spreading. Because many common sets of hashes
     * are already reasonably distributed (so don't benefit from
     * spreading), and because we use trees to handle large sets of
     * collisions in bins, we just XOR some shifted bits in the
     * cheapest possible way to reduce systematic lossage, as well as
     * to incorporate impact of the highest bits that would otherwise
     * never be used in index calculations because of table bounds.
     */
    //因为当桶数很小时，很大的hash值（它的二进制总是高位在变化），
    //在散列时，总是发生碰撞，所以使用一种方法将较高位扩展到较低位。
    //让table 在容量较小时，高位也能够参与散列运算，并且不会造成较大开销。
    static final int hash(Object key) { //重新计算key的hash值
        int h;            //用于返回的hash值
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }//如果key为null，返回0，如果key不为0，原hash值高位右移16位到低位，然后与原数进行异或。

    /**
     * Returns x's Class if it is of the form "class C implements
     * Comparable<C>", else null.
     */
    //如果对象x的类是C，如果C实现了Comparable<C>接口，那么返回C，否则返回null
    static Class<?> comparableClassFor(Object x) {  //Class<?>通配，可以表示任意一类的类型。
        if (x instanceof Comparable) {    //如果x是实现了Comparable接口的类。
            Class<?> c; Type[] ts, as; Type t; ParameterizedType p;
            if ((c = x.getClass()) == String.class) // bypass checks  如果x的类型是String类，直接返回该类型c，因为String实现了Comparable接口
                return c;
            if ((ts = c.getGenericInterfaces()) != null) {  //如果c不是字符串类，则先获取c类别实现的所有接口列表
                for (int i = 0; i < ts.length; ++i) {    //遍历接口类
                    if (((t = ts[i]) instanceof ParameterizedType) &&    //如果是泛型接口t，
                            ((p = (ParameterizedType)t).getRawType() == //获得泛型接口t的类型p。
                                    Comparable.class) &&  //如果泛型接口t的类型p是Comparable，
                            (as = p.getActualTypeArguments()) != null &&   //获取类型p(Comparable)的泛型列表，如果这个列表不为空，
                            as.length == 1 && as[0] == c) // type arg is c    //且只有1个泛型，这个泛型为c.
                        return c;                   //则说明c类型实现了接口Comparable<c> ,则返回c
                }
            }
        }
        return null;                       //没实现Comparable 接口 就返回null.
    }

    /**
     * Returns k.compareTo(x) if x matches kc (k's screened comparable
     * class), else 0.
     */
    //kc是k的可筛选比较的类型，如果x属于kc,返回 kcompareTo(x)的比较结果。否则返回0
    @SuppressWarnings({"rawtypes","unchecked"}) // for cast to Comparable
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 :
                ((Comparable)k).compareTo(x));
    }

    /**
     * Returns a power of two size for the given target capacity.
     */
    static final int tableSizeFor(int cap) {   //返回一个给定的容量的大于或等于的2的倍数
        int n = cap - 1;     //减一是为了保证原数如果已经是2的整数次幂了，那就返回原值。
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;        //结果就是将等于1的最高位数后面的位数全部变为1.
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;    //n超出了最大容量，则赋值为最大容量，否则加一变为2的整数次幂
    }

    /* ---------------- Fields -------------- */

    /**
     * The table, initialized on first use, and resized as
     * necessary. When allocated, length is always a power of two.
     * (We also tolerate length zero in some operations to allow
     * bootstrapping mechanics that are currently not needed.)
     */
    //表，在第一次使用时初始化，并根据需要调整大小。
    // 当分配时，长度总是2的幂。(我们还允许在一些操作中允许长度为零，以允许目前不需要的引导机制。)
    transient Node<K,V>[] table; //桶数组。
      //transient表示序列化对象的时候，这个属性就不会被序列化
    /**
     * Holds cached entrySet(). Note that AbstractMap fields are used
     * for keySet() and values().
     *///保存缓存entrySet ()。注意，AbstractMap字段用于keySet()和values()。
    //
    transient Set<Map.Entry<K,V>> entrySet;

    /**
     * The number of key-value mappings contained in this map.
     */
    transient int size;  //包含key-value 键值对的数量

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    //结构修改是指那些改变HashMap中映射数量或修改其内部结构的修改(例如，重新哈希)。
    // 此字段用于使HashMap的集合视图上的迭代器快速失效。(见ConcurrentModificationException)
    transient int modCount;    //表示哈希表被重新构建的次数。

    /**
     * The next size value at which to resize (capacity * load factor).
     *
     * @serial
     */
    // (The javadoc description is true upon serialization.
    // Additionally, if the table array has not been allocated, this
    // field holds the initial array capacity, or zero signifying
    // DEFAULT_INITIAL_CAPACITY.)
      //(javadoc描述在序列化时是正确的。此外，如果没有分配表数组，
    // 则该字段保存初始数组容量，为0表示,容量使用默认DEFAULT_INITIAL_CAPACITY)
    int threshold;    //初始容量，

    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    final float loadFactor;    //哈希表的负载因子

    /* ---------------- Public operations -------------- */

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    //使用指定的初始容量和负载因子，重新构建空的哈希表。如果初始容量和负载因子出现负数，抛出异常、
    public HashMap(int initialCapacity, float loadFactor) {//双参数的构造函数
        if (initialCapacity < 0)              //初始容量为负 ，抛出异常。
            throw new IllegalArgumentException("Illegal initial capacity: " +
                    initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)       //如果初始容量大于了哈希表最大容量2的30次方，则以最大容量赋值初始容量。
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))          //如果负载因子小于等于0，或者负载因子not a number，非数字值，则抛出异常
            throw new IllegalArgumentException("Illegal load factor: " +
                    loadFactor);
        this.loadFactor = loadFactor;                         //赋值hashmap的负载因子
        this.threshold = tableSizeFor(initialCapacity);      //将初始容量变为大于等于它的最小的2的整数次幂，然后赋值给初始容量
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public HashMap(int initialCapacity) {   //只指定初始容量的构造函数。负载因子默认0.75
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public HashMap() {               //没有参数的构造函数。初始容量为16.负载因子为0.75
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }

    /**
     * Constructs a new <tt>HashMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>HashMap</tt> is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <tt>Map</tt>.
     * 构造一个新的HashMap，使用与指定的Map相同的映射。
     * HashMap使用默认负载因子(0.75)和足以容纳指定Map中的映射的初始容量创建。
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     */
    public HashMap(Map<? extends K, ? extends V> m) {      //使用和指定Map相同的映射来创建哈希表。
        this.loadFactor = DEFAULT_LOAD_FACTOR;   // 负载因子为默认0.75
        putMapEntries(m, false);
    }

    /**
     * Implements Map.putAll and Map constructor
     *
     * @param m the map
     * @param evict false when initially constructing this map, else
     * true (relayed to method afterNodeInsertion).
     * evict 最初构造此映射时为false，否则为true(在nodeinsert之后转发给方法)
     */
    final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {   //接上面使用Map创建hashmap的构造函数。evict表示是否为最初构造
        int s = m.size();            //存储映射 键值对总数
        if (s > 0) {              //如果参数map不为空。
            if (table == null) { // pre-size    //初始化容器的容量。
                float ft = ((float)s / loadFactor) + 1.0F;      //当前键值对数目除以负载因子+1
                int t = ((ft < (float)MAXIMUM_CAPACITY) ?   //如果上面的值大于最大容量，则直接用最大容量。
                        (int)ft : MAXIMUM_CAPACITY);
                if (t > threshold)                    //当table为null 时，threshold保存的是初始容量(未乘0.75)，所以用ft(而不是s)来比较。
                    threshold = tableSizeFor(t);         //如果超出了。就对其进行扩容。得到大于等于它的最小2 的整数次幂作为初始阈值(将在第一次put时计入容量中)。
            }
            else if (s > threshold)        //如果table 不为空。键值对数量大于阈值。进行扩容。
                resize();
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {  //Iterator遍历 Map
                K key = e.getKey();                 //得到key,
                V value = e.getValue();                        //得到value
                putVal(hash(key), key, value, false, evict);   //把每个key-value键值对插入到hashmap中。
            }
        }
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {            //得到映射中 键值对的总数。
        return size;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    public boolean isEmpty() {     //判断hashmap是否为空。空返回true、
        return size == 0;
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, Object)
     */
    public V get(Object key) {      //根据key 获得value值。
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value; //调用getNode，
    }

    /**
     * Implements Map.get and related methods
     *
     * @param hash hash for key
     * @param key the key
     * @return the node, or null if none
     */
    final Node<K,V> getNode(int hash, Object key) {      //根据key,和key的hash出的值，找到hashmap中对应的节点。
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&    //如果表不为空，且表长度大于0，
                (first = tab[(n - 1) & hash]) != null) {            //根据hash值找到对应的表的索引位置上的桶，桶不为空时，将这个桶的头结点赋值给first。
            if (first.hash == hash && // always check first node    //判断first头结点hash值，
                    ((k = first.key) == key || (key != null && key.equals(k))))    //判断传入的hash,key和头结点first的相同，说明找到了这个点
                return first;  //返回first
            if ((e = first.next) != null) {          //如果没有返回，且这个桶的节点不止一个。这是需要分2种情况，是红黑树还是普通链表
                if (first instanceof TreeNode)            //如果是红黑树
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);  //调用红黑树的找节点方法。并返回
                do {     //如果是普通链表。则开始遍历
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k))))      //直到遍历到hash和key都相等的节点，就进行返回。
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;         //如果以上都没找到，说明这个找不到这个节点，返回null。
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the
     * specified key.
     *
     * @param   key   The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     */
    public boolean containsKey(Object key) {        //根据key判断是否含有这个映射。和get类似，就是返回值变为boolean
        return getNode(hash(key), key) != null;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public V put(K key, V value) {                   //往hashmap中添加一对键值对
        return putVal(hash(key), key, value, false, true);    //调用putVal方法。
    }

    /**
     * Implements Map.put and related methods
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to put
     * @param onlyIfAbsent if true, don't change existing value 只有缺省才覆盖，为true说明不改变已经存在的值。
     * @param evict if false, the table is in creation mode. 为false，说明哈希表处理创建模式
     * @return previous value, or null if none
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        //判断table是否为空，或长度为0 ，如果满足，则调用resize(),进行初始化，并且把数组长度赋值给n。
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((p = tab[i = (n - 1) & hash]) == null)      //(n-1) &hash, 与hash%length 相同，即散列函数，得到索引i，并且判断对应桶tab[i]是否为空。
            tab[i] = newNode(hash, key, value, null);  //如果为空，则新建一个链表节点放入哈希桶中。
        else {  //如果对应桶位置tab[i]不为空。
            Node<K,V> e; K k;
            if (p.hash == hash &&           //只有当桶的结点p的hash值和传入的hash值相同，
                    ((k = p.key) == key || (key != null && key.equals(k))))  //并且节点P的key和传入的key值相等时
                e = p;                                                          //如果满足相等，说明要进行value覆盖，先把这个节点赋值给e。
            else if (p instanceof TreeNode)       //如果桶的节点p是否是红黑树节点，如果是，就调用红黑树的putTreeVal方法，
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);  //和上一步同样返回遍历到的节点。
            else {       //走到这里，说明头结点不相等，并且，桶并非红黑树结构。
                for (int binCount = 0; ; ++binCount) {    //则遍历桶上的链表结构。 binCount用于计算遍历了多少次。
                    if ((e = p.next) == null) {     //e赋值为下一个链表节点。如果它的p.next为空，说明链表遍历到达尾部，
                        p.next = newNode(hash, key, value, null);  // 根据传入hash,key,value创建一个新的非树节点，将这个节点放入到链表的尾部。
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st //如果遍历了7次(尾部新加1个节点，这时链表节点为8),超出了链表转化为红黑树的阈值
                            treeifyBin(tab, hash);    //将链表转化为红黑树。 传入了当前传入的这个节点的hash值和hashmap的table。
                        break;      //打断遍历。
                    }
                    if (e.hash == hash &&              //如果还没到达尾部，就发现了和传入的hash,key相同的点，说明需要进行value覆盖。此时这个节点就为e。中断遍历。
                            ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;      //将e赋值为p，即遍历。
                }
            }
            //e不为空，说明传入的hash,key值在桶中找到了相同的节点，需要进行value 覆盖。修改value后直接返回不参与size++。所以size不变
            if (e != null) { // existing mapping for key
                V oldValue = e.value;              //覆盖需要保留原来的value 值。
                if (!onlyIfAbsent || oldValue == null)    // 如果没有要求不能覆盖，或者要求不能覆盖但是值为null,
                    e.value = value;                                //将这个节点的vlaue值覆盖为新传入的value值；
                afterNodeAccess(e);  //用于LinkedHashMap
                return oldValue;         //返回这个旧值。
            }
        }
        ++modCount;          //Put改变了hashmap的结构，所以modCount自加1.
        if (++size > threshold)    //插入一个有效的键值对后,(即不发生覆盖value),size要加1，并且和容量阈值进行比较
            resize();          //如果大于阈值，需要调用resize，扩容。
        afterNodeInsertion(evict);   //用于LinkedHashMap
        return null;             //未发生覆盖value,就返回null.
    }

    /**
     * Initializes or doubles table size.  If null, allocates in
     * accord with initial capacity target held in field threshold.
     * Otherwise, because we are using power-of-two expansion, the
     * elements from each bin must either stay at same index, or move
     * with a power of two offset in the new table.
     *
     * @return the table
     */
    final Node<K,V>[] resize() {      //resize，重新构造哈希表结构大小，返回 哈希桶数组。
        Node<K,V>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length; //如果旧表为空，容量桶数显然为0，如果不为空，则容量桶数为旧表的长度。
        int oldThr = threshold;  //初始容量阈值赋值给oldThr
        int newCap, newThr = 0;     //对应的，定义2个用于存储新表容量(桶数)，和新表阈值的变量（键值对）。
        if (oldCap > 0) {              //如果旧表不为空，哈希桶数目大于0，说明不是首次put。
            if (oldCap >= MAXIMUM_CAPACITY) {    //旧容量已经到达了hashmap的最大容量。
                threshold = Integer.MAX_VALUE;          //那么只能将size的阈值调大到最大整数。
                return oldTab;                           //不能扩大桶数，则调整完阈值后就只能返回。
            }
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&      //新的容量扩容2倍
                    oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; // double threshold          //新的阈值也扩大2倍
        }
        else if (oldThr > 0) //如果新表没初始化。但阈值不为0，是因为使用了带有初始容量参数的构造函数，首次put时就会出现数组为空，但初始容量不为空。
            newCap = oldThr;                       //所以这是就将指定的初始容量赋值给需要创建的容量(哈希桶数)
        else {                      // 如果就容量和旧阈值都为0，说明未指定初始容量构造了hashmap，则设置初始容量和初始阈值。
            newCap = DEFAULT_INITIAL_CAPACITY;           //初始容量就为默认的容量16
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY); //初始阈值就为默认容量*0.75
        }
        if (newThr == 0) {   //newThr还没赋值，走的上面第二条使用了带初始容量参数的构造函数，首次put需要将阈值设为容量的0.75(容量没超最大值时)，
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?    //阈值应该是新容量的0.75，
                    (int)ft : Integer.MAX_VALUE);         //如果超出了最大容量，则阈值赋值为最大整数。
        }
        threshold = newThr;        //设置好的新阈值赋值给hashmap的threshld属性。
        @SuppressWarnings({"rawtypes","unchecked"})
         //定义一个新表，容量为刚才计算出来的新容量。  （到这一步，我们已经计算好新容量，并且设置好阈值了）
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        table = newTab; //对hashmap的table成员赋值为新表。
        if (oldTab != null) {         //如果旧表不为空，
            for (int j = 0; j < oldCap; ++j) {         //遍历旧表
                Node<K,V> e;
                if ((e = oldTab[j]) != null) {           //将遍历到索引为j的表头节点赋值给e,如果表头节点不为null,说明桶不为空，
                    oldTab[j] = null;         //将表头节点直接赋值为null,便于垃圾回收。
                    if (e.next == null)    //如果上一步存的表头节点的e为空，代表旧表的这个桶上只有一个节点，
                        newTab[e.hash & (newCap - 1)] = e;  //只有一个节点时，这个hash值从新通过%(length-1)求得新索引，直接放入
                    else if (e instanceof TreeNode)  //如果这个节点是红黑树节点。
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);    //就调用split方法对这个桶中红黑树所有节点进行重新hash分布
                    else { // preserve order           //如果为普通链表节点
                        Node<K,V> loHead = null, loTail = null;//存储跟原索引位置相同的节点。
                        Node<K,V> hiHead = null, hiTail = null;//存储跟原索引+oldCap的节点。
                        //因为在旧表同索引位置，它们的n(n为2^n=oldCap)位右边相同，则扩容后，n位上为0还是为1决定了它们是散列到原索引，还是索引+oldCap上。
                        Node<K,V> next;
                        do {
                            next = e.next;
                            if ((e.hash & oldCap) == 0) { //  n位上为零，放入原索引相同位置。即链入lo链表
                                if (loTail == null)        //首次时，将loHead赋值为第一个节点。
                                    loHead = e;
                                else       //    不是第一个节点，就正常遍历，
                                    loTail.next = e;   //将遍历到尾节点串在当前节点next后面
                                loTail = e;        //更新尾结点。
                            }
                            else {              //和上面的情况相似，只是这边连接的是应该放入原索引+oldCap位置的节点。链入hi链表
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null); //e赋值为下一个链表节点。直到末尾。
                        if (loTail != null) {       //如果尾结点不为空，即lo不为空
                            loTail.next = null;    //设置tail尾结点的next为null.
                            newTab[j] = loHead;   //原索引位置桶放入lo链表的头结点
                        }
                        if (hiTail != null) {  //与上面类似
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;  //原索引位置+oldCap的位置放入hi链表头结点。
                        }
                    }
                }
            }
        }
        return newTab;     //返回扩容后的表。
    }

    /**
     * Replaces all linked nodes in bin at index for given hash unless
     * table is too small, in which case resizes instead.
     */
    //当一个桶中链表节点数超过阈值8个，调用这个方法。
    //注意如果这时候，table还很小，首选的是扩容，而不是转换为红黑树。
    //阈值为64，table容量长度大于等于64，且链表节点大于8个就转换为红黑树。（新转换为双向链表，再构建红黑树）
    final void treeifyBin(Node<K,V>[] tab, int hash) {
        int n, index; Node<K,V> e;
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)  //容量小于64，选择扩容。
            resize();
        else if ((e = tab[index = (n - 1) & hash]) != null) {       //  传入节点的桶位置不为空。且将这个桶头节点赋值给e.
            TreeNode<K,V> hd = null, tl = null;
            do {                                  //遍历刚索引位置桶上的链表。
                TreeNode<K,V> p = replacementTreeNode(e, null);     //链表节点转换为红黑树节点。
                if (tl == null)       //如果是第一次循环遍历。
                    hd = p;            //则树的头结点赋值为p.
                else {
                    p.prev = tl;      //当前节点的前一个节点赋值为tl保存的上一个节点。
                    tl.next = p;       //上一个节点的next属性设置为当前节点。
                }
                tl = p;       //tl更新为当前节点。
            } while ((e = e.next) != null);
            if ((tab[index] = hd) != null)    //如果将上面构建的双向链表的头结点赋值给这个桶
                hd.treeify(tab);        //以这个桶的头结点为根节点构建红黑树。
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     * @throws NullPointerException if the specified map is null
     */
    public void putAll(Map<? extends K, ? extends V> m) {             //将一组键值对全部放入。原理和使用键值对构造hashmap差不多，只是容量定义上有差别。
        putMapEntries(m, true);     //处理好容量和阈值问题以后，就是等同于一步一步遍历map，逐个putVal;
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param  key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public V remove(Object key) {      //根据传入的key删除一个节点。
        Node<K,V> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ?
                null : e.value;           //如果移除成功，返回移除点的value值。否则返回null。
    }

    /**
     * Implements Map.remove and related methods
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to match if matchValue, else ignored   //可以传入key对应的value，或null表示不传入value。
     * @param matchValue if true only remove if value is equal   //如果为true，表示只有和传入的值相同，才删除。
     * @param movable if false do not move other nodes while removing 如果为false，在删除时不移动其他节点。
     * @return the node, or null if none       //返回删除的节点，如果没找到删除的点，返回null
     */
    //
    final Node<K,V> removeNode(int hash, Object key, Object value,
                                                 boolean matchValue, boolean movable) {
        Node<K,V>[] tab; Node<K,V> p; int n, index;               //removeNode,第一步是从hashmap中找到节点。第二步才是删除。
        //哈希表不为空，且容量大于0，并且传入的hash值计算得到的索引位置上存在节点。
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (p = tab[index = (n - 1) & hash]) != null) {       //将头结点赋值给p
            Node<K,V> node = null, e; K k; V v;    //node用于存储在hashmap中找到的要删除的节点，如果没找到则为null。
            if (p.hash == hash &&
                    ((k = p.key) == key || (key != null && key.equals(k))))      //如果头结点和传入的key,hash相等，说明找到了要删除的点，赋值给node
                node = p;
            else if ((e = p.next) != null) {       //如果该位置不止一个节点。则要判断是红黑树还是普通链表
                if (p instanceof TreeNode)   //如果头结点是红黑树的根节点。
                    node = ((TreeNode<K,V>)p).getTreeNode(hash, key);  //调用红黑树的getTreeNode的方法找到树中节点。赋值给node.
                else {      //如果是普通链表，则要进行遍历
                    do {
                        if (e.hash == hash &&
                                ((k = e.key) == key ||
                                        (key != null && key.equals(k)))) {      //在遍历过程中，找到了hash,key都相等的节点。
                            node = e;                      //找到就赋值给node. 并退出遍历
                            break;
                        }
                        p = e;             //p节点更新为 本次循环的节点。如果上一步找到打断了，则p保存了找到节点的上个节点。
                    } while ((e = e.next) != null);  //指向下一个节点。
                }
            }
            //如果找到了这个node，就判断是否传入了值，如果传入value，还要判断value相同。
            if (node != null && (!matchValue || (v = node.value) == value ||
                    (value != null && value.equals(v)))) {
                if (node instanceof TreeNode)   //如果这个节点是红黑树中的节点。
                    ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable); //则调用红黑树移除节点的方法。
                else if (node == p)        //如果为普通链表的头结点。
                    tab[index] = node.next;         //直接舍弃头结点，将第二个节点或者null赋值给表索引上的桶。
                else      //如果为链表节点。
                    p.next = node.next;  //将要删除的上个节点的next赋值为删除节点的下一个节点。
                ++modCount;            //删除成功后，记录一次哈希表改变结构的次数。
                --size;               //删除成功后，哈希表的键值对数量少了1对。
                afterNodeRemoval(node);    //提供给linkedHashMap使用
                return node;            //返回被删除的节点。
            }
        }
        return null;           //如果没有找到这个节点，返回null。
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {              //清空hashmap
        Node<K,V>[] tab;
        modCount++;           //清空也算是改变了哈希表结构，所以次数加1。
        if ((tab = table) != null && size > 0) {  //如果表不为空，且键值对不为0.
            size = 0;      //将表清空以后，键值对为0；
            for (int i = 0; i < tab.length; ++i)   //将数组中的每一个桶都赋值null。方便垃圾回收机制。
                tab[i] = null;
        }
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value
     */
    public boolean containsValue(Object value) {
        Node<K,V>[] tab; V v;
        if ((tab = table) != null && size > 0) {      //如果表不为空，size不为0.
            for (int i = 0; i < tab.length; ++i) {          //先遍历每个桶
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {     //再遍历每个桶中的节点。
                    if ((v = e.value) == value ||           //直到找到了value 相同时，返回true。
                            (value != null && value.equals(v)))
                        return true;
                }
            }
        }
        return false;      //没找到就返回false；
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
     * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     *
     * //返回包含这个映射中键值对包含的所有的键的集合视图。这个集合是受到这个map
     * 的影响的，所以，如果对这个hashmap 做修改是会影响到这个视图的。如果
     * 在Iteration迭代器迭代这个视图时，这个映射发生了结构性的修改(除非是迭代
     * 器自己的remove操作)，那么迭代的结果是不确定的。这个集合支持元素删除，通过
     * 迭代器Iterator.remove，Set.remove,removeAll,retainAll,clear操作都可以移除映射
     * 中相对应的键值对关系。但是它不支持add或者addAll操作。
     *
     * @return a set view of the keys contained in this map
     */
    public Set<K> keySet() {            //keySet方法，返回含有所有key的集合set
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new KeySet();     //这个类是下面定义的内部类，继承AbstractSet。
            keySet = ks;
        }
        return ks;
    }

    final class KeySet extends AbstractSet<K> {
        public final int size()                 { return size; }       //因为键是唯一的，所以key.size就是hashmap.size
        public final void clear()               { HashMap.this.clear(); }          //调用hashmap.clear。
        public final Iterator<K> iterator()     { return new KeyIterator(); }   //key 迭代器
        public final boolean contains(Object o) { return containsKey(o); }      //调用hashmap.contarinsKey.
        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }          //同样是调用上面定义的移除节点的方法removeNode.
        public final Spliterator<K> spliterator() {
            return new KeySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }           //子迭代器。分裂的迭代器。
        public final void forEach(Consumer<? super K> action) {         //对keySett中每个元素都做一个action处理或者抛出一个异常。
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.key);
                }
                if (modCount != mc)       //但是不能改变hashmap的结构
                    throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own <tt>remove</tt> operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     *  此方法返回这个hashmap中所有键值对的值的一个集合视图，
     *  这个视图也受到这个映射的影响，如果改变了hashmap会影响到这个视图。
     *  当迭代器Iterator在遍历这个视图时，如果hashmap结构发生改变（除非是Iterator.remove自己
     *  操作改变的），那么这个视图返回的结果是不确定的。这个集合
     *  同样支持元素修改，通过Iterator.remove，Collection.remove，removeAll,
     * retainAll，clear等等操作都会删除hashmap中对应的键值对。但是它
     * 不支持add和addall操作。
     * @return a view of the values contained in this map
     */
    public Collection<V> values() {      //内部方法value（），返回hashmap中值的结合视图。
        Collection<V> vs = values;
        if (vs == null) {
            vs = new Values();
            values = vs;
        }
        return vs;
    }

    final class Values extends AbstractCollection<V> {
        public final int size()                 { return size; }             //键值对有几个就返回几个值。  因为不是set，所以value允许相等
        public final void clear()               { HashMap.this.clear(); }             //以下解释和 上面keyset类似。不多重复叙述
        public final Iterator<V> iterator()     { return new ValueIterator(); }
        public final boolean contains(Object o) { return containsValue(o); }
        public final Spliterator<V> spliterator() {
            return new ValueSpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super V> action) {      //对每一个value都做action处理，或者返回异常
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.value);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation, or through the
     * <tt>setValue</tt> operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
     * <tt>clear</tt> operations.  It does not support the
     * <tt>add</tt> or <tt>addAll</tt> operations.
     *和上面keySet()以及values()相似意思。因为键值对不允许重复，所以
     * 使用Set而不是Collection。
     * 迭代中如果是通过自己的remove方法，或者通过使用迭代器获得的entry中的
     * setValue方法改变hashmap的值，不会造成此次迭代的结果的不确定性。
     * 同样只支持移除，不支持添加。
     * @return a set view of the mappings contained in this map
     */
    public Set<Map.Entry<K,V>> entrySet() {       //entrySet()内部方法，获得hashmap的所有键值对关系。
        Set<Map.Entry<K,V>> es;
        return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
    }

    final class EntrySet extends AbstractSet<Map.Entry<K,V>> {   //分析类似上面key和values。
        public final int size()                 { return size; }
        public final void clear()               { HashMap.this.clear(); }
        public final Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }
        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            Object key = e.getKey();
            Node<K,V> candidate = getNode(hash(key), key);
            return candidate != null && candidate.equals(e);
        }
        public final boolean remove(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }
        public final Spliterator<Map.Entry<K,V>> spliterator() {
            return new EntrySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super Map.Entry<K,V>> action) {     //对每一个遍历到的entry做一个action处理，或返回异常
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    // Overrides of JDK8 Map extension methods
   //以下内容是对Map方法中的一个重写。并不是hsahmap的常用方法，具体可以在学习Map源码的时候了解。
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? defaultValue : e.value;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return putVal(hash(key), key, value, true, true);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return removeNode(hash(key), key, value, true, true) != null;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        Node<K,V> e; V v;
        if ((e = getNode(hash(key), key)) != null &&
                ((v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
            e.value = newValue;
            afterNodeAccess(e);
            return true;
        }
        return false;
    }

    @Override
    public V replace(K key, V value) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) != null) {
            V oldValue = e.value;
            e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
        return null;
    }

    @Override
    public V computeIfAbsent(K key,
                             Function<? super K, ? extends V> mappingFunction) {
        if (mappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
                (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
            V oldValue;
            if (old != null && (oldValue = old.value) != null) {
                afterNodeAccess(old);
                return oldValue;
            }
        }
        V v = mappingFunction.apply(key);
        if (v == null) {
            return null;
        } else if (old != null) {
            old.value = v;
            afterNodeAccess(old);
            return v;
        }
        else if (t != null)
            t.putTreeVal(this, tab, hash, key, v);
        else {
            tab[i] = newNode(hash, key, v, first);
            if (binCount >= TREEIFY_THRESHOLD - 1)
                treeifyBin(tab, hash);
        }
        ++modCount;
        ++size;
        afterNodeInsertion(true);
        return v;
    }

    public V computeIfPresent(K key,
                              BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        Node<K,V> e; V oldValue;
        int hash = hash(key);
        if ((e = getNode(hash, key)) != null &&
                (oldValue = e.value) != null) {
            V v = remappingFunction.apply(key, oldValue);
            if (v != null) {
                e.value = v;
                afterNodeAccess(e);
                return v;
            }
            else
                removeNode(hash, key, null, false, true);
        }
        return null;
    }

    @Override
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
                (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        V oldValue = (old == null) ? null : old.value;
        V v = remappingFunction.apply(key, oldValue);
        if (old != null) {
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            }
            else
                removeNode(hash, key, null, false, true);
        }
        else if (v != null) {
            if (t != null)
                t.putTreeVal(this, tab, hash, key, v);
            else {
                tab[i] = newNode(hash, key, v, first);
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return v;
    }

    @Override
    public V merge(K key, V value,
                   BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (value == null)
            throw new NullPointerException();
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
                (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        if (old != null) {
            V v;
            if (old.value != null)
                v = remappingFunction.apply(old.value, value);
            else
                v = value;
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            }
            else
                removeNode(hash, key, null, false, true);
            return v;
        }
        if (value != null) {
            if (t != null)
                t.putTreeVal(this, tab, hash, key, value);
            else {
                tab[i] = newNode(hash, key, value, first);
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return value;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Node<K,V>[] tab;
        if (action == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next)
                    action.accept(e.key, e.value);
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Node<K,V>[] tab;
        if (function == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    e.value = function.apply(e.key, e.value);
                }
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    /* ------------------------------------------------------------ */
    // Cloning and serialization

    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        HashMap<K,V> result;
        try {
            result = (HashMap<K,V>)super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
        result.reinitialize();
        result.putMapEntries(this, false);
        return result;
    }

    // These methods are also used when serializing HashSets
    final float loadFactor() { return loadFactor; }
    final int capacity() {
        return (table != null) ? table.length :
                (threshold > 0) ? threshold :
                        DEFAULT_INITIAL_CAPACITY;
    }

    /**
     * Save the state of the <tt>HashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     *             bucket array) is emitted (int), followed by the
     *             <i>size</i> (an int, the number of key-value
     *             mappings), followed by the key (Object) and value (Object)
     *             for each key-value mapping.  The key-value mappings are
     *             emitted in no particular order.
     *///hashmap的IO写入在了解IO源码的时候在补充学习。
    private void writeObject(java.io.ObjectOutputStream s)
            throws IOException {
        int buckets = capacity();
        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();
        s.writeInt(buckets);
        s.writeInt(size);
        internalWriteEntries(s);
    }

    /**
     * Reconstitute the {@code HashMap} instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        // Read in the threshold (ignored), loadfactor, and any hidden stuff
        s.defaultReadObject();
        reinitialize();
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new InvalidObjectException("Illegal load factor: " +
                    loadFactor);
        s.readInt();                // Read and ignore number of buckets
        int mappings = s.readInt(); // Read number of mappings (size)
        if (mappings < 0)
            throw new InvalidObjectException("Illegal mappings count: " +
                    mappings);
        else if (mappings > 0) { // (if zero, use defaults)
            // Size the table using given load factor only if within
            // range of 0.25...4.0
            float lf = Math.min(Math.max(0.25f, loadFactor), 4.0f);
            float fc = (float)mappings / lf + 1.0f;
            int cap = ((fc < DEFAULT_INITIAL_CAPACITY) ?
                    DEFAULT_INITIAL_CAPACITY :
                    (fc >= MAXIMUM_CAPACITY) ?
                            MAXIMUM_CAPACITY :
                            tableSizeFor((int)fc));
            float ft = (float)cap * lf;
            threshold = ((cap < MAXIMUM_CAPACITY && ft < MAXIMUM_CAPACITY) ?
                    (int)ft : Integer.MAX_VALUE);
            @SuppressWarnings({"rawtypes","unchecked"})
            Node<K,V>[] tab = (Node<K,V>[])new Node[cap];
            table = tab;

            // Read the keys and values, and put the mappings in the HashMap
            for (int i = 0; i < mappings; i++) {
                @SuppressWarnings("unchecked")
                K key = (K) s.readObject();
                @SuppressWarnings("unchecked")
                V value = (V) s.readObject();
                putVal(hash(key), key, value, false, false);
            }
        }
    }

    /* ------------------------------------------------------------ */
    // iterators
   //迭代器，在学习迭代器时，在深入学习。
    abstract class HashIterator {
        Node<K,V> next;        // next entry to return
        Node<K,V> current;     // current entry
        int expectedModCount;  // for fast-fail
        int index;             // current slot

        HashIterator() {
            expectedModCount = modCount;
            Node<K,V>[] t = table;
            current = next = null;
            index = 0;
            if (t != null && size > 0) { // advance to first entry
                do {} while (index < t.length && (next = t[index++]) == null);
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Node<K,V> nextNode() {
            Node<K,V>[] t;
            Node<K,V> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            if ((next = (current = e).next) == null && (t = table) != null) {
                do {} while (index < t.length && (next = t[index++]) == null);
            }
            return e;
        }

        public final void remove() {
            Node<K,V> p = current;
            if (p == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            current = null;
            K key = p.key;
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }

    final class KeyIterator extends HashIterator
            implements Iterator<K> {
        public final K next() { return nextNode().key; }
    }

    final class ValueIterator extends HashIterator
            implements Iterator<V> {
        public final V next() { return nextNode().value; }
    }

    final class EntryIterator extends HashIterator
            implements Iterator<Map.Entry<K,V>> {
        public final Map.Entry<K,V> next() { return nextNode(); }
    }

    /* ------------------------------------------------------------ */
    // spliterators
    //分裂迭代器
    static class HashMapSpliterator<K,V> {
        final HashMap<K,V> map;
        Node<K,V> current;          // current node    当前节点。
        int index;                  // current index, modified on advance/split  当前索引
        int fence;                  // one past last index   //最后一个索引
        int est;                    // size estimate  规模估计
        int expectedModCount;       // for comodification checks  //预计的结构修改次数

        HashMapSpliterator(HashMap<K,V> m, int origin,
                           int fence, int est,
                           int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getFence() { // initialize fence and size on first use
            int hi;
            if ((hi = fence) < 0) {
                HashMap<K,V> m = map;
                est = m.size;
                expectedModCount = m.modCount;
                Node<K,V>[] tab = m.table;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            return hi;
        }

        public final long estimateSize() {
            getFence(); // force init
            return (long) est;
        }
    }

    static final class KeySpliterator<K,V>
            extends HashMapSpliterator<K,V>
            implements Spliterator<K> {
        KeySpliterator(HashMap<K,V> m, int origin, int fence, int est,
                       int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public KeySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                    new KeySpliterator<>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        public void forEachRemaining(Consumer<? super K> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.key);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        K k = current.key;
                        current = current.next;
                        action.accept(k);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                    Spliterator.DISTINCT;
        }
    }

    static final class ValueSpliterator<K,V>
            extends HashMapSpliterator<K,V>
            implements Spliterator<V> {
        ValueSpliterator(HashMap<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public ValueSpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                    new ValueSpliterator<>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        public void forEachRemaining(Consumer<? super V> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.value);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        V v = current.value;
                        current = current.next;
                        action.accept(v);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0);
        }
    }

    static final class EntrySpliterator<K,V>
            extends HashMapSpliterator<K,V>
            implements Spliterator<Map.Entry<K,V>> {
        EntrySpliterator(HashMap<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public EntrySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                    new EntrySpliterator<>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        public void forEachRemaining(Consumer<? super Map.Entry<K,V>> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super Map.Entry<K,V>> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        Node<K,V> e = current;
                        current = current.next;
                        action.accept(e);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                    Spliterator.DISTINCT;
        }
    }

    /* ------------------------------------------------------------ */
    // LinkedHashMap support
   //提供给LinkedHashMap用的protected 方法。

    /*
     * The following package-protected methods are designed to be
     * overridden by LinkedHashMap, but not by any other subclass.
     * Nearly all other internal methods are also package-protected
     * but are declared final, so can be used by LinkedHashMap, view
     * classes, and HashSet.
     */

    // Create a regular (non-tree) node       //新建一个普通链表节点
    Node<K,V> newNode(int hash, K key, V value, Node<K,V> next) {   //创建一个新的非树节点。
        return new Node<>(hash, key, value, next);
    }

    // For conversion from TreeNodes to plain nodes
    //从树节点转化为一个普通链表节点
    Node<K,V> replacementNode(Node<K,V> p, Node<K,V> next) {
        return new Node<>(p.hash, p.key, p.value, next);
    }

    // Create a tree bin node        //新建红黑树节点
    TreeNode<K,V> newTreeNode(int hash, K key, V value, Node<K,V> next) {
        return new TreeNode<>(hash, key, value, next);
    }

    // For treeifyBin                //将节点转化为红黑树节点 的方法
    TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
        return new TreeNode<>(p.hash, p.key, p.value, next);
    }

    /**
     * Reset to initial default state.  Called by clone and readObject.
     */      //初始化所有参数。 被clone和readObject调用。
    void reinitialize() {
        table = null;
        entrySet = null;
        keySet = null;
        values = null;
        modCount = 0;
        threshold = 0;
        size = 0;
    }

    // Callbacks to allow LinkedHashMap post-actions
    //提供给linkedHashMap调用。
    void afterNodeAccess(Node<K,V> p) { }
    void afterNodeInsertion(boolean evict) { }
    void afterNodeRemoval(Node<K,V> p) { }

    // Called only from writeObject, to ensure compatible ordering.
    void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
        Node<K,V>[] tab;
        if (size > 0 && (tab = table) != null) {
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    s.writeObject(e.key);
                    s.writeObject(e.value);
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    // Tree bins
   //红黑树容器中的具体方法分析，留在对TreeMap中的方法一起分析。
    //这里只需知道每个方法完成什么作用即可。意在了解hashMap的put,get,remove中
    //调用这些方法做什么就行。
    /**
     * Entry for Tree bins. Extends LinkedHashMap.Entry (which in turn
     * extends Node) so can be used as extension of either regular or
     * linked node.
     * 树节点是继承了LinkedHahsMap的entry，这个entry是继承Node的entry。
     * 所以这个树节点它可以用作常规的或者链表的节点。
     */
    static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
        TreeNode<K,V> parent;  // red-black tree links      //用于红黑树的连接
        TreeNode<K,V> left;              //左孩子节点
        TreeNode<K,V> right;           //右孩子节点
        TreeNode<K,V> prev;    // needed to unlink next upon deletion     //因为删除时会断开next连接，所以使用prev保存前一个。
        boolean red;            //如果为true，说明是红色节点， 为false则为黑色节点
        TreeNode(int hash, K key, V val, Node<K,V> next) {
            super(hash, key, val, next);
        }    //构造函数

        /**
         * Returns root of tree containing this node.
         */
        final TreeNode<K,V> root() {        //找到红黑树节点的根节点。
            for (TreeNode<K,V> r = this, p;;) {
                if ((p = r.parent) == null)
                    return r;
                r = p;
            }
        }

        /**
         * Ensures that the given root is the first node of its bin.
         */
        //确保给定的根是其容器桶中的第一个节点。
        static <K,V> void moveRootToFront(Node<K,V>[] tab, TreeNode<K,V> root) {
            int n;
            if (root != null && tab != null && (n = tab.length) > 0) {
                int index = (n - 1) & root.hash;
                TreeNode<K,V> first = (TreeNode<K,V>)tab[index];
                if (root != first) {
                    Node<K,V> rn;
                    tab[index] = root;
                    TreeNode<K,V> rp = root.prev;
                    if ((rn = root.next) != null)
                        ((TreeNode<K,V>)rn).prev = rp;
                    if (rp != null)
                        rp.next = rn;
                    if (first != null)
                        first.prev = root;
                    root.next = first;
                    root.prev = null;
                }
                assert checkInvariants(root);
            }
        }

        /**
         * Finds the node starting at root p with the given hash and key.
         * The kc argument caches comparableClassFor(key) upon first use
         * comparing keys.
         * 这个方法调用对象一般为根节点root.
         *从调用此方法的节点p(this指代当前调用的对象)开始使用给定的hash和key查找目标节点。
         * kc参数在第一次使用比较键时缓存comparableClassFor(key)。
         *
         */
        final TreeNode<K,V> find(int h, Object k, Class<?> kc) {
            TreeNode<K,V> p = this;
            do {
                int ph, dir; K pk;
                TreeNode<K,V> pl = p.left, pr = p.right, q;
                if ((ph = p.hash) > h)
                    p = pl;
                else if (ph < h)
                    p = pr;
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                else if (pl == null)
                    p = pr;
                else if (pr == null)
                    p = pl;
                else if ((kc != null ||
                        (kc = comparableClassFor(k)) != null) &&
                        (dir = compareComparables(kc, k, pk)) != 0)
                    p = (dir < 0) ? pl : pr;
                else if ((q = pr.find(h, k, kc)) != null)
                    return q;
                else
                    p = pl;
            } while (p != null);
            return null;
        }

        /**
         * Calls find for root node.
         * 根据hash和key 得到对应的红黑树节点。
         */
        final TreeNode<K,V> getTreeNode(int h, Object k) {
            return ((parent != null) ? root() : this).find(h, k, null);      //如果不这个节点不是根节点，需要调用root()返回根节点，再调用find.
        }

        /**
         * Tie-breaking utility for ordering insertions when equal
         * hashCodes and non-comparable. We don't require a total
         * order, just a consistent insertion rule to maintain
         * equivalence across rebalancings. Tie-breaking further than
         * necessary simplifies testing a bit.
         */
        //当hashcode相等但是又不可比较时，不需要一个完整顺序，只需要一个
        //一致的插入规则来维护重平衡之间的等价性。

        //   用这个方法来比较两个对象，返回值要么大于0，要么小于0，不会为0
        //也就是说这一步一定能确定要插入的节点要么是树的左节点，要么是右节点，不然就无法继续满足二叉树结构了。
        //  先比较两个对象的类名，类名是字符串对象，就按字符串的比较规则
        // 如果两个对象是同一个类型，那么调用本地方法为两个对象生成hashCode值，再进行比较，hashCode相等的话返回-1
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null ||    // 这条if条件是必定满足的。
                    (d = a.getClass().getName().
                            compareTo(b.getClass().getName())) == 0)
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ?   //只需定义一个统一的比较规则，用于插入维护平衡即可
                        -1 : 1);
            return d;
        }

        /**
         * Forms tree of the nodes linked from this node.
         * @return root of tree
         * 从调用次方法的节点作为根节点，将链表(普通链表转化为红黑树时，
         * 要先转换为双向链表，且Node已经变为TreeNode)转化为红黑树。
         */
        final void treeify(Node<K,V>[] tab) {
            TreeNode<K,V> root = null;
            for (TreeNode<K,V> x = this, next; x != null; x = next) {     //从调用次方法的节点作为起始节点开始遍历
                next = (TreeNode<K,V>)x.next;
                x.left = x.right = null;
                if (root == null) {
                    x.parent = null;
                    x.red = false;
                    root = x;
                }
                else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K,V> p = root;;) {
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h)
                            dir = -1;
                        else if (ph < h)
                            dir = 1;
                        else if ((kc == null &&
                                (kc = comparableClassFor(k)) == null) ||
                                (dir = compareComparables(kc, k, pk)) == 0)
                            dir = tieBreakOrder(k, pk);

                        TreeNode<K,V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0)
                                xp.left = x;
                            else
                                xp.right = x;
                            root = balanceInsertion(root, x);
                            break;
                        }
                    }
                }
            }
            moveRootToFront(tab, root);  ////确保给定的根是其容器桶中的第一个节点。
        }

        /**
         * Returns a list of non-TreeNodes replacing those linked from
         * this node.
         * 返回一个非树节点的列表，替换从该节点开始的树节点。
         * 即将红黑树转化为普通链表
         */
        final Node<K,V> untreeify(HashMap<K,V> map) {
            Node<K,V> hd = null, tl = null;
            for (Node<K,V> q = this; q != null; q = q.next) {
                Node<K,V> p = map.replacementNode(q, null);
                if (tl == null)
                    hd = p;
                else
                    tl.next = p;
                tl = p;
            }
            return hd;
        }

        /**
         * Tree version of putVal.
         * //如果在putVal方法中，根据hash找到的桶的位置是第一个节点是红黑树节点时，
         * 将调用红黑树版本的putTreeVal。即往红黑树中添加此键值对创建的节点。
         * 添加过程若发现了和传入hash和key都相同的节点，则返回该节点，进行value覆盖。
         * 如果没发现就正常添加，并且返回null。
         */
        final TreeNode<K,V> putTreeVal(HashMap<K,V> map, Node<K,V>[] tab,
                                                         int h, K k, V v) {
            Class<?> kc = null;           //key 的类别
            boolean searched = false;
            TreeNode<K,V> root = (parent != null) ? root() : this;
            for (TreeNode<K,V> p = root;;) {
                int dir, ph; K pk;
                if ((ph = p.hash) > h)
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                else if ((kc == null &&
                        (kc = comparableClassFor(k)) == null) ||
                        (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        TreeNode<K,V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&              //如果找到了和输入hash,key相同的节点，直接返回
                                (q = ch.find(h, k, kc)) != null) ||
                                ((ch = p.right) != null &&
                                        (q = ch.find(h, k, kc)) != null))
                            return q;
                    }
                    dir = tieBreakOrder(k, pk);
                }

                TreeNode<K,V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    Node<K,V> xpn = xp.next;
                    TreeNode<K,V> x = map.newTreeNode(h, k, v, xpn);            //用传入的hash,key,创建的一个新节点。
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    xp.next = x;
                    x.parent = x.prev = xp;
                    if (xpn != null)
                        ((TreeNode<K,V>)xpn).prev = x;
                    moveRootToFront(tab, balanceInsertion(root, x));
                    return null;
                }
            }
        }

        /**
         * Removes the given node, that must be present before this call.
         * This is messier than typical red-black deletion code because we
         * cannot swap the contents of an interior node with a leaf
         * successor that is pinned by "next" pointers that are accessible
         * independently during traversal. So instead we swap the tree
         * linkages. If the current tree appears to have too few nodes,
         * the bin is converted back to a plain bin. (The test triggers
         * somewhere between 2 and 6 nodes, depending on tree structure).
         *  移除给定的红黑树的节点，这个节点在调用这个方法之前，必须存在。
         *  这个过程比典型红黑树删除代码复杂很多，（具体过程在TreeMap时再学习）
         *  如果这个节点删除后，出现了节点个数在2~6之间时，会触发untreeify
         *  进行在红黑树结构转化为链表。
         */
        final void removeTreeNode(HashMap<K,V> map, Node<K,V>[] tab,
                                  boolean movable) {
            int n;
            if (tab == null || (n = tab.length) == 0)
                return;
            int index = (n - 1) & hash;
            TreeNode<K,V> first = (TreeNode<K,V>)tab[index], root = first, rl;
            TreeNode<K,V> succ = (TreeNode<K,V>)next, pred = prev;
            if (pred == null)
                tab[index] = first = succ;
            else
                pred.next = succ;
            if (succ != null)
                succ.prev = pred;
            if (first == null)
                return;
            if (root.parent != null)
                root = root.root();
            if (root == null || root.right == null ||
                    (rl = root.left) == null || rl.left == null) {
                tab[index] = first.untreeify(map);  // too small
                return;
            }
            TreeNode<K,V> p = this, pl = left, pr = right, replacement;
            if (pl != null && pr != null) {
                TreeNode<K,V> s = pr, sl;
                while ((sl = s.left) != null) // find successor
                    s = sl;
                boolean c = s.red; s.red = p.red; p.red = c; // swap colors
                TreeNode<K,V> sr = s.right;
                TreeNode<K,V> pp = p.parent;
                if (s == pr) { // p was s's direct parent
                    p.parent = s;
                    s.right = p;
                }
                else {
                    TreeNode<K,V> sp = s.parent;
                    if ((p.parent = sp) != null) {
                        if (s == sp.left)
                            sp.left = p;
                        else
                            sp.right = p;
                    }
                    if ((s.right = pr) != null)
                        pr.parent = s;
                }
                p.left = null;
                if ((p.right = sr) != null)
                    sr.parent = p;
                if ((s.left = pl) != null)
                    pl.parent = s;
                if ((s.parent = pp) == null)
                    root = s;
                else if (p == pp.left)
                    pp.left = s;
                else
                    pp.right = s;
                if (sr != null)
                    replacement = sr;
                else
                    replacement = p;
            }
            else if (pl != null)
                replacement = pl;
            else if (pr != null)
                replacement = pr;
            else
                replacement = p;
            if (replacement != p) {
                TreeNode<K,V> pp = replacement.parent = p.parent;
                if (pp == null)
                    root = replacement;
                else if (p == pp.left)
                    pp.left = replacement;
                else
                    pp.right = replacement;
                p.left = p.right = p.parent = null;
            }

            TreeNode<K,V> r = p.red ? root : balanceDeletion(root, replacement);

            if (replacement == p) {  // detach
                TreeNode<K,V> pp = p.parent;
                p.parent = null;
                if (pp != null) {
                    if (p == pp.left)
                        pp.left = null;
                    else if (p == pp.right)
                        pp.right = null;
                }
            }
            if (movable)
                moveRootToFront(tab, r);
        }

        /**
         * Splits nodes in a tree bin into lower and upper tree bins,
         * or untreeifies if now too small. Called only from resize;
         * see above discussion about split bits and indices.
         *将红黑树先分为2个子树，该方法只在resize扩容时调用，即
         * 其中一个子树的hash经过新散列还是分配到原来索引位置桶中，
         * 而另一个子树是分配在索引位置+oldCap(旧容器大小)位置，
         * 再对2个子树进行判断，如果长度小于等于指定的6，就退化为链表结构。
         * @param map the map
         * @param tab the table for recording bin heads
         * @param index the index of the table being split
         * @param bit the bit of hash to split on
         */
        final void split(HashMap<K,V> map, Node<K,V>[] tab, int index, int bit) {
            TreeNode<K,V> b = this;
            // Relink into lo and hi lists, preserving order
            TreeNode<K,V> loHead = null, loTail = null;
            TreeNode<K,V> hiHead = null, hiTail = null;
            int lc = 0, hc = 0;
            for (TreeNode<K,V> e = b, next; e != null; e = next) {
                next = (TreeNode<K,V>)e.next;
                e.next = null;
                if ((e.hash & bit) == 0) {
                    if ((e.prev = loTail) == null)
                        loHead = e;
                    else
                        loTail.next = e;
                    loTail = e;
                    ++lc;
                }
                else {
                    if ((e.prev = hiTail) == null)
                        hiHead = e;
                    else
                        hiTail.next = e;
                    hiTail = e;
                    ++hc;
                }
            }

            if (loHead != null) {
                if (lc <= UNTREEIFY_THRESHOLD)
                    tab[index] = loHead.untreeify(map);
                else {
                    tab[index] = loHead;
                    if (hiHead != null) // (else is already treeified)
                        loHead.treeify(tab);
                }
            }
            if (hiHead != null) {
                if (hc <= UNTREEIFY_THRESHOLD)
                    tab[index + bit] = hiHead.untreeify(map);
                else {
                    tab[index + bit] = hiHead;
                    if (loHead != null)
                        hiHead.treeify(tab);
                }
            }
        }

        /* ------------------------------------------------------------ */
        // Red-black tree methods, all adapted from CLR
         //左旋，在重新平衡红黑树时调用
        static <K,V> TreeNode<K,V> rotateLeft(TreeNode<K,V> root,
                                                                TreeNode<K,V> p) {
            TreeNode<K,V> r, pp, rl;
            if (p != null && (r = p.right) != null) {
                if ((rl = p.right = r.left) != null)
                    rl.parent = p;
                if ((pp = r.parent = p.parent) == null)
                    (root = r).red = false;
                else if (pp.left == p)
                    pp.left = r;
                else
                    pp.right = r;
                r.left = p;
                p.parent = r;
            }
            return root;
        }
      //右旋，在重新平衡红黑树时调用
        static <K,V> TreeNode<K,V> rotateRight(TreeNode<K,V> root,
                                                                 TreeNode<K,V> p) {
            TreeNode<K,V> l, pp, lr;
            if (p != null && (l = p.left) != null) {
                if ((lr = p.left = l.right) != null)
                    lr.parent = p;
                if ((pp = l.parent = p.parent) == null)
                    (root = l).red = false;
                else if (pp.right == p)
                    pp.right = l;
                else
                    pp.left = l;
                l.right = p;
                p.parent = l;
            }
            return root;
        }
      //红黑树在插入一个节点后，开始自平衡 红黑树结构。在putTreeVal和treeify中调用。
        static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root,
                                                                      TreeNode<K,V> x) {
            x.red = true;
            for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {
                if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                }
                else if (!xp.red || (xpp = xp.parent) == null)
                    return root;
                if (xp == (xppl = xpp.left)) {
                    if ((xppr = xpp.right) != null && xppr.red) {
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    }
                    else {
                        if (x == xp.right) {
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                }
                else {
                    if (xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    }
                    else {
                        if (x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }
            //删除一个红黑树节点后，重新平衡红黑树的结构。
        static <K,V> TreeNode<K,V> balanceDeletion(TreeNode<K,V> root,
                                                                     TreeNode<K,V> x) {
            for (TreeNode<K,V> xp, xpl, xpr;;)  {
                if (x == null || x == root)
                    return root;
                else if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                }
                else if (x.red) {
                    x.red = false;
                    return root;
                }
                else if ((xpl = xp.left) == x) {
                    if ((xpr = xp.right) != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    if (xpr == null)
                        x = xp;
                    else {
                        TreeNode<K,V> sl = xpr.left, sr = xpr.right;
                        if ((sr == null || !sr.red) &&
                                (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        }
                        else {
                            if (sr == null || !sr.red) {
                                if (sl != null)
                                    sl.red = false;
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ?
                                        null : xp.right;
                            }
                            if (xpr != null) {
                                xpr.red = (xp == null) ? false : xp.red;
                                if ((sr = xpr.right) != null)
                                    sr.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                }
                else { // symmetric
                    if (xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (xpl == null)
                        x = xp;
                    else {
                        TreeNode<K,V> sl = xpl.left, sr = xpl.right;
                        if ((sl == null || !sl.red) &&
                                (sr == null || !sr.red)) {
                            xpl.red = true;
                            x = xp;
                        }
                        else {
                            if (sl == null || !sl.red) {
                                if (sr != null)
                                    sr.red = false;
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ?
                                        null : xp.left;
                            }
                            if (xpl != null) {
                                xpl.red = (xp == null) ? false : xp.red;
                                if ((sl = xpl.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }

        /**
         * Recursive invariant check
         */
        static <K,V> boolean checkInvariants(TreeNode<K,V> t) {
            TreeNode<K,V> tp = t.parent, tl = t.left, tr = t.right,
                    tb = t.prev, tn = (TreeNode<K,V>)t.next;
            if (tb != null && tb.next != t)
                return false;
            if (tn != null && tn.prev != t)
                return false;
            if (tp != null && t != tp.left && t != tp.right)
                return false;
            if (tl != null && (tl.parent != t || tl.hash > t.hash))
                return false;
            if (tr != null && (tr.parent != t || tr.hash < t.hash))
                return false;
            if (t.red && tl != null && tl.red && tr != null && tr.red)
                return false;
            if (tl != null && !checkInvariants(tl))
                return false;
            if (tr != null && !checkInvariants(tr))
                return false;
            return true;
        }
    }

}
