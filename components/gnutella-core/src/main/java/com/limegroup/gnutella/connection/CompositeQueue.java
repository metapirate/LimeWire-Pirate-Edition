package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.VendorMessage;

/**
 * A queue of messages organized by type.  Used by ManagedConnection to
 * implement the SACHRIFC flow control algorithm.  Delegates to multiple
 * MessageQueues, making sure that no one type of message dominates traffic.
 */
public class CompositeQueue implements MessageQueue {
    /*
     * IMPLEMENTATION NOTE: this class uses the SACHRIFC algorithm described at
     * http://wiki.limewire.org/index.php?title=Sachrifc .  The basic idea is to use
     * one buffer for each message type.  Messages are removed from the buffers in
     * a biased round-robin fashion.  This prioritizes some messages types while
     * preventing any one message type from dominating traffic.  Query replies
     * are further prioritized by "GUID volume", i.e., the number of bytes
     * already routed for that GUID.  Other messages are sorted by time and
     * removed in a LIFO [sic] policy.  This, coupled with timeouts, reduces
     * latency.  
     */
    
    ///////////////////////////////// Parameters //////////////////////////////


    /**
     * The producer's queues, one priority per message type. 
     *  INVARIANT: _queues.length==PRIORITIES
     */
    private MessageQueue[] _queues = new MessageQueue[PRIORITIES];
    
    /**
     * The number of queued messages.  Maintained for performance.
     *  INVARIANT: _queued == sum of _queues[i].size() 
     */
    private int _queued = 0;
    
    /**
     * The current priority of the queue we're looking at.  Necessary to preserve
     * over multiple iterations of removeNext to ensure the queue is extracted in
     * order, though not necessary to ensure all messages are correctly set.
     * As an optimization, if a message is the only one queued, _priority is set
     * to be that message's queued.
     */
    private int _priority = 0;
    
    /**
     * The priority of the last message that was added.  If removeNext detects
     * that it has gone through a cycle (and everything returned null), it marks
     * the next removeNext to use the priorityHint to jump-start on the last
     * added message.
     */
    private int _priorityHint = 0;
    
    /**
     * The status of removeNext.  True if the last call was a complete cycle
     * through all potential fields.
     */
    private boolean _cycled = true;
    
    /**
     * The number of messages we've dropped while adding or retrieving messages.
     */
    private int _dropped = 0;
    
    /**
     * A larger queue size than the standard to accommodate higher priority 
     *  messages, such as queries and query hits.
     */
    private static final int BIG_QUEUE_SIZE = 100;

    /**
     * The standard queue size for smaller messages so that we don't waste too
     * much memory on lower priority messages. */
    private static final int QUEUE_SIZE = 1;
    
    /** The max time to keep reply messages and pushes in the queues, in milliseconds. */
    private static final int BIG_QUEUE_TIME=10*1000;
    
    /** The max time to keep queries, pings, and pongs in the queues, in milliseconds. */
    public static final int QUEUE_TIME=5*1000;
    
    /** The number of different priority levels. */
    private static final int PRIORITIES = 9;
    
    /** 
     * Names for each priority. "Other" includes QRP messages and is NOT
     * reordered.  These numbers do NOT translate directly to priorities;
     * that's determined by the cycle fields passed to MessageQueue.
     */
    private static final int PRIORITY_CONTROL=0;
    private static final int PRIORITY_WATCHDOG=1;
    private static final int PRIORITY_PUSH=2;
    private static final int PRIORITY_QUERY_REPLY=3;
    private static final int PRIORITY_QUERY=4; 
    private static final int PRIORITY_PING_REPLY=5;
    private static final int PRIORITY_PING=6;
    private static final int PRIORITY_OTHER=7;    
    private static final int PRIORITY_OUR_QUERY=8 ; // Separate for re-originated leaf-queries.
    
    /**
     * Constructs a new queue with the default sizes.
     */
    public CompositeQueue() {
        this(BIG_QUEUE_TIME, BIG_QUEUE_SIZE, QUEUE_TIME, QUEUE_SIZE);
    }

    /** 
     * Constructs a new queue with the given message buffer sizes. 
     */
    public CompositeQueue(int largeTime, int largeSize, int normalTime, int normalSize) {
        _queues[PRIORITY_CONTROL]     = new SimpleMessageQueue(10, Integer.MAX_VALUE, largeSize, false);
        _queues[PRIORITY_WATCHDOG]    = new SimpleMessageQueue(1, Integer.MAX_VALUE, largeSize, true); // LIFO
        _queues[PRIORITY_PUSH]        = new PriorityMessageQueue(6, largeTime, largeSize);
        _queues[PRIORITY_QUERY_REPLY] = new PriorityMessageQueue(6, largeTime, largeSize);
        _queues[PRIORITY_QUERY]       = new PriorityMessageQueue(3, normalTime, largeSize);
        _queues[PRIORITY_PING_REPLY]  = new PriorityMessageQueue(1, normalTime, normalSize);
        _queues[PRIORITY_PING]        = new PriorityMessageQueue(1, normalTime, normalSize);
        _queues[PRIORITY_OUR_QUERY]   = new PriorityMessageQueue(10, largeTime, largeSize);
        _queues[PRIORITY_OTHER]       = new SimpleMessageQueue(1, Integer.MAX_VALUE, largeSize, false); // FIFO
    }                                                             

    /** 
     * Adds m to this, possibly dropping some messages in the process; call
     * resetDropped to get the count of dropped messages.
     * @see resetDropped 
     */
    public void add(Message m) {
        //Add m to appropriate buffer
        int priority = calculatePriority(m);
        MessageQueue queue = _queues[priority];
        queue.add(m);

        //Update statistics
        int dropped = queue.resetDropped();
        _dropped += dropped;
        _queued += 1-dropped;
        
        // Remember the priority so we can set it if we detect we cycled.
        _priorityHint = priority;
    }

    /** 
     * Returns the send priority for the given message, with higher number for
     * higher priorities.  
     */
    /* TODO: this method will eventually be moved to
     * MessageRouter and account for number of reply bytes.
     */
    private int calculatePriority(Message m) {
        if (m instanceof VendorMessage.ControlMessage)
            return PRIORITY_CONTROL;
        byte opcode=m.getFunc();
        switch (opcode) {
            case Message.F_QUERY:
                return ((QueryRequest)m).isOriginated() ? 
                    PRIORITY_OUR_QUERY : PRIORITY_QUERY;
            case Message.F_QUERY_REPLY: 
                return PRIORITY_QUERY_REPLY;
            case Message.F_PING_REPLY: 
                return (m.getHops()==0 && m.getTTL()<=2) ? 
                    PRIORITY_WATCHDOG : PRIORITY_PING_REPLY;
            case Message.F_PING: 
                return (m.getHops()==0 && m.getTTL()==1) ? 
                    PRIORITY_WATCHDOG : PRIORITY_PING;
            case Message.F_PUSH: 
                return PRIORITY_PUSH;                
            default: 
                return PRIORITY_OTHER;  //includes QRP Tables
        }
    }

    /** 
     * Removes and returns the next message to send from this.  Returns null if
     * there are no more messages to send.  The returned message is guaranteed
     * be younger than TIMEOUT milliseconds.  Messages may be dropped in the
     * process; find out how many by calling resetDropped().  For this reason
     * note that size()>0 does not imply that removeNext()!=null.
     * @return the next message, or null if none
     * @see resetDropped
     */
    public Message removeNext() {
        if(_cycled) {
            _cycled = false;
            _priority = _priorityHint;
            _queues[_priority].resetCycle();
        }
        
        //Try all priorities in a round-robin fashion until we find a
        //non-empty buffer.  This degenerates in performance if the queue
        //contains only a single type of message.
        while (_queued > 0) {
            MessageQueue queue = _queues[_priority];
            //Try to get a message from the current queue.
            Message m = queue.removeNext();
            int dropped = queue.resetDropped();
            _dropped += dropped;
            _queued -= (m == null ? 0 : 1) + dropped;  //maintain invariant
            if (m != null)
                return m;

            //No luck?  Go on to next queue.
            _priority = (_priority + 1) % PRIORITIES;
            _queues[_priority].resetCycle();
        }

        _cycled = true;
        
        //Nothing to send.
        return null;
    }

    /** 
     * Returns the number of dropped messages since the last call to
     * resetDropped().
     */
    public final int resetDropped() { 
        int ret = _dropped;
        _dropped = 0;
        return ret;
    }

    /** 
     * Returns the number of messages in this.  Note that size()>0 does not
     * imply that removeNext()!=null; messages may be expired upon sending.
     */
    public int size() { 
        return _queued;
    }
    
    /** Determines if this is empty. */
    public boolean isEmpty() { return _queued == 0; }
    
    /** Does nothing. */
    public void resetCycle() {}
}

