package com.limegroup.gnutella.filters.response;

public interface FilterFactory {
    
    ResponseFilter createResponseFilter();
    
    SearchResultFilter createResultFilter();

}
