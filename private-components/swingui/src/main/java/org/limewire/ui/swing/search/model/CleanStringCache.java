package org.limewire.ui.swing.search.model;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CleanStringCache {
    private final Matcher matcher;
    private final String replacement;
    
    private final Map<String, String> cache;
    
    public CleanStringCache(Pattern pattern, String replacement) {
        this.matcher = pattern.matcher("");
        this.replacement = replacement;
        this.cache = new WeakHashMap<String, String>();
    }

    public String getCleanString(String name) {
        String cleanedString = cache.get(name);
        if(cleanedString == null) {
            cleanedString = cleanString(name);
            cache.put(name, cleanedString);
        }
        return cleanedString;
    }
    
    /**
     * Removes all symbols and spaces in the string. 
     * Also removes any leaf elements on the name, for example: file1.txt
     * file1(1).txt, file1(2).txt.
     */
    public String cleanString(String string) {
        matcher.reset(string);
        string = matcher.replaceAll(replacement);
        return string;
    }

    public boolean matches(String string1, String string2) {
       return getCleanString(string1).equalsIgnoreCase(getCleanString(string2));
    }
}
