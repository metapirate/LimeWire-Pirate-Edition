package com.limegroup.gnutella.spam;

import java.util.Arrays;

import org.limewire.util.Base32;

/**
 * A token representing the hash of a template that may have been used to
 * create a filename.
 */
public class TemplateHashToken extends Token {

    /**
     * Unlike keywords or file extensions, templates should be quite unlikely to
     * occur in both spam and non-spam responses, so we give them a high weight.
     */
    private static final float TEMPLATE_HASH_WEIGHT = 0.9f;

    private final byte[] hash;

    TemplateHashToken(byte[] hash) {
        this.hash = hash;
    }

    @Override
    protected float getWeight() {
        return TEMPLATE_HASH_WEIGHT;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(hash);
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof TemplateHashToken))
            return false;
        return Arrays.equals(hash, ((TemplateHashToken)o).hash);
    }

    @Override
    public String toString() {
        return "template hash " + Base32.encode(hash);
    }
}
