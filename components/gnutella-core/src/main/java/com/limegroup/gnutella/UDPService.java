package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.api.connection.FWTStatusReason;
import org.limewire.core.api.connection.FirewallTransferStatus;
import org.limewire.core.api.connection.FirewallTransferStatusEvent;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.ByteBufferOutputStream;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.listener.AsynchronousEventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.observer.ReadWriteObserver;
import org.limewire.rudp.ConnectionState;
import org.limewire.rudp.UDPSocketChannelConnectionEvent;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.MACCalculator;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.service.ErrorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;

/**
 * This class handles UDP messaging services.  It both sends and
 * receives messages, routing received messages to their appropriate
 * handlers.  This also handles issues related to the GUESS proposal, 
 * such as making sure that the UDP and TCP port match and sending
 * UDP acks for queries.
 *
 * @see UDPReplyHandler
 * @see MessageRouter
 * @see QueryUnicaster
 *
 */
@EagerSingleton
public class UDPService implements ReadWriteObserver {

    private static final Log LOG = LogFactory.getLog(UDPService.class);
    
    
    private static final MACCalculator PING_GENERATOR = 
        MACCalculatorRepositoryManager.createDefaultCalculatorFactory().createMACCalculator();
	
	/**
	 * The DatagramChannel we're reading from & writing to.
	 */
	private DatagramChannel _channel;
	
	/**
	 * The list of messages to be sent, as SendBundles.
	 */
	private final List<SendBundle> OUTGOING_MSGS;
	
	/**
	 * The buffer that's re-used for reading incoming messages.
	 */
	private final ByteBuffer BUFFER;
    
	/**
	 * The maximum size of a UDP message we'll accept.
	 */
	private final int BUFFER_SIZE = 1024 * 2;
    
    /** True if the UDPService has ever received a solicited incoming UDP
     *  packet.
     */
    private volatile boolean _acceptedSolicitedIncoming = false;
    
    /** True if the UDPService has ever received a unsolicited incoming UDP
     *  packet.
     */
    private volatile boolean _acceptedUnsolicitedIncoming = false;
    
    /** The last time the _acceptedUnsolicitedIncoming was set.
     */
    private long _lastUnsolicitedIncomingTime = 0;

    /**
     * The last time we received any udp packet
     */
    private volatile long _lastReceivedAny = 0;
    
    /** The last time we sent a UDP Connect Back.
     */
    private long _lastConnectBackTime = System.currentTimeMillis();

    /**
     * Whether or not a firewall transfer has successfully been done
     */
    private volatile boolean _successfulFWT;

    void resetLastConnectBackTime() {
        _lastConnectBackTime = 
             System.currentTimeMillis() - acceptor.get().getIncomingExpireTime();
    }
    
    /** The last reported port as seen from the outside
     *  LOCKING: this
     */
    private int _lastReportedPort;

    /**
     * The class C network that last reported our port to us
     * in a solicited response
     */
    private int _lastReportedClassC;

    /**
     * The number of pongs carrying IP:Port info we have received.
     * LOCKING: this
     */
    private int _numReceivedIPPongs;

    /**
     * The GUID that we advertise out for UDPConnectBack requests.
     */
    private final GUID CONNECT_BACK_GUID = new GUID(GUID.makeGuid());

    /**
     * The GUID that we send for Pings, useful to test solicited support.
     */
    private final GUID SOLICITED_PING_GUID = new GUID(GUID.makeGuid());
    
    /**
     * Determines if this was ever started.
     */
    private boolean _started = false;


    /**
     * The time between UDP pings.  Used by the PeriodicPinger.  This is
     * useful for nodes behind certain firewalls (notably the MS firewall).
     */
    private static final long PING_PERIOD = 85 * 1000;  // 85 seconds
    
    /**
     * A buffer used for reading the header of incoming messages.
     */
    private static final byte[] IN_HEADER_BUF = new byte[23];
    
    private final NetworkManager networkManager;
    private final Provider<MessageDispatcher> messageDispatcher;
    private final Provider<IPFilter> ipFilter;
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<MessageRouter> messageRouter;
    private final Provider<Acceptor> acceptor;
    private final Provider<QueryUnicaster> queryUnicaster;
    private final ScheduledExecutorService backgroundExecutor;
    private final ConnectionServices connectionServices;
    private final MessageFactory messageFactory;
    private final PingRequestFactory pingRequestFactory;
    private final NetworkInstanceUtils networkInstanceUtils;
    private final AsynchronousEventBroadcaster<FirewallTransferStatusEvent> fwtStatusBroadcaster;

	@Inject
    public UDPService(NetworkManager networkManager,
            Provider<MessageDispatcher> messageDispatcher,
            @Named("hostileFilter") Provider<IPFilter> ipFilter,
            Provider<ConnectionManager> connectionManager,
            Provider<MessageRouter> messageRouter, Provider<Acceptor> acceptor,
            Provider<QueryUnicaster> queryUnicaster,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            ConnectionServices connectionServices,
            MessageFactory messageFactory,
            PingRequestFactory pingRequestFactory,
            NetworkInstanceUtils networkInstanceUtils,
            AsynchronousEventBroadcaster<FirewallTransferStatusEvent> fwtStatusBroadcaster,
            ListenerSupport<UDPSocketChannelConnectionEvent> channelEventListenerSupport) {
        this.networkManager = networkManager;
        this.messageDispatcher = messageDispatcher;
        this.ipFilter = ipFilter;
        this.connectionManager = connectionManager;
        this.messageRouter = messageRouter;
        this.acceptor = acceptor;
        this.queryUnicaster = queryUnicaster;
        this.backgroundExecutor = backgroundExecutor;
        this.connectionServices = connectionServices;
        this.messageFactory = messageFactory;
        this.pingRequestFactory = pingRequestFactory;
        this.networkInstanceUtils = networkInstanceUtils;
        this.fwtStatusBroadcaster = fwtStatusBroadcaster;

        OUTGOING_MSGS = new LinkedList<SendBundle>();
	    byte[] backing = new byte[BUFFER_SIZE];
	    BUFFER = ByteBuffer.wrap(backing);
        // TODO convert this to a Service and move this
        // TODO initialize()
        fwtStatusBroadcaster.broadcast(new FirewallTransferStatusEvent(
                FirewallTransferStatus.DOES_NOT_SUPPORT_FWT, FWTStatusReason.UNKNOWN));
        
        channelEventListenerSupport.addListener(new UDPConnectionListener());
    }
    
    /**
     * Schedules IncomingValidator & PeriodicPinger for periodic use.
     */
    protected void scheduleServices() {
        backgroundExecutor.scheduleWithFixedDelay(new IncomingValidator(), 
                               acceptor.get().getTimeBetweenValidates(),
                               acceptor.get().getTimeBetweenValidates(), TimeUnit.MILLISECONDS);
        backgroundExecutor.scheduleWithFixedDelay(new PeriodicPinger(), 0, PING_PERIOD, TimeUnit.MILLISECONDS);
    }
    
    /** @return The GUID to send for UDPConnectBack attempts....
     */
    public GUID getConnectBackGUID() {
        return CONNECT_BACK_GUID;
    }

    /** @return The GUID to send for Solicited Ping attempts....
     */
    public GUID getSolicitedGUID() {
        return SOLICITED_PING_GUID;
    }
    
    /**
     * Starts listening for UDP messages & allowing UDP messages to be written.
     */
    public void start() {
        if(!_started)
            scheduleServices();
        
        DatagramChannel channel;
        synchronized(this) {
            _started = true;
            channel = _channel;
        }
        
        if(channel != null)
            NIODispatcher.instance().registerReadWrite(channel, this);
    }

    /** 
     * Returns a new DatagramSocket that is bound to the given port.  This
     * value should be passed to setListeningSocket(DatagramSocket) to commit
     * to the new port.  If setListeningSocket is NOT called, you should close
     * the return socket.
     * @return a new DatagramSocket that is bound to the specified port.
     * @exception IOException Thrown if the DatagramSocket could not be
     * created.
     */
    public DatagramSocket newListeningSocket(int port) throws IOException {
        try {
            DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);
        	DatagramSocket s = channel.socket();
        	s.setReceiveBufferSize(64*1024);
        	s.setSendBufferSize(64*1024);
            s.bind(new InetSocketAddress(port));
            return s;
        } catch (SecurityException se) {
            throw new IOException("security exception on port: "+port);
        }
    }


	/** 
     * Changes the DatagramSocket used for sending/receiving.  Typically called
     * by Acceptor to commit to the new port.
     * @param datagramSocket the new listening socket, which must be be the
     *  return value of newListeningSocket(int).  A value of null disables 
     *  UDP sending and receiving.
	 */
	public void setListeningSocket(DatagramSocket datagramSocket) {
	    if(_channel != null) {
	        try {
	            LOG.debug("Closing socket");
	            _channel.close();
	        } catch(IOException ignored) {}
	    }

	    if(datagramSocket != null) {
	        boolean wasStarted;
	        synchronized(this) {
	            _channel = datagramSocket.getChannel();
	            if(_channel == null)
	                throw new IllegalArgumentException("No channel!");
                
	            wasStarted = _started;

	            // set the port in the FWT records
	            _lastReportedPort=_channel.socket().getLocalPort();
                _successfulFWT = false;
	        }

	        // If it was already started at one point, re-start to register this new channel.
	        if(wasStarted)
	            start();
	    }
	}
	
	int getListeningPort() {
	    synchronized(this) {
	        if(_channel != null)
	            return _channel.socket().getLocalPort();
	        else
	            return -1;
	    }
	}
	
	/**
	 * Shuts down this service.
	 */
	public void shutdown() {
	    setListeningSocket(null);
	}
	
	/**
	 * Notification that a read can happen.
	 */
	public void handleRead() throws IOException {
        try {
            while (true) {
                BUFFER.clear();

                SocketAddress from;
                try {
                    from = _channel.receive(BUFFER);
                } catch (IOException iox) {
                    break;
                } catch (Error error) {
                    // Stupid implementations giving bogus errors. Grrr!.
                    break;
                }

                // no packet.
                if (from == null)
                    break;

                if (!(from instanceof InetSocketAddress)) {
                    ErrorService.error(new RuntimeException("non-inet SocketAddress: " + from));
                    continue;
                }

                InetSocketAddress addr = (InetSocketAddress) from;

                if (!NetworkUtils.isValidAddress(addr.getAddress()))
                    continue;
                if (!NetworkUtils.isValidPort(addr.getPort()))
                    continue;

                // don't go further if filtered.
                if(!ipFilter.get().allow(addr.getAddress().getAddress())) {
                    LOG.debug("Received packet from hostile host");
                    return;
                }
                
                byte[] data = BUFFER.array();
                int length = BUFFER.position();
                try {
                    // we do things the old way temporarily
                    InputStream in = new ByteArrayInputStream(data, 0, length);
                    Message message = messageFactory.read(in, Network.UDP, IN_HEADER_BUF, addr);
                    if(message == null) {
                        LOG.debug("Received a null message");
                        continue;
                    }
                    processMessage(message, addr);
                } catch(IOException e) {
                    LOG.debug("Could not parse message", e);
                } catch(BadPacketException e) {
                    LOG.debug("Could not parse message", e);
                }
            } 
        } catch(Throwable t) {
            // Do not let the exceptions propogate out, as that could
            // close UDPService.
            ErrorService.error(t);
        }
	}
	
	/**
	 * Notification that an IOException occurred while reading/writing.
	 */
	public void handleIOException(IOException iox) {
        if( !(iox instanceof java.nio.channels.ClosedChannelException ) )
            ErrorService.error(iox, "UDP Error.");
        else
            LOG.debug("Swallowing a UDPService ClosedChannelException", iox);
	}
	
	/**
	 * Processes a single message. Package access for testing.
	 */
	void processMessage(Message message, InetSocketAddress addr) {
	    if(!ipFilter.get().allow(message)) {
	        LOG.debug("Received packet from hostile host");
	        return;
	    }
	    // FIXME: why do we mutate the GUIDs of ping replies?
	    if(message instanceof PingReply)
	        mutateGUID(message.getGUID(), addr.getAddress(), addr.getPort());
	    updateState(message, addr);
	    messageDispatcher.get().dispatchUDP(message, addr);
	}
	
	/** Updates internal state of the UDP Service. */
	private void updateState(Message message, InetSocketAddress addr) {
        _lastReceivedAny = System.currentTimeMillis();
        
        if (isValidForIncoming(addr)) {
            if(!_acceptedSolicitedIncoming)
                LOG.info("Can receive solicited UDP");
            _acceptedSolicitedIncoming = true;
        }
        
	    if (!isGUESSCapable()) {
            if (message instanceof PingRequest) {
                GUID guid = new GUID(message.getGUID());
                if(CONNECT_BACK_GUID.equals(guid) && isValidForIncoming(addr)) {
                    synchronized (this) {
                        if(!_acceptedUnsolicitedIncoming)
                            LOG.info("Can receive unsolicited UDP");
                        _acceptedUnsolicitedIncoming = true;
                        _lastReportedPort = networkManager.getPort(); // sent by the connect back request
                        ConnectionSettings.HAS_STABLE_PORT.setValue(true);
                    }
                    updateFWTState();
                }
                _lastUnsolicitedIncomingTime = _lastReceivedAny;
            }
            else if (message instanceof PingReply) {
                GUID guid = new GUID(message.getGUID());
                if(!SOLICITED_PING_GUID.equals(guid) || !isValidForIncoming(addr ))
                    return;
                
                
                PingReply r = (PingReply)message;
                if (r.getMyPort() != 0) {
                    synchronized(this){
                        _numReceivedIPPongs++;
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("Received IP pong from " +
                                    r.getAddress() + ":" + r.getPort() +
                                    " reporting " +
                                    r.getMyInetAddress().getHostAddress() +
                                    ":" + r.getMyPort());
                        }                    
                        if(_numReceivedIPPongs > 1) {
                            if(_lastReportedClassC != NetworkUtils.getClassC(r.getInetAddress())) {
                                if(_lastReportedPort == r.getMyPort()) {
                                    ConnectionSettings.HAS_STABLE_PORT.setValue(true);
                                } else {
                                    ConnectionSettings.HAS_STABLE_PORT.setValue(false);
                                }
                            }
                        }
                        _lastReportedPort = r.getMyPort();
                        _lastReportedClassC = NetworkUtils.getClassC(r.getInetAddress());
                    }
                    updateFWTState();
                }
                
            }
        }
        // ReplyNumberVMs are always sent in an unsolicited manner,
        // so we can use this fact to keep the last unsolicited up
        // to date
        if (message instanceof ReplyNumberVendorMessage)
            _lastUnsolicitedIncomingTime = _lastReceivedAny;
	}
	
    public static void mutateGUID(byte[] guid, InetAddress ip, int port) {
        byte[] qk = PING_GENERATOR.getMACBytes(new AddressSecurityToken.AddressTokenData(ip,port));
        for (int i = 0; i < qk.length; i++)
            guid[i] =(byte)(guid[i] ^ qk[i]);
    }
    
	/**
	 * Determines whether or not the specified message is valid for setting
	 * LimeWire as accepting UDP messages (solicited or unsolicited).
	 */
	private boolean isValidForIncoming(InetSocketAddress addr) {
            
	    String host = addr.getAddress().getHostAddress();
        
        //  If addr is connected to us, then return false.  Otherwise (not connected), only return true if either:
        //      1) the non-connected party is NOT private
        //  OR
        //      2) the non-connected party _is_ private, and the LOCAL_IS_PRIVATE is set to false
        return
                !connectionManager.get().isConnectedTo(host)
            &&  !networkInstanceUtils.isPrivateAddress(addr.getAddress())
             ;

    }
    
    /**
     * Sends the specified <tt>Message</tt> to the specified host.
     * 
     * @param msg the <tt>Message</tt> to send
     * @param host the host to send the message to
     */
    public void send(Message msg, IpPort host) {
        send(msg, host.getInetSocketAddress());
    }

	/**
	 * Sends the <tt>Message</tt> via UDP to the port and IP address specified.
     * This method should not be called if the client is not GUESS enabled.
     *
	 * @param msg  the <tt>Message</tt> to send
	 * @param ip   the <tt>InetAddress</tt> to send to
	 * @param port the port to send to
     * @throws IllegalArgumentException if msg, ip, or err is null.
	 */
    public void send(Message msg, InetAddress ip, int port) {
        send(msg, new InetSocketAddress(ip, port));
    }
    
    /**
     * Sends the specified <tt>Message</tt> to the specified host.
     * 
     * @param msg the <tt>Message</tt> to send
     * @param addr the network address of the host to send the message to
     */
    public void send(Message msg, InetSocketAddress addr) {
        if (msg == null)
            throw new IllegalArgumentException("Null Message");
        if (!NetworkUtils.isValidSocketAddress(addr))
            throw new IllegalArgumentException("Invalid addr: " + addr);
        if(_channel == null || _channel.socket().isClosed()) {
            LOG.debug("Socket not ready for writing");
            return;
        }
        int length = msg.getTotalLength();
        ByteBuffer buffer = NIODispatcher.instance().getBufferCache().getHeap(length);
        if(buffer.remaining() != length)
            throw new IllegalStateException("retrieved a buffer with wrong remaining! " +
                                            "wanted: " + length +
                                            ", had: " + buffer.remaining() +
                                            ", position: " + buffer.position() +
                                            ", limit: " + buffer.limit());

        ByteBufferOutputStream baos = new ByteBufferOutputStream(buffer);
        try {
            msg.writeQuickly(baos);
        } catch(IOException e) {
            // this should not happen -- we should always be able to write
            // to this output stream in memory
            ErrorService.error(e);
            // can't send the hit, so return
            return;
        }
       
        buffer.flip();
        if (msg instanceof PingRequest)
            mutateGUID(buffer.array(), addr.getAddress(), addr.getPort());
        
        send(buffer, addr, false);
    }
    
    public void send(ByteBuffer buffer, InetSocketAddress addr, boolean custom) { 
        synchronized(OUTGOING_MSGS) {
            OUTGOING_MSGS.add(new SendBundle(buffer, addr, custom));
            if(_channel != null)
                NIODispatcher.instance().interestWrite(_channel, true);
        }
	}
	
	/**
	 * Notification that a write can happen.
	 */
	public boolean handleWrite() throws IOException {
        try {
    	    synchronized(OUTGOING_MSGS) {
    	        while(!OUTGOING_MSGS.isEmpty()) {
                    boolean releaseBuffer = true;
                    SendBundle bundle = OUTGOING_MSGS.remove(0);
    	            try {
        	            if(_channel.send(bundle.buffer, bundle.addr) == 0) {
        	                // we removed the bundle from the list but couldn't send it,
        	                // so we have to put it back in.
        	                OUTGOING_MSGS.add(0, bundle);
                            releaseBuffer = false;
        	                return true; // no room left to send.
                        }
                    } catch(IOException ignored) {
                        LOG.warn("Ignoring exception on socket", ignored);
                    } finally {
                        if(bundle.custom) {
                            bundle.buffer.rewind();
                            releaseBuffer = false;
                        }
                        
                        if (releaseBuffer)
                            NIODispatcher.instance().getBufferCache().release(bundle.buffer);
                    }
    	        }
    	        
    	        // if there's no data left to send, we don't wanna be notified of write events.
    	        NIODispatcher.instance().interestWrite(_channel, false);
    	        return false;
    	    }
        } catch(Throwable t) {
            // Don't let it propogate, since that could close UDPService!
            ErrorService.error(t);
            return true;
        }
    }       
	
	/** Wrapper for outgoing data */
	private static class SendBundle {
	    private final ByteBuffer buffer;
	    private final SocketAddress addr;
        private final boolean custom;
	    
	    SendBundle(ByteBuffer b, InetSocketAddress addr, boolean custom) {
	        buffer = b;
	        this.addr = addr;
            this.custom = custom;
	    }
	}


	/**
	 * Returns whether or not this node is capable of sending its own
	 * GUESS queries.  This would not be the case only if this node
	 * has not successfully received an incoming UDP packet.
	 *
	 * @return <tt>true</tt> if this node is capable of running its own
	 *  GUESS queries, <tt>false</tt> otherwise
	 */	
	public boolean isGUESSCapable() {
		return canReceiveUnsolicited() && canReceiveSolicited();
	}

	/**
	 * Returns whether or not this node is capable of receiving UNSOLICITED
     * UDP packets.  It is false until a UDP ConnectBack ping has been received.
	 *
	 * @return <tt>true</tt> if this node has accepted a UNSOLICITED UDP packet.
	 */	
	public boolean canReceiveUnsolicited() {
		return _acceptedUnsolicitedIncoming;
	}

	/**
	 * Returns whether or not this node is capable of receiving SOLICITED
     * UDP packets.  
	 *
	 * @return <tt>true</tt> if this node has accepted a SOLICITED UDP packet.
	 */	
	public boolean canReceiveSolicited() {
        return _acceptedSolicitedIncoming;
	}
	
	/**
	 * 
	 * @return whether this node can do Firewall-to-firewall transfers.
	 *  Until we get back any udp packet, the answer is no.
	 *  If we have received an udp packet but are not connected, or haven't 
	 * received a pong carrying ip info yet, see if we ever disabled fwt in the 
	 * past.
	 *  If we are connected and have gotten a single ip pong, our port must be 
	 * the same as our tcp port or our forced tcp port.
	 *  If we have received more than one ip pong, they must all report the same
	 * port.
	 */
	public boolean canDoFWT(){
	    boolean retValue = false;
	    
	    // this does not affect EVER_DISABLED_FWT.
	    if (!canReceiveSolicited()) { 
            fwtStatusBroadcaster.broadcast(new FirewallTransferStatusEvent(FirewallTransferStatus.DOES_NOT_SUPPORT_FWT,
                    FWTStatusReason.NO_SOLICITED_INCOMING_MESSAGES));
            retValue = false;
        } else {
    	    boolean canDoFWTSetting = !ConnectionSettings.CANNOT_DO_FWT.getValue();
    	    if (!connectionServices.isConnected()) {
                if(canDoFWTSetting) {
                    fwtStatusBroadcaster.broadcast(new FirewallTransferStatusEvent(FirewallTransferStatus.SUPPORTS_FWT,
                            FWTStatusReason.REUSING_STATUS_FROM_PREVIOUS_SESSION));
                } else {
                    fwtStatusBroadcaster.broadcast(new FirewallTransferStatusEvent(FirewallTransferStatus.DOES_NOT_SUPPORT_FWT,
                            FWTStatusReason.REUSING_STATUS_FROM_PREVIOUS_SESSION));
                }
                retValue = canDoFWTSetting;
            } else {
        	    boolean needToUpdateState;
        	    synchronized(this) {
        	        if (_numReceivedIPPongs < 1) {
                        if(canDoFWTSetting) {
                            fwtStatusBroadcaster.broadcast(new FirewallTransferStatusEvent(FirewallTransferStatus.SUPPORTS_FWT,
                                    FWTStatusReason.REUSING_STATUS_FROM_PREVIOUS_SESSION));
                        } else {
                            fwtStatusBroadcaster.broadcast(new FirewallTransferStatusEvent(FirewallTransferStatus.DOES_NOT_SUPPORT_FWT,
                                    FWTStatusReason.REUSING_STATUS_FROM_PREVIOUS_SESSION));
                        }
                        needToUpdateState = false;
                        retValue = canDoFWTSetting;
                    } else {
                        needToUpdateState = true;
                    }
        	    }
        	    
        	    if(needToUpdateState) {
        	        updateFWTState();
        	        retValue = !ConnectionSettings.CANNOT_DO_FWT.getValue();
        	    }
            }
	    }
	    
	    return retValue;
	}
	
	private void updateFWTState() {
	    boolean newFWTSetting = true;
        FWTStatusReason reason = FWTStatusReason.UNKNOWN;
	    synchronized(this) {     	
	        if (LOG.isTraceEnabled()) {
	            LOG.trace("stable port "+ConnectionSettings.HAS_STABLE_PORT.getValue() +
	                    " last reported port "+_lastReportedPort+
	                    " our external port "+networkManager.getPort()+
	                    " our non-forced port "+acceptor.get().getPort(false)+
	                    " number of received IP pongs "+_numReceivedIPPongs+
	                    " valid external addr "+NetworkUtils.isValidAddress(
	                            networkManager.getExternalAddress()));
	        }
            if(_successfulFWT || _acceptedUnsolicitedIncoming) {
                newFWTSetting = true;
            } else {	        
                if(!NetworkUtils.isValidAddress(networkManager.getExternalAddress())) {
                    reason = FWTStatusReason.INVALID_EXTERNAL_ADDRESS;
                    newFWTSetting = false;
                }
                boolean stablePort = ConnectionSettings.HAS_STABLE_PORT.getValue();
                newFWTSetting = newFWTSetting && stablePort;
                if(!stablePort) {
                    if(_numReceivedIPPongs < 2) {
                        reason = FWTStatusReason.REUSING_STATUS_FROM_PREVIOUS_SESSION;
                    } else {
                        reason = FWTStatusReason.PORT_UNSTABLE;
                    }
                }
                
                if (_numReceivedIPPongs == 1){
                    newFWTSetting = newFWTSetting &&
                    (_lastReportedPort == acceptor.get().getPort(false) ||
                            _lastReportedPort == networkManager.getPort());
                }
            }
	    }
        if(newFWTSetting) {
            fwtStatusBroadcaster.broadcast(new FirewallTransferStatusEvent(FirewallTransferStatus.SUPPORTS_FWT,
                    FWTStatusReason.UNKNOWN));
        } else {
            fwtStatusBroadcaster.broadcast(new FirewallTransferStatusEvent(FirewallTransferStatus.DOES_NOT_SUPPORT_FWT,
                    reason));
        }
	    ConnectionSettings.CANNOT_DO_FWT.setValue(!newFWTSetting);
	}
	
	// Some getters for bug reporting 
	public boolean portStable() {
	    return ConnectionSettings.HAS_STABLE_PORT.getValue();
	}
	
	public int receivedIpPong() {
	    return _numReceivedIPPongs;
	}
	
	public int lastReportedPort() {
	    return _lastReportedPort;
	}
	
	/**
	 * @return the stable UDP port as seen from the outside.
	 *   If we have received more than one IPPongs and they report
	 * the same port, we return that.
	 *   If we have received just one IPpong, and if its address 
	 * matches either our local port or external port, return that.
	 *   If we have not received any IPpongs, return whatever 
	 * RouterService thinks our port is.
	 */
	public int getStableUDPPort() {

	    int localPort = acceptor.get().getPort(false);
	    int forcedPort = networkManager.getPort();

	    synchronized(this) {
	        if (ConnectionSettings.HAS_STABLE_PORT.getValue() && _numReceivedIPPongs > 1)
	            return _lastReportedPort;

		if (_numReceivedIPPongs == 1 &&
			(localPort == _lastReportedPort || 
				forcedPort == _lastReportedPort))
		    return _lastReportedPort;
	    }

	    return forcedPort; // we haven't received an ippong.
	}

	/**
	 * Sets whether or not this node is capable of receiving SOLICITED
     * UDP packets.  This is useful for testing UDPConnections.
	 *
	 */	
	public void setReceiveSolicited(boolean value) {
		_acceptedSolicitedIncoming = value;
	}
    
    public long getLastReceivedTime() {
        return _lastReceivedAny;
    }

	/**
	 * Returns whether or not the UDP socket is listening for incoming
	 * messsages.
	 *
	 * @return <tt>true</tt> if the UDP socket is listening for incoming
	 *  UDP messages, <tt>false</tt> otherwise
	 */
	public boolean isListening() {
        DatagramChannel channel = _channel;
		if(channel == null)
		    return false;
		    
		return (channel.socket().getLocalPort() != -1);
	}

	/** 
	 * Overrides Object.toString to give more informative information
	 * about the class.
	 *
	 * @return the <tt>DatagramSocket</tt> data
	 */
	@Override
    public String toString() {
		return "UDPService::channel: " + _channel;
	}

    private static class MLImpl implements MessageListener {
        public boolean _gotIncoming = false;

        public void processMessage(Message m, ReplyHandler handler) {
            if ((m instanceof PingRequest))
                _gotIncoming = true;
        }
        
        public void registered(byte[] guid) {}
        public void unregistered(byte[] guid) {}
    }

    private class IncomingValidator implements Runnable {
        public IncomingValidator() {}
        public void run() {
            // clear and revalidate if 1) we haven't had in incoming in an hour
            // or 2) we've never had incoming and we haven't checked in an hour
            final long currTime = System.currentTimeMillis();
            if (
                (_acceptedUnsolicitedIncoming && //1)
                 ((currTime - _lastUnsolicitedIncomingTime) > 
                  acceptor.get().getIncomingExpireTime())) 
                || 
                (!_acceptedUnsolicitedIncoming && //2)
                 ((currTime - _lastConnectBackTime) > 
                 acceptor.get().getIncomingExpireTime()))
                ) {
                
                final GUID cbGuid = new GUID(GUID.makeGuid());
                final MLImpl ml = new MLImpl();
                messageRouter.get().registerMessageListener(cbGuid.bytes(), ml);
                // send a connectback request to a few peers and clear
                if(connectionManager.get().sendUDPConnectBackRequests(cbGuid))  {
                    _lastConnectBackTime = System.currentTimeMillis();
                    Runnable checkThread = new Runnable() {
                            public void run() {
                                if ((_acceptedUnsolicitedIncoming && 
                                     (_lastUnsolicitedIncomingTime < currTime))
                                    || (!_acceptedUnsolicitedIncoming)) {
                                    if(ml._gotIncoming && !_acceptedUnsolicitedIncoming)
                                        LOG.info("Can receive unsolicited UDP");
                                    // we set according to the message listener
                                    _acceptedUnsolicitedIncoming = 
                                        ml._gotIncoming;
                                    // TODO set _lastReportedPort ??
                                    // TODO set ConnectionSettings.STABLE_PORT ??
                                    // TODO updateFWTState() ??
                                }
                                messageRouter.get().unregisterMessageListener(cbGuid.bytes(), ml);
                            }
                        };
                    backgroundExecutor.schedule(checkThread, 
                            acceptor.get().getWaitTimeAfterRequests(),
                            TimeUnit.MILLISECONDS);
                }
                else
                    messageRouter.get().unregisterMessageListener(cbGuid.bytes(), ml);
            }
        }
    }

    private class PeriodicPinger implements Runnable {
        public void run() {
            // straightforward - send a UDP ping to a host.  it doesn't really
            // matter who the guy is - we are just sending to open up any
            // potential firewall to UDP traffic
            GUESSEndpoint ep = queryUnicaster.get().getUnicastEndpoint();
            if (ep == null) return;
            // only do this if you can receive some form of UDP traffic.
            if (!canReceiveSolicited() && !canReceiveUnsolicited()) return;

            // good to use the solicited guid
            byte[] guid = getSolicitedGUID().bytes();
            PingRequest pr =
                pingRequestFactory.createPingRequest(guid, (byte)1,(byte)0);
            
            pr.addIPRequest();
            send(pr, ep.getInetAddress(), ep.getPort());
        }
    }

    private class UDPConnectionListener implements EventListener<UDPSocketChannelConnectionEvent> {
        @Override
        public void handleEvent(UDPSocketChannelConnectionEvent event) {
            if(event.getType() == ConnectionState.CONNECTED && !_successfulFWT) {
                synchronized (this) {
                    _successfulFWT = true;
                     // TODO not sure this is correct - could also call networkManager.getPort() ?
                    _lastReportedPort = event.getData().socket().getLocalPort();
                    ConnectionSettings.HAS_STABLE_PORT.setValue(true);
                }
                updateFWTState();
            }
        }
    }
}
