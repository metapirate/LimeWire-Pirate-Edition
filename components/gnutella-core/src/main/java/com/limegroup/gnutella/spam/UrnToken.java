package com.limegroup.gnutella.spam;

/**
 * A token representing a SHA1 URN.
 */
public class UrnToken extends KeywordToken {

    /**
     * We consider a URN to be a very accurate spam indicator - if the user
     * marks it as spam once, it should always be considered spam.
     */
    private static final float URN_WEIGHT = 1;
    
    UrnToken(String urn) {
        super(urn);
    }
    
    @Override
    protected float getWeight() {
        return URN_WEIGHT;
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof UrnToken))
            return false;
        return keyword.equals(((UrnToken)o).keyword);
    }
    
    @Override
    public String toString() {
        return "urn " + keyword;
    }
}
