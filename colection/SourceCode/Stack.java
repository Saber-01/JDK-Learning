/*
 * Copyright (c) 1994, 2010, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

/**
 * The <code>Stack</code> class represents a last-in-first-out
 * (LIFO) stack of objects. It extends class <tt>Vector</tt> with five
 * operations that allow a vector to be treated as a stack. The usual
 * <tt>push</tt> and <tt>pop</tt> operations are provided, as well as a
 * method to <tt>peek</tt> at the top item on the stack, a method to test
 * for whether the stack is <tt>empty</tt>, and a method to <tt>search</tt>
 * the stack for an item and discover how far it is from the top.
 * <p>
 *   此 Stack 类表示后进先出 (LIFO) 的数据结构。它在 Vector 类的基础上
 *  扩展了五个操作，这些操作让向量成为了堆栈。它提供了 push 和 pop
 *  操作，一个获取堆栈顶部元素的 peek 方法，还有一个测试堆栈是否为
 * 空的 empty 方法和一个搜索指定元素离栈顶有多远的 search 方法。
 *
 * When a stack is first created, it contains no items.
 *当一个栈被创建的时候，它不包含任何元素。
 * <p>A more complete and consistent set of LIFO stack operations is
 * provided by the {@link Deque} interface and its implementations, which
 * should be used in preference to this class.  For example:
 * Deque 接口中提供了更多 LIFO 操作的实现，使用中应该优先考虑 Deque。
 *  可以通过以下方式创建该类的对象：
 * <pre>   {@code
 *   Deque<Integer> stack = new ArrayDeque<Integer>();}</pre>
 *
 * @author  Jonathan Payne
 * @since   JDK1.0
 */
public
class Stack<E> extends Vector<E> {
    /**
     * Creates an empty Stack.
     * 创建一个空的栈
     */
    public Stack() {
    }

    /**
     * Pushes an item onto the top of this stack. This has exactly
     * the same effect as:
     * 将一个元素压到栈顶，此方法和 addElement 方法效果一样。
     *
     * <blockquote><pre>
     * addElement(item)</pre></blockquote>
     *
     * @param   item   the item to be pushed onto this stack.
     * @return  the <code>item</code> argument.
     * @see     java.util.Vector#addElement
     */
    public E push(E item) {
        addElement(item);

        return item;
    }

    /**
     * Removes the object at the top of this stack and returns that
     * object as the value of this function.
     * 移除栈顶元素并返回该元素
     * @return  The object at the top of this stack (the last item
     *          of the <tt>Vector</tt> object).
     * @throws  EmptyStackException  if this stack is empty.
     */
    public synchronized E pop() {
        E       obj;
        int     len = size();  //获得栈底层的数组中元素个数，

        obj = peek();  //如果为空，这一步会抛出异常
        removeElementAt(len - 1);  //先进先出，因为push在末尾添加，所以pop也在末尾弹出。

        return obj;      //返回弹出的值。
    }

    /**
     * Looks at the object at the top of this stack without removing it
     * from the stack.
     *返回栈顶元素（不删除）。
     * @return  the object at the top of this stack (the last item
     *          of the <tt>Vector</tt> object).
     * @throws  EmptyStackException  if this stack is empty.
     */
    public synchronized E peek() {
        int     len = size();

        if (len == 0)  //注意如果栈为空，会抛出空栈异常。
            throw new EmptyStackException();
        return elementAt(len - 1);
    }

    /**
     * Tests if this stack is empty.
     *判断栈是否为空。
     * @return  <code>true</code> if and only if this stack contains
     *          no items; <code>false</code> otherwise.
     */
    public boolean empty() {
        return size() == 0;
    }

    /**
     * Returns the 1-based position where an object is on this stack.
     * If the object <tt>o</tt> occurs as an item in this stack, this
     * method returns the distance from the top of the stack of the
     * occurrence nearest the top of the stack; the topmost item on the
     * stack is considered to be at distance <tt>1</tt>. The <tt>equals</tt>
     * method is used to compare <tt>o</tt> to the
     * items in this stack.
     *返回栈中元素的位置。如果对象 o 出现在栈中，这个方法返回离栈顶
     * 最近的该元素的距离。栈顶元素的距离被定义成 1。equals 用来判断
     * 是否匹配。
     * @param   o   the desired object.
     * @return  the 1-based position from the top of the stack where
     *          the object is located; the return value <code>-1</code>
     *          indicates that the object is not on the stack.
     */
    public synchronized int search(Object o) {
        int i = lastIndexOf(o);    //从后面开始遍历，找到第一个指定元素相等的值的索引

        if (i >= 0) {      //如果找到了，
            return size() - i;   //因为栈顶元素距离定义成1，所以要倒数过来。
        }
        return -1;     //如果没找到返回-1
    }

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    //序列化，检查版本用的，对类功能无关。
    private static final long serialVersionUID = 1224463164541339165L;
}
