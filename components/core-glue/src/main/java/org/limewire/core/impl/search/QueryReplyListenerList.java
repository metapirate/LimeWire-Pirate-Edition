package org.limewire.core.impl.search;

public interface QueryReplyListenerList {
    
    void addQueryReplyListener(byte[] guid, QueryReplyListener listener);
    
    void removeQueryReplyListener(byte[] guid, QueryReplyListener listener);

}
