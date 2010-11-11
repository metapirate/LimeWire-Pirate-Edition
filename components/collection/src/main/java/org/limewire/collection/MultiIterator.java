
package org.limewire.collection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Implements the {@link Iterator} interface. 
 <pre>
    LinkedList&lt;String&gt; l1 = new LinkedList&lt;String&gt;();
    l1.add("Abby");
    l1.add("Bob");
    
    LinkedList&lt;String&gt; l2 = new LinkedList&lt;String&gt;();
    l2.add("Anderson");
    l2.add("Baker");
    
    for(MultiIterator&lt;String&gt; miterator = 
            new MultiIterator&lt;String&gt;(l1.iterator(), l2.iterator());
            miterator.hasNext();)
        System.out.println(miterator.next());

    Output:
        Abby
        Bob
        Anderson
        Baker
    
</pre>
 */

public class MultiIterator<T> implements Iterator<T> {

	protected final Iterator<? extends T> [] iterators;
	protected int current;
    
    @SuppressWarnings("unchecked")
    public MultiIterator(Iterator<? extends T> i1) {
        this.iterators = new Iterator[] { i1 };
    }
    
    @SuppressWarnings("unchecked")
    public MultiIterator(Iterator<? extends T> i1, Iterator<? extends T> i2) {
        this.iterators = new Iterator[] { i1, i2 };
    }
    
    @SuppressWarnings("unchecked")
    public MultiIterator(Iterator<? extends T> i1, Iterator<? extends T> i2, Iterator<? extends T> i3) {
        this.iterators = new Iterator[] { i1, i2, i3 };
    }
    
    @SuppressWarnings("unchecked")
    public MultiIterator(Iterator<? extends T> i1, Iterator<? extends T> i2, Iterator<? extends T> i3, Iterator<? extends T> i4) {
        this.iterators = new Iterator[] { i1, i2, i3, i4 };
    }
	
	public MultiIterator(Iterator<? extends T>... iterators) {
		this.iterators = iterators;
	}
	
	@SuppressWarnings("unchecked")
    public MultiIterator(Iterable<? extends Iterator<? extends T>> iterators) {
	    List<Iterator<? extends T>> list = new ArrayList<Iterator<? extends T>>();
	    for(Iterator<? extends T> iterator : iterators) {
	        list.add(iterator);
	    }
	    this.iterators = list.toArray(new Iterator[list.size()]);
	}
	
	public void remove() {
		if (iterators.length == 0)
			throw new IllegalStateException();
		
		iterators[current].remove();
	}

	public boolean hasNext() {
        for (Iterator<? extends T> iterator : iterators) {
            if (iterator.hasNext())
                return true;
        }
		return false;
	}

	public T next() {
		if (iterators.length == 0)
			throw new NoSuchElementException();
		
		positionCurrent();
		return iterators[current].next();
	}
	
	protected void positionCurrent() {
		while (!iterators[current].hasNext() && current < iterators.length)
			current++;
	}

}
