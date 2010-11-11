package org.limewire.collection;

import java.util.Collection;
import java.util.Comparator;

/**
 * Gives a sorted list of elements with a maximum size. Elements are sorted
 * upon insertion to the list, but only a fixed number of items are allowed. 
 * Therefore, if the list has reached the capacity, the last ordered element
 * is removed and then the new element is inserted in the proper location.
 * 
 <pre>
    FixedSizeSortedList&lt;String&gt; fssl = new FixedSizeSortedList&lt;String&gt;(5);
    
    fssl.add("Abby");
    fssl.add("Abby");
    fssl.add("Bob");
    fssl.add("Chris");
    fssl.add("Dan");
    System.out.println(fssl);
    fssl.add("Eric");
    System.out.println(fssl);
    fssl.add("Abby");
    System.out.println(fssl);

    Output:
        [Abby, Abby, Bob, Chris, Dan]
        [Abby, Abby, Bob, Chris, Eric]
        [Abby, Abby, Abby, Bob, Chris]
</pre>
*/ 
public class FixedSizeSortedList<E> extends SortedList<E> {
    private final int capacity;

    public FixedSizeSortedList(int capacity) {
        this.capacity = capacity;
    }

    public FixedSizeSortedList(Collection<? extends E> c, Comparator<? super E> comparator, int capacity) {
        super(c, comparator);
        this.capacity = capacity;
    }

    public FixedSizeSortedList(Collection<? extends E> c, int capacity) {
        super(c);
        this.capacity = capacity;
    }

    public FixedSizeSortedList(Comparator<? super E> comparator, int capacity) {
        super(comparator);
        this.capacity = capacity;
    }
    
    @Override
    public boolean add(E e) {
        if (size() == capacity)
            remove(last());
        return super.add(e);
    }
    
    /**
     * Tries to insert an element into the list. This may fail if the list is
     * full and the new element is ordered below all existing elements, in
     * which case the new element is returned. If the list is full and the new
     * element is ordered above any existing element, the new element is
     * inserted and the lowest-ordered element is removed and returned. If the
     * list is not full, the new element is inserted and null is returned.
     * 
     *  @return null, the new element, or an existing element that was removed
     */
    public E insert(E e) {
        E ret = null;
        if (size() == capacity) {
            ret = last();
            if (comparator().compare(e, ret) < 0)
                return e;
            remove(ret);
        }
        add(e);
        return ret;
    }
}

