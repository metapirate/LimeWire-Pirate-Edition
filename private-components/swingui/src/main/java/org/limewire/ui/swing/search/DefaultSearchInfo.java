package org.limewire.ui.swing.search;

import java.util.Collections;
import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.StringUtils;

public class DefaultSearchInfo implements SearchInfo {

    private final String title;
    private final String query;
    private final SearchCategory searchCategory;
    private final SearchType searchType;
    private final Map<FilePropertyKey, String> advancedSearch;
    
    /** Creates a new SearchInfo for the given single keyword search. */
    public static DefaultSearchInfo createKeywordSearch(String query, SearchCategory searchCategory) {
        return new DefaultSearchInfo(query, query, Collections.<FilePropertyKey, String>emptyMap(), searchCategory, SearchType.KEYWORD);
    }
    
    /** Creates a new SearchInfo for the given advanced search. */
    public static DefaultSearchInfo createAdvancedSearch(String title, Map<FilePropertyKey, String> advancedSearch, SearchCategory searchCategory) {
        return new DefaultSearchInfo(title, "", advancedSearch, searchCategory, SearchType.KEYWORD);
    }
    
    public static DefaultSearchInfo createBrowseSearch(SearchType type){
        assert(type.isBrowse());
        return new DefaultSearchInfo("", "", Collections.<FilePropertyKey, String>emptyMap(), SearchCategory.ALL, type);
    }
    
    
    /** Creates a new SearchInfo for the given What's New search. */
    public static DefaultSearchInfo createWhatsNewSearch(SearchCategory searchCategory) {
        String title;
        switch(searchCategory) {
        case AUDIO: title = I18n.tr("New audio"); break;
        case DOCUMENT: title = I18n.tr("New documents"); break;
        case IMAGE: title = I18n.tr("New images"); break;
        case PROGRAM: title = I18n.tr("New programs"); break;
        case VIDEO: title = I18n.tr("New videos"); break;
        case TORRENT: title = I18n.tr("New torrents"); break;
        case OTHER:
        case ALL:
        default: title = I18n.tr("New files"); break;
        }
        return new DefaultSearchInfo(title, null, Collections.<FilePropertyKey, String>emptyMap(), searchCategory, SearchType.WHATS_NEW);
    }

    private DefaultSearchInfo(String title, String query, Map<FilePropertyKey, String> advancedSearch, SearchCategory searchCategory, SearchType searchType) {
        this.title = title;
        this.query = query;
        this.advancedSearch = advancedSearch;
        this.searchCategory = searchCategory;
        this.searchType = searchType;
    }

    @Override
    public String getSearchQuery() {
        return query;
    }
    
    @Override
    public Map<FilePropertyKey, String> getAdvancedDetails() {
        return advancedSearch;
    }

    @Override
    public String getTitle() {
        return title;
    }
    
    @Override
    public SearchCategory getSearchCategory() {
        return searchCategory;
    }
    
    @Override
    public SearchType getSearchType() {
        return searchType;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}
