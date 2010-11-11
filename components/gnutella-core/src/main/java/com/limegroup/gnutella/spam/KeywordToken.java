package com.limegroup.gnutella.spam;

/**
 * A token representing a keyword from a file name (excluding the
 * extension) or a query string.
 */
public class KeywordToken extends Token {
    
    /**
     * Spammers often echo the search tokens in the result, but we ignore
     * those tokens, so any remaining keywords should be a reasonable
     * indicator of spam. However, the same keyword may occur in a large
     * number of files, so we don't want to be too hasty.
     */
    private static final float KEYWORD_WEIGHT = 0.15f;
    
	protected final String keyword;
    
	KeywordToken(String keyword) {
        this.keyword = keyword;
	}
    
    @Override
	protected float getWeight() {
        return KEYWORD_WEIGHT;
    }
    
    @Override
    public int hashCode() {
        return keyword.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof KeywordToken))
            return false;
        return keyword.equals(((KeywordToken)o).keyword);
    }
    
	@Override
    public String toString() {
		return "keyword " + keyword;
	}
}
