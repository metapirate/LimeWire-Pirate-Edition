package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.DHTSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.IpPort;
import org.limewire.lifecycle.Service;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.util.DebugRunnable;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

/**
 * This DHT manager starts either an active or a passive DHT controller.
 * It also handles switching from one mode to the other.
 * <p>
 * This class offloads blocking operations to a thread pool
 * so that it never blocks on critical threads such as MessageDispatcher.
 */
@EagerSingleton
public class DHTManagerImpl implements DHTManager, Service {
    
    private static final Log LOG = LogFactory.getLog(DHTManagerImpl.class);
    
    /**
     * The Vendor code of this DHT Node.
     */
    private final Vendor vendor = ContextSettings.getVendor();
    
    /**
     * The Version of this DHT Node.
     */
    private final Version version = ContextSettings.getVersion();
    
    /**
     * The DHTController instance.
     */
    private DHTController controller = new NullDHTController();
    
    /**
     * List of event listeners for ConnectionLifeCycleEvents.
     */
    private final List<DHTEventListener> dhtEventListeners = new ArrayList<DHTEventListener>(1);
    
    /** 
     * The executor to use to execute blocking DHT methods, such
     * as stopping or starting a Mojito instance (which perform 
     * network and disk I/O). 
     * */
    private final Executor executor;
    
    /**
     * The executor to use for dispatching events.
     */
    private final Executor dispatchExecutor;
    
    private volatile boolean enabled = true;
    
    private final DHTControllerFactory dhtControllerFactory;
    
    /**
     * Constructs the DHTManager, using the given Executor to invoke blocking 
     * methods. The executor MUST be single-threaded, otherwise there will be 
     * failures.
     * 
     * @param service executor for executing blocking DHT methods
     * @param dhtControllerFactory creates DHT node controllers
     */
    @Inject
    public DHTManagerImpl(@Named("dhtExecutor") Executor service, DHTControllerFactory dhtControllerFactory) {
        this.executor = service;
        this.dispatchExecutor = ExecutorsHelper.newProcessingQueue("DHT-EventDispatch");
        this.dhtControllerFactory = dhtControllerFactory;
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Mojito DHT");
    }
    
    public void initialize() {
    }
    
    public void start() {
    }    
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.gnutella.dht.DHTManager#setEnabled(boolean)
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.gnutella.dht.DHTManager#isEnabled()
     */
    public boolean isEnabled() {
        if (!DHTSettings.DISABLE_DHT_NETWORK.getValue() 
                && !DHTSettings.DISABLE_DHT_USER.getValue()
                && enabled) {
            return true;
        }
        return false;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.gnutella.dht.DHTManager#start(com.limegroup.gnutella.dht.DHTManager.DHTMode)
     */
    public synchronized void start(DHTMode mode) {
        executor.execute(createSwitchModeCommand(mode));
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.gnutella.dht.DHTManager#stop()
     */
    public synchronized void stop() {
        Runnable command = new DebugRunnable(new Runnable() {
            public void run() {
                synchronized (DHTManagerImpl.this) {
                    try {
                        createSwitchModeCommand(DHTMode.INACTIVE).run();
                    } finally {
                        DHTManagerImpl.this.notifyAll();
                    }
                }
            }
        });
        
        executor.execute(command);
        
        try {
            this.wait(10000);
        } catch (InterruptedException err) {
            LOG.error("InterruptedException", err);
        }
    }
    
    /**
     * Creates and returns a Runnable that switches the DHT node from
     * the current <code>DHTMode</code> to the given <code>mode</code>.
     * 
     * @param mode the new mode of the DHT node
     * @return Runnable that switches the mode
     */
    private Runnable createSwitchModeCommand(final DHTMode mode) {
        Runnable command = new DebugRunnable(new Runnable() {
            public void run() {
                synchronized (DHTManagerImpl.this) {
                    // Controller already running in the current mode?
                    if (controller.getDHTMode() == mode) {
                        return;
                    }
                    
                    controller.stop();

                    if (mode == DHTMode.ACTIVE) {
                        controller = dhtControllerFactory.createActiveDHTNodeController(
                                vendor, version, DHTManagerImpl.this);
                    } else if (mode == DHTMode.PASSIVE) {
                        controller = dhtControllerFactory
                                .createPassiveDHTNodeController(vendor,
                                        version, DHTManagerImpl.this);
                    } else if (mode == DHTMode.PASSIVE_LEAF) {
                        controller = dhtControllerFactory.createPassiveLeafController(
                                vendor, version, DHTManagerImpl.this);
                    } else {
                        controller = new NullDHTController();
                    }
                    
                    controller.start();
                }
            }
        });
        
        return command;
    }
    
    public void addActiveDHTNode(final SocketAddress hostAddress) {
        executor.execute(new Runnable() {
            public void run() {
                synchronized(DHTManagerImpl.this) {
                    controller.addActiveDHTNode(hostAddress);
                }
            }
        });
    }
    
    public void addPassiveDHTNode(final SocketAddress hostAddress) {
        executor.execute(new Runnable() {
            public void run() {
                synchronized(DHTManagerImpl.this) {
                    controller.addPassiveDHTNode(hostAddress);
                }
            }
        });
    }

    public void addressChanged() {
        // Do this in a different thread as there are some blocking
        //disk and network ops.
        executor.execute(new DebugRunnable(new Runnable() {
            public void run() {
                synchronized(DHTManagerImpl.this) {
                    if (controller.isRunning()) {
                        controller.stop();
                        controller.start();
                    }
                }
            }
        }));
    }
    
    public synchronized List<IpPort> getActiveDHTNodes(int maxNodes){
        return controller.getActiveDHTNodes(maxNodes);
    }
    
    public synchronized DHTMode getDHTMode() {
        return controller.getDHTMode();
    }
    
    public synchronized boolean isRunning() {
        return controller.isRunning();
    }
    
    public synchronized boolean isBootstrapped() {
        return controller.isBootstrapped();
    }
    
    public synchronized boolean isMemberOfDHT() {
        return isRunning() && isBootstrapped();
    }

    public synchronized boolean isWaitingForNodes() {
        return controller.isWaitingForNodes();
    }
    
    /**
     * Adds a listener to DHT Events.
     * <p>
     * Be aware that listeners will receive events after
     * after the DHT has dispatched them.  It is possible that
     * the DHT's status may have changed between the time the 
     * event was dispatched and the time the event is received
     * by a listener.
     */
    public synchronized void addEventListener(DHTEventListener listener) {
        if(dhtEventListeners.contains(listener))
            throw new IllegalArgumentException("Listener " + listener + " already registered");
        
        dhtEventListeners.add(listener);
    }

    /**
     * Sends an event to all listeners.
     * <p>
     * Be aware that to prevent deadlock, listeners may receive
     * the event long after the DHT's status has changed, and the
     * current status may be very different.
     * <p>
     * No events will be received in a different order than they were
     * dispatched, though.
     */
    public synchronized void dispatchEvent(final DHTEvent event) {
        if(!dhtEventListeners.isEmpty()) {
            final List<DHTEventListener> listeners = new ArrayList<DHTEventListener>(dhtEventListeners);
            dispatchExecutor.execute(new Runnable() {
                public void run() {
                    for(DHTEventListener listener : listeners) {
                        listener.handleDHTEvent(event);
                    }        
                }
            });
        }
    }

    public synchronized void removeEventListener(DHTEventListener listener) {
        dhtEventListeners.remove(listener);
    }

    /**
     * This getter is for internal use only. The Mojito DHT is not meant to
     * be handled or passed around independently, as only the DHT controllers 
     * know how to interact correctly with it.
     */
    public synchronized MojitoDHT getMojitoDHT() {
        return controller.getMojitoDHT();
    }

    /**
     * Shuts the DHT down if we got disconnected from the network.
     * The nodeAssigner will take care of restarting this DHT node if 
     * it still qualifies.
     * <p>
     * If this event is not related to disconnection from the network, it
     * is forwarded to the controller for proper handling.
     */
    public void handleConnectionLifecycleEvent(final ConnectionLifecycleEvent evt) {
        Runnable command = null;
        if (evt.isDisconnectedEvent() || evt.isNoInternetEvent()) {
            command = new DebugRunnable( new Runnable() {
                public void run() {
                    synchronized(DHTManagerImpl.this) {
                        if (controller.isRunning() 
                                && !DHTSettings.FORCE_DHT_CONNECT.getValue()) {
                            controller.stop();
                            controller = new NullDHTController();
                        }
                    }
                }
            });
        } else {
            command = new Runnable() {
                public void run() {
                    synchronized(DHTManagerImpl.this) {
                        controller.handleConnectionLifecycleEvent(evt);
                    }
                }
            };
        }
        executor.execute(command);
    }
    
    public Vendor getVendor() {
        return vendor;
    }
    
    public Version getVersion() {
        return version;
    }
    
    public void handleDHTContactsMessage(final DHTContactsMessage msg) {
        executor.execute(new Runnable() {
            public void run() {
                synchronized(DHTManagerImpl.this) {
                    for (Contact node : msg.getContacts()) {
                        controller.addContact(node);
                    }
                }
            }
        });
    }
    
    /**
     * Calls the {@link MojitoDHT#put} if a bootstrappable DHT is available.
     * Also handles the locking properly to ensure thread safety.
     * 
     * @param eKey the entity key used to perform lookup in the DHT.
     * 
     * @return an instance of <code>DHTFuture</code> containing the result of the lookup. 
     * <br> Returns null if DHT is unavailable or the DHT is not bootstrapped.
     *          
     */
    public synchronized DHTFuture<FindValueResult> get(EntityKey eKey) {
        MojitoDHT mojitoDHT = getMojitoDHT();
        
        if (LOG.isDebugEnabled())
            LOG.debug("DHT:" + mojitoDHT);
        
        if (mojitoDHT == null || !mojitoDHT.isBootstrapped()) {
            LOG.debug("DHT is null or is not bootstrapped");                
            return null;
        }            

        // instantiated here so it can record its instantiation time
        DHTFuture<FindValueResult> future = mojitoDHT.get(eKey);
        return future;
    }
    
    /**
     * Calls the {@link MojitoDHT#put} if a bootstrappable DHT is available.
     * Also handles the locking properly to ensure thread safety.
     * 
     * @param key a unique id used as a key to find the associated value.
     * @param value the value which will be stored in the DHT.
     * 
     * @return an instance of <code>DHTFuture</code> containing the result of the storage.
     * <br> Returns null if DHT is unavailable or the DHT is not bootstrapped.
     */
    public synchronized DHTFuture<StoreResult> put(KUID key, DHTValue value) {
        MojitoDHT mojitoDHT = getMojitoDHT();

        if (LOG.isDebugEnabled())
            LOG.debug("DHT: " + mojitoDHT);

        if (mojitoDHT == null || !mojitoDHT.isBootstrapped()) {
            LOG.debug("DHT is null or unable to bootstrap");                
            return null;
        }
        DHTFuture<StoreResult> future = mojitoDHT.put(key, value);
        return future;
    }
}