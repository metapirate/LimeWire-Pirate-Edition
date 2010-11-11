package com.limegroup.gnutella.spam;

/**
 * A token representing a name/value pair from XML metadata.
 */
public class XMLKeywordToken extends KeywordToken {
    
    /**
     * Like keywords, XML name/value pairs may occur in a large number of
     * files, so we don't want to be too hasty about considering them spam.
     * However, they are slightly more specific than normal keywords.
     */
    private static final float XML_WEIGHT = 0.2f;

    XMLKeywordToken(String name, String value) {
        super(name + ":" + value);
    }
    
    @Override
    protected float getWeight() {
        return XML_WEIGHT;
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof XMLKeywordToken))
            return false;
        return keyword.equals(((XMLKeywordToken)o).keyword);
    }
    
    @Override
    public String toString() {
        return "xml " + keyword;
    }
}
