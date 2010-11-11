package org.limewire.collection;

/**
 * Defines the interface to manipulate a fixed size field of bits. 
 * <code>BitField</code> declares methods to return the location where a bit
 * is either set (equal to 1) or clear (equal to 0). The <code>BitField</code>
 * interface has a methods for returning the bit value at a particular location
 * and the maximum size of the field of bits.
 * 
 * Also, <code>BitField</code> has a <a href="http://en.wikipedia.org/wiki/Cardinality">
 * cardinality</a> method for working with sets.
 * <p>
 * {@link BitFieldSet}, {@link AndView}, {@link OrView}, {@link XorView} and 
 * {@link NotView} implement <code>BitField</code>. The subclasses perform 
 * various operations on the bits. 
 * <p>
<pre>
    void sampleCodeBitField(){
        
        BitSet bs = new BitSet();
        bs.set(3);
        bs.set(1);
        BitField completed = new BitFieldSet(bs, 64);
        NotView uncompleted = new NotView(completed);

        printBitField(completed,   "  completed");
        printBitField(uncompleted, "uncompleted");
                
        System.out.println("Completed " + completed.cardinality() + " out of " + completed.maxSize() + " tasks.");
        System.out.println("Uncompleted tasks: " + new NotView(completed).cardinality() + ".");
    }

    void printBitField(BitField bf, String bfName){
        System.out.print(bfName + ": ");
        for(int i = 0; i < bf.maxSize(); i++){
            int j = 0;
            if(bf.get(i))
                j = 1;          
            System.out.print(j);
        }
        System.out.println(""); 
    }
    Output:
          completed: 0101000000000000000000000000000000000000000000000000000000000000
        uncompleted: 1010111111111111111111111111111111111111111111111111111111111111
        Completed 2 out of 64 tasks.
        Uncompleted tasks: 62.
</pre>
 */
public interface BitField {
	public boolean get(int i);
	/**
	 * @return index of next set bit from index <code>i</code> or -1 if there is no bit left
	 */
	public int nextSetBit(int i);
	public int nextClearBit(int i);
	public int cardinality();
	public int maxSize();
}
