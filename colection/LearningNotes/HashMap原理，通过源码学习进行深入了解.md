@[TOC](jdk1.8 HashMap学习（包括分析部分源码）)
# 概述
HashMap是java集合类中很常用的一个数据结构，它是非常典型的，用于存储（key,value）形式的键值对映射。HashMap是基于哈希表的Map接口的非同步实现。此实现提供所有可选的映射操作，并允许使用null值和null键。此类不保证映射的顺序，特别是它不保证该顺序恒久不变
![java部分集合类](https://img-blog.csdnimg.cn/20200607112005254.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
它的继承关系图如下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200607112229736.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
# 底层结构
可以说HashMap本质上它是一个数组table，根据经过hash(key)方法得到的哈希值hash按照一定规则散列到这个数组中。不同的key经过散列后可能会分配到同一个table索引位置上，这叫哈希冲突(也叫哈希碰撞)，如果发生了哈希冲突，会采用链地址法。将这个新加入的映射（被存储为node节点）链接到节点之后，在jdk1.8之后当链表节点超过了一定阈值，链表会转化为红黑树。
![HashMap的底层结构](https://img-blog.csdnimg.cn/20200607112630476.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
jdk1.7之前，HashMap采用了数组+链表的结构，而1.8之后，使用了数组+链表+红黑树的结构。
## 为什么使用这个结构
**数组特点：**
存储区间是连续，且占用内存严重，空间复杂也很大，时间复杂为O（1）。
优点：是随机读取效率很高，原因数组是连续（随机访问性强，查找速度快）。
缺点：插入和删除数据效率低，因插入数据，需要将这个位置后面的数据在内存中要往后移的，并且它的大小固定不易动态扩展。
**链表特点：**
区间离散，占用内存宽松，空间复杂度小，时间复杂度O(N)。
优点：插入删除速度快，内存利用率高，没有大小固定，扩展灵活。
缺点：不能随机查找，每次都是从第一个开始遍历（查询效率低）。
**哈希表特点：**
以上数组和链表，都有各自的优缺点，哈希表通过权衡将2个结构进行组合，使得查询效率和插入删除效率都比较高。
# 静态变量
```java
/**
     *默认的初始容器大小为2的4次方，初始容器一定是2的次方。
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * 最大容量，如果任何构造函数中指定了一个更大的初始化容量，将会被
     *  MAXIMUM_CAPACITY 取代。
     *  此参数必须是 2 的幂，且小于等于 1 << 30。
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;//默认负载因子0.75

    /**
     * 将链表转化为红黑树的临界值。把一个元素添加到至少有
     * TREEIFY_THRESHOLD 个节点的桶里时，桶中的链表将被转化成
     * 树形结构。此变量最小为 8。
     */
    static final int TREEIFY_THRESHOLD = 8;
    /**
     * 在调整大小时，把树结构恢复成链表时的桶大小临界值。此变量应该小于
     * TREEIFY_THRESHOLD，最大为 6。
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * 当 table 数组大于此容量时，桶内链表才可能被转化成树形结构的。否则，
     * 若桶内元素太多时，直接进行扩容而不是树形化。容量应该至少为 
     * 4 * TREEIFY_THRESHOLD 来避免和树形结构化之间的冲突。
     * 即
     */
    static final int MIN_TREEIFY_CAPACITY = 64
 ```
 # 成员变量
 table就是散列的桶数组，数组的每一个位置都代表一个桶(bucket)。用来存放hash值经过散列算法得到相同索引的对象。
 threshold 为需要进行resize扩容的阈值，除了hashmap初始化以及容量超出了最大限制2^30时，大部分情况下，threshold = table.length(capacity)* loadFactor。
loadFactior为负载因子。负载因子大时，优点是填满的元素多，空间利用率高，负载因子小时，优点是冲突的概率小，链表较短，查找效率变高，但可能会进行频繁的扩容操作，也会消耗性能。默认加载因子为 0.75，是时间效率和空间效率的一种平衡。
 size 表示映射的数量，而不是table的长度(桶的数量)。size 大于阈值threshold时执行扩容操作。
 ```java
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
     * //保存缓存entrySet ()。注意，AbstractMap字段用于keySet()和values()。
     */
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
     *扩容的临界值（capacity * load factor）。超过这个值将扩容。
     * @serial
     */
    // (The javadoc description is true upon serialization.
    // Additionally, if the table array has not been allocated, this
    // field holds the initial array capacity, or zero signifying
    // DEFAULT_INITIAL_CAPACITY.)
      //(javadoc描述在序列化时是正确的。此外，如果没有分配表数组，
    // 则该字段保存初始数组容量，为0表示,容量使用默认DEFAULT_INITIAL_CAPACITY)
    int threshold;    //table为null时字段代表数组的初始容量，否则代表阈值，超过该值数组将扩容。

    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    final float loadFactor;    //哈希表的负载因子
```
# 构造函数
其中putMapEntries方法，除了在使用了指定MAP构造函数的使用调用到，在下面putAll方法也用到。
这边要先介绍 tableSizeFor方法，接收一个整型容量，返回大于等于它的2的倍数，如果这个返回值大于了最大容量，则返回的是最大容量。
```java
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

```
一共四个构造函数如下：
```java
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

```
# 关键内部类
## Node 类
其中Node为普通节点，TreeNode为红黑树节点。
```
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

```
## TreeNode
TreeNode继承了LinkedHashMap.Entry,而这个Entry继承了上面的node类，当table表中某个桶中节点超过8个，node节点要转化为TreeNode节点，然后将这些节点构成红黑树。TreeNode中含有的方法较多，具体方法作用可以学习TreeMap时再深入。这里先知道这是一个树节点类，hashmap关于红黑树的增增删改查都在这个类中实现即可。
```java
 static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
        TreeNode<K,V> parent;  // red-black tree links      //用于红黑树的连接
        TreeNode<K,V> left;              //左孩子节点
        TreeNode<K,V> right;           //右孩子节点
        TreeNode<K,V> prev;    // needed to unlink next upon deletion     //因为删除时会断开next连接，所以使用prev保存前一个。
        boolean red;            //如果为true，说明是红色节点， 为false则为黑色节点
        TreeNode(int hash, K key, V val, Node<K,V> next) {s
            super(hash, key, val, next);
        }    //构造函数
        /*下接红黑树的类方法*/
```
# 关键成员方法
## hash方法
当一个键值对存入时，由前面我们知道需要创建一个Node节点类（或TreeNode类）然后再存入我们的hashmap的桶中，而同样在删除、查找时，定位键值对到桶中位置也是很关键的第一步。在节点类中有个成员属性hash是用于存储成员的hash值的，而这个hash值正是通过hash（key）计算得到的，hashmap中使用键值对节点类中的hash存储的值与(table.length-1)做&运算得到对应的索引，代表找到的桶的位置。
```java
//因为当桶数很小时，很大的hash值（它的二进制总是高位在变化），
    //在散列时，总是发生碰撞，所以使用一种方法将较高位扩展到较低位。
    //让table 在容量较小时，高位也能够参与散列运算，并且不会造成较大开销。
    static final int hash(Object key) { //重新计算key的hash值
        int h;            //用于返回的hash值
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }//如果key为null，返回0，如果key不为0，原hash值高位右移16位到低位，然后与原数进行异或。
```
hash方法首先接收key,然后通过key的hashCode()方法计算key的原始hash值，而后hashmap为了保证在table长度很小时，避免很大的hash值总是发生碰撞，通过了上述代码计算获得了新的hash值，将其作为返回值。
在hashmap的很多方法中，找到键值对节点在table数组中的位置是方法的第一步。如何根据节点类中的hash值来计算它在桶中的索引呢。hashmap使用了以下方法。
```java
/*   index 代表在table数组中的索引位置，hash值等于hash(key),
table.length表示table的长度
*/
int index = hash & (table.length - 1);

```
对于任意一个给定的对象，只要它的hashCode()返回的值相同，那么通过hash()方法得到的返回值hash总是相同的，对于一系列的hash值，如何使这些元素在哈希表中散列均匀，以便提高哈希表的操作效率，我们首先想到的是取模运算%。
	但是模运算的计算消耗是非常巨大的，因此为了优化模运算，我们使用了上面的代码取代了模运算，这个优化是基于x mod 2^n = x & (2^n - 1)。在上面的介绍中提到table的长度总是2的n次方，并且取模运算为n mod table.length,所以对应上面公式，可以得到该运算等同于n&(table.length - 1);这是HashMap在速度上的优化，因为&比%具有更高的效率。
## get方法
get方法接收一个键值对中的键key,调用getNode找到这个key对应的节点，然后获得该节点中value进行返回。要注意的是返回为null不一定是因为不包含指定的key，而也有可能是map中这个key对应的value就是为null,以为hashmap允许value为空。而getnode方法，接收key的hash值和key，首先根据hash值找到对应的数组table的位置，判断这个位置上的桶是否为空，不为空则继续判断这个桶存储的是普通链表还是红黑树。如果是普通链表则通过next属性依次遍历链表，找到hash值和key和传入的相同，就把节点赋值返回。如果是红黑树，则要调用红黑树的方法getTreeNode（同样此方法详细可以学习TreeMap时再深入）来得到找到这个节点。
```java
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
     * 返回指定 key 对应的 value，如果指定 key 不包含任何映射返回 null。
     * 
     * 返回值为 null 并不一定是因为不包含指定 key 对应的映射，也有可能是
     *  map 允许 value 值为 null。containsKey 方法可以用来区分这两种情况。
     *   /
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
```
## put方法
put方法接收2个参数，即键值对的key和对应的value，然后调用putVal方法。该方法先判断table是否为空，为空则首先调用resize()进行初始化table。再根据传入的key的hash值找到对应的table上的位置，判断该位置上是否有其他值，如果为空就使用key,value，新建节点存放在该位置。若该位置不为空，此时有两种情况，已经存在该key,，则进行value覆盖，另一种情况是不存在该key需要进行插入。具体过程在下面源码中分析。
需要注意的是，因为节点有可能是Node类和TreeNode类，所以需要判断两种情况。且需要注意如果普通链表的节点为7个，刚好put成功，链表长度变为8个节点，就要调用treeifyBin方法将链表转化为红黑树（如果数组长度大于的话），putTreeVal方法用于红黑树结构的添加节点（同样此方法详细可以学习TreeMap时再深入）。
```java
/*
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

```
## treeifyBin方法
当一个桶中的普通链表节点超过了8个时，就会调用该方法进行红黑树转化。但是要注意的是，进行转化还有一个必要前提是table.length要大于等于64，即在容量小于64时，发生了一个桶中链表节点到达8个，则这时候是选择调用resize()进行扩容，而不是转化为红黑树。具体需要转化红黑树时，会先将普通链表节点转化为树节点，并且构造成双向链表，然后调用treeify将此链表转化为红黑树。
```java
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
```
## putAll方法
putAll方法，接收指定的Map,将map中的所有映射赋值到hashmap中，其原理就是遍历map中的每一个键值对，将得到的键值对作为参数调用上面put用到的putVal方法。
```java
 /**
     *将指定 map 的所有映射复制到此 map 中。这些映射将替代此 map 中
     * 已经存在的 key 对应的映射。
     * @param m mappings to be stored in this map
     * @throws NullPointerException if the specified map is null
     */
    public void putAll(Map<? extends K, ? extends V> m) {             //将一组键值对全部放入。原理和使用键值对构造hashmap差不多，只是容量定义上有差别。
        putMapEntries(m, true);     //处理好容量和阈值问题以后，就是等同于一步一步遍历map，逐个putVal;
    }
      /** @param m the map
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
            else if (s > threshold)        //如果table 不为空。键值对数量大于阈值。进行扩容。用于putAll;
                resize();
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {  //Iterator遍历 Map
                K key = e.getKey();                 //得到key,
                V value = e.getValue();                        //得到value
                putVal(hash(key), key, value, false, evict);   //把每个key-value键值对插入到hashmap中。
            }
        }
    }

```
## resize方法
这个方法对hashmap中table的初始化或者对table的长度进行翻倍。对table的翻倍，需要重新分配桶中元素。
扩容的第一步主要是确定newCap和newThr两个关键值。概括来说情况如下：
resize在对长度翻倍时，table!=null,即oldCap>0;（1）如果，原来table的长度oldCap已经是最大容量了，则不能进行翻倍，只将阈值设置为最大整数，就将旧表进行返回；如果，oldCap还没有达到最大容量，则对其进行翻倍，newCap=oldCap<<1;且若这个新容量大于默认初始容量，小于最大容量，就对阈值也进行翻倍，newThr=oldThr<<1。
resize在初始化时,table=null,即oldCap<=0：（2）如果使用没有指定初始容量的构造函数，则oldThr<=0。这时使用默认初始容量16，newThr为默认初始容量乘以默认负载因子。（3）使用了带有初始容量参数的构造函数，则oldThr保存了初始容量，所以newCap=oldThr，newThr在容量没超过阈值时赋值为newCap*loadFact,在超出阈值时，赋值为Integer.MAX_VALUE。
具体分支如下图：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200608175550880.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70#pic_center)
第二步则是具体扩容过程中，如果table不为空，则需要将原来每个桶中的元素转移到新的table中，使它们根据新的散列规则重新分配，同样要判断桶中是红黑树结构还是普通链表。这一步的具体过程看代码解释。
```java
  /**
     * 初始化 table size 或者对 table size 加倍。如果 table 为 null，对 table
     *  进行初始化。如果进行扩容操作，由于每次扩容都是翻倍，每个桶里的
     * 元素要么待在原来的索引里面，要么在新的 table 里偏移 2 的幂个位置。
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

```
其中TreeNode.split方法，主要是将将红黑树先分为2个子树，该方法只在resize扩容时调用，即其中一个子树的hash经过新散列还是分配到原来索引位置桶中，而另一个子树是分配在索引位置+oldCap(旧容器大小)位置， 再对2个子树进行判断，如果长度小于等于指定的6，就退化为链表结构。具体分析同样等到学习TreeMap时深入了解。
## remove方法
remove方法接收key,调用removeNode方法，如果移除成功返回删除点的value值，如果移除失败，即hashmap中没有改key，则返回null。
removeNode方法，接收的参数较多，具体在代码中@param中解释。该方法可以分为两步：
第一步是根据传入的hash，key找到要删除的节点，这个节点会被赋值给node,这一步其实和getNode是有点相似的，在定位到table中的桶后，判断出该桶是红黑树结构时也是直接调用了getTreeNode方法。而在判断出是链表节点时，在遍历过程中，当找到这个要删除的点时，会保存这个节点的上一个节点赋值给p,以便用于下一步删除过程。
第二步就是将找到的节点删除。删除时要判断，找到的点是红黑树的节点，还是普通链表节点。如果是普通链表的节点的头结点，则直接将头结点的next赋值给table[index];如果是普通链表的除头结点外的其他节点，则将上述查找时存储的上个节点p的next重新赋值为node的next；如果是红黑树节点，则调用
TreeNode的类方法removeTreeNode，该方法会删除红黑树中对应节点，并且判断桶中剩余节点，如果剩余节点小于等于6，就会转化为普通链表(具体实现在学习TreeMap时深入了解)。
```java
	/*
	*接收key,若移除成功则返回移除点的value值，若移除失败，未找到该点，则返回null。
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
     * @return the node, or nul l if none       //返回删除的节点，如果没找到删除的点，返回null
     */
    //
    final Node<K,V> removeNode(int hash, Object key, Object value,
                                                 boolean matchValue, boolean movable) {
        Node<K,V>[] tab; Node<K,V> p; int n, index;    //removeNode,第一步是从hashmap中找到节点。第二步才是删除。
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
```
## clear方法
这个方法比较简单，其实就是简单的将table数组的每一个桶都赋值为null，虚拟机就会自己完成垃圾回收，然后将size设置为初始值0；
```java
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

```
# 其他一些成员方法
## size方法
返回hashmap中含有的键值对的总数。
```java
* Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {            //得到映射中 键值对的总数。
        return size;
    }
```
## isEmpty方法
返回boolean值，如果hashmap中不存在键值对则返回true，如果存在为false；
```java
   /**
 Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    public boolean isEmpty() {     //判断hashmap是否为空。空返回true、
        return size == 0;
    }
```
##  containsKey方法
判断hashmap中是否存在一对键值对，它的key等于传入的参数key。如果存在，返回true，不存在返回false。
```java
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
```
## containsValue方法
该方法，使用双循环，先遍历每个桶，再遍历每个桶中的节点，对节点的value进行判断，如果找到相等的就返回true，否则返回false；
```java
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
```
## keySet、 values  和 entrySet方法
```java
/*
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
            ks = new KeySet();     //这个类是定义的内部类，继承AbstractSet。
            keySet = ks;
        }
        return ks;
    }
    /*
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
    /*和上面keySet()以及values()相似意思。因为键值对不允许重复，所以
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

```
## 重写Map中的方法
这些方法在学习map时候再了解。


## hashmap的IO相关方法
这些方法在学习IO写入的源码后再补充学习

## TreeNode类的分析
这些成员和类方法，留在对TreeMap的学习中进行深入了解。
