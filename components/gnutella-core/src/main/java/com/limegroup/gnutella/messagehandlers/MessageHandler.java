package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.Message;

/**
 * The interface for custom MessageHandler(s).
 */
public interface MessageHandler {
    public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler);
}
