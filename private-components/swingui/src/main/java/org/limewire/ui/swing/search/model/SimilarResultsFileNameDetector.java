package org.limewire.ui.swing.search.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.limewire.core.api.search.SearchResult;

/**
 * For every file name found in the search the parent that matches that filename
 * is put into the matchCache. A single parent might have more than 1 key. As a
 * new search result comes in, its fileNAmes are found, the parent matching him
 * is taken from the cache, and then a new parent is chosen between the two. The
 * new parent is then put in the cache for all the relevant filenames. Because
 * of ordering issues when processing items, a child might end up in the cache.
 * But when selecting a new parent, the findParent method checks items parents
 * as well. This prevents the data from being wrong when setting parents on
 * other visual search results.
 */
public class SimilarResultsFileNameDetector extends AbstractNameSimilarResultsDetector {
    private static final String REPLACE = "\\(\\d\\)|[-_.' ()]";

    public SimilarResultsFileNameDetector() {
        super(Pattern.compile(REPLACE));
    }

    @Override
    public Set<String> getCleanIdentifyingStrings(VisualSearchResult visualSearchResult) {
        List<SearchResult> coreResults = visualSearchResult.getCoreSearchResults();
        Set<String> cleanFileNames = new HashSet<String>();
        for (SearchResult searchResult : coreResults) {
            String cleanFileName = nameCache.cleanString(searchResult.getFileName());
            cleanFileNames.add(cleanFileName);
        }
        return cleanFileNames;
    }
}
