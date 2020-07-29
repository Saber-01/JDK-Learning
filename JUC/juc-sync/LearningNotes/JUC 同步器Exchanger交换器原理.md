## Exchanger

Exchanger 用于线程之间交换数据。

### 类属性

Node 节点类是线程的载体，用于保存正在等待的线程，当 match 属性为 null 时，自旋或者阻塞等待线程匹配。而当 match 不为 null 时，说明已经匹配上了，返回匹配的值。

```java
    @sun.misc.Contended static final class Node {
        // 以下字段多槽交换时使用
        // 在 arena 中的下标，多个槽位的时候使用
        int index;              // Arena index
        // 上一次记录的 Exchanger.bound
        int bound;              // Last recorded value of Exchanger.bound
        // 当前 bound 下 CAS 失败的次数
        int collides;           // Number of CAS failures at current bound
        // 以下字段单槽交换时使用
        // 用于自旋是计算随机数
        int hash;               // Pseudo-random for spins
        // 线程当前需要交换的数据
        Object item;            // This thread's current item
        //  配对线程会把自身携带的值存入 match
        volatile Object match;  // Item provided by releasing thread
        // 休眠的线程（先到达的线程）
        volatile Thread parked; // Set to this thread when parked, else null
    }
```

比较重要的属性是 slot，用于保存正在等待进行交换的线程。属性 participant 属于 ThreadLocal 类型，用于保存每个线程的 Node。

```java
    /**
     * 单槽交换节点
     */
    private volatile Node slot;
    /**
     * ThreadLocal。
     */
    private final Participant participant;
```

### exchange

**exchange**

如果是单槽交换，调用 slotExchange；如果是多槽交换，调用 arenaExchange。

```java
    /**
     * 等待其他线程到达交换点的时候（除非当前线程被中断），将指定数据传给该线程，
     * 并接受该线程的数据。
     *
     * @param x the object to exchange
     * @return the object provided by the other thread
     * @throws InterruptedException if the current thread was
     *         interrupted while waiting
     */
    @SuppressWarnings("unchecked")
    public V exchange(V x) throws InterruptedException {
        Object v;
        // 要交换的对象
        Object item = (x == null) ? NULL_ITEM : x; // translate null args
        // 如果 arena 为 null，执行 slotExchange，也就是单个槽的方法。slotExchange 传入参数为需要交换的对象
        // 如果 arena 不为 null，或者单槽交换失败时，执行 arenaExchange 多槽交换
        if ((arena != null ||
                (v = slotExchange(item, false, 0L)) == null) &&
                ((Thread.interrupted() || // disambiguates null return
                        (v = arenaExchange(item, false, 0L)) == null)))
            throw new InterruptedException();
        return (v == NULL_ITEM) ? null : (V)v;
    }
```

**slotExchange**

对于先进来的线程，将会走完这个函数的几乎所有流程，而后进来的线程，一般只会到第一个自旋的部分。

第一次自旋是所有线程都会执行的部分，每一次循环分成以下几种情况：

* slot 不等于 null，说明已经有线程在等待匹配了：尝试把 slot 设置为 null，设置成功说明匹配成功。之后设置等待线程的 match，唤醒等待的线程，最后返回。

* slot 等于 null 但是 arena 不为 null，说明应该调用的是 arenaExchange，直接 return。

* slot 等于 null 且 arena 为 null，说明是先进来的线程，把它设置成 slot，然后跳出循环。只有这一种情况不是 return，而是跳出循环，继续执行此函数后面的语句。

继续往下，当前线程现在需要做的是等待后来的线程进行匹配。

同样开始自旋，自旋过程中随机释放 CPU 让其他线程竞争。如果自旋次数已经耗尽了，还没有线程来，则当前线程开始休眠。

```java
    /**
     * 单槽交换。
     *
     * @param item the item to exchange
     * @param timed true if the wait is timed
     * @param ns if timed, the maximum wait time, else 0L
     * @return the other thread's item; or null if either the arena
     * was enabled or the thread was interrupted before completion; or
     * TIMED_OUT if timed and timed out
     */
    private final Object slotExchange(Object item, boolean timed, long ns) {
        // 当前线程携带的 Node 节点
        Node p = participant.get();
        // 当前线程
        Thread t = Thread.currentThread();
        if (t.isInterrupted()) // preserve interrupt status so caller can recheck
            return null;

        // 自旋
        for (Node q;;) {
            // q 不等于 null，说明已经有线程在 slot 上了，此线程是后面才来的。
            if ((q = slot) != null) {
                // 把 slot 置为 null
                if (U.compareAndSwapObject(this, SLOT, q, null)) {
                    // 设置 match
                    Object v = q.item;
                    q.match = item;
                    // 唤醒 slot 里的线程
                    Thread w = q.parked;
                    if (w != null)
                        U.unpark(w);
                    // 返回
                    return v;
                }
                // 把 slot 置为 null 的尝试失败了，说明多个线程竞争修改 slot。
               //  bound == 0 表示 arena 数组未初始化过，CAS操作bound将其增加SEQ
                // 创造一个 arena 数组
                if (NCPU > 1 && bound == 0 &&
                        U.compareAndSwapInt(this, BOUND, 0, SEQ))
                    arena = new Node[(FULL + 2) << ASHIFT];
            }
            // 如果 slot 为 null 但是 arena 不为 null，应该调用的是 arenaExchange，直接返回 null。
            else if (arena != null)
                return null; // caller must reroute to arenaExchange
            // slot 为 null 且 arena 为 null，说明之前没有线程到达。
            // 只有在这种情况下会 break，不会 return。
            else {
                // 设置 slot。
                // 这里只需要设置 item，不需要设置 thread 等，因为 thread 还没有休眠
                p.item = item;
                if (U.compareAndSwapObject(this, SLOT, null, p))
                    break;
                p.item = null;
            }
        }

        // 如果执行到这里说明当前线程是先到达的那一个，且已经设置好了 slot，
        // 等待另一个线程到达。
        int h = p.hash;
        long end = timed ? System.nanoTime() + ns : 0L;
        // 自旋次数
        int spins = (NCPU > 1) ? SPINS : 1;
        Object v;
        // 如果配对的线程还没来
        while ((v = p.match) == null) {
            if (spins > 0) {
                // 自旋过程中随机释放 CPU，让其他线程可以竞争
                h ^= h << 1; h ^= h >>> 3; h ^= h << 10;
                if (h == 0)
                    h = SPINS | (int)t.getId();
                else if (h < 0 && (--spins & ((SPINS >>> 1) - 1)) == 0)
                    Thread.yield();
            }
            // 配对线程已经来了，已经把 slot 改了，继续自旋
            else if (slot != p)
                spins = SPINS;
            // 配对线程还没来，spins <= 0 了
            else if (!t.isInterrupted() && arena == null &&
                    (!timed || (ns = end - System.nanoTime()) > 0L)) {
                // BLOCKER 保存当前线程
                U.putObject(t, BLOCKER, this);
                // 当前线程休眠
                p.parked = t;
                if (slot == p)
                    U.park(false, ns);
                // 当前线程被唤醒
                p.parked = null;
                U.putObject(t, BLOCKER, null);
            }
            // 超时或者其他原因，让出 slot
            else if (U.compareAndSwapObject(this, SLOT, p, null)) {
                v = timed && ns <= 0L && !t.isInterrupted() ? TIMED_OUT : null;
                break;
            }
        }
        // p.match 不为 null 了，先到的线程也可以返回了
        U.putOrderedObject(p, MATCH, null);
        p.item = null;
        p.hash = h;
        return v;
    }
```

**arenaExchange**

在 slotExchange 的基础上加上了数组分散并发请求。和 slotExchange 的主要区别在于它会根据当前线程的数据携带结点 Node 中的 index 字段计算出命中的槽位。

```java
    /**
     * 多槽交换。
     *
     * @param item the (non-null) item to exchange
     * @param timed true if the wait is timed
     * @param ns if timed, the maximum wait time, else 0L
     * @return the other thread's item; or null if interrupted; or
     * TIMED_OUT if timed and timed out
     */
    private final Object arenaExchange(Object item, boolean timed, long ns) {
        Node[] a = arena;
        // 当前线程携带的节点
        Node p = participant.get();
        for (int i = p.index;;) {                      // 当前线程的 arena 索引
            int b, m, c; long j;
            // 从 arena 数组中找出偏移地址为 (i << ASHIFT) + ABASE 的元素, 即真正可用的 Node
            Node q = (Node)U.getObjectVolatile(a, j = (i << ASHIFT) + ABASE);
            // 如果槽中节点不为 null，说明已经有线程在等待了
            if (q != null && U.compareAndSwapObject(a, j, q, null)) {
                // 获取已到达线程的数据
                Object v = q.item;                     // release
                // 设置已到达线程的 match（先到达的线程感应到 match 已设置，就会继续往后直行）
                q.match = item;
                // 唤醒阻塞的线程
                Thread w = q.parked;
                if (w != null)
                    U.unpark(w);
                return v;
            }
            // 槽位可用而且节点为 null，说明当前线程是先到达的那一个。
            else if (i <= (m = (b = bound) & MMASK) && q == null) {
                p.item = item;                         // offer
                // 如果成功设置槽位为当前线程
                if (U.compareAndSwapObject(a, j, null, p)) {
                    long end = (timed && m == 0) ? System.nanoTime() + ns : 0L;
                    Thread t = Thread.currentThread(); // wait
                    // 开始自旋
                    for (int h = p.hash, spins = SPINS;;) {
                        Object v = p.match;
                        // 有线程到达了
                        if (v != null) {
                            U.putOrderedObject(p, MATCH, null);
                            p.item = null;             // clear for next use
                            p.hash = h;
                            return v;
                        }
                        // 继续自旋
                        else if (spins > 0) {
                            h ^= h << 1; h ^= h >>> 3; h ^= h << 10; // xorshift
                            if (h == 0)                // initialize hash
                                h = SPINS | (int)t.getId();
                            else if (h < 0 &&          // approx 50% true
                                    (--spins & ((SPINS >>> 1) - 1)) == 0)
                                Thread.yield();        // two yields per wait
                        }
                        // 配对线程已到达，但是还没设置 match，继续自旋
                        else if (U.getObjectVolatile(a, j) != p)
                            spins = SPINS;       // releaser hasn't set match yet
                        else if (!t.isInterrupted() && m == 0 &&
                                (!timed ||
                                        (ns = end - System.nanoTime()) > 0L)) {
                            // 没等待任何线程，阻塞
                            U.putObject(t, BLOCKER, this); // emulate LockSupport
                            p.parked = t;              // minimize window
                            if (U.getObjectVolatile(a, j) == p)
                                U.park(false, ns);
                            p.parked = null;
                            U.putObject(t, BLOCKER, null);
                        }
                        else if (U.getObjectVolatile(a, j) == p &&
                                U.compareAndSwapObject(a, j, p, null)) {
                            // 尝试缩减 arena 数组（修改 bound）
                            if (m != 0)                // try to shrink
                                U.compareAndSwapInt(this, BOUND, b, b + SEQ - 1);
                            p.item = null;
                            p.hash = h;
                            i = p.index >>>= 1;        // descend
                            if (Thread.interrupted())
                                return null;
                            if (timed && m == 0 && ns <= 0L)
                                return TIMED_OUT;
                            break;                     // expired; restart
                        }
                    }
                }
                // 占用槽位失败
                else
                    p.item = null;                     // clear offer
            }
            // 槽位不可用
            else {
                if (p.bound != b) {                    // stale; reset
                    p.bound = b;
                    p.collides = 0;
                    i = (i != m || m == 0) ? m : m - 1;
                }
                else if ((c = p.collides) < m || m == FULL ||
                        !U.compareAndSwapInt(this, BOUND, b, b + SEQ + 1)) {
                    p.collides = c + 1;
                    i = (i == 0) ? m : i - 1;          // cyclically traverse
                }
                else
                    i = m + 1;                         // grow
                p.index = i;
            }
        }
    }
```

### Exchanger 实现生产者消费者

*程序源自 [Java多线程进阶（二一）—— J.U.C之synchronizer框架：Exchanger](https://segmentfault.com/a/1190000015963932#item-1)。*

```java
import java.util.concurrent.Exchanger;

public class test {
    public static void main(String[] args) {
        Exchanger<Message> exchanger = new Exchanger<>();
        Thread t1 = new Thread(new Consumer(exchanger), "消费者-t1");
        Thread t2 = new Thread(new Producer(exchanger), "生产者-t2");
        t1.start();
        t2.start();
    }
}

class Producer implements Runnable {
    private final Exchanger<Message> exchanger;
    public Producer(Exchanger<Message> exchanger) {
        this.exchanger = exchanger;
    }
    @Override
    public void run() {
        Message message = new Message();
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(1000);
                message.setV(String.valueOf(i));
                System.out.println(Thread.currentThread().getName() + ": 生产了数据[" + i + "]");
                message = exchanger.exchange(message);
                System.out.println(Thread.currentThread().getName() + ": 交换得到数据[" + String.valueOf(message.getV()) + "]");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class Consumer implements Runnable {
    private final Exchanger<Message> exchanger;
    public Consumer(Exchanger<Message> exchanger) {
        this.exchanger = exchanger;
    }
    @Override
    public void run() {
        Message msg = new Message();
        while (true) {
            try {
                Thread.sleep(1000);
                msg = exchanger.exchange(msg);
                System.out.println(Thread.currentThread().getName() + ": 消费了数据[" + msg.getV() + "]");
                msg.setV(null);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class Message {
    String V;
    public String getV() {
        return V;
    }
    public void setV(String v) {
        V = v;
    }
}
```

### 参考

* [Java多线程进阶（二一）—— J.U.C之synchronizer框架：Exchanger](https://segmentfault.com/a/1190000015963932#item-1)
* [ JUC工具类: Exchanger详解](https://www.pdai.tech/md/java/thread/java-thread-x-juc-tool-exchanger.html)
