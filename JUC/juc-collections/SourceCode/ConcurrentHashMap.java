package JUC.JUCCollections;
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

import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

/**
 * 一个支持完全并发的哈希表。此类遵循 Hashtable 同样的功能规范，并包含
 * Hashtable 中每个方法对应的版本，而且所有的操作都是线程安全的，
 * 检索操作不需要锁，不支持锁定整个哈希表进行访问数据的操作。
 *
 * 检索操作（包括 get）通常不会阻塞，因此可能与更新操作（包括 put 和
 * remove）重叠。检索会反应最近完成的更新操作在开始时的结果。
 * （更一般地，任何对指定 key 的检索和对该 key 的操作都是 happens-before
 * 关系）对于诸如 putAll 和 clear 这样的聚合操作，并发检索可能只反应
 * 某些条目的插入和删除。
 * 迭代器一次只能被一个线程使用。聚合方法（包括 size，isEmpty，containsValue）
 * 的结果通常只有在映射没有在其他线程中进行并发更新才有用。
 *
 * 当有太多冲突时，表将被动态扩展。
 * 扩容可能是一个相对缓慢的操作。在可能的情况下，构造时为 initialCapacity
 * 提供一个合适的值是最好的。加载因子 loadFactor 参数提供了定制初始表
 * 容量的更进一步方法，它指定了用于计算给定元素数量要分配的空间量的
 * 表密度。
 *
 * 所有方法的所有参数都必须是非 null 的。
 *
 * 此类是 Java Collections Framework 的成员。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V>
        implements ConcurrentMap<K,V>, Serializable {
    /* ---------------- Constants -------------- */

    /**
     * 最大容量。此值为 2 的幂，且只能为 1 << 30。int 值的高两位 bits
     * 用来控制。
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 默认初始容量，必须是 2 的幂。
     */
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * 数组可能的最大容量（不一定是 2 的幂）。
     * 用在 toArray 和相关方法中。
     */
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * 默认的并发级别。此字段不再使用，仅仅为了兼容以前的版本。
     */
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     * 加载因子。实际使用 n - (n >>> 2)。
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * 桶内数据结构从链式结构转变成树形结构的阈值。
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * 树形结构转变为链式结构的阈值。
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * 只有当桶的个数大于此常量的值时，才有可能将链式结构转变成树形结构。
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * 每个转移步骤最小转移节点数。范围被细分以允许多个扩容线程。此值
     * 用作下限，以避免扩容时遭遇过多的内存竞争。此值应该至少为
     * DEFAULT_CAPACITY 大小。
     */
    private static final int MIN_TRANSFER_STRIDE = 16;

    /**
     * sizeCtl 中记录 stamp 的位数。
     * 32 位数组应该至少为 6。
     */
    private static int RESIZE_STAMP_BITS = 16;

    /**
     * 帮助扩容的最大线程数。
     * 必须适应 32 - RESIZE_STAMP_BITS。
     */
    private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

    /**
     * size 在 sizeCtl 中的偏移量。
     */
    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

    /*
     * Encodings for Node hash fields. See above for explanation.
     */
    static final int MOVED     = -1; // hash for forwarding nodes
    static final int TREEBIN   = -2; // hash for roots of trees
    static final int RESERVED  = -3; // hash for transient reservations
    static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

    /** 系统 CPU 个数 */
    static final int NCPU = Runtime.getRuntime().availableProcessors();

    /** For serialization compatibility. */
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("segments", Segment[].class),
            new ObjectStreamField("segmentMask", Integer.TYPE),
            new ObjectStreamField("segmentShift", Integer.TYPE)
    };

    /* ---------------- Nodes -------------- */

    /**
     * key 和 value 都不允许为 null。
     */
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        volatile V val;
        volatile Node<K,V> next;

        Node(int hash, K key, V val, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.val = val;
            this.next = next;
        }

        public final K getKey()       { return key; }
        public final V getValue()     { return val; }
        public final int hashCode()   { return key.hashCode() ^ val.hashCode(); }
        public final String toString(){ return key + "=" + val; }
        public final V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        public final boolean equals(Object o) {
            Object k, v, u; Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    (k == key || k.equals(key)) &&
                    (v == (u = val) || v.equals(u)));
        }

        /**
         * 在链表中查找指定 key。为  map.get 提供支持。
         */
        //从当前节点开始，向后遍历找到指定的key相等的节点。
        Node<K,V> find(int h, Object k) {
            Node<K,V> e = this;
            if (k != null) {
                do {
                    K ek;
                    if (e.hash == h &&
                            ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                } while ((e = e.next) != null);
            }
            return null;
        }
    }

    /* ---------------- Static utilities -------------- */

    /**
     * spread 计算 hash 值。
     * HASH_BITS 首位为 0，其他为 1，是为了让 hash 值为正数
     * 在后续语句中将会使用：
     * index = spread(key) & (length - 1);
     */
    static final int spread(int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }

    /**
     * 返回比指定值大的最小的 2 的幂。
     */
    private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /**
     * x可比较的话，返回x的可比较类型
     */
    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            Class<?> c; Type[] ts, as; Type t; ParameterizedType p;
            if ((c = x.getClass()) == String.class) // bypass checks
                return c;
            if ((ts = c.getGenericInterfaces()) != null) {
                for (int i = 0; i < ts.length; ++i) {
                    if (((t = ts[i]) instanceof ParameterizedType) &&
                            ((p = (ParameterizedType)t).getRawType() ==
                                    Comparable.class) &&
                            (as = p.getActualTypeArguments()) != null &&
                            as.length == 1 && as[0] == c) // type arg is c
                        return c;
                }
            }
        }
        return null;
    }

    /**
     * Returns k.compareTo(x) if x matches kc (k's screened comparable
     * class), else 0.
     */
    @SuppressWarnings({"rawtypes","unchecked"}) // for cast to Comparable
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 :
                ((Comparable)k).compareTo(x));
    }

    /* ---------------- Table element access -------------- */

    /**
     * CAS 操作访问或自改 tab 指定位置的值
     */

    // 获取 tab 指定位置的 Node
    @SuppressWarnings("unchecked")
    static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
        return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
    }

    // CAS 模式将 tab 指定位置设置成指定值
    static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                        Node<K,V> c, Node<K,V> v) {
        return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
    }

    // 不使用 CAS，直接将指定位置设置成指定值
    static final <K,V> void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v) {
        U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
    }

    /* ---------------- Fields -------------- */

    /**
     * 保存节点的数组，数组的每个位置称为一个桶。在第一次插入节点的时候
     * 懒加载。数组长度总是 2 的幂。
     */
    transient volatile Node<K,V>[] table;

    /**
     * 需要用到的临时 nextTable；只有在扩容时才不为 null。
     */
    private transient volatile Node<K,V>[] nextTable;

    /**
     * 基础计数值，主要在没有竞争时使用，但也可作为 table 初始化竞争的回退。
     * 通过 CAS 方式更新。
     */
    private transient volatile long baseCount;

    /**
     * table 初始化和扩容控制。当为负值时， table 被初始化或扩容：-1 时初始化，
     * 或者为 -（1 + 活跃的扩容线程数）。否则，当 table 为 null 时，保留创建时
     * 使用的初始的 table 大小，默认为 0。初始化之后，保存下一个元素的 count
     * 值，根据该值调整表的大小。
     * 简言之，控制表的初始化和扩容操作。
     * sizeCtl = -1 表示 table 正在初始化
     * sizeCtl = 0 默认值
     * sizeCtl > 0 下次扩容的阈值
     * sizeCtl = (resizeStamp << 16) + (1 + nThreads)，表示正在进行扩容，高位存储扩容邮戳，低位存储扩容线程数加 1；
     * 初始化数组或扩容完成后，将 sizeCtl 的值设为 0.75 * n
     */
    private transient volatile int sizeCtl;

    /**
     * 节点转移时下一个需要转移的 table 索引。
     * 扩容时用到，初始时为 table.length，表示从索引 0 到 transferIndex 的
     * 节点还未转移。
     */
    private transient volatile int transferIndex;

    /**
     * 自旋锁（通过 CAS 锁定），用于扩容和创建反单元格。
     */
    private transient volatile int cellsBusy;

    /**
     * 保存 Table 中的每个节点的元素个数。非空时，数组大小为 2 的幂。
     */
    private transient volatile CounterCell[] counterCells;

    // views
    private transient KeySetView<K,V> keySet;
    private transient ValuesView<K,V> values;
    private transient EntrySetView<K,V> entrySet;


    /* ---------------- Public operations -------------- */

    /**
     * 默认构造函数
     */
    public ConcurrentHashMap() {
    }

    /**
     * 构造函数
     *
     * @param initialCapacity The implementation performs internal
     * sizing to accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity of
     * elements is negative
     */
    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException();
        int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?  //如果指定容量大于等于2的30次方，
                MAXIMUM_CAPACITY :        //则使用默认容量
                tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));  //否则将指定容量扩大1.5倍+1。然后调用tableSizeFor
        this.sizeCtl = cap;    //构造函数中sizeCtl用于记录初始化容量。
    }

    /**
     * 构造一个和指定 Map 中所有元素相同的 ConcurrentHashMap。
     *
     * @param m the map
     */
    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.sizeCtl = DEFAULT_CAPACITY; //未指定，则使用指定的默认容量16
        putAll(m);
    }

    /**
     * 根据指定参数构造 ConcurrentHashMap。
     *
     * @param initialCapacity the initial capacity. The implementation
     * performs internal sizing to accommodate this many elements,
     * given the specified load factor.
     * @param loadFactor the load factor (table density) for
     * establishing the initial table size
     * @throws IllegalArgumentException if the initial capacity of
     * elements is negative or the load factor is nonpositive
     *
     * @since 1.6
     */
    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }

    /**
     * 根据指定参数构造 ConcurrentHashMap。
     *
     * @param initialCapacity the initial capacity. The implementation
     * performs internal sizing to accommodate this many elements,
     * given the specified load factor.
     * @param loadFactor the load factor (table density) for
     * establishing the initial table size
     * @param concurrencyLevel the estimated number of concurrently
     * updating threads. The implementation may use this value as
     * a sizing hint.
     * @throws IllegalArgumentException if the initial capacity is
     * negative or the load factor or concurrencyLevel are
     * nonpositive
     */
    public ConcurrentHashMap(int initialCapacity,
                             float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (initialCapacity < concurrencyLevel)   // Use at least as many bins //指定容量如果小于指定并发数
            initialCapacity = concurrencyLevel;   // as estimated threads//会被赋值为并发数
        long size = (long)(1.0 + (long)initialCapacity / loadFactor);  //需要将指定的容量除以传入的loadFactor+1
        int cap = (size >= (long)MAXIMUM_CAPACITY) ?    //判断该size是否超过最大的容量，
                MAXIMUM_CAPACITY : tableSizeFor((int)size); //超出使用MAXIMUM_CAPACITY，反正调用tableSizeFor
        this.sizeCtl = cap;   //构造函数中sizeCtl存储初始容量。
    }

    // Original (since JDK1.2) Map methods

    /**
     * 返回大小
     */
    public int size() {
        long n = sumCount();
        return ((n < 0L) ? 0 :
                (n > (long)Integer.MAX_VALUE) ? Integer.MAX_VALUE :
                        (int)n);
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return sumCount() <= 0L; // ignore transient negative values
    }

    /**
     * 返回指定 key 对应的 value。如果不包含指定 key，返回 null。
     *
     * @throws NullPointerException if the specified key is null
     */
    public V get(Object key) {
        Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
        // 计算 hash 值
        int h = spread(key.hashCode());
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (e = tabAt(tab, (n - 1) & h)) != null) {    //e取到table中key应该被散列到的位置
            //找到了 key 对应的映射
            if ((eh = e.hash) == h) {     //如果该位置上的节点与指定节点结算出的hash相等
                if ((ek = e.key) == key || (ek != null && key.equals(ek)))   //==或者调用key的equals方法判断。
                    return e.val;  //找到就返回e的val值
            }
            // 如果头结点的 hash 小于 0，说明正在扩容，或者该位置是红黑树
            else if (eh < 0)
                // ForwardingNode.find(int h, Object k)，TreeBin.find(int h, Object k)，Node.find(int h, Object k)
                return (p = e.find(h, key)) != null ? p.val : null;
            // 遍历链表，找到就返回。
            while ((e = e.next) != null) {
                if (e.hash == h &&
                        ((ek = e.key) == key || (ek != null && key.equals(ek))))
                    return e.val;
            }
        }
        return null;
    }

    /**
     * 检查指定 table 是否包含指定 key。
     *
     * @param  key possible key
     * @return {@code true} if and only if the specified object
     *         is a key in this table, as determined by the
     *         {@code equals} method; {@code false} otherwise
     * @throws NullPointerException if the specified key is null
     */
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    /**
     * 如果此 Map 中包含一个或多个 key 指向执行的 value，则返回 true。
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the
     *         specified value
     * @throws NullPointerException if the specified value is null
     */
    public boolean containsValue(Object value) {
        if (value == null)
            throw new NullPointerException();
        Node<K,V>[] t;
        if ((t = table) != null) {
            // 遍历所有节点
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                V v;
                if ((v = p.val) == value || (v != null && value.equals(v)))
                    return true;
            }
        }
        return false;
    }

    /**
     * 将指定 key 到指定 value 的映射加入到此 table 中。key 和 value 都不能
     * 为 null。
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}
     * @throws NullPointerException if the specified key or value is null
     */
    public V put(K key, V value) {
        return putVal(key, value, false);
    }

    /** put 和 putIfAbsent 的具体实现 */
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();//key和value不能为空
        // 计算 hash 值
        int hash = spread(key.hashCode());
        int binCount = 0;
        // 自旋
        for (Node<K,V>[] tab = table;;) {
            // f：找到的索引处的节点
            // n：table.length
            // i：新节点的索引
            // fh：f.hash
            Node<K,V> f; int n, i, fh;
            // 如果 table 为 null，先初始化
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();  //同样是自旋加CAS确保一个线程在初始化
                // i 位置为 null，直接插入
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) { //从主内存中取出底层table上指定key应该散列的索引位置上的节点，
                // 如果取得的节点为null，即为空桶，则CAS 方式插入节点，成功则跳出循环
                if (casTabAt(tab, i, null,
                        new Node<K,V>(hash, key, value, null)))    //CAS新建节点放入当前桶中。
                    break;                   // no lock when adding to empty bin   插入完毕退出自旋，可以看到空桶插入不需要加锁，只要CAS操作。
            }
            // 如果取得的桶的第一个节点处于 MOVED (-1)状态，说明正在进行扩容
            // 转移完了再继续自旋
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
                // 执行到这里说明 f 是该位置的头结点，而且不为空
            else {
                V oldVal = null;
                // 非空桶，则对 f 加锁，即对这个桶上的头节点进行加锁
                synchronized (f) {
                    // 监测 i 位置是否还是 f，如果是 f 才能进行后续操作，否则继续循环
                    if (tabAt(tab, i) == f) {
                        // f.hash >= 0，说明是该桶是链式结构。f是链表头结点
                        // 对 f 开启的链表进行遍历
                        if (fh >= 0) {
                            binCount = 1;    //用于记录桶上的节点个数，有一个头节点了所以先加1
                            for (Node<K,V> e = f;; ++binCount) {   //链表遍历的for循环
                                K ek;
                                // 如果某个节点 e 的 hash 值与指定的 hash 值相等，则修改
                                // 这个节点的 value，然后跳出遍历循环
                                if (e.hash == hash &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;         //保存旧值
                                    if (!onlyIfAbsent)    //如果未开启缺省才填入，则
                                        e.val = value;   //覆盖值
                                    break;      //退出自旋
                                }
                                Node<K,V> pred = e;
                                // 如果遍历到尾节点了还没有找到，则把当前的键值对插入
                                // 到链表尾部
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key,
                                            value, null);
                                    break;
                                }
                            }
                        }
                        // 如果节点是 TreeBin 类型的节点，说明该桶内是红黑树，
                        // 调用红黑树节点的 putTreeVal 方法进行插入操作。
                        else if (f instanceof TreeBin) {
                            Node<K,V> p;
                            binCount = 2;
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                    value)) != null) {     //如果该方法返回null，说明插入了新节点，反之进行值覆盖
                                oldVal = p.val;   //保存旧值用于返回
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                //到这里插入已经结束，锁已经释放，但是要判断链表是否需要转换为红黑树，
                // 插入链表操作中已经统计桶中节点个数，此处判断，如果超过阈值，
                // 调用 treeifyBin 将链表转化为红黑树
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)   //如果大于了8个节点，则调用treeifyBin
                        treeifyBin(tab, i);  //i为桶的位置。
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        //到这里，说明没有发生值覆盖，因为值覆盖会直接返回旧值，
        // 说明put进的是新节点，所以调用 addCount 更新元素数量
        addCount(1L, binCount);
        return null;
    }

    /**
     * 将所有的映射从指定的 map 中复制到此 map 中。如果此 map 中的相同映射
     * 会被覆盖。
     *
     * @param m mappings to be stored in this map
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        tryPresize(m.size());
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            putVal(e.getKey(), e.getValue(), false);
    }

    /**
     * 从此 map 中删除指定 key 对应的映射。如果指定 key 不在此 map 中，不做
     * 任何操作。
     *
     * @param  key the key that needs to be removed
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}
     * @throws NullPointerException if the specified key is null
     */
    public V remove(Object key) {
        return replaceNode(key, null, null);
    }

    /**
     * 四个删除/替代方法的支撑函数：将指定 key 对应的 value 替换成指定的
     * value，或者删除节点
     */
    final V replaceNode(Object key, V value, Object cv) {
        int hash = spread(key.hashCode());
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            // 如果 table 不存在或者 i 位置不存在任何节点，直接跳出循环
            if (tab == null || (n = tab.length) == 0 ||
                    (f = tabAt(tab, i = (n - 1) & hash)) == null)
                break;
                // 如果该节点正在被其他线程转移（扩容），则此线程也帮助扩容，返回扩容的新表到他tab,然后重新自旋。
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                V oldVal = null;  //用于返回的旧值
                boolean validated = false;
                synchronized (f) {    //会对当前桶加锁。
                    // 确认，防止修改完成后其他线程继续修改
                    if (tabAt(tab, i) == f) {
                        // 当前为链表结构
                        if (fh >= 0) {
                            validated = true;
                            // 遍历链表
                            for (Node<K,V> e = f, pred = null;;) {
                                K ek;
                                // 如果找到指定 key 对应的映射
                                if (e.hash == hash &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    V ev = e.val;
                                    // 用于适应多个不同的函数调用 replaceNode 方法
                                    if (cv == null || cv == ev ||
                                            (ev != null && cv.equals(ev))) {
                                        oldVal = ev;
                                        // 如果 value 不为 null，则用指定 value 替换该节点的 value
                                        if (value != null)
                                            e.val = value;
                                            // value 等于 null 且 pred 不为 null，将前面节点的next赋值到后一个节点，跳过该节点，即删除找到的节点
                                        else if (pred != null)
                                            pred.next = e.next;
                                            // value 等于 null 且 pred 为 null，直接令桶为null，删除找到的节点
                                        else
                                            setTabAt(tab, i, e.next);
                                    }
                                    break;   //操作成功，直接退出自旋。
                                }
                                pred = e; //更新pred。
                                if ((e = e.next) == null)  //遍历。如果尾节点已经判断玩完了，那么退出。
                                    break;
                            }
                        }//到这里链表情况结束
                        // 如果 f 所在的桶内是树结构
                        else if (f instanceof TreeBin) {
                            validated = true;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            // 从树结构中找到指定节点
                            if ((r = t.root) != null &&
                                    (p = r.findTreeNode(hash, key, null)) != null) {
                                V pv = p.val;
                                if (cv == null || cv == pv ||
                                        (pv != null && cv.equals(pv))) {
                                    oldVal = pv;
                                    if (value != null)
                                        p.val = value;
                                    else if (t.removeTreeNode(p))  //如果为true，说明树已经太小了，小于6，需要转换为链表结构
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }
                if (validated) { //当前线程确实执行了删除流程。
                    if (oldVal != null) {
                        // 更新元素数量，数量减一
                        if (value == null)//如果是替换
                            addCount(-1L, -1);
                        return oldVal;
                    }
                    break;
                }
            }
        }
        return null;  //没找到就返回null。
    }

    /**
     * 从此 map 中删除所有映射。
     */
    public void clear() {
        long delta = 0L; // negative number of deletions
        int i = 0;
        Node<K,V>[] tab = table;
        while (tab != null && i < tab.length) {
            int fh;
            // 获取 i 位置的节点引用
            Node<K,V> f = tabAt(tab, i);
            if (f == null)
                ++i;
                // 如果 i 位置正在转移
            else if ((fh = f.hash) == MOVED) {
                tab = helpTransfer(tab, f);
                i = 0; // restart
            }
            // 尝试删除
            else {
                synchronized (f) { //删除每一个节点时加锁。
                    if (tabAt(tab, i) == f) {
                        Node<K,V> p = (fh >= 0 ? f :
                                (f instanceof TreeBin) ?
                                        ((TreeBin<K,V>)f).first : null);
                        // delta 统计该桶内的节点个数
                        while (p != null) {
                            --delta;
                            p = p.next;
                        }
                        // 该桶设置为 null
                        setTabAt(tab, i++, null);
                    }
                }
            }
        }
        // 调用 addCount 改变节点个数
        if (delta != 0L)
            addCount(delta, -1);
    }

    /**
     * 返回包含此 map 中所有 key 的集合（Set）视图。此集合由 map 支撑，对
     * map 的任何改变都会反应在此集合中，反之亦然。此集合支持删除，通过
     * Iterator.remove, Set.remove, removeAll, retainAll, 和 clear 操作。
     * 不支持 add 或者 addAll 操作。
     *
     * @return the set view
     */
    public KeySetView<K,V> keySet() {
        KeySetView<K,V> ks;
        return (ks = keySet) != null ? ks : (keySet = new KeySetView<K,V>(this, null));
    }

    /**
     * 返回包含此 map 中所有 value 的集合（Collection）视图。此集合由 map
     * 支撑，对 map 的任何改变都会反应在此集合中，反之亦然。此集合支持
     * 删除，通过Iterator.remove, Set.remove, removeAll, retainAll,
     * 和 clear 操作。不支持 add 或者 addAll 操作。
     *
     * @return the collection view
     */
    public Collection<V> values() {
        ValuesView<K,V> vs;
        return (vs = values) != null ? vs : (values = new ValuesView<K,V>(this));
    }

    /**
     * 返回包含此 map 中所有映射的集合（Set）视图。此集合由 map 支撑，对
     * map 的任何改变都会反应在此集合中，反之亦然。此集合支持删除，通过
     * Iterator.remove, Set.remove, removeAll, retainAll, 和 clear 操作。
     *
     * @return the set view
     */
    public Set<Map.Entry<K,V>> entrySet() {
        EntrySetView<K,V> es;
        return (es = entrySet) != null ? es : (entrySet = new EntrySetView<K,V>(this));
    }

    /**
     * 返回此 Map 的 hash 值，例如，map 中所有键值对的和，
     * key.hashCode() ^ value.hashCode()
     *
     * @return the hash code value for this map
     */
    public int hashCode() {
        int h = 0;
        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; )
                h += p.key.hashCode() ^ p.val.hashCode();
        }
        return h;
    }

    /**
     * 转化成字符串
     *
     * @return a string representation of this map
     */
    public String toString() {
        Node<K,V>[] t;
        int f = (t = table) == null ? 0 : t.length;
        Traverser<K,V> it = new Traverser<K,V>(t, f, 0, f);
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        Node<K,V> p;
        if ((p = it.advance()) != null) {
            for (;;) {
                K k = p.key;
                V v = p.val;
                sb.append(k == this ? "(this Map)" : k);
                sb.append('=');
                sb.append(v == this ? "(this Map)" : v);
                if ((p = it.advance()) == null)
                    break;
                sb.append(',').append(' ');
            }
        }
        return sb.append('}').toString();
    }

    /**
     * 比较此 map 和指定对象是否相等。如果指定对象也是 Map 且和此 Map 有
     * 相同的键值对映射，则返回 true。此方法执行过程中如果有多线程同时修改
     * 此 map，会出现错误结果。
     *
     * @param o object to be compared for equality with this map
     * @return {@code true} if the specified object is equal to this map
     */
    public boolean equals(Object o) {
        if (o != this) {
            if (!(o instanceof Map))
                return false;
            Map<?,?> m = (Map<?,?>) o;
            Node<K,V>[] t;
            int f = (t = table) == null ? 0 : t.length;
            Traverser<K,V> it = new Traverser<K,V>(t, f, 0, f);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                V val = p.val;
                Object v = m.get(p.key);
                if (v == null || (v != val && !v.equals(val)))
                    return false;
            }
            for (Map.Entry<?,?> e : m.entrySet()) {
                Object mk, mv, v;
                if ((mk = e.getKey()) == null ||
                        (mv = e.getValue()) == null ||
                        (v = get(mk)) == null ||
                        (mv != v && !mv.equals(v)))
                    return false;
            }
        }
        return true;
    }

    /**
     * Stripped-down version of helper class used in previous version,
     * declared for the sake of serialization compatibility
     */
    static class Segment<K,V> extends ReentrantLock implements Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        final float loadFactor;
        Segment(float lf) { this.loadFactor = lf; }
    }

    // ConcurrentMap methods

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     *         or {@code null} if there was no mapping for the key
     * @throws NullPointerException if the specified key or value is null
     */
    public V putIfAbsent(K key, V value) {
        return putVal(key, value, true);
    }

    /**
     * 删除指定映射
     *
     * @throws NullPointerException if the specified key is null
     */
    public boolean remove(Object key, Object value) {
        if (key == null)
            throw new NullPointerException();
        return value != null && replaceNode(key, null, value) != null;
    }

    /**
     * 替换指定映射的 value
     *
     * @throws NullPointerException if any of the arguments are null
     */
    public boolean replace(K key, V oldValue, V newValue) {
        if (key == null || oldValue == null || newValue == null)
            throw new NullPointerException();
        return replaceNode(key, newValue, oldValue) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     *         or {@code null} if there was no mapping for the key
     * @throws NullPointerException if the specified key or value is null
     */
    public V replace(K key, V value) {
        if (key == null || value == null)
            throw new NullPointerException();
        return replaceNode(key, value, null);
    }

    // 实现 JDK8 即之后版本的 Map 中定义的接口

    /**
     * 返回指定 key 对应的 value，如果不包含 key 对应的 map，则返回默认值
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the value to return if this map contains
     * no mapping for the given key
     * @return the mapping for the key, if present; else the default value
     * @throws NullPointerException if the specified key is null
     */
    public V getOrDefault(Object key, V defaultValue) {
        V v;
        return (v = get(key)) == null ? defaultValue : v;
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) throw new NullPointerException();
        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                action.accept(p.key, p.val);
            }
        }
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) throw new NullPointerException();
        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                V oldValue = p.val;
                for (K key = p.key;;) {
                    V newValue = function.apply(key, oldValue);
                    if (newValue == null)
                        throw new NullPointerException();
                    if (replaceNode(key, newValue, oldValue) != null ||
                            (oldValue = get(key)) == null)
                        break;
                }
            }
        }
    }

    /**
     * 如果指定的 key 没有其对应的 value，使用给定的映射函数计算 value，
     * 添加到该映射中（除非为 null）。整个函数的调用是自动的，所以每个 key
     * 最多应用一次函数。在进行计算的过程中，其他线程试图对这个 map 的
     * 更新操作可能会阻塞，因此计算应该简短，并且不能尝试更新这个 map 的
     * 其他映射。
     *
     * @param key key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return the current (existing or computed) value associated with
     *         the specified key, or null if the computed value is null
     * @throws NullPointerException if the specified key or mappingFunction
     *         is null
     * @throws IllegalStateException if the computation detectably
     *         attempts a recursive update to this map that would
     *         otherwise never complete
     * @throws RuntimeException or Error if the mappingFunction does so,
     *         in which case the mapping is left unestablished
     */
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (key == null || mappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            // 如果还没有初始化 table，则进行初始化
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
                // 如果该位置为 null，插入一个新的节点
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                Node<K,V> r = new ReservationNode<K,V>();
                synchronized (r) {
                    // 将 i 位置设置为新创建的占位节点 r，表示当前位置已经被占用了
                    if (casTabAt(tab, i, null, r)) {
                        binCount = 1;
                        Node<K,V> node = null;
                        // 创建新节点，将 i 位置设置为新的节点
                        try {
                            if ((val = mappingFunction.apply(key)) != null)
                                node = new Node<K,V>(h, key, val, null);
                        } finally {
                            setTabAt(tab, i, node);
                        }
                    }
                }
                if (binCount != 0)
                    break;
            }
            // 如果节点正在被转移，此线程也开始帮助转移
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
                // 该位置已经存在一个节点
            else {
                boolean added = false;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        // 链表结构
                        if (fh >= 0) {
                            binCount = 1;
                            // 遍历链表，如果链表中存在指定 key 对应的节点，跳出循环
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek; V ev;
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = e.val;
                                    break;
                                }
                                Node<K,V> pred = e;
                                // 到达链表尾部，根据 key 和 value 插入新的节点
                                if ((e = e.next) == null) {
                                    if ((val = mappingFunction.apply(key)) != null) {
                                        added = true;
                                        pred.next = new Node<K,V>(h, key, val, null);
                                    }
                                    break;
                                }
                            }
                        }
                        // 如果当前桶内是树结构
                        else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            // 如果找到指定 key 对应的节点，说明已经存在该节点，
                            // 不做任何操作。否则，插入新的节点
                            if ((r = t.root) != null &&
                                    (p = r.findTreeNode(h, key, null)) != null)
                                val = p.val;
                            else if ((val = mappingFunction.apply(key)) != null) {
                                added = true;
                                t.putTreeVal(h, key, val);
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (!added)
                        return val;
                    break;
                }
            }
        }
        if (val != null)
            addCount(1L, binCount);
        return val;
    }

    /**
     * 如果指定 key 对应的 value 存在，尝试为给定 key 计算新的 value。
     *
     * @param key key with which a value may be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException if the specified key or remappingFunction
     *         is null
     * @throws IllegalStateException if the computation detectably
     *         attempts a recursive update to this map that would
     *         otherwise never complete
     * @throws RuntimeException or Error if the remappingFunction does so,
     *         in which case the mapping is unchanged
     */
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            // table 还没有初始化，先初始化
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
                // 不存在指定 key，跳出循环，直接退出
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null)
                break;
                // 正在转移，此线程帮助转移
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        // 处理链式结构
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f, pred = null;; ++binCount) {
                                K ek;
                                // 如果存在该节点，计算新的 value 值，并将 key 对应节点
                                // 的 value 替换成新的 value 值。
                                // 如果计算出来的新的 value 值为 null，删除 key 对应的节点
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(key, e.val);
                                    // 新的 value 不为 null
                                    if (val != null)
                                        e.val = val;
                                        // 新的 value 为 null
                                    else {
                                        delta = -1;
                                        Node<K,V> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null)
                                    break;
                            }
                        }
                        // 处理树形结构
                        else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            // 同样的方法，先找到 key 对应的节点
                            // 然后根据计算出来的新的 value 值判断应该替换还是应该删除
                            if ((r = t.root) != null &&
                                    (p = r.findTreeNode(h, key, null)) != null) {
                                val = remappingFunction.apply(key, p.val);
                                if (val != null)
                                    p.val = val;
                                else {
                                    delta = -1;
                                    if (t.removeTreeNode(p))
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }
                if (binCount != 0)
                    break;
            }
        }
        if (delta != 0)
            addCount((long)delta, binCount);
        return val;
    }

    /**
     * 为指定的 key 计算新的 value。
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException if the specified key or remappingFunction
     *         is null
     * @throws IllegalStateException if the computation detectably
     *         attempts a recursive update to this map that would
     *         otherwise never complete
     * @throws RuntimeException or Error if the remappingFunction does so,
     *         in which case the mapping is unchanged
     */
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                // 使用 ReservationNode 占位，其它线程无法进入 synchronized 区
                // 甚至无法进入此 else if 区
                Node<K,V> r = new ReservationNode<K,V>();
                synchronized (r) {
                    // 在此 synchronized 区里，计算出新的 val，并创建新的节点放在
                    // i 位置，完成后跳出最外层 for 循环
                    if (casTabAt(tab, i, null, r)) {
                        binCount = 1;
                        Node<K,V> node = null;
                        try {
                            if ((val = remappingFunction.apply(key, null)) != null) {
                                delta = 1;
                                node = new Node<K,V>(h, key, val, null);
                            }
                        } finally {
                            setTabAt(tab, i, node);
                        }
                    }
                }
                if (binCount != 0)
                    break;
            }
            // 帮助节点转移
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                // 上面的 synchronized 区的锁释放后，有可能刚好其他线程进入
                // 此 synchronized
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        // 链式结构
                        if (fh >= 0) {
                            binCount = 1;
                            // 遍历
                            for (Node<K,V> e = f, pred = null;; ++binCount) {
                                K ek;
                                // 找到指定 key 对应节点
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(key, e.val);
                                    // 计算出来的 val 不为 null，直接替换
                                    if (val != null)
                                        e.val = val;
                                        // 计算出来的 val 为 null，删除节点
                                    else {
                                        delta = -1;
                                        Node<K,V> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    // 跳出遍历循环
                                    break;
                                }
                                // 没找到指定 key 对应节点，创建新的节点，添加在链表末尾
                                pred = e;
                                if ((e = e.next) == null) {
                                    val = remappingFunction.apply(key, null);
                                    if (val != null) {
                                        delta = 1;
                                        pred.next =
                                                new Node<K,V>(h, key, val, null);
                                    }
                                    break;
                                }
                            }
                        }
                        // 树结构
                        else if (f instanceof TreeBin) {
                            binCount = 1;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            // 找到 key 对应的节点
                            if ((r = t.root) != null)
                                p = r.findTreeNode(h, key, null);
                            else
                                p = null;
                            V pv = (p == null) ? null : p.val;
                            // 计算新的 value
                            val = remappingFunction.apply(key, pv);
                            // 如果计算出来的 value 不为 null
                            if (val != null) {
                                // 如果 key 存在，替换其对应的 value
                                if (p != null)
                                    p.val = val;
                                    // 如果 key 不存在，创建新的节点
                                else {
                                    delta = 1;
                                    t.putTreeVal(h, key, val);
                                }
                            }
                            // 如果计算出来的 value 为 null，且 p 已经存在，删除 p
                            else if (p != null) {
                                delta = -1;
                                if (t.removeTreeNode(p))
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        }
                    }
                }
                // 应该转变成树结构
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    break;
                }
            }
        }
        if (delta != 0)
            addCount((long)delta, binCount);
        return val;
    }

    /**
     * 如果指定的 key 还没有对应一个非 null 的 value，将其和指定的 value 对应。
     * 否则，将 value 替换成指定的函数计算出来的新的 value，如果计算出来的新的
     * value 为 null，则删除该节点。
     *
     * @param key key with which the specified value is to be associated
     * @param value the value to use if absent
     * @param remappingFunction the function to recompute a value if present
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException if the specified key or the
     *         remappingFunction is null
     * @throws RuntimeException or Error if the remappingFunction does so,
     *         in which case the mapping is unchanged
     */
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (key == null || value == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                if (casTabAt(tab, i, null, new Node<K,V>(h, key, value, null))) {
                    delta = 1;
                    val = value;
                    break;
                }
            }
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f, pred = null;; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(e.val, value);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        Node<K,V> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null) {
                                    delta = 1;
                                    val = value;
                                    pred.next =
                                            new Node<K,V>(h, key, val, null);
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r = t.root;
                            TreeNode<K,V> p = (r == null) ? null :
                                    r.findTreeNode(h, key, null);
                            val = (p == null) ? value :
                                    remappingFunction.apply(p.val, value);
                            if (val != null) {
                                if (p != null)
                                    p.val = val;
                                else {
                                    delta = 1;
                                    t.putTreeVal(h, key, val);
                                }
                            }
                            else if (p != null) {
                                delta = -1;
                                if (t.removeTreeNode(p))
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    break;
                }
            }
        }
        if (delta != 0)
            addCount((long)delta, binCount);
        return val;
    }

    // Hashtable legacy methods

    /**
     * 是否包含指定 value
     *
     * @param  value a value to search for
     * @return {@code true} if and only if some key maps to the
     *         {@code value} argument in this table as
     *         determined by the {@code equals} method;
     *         {@code false} otherwise
     * @throws NullPointerException if the specified value is null
     */
    public boolean contains(Object value) {
        return containsValue(value);
    }

    /**
     * 返回所有 key 的枚举。
     *
     * @return an enumeration of the keys in this table
     * @see #keySet()
     */
    public Enumeration<K> keys() {
        Node<K,V>[] t;
        int f = (t = table) == null ? 0 : t.length;
        return new KeyIterator<K,V>(t, f, 0, f, this);
    }

    /**
     * 返回所有 value 的枚举。
     *
     * @return an enumeration of the values in this table
     * @see #values()
     */
    public Enumeration<V> elements() {
        Node<K,V>[] t;
        int f = (t = table) == null ? 0 : t.length;
        return new ValueIterator<K,V>(t, f, 0, f, this);
    }

    // 专属于 ConcurrentHashMap 的方法

    /**
     * 返回 map 中映射的数量。此方法应该被用来代替 size 方法，因为 ConcurrentHashMap
     * 所包含的映射数量可以超过 int 的范围。返回的值是估计值。
     *
     * @return the number of mappings
     * @since 1.8
     */
    public long mappingCount() {
        long n = sumCount();
        return (n < 0L) ? 0L : n; // ignore transient negative values
    }

    /**
     * 创造一个由 ConcurrentHashMap 支撑的 Set。
     *
     * @param <K> the element type of the returned set
     * @return the new set
     * @since 1.8
     */
    public static <K> KeySetView<K,Boolean> newKeySet() {
        return new KeySetView<K,Boolean>
                (new ConcurrentHashMap<K,Boolean>(), Boolean.TRUE);
    }

    /**
     * 创造一个由 ConcurrentHashMap 支撑的 Set。
     *
     * @param initialCapacity The implementation performs internal
     * sizing to accommodate this many elements.
     * @param <K> the element type of the returned set
     * @return the new set
     * @throws IllegalArgumentException if the initial capacity of
     * elements is negative
     * @since 1.8
     */
    public static <K> KeySetView<K,Boolean> newKeySet(int initialCapacity) {
        return new KeySetView<K,Boolean>
                (new ConcurrentHashMap<K,Boolean>(initialCapacity), Boolean.TRUE);
    }

    /**
     * 返回此 map 中所有 key 的 Set 视图。
     *
     * @param mappedValue the mapped value to use for any additions
     * @return the set view
     * @throws NullPointerException if the mappedValue is null
     */
    public KeySetView<K,V> keySet(V mappedValue) {
        if (mappedValue == null)
            throw new NullPointerException();
        return new KeySetView<K,V>(this, mappedValue);
    }

    /* ---------------- Special Nodes -------------- */
    // 特殊节点类

    /**
     * 在 transfer 操作中，插入桶头部的节点，用于表示该桶已经处理完毕了。
     * 在节点转移的时候用于连接两个 table。
     * 该节点的 hash 值为 MOVED（具体为整型常量 -1）
     */
    static final class ForwardingNode<K,V> extends Node<K,V> {
        final Node<K,V>[] nextTable;
        // fwd 节点的 hash 值为 MOVED
        ForwardingNode(Node<K,V>[] tab) {
            super(MOVED, null, null, null);
            this.nextTable = tab;
        }

        Node<K,V> find(int h, Object k) {
            // loop to avoid arbitrarily deep recursion on forwarding nodes
            outer: for (Node<K,V>[] tab = nextTable;;) {
                Node<K,V> e; int n;
                // table不存在/key不存在/桶内不存在节点，返回 null
                if (k == null || tab == null || (n = tab.length) == 0 ||
                        (e = tabAt(tab, (n - 1) & h)) == null)
                    return null;
                for (;;) {
                    int eh; K ek;
                    // 找到指定 key 对应的节点，返回该节点
                    if ((eh = e.hash) == h &&
                            ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                    if (eh < 0) {
                        if (e instanceof ForwardingNode) {
                            tab = ((ForwardingNode<K,V>)e).nextTable;
                            continue outer;
                        }
                        else
                            return e.find(h, k);
                    }
                    // 到头了还没找到，返回 null
                    if ((e = e.next) == null)
                        return null;
                }
            }
        }
    }

    /**
     * 在 computeIfAbsent 和 compute 函数中使用的站为节点，表示该节点所在的
     * 位置已经被占用了。
     * 此类型节点的 hash 值为 RESERVED。
     */
    static final class ReservationNode<K,V> extends Node<K,V> {
        ReservationNode() {
            super(RESERVED, null, null, null);
        }

        Node<K,V> find(int h, Object k) {
            return null;
        }
    }

    /* ---------------- Table Initialization and Resizing -------------- */
    // table 的初始化和扩容

    /**
     * numberOfLeadingZeros返回最高位非零位置的前面的0的个数。
     * 返回 n（table 大小）的标志位。
     * 使用 RESIZE_STAMP_SHIFT 左移时，必须是负数。
     */
    static final int resizeStamp(int n) {
        return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
    }

    /**
     * 初始化 table。
     * 如果 sizeCtl 小于 0，说明别的数组正在进行初始化，则让出初始化权（yeild）
     * 如果 sizeCtl 大于 0，初始化一个大小为 sizeCtl 的数组，等于 0 初始化一个
     * 默认大小（16）的数组。
     * 然后设置 sizeCtl 的值为数组长度的 3/4
     * Initializes table, using the size recorded in sizeCtl.
     */
    private final Node<K,V>[] initTable() {
        Node<K,V>[] tab; int sc;
        // 自旋，直到table被初始化完毕，即table！=null,且table.length!=0。
        while ((tab = table) == null || tab.length == 0) {
            // CAS只有一个线程会成功执行，所以，只要有线程在初始化，那么sizeCtl一定为-1，
            //则调用yield,让出时间片。
            if ((sc = sizeCtl) < 0)
                Thread.yield();
                // 否则 CAS 设置 sizeCtl 为 -1，表示当前线程正在初始化
                // CAS 是为了让同时到达此处的线程，只有一个能进入这个代码块执行
                // CAS 成功之后，之后其他的线程会被因为 if ((sc = sizeCtl) < 0) 让出时间片，
                // 让初始化线程尽快完成初始化工作。
            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    if ((tab = table) == null || tab.length == 0) {
                        // 如果 sc 大于 0，说明sc存放的是初始容量，则设定sc为初始容量。否则初始容量为默认（16）
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings("unchecked")
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n]; //使用新容量创建新数组nt。
                        table = tab = nt;    //底层数组赋值。
                        // sc = 0.75 * n，即扩容阈值
                        sc = n - (n >>> 2);   //初始化完毕后的扩容阈值
                    }
                } finally {
                    // 初始化 sizeCtl 为 sc
                    sizeCtl = sc;     //阈值进行赋值
                }
                break;     //初始化成功，打断自旋
            }
        }
        return tab;     //返回过程中创建的新表
    }

    /**
     * map 中节点个数的增加或减少。如果 table 太小，而且还没有开始扩容，则开始
     * 扩容。如果已经开始扩容，调用此方法的线程帮助扩容。在扩容之后重新检查
     * 占用情况，看看是否还需要扩容，因为可能又添加了新的内容。
     * 根据参数 check 决定是否检查扩容（其实每次添加节点都会检查）
     *
     * @param x the count to add
     * @param check if < 0, don't check resize, if <= 1 only check if uncontended
     */
    private final void addCount(long x, int check) {
        CounterCell[] as; long b, s;
        // 计算元素个数其实使用了 counterCells 和 baseCount 两个变量
        // 如果 counterCells 为 null，说明之前一直没有出现过冲突，直接尝试CAS将值累加到 baseCount 上
        //这时如果CAS出现竞争，那就要进入if方法体。如果这时发现counterCells数组为null，那调用fullAddCount初始化数组。
        // 如果counterCells一开始就不为null，说明出现过竞争，那么直接进入if方法体
        // 如果当前线程对应位置的 counterCells[i] 上为null，就需要进入fullAddCount，创建新的counterCell实例进行记录。
        // 否则进行CAS更新对应位置的counterCell实例上的值，如果更新失败，，调用 fullAddCount。
        // 只有从未出现过并发冲突的时候，baseCount 才会使用到，一旦出现了并发冲突，
        // 之后所有的操作基本都只针对 CounterCell。
        // （fullAddCount 中如果在初始化容量，也会用到 baseCount）
        if ((as = counterCells) != null ||
                !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
            CounterCell a; long v; int m;
            boolean uncontended = true;
            // 如果 counterCells 不为 null，其长度不为 0，线程通过寻址找到 as 数组中
            // 属于它的 CounterCell 却为 null，直接进入fullAddCount
            // 否则CAS尝试赋值，赋值失败（uncontended赋值为false，出现并发）执行 fullAddCount 方法
            // ThreadLocalRandom 是一个线程私有的随机数生成器，每个线程的 probe
            // 都是不同的，可以认为每个线程的 probe 就是它在 CounterCell 数组中的 hash code
            if (as == null || (m = as.length - 1) < 0 ||
                    (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
                    !(uncontended =
                            U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
                // 在有竞争的时候使用 fullAddCount 计算更新元素数
                fullAddCount(x, uncontended);
                //到这里已经维护好baseCount或者维护好CountCells数组了。
                return;
            }

            if (check <= 1)
                return;
            // sumCount 计算元素总数
            s = sumCount();
        }

        //到这里说明从未出现过冲突，counterCells为null，且CAS操作baseCount成功的线程,此时s=baseCount+x。
        // 或者出现了冲突，但是CAS操作数组中countCell中的value成功了，并且check>1。此时s=sumCount();
        //检查是否扩容。check时传入的binCount，它在putVal 方法中都是大于0的，所以put默认需要检查。
        if (check >= 0) {
            Node<K,V>[] tab, nt; int n, sc;
            // 如果 map 中的节点数达到 sizeCtl（达到扩容阈值），需要扩容。如果
            // table 不为 null 且 table 的长度小于最大值限制，则可以扩容。
            while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
                    (n = tab.length) < MAXIMUM_CAPACITY) {
                // table 长度的标识
                int rs = resizeStamp(n);//扩容标记，标识为原数组长度为n的扩容。
                // 有其他线程在扩容。
                if (sc < 0) {
                    // 满足以下条件之一直接退出循环（通过检验变量是否变化）
                    // 1. sc 的低 16 位不等于标识，说明 sizeCtl 变化了
                    // 2. 此处rs有bug,忘记左移16了。sc == 标识符加 1（扩容结束了，不再有线程进行扩容）（默认
                    // 第一个线程设置 sc ==rs 左移 16 位 + 2，当第一个线程结束扩容了，
                    // 就会将 sc 减一。这个时候，sc 就等于 rs 左移16位+ 1）
                    // 3. sc == 标识符左移16位 + 65535（帮助线程已经达到最大）
                    // 4. nextTable == null，说明当前扩容还没有初始化nextTable，
                    // 5. transferIndex <= 0，不需要线程加入扩容了
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                            sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                            transferIndex <= 0)
                        break;
                    // sizeCtl 加一，表示帮助扩容的线程加一，然后进行扩容。
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                        transfer(tab, nt);  //帮助扩容
                }
                // 没有线程在扩容，则，将 sizeCtl 更新，赋值为标识符左移 16 位（此时为负数）
                // 然后加 2，表示已经有一个线程开始扩容了，然后进行扩容。
                else if (U.compareAndSwapInt(this, SIZECTL, sc,
                        (rs << RESIZE_STAMP_SHIFT) + 2))
                    transfer(tab, null);

                // while循环在最后使用了sumCount，更新了ConcurrentHashMap中节点的总数计算，
                //只有s比阈值小的时候，才会退出，这是确保多线程环境下，其他线程在扩容时，进行put。
                s = sumCount();
            }
        }
    }

    /**
     * 如果 resize 操作正在进行，帮助转移节点 f。
     */
    final Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
        Node<K,V>[] nextTab; int sc;
        // 如果 tab 不为 null，传进来的节点是 ForwardingNode，且 ForwardingNode
        // 的下一个 tab 不为 null
        if (tab != null && (f instanceof ForwardingNode) &&
                (nextTab = ((ForwardingNode<K,V>)f).nextTable) != null) { //即nextTable有值，正在扩容。
            int rs = resizeStamp(tab.length);//获得容量的标识，
            while (nextTab == nextTable && table == tab &&
                    (sc = sizeCtl) < 0) {
                // 不需要帮助转移，跳出循环
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                        sc == rs + MAX_RESIZERS || transferIndex <= 0)
                    break;
                // CAS 更新帮助转移的线程数（+1）
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                    transfer(tab, nextTab);
                    break;
                }
            }
            return nextTab;  //如果帮助扩容完成了，返回新的nextTab，
        }
        return table;//扩容完成，那么返回底层table
    }

    /**
     * 此函数主要用来做扩容前的检查。
     * 进行扩容操作的主要是 transfer 方法。
     *
     * @param size number of elements (doesn't need to be perfectly accurate)
     */
    private final void tryPresize(int size) {
        // 计算扩容后的容量，转化为红黑树中，size在传入的时候已经*2了。
        // c: size 的 1.5 倍，再加 1，再往上取最近的 2 的 n 次方。
        int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
                tableSizeFor(size + (size >>> 1) + 1);
        int sc;
        //只有sizeCtl大于等于0才进入循环，负数退出循环。
        while ((sc = sizeCtl) >= 0) {
            Node<K,V>[] tab = table; int n;
            // 如果 table 还没有初始化，初始化一个容量为 n 的数组。
            // 初始化设置 sizeCtl 为 -1，初始化完成之后将 sizeCtl 设置成数组长度的
            // 3/4
            if (tab == null || (n = tab.length) == 0) {
                n = (sc > c) ? sc : c;  //如果sc保存的初始容量大于c,就使用指定的容量，如果不大于，新数组长度应该为c。
                //CAS设置sizeCtl为-1，同一时间只有一个线程在初始化。
                if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                    try {
                        if (table == tab) {
                            @SuppressWarnings("unchecked")
                            Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                            table = nt;
                            // 数组长度的 3/4，
                            sc = n - (n >>> 2);
                        }
                    } finally {
                        // 阈值设置为数组长度的 3/4
                        sizeCtl = sc;
                    }
                }
            }
            // 如果 table 不为 null
            //且c<=sc,说明不需要扩容了，或者table的长度已经到了最大的
            else if (c <= sc || n >= MAXIMUM_CAPACITY)
                break;
            else if (tab == table) {
                //该值高16位为零，低16位的最高位为1，后面几位记录的是n的二进制中为1的最高位前的零的个数，
                //所以只要扩容前的n是相同的，那么它得到的rs标记也是相同的，说明为同一次扩容。
                int rs = resizeStamp(n);
                // 已经有线程在转移节点
                if (sc < 0) {  //进入循环后，有线程正在进行扩容操作，已经将sc变为负数。
                    Node<K,V>[] nt;
                    // 判断当前线程是否要加入扩容
                    // 1. 根据生成戳判断是否是同一个扩容操作，高 ESIZE_STAMP_BITS
                    // 位生成戳和 rs 相等则代表是同一个 n，表示是同一个扩容操作。
                    // 2 和 3. 判断当前扩容线程数是否已达到最大
                    //该条件有BUG，是错误写法，rs应该是rs<<RESIZE_STAMP_SHIFT
                    // 4 和 5. 确保 transfer 方法初始化完毕
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                            sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                            transferIndex <= 0)
                        break;
                    // 扩容线程数加 1，当前线程加入扩容行列
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                        transfer(tab, nt);   //nt不为null，协助扩容。
                }
                // 当前线程成为第一个开始转移节点的线程，CAS设置sizeCtl的值。
                // 此时 sizeCtl 的高 RESIZE_STAMP_BITS 为生成戳，标识了本次扩容操作。
                // 低 RESIZE_STAMP_SHIFT为扩容线程数+1。
                else if (U.compareAndSwapInt(this, SIZECTL, sc,
                        (rs << RESIZE_STAMP_SHIFT) + 2))
                    transfer(tab, null);   //nextTab为null,表示为第一个扩容的transfer
            }
        }
    }

    /**
     * 移动和/或复制桶里的节点到新的 table 里。
     */
    private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
        int n = tab.length, stride;
        // 确定步长，表示一个线程处理的数组长度，用来控制对 CPU 的使用，
        // 如果Cpu核数只有1，stride为n,如果不为1，则stride = tab.length/(NCPU*8)，最小为 16
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
            stride = MIN_TRANSFER_STRIDE; // subdivide range
        // 如果指定的 nextTab 为空（第一个线程开始扩容），初始化 nextTable
        // 其他线程进来帮忙时，不再创建新的 newTable。
        if (nextTab == null) {            // initiating
            try {
                // 创建一个相当于当前 table 两倍容量的数组，作为新的 table
                @SuppressWarnings("unchecked")
                Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];
                nextTab = nt;
            } catch (Throwable ex) {      // try to cope with OOME
                sizeCtl = Integer.MAX_VALUE;
                return;
            }
            nextTable = nextTab; //初始化完毕，nextTable就不为0，其他线程就可以帮忙转移了。
            transferIndex = n;    //0到transferIndex的位置是需要转移的桶所在的范围。
        }
        int nextn = nextTab.length; //新数组的长度，
        // fwd 是标志节点。当一个节点为空或者被转移之后，就设置为 fwd 节点
        // 表示这个桶已经处理过了
        ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);
        // advance 标志指的是做完了一个位置的迁移工作，可以准备做下一个位置的了
        boolean advance = true;
        // 在完成之前重新扫描一遍数组，确认已经完成。
        boolean finishing = false; // to ensure sweep before committing nextTab
        // 自旋移动每个节点，从 transferIndex 开始移动 stride 个槽的节点到新的
        // table
        // i 表示当前处理的节点索引，bound 表示需要处理节点的索引边界
        for (int i = 0, bound = 0;;) {
            Node<K,V> f; int fh;
            //这个while在线程第一次进入，会进行CAS划分任务，如果可分配却CAS失败，
            // 就会再进while循环，直到得到任务，或者此时扩容任务已全部完成
            //在线程分配完任务后，会进第一个if条件，--i向前遍历，直到i到达bound，
            //到达后，如果当前transferIndex还是大于0，说明还有任务可以分配，
            //所以会进入到CAS中继续分配任务。
            //最后的结果就是
            //每个线程处理的区间为（nextBound, nextIndex）
            while (advance) {
                int nextIndex, nextBound;
                // 首先执行 i = i - 1，如果 i 大于 bound，说明还在当前 stride 范围内
                // nextIndex、nextBound、transferIndex 等都不需要改变
                // bound 是所有线程处理区间的最低点
                if (--i >= bound || finishing)
                    advance = false;
                else if ((nextIndex = transferIndex) <= 0) {
                    i = -1; //-1 是为了进入后面的if判断，说明任务完成。
                    advance = false;
                }
                // CAS更新 transferIndex，每一次transferIndex会减少一个stride，
                // 当前线程处理的桶区间为（nextBound, nextIndex）
                // 如果下一个开始往前遍历的起点是比stride大，说明可以进行一次划分任务，
                //如果小于等于stride，就说明不可划分了，当前线程的i初始会是-1.
                else if (U.compareAndSwapInt
                        (this, TRANSFERINDEX, nextIndex,
                                nextBound = (nextIndex > stride ?
                                        nextIndex - stride : 0))) {
                    bound = nextBound; //bound为一次任务结束的边界，当i到达bound时，说明线程的任务完成了。
                    i = nextIndex - 1;
                    advance = false;
                }
            }
            //如果线程的i为-1，或者有出现扩容冲突，即可能进入到了协助扩容，
            // 但是扩容完成了，并且新的扩容开始了，将会导致i比原来的n要大，并且分配到任务但是在这里退出了。
            if (i < 0 || i >= n || i + n >= nextn) {
                int sc;
                // 已经完成转移，设置 table 为新的 table，更新 sizeCtl 为扩容后的
                // 0.75 倍（原容量的 1.5 倍）并返回
                if (finishing) {   //如果全部协助的线程都已经工作完毕，且sizeCtl和原来的值相等，设置了finnishing，说明扩容完成。
                    nextTable = null;  //nextTable赋值为null，方便下次扩容。
                    table = nextTab;   //底层table赋值为新表。
                    sizeCtl = (n << 1) - (n >>> 1); //设置阈值。
                    return;
                }
                // 当前线程 return 之后可能还有其他线程正在转移
                // 之前我们说过，sizeCtl 在迁移前会设置为 (rs << RESIZE_STAMP_SHIFT) + 2
                //然后，每有一个线程参与迁移就会将 sizeCtl 加 1，
                //这里使用 CAS 操作对 sizeCtl 进行减 1，代表做完了属于自己的任务
                if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                    // sc 初值为 （rs << RESIZE_STAMP_SHIFT) + 2）
                    // 如果还有其他线程正在操作，直接返回，不改变 finishing，
                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                        return;

                    // 到这里，说明 (sc - 2) == resizeStamp(n) << RESIZE_STAMP_SHIFT，
                    // 也就是说，所有的迁移任务都做完了，也就会进入到上面的 if(finishing){} 分支了
                    finishing = advance = true;
                    i = n; // recheck before commit，i会从n到0开始检查一遍。
                }
            }
            // 如果 i 位置节点为 null，那么放入刚刚初始化的 ForwardingNode ”空节点“，
            // 提醒其他线程该位已经处理过了
            else if ((f = tabAt(tab, i)) == null)
                advance = casTabAt(tab, i, null, fwd);
                // 该位置已经处理过了，继续往下
            else if ((fh = f.hash) == MOVED)
                advance = true; // already processed
            else {
                // 处理当前拿到的节点，此处要上锁
                synchronized (f) {
                    // 确认 i 位置仍然是 f，防止其他线程拿到锁进入修改
                    if (tabAt(tab, i) == f) {
                        // ln 保留在原位置，hn 应该移到i + n 位置
                        Node<K,V> ln, hn;
                        // 如果当前为链表节点
                        if (fh >= 0) {
                            // n 为原 table 长度，且为 2 的幂，任何数与 n 进行 & 操作后
                            // 只可能是 0 或者 n。
                            // 根据这个把链表节点分成两类，为 0 说明原来的索引小于 n，
                            // 则位置保持不变，为 n 说明已经超过了原来的 n，新的位置
                            // 应该是 n + i（n 的某一位为 1，如果需要移动，该 bit 位也
                            // 必定为 1，不然将会待在原桶，位置不变）
                            int runBit = fh & n;
                            Node<K,V> lastRun = f;
                            //这个for循环，找到最后一个维持不变的lastRun,
                            //即lastRun后面的节点都是会分到同一个新表中的桶的。
                            for (Node<K,V> p = f.next; p != null; p = p.next) {
                                int b = p.hash & n;
                                // runBit 一直在变化
                                if (b != runBit) {
                                    runBit = b;
                                    lastRun = p;
                                }
                            }
                            // 上面的循环执行完之后，lastRun 及其之后的元素在同一组。
                            // 且 runBit 就是 last 的标识
                            // 如果 runBit 等于 0，则 lastRun 及之后的元素都在原位置
                            // 否则，lastRun 及之后的元素都在新的位置
                            if (runBit == 0) {
                                ln = lastRun;
                                hn = null;
                            }
                            else {
                                hn = lastRun;
                                ln = null;
                            }
                            // 把 f 链表分成两个链表。
                            for (Node<K,V> p = f; p != lastRun; p = p.next) {
                                int ph = p.hash; K pk = p.key; V pv = p.val;
                                // 原位置
                                if ((ph & n) == 0)
                                    ln = new Node<K,V>(ph, pk, pv, ln);
                                    // i + n 位置
                                else
                                    hn = new Node<K,V>(ph, pk, pv, hn);
                            }
                            // 上述循环完成转移之后桶内的顺序并不一定是原来的顺序了
                            // 原因是lastRun后面维持正常顺序，但是头插法会倒序。

                            // 在 nextTab 的 i 位置插入一个链表
                            setTabAt(nextTab, i, ln);
                            // nextTab 的 i + n 位置插入一个链表
                            setTabAt(nextTab, i + n, hn);
                            // table 的 i 位置插入 fwd 节点，表示已经处理过了
                            setTabAt(tab, i, fwd);
                            advance = true;
                        }   //到这里处理完链表节点的一个桶了。
                        // 当前为树节点
                        else if (f instanceof TreeBin) {
                            // f 转为根节点
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            // 低位节点
                            TreeNode<K,V> lo = null, loTail = null;
                            // 高位节点
                            TreeNode<K,V> hi = null, hiTail = null;
                            int lc = 0, hc = 0;  //存放节点个数。用于判断是否新桶中也需要构建红黑树
                            // 从首个节点向后遍历
                            for (Node<K,V> e = t.first; e != null; e = e.next) {
                                int h = e.hash;
                                // 构建新的树节点
                                TreeNode<K,V> p = new TreeNode<K,V>
                                        (h, e.key, e.val, null, null);
                                // 应该放在原位置
                                if ((h & n) == 0) {
                                    if ((p.prev = loTail) == null) //赋值头节点，并设置p.prev
                                        lo = p;
                                    else   //赋值普通节点的next。
                                        loTail.next = p;
                                    loTail = p;
                                    ++lc;
                                }
                                // 应该放在 n + i 位置
                                else {
                                    if ((p.prev = hiTail) == null)
                                        hi = p;
                                    else
                                        hiTail.next = p;
                                    hiTail = p;
                                    ++hc;
                                }
                            }//到这里红黑树已经分裂成2个双向链表，下一步要进行判断：
                            // 扩容后不再需要 tree 结构，转变为链表结构，需要就构建红黑树结构。
                            // 创建 TreeBin 时，其构造函数会把双向链表结构转化成树结构
                            ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                                    (hc != 0) ? new TreeBin<K,V>(lo) : t;
                            hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                                    (lc != 0) ? new TreeBin<K,V>(hi) : t;
                            setTabAt(nextTab, i, ln);  //在新表i位置上，放入新的节点。
                            setTabAt(nextTab, i + n, hn);  //在新表i+n上。放入新节点
                            setTabAt(tab, i, fwd);    //旧表的相应位置。设置为fwd,说明该节点已经处理完毕。
                            advance = true;    //当前节点处理完毕，提示要进行下一个节点处理。
                        }
                    }
                }
            }
        }
    }

    /* ---------------- Counter support -------------- */

    /**
     * A padded cell for distributing counts.  Adapted from LongAdder
     * and Striped64.  See their internal docs for explanation.
     */
    @sun.misc.Contended static final class CounterCell {
        volatile long value;
        CounterCell(long x) { value = x; }
    }

    // 计算所有 counterCells 的元素和（节点总数）
    // 不同的线程操作的是不同的位置，最后把所有位置的和求出来，就是此 Map 的节点数
    final long sumCount() {
        CounterCell[] as = counterCells; CounterCell a;
        long sum = baseCount;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += a.value;
            }
        }
        return sum;
    }

    // 初始化 CounterCells 和更新计数
    private final void fullAddCount(long x, boolean wasUncontended) {
        int h;
        // 为 0 表示该线程的 ThreadLocalRandom 还没有初始化
        if ((h = ThreadLocalRandom.getProbe()) == 0) {
            ThreadLocalRandom.localInit();      // force initialization
            h = ThreadLocalRandom.getProbe();//生成一个非0的代表线程的hash值
            // 非竞争
            wasUncontended = true;
        }
        // 冲突标志
        boolean collide = false;                // True if last slot nonempty
        // 自旋
        for (;;) {
            CounterCell[] as; CounterCell a; int n; long v;
            // counterCells 已经初始化了
            if ((as = counterCells) != null && (n = as.length) > 0) {
                // h 位置的 CounterCell 还没有初始化。
                if ((a = as[(n - 1) & h]) == null) {
                    // 锁空闲（0 表示空闲，1 表示已经被获取），没有正在扩容
                    if (cellsBusy == 0) {            // Try to attach new Cell
                        // 创建新的 CounterCell，将 x 保存在此 CounterCell 中
                        CounterCell r = new CounterCell(x); // Optimistic create
                        // 获取锁（CAS 将 CELLSBUSY 的值变成 1），上锁原因是，反正出现slot覆盖现象导致缺失数据。
                        if (cellsBusy == 0 &&
                                U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                            boolean created = false;
                            // 尝试将创建的 CounterCell 放入 counterCells 数组中，如果
                            // 成功将 created 的值变为 true。
                            try {               // Recheck under lock
                                CounterCell[] rs; int m, j;
                                //重新获取是因为成功获取锁，有可能是因为扩容完成，所以需要更新数组引用。
                                if ((rs = counterCells) != null &&  //如果此时counterCells数组不为空
                                        (m = rs.length) > 0 &&    //取得数组长度m,
                                        rs[j = (m - 1) & h] == null) {    //当前线程散列到数组中的位置还是为null的话
                                    rs[j] = r;       //放入创建好的CounterCell。
                                    created = true;      //创建成功返回true。
                                }
                            } finally {
                                // 最后需要释放锁
                                cellsBusy = 0;
                            }
                            // 如果操作成功，说明本次addcount操作结束，跳出自旋
                            if (created)
                                break;
                            continue;           //如果放入失败，说明这个槽不再为空，则中断本次循环，继续从头开始自旋 Slot is now non-empty
                        }
                    }
                    //获取cellsBusy 的时候没成功，即cellsBusy 为1.
                    // 原因可能是当前cell正在填充，或者数组在扩容，
                    collide = false;
                }
                //到这里说明找到的h位置的CounterCell不为null。注意每一次循环，线程的hash值都会变化。
                // wasUncontended 表示前一次 CAS 更新 cell 单元是否成功
                else if (!wasUncontended)       // CAS already known to fail 如果已经是竞争失败过
                    wasUncontended = true;      //重置为true，因为后面会重新计算hash值
                    // 数组中找到位置非 null。则 CAS 尝试更新它的 value，成功则跳出循环，失败则进入下一个if判断
                else if (U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))
                    break;
                    // 竞争失败的线程到这里检查 counterCells 数组先检查是否已经扩容，如果扩容了，则本次循环退出，需要更新as
                    // 并且再寻址一次（重新循环一次会重新散列，如果数组扩容的话。）
                else if (counterCells != as || n >= NCPU) //但是当couterCells数组长度大于CPU核数时，永远不会扩容，只能重新CAS
                    collide = false;            // At max size or stale
                    // 如果进入这个 else if 块再让线程循环一次，会重新CAS一次，
                    // 如果以上步骤还是发生了collide赋值为false的情况，则还需要重新自旋一次，
                    // 冲突说明数组太小竞争太激烈，需要扩容
                else if (!collide)
                    collide = true;
                    // 到这一步，说明collide还是true，那么就可以扩容，先获取锁，
                    // 如果有其他线程在扩容，或者在放入新cell,此时会先让步。
                else if (cellsBusy == 0 &&
                        U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                    try {
                        // 扩容前的检查，因为数组可能是因为扩容完成才释放了锁已经被其他线程扩容了
                        if (counterCells == as) {// Expand table unless stale
                            CounterCell[] rs = new CounterCell[n << 1];   //容量变为2倍
                            for (int i = 0; i < n; ++i)//简单的循环，传递cell引用。
                                rs[i] = as[i];
                            counterCells = rs;
                        }
                    } finally {
                        // 释放锁
                        cellsBusy = 0;
                    }
                    // 扩容完成后，重新循环
                    collide = false;
                    continue;                   // Retry with expanded table
                }
                h = ThreadLocalRandom.advanceProbe(h); //线程生成新的hash值。
            }
            // counterCells 还没有初始化，获取锁 CELLSBUSY
            else if (cellsBusy == 0 && counterCells == as &&
                    U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                // 标志是否完成初始化
                boolean init = false;
                try {                           // Initialize table
                    if (counterCells == as) {
                        // 初始大小为 2
                        CounterCell[] rs = new CounterCell[2];
                        // 创建 CounterCell 对象
                        rs[h & 1] = new CounterCell(x);
                        counterCells = rs;
                        // 初始化成功
                        init = true;
                    }
                } finally {
                    // 释放锁
                    cellsBusy = 0;
                }
                if (init)
                    break;
            }
            // CounterCells数组为null，但已经有线程在进行初始化，所以获取锁失败，尝试 CAS 更新 baseCount，
            // 即baseCount只有在数组还未初始化或其他线程正在初始化的时候使用到，
            // 成功则跳出循环
            else if (U.compareAndSwapLong(this, BASECOUNT, v = baseCount, v + x))
                break;                          // Fall back on using base
        }
    }

    /* ---------------- Conversion from/to TreeBins -------------- */

    /**
     * 桶内链表结构转化为树结构
     */
    private final void treeifyBin(Node<K,V>[] tab, int index) {
        Node<K,V> b; int n, sc;
        if (tab != null) {
            // 扩容
            if ((n = tab.length) < MIN_TREEIFY_CAPACITY)
                tryPresize(n << 1);
            else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
                synchronized (b) {
                    if (tabAt(tab, index) == b) {
                        TreeNode<K,V> hd = null, tl = null;
                        // 将所有节点转化成 TreeNode 节点
                        for (Node<K,V> e = b; e != null; e = e.next) {
                            TreeNode<K,V> p =
                                    new TreeNode<K,V>(e.hash, e.key, e.val,
                                            null, null);
                            if ((p.prev = tl) == null)
                                hd = p;
                            else
                                tl.next = p;
                            tl = p;
                        }
                        // 将链表结构转化成树结构
                        setTabAt(tab, index, new TreeBin<K,V>(hd));
                    }
                }
            }
        }
    }

    /**
     * 树结构转化成链表结构。
     */
    static <K,V> Node<K,V> untreeify(Node<K,V> b) {
        Node<K,V> hd = null, tl = null;
        for (Node<K,V> q = b; q != null; q = q.next) {
            Node<K,V> p = new Node<K,V>(q.hash, q.key, q.val, null);
            if (tl == null)
                hd = p;
            else
                tl.next = p;
            tl = p;
        }
        return hd;
    }

    /* ---------------- TreeNodes -------------- */

    /**
     * Nodes for use in TreeBins
     */
    static final class TreeNode<K,V> extends Node<K,V> {
        TreeNode<K,V> parent;  // red-black tree links
        TreeNode<K,V> left;
        TreeNode<K,V> right;
        TreeNode<K,V> prev;    // needed to unlink next upon deletion
        boolean red;

        TreeNode(int hash, K key, V val, Node<K,V> next,
                 TreeNode<K,V> parent) {
            super(hash, key, val, next);
            this.parent = parent;
        }

        Node<K,V> find(int h, Object k) {
            return findTreeNode(h, k, null);
        }

        /**
         * 找到指定 key 所在节点
         */
        final TreeNode<K,V> findTreeNode(int h, Object k, Class<?> kc) {
            if (k != null) {
                TreeNode<K,V> p = this;
                do  {
                    int ph, dir; K pk; TreeNode<K,V> q;
                    TreeNode<K,V> pl = p.left, pr = p.right;
                    if ((ph = p.hash) > h)
                        p = pl;
                    else if (ph < h)
                        p = pr;
                    else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
                        return p;
                    else if (pl == null)
                        p = pr;
                    else if (pr == null)
                        p = pl;
                    else if ((kc != null ||
                            (kc = comparableClassFor(k)) != null) &&
                            (dir = compareComparables(kc, k, pk)) != 0)
                        p = (dir < 0) ? pl : pr;
                    else if ((q = pr.findTreeNode(h, k, kc)) != null)
                        return q;
                    else
                        p = pl;
                } while (p != null);
            }
            return null;
        }
    }

    /* ---------------- TreeBins -------------- */

    /**
     * 桶头部的 TreeNode 节点。TreeBins 不存储 key 和 value，但是保留指向 TreeNode
     * 列表和树的根节点的指针。也保留一个读写锁，用于树的重新构建。
     */
    static final class TreeBin<K,V> extends Node<K,V> {
        TreeNode<K,V> root;
        volatile TreeNode<K,V> first;
        volatile Thread waiter;
        volatile int lockState;
        // values for lockState
        static final int WRITER = 1; // set while holding write lock
        static final int WAITER = 2; // set when waiting for write lock
        static final int READER = 4; // increment value for setting read lock

        /**
         * Tie-breaking utility for ordering insertions when equal
         * hashCodes and non-comparable. We don't require a total
         * order, just a consistent insertion rule to maintain
         * equivalence across rebalancings. Tie-breaking further than
         * necessary simplifies testing a bit.
         */
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null ||
                    (d = a.getClass().getName().
                            compareTo(b.getClass().getName())) == 0)
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
                        -1 : 1);
            return d;
        }

        /**
         * Creates bin with initial set of nodes headed by b.
         */
        TreeBin(TreeNode<K,V> b) {
            super(TREEBIN, null, null, null);
            this.first = b;
            TreeNode<K,V> r = null;
            for (TreeNode<K,V> x = b, next; x != null; x = next) {
                next = (TreeNode<K,V>)x.next;
                x.left = x.right = null;
                if (r == null) {
                    x.parent = null;
                    x.red = false;
                    r = x;
                }
                else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K,V> p = r;;) {
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
                            r = balanceInsertion(r, x);
                            break;
                        }
                    }
                }
            }
            this.root = r;
            assert checkInvariants(root);
        }

        /**
         * Acquires write lock for tree restructuring.
         */
        private final void lockRoot() {
            if (!U.compareAndSwapInt(this, LOCKSTATE, 0, WRITER))
                contendedLock(); // offload to separate method
        }

        /**
         * Releases write lock for tree restructuring.
         */
        private final void unlockRoot() {
            lockState = 0;
        }

        /**
         * Possibly blocks awaiting root lock.
         */
        private final void contendedLock() {
            boolean waiting = false;
            for (int s;;) {
                if (((s = lockState) & ~WAITER) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, WRITER)) {
                        if (waiting)
                            waiter = null;
                        return;
                    }
                }
                else if ((s & WAITER) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, s | WAITER)) {
                        waiting = true;
                        waiter = Thread.currentThread();
                    }
                }
                else if (waiting)
                    LockSupport.park(this);
            }
        }

        /**
         * Returns matching node or null if none. Tries to search
         * using tree comparisons from root, but continues linear
         * search when lock not available.
         */
        final Node<K,V> find(int h, Object k) {
            if (k != null) {
                for (Node<K,V> e = first; e != null; ) {
                    int s; K ek;
                    if (((s = lockState) & (WAITER|WRITER)) != 0) {
                        if (e.hash == h &&
                                ((ek = e.key) == k || (ek != null && k.equals(ek))))
                            return e;
                        e = e.next;
                    }
                    else if (U.compareAndSwapInt(this, LOCKSTATE, s,
                            s + READER)) {
                        TreeNode<K,V> r, p;
                        try {
                            p = ((r = root) == null ? null :
                                    r.findTreeNode(h, k, null));
                        } finally {
                            Thread w;
                            if (U.getAndAddInt(this, LOCKSTATE, -READER) ==
                                    (READER|WAITER) && (w = waiter) != null)
                                LockSupport.unpark(w);
                        }
                        return p;
                    }
                }
            }
            return null;
        }

        /**
         * Finds or adds a node.
         * @return null if added
         */
        final TreeNode<K,V> putTreeVal(int h, K k, V v) {
            Class<?> kc = null;
            boolean searched = false;
            for (TreeNode<K,V> p = root;;) {
                int dir, ph; K pk;
                if (p == null) {
                    first = root = new TreeNode<K,V>(h, k, v, null, null);
                    break;
                }
                else if ((ph = p.hash) > h)
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
                    return p;
                else if ((kc == null &&
                        (kc = comparableClassFor(k)) == null) ||
                        (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        TreeNode<K,V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&
                                (q = ch.findTreeNode(h, k, kc)) != null) ||
                                ((ch = p.right) != null &&
                                        (q = ch.findTreeNode(h, k, kc)) != null))
                            return q;
                    }
                    dir = tieBreakOrder(k, pk);
                }

                TreeNode<K,V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    TreeNode<K,V> x, f = first;
                    first = x = new TreeNode<K,V>(h, k, v, f, xp);
                    if (f != null)
                        f.prev = x;
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    if (!xp.red)
                        x.red = true;
                    else {
                        lockRoot();
                        try {
                            root = balanceInsertion(root, x);
                        } finally {
                            unlockRoot();
                        }
                    }
                    break;
                }
            }
            assert checkInvariants(root);
            return null;
        }

        /**
         * Removes the given node, that must be present before this
         * call.  This is messier than typical red-black deletion code
         * because we cannot swap the contents of an interior node
         * with a leaf successor that is pinned by "next" pointers
         * that are accessible independently of lock. So instead we
         * swap the tree linkages.
         *
         * @return true if now too small, so should be untreeified
         */
        final boolean removeTreeNode(TreeNode<K,V> p) {
            TreeNode<K,V> next = (TreeNode<K,V>)p.next;
            TreeNode<K,V> pred = p.prev;  // unlink traversal pointers
            TreeNode<K,V> r, rl;
            if (pred == null)
                first = next;
            else
                pred.next = next;
            if (next != null)
                next.prev = pred;
            if (first == null) {
                root = null;
                return true;
            }
            if ((r = root) == null || r.right == null || // too small
                    (rl = r.left) == null || rl.left == null)
                return true;
            lockRoot();
            try {
                TreeNode<K,V> replacement;
                TreeNode<K,V> pl = p.left;
                TreeNode<K,V> pr = p.right;
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
                        r = s;
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
                        r = replacement;
                    else if (p == pp.left)
                        pp.left = replacement;
                    else
                        pp.right = replacement;
                    p.left = p.right = p.parent = null;
                }

                root = (p.red) ? r : balanceDeletion(r, replacement);

                if (p == replacement) {  // detach pointers
                    TreeNode<K,V> pp;
                    if ((pp = p.parent) != null) {
                        if (p == pp.left)
                            pp.left = null;
                        else if (p == pp.right)
                            pp.right = null;
                        p.parent = null;
                    }
                }
            } finally {
                unlockRoot();
            }
            assert checkInvariants(root);
            return false;
        }

        /* ------------------------------------------------------------ */
        // Red-black tree methods, all adapted from CLR

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

        private static final sun.misc.Unsafe U;
        private static final long LOCKSTATE;
        static {
            try {
                U = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TreeBin.class;
                LOCKSTATE = U.objectFieldOffset
                        (k.getDeclaredField("lockState"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /* ----------------Views -------------- */

    /**
     * Base class for views.
     */
    abstract static class CollectionView<K,V,E>
            implements Collection<E>, java.io.Serializable {
        private static final long serialVersionUID = 7249069246763182397L;
        final ConcurrentHashMap<K,V> map;
        CollectionView(ConcurrentHashMap<K,V> map)  { this.map = map; }

        /**
         * Returns the map backing this view.
         *
         * @return the map backing this view
         */
        public ConcurrentHashMap<K,V> getMap() { return map; }

        /**
         * Removes all of the elements from this view, by removing all
         * the mappings from the map backing this view.
         */
        public final void clear()      { map.clear(); }
        public final int size()        { return map.size(); }
        public final boolean isEmpty() { return map.isEmpty(); }

        // implementations below rely on concrete classes supplying these
        // abstract methods
        /**
         * Returns an iterator over the elements in this collection.
         *
         * <p>The returned iterator is
         * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
         *
         * @return an iterator over the elements in this collection
         */
        public abstract Iterator<E> iterator();
        public abstract boolean contains(Object o);
        public abstract boolean remove(Object o);

        private static final String oomeMsg = "Required array size too large";

        public final Object[] toArray() {
            long sz = map.mappingCount();
            if (sz > MAX_ARRAY_SIZE)
                throw new OutOfMemoryError(oomeMsg);
            int n = (int)sz;
            Object[] r = new Object[n];
            int i = 0;
            for (E e : this) {
                if (i == n) {
                    if (n >= MAX_ARRAY_SIZE)
                        throw new OutOfMemoryError(oomeMsg);
                    if (n >= MAX_ARRAY_SIZE - (MAX_ARRAY_SIZE >>> 1) - 1)
                        n = MAX_ARRAY_SIZE;
                    else
                        n += (n >>> 1) + 1;
                    r = Arrays.copyOf(r, n);
                }
                r[i++] = e;
            }
            return (i == n) ? r : Arrays.copyOf(r, i);
        }

        @SuppressWarnings("unchecked")
        public final <T> T[] toArray(T[] a) {
            long sz = map.mappingCount();
            if (sz > MAX_ARRAY_SIZE)
                throw new OutOfMemoryError(oomeMsg);
            int m = (int)sz;
            T[] r = (a.length >= m) ? a :
                    (T[])java.lang.reflect.Array
                            .newInstance(a.getClass().getComponentType(), m);
            int n = r.length;
            int i = 0;
            for (E e : this) {
                if (i == n) {
                    if (n >= MAX_ARRAY_SIZE)
                        throw new OutOfMemoryError(oomeMsg);
                    if (n >= MAX_ARRAY_SIZE - (MAX_ARRAY_SIZE >>> 1) - 1)
                        n = MAX_ARRAY_SIZE;
                    else
                        n += (n >>> 1) + 1;
                    r = Arrays.copyOf(r, n);
                }
                r[i++] = (T)e;
            }
            if (a == r && i < n) {
                r[i] = null; // null-terminate
                return r;
            }
            return (i == n) ? r : Arrays.copyOf(r, i);
        }

        /**
         * Returns a string representation of this collection.
         * The string representation consists of the string representations
         * of the collection's elements in the order they are returned by
         * its iterator, enclosed in square brackets ({@code "[]"}).
         * Adjacent elements are separated by the characters {@code ", "}
         * (comma and space).  Elements are converted to strings as by
         * {@link String#valueOf(Object)}.
         *
         * @return a string representation of this collection
         */
        public final String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            Iterator<E> it = iterator();
            if (it.hasNext()) {
                for (;;) {
                    Object e = it.next();
                    sb.append(e == this ? "(this Collection)" : e);
                    if (!it.hasNext())
                        break;
                    sb.append(',').append(' ');
                }
            }
            return sb.append(']').toString();
        }

        public final boolean containsAll(Collection<?> c) {
            if (c != this) {
                for (Object e : c) {
                    if (e == null || !contains(e))
                        return false;
                }
            }
            return true;
        }

        public final boolean removeAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean modified = false;
            for (Iterator<E> it = iterator(); it.hasNext();) {
                if (c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

        public final boolean retainAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean modified = false;
            for (Iterator<E> it = iterator(); it.hasNext();) {
                if (!c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

    }

    /**
     * A view of a ConcurrentHashMap as a {@link Set} of keys, in
     * which additions may optionally be enabled by mapping to a
     * common value.  This class cannot be directly instantiated.
     * See {@link #keySet() keySet()},
     * {@link #keySet(Object) keySet(V)},
     * {@link #newKeySet() newKeySet()},
     * {@link #newKeySet(int) newKeySet(int)}.
     *
     * @since 1.8
     */
    public static class KeySetView<K,V> extends CollectionView<K,V,K>
            implements Set<K>, java.io.Serializable {
        private static final long serialVersionUID = 7249069246763182397L;
        private final V value;
        KeySetView(ConcurrentHashMap<K,V> map, V value) {  // non-public
            super(map);
            this.value = value;
        }

        /**
         * Returns the default mapped value for additions,
         * or {@code null} if additions are not supported.
         *
         * @return the default mapped value for additions, or {@code null}
         * if not supported
         */
        public V getMappedValue() { return value; }

        /**
         * {@inheritDoc}
         * @throws NullPointerException if the specified key is null
         */
        public boolean contains(Object o) { return map.containsKey(o); }

        /**
         * Removes the key from this map view, by removing the key (and its
         * corresponding value) from the backing map.  This method does
         * nothing if the key is not in the map.
         *
         * @param  o the key to be removed from the backing map
         * @return {@code true} if the backing map contained the specified key
         * @throws NullPointerException if the specified key is null
         */
        public boolean remove(Object o) { return map.remove(o) != null; }

        /**
         * @return an iterator over the keys of the backing map
         */
        public Iterator<K> iterator() {
            Node<K,V>[] t;
            ConcurrentHashMap<K,V> m = map;
            int f = (t = m.table) == null ? 0 : t.length;
            return new KeyIterator<K,V>(t, f, 0, f, m);
        }

        /**
         * Adds the specified key to this set view by mapping the key to
         * the default mapped value in the backing map, if defined.
         *
         * @param e key to be added
         * @return {@code true} if this set changed as a result of the call
         * @throws NullPointerException if the specified key is null
         * @throws UnsupportedOperationException if no default mapped value
         * for additions was provided
         */
        public boolean add(K e) {
            V v;
            if ((v = value) == null)
                throw new UnsupportedOperationException();
            return map.putVal(e, v, true) == null;
        }

        /**
         * Adds all of the elements in the specified collection to this set,
         * as if by calling {@link #add} on each one.
         *
         * @param c the elements to be inserted into this set
         * @return {@code true} if this set changed as a result of the call
         * @throws NullPointerException if the collection or any of its
         * elements are {@code null}
         * @throws UnsupportedOperationException if no default mapped value
         * for additions was provided
         */
        public boolean addAll(Collection<? extends K> c) {
            boolean added = false;
            V v;
            if ((v = value) == null)
                throw new UnsupportedOperationException();
            for (K e : c) {
                if (map.putVal(e, v, true) == null)
                    added = true;
            }
            return added;
        }

        public int hashCode() {
            int h = 0;
            for (K e : this)
                h += e.hashCode();
            return h;
        }

        public boolean equals(Object o) {
            Set<?> c;
            return ((o instanceof Set) &&
                    ((c = (Set<?>)o) == this ||
                            (containsAll(c) && c.containsAll(this))));
        }

        public Spliterator<K> spliterator() {
            Node<K,V>[] t;
            ConcurrentHashMap<K,V> m = map;
            long n = m.sumCount();
            int f = (t = m.table) == null ? 0 : t.length;
            return new KeySpliterator<K,V>(t, f, 0, f, n < 0L ? 0L : n);
        }

        public void forEach(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            Node<K,V>[] t;
            if ((t = map.table) != null) {
                Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
                for (Node<K,V> p; (p = it.advance()) != null; )
                    action.accept(p.key);
            }
        }
    }

    /**
     * A view of a ConcurrentHashMap as a {@link Collection} of
     * values, in which additions are disabled. This class cannot be
     * directly instantiated. See {@link #values()}.
     */
    static final class ValuesView<K,V> extends CollectionView<K,V,V>
            implements Collection<V>, java.io.Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        ValuesView(ConcurrentHashMap<K,V> map) { super(map); }
        public final boolean contains(Object o) {
            return map.containsValue(o);
        }

        public final boolean remove(Object o) {
            if (o != null) {
                for (Iterator<V> it = iterator(); it.hasNext();) {
                    if (o.equals(it.next())) {
                        it.remove();
                        return true;
                    }
                }
            }
            return false;
        }

        public final Iterator<V> iterator() {
            ConcurrentHashMap<K,V> m = map;
            Node<K,V>[] t;
            int f = (t = m.table) == null ? 0 : t.length;
            return new ValueIterator<K,V>(t, f, 0, f, m);
        }

        public final boolean add(V e) {
            throw new UnsupportedOperationException();
        }
        public final boolean addAll(Collection<? extends V> c) {
            throw new UnsupportedOperationException();
        }

        public Spliterator<V> spliterator() {
            Node<K,V>[] t;
            ConcurrentHashMap<K,V> m = map;
            long n = m.sumCount();
            int f = (t = m.table) == null ? 0 : t.length;
            return new ValueSpliterator<K,V>(t, f, 0, f, n < 0L ? 0L : n);
        }

        public void forEach(Consumer<? super V> action) {
            if (action == null) throw new NullPointerException();
            Node<K,V>[] t;
            if ((t = map.table) != null) {
                Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
                for (Node<K,V> p; (p = it.advance()) != null; )
                    action.accept(p.val);
            }
        }
    }

    /**
     * A view of a ConcurrentHashMap as a {@link Set} of (key, value)
     * entries.  This class cannot be directly instantiated. See
     * {@link #entrySet()}.
     */
    static final class EntrySetView<K,V> extends CollectionView<K,V,Map.Entry<K,V>>
            implements Set<Map.Entry<K,V>>, java.io.Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        EntrySetView(ConcurrentHashMap<K,V> map) { super(map); }

        public boolean contains(Object o) {
            Object k, v, r; Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (r = map.get(k)) != null &&
                    (v = e.getValue()) != null &&
                    (v == r || v.equals(r)));
        }

        public boolean remove(Object o) {
            Object k, v; Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    map.remove(k, v));
        }

        /**
         * @return an iterator over the entries of the backing map
         */
        public Iterator<Map.Entry<K,V>> iterator() {
            ConcurrentHashMap<K,V> m = map;
            Node<K,V>[] t;
            int f = (t = m.table) == null ? 0 : t.length;
            return new EntryIterator<K,V>(t, f, 0, f, m);
        }

        public boolean add(Entry<K,V> e) {
            return map.putVal(e.getKey(), e.getValue(), false) == null;
        }

        public boolean addAll(Collection<? extends Entry<K,V>> c) {
            boolean added = false;
            for (Entry<K,V> e : c) {
                if (add(e))
                    added = true;
            }
            return added;
        }

        public final int hashCode() {
            int h = 0;
            Node<K,V>[] t;
            if ((t = map.table) != null) {
                Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
                for (Node<K,V> p; (p = it.advance()) != null; ) {
                    h += p.hashCode();
                }
            }
            return h;
        }

        public final boolean equals(Object o) {
            Set<?> c;
            return ((o instanceof Set) &&
                    ((c = (Set<?>)o) == this ||
                            (containsAll(c) && c.containsAll(this))));
        }

        public Spliterator<Map.Entry<K,V>> spliterator() {
            Node<K,V>[] t;
            ConcurrentHashMap<K,V> m = map;
            long n = m.sumCount();
            int f = (t = m.table) == null ? 0 : t.length;
            return new EntrySpliterator<K,V>(t, f, 0, f, n < 0L ? 0L : n, m);
        }

        public void forEach(Consumer<? super Map.Entry<K,V>> action) {
            if (action == null) throw new NullPointerException();
            Node<K,V>[] t;
            if ((t = map.table) != null) {
                Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
                for (Node<K,V> p; (p = it.advance()) != null; )
                    action.accept(new MapEntry<K,V>(p.key, p.val, map));
            }
        }

    }



    // Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final long SIZECTL;
    private static final long TRANSFERINDEX;
    private static final long BASECOUNT;
    private static final long CELLSBUSY;
    private static final long CELLVALUE;
    private static final long ABASE;
    private static final int ASHIFT;

    static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ConcurrentHashMap.class;
            SIZECTL = U.objectFieldOffset
                    (k.getDeclaredField("sizeCtl"));
            TRANSFERINDEX = U.objectFieldOffset
                    (k.getDeclaredField("transferIndex"));
            BASECOUNT = U.objectFieldOffset
                    (k.getDeclaredField("baseCount"));
            CELLSBUSY = U.objectFieldOffset
                    (k.getDeclaredField("cellsBusy"));
            Class<?> ck = CounterCell.class;
            CELLVALUE = U.objectFieldOffset
                    (ck.getDeclaredField("value"));
            Class<?> ak = Node[].class;
            ABASE = U.arrayBaseOffset(ak);
            int scale = U.arrayIndexScale(ak);
            if ((scale & (scale - 1)) != 0)
                throw new Error("data type scale not a power of two");
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}