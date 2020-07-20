学习本篇，你需要先学习文章
[JUC锁 Locks核心类，AQS原理，通过源码进行学习深入了解1](https://blog.csdn.net/qq_30921153/article/details/107453773)
# 概述
前文介绍了AQS的基本原理，以及不同模式的acquire 如何在阻塞时加入到同步队列，以及维护同步队列的过程。而AQS的独占模式中，还实现了条件等待，即condition，await，signal，signalAll等重要的操作。
condition等待队列如下：
![等待队列](https://img-blog.csdnimg.cn/20200720150053330.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
接下来要分析的就是AQS的内部类ConditionObject，通过源码分析它是如何实现等待和唤醒操作的。
# ConditionObject内部类
根据上面提供的等待队列的图示，可以知道，和同步队列不同，等待队列是单向队列，它使用的是节点中的nextWaiter这个变量存储下一个等待队列中的节点。而ConditionObject类内部，定义了fistWaiter和lastWaiter用于指向等待队列的第一个节点和最后一个节点。
```java
        /** First node of condition queue. */
        /** Condition 队列的第一个节点 */
        private transient Node firstWaiter;
        /** Last node of condition queue. */
        /** Condition 队列的最后一个节点 */
        private transient Node lastWaiter;
```
***
## Condition的内部支撑方法
### isOnSyncQueue
```java
/**
     * Returns true if a node, always one that was initially placed on
     * a condition queue, is now waiting to reacquire on sync queue.
     *  如果一个节点现在正在同步队列上等待重新 acquire，则返回 true。
     * @param node the node
     * @return true if is reacquiring
     */
    final boolean isOnSyncQueue(Node node) {
        //如果waitStatus为-2，说明它在condition队列中，
        //node.prev说明为头节点，但是头节点是当前获得资源的线程节点，它不算在同步队列中
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false;
        //如果它有后继，那么肯定在队列中，
        //如果为null，肯定不会有后继，因为，取消节点的acquire，节点next赋值也是指向自身而已。
        if (node.next != null) // If has successor, it must be on queue
            return true;
        /*
         * node.prev can be non-null, but not yet on queue because
         * the CAS to place it on queue can fail. So we have to
         * traverse from tail to make sure it actually made it.  It
         * will always be near the tail in calls to this method, and
         * unless the CAS failed (which is unlikely), it will be
         * there, so we hardly ever traverse much.
         */
        /**
         * node.prev 可以是非空的，但还不能确定在队列上，因为将它放在队列上
         * 的 设置为尾节点的CAS操作可能会失败。所以我们必须从 tail 开始遍历确保它确实成功了。
         * 在对这个方法的调用中，它总是在尾部附近，除非 CAS 失败（这是不太
         * 可能的），否则它将永远在那里，因此我们几乎不会遍历太多。
         */
        return findNodeFromTail(node);
    }

    /**
     * Returns true if node is on sync queue by searching backwards from tail.
     * Called only when needed by isOnSyncQueue.
     * 从 tail 开始向前遍历，如果 node 在同步队列上返回 true。
     *   只在 isOnSyncQueue 方法中才会调用此方法。
     * @return true if present
     */
    private boolean findNodeFromTail(Node node) {
        Node t = tail;
        for (;;) {
            if (t == node)    //找到这个节点直接返回true。
                return true;
            if (t == null)   //到头节点还没找到
                return false;    //返回false。
            t = t.prev;
        }
    }
```
***
### transferForSignal
```java
   /**
     * Transfers a node from a condition queue onto sync queue.
     * Returns true if successful.
     * 把 condition 队列中的节点移动到 sync 队列中。
     *  如果成功返回 true。
     * @param node the node
     * @return true if successfully transferred (else the node was
     * cancelled before signal)
     */
    final boolean transferForSignal(Node node) {
        /*
         * If cannot change waitStatus, the node has been cancelled.
         * 如果不能改变状态，说明节点已经被取消了。
         */
        //只有原来的值是, Node.CONDITION才可以改为0，
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;  //如果修改失败，说明节点被取消了，即返回false

        /*
         * Splice onto queue and try to set waitStatus of predecessor to
         * indicate that thread is (probably) waiting. If cancelled or
         * attempt to set waitStatus fails, wake up to resync (in which
         * case the waitStatus can be transiently and harmlessly wrong).
         * 移动到 sync 队列中，并设置前驱节点的状态来表明线程正在等待。
         * 如果取消或者尝试设置状态失败，则唤醒并重新同步（在这种情况下，
         * 等待状态可能暂时错误，但不会造成任何伤害）。
         */
        Node p = enq(node);   //将当前在condition队列中的节点加入到同步队列的尾部，并返回先前的tail
        int ws = p.waitStatus;       //得到当前节点前驱节点的等待状态
        //如果该节点被取消，或者该节点设置为signal失败
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            //则重新唤醒，让其尝试获取资源，重新进行同步状态维护，
            //如果其获取失败，它会在自旋方法中自己维护自己的前驱节点状态。并重新unpark等待acquire。
            LockSupport.unpark(node.thread);
        return true;
    }

```
***
### transferAfterCancelledWait
```java
  /**
     * Transfers node, if necessary, to sync queue after a cancelled wait.
     * Returns true if thread was cancelled before being signalled.
     *如有必要，在取消等待后将节点传输到同步队列。
     * 如果线程在发出信号之前被取消，则返回true。
     * @param node the node
     * @return true if cancelled before the node was signalled
     */
    final boolean transferAfterCancelledWait(Node node) {
        //如果修改成功，说明节点的等待状态还是-2，即还在等待队列中，
        //还没有被转移到同步队列中，则enq，让它加入到同步队列。
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            enq(node);  //将线程节点加入到同步队列中。
            return true;
        }
        /*
         * If we lost out to a signal(), then we can't proceed
         * until it finishes its enq().  Cancelling during an
         * incomplete transfer is both rare and transient, so just
         * spin.
         * 如果我们丢失了一个signal，则我们不能继续，
         * 直到它完成它的enq()。在不完全转移期间取消是罕见的和短暂的，所以只旋转。
         */
        //如果节点已经不在等待队列中了，
        //但是判断也不在同步队列中，说明发生不完全转移期间取消，则自旋
        while (!isOnSyncQueue(node))
            // 从运行状态转换到就绪状态（让出时间片）
            Thread.yield();
        return false;
    }
```
***
### fullyRelease
```java
/**
     * Invokes release with current state value; returns saved state.
     * Cancels node and throws exception on failure.
     *  使用当前状态值调用 release；返回保存的状态。
     *   失败则取消节点并抛出异常。
     * @param node the condition node for this wait
     * @return previous sync state
     */
    final int fullyRelease(Node node) {
        boolean failed = true;
        try {
            int savedState = getState();  //得到还没释放时的同步状态
            if (release(savedState)) {   //释放所有资源，如果成功
                failed = false;
                return savedState;  //返回未释放前的状态值
            } else {       //失败抛出异常
                throw new IllegalMonitorStateException();
            }
        } finally {
            if (failed)            //释放失败，
                node.waitStatus = Node.CANCELLED;     //就取消节点
        }
    }
```
***
## ConditionObject内部方法
### addConditionWaiter
```java
 /**
         * Adds a new waiter to wait queue.
         * @return its new wait node
         * 添加新的等待线程到等待队列中。
         */
        private Node addConditionWaiter() {
            Node t = lastWaiter;    //记录tail
            // If lastWaiter is cancelled, clean out.
            //如果lastWaiter的状态不是-2，说明被取消
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();   //维护队列的状态，该方法会删除等待队列所有的取消节点
                t = lastWaiter;       // t再更新为未取消的lastWaiter
            }
            //建立一个当前线程的等待状态为CONDITION的新节点。
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            if (t == null)    //如果此时队列为空
                firstWaiter = node;    //那么维护队列firstWaiter
            else     //如果不为空
                t.nextWaiter = node;     // 维护单向链表的连接操作
            lastWaiter = node;        //更新队列的尾节点lastWaiter
            return node;
        }
```
***
### unlinkCancelledWaiters
```java
  /**
         *  从 condition 队列中删除已取消（状态不是 CONDITION 即为已取消）
         *   的等待节点。
         *   只有在持有锁的时候才调用。在 condition 队列中等待时如果发生节点
         *   取消，且看到 lastWaiter 被取消然后插入新节点时调用。
         *  （addConditionWaiter 函数中调用）。需要使用此方法在没有 signal 的
         * 时候避免保留垃圾。因此即使它需要完整的遍历，也只有在没有信号的
         * 情况下发生超时或者取消时，它才会起作用。它将会遍历所有节点，而不是
         * 停在一个特定的目标上来取消垃圾节点的连接，且不需要在取消频繁发生时
        * 进行多次重复遍历。
         *
         */
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;        //从firstWaiter开始
            Node trail = null;
            while (t != null) {       //未到末尾，就继续遍历
                Node next = t.nextWaiter;    //next存储下一个等待的节点，
                if (t.waitStatus != Node.CONDITION) {  //如果当前节点已经被取消
                    t.nextWaiter = null;       //就将当前节点的指针赋值为null，方便GC
                    if (trail == null)      //如果这时候trail还没赋值。说明还未碰到正常节点，
                        firstWaiter = next;     //那么firstWaiter就更新为现在取消节点的下一个节点，
                    else   //如果trail已经赋值了，说明前面碰到了正常节点，说明firstWaiter已经维护完成
                        trail.nextWaiter = next;    //那么就将trail保存的前一个正常节点的下一个指针赋值给next，
                    if (next == null)       //如果发现，下一个指针是null，说明最后一个节点也是取消状态，所以要更新lastWaiter
                        lastWaiter = trail;
                }
                else  //碰到了正常节点，
                    trail = t;   //如果遇到未取消的正常节点，trail就赋值为当前节点。
                t = next;       //指针遍历
            }
        }
```
***
### signal 和 doSignal

```java
 /**
         * Moves the longest-waiting thread, if one exists, from the
         * wait queue for this condition to the wait queue for the
         * owning lock.
         *将最长等待的线程，（第一个节点）如果存在的话，从 condition 等待
         * 队列移动到拥有锁的等待队列。
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signal() {
            if (!isHeldExclusively()) //判断当前线程是否独占资源
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignal(first);     //唤醒等待队列第一个线程，让它转移到同步队列中
        }
   /**
         * Removes and transfers nodes until hit non-cancelled one or
         * null. Split out from signal in part to encourage compilers
         * to inline the case of no waiters.
         * 删除和转变节点，直到命中一个并未取消或者为 null 的节点。
         * @param first (non-null) the first node on condition queue
         */
        private void doSignal(Node first) {
            do {
                if ( (firstWaiter = first.nextWaiter) == null) //如果下一个节点是null，
                    lastWaiter = null;   //说明队列为空了，即维护lastWaiter。
                first.nextWaiter = null;     //说明通过signal加入到同步队列中的节点，nextWaiter都是null
            } while (!transferForSignal(first) &&  //如果转移节点到同步队列成功就结束循环，如果失败了
                    (first = firstWaiter) != null);       //则更新first为下一节点，如果下一个节点为null，则结束循环
        }
```
***
### signalAll 和 doSignalAll
```java
 /**
         * Moves all threads from the wait queue for this condition to
         * the wait queue for the owning lock.
         *将 condition 中等待的所有线程移动到拥有锁的等待队列。
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signalAll() {
            if (!isHeldExclusively())     //判断当前线程是否独占资源， 独占同步状态
                throw new IllegalMonitorStateException();   //非独占，抛出监视状态异常
            Node first = firstWaiter;   //从第一个节点开始
            if (first != null)
                doSignalAll(first);    //将所有的节点转移到同步对列中
        }
          /**
         * Removes and transfers all nodes.
         * 移除和转移全部节点。
         * @param first (non-null) the first node on condition queue
         */
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;  //队列状态更新为空，维护首尾指针
            do {
                Node next = first.nextWaiter;   //保存first下一个节点
                first.nextWaiter = null;       //说明通过signalAll加入到同步队列中的节点，nextWaiter都是null
                transferForSignal(first);    //转移first
                first = next;      //first更新为它的下一个节点
            } while (first != null);  //一直遍历到队列末尾，停止
        }
```
***
## 不同方式的await方法
方法 | 作用
---- | -----
awaitUninterruptibly| 无法中断，即忽略中断的wait方法
await()|实现可中断 condition 的 wait 方法
awaitNanos|实现 condition 里有时间限制的 wait 方法，限时内可中断
awaitUntil| 实现指定截止时间的wait方法，等待时间可中断
await(long time, TimeUnit unit)| 实现指定时间数和时间单位的wait方法，等待时间可中断
这些方法都会调用addConditionWaiter方法创建一个新的node加入到当前condition的等待队列中，
```java
private Node addConditionWaiter() {
            Node t = lastWaiter;    //记录tail
            // If lastWaiter is cancelled, clean out.
            //如果lastWaiter的状态不是-2，说明被取消
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();   //维护队列的状态，该方法会删除等待队列所有的取消节点
                t = lastWaiter;       // t再更新为未取消的lastWaiter
            }
            //建立一个当前线程的等待状态为CONDITION的新节点。
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            if (t == null)    //如果此时队列为空
                firstWaiter = node;    //那么维护队列firstWaiter
            else     //如果不为空
                t.nextWaiter = node;     // 维护单向链表的连接操作
            lastWaiter = node;        //更新队列的尾节点lastWaiter
            return node;
        }
```
这个方法首先检查尾节点是否被中断或限时取消，如果被取消，则调用unlinkCancelledWaiters删除掉等待队列中所有已经被取消的节点，再将维护后的等待队列lastWaiter赋值给t。
然后会新建一个node：
```java
 Node node = new Node(Thread.currentThread(), Node.CONDITION);
 ```
 指定了node中thread为当前线程，waitStatus为Node.CONDITION，说明此节点在等待队列上。
然后await会调用fullyRelease方法，释放当前的同步状态，并且保存释放前的同步状态(用于恢复后使用该值在同步队列中争取资源)。释放同步状态，就会调用release-tryRelease-unparkSuccessor，最终会唤醒同步队列head之后的节点。
 注意：
 >注意await的调用一定是在lock和unlock之间(因为其调用了fullyRelease方法)，则一个线程在获得资源的情况下，该线程要么没有进同步队列，要么就是已经在同步队列的head位置，其代表线程的node已经没有意义了，因为head中thread已经被赋值为null，所以，线程调用到await时，同步队列已经不包含代表该线程的node,因此需要新建一个等待状态的节点放入等待队列的尾部，然后经过 fullyRelease-release-tryRelease-unparkSuccessor，该线程会唤醒同步队列中在head之后的节点。
***
具体源码分析：
### awaitUninterruptibly
不可中断的await方法
```java
 /**
         * Implements uninterruptible condition wait.
         * 实现不中断的 condition 队列上等待。
         * <ol>
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * </ol>
         */
        public final void awaitUninterruptibly() {//无法中断的await
            Node node = addConditionWaiter();    //创建新节点加入到等待队列末尾，并得到这个新节点
            //将资源释放，释放失败会抛出异常，
            //所以await方法不在lock与unlock间使用，会抛出异常
            //释放资源成功，返回未释放时候的state。
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {    //如果当前线程节点不在同步队列中。就继续循环
                //将当前线程park，而当其他线程使用了同一个condition调用了signal或者signalAll方法
                //该线程将被转移到同步队列中去。
                //而在transferForSignal,有可能因为前驱节点取消状态后前驱节点设置signal失败，而unpark，
                //这种情况下，会在下面acquireQueued中重新进入不可中断的独占模式acquire。
                //另一种是设置成功，就不会unpark，等到释放资源，调用unparkSuccessor时唤醒，从当前位置开始继续执行
                //这种情况，同样会进入acquireQueued中。获取资源。
                //所以总结来说，await的线程，接收到signal和signalAll方法后只是到同步队列中。需要获取资源才可继续进行
                LockSupport.park(this);
                //因为park的线程运到线程中断，也会唤醒，所以需要判断
                //但是如果该线程没有被signal唤醒因为中断而继续，也无法继续运行，因为它会重新进入while循环，继续park掉。
                if (Thread.interrupted())// 如果线程中断，用标志位 interrupted 记录
                    interrupted = true;  //如果是因为中断而继续运行，就中断标志位记录为true。
            }
            //进入的是不可中断的获取资源方法acquireQueued，即只有获得到资源才会退出自旋方法。
            //如果acquireQueued过程中断过，它返回true，或者该线程自己在等待signal时也被中断过
            if (acquireQueued(node, savedState) || interrupted)   //就补充一次自我中断，
                selfInterrupt();
        }
```
park在while循环中，退出的条件只有isOnSyncQueue返回true，即只有接收到signal或者signalAll信号，调用transferForSignal方法将节点转移到同步队列中，才会退出while循环，结束await。
注意结束await后，还是需要调用acquireQueued在队列中争抢资源，争取失败，就会unpark在同步队列中，直到获取到资源线程继续执行await下方的代码。
acquireQueued方法如下：
```java
/**
     * Acquires in exclusive uninterruptible mode for thread already in
     * queue. Used by condition wait methods as well as acquire.
     *同步队列中的线程自旋时，以独占且不可中断的方式 acquire。
     * 用于 condition 等待方式中的 acquire。
     * @param node the node
     * @param arg the acquire argument  获取的参数
     * @return {@code true} if interrupted while waiting  //如果在等待期间中断
     */
    //注意就算中断，也会继续自旋，知道获得资源为止，才会退出自旋，
    //且如果这个过程中存在因为线程中断而退出park的情况，都要返回true。
final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true; // 标记是否成功拿到资源
        try {
            boolean interrupted = false;       //标记等待过程中是否被中断过
            for (;;) {    //线程自旋
                final Node p = node.predecessor();// 获取 node 的前一个节点
                if (p == head && tryAcquire(arg)) {//如果前一节点是头节点，就尝试获取资源，
                    //获取成功继续执行
                    setHead(node);    //将传入的node设为新的头节点。
                    p.next = null; // help GC
                    failed = false;    //成功拿到锁，即改变failed标志位，
                    return interrupted;       //返回中断状态，用于辨别是正常unpark还是中断退出的。
                }
                //如果上面没有返回。说明获取失败或者前驱节点不是头节点，就需要维护节点的状态
                if (shouldParkAfterFailedAcquire(p, node) && //将节点前驱节点设置为signal。
                        parkAndCheckInterrupt())  //park线程，并且如果线程是因为中断结束park。，
                    interrupted = true;    //则，中断标志设置为true。
            }
        } finally {  //如果最后
            if (failed)       //也没有拿到资源。
                cancelAcquire(node);     //则取消正在进行的acquire。
        }
    }
```
该方法也是忽略中断的，直到争取到资源才会退出同步队列继续执行代码。
过程中如果发生中断，方法返回true，未发生则返回false。
因为该方法和awaitUninterruptibly都忽略中断，但是都有记录了是否中断过的标志。
所以awaitUninterruptibly方法要执行以下代码，补充一次自我中断，因为Thread.interrupted()会清除线程的中断状态。
```java
//进入的是不可中断的获取资源方法acquireQueued，即只有获得到资源才会退出自旋方法。
            //如果acquireQueued过程中断过，它返回true，或者该线程自己在等待signal时也被中断过
            if (acquireQueued(node, savedState) || interrupted)   //就补充一次自我中断，
                selfInterrupt();
```
***
### await
await以及其他几种设置时间的await方法他们都是等待过程可中断的，与不可中断的awaitUninterruptibly方法的区别主要是在while循环中，它可以通过线程中断的方法退出等待，并不一定要signal或signalAll，这就导致我们需要记录中断是发生在signal之前还是之后，即该节点在等待signal到来之前就被中断了，我们就需要抛出异常，因为await方法是可中断，要提示用户被中断过。如果是已经接收过signal后中断的，就和awaitUninterruptibly一样需要不从一次自我中断。
condition中主要通过以下方法：判断和处理signal之前还是之后被中断的
```java
/*
         * For interruptible waits, we need to track whether to throw
         * InterruptedException, if interrupted while blocked on
         * condition, versus reinterrupt current thread, if
         * interrupted while blocked waiting to re-acquire.
         * 对于可中断的等待，我们需要跟踪如果阻塞在 condition 中的时候，
         * 是否抛出 InterruptedException，如果在阻塞中中断等待重新获取时，
         * 是否重新中断当前线程。
         */

        /** Mode meaning to reinterrupt on exit from wait */
        /** 退出等待状态时重新中断 */
        private static final int REINTERRUPT =  1;
        /** Mode meaning to throw InterruptedException on exit from wait */
        /** 退出等待状态时抛出 InterruptException */
        private static final int THROW_IE    = -1;

        /**
         * Checks for interrupt, returning THROW_IE if interrupted
         * before signalled, REINTERRUPT if after signalled, or
         * 0 if not interrupted.
         *  检查中断，如果中断发生在 signal 之前则返回 THROW_IE，如果在 signal
         *   之后则返回 REINTERRUPT，如果没有发生返回 0.
         */
        //判断中断发生在什么期间
        //如果未发生，返回0，
        //如果发生了，调用transferAfterCancelledWait(node)方法，
        //如果该方法，成功将当前线程的node转移到同步队列，返回了true，则返回THROW_IE，说明退出了等待状态要抛出异常
        //而如果该方法，发现node已经不在等待队列了，返回了false，则返回REINTERRUPT
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                    (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                    0;
        }

        /**
         * Throws InterruptedException, reinterrupts current thread, or
         * does nothing, depending on mode.
         * 抛出 InterruptedException，再次中断当前线程，或者不做任何操作，
         * 取决于具体的模式。
         */
        private void reportInterruptAfterWait(int interruptMode)
                throws InterruptedException {
            if (interruptMode == THROW_IE)  //如果是等待过程，signal之前中断的。
                throw new InterruptedException();   //则抛出中断异常
            else if (interruptMode == REINTERRUPT)     //如果是signal之后中断的，需要补充一次中断。
                selfInterrupt();
        }
```
***
await 方法的源码如下：
```java
   /**
         * Implements interruptible condition wait.
         * 实现可中断 condition 的 wait 方法。
         *
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled or interrupted.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted())   //可中断的方法，都要实现判断当前线程判断标志
                throw new InterruptedException();
            Node node = addConditionWaiter();       //创建当前线程节点加入到等待队列
            int savedState = fullyRelease(node);   //释放当前线程的资源，并且保存未释放前的同步状态
            int interruptMode = 0;
            // 如果节点不在 sync 中一直循环（阻塞）
            // 同时检查是否发生中断，如果发生则中止循环
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                 //和不可中断的await不同，这里要判断是在等待signal时候中断，还是已经在等待队列，发生中断。
                //如果在signal之前就中断，interruptMode记为-1，并且当前节点被移到同步队列
                //如果在signal之后才中断，interruptMode记为1.
                //而如果没有发生中断interruptMode记为0；
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;     //如果返回的是不为0，说明发生中断，因为await可中断，所以要退出循环。
            }
            //走到这一步，说明线程节点已经移动到了同步队列中
            //使用释放前的状态值去请求资源，如获取失败，则加入到同步队列中等待。
            //调用的是不可中断的acquire，所以如果返回true，说明获取过程中曾被中断唤醒过，
            //而且之前等待signal时，并没有被中断，则将interruptMode赋值为REINTERRUPT(1)。
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            //到这一步，说明已经node已经获取到资源了，这时发现node.nextWaiter不为null，
            //说明该节点在没有接收到signal或signalAll情况下，通过中断加入到了同步对列。
            if (node.nextWaiter != null) // clean up if cancelled
                unlinkCancelledWaiters();     //所以需要清理掉这样取消了的节点。
            if (interruptMode != 0)      //如果非正常退出await，或者非正常获取到acquire。都要进行维护中断状态。
                reportInterruptAfterWait(interruptMode);//signal之前中断抛出中断异常，否则其他情况补充一次线程中断
        }
```
注意到while循环中，如果是因为中断而唤醒park，则调用checkInterruptWhileWaiting检查中断什么时候发生。并break打断循环，说明中断可以退出await等待。
这里要注意的是：
>不管是等待过程可中断，还是不可中断，它们在退出while循环以后，都是已经加入到同步队列中去的，signal之后的调用transferForSignal加入，signal之前因为中断或限时取消等待的是调用transferAfterCancelledWait。它们都需要调用acquireQueued去重新争抢资源。不同的是：
>* 调用了transferAfterCancelledWait方法进行转移的，它们没有经过doSignal或者doSignalAll，即它们还在同步队列中，nextWaiter还是非空的，但是它们节点的stateWaiter已经被修改成0，所以这时要调用unlinkCancelledWaiters，清理等待队列中被取消的节点。
>* 可中断的await，如果是等待signal之前被中断，它需要抛出异常。
***
### awaitNanos、awaitUntil、 await(long time, TimeUnit unit)
这三个方法本质上和await是大同小异的，只是指定了时间。
它们是限时等待，等待期间可以被中断，signal或signalAll唤醒。
不管以何种方法退出（到时、中断、唤醒），它们都会加入到同步队列中，并且只有争取到资源才会继续执行。
```java
  /**
         * Implements timed condition wait.
         * <ol>实现 condition 里有时间限制的 wait 方法。
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted())   //等待过程也是可中断的，所以需要提前判断一次
                throw new InterruptedException();
            Node node = addConditionWaiter();      //新建节点加入等待队列
            int savedState = fullyRelease(node);     //释放同步状态，并保存释放前状态
            final long deadline = System.nanoTime() + nanosTimeout; //得到截止时间
            int interruptMode = 0;   //和await方法中类似效果。
            while (!isOnSyncQueue(node)) {     //因为可中断，所以循环可以break。
                if (nanosTimeout <= 0L) {     //如果指定等待时间为负数，在等待队列中取消节点。
                    transferAfterCancelledWait(node); //将节点转移到同步队列。
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)   //如果还未到时限
                    LockSupport.parkNanos(this, nanosTimeout);  //park线程nanosTimeout
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)//如果等待过程中断，就退出。
                    break;
                nanosTimeout = deadline - System.nanoTime();   //剩余时间。
            }
            //不管是中断，还是限时到了，还是signal，都会进同步队列，
            //直到获取到资源才线程才会继续
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)//限时结束，或者中断而加入同步队列的都需要清楚取消的节点
                unlinkCancelledWaiters();
            if (interruptMode != 0) //如果是signal之前被中断抛出异常，之后则补充一次线程中断
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }
   /**
         * Implements absolute timed condition wait.
         * 实现指定截止时间的 condition 中 wait 方法。
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        //和上面方法基本类似
        public final boolean awaitUntil(Date deadline)
                throws InterruptedException {
            long abstime = deadline.getTime();//转化为毫秒
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {//如果截止时间小于当前系统时间
                    timedout = transferAfterCancelledWait(node);  //直接取消节点，并且将节点放入同步队列尾部，转移成功返回true， 表示超时
                    break;
                }
                LockSupport.parkUntil(this, abstime);  //如果没有则调用parkUntil参数为指定的时间
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)  //等待过程可能被中断，检查中断并处理
                    break;
            }
            //不管是时间到了，还是中断了，还是指定时间超时，或者正常接收到signal，
            //都会进入同步队列获取资源。
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)  //如果是取消状态的节点，即超时取消，或中断取消。
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);  //signal前中断会抛出异常。之后会补充中断
            return !timedout;
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        //该方法和awaitNanos(long)基本相同 ，只是参数时间不同，所以将指定时间转化为纳秒即可。
        public final boolean await(long time, TimeUnit unit)
                throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }
```
***
# 队列检查和监控方法
## condition队列检查和监控方法
```java
 // Instrumentation methods for conditions
    // 监测 Condition 队列的方法
    /**
     * Queries whether the given ConditionObject
     * uses this synchronizer as its lock.
     * 检查给定 ConditionObject 是否使用此同步器作为其 lock。
     * @param condition the condition
     * @return {@code true} if owned
     * @throws NullPointerException if the condition is null
     */
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * Queries whether any threads are waiting on the given condition
     * associated with this synchronizer. Note that because timeouts
     * and interrupts may occur at any time, a {@code true} return
     * does not guarantee that a future {@code signal} will awaken
     * any threads.  This method is designed primarily for use in
     * monitoring of the system state.
     * 检查是否有线程等待在和此同步器关联的给定 condition 上。注意由于时间
     * 到期和线程中断会在任何时候发生，返回 true 并不保证未来的信号会唤醒
     * 任何线程。此方法用于监控系统状态。
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    //判断在指定condition的队列中是否有等待的线程
    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.hasWaiters();
    }

    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with this synchronizer. Note that
     * because timeouts and interrupts may occur at any time, the
     * estimate serves only as an upper bound on the actual number of
     * waiters.  This method is designed for use in monitoring of the
     * system state, not for synchronization control.
     * 返回与此同步器关联的 condition 上等待的线程数。注意由于时间到期和
     * 线程中断会在任何时候发生，因此估计值仅仅作为实际等待着数量的上限。
     *  此方法用于监视系统状态，而不是作为同步器控制。
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitQueueLength();
    }

    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with this
     * synchronizer.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate. The elements of the
     * returned collection are in no particular order.
     *返回一个包含与此同步器关联的 condition 上所有线程的集合。由于构建此
     *集合的时候，实际线程集合可能会动态变化，返回的集合只是一个最佳的
     *  估计。返回集合的元素没有特定顺序。
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitingThreads();
    }
  /**
         * Returns true if this condition was created by the given
         * synchronization object.
         *如果这个condition是被指定的同步器创建的，则返回true。
         * @return {@code true} if owned
         */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this; //和condition外部类的this指针比较
        }

        /**
         * Queries whether any threads are waiting on this condition.
         * Implements {@link AbstractQueuedSynchronizer#hasWaiters(ConditionObject)}.
         *查询是否有线程在 condition 中等待。
         * @return {@code true} if there are any waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final boolean hasWaiters() {
            if (!isHeldExclusively())  //判断当前线程是否独占同步状态
                throw new IllegalMonitorStateException();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {//从firstWaiter开始遍历
                if (w.waitStatus == Node.CONDITION)   //只要发现状态为-2的就返回true
                    return true;
            }
            return false;   //没发现返回false，说明没有condition队列没有节点
        }

        /**
         * Returns an estimate of the number of threads waiting on
         * this condition.
         * 返回在 condition 中等待的线程数估计值。
         * Implements {@link AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)}.
         *
         * @return the estimated number of waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final int getWaitQueueLength() {
            if (!isHeldExclusively())   //判断当前线程是否独占同步状态
                throw new IllegalMonitorStateException();
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)     //遍历，发现一个等待状态为-2，就是等待队列有效节点，就记数
                    ++n;
            }
            return n;
        }

        /**
         * Returns a collection containing those threads that may be
         * waiting on this Condition.
         * 返回在 condition 上等待的所有线程的集合。
         * Implements {@link AbstractQueuedSynchronizer#getWaitingThreads(ConditionObject)}.
         *
         * @return the collection of threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively())   //判断当前线程是否独占
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<Thread>();     //返回存储线程类型的arraylist
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
```
***
## AQS类的关于队列检查和监控方法
### hasQueuedThreads 和 hasContended
```java
  // Queue inspection methods
    // 队列检查方法
    /**
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations due to interrupts and timeouts may occur
     * at any time, a {@code true} return does not guarantee that any
     * other thread will ever acquire.
     * 查询是否有线程等待 acquire。由于任何时间的中断和时限到期将导致线程
     *  取消，返回 true 并不能保证任何其他线程 acquire。
     * <p>In this implementation, this operation returns in
     * constant time.
     *此实现在常数时间内完成。
     * @return {@code true} if there may be other threads waiting to acquire
     */
    //即检查头尾节点是否相等，如果相等说明队列为空。返回false
    //如果不相等，说明同步队列中有线程在等待，返回true
    //因为线程可中断和限时acquire的原因，这个true只能保证那一刻的队列状态
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     * Queries whether any threads have ever contended to acquire this
     * synchronizer; that is if an acquire method has ever blocked.
     * 查询是否有任何线程竞争过此同步器；即是否有 acquire 方法阻塞了。
     * <p>In this implementation, this operation returns in
     * constant time.
     *此实现在常数时间内完成。
     * contend：竞争
     * @return {@code true} if there has ever been contention
     */
    public final boolean hasContended() {
        return head != null;   //head不为null，说明曾经有线程竞争过，即队列已经初始化过
    }
```
***
### getFirstQueuedThread 
```java
    /**
     * Returns the first (longest-waiting) thread in the queue, or
     * {@code null} if no threads are currently queued.
     *返回队列中第一个阻塞的线程（等待时间最久的线程），如果队列中没有线程
     *  返回 null。
     * <p>In this implementation, this operation normally returns in
     * constant time, but may iterate upon contention if other threads are
     * concurrently modifying the queue.
     *此实现通常在常数时间内返回，但是如果其他线程同时修改队列，会在竞争
     *  的基础上迭代。
     * @return the first (longest-waiting) thread in the queue, or
     *         {@code null} if no threads are currently queued
     */
    //得到队列中第一个阻塞的线程
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * Version of getFirstQueuedThread called when fastpath fails
     * fastpath失败时调用的getFirstQueuedThread版本
     */
    private Thread fullGetFirstQueuedThread() {
        /*
         *
         *  第一个节点通常是 head.next。尝试获取它的线程字段，确保一致性读取：
         * 如果线程字段为 null 或者 s.prev不再是 head，其他线程并发调用 setHead
         * 在两次读取之间。我们在遍历之前尝试两次。
         */
        Node h, s;
        Thread st;
        //遍历前判断两次，确保取到的确实是不为空的head的next指针中的thread。
        if (((h = head) != null && (s = h.next) != null &&
                s.prev == head && (st = s.thread) != null) ||
                ((h = head) != null && (s = h.next) != null &&
                        s.prev == head && (st = s.thread) != null))
            return st;

        /*
         *
         * 运行到这一步表示 head.next 还没有设置，或者在执行 setHead 之后仍然
         * 没有设置。所以我们必须检查是否 tail 是第一个节点。如果不是，从 tail
         * 节点向 head 遍历直到找到第一个有效节点。
         */

        Node t = tail;
        Thread firstThread = null;
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null)            //从tail开始，找到最后一个有效节点。即为从head后的第一个有效节点
                firstThread = tt;
            t = t.prev;
        }
        return firstThread;
    }
```
***
### isQueued(Thread thread) 和 apparentlyFirstQueuedIsExclusive
```java
    /**
     * Returns true if the given thread is currently queued.
     * 如果指定的线程正在排队即返回 true。
     * <p>This implementation traverses the queue to determine
     * presence of the given thread.
     *此实现通过遍历队列来查找指定线程。（从 tail 到 head）
     *
     * @param thread the thread
     * @return {@code true} if the given thread is on the queue
     * @throws NullPointerException if the thread is null
     */
    //查看当前线程是否在图同步队列中
    public final boolean isQueued(Thread thread) {
        if (thread == null)     //指定线程为空，则抛出异常
            throw new NullPointerException();
        for (Node p = tail; p != null; p = p.prev)   //从队列尾部开始遍历，因为头部经常会被设置。
            if (p.thread == thread)     //找到一个节点中的thread等于指定参数，就返回true
                return true;
        return false;  //没找到返回false
    }

    /**
     * Returns {@code true} if the apparent first queued thread, if one
     * exists, is waiting in exclusive mode.  If this method returns
     * {@code true}, and the current thread is attempting to acquire in
     * shared mode (that is, this method is invoked from {@link
     * #tryAcquireShared}) then it is guaranteed that the current thread
     * is not the first queued thread.  Used only as a heuristic in
     * ReentrantReadWriteLock.
     * 如果第一个显式排队的线程正在以独占模式等待，返回 true。如果此方法
     * 返回 true，且当前线程正在试图以共享模式 acquire（即在
     *  tryAcquireShared 方法中调用），那么当前线程不是队列中第一个线程。
     *  在 ReetrantReadWriteLock 用于启发（？）
     */
    //是否队列的第一个等待获取的是以独占模式在请求资源
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null &&       //头节点不为空
                (s = h.next)  != null &&    //头节点的next也不为空
                !s.isShared()         &&           //如果head.next不是共享节点，
                s.thread != null;             //且这个节点的thread是有效的。那么返回true
    }
```
***
### hasQueuedPredecessors
```java
    /**
     * Queries whether any threads have been waiting to acquire longer
     * than the current thread.
     * 查询是否有任何线程比当前线程等待执行 acquire 的时间还长。
     *
     * <p>An invocation of this method is equivalent to (but may be
     * more efficient than):
     *  <pre> {@code
     * getFirstQueuedThread() != Thread.currentThread() &&
     * hasQueuedThreads()}</pre>
     *此方法的调用等价于（但是可能更有效）：
     * getFirstQueuedThread() != Thread.currentThread() && hasQueuedThreads()
     *
     *注意，由于中断和超时导致的取消可能随时发生，返回 true 并不能保证
     * 其他线程在此线程会 acuqire。同样，在此方法返回 false 之后，由于队列
     *  为空，其他线程也可能成功入队。
     *

     *此方法用来作为一个公平的同步器，以避免碰撞。这样的一个同步器的
     * tryAcquire 方法应该返回 false，如果此方法返回 true（除非这是一个
     * 可重入的 acquire），它的 tryAcquireShared 方法应该返回一个负值。
     * 例如，一个公平的，可重入的，独占模式同步器的 tryAcquire 方法应该
     *  看起来类似于：
     *  <pre> {@code
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) {   可重入
     *     // A reentrant acquire; increment hold count
     *     return true;
     *   } else if (hasQueuedPredecessors()) {  //如果等待队列中第一个等待acquire的线程不是自己，
     *     return false;    //说明无法获取资源
     *   } else {          //如果没有正在等待获取资源的线程。
     *     // try to acquire normally   就正常acquire。
     *   }
     * }}</pre>
     *
     * @return {@code true} if there is a queued thread preceding the
     *         current thread, and {@code false} if the current thread
     *         is at the head of the queue or the queue is empty
     * @since 1.7
     */
    //
    public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s;
        return h != t && //队列不为空， 且头节点的next不是当前线程
                ((s = h.next) == null || s.thread != Thread.currentThread());
    }

```
***
### getQueueLength 和 getQueuedThreads
```java
    // Instrumentation and monitoring methods
    //仪器仪表和监控方法
    /**
     * Returns an estimate of the number of threads waiting to
     * acquire.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring system state, not for synchronization
     * control.
     * 返回等待 acquire 线程数量的估计值。这个值只是一个估计值，因为当这个
     * 方法遍历内部数据结构的时候线程数可能会动态变化。此方法用于监控系统
     *  状态，不用于同步控制。
     * @return the estimated number of threads waiting to acquire
     */
    //返回等待队列中acquire的线程数量的估计。
    public final int getQueueLength() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) { //一样从tail向前遍历
            if (p.thread != null)   //当前队列中有效的线程节点才会被记录
                ++n;
        }
        return n;       //返回记录的线程个数n
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive monitoring facilities.
     *返回一个包含正在等待 acquire 的所有线程的集合。因为在构造这个结果的
     *时候，实际的线程集合可能会动态变化，返回的集合只是一个最佳效果的
     * 估计。返回集合的元素没有特定的顺序。此方法用于构建更广泛监视工具。
     * @return the collection of threads
     */
    //同样返回的值只是估计值，因为线程集合是动态变化的。
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();  //使用arraylist记录
        for (Node p = tail; p != null; p = p.prev) {    //同样从tail开始遍历
            Thread t = p.thread;
            if (t != null)          //如果节点线程不为null
                list.add(t);      //添加当前线程到ArrayList中
        }
        return list;
    }
```
***
### getExclusiveQueuedThreads 和 getSharedQueuedThreads
```java
    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in exclusive mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to an exclusive acquire.
     * 返回一个包含以独占模式等待 acquire 的线程集合。它具有和 getQueuedThreads
     * 相同的属性，只是它只返回独占模式等待的线程。
     * @return the collection of threads
     */
    //和上面的getQueuedThreads不同，这个方法只返回以独占模式等待的线程集合
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) {     //多了一步判断。即如果不是共享模式，才进行下一步加入判断
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in shared mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to a shared acquire.
     *返回一个包含以共享模式等待 acquire 的线程集合。它具有和 getQueuedThreads
     *  相同的属性，只是它只返回独占模式等待的线程。
     * @return the collection of threads
     */
    //和上面2个方法类似，只是返回的是共享模式等待的线程集合
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) {      //只多了一个判断条件
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }
```
***
### toString
```java
    /**
     * Returns a string identifying this synchronizer, as well as its state.
     * The state, in brackets, includes the String {@code "State ="}
     * followed by the current value of {@link #getState}, and either
     * {@code "nonempty"} or {@code "empty"} depending on whether the
     * queue is empty.
     *返回能识别此同步器和其状态的字符串。
     * @return a string identifying this synchronizer, as well as its state
     */
    public String toString() {
        int s = getState();  //得到同步器状态
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() +
                "[State = " + s + ", " + q + "empty queue]";
    }

```
***
# AQS 总结
* 提供一套模板框架
AQS 中使用了模板方法设计模式，在自定义同步器的时候需要重写以下模板方法：

方法名 |描述
-------- | -----
tryAcquire	|排它获取（资源数）
tryRelease	|排它释放（资源数）
tryAcquireShared	|共享获取（资源数）
tryReleaseShared	|共享获取（资源数）
isHeldExclusively|	是否排它状态
以上方法在 AQS 类中默认抛出 UnsupportedOperationException 异常。

前面说到 AbstractQueuedSynchronizer 依赖两种队列，其中最核心的是同步队列:
* 每一个结点都是由前一个结点唤醒。
* 当结点发现前驱结点是 head 并且尝试获取成功，则会轮到该线程运行。
*  signal 操作会把Condition 队列中的结点向同步队列中转移。
* 中断或限时结束的await也会将节点连接到同步队列后。
* 当结点的状态为 SIGNAL 时，表示后面的结点需要运行。

# 参考
[AQS原理](https://www.pdai.tech/md/java/thread/java-thread-x-lock-AbstractQueuedSynchronizer.html)
[J.U.C之locks框架：AQS综述(1)](https://segmentfault.com/a/1190000015562787)




