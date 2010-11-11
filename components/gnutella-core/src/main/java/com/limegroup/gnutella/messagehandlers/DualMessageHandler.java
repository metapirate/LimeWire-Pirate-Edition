package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.Message;

/** A message handler that wraps two others. */
public class DualMessageHandler implements MessageHandler {

    private final MessageHandler a;
    private final MessageHandler b;
    
    public DualMessageHandler(MessageHandler a, MessageHandler b) {
        this.a = a;
        this.b = b;
    }
    
    public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
        a.handleMessage(msg, addr, handler);
        b.handleMessage(msg, addr, handler);
    }
    
    @Override
    public String toString() {
        return "DualHandler for {" + a + "} and {" + b + "}";
    }
}
