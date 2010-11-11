package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.messages.Message;

/**
 * A queue of messages.
 */
public interface MessageQueue {
    
    
    /** Adds a new message. */
    public void add(Message m);
    
    /** Removes the next message. */
    public Message removeNext();
    
    /** Resets the amount of messages dropped, returning the current value. */
    public int resetDropped();
    
    /** Gets the current size of queued messages.  Does not guarantee one will be returned. */
    public int size();
    
    /** Resets the number of messages in the cycle.  Optional operation. */
    public void resetCycle();
    
    /** Determines if this is empty. */
    public boolean isEmpty();
}