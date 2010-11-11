package org.limewire.ui.swing.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.collection.SortedList;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.i18n.I18nMarker;
import org.limewire.ui.swing.util.FilePropertyKeyUtils;
import org.limewire.ui.swing.util.Translator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Used to generate {@link SearchInfo} objects for a search based on an advanced search defined by
 *  a map of key/value or an encoded search string. 
 */
@Singleton
public class KeywordAssistedSearchBuilder {

    private static final String UNTRANSLATED_SEPARATOR = I18nMarker.marktr(":");
    
    /**
     * Turns on and off format checking for advanced queries.
     * 
     * <p> NOTE: The algorithm with format checking off can do a good job
     *            of getting the maximum information from a desirable query but 
     *            can lead to false positives since it assumes almost all queries
     *            are intended to be advanced.
     */
    private static final boolean CHECK_FORMAT = true;
    
    private final Translator translator;
    
    
    @Inject
    KeywordAssistedSearchBuilder(Translator translator) {
        this.translator = translator;
    }
    
    /**
     * @return a full composite query String from a map of the desired properties and 
     *          their values to search on.  
     */
    String createCompositeQuery(Map<FilePropertyKey, String> advancedSearch, SearchCategory category) {
        String keySeparator = getTranslatedKeySeprator();
        
        StringBuilder sb = new StringBuilder();
        for(FilePropertyKey key : advancedSearch.keySet()) {
            String value = advancedSearch.get(key);
            if (value != null && value.trim().length() > 0) {
                sb.append(translator.translate(
                        FilePropertyKeyUtils.getUntraslatedDisplayName(key, category))
                        .toLowerCase());
                sb.append(keySeparator);
                sb.append(value);
                sb.append(' ');
            }
        }     
        
        int len = sb.length();
        if (len > 0) {
            sb.deleteCharAt(len-1);
        }

        return sb.toString();
    }

    /**
     * Returns the string used to separate key/value pairs in a compound advanced search query. 
     */
    String getTranslatedKeySeprator() {
        return translator.translateWithComment("content separator ie. \"name:limewire\"", UNTRANSLATED_SEPARATOR);
    }
    
    /** 
     * @return a new {@link SearchInfo} based on the advanced search map and category. 
     */
    public SearchInfo createAdvancedSearch(Map<FilePropertyKey,String> advancedSearch, 
            SearchCategory searchCategory) {
        
        return DefaultSearchInfo.createAdvancedSearch(createCompositeQuery(advancedSearch, searchCategory),
                advancedSearch, searchCategory);
    }
    /**
     * Attempts to generate an advanced search based on {@link SearchInfo} 
     *  from a {@link SearchCategory} and encoded composite query string. 
     *  
     * @return null if the query could not be parsed otherwise the corresponding {@link SearchInfo}.
     */
    public SearchInfo attemptToCreateAdvancedSearch(String query, SearchCategory searchCategory) {

        // Advanced search in the all or other category is impossible
        if (searchCategory == SearchCategory.ALL || searchCategory == SearchCategory.OTHER) { 
            return null;
        }
        
        String translatedKeySeparator = getTranslatedKeySeprator();
        String untranslatedKeySeparator = UNTRANSLATED_SEPARATOR;
        
        // Only attempt to parse an advanced search if the query has at least one special
        //  key separator sequence
        if (query.indexOf(translatedKeySeparator) > 0 || query.indexOf(untranslatedKeySeparator) > 0) {

            String lowerCaseUntranslatedQuery = translator.toLowerCaseEnglish(query);
            String lowerCaseTranslatedQuery = translator.toLowerCaseCurrentLocale(query);
            
            Map<FilePropertyKey,String> map = new HashMap<FilePropertyKey,String>();

            List<KeyPacket> foundKeys = new SortedList<KeyPacket>();
            
            // Check the query and record possible key locations in English and current language.
            // NOTE: English is preferred over the current language in all cases for consistency.
            for (FilePropertyKey candidateKey : FilePropertyKey.values()) {
                
                String untranslatedKeyName
                    = FilePropertyKeyUtils.getUntraslatedDisplayName(candidateKey, searchCategory);
                
                // Check for English key
                KeyPacket keyPacket = attemptToFindKey(candidateKey, 
                        translator.toLowerCaseEnglish(untranslatedKeyName),
                        untranslatedKeySeparator, lowerCaseUntranslatedQuery);
                
                // If the key was found then add it to the list of found keys
                if (keyPacket != null) {
                    foundKeys.add(keyPacket);
                    continue;
                }
                
                if (translator.isCurrentLanguageEnglish()) {
                    // If we are already in English then we don't need to try and translate the key
                    continue;
                }
                
                // If the key in English was not found check if the translated key was
                keyPacket = attemptToFindKey(candidateKey,
                        translator.toLowerCaseCurrentLocale(
                                translator.translate(untranslatedKeyName)), 
                        translatedKeySeparator, lowerCaseTranslatedQuery);
                
                // If the translated key was found then record its location and contents
                if (keyPacket != null) {
                    foundKeys.add(keyPacket);
                }
            }

            // If no values were successfully parsed then do not attempt to create an
            //  advanced search (should fall back to regular search)
            if (foundKeys.size() == 0) {
                return null;
            }
            
            // Check the format, if there is leading non key data before the first key
            //  this probably means this is not actually an intended advanced search
            if (CHECK_FORMAT) {
                if (query.substring(0, foundKeys.get(0).getStartIndex()).trim().length() > 0) {
                    return null;
                }
            }
            
            // Find the value for each found key and insert it in the map
            // (iterate to one before the last because the last key is a special case)
            for ( int i=0 ; i<foundKeys.size()-1 ; i++ ) {
                attemptToParseValue(query, map, foundKeys.get(i), foundKeys.get(i+1).startIndex);
            }
           
            // Make sure the last key corresponds to a valid key/value pair
            //  and if not merge it with the value of the second last key            
            KeyPacket currentPacket = foundKeys.get(foundKeys.size()-1);
            if (currentPacket.getEndIndex() != query.length()) {
                attemptToParseValue(query, map, currentPacket, query.length());
            } 
            else {
                if(foundKeys.size() < 2) {
                    return null;
                } else {
                    KeyPacket secondLastPacket = foundKeys.get(foundKeys.size()-2);
                    map.remove(secondLastPacket);
                    attemptToParseValue(query, map, secondLastPacket, query.length());
                }
            }
            
            // If no values were successfully parsed then do not attempt to create an
            //  advanced search (should fall back to regular search)
            if (map.size() == 0) {
                return null;
            }
            
            return createAdvancedSearch(map, searchCategory);
        }
        
        return null;        
    }
    
    /**
     * Attempts to parse the value within the bounds and puts it in the map
     *  if possible.
     */
    private static void attemptToParseValue(String query, Map<FilePropertyKey,String> map, 
            KeyPacket currentPacket, int nextIndex) {
        
        // Parse the value between the end of the current key and start of the last
        String value = query.substring(currentPacket.getEndIndex(), 
                nextIndex).trim();
        
        // Only insert into the map if there is data
        if (value.length() > 0) {
            map.put(currentPacket.getAssociatedKey(), value);
        }
    }
    
    private static KeyPacket attemptToFindKey(FilePropertyKey key, String lowerCaseKeyText, 
            String keySeparator, String lowerCaseQuery) {
        return attemptToFindKey(key, lowerCaseKeyText, keySeparator, lowerCaseQuery, 0);
    }
    
    private static KeyPacket attemptToFindKey(FilePropertyKey key, String lowerCaseKeyText, 
            String keySeparator, String lowerCaseQuery, int startSearchFrom) {
        
        int startIndex = lowerCaseQuery.indexOf(lowerCaseKeyText+keySeparator, startSearchFrom);

        if (startIndex > -1) {
            
            // Make sure there is at least a white space before the key so we don't mush things together
            if (startIndex > 0) {
                if (lowerCaseQuery.substring(startIndex-1, startIndex).trim().length() > 0) {
                    // This is probably part of another value and not an actual key
                    return attemptToFindKey(key, lowerCaseKeyText, keySeparator,
                            lowerCaseQuery, startIndex+lowerCaseKeyText.length());
                }
            }
            
            return new KeyPacket(key, startIndex, startIndex+lowerCaseKeyText.length()+keySeparator.length());
        }
        
        return null;
    }
    
    
    public CategoryOverride parseCategoryOverride(String query) {
        String translatedKeySeparator = getTranslatedKeySeprator();
        String untranslatedKeySeparator = UNTRANSLATED_SEPARATOR;
        
        // Only attempt to parse an advanced search if the query has at least one special
        //  key separator sequence
        
        boolean firstSeparatorIsUntranslated = true;
        int firstSeparatorPosition = query.indexOf(untranslatedKeySeparator);
        if (firstSeparatorPosition < 0) { 
            firstSeparatorPosition = query.indexOf(translatedKeySeparator);
            firstSeparatorIsUntranslated = false;
        }
        
        SearchCategory querySelectedSearchCategory = null;
        
        // There should be at least one key separator and it must be forward of the first character
        if (firstSeparatorPosition > 0) {
            
            // Check if the first token is a SearchCategory override
            querySelectedSearchCategory = attemptToParseSearchCategory(
                    query.substring(0, firstSeparatorPosition).trim(),
                    firstSeparatorIsUntranslated);
        
            // If the users language uses the same "colon" as English check again for an override in the 
            //  other language
            if (querySelectedSearchCategory == null && !translator.isCurrentLanguageEnglish()
                    && translatedKeySeparator == untranslatedKeySeparator) {
                
                    querySelectedSearchCategory = attemptToParseSearchCategory(
                            query.substring(0, firstSeparatorPosition).trim(),
                            !firstSeparatorIsUntranslated);
            } 
        }
        
        if (querySelectedSearchCategory == null) {
            return null;
        } 
        else {
            return new CategoryOverride(querySelectedSearchCategory,
                query.substring((firstSeparatorPosition+1)));
        }
    }
    
    private SearchCategory attemptToParseSearchCategory(String firstTerm, boolean firstSeparatorIsUntranslated) {
        if (firstSeparatorIsUntranslated) {
            String candidateTerm = translator.toLowerCaseEnglish(firstTerm);
            for ( SearchCategory category : SearchCategory.values() ) {
                if (translator.toLowerCaseEnglish(
                        SearchCategoryUtils.getUntranslatedName(category)).equals(candidateTerm)) {
                    return category;
                }
            }
        }
        else {
            String candidateTerm = translator.toLowerCaseCurrentLocale(firstTerm);
            for ( SearchCategory category : SearchCategory.values() ) {
                if (translator.toLowerCaseCurrentLocale(translator.translate(
                        SearchCategoryUtils.getUntranslatedName(category))).equals(candidateTerm)) {
                    return category;
                }
            }
        }
        
        return null;
    }
    
    public static class CategoryOverride {
        private final SearchCategory category;
        private final String cutQuery;
        
        public CategoryOverride(SearchCategory category, String cutQuery) {
            this.category = category;
            this.cutQuery = cutQuery;
        }
        public SearchCategory getCategory() {
            return category;
        }
        public String getCutQuery() {
            return cutQuery;
        }
    }
    
    /**
     * Helper class for storing triples matching found key to its location and 
     *  and size within a query. 
     */
    private static class KeyPacket implements Comparable<KeyPacket> {
        private final FilePropertyKey associatedKey;
        private final int startIndex;
        private final int endIndex;
        
        public KeyPacket(FilePropertyKey associatedKey, int startIndex, int endIndex) {
            this.associatedKey = associatedKey;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
        
        public FilePropertyKey getAssociatedKey() {
            return associatedKey;
        }
        
        public int getStartIndex() {
            return startIndex;
        }
        
        public int getEndIndex() {
            return endIndex;
        }

        @Override
        public int compareTo(KeyPacket o) {
            if (startIndex > o.startIndex) {
                return 1;
            } else if (startIndex < o.startIndex) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
