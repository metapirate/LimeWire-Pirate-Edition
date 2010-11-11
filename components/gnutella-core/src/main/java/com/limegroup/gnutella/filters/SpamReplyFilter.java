package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;

/** 
 * Filter for replies that are abusing the network.
 */
public class SpamReplyFilter implements SpamFilter {

    public boolean allow(Message m) {
        if (! (m instanceof QueryReply))
            return true;

        String vendor = ((QueryReply) m).getVendor();
        return !vendor.equals("MUTE");
    }

}
