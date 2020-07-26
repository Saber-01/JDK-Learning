# 概述
ConcurrentLinkedQueue 是非阻塞无界并发队列，主要利用 CAS 实现多线程环境下的并发安全，元素入队出队规则为 FIFO (first-in-first-out 先入先出) 。
由于是完全基于无锁算法实现的，所以当出现多个线程同时进行修改队列的操作（比如同时入队），很可能出现CAS修改失败的情况，那么失败的线程会进入下一次自旋，再尝试入队操作，直到成功。所以，在并发量适中的情况下，ConcurrentLinkedQueue一般具有较好的性能。

# 成员变量
此类中节点的组织形式为单向链表形式，类属性维护两个指向节点的引用，分别为 head 和 tail，表示头结点和尾节点。

为了提高效率，head 并非在每个时刻都指向队列绝对意义上的头结点，同样 tail 并非在每个时刻都指向队列绝对意义上的尾结点，因为并不是每次更新操作都会实时更新 head 和 tail（如果每次操作都实时更新，那并发环境下的更新将会无限趋近于同步状态下的更新，效率较低。实际上，在入队或出队操作中检查到 head/tail 和实际位置相差一个节点时，才会通过 CAS 更新它们的位置，并且当 CAS 失败时，当前线程并不会二次尝试）。但是它们遵循一定的规则，这些规则在下面的注释行中已经罗列出来。
```java
  /**
     * head 是从第一个节点可以在 O(1) 时间内到达的节点。
     * 不变性：
     * - 所有存活（item 非 null）的节点都可以通过 head 的 succ() 访问到。
     * - head 不等于 null。
     * - head 的 next 不能指向自己。
     * 可变性：
     * - head 的 item 可能为 null，也可能不为 null。
     * - 允许 tail 滞后于 head，即从 head 开始遍历队列，不一定能到达 tail。
     */
    private transient volatile Node<E> head;

    /**
     * tail 是从最后一个节点（node.next == null）可以在 O(1) 时间内到达的节点。
     * 不变性：
     * - 最后一个节点可以通过 tail 的 succ() 访问到。
     * - tail 不等于 null。
     * 可变性：
     * - tail 的 item 可能为 null，也可能不为 null。
     * - 允许 tail 滞后于 head，即从 head 开始遍历队列，不一定能到达 tail。
     * - tail 的 next 可以指向自身。
     */
    private transient volatile Node<E> tail;

```
# 内部类 node
节点类 Node 是保存元素值的封装类，除了包含基本的 item 变量和 next “指针”之外，还提供了在并发环境下的 CAS 原子操作 casItem、lazySetNext、casNext，它们构成了并发环境下完成入队出队操作的基础。
```java
// 节点类
    private static class Node<E> {
        volatile E item;
        volatile Node<E> next;

        /**
         * 构造函数。
         */
        Node(E item) { //UNSAFE方法，直接存入内存，保证其他线程的可见性
            UNSAFE.putObject(this, itemOffset, item);
        }

        // CAS 方式改变节点的值
        boolean casItem(E cmp, E val) {
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        // 延迟设置节点的 next，不保证值的改变被其它线程看到。减少不必要的内存屏障，
        // 提高程序效率。
        void lazySetNext(Node<E> val) {
            UNSAFE.putOrderedObject(this, nextOffset, val);
        }

        // CAS 方式更新 next 指向
        boolean casNext(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        // Unsafe mechanics

        private static final sun.misc.Unsafe UNSAFE;
        private static final long itemOffset;
        private static final long nextOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Node.class;
                itemOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }
```
# 关键方法
## poll
出队操作，即从队列头部弹出并返回该值。因为该队列是HOPS模式，可以简单理解为，头节点和尾节点如果在第一次判断就满足指向真实有效的队列头尾时，那么本次操作只负责节点相关操作，并不会更新head和tail的引用，所以在一般的情况中，head和tail并不会时刻都指向队列真实的头尾位置。因此出队操作，需要找到一个真实的队列头部进行弹出，因为该队列是禁止null元素，所以就是要找到一个item不为null的进行弹出。具体操作如下：
* 首先第一次自旋开始会得到head的引用，分别赋值到h和p。并且取出当前p的item，即头节点的item
* 判断头节点的item是否为null，如果不为null，则CAS设置其为null。并进入if语句继续判断
	* 如果p!=h，即如果当前是第一次自旋，那么p=h，因为这两个引用都没有更新，就不执行更新head的操作，即也就是一开始head.item!=null时，此时head指向真实的队列头节点，那么设置为null后，并不会更新head，而是直接返回item值退出自旋
	*  如果p=h，即至少执行过一次p=q，找到了真正的头节点，那么就调用updateHead设置新的头节点，并且原先的头节点的next会指向自身。
* 如果头节点的item为null，且(q = p.next) == null，说明队列已经没有有效节点，更新updateHead，返回null。
* 如果发现p=q=p.next。说明当前头节点发生更新，因为发生更新后，原先头节点的next就会指向自身。那么直接跳到restartFromHead，即重新执行h=head,p=h，更新头节点后再开始自旋。
* 如果以上都没满足，即只是item为null，那么将p=q，向后遍历，开始第二次自旋。
```java
 // 从队列头部取元素
    public E poll() {
        restartFromHead:
        for (;;) {
            // 从队列头部开始扫描
            for (Node<E> h = head, p = h, q;;) {
                E item = p.item;

                // 如果当前节点的 item 不为 null，表示找到了头结点，
                // CAS 修改当前节点的 item 为 null
                if (item != null && p.casItem(item, null)) {
                    if (p != h) // 一次跳两个节点。
                        // 如果 p 的 next 不等于 null，将 head 设置为 p 的 next
                        // （因为 p 中的 item 会被返回，p 即将变成无效节点）
                        // 如果 p 的 next 等于 null，说明找到的节点也是队列最后一个节点，则将 head 设置为 p
                        updateHead(h, ((q = p.next) != null) ? q : p);
                    return item;
                }
                // 以下三种情况前提为 item 等于 null
                // 当前节点的下一个节点为 null（且当前 item 为 null），说明队列中
                // 已经没有有效节点了。将 head 指针设置为 p，并将之前头结点的
                // next 指向自己
                // 返回 null。
                else if ((q = p.next) == null) {
                    updateHead(h, p);
                    return null;
                }
                // 如果此时头节点被更新了，说明有线程成功设置了头节点。重新开始外层自旋，即会更新h和p指向新的头节点。
                else if (p == q)
                    continue restartFromHead;

                // 仅仅只是 item 等于 null，并且队列还有节点，则继续往后查找有效 item
                else
                    p = q;
            }
        }
    }
```
***
## offer
offer 函数用于入队操作。在此队列中 head 并不一定指向队列最头部元素（最头部元素并不一定是有效元素，因为其 item 可能为 null，是因为 item 可能已经被获取，但是还没有显式地删除头部节点），tail 也不一定指向最后一个元素。
* 首先先用传入的e创建一个新节点，取得tail赋值到t和p，开始自旋：
* 使用q=p.next，得到p的下一个节点，如果当前线程为第一次自旋，那么p就会是tail的下一个节点。
* 第一个情况，p==null 。说明是队列的最后一个节点，
	* 尝试设置p的next指向新节点，成功进入判断：
		* p!=t，如果满足，说明至少执行过一次自旋寻找真正的tail，所以尝试CAS更新tail为新建的节点。
		* 不满足，说明第一次自旋就满足q==null.说明tail指向真正的队列尾节点，那么不更新，直接返回true
	* 失败，重新开始自旋。会重新更新q的值。
* 第二种情况，p!=null,且p==q,next指针会指向自身的只有在updateHead方法设置了新的头节点时会出现，说明说明了当前p已经被设置成过期的头节点，将t赋值给tail，那么判断原始t和新的t是否相等，
	* tail变化了，那么p指向新的tail。
	* tail 没有变化，也就是现在tail已经在head前面了，那么指向head，开始重新自旋。
* 第三种情况，如果p!=null ,且p!=q，那么说明的p还未到真实的tail节点：
	* 判断如果p已经更新过一次，现在p!=t，那将t赋值为tail，将t和之前的t记录的tail比较，如果tail发生了变化，那么p直接指向tail。
	* 如果不满足以上条件，直接p赋值为q,即向后遍历，继续自旋。
```java
 /**
     * 将指定元素添加到队列尾部。
     * 由于队列时无界队列，此方法不会返回 false。
     *
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        checkNotNull(e); //检查非null。
        final Node<E> newNode = new Node<E>(e);  //用e新建一个节点。

        // 由于松弛阈值的存在，tail 并不一定每时每刻都指向队列的最后一个节点，
        // 自旋从 tail 节点开始查找最后一个节点
        for (Node<E> t = tail, p = t;;) {
            Node<E> q = p.next;//q为tail的下一个节点。
            // 只有进入这一个分支才会添加next节点，才可能返回
            if (q == null) {  //如果tail的next指向null，说明是最后一个节点。
                // CAS 将 p 的 next 指向新创建的 newNode
                if (p.casNext(null, newNode)) {
                    // 成功在队列尾部插入新的节点
                    // p不等于t,说明传入的p至少在自旋时更新了一次，则 tail 和真正的尾节点之间已经隔了一个
                    // 节点了。所以需要更新 tail 指向真正的尾节点（注意 casTail 可能执行失败）。
                    if (p != t)  //成功一次会跳2个节点。
                        casTail(t, newNode);  //就算这一步失败了，也没事。因为tail本身就是延迟更新。
                    // 执行完毕，返回 true
                    return true;   //只有放在了末尾才会退出自旋。
                }
            }
            else if (p == q)
                // p 为标记节点，这种情况发生在元素入队的同时又出队了
                // t != (t = tail) 左边t为原先的，右边t为赋值后的。
                // 如果 tail 节点没有变化，p 指向 tail，然后重新循环
                // 如果 tail 节点变化了，从 head 节点开始往后遍历
                // t 永远指向新的 tail
                p = (t != (t = tail)) ? t : head;
            else
                // q 不为 null 而且 p 不是标记节点
                // 如果 p 不等于原来的尾节点 t，那么将t=tail,并且如果t!=原来的t了 则p 等于新的 tail
                // 否则 p 往后移动，继续往后查找
                p = (p != t && t != (t = tail)) ? t : q;
        }
    }
```
***
## peek
注意：head一定会指向一个node节点，只是这个node可能有效可能无效，即可能head.item不为null，可能实际队列头节点已经变化，head.item为null。
```java
  // 获取头部节点的 item
    public E peek() {
        restartFromHead:
        for (;;) {
            // 从 head 节点开始查找
            for (Node<E> h = head, p = h, q;;) {
                E item = p.item;
                // 一种情况是item不为null，一种是队列为空(item取出也为null)，所以两种情况返回都正确
                if (item != null || (q = p.next) == null) {
                    updateHead(h, p); //h和p不相同时，才会更新头节点。
                    return item;
                }
                // 如果刚好碰到head被重新设置，即item为null，next指向自身。
                else if (p == q)
                    continue restartFromHead;
                 //如果item为null，则继续往后
                else            //如果发现item为null，那么继续向下遍历。
                    p = q;
            }
        }
    }
```
# 总结
ConcurrentLinkedQueue使用了自旋+CAS的非阻塞算法来保证线程并发访问时的数据一致性。由于队列本身是一种链表结构，所以虽然算法看起来很简单，但其实需要考虑各种并发的情况，实现复杂度较高，并且ConcurrentLinkedQueue不具备实时的数据一致性，实际运用中，队列一般在生产者-消费者的场景下使用得较多，所以ConcurrentLinkedQueue的使用场景并不如阻塞队列那么多。

另外，关于ConcurrentLinkedQueue还有以下需要注意的几点：

* ConcurrentLinkedQueue的迭代器是弱一致性的，这在并发容器中是比较普遍的现象，主要是指在一个线程在遍历队列结点而另一个线程尝试对某个队列结点进行修改的话不会抛出ConcurrentModificationException，这也就造成在遍历某个尚未被修改的结点时，在next方法返回时可以看到该结点的修改，但在遍历后再对该结点修改时就看不到这种变化。
* size方法需要遍历链表，所以在并发情况下，其结果不一定是准确的，只能提供一个估计量。
* 显而易见在 offer 方法中，如果获取不到数据，将会直接返回 null，不会有任何的等待。这一点是 ConcurrentLinkedQueue 和阻塞队列在功能上最大的区别。线程池中会调用队列的 take 方法获取元素，如果获取不到，需要等待，直到获取到为止，所以线程池不能使用 ConcurrentLinkedQueue。
## HOPS(延迟更新的策略)的设计

通过上面对offer和poll方法的分析，我们发现tail和head是延迟更新的，两者更新触发时机为：
*  tail更新触发时机：当tail指向的节点的下一个节点不为null的时候，会执行定位队列真正的队尾节点的操作，找到队尾节点后完成插入之后才会通过casTail进行tail更新；当tail指向的节点的下一个节点为null的时候，只插入节点不更新tail。
*   head更新触发时机：当head指向的节点的item域为null的时候，会执行定位队列真正的队头节点的操作，找到队头节点后完成删除之后才会通过updateHead进行head更新；当head指向的节点的item域不为null的时候，只删除节点不更新head。

那么这样设计的意图是什么呢? 如果让tail永远作为队列的队尾节点，实现的代码量会更少，而且逻辑更易懂。但是，这样做有一个缺点，如果大量的入队操作，每次都要执行CAS进行tail的更新，汇总起来对性能也会是大大的损耗。如果能减少CAS更新的操作，无疑可以大大提升入队的操作效率，所以每间隔1次(tail和队尾节点的距离为1)进行才利用CAS更新tail。对head的更新也是同样的道理，虽然，这样设计会多出在循环中定位队尾节点，但总体来说读的操作效率要远远高于写的性能，因此，多出来的在循环中定位尾节点的操作的性能损耗相对而言是很小的。



