package com.limegroup.gnutella.connection;

import java.util.LinkedList;
import java.util.List;

import com.limegroup.gnutella.messages.Message;

/**
 * A very basic queue of messages.
 * <p>
 * All messages are FIFO.
 */
public class BasicQueue implements MessageQueue {
    
    private List<Message> QUEUE = new LinkedList<Message>();
    
    
    /** Adds a new message. */
    public void add(Message m) {
        QUEUE.add(m);
    }
    
    /** Removes the next message. */
    public Message removeNext() {
        if(QUEUE.isEmpty())
            return null;
        else
            return QUEUE.remove(0);
    }
    
    /** No-op. */
    public int resetDropped() { return 0; }
        
    
    /** Returns the number of queued messages. */
    public int size() {
        return QUEUE.size();
    }
    
    /** No op. */
    public void resetCycle() {}
    
    /** Determines if this is empty. */
    public boolean isEmpty() {
        return QUEUE.isEmpty();
    }
    
    public Object getDroppedStats() {
        return "basic queue";
    }
    
}