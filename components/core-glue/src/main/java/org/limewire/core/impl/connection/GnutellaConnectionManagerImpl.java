package org.limewire.core.impl.connection;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.connection.ConnectionItem;
import org.limewire.core.api.connection.ConnectionLifecycleEventType;
import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.util.Objects;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.connection.RoutedConnection;

/**
 * An implementation of GnutellaConnectionManager for the live core. 
 */
@EagerSingleton
public class GnutellaConnectionManagerImpl 
    implements GnutellaConnectionManager, ConnectionLifecycleListener {

    /** The number of messages a connection must have sent before we consider it stable. */
    private static final int STABLE_THRESHOLD = 5;
    
    private final ConnectionManager connectionManager;
    private final PropertyChangeSupport changeSupport = new SwingSafePropertyChangeSupport(this);
    private final ConnectionServices connectionServices;

    /** Mapping of connections to ConnectionItem instances. */
    private final Map<RoutedConnection, ConnectionItem> connectionMap;
    
    /** List of ConnectionItem instances. */
    private final EventList<ConnectionItem> connectionItemList;
    
    private volatile ConnectionStrength currentStrength = ConnectionStrength.DISCONNECTED;
    
    volatile ConnectionLifecycleEventType lastStrengthRelatedEvent;
    volatile long lastIdleTime;
    
    /**
     * Constructs the live implementation of GnutellaConnectionManager using 
     * the specified connection and library services.
     */
    @Inject
    public GnutellaConnectionManagerImpl(
            ConnectionManager connectionManager,
            ConnectionServices connectionServices) {
        
        this.connectionManager = Objects.nonNull(connectionManager, "connectionManager");
        this.connectionServices = connectionServices;

        // Create map of connection items.
        connectionMap = new HashMap<RoutedConnection, ConnectionItem>();
        
        // Create list of connection items as thread safe list.
        connectionItemList = GlazedListsFactory.threadSafeList(
                new BasicEventList<ConnectionItem>());
    }

    /**
     * Adds this as a listener for core connection events. 
     */
    @Inject
    void registerListener() {
        connectionManager.addEventListener(this);    
    }
    
    /**
     * Register the periodic connection strength updater service.
     */
    @Inject 
    void registerService(ServiceRegistry registry, final @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        registry.register(new Service() {
            private volatile ScheduledFuture<?> meter;
            private volatile ConnectionLifecycleListener listener;
            
            @Override
            public String getServiceName() {
                return "Connection Strength Meter";
            }
            
            @Override
            public void initialize() {
                listener = new ConnectionLifecycleListener() {
                    @Override
                    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
                        switch(evt.getType()) {
                        case NO_INTERNET:
                        case CONNECTION_INITIALIZED:
                        case CONNECTED:
                            lastStrengthRelatedEvent = evt.getType();
                            break;
                        }
                    }
                };
                connectionManager.addEventListener(listener);
            }
            
            @Override
            public void start() {
                meter = backgroundExecutor.scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        setConnectionStrength(calculateStrength());
                    }
                }, 0, 1, TimeUnit.SECONDS);
            }
            
            @Override
            public void stop() {
                if(meter != null) {
                    meter.cancel(false);
                    meter = null;
                }
                if(listener != null) {
                    connectionManager.removeEventListener(listener);
                    listener = null;
                }
            }
        });
    }
    
    private void setConnectionStrength(ConnectionStrength newStrength) {
        ConnectionStrength oldStrength = currentStrength;
        currentStrength = newStrength;
        changeSupport.firePropertyChange(GnutellaConnectionManager.CONNECTION_STRENGTH, oldStrength, newStrength);
    }
    
    ConnectionStrength calculateStrength() {
        int stable = connectionManager.countConnectionsWithNMessages(STABLE_THRESHOLD);
            
        ConnectionStrength strength;

        if(stable == 0) {
            int initializing = connectionManager.getNumFetchingConnections();
            int connections = connectionManager.getNumInitializedConnections();
            // No initializing or stable connections
            if(initializing == 0 && connections == 0) {
                //Not attempting to connect at all...
                if(!connectionManager.isConnecting()) {
                    strength = ConnectionStrength.DISCONNECTED;
                } else {
                    //Attempting to connect...
                    strength = ConnectionStrength.CONNECTING;
                }
            } else if(connections == 0) {
                // No initialized, all initializing - connecting
                strength = ConnectionStrength.CONNECTING;
            } else {
                // Some initialized - poor connection.
                strength = ConnectionStrength.WEAK;
            }
        } else if(connectionManager.isConnectionIdle()) {
            lastIdleTime = System.currentTimeMillis();
            strength = ConnectionStrength.FULL;
        } else {
            int preferred = connectionManager.getPreferredConnectionCount();
            // account for pro having more connections.
            preferred -= 2;
            
            // ultrapeers don't need as many...
            if(connectionManager.isSupernode()) {
                preferred -= 5;
            }
            
            preferred = Math.max(1, preferred); // prevent div by 0

            double percent = (double)stable / (double)preferred;
            if(percent <= 0.15) {
                strength = ConnectionStrength.WEAK;
            } else if(percent <= 0.30) {
                strength = ConnectionStrength.WEAK_PLUS;
            } else if(percent <= 0.5) {
                strength = ConnectionStrength.MEDIUM;
            } else if(percent <= 0.75) {
                strength = ConnectionStrength.MEDIUM_PLUS;
            } else if(percent <= 1) {
                strength = ConnectionStrength.FULL;
            } else /* if(percent > 1) */ {
                strength = ConnectionStrength.TURBO;
            }
        }
        
        switch(strength) {
        case DISCONNECTED:
        case CONNECTING:
            if(lastStrengthRelatedEvent == ConnectionLifecycleEventType.NO_INTERNET) {
                strength = ConnectionStrength.NO_INTERNET;
            }    
        }
        
        switch(strength) {
        case CONNECTING:
        case WEAK:
            // if one of these four, see if we recently woke up from
            // idle, and if so, report as 'waking up' instead.
            long now = System.currentTimeMillis();
            if(now < lastIdleTime + 15 * 1000)
                strength = ConnectionStrength.MEDIUM;
        }
                
        return strength;
    }

    @Override
    public boolean isConnected() {
        return connectionServices.isConnected();
    }

    @Override
    public boolean isUltrapeer() {
        return connectionManager.isSupernode();
    }

    @Override
    public void connect() {
        connectionServices.connect();
    }
    
    @Override
    public void disconnect() {
        connectionServices.disconnect();
    }
    
    @Override
    public void restart() {
        connectionManager.disconnect(true);
        connectionManager.connect();
    }
    
    @Override
    public ConnectionStrength getConnectionStrength() {
        return currentStrength;
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Returns the list of connections.  An application should NOT assume that 
     * the returned list is Swing-compatible; Swing is suppported by wrapping
     * the resulting list via a call to <code>
     * GlazedListsFactory.swingThreadProxyEventList()</code>.
     */
    @Override
    public EventList<ConnectionItem> getConnectionList() {
        return connectionItemList;
    }

    /**
     * Removes the specified connection from the list.
     */
    @Override
    public void removeConnection(ConnectionItem item) {
        if (item instanceof CoreConnectionItem) {
            RoutedConnection connection = ((CoreConnectionItem) item).getRoutedConnection();
            connectionServices.removeConnection(connection);
        }
    }

    /**
     * Attempts to establish a connection to the specified host and port.
     */
    @Override
    public void tryConnection(String hostname, int portnum, boolean useTLS) {
        connectionServices.connectToHostAsynchronously(hostname, portnum,
                useTLS ? ConnectType.TLS : ConnectType.PLAIN);
    }

    /**
     * Handles connection lifecycle events fired by the ConnectionManager.
     */
    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        // Get connection.
        RoutedConnection c = evt.getConnection();
        if (c != null) {
            if (evt.isConnectionInitializingEvent()) {
                // Add new connection.
                addConnection(c);

            } else if (evt.isConnectionInitializedEvent()) {
                // Update status when connection is fully initialized.
                updateConnection(c);

            } else if (evt.isConnectionClosedEvent()) {
                // Remove connection when closed.
                removeConnection(c);
            }
        }
    }
    
    /**
     * Adds the specified connection to the list.
     */
    private void addConnection(RoutedConnection connection) {
        ConnectionItem connectionItem = connectionMap.get(connection);
        if (connectionItem == null) {
            connectionItem = new CoreConnectionItem(connection);
            connectionMap.put(connection, connectionItem);
            connectionItemList.add(connectionItem);
        }
    }
    
    /**
     * Updates the specified connection in the list.
     */
    private void updateConnection(RoutedConnection connection) {
        ConnectionItem connectionItem = connectionMap.get(connection);
        if (connectionItem != null) {
            connectionItem.update();
        }
    }
    
    /**
     * Removes the specified connection from the list.
     */
    private void removeConnection(RoutedConnection connection) {
        ConnectionItem connectionItem = connectionMap.get(connection);
        if (connectionItem != null) {
            connectionItemList.remove(connectionItem);
            connectionMap.remove(connection);
        }
    }
}
