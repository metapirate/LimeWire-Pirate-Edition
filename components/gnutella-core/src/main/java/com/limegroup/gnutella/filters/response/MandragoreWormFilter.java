package com.limegroup.gnutella.filters.response;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.messages.QueryReply;

@Singleton
class MandragoreWormFilter implements ResponseFilter {

    private final SearchServices searchServices;
    
    @Inject public MandragoreWormFilter(SearchServices searchServices) {
        this.searchServices = searchServices;
    }
    
    @Override
    public boolean allow(QueryReply qr, Response response) {
        return !searchServices.isMandragoreWorm(qr.getGUID(), response);
    }
    
}
