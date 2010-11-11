package org.limewire.collection;

import java.io.Serializable;

import org.limewire.util.ByteUtils;

/**
 * An implementation of range that stores data in longs.
 */
public class LongInterval extends Range implements Serializable {
    private static final long serialVersionUID = -2562093104400487445L;
    
    private final long low;
    private final long high;
    protected LongInterval(long low, long high) {
        if(high < low)
            throw new IllegalArgumentException("low: " + low +
                                            ", high: " + high);
        if(low < 0)
            throw new IllegalArgumentException("low < min int:"+low);
        // high <= MAX_VALUE implies
        // low <= MAX_VALUE.  Only one check is necessary.
        if(high > MAX_VALUE)
            throw new IllegalArgumentException("high > max int:"+high);
        
        this.low = low;
        this.high = high;
    }
    
    protected LongInterval(long singleton) {
        this(singleton, singleton);
    }
    
    @Override
    public final long getLow() {
        return low;
    }
    
    @Override
    public final long getHigh() {
        return high;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.collection.Range#toBytes()
     */
    @Override
    public byte [] toBytes() {
        if (isLong()) {
            byte [] res = new byte[10];
            toBytes(res,0);
            return res;
        } else {
            byte [] res = new byte[8];
            toBytes8(res, 0);
            return res;
        }
    }
    
    /* (non-Javadoc)
     * @see org.limewire.collection.Range#toBytes(byte[], int)
     */
    @Override
    public void toBytes(byte [] dest, int offset) {
        if (!isLong()) {
            toBytes8(dest, offset);
            return;
        }
        dest[offset + 0] = (byte) (low >> 32);
        dest[offset + 1] = (byte) (low >> 24);
        dest[offset + 2] = (byte) (low >> 16);
        dest[offset + 3] = (byte) (low >> 8);
        dest[offset + 4] = (byte) (low);
        dest[offset + 5] = (byte) (high >> 32);
        dest[offset + 6] = (byte) (high >> 24);
        dest[offset + 7] = (byte) (high >> 16);
        dest[offset + 8] = (byte) (high >> 8);
        dest[offset + 9] = (byte) (high);
    }
    
    private void toBytes8(byte [] dest, int offset) {
        ByteUtils.int2beb((int)low,dest,offset);
        ByteUtils.int2beb((int)high,dest,offset+4);
    }
    
    @Override
    public final boolean isLong() {
        // if intervals are properly constructed through the Range factory
        // method this would not happen, but just in case we check.
        return high > Integer.MAX_VALUE;
    }
}
