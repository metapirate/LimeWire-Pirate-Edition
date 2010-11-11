package com.limegroup.gnutella.dht.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.limewire.mojito.messages.MessageFactory;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.MessageFactory.MessageParser;

/**
 * The LimeDHTMessageParser class delegates parse
 * requests to Mojito's MessageFactory
 */
class MessageParserDelegate implements MessageParser {
    
    /**
     * A handle to Mojito's MessageFactory
     */
    private final MessageFactory factory;
    
    public MessageParserDelegate(MessageFactory factory) {
        this.factory = factory;
    }
    
    public Message parse(byte[] header, byte[] payload, 
            Network network, byte softMax, SocketAddress addr) throws BadPacketException, IOException {
        
        return (Message)factory.createMessage(addr, 
                ByteBuffer.wrap(header), 
                ByteBuffer.wrap(payload));
    }
}
