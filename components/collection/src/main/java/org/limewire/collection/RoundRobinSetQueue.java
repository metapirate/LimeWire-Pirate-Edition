
package org.limewire.collection;

import java.util.HashSet;
import java.util.Set;

/**
 * A round-robin queue where types are unique.
<pre>
    LinkedList&lt;String&gt; ll = new LinkedList&lt;String&gt;();
    ll.add("Abby");
    ll.add("Bob");
    ll.add("Bob"); //duplicate that isn't enqueue'd in rrq      
    System.out.println("ll size: " + ll.size());

    RoundRobinSetQueue&lt;String&gt; rrq = new RoundRobinSetQueue&lt;String&gt;();
    for(String o : ll)
        rrq.enqueue(o);
    
    System.out.println("rrq size: " + rrq.size() + " content: ");
        
    for(int i = 0; i < rrq.size()  ; i++)
        System.out.println(rrq.next());

    Output:
        ll size: 3
        rrq size: 2 content: 
        Abby
        Bob
    
</pre>
 */
public class RoundRobinSetQueue<T> extends RoundRobinQueue<T> {
	
	private Set<T> _uniqueness;
	
	public RoundRobinSetQueue() {
		super();
		_uniqueness =  new HashSet<T>();
	}

	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.util.RoundRobinQueue#enqueue(java.lang.Object)
	 */
	@Override
    public synchronized void enqueue(T value) {
		if (_uniqueness.add(value)) 
			super.enqueue(value);
		
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.util.RoundRobinQueue#remove(java.lang.Object)
	 */
	@Override
    public synchronized void remove(T o) {
		if (_uniqueness.contains(o)) {
			_uniqueness.remove(o);
			super.remove(o);
		}
		
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.util.RoundRobinQueue#removeAllOccurences(java.lang.Object)
	 */
	@Override
    public synchronized void removeAllOccurences(T o) {
		remove(o);
	}
}
