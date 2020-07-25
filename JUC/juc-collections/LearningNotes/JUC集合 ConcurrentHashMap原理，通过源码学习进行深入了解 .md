# 概述
ConcurrentHashMap 是在 HashMap 的基础上进行改进的线程安全 Map 类，在开始 ConcurrentHashMap 前，务必提前了解 HashMap 的原理和基本思想。

ConcurrentHashMap 中使用 synchronized 进行锁定，主要用在 put 、remove操作以及扩容操作中，而且每一次锁住的只有当前的桶（数组的单个槽位）。

ConcurrentHashMap 和 Hashtable 对象的 key、value 值不可为 null，而 HashMap 对象的 key、value 值可为 null。
# 底层结构
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200725105355176.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
数组的每一个位置table[i]代表了一个桶，当插入键值对时，会根据键的hash值映射到不同的桶位置，table一共可以包含4种不同类型的桶：Node、TreeBin、ForwardingNode、ReservationNode。上图中，不同的桶用不同颜色表示。可以看到，有的桶链接着链表，有的桶链接着树，这也是JDK1.8中ConcurrentHashMap的特殊之处，后面会详细讲到。
需要注意的是：TreeBin所链接的是一颗红黑树，红黑树的结点用TreeNode表示，所以ConcurrentHashMap中实际上一共有五种不同类型的Node结点。

之所以用TreeBin而不是直接用TreeNode，是因为红黑树的操作比较复杂，包括构建、左旋、右旋、删除，平衡等操作，用一个代理结点TreeBin来包含这些复杂操作，其实是一种“职责分离”的思想。另外TreeBin中也包含了一些加/解锁的操作。
(注意本文并不会对红黑树的自身方法进行描述)

# 常量/字段
## 静态常量
```java
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
```
***
## 成员变量
使用新的变量 sizeCtl 控制初始化和扩容，sizeCtl 可能存在下面四个值：

-1，表示正在初始化

-(1 + nThreads)，表示有 n 个线程正在扩容

0，默认值

大于 0，初始化或扩容完成后下一次的扩容的阈值

counterCells 数组保存了集合中元素的个数（和 baseCount 协同工作），设计成数组是为了分担多线程同时修改集合元素个数的压力。当某个线程新增一个元素时，只需要在数组中专属（其实是由产生的随机数决定的）的位置（或者 baseCount 上）加一就可以了。LongAdder 也采用了这种思想。
```java

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
     * sizeCtl <0,则其= (resizeStamp << 16) + (1 + nThreads)，表示正在进行扩容，高位存储扩容邮戳，低位存储扩容线程数加 1；
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
```
***
# 内部节点类
Node 节点是基础节点。TreeNode 继承自 Node，表示树的节点。TreeBin 表示一个树结构，TreeNode 作为 TreeBin 的属性而存在。ForwardingNode 在扩容时作为占位节点，表示当前节点已经被移动，ForwardingNode 类中包括属性 nextTable，指向新创建的数组。
* Node<K, V>： 保存k-v、k的hash值和链表的 next 节点引用，其中 V 和 next 用volatile修饰，保证多线程环境下的可见性。
* TreeNode<K, V>： 红黑树节点类,当链表长度>=8且数组长度>=64时，Node 会转为 TreeNode，但它不是直接转为红黑树，而是把这些 TreeNode 节点放入TreeBin 对象中，由 TreeBin 完成红黑树的封装。
* TreeBin<K, V>： 封装了 TreeNode，红黑树的根节点，也就是说在 ConcurrentHashMap 中红黑树存储的是 TreeBin 对象。
* ForwardingNode<K, V>： 在节点转移时用于连接两个 table（table和nextTable）的节点类。包含一个 nextTable 指针，用于指向下一个table。而且这个节点的 k-v 和 next 指针全部为 null，hash 值为-1。只有在扩容时发挥作用，作为一个占位节点放在 table 中表示当前节点已经被移动。
* ReservationNode<K,V>： 在computeIfAbsent和compute方法计算时当做一个占位节点，表示当前节点已经被占用，在compute或computeIfAbsent的 function 计算完成后插入元素。hash值为-3。


节点类比较容易懂，具体自己可以查看jdk源码。

# 关键方法
## put
外部是一个for循环自旋，直到满足条件才会退出循环。循环中主要判断以下情况：
* 如果table为null,那么调用initTable方法进行初始化。
* 如果从主内存中取出底层table上指定key应该散列的索引位置上的桶为null，那么CAS方法放入新建的node，然后break。
* 如果对应位置上有节点，但是节点的hash值为-1，说明Map正在扩容，那么调用helpTransfer帮助扩容，完成后得到扩容后的新表再重新自旋。
* 以上条件都不满足，则进入synchronized代码块，对当前桶i上的头节点上锁。然后分为2种情况：
	* 如果节点hash值大于0，说明为普通链表节点，则从头节点开始遍历，如果遇到相等的key，进行值覆盖，然后break；如果遍历到链表末尾还没有找到，则新建节点放入链表末尾，break。注意遍历中会使用binCount记数，该值用于确定是否需要转换为红黑树。
	* 如果节点hash为-2，是TreeBin的实例，则调用红黑树的put方法。break。
```java
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
```
***
## remove
与插入类似，每一次自旋操作分为以下情况：

* 如果数组不存在或 i 位置不存在任何节点，直接退出自旋。

* 如果该节点正在扩容，当前线程进入 helpTransfer 帮助扩容。从 helpTransfer 出来之后再继续自旋。

* 桶中有元素，（使用 synchronized 锁定之后）在桶内查找。synchronized 锁定的是整个桶，这一步骤里面的修改操作不需要 CAS，但是用Unsafe方法使得修改操作后，桶更新回主内存中。
* 注意，最后需要用validated判断是否执行了第三个情况的方法流程。只有执行了，才会判断并且break，否则继续自旋

```java
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
                                    break;   //操作成功，直接退出内部for循环。
                                }
                                pred = e; //更新pred。
                                if ((e = e.next) == null)  //遍历。如果尾节点已经判断完了，那么退出。
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
                        if (value == null)//如果是删除，不是替换，则调用addCount，数量减1
                            addCount(-1L, -1);
                        return oldVal;
                    }
                    break;
                }
            }
        }
        return null;  //没找到就返回null。
    }
```

***
##  扩容方法
### 初始化方法initTable
主要判断变量sizeCtl的正负性：
* 如果sizeCtl为-1，则说明有线程正在初始化，那么其他线程不能参与，只能等待，所以调用Thread.yield方法让出时间片。
* 如果不为负，那么当前线程执行初始化，尝试CAS设置sizeCtl为-1，为了提醒别的线程不要重复初始化。
```java
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
```
***
### tryPresize
这个方法其实是扩容前的检查工作，首先计算指定size需要底层数组的长度 赋值为c，然后进入while循环，进入条件是sizeCtl大于等于0，即sizeCtl不能为负数，即当前如果有线程正在初始化或者扩容，当前线程不会进入while，直接退出。
while循环内部：
* 如果table为空，CAS设置sizeCtl为-1，进行初始化，初始化完以后，将sizeCtl设置为阈值，0.75*n
* 如果sc大于c，或者数组已经达到最大的容量了，那么无需扩容，直接break。
* 进入扩容，先调用resizeStamp得到此次扩容的唯一标识，CAS尝试将sizeCtl更改为(rs << RESIZE_STAMP_SHIFT) + 2)。如果修改成功，则当前线程作为第一个扩容线程，调用transfer方法，开始扩容。
```java
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
                if (sc < 0) {  //该循环不会进入此IF语句
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
```
***
### helpTransfer 帮助扩容
当put,remove等结构修改性的操作时，如果遇到桶是ForwardingNode，说明当前桶正在转移到新表中，有线程在扩容，就会调用helpTransfer帮助扩容，方法中，会判断是否能够参与此次扩容，如果可以，就会CAS修改sizeCtl,将低16位的值+1，表示正在扩容的线程数加1。
```java
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
```
***
### transfer 转移节点
将数组扩容成原来的两倍。第一个开始扩容的线程创造一个容量为原容量两倍的新数组。

每一个线程完成一段数组中节点的转移。用 stride 控制每个线程每一次需要处理的数组长度，用 transferIndex 记录已经处理过或者有线程正在处理的最小槽索引。

自旋完成扩容操作。

每一次自旋需要判断这一次处理的是哪一个位置，也就是 i 的位置。比 transferIndex 大的索引位置已经分配给之前的线程，当前线程从 transferIndex 位置开始处理，从后往前，处理的索引范围是 transferIndex - stride 到 transferIndex。

处理完了当前位置 i 之后，继续处理 --i。如果当前范围内的所有位置都已经处理完了，根据 transferIndex 从 table 又分出一块 stride 给当前线程处理。这一流程是在 while (advance) {...} 这段代码中完成的。

确定了应该处理哪一个位置之后，就可以执行转移操作了。

执行转移操作时主要有以下几种情况：

* 如果当前线程已经完成转移，sizeCtl 减一后直接返回。最后一个线程完成扩容，设置 finishing 为 true 表示扩容结束。线程设置好 table、sizeCtl 变量之后，扩容结束。

* 如果 i 位置节点为 null，将其设为 fwd，提醒其他线程该位已经处理过了。

* 如果 i 位置已经处理过了，继续往后处理其他位置。该判断主要是最后i从n到0检查每一个桶是否转移完毕时用到。

* 处理 i 位置。同样地，处理之前使用 synchronized 上锁。

无论桶里是链式结构还是树状结构，都将链表拆分成两个链表，分别放在原位置和新位置上。具体实现上，使用了数组容量为 2 的幂这一点来简化操作（只判断标志位），使用了 lastRun 来提高效率。
```java
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
```
***
## 记数方法
### addCount
addCount

计算元素个数同时使用到了 counterCells 和 baseCount 两个变量。

addCount 分成两个部分，上半部分是更新计数，下半部分是根据需要扩容。

只有从未出现过并发冲突的时候，baseCount 才会使用到，也就是直接在 baseCount 上面更新计数。一旦出现了并发冲突，之后所有的操作基本都只针对 counterCells。（fullAddCount 中如果在扩容，也会用到 baseCount）

在 counterCells 还没有初始化或者将要操作的槽还没有初始化或者槽中出现了竞争的时候，调用 fullAddCount 完成更新计数。
```java
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
```
***
### fullAddCount
在 fullAddCount 中的自旋分成以下三种情况：

* counterCells 已经初始化。此种情况又可以根据以下条件分别讨论：

	* 如果 counterCells 中 h 位置的 CounterCell 还没有初始化，则获取 CELLSBUSY 锁，然后创建 CounterCell 对象并初始化，操作成功则跳出自旋。
	* 如果 counterCells 中 h 位置的 CounterCell 已经初始化了，尝试 CAS 更新计数，操作成功则跳出自旋。
	* 如果 counterCells 中 h 位置竞争太激烈且还没有达到 CPU 的上限，则先扩容，扩容后再继续自旋。
* counterCells 还没有初始化。首先获取 CELLSBUSY 锁，然后创建 CounterCell 数组，初始大小为 2。初始化成功之后就释放锁。

* counterCells 正在初始化中。尝试更新 baseCount，成功则跳出自旋。
```java
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
```
***
### sumCount
使用counterCells其实是用空间换时间的思想，创建一个countCell数组，每一个元素counterCell存放的是来自各线程的操作记数。baseCount是最初没出现竞争时候，线程直接CAS操作的值，而一旦出现竞争，不会选择自旋等待成功，而是，重新使用线程方法getProbe和adVanceProbe获得代表线程hash的值，然后计算散列到counterCells数组的位置，对这个位置上的值进行CAS操作，这样就减少了CAS自旋等待的时间。提高了记数效率。
所以，计算总数时，是将每一个countCell元素都累加起来，然后再加上baseCount，就是次Map当前包含的映射数量了。
```java
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
```
# 总结
到此，ConcurrentHashMap的分析就告一段落了。总的来说源码比较复杂，真正理解它还是需要一些耐心的。重点是它的数据结构和扩容的实现。
ConcurrentHashMap 源码分析到此结束，希望对大家有所帮助，如您发现文章中有不妥的地方，请留言指正，谢谢。
# 参考
[UC源码分析-集合篇（一）：ConcurrentHashMap](https://www.jianshu.com/p/0fb89aefac66)
[JUC 集合 ConcurrentHashMap详解](https://www.pdai.tech/md/java/thread/java-thread-x-juc-collection-ConcurrentHashMap.html)

