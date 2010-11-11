package org.limewire.collection;

import java.util.Iterator;

/**
 * Provides an iterator that iterates over two other iterators, in order.
 * 
<pre>
    LinkedList&lt;String&gt; l1 = new LinkedList&lt;String&gt;();
    LinkedList&lt;String&gt; l2 = new LinkedList&lt;String&gt;();
    for(int i = 0; i < 5; i++){
        l1.add(String.valueOf(i));
        l2.add(String.valueOf(i + 10));
    }

    for(DualIterator&lt;String&gt; di = 
            new DualIterator&lt;String&gt;(l1.iterator(), l2.iterator()); di.hasNext();)    
        System.out.println(di.next());      

    Output:
        0
        1
        2
        3
        4
        10
        11
        12
        13
        14

</pre>
 */
public class DualIterator<T> implements Iterator<T> {
    
    /**
     * The primary iterator.
     */
    private final Iterator<T> i1;
    
    /**
     * The secondary iterator.
     */
    private final Iterator<T> i2;
    
    /**
     * Whether or not you have reached the secondary iterator.
     */
    private boolean onOne;
    
    /**
     * Constructs a new DualIterator backed by two iterators.
     */
    public DualIterator(Iterator<T> a, Iterator<T> b) {
        i1 = a; i2 = b;
        onOne = true;
    }
    
    /**
     * Determines if there are any elements left in either iterator.
     */
    public boolean hasNext() {
        return i1.hasNext() || i2.hasNext();
    }
    
    /**
     * Retrieves the next element from the current backing iterator.
     */
    public T next() {
        if(i1.hasNext())
            return i1.next();
        else {
            onOne = false;
            return i2.next();
        }
    }
    
    /**
     * Removes the element from the current backing iterator.
     */
    public void remove() {
        if(onOne)
            i1.remove();
        else
            i2.remove();
    }
}
