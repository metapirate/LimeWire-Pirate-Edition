package com.limegroup.gnutella;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.api.connection.FirewallStatus;
import org.limewire.core.api.connection.FirewallStatusEvent;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Join;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.AsynchronousEventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.AsyncConnectionDispatcher;
import org.limewire.net.BlockingConnectionDispatcher;
import org.limewire.net.ConnectionAcceptor;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.nio.SocketFactory;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.observer.AcceptObserver;
import org.limewire.service.MessageService;
import org.limewire.setting.SettingsGroupManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.filters.IPFilter;

/**
 * Listens on ports, accepts incoming connections, and dispatches threads to
 * handle those connections.  Currently supports Gnutella messaging, HTTP, and
 * chat connections over TCP; more may be supported in the future.<p> 
 * This class has a special relationship with UDPService and should really be
 * the only class that intializes it.  See setListeningPort() for more
 * info.
 */
@EagerSingleton
public class AcceptorImpl implements ConnectionAcceptor, SocketProcessor, Acceptor, Service {

    private static final Log LOG = LogFactory.getLog(AcceptorImpl.class);

    public static final long DEFAULT_INCOMING_EXPIRE_TIME = 30 * 60 * 1000; // 30 minutes
    
    public static final long DEFAULT_WAIT_TIME_AFTER_REQUESTS = 30 * 1000;    // 30 seconds
    public static final long DEFAULT_TIME_BETWEEN_VALIDATES = 10 * 60 * 1000; // 10 minutes
    
    // various time delays for checking of firewalled status.
    private long incomingExpireTime = DEFAULT_INCOMING_EXPIRE_TIME;
    private long waitTimeAfterRequests = DEFAULT_WAIT_TIME_AFTER_REQUESTS;
    private long timeBetweenValidates = DEFAULT_TIME_BETWEEN_VALIDATES;
    
    /** Task for validating incoming requests */
    private final IncomingValidator incomingValidator = new IncomingValidator();
    
    /**
     * The socket that listens for incoming connections. Can be changed to
     * listen to new ports.
     *
     * LOCKING: obtain _socketLock before modifying either.  Notify _socketLock
     * when done.
     */
    private volatile ServerSocket _socket=null;

    /**
     * The port of the server socket.
     */
    private volatile int _port = 6346;
    
    /**
     * The real address of this host--assuming there's only one--used for pongs
     * and query replies.  This value is ignored if FORCE_IP_ADDRESS is
     * true. This is initialized in three stages:
     *   1. Statically initialized to all zeroes.
     *   2. Initialized in the Acceptor thread to getLocalHost().
     *   3. Initialized each time a connection is initialized to the local
     *      address of that connection's socket. 
     *
     * Why are all three needed?  Step (3) is needed because (2) can often fail
     * due to a JDK bug #4073539, or if your address changes via DHCP.  Step (2)
     * is needed because (3) ignores local addresses of 127.x.x.x.  Step (1) is
     * needed because (2) can't occur in the main thread, as it may block
     * because the update checker is trying to resolve addresses.  (See JDK bug
     * #4147517.)  Note this may delay the time to create a listening socket by
     * a few seconds; big deal!
     *
     * LOCKING: obtain Acceptor.class' lock 
     */
    private byte[] _address = new byte[4];
    
    /**
     * The external address.  This is the address as visible from other peers.
     *
     * LOCKING: obtain Acceptor.class' lock
     */
    private byte[] _externalAddress = new byte[4];
    
	/**
	 * Variable for whether or not we have accepted an incoming connection --
	 * used to determine firewall status.
	 */
	private volatile boolean _acceptedIncoming = false;
	
    /**
     * Keep track of the last time we re-validated.
     */
    private volatile long _lastConnectBackTime = 0;

    /**
     * Whether or not this Acceptor was started.  All connections accepted prior
     * to starting are dropped.
     */
    private volatile boolean _started;
    
    private final Object ADDRESS_LOCK = new Object();
    
    private final NetworkManager networkManager;
    private final Provider<UDPService> udpService;
    private final Provider<MulticastService> multicastService;
    private final Provider<ConnectionDispatcher> connectionDispatcher;
    private final ScheduledExecutorService backgroundExecutor;
    private final AsynchronousEventBroadcaster<FirewallStatusEvent> firewallBroadcaster;
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<IPFilter> ipFilter;
    private final ConnectionServices connectionServices;
    private final Provider<UPnPManager> upnpManager;
    
    private final boolean upnpEnabled; 
    
    @Inject
    public AcceptorImpl(NetworkManager networkManager,
            Provider<UDPService> udpService,
            Provider<MulticastService> multicastService,
            @Named("global") Provider<ConnectionDispatcher> connectionDispatcher,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            AsynchronousEventBroadcaster<FirewallStatusEvent> firewallBroadcaster,
            Provider<ConnectionManager> connectionManager,
            Provider<IPFilter> ipFilter, 
            ConnectionServices connectionServices,
            Provider<UPnPManager> upnpManager) {
        this.networkManager = networkManager;
        this.udpService = udpService;
        this.multicastService = multicastService;
        this.connectionDispatcher = connectionDispatcher;
        this.backgroundExecutor = backgroundExecutor;
        this.firewallBroadcaster = firewallBroadcaster;
        this.connectionManager = connectionManager;
        this.ipFilter = ipFilter;
        this.connectionServices = connectionServices;
        this.upnpManager = upnpManager;
        
        // capture UPnP setting on construction, so start/stop can
        // work even if setting changes between the two.
        upnpEnabled = !ConnectionSettings.DISABLE_UPNP.getValue();
    }
    
    /** Returns true if UPnP was enabled when Acceptor was constructed. */
    private boolean isUPnPEnabled() {
        return upnpEnabled;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#setAddress(java.net.InetAddress)
     */
	public void setAddress(InetAddress address) {
		byte[] byteAddr = address.getAddress();
		if( !NetworkUtils.isValidAddress(byteAddr) )
		    return;
		    
		if( byteAddr[0] == 127 &&
           ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) {
            return;
        }

        boolean addrChanged = false;
		synchronized(ADDRESS_LOCK) {
		    if( !Arrays.equals(_address, byteAddr) ) {
			    _address = byteAddr;
			    addrChanged = true;
			}
		}
		
		if(addrChanged) {
            LOG.infof("Setting address to {0}", address);
		    networkManager.addressChanged();
        }
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#setExternalAddress(java.net.InetAddress)
     */
	public void setExternalAddress(InetAddress address) {
	    byte[] byteAddr = address.getAddress();

		if( byteAddr[0] == 127 &&
           ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) {
            return;
        }

        boolean addrChanged = false;
        synchronized(ADDRESS_LOCK) {
            if( !Arrays.equals(_externalAddress, byteAddr) ) {
                LOG.debugf("setting external address {0}", address);
			    _externalAddress = byteAddr;
			    addrChanged = true;
			}
		}
        if(addrChanged) 
            networkManager.externalAddressChanged();
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#init()
     */
	public void bindAndStartUpnp() {
        int tempPort;
        // try a random port if we have not received an incoming connection  
        // and have been running on the default port (6346) 
        // and the user has not changed the settings
        boolean tryingRandom = NetworkSettings.PORT.isDefault() && 
                !ConnectionSettings.EVER_ACCEPTED_INCOMING.getValue() &&
                !ConnectionSettings.FORCE_IP_ADDRESS.getValue();
        
        Random gen = null;
        if (tryingRandom) {
            gen = new Random();
            tempPort = gen.nextInt(50000)+2000;
        }
        else
            tempPort = NetworkSettings.PORT.getValue();

        //0. Get local address.  This must be done here because it can
        //   block under certain conditions.
        //   See the notes for _address.
        try {
            if(isUPnPEnabled()) {
                if (LOG.isDebugEnabled())
                    LOG.debug("setting address to local address: " + NetworkUtils.getLocalAddress());
                setAddress(NetworkUtils.getLocalAddress());
            } else {
                if (LOG.isDebugEnabled())
                    LOG.debug("setting address to localhost: " + InetAddress.getLocalHost());
                setAddress(InetAddress.getLocalHost());
            }
        } catch (UnknownHostException e) {
        } catch (SecurityException e) {
        }

        // Create the server socket, bind it to a port, and listen for
        // incoming connections.  If there are problems, we can continue
        // onward.
        //1. Try suggested port.
		int oldPort = tempPort;
        try {
			setListeningPort(tempPort);
			_port = tempPort;
        } catch (IOException e) {
            LOG.warn("can't set initial port", e);
        
            // 2. Try 20 different ports. 
            int numToTry = 20;
            for (int i=0; i<numToTry; i++) {
                if(gen == null)
                    gen = new Random();
                tempPort = gen.nextInt(50000);
                tempPort += 2000;//avoid the first 2000 ports
                
				// do not try to bind to the multicast port.
				if (tempPort == ConnectionSettings.MULTICAST_PORT.getValue()) {
				    numToTry++;
				    continue;
				}
                try {
                    setListeningPort(tempPort);
					_port = tempPort;
                    break;
                } catch (IOException e2) { 
                    LOG.warn("can't set port", e2);
                }
            }

            // If we still don't have a socket, there's an error
            if(_socket == null) {
                MessageService.showError(I18nMarker.marktr("LimeWire was unable to set up a port to listen for incoming connections. Some features of LimeWire may not work as expected."));
            }
        }
        if(LOG.isInfoEnabled())
            LOG.info("Listening on port " + _port);
        
        if (_port != oldPort || tryingRandom) {
            NetworkSettings.PORT.setValue(_port);
            SettingsGroupManager.instance().save();
            networkManager.portChanged();
        }
       
        // Make sure UPnP gets setup.
        if(upnpManager.get().isNATPresent()) {
            setupUPnP();
        } else {
            upnpManager.get().addListener(new UPnPListener() {
                public void natFound() {
                    setupUPnP();
                }
            });
        }
	}
	
	private void setupUPnP() {
        // if we created a socket and have a NAT, and the user is not 
        // explicitly forcing a port, create the mappings 
        if (_socket != null && isUPnPEnabled()) {
        	boolean natted = upnpManager.get().isNATPresent();
        	boolean validPort = NetworkUtils.isValidPort(_port);
        	boolean forcedIP = ConnectionSettings.FORCE_IP_ADDRESS.getValue() &&
				!ConnectionSettings.UPNP_IN_USE.getValue();
        	
        	if(LOG.isDebugEnabled())
        	    LOG.debug("Natted: " + natted + ", validPort: " + validPort + ", forcedIP: " + forcedIP);
        	
        	if(natted && validPort && !forcedIP) {
        		int mappedPort = upnpManager.get().mapPort(_port, getAddress(false));
        		if(LOG.isDebugEnabled())
        		    LOG.debug("UPNP port mapped: " + mappedPort);
        		
			    //if we created a mapping successfully, update the forced port
			    if (mappedPort != 0 ) {			        
			        //  mark UPNP as being on so that if LimeWire shuts
			        //  down prematurely, we know the FORCE_IP was from UPnP
			        //  and that we can continue trying to use UPnP
        		    ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
        	        ConnectionSettings.FORCED_PORT.setValue(mappedPort);
        	        ConnectionSettings.UPNP_IN_USE.setValue(true);
        	        if (mappedPort != _port)
        	            networkManager.portChanged();
        		
        		    // we could get our external address from the NAT but its too slow
        		    // so we clear the last connect back times and re-validate cause our
        	        // status may have changed.
        		    resetLastConnectBackTime();
        		    udpService.get().resetLastConnectBackTime();
        		    if (!acceptedIncoming())
        		        incomingValidator.run();
			    }			        
        	}
        }
	}
	
	@Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
        registry.register(new Service() {
            public String getServiceName() {
                return "UPnP";
            }
            
            public void initialize() {
            }
            
            public void start() {
                bindAndStartUpnp();
            }
            
            @Asynchronous (join = Join.TIMEOUT, timeout = 30, daemon = true)
            public void stop() {
                upnpManager.get().clearMappings();
            }
        }).in("EarlyBackground");
    }
	
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#start()
     */
	public void start() {
        multicastService.get().start();
        udpService.get().start();
        connectionDispatcher.get().addConnectionAcceptor(this, false, "CONNECT", "\n\n");
        backgroundExecutor.scheduleWithFixedDelay(incomingValidator,
                timeBetweenValidates, timeBetweenValidates,
                TimeUnit.MILLISECONDS);
        _started = true;
    }
	
	public String getServiceName() {
	    return org.limewire.i18n.I18nMarker.marktr("Connection Listener");
	}
	
	public void initialize() {
        firewallBroadcaster.broadcast(new FirewallStatusEvent(FirewallStatus.FIREWALLED));
	}
	
	public void stop() {
	    try {
	        setListeningPort(0);
	    } catch(IOException ignored) {}
	    shutdown();
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#isAddressExternal()
     */
	public boolean isAddressExternal() {
        if (!ConnectionSettings.LOCAL_IS_PRIVATE.getValue())
            return true;
	    synchronized(ADDRESS_LOCK) {
	        return Arrays.equals(getAddress(true), _externalAddress);
	    }
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#isBlocking()
     */
	public boolean isBlocking() {
	    return false;
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#getExternalAddress()
     */
	public byte[] getExternalAddress() {
	    synchronized(ADDRESS_LOCK) {
	        return _externalAddress;
        }
	}

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#getAddress(boolean)
     */
    public byte[] getAddress(boolean preferForcedAddress) {        
		if(preferForcedAddress) {
		    if (ConnectionSettings.FORCE_IP_ADDRESS.getValue()) {
		        String address = 
		            ConnectionSettings.FORCED_IP_ADDRESS_STRING.get();
		        try {
		            InetAddress ia = InetAddress.getByName(address);
		            byte[] addr = ia.getAddress();
		            return addr;
		        } catch (UnknownHostException err) {
		            // ignore and return _address
		        }
		    } else if (_acceptedIncoming) {
		        // return valid external address as forced address if we
		        // can accept incoming connections, to advertise the right
		        // address to peers as a non-firewalled peer
		        // this can happen when the firewall does port forwarding,
		        // but the client is not explicitly configured to do it
		        synchronized (ADDRESS_LOCK) {
		            if (NetworkUtils.isValidAddress(_externalAddress)) {
		                return _externalAddress;
		            }
                }
		    }
		}
		    
		synchronized (ADDRESS_LOCK) {
		    return _address;
		}
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#getConnectionDispatcher()
     */
    public ConnectionDispatcher getConnectionDispatcher() {
        return connectionDispatcher.get();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#getPort(boolean)
     */
    public int getPort(boolean preferForcedPort) {
        if(preferForcedPort && ConnectionSettings.FORCE_IP_ADDRESS.getValue())
			return ConnectionSettings.FORCED_PORT.getValue();
        return _port;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#setListeningPort(int)
     */
    public void setListeningPort(int port) throws IOException {
        //1. Special case: if unchanged, do nothing.
        if (_socket!=null && _port==port)
            return;
        //2. Special case if port==0.  This ALWAYS works.
        //Note that we must close the socket BEFORE grabbing
        //the lock.  Otherwise deadlock will occur since
        //the acceptor thread is listening to the socket
        //while holding the lock.  Also note that port
        //will not have changed before we grab the lock.
        else if (port==0) {
            LOG.trace("shutting off service.");
            IOUtils.close(_socket);            
            _socket=null;
            _port=0;

            //Shut off UDPService also!
            udpService.get().setListeningSocket(null);
            //Shut off MulticastServier too!
            multicastService.get().setListeningSocket(null);            

            LOG.trace("service OFF.");
            return;
        }
        //3. Normal case.  See note about locking above.
        /* Since we want the UDPService to bind to the same port as the 
         * Acceptor, we need to be careful about this case.  Essentially, we 
         * need to confirm that the port can be bound by BOTH UDP and TCP 
         * before actually acceping the port as valid.  To effect this change,
         * we first attempt to bind the port for UDP traffic.  If that fails, a
         * IOException will be thrown.  If we successfully UDP bind the port 
         * we keep that bound DatagramSocket around and try to bind the port to 
         * TCP.  If that fails, a IOException is thrown and the valid 
         * DatagramSocket is closed.  If that succeeds, we then 'commit' the 
         * operation, setting our new TCP socket and UDP sockets.
         */
        else {
            
            if(LOG.isDebugEnabled())
                LOG.debug("changing port to " + port);

            DatagramSocket udpServiceSocket = udpService.get().newListeningSocket(port);

            LOG.trace("UDP Service is ready.");
            
            MulticastSocket mcastServiceSocket = null;
            try {
                InetAddress mgroup = InetAddress.getByName(
                    ConnectionSettings.MULTICAST_ADDRESS.get()
                );
                mcastServiceSocket =                            
                    multicastService.get().newListeningSocket(
                        ConnectionSettings.MULTICAST_PORT.getValue(), mgroup
                    );
                LOG.trace("multicast service setup");
            } catch(IOException e) {
                LOG.warn("can't create multicast socket", e);
            }
            
        
            //a) Try new port.
            ServerSocket newSocket=null;
            try {
                newSocket = SocketFactory.newServerSocket(port, new SocketListener());
            } catch (IOException e) {
                LOG.warn("can't create ServerSocket", e);
                udpServiceSocket.close();
                throw e;
            } catch (IllegalArgumentException e) {
                LOG.warn("can't create ServerSocket", e);
                udpServiceSocket.close();
                throw new IOException("could not create a listening socket");
            }
            //b) Close old socket
            IOUtils.close(_socket);
            
            //c) Replace with new sock.
            _socket=newSocket;
            _port=port;

            LOG.trace("Acceptor ready..");

            // Commit UDPService's new socket
            udpService.get().setListeningSocket(udpServiceSocket);
            // Commit the MulticastService's new socket
            // if we were able to get it
            if (mcastServiceSocket != null) {
                multicastService.get().setListeningSocket(mcastServiceSocket);
            }

            if(LOG.isDebugEnabled())
                LOG.debug("listening UDP/TCP on " + _port);
            networkManager.portChanged();
        }
    }


	/* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#acceptedIncoming()
     */
	public boolean acceptedIncoming() {
        return _acceptedIncoming;
	}

	/**
	 * For testing.
	 */
	protected void setAcceptedIncoming(boolean incoming) {
        _acceptedIncoming = incoming;
    }
	
	/**
	 * Sets the new incoming status.
	 * Returns whether or not the status changed.
	 */
	boolean setIncoming(boolean canReceiveIncoming) {
	    synchronized(ADDRESS_LOCK) {
            if (canReceiveIncoming) 
                incomingValidator.cancelReset();
    	    
    	    if (_acceptedIncoming == canReceiveIncoming)
                return false;
            
    	    _acceptedIncoming = canReceiveIncoming;
            if(canReceiveIncoming) {
                firewallBroadcaster.broadcast(new FirewallStatusEvent(FirewallStatus.NOT_FIREWALLED));
            } else {
                firewallBroadcaster.broadcast(new FirewallStatusEvent(FirewallStatus.FIREWALLED)); 
            }
	    }
	    
        if(canReceiveIncoming) {
            ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        }
    
        return true;
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#acceptConnection(java.lang.String, java.net.Socket)
     */
	public void acceptConnection(String word, Socket s) {
	    checkFirewall(s.getInetAddress());
        IOUtils.close(s);
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#checkFirewall(java.net.InetAddress)
     */
	void checkFirewall(InetAddress address) {
		// we have accepted an incoming socket -- only record
        // that we've accepted incoming if it's definitely
        // not from our local subnet and we aren't connected to
        // the host already.
        boolean changed = false;
        if(isOutsideConnection(address)) {
            changed = setIncoming(true);
        }
        if(changed)
            networkManager.incomingStatusChanged();
    }


    /**
     * Listens for new incoming sockets & starts a thread to
     * process them if necessary.
     */
	private class SocketListener implements AcceptObserver {
        
        public void handleIOException(IOException iox) {
            LOG.warn("IOX while accepting", iox);
        }
        
        public void shutdown() {
            LOG.debug("shutdown one SocketListener");
        }
        
        public void handleAccept(Socket client) {
            processSocket(client);
        }
    }
    
    /* (non-Javadoc)
	 * @see com.limegroup.gnutella.SocketProcessor#processSocket(java.net.Socket)
	 */
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#processSocket(java.net.Socket)
     */
    public void processSocket(Socket client) {
        processSocket(client, null);
    }
    
    /* (non-Javadoc)
	 * @see com.limegroup.gnutella.SocketProcessor#processSocket(java.net.Socket, java.lang.String)
	 */
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#processSocket(java.net.Socket, java.lang.String)
     */
    public void processSocket(Socket client, String allowedProtocol) {
        if (!_started) {
            IOUtils.close(client);
            return;
        }

        // If the client was closed before we were able to get the address,
        // then getInetAddress will return null.
        InetAddress address = client.getInetAddress();
        if (address == null || !NetworkUtils.isValidAddress(address) ||
        		!NetworkUtils.isValidPort(client.getPort())) {
            IOUtils.close(client);
            LOG.warn("connection closed while accepting");
        } else if (!ipFilter.get().allow(address.getAddress())) {
            if (LOG.isWarnEnabled())
                LOG.warn("Ignoring banned host: " + address);
            IOUtils.close(client);
        } else {
            if (LOG.isDebugEnabled())
                LOG.debug("Dispatching new client connecton: " + address);

            // Set our IP address of the local address of this socket.
            InetAddress localAddress = client.getLocalAddress();
            setAddress(localAddress);

            try {
                client.setSoTimeout(Constants.TIMEOUT);
            } catch (SocketException se) {
                IOUtils.close(client);
                return;
            }

            // Dispatch asynchronously if possible.
            if (client instanceof NIOMultiplexor) {// supports non-blocking reads
                ((NIOMultiplexor) client).setReadObserver(new AsyncConnectionDispatcher(connectionDispatcher.get(), client, allowedProtocol, networkManager.isIncomingTLSEnabled()));
            } else {
                ThreadExecutor.startThread(new BlockingConnectionDispatcher(connectionDispatcher
                        .get(), client, allowedProtocol), "ConnectionDispatchRunner");
            }
        }
    }
    
    /**
     * Determines whether or not this INetAddress is found an outside source, so as to correctly set "acceptedIncoming"
     * to true.
     * 
     * This ignores connections from private or local addresses, ignores those who may be on the same subnet, and
     * ignores those who we are already connected to.
     */
    private boolean isOutsideConnection(InetAddress addr) {
        // short-circuit for tests.
        if(!ConnectionSettings.LOCAL_IS_PRIVATE.getValue())
            return true;
        
        return !connectionServices.isConnectedTo(addr) &&
               !NetworkUtils.isLocalAddress(addr);
	}
    
    /**
     * Resets the last connectback time.
     */
    public void resetLastConnectBackTime() {
        _lastConnectBackTime = 0; // long ago 
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.Acceptor#shutdown()
     */
    public void shutdown() {
        shutdownUPnP();
    }
    
    private void shutdownUPnP() {
        if(isUPnPEnabled() &&
           upnpManager.get().isNATPresent() &&
           upnpManager.get().mappingsExist() &&
           ConnectionSettings.UPNP_IN_USE.getValue()) {
        	// reset the forced port values - must happen before we save them to disk
        	ConnectionSettings.FORCE_IP_ADDRESS.revertToDefault();
        	ConnectionSettings.FORCED_PORT.revertToDefault();
        	ConnectionSettings.UPNP_IN_USE.revertToDefault();
        }
    }    
    
    /**
     * (Re)validates acceptedIncoming.
     */
    
    private class IncomingValidator implements Runnable {
        private final AtomicBoolean validating = new AtomicBoolean(false);
        private AtomicReference<Future<?>> futureRef = new AtomicReference<Future<?>>();
        
        public void run() {
            if (validating.getAndSet(true)) {
                LOG.debug("Attempt to validate while already validating, aborting check");
                return;
            }
            
            // clear and revalidate if we haven't done so in a while
            final long currTime = System.currentTimeMillis();
            if (currTime - _lastConnectBackTime > incomingExpireTime) {
                LOG.debug("Time elapsed -- triggering TCP connectbacks");
                // send a connectback request to a few peers and clear
                // _acceptedIncoming IF some requests were sent.
                if(connectionManager.get().sendTCPConnectBackRequests())  {
                    LOG.debug("Sent TCP connectbacks, scheduling unset of accept-incoming");
                    _lastConnectBackTime = currTime;
                    Runnable resetter = new Runnable() {
                        public void run() {
                            boolean changed = setIncoming(false);
                            if(changed)
                                networkManager.incomingStatusChanged();
                        }
                    };
                    // Cancel any old future before we schedule this one
                    Future<?> oldRef = futureRef.get();
                    if(oldRef != null)
                        oldRef.cancel(false);
                    futureRef.set(backgroundExecutor.schedule(resetter, 
                                           waitTimeAfterRequests, TimeUnit.MILLISECONDS));
                } 
            }
            validating.set(false);
        }
        
        void cancelReset() {
            Future<?> resetter = futureRef.get();
            if (resetter != null) {
                resetter.cancel(false);
                // unset the ref if it's still the current future
                futureRef.compareAndSet(resetter, null);
            }
        }
    }

    public long getIncomingExpireTime() {
        return incomingExpireTime;
    }

    /**
     * Only used for testing.
     */
    void setIncomingExpireTime(long incomingExpireTime) {
        this.incomingExpireTime = incomingExpireTime;
    }

    public long getWaitTimeAfterRequests() {
        return waitTimeAfterRequests;
    }

    /**
     * Only used for testing.
     */
    void setWaitTimeAfterRequests(long waitTimeAfterRequests) {
        this.waitTimeAfterRequests = waitTimeAfterRequests;
    }

    public long getTimeBetweenValidates() {
        return timeBetweenValidates;
    }

    void setTimeBetweenValidates(long timeBetweenValidates) {
        this.timeBetweenValidates = timeBetweenValidates;
    }
    
}
