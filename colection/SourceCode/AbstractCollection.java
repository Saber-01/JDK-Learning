/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * This class provides a skeletal implementation of the <tt>Collection</tt>
 * interface, to minimize the effort required to implement this interface. <p>
 *本类提供Collection接口的基本实现，以最小化此接口所需工作
 *
 * To implement an unmodifiable collection, the programmer needs only to
 * extend this class and provide implementations for the <tt>iterator</tt> and
 * <tt>size</tt> methods.  (The iterator returned by the <tt>iterator</tt>
 * method must implement <tt>hasNext</tt> and <tt>next</tt>.)<p>
 * 要实现不可修改的collection，程序员只需要扩展该类并且为iterator和
 * size方法提供方法的实现。（iterator方法返回的迭代器必须实现
 * hasNext和next。）
 *
 * To implement a modifiable collection, the programmer must additionally
 * override this class's <tt>add</tt> method (which otherwise throws an
 * <tt>UnsupportedOperationException</tt>), and the iterator returned by the
 * <tt>iterator</tt> method must additionally implement its <tt>remove</tt>
 * method.<p>
 *要实现可修改的collection，程序员还必须重写add方法（否则会抛出
 * UnsupportedOperationException异常），iterator方法返回的迭代器
 * 必须实现remove方法。
 *
 * The programmer should generally provide a void (no argument) and
 * <tt>Collection</tt> constructor, as per the recommendation in the
 * <tt>Collection</tt> interface specification.<p>
 * 程序员必须提供一个void类型的Collection无参构造函数
 *
 * The documentation for each non-abstract method in this class describes its
 * implementation in detail.  Each of these methods may be overridden if
 * the collection being implemented admits a more efficient implementation.<p>
 *本类中每一个非抽象的方法的文档详细描述了其实现。如果正在被实现的
 * 集合允许更有效的实现，这些方法都必须被重写。
 *
 * This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *本类是Java Collections Framework的成员.
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see Collection
 * @since 1.2
 */

public abstract class AbstractCollection<E> implements Collection<E> {
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     *  唯一的构造函数.。（用于子类构造函数调用，通常是隐式的。）
     */
    protected AbstractCollection() {
    }

    // Query Operations
   //查询操作
    /**
     * Returns an iterator over the elements contained in this collection.
     *返回一个包含集合中每个元素的迭代器
     * @Saber-01
     * 注意：因为java中继承的父类是抽象类时，子类需要将抽象类中所有的抽象方法
     *  都实现。所以凡是在AbstactCollection中定义的abstract方法在其子类中都必须实现。
     * @return an iterator over the elements contained in this collection
     */
    public abstract Iterator<E> iterator();

    public abstract int size();     //获取集合中含有的元素个数

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns <tt>size() == 0</tt>.
     * 这个实现基于size()。如果获得的元素个数为0，则返回true，反之返回false。
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation iterates over the elements in the collection,
     * checking each element in turn for equality with the specified element.
     *使用iterator方法创建一个迭代器，通过迭代器遍历集合，使用(o==null ? e==null : o.equals(e))判断。
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean contains(Object o) {
        Iterator<E> it = iterator();
        if (o==null) {
            while (it.hasNext())
                if (it.next()==null)
                    return true;
        } else {
            while (it.hasNext())
                if (o.equals(it.next()))
                    return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns an array containing all the elements
     * returned by this collection's iterator, in the same order, stored in
     * consecutive elements of the array, starting with index {@code 0}.
     * The length of the returned array is equal to the number of elements
     * returned by the iterator, even if the size of this collection changes
     * during iteration, as might happen if the collection permits
     * concurrent modification during iteration.  The {@code size} method is
     * called only as an optimization hint; the correct result is returned
     * even if the iterator returns a different number of elements.
     *此实现返回一个数组，其中包含此集合的迭代器按序返回的所有元素，
     * 存储在数组的连续元素中，从索引0开始。返回数组的长度等于迭代器
     * 返回的元素的个数，即使这个集合的大小在迭代期间发生了变化，即
     * 集合允许在迭代期间并发修改时可能发生的那样。size方法仅作为优化
     * 提示调用；即使迭代器返回的元素个数不同，此方法也会返回正确的
     *  结果。
     * <p>This method is equivalent to:
     *
     *  <pre> {@code
     * List<E> list = new ArrayList<E>(size());
     * for (E e : this)
     *     list.add(e);
     * return list.toArray();
     * }</pre>
     */
    public Object[] toArray() {
        // Estimate size of array; be prepared to see more or fewer elements
        Object[] r = new Object[size()];     // 创建一个Object数组，大小为集合中元素的数量
        Iterator<E> it = iterator();       //返回集合的迭代器
        for (int i = 0; i < r.length; i++) {      //使用迭代器进行循环遍历，
            //如果集合中元素比预期的少，及在for循环中迭代器事先迭代完成，
            // 则调用Array.copyOf方法，将已经复制到数组r的i个引用复制到新数组中，返回的新数组长度为i，
            if (! it.hasNext()) // fewer elements than expected
                // Arrays的copyOf()方法传回的数组是新的数组对象，改变传回数组中的
                // 元素值，不会影响原来的数组。copyOf()的第二个自变量指定要建立的
                // 新数组长度，如果新数组的长度超过原数组的长度，则保留数组默认值0
                return Arrays.copyOf(r, i);
            r[i] = it.next();
        }
        // 集合中元素比预期的多，及上述循环正常完成以后发现还有元素，则未迭代完。
        // 需要调用finishToArray()方法生成新数组。
        // 如果集合中元素和预期一样，则返回r。
        return it.hasNext() ? finishToArray(r, it) : r;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns an array containing all the elements
     * returned by this collection's iterator in the same order, stored in
     * consecutive elements of the array, starting with index {@code 0}.
     * If the number of elements returned by the iterator is too large to
     * fit into the specified array, then the elements are returned in a
     * newly allocated array with length equal to the number of elements
     * returned by the iterator, even if the size of this collection
     * changes during iteration, as might happen if the collection permits
     * concurrent modification during iteration.  The {@code size} method is
     * called only as an optimization hint; the correct result is returned
     * even if the iterator returns a different number of elements.
     *此实现返回一个数组，其中包含该集合的迭代器以相同的顺序返回的所有
     * 元素。这些元素储存在数组的连续空间中，从索引0开始。如果迭代器返回的
     * 元素数量太大，不能完全存入指定数组，那么新分配一个长度等于迭代器返回
     * 元素数量的数组，将元素存入其中。若迭代过程中允许并发修改，那么迭代
     *  过程中集合大小可能发生变化。size方法仅作为优化提示调用；即使迭代器
     *  返回的元素个数不同，此方法也会返回正确的结果。
     * <p>This method is equivalent to:
     *
     *  <pre> {@code
     * List<E> list = new ArrayList<E>(size());
     * for (E e : this)
     *     list.add(e);
     * return list.toArray(a);
     * }</pre>
     *注意：返回数组的运行时类型是指定数组的运行时类型。
     *
     * @throws ArrayStoreException  {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        // Estimate size of array; be prepared to see more or fewer elements
        //估计数组的长度，用于比较查看是比预期元素多还是少
        int size = size();
        //如果目标数组a的长度大于集合的长度，即可以容纳集合所有元素，则r赋值为a。
        //如果目标数组a的长度小于集合的长度，即无法容纳集合所有元素，这时新创建一个
        //Array实例，该数组的长度为size，存放的数据类型和指定数组a相同。
        T[] r = a.length >= size ? a :
                (T[])java.lang.reflect.Array
                        .newInstance(a.getClass().getComponentType(), size);
        Iterator<E> it = iterator();    //得到集合的迭代器

        for (int i = 0; i < r.length; i++) {        //循环迭代
            if (! it.hasNext()) { // fewer elements than expected     //如果元素比预期的少，
                if (a == r) {       //如果数组a和r相等，说明之前判断中a.length>=size,目标数组a可以放下集合所有元素。
                    r[i] = null; // null-terminate          // 则将a剩余的位置上全部填为null。
                }
                //如果a,r不相等，说明a.length<size,但是因为元素比预期少，所以要再判断目标数组a.length和实际元素i。
                //如果数组a还是无法容纳下集合的实际元素个数，则直接调用Arrays.copyOf方法，
                // 将创建一个长度为i的刚好将集合中元素全部放下的新数组，并返回。
                else if (a.length < i) {
                    return Arrays.copyOf(r, i);
                } else {          //如果发现a.length>=i,即目标数组可以容纳下实际集合中元素。则不要新建数组，还是使用数组a。
                    System.arraycopy(r, 0, a, 0, i);  //因为元素存在了r中，需要从r中将元素引用复制到a中，复制个数为i。
                    if (a.length > i) {               //如果目标数组a的长度比集合实际个数要大，那么数组a填满集合元素后，还会有剩余位置
                        a[i] = null;            //a中剩余位置都填上null。
                    }
                }
                return a;
            }
            r[i] = (T)it.next();    //每次比迭代器遍历到的值转换为运行时类型后，存入数组r中。
        }
        // more elements than expected
        // 集合中元素比预期的多，及上述循环正常完成以后发现还有元素，则未迭代完。
        // 需要调用finishToArray()方法生成新数组。
        // 如果集合中元素和预期一样，则返回r。
        return it.hasNext() ? finishToArray(r, it) : r;
    }

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     * 要分配数组的最大长度。
     * 一些虚拟机在数组中保留一些头信息。
     * 试图分配更大的数组可能会导致OutOfMemoryError:请求的数组大小超过
     * 虚拟机限制
     * @Saber-01
     * 在数组的对象头里有一个_length字段，记录数组长度，只需要去
     * 读_length字段就可以了。ArrayList中定义的最大长度为Integer最大值减8，
     * 这个8就是就是存了数组_length字段。
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * Reallocates the array being used within toArray when the iterator
     * returned more elements than expected, and finishes filling it from
     * the iterator.
     *  当迭代器返回比预期更多的元素时，重新分配toArray中使用的数组，并从
     *  迭代器中取出元素完成填充。
     * @param r the array, replete with previously stored elements
     * @param it the in-progress iterator over this collection
     * @return array containing the elements in the given array, plus any
     *         further elements returned by the iterator, trimmed to size
     */
    @SuppressWarnings("unchecked")
    private static <T> T[] finishToArray(T[] r, Iterator<?> it) {
        //注意迭代器是从上层方法传过来的，不是从头开始迭代
        int i = r.length;
        while (it.hasNext()) {        //继续循环取出迭代器的下一个元素。
            int cap = r.length;
            if (i == cap) {//如果达到了数组最大的容量，则进行扩容。
                int newCap = cap + (cap >> 1) + 1;
                // 扩容后长度newCap，如果newCap大于MAX_ARRAY_SIZE，那么
                // 设置数组长度为Integer.MAX_VALUE
                // overflow-conscious code
                if (newCap - MAX_ARRAY_SIZE > 0)
                    newCap = hugeCapacity(cap + 1);
                r = Arrays.copyOf(r, newCap);
            }
            r[i++] = (T)it.next();
        }
        // trim if overallocated
        return (i == r.length) ? r : Arrays.copyOf(r, i);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow  //内存溢出。
            throw new OutOfMemoryError
                    ("Required array size too large");
        return (minCapacity > MAX_ARRAY_SIZE) ?    //如果所能允许的最小容量比所能分配的数组最大长度要大，
                Integer.MAX_VALUE :        //则新的容量为 Integer.MAX_VALUE
                MAX_ARRAY_SIZE;           //若小于等于则设置新容量为所能分配的数组最大长度MAX_ARRAY_SIZE
    }

    // Modification Operations
// 修改操作
    /**
     * {@inheritDoc}
     *
     * <p>This implementation always throws an
     * <tt>UnsupportedOperationException</tt>.
     *未实现的add操作。抛出UnsupportedOperationException异常
     * add方法没有使用abstract关键字，说明子类可以不用实现，但是
     * 对于一些集合想要实现可修改的话，如果不重写add方法，将
     * 抛出UnsupportedOperationException异常
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     * @throws IllegalStateException         {@inheritDoc}
     */

    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation iterates over the collection looking for the
     * specified element.  If it finds the element, it removes the element
     * from the collection using the iterator's remove method.
     *此实现遍历集合寻找指定的元素。如果找到则使用迭代器的remove方法从
     *  集合中删除元素。
     *
     * <p>Note that this implementation throws an
     * <tt>UnsupportedOperationException</tt> if the iterator returned by this
     * collection's iterator method does not implement the <tt>remove</tt>
     * method and this collection contains the specified object.
     *注意如果这个集合包含指定元素但迭代器中没有实现remove方法，那么
     *  将抛出UnsupportedOperationException异常。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     */
    public boolean remove(Object o) {
        Iterator<E> it = iterator();          //得到集合的迭代器
        if (o==null) {              //判断传入的对象是否为null，null将直接使用==来判断。
            while (it.hasNext()) {
                if (it.next()==null) {
                    //调用了迭代器的remove方法，所以如果子类实现的迭代器中没有实现remove方法。
                    //将会抛出UnsupportedOperationException异常
                    it.remove();
                    return true;
                }
            }
        } else {               //如果对象不为null，则使用对象的equals方法进行判断相等。
            while (it.hasNext()) {
                if (o.equals(it.next())) {
                    it.remove();
                    return true;
                }
            }
        }
        return false;           //以上没有返回说明。集合未找到元素，删除失败，返回false
    }


    // Bulk Operations
//批量操作
    /**
     * {@inheritDoc}
     *
     * <p>This implementation iterates over the specified collection,
     * checking each element returned by the iterator in turn to see
     * if it's contained in this collection.  If all elements are so
     * contained <tt>true</tt> is returned, otherwise <tt>false</tt>.
     *遍历指定集合，检查是否所有元素都在本集合中，如果都在返回true，否则
     * 返回false。
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @see #contains(Object)
     */
    public boolean containsAll(Collection<?> c) {
        for (Object e : c)
            if (!contains(e))
                return false;
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation iterates over the specified collection, and adds
     * each object returned by the iterator to this collection, in turn.
     *这个方法遍历指定集合，把指定集合迭代器返回的所有元素一次添加到本集
     * 合中。
     * <p>Note that this implementation will throw an
     * <tt>UnsupportedOperationException</tt> unless <tt>add</tt> is
     * overridden (assuming the specified collection is non-empty).
     *注意如果add方法没有被重写，那么将会抛出
     * UnsupportedOperationException异常。（假设指定集合不为空）
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     * @throws IllegalStateException         {@inheritDoc}
     *
     * @see #add(Object)
     */
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c)
            if (add(e))
                modified = true;
        return modified;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation iterates over this collection, checking each
     * element returned by the iterator in turn to see if it's contained
     * in the specified collection.  If it's so contained, it's removed from
     * this collection with the iterator's <tt>remove</tt> method.
     *这个方法遍历本集合，依次检查迭代器中的每一个元素是否包含在指定集合
     *   里。如果包含在指定集合里，使用迭代器的remove方法把它从本集合中删除。
     * <p>Note that this implementation will throw an
     * <tt>UnsupportedOperationException</tt> if the iterator returned by the
     * <tt>iterator</tt> method does not implement the <tt>remove</tt> method
     * and this collection contains one or more elements in common with the
     * specified collection.
     * 如果这个集合包含和指定集合相等的一个或多个元素，
     * 并且这时iterator方法返回的迭代器没有实现remove方法，
     * 那么这个实现将会抛出UnsupportedOperationException异常。
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     *
     * @see #remove(Object)
     * @see #contains(Object)
     */
    public boolean removeAll(Collection<?> c) {
        //判断集合是否为空。
        Objects.requireNonNull(c);
        boolean modified = false;
        Iterator<?> it = iterator();  //获得集合的迭代器
        while (it.hasNext()) {            //循环判断每一个值
            if (c.contains(it.next())) {                   //如果值存在在指定的集合中
                it.remove();                 //使用迭代器的remove方法删除。
                modified = true;
            }
        }
        return modified;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation iterates over this collection, checking each
     * element returned by the iterator in turn to see if it's contained
     * in the specified collection.  If it's not so contained, it's removed
     * from this collection with the iterator's <tt>remove</tt> method.
     *这个方法遍历本集合，依次检查当前集合的迭代器返回的每个元素，是否
     *包含在指定集合中。如果不包含（removeAll方法中是包含即删除），使用
     *  迭代器的remove方法从本集合中删除它。
     * <p>Note that this implementation will throw an
     * <tt>UnsupportedOperationException</tt> if the iterator returned by the
     * <tt>iterator</tt> method does not implement the <tt>remove</tt> method
     * and this collection contains one or more elements not present in the
     * specified collection.
     *注意，如果这个集合包含指定集合中不存在的一个或多个元素，
     * 这时iterator方法返回的迭代器没有实现remove方法，
     * 那么这个实现将抛出一个UnsupportedOperationException异常。
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     *
     * @see #remove(Object)
     * @see #contains(Object)
     */
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);   //判断集合是否为null,为null,抛出异常。
        boolean modified = false;
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation iterates over this collection, removing each
     * element using the <tt>Iterator.remove</tt> operation.  Most
     * implementations will probably choose to override this method for
     * efficiency.
     * 此实现遍历本集合，使用迭代器的remove方法删除每个元素。为了提高效率，
     * 大多数实现会重写这个操作。
     * <p>Note that this implementation will throw an
     * <tt>UnsupportedOperationException</tt> if the iterator returned by this
     * collection's <tt>iterator</tt> method does not implement the
     * <tt>remove</tt> method and this collection is non-empty.
     *注意，如果这个集合的迭代器方法返回的迭代器没有实现remove方法，并且
     *这个集合是非空的，那么这个实现将抛出一个
     * UnsupportedOperationException异常。
     * @throws UnsupportedOperationException {@inheritDoc}
     */
    public void clear() {
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            // remove 将会删除上次调用 next 时返回的元素。先调用next再调用
            // remove 才会删除元素。next 和 remove 方法具有依赖性，必须先用
            // next，再使用remove。如果先用remove方法会抛出
            // IllegalStateException异常。
            it.next();
            it.remove();
        }
    }


    //  String conversion

    /**
     * Returns a string representation of this collection.  The string
     * representation consists of a list of the collection's elements in the
     * order they are returned by its iterator, enclosed in square brackets
     * (<tt>"[]"</tt>).  Adjacent elements are separated by the characters
     * <tt>", "</tt> (comma and space).  Elements are converted to strings as
     * by {@link String#valueOf(Object)}.
     *返回此集合的字符串表示形式。字符串表示形式由迭代器按序返回的所有元素
     * 组成，这些元素用方括号 ("[]") 括起来。相邻元素用字符 ", " （逗号和空格）
     * 隔开。元素通过String.valueOf(Object)转换为字符串。
     * @return a string representation of this collection
     */
    public String toString() {
        Iterator<E> it = iterator();    //得到集合的迭代器。
        if (! it.hasNext())               //如果一开始迭代器就没值，说明为空，则返回“[]”
            return "[]";

        StringBuilder sb = new StringBuilder();
        sb.append('[');               //字符串首部先加一个[
        for (;;) {
            E e = it.next();              //取得迭代器下一个元素。
            sb.append(e == this ? "(this Collection)" : e);      //如果元素不等于当前集合的this自引用，则往结果字符串加入e。
            if (! it.hasNext())        //循环过程若发现迭代完成
                return sb.append(']').toString();     //则在末尾加入]作为字符串结尾，并将StringBuilder对象转为String返回。
            sb.append(',').append(' ');      //每次循环加入元素后，使用，和空格 隔开。
        }
    }

}
