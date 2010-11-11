package com.limegroup.gnutella.spam;

/**
 * A token representing the file size.
 */
public class SizeToken extends Token {

    /** 
     * Exact file size is a fairly accurate identifier of a file, so we will
     * consider a certain file size spam after only a couple of bad ratings.
     */
    private static final float SIZE_WEIGHT = 0.6f;
    
    private final long size;
    
    public SizeToken(long size) {
        this.size = size;
    }
    
    @Override
    protected float getWeight() {
        return SIZE_WEIGHT;
    }
    
    @Override
    public int hashCode() {
        return (int)size;
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof SizeToken))
            return false;
        return size == ((SizeToken)o).size;
    }
    
    @Override
    public String toString() {
        return "size " + size;
    }
}
