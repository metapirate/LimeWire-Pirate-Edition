
package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;
/**
 * filters out queries containing hash urns. 
 */
public class HashFilter implements SpamFilter {

    public boolean allow(Message m) {
        if (! (m instanceof QueryRequest))
            return true;

		QueryRequest qr = (QueryRequest)m;
		
        return !qr.hasQueryUrns();
    }

}
