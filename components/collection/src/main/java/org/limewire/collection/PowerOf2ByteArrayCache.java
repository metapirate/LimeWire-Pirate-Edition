package org.limewire.collection;


/** 
 * Creates a byte array with a size that is a power of 2 (byte array size = 2^power).
 * If you try to get a byte array with a size that is not a power of two, the value 
 * is set to the next power of 2. For example, setting a value of 511 returns a byte 
 * array of size 512 because 511 is not a power of 2 (2^8 = 256 and 2^9 = 512). 
 * (If you attempt to create a byte array less than a size zero (base 2^-2), 
 * the size is set to 1.)
 * <p>
 * Additionally, <code>PowerOf2ByteArrayCache</code> stores the total size of 
 * cached byte[]s.
 * <pre>
    PowerOf2ByteArrayCache p2 = new PowerOf2ByteArrayCache();

    byte[] ba4 = p2.get(4);
    System.out.println("ba4 size: " + ba4.length + " cache size is " + p2.getCacheSize());

    byte[] ba5 = p2.get(5);
    System.out.println("ba5 size: " + ba5.length + " cache size is " + p2.getCacheSize());

    byte[] ba8 = p2.get(8);
    System.out.println("ba8 size: " + ba8.length + " cache size is " + p2.getCacheSize());

    byte[] ba9 = p2.get(9);
    System.out.println("ba9 size: " + ba9.length + " cache size is " + p2.getCacheSize());  
    
    Output:
        ba4 size: 4 cache size is 4
        ba5 size: 8 cache size is 12
        ba8 size: 8 cache size is 12
        ba9 size: 16 cache size is 28

  </pre>
 */

public class PowerOf2ByteArrayCache {
    
    private final IntHashMap<byte[]> CACHE = new IntHashMap<byte[]>(20);
    private volatile int totalStored = 0;
    
    /**
     * @return a byte array of the specified size, using cached one
     * if possible.
     */
    public byte [] get(int size) {
        int exp;
        for (exp = 1 ; exp < size ; exp*=2);
        // exp is now >= size.  it will be equal
        // to size if size was a power of two.
        // otherwise, it will be the first power of 2
        // greater than size.
        // since we want to cache only powers of two,
        // we will use exp from hereon.
        
        byte[] ret = CACHE.get(exp);
        if (ret == null) {
            ret = new byte[exp];
            totalStored += exp;
            CACHE.put(exp, ret);
        }
        return ret;
    }
    
    public int getCacheSize() {
        return totalStored;
    }
    
    
    /** Erases all data in the cache. */
    public void clear() {
        CACHE.clear();
        totalStored = 0;
    }

}
