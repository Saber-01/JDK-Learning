# 概述
变量值的共享可以使用public static 变量的形式实现，所有的线程都是用同一个public static变量，那如何实现每一个线程都有自己的变量呢，jdk提供的ThreadLocal就是用于解决这样的问题。
ThreadLocal类提供了线程局部 (thread-local) 变量。这些变量与普通变量不同，每个线程都可以通过其 get 或 set方法来访问自己的独立初始化的变量副本。ThreadLocal 实例通常是类中的 private static 字段，它们希望将状态与某一个线程（例如，用户 ID 或事务 ID）相关联。
## 整体关系
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200721211605854.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)

# 基本原理
ThreadLocal中的嵌套内部类ThreadLocalMap，这个类本质上是一个map，和HashMap之类的实现相似，依然是key-value的形式，其中有一个内部类Entry，其中key可以看做是ThreadLocal实例，但是其本质是持有ThreadLocal实例的弱引用（之后会详细说到）。
通过 ThreadLocal 在指定的线程中存储数据。数据存储过后，只有该线程才能获取到数据，其他线程无法获取数据。
实现上述目的的 ThreadLocal 依赖的是 ThreadLocalMap。每个线程内部绑定一个 ThreadLocalMap，在此类的映射中，将 ThreadLocal 对象作为 key，将希望存入的值作为 value。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200721211731911.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)

# 关键内部类ThreadLocalMap
此 map 和 HashMap 一样使用 Entry 作为 key-value 的存储结构。和 HashMap 不同的是，Entry 继承自 WeakReference 弱引用，用于在线程被销毁的时候，对 key进行垃圾回收。
发生哈希碰撞时，HashMap 使用双向链表和红黑树组织桶内数据结构，而 ThreadLocalMap 使用线性探查解决。
ThreadLocalMap 会频繁检查 table 数组中失效的 Entry，即被回收后key为null的entry，并进行垃圾回收和重新整理。
## Entry
```java
   /**
         * entry 继承自 WeakReference，使用 ThreadLocal 作为 key。
         * key 为 null（entry.get() == null）表示 key 不再被引用，entry 可以从
         * table 里删除了。
         *
         * Entry 继承 WeakReference，使用弱引用，可以将 ThreadLocal 对象
         * 的生命周期和线程生命周期解绑，持有对ThreadLocal的弱引用，可以
         * 使 ThreadLocal 在没有其他强引用的时候被回收掉，这样可以避免因为
         * 线程得不到销毁导致 ThreadLocal 对象无法被回收。
         */
        static class Entry extends WeakReference<ThreadLocal<?>> {
            // key 对应的 value
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k); //使用k在底层创建了一个弱引用
                value = v;
            }
        }
```
super(k)会调用弱引用类中的方法，将内部referent的值设置为k。value则为简单的赋值，为强引用。这也是频繁删除失效entry的原因
## 成员变量
```java
 /**
         * 初始容量，必须是 2 的幂。
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * 存放数据的数组，每个元素为 entry。
         * 必要时扩容。
         * table 的长度总是 2 的幂。
         */
        private ThreadLocal.ThreadLocalMap.Entry[] table;

        /**
         * table 中 entry 的数量
         * The number of entries in the table.
         */
        private int size = 0;

        /**
         * 扩容的阈值。设置为2/3 的 table.length
         */
        private int threshold; // Default to 0

```
***
## 构造函数
第一个构造函数在首次创建Thread中的threadLocals为null时，创建新Map时候会调用的，
第二个是createInheritedMap方法中会使用的，该方法在Thread的初始化函数中用于继承父类的inheritableThreadLocals使用。
```java
  /**
         * Construct a new map initially containing (firstKey, firstValue).
         * ThreadLocalMaps are constructed lazily, so we only create
         * one when we have at least one entry to put in it.
         */
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            table = new Entry[INITIAL_CAPACITY];
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            table[i] = new Entry(firstKey, firstValue);
            size = 1;
            setThreshold(INITIAL_CAPACITY);
        }

        /**
         * Construct a new map including all Inheritable ThreadLocals
         * from given parent map. Called only by createInheritedMap.
         *
         * @param parentMap the map associated with parent thread.
         */
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new Entry[len];

            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }
```
***
## 工具类方法
### setThreshold、 nextIndex、prevIndex
```java
/**
         * 将扩容阈值设置为长度的 2/3
         */
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        /**
         * 计算下一个位置。table 可看成环形数组，当到达数组最后一个索引时，
         * 需要回到数组头部。
         */
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * 计算前一个位置。table 可看成环形数组。
         */
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }
```
setThreshold将阈值设置为 table 数组长度的 2/3，而 table 数组长度保持为 2 的幂。

为了符合线性探查的要求，将 table 作为循环数组，所以利用nextIndex和prevIndex方便计算下一个索引和上一个索引。
***
### expungeStaleEntry
```java
 /**
         * 通过 rehash 位于 staleSlot 和下一个空槽之间的任何可能碰撞的 entry，
         * 删除无效的 entry。还将删除到 null 槽之前遇到的其他无效 entry。
         *
         * @param staleSlot index of slot known to have null key
         * @return the index of the next null slot after staleSlot
         * (all between staleSlot and this slot will have been checked
         * for expunging).
         */
     
        private int expungeStaleEntry(int staleSlot) {
            ThreadLocal.ThreadLocalMap.Entry[] tab = table;
            int len = tab.length;

            // 删除 staleSlot 位置的无效 entry
            tab[staleSlot].value = null;
            tab[staleSlot] = null;
            size--;

            // Rehash until we encounter null
            ThreadLocal.ThreadLocalMap.Entry e;
            int i;
            // 从 staleSlot+1位置 开始向后扫描一段连续的 entry，遇到空槽停止
            for (i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();//得到entry的软引用key
                // 如果遇到的 key 为 null，表示无效 entry，进行清理
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
                } else {
                    // 遇到的 key 不为 null，计算索引
                    int h = k.threadLocalHashCode & (len - 1);  //计算key不为null时key的散列值
                    // 计算出来的索引 h 与当前的索引 i 不一致，从计算出来的索引
                    // 开始，向后查找到第一个空槽，把当前 entry 移动到其正确的
                    // 位置。同时将 i 处置为 null。
                    if (h != i) { //如果当前散列值不为i，说明第一次散列时位置上存在值，只是向后移动找空位填造成的
                        tab[i] = null;   //就将当前桶赋值为null，方便GC

                        // Unlike Knuth 6.4 Algorithm R, we must scan until
                        // null because multiple entries could have been stale.
                        //然后将正确的散列值h放到对应位置上，如果发现被占用，
                        //就向后移动，直到找到空桶进行填入
                        while (tab[h] != null)
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            // 返回遇到的空槽的索引。即table[i]==null
            return i;
        }
```
 方法概括为：
*  首先删除table中stateSlot这个桶上的entry，进行回收
 * 然后将指定的参数staleSlot代表的桶位置作为初始遍历起点
* 直到遇到之后的第一个空桶为止，才停止循环，即遇到table[i]=null停止
* 在遍历过程中，将遇到的每一个entry的key进行判断，
* 如果key为null就重置entry为null，方便GC，
* 如果key不为null，则对这个entry进行重新散列，散列规则和前面一致，用key的hashcode做与运算得到的正确位置的h，如果桶h不为空，则填充到之后的第一个空桶中。
***
### replaceStaleEntry
```java
   /**
         * 替换无效的 entry。
         *
         * @param  key the key
         * @param  value the value to be associated with key
         * @param  staleSlot index of the first stale entry encountered while
         *         searching for key.
         */
        private void replaceStaleEntry(ThreadLocal<?> key, Object value,
                                       int staleSlot) {
            ThreadLocal.ThreadLocalMap.Entry[] tab = table;
            int len = tab.length;
            ThreadLocal.ThreadLocalMap.Entry e;

            // 由于使用的是线性探查，所以需要向后查找和向前查找，确保 entry
            // 放在最前面的空桶里，确保清除了所有的无效 entry


            // 根据转入的无效 entry 的位置（staleSlot），向前扫描一段连续的
            // entry。直到遇到第一个为null的桶
            // Back up to check for prior stale entry in current run.
            // We clean out whole runs at a time to avoid continual
            // incremental rehashing due to garbage collector freeing
            // up refs in bunches (i.e., whenever the collector runs).

            int slotToExpunge = staleSlot;//记录要开始删除的无效桶位置的起点

            //首先需要从传入staleSlot-1开始向前遍历
            //此循环只会在遇到第一个为null的桶时结束，
            //所以slotToExpunge记录的是循环的最后一个放的是无效entry的桶的索引
            //即理解为，这个索引到staleSlot之间至少存在一个无效entry。
            for (int i = prevIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = prevIndex(i, len))
                // 如果是无效的，更新 slotToExpunge 记录此时的索引
                if (e.get() == null)  //entry的key为null，说明无效。
                    slotToExpunge = i;

              //注意从上面循环结束的 slotToExpunge，只有在没被更新的情况下，
            //下面这个循环才会去设置slotToExpunge。
            // 即如果slotToExpunge！=staleSlot，那么经过下面的循环slotToExpunge也不会改变了

            // 从 staleSlot+1 开始向后遍历，直到遇到第一个为null的桶，或满足条件
            for (int i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get(); //得到entry的key

                // If we find key, then we need to swap it
                // with the stale entry to maintain hash table order.
                // The newly stale slot, or any other stale slot
                // encountered above it, can then be sent to expungeStaleEntry
                // to remove or rehash all of the other entries in run.
                // 如果找到了 key，把 key 对应的 value 设置成指定的 value。
                if (k == key) {
                    e.value = value;  //覆盖值

                    // 把 stableSlot 无效的引用转移到索引 i 位置，然后将
                    // stableSlot 位置设置成有效的指定 key-value
                    //即根据传入的key找到的对应entry和传入的staleSlot上的无效桶上的entry交换。
                    tab[i] = tab[staleSlot];
                    tab[staleSlot] = e;

                    // Start expunge at preceding stale entry if it exists
                    // 如果向前查找没有找到无效的 entry，并且查找过程中也没有遇到无效entry
                    // 则更新 slotToExpunge为当前的i。
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;  //将要开始删除的位置赋值为交换后无效entry的桶位置i
                    //使用更新的slotToExpunge进行expungeStaleEntry，
                    // 即会删除当前位置开始到后面出现的第一个null值的桶之间的所有无效的entry，
                    //并将有效的entry进行重新散列。
                    cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
                    return;
                }

                // If we didn't find stale entry on backward scan, the
                // first stale entry seen while scanning for key is the
                // first still present in the run.
                // 如果向后扫描过程中，还没遇到相等的key之前遇到了无效entry，
                // 且之前向前扫描中没有找到无效的 entry，
                // 则更新 slotToExpunge 为当前的 i，
                //注意更新完staleToExpunge后，下一次运到无效entry不需要更新了
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
                // 如果向前扫描找到了无效的 entry，则 slotToExpunge 不会变
            }

            // 经过向前和向后查找之后，若 staleSlot 位置的 value 为空，表示
            // key 之前不存在，则直接新增一个 entry
            tab[staleSlot].value = null;
            tab[staleSlot] = new ThreadLocal.ThreadLocalMap.Entry(key, value);

            // slotToExpunge 不等于 staleSlot，说明有无效的 entry 需要清理
            if (slotToExpunge != staleSlot)
                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
        }
```
* 该方法除了替换，还会包括一个向前遍历和一个向后遍历的循环
 * 除了找到对应的key进行替换后，
 * 还会确定一个slotToExpunge,这个slotToExpunge的优先级一定是：staleSlot-1向前遍历找到的索引>staleSlot+1 向后遍历还未找到对应entry时遇到的无效entry的slot索引>最后找到对应entry后进行交换后的slot索引。
 * 从slotToExpunge开始调用一次expungeStaleEntry。
 * 并从返回的值为null的桶的索引开始再进行一次cleanSomeSlots。
 
***
###  cleanSomeSlots
```java
    /**
         * 启发式扫描一些单元格，寻找无效的 entry。这是在添加新元素或删除
         * 另一个无效元素时调用的。它执行 log 级次数的扫描，以平衡
         * 无扫描（快速但保留垃圾）和与元素成比例的扫描次数，后者将会找到
         * 所有垃圾但是会导致一些插入花费 O(n) 时间。
         *
         * @param i a position known NOT to hold a stale entry. The
         * scan starts at the element after i.
         *
         * @param n scan control: {@code log2(n)} cells are scanned,
         * unless a stale entry is found, in which case
         * {@code log2(table.length)-1} additional cells are scanned.
         * When called from insertions, this parameter is the number
         * of elements, but when from replaceStaleEntry, it is the
         * table length. (Note: all this could be changed to be either
         * more or less aggressive by weighting n instead of just
         * using straight log n. But this version is simple, fast, and
         * seems to work well.)
         *
         * @return true if any stale entries have been removed.
         */
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            ThreadLocal.ThreadLocalMap.Entry[] tab = table;
            int len = tab.length;//table长度
            do {
                i = nextIndex(i, len); //取到下一个索引
                ThreadLocal.ThreadLocalMap.Entry e = tab[i]; //取到下一个索引对应桶上的entry
                if (e != null && e.get() == null) {   //如果entry不为null，且entry.key为null，说明entry过期
                    // 如果遇到过期entry，则重置n，说明又要进行log2(n)次的遍历
                    n = len;
                    removed = true;
                    // 调用 expungeStaleEntry 删除无效的 entry
                    i = expungeStaleEntry(i);
                }
            } while ( (n >>>= 1) != 0); //>>>无整数右移，即n/2,
            // 如果进行过删除无效 entry 的操作，返回 true
            return removed;
        }
```
注意以上方法中判断循环的条件是 (n >>>= 1) != 0，但是n值会在遍历到key为null的无效entry时进行重置。
### expungeStaleEntries
该方法比较简单，就是遍历底层的table，如果遇到key为null的无效entry对这个桶索引调用expungeStaleEntry方法进行清理。
```java
/**
         * 清除 table 中所有的无效 entry。
         */
        private void expungeStaleEntries() {
            ThreadLocal.ThreadLocalMap.Entry[] tab = table;
            int len = tab.length;
            for (int j = 0; j < len; j++) {
                ThreadLocal.ThreadLocalMap.Entry e = tab[j];
                if (e != null && e.get() == null)
                    expungeStaleEntry(j);
            }
        }
```
***
## getEntry
```java
 /**
         * 获取与 key 关联的 entry。此方法是快速查找：直接命中存在的 key，
         * 否则将会跳转到 getEntryAfterMiss。这是为了最大限度提高直接命中
         * 的性能设计的，部分原因是为了使这种方法线性化。
         *
         * @param  key the thread local object
         * @return the entry associated with key, or null if no such
         */
        private ThreadLocal.ThreadLocalMap.Entry getEntry(ThreadLocal<?> key) {
            int i = key.threadLocalHashCode & (table.length - 1);
            ThreadLocal.ThreadLocalMap.Entry e = table[i];
            if (e != null && e.get() == key)    //如果找到的桶位置上不为null，key也相等，那么直接取出当前entry
                return e;
            else
                return getEntryAfterMiss(key, i, e);
        }

        /**
         * 当直接查找索引没有找到该 key 时的 getEntry 版本。
         *
         * @param  key the thread local object
         * @param  i the table index for key's hash code
         * @param  e the entry at table[i]
         * @return the entry associated with key, or null if no such
         */
        private ThreadLocal.ThreadLocalMap.Entry getEntryAfterMiss(ThreadLocal<?> key, int i, ThreadLocal.ThreadLocalMap.Entry e) {
            ThreadLocal.ThreadLocalMap.Entry[] tab = table;
            int len = tab.length;

            //从上面找到的key散列后本应放的位置开始遍历
            //过程中遇到entry如果key为null，则会调用expungeStaleEntry从i开始删除过期条目到下一个空桶为止
            //如果遇到key相等的，就直接返回这个entry，否则遍历到第一个空桶为止。
            while (e != null) {
                ThreadLocal<?> k = e.get();
                if (k == key)
                    return e;
                // 清理无效的 entry
                if (k == null)
                    expungeStaleEntry(i);
                    // 线性探查向后查找
                else
                    i = nextIndex(i, len);
                e = tab[i];
            }
            return null;    //没在循环中返回，说明没找到。返回null
        }
```
* 根据传入的key，进行一次散列，得到散列值i,如果桶i中，也就是e=table[i]!=null，就接着判断e.get()获得的key
* 如果key也相等，说明直接命中，就返回e，如果不相等，则需要进一步在getEntryAfterMiss中判断
* 从上面找到的key散列后本应放的位置i开始遍历,循环正常终止的条件为下一个空桶为止。
* 在遍历过程中，如果遇到key=null的无效entry还会调用expungeStaleEntry进行清理。
* 如果遇到entry的key等于传入的key时，就返回e。
* 如果循环结束还没返回，说明没有找到，则返回null。
* ***
## set
```java
  /**
         * 将指定 key 对应的 value 设置成指定 value。
         *
         * @param key the thread local object
         * @param value the value to be set
         */
        private void set(ThreadLocal<?> key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.

            ThreadLocal.ThreadLocalMap.Entry[] tab = table;
            int len = tab.length;
            // 计算所在位置的索引
            int i = key.threadLocalHashCode & (len-1);

            // 根据计算出来的索引找到在数组中的位置，如果已经被占用则使用
            // 线性探测往后查找
            for (ThreadLocal.ThreadLocalMap.Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {

                // 获取 entry 的 key
                ThreadLocal<?> k = e.get();

                // 如果 key 就是指定的 key，即找到了其所在 entry，设置 value 值后返回
                if (k == key) {
                    e.value = value;   //覆盖
                    return;
                }
                // 如果 k 为 null，说明被回收了，该位置可以使用，使用新的
                // key-value 替换
                // 此时可能还没有找到 key，key 可能存在数组后面的位置
                if (k == null) {
                    //该方法除了替换，还会包括一个向前遍历和一个向后遍历的循环
                    //除了找到对应的key进行替换后，
                    //还会确定一个slotToExpunge,
                    //从slotToExpunge开始调用一次expungeStaleEntry,
                    //并从返回的值为null的桶的索引开始再进行一次cleanSomeSlots。
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }
            //如果没找到传入的key，则新建entry。
            tab[i] = new ThreadLocal.ThreadLocalMap.Entry(key, value);
            int sz = ++size;    //size自加

            // cleanSomeSlots 清除 table[index] != null && table[index].get() == null
            // 的对象。这种 key 关联的对象已经被回收了。
            // 如果没有清除任何 entry，且使用量达到了阈值，则 rehash 扩容。
            //注意rehash会先使用expungeStaleEntries删除table中所有的失效entry后
            //再使用size和table的一半容量比较，如果size大于等于length/2，才会进行resize.
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }
```
* for循环从传入的key经过计算应该散列到的桶的索引i开始遍历，退出条件为直到遇到第一个为null值的table[i]
* 遍历过程中，如果entry的key和传入的key相等，会进行value的值覆盖，并返回
* 如果entry的key为null，则会调用replaceStaleEntry进行entry的整个覆盖。
* 在replaceStaleEntry方法中也会删除table中无效的entry，方法调用结束后，set会返回
* 如果以上条件没有返回，说明table中没有找到传入的key，那么就在循环结束的空桶位置新建entry放入，size自加。
* 然后调用一次cleanSomeSlots进行删除无效entry，如果此次清楚删除掉了一些无效entry，那么不进行扩容判断。
* 如果此次cleanSomeSlots没有清楚掉任意无效entry，那就将当前size和threshold进行比较
* 如果table中包含的键值对数量超过了threshold就进行rehash方法，进入扩容方法中。
## remove
```java
  /**
         * 删除指定 key 对应的 entry。
         */
        private void remove(ThreadLocal<?> key) {
            ThreadLocal.ThreadLocalMap.Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);
            for (ThreadLocal.ThreadLocalMap.Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    // 调用 WeakReference 的 clear 方法设置key值为null。
                    e.clear();
                    // 连续段内清除无效 entry
                    expungeStaleEntry(i);  //expungeStaleEntry方法从当前i位置清楚无效entry
                    return;
                }
            }
        }
```
* 对传入的key进行散列运算，得到i.
* 从桶i开始遍历，直到遇到一个空桶null为止
* 遍历过程，直到key相同的entry，调用软引用删除方法clear将内部key赋值为null，
* 然后调用expungeStaleEntry，从当前i开始到后面第一个为null的桶进行一次无效entry清理。
***
## 扩容方法
注意rehash并不是一定扩容，而是先调用一次expungeStaleEntries，清楚掉table中所有无效的entry，并让元素重新散列。
然后再判断删除后的键值对数量size 和1/2的table长度len比较，如果满足条件，就进行resize扩容。
* resize扩容方法中，新table容量扩大为老容量的两倍，所以它仍然是2的幂。
* 遍历旧表的过程中，再一次判断遍历到的entry是否key为null
* 如果为null，则令其value=null，方便GC。
* 如果，entry有效，则先计算散列索引，在线性检查插入新列表的空桶中，并且count计数加1
* 最后对threshold和size和table进行赋值，完成扩容。

```java
  /**
         * Re-pack and/or re-size the table. First scan the entire
         * table removing stale entries. If this doesn't sufficiently
         * shrink the size of the table, double the table size.
         */
        private void rehash() {
            expungeStaleEntries(); //先清楚table中所有的无效entry，且所有元素都经过重新散列。

            // Use lower threshold for doubling to avoid hysteresis
            // 全部清理过后 size 减小了，所以判断如果 size 大于 len / 2 即扩容
            //桶中元素个数如果超过了table长度的一半就进行扩容。
            //threshold=2/3len,threshld/4=1/6len,所以 threshold - threshold / 4= 1/2 len
            if (size >= threshold - threshold / 4)
                resize();
        }

        /**
         * table 的容量扩大为原来的两倍（仍然为 2 的幂）
         */
        //resize过程中也会删除掉过期的entry
        private void resize() {
            ThreadLocal.ThreadLocalMap.Entry[] oldTab = table; //得到旧表
            int oldLen = oldTab.length;   //得到旧表长度
            int newLen = oldLen * 2;   //乘2进行扩容
            //使用新容量作为指定长度，创建存放entry的新表
            ThreadLocal.ThreadLocalMap.Entry[] newTab = new ThreadLocal.ThreadLocalMap.Entry[newLen];
            int count = 0; //存放有效值，即entry中失效的不会被记录

            for (int j = 0; j < oldLen; ++j) {
                ThreadLocal.ThreadLocalMap.Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    // 虽然做过清理，但扩容过程中数组动态变化，可能又存在 k == null
                    if (k == null) {
                        e.value = null; // Help the GC
                    } else {
                        // 计算索引
                        int h = k.threadLocalHashCode & (newLen - 1);
                        // 使用线性探测查找空槽
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }

            // 设置新的阈值，为当前容量的2/3
            setThreshold(newLen);
            size = count; //size赋值为有效值count
            table = newTab;   //将底层数组进行赋值
        }
```
***
# ThreadLocal关键方法
## 构造函数即工具方法
ThreadLocal只有无参构造函数
getMap接收一个Thread对象，返回线程中的threadLoals记录的Map。
initialValue方法用户可以重写，用于指定get方法如果Map为null时，设置的初始vlaue。
setInitialValue在线程Map为空情况下调用ThreadLocal.get时调用的初始化Map。
createMaps调用ThreadLocalMap的构造函数，对当前线程的Map进行创建赋值。
```java
   /**
     * 构造函数
     * @see #withInitial(java.util.function.Supplier)
     */
    public ThreadLocal() {
    }
	
	ThreadLocal.ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;  //每个线程中都存放了属于自己的ThreadLocal.ThreadLockMap。
    }
    /**
     * 为这个 thread-local 变量返回当前线程的初始 value。此方法在线程第一次
     * 使用 get 方法访问变量时调用，除非线程之前使用了 set 方法。
     *
     * @return the initial value for this thread-local
     */
    protected T initialValue() {
        return null;
    }
  /**
     * Variant of set() to establish initialValue. Used instead
     * of set() in case user has overridden the set() method.
     *
     * @return the initial value
     */
    private T setInitialValue() {
        // value 默认为 null
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocal.ThreadLocalMap map = getMap(t);
        // map 不为 null 表示已经初始化，则调用 set 设置 value 即可
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
        return value;
    }
/**
     * 创建和 ThreadLocal 关联的 map
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the map
     */
    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocal.ThreadLocalMap(this, firstValue);
    }
```
***
## get
在线程执行过程中，调用某个 ThreadLocal 的 get 方法，即获取此线程和该 ThreadLocal 绑定的 value 值。

首先获取该线程的 ThreadLocalMap 变量，然后在获取到的 map 中，根据指定的作为 key 的 ThreadLocal，查找其对应的 value 并返回。如果Map为null，或者没有找到该key对应entry，则进行初始化（延迟初始化模式）。
```java
  /**
     * 返回当前线程的 tread-local 变量副本的值。如果当前线程没有该值，首先
     * 将其初始化为调用 initialValue 返回的值。
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        Thread t = Thread.currentThread();
        // 获取当前线程的 ThreadLocalMap 实例
        ThreadLocal.ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocal.ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        // map 为空则初始化，或者entry没有找到
        return setInitialValue();
    }
   

```
***
## set
调用 ThreadLocal 的 set 方法，将 value 和指定的线程绑定。在具体的实现方法中，和 get 类似，首先获取该线程的 ThreadLocalMap 对象，若该 map 存在则调用 map.set 方法设置 value，否则创建新的 map 作为 ThreadLocalMap。
```java
 /**
     * 设置当前线程的 thread-local 变量副本为指定的 value。大多数子类不需要
     * 重写此方法，仅仅依靠 initialValue 来设置 value。
     *
     * @param value the value to be stored in the current thread's copy of
     *        this thread-local.
     */
    public void set(T value) {
        // 获取当前线程
        Thread t = Thread.currentThread();
        // 获取当前线程持有的 ThreadLocalMap
        ThreadLocal.ThreadLocalMap map = getMap(t);
        // 如果持有则设置 Map 中此 ThreadLocal 对应的 value
        if (map != null)
            map.set(this, value);
            // 否则创造一个新的 Map 并设置值
        else
            createMap(t, value);
    }
```
***
## remove
调用 ThreadLocalMap 的 remove 方法，删除 key 为此 ThreadLocal 的键值对。
```java
 /**
     * 删除当前线程的 value。
     *
     * @since 1.5
     */
    public void remove() {
        ThreadLocal.ThreadLocalMap m = getMap(Thread.currentThread());
        if (m != null)
            m.remove(this);
    }
```
# 为什么 key 使用弱引用以及可能产生的内存泄露
如果使用强引用，当 ThreadLocal 对象的强引用被回收了，ThreadLocalMap 本身还持有 ThreadLocal 的强引用（因为 ThreadLocalMap 的 key 指向 ThreadLocal）。如果没有手动删除这个 key，ThreadLocal 就不会被回收。只要线程不消亡，ThreadLocalMap 引用的对象就不会被回收。可以认为这导致内存泄露（本该回收的无用对象没有被回收）。

如果使用弱引用，当 ThreadLocal 的强引用被回收了，就只剩下 ThreadLocalMap 持有的弱引用对象了，在下一次 gc 的时候，这个 ThreadLocal 就会被回收。但是 ThreadLocal 只是 key，其对应的 value 不是弱引用，不会被回收，当 key 变成 null 之后，value 再也无法被访问到，内存泄露依然存在。

这就是上面 expungeStaleEntry、cleanSomeSlots、expungeStaleEntries 方法存在的原因。每次 get/set/remove ThreadLocalMap 中的值的时候，会自动清理 key 为 null 的 value。

那为什么不对 value 使用弱引用呢？因为 value 在其他地方没有保留任何强引用，如果在这里使用弱引用，将会在任何一次 gc 的时候被回收，很明显这样是不对的。

强引用对象是指不会被回收的对象；软引用对象是指内部不足的时候回收的对象；弱引用对象是指存活到垃圾回收前的对象，此类对象在垃圾回收发生时立刻进行回收。

## 解决方法

如果 ThreadLocal 的强引用一直存在，只要线程不死，ThreadLocalMap 里的 key-value 将会一直存在（value 将会一直存在），因为无法通过弱引用来删除。

所以当某个 ThreadLocal 不再使用时，最好使用 ThreadLocal.remove 删除键值对。
# 参考
[ThreadLocal源码分析](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/ThreadLocal.md)
[ThreadLocal原理](https://www.jianshu.com/p/0ba78fe61c40)
