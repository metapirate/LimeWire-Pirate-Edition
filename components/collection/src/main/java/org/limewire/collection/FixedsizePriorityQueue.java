package org.limewire.collection;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Provides a priority queue with bounded size in an 
 * <a href="http://en.wikipedia.org/wiki/AVL_tree">AVL tree</a>.
 * <code>FixedsizePriorityQueue</code> guarantees the lowest priority element
 * is ejected when exceeding capacity. 
 * <p>
 * <code>FixedsizePriorityQueue</code> provides {@link #extractMax()} to extract 
 * the maximum element. <code>FixedsizePriorityQueue</code> requires an explicit 
 * {@link Comparator}; <code>FixedsizePriorityQueue</code> won't use the natural
 * ordering of values.
 * <p>
 * Fetching the max element takes O(lg N) time, where N is the number of
 * elements. Also, extracting and adding elements is O(lg N) time.  
 * <p>
 * This class is not thread-safe.
<pre>
    FixedsizePriorityQueue&lt;String&gt; fpq = 
        new FixedsizePriorityQueue&lt;String&gt;(Comparators.stringComparator(), 3);
    fpq.insert("Abby");
    fpq.insert("Bob");
    fpq.insert("Chris");
    System.out.println(fpq);
    System.out.println("Inserting another String pushes out an element (" + fpq.insert("Dan") + ") since the max. size was reached.");
    System.out.println(fpq);

    System.out.println("Minimum element: " + fpq.getMin());
    System.out.println("Maximum element: " + fpq.getMax());
    fpq.extractMax();
    System.out.println(fpq);
    
    Output:
        [Abby, Bob, Chris]
        Inserting another String pushes out an element (Abby) since the max. size was reached.
        [Bob, Chris, Dan]
        Minimum element: Bob
        Maximum element: Dan
        [Bob, Chris]

</pre>
 */
public class FixedsizePriorityQueue<E> implements Iterable<E> {
    /** 
     * The underlying data structure.
     * INVARIANT: tree.size()<=capacity 
     * INVARIANT: all elements of tree instanceof Node
     */
    private SortedList<E> tree;
    /** The maximum number of elements to hold. */
    private int capacity;

       /**
     * Creates a new FixedsizePriorityQueue that will hold at most 
     * <tt>capacity</tt> elements.
     * @param comparator expresses priority.  Note that
     *  comaparator.compareTo(a,b)==0 does not imply that a.equals(b).
     * @param capacity the maximum number of elements
     * @exception IllegalArgumentException capacity negative
     */
    public FixedsizePriorityQueue(Comparator<? super E> comparator, int capacity) 
            throws IllegalArgumentException {
        if (capacity<=0)
            throw new IllegalArgumentException();
        tree=new SortedList<E>(comparator);
        this.capacity=capacity;
    }
    
    public void clear() {
        tree.clear();
    }
    
    public boolean isFull() {
        return size() >= capacity();
    }

    /**
     * Adds x to this, possibly removing some lower priority entry if necessary
     * to ensure this.size()<=this.capacity().  If this has capacity, x will be
     * added even if already in this (possibly with a different priority).
     *
     * @param x the entry to add
     * @return the element ejected, possibly x, or null if none 
     */
    public E insert(E x) {
        if (!isFull()) {
            //a) Size less than capacity.  Just add x.
            boolean added=tree.add(x);
            assert added;
            return null;
        } else {
            //Ensure size does not exceed capacity.    
            //Micro-optimizations are possible.
            E smallest = tree.first();
            if (tree.comparator().compare(x,smallest)>0) {
                //b) x larger than smallest of this: remove smallest and add x
                tree.remove(smallest);
                boolean added=tree.add(x);
                assert added;
                return smallest;
            } else {
                //c) Otherwise do nothing.
                return x;
            }
        }
    }
    
    public E extractMax() {
        E e = getMax();
        remove(e);
        return e;
    }
    
    /**
     * Returns the highest priority element of this.
     * @exception NoSuchElementException this.size()==0
     */
    public E getMax() throws NoSuchElementException {
        return tree.last();
    }

   /**
     * Returns the lowest priority element of this.
     * @exception NoSuchElementException this.size()==0
     */
    public E getMin() throws NoSuchElementException {
        return tree.first();
    }

    /** 
     * Returns true if this contains o.  Runs in O(N) time, where N is
     * number of elements in this.
     *
     * @param o this contains a x s.t. o.equals(x).  Note that
     *  priority is ignored in this operation.
     */
    public boolean contains(E o) {
        return tree.contains(o);
    }

    /** 
     * Removes the first occurrence of  o.
     *
     * @return true this contained an x such that o.equals(x).
     */
    public boolean remove(E o) {
        return tree.remove(o);
    }

    /** 
     * Returns an iterator of the elements in this, from <b>worst to best</b>.
     */
    public Iterator<E> iterator() {
        return tree.iterator();            
    }

    /**
     * Returns the number of elements in this.
     */
    public int size() {
        return tree.size();
    }
    
    /**
     * Returns the maximum number of elements this can hold.
     * @return the value passed to this constructor
     */
    public int capacity() {
        return capacity;
    }

    @Override
    public String toString() {
        return tree.toString();
    }
}
