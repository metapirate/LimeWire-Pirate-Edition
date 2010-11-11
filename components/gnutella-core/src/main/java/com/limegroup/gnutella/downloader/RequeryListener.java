package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RequeryManager.QueryType;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * A listener that RequeryManager uses, to inform
 * of state-changing on the requerying.
 */
interface RequeryListener {

    /** Notification that the current lookup has finished. */
    void lookupFinished(QueryType queryType);
    //requeryListener.setStateIfExistingStateIs(DownloadStatus.GAVE_UP, DownloadStatus.QUERYING_DHT);

    /** Notification that lookup has started (and will take some time) */
    void lookupStarted(QueryType queryType, long lookupLength);
    //   requeryListener.setState(DownloadStatus.QUERYING_DHT, length)
    //   requeryListener.setState(DownloadStatus.WAITING_FOR_GNET_RESULTS, length)

    /** Returns the SHA1 URN this listener is listening for. */
    URN getSHA1Urn();

    /** Requests the listener to create a query that can be sent. */
    QueryRequest createQuery();

    /** Notification that a lookup is pending. */
    void lookupPending(QueryType gnutella, int connectingWaitTime);
    // requeryListener.setState(DownloadStatus.WAITING_FOR_CONNECTIONS, CONNECTING_WAIT_TIME);
    
    
    

}
