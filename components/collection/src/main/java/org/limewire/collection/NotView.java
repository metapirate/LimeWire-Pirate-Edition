package org.limewire.collection;

/**
 * Provides a not view for a {@link BitField}.  
 * See <a 
 * href="http://en.wikipedia.org/wiki/Logical_NOR">not</a> for more 
 * information.
<pre>
    void sampleCodeNotView(){
        BitSet bs1 = new BitSet();
        bs1.set(0);
        bs1.set(1);
                
        BitField bf1 = new BitFieldSet(bs1, 5);
               
        printBitField(bf1, "bf1");
        
        NotView nv = new NotView(bf1);

        printBitField(nv, " nv");
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
        bf1: 11000
         nv: 00111
</pre>    
 */
public class NotView implements BitField {

	private final BitField bf; 
	public NotView(BitField bf) {
		this.bf = bf;
	}
	public int cardinality() {
		return bf.maxSize() - bf.cardinality();
	}

	public boolean get(int i) {
		return !bf.get(i);
	}

	public int maxSize() {
		return bf.maxSize();
	}

	public int nextClearBit(int i) {
		return bf.nextSetBit(i);
	}

	public int nextSetBit(int i) {
		return bf.nextClearBit(i);
	}

}
