package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.limewire.io.Connectable;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.VendorMessage;

/**
 * Defines an interface by which two parties can communicate using the Gnutella
 * protocol over a {@link Socket}. Any capabilities or bandwidth statistics
 * about the connection are retrievable using the
 * {@link #getConnectionCapabilities()} or
 * {@link #getConnectionBandwidthStatistics()} methods.
 * <p>
 * <code>Connection</code> exposes no methods for reading or 'starting', as it
 * is possible for two implementations to use very different methods for
 * processing incoming connections. For example, a blocking implementation would
 * allow the caller to process messages on a case-by-case basis, reading one at
 * a time. An asynchronous implementation (such as implementations of
 * {@link RoutedConnection}) would have a <code>start</code> method that would funnel
 * received messages to a third party.<p>
 */
public interface Connection extends IpPort, Connectable {

    public ConnectionCapabilities getConnectionCapabilities();

    public ConnectionBandwidthStatistics getConnectionBandwidthStatistics();

    /**
     * Call this method when you want to handle us to handle a VM. We may....
     */
    public void handleVendorMessage(VendorMessage vm);

    /**
     * Call this method when the Connection has been initialized and accepted as
     * 'long-lived'.
     */
    public void sendPostInitializeMessages();

    /**
     * Call this method if you want to send your neighbours a message with your
     * updated capabilities.
     */
    public void sendUpdatedCapabilities();

    /**
     * Sets the port where the connected node listens at, not the one got from
     * socket.
     */
    void setListeningPort(int port);

    /**
     * Returns the time this connection was established, in milliseconds since
     * January 1, 1970.
     * 
     * @return the time this connection was established
     */
    public long getConnectionTime();

    /**
     * Used to determine whether the connection is incoming or outgoing.
     */
    public boolean isOutgoing();

    /**
     * Sends a message. The message may be buffered, so call flush() to
     * guarantee that the message is sent synchronously. This method is NOT
     * thread-safe. Behavior is undefined if two threads are in a send call at
     * the same time for a given connection.
     * 
     * @requires this is fully initialized
     * @modifies the network underlying this
     * @effects send m on the network. Throws IOException if problems arise.
     */
    public void send(Message m) throws IOException;

    /**
     * Returns the IP address of the remote host as a string.
     * 
     * @return the IP address of the remote host as a string
     */
    public String getAddress();

    /**
     * Accessor for the port number this connection is listening on. Note that
     * this is NOT the port of the socket itself. For incoming connections, the
     * getPort method of the java.net.Socket class returns the ephemeral port
     * that the host connected with. This port, however, is the port the remote
     * host is listening on for new connections, which we set using Gnutella
     * connection headers in the case of incoming connections. For outgoing
     * connections, this is the port we used to connect to them -- their
     * listening port.
     * 
     * @return the listening port for the remote host
     */
    public int getPort();

    /**
     * Gets the port that this connection is listening on. If this is an
     * outgoing connection, it will return the port to which the socket
     * connected. Otherwise, if it is an incoming connection, it will return the
     * port that the remote side had in the Listen-IP header. If there was no
     * port describe, it will return -1.
     */
    public int getListeningPort();

    /**
     * Returns the InetSocketAddress of the foreign host this is connected to.
     * This is a combination of the getInetAddress() & getPort() methods, it is
     * not the remote socket address (as the listening port may have been
     * updated by connection headers).
     * 
     * @throws IllegalStateException if this is not initialized
     */
    public InetSocketAddress getInetSocketAddress() throws IllegalStateException;

    /**
     * Returns the address of the foreign host this is connected to.
     * 
     * @exception IllegalStateException this is not initialized
     */
    public InetAddress getInetAddress() throws IllegalStateException;

    /**
     * Accessor for the <tt>Socket</tt> for this connection.
     * 
     * @return the <tt>Socket</tt> for this connection
     * @throws IllegalStateException if this connection is not yet initialized
     */
    public Socket getSocket() throws IllegalStateException;

    /**
     * Checks whether this connection is considered a stable connection, meaning
     * it has been up for enough time to be considered stable.
     * 
     * @return <tt>true</tt> if the connection is considered stable, otherwise
     *         <tt>false</tt>
     */
    public boolean isStable();

    /**
     * Checks whether this connection is considered a stable connection, by
     * comparing the time it was established with the <tt>millis</tt>
     * argument.
     * 
     * @return <tt>true</tt> if the connection is considered stable, otherwise
     *         <tt>false</tt>
     */
    public boolean isStable(long millis);

    /**
     * Returns the value of the given outgoing (written) connection property, or
     * null if no such property. For example, getProperty("X-Supernode") tells
     * whether I am a supernode or a leaf node. If I wrote a property multiple
     * time during connection, returns the latest.
     */
    public String getPropertyWritten(String name);

    /**
     * @return true until close() is called on this Connection
     */
    public boolean isOpen();

    /**
     * Closes the Connection's socket and thus the connection itself.
     */
    public void close();

    /**
     * Returns true if the outgoing stream is deflated.
     * 
     * @return true if the outgoing stream is deflated.
     */
    public boolean isWriteDeflated();

    /**
     * Returns true if the incoming stream is deflated.
     * 
     * @return true if the incoming stream is deflated.
     */
    public boolean isReadDeflated();

    /**
     * Returns true if no capabilites VM is received and the connection is TLS
     * encoded, or if a capabilites VM is received and it advertises support for
     * TLS. Otherwise, returns false.
     */
    public boolean isTLSCapable();

    /** Returns true if the connection is currently over TLS. */
    public boolean isTLSEncoded();

    /**
     * Returns whether or not we should allow new pings on this connection. If
     * we have recently received a ping, we will likely not allow the second
     * ping to go through to avoid flooding the network with ping traffic.
     * 
     * @return <tt>true</tt> if new pings are allowed along this connection,
     *         otherwise <tt>false</tt>
     */
    public boolean allowNewPings();

    /**
     * Returns whether or not we should allow new pongs on this connection. If
     * we have recently received a pong, we will likely not allow the second
     * pong to go through to avoid flooding the network with pong traffic. In
     * practice, this is only used to limit pongs sent to leaves.
     * 
     * @return <tt>true</tt> if new pongs are allowed along this connection,
     *         otherwise <tt>false</tt>
     */
    public boolean allowNewPongs();

    /**
     * Access the locale preference of the connected servent.
     */
    public String getLocalePref();

}