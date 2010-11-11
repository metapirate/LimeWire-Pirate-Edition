package org.limewire.core.impl.friend;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.ConnectBackRequestFeature;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.impl.address.FriendAddressResolver;
import org.limewire.friend.impl.address.FriendFirewalledAddress;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectBackRequest;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.AddressConnector;
import org.limewire.net.address.FirewalledAddress;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.rudp.UDPSelectorProvider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.SocketProcessor;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.downloader.PushedSocketHandler;
import com.limegroup.gnutella.downloader.PushedSocketHandlerRegistry;

/**
 * Connects an {@link org.limewire.friend.impl.address.FriendFirewalledAddress} and tries to get a socket for it.
 */
@EagerSingleton
class FriendFirewalledAddressConnector implements AddressConnector, PushedSocketHandler {

    private static final Log LOG = LogFactory.getLog(FriendFirewalledAddressConnector.class, LOGGING_CATEGORY);
    
    private final PushDownloadManager pushDownloadManager;
    private final NetworkManager networkManager;
    private final ScheduledExecutorService backgroundExecutor;
    
    final List<PushedSocketConnectObserver> observers = new CopyOnWriteArrayList<PushedSocketConnectObserver>();

    private final Provider<UDPSelectorProvider> udpSelectorProvider;

    private final Provider<SocketProcessor> socketProcessor;

    private final FriendAddressResolver friendAddressResolver;

    @Inject
    public FriendFirewalledAddressConnector(FriendAddressResolver friendAddressResolver, PushDownloadManager pushDownloadManager,
            NetworkManager networkManager, @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<UDPSelectorProvider> udpSelectorProvider,
            Provider<SocketProcessor> socketProcessor) {
        this.friendAddressResolver = friendAddressResolver;
        this.pushDownloadManager = pushDownloadManager;
        this.networkManager = networkManager;
        this.backgroundExecutor = backgroundExecutor;
        this.udpSelectorProvider = udpSelectorProvider;
        this.socketProcessor = socketProcessor;
    }
    
    @Inject 
    void register(SocketsManager socketsManager) {
        socketsManager.registerConnector(this);
    }
    
    @Inject
    void register(PushedSocketHandlerRegistry pushedSocketHandlerRegistry) {
        pushedSocketHandlerRegistry.register(this);
    }
    
    @Override
    public boolean canConnect(Address address) {
        if (address instanceof FriendFirewalledAddress) {
            FriendFirewalledAddress friendFirewalledAddress = (FriendFirewalledAddress)address;
            // let push download manager decide if we're locally capabable of connecting
            boolean canConnect = pushDownloadManager.canConnect(friendFirewalledAddress.getFirewalledAddress());
            LOG.debugf("{0} connect remote address {1}, because PDM cannot connect {2}", (canConnect ? "can" : "can not"), address, friendFirewalledAddress.getFirewalledAddress());
            return canConnect;
        }
        LOG.debugf("can not connect remote address {0}", address);
        return false;
    }

    @Override
    public void connect(Address address, ConnectObserver observer) {
        try {
            connectSendingConnectBack(address, observer);
        } catch (ConnectBackRequestException ce) {
            LOG.debugf(ce, "could not send connect back request {0}", address);
            // fall back on push download manager
            pushDownloadManager.connect(((FriendFirewalledAddress)address).getFirewalledAddress(), observer);
        }
    }
    
    /**
     * @throws ConnectBackRequestException if sending the connect back request fails
     */
    void connectSendingConnectBack(Address address, ConnectObserver observer) throws ConnectBackRequestException {
        FriendFirewalledAddress friendFirewalledAddress = (FriendFirewalledAddress)address;
        FirewalledAddress firewalledAddress = friendFirewalledAddress.getFirewalledAddress();
        GUID clientGuid = firewalledAddress.getClientGuid();
        Connectable publicAddress = networkManager.getPublicAddress();
        if (!NetworkUtils.isValidIpPort(publicAddress)) {
            LOG.debugf("not a valid public address yet: {0}", publicAddress);
            observer.handleIOException(new ConnectException("no valid address yet: " + publicAddress));
            return;
        }
        
        boolean isFWT = !networkManager.acceptedIncomingConnection();
        
        FriendPresence presence = friendAddressResolver.getPresence(friendFirewalledAddress.getFriendAddress());
        if (presence == null) {
            throw new ConnectBackRequestException("no presence available for: " + friendFirewalledAddress.getFriendAddress());
        }
        FeatureTransport<ConnectBackRequest> transport = presence.getTransport(ConnectBackRequestFeature.class);
        if (transport == null) {
            throw new ConnectBackRequestException("no transport for presence: " + presence);
        }
        /* there's a slight race condition, if a connection was just accepted between getting the address
         * and checking for it in the call below, but this should only change the address wrt to port vs
         * udp port which are usually the same anyways.
         */
        final PushedSocketConnectObserver pushedSocketObserver = new PushedSocketConnectObserver(firewalledAddress, observer);
        observers.add(pushedSocketObserver);
        try {
            transport.sendFeature(presence, new ConnectBackRequest(publicAddress, clientGuid, isFWT ? networkManager.supportsFWTVersion() : 0));
        } catch (FriendException e) {
            // clean up observer
            observers.remove(pushedSocketObserver);
            throw new ConnectBackRequestException(e);
        }
        
        if (isFWT) {
            LOG.debug("Starting fwt communication");
            assert NetworkUtils.isValidIpPort(firewalledAddress.getPublicAddress()) : "invalid public address" + firewalledAddress;
            AbstractNBSocket socket = udpSelectorProvider.get().openSocketChannel().socket();
            socket.connect(firewalledAddress.getPublicAddress().getInetSocketAddress(), 20000, new ConnectObserver() {
                @Override
                public void handleConnect(Socket socket) throws IOException {
                    LOG.debugf("handling socket: {0}", socket);
                    // have to route connected socket through socket processor and PushDownloadManager
                    // so parsing of the GIV line is taken care of
                    socketProcessor.get().processSocket(socket, "GIV");
                }
                @Override
                public void handleIOException(IOException iox) {
                    pushedSocketObserver.handleIOException(iox);
                }
                @Override
                public void shutdown() {
                    pushedSocketObserver.handleIOException(new IOException("shutdown"));
                }
            });
        } else {
            // wait for the other side to open a TCP connection to this peer
        }
        scheduleExpirerFor(pushedSocketObserver, 30 * 1000);
    }

    private void scheduleExpirerFor(final PushedSocketConnectObserver pushedSocketObserver, int timeout) {
        backgroundExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                observers.remove(pushedSocketObserver);
                pushedSocketObserver.handleTimeout();
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean acceptPushedSocket(String file, int index, byte[] clientGUID, Socket socket) {
        for (PushedSocketConnectObserver observer: observers) {
            if (observer.acceptSocket(clientGUID, socket)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Keeps connection state around and notifies the original {@link ConnectObserver}
     * of failures or success, ensuring that only one event is reported to it.
     */
    static class PushedSocketConnectObserver {

        private final FirewalledAddress firewalledAddress;
        private final ConnectObserver observer;
        final AtomicBoolean acceptedOrFailed = new AtomicBoolean(false);

        public PushedSocketConnectObserver(FirewalledAddress firewalledAddress, ConnectObserver observer) {
            this.firewalledAddress = firewalledAddress;
            this.observer = observer;
        }

        public boolean acceptSocket(byte[] clientGuid, Socket socket) {
            if (Arrays.equals(clientGuid, firewalledAddress.getClientGuid().bytes())) {
                Connectable expectedAddress = firewalledAddress.getPublicAddress();
                if (NetworkUtils.isValidIpPort(expectedAddress) && !expectedAddress.getInetAddress().equals(socket.getInetAddress())) {
                    LOG.debugf("received socket from unexpected location, expected: {0}, actual: {1}", expectedAddress, socket);
                    return false;
                }
                if (acceptedOrFailed.compareAndSet(false, true)) {
                    try {
                        LOG.debugf("handling connect from: {0}", socket);
                        observer.handleConnect(socket);
                    } catch (IOException ie) {
                        IOUtils.close(socket);
                    }
                    return true;
                }
            }
            return false;
        }
        
        public void handleTimeout() {
            LOG.debug("handling timeout");
            handleIOException(new ConnectException("connect request timed out"));
        }
        
        public void handleIOException(IOException ie) {
            LOG.debug("handling io exception", ie);
            if (acceptedOrFailed.compareAndSet(false, true)) {
                observer.handleIOException(ie);
            }
        }
    }
    
    static class ConnectBackRequestException extends Exception {
        
        public ConnectBackRequestException(String message) {
            super(message);
        }
        
        public ConnectBackRequestException(Throwable cause) {
            super(cause);
        }
    }
}
