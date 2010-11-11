package com.limegroup.gnutella.connection;


import com.limegroup.gnutella.messages.Message;

/**
 * A priority queue for messages. Subclasses override the add,
 * removeNextInternal, and size template methods to implement different
 * prioritization policies.  NOT THREAD SAFE.<p>
 *
 * This class is designed for speed; hence the somewhat awkward use of
 * resetCycle/extractMax instead of a simple iterator.  Likewise, this class has
 * a resetDropped() method instead of returning a (Message, int) pair in
 * removeNext(); 
 */
public abstract class AbstractMessageQueue implements MessageQueue {
    /** The number of messages per cycle, and number left for this cycle. 
     *  INVARIANT: 0<=_leftInCycle<=cycleSize */
    private final int _cycleSize;
    private int _leftInCycle;
    /** The oldest message to return, in milliseconds. */
    private int _timeout;
    /** The number of messages dropped since the last call to resetDropped(). */
    protected int _dropped;

    /**
     * @param cycle the number of messages to return per cycle, i.e., between 
     *  calls to resetCycle.  This is used to tweak the ratios of various 
     *  message types.
     * @param timeout the max time to keep queued messages, in milliseconds.
     *  Set this to Integer.MAX_VALUE to avoid timeouts.
     */
    protected AbstractMessageQueue(int cycle, int timeout) 
            throws IllegalArgumentException {
        if (timeout<=0)
            throw new IllegalArgumentException("Timeout too small: "+cycle);
        if (cycle<=0)
            throw new IllegalArgumentException("Cycle too small: "+cycle);

        this._cycleSize=cycle;
        this._leftInCycle=cycle;
        this._timeout=timeout;
    }

    /** 
     * Adds m to this.  Message may be dropped in the process; find out how many
     * by calling resetDropped().
     */
    public void add(Message m) {
        Message dropmsg = addInternal(m);
        if (dropmsg != null) {
            drop(dropmsg);
        }
    }
    
    private void drop(Message m) {
        _dropped++;
    }
    
    /**
     * Add m to this, returns any message that had to dropped to make room in
     * a queue.
     */
    protected abstract Message addInternal(Message m);

    /** 
     * Removes and returns the next message to send from this during this cycle.
     * Returns null if there are no more messages to send in this cycle.  The
     * returned message is guaranteed be younger than TIMEOUT milliseconds.
     * Messages may be dropped in the process; find out how many by calling
     * resetDropped().  (Subclasses should implement the removeNextInternal
     * method and be sure to update the _dropped field if necessary.)  
     * @return the next message, or null if none
     */
    public Message removeNext() {
        if (_leftInCycle==0)
            return null;         //Nothing left for cycle.
        
        long expireTime=System.currentTimeMillis()-_timeout;
        while (true) {
            Message m=removeNextInternal();
            if (m==null)
                return null;     //Nothing left, give up.
            if (m.getCreationTime()<expireTime) {
                drop(m);
                continue;        //Too old.  Keep searching.
            }

            _leftInCycle--;
            return m;            //Normal case.
        }
    }  

    /** Same as removeNext, but ignores message age and cycle. 
     *  @return the next message to send, or null if this is empty */
    protected abstract Message removeNextInternal();
      
    /** Resets the cycle counter used to control removeNext(). */
    public void resetCycle() {
        this._leftInCycle=_cycleSize;
    }

    /** Returns the number of dropped messages since the last call to
     *  resetDropped(). */
    public final int resetDropped() {
        int ret=_dropped;
        _dropped=0;
        return ret;
    }

    /** Returns the number of queued messages. */
    public abstract int size();

    /** Returns true if this has any queued messages. */
    public boolean isEmpty() {
        return size()==0;
    }

    //No unit tests; this code is covered by tests in ManagedConnection.
    //(Actually most of this code used to be in ManagedConnection.)
}
