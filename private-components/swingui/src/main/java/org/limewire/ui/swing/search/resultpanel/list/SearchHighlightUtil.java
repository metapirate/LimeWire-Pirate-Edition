package org.limewire.ui.swing.search.resultpanel.list;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.Objects;

class SearchHighlightUtil {
    private static final Log LOG = LogFactory.getLog(SearchHighlightUtil.class);
    
    private final StringBuilder builderBuffer = new StringBuilder();
    private final Matcher matcher;

     SearchHighlightUtil(String search) {
         // Create literal string with escaped regex characters.
         String literalSearch = createLiteralSearch(Objects.nonNull(search, "search"));
         
         // Create pattern to match on word boundary, and match content.
         Pattern pattern = Pattern.compile("\\b(" + literalSearch + ")", Pattern.CASE_INSENSITIVE);
         matcher = pattern.matcher("");
    }

    /**
     * Returns a string that highlights the specified search text within the
     * specified content.  Highlights are indicated using HTML &lt;b&gt; tags.
     * Multiple search words are separated by space characters; matches occur
     * on word boundaries.
     */
    public String highlight(String content) {
        if (content == null) {
            return "";
        }
        
        matcher.reset(content);
        builderBuffer.setLength(0);
        int index = 0;
        while (matcher.find()) {
            int startIndex = matcher.start();
            builderBuffer.append(content.substring(index, startIndex));
            String word = matcher.group();
            builderBuffer.append("<b>").append(word).append("</b>");
            index = matcher.end();
            LOG.debugf("Start: {0} url: {1} end: {2}", startIndex, word, matcher.end());
        }

        if (builderBuffer.length() > 0) {
            builderBuffer.append(content.substring(index));
            return builderBuffer.toString();
        }
        return content;
    }
    
    /**
     * Creates literal string for use in the highlight pattern.  The method 
     * splits the specified search string into space-separated tokens, quotes 
     * each token to escape regex characters, and inserts an OR operator 
     * between each token.  Zero-length tokens caused by consecutive space 
     * characters are ignored.
     */
    private static String createLiteralSearch(String search) {
        // Split search string into space-separated tokens.
        String[] tokens = search.split(" ");

        // Build literal string. 
        StringBuilder bldr = new StringBuilder();
        for (String token : tokens) {
            if (token.length() > 0) {
                // Precede each token with OR operator.
                if (bldr.length() > 0) {
                    bldr.append("|");
                }
                // Append quoted token to result.
                String literal = Pattern.quote(token);
                bldr.append(literal);
            }
        }
        
        // Return result.
        return bldr.toString();
    }
}
