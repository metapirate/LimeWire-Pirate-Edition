/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package org.limewire.mojito;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.concurrent.DHTExecutorService;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.concurrent.DHTValueFuture;
import org.limewire.mojito.concurrent.DefaultDHTExecutorService;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueFactoryManager;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.DatabaseCleaner;
import org.limewire.mojito.db.EvictorManager;
import org.limewire.mojito.db.Storable;
import org.limewire.mojito.db.StorableModelManager;
import org.limewire.mojito.db.StorablePublisher;
import org.limewire.mojito.db.impl.DatabaseImpl;
import org.limewire.mojito.exceptions.NotBootstrappedException;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.MessageDispatcherFactory;
import org.limewire.mojito.io.MessageDispatcherFactoryImpl;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherEvent;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherListener;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherEvent.EventType;
import org.limewire.mojito.manager.BootstrapManager;
import org.limewire.mojito.manager.FindNodeManager;
import org.limewire.mojito.manager.FindValueManager;
import org.limewire.mojito.manager.GetValueManager;
import org.limewire.mojito.manager.PingManager;
import org.limewire.mojito.manager.StoreManager;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.messages.MessageFactory;
import org.limewire.mojito.messages.MessageHelper;
import org.limewire.mojito.messages.PingRequest;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.FindNodeResult;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.BucketRefresher;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.routing.RouteTable.PurgeMode;
import org.limewire.mojito.routing.RouteTable.SelectMode;
import org.limewire.mojito.routing.impl.LocalContact;
import org.limewire.mojito.routing.impl.RouteTableImpl;
import org.limewire.mojito.security.SecurityTokenHelper;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.CryptoUtils;
import org.limewire.mojito.util.DHTSizeEstimator;
import org.limewire.mojito.util.HostFilter;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;
import org.limewire.service.ErrorService;


/**
 * The Context is the heart of Mojito where everything comes 
 * together. 
 */
public class Context implements MojitoDHT, RouteTable.ContactPinger {
    
    private static final Log LOG = LogFactory.getLog(Context.class);
    
    /**
     * The name of this Mojito instance
     */
    private final String name;
    
    private final StorablePublisher valuePublisher;
    private final DatabaseCleaner databaseCleaner;
    
    private volatile boolean bucketRefresherDisabled = false;
    private final BucketRefresher bucketRefresher;
    
    private final PingManager pingManager;
    private final FindNodeManager findNodeManager;
    private final FindValueManager findValueManager;
    private final StoreManager storeManager;
    private final BootstrapManager bootstrapManager;
    private final GetValueManager getValueManager;
    
    private volatile KeyPair keyPair;
    
    private volatile Database database;
    private volatile RouteTable routeTable;
    private volatile MessageDispatcher messageDispatcher;
    private volatile MessageHelper messageHelper;
    
    private volatile DHTSizeEstimator estimator;
    
    /** The ExecutorService we're using to schedule tasks */
    private volatile DHTExecutorService executorService;
    
    /** The provider interface to create SecurityTokens */
    private volatile SecurityToken.TokenProvider tokenProvider;
    
    /** Manager of repositories of MAC Calculators */
    private volatile MACCalculatorRepositoryManager MACCalculatorRepositoryManager;
    
    private final SecurityTokenHelper tokenHelper;
    
    private final DHTValueFactoryManager factoryManager = new DHTValueFactoryManager();
    
    private final StorableModelManager publisherManager = new StorableModelManager();
    
    private final EvictorManager evictorManager = new EvictorManager();
    
    private volatile HostFilter hostFilter = null;
    
    /**
     * Constructor to create a new Context
     */
    Context(String name, Vendor vendor, Version version, boolean firewalled) {
        this.name = name;
        
        PublicKey masterKey = null;
        try {
            File file = new File(ContextSettings.MASTER_KEY.get());
            if (file.exists() && file.isFile()) {
                masterKey = CryptoUtils.loadPublicKey(file);
            }
        } catch (InvalidKeyException e) {
            LOG.debug("InvalidKeyException", e);
        } catch (SignatureException e) {
            LOG.debug("SignatureException", e);
        } catch (IOException e) {
            LOG.debug("IOException", e);
        } 
        keyPair = new KeyPair(masterKey, null);
        
        executorService = new DefaultDHTExecutorService(getName());
        
        MACCalculatorRepositoryManager = new MACCalculatorRepositoryManager();
        tokenProvider = new SecurityToken.AddressSecurityTokenProvider(MACCalculatorRepositoryManager);
        
        tokenHelper = new SecurityTokenHelper(this);
        
        setRouteTable(null);
        setDatabase(null, false);
        
        setMessageDispatcher(null);
        
        messageHelper = new MessageHelper(this);
        valuePublisher = new StorablePublisher(this);
        databaseCleaner = new DatabaseCleaner(this);
        
        bucketRefresher = new BucketRefresher(this);
        
        pingManager = new PingManager(this);
        findNodeManager = new FindNodeManager(this);
        findValueManager = new FindValueManager(this);
        storeManager = new StoreManager(this);
        bootstrapManager = new BootstrapManager(this);
        getValueManager = new GetValueManager(this);
        
        getLocalNode().setVendor(vendor);
        getLocalNode().setVersion(version);
        getLocalNode().setFirewalled(firewalled);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getName()
     */
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getVendor()
     */
    public Vendor getVendor() {
        return getLocalNode().getVendor();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getVersion()
     */
    public Version getVersion() {
        return getLocalNode().getVersion();
    }
    
    /**
     * Returns the master public key
     */
    public PublicKey getPublicKey() {
        KeyPair keyPair = this.keyPair;
        if (keyPair != null) {
            return keyPair.getPublic();
        }
        return null;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.MojitoDHT#getKeyPair()
     */
    public KeyPair getKeyPair() {
        return keyPair;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.MojitoDHT#setKeyPair(java.security.KeyPair)
     */
    public void setKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getLocalNode()
     */
    public LocalContact getLocalNode() {
        return (LocalContact)getRouteTable().getLocalNode();
    }
    
    /**
     * Generates a new random Node ID for the local Node, 
     * rebuild the routing table with this new ID and purge 
     * the database (it doesn't make sense to keep the key-values
     * from our old node ID).
     * 
     * WARNING: Meant to be called only by BootstrapManager 
     *          or MojitoFactory!
     */
    public void changeNodeID() {
        
        KUID newID = KUID.createRandomID();
        
        if (LOG.isInfoEnabled()) {
            LOG.info("Changing local Node ID from " + getLocalNodeID() + " to " + newID);
        }
        
        setLocalNodeID(newID);
        purgeDatabase();
    }
    
    /**
     * Rebuilds the routeTable with the given local node ID.
     * This will effectively clear the route table and
     * re-add any previous node in the MRS order.
     * 
     * @param localNodeID the local node's KUID
     */
    private void setLocalNodeID(KUID localNodeID) {
        RouteTable routeTable = getRouteTable();
        synchronized (routeTable) {
            // Change the Node ID
            getLocalNode().setNodeID(localNodeID);
            
            routeTable.purge(PurgeMode.PURGE_CONTACTS, 
                    PurgeMode.MERGE_BUCKETS,
                    PurgeMode.STATE_TO_UNKNOWN);
            
            assert(getLocalNode().equals(routeTable.get(localNodeID)));
        }
        
        getStorableModelManager().handleContactChange(this);
    }
    
    /**
     * Returns true if the given Contact is equal to the
     * local Node.
     */
    public boolean isLocalNode(Contact node) {
        return node.equals(getLocalNode());
    }
    
    /**
     * Returns true if the given KUID and SocketAddress are
     * equal to local Node's KUID and SocketAddress (contact address)
     */
    public boolean isLocalNode(KUID nodeId, SocketAddress addr) {
        return isLocalNodeID(nodeId) && isLocalContactAddress(addr);
    }
    
    /**
     * Returns true if the given KUID is equal to local Node's KUID
     */
    public boolean isLocalNodeID(KUID nodeId) {
        return nodeId != null && nodeId.equals(getLocalNodeID());
    }
    
    /**
     * Returns true if the given SocketAddress is equal to local 
     * Node's SocketAddress
     */
    public boolean isLocalContactAddress(SocketAddress address) {
        return getContactAddress().equals(address);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getLocalNodeID()
     */
    public KUID getLocalNodeID() {
        return getLocalNode().getNodeID();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#setMessageDispatcher(java.lang.Class)
     */
    public synchronized MessageDispatcher setMessageDispatcher(MessageDispatcherFactory messageDispatcherFactory) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch MessageDispatcher while " + getName() + " is running");
        }

        if (messageDispatcherFactory == null) {
            messageDispatcherFactory = new MessageDispatcherFactoryImpl();
        }
        
        messageDispatcher = messageDispatcherFactory.create(this);
        return messageDispatcher;
    }
    
    /**
     * Returns the MessageDispatcher
     */
    public MessageDispatcher getMessageDispatcher() {
        // Not synchronized 'cause only called when Mojito is running and 
        // while Mojito is running you cannot change the MessageDispatcher
        return messageDispatcher;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#setRouteTable(com.limegroup.mojito.routing.RouteTable)
     */
    public synchronized void setRouteTable(RouteTable routeTable) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch RouteTable while " + getName() + " is running");
        }
        
        if (this.routeTable != null && this.routeTable == routeTable) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot set the same instance multiple times");
            }
            //throw new IllegalArgumentException();
            return;
        }
        
        if (routeTable == null) {
            routeTable = new RouteTableImpl();
        }
        
        routeTable.setContactPinger(this);
        routeTable.setNotifier(getDHTExecutorService());

        this.routeTable = routeTable;
        
        if (database != null) {
            purgeDatabase();
        }
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getRouteTable()
     */
    public RouteTable getRouteTable() {
        return routeTable;
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#setDatabase(com.limegroup.mojito.db.Database)
     */
    public synchronized void setDatabase(Database database) {
        setDatabase(database, true);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getDatabase()
     */
    public Database getDatabase() {
        return database;
    }
    
    /**
     * Sets the Database
     * 
     * @param database the Database (can be null to use the default Database implementation)
     * @param remove whether or not to remove non local DHTValues
     */
    private synchronized void setDatabase(Database database, boolean remove) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch Database while " + getName() + " is running");
        }
        
        if (this.database != null && this.database == database) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cannot set the same instance multiple times");
            }
            return;
        }
        
        if (database == null) {
            database = new DatabaseImpl();
        }
        
        this.database = database;
        purgeDatabase();
    }
    
    /**
     * Purge Database makes sure the originator of all local DHTValues 
     * is the LocalContact and that all non local DHTValues get removed
     * from the Database.
     */
    private void purgeDatabase() {
        // We're assuming the Node IDs are totally random so
        // chances are slim to none that we're responsible 
        // for the values again. Even if we are there's no way
        // to test it until we've fully re-bootstrapped in
        // which case the other guys will send us the values
        // anyways as from their perspective we're just a new
        // node.
        database.clear();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#setMessageFactory(com.limegroup.mojito.messages.MessageFactory)
     */
    public synchronized void setMessageFactory(MessageFactory messageFactory) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch MessageFactory while " + getName() + " is running");
        }
        
        messageHelper.setMessageFactory(messageFactory);
    }
    
    /**
     * Returns the current MessageFactory. In most cases you want to use
     * the MessageHelper instead which is a simplified version of the
     * MessageFactory.
     */
    public MessageFactory getMessageFactory() {
        // Not synchronized 'cause only called when Mojito is running and 
        // while Mojito is running you cannot change the MessageHelper
        return messageHelper.getMessageFactory();
    }
    
    /**
     * Sets the MessageHelper
     */
    public synchronized void setMessageHelper(MessageHelper messageHelper) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch MessageHelper while " + getName() + " is running");
        }
        
        this.messageHelper = messageHelper;
    }
    
    /**
     * Returns the current MessageHelper which is a simplified
     * MessageFactory
     */
    public MessageHelper getMessageHelper() {
        // Not synchronized 'cause only called when Mojito is running and 
        // while Mojito is running you cannot change the MessageHelper
        return messageHelper;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#setHostFilter(com.limegroup.mojito.util.HostFilter)
     */
    public synchronized void setHostFilter(HostFilter hostFilter) {
        this.hostFilter = hostFilter;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.MojitoDHT#getHostFilter()
     */
    public HostFilter getHostFilter() {
        return hostFilter;
    }
    
    /**
     * Sets the TokenProvider
     */
    public synchronized void setSecurityTokenProvider(SecurityToken.TokenProvider tokenProvider) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch TokenProvider while " + getName() + " is running");
        }
        this.tokenProvider = tokenProvider;
    }
    
    /**
     * Returns the TokenProvider
     */
    public SecurityToken.TokenProvider getSecurityTokenProvider() {
        return tokenProvider;
    }
    
    
    public synchronized void setMACCalculatorRepositoryManager(MACCalculatorRepositoryManager manager) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch MACManager while " + getName() + " is running");
        }
        this.MACCalculatorRepositoryManager = manager;
    }
    
    public MACCalculatorRepositoryManager getMACCalculatorRepositoryManager() {
        return MACCalculatorRepositoryManager;
    }
    
    /**
     * Returns the SecurityTokenHelper
     */
    public SecurityTokenHelper getSecurityTokenHelper() {
        return tokenHelper;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#setExternalPort(int)
     */
    public void setExternalPort(int port) {
        getLocalNode().setExternalPort(port);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getExternalPort()
     */
    public int getExternalPort() {
        return getLocalNode().getExternalPort();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getContactAddress()
     */
    public SocketAddress getContactAddress() {
        return getLocalNode().getContactAddress();
    }
    
    /**
     * Sets the contact address of the local Node. Effectively
     * we're maybe only using the port number.
     */
    public void setContactAddress(SocketAddress externalAddress) {
        getLocalNode().setContactAddress(externalAddress);
    }
    
    /**
     * Sets the local Node's external address (the address other are 
     * seeing if this Node is behind a NAT router)
     */
    public void setExternalAddress(SocketAddress externalSocketAddress) {
        boolean changed = getLocalNode().setExternalAddress(externalSocketAddress);
        if (changed) {
            getStorableModelManager().handleContactChange(this);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getLocalAddress()
     */
    public SocketAddress getLocalAddress() {
        return getLocalNode().getSourceAddress();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#isFirewalled()
     */
    public boolean isFirewalled() {
        return getLocalNode().isFirewalled();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#setDHTExecutorService(com.limegroup.mojito.concurrent.DHTExecutorService)
     * this method is not really used anywhere and not even tested...
     */
    public void setDHTExecutorService(DHTExecutorService executorService) {
        if (executorService == null) {
            executorService = new DefaultDHTExecutorService(getName());
        }
        
        this.executorService = executorService;
        RouteTable rt = getRouteTable();
        if (rt != null)
            rt.setNotifier(executorService);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getDHTExecutorService()
     */
    public DHTExecutorService getDHTExecutorService() {
        return executorService;
    }
    
    /**
     * Sets whether or not the BucketRefresher should be disabled
     */
    public synchronized void setBucketRefresherDisabled(boolean bucketRefresherDisabled) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot disable BucketRefresher while " + getName() + " is running");
        }
        this.bucketRefresherDisabled = bucketRefresherDisabled;
    }
    
    /**
     * Returns whether or not the BucketRefresher is disabled
     */
    public boolean isBucketRefresherDisabled() {
        return bucketRefresherDisabled;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.MojitoDHT#getDHTValueFactoryManager()
     */
    public DHTValueFactoryManager getDHTValueFactoryManager() {
        return factoryManager;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.MojitoDHT#getStorableModelManager()
     */
    public StorableModelManager getStorableModelManager() {
        return publisherManager;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.MojitoDHT#getEvictorManager()
     */
    public EvictorManager getEvictorManager() {
        return evictorManager;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#isRunning()
     */
    public boolean isRunning() {
        MessageDispatcher messageDispatcher = this.messageDispatcher;
        if (messageDispatcher != null) {
            return messageDispatcher.isRunning();
        }
        return false;
    }

    /**
     * Returns the BootstrapManager
     */
    public BootstrapManager getBootstrapManager() {
        return bootstrapManager;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.MojitoDHT#isBootstrapping()
     */
    public boolean isBootstrapping() {
        return isRunning() && bootstrapManager.isBootstrapping();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#isBootstrapped()
     */
    public boolean isBootstrapped() {
        return isRunning() && bootstrapManager.isBootstrapped();
    }
    
    /**
     * A helper method to set whether or not the Mojito DHT
     * instance is bootstrapped.
     * 
     * Note: This method is primarily for testing.
     */
    public synchronized void setBootstrapped(boolean bootstrapped) {
        bootstrapManager.setBootstrapped(bootstrapped);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#isBound()
     */
    public boolean isBound() {
        MessageDispatcher messageDispatcher = this.messageDispatcher;
        if (messageDispatcher != null) {
            return messageDispatcher.isBound();
        }
        return false;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#bind(int)
     */
    public synchronized void bind(int port) throws IOException {
        bind(new InetSocketAddress(port));
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#bind(java.net.InetAddress, int)
     */
    public synchronized void bind(InetAddress addr, int port) throws IOException {
        bind(new InetSocketAddress(addr, port));
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#bind(java.net.SocketAddress)
     */
    public synchronized void bind(SocketAddress bindAddr) throws IOException {
        if (isBound()) {
            throw new IOException(getName() + " is already bound");
        }
        
        final int port = ((InetSocketAddress)bindAddr).getPort();
        if (port == 0) {
            throw new IOException("Cannot bind Socket to Port " + port);
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Binding " + getName() + " to address: " + bindAddr);
        }
        
        // If we not firewalled and the external port has not 
        // been set yet then set it to the same port as the 
        // local address.
        if (!isFirewalled() && getExternalPort() == 0) {
            setExternalPort(port);
        }
        
        getLocalNode().setSourceAddress(bindAddr);
        getLocalNode().nextInstanceID();
        
        messageDispatcher.bind(bindAddr);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#start()
     */
    public synchronized void start() {
        if (!isBound()) {
            throw new IllegalStateException(getName() + " is not bound");
        }
        
        if (isRunning()) {
            if(LOG.isDebugEnabled()) {
                LOG.debug(getName() + " is already running!");
            }
            return;
        }
        
        // Startup the local Node
        getLocalNode().shutdown(false);
        
        executorService.start();
        
        pingManager.init();
        findNodeManager.init();
        findValueManager.init();
        
        estimator = new DHTSizeEstimator();
        messageDispatcher.start();
        
        if (!isBucketRefresherDisabled()) {
            bucketRefresher.start();
        }
        
        valuePublisher.start();
        databaseCleaner.start();
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.MojitoDHT#stop()
     */
    public synchronized void stop() {
        if (!isRunning()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(getName() + " is not running");
            }
            return;
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Stopping " + getName());
        }
        
        // Stop the Bucket refresher and the value manager
        bucketRefresher.stop();
        valuePublisher.stop();
        databaseCleaner.stop();
        
        // Shutdown the local Node
        Contact localNode = getLocalNode();
        localNode.shutdown(true);
        
        if (isBootstrapped() && !isFirewalled()
                && ContextSettings.SEND_SHUTDOWN_MESSAGE.getValue()) {
            
            // We're nice guys and send shutdown messages to the 2*k-closest
            // Nodes which should help to reduce the overall latency.
            int m = ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.getValue();
            int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
            int count = m*k;
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending shutdown message to " + count + " Nodes");
            }
            
            Collection<Contact> nodes = getRouteTable().select(
                    localNode.getNodeID(), count, SelectMode.ALIVE);
            
            final CountDownLatch latch = new CountDownLatch(nodes.size());
            MessageDispatcherListener listener = new MessageDispatcherListener() {
                public void handleMessageDispatcherEvent(MessageDispatcherEvent evt) {
                    if (evt.getEventType() != EventType.MESSAGE_SENT) {
                        return;
                    }
                    
                    DHTMessage message = evt.getMessage();
                    if (!(message instanceof PingRequest)) {
                        return;
                    }
                    
                    latch.countDown();
                }
            };
            
            try {
                // Register the listener
                messageDispatcher.addMessageDispatcherListener(listener);
                
                // Send the shutdown Messages
                for (Contact node : nodes) {
                    if (!node.equals(localNode)) {
                        // We are not interested in the responses as we're going
                        // to shutdown. Send pings without a response handler.
                        RequestMessage request = getMessageFactory()
                            .createPingRequest(localNode, node.getContactAddress());
                        
                        try {
                            messageDispatcher.send(node, request, null);
                        } catch (IOException err) {
                            LOG.error("IOException", err);
                        }
                    }
                }
                
                // Wait for the messages being sent
                try {
                    if (!latch.await(1000L, TimeUnit.MILLISECONDS)) {
                        LOG.info("Not all shutdown messages were sent");
                    }
                } catch (InterruptedException err) {
                    LOG.error("InterruptedException", err);
                }
                
            } finally {
                // Remove the listener
                messageDispatcher.removeMessageDispatcherListener(listener);
            }
        }
        
        messageDispatcher.stop();
        executorService.stop();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#close()
     */
    public synchronized void close() {
        stop();
        
        messageDispatcher.close();
        bootstrapManager.setBootstrapped(false);
        
        if (estimator != null) {
            estimator.clear();
        }
        
        setExternalPort(0);
    }
    
    /**
     * Returns a Set of active Contacts sorted by most recently
     * seen to least recently seen
     */
    private Set<Contact> getActiveContacts() {
        Set<Contact> nodes = new LinkedHashSet<Contact>();
        Collection<Contact> active = getRouteTable().getActiveContacts();
        active = ContactUtils.sort(active);
        nodes.addAll(active);
        nodes.remove(getLocalNode());
        return nodes;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#ping()
     */
    public DHTFuture<PingResult> findActiveContact() {
        return pingManager.ping(getActiveContacts());
    }
    
    /**
     * Tries to ping a Set of hosts
     */
    public DHTFuture<PingResult> ping(Set<? extends SocketAddress> hosts) {
        return pingManager.pingAddresses(hosts);
    }
    
    /**
     * Pings the DHT node with the given SocketAddress. 
     * 
     * @param address The address of the remote Node
     */
    public DHTFuture<PingResult> ping(SocketAddress address) {
        return pingManager.ping(address);
    }
    
    /** 
     * Pings the given Node 
     */
    public DHTFuture<PingResult> ping(Contact node) {
        return pingManager.ping(node);
    }
    
    public void ping(final Contact node, final DHTFutureAdapter<PingResult> listener) {
        Runnable command = new Runnable() {
            public void run() {
                try {
                    DHTFuture<PingResult> future = ping(node);
                    if (listener != null) {
                        future.addFutureListener(listener);
                    }
                } catch (RejectedExecutionException err) {
                    ErrorService.error(err);
                }
            }
        };
        
        getDHTExecutorService().execute(command);
    }
    
    /** 
     * Sends a special collision test Ping to the given Node 
     */
    public DHTFuture<PingResult> collisionPing(Contact node) {
        return pingManager.collisionPing(node);
    }
    
    /** 
     * Sends a special collision test Ping to the given Node 
     */
    public DHTFuture<PingResult> collisionPing(Set<? extends Contact> nodes) {
        return pingManager.collisionPing(nodes);
    }
    
    /**
     * A helper method that throws a NotBootstrappedException if
     * Mojito is not bootstrapped
     */
    private void throwExceptionIfNotBootstrapped(String operation) throws NotBootstrappedException {
        if (!isBootstrapped()) {
            if (ContextSettings.THROW_EXCEPTION_IF_NOT_BOOTSTRAPPED.getValue()) {
                throw new NotBootstrappedException(getName(), operation);
            } else if (LOG.isInfoEnabled()) {
                LOG.info(NotBootstrappedException.getErrorMessage(getName(), operation));
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.MojitoDHT#get(org.limewire.mojito.db.EntityKey)
     */
    public DHTFuture<FindValueResult> get(EntityKey entityKey) {
        if (entityKey.isLookupKey()) {
            throwExceptionIfNotBootstrapped("get()");
            return findValueManager.lookup(entityKey);
        } else {
            return getValueManager.get(entityKey);
        }
    }
    
    /** 
     * Starts a Node lookup for the given KUID 
     */
    public DHTFuture<FindNodeResult> lookup(KUID lookupId) {
        return findNodeManager.lookup(lookupId);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#bootstrap(com.limegroup.mojito.routing.Contact)
     */
    public DHTFuture<BootstrapResult> bootstrap(Contact node) {
        return bootstrapManager.bootstrap(node);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#bootstrap(com.limegroup.mojito.routing.Contact)
     */
    public DHTFuture<BootstrapResult> bootstrap(SocketAddress dst) {
        return bootstrapManager.bootstrap(Collections.singleton(dst));
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#put(com.limegroup.mojito.KUID, com.limegroup.mojito.db.DHTValue)
     */
    public DHTFuture<StoreResult> put(KUID key, DHTValue value) {
        return put(DHTValueEntity.createFromValue(this, key, value));
    }
    
    /**
     * @param entity The value to store
     * @param immediateStore Whether or not to store the value immediately
     */
    public DHTFuture<StoreResult> put(DHTValueEntity entity) {
        if (!isRunning()) {
            throw new IllegalStateException(getName() + " is not running");
        }
        
        // If we're bootstrapped then store the value immediately
        // or let the DHTValueManager do its work
        if (isBootstrapped()) {
            return store(entity);
            
        // And if we're not bootstrapped then return a fake Future
        // and let the DHTValueManager do its work once we're bootstrapped
        } else {
            String operation = (entity.getValue().size() == 0) ? "remove()" : "put()";
            Exception ex = new NotBootstrappedException(getName(), operation);
            return new DHTValueFuture<StoreResult>(ex);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#remove(com.limegroup.mojito.KUID)
     */
    public DHTFuture<StoreResult> remove(KUID key) {
        // To remove a KeyValue you just store an empty value!
        return put(key, DHTValue.EMPTY_VALUE);
    }
    
    /**
     * Stores the given Storable
     */
    public DHTFuture<StoreResult> store(Storable storable) {
        return store(DHTValueEntity.createFromStorable(this, storable));
    }
    
    /** 
     * Stores the given DHTValue 
     */
    public DHTFuture<StoreResult> store(DHTValueEntity entity) {
        return store(Collections.singleton(entity));
    }
   
    /**
     * Stores a Collection of DHTValue(s). All values must have the same
     * valueId!
     */
    public DHTFuture<StoreResult> store(Collection<? extends DHTValueEntity> values) {
        throwExceptionIfNotBootstrapped("store()");
        return storeManager.store(values);
    }
    
    /**
     * Stores a Collection of DHTValue(s) at the given Node. 
     * All values must have the same valueId!
     */
    public DHTFuture<StoreResult> store(Contact node, SecurityToken securityToken, 
            Collection<? extends DHTValueEntity> values) {
        
        return storeManager.store(node, securityToken, values);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#size()
     */
    public synchronized BigInteger size() {
        if (!isRunning()) {
            return BigInteger.ZERO;
        }
        
        return estimator.getEstimatedSize(getRouteTable());
    }
    
    /**
     * Adds the approximate DHT size as returned by a remote Node.
     * The average of the remote DHT sizes is incorporated into
     * our local computation.
     */
    public void addEstimatedRemoteSize(BigInteger remoteSize) {
    	estimator.addEstimatedRemoteSize(remoteSize);
    }
    
    /**
     * Updates the approximate DHT size based on the given Contacts
     */
    public void updateEstimatedSize(Collection<? extends Contact> nodes) {
        estimator.updateSize(nodes);
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        
        buffer.append("Local Node: ").append(getLocalNode()).append("\n");
        buffer.append("Is running: ").append(isRunning()).append("\n");
        buffer.append("Is bootstrapped/ing: ").append(isBootstrapped()).append("/")
                                            .append(isBootstrapping()).append("\n");
        buffer.append("Database Size (Keys): ").append(getDatabase().getKeyCount()).append("\n");
        buffer.append("Database Size (Values): ").append(getDatabase().getValueCount()).append("\n");
        buffer.append("RouteTable Size: ").append(getRouteTable().size()).append("\n");
        buffer.append("Estimated DHT Size: ").append(size()).append("\n");
        
        return buffer.toString();
    }
}