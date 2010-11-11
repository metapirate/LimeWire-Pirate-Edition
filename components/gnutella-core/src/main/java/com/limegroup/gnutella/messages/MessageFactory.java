package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;

import com.limegroup.gnutella.messages.Message.Network;

/**
 * A factory for creating Gnutella messages.
 * <p>
 * MessageFactory delegates the parsing of specific messages to a MessageParser.
 * MessageParsers can be installed using
 * {@link #setParser(byte, com.limegroup.gnutella.messages.MessageFactory.MessageParser)}.
 * When reading a message from a stream, various optional parameters can be
 * provided. In all cases, a {@link Network} is required to know which Network
 * the Message was read from. Optional parameters are <code>softMax</code>,
 * which can be used to limit the maximum number of hops a message should
 * travel, <code>headerBuf</code> which can be used as an optimization to
 * reduce byte[] allocations, and <code>addr</code> which can be used to know
 * what host this message was read from.
 * 
 */
public interface MessageFactory {

    /**
     * Registers a MessageParser under the provided functionId.
     * 
     * @param functionId the ID of the function (MessageParser)
     * @param parser the MessageParser
     */
    public void setParser(byte functionId, MessageParser parser);

    /**
     * Returns a MessageParser for the provided functionId or null
     * if no such MessageParser is registered.
     */
    public MessageParser getParser(byte functionId);

    /**
     * @modifies in
     * @effects reads a packet from the network and returns it as an instance of
     *          a subclass of Message, unless one of the following happens:
     *          <ul>
     *          <li>No data is available: returns null
     *          <li>A bad packet is read: BadPacketException. The client should
     *          be able to recover from this.
     *          <li>A major problem occurs: IOException. This includes reading
     *          packets that are ridiculously long and half-completed messages.
     *          The client is not expected to recover from this.
     *          </ul>
     * @param network the network the message was read from.
     */
    public Message read(InputStream in, Network network, byte softMax)
            throws BadPacketException, IOException;

    /**
     * @modifies in
     * @effects reads a packet from the network and returns it as an instance of
     *          a subclass of Message, unless one of the following happens:
     *          <ul>
     *          <li>No data is available: returns null
     *          <li>A bad packet is read: BadPacketException. The client should
     *          be able to recover from this.
     *          <li>A major problem occurs: IOException. This includes reading
     *          packets that are ridiculously long and half-completed messages.
     *          The client is not expected to recover from this.
     *          </ul>
     */
    public Message read(InputStream in, Network network)
            throws BadPacketException, IOException;

    /**
     * @requires buf.length==23
     * @effects exactly like Message.read(in), but buf is used as scratch for
     *          reading the header. This is an optimization that lets you avoid
     *          repeatedly allocating 23-byte arrays. buf may be used when this
     *          returns, but the contents are not guaranteed to contain any
     *          useful data.
     * @param network the network the message was read from.
     */
    public Message read(InputStream in, Network network, byte[] headerBuf, byte softMax)
            throws BadPacketException, IOException;

    /**
     * Reads a message using the specified buffer & network and the default soft
     * max.
     */
    public Message read(InputStream in, Network network, byte[] headerBuf, SocketAddress addr)
            throws BadPacketException, IOException;

    /**
     * @param network
     *            the network this was received from.
     * @requires buf.length==23
     * @effects exactly like Message.read(in), but buf is used as scratch for
     *          reading the header. This is an optimization that lets you avoid
     *          repeatedly allocating 23-byte arrays. buf may be used when this
     *          returns, but the contents are not guaranteed to contain any
     *          useful data.
     */
    public Message read(InputStream in, Network network, byte[] headerBuf,
            byte softMax, SocketAddress addr) throws BadPacketException, IOException;

    /**
     * Creates a message based on the header & payload. The header, starting at
     * headerOffset, MUST be >= 19 bytes. Additional headers bytes will be
     * ignored and the byte[] will be discarded. (Note that the header is
     * normally 23 bytes, but we don't need the last 4 here.) The payload MUST
     * be a unique byte[] of that payload. Nothing can write into or change the
     * byte[].
     */
    public Message createMessage(byte[] header, byte[] payload, Network network,
            byte softMax, SocketAddress addr) throws BadPacketException, IOException;


    /**
     * The interface for custom MessageParser(s).
     */
    public interface MessageParser {
        public Message parse(byte[] header, byte[] payload,
                Network network, byte softMax, SocketAddress addr) throws BadPacketException, IOException;
    }
   
}