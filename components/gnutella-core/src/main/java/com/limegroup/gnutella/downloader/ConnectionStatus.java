package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.tigertree.HashTree;

/**
 * Simple class that enumerates values for the status of
 * requesting a file.
 *
 * Possible options are:
 * <ul>
 * <li>NoFile (the server is not giving us the file)</li>
 * <li>Queued (the server queued us)</li>
 * <li>Connected (we are connected and should download)</li>
 * <li>NoData (we have no data to request)</li>
 * <li>PartialData (the server has other data to use)</li>
 * <li>ThexResponse (the server just gave us a HashTree)</li>
 * </ul>
 */
public class ConnectionStatus {
    
    enum StatusType {
        NO_FILE,
        QUEUED,
        CONNECTED,
        NO_DATA,
        PARTIAL_DATA,
        THEX_RESPONSE;
    }
    
    /** The status of this connection. */
    private final StatusType STATUS;

    /** The queue position. Only valid if queued. */
    private final int QUEUE_POSITION;

    /** The queue poll time. Only valid if queued. */
    private final int QUEUE_POLL_TIME;

    /** The hash tree. Only valid if thex response. */
    private final HashTree HASH_TREE;
    
    /** The code that caused this status, -1 if unknown. */
    private final int CODE;
    
    /** The sole NO_FILE instance. */
    private static final ConnectionStatus NO_FILE = new ConnectionStatus(StatusType.NO_FILE);

    /** The sole CONNECTED instance. */
    private static final ConnectionStatus CONNECTED = new ConnectionStatus(StatusType.CONNECTED);

    /** The sole NO_DATA instance. */
    private static final ConnectionStatus NO_DATA = new ConnectionStatus(StatusType.NO_DATA);

    /** The sole PARTIAL_DATA instance. */
    private static final ConnectionStatus PARTIAL_DATA = new ConnectionStatus(StatusType.PARTIAL_DATA);
       
    /** Constructs a ConnectionStatus of the specified status. */
    private ConnectionStatus(StatusType status) {
        if(status == StatusType.QUEUED || status == StatusType.THEX_RESPONSE)
            throw new IllegalArgumentException();
        STATUS = status;
        QUEUE_POSITION = -1;
        QUEUE_POLL_TIME = -1;
        CODE = -1;
        HASH_TREE = null;
    }
    
    /** Constructs a ConnectionStatus for being queued. */
    private ConnectionStatus(StatusType status, int queuePos, int queuePoll) {
        if(status != StatusType.QUEUED)
            throw new IllegalArgumentException();
            
        STATUS = status;
        QUEUE_POSITION = queuePos;
        QUEUE_POLL_TIME = queuePoll;
        CODE = -1;
        HASH_TREE = null;
    }
    
    private ConnectionStatus(StatusType status, HashTree tree) {
        if(status != StatusType.THEX_RESPONSE)
            throw new IllegalArgumentException();
        if(tree == null)
            throw new NullPointerException("null tree");
            
        STATUS = status;
        HASH_TREE = tree;
        CODE = -1;
        QUEUE_POSITION = -1;
        QUEUE_POLL_TIME = -1;
    }    
    
    /**
     * Returns a ConnectionStatus for the server not having the file.
     */
    static ConnectionStatus getNoFile() {
        return NO_FILE;
    }
    
    /**
     * Returns a ConnectionStatus for being connected.
     */
    static ConnectionStatus getConnected() {
        return CONNECTED;
    }
    
    /**
     * Returns a ConnectionStatus for us not having data.
     */
    static ConnectionStatus getNoData() {
        return NO_DATA;
    }
    
    /**
     * Returns a ConnectionStatus for the server having other partial data.
     */
    static ConnectionStatus getPartialData() {
        return PARTIAL_DATA;
    }
    
    /**
     * Returns a ConnectionStatus for being queued with the specified position
     * and poll time (in seconds).
     */
    static ConnectionStatus getQueued(int pos, int poll) {
        // convert to milliseconds & add an extra second.
        poll *= 1000;
        poll += 1000;
        return new ConnectionStatus(StatusType.QUEUED, pos, poll);
    }
    
    /**
     * Returns a ConnectionStatus for having a THEX tree.
     */
    static ConnectionStatus getThexResponse(HashTree tree) {
        return new ConnectionStatus(StatusType.THEX_RESPONSE, tree);
    }
    
    /**
     * Returns the type of this ConnectionStatus.
     */
    StatusType getType() {
        return STATUS;
    }
    
    /**
     * Determines if this is a NoFile ConnectionStatus.
     */
    boolean isNoFile() {
        return STATUS == StatusType.NO_FILE;
    }
    
    /**
     * Determines if this is a Connected ConnectionStatus.
     */    
    public boolean isConnected() {
        return STATUS == StatusType.CONNECTED;
    }
    
    /**
     * Determines if this is a NoData ConnectionStatus.
     */
    boolean isNoData() {
        return STATUS == StatusType.NO_DATA;
    }
    
    /**
     * Determines if this is a PartialData ConnectionStatus.
     */
    boolean isPartialData() {
        return STATUS == StatusType.PARTIAL_DATA;
    }
    
    /**
     * Determines if this is a Queued ConnectionStatus.
     */
    boolean isQueued() {
        return STATUS == StatusType.QUEUED;
    }
    
    /**
     * Determines if this is a ThexResponse ConnectionStatus.
     */
    public boolean isThexResponse() {
        return STATUS == StatusType.THEX_RESPONSE;
    }
    
    /**
     * Determines the queue position.  Throws IllegalStateException if called
     * when the status is not queued.
     */
    int getQueuePosition() {
        if(!isQueued())
            throw new IllegalStateException();
        return QUEUE_POSITION;
    }
    
    /**
     * Determines the queue poll time (in milliseconds).
     * Throws IllegalStateException if called when the status is not queued.
     */
    int getQueuePollTime() {
        if(!isQueued())
            throw new IllegalStateException();
        return QUEUE_POLL_TIME;
    }
    
    /**
     * Returns the HashTree.
     * Throws IllegalStateException if called when the status is not ThexResponse.
     */
    public HashTree getHashTree() {
        if(!isThexResponse())
            throw new IllegalStateException();
        return HASH_TREE;
    }
    
    /** Returns the HTTP response code that caused this status, or -1 if unknown. */
    public int getCode() {
        return CODE;
    }

    @Override
    public String toString() {
        return STATUS.toString();
    }
}
