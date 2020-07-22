# 概述
ReentrantReadWriteLock 是 ReadWriteLock 接口的具体实现。和 ReentrantLock 一样，它使用 Sync （继承自 AQS 抽象类）作为锁的同步器，支持公平同步器和非公平同步器，分别在 FairSync 和 NonfairSync 中实现。

在 AQS 同步器的基础上，此 Lock 实现了两种类型的锁，并把它们作为内部属性。这两种锁分别是读锁（共享锁） ReadLock 和 写锁（独占锁，排他锁）WriteLock。

ReentrantReadWriteLock有五个内部类，五个内部类之间也是相互关联的。内部类的关系如下图所示：
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020072219421226.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
如上图所示，Sync继承自AQS、NonfairSync继承自Sync类、FairSync继承自Sync类；ReadLock实现了Lock接口、ReadLock也实现了Lock接口。
具体他们如何联系，接下来看源码分析就知道了。
# 内部类Sync
和ReentrantLock中的Sync类一样，该类继承AbstractQueuedSynchronizer类，所以Sync是基于AQS实现的一个同步器，我们知道AQS中已经指定了一套完整的管理同步队列和等待队列的方案，以及定义了不同的获取方式，默认支持了独占模式和共享模式。而Sync要做的就是定义同步状态的含义，以及决定什么同步状态下，资源可以访问到。我们可以在分析源码过程中思考这个问题。
## Sync类成员变量及构造函数
### state的定义
在ReentrantReadWriteLock中，对同步状态有如下规定：
state原本是32位int类型整数，将其划分为高16位和低16位。
高16位代表读锁的同步器状态，记录了它一共被线程持有了多少个数，
低16位代表写锁的同步器状态，记录了它一共被线程持有了多少个数。
所以不管是读锁还是写锁，他们的最大锁数是2的16次方-1。
SHARED_UNIT表示读锁状态加1是，state需要加的一个单位数，它是2的16次方。
EXCLUSIVE_MASK 表示的是写锁的掩码，它是2的16次方-1，即它的低16位都是1。高位都是0.
```java
     /*
         * Read vs write count extraction constants and functions.
         * Lock state is logically divided into two unsigned shorts:
         * The lower one representing the exclusive (writer) lock hold count,
         * and the upper the shared (reader) hold count.
         */
        // 最多支持 65535(1<<16 - 1) 个写锁和 65535 个读锁
        // int 值的低十六位表示写锁计数，高十六位表示持有读锁的线程数
        static final int SHARED_SHIFT   = 16;
        // 增加一个线程获取读锁，则持有数加 SHARED_UNIT，因为只有高十六位
        // 才表示读锁数量
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
        // 锁的最大数量
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
        // 写锁计数掩码（低十六位的二进制全部为 1）
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

```
### sharedCount和exclusiveCount
```java
   /** Returns the number of shared holds represented in count  */
        // 返回当前持有读锁的线程数（高十六位表示读锁计数，所以无符号右移 16 位）
        static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
        /** Returns the number of exclusive holds represented in count  */
        // 返回写锁的重入次数（获取低十六位）
        static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }
```
sharedCount获取读锁的状态，>>>表示无符号右移，高位补0而不是符号位。
exclusiveCount获取写锁的状态，通过和掩码取&，即获得低16位的数。
***
### HoldCounter 和 ThreadLocalHoldCounter
HoldCounter是对线程id以及对应线程持有读锁个数的一个包装。
ThreadLocalHoldCounter继承ThreadLocal < HoldCounter >，并且重写了initialValue方法，使得第一次get的初始值不再是null，而是空的HoldCounter实例。
对于该类的使用可以参考上一篇文章[JUC ThreadLocal原理，通过源码进行学习深入了解](https://blog.csdn.net/qq_30921153/article/details/107498323)
```java
        /**
         * 持有读锁的线程计数器。记录单个线程持有的读锁数量。
         */
        //线程持有几个读锁的计时器类，
            //2个成员函数，int类型的读锁个数，long类型的线程ID。
        static final class HoldCounter {
            int count = 0;
            // Use id, not reference, to avoid garbage retention
            // 当前线程的 id
            final long tid = getThreadId(Thread.currentThread());
        }

        /**
         * ThreadLocal 的子类。
         */
        //继承ThreadLocal的子类，
        static final class ThreadLocalHoldCounter
                extends ThreadLocal<HoldCounter> {
            //重写了initialValue方法，则初始化thread中的Map中的key时，不为null，而是HoldCounter的默认实例
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }
```
### 读锁中使用的变量
这些变量主要是关于线程与线程持有读锁数的一个关系记录。
readHolds继承ThreadLocals，所以功能主要是让线程持有自己的一份HoldCounter 变量。
cachedHoldCounter是为了节省 readHolds的查找。
firstReader 和firstReaderHoldCount是为了实现写锁降级读锁时会用到的变量。
构造函数Sync()中对readHolds进行了初始化赋值。并使得它在所有线程中可见。
```java
        /**
         * 当前线程持有的可重入读锁的数量。
         * 仅在构造函数和 readObject 中初始化。
         * 当读线程的持有计数下降到 0 时删除。
         */
        private transient ThreadLocalHoldCounter readHolds;

        /**
         * 成功获取 readLock 的最后一个线程的持有计数。此变量在通常情况下
         * 节省了 ThreadLocal 的查找，因为下一个要 release 的线程是最后一个
         * acquire 的。这是 non-volatile 的，只作为一种启发使用，而且对于线程
         * 缓存来说非常好。
         *
         * 可以比存储读持有计数的线程活的更久，但是通过不保留线程的引用来
         * 避免垃圾保留。
         *
         * 通过良性呃数据竞争访问；依赖于内存模型的 final 字段和非空保证。
         */
        private transient HoldCounter cachedHoldCounter;

        /**
         * firstReader 是获得读锁的第一个线程。
         * firstReaderHoldCount 是 firstReader 的持有计数。
         *
         * 更准确地说，firstReader 是最后一次将共享计数从 0 更改为 1 的唯一
         * 线程，并且从那时起就没有释放读锁；如果没有这样的线程则为 null。
         */
        private transient Thread firstReader = null;
        private transient int firstReaderHoldCount;

        // 构造函数
        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            setState(getState()); // 确保readHolds的可见性。ensures visibility of readHolds
        }

```
## Sync工具方法
### readerShouldBlock和writerShouldBlock
```java
  /**
         * 如果当前线程在尝试获取读锁时应该阻塞，则返回 true。
         */
        abstract boolean readerShouldBlock();

        /**
         * 如果当前线程在尝试获取读锁时应该阻塞，则返回 true。
         */
        abstract boolean writerShouldBlock();
```
为了能明白公平锁和非公平锁在tryAcquire 和tryAcquireShared中的差别，这里先将子类NonfairSync和FairSync中的实现进行分析：
#### NonfairSync
```java
 /**
     * 同步器的非公平版本
     * 实现了 writerShouldBlock 和 readerShouldBlock
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -8159625535654395037L;
        final boolean writerShouldBlock() {
            return false; // writers can always barge
        }
        final boolean readerShouldBlock() {
            /* As a heuristic to avoid indefinite writer starvation,
             * block if the thread that momentarily appears to be head
             * of queue, if one exists, is a waiting writer.  This is
             * only a probabilistic effect since a new reader will not
             * block if there is a waiting writer behind other enabled
             * readers that have not yet drained from the queue.
             */
            return apparentlyFirstQueuedIsExclusive();
        }
    }
```
* 在非公平锁中，写锁尝试获取资源时，无需观察同步队列情况，即writerShouldBlock无条件返回false。
* 而读锁，需要调用apparentlyFirstQueuedIsExclusive方法，源码如下，该方法主要是判断同步队列中head.next即第一个等待的是否是独占模式的节点，即判断同步队列中下一个等待获取资源的是写锁，就返回true，readerShouldBlock也返回true。
* 以上说明，读锁在非公平锁中争取锁时，如果下一个等待唤醒的是独占模式的写操作线程。那读锁不去争取资源，直接
```java
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
#### FairSync
```java
 /**
     * 同步器的非公平版本
     * 实现了 writerShouldBlock 和 readerShouldBlock（有后继者则必须等待）
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -2274990926593161451L;
        final boolean writerShouldBlock() {
            return hasQueuedPredecessors();
        }
        final boolean readerShouldBlock() {
            return hasQueuedPredecessors();
        }
    }
```
* 公平锁中读锁和写锁都调用hasQueuedPredecessors方法，该方法只判断同步队列是否有线程在等待获取资源，不管是独占模式还是共享模式，也就是不管是争取写锁还是读锁而阻塞的线程，只要是有，那么写操作和读操作请求都不去争取资源，直接默认争取失败。
***
## Sync核心方法
### tryAcquire 和 tryRelease
(1)tryAcquire 
```java
 // 获取独占锁，基本和 ReentrantLock 一样
        protected final boolean tryAcquire(int acquires) {
            /*
             * Walkthrough:
             * 1. If read count nonzero or write count nonzero
             *    and owner is a different thread, fail.
             * 2. If count would saturate, fail. (This can only
             *    happen if count is already nonzero.)
             * 3. Otherwise, this thread is eligible for lock if
             *    it is either a reentrant acquire or
             *    queue policy allows it. If so, update state
             *    and set owner.
             */
            Thread current = Thread.currentThread();
            int c = getState();    //得到同步器的state
            int w = exclusiveCount(c);  //得到state中记录写锁的计数值，它在state中的后低6位中

            // 写锁或读锁已经被获取,只有当前线程独占了写锁，即重入才可以。
            //@注意：如果已经先获取了写锁，不管之后该线程有没有获得读锁，再次重入写锁也是可以的。
            if (c != 0) {
                // (Note: if c != 0 and w == 0 then shared count != 0)
                // 写锁为 0 （即读锁已被获取）或者得到锁的不是当前线程，则获取失败
                // 说明：其他线程持有读锁时，当前线程写锁不能获取到资源
                //当前线程持有读锁时，如果线程未持有独占锁，即独占锁的持有者不是当前线程，也不能获取到资源。
                //即读锁不能升级成写锁。
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                // 写锁重入持有计数已达到限制，则获取失败，最大为2的16次方-1
                if (w + exclusiveCount(acquires) > MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                // Reentrant acquire
                // 重入获取成功，当且仅当，持有w!=0&&current==getExclusiveOwnerThread()
                setState(c + acquires);
                return true;
            }
            //到这一步说明state为0，则读锁写锁都没有线程拥有。
            // 当前线程应该阻塞，或者 CAS 设置状态失败，则获取锁失败
            // 1.如果是公平锁，那么writerShouldBlock只允许队列头的线程获取锁
            // 2.如果是非公平锁，不做限制，writerShouldBlock直接返回false
            if (writerShouldBlock() ||
                    !compareAndSetState(c, c + acquires))
                return false;
            // 否则获取成功,设置独占写锁。
            setExclusiveOwnerThread(current);
            return true;
        }
```

***

(2)tryRelease
```java
   /*
         * Note that tryRelease and tryAcquire can be called by
         * Conditions. So it is possible that their arguments contain
         * both read and write holds that are all released during a
         * condition wait and re-established in tryAcquire.
         */

        // 释放独占锁，基本和 ReentrantLock 一样
        protected final boolean tryRelease(int releases) {
            // 当前线程不是持有独占锁的线程，抛出异常
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            // 持有计数减少
            int nextc = getState() - releases;
            //独占锁的state状态是否为0，为0 才是true，不为0 为false
            boolean free = exclusiveCount(nextc) == 0;
            // 只有持有计数为 0 的时候才会完全释放
            if (free)
                setExclusiveOwnerThread(null); //当独占资源状态为0时，释放当前线程锁定
            setState(nextc);  //更新state。
            return free;   //成功释放返回true。失败返回false
        }
```
***
### tryAcquireShared和 tryReleaseShared
(1)tryAcquireShared
```java
 // 共享模式获取锁（获取读锁）
        protected final int tryAcquireShared(int unused) {
            /*
             * Walkthrough:
             * 1. If write lock held by another thread, fail.
             * 2. Otherwise, this thread is eligible for
             *    lock wrt state, so ask if it should block
             *    because of queue policy. If not, try
             *    to grant by CASing state and updating count.
             *    Note that step does not check for reentrant
             *    acquires, which is postponed to full version
             *    to avoid having to check hold count in
             *    the more typical non-reentrant case.
             * 3. If step 2 fails either because thread
             *    apparently not eligible or CAS fails or count
             *    saturated, chain to version with full retry loop.
             */
            Thread current = Thread.currentThread();
            int c = getState();
            // exclusiveCount(c) != 0 独占计数不等于 0，说明有线程持有写锁
            // 持有写锁的线程如果不是当前线程，则当前线程不能获取读锁。
            //返回-1。负数表示获取失败
            if (exclusiveCount(c) != 0 &&
                    getExclusiveOwnerThread() != current)
                return -1;

            //如果没有返回，说明要么写锁没有被其他线程拥有，要么写锁被当前线程拥有
            //获取写锁后，可以再获取读锁，这时锁降级的原理。
            // 获取持有读锁的同步器状态值，
            int r = sharedCount(c);//r记录进行CAS前的读锁的状态值

            // 1.readerShouldBlock(),在非公平锁中：如果exclusiveCount(c)不为0，就有可能导致同步队列中
            //存在竞争写锁而阻塞的线程，如果head.next是个独占模式节点，则发返回true。
            //在公平锁中：如果同步队列中有正在等待的线程，不管是acquire读锁还是写锁，都会返回true。
            // 2.没有达到最大数量
            // 3.满足前面条件就进行CAS 更新，将c变为c+SHARED_UNIT。
            //成功将进行firstReader、firstReaderHoldCount、cachedHoldCounter的状态更新。
            if (!readerShouldBlock() &&
                    r < MAX_COUNT &&
                    compareAndSetState(c, c + SHARED_UNIT)) {

                // 如果未CAS之前的读锁状态为0，说明当前线程是第一个获得读锁的线程
                //第一次获得读锁的线程，没有调用readHolds.get，说明线程map中没有存储该映射。
                if (r == 0) {
                    firstReader = current;// 初始化 firstReader 和 firstReaderHoldCount
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) {//读锁不是第一次被获取。
                    // 但是当前线程就是首次获取的线程，即读锁重入，
                    // 计数加 1
                    firstReaderHoldCount++;
                } else {//不是第一次获取读锁，且当前线程也不是firstReader。则写锁状态必定为0，
                    //cachedHoldCounter为记录的最后一次获得读锁的线程。
                    HoldCounter rh = cachedHoldCounter;
                    //如果最后一次获取读锁的线程为空，或者不是当前线程，则更新cachedHoldCounter。
                    if (rh == null || rh.tid != getThreadId(current))
                        //readHolds的get会以当前线程为key，到当前线程的threadLocals映射中找到记录的holdCount
                        //如果线程之前没有拥有过该读锁，那么它会新建一个ThreadLocal.ThreadLocalMap，
                        //并且以key=当前线程，value=new HoldCounter()的entry放入Map中，
                        //这个Map最后会赋值给当前线程的threadLocals。
                        cachedHoldCounter = rh = readHolds.get();
                    else if (rh.count == 0)     //如果当前线程是最后一次获得读锁的线程，但记数为0。
                        readHolds.set(rh);  //在count为1时，释放过读锁，则一定调用过readHold.remove，所以要重新set。
                    //不等于null，并且当前线程是cachedHoldCounter缓存中的最后一个线程，
                    //则直接从变量rh中拿，不从map中获取，性能快那么一点点。
                    rh.count++;
                }
                // 返回 1 表示获取成功
                return 1;
            }
            // 一次获取读锁失败后，尝试循环获取
            //失败原因可能有，CAS过程中失败，公平锁中，同步队列中有节点等待。
            //非公平锁中，同步队列中head.next是一个独占模式节点在等待
            //或者锁超过了2的16次方-1
            return fullTryAcquireShared(current);
        }
```
***
接着分析fullTryAcquireShared
```java
  /**
         * 读锁 acquire 的完整版本，包含 CAS 失败或者 tryAcquireShared 中
         * 没有获取可重入读锁的情况
         * tryAcquireShared 是一次快速获取的情况，且里面用到的 CAS 只允许一个
         * 线程获取成功，但读锁是共享的，所以需要此函数来循环获取，直到成功。
         */
        //该方法处理CAS失败，或者重入失败的情况。
        final int fullTryAcquireShared(Thread current) {
            /*
             * This code is in part redundant with that in
             * tryAcquireShared but is simpler overall by not
             * complicating tryAcquireShared with interactions between
             * retries and lazily reading hold counts.
             */
            HoldCounter rh = null;
            for (;;) {
                int c = getState();
                // 如果有线程获取到了写锁
                if (exclusiveCount(c) != 0) {
                    // 判断获取到写锁的是不是当前线程，如果不是则返回获取失败
                    //但是如果是当前线程持有写锁，那么可以尝试获取多次读锁，这个在低下会进行处理。
                    if (getExclusiveOwnerThread() != current)
                        return -1;
                }
                //没有线程获得了写锁，但是因为公平锁队列存在等待节点，
                //或者非公平锁中，下一个是独占模式节点，即争取写锁。
                else if (readerShouldBlock()) {
                    // Make sure we're not acquiring read lock reentrantly
                    // 如果当前线程是 firstReader，一定是重入，
                    //因为如果firstReaderHoldCount为0，firstReader应该为null。
                    if (firstReader == current) {
                        //重入在下面代码判断。
                        // assert firstReaderHoldCount > 0;
                    } else {    //如果不是第一个获取读锁的线程，有可能在重入，也有可能是首次获取读锁
                        if (rh == null) {
                            rh = cachedHoldCounter;
                            // 如果当前线程不是缓存的最后一个线程，从 readHolds 里读取
                            if (rh == null || rh.tid != getThreadId(current)) {
                                rh = readHolds.get();
                                if (rh.count == 0)   //如果发现当前线程是首次获取锁，那么它确实应该被阻塞。
                                    readHolds.remove();  //将get创建的映射删除掉，反正内存泄漏，
                            }
                        }
                        // 注意：
                        // 已经获取了读锁的线程重入时，不能阻塞，阻塞会导致死锁。
                        // 所以如果得到count为0，说明该线程不是重入，那么它就应该被阻塞。
                        if (rh.count == 0)
                            return -1;    //失败返回-1.
                    }
                }
                //到这一步，只可能是重入，或者锁降级的情况、或者是锁过多了。
                // 读锁的数量达到限制
                if (sharedCount(c) == MAX_COUNT)  //先排除锁过多情况
                    throw new Error("Maximum lock count exceeded");
                // 此线程不应该被阻塞，且读锁的数量没有达到限制，那么可以获取读锁
                // 获取读锁成功，下面的处理和 tryAcquireShared 类似
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (sharedCount(c) == 0) {   //再考虑锁降级的情况，
                        firstReader = current;   //持有写锁的线程一定是第一个获得读锁的线程。
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {   //如果是锁降级中的读锁重入。
                        firstReaderHoldCount++;   //那么记数自加
                    } else {    //写锁未被拥有的情况下的读锁重入。
                        if (rh == null)
                            rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                        cachedHoldCounter = rh; // 更新cachedHoldCounter
                    }
                    return 1;
                }
            }
        }
```
***
(2)tryReleaseShared
```java
 // 共享模式释放锁
        protected final boolean tryReleaseShared(int unused) {
            Thread current = Thread.currentThread();
            // 如果当前为第一个获取读锁的线程，即最后一个使读锁的state从0变为1的线程
            if (firstReader == current) {
                // assert firstReaderHoldCount > 0;
                // 如果只持有一次，则可以释放，将 firstReader 设为空，否则计数
                // 减 1
                if (firstReaderHoldCount == 1)   //只持有1次，就释放 并重置firstReader
                    firstReader = null;
                else    //不止1次，则次数减1
                    firstReaderHoldCount--;
            } else {    //如果当前线程不是第一个获得读锁的
                // 获取当前线程的计数器
                HoldCounter rh = cachedHoldCounter;
                //如果当前线程不是最后一个获取所得线程，即无法从cachedHoldCounter中直接获取
                if (rh == null || rh.tid != getThreadId(current))
                    rh = readHolds.get(); //则获得当前线程map中存储的HoldCounter
                // 得到当前线程持有读锁的次数count。
                int count = rh.count;
                // 持有数小于等于1，
                if (count <= 1) {
                    //注意以下remove只是将线程的map中的底层table的entry引用删除了，如果cachedHoldCounter此时引用entry，它的值不会为null。
                    readHolds.remove();//不管是count为1，还是为0，将threadLocals中的映射直接删除，避免内存泄漏，
                    if (count <= 0)   //如果小于等于0，说明没有lock就进行unlock，抛出异常。
                        throw unmatchedUnlockException();
                }
                // 线程持有锁数减1
                --rh.count;
            }
            // 更新完持有计数之后，自旋更新同步器状态，把读锁的数量减 1
            //自旋的原因是因为，同一时间有可能有多个读锁正在进行释放锁
            for (;;) {
                int c = getState();
                int nextc = c - SHARED_UNIT;   //释放资源，同步器状态需要减SHARED_UNIT。
                if (compareAndSetState(c, nextc)) //只有成功设置同步器状态后，才会返回
                    // Releasing the read lock has no effect on readers,
                    // but it may allow waiting writers to proceed if
                    // both read and write locks are now free.
                    // 表示是否完全释放读锁资源。
                    return nextc == 0;  //说明只有读锁完全释放，才会唤醒后继线程，写锁才有机会争取到。
            }
        }
```
***
## ReadLock类 和 WriteLock类
核心方法都是使用Sync，可参考ReentrantLock如何调用Sync中方法。
这边只讲它们怎么联系起来
```java
public static class ReadLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -5992448646407690164L;
        private final Sync sync;  //读锁类的内部同步器

        /**
         * 构造函数
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;   //在构造函数中同步器会使用读写锁中同步器
        }
 ***以下代码省略
```
ReadLock构造函数中，会将自己锁的同步器，赋值为ReentrantReadWriteLock中的Sync.
```java
/**
     * 写锁的实现
     */
    public static class WriteLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -4992448646407690164L;
        private final Sync sync;

        /**
         * 构造函数
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }
 ***以下代码省略
```
WriteLock构造函数中，会将自己锁的同步器，赋值为ReentrantReadWriteLock中的Sync.
***
## ReentrantReadWriteLock类
### 类的属性
```java
public class ReentrantReadWriteLock
        implements ReadWriteLock, java.io.Serializable {
    // 版本序列号    
    private static final long serialVersionUID = -6992448646407690164L;    
    // 读锁
    private final ReentrantReadWriteLock.ReadLock readerLock;
    // 写锁
    private final ReentrantReadWriteLock.WriteLock writerLock;
    // 同步队列
    final Sync sync;
    
    private static final sun.misc.Unsafe UNSAFE;
    // 线程ID的偏移地址
    private static final long TID_OFFSET;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            // 获取线程的tid字段的内存地址
            TID_OFFSET = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("tid"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
```
说明: 可以看到ReentrantReadWriteLock属性包括了
* ReentrantReadWriteLock.ReadLock对象，表示读锁，在构造函数中赋值
* ReentrantReadWriteLock.WriteLock对象，表示写锁，在构造函数中赋值
* Sync对象，表示同步器,在构造函数中赋值时，会根据是否传入fair参数，开启公平锁或非公平锁，决定实例化NonfairSync还是FairSync
***
### 构造函数 和 获取读写锁方法
```java

    /**
     * ReentrantReadWriteLock 构造函数，默认使用非公平排序属性。
     */
    public ReentrantReadWriteLock() {
        this(false);
    }

    /**
     * 使用给定的公平性策略创造一个新的 ReentrantReadWriteLock。
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

    //提供给ReentrantReadWriteLock实例的获取写锁和读锁的方法
    public WriteLock writeLock() { return writerLock; }
    public ReadLock  readLock()  { return readerLock; }
```
说明:
*  writeLock() 方法和 readLock() 方法会返回内部的写锁和读锁。
* ReentrantReadWriteLock(boolean fair)可以指定设置公平策略或者非公平策略，该构造函数，首先赋值自身的sync ,再将赋值后的自身传入ReadLock和WriteLock构造函数中，在上面的类分析中，可以知道，这两个构造函数会将ReentrantReadWriteLock中已经赋值的公平锁或非公平锁同步器赋值给readerLock 和writerLock中自己的sync。这样就实现了，读写锁也开启了公平/不公平策略。
* ReentrantReadWriteLock()无参构造函数。默认开启无公平策略。

***
对ReentrantReadWriteLock的操作基本上都转化为了对Sync对象的操作，而Sync的函数已经分析过，不再累赘。
# 总结
* 同一个线程中，在没有释放读锁的情况下，就申请写锁，叫锁升级，ReentrantReadWriteLock 不允许锁升级。

* 同一个线程中，在没有释放写锁的情况下，就申请读锁，叫锁降级，ReentrantReadWriteLock 支持锁降级。

* RentrantReadWriteLock不支持锁升级(把持读锁、获取写锁，最后释放读锁的过程)。目的也是保证数据可见性，如果读锁已被多个线程获取，其中任意线程成功获取了写锁并更新了数据，则其更新对其他获取到读锁的线程是不可见的。

* 锁降级中读锁的获取是否必要呢? 答案是必要的。主要是为了保证数据的可见性，如果当前线程不获取读锁而是直接释放写锁，假设此刻另一个线程(记作线程T)获取了写锁并修改了数据，那么此时才获取读锁去读数据，是读不到原始数据的。而如果遵循锁降级的步骤，则线程T将会被阻塞，直到当前线程使用数据并释放读锁之后，线程T才能获取写锁进行数据更新。
# 参考
[JUC锁: ReentrantReadWriteLock详解](https://www.pdai.tech/md/java/thread/java-thread-x-lock-ReentrantReadWriteLock.html)
[J.U.C之locks框架：基于AQS的读写锁(5)](https://segmentfault.com/a/1190000015807600)
