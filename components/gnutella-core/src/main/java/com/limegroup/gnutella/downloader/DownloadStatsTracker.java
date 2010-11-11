package com.limegroup.gnutella.downloader;


/**
 * Used to track statistics about the way clients connect to peers for downloads 
 * (i.e., direct or push), and why pushes are done.
 */
public interface DownloadStatsTracker {
    
    /**
     * Increment a counter indicating connecting directly succeeded.
     */
    void successfulDirectConnect();

    /**
     * Increment a counter indicating connecting directly failed.
     */
    void failedDirectConnect();

    /**
     * Increment a counter indicating connecting with a push succeeded.
     */
    void successfulPushConnect();

    /**
     * Increment a counter indicating connecting directly failed.
     */
    void failedPushConnect();

    /**
     * Increment a counter corresponding to a particular reasons for
     * attempting to connect via a push.
     */
    void increment(PushReason reason);

    /**
     * An enumeration of the reasons for doing a push, as opposed to connecting directly.
     */
    public enum PushReason {DIRECT_FAILED, MULTICAST_REPLY, PRIVATE_NETWORK, INVALID_PORT, FIREWALL}
}
