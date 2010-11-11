package com.limegroup.gnutella.spam;

/**
 * A token representing a file extension.
 */
public class FileExtensionToken extends KeywordToken {

    /**
     * A file extension is a poor indicator of spam, so we give it a lower
     * weight than other keywords.
     */
    private static final float EXTENSION_WEIGHT = 0.05f;
    
    FileExtensionToken(String extension) {
        super(extension);
    }
    
    @Override
    protected float getWeight() {
        return EXTENSION_WEIGHT;
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof FileExtensionToken))
            return false;
        return keyword.equals(((FileExtensionToken)o).keyword);
    }
    
    @Override
    public String toString() {
        return "extension " + keyword;
    }
}
