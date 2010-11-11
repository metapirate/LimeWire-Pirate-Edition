package com.limegroup.gnutella.connection;

import org.limewire.nio.observer.Shutdownable;

import com.limegroup.gnutella.messages.Message;

/**
 * Basic interface allowing various asynchronous message senders.
 */
public interface OutputRunner extends Shutdownable {
    
    public void send(Message m);

}