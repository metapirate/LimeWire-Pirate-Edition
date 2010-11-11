package com.limegroup.gnutella.filters;

import org.limewire.io.GUID;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;

/** 
 * Blocks over-zealous automated requeries.
 */
public class RequeryFilter implements SpamFilter {
    public boolean allow(Message m) {
        if (m instanceof QueryRequest)
            return allow((QueryRequest)m);
        else
            return true;        
    }

    private boolean allow(QueryRequest q) {
        //Kill automated requeries from LW 2.3 and earlier.
        byte[] guid=q.getGUID();
        if (GUID.isLimeGUID(guid)) {
            if (GUID.isLimeRequeryGUID(guid, 0)             //LW 2.2.0-2.2.3
                    || GUID.isLimeRequeryGUID(guid, 1)) {   //LW 2.2.4-2.3.x
                return false;
            }
        }
        return true;
    }
}
