# 概述
LinkedHashMap 继承自 HashMap，在 HashMap 基础上，通过维护一条双向链表，解决了 HashMap 不能随时保持遍历顺序和插入顺序一致的问题。除此之外，LinkedHashMap 对访问顺序也提供了相关支持。在一些场景下，该特性很有用，比如缓存。在实现上，LinkedHashMap 很多方法直接继承自 HashMap，仅为维护双向链表覆写了部分方法。所以，要看懂 LinkedHashMap 的源码，需要先看懂 HashMap 的源码。关于 HashMap 的源码分析参考：[HashMap原理，通过源码学习进行深入了解](https://blog.csdn.net/qq_30921153/article/details/106598034)
# 关键变量
除了子类的HashMap的成员变量外，LinkedHashMap因为需要维护一条双向链表，所以类需要多存储两个节点，即链表的头节点head和尾节点tail。
除此之外，因为LinkedHashMap 对访问顺序也提供了相关支持，需要设置一个标志accessOrder，代表不同的模式存储，当它为true时，开启访问模式，在LinkedHashMap调用到put、putIfAbsent、get、getOrDefault、compute、computeIfPresent、merge方法时，会将最先访问的数据放到末尾。具体在下面会介绍。
```java
	private static final long serialVersionUID = 3801124242820219131L;

    /**
     * The head (eldest) of the doubly linked list.
     *双向链表的头结点。
     */
    transient LinkedHashMap.Entry<K,V> head;

    /**
     * The tail (youngest) of the doubly linked list.
     * 双向链表的尾节点（最新插入的节点）。
     */
    transient LinkedHashMap.Entry<K,V> tail;
      /**
     * The iteration ordering method for this linked hash map: <tt>true</tt>
     * for access-order, <tt>false</tt> for insertion-order.
     *节点存储的顺序：
     *如果为 true 表示以访问（get、put）模式存储，最新访问的放在链表末尾
     * 如果为 false 表示以插入（put）模式存储，最近插入的放在链表末尾（注意此处的插入不包括修改）
     * @serial
     */
    final boolean accessOrder;

```
# 内部类
```java
static class Entry<K,V> extends HashMap.Node<K,V> {
        Entry<K,V> before, after;      //多了2个指向头尾节点的指针。
        Entry(int hash, K key, V value, Node<K,V> next) {
            super(hash, key, value, next);
        }
    }
```
# 关键方法
## 工具方法
```java
 // internal utilities
    // 内部工具
    // link at the end of list
    // 链表末尾添加节点
    private void linkNodeLast(LinkedHashMap.Entry<K,V> p) {
        LinkedHashMap.Entry<K,V> last = tail;   //存储末尾节点
        tail = p;
        if (last == null)  //如果一个节点
            head = p;    //更新头节点
        else {
            p.before = last;   //双向链表维护。
            last.after = p;
        }
    }
    // apply src's links to dst
    // 将 src 节点替换成 dst 节点
    private void transferLinks(LinkedHashMap.Entry<K,V> src,
                               LinkedHashMap.Entry<K,V> dst) {
        LinkedHashMap.Entry<K,V> b = dst.before = src.before;
        LinkedHashMap.Entry<K,V> a = dst.after = src.after;
        if (b == null)
            head = dst;
        else
            b.after = dst;
        if (a == null)
            tail = dst;
        else
            a.before = dst;
    }
```
## 重写HashMap中的方法
### newNode方法和replacement等方法
之所以LinkedHashMap底层也是拉链式存储，是因为该类直接使用了HashMap中的大部分方法，只是为了多维护一条双链表，LinkedHashMap将原本在HashMap的一些对于节点新建或操作的时候会调用的一些方法进行了重写。
具体如下：
```java
   void reinitialize() {
        super.reinitialize();
        head = tail = null;
    }
    // 重写了hashmap中的方法，所以putVal中的newNode方法会被替换成调用这个版本
    Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
        LinkedHashMap.Entry<K,V> p =
                new LinkedHashMap.Entry<K,V>(hash, key, value, e);
        linkNodeLast(p);   //多了一步，添加到链表的末尾步骤
        return p;
    }
    // 将树节点转化为 LinkedHashMap 节点，回收原来的节点
    Node<K,V> replacementNode(Node<K,V> p, Node<K,V> next) {
        LinkedHashMap.Entry<K,V> q = (LinkedHashMap.Entry<K,V>)p;
        LinkedHashMap.Entry<K,V> t =
                new LinkedHashMap.Entry<K,V>(q.hash, q.key, q.value, next);
        transferLinks(q, t);  //用t替换q。
        return t;
    }
    // 创建树节点
    TreeNode<K,V> newTreeNode(int hash, K key, V value, Node<K,V> next) {
        TreeNode<K,V> p = new TreeNode<K,V>(hash, key, value, next);
        linkNodeLast(p);  //同样多了一步在链表后添加节点。
        return p;
    }
     //将普通节点转化为红黑树节点，
    TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
        LinkedHashMap.Entry<K,V> q = (LinkedHashMap.Entry<K,V>)p;
        TreeNode<K,V> t = new TreeNode<K,V>(q.hash, q.key, q.value, next);
        transferLinks(q, t);
        return t;
    }
```
以上方法实质上，都是为了让HashMap的一些操作，也可以对维护的从head到tail的双向链表适用，即重写的目的就是Map在添加删除转化节点等操作时，同时维护节点间的双链表结构。
***
# 核心方法
在HashMap源码中有如下代码段：
```java
 // Callbacks to allow LinkedHashMap post-actions
    //提供给linkedHashMap调用。
    void afterNodeAccess(Node<K,V> p) { }
    void afterNodeInsertion(boolean evict) { }
    void afterNodeRemoval(Node<K,V> p) { }
    
```
而这三个代码是LinkedHashMap的最重要的方法。
## afterNodeRemoval方法

```java
 // 节点删除之后的操作
    void afterNodeRemoval(Node<K,V> e) { // unlink
        LinkedHashMap.Entry<K,V> p =   //转化为LinkedHashMap节点，得到前后索引b和a
                (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
        p.before = p.after = null;//方便GC
        if (b == null)   // 维护双向链表结构
            head = a;
        else
            b.after = a;
        if (a == null)
            tail = b;
        else
            a.before = b;
    }
```
## afterNodeInsertion方法
当满足 removeEldestEntry(first)方法放回true的情况下，则要删除处于链表头部的节点，因为最近最新访问的会被放在链表尾部，或者插入模式中，最近最新插入的会在链表尾部。即LRU淘汰需要重写方法。
```java
//插入后调用的维护双向链表结构
 // 移除最老的节点
    //evict为false表示处于构建hashmap的过程，true表示往创建好的Map中新增
    void afterNodeInsertion(boolean evict) { // possibly remove eldest
        LinkedHashMap.Entry<K,V> first;
        // 默认的 removeEldestEntry 永远返回 false，实现 LRU 需要重写此函数
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }
```
```java
/*
     *此方法通常不会以任何方式修改映射，而是允许映射根据其返回值的指示修改自身。
     * 允许此方法直接修改映射，但如果这样做，则必须返回<tt>false</tt>
     * （表示映射不应尝试进一步修改）。
     * 在此方法中修改映射后返回<tt>true</tt>的效果未指定
     * <p>This implementation merely returns <tt>false</tt> (so that this
     * map acts like a normal map - the eldest element is never removed).
     * 此方法在 put 和 putAll 里调用，用于删除最老的节点。
     * @param    eldest The least recently inserted entry in the map, or if
     *           this is an access-ordered map, the least recently accessed
     *           entry.  This is the entry that will be removed it this
     *           method returns <tt>true</tt>.  If the map was empty prior
     *           to the <tt>put</tt> or <tt>putAll</tt> invocation resulting
     *           in this invocation, this will be the entry that was just
     *           inserted; in other words, if the map contains a single
     *           entry, the eldest entry is also the newest.
     *      映射中最近插入的最旧的条目，
     *     或者如果这是按访问顺序排列的映射，
     *      则是最近访问最少的条目。如果此方法返回<tt>true</tt>，
     *      则将删除此项。如果映射在导致此调用的<tt>put</tt>或<tt>putAll</tt>调用之前为空，
     *     则这将是刚刚插入的条目；
     *       换句话说，如果映射包含单个条目，则最早的条目也是最新的条目
     * @return   <tt>true</tt> if the eldest entry should be removed
     *           from the map; <tt>false</tt> if it should be retained.
     */
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return false;
    }
```
而其中removeEldesEntry就是LinkedHashMap实现缓存管理用到的方法，通常要重写该方法：
案例：最大容量为 100 的 LinkedHashMap：
```java
private static final int MAX_ENTRIES = 100;

  protected boolean removeEldestEntry(Map.Entry eldest) {
    return size() > MAX_ENTRIES;
    }
```
案例中，当Map中数据操作100时，就会进行淘汰，从链表头部删除元素。
***
## afterNodeAccess方法
如果accessOrder为true，则说明访问模式，这时此方法会在访问数据后生效，即将访问的节点移动到链表末尾，同时要维护双向链表的结构，以及存储的头部和尾部。
```java
  // 访问节点之后调整链表结构
    // 将访问的节点移动到链表末尾
    void afterNodeAccess(Node<K,V> e) { // move node to last
        LinkedHashMap.Entry<K,V> last;
        // 如果 accessOrder 为 true 说明是访问模式，需要调整
        // 如果 accessOrder 为 false 说明是插入模式，不需要调整
        if (accessOrder && (last = tail) != e) {   //访问模式，并且这个值不在链表的末尾。
            LinkedHashMap.Entry<K,V> p =
                    (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
            // p 当前节点
            // b 前一个节点
            // a 后一个节点
            p.after = null;   //放到末尾所以after为null
            if (b == null)    //如果传入的是头节点
                head = a;   //则移动后，头节点更新为传入节点的after
            else                //如果不是头节点
                b.after = a;   //则前面节点b的after要指向a.
            if (a != null)          //如果传入节点不是尾结点
                a.before = b;     //则后面节点a的before要指向b
            else            //如果是尾结点，
                last = b;     //last赋值为传入节点的前一个节点。
            if (last == null)   //如果b==null.a==nuall.链表为空。
                head = p;      //头节点指向p。
            else {
                p.before = last;
                last.after = p;
            }
            tail = p;    //更新链表尾部指向p。
            ++modCount;
        }
    }
```
# 总结
在日常开发中，LinkedHashMap 的使用频率虽不及 HashMap，但它也个重要的实现。在 Java 集合框架中，HashMap、LinkedHashMap 和 TreeMap 三个映射类基于不同的数据结构，并实现了不同的功能。HashMap 底层基于拉链式的散列结构，并在 JDK 1.8 中引入红黑树优化过长链表的问题。基于这样结构，HashMap 可提供高效的增删改查操作。LinkedHashMap 在其之上，通过维护一条双向链表，实现了散列数据结构的有序遍历。TreeMap 底层基于红黑树实现，利用红黑树的性质，实现了键值对排序功能。
不难看出 LinkedHashMap 就只是在 HashMap 之上添加了一个 LinkedList 而已，并没有构造新的节点或结构，而且维护双向链表的代码也非常简单。
LinkedHashMap 中比较重要的是 accessOrder 属性，它定义了双向链表中节点的存储序列，可用于实现 LRU 等淘汰算法。
关于LRU的案例可参考：[LRU 和 LFU 缓存淘汰策略](https://blog.csdn.net/Victorgcx/article/details/104378378)


