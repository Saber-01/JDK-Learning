# 概述
ReentrantLock实现了一个可重入、可中断、可选择公平或非公平竞争的独占模式的锁。
该类实现了lock接口，内部使用了一个同步器sync来维护同步状态、阻塞/唤醒线程、管理等待队列等。
而这个同步器就是基于AbstractQueuedSynchronizer来实现的。
# 内部类
ReentrantLock总共有三个内部类，并且三个内部类是紧密相关的，下面先看三个类的关系。
![内部类](https://img-blog.csdnimg.cn/20200720215901766.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
　　这三个类中，最底层都是基于AQS实现，其中Sync中将NonfairSync和FairSync两个类都会用到的方法抽取了出来作为他们的父类。
　　我们知道想要基于AQS自定义一个同步器，如果独占模式的话，我们需要在子类中实现三个方法：
tryAcquire、tryRelease和isHeldExclusively这三个方法。并且如果是想多实现条件等待的功能，
我们还需要一个方法去创建conditionObject类。
　　Sync中已经对除tryAcquire的其他方法进行了重写实现。那么为什么ReentrantLock内部还需要额外创建2个类去继承Sync呢?答案从这2个类的名字就可以得到。因为ReentrantLock之所以实现了根据传入的参数觉得公平锁还是非公平锁，就是因为内部多创建了NonfairSync和FairSync这2个类。可以从类的构造方法中看到：
```java

  /** 提供所有实现机制的同步器 */
    private final Sync sync;   //锁的构造函数中，根据是否开启公平锁，会被赋值为不同的Sync子类

 /**
     * Creates an instance of {@code ReentrantLock}.
     * This is equivalent to using {@code ReentrantLock(false)}.
     * 创建 ReentrantLock 实例。
     * 相当于使用 ReentrantLock(false)，默认为非公平锁。
     */
    public ReentrantLock() {  //默认非公平
        sync = new NonfairSync();
    }

    /**
     * Creates an instance of {@code ReentrantLock} with the
     * given fairness policy.
     *使用给定的公平/非公平策略创造一个 ReentrantLock 实例
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
    public ReentrantLock(boolean fair) {   //true表示开启公平锁，false表示不公平
        sync = fair ? new FairSync() : new NonfairSync();
    }
```
* ReentrantLock提供了两个构造函数，无参构函数，默认创建一个NonfairSync类即非公平锁。
而指定boolean类型参数fair的构造函数，传入true时，创建的是FairSync类即公平锁。这也是ReentrantLock类提供给用户指定是否开启公平锁的关键。
* sync是ReentrantLock内部的同步器，它在锁的构造函数中，根据是否开启公平锁，会被赋值为不同的Sync子类。
***
## Sync 
从以上分析，可知，Sync继承于AQS类，作为一个使用AQS实现的独占同步器类，重写了tryRelease和isHeldExclusively方法，并提供newCondition方法，除此之外，还提供了外部方法需要用到的工具类方法。
源码如下：
```java
 /**
     * 此锁同步控制的基础。下层子类分为公平版本和非公平版本。使用 AQS
     * 状态表示锁的持有数量。
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        /**
         * Performs {@link Lock#lock}. The main reason for subclassing
         * is to allow fast path for nonfair version.
         * 用于 Lock.lock 实现。子类化的主要原因是允许非公平版本的快速实现
         */
        abstract void lock();

        /**
         * Performs non-fair tryLock.  tryAcquire is implemented in
         * subclasses, but both need nonfair try for trylock method.
         * 非公平锁的 tryAcquire 实现。
         *  非公平的 tryLock。tryAcquire 在子类中实现，但是同时需要 trylock
         *  方法中的非公平尝试。
         */
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();  //得到同步状态
            if (c == 0) {
                //如果没有线程独占资源，即没有线程持有锁
                //CAS方法将同步器状态设置为传入的参数
                //并且把当前线程设置为独占该资源，即持有锁
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;   //获取成功返回true
                }
            }
            //如果有线程持有锁，判断锁是否是当前线程自己持有，即可重入
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;   //重入一次加重新叠加同步状态的state。
                if (nextc < 0) // overflow 重入次数过多，抛出异常
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;    //重入成功也返回true
            }
            return false;  //获取失败返回false
        }
        //  tryRelease 的实现
        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;   // 计算释放后的 state 值
            //如果当前线程不是独占资源的线程，会抛出非法监控状态异常
            //即unlock之前必须要lock，才不会抛异常
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {  //只有state状态为0，线程才会取消独占状态、
                // state 计数为 0，重入锁已经全部释放，可以唤醒下一个线程。
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);   //设置同步器的同步状态
            return free;   //重入锁释放成功返回true。此时state为0
        }

        //判断当前线程是否独占资源，是否持有锁
        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        //新建一个condition条件。
        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // Methods relayed from outer class
        //从外部类传递的方法

   // 返回持有锁的线程
        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }
        // 获取状态（重入计数）
        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }
        // 是否有线程持有锁
        final boolean isLocked() {
            return getState() != 0;
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
                throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }
    }
```
***
## NonfairSync 
非公平同步器 NonfairSync 继承自 Sync，是非公平锁的基础。
```java
   /**
     * Sync object for non-fair locks
     * 非公平锁 Sync 的实现
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;

        /**
         * Performs lock.  Try immediate barge, backing up to normal
         * acquire on failure.
         * 实现 lock 函数。
         */
        final void lock() {
            // 通过 CAS 方式将状态值从 0 设置成 1，如果设置
            // 成功则调用 setExclusiveOwnerThread 将当前线程设置为持有锁。
            // 设置成功，说明之前没有线程持有锁，则将当前线程设置为独占该锁
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                //会先调用AQS的acquire方法，不可中断获取资源，
            //但是该方法中tryAcquire使用的确实当前AQS子类的，这时java的动态分派决定的
            //该类的tryAcquire调用Sync中的nonfairTryAcquire方法
            //该方法尝试获取，成功则将state从0设置为1，失败则继续判断是否是重入
            //重入成功，state也加1,返回true。
                acquire(1);
        }

        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }
```
***
## FairSync
公平锁同步器 FairSync 同样继承自 Sync。
源码分析如下：
```java
/**
     * Sync object for fair locks
     *  公平锁 Sync 的实现
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        final void lock() {
            //与非公平锁不同，非公平锁会先CAS操作资源，如果操作成功，就独占资源，线程就持有了锁
            //公平锁直接调用acquire方法，
            acquire(1);
        }

        /**
         * Fair version of tryAcquire.  Don't grant access unless
         * recursive call or no waiters or is first.
         *  tryAcquire 的公平锁版本。
         */
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) { //只有在state为0，说明锁未被任何线程持有才继续执行
                //hasQueuedPredecessors查询是否有任何线程比当前线程等待执行 acquire 的时间还长
                // !hasQueuedPredecessors返回true说明同步队列中下一个等待唤醒的线程是当前线程，或者同步队列为空时
                //如果满足条件，就用 CAS 的 方式修改状态，修改为 acquires
                if (!hasQueuedPredecessors() &&
                        compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);   //并设置当前线程独占此锁。
                    return true;
                }
            }
            //如果锁被任意线程占用，判断是不是被当前线程占用，现在正在重入。
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;  //如果是重入，state也要进行叠加
                if (nextc < 0)
                    //如果重入的次数过多，导致溢出了，则抛出异常
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);   //设置state。
                return true;
            }
            return false;
        }
    }
```
## 公平锁的实现原理
通过源码可以发现，公平锁之所以可以实现公平获取，首先它在lock方法中与非公平锁不同，
* 非公平锁会先使用compareAndSetState(0, 1)操作资源，如果操作成功，就调用setExclusiveOwnerThread(Thread.currentThread())独占资源，线程就持有了锁。如果这一步失败，再调用acquire方法
* 公平锁直接调用acquire方法。

其次，非公平锁调用的是自己版本的tryAcquire，内部调用的是nonfairtryAcquire。而公平锁调用也是自己版本的tryAcquire，我们将这两个方法源码再次拿出分析对比：
nonfairtryAcquire源码：
```java
final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();  //得到同步状态
            if (c == 0) {
                //如果没有线程独占资源，即没有线程持有锁
                //CAS方法将同步器状态设置为传入的参数
                //并且把当前线程设置为独占该资源，即持有锁
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;   //获取成功返回true
                }
            }
            //如果有线程持有锁，判断锁是否是当前线程自己持有，即可重入
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;   //重入一次加重新叠加同步状态的state。
                if (nextc < 0) // overflow 重入次数过多，抛出异常
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;    //重入成功也返回true
            }
            return false;  //获取失败返回false
        }
            
```
* 首先判断同步器的同步状态是否为0，如果为0，说明当前没有线程独占资源，即没有线程持有锁，
* 那么就是用CAS方法将同步器状态设置为acquires(ReentrantLock中该值为1)。
* 如果设置成功，说明获取资源成功，调用 setExclusiveOwnerThread(current)设置当前线程独占资源，持有锁，返回 true。
* 如果同步状态不为0，即有线程持有锁，判断锁是否是当前线程自己持有，如果是，就重入，这也是ReentrantLock为什么可重入的原因
* 重入一次就要将同步器状态累加一次acquires(1),然后判断是否重入过多，导致溢出，如果移除抛出异常，
* 如果没溢出，调用setState设置状态，返回true，重入也算争取资源成功。
* ***
公平锁版本FairSync中的tryAcquire源码如下：
```java
 /**
         * Fair version of tryAcquire.  Don't grant access unless
         * recursive call or no waiters or is first.
         *  tryAcquire 的公平锁版本。
         */
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) { //只有在state为0，说明锁未被任何线程持有才继续执行
                //hasQueuedPredecessors查询是否有任何线程比当前线程等待执行 acquire 的时间还长
                // !hasQueuedPredecessors返回true说明同步队列中下一个等待唤醒的线程是当前线程，或者同步队列为空时
                //如果满足条件，就用 CAS 的 方式修改状态，修改为 acquires
                if (!hasQueuedPredecessors() &&
                        compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);   //并设置当前线程独占此锁。
                    return true;
                }
            }
            //如果锁被任意线程占用，判断是不是被当前线程占用，现在正在重入。
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;  //如果是重入，state也要进行叠加
                if (nextc < 0)
                    //如果重入的次数过多，导致溢出了，则抛出异常
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);   //设置state。
                return true;
            }
            return false;
        }
```
* 首先判断同步器的同步状态是否为0，为0说明当前没有线程独占资源，即没有线程持有锁
* 接下来使用hasQueuedPredecessors()进行判断，这一步也是公平锁原理所在
 源码如下：
```java
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
分析可知!hasQueuedPredecessors返回true说明同步队列中下一个等待唤醒的线程是当前线程，或者同步队列为空时才去尝试调用CAS方法将同步器状态设置为acquires(ReentrantLock中该值为1)。
* 接下来的步骤就和非公平锁的方法一模一样。
***
# 关键方法
重入锁 ReentrantLock 中的核心方法包括 lock，lockInterruptibly，tryLock，unlock，newCondition 等，均基于同步器实现。
源码如下：
```java
 /**
     * Acquires the lock.
     * acquire 锁。
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately, setting the lock hold count to one.
     *如果锁没有被其他线程持有则 acquire 锁，并立即返回，设置锁的持有数
     *   为 1。
     * <p>If the current thread already holds the lock then the hold
     * count is incremented by one and the method returns immediately.
     *如果当前线程已经持有锁，那么持有计数加 1，然后立刻返回。
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling
     * purposes and lies dormant until the lock has been acquired,
     * at which time the lock hold count is set to one.
     * 如果锁被其他线程持有，那么当前线程进入睡眠。直到锁被 acquire，此时
     * 锁持有的计数被设置成 1。
     */
    public void lock() {
        sync.lock();
    }

    /**
      *
     * acquire 锁，除非当前线程被中断。
     *
     *如果锁不被其他线程持有，则获取锁并立即返回，将持有计数设置为 1。
      *
     *如果当前线程已经持有此锁，那么持有计数加 1，然后立即返回。
     *
     *如果锁被其他线程持有，那么当前线程进入睡眠。直到发生以下两种情况之一：
     * 此锁被当前线程 acquire；
     *或者其他线程中断了此线程。
     * </ul>
     *如果锁被当前线程 acquire，持有计数设置为 1。
     * <p>If the current thread:
     *如果当前线程：
     *在进入此方法时其中断状态已设置，或者在获取锁时被中断了，那么抛出
     *   InterruptedException 异常，并清除当前线程的中断状态。
     *
     * </ul>
     *
     * <p>In this implementation, as this method is an explicit
     * interruption point, preference is given to responding to the
     * interrupt over normal or reentrant acquisition of the lock.
     * 在此实现中，由于这个方法是显式中断点，所以优先相应中断而不是正常的
     *   或者可重入的锁获取。
     * @throws InterruptedException if the current thread is interrupted
     */
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**

     * 如果在调用时锁没有被其他线程持有，则 acquire 锁。
     *如果锁不被其他线程持有，则 acquire 锁，并立即返回 true，设置锁的
     *持有计数为 1。当这个锁被设置成公平锁策略时，如果锁可用，调用 tryLock
     * 会立即获得锁，不管其它线程是否正在等待锁。这种行为在某些情况下是
     * 有用的，即使它破坏了公平性。如果想为这个锁设置公平性，使用
     *   tryLock(0, TimeUnit.SECONDS)即可（他会检测中断）。
     *
     *如果当前线程已经持有该锁，那么持有计数将增加 1，然后此方法返回 true。
     *如果锁被其他线程持有，此方法立即返回 false。
     * @return {@code true} if the lock was free and was acquired by the
     *         current thread, or the lock was already held by the current
     *         thread; and {@code false} otherwise
     */
    //公平锁中，这个方法在判断都锁可用时，也会马上获得锁，不管其它线程是否正在等待锁。
    // 这种行为在某些情况下是有用的，即使它破坏了公平性。
    // 如果想为这个锁设置公平性，使用 tryLock(0, TimeUnit.SECONDS)。
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    /**
     * 如果在给定的等待时间内没有其他线程持有锁，且当前线程没有中断，
     * 则 acquire 锁。
     *
     * 如果锁不被其他线程持有，则 acquire 锁，并立即返回 true，设置持有计数
     * 为 1。当这个锁被设置成使用公平策略时，如果有其他线程正在等待锁，
     * 那么锁不会被 acquire 到。这与 tryLock 形成对比。如果想要一个定时的
     * tryLock，它允许对一个公平的锁进行操作，那么把定时的和不定时的
     * 结合在一起：
     * if (lock.tryLock() ||
     *     lock.tryLock(timeout, unit)) {
     *   ...
     * }}
     *
     * 如果当前线程已经持有锁，那么持有计数加 1，然后方法返回 true。
     *
     * 如果锁被其他线程持有，那么当前线程进入睡眠。直到发生以下三种情况之一：
     * 此锁被当前线程 acquire；或者其他线程中断了此线程；或者时间到期。
     *
     * 如果锁被当前线程 acquire，返回 true，持有计数设置为 1。
     *
     * 如果当前线程：
     * 在进入此方法时其中断状态已设置，或者在获取锁时被中断了，那么抛出
     * InterruptedException 异常，并清除当前线程的中断状态。
     *
     * 如果指定的等待时间到期了，返回 false。如果时间小于等于 0，此方法
     * 不会再等待。
     *
     * 在此实现中，由于这个方法是显式中断点，所以优先相应中断而不是正常的
     * 或者可重入的锁获取，也不是等待时间的流逝。
     *
     * @param timeout the time to wait for the lock
     * @param unit the time unit of the timeout argument
     * @return {@code true} if the lock was free and was acquired by the
     *         current thread, or the lock was already held by the current
     *         thread; and {@code false} if the waiting time elapsed before
     *         the lock could be acquired
     * @throws InterruptedException if the current thread is interrupted
     * @throws NullPointerException if the time unit is null
     */
    //与tryLock不同，该方法，在公平锁中，也是公平的，即在等待期间
    //如果有其他队列在等待获取，它不会尝试获取。
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * Attempts to release this lock.
     *尝试释放锁。
     * <p>If the current thread is the holder of this lock then the hold
     * count is decremented.  If the hold count is now zero then the lock
     * is released.  If the current thread is not the holder of this
     * lock then {@link IllegalMonitorStateException} is thrown.
     *如果当前线程是这个锁的持有者，那么持有计数将递减。如果持有计数现在
     * 为 0，则释放锁。如果当前线程不是这个锁的持有者，则抛出
     * IllegalMonitorStateException 异常。
     * @throws IllegalMonitorStateException if the current thread does not
     *         hold this lock
     */
    public void unlock() {
        sync.release(1);
    }

    /**
     * Returns a {@link Condition} instance for use with this
     * {@link Lock} instance.
     *返回与此 Lock 实例一起使用的 Condition 实例。
     * <p>The returned {@link Condition} instance supports the same
     * usages as do the {@link Object} monitor methods ({@link
     * Object#wait() wait}, {@link Object#notify notify}, and {@link
     * Object#notifyAll notifyAll}) when used with the built-in
     * monitor lock.
     * 返回的 Condition 实例支持 Object 监视器同样的以下方法：wait，notify，
     *      * notifyAll。
     * <ul>
     *
     * <li>If this lock is not held when any of the {@link Condition}
     * {@linkplain Condition#await() waiting} or {@linkplain
     * Condition#signal signalling} methods are called, then an {@link
     * IllegalMonitorStateException} is thrown.
     * 调用 Condition 中的 await 或者 signal 方法时如果没有持有锁，那么将会
     *  抛出 IllegalMonitorStateException 异常。
     *
     * <li>When the condition {@linkplain Condition#await() waiting}
     * methods are called the lock is released and, before they
     * return, the lock is reacquired and the lock hold count restored
     * to what it was when the method was called.
     * 当 Condition 的 await 方法被调用时，如果锁被释放，在返回之前，会再次
     *  获取锁，并将持有计数恢复到方法被调用时的值。
     *
     * <li>If a thread is {@linkplain Thread#interrupt interrupted}
     * while waiting then the wait will terminate, an {@link
     * InterruptedException} will be thrown, and the thread's
     * interrupted status will be cleared.
     *如果一个线程在等待时被中断，那么等待将会停止，抛出 InterruptedException
     *   异常，线程的中断状态将被清除。
     * <li> Waiting threads are signalled in FIFO order.
     * 等待的线程按 FIFO 的顺序唤醒。
     * <li>The ordering of lock reacquisition for threads returning
     * from waiting methods is the same as for threads initially
     * acquiring the lock, which is in the default case not specified,
     * but for <em>fair</em> locks favors those threads that have been
     * waiting the longest.
     *从等待方法返回的线程的锁重新 acquire 的顺序与最初获取锁的线程相同
     * （在默认情况下未指定），但对于公平锁，优先使用那些等待时间最长的线程。
     * </ul>
     *
     * @return the Condition object
     */
    public Condition newCondition() {
        return sync.newCondition();
    }
```
# ReentrantLock 总结
* ReentrantLock 是 Lock 接口的可重入锁实现，完全基于 AQS 抽象类。
* 由于 ReentrantLock 是独占锁，用户只需继承 AQS，实现自身同步器中 tryAcquire 和 tryRelease 方法即可,如果需要条件等待，则额外实现一个方法，用于新建AQS内部类ConditionObject类即可
* 当前持有锁的线程重入一次，状态值加 1，当状态值降到 0 时，才能释放锁。
* ReentrantLock 中同步器状态state对应的锁(资源)的含义：![State含义](https://img-blog.csdnimg.cn/20200720232453712.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwOTIxMTUz,size_16,color_FFFFFF,t_70)
# 参考
[JUC锁: ReentrantLock详解](https://www.pdai.tech/md/java/thread/java-thread-x-lock-ReentrantLock.html)
[ReentrantLock源码分析](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/md/JUC/ReentrantLock.md)
