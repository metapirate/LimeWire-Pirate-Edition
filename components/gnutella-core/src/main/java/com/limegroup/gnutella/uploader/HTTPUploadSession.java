package com.limegroup.gnutella.uploader;

import java.net.InetAddress;

import org.limewire.http.reactor.HttpIOSession;

import com.limegroup.gnutella.BandwidthTrackerImpl;
import com.limegroup.gnutella.uploader.HTTPUploadSessionManager.QueueStatus;

/**
 * Stores the state of an HTTP connection that serves multiple requests.
 * Maintains the queuing status and keeps a reference to an {@link HTTPUploader}
 * that represents the current request.
 */
public class HTTPUploadSession extends BandwidthTrackerImpl implements UploadSlotUser {

    /**
     * The min and max allowed times (in milliseconds) between requests by
     * queued hosts.
     */
    public static final int MIN_POLL_TIME = 45000; // 45 sec

    public static final int MAX_POLL_TIME = 120000; // 120 sec

    private HTTPUploader uploader;

    private final InetAddress host;

    private final UploadSlotManager slotManager;

    /** The last time this session was polled if queued */
    private volatile long lastPollTime;

    private QueueStatus queueStatus = QueueStatus.UNKNOWN;

    private HttpIOSession ioSession;

    public HTTPUploadSession(UploadSlotManager slotManager, InetAddress host, HttpIOSession ioSession) {
        this.slotManager = slotManager;
        this.host = host;
        this.ioSession = ioSession;
    }

    /**
     * Sets the <code>uploader</code> that represents the current request.
     */
    public void setUploader(HTTPUploader uploader) {
        this.uploader = uploader;
    }

    /**
     * Returns the <code>uploader</code> that represents the current request.
     * 
     * @return null, if no request has been handled or the session has been
     *         closed
     */
    public HTTPUploader getUploader() {
        return uploader;
    }

    /**
     * @see UploadSlotManager#positionInQueue(UploadSlotUser)
     */
    int positionInQueue() {
        return slotManager.positionInQueue(this);
    }

    public String getHost() {
        return host.getHostAddress();
    }

    /**
     * @return the same value as {@link #getHost()}
     */
    public InetAddress getConnectedHost() {
        return host;
    }

    /**
     * Notifies the session of a queue poll.
     * 
     * @return true if the poll was too soon
     */
    public boolean poll() {
        long now = System.currentTimeMillis();
        return lastPollTime + MIN_POLL_TIME > now;
    }

    /**
     * Throws an exception since <code>HTTPUploader</code>s are not
     * Interruptible.
     */
    public void releaseSlot() {
        throw new UnsupportedOperationException();        
    }

    public void measureBandwidth() {
        HTTPUploader uploader = getUploader();
        if (uploader != null) {
            // this will invoke HTTPUploadSession.measureBandwidth(int);
            uploader.measureBandwidth();
        }
    }

    public QueueStatus getQueueStatus() {
        return queueStatus;
    }
    
    public void setQueueStatus(QueueStatus status) {
        this.queueStatus = status;
        updatePollTime(status);
    }

    /**
     * Returns true, if the current queue status allows uploading.
     */
    public boolean canUpload() {
        return queueStatus == QueueStatus.ACCEPTED || queueStatus == QueueStatus.BYPASS;
    }

    /**
     * Returns true, if the current queue status is {@link QueueStatus#ACCEPTED}.
     */
    public boolean isAccepted() {
        return queueStatus == QueueStatus.ACCEPTED;
    }

    /**
     * Returns true, if the current queue status is {@link QueueStatus#QUEUED}.
     */
    public boolean isQueued() {
        return queueStatus == QueueStatus.QUEUED;
    }

    public void updatePollTime(QueueStatus status) {
        if (status == QueueStatus.ACCEPTED || status == QueueStatus.BYPASS) {
            lastPollTime = 0;
        } else if (status == QueueStatus.QUEUED) {
            lastPollTime = System.currentTimeMillis();
        }        
    }

    /**
     * Returns the underlying I/O session for the HTTP connection.
     */
    public HttpIOSession getIOSession() {
        return ioSession;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[host=" + getHost() + ",fileName=" + (uploader != null ? uploader.getFileName() : "no uploader yet") + ",queueStatus=" + queueStatus + "]";
    }

}
