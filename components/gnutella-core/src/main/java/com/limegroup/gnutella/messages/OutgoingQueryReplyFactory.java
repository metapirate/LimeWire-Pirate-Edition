package com.limegroup.gnutella.messages;

import java.util.List;

import org.limewire.security.SecurityToken;

import com.limegroup.gnutella.Response;

public interface OutgoingQueryReplyFactory {
    
    public List<QueryReply> createReplies(Response[] responses, QueryRequest queryRequest,
            SecurityToken securityToken, int responsesPerReply);
    
    List<QueryReply> createReplies(Response[] responses, int responsesPerReply,
            SecurityToken securityToken, byte[] guid, byte ttl, boolean isMulticast,
            boolean requestorCanDoFWT);
    
    /**
     * @return the compressed xml bytes for a response, can be empty byte array
     */
    byte[] getCompressedXmlBytes(Response response);
}
