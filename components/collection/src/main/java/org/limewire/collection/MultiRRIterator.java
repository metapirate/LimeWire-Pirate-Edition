
package org.limewire.collection;

import java.util.Iterator;

/**
 * Is a round robin {@link Iterator} for multiple interators.
 * 
<pre>
    LinkedList&lt;Integer&gt; l1 = new LinkedList&lt;Integer&gt;();
    LinkedList&lt;Integer&gt; l2 = new LinkedList&lt;Integer&gt;();
    for(int i = 0; i < 5; i++){
        l1.add(i);
        l2.add(i + 80);
    }
                            
    for(MultiRRIterator&lt;Integer&gt; mRRiterator = new MultiRRIterator&lt;Integer&gt;(l1.iterator(), l2.iterator());
            mRRiterator.hasNext();)
        System.out.println(mRRiterator.next());
    
    Output:
        0
        80
        1
        81
        2
        82
        3
        83
        4
        84    
</pre>
 */

public class MultiRRIterator<T> extends MultiIterator<T> {
	
    public MultiRRIterator(Iterator<? extends T> i1) {
        super(i1);
        current = iterators.length - 1;
    }
    
    public MultiRRIterator(Iterator<? extends T> i1, Iterator<? extends T> i2) {
        super(i1, i2);
        current = iterators.length - 1;
    }
    
    public MultiRRIterator(Iterator<? extends T> i1, Iterator<? extends T> i2, Iterator<? extends T> i3) {
        super(i1, i2, i3);
        current = iterators.length - 1;
    }
    
    public MultiRRIterator(Iterator<? extends T> i1, Iterator<? extends T> i2, Iterator<? extends T> i3, Iterator<? extends T> i4) {
        super(i1, i2, i3, i4);
        current = iterators.length - 1;
    }
    
	public MultiRRIterator(Iterator<? extends T> ... iterators ) {
		super(iterators);
		current = iterators.length - 1;
	}
	
	@Override
    protected void positionCurrent() {
		int steps = 0;
		while (steps <= iterators.length) {
			if (current == iterators.length-1)
				current = -1;
			if (iterators[++current].hasNext())
				break;
			steps++;
		}
	}
}
