package JUC.JUCCollections;
/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;

/**
 * 一种由 Delayed 元素组成的无界阻塞队列，元素只能在延迟到期之后才能被
 * 获取。队列的头部是延迟到期时间最长的 Delayed 元素。如果没有延迟到期
 * 的元素，则没有头部元素，poll 返回 null。当一个元素的 getDelay() 方法
 * 返回的元素小于等于 0，表示延迟已经到期。即使延迟未到期的元素不能使用
 * take 或者 poll 删除，它们也被视为正常元素。例如，size 方法返回到期和
 * 未到期所有元素的总和。此队列不允许 null 元素。
 *
 * 此类和它的迭代器实现了 Collection 和 Iterator 的所有可选操作。迭代器
 * 不保证任何顺序。
 *
 * 此类是 Java Collections Framework 的成员。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public class DelayQueue<E extends Delayed> extends AbstractQueue<E>
        implements BlockingQueue<E> {

    // 保证线程安全的锁
    private final transient ReentrantLock lock = new ReentrantLock();

    // 用于排序的优先队列
    private final PriorityQueue<E> q = new PriorityQueue<E>();

    /**
     * 等待队列头部元素的线程。Leader-Follower 模式的这种变体可以最小化
     * 不必要的等待时间。当一个线程成为 leader 时，它等待下一个 delayed，
     * 其他线程无限期等待。leader 线程在从 take/poll 返回之前，必须唤醒
     * 其他线程，除非其他线程在此期间成为 leader。每当队列头被一个具有
     * 较早过期时间的元素替换时，通过将 leader 设置为 null 使其无效，并且
     * 会通知一些正在等待的线程，但不一定是当前的 leader。因此等待线程
     * 必须准备在等待时获取和失去领导权。
     */
    private Thread leader = null;

    /**
     * 当一个新的元素到队列头部或者一个新的线程需要成为 leader 时唤醒
     * condition 队列的线程。
     */
    private final Condition available = lock.newCondition();

    /**
     * 创造一个空的队列。
     */
    public DelayQueue() {}

    /**
     * 创造一个包含指定集合所有元素的队列。
     *
     * @param c the collection of elements to initially contain
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    public DelayQueue(Collection<? extends E> c) {
        this.addAll(c);
    }

    /**
     * 指定元素插入到队列。
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        return offer(e);
    }

    /**
     * 指定元素插入到队列。
     *
     * @param e the element to add
     * @return {@code true}
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 插入到底层的PriorityQueue<E>队列。
            q.offer(e);
            if (q.peek() == e) {  //如果排序后，是在队列头，则唤醒一个出队线程
                leader = null;     //重置leader。因为队列头已经更新了
                available.signal();    //唤醒
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 指定元素插入到队列中。
     *
     * @param e the element to add
     * @throws NullPointerException {@inheritDoc}
     */
    public void put(E e) {
        offer(e);
    }

    /**
     * 指定元素插入到队列中。
     *
     * @param e the element to add
     * @param timeout This parameter is ignored as the method never blocks
     * @param unit This parameter is ignored as the method never blocks
     * @return {@code true}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e);
    }

    /**
     * 获取并删除队列头部元素，没有返回 null。
     *
     * @return the head of this queue, or {@code null} if this
     *         queue has no elements with an expired delay
     */
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 获取头部元素
            E first = q.peek();
            // 如果没有元素或时间没有到期，返回 null
            if (first == null || first.getDelay(NANOSECONDS) > 0)
                return null;
            else
                //如果过期，则 出队列，从优先队列中取出
                return q.poll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取并删除队列头部元素，如果没有延迟到期的元素，等待直到获取成功。
     *
     * @return the head of this queue
     * @throws InterruptedException {@inheritDoc}
     */
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (;;) {  //自旋判断
                E first = q.peek();//取得队列头的元素
                // 如果没有元素可用，进入 available 队列等待，
                if (first == null)
                    available.await();
                else {   //如果不为null，有元素，
                    // 获取头元素的剩余时间，因为只有到期了才会取出
                    long delay = first.getDelay(NANOSECONDS);
                    // 没有剩余时间，可以出队列了
                    if (delay <= 0)   //如果到期了，那么出队列
                        return q.poll();
                    // 如果没到期，那么first重置为null，方便GC
                    first = null;
                    //如果 leader 不为 null，说明有其他线程已经获取到 leader，
                    // 那么进入available 队列等待
                    if (leader != null)
                        available.await();
                    else {   //如果leader为空，
                        // 获取执行此次take的当前线程，将 leader 设置为当前线程
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread;
                        try {
                            // 在 available 中进行限期等待。时间到了会自己运行。
                            available.awaitNanos(delay);
                        } finally {
                            // 如果等待过程中leader没有被重置，那么将 leader 置为 null，重新循环
                            // 再次尝试获取元素。
                            if (leader == thisThread)
                                leader = null;
                        }
                    }
                }
            }
        } finally {
            if (leader == null && q.peek() != null)   //当一个take成功获取到元素后，或者因为中断取消获取
                available.signal();    //进行唤醒等待在available中的线程。
            lock.unlock();  //解锁
        }
    }

    /**
     * 获取并删除队列头部元素，如果没有延迟到期的元素，等待直到指定
     * 时间片到期或者获取成功。
     *
     * @return the head of this queue, or {@code null} if the
     *         specified waiting time elapses before an element with
     *         an expired delay becomes available
     * @throws InterruptedException {@inheritDoc}
     */
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (;;) {
                E first = q.peek();
                if (first == null) {
                    if (nanos <= 0)
                        return null;
                    else
                        nanos = available.awaitNanos(nanos);
                } else {
                    long delay = first.getDelay(NANOSECONDS);
                    if (delay <= 0)
                        return q.poll();
                    // 获取失败，返回 null
                    if (nanos <= 0)
                        return null;
                    first = null; // don't retain ref while waiting
                    if (nanos < delay || leader != null)
                        nanos = available.awaitNanos(nanos);
                    else {
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread;
                        try {
                            long timeLeft = available.awaitNanos(delay);
                            nanos -= delay - timeLeft;
                        } finally {
                            if (leader == thisThread)
                                leader = null;
                        }
                    }
                }
            }
        } finally {
            if (leader == null && q.peek() != null)
                available.signal();
            lock.unlock();
        }
    }

    /**
     * 获取但不删除队列头部元素，如果队列为空返回 null。和 poll 不同的是，
     * 如果队列中没有延迟到期的元素，此方法会返回即将延迟到期的元素。
     *
     * @return the head of this queue, or {@code null} if this
     *         queue is empty
     */
    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.peek();
        } finally {
            lock.unlock();
        }
    }

    // 返回优先队列的长度。
    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 如果延迟到期返回第一个元素。
     * 由 drainTo 调用。持有锁时调用。
     */
    private E peekExpired() {
        // assert lock.isHeldByCurrentThread();
        E first = q.peek();
        return (first == null || first.getDelay(NANOSECONDS) > 0) ?
                null : first;
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = 0;
            // 循环获取第一个元素，将其添加到指定集合
            for (E e; (e = peekExpired()) != null;) {
                c.add(e);       // In this order, in case add() throws.
                q.poll();
                ++n;
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        if (maxElements <= 0)
            return 0;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = 0;
            for (E e; n < maxElements && (e = peekExpired()) != null;) {
                c.add(e);       // In this order, in case add() throws.
                q.poll();
                ++n;
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 清除所有元素。
     */
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            q.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 无界队列。直接返回 Integer.MAX_VALUE。
     *
     * @return {@code Integer.MAX_VALUE}
     */
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    /**
     * 返回数组。
     *
     * @return an array containing all of the elements in this queue
     */
    public Object[] toArray() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.toArray();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 返回数组。
     *
     * @param a the array into which the elements of the queue are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this queue
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this queue
     * @throws NullPointerException if the specified array is null
     */
    public <T> T[] toArray(T[] a) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.toArray(a);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 删除指定元素，无论是否到期。
     */
    public boolean remove(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.remove(o);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Identity-based version for use in Itr.remove
     */
    void removeEQ(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (Iterator<E> it = q.iterator(); it.hasNext(); ) {
                if (o == it.next()) {
                    it.remove();
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 返回迭代器。
     *
     * @return an iterator over the elements in this queue
     */
    public Iterator<E> iterator() {
        return new Itr(toArray());
    }

    /**
     * 迭代器。
     */
    private class Itr implements Iterator<E> {
        final Object[] array; // Array of all elements
        int cursor;           // index of next element to return
        int lastRet;          // index of last element, or -1 if no such

        Itr(Object[] array) {
            lastRet = -1;
            this.array = array;
        }

        public boolean hasNext() {
            return cursor < array.length;
        }

        @SuppressWarnings("unchecked")
        public E next() {
            if (cursor >= array.length)
                throw new NoSuchElementException();
            lastRet = cursor;
            return (E)array[cursor++];
        }

        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            removeEQ(array[lastRet]);
            lastRet = -1;
        }
    }

}

