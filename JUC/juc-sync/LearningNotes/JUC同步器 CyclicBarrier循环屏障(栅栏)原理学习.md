## CyclicBarrier

允许一组线程等待彼此全部达到共同屏障点的同步辅助工具。


### 类属性

当线程执行完毕，进入 await 方法中，将 count 计数减 1，并且进入 Condition 队列中等待被唤醒。当所有线程到达屏障处时，屏障被打开，所有线程被唤醒，此 generation 结束。开启下一个 generation 时 count 被重置为 parties。

```java
    /** 守护 barrier 入口的锁 */
    private final ReentrantLock lock = new ReentrantLock();
    /** 在此 Condition 上等待直到所有线程到达 barrier */
    private final Condition trip = lock.newCondition();
    /** 要屏障的线程数 */
    private final int parties;
    /** 当线程都到达 barrier，运行的 barrierCommand */
    private final Runnable barrierCommand;
    /** 当前的 generation */
    private Generation generation = new Generation();

    /**
     * 仍然需要等待的线程数量。每一个 generation 从 parties 降到 0。创建新的
     * generation 或者 broken 的时候重置为需要拦截的线程数。
     * parties 表示需要拦截的线程数。
     */
    private int count;
```

### 成员函数

CyclicBarrier 的核心方法为 await。

调用 **await** 方法让线程在屏障前等待，此类的核心成员是在 await 中调用的 **dowait** 方法。

**dowait** 方法主要分为两个部分。当线程全部到达屏障前时，下一步需要做的是打破屏障，打破屏障的一系列操作包括执行屏障指令 command，然后唤醒所有等待的线程，最后开启下一个 generation。另一种情况是，如果有线程没有到达屏障前，即 count 计数没有降到 0，则线程进入 Condition 队列等待。

```java
    /**
     * 在屏障前等待，直到所有的线程到达屏障。
     *
     * 如果当前线程没有最后到达的，那么出于线程调度的目的，它将会休眠，
     * 直到发生以下情况之一：
     * 最后一个线程到达；
     * 其他线程中断了当前线程；
     * 其他线程中断了某一个线程；
     * 等待屏障过程中其他线程等待时间到了；
     * 其他线程调用了 reset。
     *
     * 如果当前线程：
     * 在进入此函数之前有中断状态；或者等待时被中断
     * 那么抛出 InterruptedException 异常并清除当前线程的中断状态。
     *
     * 如果在任何线程等待的时候屏障被 reset 了，或者如果 await 被调用时
     * 屏障被打破了，或者线程等待时发生了上面的事情，则抛出
     * BrokenBarrierException 异常。
     *
     * 如果等待时任何线程被中断了，所有等待的线程都会抛出
     * BrokenBarrierException 异常，同时屏障被置为打破状态。
     *
     * 如果当前线程是最后一个到达的线程，且构造此对象时制定了屏障行为，
     * 那么在允许其他线程继续执行之前，将会执行此屏障操作。
     * 如果执行此操作过程中发生了异常，则异常将会被传递到当前线程中，屏障
     * 被置为打破状态。
     *
     * @return the arrival index of the current thread, where index
     *         {@code getParties() - 1} indicates the first
     *         to arrive and zero indicates the last to arrive
     * @throws InterruptedException if the current thread was interrupted
     *         while waiting
     * @throws BrokenBarrierException if <em>another</em> thread was
     *         interrupted or timed out while the current thread was
     *         waiting, or the barrier was reset, or the barrier was
     *         broken when {@code await} was called, or the barrier
     *         action (if present) failed due to an exception
     */
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe); // cannot happen
        }
    }
    
    /**
     * barrier 的主要代码，包括多种策略。
     */
    private int dowait(boolean timed, long nanos)
            throws InterruptedException, BrokenBarrierException,
            TimeoutException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 获取当前 generation
            final Generation g = generation;

            // 判断当前 generation 是否是打破状态，如果是抛出异常
            if (g.broken)
                throw new BrokenBarrierException();

            // 如果线程已经有了中断状态，则打破屏障，唤醒所有线程，并抛出中断异常
            if (Thread.interrupted()) {
                breakBarrier();
                throw new InterruptedException();
            }

            int index = --count;
            // 如果所有线程已经到达屏障
            if (index == 0) {  // tripped
                boolean ranAction = false;
                try {
                    // 执行屏障指令（屏障操作），执行完成后打破屏障，唤醒所有线程
                    final Runnable command = barrierCommand;
                    if (command != null)
                        command.run();
                    ranAction = true;
                    nextGeneration();
                    return 0;
                } finally {
                    if (!ranAction)
                        breakBarrier();
                }
            }

            // 自旋进入 Condition 等待，直到满足 tripped, broken, interrupted, timed out 之一。
            for (;;) {
                try {
                    if (!timed)
                        // 线程进入 Condition 队列中等待会释放持有的锁
                        trip.await();
                    else if (nanos > 0L)
                        nanos = trip.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    if (g == generation && ! g.broken) {
                        breakBarrier();
                        throw ie;
                    } else {
                        // We're about to finish waiting even if we had not
                        // been interrupted, so this interrupt is deemed to
                        // "belong" to subsequent execution.
                        Thread.currentThread().interrupt();
                    }
                }

                if (g.broken)
                    throw new BrokenBarrierException();

                if (g != generation)
                    return index;

                if (timed && nanos <= 0L) {
                    // 超时，打破当前屏障
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            lock.unlock();
        }
    }
```

### CountDownLatch 和 CyclicBarrier 的区别

CountDownLatch 和 ReentrantLock 比较相似，都是自己重写了 AQS 的某些方法；CyclicBarrier 没有重写 AQS，只是使用了 ReentrantLock 和其中的 Condition 用于暂时让线程休眠。

CountDownLatch 让一个或多个线程等待其他线程完成执行；CyclicBarrier 不存在主线程（等待）子线程的情况，它是多个线程之间的相互等待。

CountDownLatch 的计数器无法被重置，一次执行完成之后即无效；CyclicBarrier的计数器可以被重置后循环使用。

### 应用场景

CountDownLatch 可用于游戏开始阶段，主线程为控制游戏开始的线程，其他线程为游戏玩家的线程，只有当所有的玩家线程准备好之后，主线程才继续下一步动作，即开始游戏。

CyclicBarrier 可用于等待所有线程执行完成之后再开启下一个任务，如多线程分别计算最后合并结果。
