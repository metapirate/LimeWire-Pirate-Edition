package com.limegroup.gnutella.connection;

/**
 * Defines the interface to retrieve statistics about the number of messages
 * sent or dropped over a {@link Connection}.
 */
public interface ConnectionMessageStatistics {

    /** Returns the number of queries received over this connection. */
    public long getNumQueriesReceived();
    
    /** Returns the number of query replies received over this connection. */
    public long getNumQueryReplies();

    /**
     * A callback for the ConnectionManager to inform this connection that a
     * message was dropped. This happens when a reply received from this
     * connection has no routing path.
     */
    public void countDroppedMessage();

    /** Returns the number of messages sent on this connection. */
    public int getNumMessagesSent();

    /** Returns the number of messages received on this connection. */
    public int getNumMessagesReceived();

    /**
     * Returns the number of messages I dropped while trying to send on this
     * connection. This happens when the remote host cannot keep up with me.
     */
    public int getNumSentMessagesDropped();

    /**
     * The number of messages received on this connection either filtered out or
     * dropped because we didn't know how to route them.
     */
    public long getNumReceivedMessagesDropped();

}
