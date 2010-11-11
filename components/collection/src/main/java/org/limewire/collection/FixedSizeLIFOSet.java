package org.limewire.collection;

/**
 * A fixed size <code>Set</code> where the last added element is the first 
 * item in the list. Upon reaching the capacity of elements, 
 * <code>FixedSizeLIFOSet</code> removes either the last item inserted, first 
 * out (LIFO, default) or removes the first item inserted, first out (FIFO). 
 * <p>
 * This class is a hash-based <code>Set</code> and therefore, objects must correctly
 * contain a {@link #hashCode()} and {@link #equals(Object)}.
 <pre>
    System.out.println("EjectionPolicy: LIFO");
    FixedSizeLIFOSet&lt;String&gt; lifo = new FixedSizeLIFOSet&lt;String&gt;(3, FixedSizeLIFOSet.EjectionPolicy.LIFO);

    lifo.add("Abby");
    lifo.add("Bob");
    lifo.add("Chris");
    System.out.println(lifo);
    System.out.println("Last in: Chris, First in Abby");
    lifo.add("Dan");
    System.out.println(lifo);

    System.out.println("\nEjectionPolicy: FIFO");

    FixedSizeLIFOSet&lt;String&gt; fifo = new FixedSizeLIFOSet&lt;String&gt;(3, FixedSizeLIFOSet.EjectionPolicy.FIFO);

    fifo.add("Abby");
    fifo.add("Bob");
    fifo.add("Chris");
    System.out.println(fifo);
    System.out.println("Last in: Chris, First in Abby");
    fifo.add("Dan");
    System.out.println(fifo);

    Output:
        EjectionPolicy: LIFO
        [Chris, Bob, Abby]
        Last in: Chris, First in Abby
        [Dan, Bob, Abby]
        
        EjectionPolicy: FIFO
        [Chris, Bob, Abby]
        Last in: Chris, First in Abby
        [Dan, Chris, Bob]

 </pre>

 */
public class FixedSizeLIFOSet<E> extends LIFOSet<E> {

	/**
	 * The EjectionPolicy controls which element should
	 * be removed from the Set if has reached its maximum
	 * capacity.
	 */
	public static enum EjectionPolicy {
		/**
		 * Removes the last-in (newest) element from the
		 * Set if it has reached its maximum capacity.
		 */
		LIFO,
		
		/**
		 * Removes the first-in (eldest) element from the
		 * Set if has reached its maximum capacity.
		 */
		FIFO
	}
	
    final int maxSize;
    
    private final EjectionPolicy policy;
    
    public FixedSizeLIFOSet(int maxSize) {
        this(maxSize, EjectionPolicy.LIFO);
    }

    public FixedSizeLIFOSet(int maxSize, EjectionPolicy policy) {
    	this.maxSize = maxSize;
    	this.policy = policy;
    }
    
    public FixedSizeLIFOSet(int maxSize, int initialCapacity, float loadFactor) {
    	this(maxSize, initialCapacity, loadFactor, EjectionPolicy.LIFO);
    }
    
    public FixedSizeLIFOSet(int maxSize, int initialCapacity, float loadFactor, EjectionPolicy policy) {
        super(initialCapacity, loadFactor);
        this.maxSize = maxSize;
        this.policy = policy;
    }
    
    @Override
    public boolean add(E o) {
    	boolean added = super.add(o);
    	if (added && size() > maxSize) {
    		if (policy == EjectionPolicy.FIFO) {
    			remove(0);
    			
    		} else { // EjectionPolicy.LIFO
    			remove(Math.max(0, size()-2));
    		}
    		
    		assert (size() <= maxSize);
    	}
        
        return added;
    }
}
