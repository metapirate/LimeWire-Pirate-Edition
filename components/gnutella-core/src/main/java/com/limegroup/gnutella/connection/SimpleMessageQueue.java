package com.limegroup.gnutella.connection;

import org.limewire.collection.Buffer;

import com.limegroup.gnutella.messages.Message;


/**
 * Simple LIFO or FIFO message queue.
 */
public class SimpleMessageQueue extends AbstractMessageQueue {
    private Buffer<Message> _buf;
    private boolean _lifo;
    
    /**
     * @param cycle the number of messages to return per cycle, i.e., between 
     *  calls to resetCycle.  This is used to tweak the ratios of various 
     *  message types.
     * @param timeout the max time to keep queued messages, in milliseconds.
     *  Set this to Integer.MAX_VALUE to avoid timeouts.
     * @param capacity the maximum number of elements this can store.
     * @param lifo true if this is last-in-first-out, false if this is 
     *  first-in-first-out.
     */
    public SimpleMessageQueue(int cycle, 
                                 int timeout, 
                                 int capacity, 
                                 boolean lifo) {
        super(cycle, timeout);
        this._buf=new Buffer<Message>(capacity);
        this._lifo=lifo;
    }

    @Override
    protected Message addInternal(Message m) {
        return _buf.addLast(m);
    }

    @Override
    protected Message removeNextInternal() {
        if (_buf.isEmpty())
            return null;

        if (_lifo)
            return _buf.removeLast();
        else
            return _buf.removeFirst();
    }
    
    @Override
    public int size() {
        return _buf.size();
    }
}
