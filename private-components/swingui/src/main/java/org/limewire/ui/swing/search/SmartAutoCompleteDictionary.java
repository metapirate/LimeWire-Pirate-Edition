package org.limewire.ui.swing.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Dictionary of auto-complete suggestions for smart queries with 
 * field-specific values based on the category and the query text.
 */
class SmartAutoCompleteDictionary {

    private final SearchCategory searchCategory;
    private final String keySeparator;
    
    /**
     * Constructs a SmartAutoCompleteDictionary for the specified search
     * category.
     */
    @Inject
    public SmartAutoCompleteDictionary(@Assisted SearchCategory searchCategory,
            KeywordAssistedSearchBuilder keywordSearchBuilder) {
        this.searchCategory = searchCategory;
        this.keySeparator = keywordSearchBuilder.getTranslatedKeySeprator();
    }

    /**
     * Returns a collection of smart queries for the specified input text.
     */
    public Collection<SmartQuery> getPrefixedBy(String s) {
        // Return empty set if key separator is found. (Colon in US English.)
        // This means the user is manually entering a field-specific query.
        if (s.indexOf(keySeparator) >= 0) {
            return Collections.<SmartQuery>emptySet();
        }
        
        switch (searchCategory) {
        case AUDIO:
            // Create smart entries for audio fields.
            return createAudioSmartQueries(s);
            
        case VIDEO:
            // Create smart entries for video fields.
            return createVideoSmartQueries(s);
            
        default:
            return Collections.<SmartQuery>emptySet();
        }
    }
    
    /**
     * Returns the search category associated with this dictionary.
     */
    public SearchCategory getSearchCategory() {
        return searchCategory;
    }

    /**
     * Returns true if the dictionary can immediately return results, or false
     * if it will block for any reason.
     */
    public boolean isImmediate() {
        return true;
    }

    /**
     * Returns a collection of audio field-specific queries using the 
     * specified input string.
     */
    private Collection<SmartQuery> createAudioSmartQueries(String s) {
        List<SmartQuery> entries = new ArrayList<SmartQuery>();
        
        // Split input text using dash.
        String[] fields = s.split("-");
        
        // Add smart query entries.
        if (fields.length == 1 ||
                (fields.length > 1 && StringUtils.isEmpty(fields[1]))) {
            entries.add(new SmartQuery(searchCategory).addData(FilePropertyKey.AUTHOR, fields[0].trim()));
            entries.add(new SmartQuery(searchCategory).addData(FilePropertyKey.TITLE, fields[0].trim()));
            entries.add(new SmartQuery(searchCategory).addData(FilePropertyKey.ALBUM, fields[0].trim()));
            
        } else if (fields.length == 2 ||
                (fields.length > 2 && StringUtils.isEmpty(fields[2]))) {
            entries.add(new SmartQuery(searchCategory).addData(FilePropertyKey.AUTHOR, fields[0].trim()).
                    addData(FilePropertyKey.TITLE, fields[1].trim()));
            
        } else if (fields.length > 2) {
            entries.add(new SmartQuery(searchCategory).addData(FilePropertyKey.AUTHOR, fields[0].trim()).
                    addData(FilePropertyKey.TITLE, fields[1].trim()).addData(FilePropertyKey.ALBUM, fields[2].trim()));
            entries.add(new SmartQuery(searchCategory).addData(FilePropertyKey.AUTHOR, fields[0].trim()).
                    addData(FilePropertyKey.ALBUM, fields[1].trim()).addData(FilePropertyKey.TITLE, fields[2].trim()));
        }
        
        return entries;
    }
    
    /**
     * Returns a collection of video field-specific queries using the 
     * specified input string.
     */
    private Collection<SmartQuery> createVideoSmartQueries(String s) {
        List<SmartQuery> entries = new ArrayList<SmartQuery>();
        
        // Split input text using dash.
        String[] fields = s.split("-");
        
        // Add smart query entries.
        if (fields.length == 1) {
            // Add entries only if the field is an integer.
            if (fields[0].trim().matches("\\d+")) {
                entries.add(new SmartQuery(searchCategory).addData(FilePropertyKey.YEAR, fields[0].trim()));
                entries.add(new SmartQuery(searchCategory).addData(FilePropertyKey.TITLE, fields[0].trim()));
            }
            
        } else if (fields.length > 1) {
            // Add entry only if one of the fields is an integer.
            if (fields[1].trim().matches("\\d+")) {
                entries.add(new SmartQuery(searchCategory).addData(FilePropertyKey.TITLE, fields[0].trim()).
                        addData(FilePropertyKey.YEAR, fields[1].trim()));
            } else if (fields[0].trim().matches("\\d+")) {
                entries.add(new SmartQuery(searchCategory).addData(FilePropertyKey.YEAR, fields[0].trim()).
                        addData(FilePropertyKey.TITLE, fields[1].trim()));
            }
        }
        
        return entries;
    }
}
