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
 * Written by Josh Bloch of Google Inc. and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/.
 */

package java.util;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * Resizable-array implementation of the {@link Deque} interface.  Array
 * deques have no capacity restrictions; they grow as necessary to support
 * usage.  They are not thread-safe; in the absence of external
 * synchronization, they do not support concurrent access by multiple threads.
 * Null elements are prohibited.  This class is likely to be faster than
 * {@link Stack} when used as a stack, and faster than {@link LinkedList}
 * when used as a queue.
 *Deque接口的大小可调整数组的实现。Array deques 没有严格的容量限制；
 * 可以根据需要增长。它们不是线程安全的类，在缺乏外部同步的情况下，
 *  它们不支持多线程同时访问。禁止 null 元素。这个类作为堆栈的时候比
 * Stack 要快，作为队列的时候比 LinkedList 要快。
 *
 * <p>Most {@code ArrayDeque} operations run in amortized constant time.
 * Exceptions include {@link #remove(Object) remove}, {@link
 * #removeFirstOccurrence removeFirstOccurrence}, {@link #removeLastOccurrence
 * removeLastOccurrence}, {@link #contains contains}, {@link #iterator
 * iterator.remove()}, and the bulk operations, all of which run in linear
 * time.
 * ArrayDeque 支持的大多数运算都是在常数时间内运行。remove,
 *  removeFirstOccurrence, removeLastOccurrence, contains,
 *  iterator.remove() 和批量操作在线性时间内完成。
 *
 * <p>The iterators returned by this class's {@code iterator} method are
 * <i>fail-fast</i>: If the deque is modified at any time after the iterator
 * is created, in any way except through the iterator's own {@code remove}
 * method, the iterator will generally throw a {@link
 * ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
 * 这个类的 iterator 方法支持 fail-fast：在迭代器创建之后如果队列被修改，
 *  除非是迭代器自身的 remove 方法，否则迭代器会抛出
 *  ConcurrentModificationException 异常。因此，在面对并发修改的时候，
 *  迭代器会快速干净地失败，而不会在未来不确定的时间出现未知风险和
 *  不确定的行为。
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 * 注意，迭代器的 fast-fail 行为无法得到保证，因为一般来说，不可能对是否出现
 *   不同步并发修改做出任何硬性保证。fast-fail 迭代器会尽最大努力抛出
 *   ConcurrentModificationException。因此，为提高这类迭代器的正确性而编写
 * 一个依赖于此异常的程序是错误的做法：迭代器的 fast-fail 行为应该仅用于
 * 检测bug。
 *
 * <p>This class and its iterator implement all of the
 * <em>optional</em> methods of the {@link Collection} and {@link
 * Iterator} interfaces.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *这个类是 Java Collections Framework 的成员.
 *
 * @Saber-01 这个类中重要的函数有计算大小的 calculateSize，删除指定索引
 *  位置元素的 delete
 * @author  Josh Bloch and Doug Lea
 * @since   1.6
 * @param <E> the type of elements held in this collection
 */
//@Saber-01 注意队列的每个操作都要维护head和tail，添加操作还要判断是否需要扩容。
    //而因为tail原先上就没有值，而head上原先有值，
    //所以对队列头部和尾部的大部分操作，head总是先取值，再维护，
    //而tail则是先维护后，再取值。
public class ArrayDeque<E> extends AbstractCollection<E>
        implements Deque<E>, Cloneable, Serializable
{
    /**
     * The array in which the elements of the deque are stored.
     * The capacity of the deque is the length of this array, which is
     * always a power of two. The array is never allowed to become
     * full, except transiently within an addX method where it is
     * resized (see doubleCapacity) immediately upon becoming full,
     * thus avoiding head and tail wrapping around to equal each
     * other.  We also guarantee that all array cells not holding
     * deque elements are always null.
     *  队列的元素都存储在这个数组里。队列的容量就是这个数组的长度，
     *  其长度总是 2 的幂。数组永远不允许变成满的，除非
     *  是在 addX 方法中。当数组变成满的时候，它会立刻调整大小 （参阅
     *  doubleCapacity），这样就避免了头和尾互相缠绕，使其相等。我们还
     *  保证所有不包含元素的数组单元格始终为 null。
     */
    transient Object[] elements; //  非私有成员，以简化嵌套类的访问。

    /**
     * The index of the element at the head of the deque (which is the
     * element that would be removed by remove() or pop()); or an
     * arbitrary number equal to tail if the deque is empty.
     * 队列头部元素的索引（该元素将被 remove 或者 pop 删除）；如果
     * 队列为空，将会是等于 tail 的数。
     * 如果队列不为空，则这个索引上有值。
     */
    transient int head;

    /**
     * The index at which the next element would be added to the tail
     * of the deque (via addLast(E), add(E), or push(E)).
     * 队列尾部索引，将下一个元素添加到该索引的下一个位置（通过 addLast(E)，
     *  add(E)，或者 push(E)）。
     *  队列尾部指的是，队列中存储的最后一个元素的下一个位置，
     *  即实际数组中tail索引位置上并没有值
     * 这也是为什么说数组永远不允许满。除非是在addX方法中短暂存在，因为它会马上扩容
     */
    transient int tail;

    /**
     * The minimum capacity that we'll use for a newly created deque.
     * Must be a power of 2.
     *  一个新创建的队列的最小容量。必须是 2 的幂。2^3，
     */
    private static final int MIN_INITIAL_CAPACITY = 8;

    // ******  Array allocation and resizing utilities ******
    // 数组空间分配和再分配工具
    //只在构造函数和readObject中使用。
    /**
     * Allocates empty array to hold the given number of elements.
     * 分配可以容纳指定个数元素的空数组，主要是确定新数组的容量。
     *
     * 计算容量，大于 numElement 且 2 的整数次方的最小的数
     *     比如，3 算出来是 8，9 算出来是 16，33 算出来是 64
     * @param numElements  the number of elements to hold
     */
    private void allocateElements(int numElements) {
        int initialCapacity = MIN_INITIAL_CAPACITY;
        // Find the best power of two to hold elements.
        //找到一个最合适的2的幂当做数组容量。
        // Tests "<=" because arrays aren't kept full.
        //取=号是因为，数组不可能保持满状态
        //如果指定容量都比最小的初始容量小，那么它没必要进行重新分配。
        if (numElements >= initialCapacity) {
            initialCapacity = numElements;
            initialCapacity |= (initialCapacity >>>  1);
            initialCapacity |= (initialCapacity >>>  2);
            initialCapacity |= (initialCapacity >>>  4);
            initialCapacity |= (initialCapacity >>>  8);
            initialCapacity |= (initialCapacity >>> 16);   //到这一步会将initialCapacity最高位为1的后面位数全变为1
            initialCapacity++; //加1则，前面一位进位为1，后面全部变为0，即变成大于initialCapacity的2的幂。

            //为了防止进行这一波操作之后，得到了负数，即原来第31位为1，
            // 得到的结果第32位将为1，第32位为符号位，1代表负数，
            // 这样的话就必须回退一步，将得到的数右移一位（即2 ^ 30）
            if (initialCapacity < 0)   // Too many elements, must back off
                initialCapacity >>>= 1;// Good luck allocating 2 ^ 30 elements
        }
        elements = new Object[initialCapacity];   //创建了一个新容量的数组。
    }

    /**
     * Doubles the capacity of this deque.  Call only when full, i.e.,
     * when head and tail have wrapped around to become equal.
     *将队列容量设置为当前的两倍，当队列满时调用，即 head 和 tail
     *   相遇的时候，即只在head和tail相等时调用。
     *   扩容函数。
     */
    //原理很简单：因为当前数组为满状态，只要将head右边的数移动到新建数组的最开始的地方，
    //即初始化head为0，然后将原来数组head左边的数(个数为head的值)全部右移，移动的步数就是上一步左移的元素个数。
    //即初始化tail为n,即完成了扩容。
    private void doubleCapacity() {
        // assert 如果表达式为 true 则继续执行，如果为 false 抛出
        // AssertionError，并终止执行
        assert head == tail;     //断言head==tail,只有head==tail才会进行执行
        int p = head;                       //存储队列头部索引
        int n = elements.length;            //存储底部数组的长度，即容量
        int r = n - p; // 相减得到了p右边的元素个数r
        int newCapacity = n << 1;      //新容量为旧容量的2倍
        if (newCapacity < 0)       //如果溢出，2的31次方已经溢出，变为负数，
            throw new IllegalStateException("Sorry, deque too big");  //抛出队列太大的异常。
        Object[] a = new Object[newCapacity];     //创建新数组，
        //将原来数组从p(包括p)之后的r个元素全部左移到新数组的开始位置0，
        System.arraycopy(elements, p, a, 0, r);
        //将原来数组从0到p(不包括p)的p个元素全部右移到新数组的r位置。
        System.arraycopy(elements, 0, a, r, p);
        elements = a;        //新数组赋值给队列底层数组
        head = 0;        //初始化队列头索引
        tail = n;     //初始化队列尾索引
    }

    /**
     * Copies the elements from our element array into the specified array,
     * in order (from first to last element in the deque).  It is assumed
     * that the array is large enough to hold all elements in the deque.
     *按顺序（从队列的第一个元素到最后一个元素） 将元素数组中的元素
     *  复制到指定的数组中。假设数组足够大，可以容纳队列中所有元素。
     *
     * @return its argument
     */
    private <T> T[] copyElements(T[] a) {
        if (head < tail) {      //如果head在tail之前，则可以一次复制即可
            //将队列底层数组从head开始的size()个全部元素转移到指定数组a的起始位置0
            System.arraycopy(elements, head, a, 0, size());
        } else if (head > tail) {       //如果head在tail之后，则要分两次复制。
            //因为这时数组只有[tail,head)上没有元素值，
            //所以可以将head之后的值左移
            //将tail之前的值右移
            int headPortionLen = elements.length - head;  //记录head右边有多少个值
            System.arraycopy(elements, head, a, 0, headPortionLen); //左移
            System.arraycopy(elements, 0, a, headPortionLen, tail);    //右移
        }
        return a;           //返回新数组。
    }

    /**
     * Constructs an empty array deque with an initial capacity
     * sufficient to hold 16 elements.
     * 构造一个容量为 16 的空队列
     */
    public ArrayDeque() {       //无参构造函数，默认的底层数组大小为16.
        elements = new Object[16];
    }

    /**
     * Constructs an empty array deque with an initial capacity
     * sufficient to hold the specified number of elements.
     *构造初始容量为指定大小的空队列。
     *
     * @param numElements  lower bound on initial capacity of the deque
     */
    public ArrayDeque(int numElements) { //如果指定初始容量小于8，将会返回容量为8的新数组。
        allocateElements(numElements);   //调用allocateElements方法，分配新数组
    }

    /**
     * Constructs a deque containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.  (The first element returned by the collection's
     * iterator becomes the first element, or <i>front</i> of the
     * deque.)
     * 构造一个包含指定集合所有元素的队列，按照集合迭代器返回的顺序。
     *   （集合迭代器返回的第一个元素作为队列第一个元素，或者队列的 front）
     * @param c the collection whose elements are to be placed into the deque
     * @throws NullPointerException if the specified collection is null
     */
    public ArrayDeque(Collection<? extends E> c) {
        allocateElements(c.size());//调用allocateElements方法，分配新数组
        addAll(c);                          //然后调用addAll函数，此方法在子类AbstractCollection中，
    }

    // The main insertion and extraction methods are addFirst,
    // addLast, pollFirst, pollLast. The other methods are defined in
    // terms of these.
    // 最核心的插入和提取方法是 addFirst，addLast，pollFirst，pollLast。
    // 其他方法根据这些来定义。
    /**
     * Inserts the specified element at the front of this deque.
     *在队列头部插入指定值，
     * @param e the element to add
     * @throws NullPointerException if the specified element is null
     */
    public void addFirst(E e) {
        if (e == null)
            throw new NullPointerException();
        //当前head处是存在值的，所以插入前，需要先移动head指针。
        //与底层数组长度-1做与运算，在数组长度为2的幂的情况下，相当于取模(模为数组长度)的运算。
        //与hashmap散列的hash算法是一个道理。
        //正常非循环数组实现的队列的话，存在索引越界的情况，
        // 而ArrayDeque之所以是循环数组实现，就是因为有head = (head - 1) & (elements.length - 1)的运算
        //elements.length是2的幂，即是一个最高位为1，右边全为0的数，(如：16=10000b)
        //2的幂减一以后为最高位1后面全为1的数，如：16-1=15=1111b
        //小于length的非负数和这样的数做位与运算，显然是本身，即head-1>=0时，head-1 不变。
        //如果此时head已经为0到达数组头部，head-1就为负数-1，
        //而此时-1的在计算机中二进制位11....1111，全为1，和前面所说的000....01111这样的数做位与时
        //得到的是000....01111，即事例中数组长度为16的情况下，此时head-1=-1时候，head会赋值为15。
        //即实现了head到达0以后不越界，而是跳到数组末尾去，这就是循环数组的原理。

        elements[head = (head - 1) & (elements.length - 1)] = e;    //将更新后的head索引出放入指定值
        if (head == tail)       //判断队列是否满
            doubleCapacity();  //扩容函数只在head==tail时候调用。
    }

    /**
     * Inserts the specified element at the end of this deque.
     * 把指定元素添加到队列末尾。
     * <p>This method is equivalent to {@link #add}.
     *
     * @param e the element to add
     * @throws NullPointerException if the specified element is null
     */
    public void addLast(E e) {
        if (e == null)
            throw new NullPointerException();
        elements[tail] = e;      //tail处原来就没值，所以直接赋值，
        //和上面的原理类似，可理解为tail+1做模为数组长度的模运算
        //即越界时，tail+1==数组长度，此时tail会赋值为0。跳到数组头部
        if ( (tail = (tail + 1) & (elements.length - 1)) == head)    //判断队列是否满
            doubleCapacity();
    }

    /**
     * Inserts the specified element at the front of this deque.
     *把指定元素插入到队列开头。
     *   添加成功返回 true。
     * @param e the element to add
     * @return {@code true} (as specified by {@link Deque#offerFirst})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    /**
     * Inserts the specified element at the end of this deque.
     *把指定元素添加到队列末尾。
     * 添加成功返回 true。
     * @param e the element to add
     * @return {@code true} (as specified by {@link Deque#offerLast})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     * 删除第一个元素并返回该元素。
     *元素为空抛出异常。
     */
    public E removeFirst() {
        E x = pollFirst();
        if (x == null)
            throw new NoSuchElementException();
        return x;
    }

    /**
     * 删除最后一个元素并返回该元素。
     * 元素为空抛出异常。
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E removeLast() {
        E x = pollLast();
        if (x == null)
            throw new NoSuchElementException();
        return x;
    }

    // 删除第一个元素。（将该元素设置为 null）
    // 元素为空返回 null。
    public E pollFirst() {
        int h = head;      //记录队列头索引
        @SuppressWarnings("unchecked")
        E result = (E) elements[h];           //取的队列头部元素
        // Element is null if deque empty
        if (result == null)    //如果数组为空
            return null;//返回空
        elements[h] = null;     // Must null out slot 将删除的位置上填上null
        head = (h + 1) & (elements.length - 1);    //维护head
        return result;      //返回记录的值
    }

    // 删除最后一个元素。（将该元素设置为 null）
    // 元素为空返回 null。
    public E pollLast() {
        int t = (tail - 1) & (elements.length - 1);  //tail原来没有值，所以需要减1，
        @SuppressWarnings("unchecked")
        E result = (E) elements[t];    //取得队列尾值，
        if (result == null)        //如果队列为空
            return null;
        elements[t] = null;       //不为空，则删除位置 赋值为null
        tail = t;        //维护tail
        return result;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     *  返回队列的第一个元素。
     *  该元素为空抛出异常。
     *
     */
    public E getFirst() {
        @SuppressWarnings("unchecked")
        E result = (E) elements[head];   //直接去head位置上的值
        if (result == null)
            throw new NoSuchElementException();
        return result;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     * 返回队列的最后一个元素。
     *  该元素为空抛出异常。
     */
    public E getLast() {
        @SuppressWarnings("unchecked")
        E result = (E) elements[(tail - 1) & (elements.length - 1)];//因为tail原先没有值，所以要先减1。
        if (result == null)
            throw new NoSuchElementException();
        return result;
    }

    // 返回队列的第一个元素,为空不抛出异常。返回null
    @SuppressWarnings("unchecked")
    public E peekFirst() {
        // elements[head] is null if deque empty
        return (E) elements[head];
    }
    // 返回队列的最后一个元素,为空不抛出异常。返回null
    @SuppressWarnings("unchecked")
    public E peekLast() {
        return (E) elements[(tail - 1) & (elements.length - 1)];
    }

    /**
     * Removes the first occurrence of the specified element in this
     * deque (when traversing the deque from head to tail).
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * {@code o.equals(e)} (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     * 删除队列中指定元素的第一个出现项（从头到尾遍历）。
     *如果队列不包含该元素，不作出任何改变。
     * 如果队列包含指定元素返回 true。
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     */
    public boolean removeFirstOccurrence(Object o) {
        if (o == null)              //因为队列不含null，所以为null，直接返回false
            return false;
        int mask = elements.length - 1;   //做与运算用得。
        int i = head;          //从head开始遍历
        Object x;
        while ( (x = elements[i]) != null) {  //直到碰到null。
            if (o.equals(x)) {
                delete(i);            //调用delete方法进行删除。
                return true;
            }
            i = (i + 1) & mask;       //i+1,做与时保证不越界。
        }
        return false;
    }

    /**
     * Removes the last occurrence of the specified element in this
     * deque (when traversing the deque from head to tail).
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the last element {@code e} such that
     * {@code o.equals(e)} (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     *  删除队列中指定元素的最后一个出现项（从尾到头遍历）。
     *  如果队列不包含该元素，不作出任何改变。
     *  如果队列包含指定元素返回 true。
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     */
    public boolean removeLastOccurrence(Object o) {
        if (o == null)
            return false;
        int mask = elements.length - 1;
        int i = (tail - 1) & mask;   //原先tail没有值，所以移动到最后一个元素索引上
        Object x;
        while ( (x = elements[i]) != null) {    //直到碰到null。
            if (o.equals(x)) {    //使用equals判断
                delete(i);       //调研delete删除
                return true;
            }
            i = (i - 1) & mask;    //i-1,
        }
        return false;
    }

    // *** Queue methods ***
// 队列相关方法
    /**
     * Inserts the specified element at the end of this deque.
     *把指定元素插入到队列尾部
     * <p>This method is equivalent to {@link #addLast}.
     *此方法等价于 addLast
     * @param e the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        addLast(e);
        return true;
    }

    /**
     * Inserts the specified element at the end of this deque.
     *把指定元素插入到队列尾部
     * <p>This method is equivalent to {@link #offerLast}.
     *此方法等价于 offerLast
     * @param e the element to add
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        return offerLast(e);
    }

    /**
     * Retrieves and removes the head of the queue represented by this deque.
     * 检索并删除队列的头部元素
     * This method differs from {@link #poll poll} only in that it throws an
     * exception if this deque is empty.
     * 此方法和 poll 的区别只有：如果队列为空它会抛出异常
     * <p>This method is equivalent to {@link #removeFirst}.
     *此方法等价于 removeFirst
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E remove() {
        return removeFirst();
    }

    /**
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque), or returns
     * {@code null} if this deque is empty.
     *检索并删除队列的头部元素（即队列的第一个元素），如果队列为空
     *  返回 null。
     * <p>This method is equivalent to {@link #pollFirst}.
     *此方法等价于 pollFirst。
     * @return the head of the queue represented by this deque, or
     *         {@code null} if this deque is empty
     */
    public E poll() {
        return pollFirst();
    }

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque.  This method differs from {@link #peek peek} only in
     * that it throws an exception if this deque is empty.
     *检索但不删除队列的头部元素。这个方法和 peek 不同的地方只有：如果
     *   队列为空会抛出异常。
     * <p>This method is equivalent to {@link #getFirst}.
     *这个方法等价于 getFirst。
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E element() {
        return getFirst();
    }

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque, or returns {@code null} if this deque is empty.
     *检索但不删除队列的头部元素，如果队列为空返回 null。
     * <p>This method is equivalent to {@link #peekFirst}.
     *此方法等价于 peekFirst。
     * @return the head of the queue represented by this deque, or
     *         {@code null} if this deque is empty
     */
    public E peek() {
        return peekFirst();
    }

    // *** Stack methods ***
    // 堆栈相关操作
    /**
     * Pushes an element onto the stack represented by this deque.  In other
     * words, inserts the element at the front of this deque.
     * 把元素 push 到队列代表的堆栈里面。换句话说，把元素插入到队列头部。
     * <p>This method is equivalent to {@link #addFirst}.
     * 此方法等价于 addFirst。
     * @param e the element to push
     * @throws NullPointerException if the specified element is null
     */
    public void push(E e) {
        addFirst(e);
    }

    /**
     * Pops an element from the stack represented by this deque.  In other
     * words, removes and returns the first element of this deque.
     *对队列所代表的的堆栈进行 pop 操作。换句话说，删除并返回队列的
     *  第一个元素。
     * <p>This method is equivalent to {@link #removeFirst()}.
     *此方法等价于 removeFirst。
     * @return the element at the front of this deque (which is the top
     *         of the stack represented by this deque)
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E pop() {
        return removeFirst();
    }
    //检查不变性，
    private void checkInvariants() {
        assert elements[tail] == null;   //检查tail
        assert head == tail ? elements[head] == null :     //tail==head时，队列必须为空，
                (elements[head] != null &&             //不为空，则head上必须有值，且tail前一个位置必须有值。
                        elements[(tail - 1) & (elements.length - 1)] != null);
        assert elements[(head - 1) & (elements.length - 1)] == null;       //head前一个位置必须无值。
    }

    /**
     * Removes the element at the specified position in the elements array,
     * adjusting head and tail as necessary.  This can result in motion of
     * elements backwards or forwards in the array.
     * 删除指定位置的元素，根据需要调整 head 和 tail。这可能导致数组中
     *  的元素向后或向前移动。
     * <p>This method is called delete rather than remove to emphasize
     * that its semantics differ from those of {@link List#remove(int)}.
     * 这个方法被称为 delete 而不是 remove，是为了强调它的语义和
     * remove 不同。
     *
     * @return true if elements moved backwards
     */
    private boolean delete(int i) {
        checkInvariants();        //检查不变性
        final Object[] elements = this.elements;    //得到当前队列底层数组
        final int mask = elements.length - 1;       //掩码，用于做位与运算，
        final int h = head;        //保存队列头
        final int t = tail;           //保存队列尾
        final int front = (i - h) & mask;      //计算当前位置到队列头的距离，等同于之间的元素个数
        final int back  = (t - i) & mask;     //计算当前位置到队列尾的距离，等同于之间的元素个数+1，因为tail上为null。

        // Invariant: head <= i < tail mod circularity
        //不变的情况下，i肯定夹在head和tail中间，即front肯定会小于队列头到尾的距离。
        if (front >= ((t - h) & mask))    //大于等于的话，就抛出异常
            throw new ConcurrentModificationException();

        // Optimize for least element motion
        //// 为移动尽量少的元素做优化，如果离头部比较近，
        // 则将该位置到头部的元素进行移动，如果离尾部比较近，
        // 则将该位置到尾部的元素进行移动。
        if (front < back) {    //离头部更近，则移动head到i之间的元素。
            //因为是队列是循环数组实现，所以此处还需要判断是移动1次还是2次。
            if (h <= i) {       //如果指定位置在头部的后面，只需要[head,i)上的元素右移一位。
                System.arraycopy(elements, h, elements, h + 1, front);
            } else { // Wrap around    //如果已经环绕，i 在头部前面
                //先将[0,i)的元素右移一位，
                System.arraycopy(elements, 0, elements, 1, i);
                //后将底层数组最后一个元素赋值给底层数组第一个元素，
                elements[0] = elements[mask];
                //最后将[head,mask)上的mask-h个元素右移一位。
                System.arraycopy(elements, h, elements, h + 1, mask - h);
            }
            elements[h] = null; //因为移动的是靠近头部的元素，所以需要维护head。
            head = (h + 1) & mask;   //维护head
            return false;
        } else {                  //如果离尾部更近，移动i到tail之间的元素。
            if (i < t) { // Copy the null tail as well   //没出现环绕。只需一次向左移动。
                System.arraycopy(elements, i + 1, elements, i, back);
                tail = t - 1;  //维护tail。
            } else { // Wrap around    //出现环绕要移动2次。
                //首先将[i+1,mask]的一共mast-i个元素左移一格
                System.arraycopy(elements, i + 1, elements, i, mask - i);
                //将数组索引0位置的元素赋值给mask位置即底层数组末尾
                elements[mask] = elements[0];
                System.arraycopy(elements, 1, elements, 0, t);
                tail = (t - 1) & mask;    //维护tail
            }
            return true;
        }
    }

    // *** Collection Methods ***
    // 集合相关的方法
    /**
     * Returns the number of elements in this deque.
     *返回队列中的元素
     * @return the number of elements in this deque
     */
    public int size() {
        return (tail - head) & (elements.length - 1);
    }

    /**
     * Returns {@code true} if this deque contains no elements.
     *如果队列不包含任何元素返回 true。
     * @return {@code true} if this deque contains no elements
     */
    public boolean isEmpty() {     //为空时，head==tail
        return head == tail;
    }
// 队列迭代器
    /**
     * Returns an iterator over the elements in this deque.  The elements
     * will be ordered from first (head) to last (tail).  This is the same
     * order that elements would be dequeued (via successive calls to
     * {@link #remove} or popped (via successive calls to {@link #pop}).
     *返回一个遍历队列中的值的迭代器。
     * 这些值是从队列头到队列尾进行排序的。
     * 这个顺序也是remove和pop的顺序。
     * @return an iterator over the elements in this deque
     */
    public Iterator<E> iterator() {//返回双端队列迭代器
        return new DeqIterator();
    }

    public Iterator<E> descendingIterator() { //返回倒序的双端队列迭代器
        return new DescendingIterator();
    }

    private class DeqIterator implements Iterator<E> {  //队列迭代器类
        /**
         * Index of element to be returned by subsequent call to next.
         *要由对next的后续调用返回的元素的索引
         */
        private int cursor = head;

        /**
         * Tail recorded at construction (also in remove), to stop
         * iterator and also to check for comodification.
         * 创建和删除迭代器时记录尾部位置，为了能停止迭代器以及检查并发修改。
         */
        private int fence = tail;

        /**
         * Index of element returned by most recent call to next.
         * Reset to -1 if element is deleted by a call to remove.
         * 上一个返回元素的索引。如果删除元素，则 lastRet 设置为 -1。
         */
        private int lastRet = -1;     //最后一次遍历的元素的位置

        public boolean hasNext() {    //判断是否有下一个元素，
            return cursor != fence;  //即还没到末尾
        }

        public E next() {     //返回下一个队列中的元素。会更新lastRet,cursor
            if (cursor == fence)     //如果已经到达末尾，抛出异常
                throw new NoSuchElementException();
            @SuppressWarnings("unchecked")
            E result = (E) elements[cursor];       //取出cursor对应的值
            // This check doesn't catch all possible comodifications,
            // but does catch the ones that corrupt traversal
            //此检查不会捕获所有可能的共修改，但会捕获损坏遍历的那些
            if (tail != fence || result == null)  //再次检查
                throw new ConcurrentModificationException();
            lastRet = cursor;   //最后返回的元素位置更新为cursor
            cursor = (cursor + 1) & (elements.length - 1);     //cursor加1，注意循环数组中，都要做与运算
            return result;
        }

        public void remove() {
            if (lastRet < 0)      //如果迭代器才发生结构修改的操作或者初始还未开始遍历，lastRet就为-1
                throw new IllegalStateException();
            //delete返回true，说明移动的是靠近尾部的元素。则需要将cursor往左移动一位。避免丢失元素
            if (delete(lastRet)) { // if left-shifted, undo increment in next() 如果左移，则撤消next（）中的增量
                cursor = (cursor - 1) & (elements.length - 1);  //删除了一个，底层数组会左移，所以cursor要自减，
                fence = tail;        //更新队列尾部、
            }
            lastRet = -1;
        }
        // 从 cursor 开始的遍历，对每一个队列中的元素执行action操作，即迭代器支持lambda
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            Object[] a = elements;
            int m = a.length - 1, f = fence, i = cursor;
            cursor = f;
            while (i != f) {
                @SuppressWarnings("unchecked") E e = (E)a[i];
                i = (i + 1) & m;
                if (e == null)
                    throw new ConcurrentModificationException();
                action.accept(e);
            }
        }
    }

    private class DescendingIterator implements Iterator<E> {
        /*
         * This class is nearly a mirror-image of DeqIterator, using
         * tail instead of head for initial cursor, and head instead of
         * tail for fence.
         * 倒序从尾结点开始的遍历。
         */
        private int cursor = tail;   //相反，cursor从tail开始。
        private int fence = head;      //fence变成head。
        private int lastRet = -1;

        public boolean hasNext() {
            return cursor != fence;          //未到head。因为next中会提前将cursor减一再取值。
        }

        public E next() {        //返回下一个元素
            if (cursor == fence)           //如果到了列表头。抛出异常
                throw new NoSuchElementException();
            cursor = (cursor - 1) & (elements.length - 1);  //倒序，这里是减一，而且在取值前。
            @SuppressWarnings("unchecked")
            E result = (E) elements[cursor];
            if (head != fence || result == null)  //再次检查
                throw new ConcurrentModificationException();
            lastRet = cursor;
            return result;
        }

        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            if (!delete(lastRet)) {   //如果移动的是靠近头部的元素。即右移
                cursor = (cursor + 1) & (elements.length - 1); //那么倒序的话，需要将cursor往右后退一个，即+1.
                fence = head;    //更新头部
            }
            lastRet = -1;
        }
    }

    /**
     * Returns {@code true} if this deque contains the specified element.
     * More formally, returns {@code true} if and only if this deque contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *如果队列包含指定元素返回 true。
     *
     * @param o object to be checked for containment in this deque
     * @return {@code true} if this deque contains the specified element
     */
    public boolean contains(Object o) {
        if (o == null)              //如果null,直接返回false，因为队列不允许值为null
            return false;
        int mask = elements.length - 1;      //掩码。用于出现环绕的情况
        int i = head;              //从队列头开始遍历
        Object x;
        while ( (x = elements[i]) != null) {      //为null才退出循环
            if (o.equals(x))      //使用对象的equals判断
                return true;
            i = (i + 1) & mask;           //i++，只是作位与运算，因为循环数组
        }
        return false;        //，没找到返回false
    }

    /**
     * Removes a single instance of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * {@code o.equals(e)} (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     ** 从队列中删除指定元素的第一个实例。如果队列不包含该元素不作出任何
     *  改变。如果包含指定元素返回 true。
     * <p>This method is equivalent to {@link #removeFirstOccurrence(Object)}.
     *此方法等价于 removeFirstOccurrence。
     * @param o element to be removed from this deque, if present
     * @return {@code true} if this deque contained the specified element
     */
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    /**
     * Removes all of the elements from this deque.
     * The deque will be empty after this call returns.
     * 从队列中删除所有元素。此方法调用后队列为空。
     */
    public void clear() {
        int h = head;     //存储队列头
        int t = tail;       //存储队列尾
        if (h != t) { // clear all cells  //如果队列不为空
            head = tail = 0;      //初始化head和tail为0
            int i = h;                //从i=h=原来的队列头开始遍历
            int mask = elements.length - 1;
            do {
                elements[i] = null;      //使数组的每一个位置都是null。为了GC
                i = (i + 1) & mask;             //循环数组版的i++
            } while (i != t);    //直到原来的队列尾部
        }
    }

    /**
     * Returns an array containing all of the elements in this deque
     * in proper sequence (from first to last element).
     *返回一个包含队列所有元素的数组，顺序为从第一个元素到最后一个元素。
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this deque.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *返回的数组是“安全”的，因为队列不会保留任何对它的引用。即该数组保存
     *在新分配的内存空间里。调用者可以任意修改返回的数组。
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *   //调用copyElements，参数为新建的Object类型长度为size的数组。
     * @return an array containing all of the elements in this deque
     */
    public Object[] toArray() {
        return copyElements(new Object[size()]);
    }

    /**
     * Returns an array containing all of the elements in this deque in
     * proper sequence (from first to last element); the runtime type of the
     * returned array is that of the specified array.  If the deque fits in
     * the specified array, it is returned therein.  Otherwise, a new array
     * is allocated with the runtime type of the specified array and the
     * size of this deque.
     *返回一个按正确的顺序包含 deque 中的所有元素的数组
     *（从第一个元素到最后一个元素）；返回数组的运行时类型是指定数组的
     *  运行时类型。如果指定的数组能容纳队列的所有元素，则返回指定数组。
     *  否则，将按照指定数组的运行时类型和该 deque 的大小分配一个新数组。
     *
     * <p>If this deque fits in the specified array with room to spare
     * (i.e., the array has more elements than this deque), the element in
     * the array immediately following the end of the deque is set to
     * {@code null}.
     *如果指定的数组还有多余的空间（即指定数组比列表有更多的元素），在数组
     * 中紧跟队列末尾的元素被设置为 null。
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *这个方法充当基于数组和基于集合API的桥梁（集合与数组的转换）。
     *   此外，该方法允许精确控制输出数组的运行时类型，并且在某些情况
     *  下可以用于节省分配成本。
     * <p>Suppose {@code x} is a deque known to contain only strings.
     * The following code can be used to dump the deque into a newly
     * allocated array of {@code String}:
     *
     *  <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the deque are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this deque
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this deque
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        int size = size();   //得到数组的元素个数
        if (a.length < size)     //如果目标数组不能容纳队列所有元素
            a = (T[])java.lang.reflect.Array.newInstance(      //创建一个新的size长度的类型为目标数组a相同类型的新数组
                    a.getClass().getComponentType(), size);
        copyElements(a);       //调用copyElements，
        if (a.length > size)   //在紧接着队列元素的后面设置为null
            a[size] = null;
        return a;
    }

    // *** Object methods ***
    // Object 相关操作
    /**
     * Returns a copy of this deque.
     ** 返回队列的克隆
     * @return a copy of this deque
     */
    public ArrayDeque<E> clone() {
        try {
            @SuppressWarnings("unchecked")
            ArrayDeque<E> result = (ArrayDeque<E>) super.clone(); //调用Object的clone方法
            result.elements = Arrays.copyOf(elements, elements.length);   //再使用Array.copyOf方法复制并返回新数组
            return result;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    private static final long serialVersionUID = 2340985798034038923L;
           //序列化用，用于比较版本，对类功能没有意义。
    /**
     * Saves this deque to a stream (that is, serializes it).
     *把队列存储在 stream 里，即序列化。
     * @serialData The current size ({@code int}) of the deque,
     * followed by all of its elements (each an object reference) in
     * first-to-last order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        s.defaultWriteObject();

        // Write out size
        s.writeInt(size());

        // Write out elements in order.
        int mask = elements.length - 1;
        for (int i = head; i != tail; i = (i + 1) & mask)
            s.writeObject(elements[i]);
    }

    /**
     * Reconstitutes this deque from a stream (that is, deserializes it).
     * 反序列化。
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();

        // Read in size and allocate array
        int size = s.readInt();
        allocateElements(size);
        head = 0;
        tail = size;

        // Read in all elements in the proper order.
        for (int i = 0; i < size; i++)
            elements[i] = s.readObject();
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * deque.
     *  分裂迭代器。支持快速失败。
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, {@link Spliterator#ORDERED}, and
     * {@link Spliterator#NONNULL}.  Overriding implementations should document
     * the reporting of additional characteristic values.
     *
     * @return a {@code Spliterator} over the elements in this deque
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return new DeqSpliterator<E>(this, -1, -1);
    }

    static final class DeqSpliterator<E> implements Spliterator<E> {
        private final ArrayDeque<E> deq;
        private int fence;  // -1 until first use
        private int index;  // current index, modified on traverse/split

        /** Creates new spliterator covering the given array and range */
        DeqSpliterator(ArrayDeque<E> deq, int origin, int fence) {
            this.deq = deq;
            this.index = origin;
            this.fence = fence;
        }

        private int getFence() { // force initialization
            int t;
            if ((t = fence) < 0) {
                t = fence = deq.tail;
                index = deq.head;
            }
            return t;
        }

        public DeqSpliterator<E> trySplit() {
            int t = getFence(), h = index, n = deq.elements.length;
            if (h != t && ((h + 1) & (n - 1)) != t) {
                if (h > t)
                    t += n;
                int m = ((h + t) >>> 1) & (n - 1);
                return new DeqSpliterator<>(deq, h, index = m);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> consumer) {
            if (consumer == null)
                throw new NullPointerException();
            Object[] a = deq.elements;
            int m = a.length - 1, f = getFence(), i = index;
            index = f;
            while (i != f) {
                @SuppressWarnings("unchecked") E e = (E)a[i];
                i = (i + 1) & m;
                if (e == null)
                    throw new ConcurrentModificationException();
                consumer.accept(e);
            }
        }

        public boolean tryAdvance(Consumer<? super E> consumer) {
            if (consumer == null)
                throw new NullPointerException();
            Object[] a = deq.elements;
            int m = a.length - 1, f = getFence(), i = index;
            if (i != fence) {
                @SuppressWarnings("unchecked") E e = (E)a[i];
                index = (i + 1) & m;
                if (e == null)
                    throw new ConcurrentModificationException();
                consumer.accept(e);
                return true;
            }
            return false;
        }

        public long estimateSize() {
            int n = getFence() - index;
            if (n < 0)
                n += deq.elements.length;
            return (long) n;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED |
                    Spliterator.NONNULL | Spliterator.SUBSIZED;
        }
    }

}
