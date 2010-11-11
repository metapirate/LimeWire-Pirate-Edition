package org.limewire.net;


import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.io.Address;
import org.limewire.io.NetworkUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.io.AddressConnectingLoggingCategory;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.AddressConnector;
import org.limewire.net.address.AddressResolutionObserver;
import org.limewire.net.address.AddressResolver;
import org.limewire.nio.NBSocket;
import org.limewire.nio.NBSocketFactory;
import org.limewire.nio.observer.ConnectObserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/** Factory for creating Sockets. */
@Singleton
public class SocketsManagerImpl implements SocketsManager, EventBroadcaster<ConnectivityChangeEvent>, ListenerSupport<ConnectivityChangeEvent> {
    
    private final static Log LOG = LogFactory.getLog(SocketsManagerImpl.class, AddressConnectingLoggingCategory.CATEGORY);
    
    private final SocketController socketController;
    
    private final List<AddressResolver> addressResolvers = new CopyOnWriteArrayList<AddressResolver>();
    
    private final List<AddressConnector> addressConnectors = new CopyOnWriteArrayList<AddressConnector>();
    
    private final EventMulticaster<ConnectivityChangeEvent> connectivityEventMulticaster = new EventMulticasterImpl<ConnectivityChangeEvent>();
    
    public SocketsManagerImpl() {
        this(new SimpleSocketController(new ProxyManagerImpl(new EmptyProxySettings(), new SimpleNetworkInstanceUtils()), new EmptySocketBindingSettings()));
    }
    
    @Inject
    public SocketsManagerImpl(SocketController socketController) {
        this.socketController = socketController;
    }

    public Socket create(ConnectType type) throws IOException {
        return type.getFactory().createSocket();
    }

    public Socket connect(NBSocket socket, InetSocketAddress localAddr, InetSocketAddress addr, int timeout, ConnectType type) throws IOException {
        return connect(socket, localAddr, addr, timeout, null, type);    
    }

    public Socket connect(InetSocketAddress addr, int timeout) throws IOException {
        return connect(addr, timeout, ConnectType.PLAIN);
    }

    public Socket connect(InetSocketAddress addr, int timeout, ConnectType type) throws IOException {
        return connect(addr, timeout, null, type);
    }

    public Socket connect(InetSocketAddress addr, int timeout, ConnectObserver observer) throws IOException {
        return connect(addr, timeout, observer, ConnectType.PLAIN);
    }
    
    public Socket connect(InetSocketAddress addr, int timeout, ConnectObserver observer, ConnectType type) throws IOException {
        return connect(null, null, addr, timeout, observer, type);    
    }

    public Socket connect(final NBSocket socket, InetSocketAddress localAddr, InetSocketAddress addr, int timeout, ConnectObserver observer, ConnectType type) throws IOException {
        if(!NetworkUtils.isValidPort(addr.getPort()))  
            throw new IllegalArgumentException("port out of range: "+addr.getPort());
        if(addr.isUnresolved())
            throw new IOException("address must be resolved!");
        
        if(socket == null) {
            return socketController.connect(type.getFactory(), addr, null, timeout, observer);
	    } else {
	        NBSocketFactory factory = new NBSocketFactory() {
                @Override
                public NBSocket createSocket() throws IOException {
                    return socket;
                }

                @Override
                public NBSocket createSocket(String host, int port) throws IOException,
                        UnknownHostException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public NBSocket createSocket(InetAddress host, int port) throws IOException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public NBSocket createSocket(String host, int port, InetAddress localHost,
                        int localPort) throws IOException, UnknownHostException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public NBSocket createSocket(InetAddress address, int port,
                        InetAddress localAddress, int localPort) throws IOException {
                    throw new UnsupportedOperationException();
                }
	            
	        };
            return socketController.connect(factory, addr, localAddr, timeout, observer);
        }
    }

    public boolean removeConnectObserver(ConnectObserver observer) {
        return socketController.removeConnectObserver(observer);
    }	

	public int getNumAllowedSockets() {
        return socketController.getNumAllowedSockets();
	}

    public int getNumWaitingSockets() {
        return socketController.getNumWaitingSockets();
    }

    private AddressResolver getResolver(Address address) {
        for (AddressResolver resolver : addressResolvers) {
            if (resolver.canResolve(address)) {
                LOG.debugf("found resolver: {0} for: {1}", resolver, address);
                return resolver;
            }
        }
        return null;
    }
    
    private AddressConnector getConnector(Address address) {
        for (AddressConnector connector : addressConnectors) {
            if (connector.canConnect(address)) {
                LOG.debugf("found connector: {0} for: {1}", connector, address);
                return connector;
            }
        }
        return null;
    }
    
    @Override
    public boolean canConnect(Address address) {
        return getConnector(address) != null;
    }

    @Override
    public boolean canResolve(Address address) {
        return getResolver(address) != null;
    }
    
    @Override
    public <T extends ConnectObserver> T connect(Address address, final T observer) {
        // feel free to rework this logic with more use cases that don't fit the model
        // for example we're only doing one cycle of address resolution, might have to 
        // be done iteratively if addresses are resolved to address that need more resolution
        if (address == null) { 
            throw new NullPointerException("address must not be null");
        }
        if (canResolve(address)) {
            LOG.debugf("trying to resolve for connect: {0}", address);
            resolve(address, new AddressResolutionObserver() {
                @Override
                public void resolved(Address address) {
                    connectUnresolved(address, observer);
                }
                @Override
                public void handleIOException(IOException iox) {
                    observer.handleIOException(iox);
                }
                @Override
                public void shutdown() {
                    // observer.shutdown();
                }
            }); 
        } else {
            LOG.debugf("trying to connect unresolved: {0}", address);
            connectUnresolved(address, observer);
        }
        return observer;
    }
    
    private void connectUnresolved(Address address, ConnectObserver observer) {
        AddressConnector connector = getConnector(address);
        if (connector != null) {
            connector.connect(address, observer);
        } else {
            observer.handleIOException(new ConnectException("no connector ready to connect to: " + address));
            observer.shutdown();
        }
    }

    @Override
    public <T extends AddressResolutionObserver> T resolve(final Address address, final T observer) {
        // feel free to rework this logic with more use cases that don't fit the model
        if (address == null) { 
            throw new NullPointerException("address must not be null");
        }
        AddressResolver resolver = getResolver(address);
        if (resolver != null) {
            resolver.resolve(address, new AddressResolutionObserver() {
                @Override
                public void resolved(Address resolvedAddress) {
                    LOG.debugf("resolved {0} to {1}", address, resolvedAddress);
                    if (canResolve(resolvedAddress)) {
                        resolve(resolvedAddress, this);
                    } else {
                        observer.resolved(resolvedAddress);
                    }
                }
                @Override
                public void handleIOException(IOException iox) {
                    observer.handleIOException(iox);
                }
                @Override
                public void shutdown() {
                    observer.shutdown();
                }
            });
        } else {
            LOG.debugf("not resolver found for: {0}", address);
            observer.handleIOException(new IOException(address + " cannot be resolved"));
        }
        return observer;
    }
    
    @Override
    public void registerConnector(AddressConnector connector) {
        addressConnectors.add(connector);
    }

    @Override
    public void registerResolver(AddressResolver resolver) {
        addressResolvers.add(resolver);
    }

    @Override
    public void addListener(EventListener<ConnectivityChangeEvent> listener) {
        connectivityEventMulticaster.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<ConnectivityChangeEvent> listener) {
        return connectivityEventMulticaster.removeListener(listener);
    }

    @Override
    public void broadcast(ConnectivityChangeEvent event) {
        connectivityEventMulticaster.broadcast(event);
    }
}
