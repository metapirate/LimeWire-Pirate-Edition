package com.limegroup.gnutella.spam;

import java.util.Locale;

/**
 * A token representing a template that may have been used to create a filename.
 */
public class TemplateToken extends KeywordToken {

    /**
     * Unlike keywords or file extensions, templates should be quite unlikely to
     * occur in both spam and non-spam responses, so we can give them a fairly
     * high weight.
     */
    private static final float TEMPLATE_WEIGHT = 0.9f;

    /**
     * Short templates should be given less weight because they're more likely
     * to occur in both spam and non-spam responses.
     */
    private static final int SHORT_TEMPLATE_LENGTH = 8;

    /**
     * The string that is used to replace the query when creating a template
     * from a filename.
     */
    private static final String REPLACEMENT_STRING = "****";

    private final float weight;

    /**
     * Private constructor - create tokens by calling create().
     */
    private TemplateToken(String template) {
        super(template);
        int length = template.length() - REPLACEMENT_STRING.length();
        if(length >= SHORT_TEMPLATE_LENGTH)
            weight = TEMPLATE_WEIGHT;
        else
            weight = TEMPLATE_WEIGHT * length / SHORT_TEMPLATE_LENGTH;
    }

    /**
     * If the filename contains the query (ignoring case), returns a new token
     * representing the filename with the query replaced with a fixed string.
     * Digits and whitespace are stripped from the start of the template.
     * Otherwise returns null.
     */
    public static TemplateToken create(String query, String filename) {
        query = query.trim().toLowerCase(Locale.US);
        filename = filename.trim().toLowerCase(Locale.US);
        if(filename.contains(query) && !filename.equals(query)) {
            String template = filename.replace(query, REPLACEMENT_STRING);
            template = template.replaceFirst("^[0-9\\s]*", "");
            return new TemplateToken(template);
        }
        return null;
    }

    @Override
    protected float getWeight() {
        return weight;
    }

    @Override public boolean equals(Object o) {
        if(!(o instanceof TemplateToken))
            return false;
        return keyword.equals(((TemplateToken)o).keyword);
    }

    @Override
    public String toString() {
        return "template " + keyword;
    }
}
