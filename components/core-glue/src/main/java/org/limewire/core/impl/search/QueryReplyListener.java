package org.limewire.core.impl.search;

import java.util.Set;

import org.limewire.io.IpPort;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.messages.QueryReply;

public interface QueryReplyListener {
    
    void handleQueryReply(RemoteFileDesc rfd, QueryReply queryReply, Set<? extends IpPort> locs);

}
