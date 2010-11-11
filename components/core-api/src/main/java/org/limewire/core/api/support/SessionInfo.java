package org.limewire.core.api.support;

public interface SessionInfo {

    public int getNumberOfPendingTimeouts();

    /**
     * Returns the number of downloads waiting to be started.
     */
    public int getNumWaitingDownloads();

    /**
     * Returns the number of individual downloaders.
     */
    public int getNumIndividualDownloaders();

    /**
     * Returns the current uptime.
     */
    public long getCurrentUptime();
    
    /**
     * @return the clock time when uptime began recording from.
     */
    public long getStartTime();

    /**
     * Returns the number of active ultrapeer -> leaf connections.
     */
    public int getNumUltrapeerToLeafConnections();

    /**
     * Returns the number of leaf -> ultrapeer connections.
     */
    public int getNumLeafToUltrapeerConnections();

    /**
     * Returns the number of ultrapeer -> ultrapeer connections.
     */
    public int getNumUltrapeerToUltrapeerConnections();

    /**
     * Returns the number of old unrouted connections.
     */
    public int getNumOldConnections();

    public long getCreationCacheSize();

    public long getDiskControllerByteCacheSize();

    public long getDiskControllerVerifyingCacheSize();

    public int getDiskControllerQueueSize();

    public long getByteBufferCacheSize();

    public int getNumberOfWaitingSockets();
    
    /**
     * Returns whether or not this node is capable of sending its own
     * GUESS queries.  This would not be the case only if this node
     * has not successfully received an incoming UDP packet.
     *
     * @return <tt>true</tt> if this node is capable of running its own
     *  GUESS queries, <tt>false</tt> otherwise
     */
    public boolean isGUESSCapable();
    
    public boolean canReceiveSolicited();
    
    public boolean acceptedIncomingConnection();
    
    public int getPort();

    public boolean isConnected();

    public boolean isSupernode();

    public boolean isShieldedLeaf();

    public int getNumQueuedUploads();

    public int getSharedFileListSize();
    
    public int getManagedFileListSize();
    
    public int getAllFriendsFileListSize();

    public int getNumActiveDownloads();

    public boolean isUdpPortStable();

    public boolean canDoFWT();

    public int lastReportedUdpPort();

    public int receivedIpPong();

    public int getNumConnectionCheckerWorkarounds();

    public String getUploadSlotManagerInfo();

    public long[] getSelectStats();

    public int getNumActiveUploads();

    public boolean isLifecycleLoaded();

}