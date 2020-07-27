## ConcurrentLinkedDeque

ConcurrentLinkedDeque 是非阻塞双向无界并发队列，主要利用 CAS 实现所线程环境下的并发安全，元素入队出队规则支持 FIFO (first-in-first-out 先入先出) 和 FILO (first-in-last-out 先入后出)。

### 类属性

此类的实现方式和 ConcurrentLinkedQueue 基本一致。最重要的属性还是两个保存节点引用的变量 head、tail。

```java
    /**
     * head 节点必须是从列表中第一个节点可以在 O(1) 时间内访问到的节点。
     * 不变性：
     * - 第一个节点总是可以从 head 通过 head.prev 在 O(1) 时间内到达
     * - 所有有效的节点都可以从第一个节点通过 succ() 到达
     * - head != null
     * - head 的 next 不能指向自身
     * - head 不会是 gc-unlinked 节点（但是可能是 unlinked 节点）
     * 可变性:
     * - head.item 可能为 null
     * - head 可能从第一个或者最后一个节点或者 tail 节点不可达
     */
    private transient volatile Node<E> head;

    /**
     * tail 节点必须是从列表中第一个节点可以在 O(1) 时间内访问到的节点。
     * 不变性：
     * - 最后一个节点总是可以从 tail 通过 head.next 在 O(1) 时间内到达
     * - 所有有效的节点都可以从第一个节点通过 pred() 到达
     * - tail != null
     * - tail 不会是 gc-unlinked 节点（但是可能是 unlinked 节点）
     * 可变性：
     * - tail.item 可能为 null
     * - tail 可能从第一个或者最后一个节点或者 head 节点不可达
     */
    private transient volatile Node<E> tail;
```

### 内部类 Node

相对于 ConcurrentLinkedQueue，节点类 Node 的概念更复杂，主要有以下几种形式：

* 有效节点：满足 item != null 的节点即为有效节点

* 第一个结点(first node) / 最后一个节点(last node)：prev 为 null （next 不为 null）且没有自链接的节点为第一个节点，next 为 null（prev 不为 null）且没有自链接的节点为最后一个节点。第一个节点和最后一个节点的 item 可能为 null。

* 活跃节点（active node）：有效节点和第一个节点、最后一个节点的统称。

* 自链接节点：prev 或 next 指向自身，用于解除连接的操作中

* head/tail：head 和 tail 不一定是第一个/最后一个节点，但是从 head 通过 prev 总是可以找到 first node，从 tail 通过 next 总是可以找到 last node。

```java
    static final class Node<E> {
        volatile Node<E> prev;
        volatile E item;
        volatile Node<E> next;

        Node() {  // default constructor for NEXT_TERMINATOR, PREV_TERMINATOR
        }

        /**
         * 构造函数
         */
        Node(E item) {
            UNSAFE.putObject(this, itemOffset, item);
        }

        // CAS 更新 item
        boolean casItem(E cmp, E val) {
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        // 设置 next 属性
        void lazySetNext(Node<E> val) {
            UNSAFE.putOrderedObject(this, nextOffset, val);
        }

        // CAS 设置 next 属性
        boolean casNext(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        // 设置 prev 属性
        void lazySetPrev(Node<E> val) {
            UNSAFE.putOrderedObject(this, prevOffset, val);
        }

        // CAS 设置 prev 属性
        boolean casPrev(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, prevOffset, cmp, val);
        }
    }
```

### 成员函数

此类实现了 Deque 接口的所有方法，但基本上都依赖于 linkFirst、unlink 等核心方法构建。

#### 添加

在队列头部添加元素的函数为 **linkFirst**，相应地在尾部添加元素的函数为 **linkLast**。

函数实现的流程和步骤在注释中已经详细说明。如果是在头部添加元素，那么首先要找到第一个节点（并不一定是有效节点或 head 节点）。从 head 开始往前探查，直到找到第一个节点，将新的节点添加到该节点前面，然后 CAS 设置 head（允许失败）。如果遇到自链接节点，那么重新从 head 开始往前查找。

值得一提的是，在此类和 ConcurrentLinkedQueue 中，大多数实现的基本框架均涉及到两层循环，内层循环用于检索，而外层循环则用于重新从 head/tail 开始查找。这样做是因为并发环境下 head/tail 会被其他线程不断修改，那么当前节点的状态也会不断变化。在 CAS 的基础上，不同情况下（一般是 3 种）分别进行判断，分别处理。当当前节点变成自链接节点时，根据 head/tail 的约束，此时可以进入外层循环，从 head/tail 开始重新检索。（由此可以推测，head/tail 的更新势必需要 CAS 保证安全。而从源码可以看到，作者确实是这样做的。）

```java
    /**
     * 将元素 e 包装成一个节点，添加到队列头部
     */
    private void linkFirst(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<E>(e);

        restartFromHead:
        for (;;)
            // 从 head 开始往前寻找第一个节点（并不一定是有效节点）
            for (Node<E> h = head, p = h, q;;) {
                // 如果 p.prev 等于 null，跳过此 if 块，p 可能是自链接节点或第一个节点
                // 如果 p.prev 不等于 null，把 p，q 都向前移动一位
                // 移动过后，如果 p.prev 等于 null，p 有可能是第一个节点，跳过此 if 块
                // 如果 p.prev 不等于 null，p 肯定不是第一个节点，进入此 if 块，继续往前扫描
                if ((q = p.prev) != null &&
                        (q = (p = q).prev) != null)
                    // 如果 head 被修改，返回 head 重新开始查找
                    p = (h != (h = head)) ? h : q;
                // 如果是自链接节点，重新从 head 开始扫描
                else if (p.next == p) // PREV_TERMINATOR
                    continue restartFromHead;
                else {
                    // p 是第一个节点，将新创建节点的 next 设置为 p
                    newNode.lazySetNext(p);
                    // 将新节点的 prev 设置为 null
                    if (p.casPrev(null, newNode)) {
                        // CAS 将 head 设置为 p，允许 CAS 失败
                        if (p != h)
                            casHead(h, newNode);
                        return;
                    }
                }
            }
    }

    /**
     * 元素 e 包装成一个节点，添加到队列头部
     */
    private void linkLast(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<E>(e);

        restartFromTail:
        for (;;)
            // 从队列尾部开始往后扫描
            for (Node<E> t = tail, p = t, q;;) {
                // 不是最后一个节点，继续往后
                if ((q = p.next) != null &&
                        (q = (p = q).next) != null)
                    p = (t != (t = tail)) ? t : q;
                // 自链接节点，从新的 tail 开始重新扫描
                else if (p.prev == p) // NEXT_TERMINATOR
                    continue restartFromTail;
                // p 是最后一个节点
                else {
                    newNode.lazySetPrev(p);
                    if (p.casNext(null, newNode)) {
                        if (p != t)
                            casTail(t, newNode);
                        return;
                    }
                }
            }
    }
```

#### 删除

**unlink**

unlink 是删除元素最核心的方法，流程较复杂。

一共分为三种情况，前两种是删除节点为第一个节点或最后一个节点的情况，分别调用 unlinkFirst 或 unlinkLast 函数完成。

第三种情况是删除节点为中间节点。首先从删除节点的前一个节点开始往前扫描，找到即退出循环，然后从删除节点的后一个节点开始往后扫描，找到即退出循环（遇到自链接节点直接返回）。最后清除范围内的无效节点，更新 head、tail，设置要删除的节点自链接。

```java
    /**
     * 删除节点 x
     */
    void unlink(Node<E> x) {

        final Node<E> prev = x.prev;
        final Node<E> next = x.next;
        // 节点为第一个节点
        if (prev == null) {
            unlinkFirst(x, next);
            // 节点为最后一个节点
        } else if (next == null) {
            unlinkLast(x, prev);
        } else {
            Node<E> activePred, activeSucc;
            boolean isFirst, isLast;
            int hops = 1;

            // 从 prev 向前扫描，找到 item 不为 null 的前驱节点（有效节点）
            for (Node<E> p = prev; ; ++hops) {
                // 找到了
                if (p.item != null) {
                    activePred = p;
                    isFirst = false;
                    break;
                }
                Node<E> q = p.prev;
                if (q == null) {
                    // p 是自链接节点
                    if (p.next == p)
                        return;
                    // p 是第一个节点
                    activePred = p;
                    isFirst = true;
                    break;
                }
                // p 已经是自链接节点了，直接返回
                else if (p == q)
                    return;
                // 继续往前
                else
                    p = q;
            }

            // 从 next 向后扫描，找到 item 不为 null 的后继节点
            for (Node<E> p = next; ; ++hops) {
                // 找到了
                if (p.item != null) {
                    activeSucc = p;
                    isLast = false;
                    break;
                }
                Node<E> q = p.next;
                if (q == null) {
                    // p 是自链接节点
                    if (p.prev == p)
                        return;
                    // p 是最后一个节点
                    activeSucc = p;
                    isLast = true;
                    break;
                }
                // 自链接节点
                else if (p == q)
                    return;
                // 继续往后
                else
                    p = q;
            }

            // 无节点跳跃并且操作的节点有 first 或 last 时，不更新链表
            if (hops < HOPS
                    // always squeeze out interior deleted nodes
                    && (isFirst | isLast))
                return;

            // 删除 activePred 之后的连续无效节点
            skipDeletedSuccessors(activePred);
            // 删除 activeSucc 之前的连续无效节点
            skipDeletedPredecessors(activeSucc);
            // 完成删除操作后，原 x 左右不存在无效节点（除非第一个节点到 x
            // 而且/或者 x 到最后一个节点之间为无效节点）

            // 第一个或最后一个节点是无效节点，
            if ((isFirst | isLast) &&

                    // 再次检查是否连接上，且是否满足条件（已经删除 x 及前后无效节点）
                    (activePred.next == activeSucc) &&
                    (activeSucc.prev == activePred) &&
                    (isFirst ? activePred.prev == null : activePred.item != null) &&
                    (isLast  ? activeSucc.next == null : activeSucc.item != null)) {

                // 确保 x 不能从 head 到达
                updateHead();
                // 确保 x 不能从 tail 到达
                updateTail();

                // 设置 x 的 prev 指向自己
                x.lazySetPrev(isFirst ? prevTerminator() : x);
                // 设置 x 的 next 指向自己
                x.lazySetNext(isLast  ? nextTerminator() : x);
            }
        }
    }
```

**unlinkFirst/unlinkLast**

unlinkFirst 在删除节点为 first 节点时调用，用来清除 first 节点之后的连续无效节点（此处不涉及到 head，所以不需要两层循环）。方法的思路为从 first.next 开始往后查找，找到有效节点为止，然后将 first.next 指向该有效节点。unlinkLast 基本一样。

注意使用到 CAS 方式的更新仅为设置 first 的 next，且允许失败。

注意函数执行完之后，first 节点依然是原来的 first 节点，不管它有效还是无效。

```java
    /**
     * 删除队列头部的无效节点（item == null）
     */
    private void unlinkFirst(Node<E> first, Node<E> next) {
        // 从 next 开始往后寻找有效节点
        for (Node<E> o = null, p = next, q;;) {
            // p 可能是有效节点，可能是最后一个节点
            if (p.item != null || (q = p.next) == null) {
                // o 为 null 说明第一次循环就到这儿了，说明 next 节点为有效节点，直接返回。
                // o 不为 null 且 p 不是自链接节点，CAS 将参数 first 节点的 next 设置为 p
                if (o != null && p.prev != p && first.casNext(next, p)) {
                    // 删除 p 之前的连续无效节点
                    skipDeletedPredecessors(p);
                    // 如果满足以下三个条件：
                    // 1. 检查现在的 first 的前一个节点是否为 null（如果 p 是第一个节点）
                    // 2. p 可以是最后一个节点；如果不是，必须是有效节点
                    // 3. p 的 prev 是 first 节点（p 是 first 的后一个节点）
                    if (first.prev == null &&
                            (p.next == null || p.item != null) &&
                            p.prev == first) {

                        // 确保 o 不能从 head 到达
                        updateHead();
                        // 确保 o 不能从 tail 到达
                        updateTail();

                        // 设置 o 的 next 指向自身
                        o.lazySetNext(o);
                        // 设置 o 的 prev 指向 PREV_TERMINATOR
                        o.lazySetPrev(prevTerminator());
                    }
                }
                return;
            }
            // 自链接节点
            else if (p == q)
                return;
            // 继续往后
            else {
                o = p;
                p = q;
            }
        }
    }

    /**
     * 删除队列尾部的无效节点，与 unlinkFirst 基本一样
     */
    private void unlinkLast(Node<E> last, Node<E> prev) {
        for (Node<E> o = null, p = prev, q;;) {
            if (p.item != null || (q = p.prev) == null) {
                if (o != null && p.next != p && last.casPrev(prev, p)) {
                    skipDeletedSuccessors(p);
                    if (last.next == null &&
                            (p.prev == null || p.item != null) &&
                            p.next == last) {

                        updateHead(); // Ensure o is not reachable from head
                        updateTail(); // Ensure o is not reachable from tail

                        // Finally, actually gc-unlink
                        o.lazySetPrev(o);
                        o.lazySetNext(nextTerminator());
                    }
                }
                return;
            }
            else if (p == q)
                return;
            else {
                o = p;
                p = q;
            }
        }
    }
```

**skipDeletedPredecessors/skipDeletedSuccessors**

skipDeletedPredecessors 和 skipDeletedSuccessors 用于删除指定节点之前/之后的连续无效节点。

```java
    // 删除 x 之前的连续无效节点
    private void skipDeletedPredecessors(Node<E> x) {
        whileActive:
        do {
            // 从 prev 开始往前查找
            Node<E> prev = x.prev;
            Node<E> p = prev;
            findActive:
            for (;;) {
                // 找到可用节点
                if (p.item != null)
                    break findActive;
                Node<E> q = p.prev;
                if (q == null) {
                    // 自链接节点
                    if (p.next == p)
                        continue whileActive;
                    // 不是自链接节点说明已经到第一个节点了
                    break findActive;
                }
                else if (p == q)
                    continue whileActive;
                else
                    p = q;
            }

            // 已经找到可用节点 p，将 x 的 prev 设置为 p
            if (prev == p || x.casPrev(prev, p))
                return;

        } while (x.item != null || x.next == null);
    }

    // 删除 x 之后的连续无效节点
    private void skipDeletedSuccessors(Node<E> x) {
        whileActive:
        do {
            // 从 next 开始往后查找
            Node<E> next = x.next;
            Node<E> p = next;
            findActive:
            for (;;) {
                // 找到可用的节点
                if (p.item != null)
                    break findActive;
                Node<E> q = p.next;
                if (q == null) {
                    // 自链接节点
                    if (p.prev == p)
                        continue whileActive;
                    // 不是自链接节点说明已经到最后一个节点了
                    break findActive;
                }
                // 自链接节点
                else if (p == q)
                    continue whileActive;
                // 继续往后查找
                else
                    p = q;
            }

            // 找到可用的节点 p，将 x 的 next 设置为 p
            if (next == p || x.casNext(next, p))
                return;

        } while (x.item != null || x.prev == null);
    }
```

**updateHead/updateTail**

updateHead 和 updateTail 用于更新 head 和 tail，同样使用两层循环，用于不断应对多线程对 head/tail 的修改。在更新 head/tail 的时候使用 CAS 保证线程安全。不允许更新失败，更新失败会自旋重新进入更新流程。

```java
    /**
     * 确保在调用此方法之前未链接的节点在返回后无法从 head 访问。不保证消除
     * 松弛，此方法运行期间，只有 head 会指向处于活动状态的节点。
     */
    private final void updateHead() {
        Node<E> h, p, q;
        restartFromHead:
        // h 的 item 为 null 而且 h 不是第一个节点时
        // 从 head 往 prev 方向查找
        while ((h = head).item == null && (p = h.prev) != null) {
            for (;;) {
                // 如果 p 的前一个节点为 null，进入
                // 如果 p 往前移动一个之后，其前一个节点为 null，也进入
                if ((q = p.prev) == null ||
                        (q = (p = q).prev) == null) {
                    // 将 head 设置为 p，然后返回
                    if (casHead(h, p))
                        return;
                    // 重新获取 head 再循环
                    else
                        continue restartFromHead;
                }
                // head 被改变了，重新获取 head 然后循环
                else if (h != head)
                    continue restartFromHead;
                // 往前查找
                else
                    p = q;
            }
        }
    }

    /**
     * 确保在调用此方法之前未链接的节点在返回后无法从 tail 访问。不保证消除
     * 松弛，此方法运行期间，只有 tail 会指向处于活动状态的节点。
     */
    private final void updateTail() {
        // Either tail already points to an active node, or we keep
        // trying to cas it to the last node until it does.
        Node<E> t, p, q;
        restartFromTail:
        while ((t = tail).item == null && (p = t.next) != null) {
            for (;;) {
                if ((q = p.next) == null ||
                        (q = (p = q).next) == null) {
                    // It is possible that p is NEXT_TERMINATOR,
                    // but if so, the CAS is guaranteed to fail.
                    if (casTail(t, p))
                        return;
                    else
                        continue restartFromTail;
                }
                else if (t != tail)
                    continue restartFromTail;
                else
                    p = q;
            }
        }
    }
```

#### 其它

succ 用于获取当前节点的下一个节点，pred 用于获取当前节点的上一个节点（不一定是有效节点）。

first 用于获取第一个节点（从 head 开始向前扫描），last 用于获取最后一个节点（从 tail 开始向后扫描）。

**succ/pred/first/last**

```java
    /**
     * 返回 p 的后继节点，如果 p 是自链接节点，返回第一个节点。
     */
    final Node<E> succ(Node<E> p) {
        // TODO: should we skip deleted nodes here?
        Node<E> q = p.next;
        return (p == q) ? first() : q;
    }

    /**
     * 返回 p 的前驱节点，如果 p 是前驱自链接节点，返回最后一个节点。
     */
    final Node<E> pred(Node<E> p) {
        Node<E> q = p.prev;
        return (p == q) ? last() : q;
    }
    
    /**
     * 返回第一个节点，第一个节点 p 满足：p.prev == null && p.next != p
     * 返回的节点在逻辑上可能已经被删除。确保 head 指向了返回的节点。
     */
    Node<E> first() {
        restartFromHead:
        for (;;)
            // 从 head 开始往前查找
            for (Node<E> h = head, p = h, q;;) {
                // p 的前一个节点和前前节点都不为 null
                // 如果 head 改变，从新的 head 开始循环，否则往前移两步即可
                if ((q = p.prev) != null &&
                        (q = (p = q).prev) != null)
                    p = (h != (h = head)) ? h : q;
                // p 的前一个节点为 null 或者 p 的前前节点为 null
                // p 为 head 返回 head
                // p 不为 head，将 head 设置为 p
                // 然后返回 p
                else if (p == h
                        || casHead(h, p))
                    return p;
                // head 改变，重新从 head 开始
                else
                    continue restartFromHead;
            }
    }

    /**
     * 返回最后一个节点，最后一个节点 p 满足：p.next == null && p.prev != p
     * 返回的节点在逻辑上可能已经被删除。确保 tail 指向了返回的节点。
     */
    Node<E> last() {
        restartFromTail:
        for (;;)
            for (Node<E> t = tail, p = t, q;;) {
                if ((q = p.next) != null &&
                        (q = (p = q).next) != null)
                    p = (t != (t = tail)) ? t : q;
                else if (p == t
                        || casTail(t, p))
                    return p;
                else
                    continue restartFromTail;
            }
    }
```

### 参考

* [JUC源码分析-集合篇（五）：ConcurrentLinkedDeque](https://www.jianshu.com/p/602b3240afaf)
