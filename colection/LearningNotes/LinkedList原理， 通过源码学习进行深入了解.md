@[TOC](LinkedList 基于源码学习)
# 概述
链表（Linked list）是一种常见的基础数据结构，是一种线性表，但是并不会按线性的顺序存储数据，而是在每一个节点里存到下一个节点的地址。
链表可分为单向链表和双向链表。

一个单向链表包含两个值: 当前节点的值和一个指向下一个节点的链接。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200713151942476.png)

一个双向链表有三个整数值: 数值、向后的节点链接、向前的节点链接。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200713151958325.png)


Java LinkedList（链表） 类似于 ArrayList，是一种常用的数据容器，它采用的是双向链表的设计，每个节点存储数值以外，还存储了向前和向后的节点引用。

与 ArrayList 相比，LinkedList 的增加和删除对操作效率更高，而查找和修改的操作效率较低。
## 继承关系
它是java集合框架中的一员，它的继承关系是：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200713152406542.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)

可以看到LinkedList继承了AbstractSequentialList,实现了四个接口：
（1）Serializble。即它支持序列化
（2）Cloneable。实现了Cloneable接口，即覆盖了函数clone()，能被克隆
（3）List, 实现了List接口，说明实现了线性表的所有操作。
（4）Deque,实现了该接口，说明实现了双向队列的所有操作。
# 成员变量
由于 LinkedList 链表由节点连接而成，所以类属性中，不需要数组作为支撑，也不需要“列表容量”等属性。设置 first 指向链表中第一个节点，设置 last 指向链表最后一个节点，然后size记录一个所有元素的个数。
```java
//序列化时用到，用于比较版本，对类功能没有意义。
private static final long serialVersionUID = 876323262645176354L;
  transient int size = 0;// 链表中元素的个数，transient 表示序列化时不包括该变量

    /**
     * Pointer to first node.指向第一个节点的指针
     * Invariant: (first == null && last == null) ||
     *            (first.prev == null && first.item != null)
     */
    transient Node<E> first;

    /**
     * Pointer to last node.  指向链表的最后一个节点的 指针
     * Invariant: (first == null && last == null) ||
     *            (last.next == null && last.item != null)
     */
    transient Node<E> last;

    /**
     * Constructs an empty list.
     * 无参构造函数，创建一个空链表。
     */
```
# 内部节点类
列表中，并不是直接存储数据值，而是将数据值放在节点类中，列表再存储和链接各个节点。
列表节点类的定义如下：
属性 item 用来存储元素的值，next 指向下一个节点，prev 指向上一个节点。
```java
 private static class Node<E> {      //链表的节点类
        E item;       //存储元素值
        Node<E> next;     //上一个节点的引用
        Node<E> prev;     //下一个节点的引用

        Node(Node<E> prev, E element, Node<E> next) {     //构造函数。
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }
```
# 构造函数
因为链表是有每个节点连接而成，不存在扩容问题，理论上，只要内存够大，链表是无限制容量的。所以和底层为数组实现的线性表不同，其初始化不需要指定相关容量的参数。所以只有2个构造函数：
```java
/**
     * Constructs an empty list.
     * 无参构造函数，创建一个空链表。
     */
    public LinkedList() {
    }

    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *构造函数：构造一个包括指定集合所有元素的列表，按照集合迭代器
     *  返回的顺序。
     *  使用指定集合创建列表，若集合为null，会抛出异常
     * @param  c the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     */
    public LinkedList(Collection<? extends E> c) {
        this();                //实际是调用无参构造函数构建列表后，再调用addAll。
        addAll(c);
    }
```
# 成员方法
## 内部工具类方法
因为链表同时实现了List接口和Deque接口，这两个接口方法中，有些方法的实现原理其实是相同的，即这些操作有些是有交集的，所以LinkedList首先将被内部方法调用的方法抽离出来，以下我自己将它们称为了内部工具类方法，这些方法的限定词要么是默认的，要么是private，所以用户一般是访问不到的。
具体有以下几个方法：
方法名     | 参数 | 作用  |
-------- | ----- |------ |
linkFirst|待插入的节点元素值e  | 在表头添加元素值为e的节点 |
linkLast|待插入节点元素值e  | 在表尾添加元素值为e的节点 |
linkBefore|待插入的节点元素值e和指定节点succ  |在指定的节点 succ 之前插入节点值为E的节点  |
unlinkFirst| 指定的头节点f |移除头结点f，并返回 f 的值  |
unlinkLast| 指定的尾节点l | 删除表尾节点l。并返回表尾节点值。 |
unlink| 指定的任意节点x |  删除一个非空节点 x，并返回删除节点的值|
node|索引index| 返回指定位置index的节点
***
### linkFirst、linkLast、linkBefore 方法
```java
 /**
     * Links e as first element.
     *  在表头添加元素。
     */
    private void linkFirst(E e) {
        final Node<E> f = first;   //记录表头节点
        //创建一个新的节点，next赋值为存放的先前头结点f，prev指向null，因为是表头节点
        final Node<E> newNode = new Node<>(null, e, f);
        first = newNode;   //更新列表的头结点。
        if (f == null)      //如果刚开始列表头结点为null，说明列表为空，这时，还要更新last指向新建的头结点。
            //因为如果列表只有一个节点，那么first==last==newNode。
            last = newNode;
        else
            f.prev = newNode;      //创建新节点后，还要将原来头结点f(现在变成第二个节点)的prev指针指向新建的表头节点。
        size++;    //长度加1。
        modCount++;
    }

    /**
     * Links e as last element.
     * 在表尾添加元素
     */
    void linkLast(E e) {
        final Node<E> l = last;   //记录尾结点
        //创建一个新的节点，prev指向先前的尾结点l,next指向null，因为新节点要作为表尾节点。
        final Node<E> newNode = new Node<>(l, e, null);
        last = newNode;       //创建好后，更新列表的表尾节点。
        if (l == null)       //如果一开始表尾节点为null，说明列表为空，
            first = newNode;  //需要将first也赋值为新节点。因为如果列表只有一个节点，那么first==last==newNode。
        else
            l.next = newNode;    //如果不为空，那么先前的表尾节点l(现在变成倒数第二个节点)的next要更新为新建的表尾节点
        size++;   //长度加1。
        modCount++;
    }

    /**
     * Inserts element e before non-null Node succ.
     * 在指定的非空节点 succ 之前插入节点值为E的节点
     */
    void linkBefore(E e, Node<E> succ) {
        // assert succ != null;
        final Node<E> pred = succ.prev;  //先记录指定的非空节点的前一个节点的引用prev
        //新建一个节点值为e的节点，next指向指定的succ节点，pre指向事先记录的节点引用pred。
        final Node<E> newNode = new Node<>(pred, e, succ);
        succ.prev = newNode;             //指定节点的prev更新为插入的新节点
        if (pred == null)            //需要判断指定的节点是否是表头节点，
            first = newNode;      //如果是，则列表记录的表头节点要更新，
        else                    //如果不是表头节点，
            pred.next = newNode;     //则将pred指向的指定节点succ的前一个节点的next指向新插入的节点。
        size++;        //列表元素个数加1
        modCount++;
    }
```
***
### unlinkFirst、unlinkLast、unlink 方法
```java
    /**
     * Unlinks non-null first node f.
     *  移除头结点f，并返回 f 的值
     *
     */
    private E unlinkFirst(Node<E> f) {
        // assert f == first && f != null;
        final E element = f.item;    //先记录f节点的item值用于返回
        final Node<E> next = f.next;       //记录f节点的下一个节点
        f.item = null;        //设置f节点中的属性为null，方便GC
        f.next = null; // help GC
        //因为会删除头结点，所以直接头结点更新为f的下一个节点即可
        first = next;
        if (next == null)    //还要判断f是不是表尾节点，
            last = null;       //如果f是表尾节点， 那么next就为null，这时列表为空，表尾节点也要赋值为null。
        else              //如果f不是表尾节点。
            next.prev = null;       //需要将f的下一个节点的prev属性设置为null，因为它已经变成头结点。
        size--;           //删除头结点后 ，元素个数减一
        modCount++;
        return element;
    }

    /**
     * Unlinks non-null last node l.
     * 删除表尾节点。并返回表尾节点值。
     */
    private E unlinkLast(Node<E> l) {
        // assert l == last && l != null;
        final E element = l.item;  // 先记录表尾节点的item值，用于返回
        final Node<E> prev = l.prev;     //记录表尾节点的前一个节点
        l.item = null;         //将表尾节点中的属性设置为null，方便GC
        l.prev = null; // help GC
        last = prev;              //删除的是尾结点，所以列表的尾结点直接更新为l的前一个节点。
        if (prev == null)    // 要判断l是不是表头节点，即判断原先列表是不是只有一个节点
            first = null;        //如果原先列表只有一个节点，删除后，列表为空，表头节点也要设置为null。
        else
            prev.next = null;   //如果l不是尾结点，则需要将l的上一个节点的next属性更新为null，因为它变成了尾结点。
        size--;//删除尾结点后 ，元素个数减一
        modCount++;
        return element;
    }

    /**
     * Unlinks non-null node x.
     * 删除一个非空节点 x，并返回删除节点的值，
     * 考虑了 节点是 表头节点/表尾节点的情况。
     */
    E unlink(Node<E> x) {
        // assert x != null;   //确定节点不为null
        final E element = x.item;    // 记录节点item值用于返回
        final Node<E> next = x.next;   //记录节点的下一个节点
        final Node<E> prev = x.prev;   //记录节点的上一个节点

        if (prev == null) {        //如果上一个节点为null，说明x为表头节点，
            first = next;             //删除就是将表头节点直接定义成x的下一个节点。
        } else {             //如果x存在上一个节点，
            prev.next = next;      //则上一个节点的next要更新为x的下一个节点，
            x.prev = null;           //x.prev属性利用完成赋值为null，方便GC
        }

        if (next == null) {    //如果下一个节点为null，即x为表尾节点，
            last = prev;          //则删除会更新表尾节点，即表尾节点直接赋值为x的上一个节点
        } else {                 //如果x存在下一个节点，
            next.prev = prev;     //则下一个节点的prev属性更新成x的上一个节点。
            x.next = null;             //x.next属性利用完成赋值为null，方便GC
        }

        x.item = null;       //将item赋值为null，方便GC
        size--;     //删除一个节点，所以列表size--
        modCount++;
        return element;
    }
```
***
### node方法
```java
 /**
     * Returns the (non-null) Node at the specified element index.
     * 返回指定索引处的节点
     */
    Node<E> node(int index) {
        // assert isElementIndex(index);
        //确定调用时，index是合法的
        //注意因为是双向链表，可以从表头或者表尾都可以进行遍历
        //为了更快的遍历，肯定是选择更靠近索引index的那一端开始遍历
        //即看index是落在链表的哪一半部分。
        if (index < (size >> 1)) {     //如果index落在前半部分，
            Node<E> x = first;    //则从头结点开始遍历。
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else {                        //如果index落在后半部分
            Node<E> x = last;             //则从尾结点开始遍历。
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }
```
 ***
 ### 检查索引合法性的方法
```java
    /**
     * Tells if the argument is the index of an existing element.
     * 判断索引是不是列表中元素的索引。
     */
    private boolean isElementIndex(int index) {
        return index >= 0 && index < size;
    }

    /**
     * Tells if the argument is the index of a valid position for an
     * iterator or an add operation.
     *判断索引对于迭代器和add操作是否是合法的。
     */
    private boolean isPositionIndex(int index) {
        return index >= 0 && index <= size;
    }

    /**
     * Constructs an IndexOutOfBoundsException detail message.
     * Of the many possible refactorings of the error handling code,
     * this "outlining" performs best with both server and client VMs.
     *  出现 IndexOutOfBoundsException 异常时的详细信息。
     *    在错误处理代码许多可能的重构中，这种“描述”对服务器和客户端
     *    虚拟机都表现最好。
     */
    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size;
    }

    private void checkElementIndex(int index) {  //检查索引的合法性，取元素值版本的
        if (!isElementIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));  //不满足抛出异常
    }

    private void checkPositionIndex(int index) {    //检查索引的合法性。add和迭代器版本的
        if (!isPositionIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));//不满足抛出异常
    }
```
***
## Deque相关方法
LinkedList实现了双端队列的接口，而双端队列的接口中，包含了自身、单向队列(Queue)、栈中的方法。
注意：以下列举的是Deque接口源文件中对于单向队列、栈、双端队列的方法的一些规定。
***
### 单向队列方法
方法     | 作用
-------- | -----
 public boolean add(E e)| 向队列尾添加一个元素，失败抛出异常
 public boolean offer(E e)|向队列尾添加一个元素，失败返回false
  public E element()|得到队列头部的元素值，失败抛出异常
 public E peek()|得到队列头部的元素值，失败返回null
 public E remove()|删除并返回队列头部的元素值，失败抛出异常
public E poll()|删除并返回队列头部的元素值，失败返回null
注意：队列方法对于添加、删除、取值操作，都实现了2种版本的方法，一种失败会抛出异常，一种失败不会抛出异常。而向队列尾添加元素的操作add和offer在LinkList中，因为链表是无限容量的，一般不存在列表无法容纳元素的情况，所以在LinkList的所有添加操作中，都不回抛出此类异常。即add和offer以及下面的addFirst、addLast、offerFirst、offerLast都是不会抛出异常的。

以上表格中方法在LinkedList中的实现如下(有些等同于双端队列的方法，甚至直接调用双端队列方法)：

```java
/**
     * Appends the specified element to the end of this list.
     *在列表尾部添加元素
     *
     * <p>This method is equivalent to {@link #addLast}.
     * 此方法等同于 addLast
     * @param e element to be appended to this list
     * @return {@code true} (as specified by {@link Collection#add})
     */
    public boolean add(E e) {
        linkLast(e);
        return true;
    }
     /**
     * Adds the specified element as the tail (last element) of this list.
     * 把指定元素添加到列表末尾
     * 等同于offerLast 以及 add
     * @param e the element to add
     * @return {@code true} (as specified by {@link Queue#offer})
     * @since 1.5
     */
    public boolean offer(E e) {
        return add(e);
    }
     /**
     * Retrieves, but does not remove, the head (first element) of this list.
     *检索但不删除链表的头部（第一个元素）
     *  列表为空抛出异常
     *  注意，会抛出异常
     * @return the head of this list
     * @throws NoSuchElementException if this list is empty
     * @since 1.5
     */
    public E element() {      //方法等同于getFirst
        return getFirst();
    }
    /**
     * Retrieves, but does not remove, the head (first element) of this list.
     *检索但不删除列表的头元素（第一个元素）
     *  不会抛出任何异常
     *  区别element，其会抛出异常。
     *  方法等同于peekFirst
     * @return the head of this list, or {@code null} if this list is empty
     * @since 1.5
     */
    public E peek() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;//头结点为空返回null，不为空，返回头结点item值
    }
    /**
     * Retrieves and removes the head (first element) of this list.
     *检索并删除列表的头元素（第一个元素）
     *  列表为空抛出异常
     *  方法等同于removeFirst
     * @return the head of this list
     * @throws NoSuchElementException if this list is empty
     * @since 1.5
     */
    public E remove() {
        return removeFirst();
    }
 /**
     * Retrieves and removes the head (first element) of this list.
     *检索并移除链表的头元素（第一个元素）
     * 列表为空时返回 null，不抛出异常
     * 等同于pollFirst
     * @return the head of this list, or {@code null} if this list is empty
     * @since 1.5
     */
    public E poll() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }
```
***
### 栈的方法
方法     | 作用
-------- | -----
public E pop()|弹出栈顶元素并返回值，失败抛出异常。
public E peek()|取得栈顶元素的值，失败返回null
public void push(E e)|向栈推入一个元素，失败抛出异常。
注意：栈和单向队列中peek是同一方法。
它们在LinkedList中实现如下：
```java
 /**
     * Pops an element from the stack represented by this list.  In other
     * words, removes and returns the first element of this list.
     *从列表所代表的栈里 pop 出一个元素，即移除并返回列表的第一个元素。
     * 如果列表为空抛出异常。
     * <p>This method is equivalent to {@link #removeFirst()}.
     *
     * @return the element at the front of this list (which is the top
     *         of the stack represented by this list)
     * @throws NoSuchElementException if this list is empty
     * @since 1.6
     */
    public E pop() {
        return removeFirst();
    }
    /**
     * Pushes an element onto the stack represented by this list.  In other
     * words, inserts the element at the front of this list.
     * 将指定元素 push 到列表所代表的的栈里，即插入到列表表头。
     *
     * <p>This method is equivalent to {@link #addFirst}.
     *
     * @param e the element to push
     * @since 1.6
     */
    public void push(E e) {
        addFirst(e);
    }
     /**
     * Retrieves, but does not remove, the head (first element) of this list.
     *检索但不删除列表的头元素（第一个元素）
     *  不会抛出任何异常
     *  区别element，其会抛出异常。
     *  方法等同于peekFirst
     * @return the head of this list, or {@code null} if this list is empty
     * @since 1.5
     */
    public E peek() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;//头结点为空返回null，不为空，返回头结点item值
    }
```
***
### 双端队列方法
方法     | 作用
-------- | -----
 public void addFirst(E e)|向双端队列头部加入元素e，等同栈方法push
 public void addLast(E e) |向双端队列尾部加入元素e，等同单向队列add
 public boolean offerFirst(E e)|向双端队列头部加入元素e，返回true
 public boolean offerLast(E e)|向双端队列尾部加入元素e,返回true，等同单向队列offer
  public E getFirst()|获取队列头部元素，失败抛出异常，等同单向队列element
  public E getLast()|获取队列尾部元素，失败抛出异常
  public E peekFirst()|获取队列头部元素，失败返回null，等同单向队列和栈的peek
  public E peekLast()|获取队列尾部元素，失败返回null
  public E removeFirst()| 移除并返回队列头部元素，失败抛出异常，等同单向队列remove和栈的pop
  public E removeLast()|移除并返回队列尾部元素，失败抛出异常
  public E pollFirst()|移除并返回队列头部元素，失败返回null，等同单向队列的poll方法
  public E pollLast()|移除并返回队列尾部元素，失败返回null

这些方法在LinkedList中的实现如下：

```java
/**
     * Inserts the specified element at the beginning of this list.
     * 在列表头添加指定元素
     * 双端队列的实现
     * @param e the element to add
     */
    public void addFirst(E e) {
        linkFirst(e);
    }
     /**
     * Appends the specified element to the end of this list.
     *在列表尾添加指定元素
     * 双端队列的实现
     * <p>This method is equivalent to {@link #add}.
     *此方法等价于 add。
     * @param e the element to add
     */
    public void addLast(E e) {
        linkLast(e);
    }
    /**
     * Inserts the specified element at the front of this list.
     * 把指定元素添加到列表头部
     *
     * @param e the element to insert
     * @return {@code true} (as specified by {@link Deque#offerFirst})
     * @since 1.6
     */
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }
     /**
     * Inserts the specified element at the end of this list.
     * 把指定元素添加到列表尾部
     * @param e the element to insert
     * @return {@code true} (as specified by {@link Deque#offerLast})
     * @since 1.6
     */
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }
      /**
     * Returns the first element in this list.
     *返回列表的第一个元素。
     * 双端队列的实现
     * @return the first element in this list
     * @throws NoSuchElementException if this list is empty
     */
    public E getFirst() {
        final Node<E> f = first;  //直接获取列表的first头结点
        if (f == null)    //如果列表为空
            throw new NoSuchElementException();  //抛出异常
        return f.item; //正常则返回头结点的item
    }

    /**
     * Returns the last element in this list.
     *返回列表的最后一个元素
     *  双端队列的实现
     * @return the last element in this list
     * @throws NoSuchElementException if this list is empty
     */
    public E getLast() {
        final Node<E> l = last;  //直接获取列表的last表尾节点
        if (l == null)       //如果列表为空
            throw new NoSuchElementException();  //抛出异常
        return l.item;    //正常则返回表尾节点的item
    }
 /**
     * Retrieves, but does not remove, the first element of this list,
     * or returns {@code null} if this list is empty.
     *检索但不删除列表的第一个元素。
     * 如果列表为空返回 null，不会抛出异常。
     * @return the first element of this list, or {@code null}
     *         if this list is empty
     * @since 1.6
     */
    public E peekFirst() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
    }

    /**
     * Retrieves, but does not remove, the last element of this list,
     * or returns {@code null} if this list is empty.
     *检索但不删除列表的最后一个元素，列表为空返回 null。
     *
     * @return the last element of this list, or {@code null}
     *         if this list is empty
     * @since 1.6
     */
    public E peekLast() {
        final Node<E> l = last;
        return (l == null) ? null : l.item;
    }
     /**
     * Removes and returns the first element from this list.
     *移除并返回列表的第一个元素
     * 双端队列的实现
     * @return the first element from this list
     * @throws NoSuchElementException if this list is empty
     */
    public E removeFirst() {
        final Node<E> f = first;
        if (f == null)  //判断头结点是否为空
            throw new NoSuchElementException(); //为空抛出异常
        return unlinkFirst(f);  //正常调用unlinkFirst
    }

    /**
     * Removes and returns the last element from this list.
     *移除并返回列表的最后一个元素
     * 双端队列的实现
     * @return the last element from this list
     * @throws NoSuchElementException if this list is empty
     */
    public E removeLast() {
        final Node<E> l = last;
        if (l == null)//判断尾节点是否为空
            throw new NoSuchElementException();//为空抛出异常
        return unlinkLast(l); //正常调用unlinkLast
    }
    /**
     * Retrieves and removes the first element of this list,
     * or returns {@code null} if this list is empty.
     *检索并删除列表的第一个元素，如果列表为空返回 null。
     * @return the first element of this list, or {@code null} if
     *     this list is empty
     * @since 1.6
     */
    public E pollFirst() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }

    /**
     * Retrieves and removes the last element of this list,
     * or returns {@code null} if this list is empty.
     *检索并删除列表的最后一个元素，如果列表为空返回 null。
     * @return the last element of this list, or {@code null} if
     *     this list is empty
     * @since 1.6
     */
    public E pollLast() {
        final Node<E> l = last;
        return (l == null) ? null : unlinkLast(l);
    }

```
双端队列中还提供了删除队列中指定对象o的两种方法：
（1） public boolean removeFirstOccurrence(Object o)
```java
/**
     * Removes the first occurrence of the specified element in this
     * list (when traversing the list from head to tail).  If the list
     * does not contain the element, it is unchanged.
     *移除列表中和指定元素匹配的第一个元素（从头到尾遍历）。如果
     *  列表不包含该元素则不做出任何改变。
     * @param o element to be removed from this list, if present
     * @return {@code true} if the list contained the specified element
     * @since 1.6
     */
    public boolean removeFirstOccurrence(Object o) { //该方法显然和remove等价。都是从头到尾，删除匹配的第一个元素
        return remove(o);
    }
```
（2）public boolean removeLastOccurrence(Object o)
```java
 /**
     * Removes the last occurrence of the specified element in this
     * list (when traversing the list from head to tail).  If the list
     * does not contain the element, it is unchanged.
     *移除列表中最后一个和指定元素匹配的元素（从头到尾遍历）。如果
     * 列表不包含该元素则不做任何改变。
     * @param o element to be removed from this list, if present
     * @return {@code true} if the list contained the specified element
     * @since 1.6
     */
    //和remove类似，只是，从尾部last开始遍历，
    // 同样分成两种情况，一种是指定元素为空，一种是不为空。
    // 如果不分，那么当指定元素为空时，访问 x.item 抛出异常
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;     //同样没找到就返回false。
    }
```
## List相关方法
### add方法
add方法，根据需要，设计了参数不同的2个方法。
（1） public boolean add(E e)
和双端列表中的add方法同一个实现。
```java
/**
     * Appends the specified element to the end of this list.
     *在列表尾部添加元素
     *
     * <p>This method is equivalent to {@link #addLast}.
     * 此方法等同于 addLast
     * @param e element to be appended to this list
     * @return {@code true} (as specified by {@link Collection#add})
     */
    public boolean add(E e) {
        linkLast(e);
        return true;
    }
```
（2）public void add(int index, E element)
```java
  /**
     * Inserts the specified element at the specified position in this list.
     * Shifts the element currently at that position (if any) and any
     * subsequent elements to the right (adds one to their indices).
     *在列表中指定位置插入特定元素。把当前位置的元素和后续的所有
     *   元素右移一位（索引加一）。
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public void add(int index, E element) {
        checkPositionIndex(index);  //检查索引的合法性，允许在size处插入

        if (index == size)           //index等于size的话即在表尾插入，调用linkLast
            linkLast(element);
        else             //如果正常插入，先用node方法获得节点再调用linkBefore 在指定索引的节点前插入新节点。
            linkBefore(element, node(index));
    }
```
在指定位置index处添加元素，首先先判断index合法性，然后如果index==size，说明在表尾插入，直接调用linkLast方法，如果不是表尾插入，则正常插入，需要先调用node方法，获得索引对应位置的节点，然后再调用linkBefore方法在指定节点前插入元素。
***
### addAll方法
addAll方法和add的区别主要是添加的元素参数数量的不同。addAll方法传入的不再是一个值，而是一个包含元素的集合c,它同样根据是否传入index，有两个版本。
（1） public boolean addAll(Collection<? extends E> c)
```java
/**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the specified
     * collection's iterator.  The behavior of this operation is undefined if
     * the specified collection is modified while the operation is in
     * progress.  (Note that this will occur if the specified collection is
     * this list, and it's nonempty.)
     *把指定集合中所有元素，按照集合迭代器返回的顺序，添加到列表的
     * 末尾。在这个操作（方法）进行时，指定集合被修改了，则此操作的
     * 行为是不确定的。（注意：如果指定的集合是当前列表，并且它是非空的，
     *  就会发生这种情况）
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     */
    public boolean addAll(Collection<? extends E> c) {
        return addAll(size, c);     //调用的是add(index,c)方法，在size处就是在列表尾巴添加
    }
```
实际上它只是简单的调用了addAll带索引版本的方法，表尾添加，直接传入的index=size即可。
***
（2） public boolean addAll(int index, Collection<? extends E> c)
注意链表的很多操作，都要考虑链表是否为空，对于节点的操作，
还要考虑到节点是否是头节点或者是尾结点。
注意添加和删除都要维护列表中的first和last指针的值。
并且要时刻维护链表的双向结构，即维护节点中的prev和next属性。
```java
 /**
     * Inserts all of the elements in the specified collection into this
     * list, starting at the specified position.  Shifts the element
     * currently at that position (if any) and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in the list in the order that they are returned by the
     * specified collection's iterator.
     *将参数集合中的所有元素插入到指定位置。将原来位置及其之后的
     * 元素后移（索引相应增加）。添加的顺序为参数集合迭代器返回的
     *  顺序。
     * @param index index at which to insert the first element
     *              from the specified collection
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException if the specified collection is null
     */
    //注意：在表头和表尾拆入，即index=0，index=size时，情况下要维护
    //first和last的值。
    public boolean addAll(int index, Collection<? extends E> c) {
        checkPositionIndex(index);     //  检查索引合法性。

        Object[] a = c.toArray();    //将集合c转化为数组。
        int numNew = a.length;     //取得集合c的长度。
        if (numNew == 0)                 //如果长度为0，即不会发生改变，返回false。
            return false;
          //需要保存节点的位置，方便插入以后链表的连接。
        Node<E> pred, succ;            //pred存储位置的前一个节点，succ存储插入位置的一个节点。
        if (index == size) {   //如果要插入的位置是size，即在表尾添加集合，需要改变尾结点。
            succ = null;         //插入位置size没有元素，所以为null
            pred = last;      //size-1位置是表尾，所以pred为尾节点last
        } else {         //如果不是尾部插入
            succ = node(index);     //  赋值为插入位置的节点。
            pred = succ.prev;     //赋值为插入位置的前一个节点。
        }

        for (Object o : a) {      //遍历数组a,也就是集合c转化来的数组
            @SuppressWarnings("unchecked") E e = (E) o;     //转化元素值类型为E
            //新建节点，pred保存了上一次循环中加入的新节点，
            //本次添加的节点的prev显然指向上一次次先添加的节点pred。
            //item为本次遍历到的值e。
            Node<E> newNode = new Node<>(pred, e, null);
            //如果pred为null,只会出现在addAll传入的index为0时，即从表头位置插入，则需要维护first
            //并且是插入时的首次循环，遇到。
            if (pred == null)
                first = newNode;      //那么表头节点更新为首次循环插入的新节点。
            else        //不是从0开始插入，或者非首次循环。
                pred.next = newNode;     //将上一个节点的next赋值为当前节点。
            pred = newNode;    //一次循环结束，需要更新pred的值，使得在每次循环中都记录着上次循环的节点。
        }
           //头节点维护完后，还要维护last尾节点
        if (succ == null) {   //如果插入位置值为null，即在表尾后size处插入，
            last = pred;       //则将尾节点，设置为上面循环记录的最后一个节点，即插入的最后一个节点。
        } else {      //如果非表尾后插入，则要实现双向连接。
            pred.next = succ;    //将最后一次插入的节点的next值赋值为原来记录的插入位置处的节点，
            succ.prev = pred;  //反过来，原来插入位置处的节点的prev指针也要指向插入的最后一个节点。
        }

        size += numNew;    //元素个数添加了集合所含数量numNew
        modCount++;
        return true;
    }
```
***
### get方法
```java
  // Positional Access Operations
    // 基于位置的访问操作，与list相关。
    /**
     * Returns the element at the specified position in this list.
     * 返回列表中指定位置的元素
     * @param index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E get(int index) {
        checkElementIndex(index);      //检查索引合法性。
        return node(index).item;      //通过node（index）获得节点，再返回节点的item值
    }
```
***
### set方法
```java
/**
     * Replaces the element at the specified position in this list with the
     * specified element.
     *把列表中指定位置的元素值设置为特定的对象，并返回原来的值
     *  //注意并没有新建节点，只是修改节点上的值。
     * @param index index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E set(int index, E element) {
        checkElementIndex(index);  //检查节点的合法值。
        Node<E> x = node(index);   //保存位置的节点
        E oldVal = x.item;     // 将旧值存储起来
        x.item = element;     //将节点中的值更改为新的值element
        return oldVal;   //返回旧值
    }
```
***
### remove方法
ArrayList 提供两种删除方法，一种是删除指定索引处的元素，一种是删除和指定对象相同的元素。
（1）public E remove(int index)
```java
/**
     * Removes the element at the specified position in this list.  Shifts any
     * subsequent elements to the left (subtracts one from their indices).
     * Returns the element that was removed from the list.
     *移除列表中指定位置的元素。把当前位置的元素和后续的所有
     *元素左移一位（索引减一）。最后返回从列表中移除的元素。
     * @param index the index of the element to be removed
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E remove(int index) {   //指定位置移除
        checkElementIndex(index);//检查索引合法性
        return unlink(node(index));     //先调用node方法获得节点，再用unlink删除
    }
```
（2） public boolean remove(Object o)
```java
 /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present.  If this list does not contain the element, it is
     * unchanged.  More formally, removes the element with the lowest index
     * {@code i} such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * (if such an element exists).  Returns {@code true} if this list
     * contained the specified element (or equivalently, if this list
     * changed as a result of the call).
     *如果指定元素存在，移除列表中和指定元素相等的第一个元素。如果
     *  列表不包括该元素，不做任何变化。
     *  如果列表发生了改变，即列表包含了指定元素，返回true。
     * @param o element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element
     */
    public boolean remove(Object o) {
        if (o == null) {             //如果传入对象为null
            for (Node<E> x = first; x != null; x = x.next) {     //从表头开始遍历链表，
                if (x.item == null) {           //遇到节点item==null，执行
                    unlink(x);                 //调用unlink方法删除节点
                    return true;
                }
            }
        } else {//如果传入对象不为null
            for (Node<E> x = first; x != null; x = x.next) {   //从表头开始遍历链表
                if (o.equals(x.item)) {//使用传入对象的equals方法判断相等
                    unlink(x);  //调用unlink方法删除节点
                    return true;
                }
            }
        }
        return false;   //如果以上过程没返回，则说明未找到，返回false。
    }

```
***

### size方法
```java
/**
     * Returns the number of elements in this list.
     *返回列表元素个数。
     * @return the number of elements in this list
     */
    public int size() {
        return size;
    }
```
***
### contain方法
```java
  /**
     * Returns {@code true} if this list contains the specified element.
     * More formally, returns {@code true} if and only if this list contains
     * at least one element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *如果列表包含指定元素返回 true。
     *
     * @param o element whose presence in this list is to be tested
     * @return {@code true} if this list contains the specified element
     */
    public boolean contains(Object o) {  //调用的是indexOf判断，如果不存在返回索引就是-1,存在则！=-1。
        return indexOf(o) != -1;
    }
```
***
### clear方法
```java
/**
     * Removes all of the elements from this list.
     * The list will be empty after this call returns.
     * 删除列表中所有元素。
     *   调用此方法之后，列表将为空。
     */
    public void clear() {
        // Clearing all of the links between nodes is "unnecessary", but:
        // - helps a generational GC if the discarded nodes inhabit
        //   more than one generation
        // - is sure to free memory even if there is a reachable Iterator
        for (Node<E> x = first; x != null; ) {    //从头节点开始遍历。
            Node<E> next = x.next;
            x.item = null;     //方便GC。
            x.next = null;
            x.prev = null;
            x = next;   //x指向下一个节点。
        }
        first = last = null;     //删除完毕后，重置first和last值为null
        size = 0;    //size变为0
        modCount++;
    }
```
***
### clone方法
```java
 /**
     * Returns a shallow copy of this {@code LinkedList}. (The elements
     * themselves are not cloned.)
     *返回 LinkedList 的浅拷贝，元素的值没有拷贝，但是节点不是原来的
     * 节点。
     * @return a shallow copy of this {@code LinkedList} instance
     */
    public Object clone() {
        LinkedList<E> clone = superClone();  //object方法返回的副本，内部属性还是原来保存的值

        // Put clone into "virgin" state
        clone.first = clone.last = null;       //初始化内部属性。
        clone.size = 0;
        clone.modCount = 0;

        // Initialize clone with our elements
        for (Node<E> x = first; x != null; x = x.next)  //遍历当前链表，对每一个节点进行添加。
            clone.add(x.item);

        return clone;
    }
```
***
### toArray方法
ArrayList类提供了两个列表转换为数组的方法。
（1） public Object[] toArray()
```java
 /**
     * Returns an array containing all of the elements in this list
     * in proper sequence (from first to last element).
     *按序（从第一个到最后一个元素）返回一个包含列表中所有元素的数组。
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this list.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *返回的数组是安全的，列表不会保留对它的任何引用。（换句话说，
     * 此方法会重新分配一个数组空间）。因此调用者可以对返回的数组进行
     *  任意修改。
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this list
     *         in proper sequence
     */
    public Object[] toArray() {
        Object[] result = new Object[size];  //创建一个可以容纳size个元素的Object数组
        int i = 0;
        for (Node<E> x = first; x != null; x = x.next)    //循环遍历，将每个节点item取出赋值。
            result[i++] = x.item;
        return result;
    }
```
（2） public T[] toArray(T[] a)
```java
 /**
     
     *按序（从第一个到最后一个元素）返回一个包含列表中所有元素的数组；
     * 返回数组的运行时类型是指定数组的类型。
     *  如果列表适合指定的数组，则返回到指定数组中（指定数组长度和
     *   列表 size 大小相等）否则一个以指定数组的类型为运行类型，大小为
     *   列表 size 的新数组将被分配新的空间。

     * 如果列表适合指定的数组并且还有剩余空间（即指定数组比列表有更多
     *  的元素），在数组中紧跟集合末尾的元素被设置为 null。（仅当调用者
     *  知道列表中不包含任何 null 元素，在决定列表长度时才是有用的）

     *这个方法充当基于数组和基于集合API的桥梁（集合与数组的转换）。
     * 此外，该方法允许精确控制输出数组的运行时类型，并且在某些情况
     * 下可以用于节省分配成本。
     * <p>Suppose {@code x} is a list known to contain only strings.
     * The following code can be used to dump the list into a newly
     * allocated array of {@code String}:
     *
     * <pre>
     *     String[] y = x.toArray(new String[0]);</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the list are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing the elements of the list
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this list
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size)  //如果目标数组长度不够，
            a = (T[])java.lang.reflect.Array.newInstance( //创建一个以指定数组类型为运行类型的长度为size的新数组。
                    a.getClass().getComponentType(), size);
        int i = 0;
        Object[] result = a;
        for (Node<E> x = first; x != null; x = x.next)       //遍历链表，将各个节点的item取出赋值
            result[i++] = x.item;

        if (a.length > size)     //如果目标数组的长度大于size，则需要将紧跟集合末尾的位置赋值为null。
            a[size] = null;

        return a;
    }
```

***
# LinkedList 小结
* LinkedList 是双向链表，除了实现了基本的链表操作外，还实现了栈、队列和双向队列的所有方法，所以 LinkedList 不止是链表，还可当成上述所有的数据结构实例。

* 不存在容量不足等问题。（如果内存足够大的话。）

* 位置访问操作实现思路为从前往后遍历，或者从后往前遍历，直到找到指定节点为止。位置访问效率很低，而插入删除的效率比较高。
# ArrayList 和 LinkedList 对比
* 底层实现的原理不同：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200713174531440.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)ArrayList 的底层数据结构是数组，对列表的所有操作实际上都是对数组的操作，元素存储在数组的每一个槽中。
LinkedList 的底层数据类型是自定义的节点类（Node），元素的值保存在节点类的 item 中，而节点类中的 next 和 prev 属性（指向其他节点的“指针”）将所有节点联系在一起。

* 扩容

ArrayList 扩容时重新分配一个更长的数组，将原数组中所有元素复制过来，并回收原数组所占用的内存空间。

LinkedList 的每一次插入删除操作都会为新的节点分配内存空间或者收回旧的节点所占用内存空间，不需要专门扩容。

* 位置访问操作

ArrayList 的底层数据结构为数组，所有的位置访问操作都是数组的随机访问操作，时间复杂度为 O(1)；

LinkedList 类中保存了第一个节点和最后一个节点的引用，而节点之间通过“指针”产生联系，且内存空间不一定连续，位置访问操作必须从第一个节点或者最后一个节点开始遍历，直到找到指定节点为止，时间复杂度为 O(n)。

* 插入删除操作

ArrayList 如果在容量足够的时候，将插入位置及之后的元素向右移动一位，然后执行插入操作，在容量不够的时候，先执行扩容操作，再插入。删除时将删除位置之后的所有元素向左移动一位。

LinkedList 插入时只需要创建新的节点并修改前后指针的指向即可。删除时只需要修改前后指针的指向即可，并将要删除的节点及节点中的元素和指针指向 null，虚拟机会自动回收内存空间。

* 空间花费

ArrayList 的空间花费主要体现在在为 list 列表的结尾预留一定的容量空间。

LinkedList 的空间花费则体现在它的每一个节点都需要消耗相当的空间用来存储“指针”。

* 其他

对于随机访问， ArrayList 优于 LinkedList， 因为 ArrayList 中的数组支持随机访问。

对于新增和删除操作 add 和 remove， LinedList 比较占优势， 因为 ArrayList 要移动数组中的大量数据。

## ArrayList 和 LinkedList 时间消耗
对 ArrayList 和 LinkedList 分别进行 n 次添加，删除，查询操作，测试程序 LinkedListAnalysis.java 在此项目的 src/Analysis 目录下。

实验结果如下图所示：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200713175303642.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
实验结果与上述分析基本吻合。LinkedList 在查询时的性能，远远不如 ArrayList。
而对于插入而言，如果是在指定节点处插入，那么 LinkedList 性能较好，如果是在指定索引处插入，LinkedList 首先要遍历链表找到指定索引处的节点，所以这种情况下的插入性能并不一定优于 ArrayList。
值得注意的是，在 n 较大时，LinkedList 重复执行在列表尾部添加元素这一操作时，时间消耗超过了 ArrayList，可能是因为 LinkedList 频繁的 new，在一定程度上影响了其添加操作的性能。

**LinkedList 固然功能强大，但是在很多情况下，其并非完全优于 ArrayList。恰恰相反，很多情况下 ArrayList 才是性能最优的选择（仅针对 List 而言）。**
