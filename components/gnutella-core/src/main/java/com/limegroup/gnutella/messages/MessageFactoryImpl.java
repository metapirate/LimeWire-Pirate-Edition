package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.MessageSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.util.ByteUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.util.DataUtils;

/**
 * Factory to turn binary input as read from Network to Message
 * Objects.
 */
@EagerSingleton
public class MessageFactoryImpl implements MessageFactory {
    
    private static final Log LOG = LogFactory.getLog(MessageFactoryImpl.class);
    
    /** Array of MessageParser(s) */
    private final MessageParser[] PARSERS = new MessageParser[0xFF + 1];
    
    /**
     * Cached soft max ttl -- if the TTL+hops is greater than SOFT_MAX,
     * the TTL is set to SOFT_MAX-hops.
     */
    private final byte defaultSoftMax = ConnectionSettings.SOFT_MAX.getValue();
    
    public MessageFactoryImpl() {
    }
    
    @Inject
    public MessageFactoryImpl(MessageParserBinder messageParserBinder) {
        messageParserBinder.bind(this);
    }
    
    public void setParser(byte functionId, MessageParser parser) {
        if (parser == null) {
            throw new NullPointerException("MessageParser is null");
        }
        
        int index = functionId & 0xFF;
        
        Object o = null;
        synchronized (PARSERS) {
            o = PARSERS[index];
            PARSERS[index] = parser;
        }
        
        if (o != null && LOG.isErrorEnabled()) {
            LOG.error("There was already a MessageParser of type " 
                    + o.getClass() + " registered for functionId " + functionId);
        }
    }
    
    public MessageParser getParser(byte functionId) {
        return PARSERS[functionId & 0xFF];
    }

    public Message read(InputStream in, Network network)
            throws BadPacketException, IOException {
        return read(in, network, new byte[23], defaultSoftMax, null);
    }

    public Message read(InputStream in, Network network, byte softMax)
            throws BadPacketException, IOException {
        return read(in, network, new byte[23], softMax, null);
    }

    public Message read(InputStream in, Network network, byte[] buf, byte softMax)
            throws BadPacketException, IOException {
        return read(in, network, buf, softMax, null);
    }

    public Message read(InputStream in, Network network, byte[] buf, SocketAddress addr)
            throws BadPacketException, IOException {
        return read(in, network, buf, defaultSoftMax, addr);
    }

    public Message read(InputStream in, Network network, byte[] buf,
            byte softMax, SocketAddress addr) throws BadPacketException, IOException {

        // 1. Read header bytes from network. If we timeout before any
        // data has been read, return null instead of throwing an
        // exception.
        for (int i = 0; i < 23;) {
            int got;
            try {
                got = in.read(buf, i, 23 - i);
            } catch (InterruptedIOException e) {
                // have we read any of the message yet?
                if (i == 0)
                    return null;
                else
                    throw e;
            }
            if (got == -1) {
                throw new IOException("Connection closed.");
            }
            i += got;
        }

        // 2. Unpack.
        int length = ByteUtils.leb2int(buf, 19);
        // 2.5 If the length is hopelessly off (this includes lengths >
        // than 2^31 bytes, throw an irrecoverable exception to
        // cause this connection to be closed.
        if (length < 0 || length > MessageSettings.MAX_LENGTH.getValue()) {
            throw new IOException("Unreasonable message length: " + length);
        }

        // 3. Read rest of payload. This must be done even for bad
        // packets, so we can resume reading packets.
        byte[] payload = null;
        if (length != 0) {
            payload = new byte[length];
            for (int i = 0; i < length;) {
                int got = in.read(payload, i, length - i);
                if (got == -1) {
                    throw new IOException("Read EOF before EOM.");
                }
                i += got;
            }
        } else {
            payload = DataUtils.EMPTY_BYTE_ARRAY;
        }

        return createMessage(buf, payload, network, softMax, addr);
    }

    public Message createMessage(byte[] header, byte[] payload,
            Network network, byte softMax, SocketAddress addr) throws BadPacketException, IOException {
        if (header.length < 19) {
            throw new IllegalArgumentException("header must be >= 19 bytes.");
        }
        
        byte func = header[16];
        
        // Get Parser based on opcode.
        MessageParser parser = getParser(func);
        if (parser == null) {
            throw new BadPacketException("Unrecognized function code: " + func);
        }
        
        return parser.parse(header, payload, network, softMax, addr);
    }
    
}
