package com.limegroup.gnutella.filters.response;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryReply;

public interface ResponseFilter {
    
    boolean allow(QueryReply qr, Response response);

}
