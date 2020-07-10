@[TOC](ArrayList 基于jdk8源码学习)
# 概述
ArrayList 是变长集合类，基于定长数组实现。是一个其容量能够动态增长的动态数组，是一个非线性安全类，它继承了AbstractList，实现了List、RandomAccess, Cloneable, java.io.Serializable。
## 继承关系
它的继承关系图为：
![ArrayList继承关系](https://img-blog.csdnimg.cn/20200710150853828.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
上面这张图基本上描述的很清晰了，实现了四个接口一个抽象类。它继承了AbstractList抽象类，实现了List、RandomAccess, Cloneable, Serializable接口。

它继承于AbstractList，实现了List,RandomAccess[随机访问],Cloneable[可克隆], java.io.Serializable[序列化]这些接口。
ArrayList 继承了AbstractList，实现了List。它是一个数组队列，提供了相关的添加、删除、修改、遍历等功能
ArrayList 实现了RandmoAccess接口，即提供了快速随机访问功能。
ArrayList 实现了Cloneable接口，即覆盖了函数clone()，能被克隆
ArrayList 实现java.io.Serializable接口，这意味着ArrayList支持序列化，
> * RandomAccess接口：
        * 标记性接口，用来快速随机存取，实现了该接口之后，使用普通的for循环来
        * 遍历，性能更高，例如ArrayList。而没有实现该接口的话，使用Iterator来
        * 迭代，这样性能更高，例如linkedList。所以这个标记性只是为了让我们知道
        * 用什么样的方式去获取数据性能更好。　　
>* Cloneable接口：
>*实现了该接口，就可以使用Object.Clone()方法了，列表能被克隆。
 >* Serializable接口：
 *实现该序列化接口，表明该类可以被序列化，能够从类变成节流传输，然后还能
  *从字节流变成原来的类。

# 成员变量
```java
   private static final long serialVersionUID = 8683452581122892189L;
//序列化用，用于比较版本，对类功能没有意义。
    
    private static final int DEFAULT_CAPACITY = 10;
   //默认的初始容器的容量为10。
    
    //所有的空的arraylist实例共享此数组，为了防止一个应用中创建多个空数组，导致空间浪费。
            //注意：此实例只被指定初始容量为0的含参构造函数创建的空数组 共享。
    private static final Object[] EMPTY_ELEMENTDATA = {};

   
    //没有指定初始容量的无参构造函数创建的空的ArrayList实例共享此空数组。
            //注意：和 EMPTY_ELEMENTDATA 区分开是
    //  为了知道当第一个元素被添加时需要扩容多少。
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

   
    //这一个数组用来存储 ArrayList 元素。ArrayList 的容量是这个数组的长度。
    //  添加第一个元素的时候任何满足
    //  elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA
    // 的空 ArrayList 会将容量扩展到 DEFAULT_CAPACITY。
    //
    //注意：transient关键字是主要用于序列化操作中（网络传输）。如果希望信息不想被学序列化传输，
     //加上此关键字后，就不会被序列化，即这个字段的生命周期仅存于调用者内存中而不会写到磁盘里持久化。
    transient Object[] elementData; // non-private to simplify nested class access

    
    private int size;  //ArrayList容量大小。(包含元素的数量),注意与elementData.length区别。
   
 /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    //能分配的最大数组的大小。
    //  些虚拟机会保留一些头消息，占用部分空间。
    // 尝试分配比这个值更大的空间可能会抛出 OutOfMemoryError 错误：请求的
    // 数组大小超过了虚拟机的限制。
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;  //能分配的最大的数组大小。
```
# 构造函数
一共三个构造函数，分别是无参构造函数ArrayList()，指定了初始容量的构造函数ArrayList(int initialCapacity)，以及使用集合c作为参数的构造函数ArrayList(Collection<? extends E> c)。
注意为了节省空间，反正一个应用创建多个空的ArrayList导致浪费空间。因此:
在无参构造函数ArrayList()的初始化对象的过程中，底层的定长Object[] elementData 直接赋值为DEFAULTCAPACITY_EMPTY_ELEMENTDATA，即所有无参构造函数创建的空ArrayList实例共享一个空的Object[]数组。
而在另外两个构造函数中，如果ArrayList(int initialCapacity)指定的参数为0，这个实例将共享EMPTY_ELEMENTDATA空数组；如果ArrayList(Collection<? extends E> c)中指定的集合c为空，则该实例也共享EMPTY_ELEMENTDATA空数组。
```java
 /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param  initialCapacity  the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity
     *         is negative
     */
    //指定了初始容量的构造函数
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {          //如果指定的容量大于0
            this.elementData = new Object[initialCapacity];    //则创建一个Object数组，大小为指定的容量。
        } else if (initialCapacity == 0) {          //如果指定的容量为0。
            this.elementData = EMPTY_ELEMENTDATA;     //含参构造函数构造的空数组将共享EMPTY_ELEMENTDATA空数组。
        } else {     //小于0,则抛出异常
            throw new IllegalArgumentException("Illegal Capacity: "+
                    initialCapacity);
        }
    }

    /**
     * Constructs an empty list with an initial capacity of ten.
     */
    //没有参数的构造函数
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA; //没有指定初始容量的无参构造函数创建的空数组共享此空数组。
    }

    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param c the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     */
    //使用指定的集合，构造一个包含了该集合所有元素的ArrayList，顺序是按照集合使用
    //Iterator迭代器放回的顺序。
    public ArrayList(Collection<? extends E> c) {
        elementData = c.toArray();    //返回一个包含了集合C所有元素的数组Object[]。
        if ((size = elementData.length) != 0) {       //如果集合c不为空
            // c.toArray might (incorrectly) not return Object[] (see 6260652)
            //集合c可能不会正确的返回一个object数组。所以需要判断。
            if (elementData.getClass() != Object[].class)    //如果集合数组c.toArray返回的不是Object数组类型。
                elementData = Arrays.copyOf(elementData, size, Object[].class);//使用Arrays.copyOf将数组转化成size长度的Object数组。
        } else {  //如果集合为空
            // replace with empty array.
            this.elementData = EMPTY_ELEMENTDATA;    //参构造函数构造的空数组将共享EMPTY_ELEMENTDATA空数组。
        }
    }

```
# 关键成员方法
## add方法
add方法，根据需要，设计了参数不同的2个方法。
（1） public boolean add(E e) 
```java
/**
     * Appends the specified element to the end of this list.
     *    在列表的末尾添加指定的值。
     * @param e element to be appended to this list
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     */
    public boolean add(E e) {
        //确保容量充足。
        ensureCapacityInternal(size + 1);  // Increments modCount!!
        elementData[size++] = e;        //size自加，并且在末尾位置后放入新值。
        return true;           //成功返回true
    }
```
首先先调用ensureCapacityInternal(size+1)确保列表可以容纳size+1个元素。
然后直接将底层存储数组elementData的存放的最后一个元素位置(size-1)的后一个位置(size)处放入参数e，然后size++，最后成功插入返回true。
***
（2） public void add(int index, E element)
```java
/**
     * Inserts the specified element at the specified position in this
     * list. Shifts the element currently at that position (if any) and
     * any subsequent elements to the right (adds one to their indices).
     *在列表指定位置添加元素。把该位置及之后的所有元素向后移动（索引加一）。
     *
     * @param index index at which the specified element is to be inserted  需要放入的索引位置
     * @param element element to be inserted           //插入的新值
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public void add(int index, E element) {
        rangeCheckForAdd(index);     //ADD版本的数组边界监测
        //确保列表能够容纳size+1个元素。即确保容量充足
        ensureCapacityInternal(size + 1);  // Increments modCount!!
        //调用了System.arraycopy方法，从index开始移动到index+1。即将index之后的元素全部向后移动一位。
        System.arraycopy(elementData, index, elementData, index + 1,
                size - index);
        elementData[index] = element;   //移动后，将新值插入到index位置。
        size++;        //列表包含的元素个数加1.
    }
```
因为传入的参数中包含index索引，所以需要对索引进行判断。
其中rangeCheckForAdd(index)方法就是判断索引的合法性。
```java
/**
     * A version of rangeCheck used by add and addAll.
     * 被add和addAll使用的索引检查版本。
     */
    private void rangeCheckForAdd(int index) {
        if (index > size || index < 0)           //add版本，所以允许index=size。且，显然index不能为负
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }
```
判断合理性后，同样紧接着要调用ensureCapacityInternal(size+1)确保列表可以容纳size+1个元素。
然后调用System.arraycopy方法，将列表中在index后面的元素都向右移动一个位置，移动完毕后在index位置处放入插入的参数值element。最后 size++ 自加更新size值。
***
System.arraycopy方法为存放在	System.java中的本地方法。
```java
 //@saber-01
  public static native void arraycopy(Object src,  int  srcPos,
                                        Object dest, int destPos,
                                        int length);
   // System.arraycopy(Object src,  int  srcPos,Object dest, int destPos,int length);
    //src：源对象
    //srcPos：源数组中的起始位置
    //dest：目标数组对象
    //destPos：目标数据中的起始位置
    //length：要拷贝的数组元素的数量
```
***
## 扩容方法
上面add方法中在检查列表是否有足够容量时调用了ensureCapacityInternal方法。这个方法其实就是ArrayList扩容方法的入口。调用关系可通过如下源码分析：
注意：public void ensureCapacity(int minCapacity)该方法是public方法，是提供给用户使用的。如果有需要，增加 ArrayList 实例的容量，来确保它可以容纳至少指定最小容量的元素。
而ensureCapacityInternal方法是private方法，是提供给ArrayList内部需要确保容量时候调用的。
```java

 private void ensureCapacityInternal(int minCapacity) {      //内部的确保具有最小容量的方法，它是扩容的入口方法
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {  //如果没有指定构造函数创建的列表
            minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);  //取默认容量10和传入参数中最大的那个值。
        }
        ensureExplicitCapacity(minCapacity);
    }
 /**
     * Increases the capacity of this <tt>ArrayList</tt> instance, if
     * necessary, to ensure that it can hold at least the number of elements
     * specified by the minimum capacity argument.
     *如果有需要，增加 ArrayList 实例的容量，来确保它可以容纳至少指定
     *  最小容量的元素。
     * @param   minCapacity   the desired minimum capacity
     */
    public void ensureCapacity(int minCapacity) {   //扩容的外部方法，可以在要传入大量的元素前，事先调用扩容，减少递增式再分配
        int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
                // any size if not default element table  //如果不是默认元素表，则为任意大小
                ? 0                //如果数组没有共享无参构造函数构建的空列表共享的数组。即当前数组不为空，或者使用了指定大小的构造函数创建的空数组。
                // larger than default for default empty table. It's already
                // supposed to be at default size.
                : DEFAULT_CAPACITY;        //如果数组共享了上面描述的数组，则说明没有指定初始容量，则使用默认初始容量。

        if (minCapacity > minExpand) {    //无参构造函数构造的空数组，最小容量默认为10，如果指定的最小容量小于等于10，就没有必要扩容。
            ensureExplicitCapacity(minCapacity);
        }
    }

    private void ensureExplicitCapacity(int minCapacity) {
        modCount++;   //数组修改次数加1，不管是否调用了grow函数进行扩容。

        // overflow-conscious code
        if (minCapacity - elementData.length > 0)      //如果这个指定的容量比当前数组的长度大。即当前数组长度不足以存放要求的最小数量时才进行扩充。
            grow(minCapacity);           //调用grow函数
    }
```
***
通过源码可以知道，确保容量的方法，会调用ensureExplicitCapacity方法，该方法又会去调用grow方法。而grow就是ArrayList扩容的核心方法，代码如下(这边将用到的成员变量再次提供):
```java
 /**
    //能分配的最大数组的大小。
    //  些虚拟机会保留一些头消息，占用部分空间。
    // 尝试分配比这个值更大的空间可能会抛出 OutOfMemoryError 错误：请求的
    // 数组大小超过了虚拟机的限制。
    */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;  //能分配的最大的数组大小。
    /**
     * Increases the capacity to ensure that it can hold at least the
     * number of elements specified by the minimum capacity argument.
     *增大容量确保可以容纳指定最小容量的元素。
     * @param minCapacity the desired minimum capacity
     */
    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = elementData.length;  //老容量为当前数组的长度。
        int newCapacity = oldCapacity + (oldCapacity >> 1);  //新容量为老容量加上半个老容量，即扩容为老容量的1.5倍。
        if (newCapacity - minCapacity < 0)       // 如果扩容后的新容量还是比指定的最小容量小，或者上一步可能存在溢出，
            newCapacity = minCapacity;        //则直接使用指定的最小容量作为新的容量。即继续扩容到指定的最小的容量。
        if (newCapacity - MAX_ARRAY_SIZE > 0)               //如果扩容后的新容量大于了最大的能分配的数组的大小
            newCapacity = hugeCapacity(minCapacity);            //如果 newCapacity 比最大容量还大，调用 hugeCapacity 函数进行判断
        // minCapacity is usually close to size, so this is a win:
        // elementData 最终指向 newCapacity 大小的新数组空间
        elementData = Arrays.copyOf(elementData, newCapacity);
    }
    //判断扩容的新容量是通过minCapacity赋值，还是通过老容量扩容1.5倍得到的。两者在新容量超出最大可分配数组大小时返回的值不同。
    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow  //如果小于0，说明该整数值溢出了
            throw new OutOfMemoryError(); //抛出溢出异常
        return (minCapacity > MAX_ARRAY_SIZE) ? //如果指定最小容量大于最大可以分配的数组的大小，
                Integer.MAX_VALUE :             //则返回最大的整数值。
                MAX_ARRAY_SIZE;                   //如果没有大于，则返回最大的可分配的数组大小
    }

```
在grow函数中，要实现增大容量以便列表至少可以容纳参数指定的数量的元素。其主要流程是先确定新的数组的长度，即newCapacity。
* 首先将新容量赋值为当前旧容量oldCapacity(即elementData.length)的1.5倍；如果这时新容量还是小于指定的最小容量minCapacity,那么直接使用指定的最小容量作为newCapacity新容量。
* 经过上面过程判断后若此时发现新容量(不管是通过1.5倍扩容还是通过minCapacity赋值)大于了类指定的所允许的数组的最大长度MAX_ARRAY_SIZE，那么需要调用hugeCapacity方法进一步判断。
* hugeCapacity方法主要判断当前指定的minCapacity是否已经大于最大允许的容量MAX_ARRAY_SIZE，如果大于，则将新容量设置为Integer.MAX_VALUE，如果没有大于，即指定最小容量还没超过MAX_ARRAY_SIZE，则新容量设置为MAX_ARRAY_SIZE。

通过上述过程确定好newCapacity后，grow方法将调用Arrays.copyOf(elementData, newCapacity)方法，该方法返回一个新的数组，数组长度为第二个参数，即newCapacity,并且将第一个参数旧的elementData数组中的所有元素通过 System.arraycopy方法全部复制到新的数组中。这个新数组就是扩容以后ArrayList中的elementData数组。
***
Arrays.copyOf 方法源码：
```java
  public static <T> T[] copyOf(T[] original, int newLength) {
        return (T[]) copyOf(original, newLength, original.getClass());
    }
    
  public static <T,U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
        @SuppressWarnings("unchecked")
        T[] copy = ((Object)newType == (Object)Object[].class)
            ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }
```
***
## addAll方法
addAll方法和add的区别主要是添加的元素参数数量的不同。addAll方法传入的不再是一个值，而是一个包含元素的集合c,它同样根据是否传入index，有两个版本。
（1） public boolean addAll(Collection<? extends E> c) 
```java
/**
     * 把指定集合的所有元素，按照迭代器指定集合迭代器返回的顺序，添加到列表
     *  末尾。如果指定集合在操作过程中被修改，则这个操作的行为是不确定的。
     * @param c collection containing elements to be added to this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     */
    //@saber-01 注意：结构性修改操作，一般步骤：检查边界，确保容量(添加元素时)，添加或删除，维护数组连续。
    public boolean addAll(Collection<? extends E> c) {
        Object[] a = c.toArray();   //得到集合c的object数组a
        int numNew = a.length;    //得到数组a的长度。
        ensureCapacityInternal(size + numNew);  // Increments modCount    //确保容量充足
        //从原数组a的0开始复制到目标数组(即当前列表中的elementData数组)的size索引开始。复制numNew(集合c中元素个数)个元素。
        System.arraycopy(a, 0, elementData, size, numNew);
        size += numNew;     //复制过后，列表元素个数size增加numNew个。
        return numNew != 0;                //如果集合含有元素，则返回true，为空返回false。
    }
```
***
（2） public boolean addAll(int index, Collection<? extends E> c)
相对于上面方法，由于指定了index，则需要将index后面的元素向右移动，需要调用System.arraycopy(elementData, index, elementData, index + numNew, numMoved)方法
即数组中index索引后面的numMoved个数都要移动到index+numNew索引后，以空出numNew个位置存放插入的c中的元素。

```java
/**
     *从指定位置开始，将指定集合的所有元素插入列表中。把当前位置及之后的
     *  元素向右移动（增加索引）。新增加的元素将按照指定集合迭代器的返回顺序
     * 出现在列表中。
     *
     * @param index index at which to insert the first element from the  插入的位置
     *              specified collection
     * @param c collection containing elements to be added to this list    //指定的集合
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException if the specified collection is null
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);  //检测边界

        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityInternal(size + numNew);  // Increments modCount

        int numMoved = size - index;        //计算需要移动的元素个数。
        if (numMoved > 0)    //如果不是在最后一个元素后面加入，就需要移动元素。
            //将数组从index位置开始的numMoved数向右移动numNew个位置，腾出位置存放集合c的元素。
            System.arraycopy(elementData, index, elementData, index + numNew,
                    numMoved);
        //腾出位置后，进行复制。
        System.arraycopy(a, 0, elementData, index, numNew);
        size += numNew;           //复制过后，列表元素个数size增加numNew个。
        return numNew != 0;      //如果集合含有元素，说明改变了列表，返回true，反之，返回false。
    }
```
***
## get方法
返回 ArrayList 中指定索引位置的元素。
```java
/**
     * Returns the element at the specified position in this list.
     *返回列表中指定位置的元素
     * @param  index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E get(int index) {            //传入一个索引
        rangeCheck(index);            //检测索引合法性。检测索引是否越界，越界抛出异常

        return elementData(index);    //   返回数组中对应索引位置的值。类型为E。
    }
    
     // Positional Access Operations
    // 位置访问相关操作
    @SuppressWarnings("unchecked")
    E elementData(int index) {    //默认访问权限只能当前包当前类使用。传入一个索引，返回数组索引位置上的值,值类型为传入的模板E类型
        return (E) elementData[index];
    }
```
***
## set 方法
用指定元素替换列表中指定位置的元素，返回被替换的元素。
```java
/**
     * Replaces the element at the specified position in this list with
     * the specified element.
     *用指定元素替换列表中某一位置的元素。返回值是该位置的旧值。
     * @param index index of the element to replace     要替换的元素在数组中的位置
     * @param element element to be stored at the specified position  要替换成的值，即用这个参数值作为新的对应位置上的值。
     * @return the element previously at the specified position   //返回被替换的值，即原来索引上的旧值
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E set(int index, E element) {
        rangeCheck(index);      //检测索引是否越界。

        E oldValue = elementData(index);      //得到数组中对应位置原来的值,类型为E.
        elementData[index] = element;         //将新值赋值到数组指定位置。
        return oldValue;             //返回旧值。
    }
```
***
# remove方法
ArrayList 提供两种删除方法，一种是删除指定索引处的元素，一种是删除和指定对象相同的元素。
（1）public E remove(int index)
```java
/**
     *删除列表指定位置的元素。
     *   把后续元素向左移动（索引减一）。
     * @param index the index of the element to be removed  要删除元素的位置。
     * @return the element that was removed from the list    返回被删除的元素的值。
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E remove(int index) {
        rangeCheck(index);     //检测索引是否越界

        modCount++;             //列表结构修改次数加1
        E oldValue = elementData(index);            //先取出索引位置的旧值，用于返回

        int numMoved = size - index - 1;     //计算需要向左移动的位数。
        if (numMoved > 0)      //如果删除的不是最后一个元素。
            //调用System.arraycopy函数从将数组index+1开始的numMoved个元素移动到从index开始，即index后面的值全部向左移动一位
            System.arraycopy(elementData, index+1, elementData, index,
                    numMoved);
        elementData[--size] = null; // clear to let GC do its work //列表元素个数减1，并且末尾位置设置为null,方便GC回收。

        return oldValue;     //返回被删除的值
    }
```
首先以为传入了索引index，就要调用rangeCheck(index)检查索引的合法性。
```java
/**
     * Checks if the given index is in range.  If not, throws an appropriate
     * runtime exception.  This method does *not* check if the index is
     * negative: It is always used immediately prior to an array access,
     * which throws an ArrayIndexOutOfBoundsException if index is negative.
     * 检查给定的索引是否在范围内。如果不在，抛出适当的运行时异常。这个方法
     * 不会检查索引是否是负数：这个方法总是在访问数组之前使用，如果索引为
     *  负数，访问数组时会抛出 ArrayIndexOutOfBoundsException 异常。
     */
    private void rangeCheck(int index) {       //传入索引，对索引的合法性进行检测。
        if (index >= size)          //如果索引大于等于size。抛出索引越界异常。 小于0的话会在elementData(int index)抛出异常。
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }
```
检查完索引合法性后，取出elementData索引位置的旧值存储到oldValue用于返回。
因为要保持底层数组的连续性，需要向左移动元素。
即调用System.arraycopy(elementData, index+1, elementData, index,numMoved);
numMoved为计算出来的需要移动的元素个数。
移动后，还需要将当前数组最后一个元素的后一个位置size-1处赋值为null，方便GC。
最后size--更新，返回旧值oldValue。
***
（2）  public boolean remove(Object o) 
```java
 /* 删除列表中第一次出现的指定元素，如果它存在的话。如果该元素不存在，
     * 不作任何变化。更正式地说，删除满足条件
     *  (o==null ? get(i)==null : o.equals(get(i))) 的索引值最小的元素。如果操作
     *成功则返回 true。
     *
     * @param o element to be removed from this list, if present
     * @return <tt>true</tt> if this list contained the specified element
     */
    public boolean remove(Object o) {  //删除于指定元素相等的第一次出现在数组中的元素。没找到就什么也没发生。
        if (o == null) {     //如果传入对象为null
            for (int index = 0; index < size; index++)     //遍历数组，
                if (elementData[index] == null) {      //找到第一个为null 的元素，
                    fastRemove(index);              //快速删除找到的索引上的元素
                    return true;                 //删除成功返回true
                }
        } else {                      //如果不为空
            for (int index = 0; index < size; index++)        //遍历数组
                if (o.equals(elementData[index])) {               //使用对象的equals方法判断相等。
                    fastRemove(index);                 //找到第一个相等的值后，直接快速删除
                    return true;                      //删除成功返回true
                }
        }
        return false;            //如果以上步骤没返回，说明没找到要删除的对象o，返回false
    }
     /*
     * Private remove method that skips bounds checking and does not
     * return the value removed.
     * 跳过边界检查并且不返回旧值的私有快速删除函数。效果和remove类似。
     */
    private void fastRemove(int index) {
        modCount++;
        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index,
                    numMoved);
        elementData[--size] = null; // clear to let GC do its work
    }
```
因为传入了一个object对象，需要先判断是否为null,为null的话，则遍历elementData数组，找到第一个为null的元素，调用fastRemove方法（和remove(index)方法几乎类似）快速删除。删除成功返回true。
若不为null，则遍历elementData数组，使用传入对象的equals方法寻找第一个等于参数对象o的元素，调用fastRemove方法进行删除，删除成功返回true。如果以上过程都没找到传入的对象o,即删除失败返回false。
***
## removeAll 和 retailAll
这两种方法都接收一个参数Collection<?> c集合对象。底层都调用了private boolean batchRemove(Collection<?> c, boolean complement) 方法，只是第二个参数设置不同而已。
batchRemove方法首先遍历数组elementData,通过定义2个游标r和w，r用来顺序遍历，w用来记录下一个保留的值应该放在数组中哪个位置。而判断是保留什么值是通过条件(c.contains(elementData[r]) == complement)判断的。
* removeAll传入的第二个参数complement为false，则(c.contains(elementData[r])要返回false，说明只有c中不包含当前遍历的元素，才会进行保留，遍历整个数组后，效果就是去掉了列表中存在在集合c中的元素。
* retailAll传入的第二个参数complement为true，则(c.contains(elementData[r])要返回true。说明只有c中包含当前遍历的元素，才会进行保留，遍历整个数组后，效果就是保留了列表中存在在集合c中的元素。

通过上述过程已经对elementData中需要保留的元素都放在了数组的前w位置，剩下工作就是将w后的位置都复制为null,方便GC，并且将列表含有的元素size复制为w。
如果列表发生了改变，则返回true，反之，返回false。
```java
/**
     * Removes from this list all of its elements that are contained in the
     * specified collection.
     *移除列表中和指定集合相同的元素。
     * 注：即是remove方法的集合版
     * @param c collection containing elements to be removed from this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the class of an element of this list
     *         is incompatible with the specified collection
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this list contains a null element and the
     *         specified collection does not permit null elements
     * (<a href="Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see Collection#contains(Object)
     */
    public boolean removeAll(Collection<?> c) {
        // 使用 Objects 工具类检查集合 c 是否指向 null。为null，会抛出NullPointerException空指针异常
        Objects.requireNonNull(c);
        // 根据第二个参数判断是删除还是保留。为false，说明是删除数组中与集合c中元素相同的元素。
        return batchRemove(c, false);
    }

    /**
     * Retains only the elements in this list that are contained in the
     * specified collection.  In other words, removes from this list all
     * of its elements that are not contained in the specified collection.
     *保留列表中和指定集合相同的元素（求交集）。换句话说，移除列表中有的而
     *指定集合中没有的那些元素。
     *
     * @param c collection containing elements to be retained in this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the class of an element of this list
     *         is incompatible with the specified collection
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this list contains a null element and the
     *         specified collection does not permit null elements
     * (<a href="Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see Collection#contains(Object)
     */
    public boolean retainAll(Collection<?> c) {
        // 使用 Objects 工具类检查集合 c 是否指向 null，为null，会抛出NullPointerException空指针异常
        Objects.requireNonNull(c);
        //根据第二个参数判断是删除还是保留。为true，说明是保留数组中与集合c中元素相同的元素，删除其他元素。
        return batchRemove(c, true);
    }
    // complement 为 true，保留集合中存在的元素；complement 为 false 时删除。
    private boolean batchRemove(Collection<?> c, boolean complement) {
        // 对于一个final变量，如果是基本数据类型的变量，则其数值一旦在初始化
        //  之后便不能更改；如果是引用类型的变量，则在对其初始化之后便不能
        //再让其指向另一个对象。
        final Object[] elementData = this.elementData;
        int r = 0, w = 0;
        boolean modified = false;
        try {
            for (; r < size; r++)       //遍历数组。
                // complement 为 false：若 c 不包含 elementData[r]，if成立，则保留该 elementData[r]。
                // complement 为 true：若 c 包含 elementData[r]，if成立，保留该值 elementData[r]。
                //循环结果就是complement为true，数组剩下的为包含在c中的元素，若为false，则剩下的是不包含在c中的元素
                if (c.contains(elementData[r]) == complement)
                    elementData[w++] = elementData[r];
        } finally {
            // Preserve behavioral compatibility with AbstractCollection,
            // even if c.contains() throws.
            //保持与AbstractCollection的行为兼容性，
            //即使c.contains（）抛出异常，也要执行finally代码块。
            if (r != size) {   //如果因为异常退出循环，r还没遍历到size。
                System.arraycopy(elementData, r,            //将r后面的元素直接复制到w位置后，
                        elementData, w,
                        size - r);
                w += size - r;           //w向右移动size-r个位置。
            }
            if (w != size) {                 //如果w不等于size，即列表发生了变化。
                // clear to let GC do its work
                for (int i = w; i < size; i++)       //将后面多余位置上原来的值赋值为null。方便GC。
                    elementData[i] = null;
                modCount += size - w;
                size = w;                //设置新长度为w。
                modified = true;      //因为列表发生了变化，所以为true。
            }
        }
        return modified;
    }

```
***
## clear方法
移除列表中所有的元素，size重置为0，遍历elementData数组，所有位置全部赋值为null,方便GC。
```java
 /**
     * Removes all of the elements from this list.  The list will
     * be empty after this call returns.
     * 删除列表中的所有元素。此方法调用后列表为空。
     */
    public void clear() {
        modCount++;

        // clear to let GC do its work
        for (int i = 0; i < size; i++)        //遍历数组，
            elementData[i] = null;        //每个数组位置全部设为null,方便GC

        size = 0;      //列表元素个数设为0
    }
```
# 其他成员方法
## size方法
返回ArrayList中含有的元素的个数。
```java
/**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    public int size() {    //返回的是列表中元素的个数。而不是数组的长度。
        return size;
    }
```
***
## isEmpty方法
返回boolean值，如果Arrayl中不存在元素则返回true，如果存在为false；
```java
public boolean isEmpty() {      //用于判断列表是否为空，即判断列表是否不含元素。
        return size == 0;        //不含元素，返回true,代表为空，反之，返回false，代表列表含有元素。
    }
```
***
## contains方法
传入一个对象，调用indexOf方法，判断列表中是否存在该对象，存在返回true，不存在返回false。
```java
/**
     * Returns <tt>true</tt> if this list contains the specified element.
     * More formally, returns <tt>true</tt> if and only if this list contains
     * at least one element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *如果列表包含指定元素返回 true。
     *     更正式地，当且仅当列表包含一个元素 e 且满足
     *    (o==null ? e==null : o.equals(e)) 时返回true。
     *
     * @param o element whose presence in this list is to be tested
     * @return <tt>true</tt> if this list contains the specified element
     */
    public boolean contains(Object o) {
        return indexOf(o) >= 0;      //如果indexOf返回的值不是-1，即大于等于0.说明找的到该元素。返回true。反正，返回false。
    }
```
***
## indexOf方法
传入一个对象，返回列表中第一次出现该对象的索引位置，如果找不到该对象，则返回-1。
根据传入的对象是否为null，判断时决定是否采取对象对象的equals方法判断相等。
注意不管列表中包含了几个该对象，返回的是对象第一次出现时的索引。
```java
/**
     * Returns the index of the first occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the lowest index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     *  返回列表中第一次出现指定元素的索引位置，如果列表不包含该元素返回 -1。
     *   更正式地说，如果满足
     *   (o==null ? get(i)==null : o.equals(get(i))) 则返回最小的索引值 i，否则认为
     *    索引不存在则返回-1。
     */
    public int indexOf(Object o) {
        if (o == null) {      //如果传入的是null
            for (int i = 0; i < size; i++)       //遍历数组中各个元素。size代表元素数量。
                if (elementData[i]==null)       //找到第一个为null的元素，立即返回索引i
                    return i;
        } else {                            //如果传入的不为null
            for (int i = 0; i < size; i++)       //遍历数组各个元素
                if (o.equals(elementData[i]))      //调用对象的equals方法判断，找到第一个相等的元素后立即返回索引i
                    return i;
        }
        return -1;          //如果上面判断中都没有返回，说明没有找到，则返回-1；
    }
```
***
## lastIndexOf方法
与indexOf方法类似，差别在于该方法返回的是对象最后一次出现时的索引位置。
```java
	/**
     * Returns the index of the last occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the highest index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     * 返回列表中最后一次出现指定元素的索引位置，如果列表不包含该元素返回 -1。
     *  更正式地说，如果满足
     *   (o==null ? get(i)==null : o.equals(get(i))) 则返回最大的索引值 i，否则认为
     *  索引不存在则返回-1。
     */
    public int lastIndexOf(Object o) {
        if (o == null) {      //如果传入的是null。
            for (int i = size-1; i >= 0; i--)       //从最后一个元素开始遍历，(最后一个元素的索引位置为size-1)
                if (elementData[i]==null)       //一旦找到第一个为null元素，就返回索引i，
                    return i;
        } else {              //如果传入不为null。
            for (int i = size-1; i >= 0; i--)          //从最后一个元素开始遍历
                if (o.equals(elementData[i]))          //调用对象的equals方法，找到第一个相等的值就返回对应的索引i
                    return i;
        }
        return -1;       //如果上述过程都没返回，则说明数组中不含有该元素，返回-1；
    }
```
***
## clone方法
clone方法返回的是原来ArrayList对象的副本，其内部调用的是Arrays.copyOf方法。
***
**注意：**
对于基本数据类型来说clone()方法实现数组拷贝也属于深拷贝。
对于引用类型来说，clone都是浅拷贝，拷贝的都是对象的引用。即通过clone得到的副本中的对象引用去修改一个对象内部的成员属性，则原来的ArrayList存放的该对象的引用访问到该对象时，内部也发生了修改。但是对于这个引用来说，它是深拷贝，即可以将副本中的引用指向另一个对象，而原来ArrayList中的该引用值还是指向原来对象。
***
```java
/**
     * Returns a shallow copy of this <tt>ArrayList</tt> instance.  (The
     * elements themselves are not copied.)
     *返回 ArrayList 实例的拷贝。
     * @saber 对于基本数据类型来说clone()方法实现数组拷贝也属于深拷贝。
     * 对于引用类型来说，clone都是浅拷贝，拷贝的都是对象的引用。即修改clone得到的副本
     * 同样也会影响原来的ArrayList。
     * @return a clone of this <tt>ArrayList</tt> instance
     */
    public Object clone() {
        try {
            ArrayList<?> v = (ArrayList<?>) super.clone();  //浅拷贝，创建了一个当前对象的副本。(ArrayList<?>为显示强制转换)
            //这里副本对象中的数组还是当前数组，所以需要用Arrays.copyOf创建一个新的数组。
            v.elementData = Arrays.copyOf(elementData, size);  //对副本中的成员elementData数组使用copyOf进行重新赋值。
            // 注意：这里新建的数组地址虽然不同了(即数组中各元素都存放在新地址)，但是因为java中对象的赋值都是引用传递，所以副本和原来数组中存放的
            //对象引用指向了同一个对象的实际位置，通过副本数组中存放的引用改变了对象，也会反映到原来数组通过引用得到的对象的值。
            v.modCount = 0;      //副本的初始修改值设置为0。
            return v;      //返回这个副本
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
    }
```
***
## toArray方法
ArrayList类提供了两个列表转换为数组的方法。
（1） public Object[] toArray()
内部也是调用的Arrays.copyOf方法，对比clone源码可发现，原理是相同的。
```java
 /**
     
     * @return an array containing all of the elements in this list in
     *         proper sequence
     */
    //以适当的顺序（从第一个元素到最后一个元素）返回包含列表所有元素的数组。
    //
    //  返回的数组是安全的，因为原列表中不包含它的引用。（换句话说，返回的
    //  数组是新分配的内存空间）。调用者可以任意修改返回的数组。
    public Object[] toArray() {
        return Arrays.copyOf(elementData, size);      //实际上原理和clone是一样的。
    }
```
***
（2） public <T> T[] toArray(T[] a)
指定了要转移的目标数组a。如果目标数组a不允许放下当前ArrayList的所有元素，即size>a.length，则调用 Arrays.copyOf(elementData, size, a.getClass())方法，返回一个长度为size的新数组。
如果目标数组a可以容纳当前ArrayList的所有元素，即size<=a.length，则调用 System.arraycopy(elementData, 0, a, 0, size)，将当前ArrayList中的elementData数组中的size个元素全部复制到a中。并且进一步判断，如果size>a.length,还要将目标数组a的由当前数组的末尾元素复制的元素的下一个位置赋值为null。
```java
 /**
     *
     * 以适当的顺序（从第一个元素到最后一个元素）返回包含列表所有元素的数组。
     *  返回数组的运行时类型是指定数组的类型。如果指定数组能完全容纳列表所有
     *  元素，那么将所有元素存入指定数组内。否则，在内存中分配足以容纳所有
     * 元素的新的数组空间。
     *
     *  如果指定的数组还有多余的空间（即指定数组比列表有更多的元素），在数组
     *  中紧跟集合末尾的元素被设置为 null。
     * @param a the array into which the elements of the list are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing the elements of the list
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this list
     * @throws NullPointerException if the specified array is null
     */
    //@saber-01
   // System.arraycopy(Object src,  int  srcPos,Object dest, int destPos,int length);
    //src：源对象
    //srcPos：源数组中的起始位置
    //dest：目标数组对象
    //destPos：目标数据中的起始位置
    //length：要拷贝的数组元素的数量
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size)     //如果目标数组的长度比当前列表中元素个数小，
            // Make a new array of a's runtime type, but my contents:
            //创建一个新的数组，使用copyOf函数，且指定了运行时类型为参数a的类型T，返回一个新的T[]类型数组。
            return (T[]) Arrays.copyOf(elementData, size, a.getClass());
        //如果目标数组长度大于等于当前数组元素个数size。则调用System.arraycopy。
        System.arraycopy(elementData, 0, a, 0, size);     //将size个元素全部复制到a中。
        if (a.length > size)             //如果目标数组长度大于当前数组元素个数size，
            a[size] = null;                 //则在目标数组a的紧跟着当前数组的末尾元素的位置上赋值为null。
        return a;
    }
```
***
# ArrayList 小结
* 元素存储在数组里，位置访问操作就是数组的位置访问操作，所以查找效率较高，时间消耗为 O(1)，但是插入删除效率低，因为插入删除操作会移动数组指定位置的前方或后方的所有元素。

* 插入删除等基本方法均用到 Arrays.copy() 或 System.arraycopy 函数进行批量数组元素的复制。（其实不止是 ArrayList，所有的以数组为底层存储结构的集合的批量复制都是用这两个函数。）

* 每次增加元素的时候，都需要调用 ensureCapacity 方法确保足够的容量。一般情况下单次扩容 1.5 倍，如果需要的容量大于 1.5 倍，直接扩容到指定容量。
* ArrayList自己实现了序列化和反序列化的方法，因为它自己实现了private void writeObject(java.io.ObjectOutputStream s)、private void readObject(java.io.ObjectInputStream s) 方法
* ArrayList基于数组方式实现，无容量的限制（会扩容）
* 添加元素时可能要扩容（所以最好预判一下），删除元素时不会减少容量（若希望减少容量，trimToSize()），删除元素时，将删除掉的位置元素置为null，下次gc就会回收这些元素所占的内存空间。
* 线程不安全，会出现fall-fail。
* add(int index, E element)：添加元素到数组中指定位置的时候，需要将该位置及其后边所有的元素都整块向后复制一位
* get(int index)：获取指定位置上的元素时，可以通过索引直接获取（O(1)）
* remove(Object o)需要遍历数组，而remove(int index)不需要遍历数组，只需判断index是否符合条件即可，效率比remove(Object o)高
* contains(E)需要遍历数组
* 使用iterator遍历可能会引发多线程异常
# ArrayList 的一些对比
## 与数组的比较
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200710181749948.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
## 与LinkList、Vector对比区别
（1）ArrayList 本质上是一个可改变大小的数组.当元素加入时,其大小将会动态地增长.内部的元素可以直接通过get与set方法进行访问.元素顺序存储 ,随机访问很快，删除非头尾元素慢，新增元素慢而且费资源 ,较适用于无频繁增删的情况 ,比数组效率低，如果不是需要可变数组，可考虑使用数组 ,非线程安全.

（2）LinkedList 是一个双链表,在添加和删除元素时具有比ArrayList更好的性能.但在get与set方面弱于ArrayList。适用于 ：没有大规模的随机读取，有大量的增加/删除操作.随机访问很慢，增删操作很快，不耗费多余资源 ,允许null元素,非线程安全.

（3）Vector （类似于ArrayList）但其是同步的，开销就比ArrayList要大。如果你的程序本身是线程安全的，那么使用ArrayList是更好的选择。 Vector和ArrayList在更多元素添加进来时会请求更大的空间。Vector每次请求其大小的双倍空间，而ArrayList每次对size增长50%(即老容量的1.5倍)。
