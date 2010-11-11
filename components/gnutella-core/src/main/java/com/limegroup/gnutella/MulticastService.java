package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

import com.limegroup.gnutella.messages.Message;

/**
 * Defines an interface for sending and receiving multicast messages.
 * Currently, this only listens for messages from the Multicast group.
 * Sending is done on the GUESS port, so that other nodes can reply
 * appropriately to the individual request, instead of multicasting
 * replies to the whole group.
 *
 * @see UDPService
 * @see MessageRouter
 */
public interface MulticastService {
    /**
     * Starts the Multicast service.
     */
    public void start();

    /** 
     * Returns a new MulticastSocket that is bound to the given port.  This
     * value should be passed to setListeningSocket(MulticastSocket) to commit
     * to the new port.  If setListeningSocket is NOT called, you should close
     * the return socket.
     * @return a new MulticastSocket that is bound to the specified port.
     * @exception IOException Thrown if the MulticastSocket could not be
     * created.
     */
    public MulticastSocket newListeningSocket(int port, InetAddress group) throws IOException;

    /** 
     * Changes the MulticastSocket used for sending/receiving.
     * This must be common among all instances of LimeWire on the subnet.
     * It is not synched with the typical gnutella port, because that can
     * change on a per-servent basis.
     * Only MulticastService should mutate this.
     * @param multicastSocket the new listening socket, which must be be the
     *  return value of newListeningSocket(int).  A value of null disables 
     *  Multicast sending and receiving.
     */
    public void setListeningSocket(MulticastSocket multicastSocket);

    /**
     * Sends the <tt>Message</tt> using UDPService to the multicast
     * address/port.
     *
     * @param msg  the <tt>Message</tt> to send
     */
    public void send(Message msg);

    /**
     * Returns whether or not the Multicast socket is listening for incoming
     * messsages.
     *
     * @return <tt>true</tt> if the Multicast socket is listening for incoming
     *  Multicast messages, <tt>false</tt> otherwise
     */
    public boolean isListening();
}
