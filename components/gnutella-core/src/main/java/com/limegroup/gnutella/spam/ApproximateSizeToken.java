package com.limegroup.gnutella.spam;

/**
 * A token representing the approximate file size.
 */
public class ApproximateSizeToken extends Token {

    /** 
     * Spammers sometimes modify their files to make it more difficult to
     * filter by URN or size - we consider the approximate size, but give it
     * a lower weight than the exact size
     */
    private static final float APPROXIMATE_SIZE_WEIGHT = 0.3f;
    
    /**
     * How many bits of the size should be discarded?
     */
    private static final int SHIFT = 8;
    
    private final long size;
    
    public ApproximateSizeToken(long size) {
        this.size = size >> SHIFT << SHIFT;
    }

    @Override
    protected float getWeight() {
        return APPROXIMATE_SIZE_WEIGHT;
    }
    
    @Override
    public int hashCode() {
        return (int)size;
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof ApproximateSizeToken))
            return false;
        return size == ((ApproximateSizeToken)o).size;
    }
    
    @Override
    public String toString() {
        return "approximate size " + size;
    }
}
