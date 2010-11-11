package org.limewire.rudp;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * A selection key for UDP connections.
 */
class UDPSelectionKey extends SelectionKey {
    
    private final Selector selector;
    private final SelectableChannel channel;
    private volatile boolean valid = true;
    
    private int readyOps = 0;
    private volatile int interestOps = 0;
    
    UDPSelectionKey(Selector selector, Object attachment,
                    SelectableChannel channel, int ops) {
        this.selector = selector;
        this.channel = channel;
        interestOps(ops);
        attach(attachment);
    }

    @Override
    public void cancel() {
        valid = false;
    }

    @Override
    public SelectableChannel channel() {
        return channel;
    }

    @Override
    public int interestOps() {
        return interestOps;
    }

    @Override
    public SelectionKey interestOps(int ops) {
        if(!isValid())
            throw new CancelledKeyException();
        
        this.interestOps = ops;
        return this;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public int readyOps() {
        if(!isValid())
            throw new CancelledKeyException();
        return readyOps;
    }
    
    public void setReadyOps(int readyOps) {
        this.readyOps = readyOps;
    }

    @Override
    public Selector selector() {
        return selector;
    }
}
