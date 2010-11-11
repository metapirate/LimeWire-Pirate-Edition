package com.limegroup.gnutella;

/**
 * Interface to bind list of {@link IncomingMessageHandler}s to a {@link MessageRouter}.
 */
public interface MessageHandlerBinder {

    public void bind(MessageRouter messageRouter);
    
}
