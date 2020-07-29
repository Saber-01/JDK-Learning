## Semaphore

计数信号量，允许指定数量的线程同时访问某个资源，可以将信号量看做是在向外分发使用资源的许可证，只有成功获取许可证，才能使用资源。


### 内部类 Sync

Semaphore 和 CountDownLatch 基本一样，同样是基于 AQS 实现一个公平/非公平同步器，通过状态值控制锁的获取和释放。

基类 Sync 中在 nonfairTryAcquireShared 函数中实现了非公平版本的 tryAcquire 方法。总体思路较简单，就是自旋获取许可证。状态的值表示许可证数量，如果有剩余可用的许可证，使用 CAS 方式尝试改变状态值，改变成功即获取成功。如果没有剩余可用许可证了，返回一个负数宣告尝试获取失败。

```java
        // 非公平 tryAcquire 的实现
        final int nonfairTryAcquireShared(int acquires) {
            // 自旋
            for (;;) {
                // 可用许可证数量
                int available = getState();
                // 如果当前线程获取成功后剩余的许可证数量
                int remaining = available - acquires;
                // remaining >= 0 才会通过 CAS 改变 state（许可证数）的值
                // remaining < 0 只会返回一个负值
                if (remaining < 0 ||
                        compareAndSetState(available, remaining))
                    return remaining;
            }
        }
```

尝试释放许可证的方法和 release 方法对应，自旋尝试用 CAS 的方式增加 Semaphore 中许可证的数量，增加成功则表示释放成功。

```java
        // 释放资源和许可证（公平版和非公平版相同）
        protected final boolean tryReleaseShared(int releases) {
            for (;;) {
                int current = getState();
                // 如果释放之后，总的可用许可证数量
                int next = current + releases;
                if (next < current) // overflow
                    throw new Error("Maximum permit count exceeded");
                // 改变状态
                if (compareAndSetState(current, next))
                    return true;
            }
        }
```

公平版本的 tryAcquire 在非公平的基础上多了一个步骤，在获取之前，检查当前线程是否有前驱节点，如果有，说明公平模式下当前线程并没有资格获取许可证，返回获取失败。

```java
        // 公平版 tryAcquire
        protected int tryAcquireShared(int acquires) {
            for (;;) {
                // 和公平版本不同的是，需要判断是否有前驱节点，如果有，返回 -1，
                // 即不允许获取
                if (hasQueuedPredecessors())
                    return -1;
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                        compareAndSetState(available, remaining))
                    return remaining;
            }
        }
```

### 成员函数

通常情况下，会使用 acquire 获取一个许可证，使用 release 释放一个许可证。除了这两个方法以外，还有包括等待时间、获取/释放许可证数量、是否相应中断、是否进入同步队列等待等参数的方法可以供程序员调用。它们和普通 Lock 中同名的函数的作用大同小异，此处不做说明，详见[完整源码解析](https://github.com/Augustvic/JavaSourceCodeAnalysis/blob/master/src/JUC/Semaphore.java)。

### 应用实例

信号量中许可证数量设置为 3，仅允许同一时间最多 3 个线程拿到许可证，访问资源。注意此处使用 Executors 创建线程池仅用作示例，实际生产环境中禁止使用。

```java
public class test {
    private static SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
    public static void main(String[] args) {
        // 创建 newFixedThreadPool 仅用作示例
        ExecutorService service = Executors.newFixedThreadPool(7);
        Semaphore semaphore = new Semaphore(3);
        CountDownLatch latch = new CountDownLatch(7);
        System.out.println(Thread.currentThread().getName() + " 在线程池开启之前，查询到当前许可证数量为 " + semaphore.availablePermits());
        for (int i = 0; i < 7; i++) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        semaphore.acquire();
                        System.out.println(Thread.currentThread().getName() + " 获取到许可证，当前时间为 " + df.format(new Date()));
                        Thread.sleep(1000);
                        System.out.println(Thread.currentThread().getName() + " 准备释放许可证，释放前查询到当前可使用许可证数量为 " + semaphore.availablePermits());
                        System.out.println(Thread.currentThread().getName() + " 准备释放许可证，当前时间为 " + df.format(new Date()));
                        semaphore.release();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            };
            service.execute(task);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            service.shutdown();
        }
        System.out.println(Thread.currentThread().getName() + " 在线程池关闭之后，查询到当前许可证数量为 " + semaphore.availablePermits());
    }
}
```

### 实现消费者生产者模式

```java
public class test {
    public static void main(String[] args) {
        SharedData shared = new SharedData();

        Thread producer1 = new Thread(new Producer(shared),"producer1");
        Thread producer2 = new Thread(new Producer(shared),"producer2");
        Thread consumer1 = new Thread(new Consumer(shared),"consumer1");
        Thread consumer2 = new Thread(new Consumer(shared),"consumer2");

        consumer1.start();
        consumer2.start();
        producer1.start();
        producer2.start();
    }

    private static test t = new test();

    static class SharedData {
        private LinkedList<Integer> l = new LinkedList<>();
        private Lock lock = new ReentrantLock();
        private Semaphore semProducer = new Semaphore(3);
        private Semaphore semConsumer = new Semaphore(0);
        Random rn = new Random();

        public void get() throws InterruptedException{
            try {
                semConsumer.acquire();
                lock.lock();
                int val = l.removeFirst();
                System.out.println(Thread.currentThread().getName() + " 正在消费数据为：" + val + "    缓冲区目前大小为：" + l.size());
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
                semProducer.release();
            }
        }

        public void put() throws InterruptedException{
            try {
                semProducer.acquire();
                lock.lock();
                int val = rn.nextInt(100);
                l.add(val);
                System.out.println(Thread.currentThread().getName() + " 正在生产数据为：" + val + "    缓冲区目前大小为：" + l.size());
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
                semConsumer.release();
            }
        }
    }

    static class Consumer implements Runnable{
        private SharedData data;
        public Consumer(SharedData data) {
            this.data = data;
        }
        @Override
        public void run() {
            for(int i = 0; i < 10; i++) {
                try {
                    data.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class Producer implements Runnable{
        private SharedData data;
        public Producer(SharedData data) {
            this.data = data;
        }
        @Override
        public void run() {
            for(int i = 0; i < 10; i++) {
                try {
                    data.put();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

### 其他场景

* Semaphore 初始化有 1 个令牌，线程调用 1 次 release 方法（没有 acquire），然后其他线程一次性获取 2 个令牌，是可以成功获取到的。因为 release 会无条件添加令牌，不会管初始的令牌数是多少。
