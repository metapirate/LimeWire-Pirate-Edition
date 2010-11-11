package com.limegroup.gnutella.messagehandlers;

import java.util.Arrays;
import java.util.Set;

import org.limewire.collection.IntSet;
import org.limewire.io.GUID;
import org.limewire.security.SecurityToken;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;

/**
 * A session of OOB result exchange between the local host and a remote host.
 */
class OOBSession {
    
    private final SecurityToken token;
    private final IntSet urnHashCodes;
    
    private IntSet responseHashCodes;
    
    private final int requestedResponseCount;
    private final GUID guid;
    
    OOBSession(SecurityToken token, int requestedResponseCount, GUID guid) {
        this.token = token;
        this.requestedResponseCount = requestedResponseCount;
        this.urnHashCodes = new IntSet(requestedResponseCount);
        this.guid = guid;
	}
    
    GUID getGUID() {
        return guid;
    }
	
    /**
     * Counts the responses uniquely. 
     */
    int countAddedResponses(Response[] responses) {
        int added = 0;
        for (Response response : responses) {
            Set<URN> urns = response.getUrns();
            if (!urns.isEmpty()) {
                added += urnHashCodes.add(urns.iterator().next().hashCode()) ? 1 : 0;
            }
            else {
                // create lazily since responses should have urns
                if (responseHashCodes == null) {
                    responseHashCodes = new IntSet();
                }
                added += responseHashCodes.add(response.hashCode()) ? 1 : 0;
            }
        }
        
        return added;
    }
    
    /**
     * Returns the number of results that are still expected to come in.
     */
    final int getRemainingResultsCount() {
        return requestedResponseCount - urnHashCodes.size() - (responseHashCodes != null ? responseHashCodes.size() : 0); 
    }
    
	@Override
    public boolean equals(Object o) {
		if (! (o instanceof OOBSession))
			return false;
		OOBSession other = (OOBSession) o;
		return Arrays.equals(token.getBytes(), other.token.getBytes());
	}
}