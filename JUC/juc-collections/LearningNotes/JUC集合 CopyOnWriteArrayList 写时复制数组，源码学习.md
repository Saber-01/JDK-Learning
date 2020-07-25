# 概述
CopyOnWriteArrayList是ArrayList 的一个线程安全的变体，其中所有可变操作(add、set 等等)都是通过对底层数组进行一次新的拷贝来实现的。COW模式的体现。
# 成员变量
lock ：用于修改性操作时需要加的重入锁。
array：是底层存放元素的数组，在多线程操作下，其引用经常变化。
```java
   /** 保护所有数据更改操作的锁 */
    final transient ReentrantLock lock = new ReentrantLock();

    /** 数组。只能通过 getArray/setArray 访问此数组（不是数组中的元素）。*/
    private transient volatile Object[] array;
```

# 关键方法
此类中的方法和 ArrayList 中的方法基本一致，包括 add，remove，set 等。

此类不需要扩容方法，因为每一步的修改操作都创造了一个新的数，新数组的长度是可以指定的。首先从原数组里复制一份新的数组，在新数组里执行写入操作。执行完成之后，将属性 array 指向新的数组，旧数组由垃圾回收器自行回收。

创建（复制）数组的操作有两种，一种是 Arrays.copyOf，调用后立刻无条件创建新的数组；一种是 System.arraycopy，将源数组复制到目标数组里。

获得数组和设置数组的方法：
```java
 /**
     * 获取数组。包访问权限。可以通过 CopyOnWriteArraySet 访问。
     */
    final Object[] getArray() {
        return array;
    }

    /**
     * 设置数组。
     */
    final void setArray(Object[] a) {
        array = a;
    }
```
***
## set
```java
  /**
     * 将列表中指定位置元素替换成指定的值。
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E set(int index, E element) {
        final ReentrantLock lock = this.lock;
        // 替换之前上锁
        lock.lock();
        try {
            Object[] elements = getArray(); //得到底层数组array
            E oldValue = get(elements, index);  //得到替换前的旧值

            if (oldValue != element) {           //如果旧值不等于替换的值
                int len = elements.length;     //得到数组长度
                // Arrays.copyOf 创建了一个新数组，并指定新数组长度,写时复制的思想
                Object[] newElements = Arrays.copyOf(elements, len);
                newElements[index] = element; //修改新数组中对应位置的值
                // 将修改后的新数组设定为存储元素的数组
                setArray(newElements);
            } else {
                // Not quite a no-op; ensures volatile write semantics
                setArray(elements);    //如果相等，那么原数组不变化，
            }
            return oldValue;  //返回旧值
        } finally {  //解锁
            lock.unlock();
        }
    }
```

## get 
```java
 /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E get(int index) {
        return get(getArray(), index);
    }
    private E get(Object[] a, int index) {
        return (E) a[index];
    }
```

## add
 add(E e)方法在数组末尾添加元素e,先lock(),直接调用Array.copyOf将数组中的值全部赋值到新数组中，新数组长度为旧数组+1。多出的最后一个空位，将e放入。最后调用setArray设置新的array，返回true，并解锁。
 add(int index, E element)在列表指定位置插入指定值，此方法同样在lock和unlock之间，保证同步性，首先以旧数组长度+1为指定长度创建新数组，然后进行两次System.arraycopy,先将index之前的元素复制到新数组从0开始的位置，再将index之后的元素，复制到新数组index+1开始的位置。最后新数组空出的index位置上放入e,然后调用setArray设置新的array。
```java
 /**
     * 将指定元素添加到列表末尾。
     *
     * @param e element to be appended to this list
     * @return {@code true} (as specified by {@link Collection#add})
     */
    public boolean add(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();//上锁
        try {
            Object[] elements = getArray();
            int len = elements.length;
            // 同样创建新数组，新数组长度为原数组+1，并将元素添加到新数组尾部，然后将新数组设置成
            // 支撑数组
            Object[] newElements = Arrays.copyOf(elements, len + 1);
            newElements[len] = e;  //将元素放入尾部
            setArray(newElements);  //新数组设置为底层的数组
            return true;  //插入成功返回true
        } finally {
            lock.unlock();  //解锁
        }
    }
    /**
     * 在列表中指定位置插入指定元素（插入后元素在 index 位置）。将 index
     * 位置及之后的元素往右移一位（索引加 1）。
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public void add(int index, E element) {
        final ReentrantLock lock = this.lock;
        lock.lock();//上锁
        try {
            Object[] elements = getArray();  //得到原数组
            int len = elements.length;
            if (index > len || index < 0)    //检查索引合法性
                throw new IndexOutOfBoundsException("Index: "+index+
                        ", Size: "+len);
            Object[] newElements;
            // 需要移动的元素个数
            int numMoved = len - index;
            // 如果不用移动任何元素，则和add(E)一样，创造一个比当前数组容量大 1 的数组。
            if (numMoved == 0)
                newElements = Arrays.copyOf(elements, len + 1);
            else {   //需要移动，
                // 初始化新数组的固定长度为之前长度+1
                newElements = new Object[len + 1];
                // 将 elements 里的元素从0开始放入到 newElements 从0开始的位置，复制个数为index，即将index前面元素先移动
                System.arraycopy(elements, 0, newElements, 0, index);
                //然后将elements中index之后的元素，移动到新数组的index+1开始的位置，移动个数为计算出来的numMoved。
                System.arraycopy(elements, index, newElements, index + 1,
                        numMoved);
            }
            //移动完毕后，在index处放入新值
            newElements[index] = element;
            setArray(newElements);  //设置新数组
        } finally {
            lock.unlock();  //解锁
        }
    }
```
***
## indexOf 和lastIndexOf
indexOf 操作相当于读操作，没有对内存写入，所以没有加锁。和 ArrayList 一样，查找第一次出现的索引从前往后扫描，查找最后一次出现的索引从后往前扫描。
```java
 /**
     * indexOf 的静态版本，允许重复调用而无需每次重新获取数组。查找范围为
     * index（包含）到 fence（不包含）。
     * 注意：没有加锁
     * @param o element to search for
     * @param elements the array
     * @param index first index to search
     * @param fence one past last index to search
     * @return index of element, or -1 if absent
     */
    private static int indexOf(Object o, Object[] elements,
                               int index, int fence) {
        if (o == null) {
            for (int i = index; i < fence; i++)
                if (elements[i] == null)
                    return i;
        } else {
            for (int i = index; i < fence; i++)
                if (o.equals(elements[i]))
                    return i;
        }
        return -1;
    }

    /**
     * lastIndexOf 的静态版本。从索引为 index 位置开始查找。
     * 没有加锁。
     * @param o element to search for
     * @param elements the array
     * @param index first index to search
     * @return index of element, or -1 if absent
     */
    private static int lastIndexOf(Object o, Object[] elements, int index) {
        if (o == null) {
            for (int i = index; i >= 0; i--)
                if (elements[i] == null)
                    return i;
        } else {
            for (int i = index; i >= 0; i--)
                if (o.equals(elements[i]))
                    return i;
        }
        return -1;
    }
```
***
## addAll
和add的区别，就是移动的元素个数不同，具体操作是很类似的。当前和其之后的元素都会向右移动然后空出集合的个数的位置，以便集合元素的放入。
```java
 /**
     * 将指定集合中的所有元素加入到列表末尾，添加顺序为指定集合迭代器
     * 返回的顺序。
     *
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     * @see #add(Object)
     */
    public boolean addAll(Collection<? extends E> c) {
        Object[] cs = (c.getClass() == CopyOnWriteArrayList.class) ?
                ((CopyOnWriteArrayList<?>)c).getArray() : c.toArray();
        if (cs.length == 0)
            return false;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len == 0 && cs.getClass() == Object[].class)
                setArray(cs);
            else {
                // 直接创建新的数组，而不是加在原来的数组后面
                Object[] newElements = Arrays.copyOf(elements, len + cs.length);
                //此时cs为底层数组。
                System.arraycopy(cs, 0, newElements, len, cs.length);
                setArray(newElements);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 在此列表中指定位置插入指定集合中所有元素。将当前位置和其之后的元素
     * 向右移动（索引增加）。插入的顺序和集合迭代器返回的顺序一致。
     *
     * @param index index at which to insert the first element
     *        from the specified collection
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException if the specified collection is null
     * @see #add(int,Object)
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        Object[] cs = c.toArray();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            // 判断边界
            if (index > len || index < 0)
                throw new IndexOutOfBoundsException("Index: "+index+
                        ", Size: "+len);
            if (cs.length == 0)
                return false;
            // 需要移动的元素个数
            int numMoved = len - index;
            Object[] newElements;
            if (numMoved == 0)
                // 创建可以容纳所有元素的数组，并将列表元素复制到该数组里
                newElements = Arrays.copyOf(elements, len + cs.length);
            else {
                // 创建可以容纳所有元素的数组，将分别将 index 之前，index 及
                // 之后的元素复制到对应的位子（中间空出 cs 的位置）
                newElements = new Object[len + cs.length];
                //分两段复制
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index,
                        newElements, index + cs.length,
                        numMoved);
            }
            // 将指定集合元素复制到 index 及之后
            System.arraycopy(cs, 0, newElements, index, cs.length);
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }
```
## addIfAbsent
只有数组中不含有该元素时，才进行添加操作。
```java
/**
     * 添加元素，如果它不存在的话。
     *
     * @param e element to be added to this list, if absent
     * @return {@code true} if the element was added
     */
    public boolean addIfAbsent(E e) {
        // 快照是留下来的旧的数组，最新的数组和快照没有关系
        Object[] snapshot = getArray();
        // 调用 index 判断是否存在该元素，只有在快照数组中没找到，才会去方法中找。
        return indexOf(e, snapshot, 0, snapshot.length) >= 0 ? false :
                addIfAbsent(e, snapshot);
    }

    /**
     * A version of addIfAbsent using the strong hint that given
     * recent snapshot does not contain e.
     * 和 remove(Object o, Object[] snapshot, int index) 类似的操作。
     */
    private boolean addIfAbsent(E e, Object[] snapshot) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] current = getArray();
            int len = current.length;
            if (snapshot != current) {
                // Optimize for lost race to another addXXX operation
                // 首先在快照的长度和最新数组的长度值较小的范围内查找
                int common = Math.min(snapshot.length, len);
                for (int i = 0; i < common; i++)
                    // 如果已经存在该元素，返回 false.
                    // 如果 current[i] == snapshot[i]，说明该位置没有被其他线程修改过，
                    // 同时该位置是不可能存在的，因为之前已经检查过了
                    if (current[i] != snapshot[i] && eq(e, current[i]))
                        return false;
                // 如果在没查找的范围内找到了，也返回 false
                if (indexOf(e, current, common, len) >= 0)
                    return false;
            }
            // 复制
            Object[] newElements = Arrays.copyOf(current, len + 1);
            newElements[len] = e;
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }
```
## addAllAbsent
```java
 /**
     * 将指定集合中所有不存在于列表中的元素加入到列表中（求并集），按
     * 指定集合迭代器返回的顺序加入。
     *
     * 如果加入列表的元素有重复，只加入一次。
     *
     * @param c collection containing elements to be added to this list
     * @return the number of elements added
     * @throws NullPointerException if the specified collection is null
     * @see #addIfAbsent(Object)
     */
    public int addAllAbsent(Collection<? extends E> c) {
        Object[] cs = c.toArray(); //cs既用来存放集合的元素，也用来存放需要转移到数组中去的元素。
        if (cs.length == 0)
            return 0;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            int added = 0;
            // uniquify and compact elements in cs
            // 从集合中找出不存在于列表中的元素（还需要判断它和已处理过的
            // 元素不相等，为了删除重复的元素）
            // 将找出的元素保存在cs 数组前面
            for (int i = 0; i < cs.length; ++i) {
                Object e = cs[i];
                if (indexOf(e, elements, 0, len) < 0 && //只有还未存在的旧表中，
                        indexOf(e, cs, 0, added) < 0)   //且也没存在在已经转移的数据中。
                    cs[added++] = e;
            }
            // 复制
            if (added > 0) { //如果添加了值。
                //先创建新数组，把原来的数组全部复制到新数组中去，并且新数组长度为原来数组加上添加到集合的数组。
                Object[] newElements = Arrays.copyOf(elements, len + added);
                System.arraycopy(cs, 0, newElements, len, added);  //将cs中可以添加到新数组中的，移到len后面。
                setArray(newElements);
            }
            return added;   //返回添加的有效个数
        } finally {
            lock.unlock();  //解锁
        }
    }
```
## remove
remove(int index) 删除列表中指定位置的元素。将之后的元素往前移动一格。并返回删除的元素。

remove(Object o) 删除指定的元素。此时先调用 indexOf 找到其所在的位置，如果这一步直接没找到，那么说明不需要删除，直接返回false，反之如果找到，需要到下一步进行加锁修改操作。先加锁，比较传入的旧数组（快照）和当前数组，如果还是在当前数组中找到要删除的值，就创造新数组，接着就和remove（index）方法一样调用两次System.arraycopy方法处理新数组，然后setArray设置底层数组。
```java
  /**
     * 删除列表中指定位置的元素。
     * 将之后的元素往前移动一格。并返回删除的元素。
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E remove(int index) {
        final ReentrantLock lock = this.lock;
        lock.lock();  //上锁
        try {
            Object[] elements = getArray();
            int len = elements.length;
            E oldValue = get(elements, index);  //先获得对应位置旧值，如果index不合法，这一步会抛出异常
            int numMoved = len - index - 1; //需要移动的元素个数。
            // 删除的元素是最后一个元素，直接赋值少一个个数即可。
            if (numMoved == 0)
                setArray(Arrays.copyOf(elements, len - 1));
            else {
                // 否则将其后的元素向前移动一位
                Object[] newElements = new Object[len - 1]; //同样先固定长度为原始长度-1
                //这一步先赋值前index个值
                System.arraycopy(elements, 0, newElements, 0, index);
                //再赋值index+1后的这些元素值，放到新数组index处。
                System.arraycopy(elements, index + 1, newElements, index,
                        numMoved);
                setArray(newElements);   //赋值新数组给底层数组
            }
            return oldValue;//返回删除的旧值
        } finally {
            lock.unlock();  //解锁
        }
    }

    /**
     * 从列表中删除第一次出现的指定元素，如果其存在的话。如果不包含该元素，
     * 不做出任何改变。
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element
     */
    public boolean remove(Object o) {   //没有上锁，在调用的remove版本中上锁
        Object[] snapshot = getArray();
        // 要删除元素的索引
        int index = indexOf(o, snapshot, 0, snapshot.length); //如果存在，返回值不是-1.
        return (index < 0) ? false : remove(o, snapshot, index);   //在旧数组中没找到，就直接返回，否则调用加锁的remove。
    }

    /**
     * A version of remove(Object) using the strong hint that given
     * recent snapshot contains o at the given index.
     */
    private boolean remove(Object o, Object[] snapshot, int index) {
        final ReentrantLock lock = this.lock;
        lock.lock(); //上锁。
        try {
            Object[] current = getArray(); //得到当前数组引用。
            int len = current.length;
            // 如果快照已经不是当前的数组了
            if (snapshot != current) findIndex: {
                int prefix = Math.min(index, len);//更新遍历的结束点。
                // 遍历找到该元素现在的位置
                for (int i = 0; i < prefix; i++) {
                    // 如果 current[i] == snapshot[i]，数组当前位置没有被改变
                    // 如果不相等，而且 o 等于 curr[i]，说明snapshot与current之间修改了数组,已经找到现在所在位置
                    if (current[i] != snapshot[i] && eq(o, current[i])) {
                        index = i;  //index 赋值
                        break findIndex; //打断这个if语句块
                    }
                }
                // 运行到这一步的 index 大于等于 len，说明数组缩短了
                // 如果在 len 范围内没找到，说明元素在列表中已经不存在了
                if (index >= len)
                    return false;
                // index < len
                // 当前 index 的元素就是要找的元素则跳出if，否则从 index 开始，
                // 查找 index - len 范围内是否存在
                if (current[index] == o)
                    break findIndex;
                index = indexOf(o, current, index, len);
                if (index < 0)  //如果没有找到，返回false。
                    return false;
            }
            // 到这里，说明找到了索引，执行复制操作，和remove（index）方法一样原理。
            Object[] newElements = new Object[len - 1];
            System.arraycopy(current, 0, newElements, 0, index);
            System.arraycopy(current, index + 1,
                    newElements, index,
                    len - index - 1);
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();  //解锁
        }
    }
```

## 迭代器

COWIterator表示迭代器，其也有一个Object类型的数组作为CopyOnWriteArrayList数组的快照，这种快照风格的迭代器方法在创建迭代器时使用了对当时数组状态的引用。此数组在迭代器的生存期内不会更改，因此不可能发生冲突，并且迭代器保证不会抛出 ConcurrentModificationException。创建迭代器以后，迭代器就不会反映列表的添加、移除或者更改。
并且注意：在迭代器上进行的元素更改操作(remove、set 和 add)不受支持。这些方法将抛出 UnsupportedOperationException。
```java
 static final class COWIterator<E> implements ListIterator<E> {
        /** 数组快照*/
        private final Object[] snapshot;
        /** 下次调用 next 时的元素索引  */
        private int cursor;

        private COWIterator(Object[] elements, int initialCursor) {
            cursor = initialCursor;
            snapshot = elements;
        }

        public boolean hasNext() {
            return cursor < snapshot.length;
        }

        public boolean hasPrevious() {
            return cursor > 0;
        }

        @SuppressWarnings("unchecked")
        public E next() {
            if (! hasNext())
                throw new NoSuchElementException();
            return (E) snapshot[cursor++];
        }

        @SuppressWarnings("unchecked")
        public E previous() {
            if (! hasPrevious())
                throw new NoSuchElementException();
            return (E) snapshot[--cursor];
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor-1;
        }

        /**
         * 不支持。调用时总是抛出 UnsupportedOperationException 异常。
         * @throws UnsupportedOperationException always; {@code remove}
         *         is not supported by this iterator.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * 不支持。总是抛出 UnsupportedOperationException 异常。
         * @throws UnsupportedOperationException always; {@code set}
         *         is not supported by this iterator.
         */
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        /**
         * 不支持。总是抛出 UnsupportedOperationException 异常。
         * @throws UnsupportedOperationException always; {@code add}
         *         is not supported by this iterator.
         */
        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            Object[] elements = snapshot;
            final int size = elements.length;
            for (int i = cursor; i < size; i++) {
                @SuppressWarnings("unchecked") E e = (E) elements[i];
                action.accept(e);
            }
            cursor = size;
        }
    }
```

# 总结
毋庸置疑，CopyOnWriteArrayList 是线程安全的。它使用了一种叫写时复制（COW）的方法，任何写操作都会创造一个新的数组，用于替换原数组。写操作需要加锁，避免多个线程同时执行写操作。读操作不需要加锁，所以读到的数据并不一定完全是最新的数据。

线程并发读数组有以下几种情况：

1、如果写操作未完成，那么直接读取原数组的数据；

2、如果写操作完成，但是引用还未指向新数组，那么也是读取原数组数据；

3、如果写操作完成，并且引用已经指向了新的数组，那么直接从新数组中读取数据。

CopyOnWriteArrayList 主要用到读写分离和开辟新的空间这两套方案来解决并发冲突。

由于所有的写操作都需要创造新的数组，并且包含大量的数组元素复制操作，所以 CopyOnWriteArrayList 适合读多写少的操作。在数据量大时，尽量避免使用 CopyOnWriteArrayList 作为容器。
