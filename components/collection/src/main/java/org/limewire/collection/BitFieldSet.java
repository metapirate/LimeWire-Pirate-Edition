package org.limewire.collection;

/**
 * Provides a <code>BitField</code> implementation for a {@link BitSet} object.
 * 
 <pre>   
    void sampleCodeBitFieldSet(){
        BitSet bs1 = new BitSet();
        bs1.set(1);
        bs1.set(3);
        
        BitSet bs2 = new BitSet();
        bs2.set(2);
        
        BitSet bs3 = new BitSet();
        bs3.set(3);
        
        BitField bf1 = new BitFieldSet(bs1, 16);
        BitField bf2 = new BitFieldSet(bs2, 16);
        BitField bf3 = new BitFieldSet(bs3, 16);

        PrintBFInfo(bf1, "bf1");
        PrintBFInfo(bf2, "bf2");
        PrintBFInfo(bf3, "bf3");      
    }

    void PrintBFInfo(BitField bf, String bfn){
        System.out.println(bfn);
        for(int i = 0; i < bf.maxSize(); i++){
            int j = 0;
            if(bf.get(i))
                j = 1;          
            System.out.print(j);
        }
        System.out.println(); 

        System.out.println("cardinality: " + bf.cardinality());
        System.out.println("Next clear bit is: " + bf.nextClearBit(2));
        System.out.println("Next set bit is: " + bf.nextSetBit(0));
        System.out.println();         
    }

    Output:
        bf1
        0101000000000000
        cardinality: 2
        Next clear bit is: 2
        Next set bit is: 1
        
        bf2
        0010000000000000
        cardinality: 1
        Next clear bit is: 3
        Next set bit is: 2
        
        bf3
        0001000000000000
        cardinality: 1
        Next clear bit is: 2
        Next set bit is: 3
 </pre>
 * 
 */
public class BitFieldSet implements BitField {

	private final int maxSize;
	private final BitSet bs;
	
	/**
	 * Constructs a BitField view over the passed bitset with the
	 * specified size. 
	 */
	public BitFieldSet(BitSet bs, int maxSize) {
		this.bs = bs;
		this.maxSize = maxSize;
	}
	
	public int maxSize() {
		return maxSize;
	}

	public int cardinality() {
		if (bs.length() <= maxSize)
			return bs.cardinality();
		else
			return bs.get(0, maxSize).cardinality(); // expensive, avoid.
	}

	public boolean get(int i) {
		if (i > maxSize)
			throw new IndexOutOfBoundsException();
		return bs.get(i);
	}

	public int nextClearBit(int i) {
		int ret = bs.nextClearBit(i); 
		return ret >= maxSize ? -1 : ret;
	}

	public int nextSetBit(int i) {
		int ret = bs.nextSetBit(i);
		return ret >= maxSize ? -1 : ret;
	}

}
