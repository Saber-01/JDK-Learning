@[TOC](PriorityQueue基于jdk8源码学习)
# 概述
一个基于优先级堆的无界优先级队列。根据 Comparable 比较器的自然顺序确定优先级元素的排列顺序，或者根据构造队列时创建的 Comparator 比较器排列队列元素。优先级队列不允许 null 元素。依赖于自然顺序的优先级队列 也不允许插入不可比较的对象（这样做可能抛出 ClassCastException 异常）。
队列的头部元素是于指定顺序相关的最小的元素。如果多个元素满足该条件，那么头部元素是其中任意一个。队列的检索操作 poll, remove, peek, element 等会访问队列的头部元素。
注意该实现不是同步的。多线程不应该同时修改优先级队列，而应该使用线程安全的 PriorityBlockingQueue 类。
此实现提供了时间代价为 O(log(n)) 的入队和出队方法：offer, poll, remove,  add；提供了线性时间代价的 remove 和 contains 方法；除此之外，还有常数时间代价的 peek, element, size 方法。
## 原理
Java中PriorityQueue实现了Queue接口，不允许放入null元素；其通过堆实现，具体说是通过完全二叉树（complete binary tree）实现的小顶堆（任意一个非叶子节点的权值，都不大于其左右子节点的权值），也就意味着可以通过数组来作为PriorityQueue的底层实现。
实现的原理图如下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200716123420278.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
上图中我们给每个元素按照层序遍历的方式进行了编号，如果你足够细心，会发现父节点和子节点的编号是有联系的，更确切的说父子节点的编号之间有如下关系：

leftNo = parentNo*2+1

rightNo = parentNo*2+2

parentNo = (nodeNo-1)/2

通过上述三个公式，可以轻易计算出某个节点的父节点以及子节点的下标。这也就是为什么可以直接用数组来存储堆的原因。

PriorityQueue的peek()和element操作是常数时间，add(), offer(), 无参数的remove()以及poll()方法的时间复杂度都是log(N)。

## 继承关系
它是java集合框架中的一员，它的继承关系是：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200716122251169.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)

继承于抽象队列类，即PriorityQueue实现了基本的队列操作和集合操作。
实现了Serializable接口，说明该类可以序列化。
# 成员变量
默认初始容量为11，即未指定初始容量的构造函数创建队列时，初始容量为11。
可分配的最大的数组容量为Integer.MAX_VALUE - 8。
queue则是优先队列的底层数组。
```java
 private static final long serialVersionUID = -7720805057305804111L;
   //序列化，比较版本用的，对类功能没有影响
 
 /* The maximum size of array to allocate.
     *  数组最多能容纳的元素个数。
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     * 些虚拟机会保留一些头消息，占用部分空间。
     *   尝试分配比这个值更大的空间可能会抛出 OutOfMemoryError 错误：请求的
     *  数组大小超过了虚拟机的限制。
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private static final int DEFAULT_INITIAL_CAPACITY = 11;
     //默认的初始容量
    /**
     * Priority queue represented as a balanced binary heap: the two
     * children of queue[n] are queue[2*n+1] and queue[2*(n+1)].  The
     * priority queue is ordered by comparator, or by the elements'
     * natural ordering, if comparator is null: For each node n in the
     * heap and each descendant d of n, n <= d.  The element with the
     * lowest value is in queue[0], assuming the queue is nonempty.
     *
     * 优先级队列表现为一个平衡的二进制堆：queue[n] 的两个子队列分别为
     * queue[2*n+1] 和 queue[2*(n+1)]。如果队列的顺序由比较器决定，或者按
     * 元素的自然顺序排序。对于堆中的每个节点 n 和 n 的每个后代 d，有 n <= d。
     * 假定队列非空，堆中最小值的元素为 queue[0]。
     */
    transient Object[] queue; // non-private to simplify nested class access
    //非私有以简化嵌套类访问
    /**
     * The number of elements in the priority queue.
     * 优先级队列中的元素个数。
     */
    private int size = 0;

    /**
     * The comparator, or null if priority queue uses elements'
     * natural ordering.
     * 比较器。如果使用元素的自然顺序排序的话此比较器为 null。
     */
    private final Comparator<? super E> comparator;

    /**
     * The number of times this priority queue has been
     * <i>structurally modified</i>.  See AbstractList for gory details.
     * 优先级队列被结构性修改的次数。
     */
    transient int modCount = 0; // non-private to simplify nested class access
    
```
***
# 构造函数
有7个构造函数：
提供了可以指定初始容量和比较器的（可以同时指定，也可以都不指定，也可以指定其中一个）4个构造函数。
提供使用优先队列PriorityQueue和SortedSet类集合，或者普通集合 作为初始队列元素的3个构造方法。
未指定初始容量，使用DEFAULT_INITIAL_CAPACITY=11作为初始数组queue的长度。
未指定比较器comparator,则comparator为null。
```java
/**
     * 创建一个容量为默认初始容量的优先级队列。其中元素的顺序为
     * Comparable 自然顺序。
     */
    public PriorityQueue() {
        this(DEFAULT_INITIAL_CAPACITY, null);
    }

    /**
     * 创建一个特定初始容量的优先级队列，其中元素的顺序为 Comparable 的
     * 自然顺序。
     * @param initialCapacity the initial capacity for this priority queue
     * @throws IllegalArgumentException if {@code initialCapacity} is less
     *         than 1
     */
    public PriorityQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    /**
     *创建一个容量为默认初始容量，元素排列顺序为比较器指定顺序的优先级
     *     队列。
     * @param  comparator the comparator that will be used to order this
     *         priority queue.  If {@code null}, the {@linkplain Comparable
     *         natural ordering} of the elements will be used.
     * @since 1.8
     */
    public PriorityQueue(Comparator<? super E> comparator) {
        this(DEFAULT_INITIAL_CAPACITY, comparator);
    }

    /**
     *创建一个容量为默认初始容量，元素排列顺序为比较器指定顺序的优先级
     * 队列。
     * @param  initialCapacity the initial capacity for this priority queue
     * @param  comparator the comparator that will be used to order this
     *         priority queue.  If {@code null}, the {@linkplain Comparable
     *         natural ordering} of the elements will be used.
     * @throws IllegalArgumentException if {@code initialCapacity} is
     *         less than 1
     */
    public PriorityQueue(int initialCapacity,
                         Comparator<? super E> comparator) {
        // Note: This restriction of at least one is not actually needed,
        // but continues for 1.5 compatibility
        if (initialCapacity < 1)     //指定容量小于1，抛出异常，注意不允许指定容量为0
            throw new IllegalArgumentException();
        this.queue = new Object[initialCapacity];       //创建新数组
        this.comparator = comparator;       //比较器赋值
    }

    /**
     *创建一个包含指定集合所有元素的优先级队列。如果指定集合是一个
     *  SortedSet 的实例或者是另一个 PriorityQueue，这个优先级队列中的元素
     *  会以相同的顺序排列。否则，这个优先级队列将根据元素的自然顺序排列。
     * @param  c the collection whose elements are to be placed
     *         into this priority queue
     * @throws ClassCastException if elements of the specified collection
     *         cannot be compared to one another according to the priority
     *         queue's ordering
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    @SuppressWarnings("unchecked")
    public PriorityQueue(Collection<? extends E> c) {
        if (c instanceof SortedSet<?>) {      //判断是否是SortedSet
            SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
            this.comparator = (Comparator<? super E>) ss.comparator();   //赋值比较器
            initElementsFromCollection(ss);    //排序set版本的初始化值
        }
        else if (c instanceof PriorityQueue<?>) { //判断是否是PriorityQueue
            PriorityQueue<? extends E> pq = (PriorityQueue<? extends E>) c;
            this.comparator = (Comparator<? super E>) pq.comparator();//赋值比较器
            initFromPriorityQueue(pq);    //优先队列的初始化值
        }
        else {      //如果不是可排序的，则使用默认比较器
            this.comparator = null;   //   即比较器赋值为null
            initFromCollection(c);   //普通集合版本的初始化值
        }
    }

    /**
     *创建一个包含指定集合所有元素的优先级队列。这个优先级队列中的元素
     *   会以指定队列相同的顺序排序。
     * @param  c the priority queue whose elements are to be placed
     *         into this priority queue
     * @throws ClassCastException if elements of {@code c} cannot be
     *         compared to one another according to {@code c}'s
     *         ordering
     * @throws NullPointerException if the specified priority queue or any
     *         of its elements are null
     */
    @SuppressWarnings("unchecked")
    public PriorityQueue(PriorityQueue<? extends E> c) {    //和上面传入集合是优先队列时候处理一样
        this.comparator = (Comparator<? super E>) c.comparator();
        initFromPriorityQueue(c);
    }

    /**
     *创建一个包含指定 SortedSet 的所有元素的优先级队列。这个优先级队列
     *  中的元素以给定 SortedSet 的顺序排列。
     * @param  c the sorted set whose elements are to be placed
     *         into this priority queue
     * @throws ClassCastException if elements of the specified sorted
     *         set cannot be compared to one another according to the
     *         sorted set's ordering
     * @throws NullPointerException if the specified sorted set or any
     *         of its elements are null
     */
    @SuppressWarnings("unchecked")
    public PriorityQueue(SortedSet<? extends E> c) {      //和上面传入集合是排序set时处理一样。
        this.comparator = (Comparator<? super E>) c.comparator();
        initElementsFromCollection(c);
    }

```
***
使用不同类型集合创建优先队列时，会调用的三个方法：
```java
  // 根据给定的优先级队列初始化此优先级队列，将所有元素复制到此队列中
    // 直接将给定的优先级队列转化为数组，赋值给此队列
    private void initFromPriorityQueue(PriorityQueue<? extends E> c) {  //优先队列版本的初始化值
        if (c.getClass() == PriorityQueue.class) {   //先判断传入的是否是优先队列类
            this.queue = c.toArray();   //是的话，将指定优先队列转化为数组，赋值给当前队列底层数组queue
            this.size = c.size();
        } else {
            initFromCollection(c);      //如果发现不是优先队列，调用普通集合版本的初始化方法
        }
    }
    // 根据给定的 Collection 初始化此优先级队列，将所有元素复制到此队列中
    private void initElementsFromCollection(Collection<? extends E> c) {  //SortedSet版本，普通集合也会调用到
        Object[] a = c.toArray();    //得到集合的数组。
        // If c.toArray incorrectly doesn't return Object[], copy it.
        //如果不是返回Object数组，则使用Array.copyOf进行类型转化。
        if (a.getClass() != Object[].class)
            a = Arrays.copyOf(a, a.length, Object[].class);
        int len = a.length;     //得到元素个数
        if (len == 1 || this.comparator != null)  //有排序功能集合，或者只有一个元素的集合执行检查空值。
            for (int i = 0; i < len; i++)   //判断集合得到的数组是否全不为null，因为优先队列不允许元素为null，
                if (a[i] == null)  //只要有一个为null,就会抛出空指针异常
                    throw new NullPointerException();
        this.queue = a;
        this.size = a.length;     //排完序的集合初始化到这步就完成。
    }

    /**
     * Initializes queue array with elements from the given Collection.
     * 根据给定的集合中的元素初始化此队列。普通集合版本，
     * @param c the collection
     */
    private void initFromCollection(Collection<? extends E> c) {  //普通集合版本
        initElementsFromCollection(c);
        heapify();              //堆处理
    }
```
# 扩容
PriorityQueue,底层实现时数组，和集合框架中使用数组作为底层结构的类一样，都需要考虑扩容的问题，即在元素添加到数组之前，需要判断数组容量是否够，以及不够时候，怎么进行扩容。PriorityQueue类中使用了grow方法，该方法主要在add 和 offer添加元素时，如果容量不够就要调用此方法扩容。
扩容结果：
如果就容量小于64，则新容量为老容量*2+2。
如果大于等于64，则扩容为老容量的1.5倍。
如果新容量超出MAX_ARRAY_SIZE ，则判断minCapacity是否超出MAX_ARRAY_SIZE ，
超出则用 Integer.MAX_VALUE，否则用MAX_ARRAY_SIZE。
```java
 /**
     * Increases the capacity of the array.
     *扩大数组的容量
     * @param minCapacity the desired minimum capacity
     */
    private void grow(int minCapacity) {
        int oldCapacity = queue.length;    //记录旧容量
        // Double size if small; else grow by 50%
        //如果容量小于64，则扩容的新容量为2*老容量+2，
        //否则新容量为老容量的1.5倍。
        //因为容量都是+1然后判断，并且最小容量为1，则扩容以后的新容量肯定大于minCapacity
        //这和ArrayList不同，不需要判断扩容以后，新容量是否大于minCapacity
        int newCapacity = oldCapacity + ((oldCapacity < 64) ?
                (oldCapacity + 2) :
                (oldCapacity >> 1));
        // overflow-conscious code
        // 如果新容量比规定的最大容量还要大，那么将新容量设置为整型最大值
        if (newCapacity - MAX_ARRAY_SIZE > 0)     //如果扩容大于最大允许的数组长度
            newCapacity = hugeCapacity(minCapacity); //调用hugeCapacity进行判断。
        queue = Arrays.copyOf(queue, newCapacity);   //同样适用Array.copyOf进行复制。
    }
    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // 溢出
            throw new OutOfMemoryError();
        // 指定容量大于规定的最大容量，将新容量设置为整型最大值，否则设置
        // 为规定的最大容量。
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }
```
***
# 关键方法
主要是队列方法，但是每一步对优先队列的结构修改都要维护最小堆/最大堆的结构。
维护结构的主要方法是siftDown和siftUp。
而如果从一个无序集合创建优先队列，还需要调用heapify，建立整个最小堆/最大堆。
## siftDown 和 siftUp
（1）siftUp方法，即插入一个值为x的元素到k位置时，因为优先队列要维护最小堆的结构，所以需要上移和下移元素，而siftUp，则是上移操作，它的原理图是：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200716150611472.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
即当元素添加到队列末尾时，需要上调至合适位置以满足整个数组时最小堆的结构。即在最小堆中任意父节点要小于等于其孩子节点的值。
所以siftUp移动的原理是，从插入位置k开始，通过parent=（k-1）>>>1，获得父节点的索引位置。
然后判断插入值是否大于等于父节点值，如果满足，则说明已经是最小堆结构，停止循环。
如果不满足，说明，需要将当前父节点下放到k位置，然后k更新为parent，下次循环将取此次循环的父节点的父节点进行比较，这样就实现了将插入位置层层上移的过程。
知道满足插入值大于等于父节点值，就停下，此时的k就是插入值应该放置在优先队列中的索引。
源码实现如下：
注意：根据是否指定了比较器，分为两个siftUp方法，而这两个方法原理相同，只是比较时代码略有不同
```java
 /**
     * Inserts item x at position k, maintaining heap invariant by
     * promoting x up the tree until it is greater than or equal to
     * its parent, or is the root.
     *在 k 位置插入元素 x（从 k 位置开始调整堆，找到元素 x 的位置，忽略
     * 原先 k 位置的元素），通过向上提升 x 直到它大于等于它的父元素或者
     *   根元素，来保证满足堆成立的条件。
     * To simplify and speed up coercions and comparisons. the
     * Comparable and Comparator versions are separated into different
     * methods that are otherwise identical. (Similarly for siftDown.)
     *为了简化和加速强制转换和比较，Comparable（元素的默认比较器）
     * 和 Comparator（指定的比较器）被分成不同的方法，这两个方法基本
     * 等同。（siftDown 同理）
     * @param k the position to fill
     * @param x the item to insert
     */
    private void siftUp(int k, E x) {
        if (comparator != null)       //有指定的比较器。
            siftUpUsingComparator(k, x);
        else        //如果没有指定，即元素实现了Comparable接口，则使用元素默认比较器。
            siftUpComparable(k, x);
    }

    @SuppressWarnings("unchecked")
    //传入的参数是，出插入的x,和填充到队列中的初始位置k。
    private void siftUpComparable(int k, E x) {   //元素默认比较器的向上移动插入元素的版本。
        // <? extends E>，集合中元素类型上限为 E，即只能是 E 或者 E 的子类
        // <? super E>，集合中元素类型下限为 E，即只能是 E 或者 E 的父类
        //Comparable<? super E>表示实现了Comparable接口的类，且这个接口中T是E本身或父类。
        Comparable<? super E> key = (Comparable<? super E>) x;
        while (k > 0) {
            int parent = (k - 1) >>> 1;      //获取k的父节点。
            Object e = queue[parent];      //取得父节点
            if (key.compareTo((E) e) >= 0)    //如果父节点和插入值相比，插入值大于等于父节点的值。
                break;   //说明不需要上移了
            queue[k] = e;      //如果父节点值大于插入值，则父节点交换到当前k,即当前比较的父节点的子节点位置
            k = parent;    //更新k位置原先父节点位置，进行下一次判断。即代表插入的节点本次循环上移到该位置。
        }
        queue[k] = key;     //找到插入值需要上移到的位置后，将数组中对应位置，赋值为插入元素的值。
    }

    @SuppressWarnings("unchecked")
    private void siftUpUsingComparator(int k, E x) {//使用了比较器的上移插入元素的版本
        while (k > 0) {
            int parent = (k - 1) >>> 1;         //获取k的父节点。
            Object e = queue[parent];    //获取父节点值
            if (comparator.compare(x, (E) e) >= 0)  //如果插入值大于等于父节点的值。
                break;  //上移完毕
            queue[k] = e;     //否则，将父节点下移到当前的k位置，即当前比较父节点的子节点位置，
            k = parent;    //更新k位置原先父节点位置，进行下一次判断。即代表插入的节点本次循环上移到该位置。
        }
        queue[k] = x;//找到插入值需要上移到的位置后，将数组中对应位置，赋值为插入元素的值。
    }
```
***
（2）siftDown 方法用于在指定位置插入指定元素，然后向下调整 x （和其子节点进行比较）直到满足堆成立的条件。在poll方法中，下移的是队列末尾的元素。
原理如下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200716151753902.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
当一个元素插入到队列头时，需要将其下移，以满足最小堆的结构。siftDown方法就是实现在指定位置k,插入元素后，进行下移维护堆结构。
siftDown的实现原理：
首先是获取当前位置的左子节点：child= (k<<1)+1,
再获得右子节点：right = (k<<1)+2;
取得c为左右节点中值最小的那一个，如果左右子节点相等，则赋值为左子节点。
使用c与当前k位置上的值key（也就是传入的x）作比较，如果key>c，则需要下移
下移就是就是将左右子节点中大的那个值交换到当前父节点，queue[k]=c。
而后k更新为左右子节点中大的那个的索引，下一轮循环将和当前值偏小的那个子节点的左右子节点比较。
直到满足，当前插入的值小于等于本次循环的左右子节点的最小值。那么停止循环，这时已经不需要下移了。而注意，任何时候这个k，要保证大于size的一半，因为只有下移到非叶子节点，才会需要继续判断。
源码实现如下：
注意：根据是否指定了比较器，分为两个siftUp方法，而这两个方法原理相同，只是比较时代码略有不同
```java
    /**
     * Inserts item x at position k, maintaining heap invariant by
     * demoting x down the tree repeatedly until it is less than or
     * equal to its children or is a leaf.
     * 在 k 位置插入元素 x，通过向下调整 x 直到它小于等于它的子节点或者
     *   叶节点，来保证满足堆成立的条件。
     * @param k the position to fill
     * @param x the item to insert
     */
    private void siftDown(int k, E x) {   //向下调整插入的元素
        if (comparator != null)
            siftDownUsingComparator(k, x);  //使用Comparator比较器方法
        else
            siftDownComparable(k, x);       //元素本身是实现了Comparable
    }

    @SuppressWarnings("unchecked")
    //传入的是，初始插入位置为k，值为x的元素
    private void siftDownComparable(int k, E x) {   //元素本身实现了Comparable
        Comparable<? super E> key = (Comparable<? super E>)x;    //转化为实现了Comparable接口的类，且接口是E及E的父类的
        int half = size >>> 1;        //    //得到队列中间一半的位置索引
        //如果k小于中间位置索引，则循环，因为叶堆的叶子节点索引大于中间的索引。
        //即只要k,为非叶子节点，就循环。
        while (k < half) {
            int child = (k << 1) + 1; // assume left child is least  //得到k位置的孩子左子节点索引，
            Object c = queue[child];   //取得左子节点值
            int right = child + 1;   //取得右子节点的索引，
            if (right < size &&         //如果有右子节点
                    ((Comparable<? super E>) c).compareTo((E) queue[right]) > 0) //比较，如果右子节点值小于左子节点
                c = queue[child = right];    //则c赋值为右子节点，即c会赋值为，左右子节点中较小的数，如果左右相等，取左子节点。
            //使用左右子节点最大的值和传入的元素值进行比较，
            //因为下移，所以直到元素值小于等于当前左右子节点中最小值，说明不需要下移了。
            if (key.compareTo((E) c) <= 0)
                break;
            queue[k] = c;       //如果不满足，则将较小的孩子节点，交换到其父节点位置，即当前的k位置。
            k = child;              //k更新为较小的孩子节点的位置，方便下次判断。
        }
        queue[k] = key;    //找到插入值应该放的位置后，放入元素值。
    }

    @SuppressWarnings("unchecked")
    //和上面类似
    private void siftDownUsingComparator(int k, E x) {
        int half = size >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            Object c = queue[child];
            int right = child + 1;
            if (right < size &&
                    comparator.compare((E) c, (E) queue[right]) > 0)  //比较时使用this.comparator.compare方法比较。
                c = queue[child = right];  //c赋值为较小的孩子节点值
            if (comparator.compare(x, (E) c) <= 0)    //直到插入的节点大于等于当前左右子节点最小值
                break;      //结束下移
            queue[k] = c;
            k = child;
        }
        queue[k] = x;
    }

```
##  heapify 和 removeAt
(1)heapify 
将整个堆进行最小堆调整，思想就是从非叶子节点开始进行下移操作，倒序操作说明方法建立最小堆/最大堆是从下到上的。即先让最底层符合最小堆/最大堆结构然后保证上层的下移操作交换的都是当前其子堆中最小/最大的值。只要这样遍历到队列头，每一次都进行下移操作即可实现将整个队列变为优先队列
即建立起最小堆/最大堆。
```java
  /**
     * Establishes the heap invariant (described above) in the entire tree,
     * assuming nothing about the order of the elements prior to the call.
     * 在整个树中建立堆不变量（如上所述），
     *
     * 不假设调用之前元素的顺序。
     */
    @SuppressWarnings("unchecked")
    //将整个堆进行最小堆调整，思想就是从非叶子节点开始进行下移操作
    //倒序操作，说明方法建立最小堆/最大堆是从下到上的。即先让最底层符合最小堆/最大堆结构
    //然后保证上层的下移操作交换的都是当前其子堆中最小/最大的值。
    //只要这样遍历到队列头，每一次都进行下移操作即可实现将整个队列变为优先队列
    //即建立起最小堆/最大堆。
    private void heapify() {
        for (int i = (size >>> 1) - 1; i >= 0; i--) //即从队列中间位置开始遍历
            siftDown(i, (E) queue[i]);       //逐个进行下移操作，
    }
```
***
(2)removeAt
在优先队列的头和尾部插入，只需要下移和上移操作，而在指定位置删除后，会把队列最后一个元素插入到删除的这个位置上，而这时候，有可能需要上移，也有可能需要下移。因为最小堆的左右子树无法保证大于还是小于的情况。
所以removeAt首先调用siftDown(i, moved)，如果发现，这时queue[i]==moved还是成立，说明没有发生下移，则还需要调用siftUp(i, moved)上移操作，如果发现queue[i] != moved，即至少发生了一次上移，
说明队列后的元素，跑到了i索引前，这样迭代器的遍历就无法遍历到这个值了，所以这时removeAt返回moved，让迭代器iterator.remove方法调用此方法后，判断返回值如果不为null，即将这个值将入到丢失队列中。如果插入末尾元素，未发生上移操作，则返回null，让迭代器知道没有发生数据丢失，那迭代器就正常迭代。
```java
  /**
     * Removes the ith element from queue.
     *从队列中删除第 i 个元素。
     * Normally this method leaves the elements at up to i-1,
     * inclusive, untouched.  Under these circumstances, it returns
     * null.  Occasionally, in order to maintain the heap invariant,
     * it must swap a later element of the list with one earlier than
     * i.  Under these circumstances, this method returns the element
     * that was previously at the end of the list and is now at some
     * position before i. This fact is used by iterator.remove so as to
     * avoid missing traversing elements.
     * 即删除一个值，就是将队列最后一个元素，提到这个i位置上
     * 然后先进行下移操作，因为下移操作只会将这个元素移到i后面的索引，
     * 并且后面索引上的值也只会移到i包括i后面，这些元素在迭代器遍历是，都不回丢失
     * 索引如果只是下移操作，直接返回null。
     * 但是如果下移操作没有执行，即queue[i]==moved,则说明应该进行上移判断
     * 如果发生了一次上移操作，说明这个原来在索引i后的元素，移动到了索引i之前，
     * 那么迭代器遍历就会缺失这个元素，所以要返回moved进行额外保存。
     */
    @SuppressWarnings("unchecked")
    private E removeAt(int i) {
        // assert i >= 0 && i < size; 断言，i肯定满足合法性
        modCount++;
        int s = --size;   //size更新后，赋值给s
        if (s == i) // removed last element //如果i==s,说明是最后一个元素的索引
            queue[i] = null;     //直接令最后一个元素为null。
        else {
            E moved = (E) queue[s];             //先保存队列最后一个元素值。
            queue[s] = null;          //将最后一个位置赋值为null。
            siftDown(i, moved);  //将最后这个值插入到当前删除位置的地方，并进行堆下移操作。
            // 如果没有向下调整，说明可能它的位置在上面，接着向上调整
            if (queue[i] == moved) {   //当前i上的值还是下移前保存的值，说明没有进行下移
                siftUp(i, moved);      //则进行上移
                if (queue[i] != moved)      //如果当前i不等于上移前保存的值，说明至少上移了一次。
                    return moved;      //返回原队列末尾的值
            }
        }
        return null;  //否则返回null。
    }
```
## add 和 offer
这两个方法原理相同，先检查插入值是否为null，再检查是否需要扩容。
如果不是第一次添加，即添加后队列不止一个元素，则需要调用siftUp进行上移。
原理见上面siftUp方法

```java
 /**
     * Inserts the specified element into this priority queue.
     *将指定元素插入到优先级队列中
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws ClassCastException if the specified element cannot be
     *         compared with elements currently in this priority queue
     *         according to the priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        return offer(e);
    }

    /**
     * Inserts the specified element into this priority queue.
     *将指定元素插入到优先级队列中
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws ClassCastException if the specified element cannot be
     *         compared with elements currently in this priority queue
     *         according to the priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        if (e == null)    //为null会抛出空指针异常
            throw new NullPointerException();
        modCount++;
        int i = size;
        if (i >= queue.length)   //判断是否需要扩容
            grow(i + 1);
        size = i + 1;  //更新size
        if (i == 0)     //如果是第一次添加元素，
            queue[0] = e;
        else       //不是第一次，需要调用siftUp进行添加，因为需要维护堆的结构
            siftUp(i, e);
        return true;
    }
```
## element 和 peek
原理如下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200716154617523.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
返回数组中第一个元素值，即堆顶。
element 实际上使用的是子类AbstractQueue中的方法
```java
 public E element() {
        E x = peek();
        if (x != null)
            return x;
        else
            throw new NoSuchElementException();
    }
```
peek在priorityqueue中定义为：
```java
 @SuppressWarnings("unchecked")
    public E peek() {   //取得队列头部元素，队列为空返回null
        return (size == 0) ? null : (E) queue[0];
    }
```
***
## remove 和 poll
:这两个方法原理相同，删除队列头部元素，并返回这个值，删除后，即将队列末尾的值填充到队首，然后进行siftDown下移操作。
原理见上面siftDown方法
remove 实际上使用的是子类AbstractQueue中的方法
```java
 public E remove() {
        E x = poll();
        if (x != null)
            return x;
        else
            throw new NoSuchElementException();
    }
```
poll在priorityqueue中定义为：
```java
 @SuppressWarnings("unchecked")
    //实际上就是取出队首元素，即堆顶元素，然后用最后一个队列元素填充
    //然后在进行下移操作，因为放到了队首，只可能进行siftDown操作。
    public E poll() {           //弹出队列头部元素，队列为空返回null。
        if (size == 0)
            return null;
        int s = --size;        //size自减，并取得最后一个元素的值
        modCount++;
        E result = (E) queue[0];         //保存队列头部元素值，用于返回
        E x = (E) queue[s];                //保存队尾元素值，用于比较
        queue[s] = null;      //队列为空赋值为null。
        if (s != 0)           //只要原先队列不止一个元素，
            siftDown(0, x);       //就从0队首位置开始进行下移操作。
        return result;     //返回堆顶值
    }
```

# PriorityQueue 小结
* 扩容机制和线性数据结构的机制基本相似，一般情况下扩容为 1.5 * oldCapacity 或者 2 * oldCapacity + 2。

* 队列里不允许 null 元素。

* 底层数据结构是存储在使用数组实现的堆，插入删除操作依赖堆的调整函数 siftUp 和 siftDown，对于不同的比较器，堆的调整过程中的判断条件也不同。
# 参考：
https://www.cnblogs.com/Elliott-Su-Faith-change-our-life/p/7472265.html
https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/Collections/PriorityQueue.md
