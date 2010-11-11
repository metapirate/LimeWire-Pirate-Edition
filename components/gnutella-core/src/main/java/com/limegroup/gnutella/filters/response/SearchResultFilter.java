package com.limegroup.gnutella.filters.response;

import org.limewire.core.api.search.SearchResult;

import com.limegroup.gnutella.xml.LimeXMLDocument;

public interface SearchResultFilter {

    boolean allow(SearchResult result, LimeXMLDocument document);
    
}
