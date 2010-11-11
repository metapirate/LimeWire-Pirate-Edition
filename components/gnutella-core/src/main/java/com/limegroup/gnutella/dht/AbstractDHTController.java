package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Buffer;
import org.limewire.collection.FixedSizeLIFOSet;
import org.limewire.collection.FixedSizeLIFOSet.EjectionPolicy;
import org.limewire.concurrent.ManagedThread;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.IpPort;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.routing.RouteTable.RouteTableEvent;
import org.limewire.mojito.routing.RouteTable.RouteTableListener;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.HostFilter;
import org.limewire.security.SignatureVerifier;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.dht.DHTEvent.Type;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.dht.db.AbstractAltLocValue;
import com.limegroup.gnutella.dht.db.AbstractPushProxiesValue;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;
import com.limegroup.gnutella.util.EventDispatcher;

/**
 * The controller for the LimeWire DHT. A node should connect to the DHT only 
 * if it has previously been designated as capable by the <tt>NodeAssigner</tt> 
 * or if it is forced to. Once the node is a DHT node 
 * (if <tt>EXCLUDE_ULTRAPEERS</tt> is set to true) 
 * it should not try to connect as an Ultrapeer. 
 * <p>
 * The <code>NodeAssigner</code> should be the only class to have the authority 
 * to initialize the DHT and connect to the network.
 * <p>
 * This controller can be in one of the four following states:
 * <ul>
 * <li> not running.
 * <li> running and bootstrapping: the DHT is trying to bootstrap.
 * <li> running and waiting: the DHT has failed the bootstrap and is waiting 
 * for additional bootstrap hosts.
 * <li> running and bootstrapped.
 * </ul>
 * Nodes are bootstrap in the following order:
 * <ol> 
 * <li>If we have received hosts from the Gnutella network, try them.
 * <li>Else try the persisted routing table (stored contacts from the last session).
 * <li>Else try the SIMPP list.
 * <li>Else start the node fetcher and wait for hosts coming from the network.
 * </ol>
 * <strong>Warning:</strong> The methods in this class are NOT synchronized.
 * <p>
 * The current implementation is specific to the Mojito DHT. 
 */
public abstract class AbstractDHTController implements DHTController {
    
    private static final String PUBLIC_KEY =
        "GCBADOBQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQUAAFAMBADVUPOYXTTF3IOLCXBEDV7ZPCCW2MDZIM2T2JHUA4HLLGRQCING7PUOJOHCC6CNALSXXYXH3HZHKA5TGV4LC2WGYK5UEYY6BYQBKDQ6RV5JE2XPBJPRT5E5SWDGD2PJPOE34ZSSKBYLQCWQEMN46HZGH75DAIDATD3S3FLETRCHRNWWK6JT7TC4DELTHMAMTZPLTCPYDLPX2T5A";
    
    protected final Log LOG = LogFactory.getLog(getClass());
    
    /**
     * The instance of the DHT.
     */
    protected final MojitoDHT dht;

    /**
     * The DHT bootstrapper instance.
     */
    protected final DHTBootstrapper bootstrapper;
    
    /**
     * The random node adder.
     */
    private final RandomNodeAdder dhtNodeAdder = new RandomNodeAdder();
    
    /**
     * The forwarder of nodes to passive leafs.
     */
    private final NodeForwarder nodeForwarder = new NodeForwarder();
    
    /**
     * The DHT event dispatcher.
     */
    private final EventDispatcher<DHTEvent, DHTEventListener> dispatcher;
    
    /**
     * The mode of this DHTController.
     */
    private final DHTMode mode;
    
    /**
     * Get and save the current RouteTable version.
     */
    private final int routeTableVersion;
    
    private final DHTControllerFacade dhtControllerFacade;
    
    public AbstractDHTController(Vendor vendor, Version version,
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher,
            final DHTMode mode, DHTControllerFacade dhtControllerFacade) {

        this.dhtControllerFacade = dhtControllerFacade;
        
        switch(mode) {
            case ACTIVE:
                routeTableVersion = DHTSettings.ACTIVE_DHT_ROUTETABLE_VERSION.getValue();
                break;
            case PASSIVE:
                routeTableVersion = DHTSettings.PASSIVE_DHT_ROUTETABLE_VERSION.getValue();
                break;
            default:
                routeTableVersion = -1;
        }
        
        this.dispatcher = dispatcher;
        this.mode = mode;
        
        this.dht = createMojitoDHT(vendor, version);

        assert (dht != null);
        dht.setMessageDispatcher(dhtControllerFacade.getMessageDispatcherFactory());
        dht.setMACCalculatorRepositoryManager(dhtControllerFacade.getMACCalculatorRespositoryManager());
        dht.setSecurityTokenProvider(dhtControllerFacade.getSecurityTokenProvider());                
        dht.getDHTExecutorService().setThreadFactory(new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                return new ManagedThread(runnable);
            }
        });
        
        dht.setHostFilter(new FilterDelegate());
        
        dht.getDHTValueFactoryManager().addValueFactory(
                AbstractAltLocValue.ALT_LOC, dhtControllerFacade.getAltLocValueFactory());
        
        dht.getDHTValueFactoryManager().addValueFactory(
                AbstractPushProxiesValue.PUSH_PROXIES, dhtControllerFacade.getPushProxyValueFactory());
        
        PublicKey publicKey = SignatureVerifier.readKey(PUBLIC_KEY, "DSA");
        KeyPair keyPair = new KeyPair(publicKey, null);
        dht.setKeyPair(keyPair);
        
        dht.getStorableModelManager().addStorableModel(
                AbstractAltLocValue.ALT_LOC, dhtControllerFacade.getAltLocModel());
        
        this.bootstrapper = dhtControllerFacade.getDHTBootstrapper(this);
        
        // If we're an Ultrapeer we want to notify our firewalled
        // leafs about every new Contact
        if (dhtControllerFacade.isActiveSupernode()) {
            dht.getRouteTable().addRouteTableListener(new RouteTableListener() {
                public void handleRouteTableEvent(RouteTableEvent event) {
                    switch(event.getEventType()) {
                        case ADD_ACTIVE_CONTACT:
                        case ADD_CACHED_CONTACT:
                        case UPDATE_CONTACT:
                            Contact node = event.getContact();
                            if (mode == DHTMode.ACTIVE || !dht.getLocalNodeID().equals(node.getNodeID())) {
                                    nodeForwarder.addContact(node);
                            }
                            break;
                    }
                }
            });
        }

        DHTSettings.DHT_NODE_ID.set(dht.getLocalNodeID().toHexString());
    }

    /**
     * Returns the current RouteTable version.
     */
    protected final int getRouteTableVersion() {
        return routeTableVersion;
    }
    
    /**
     * A factory method to create MojitoDHTs.
     */
    protected abstract MojitoDHT createMojitoDHT(Vendor vendor, Version version);
    
    public DHTMode getDHTMode() {
        return mode;
    }

    public List<IpPort> getActiveDHTNodes(int maxNodes) {
        return Collections.emptyList();
    }

    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
    }
    
    /**
     * Start the Mojito DHT and connects it to the network in either passive 
     * mode, or active mode (if we are a Gnutella leaf node). For the node to 
     * be either passive or active mode, it must meet certain bandwidth settings 
     * and must be able to receive solicited UDP (for example, non-firewalled).     
     * <p>
     * The start preconditions are the following:
     * <p>
     * We are not already connected, AND 
     * <ul>
     * <li>we are not currently running, or
     * <li>we want to force a connection (FORCE_DHT_CONNECT is true) 
     */
    public void start() {
        if (isRunning() || (!DHTSettings.FORCE_DHT_CONNECT.getValue() 
                && !dhtControllerFacade.isConnected())) {
            return;
        }
                
        if(LOG.isDebugEnabled()) {
            LOG.debug("Initializing the DHT");
        }
        
        try {
            InetAddress addr = InetAddress.getByAddress(dhtControllerFacade.getAddress());
            int port = dhtControllerFacade.getPort();
            if (LOG.isDebugEnabled()) {
                LOG.debug("binding dht to: " + new InetSocketAddress(addr, port));
            }
            dht.bind(new InetSocketAddress(addr, port));
            dht.start();
            if (dhtControllerFacade.isActiveSupernode()) 
                nodeForwarder.start();
            
            // Bootstrap only if we're not a passive leaf node
            if (getDHTMode() != DHTMode.PASSIVE_LEAF) {
                bootstrapper.bootstrap();
            }
            
            dispatcher.dispatchEvent(new DHTEvent(this, Type.STARTING));
        } catch (IOException err) {
            LOG.error("IOException", err);
            ErrorService.error(err);
        }
    }
    
    /**
     * Shuts down the DHT. If this is an active node, it sends the updated 
     * capabilities to its ultrapeers and stores the node's route table to a 
     * file (active.mojito). Otherwise, as a passive node, it saves a list 
     * of Most Recently Seen (MRS) nodes to bootstrap for the next session
     * as long as there was at least two <code>Contact</code>s in the route 
     * table.
     * <p>
     * The persisted route table (stored contacts from the last session) is 
     * used as a secondary means to bootstrap, if the node didn't receive any 
     * hosts through the Gnutella network to bootstrap.
     */
    public void stop() {
        LOG.debug("Shutting down DHT Controller");
        
        bootstrapper.stop();
        dhtNodeAdder.stop();
        nodeForwarder.stop();
        dht.close();
        
        dispatcher.dispatchEvent(new DHTEvent(this, Type.STOPPED));
    }
    
    /**
     * If this node is not bootstrapped, passes the given hostAddress
     * to the DHT bootstrapper. 
     * If it is already bootstrapped, this randomly tries to add the node
     * to the DHT routing table.
     * 
     * @param hostAddress the SocketAddress of the DHT host.
     * @param addToDHTNodeAdder true to add to the random node adder if the DHT 
     * is bootstrapped.
     */
    protected void addActiveDHTNode(SocketAddress hostAddress, boolean addToDHTNodeAdder) {
        if(!dht.isBootstrapped()){
            bootstrapper.addBootstrapHost(hostAddress);
        } else if(addToDHTNodeAdder){
            dhtNodeAdder.addDHTNode(hostAddress);
            dhtNodeAdder.start();
        }
    }
    
    public void addActiveDHTNode(SocketAddress hostAddress) {
        addActiveDHTNode(hostAddress, true);
    }
    
    public void addPassiveDHTNode(SocketAddress hostAddress) {
        if (!dht.isBootstrapped()) {
            bootstrapper.addPassiveNode(hostAddress);
        }
    }
    
    public void addContact(Contact node) {
        if (getDHTMode() == DHTMode.PASSIVE_LEAF) {
            getMojitoDHT().getRouteTable().add(node);
        }
    }
    
    /**
     * Returns a list of the Most Recently Seen nodes from the Mojito 
     * routing table.
     * 
     * @param numNodes the number of nodes to return
     * @param excludeLocal true to exclude the local node
     * @return a list of DHT <tt>IpPorts</tt>
     */
    protected List<IpPort> getMRSNodes(int numNodes, boolean excludeLocal) {
        Collection<Contact> nodes = ContactUtils.sort(
                dht.getRouteTable().getActiveContacts(), numNodes + 1); //it will add the local node!
        
        KUID localNode = dht.getLocalNodeID();
        List<IpPort> ipps = new ArrayList<IpPort>();
        for(Contact cn : nodes) {
            if(excludeLocal && cn.getNodeID().equals(localNode)) {
                continue;
            }
            ipps.add(new IpPortRemoteContact(cn));
        }
        return ipps;
    }
    
    public boolean isRunning() {
        return dht.isRunning();
    }
    
    public boolean isBootstrapped() {
        return dht.isBootstrapped();
    }

    public boolean isWaitingForNodes() {
        return bootstrapper.isWaitingForNodes();
    }
    
    public MojitoDHT getMojitoDHT() {
        return dht;
    }
    
    /**
     * Sends the updated <code>CapabilitiesVM</code> to our connections. This 
     * is used when a node has successfully bootstrapped to the network and wants 
     * to notify its Gnutella peers that they can now bootstrap off of him.
     */
    public void sendUpdatedCapabilities() {
        
        LOG.debug("Sending updated capabilities to our connections");
        
        dhtControllerFacade.updateCapabilities();
        dhtControllerFacade.sendUpdatedCapabilities();
        
        if (isRunning())
            dispatcher.dispatchEvent(new DHTEvent(this, Type.CONNECTED));
    }
    
    /**
     * A helper class to easily go back and forth 
     * from the DHT's RemoteContact to Gnutella's IpPort.
     */
    private static class IpPortRemoteContact implements IpPort {
        
        private InetSocketAddress addr;
        
        public IpPortRemoteContact(Contact node) {
            
            if(!(node.getContactAddress() instanceof InetSocketAddress)) {
                throw new IllegalArgumentException("Contact not instance of InetSocketAddress");
            }
            
            addr = (InetSocketAddress) node.getContactAddress();
        }
        
        @Override public String getAddress() {
            return getInetAddress().getHostAddress();
        }

        @Override public InetAddress getInetAddress() {
            return addr.getAddress();
        }

        @Override public int getPort() {
            return addr.getPort();
        }

        @Override public InetSocketAddress getInetSocketAddress() {
            return addr;
        }
    }
    
    /**
     * Used to fight against possible DHT clusters  by periodically sending 
     * a Mojito ping to the last MAX_SIZE DHT nodes seen in the Gnutella 
     * network. It is effectively randomly adding them to the DHT routing table.
     */
    class RandomNodeAdder implements Runnable {
        
        private static final int MAX_SIZE = 30;
        
        private final Set<SocketAddress> dhtNodes;
        
        private ScheduledFuture<?> timerTask;
        
        private boolean isRunning;
        
        public RandomNodeAdder() {
            dhtNodes = new FixedSizeLIFOSet<SocketAddress>(MAX_SIZE, EjectionPolicy.FIFO);
        }
        
        public synchronized void start() {
            if(isRunning) {
                return;
            }
            long delay = DHTSettings.DHT_NODE_ADDER_DELAY.getValue();
            timerTask = dhtControllerFacade.scheduleWithFixedDelay(this, delay, delay, TimeUnit.MILLISECONDS);
            isRunning = true;
        }
        
        synchronized void addDHTNode(SocketAddress address) {
            dhtNodes.add(address);
        }
        
        public void run() {
            
            List<SocketAddress> nodes = null;
            synchronized (this) {
                
                if(!isRunning()) {
                    return;
                }
                
                nodes = new ArrayList<SocketAddress>(dhtNodes);
                dhtNodes.clear();
            }
            
            synchronized(dht) {
                for(SocketAddress addr : nodes) {
                    
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("RandomNodeAdder pinging: "+ addr);
                    }
                    
                    dht.ping(addr);
                }
            }
                
        }
        
        synchronized boolean isRunning() {
            return isRunning;
        }
        
        synchronized void stop() {
            if(timerTask != null) {
                // TODO: should this attempt to cancel the running task, or not?
                timerTask.cancel(true);
            }
            dhtNodes.clear();
            isRunning = false;
        }
    }
    
    private class FilterDelegate implements HostFilter {
        
        public boolean allow(SocketAddress addr) {
            return dhtControllerFacade.allow(addr);
        }
    }
    
    /**
     * Forwards contacts to passive leafs.
     */
    private class NodeForwarder implements Runnable {
        /**
         * Contacts to forward to passive leafs.
         */
        private final Buffer<Contact> contactsToForward = new Buffer<Contact>(10);
        
        /**
         * Future that actually forwards.
         */
        private volatile Future<?> forwarderFuture;
        
        void start() {
            forwarderFuture = dhtControllerFacade.scheduleWithFixedDelay(this, 60, 60, TimeUnit.SECONDS);
        }
        
        synchronized void addContact(Contact contact) {
            contactsToForward.add(contact);
        }
        
        public void run() {
            if (!DHTSettings.ENABLE_PASSIVE_LEAF_DHT_MODE.getValue() || !isRunning()) {
                return;
            }
            Collection<Contact> contacts;
            synchronized(this) {
                if (contactsToForward.isEmpty())
                    return;
                contacts = new ArrayList<Contact>(10);
                for(Contact c : contactsToForward)
                    contacts.add(c);
                // do not erase contacts - can be re-forwarded to new connections
            }

            DHTContactsMessage msg = new DHTContactsMessage(contacts);
            List<RoutedConnection> list = dhtControllerFacade.getInitializedClientConnections();
            for (RoutedConnection mc : list) {
                if (mc.isPushProxyFor()
                        && mc.getConnectionCapabilities().remoteHostIsPassiveLeafNode() > -1) {
                    mc.send(msg);
                }
            }
        }
        
        void stop() {
            Future<?> f = forwarderFuture;
            if (f != null)
                f.cancel(false);
        }
    }
}
