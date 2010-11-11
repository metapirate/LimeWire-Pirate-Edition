package org.limewire.ui.swing.search.resultpanel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.limewire.inject.LazySingleton;

@LazySingleton
public class SearchResultTruncatorImpl implements SearchResultTruncator {
    private static final String SINGLE_SPACE = " ";
    private static final String EMPTY_STRING = "";
    private static final String OPEN_TAG = "<b>";
    private static final String CLOSE_TAG = "</b>";
    private static final String ELLIPSIS = "...";
    private static final int ELLIPSIS_SHRINK_INCREMENT = ELLIPSIS.length() + 1;
    private final Matcher findHTMLMinusBoldTags;
    private final Matcher findMultipleWhitespaceChars;
    private final Matcher findWhitespaceMinusSpaceChars;
    
    public SearchResultTruncatorImpl() {
        Pattern patternHTMLMinusBoldTags = Pattern.compile("[<][/]?[\\w =\":#&&[^b]]*[>]");
        findHTMLMinusBoldTags = patternHTMLMinusBoldTags.matcher("");
        Pattern patternMultipleWhitespaceChars = Pattern.compile("[\\s]++");
        findMultipleWhitespaceChars = patternMultipleWhitespaceChars.matcher("");
        Pattern patternWhitespaceMinusSpaceChars = Pattern.compile("[\\s&&[^ ]]");
        findWhitespaceMinusSpaceChars = patternWhitespaceMinusSpaceChars.matcher("");
    }
    

    @Override
    public String truncateHeading(String headingText, int visibleWidthPixels, FontWidthResolver resolver) {
        //Strip HTML characters *except* <b>bold-wrapped</b> strings
        headingText = replaceAll(findHTMLMinusBoldTags, headingText, EMPTY_STRING);
        if (resolver.getPixelWidth(headingText) <= visibleWidthPixels) {
            return headingText;
        }
        
        //Strip multiple whitespace characters (spaces, \r, \n, \t) and embedded whitespace characters
        String truncated = replaceAll(findMultipleWhitespaceChars, headingText, SINGLE_SPACE);
               truncated = replaceAll(findWhitespaceMinusSpaceChars, truncated, EMPTY_STRING);
        
        do {
            if (truncated.length() < ELLIPSIS_SHRINK_INCREMENT) {
                break;
            }
            if (getEndEdge(truncated) >= (truncated.length() - (truncated.contains(ELLIPSIS) ? ELLIPSIS.length() : 0))) {
                truncated = ELLIPSIS + truncated.substring(ELLIPSIS_SHRINK_INCREMENT);
            } else if (getLeadEdge(truncated) >= 0) {
                truncated = truncated.substring(0, truncated.length() - ELLIPSIS_SHRINK_INCREMENT) + ELLIPSIS;
            }
        } while (resolver.getPixelWidth(truncated) > visibleWidthPixels);
        
        return truncated;
    }
    
    private String replaceAll(Matcher pattern, String source, String replacement) {
        return pattern.reset(source).replaceAll(replacement);
    }

    private int getLeadEdge(String headingText) {
        int indexOf = headingText.indexOf(OPEN_TAG);
        return indexOf == -1 ? 0 : indexOf;
    }

    private int getEndEdge(String headingText) {
        int closeIndex = headingText.indexOf(CLOSE_TAG);
        return closeIndex == -1 ? 0 : closeIndex + CLOSE_TAG.length();
    }
}
