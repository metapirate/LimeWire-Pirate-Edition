package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;

/**
 * Sends and receives multicast messages.
 * Currently, this only listens for messages from the Multicast group.
 * Sending is done on the GUESS port, so that other nodes can reply
 * appropriately to the individual request, instead of multicasting
 * replies to the whole group.
 *
 * @see UDPService
 * @see MessageRouter
 */
@Singleton
public final class MulticastServiceImpl implements MulticastService, Runnable {

    private static final Log LOG =
        LogFactory.getLog(MulticastServiceImpl.class);
    
    /** 
     * LOCKING: Grab the _recieveLock before receiving.  grab the _sendLock
     * before sending.  Moreover, only one thread should be wait()ing on one of
     * these locks at a time or results cannot be predicted.
     * This is the socket that handles sending and receiving messages over 
     * Multicast.
     * (Currently only used for recieving)
     */
    private volatile MulticastSocket _socket;
    
    /**
     * Used for synchronized RECEIVE access to the Multicast socket.
     * Should only be used by the Multicast thread.
     */
    private final Object _receiveLock = new Object();
    
    /**
     * The group we're joined to listen to.
     */
    private InetAddress _group = null;
    
    /**
     * The port of the group we're listening to.
     */
    private int _port = -1;

    /**
     * Constant for the size of Multicast messages to accept -- dependent upon
     * IP-layer fragmentation.
     */
    private final int BUFFER_SIZE = 1024 * 32;
    
    /**
     * Buffer used for reading messages.
     */
    private final byte[] HEADER_BUF = new byte[23];

    /**
     * The thread for listening of incoming messages.
     */
    private final Thread MULTICAST_THREAD;
    
    private final Provider<UDPService> udpService;
    private final Provider<MessageDispatcher> messageDispatcher;

    private final MessageFactory messageFactory;

    @Inject
    MulticastServiceImpl(Provider<UDPService> udpService,
            Provider<MessageDispatcher> messageDispatcher,
            MessageFactory messageFactory) {
        this.udpService = udpService;
        this.messageDispatcher = messageDispatcher;
        this.messageFactory = messageFactory;
        MULTICAST_THREAD = ThreadExecutor.newManagedThread(this, "MulticastService");
        MULTICAST_THREAD.setDaemon(true);
    }

    /**
     * Starts the Multicast service.
     */
    @Override
    public void start() {
        MULTICAST_THREAD.start();
    }

    /** 
     * Returns a new MulticastSocket that is bound to the given port.  This
     * value should be passed to setListeningSocket(MulticastSocket) to commit
     * to the new port.  If setListeningSocket is NOT called, you should close
     * the return socket.
     * @return a new MulticastSocket that is bound to the specified port.
     * @exception IOException Thrown if the MulticastSocket could not be
     * created.
     */
    @Override
    public MulticastSocket newListeningSocket(int port, InetAddress group) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Binding port " + port + ", group address " + group);
        try {
            MulticastSocket sock = new MulticastSocket(port);
            // Bind to a specific interface unless we're doing loopback tests
            if(!ConnectionSettings.ALLOW_MULTICAST_LOOPBACK.getValue())
                sock.setInterface(chooseInterface());
            sock.setTimeToLive(3);
            sock.joinGroup(group);
            if(LOG.isDebugEnabled())
                LOG.debug("Bound to " + sock.getInterface().getHostAddress());
            _port = port;
            _group = group;
            return sock;
        } catch(SocketException se) {
            LOG.debug("Could not bind port", se);
            throw new IOException("socket could not be set on port: "+port);
        } catch(SecurityException se) {
            LOG.debug("Could not bind port", se);
            throw new IOException("security exception on port: "+port);
        }
    }

    /**
     * Returns the interface to which the multicast socket should be bound.
     */
    static InetAddress chooseInterface() throws SocketException {
        // If the user has chosen a specific network interface, bind to it.
        if(ConnectionSettings.CUSTOM_NETWORK_INTERFACE.getValue()) {
            try {
                String addr = ConnectionSettings.CUSTOM_INETADRESS.get();
                if(LOG.isDebugEnabled())
                    LOG.debug("Binding to configured interface " + addr);
                return InetAddress.getByName(addr);
            } catch(UnknownHostException fallThrough) {
                LOG.debug("Failed to bind to configured interface", fallThrough);
            }
        }
        // Try to find a LAN interface to bind to.
        Enumeration<NetworkInterface> ifaces =
            NetworkInterface.getNetworkInterfaces();
        while(ifaces.hasMoreElements()) {
            NetworkInterface iface = ifaces.nextElement();
            if(iface.supportsMulticast()) {
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while(addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if(addr.isSiteLocalAddress()) {
                        LOG.debug("Binding to LAN interface " + addr.getHostAddress());
                        return addr;
                    }
                }
            }
        }
        // If there are no LAN interfaces, bind to 0.0.0.0. We try to avoid this
        // because calling joinGroup() on a socket bound to 0.0.0.0 doesn't join
        // the multicast group on all interfaces.
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4082533
        LOG.debug("No suitable interfaces found");
        try {
            return InetAddress.getByAddress(new byte[]{0, 0, 0, 0});
        } catch(UnknownHostException onlyIfAddressLengthIsIllegal) {
            throw new RuntimeException(onlyIfAddressLengthIsIllegal);
        }
    }

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
    @Override
    public void setListeningSocket(MulticastSocket multicastSocket) {
        //a) Close old socket (if non-null) to alert lock holders...
        if(_socket != null) {
            LOG.debug("Closing socket");
            _socket.close();
        }
        //b) Replace with new sock.  Notify the udpThread.
        synchronized (_receiveLock) {
            // if the input is null, then the service will shut off ;) .
            // leave the group if we're shutting off the service.
            if(multicastSocket == null && _socket != null && _group != null) {
                try {
                    if(!_socket.isClosed())
                        _socket.leaveGroup(_group);
                } catch(IOException e) {
                    LOG.debug("Could not leave multicast group", e);
                }                        
            }
            _socket = multicastSocket;
            _receiveLock.notify();
        }
    }

    /**
     * Busy loop that accepts incoming messages sent over the
     * multicast socket and dispatches them to their appropriate handlers.
     */
    @Override
    public void run() {
        try {
            byte[] datagramBytes = new byte[BUFFER_SIZE];
            while (true) {
                // prepare to receive
                DatagramPacket datagram = new DatagramPacket(datagramBytes, 
                                                             BUFFER_SIZE);
                
                // when you first can, try to recieve a packet....
                // *----------------------------
                synchronized (_receiveLock) {
                    while (_socket == null || _socket.isClosed()) {
                        try {
                            _receiveLock.wait();
                        }
                        catch (InterruptedException ignored) {
                            continue;
                        }
                    }
                    LOG.debug("Ready to receive");
                    try {
                        _socket.receive(datagram);
                    } 
                    catch(InterruptedIOException e) {
                        continue;
                    } 
                    catch(IOException e) {
                        LOG.debug("Could not receive packet", e);
                        continue;
                    } 
                }
                // ----------------------------*                
                // process packet....
                // *----------------------------
                if(!NetworkUtils.isValidAddress(datagram.getAddress())) {
                    LOG.debug("Received packet with invalid address");
                    continue;
                }
                if(!NetworkUtils.isValidPort(datagram.getPort())) {
                    LOG.debug("Received packet with invalid port");
                    continue;
                }
                
                byte[] data = datagram.getData();
                try {
                    // we do things the old way temporarily
                    InputStream in = new ByteArrayInputStream(data);
                    Message message = messageFactory.read(in, Network.MULTICAST, HEADER_BUF, datagram.getSocketAddress());
                    if(message == null) {
                        LOG.debug("Received a null message");
                        continue;
                    }
                    LOG.debug("Received a multicast message");
                    messageDispatcher.get().dispatchMulticast(message, (InetSocketAddress)datagram.getSocketAddress());
                }
                catch (IOException e) {
                    LOG.debug("Could not parse packet", e);
                    continue;
                }
                catch (BadPacketException e) {
                    LOG.debug("Could not parse packet", e);
                    continue;
                }
                // ----------------------------*
            }
        } catch(Throwable t) {
            ErrorService.error(t);
        }
    }

    /**
     * Sends the <tt>Message</tt> using UDPService to the multicast
     * address/port.
     *
     * @param msg  the <tt>Message</tt> to send
     */
    @Override
    public synchronized void send(Message msg) {
        // only send the msg if we've initialized the port.
        if(_port == -1) {
            LOG.debug("Socket not ready for writing");
        } else {
            LOG.debug("Sending a multicast message");
            udpService.get().send(msg, _group, _port);
        }
    }

    /**
     * Returns whether or not the Multicast socket is listening for incoming
     * messsages.
     *
     * @return <tt>true</tt> if the Multicast socket is listening for incoming
     *  Multicast messages, <tt>false</tt> otherwise
     */
    @Override
    public boolean isListening() {
        int port = -1;
        if(_socket != null)
            port = _socket.getLocalPort();
        if(port == -1) {
            LOG.debug("Not listening");
            return false;
        }
        return true;
    }

    /** 
     * Overrides Object.toString to give more informative information
     * about the class.
     *
     * @return the <tt>MulticastSocket</tt> data
     */
    @Override
    public String toString() {
        return "MulticastService\r\nsocket: "+_socket;
    }
}
