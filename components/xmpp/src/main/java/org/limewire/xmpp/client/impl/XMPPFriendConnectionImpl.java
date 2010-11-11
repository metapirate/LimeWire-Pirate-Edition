package org.limewire.xmpp.client.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.ChatStateManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.FriendPresenceEvent;
import org.limewire.friend.api.FriendRequestEvent;
import org.limewire.friend.api.RosterEvent;
import org.limewire.friend.api.feature.AddressFeature;
import org.limewire.friend.api.feature.AuthTokenFeature;
import org.limewire.friend.api.feature.ConnectBackRequestFeature;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.api.feature.FileOfferFeature;
import org.limewire.friend.api.feature.LibraryChangedNotifierFeature;
import org.limewire.friend.impl.feature.LimewireFeatureInitializer;
import org.limewire.friend.impl.feature.NoSaveFeature;
import org.limewire.friend.impl.util.PresenceUtils;
import org.limewire.listener.AsynchronousEventBroadcaster;
import org.limewire.listener.AsynchronousEventMulticaster;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventRebroadcaster;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.activity.XmppActivityEvent;
import org.limewire.xmpp.api.client.JabberSettings;
import org.limewire.xmpp.client.impl.features.NoSaveFeatureInitializer;
import org.limewire.xmpp.client.impl.messages.address.AddressIQListener;
import org.limewire.xmpp.client.impl.messages.address.AddressIQListenerFactory;
import org.limewire.xmpp.client.impl.messages.address.AddressIQProvider;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQListener;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQListenerFactory;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQProvider;
import org.limewire.xmpp.client.impl.messages.connectrequest.ConnectBackRequestIQ;
import org.limewire.xmpp.client.impl.messages.connectrequest.ConnectBackRequestIQListener;
import org.limewire.xmpp.client.impl.messages.connectrequest.ConnectBackRequestIQListenerFactory;
import org.limewire.xmpp.client.impl.messages.connectrequest.ConnectBackRequestIQProvider;
import org.limewire.xmpp.client.impl.messages.discoinfo.DiscoInfoListener;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQListener;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQListenerFactory;
import org.limewire.xmpp.client.impl.messages.library.LibraryChangedIQ;
import org.limewire.xmpp.client.impl.messages.library.LibraryChangedIQListener;
import org.limewire.xmpp.client.impl.messages.library.LibraryChangedIQListenerFactory;
import org.limewire.xmpp.client.impl.messages.nosave.NoSaveIQ;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Implements a {@link FriendConnection} using XMPP.
 * <p>
 * It wraps a {@link XMPPConnection} and keeps track of all the listeners
 * created around that connection and the list of users that is online.
 */
public class XMPPFriendConnectionImpl implements FriendConnection {

    private static final Log LOG = LogFactory.getLog(XMPPFriendConnectionImpl.class);

    private final FriendConnectionConfiguration configuration;
    private final EventBroadcaster<FriendRequestEvent> friendRequestBroadcaster;
    private final AsynchronousEventMulticaster<FriendConnectionEvent> connectionMulticaster;
    private final AddressFactory addressFactory;
    private final EventMulticaster<FeatureEvent> featureSupport;
    private final ListeningExecutorService executorService;
    private final List<ConnectionConfigurationFactory> connectionConfigurationFactories;
    private final AddressIQListenerFactory addressIQListenerFactory;
    private final AuthTokenIQListenerFactory authTokenIQListenerFactory;
    private final LibraryChangedIQListenerFactory libraryChangedIQListenerFactory;
    private volatile AddressIQListener addressIQListener;
    private volatile AuthTokenIQListener authTokenIQListener;
    private volatile LibraryChangedIQListener libraryChangedIQListener;
    private volatile ConnectBackRequestIQListener connectRequestIQListener;
    private volatile FileTransferIQListener fileTransferIQListener;

    private final EventListenerList<RosterEvent> rosterListeners;
    private final Map<String, XMPPFriendImpl> friends;
    private final SmackConnectionListener smackConnectionListener;
    private final AtomicBoolean loggedIn = new AtomicBoolean(false);
    private final AtomicBoolean loggingIn = new AtomicBoolean(false);

    private volatile org.jivesoftware.smack.XMPPConnection connection;
    private volatile DiscoInfoListener discoInfoListener;

    private final ConnectBackRequestIQListenerFactory connectBackRequestIQListenerFactory;
    private final FileTransferIQListenerFactory fileTransferIQListenerFactory;

    private final ListenerSupport<FriendPresenceEvent> friendPresenceSupport;

    private final FeatureRegistry featureRegistry;
    private final IdleStatusMonitorFactory idleStatusMonitorFactory;
    private IdleStatusMonitor idleStatusMonitor;
    
    private volatile NoSaveFeatureInitializer noSaveFeatureInitializer;
    private final JabberSettings jabberSettings;
    private final ListenerSupport<XmppActivityEvent> xmppActivitySupport;
    private EventListener<XmppActivityEvent> xmppActivityListener;

    @Inject
    public XMPPFriendConnectionImpl(@Assisted FriendConnectionConfiguration configuration,
                                    @Assisted ListeningExecutorService executorService,
                                    AsynchronousEventBroadcaster<RosterEvent> rosterBroadcaster,
                                    EventBroadcaster<FriendRequestEvent> friendRequestBroadcaster,
                                    AsynchronousEventMulticaster<FriendConnectionEvent> connectionMulticaster,
                                    AddressFactory addressFactory,
                                    EventMulticaster<FeatureEvent> featureSupport,
                                    List<ConnectionConfigurationFactory> connectionConfigurationFactories,
                                    AddressIQListenerFactory addressIQListenerFactory,
                                    AuthTokenIQListenerFactory authTokenIQListenerFactory,
                                    ConnectBackRequestIQListenerFactory connectBackRequestIQListenerFactory,
                                    LibraryChangedIQListenerFactory libraryChangedIQListenerFactory,
                                    FileTransferIQListenerFactory fileTransferIQListenerFactory,
                                    ListenerSupport<FriendPresenceEvent> friendPresenceSupport,
                                    FeatureRegistry featureRegistry,
                                    IdleStatusMonitorFactory idleStatusMonitorFactory,
                                    JabberSettings jabberSettings,
                                    ListenerSupport<XmppActivityEvent> xmppActivitySupport) {
        this.configuration = configuration;
        this.friendRequestBroadcaster = friendRequestBroadcaster;
        this.connectionMulticaster = connectionMulticaster;
        this.addressFactory = addressFactory;
        this.featureSupport = featureSupport;
        this.executorService = executorService;
        this.connectionConfigurationFactories = connectionConfigurationFactories;
        this.addressIQListenerFactory = addressIQListenerFactory;
        this.authTokenIQListenerFactory = authTokenIQListenerFactory;
        this.libraryChangedIQListenerFactory = libraryChangedIQListenerFactory;
        this.connectBackRequestIQListenerFactory = connectBackRequestIQListenerFactory;
        this.fileTransferIQListenerFactory = fileTransferIQListenerFactory;
        this.friendPresenceSupport = friendPresenceSupport;
        this.featureRegistry = featureRegistry;
        this.idleStatusMonitorFactory = idleStatusMonitorFactory;
        this.jabberSettings = jabberSettings;
        this.xmppActivitySupport = xmppActivitySupport;
        rosterListeners = new EventListenerList<RosterEvent>();
        // FIXME: this is only used by tests
        if(configuration.getRosterListener() != null) {
            rosterListeners.addListener(configuration.getRosterListener());
        }
        rosterListeners.addListener(new EventRebroadcaster<RosterEvent>(rosterBroadcaster));
        friends = new TreeMap<String, XMPPFriendImpl>(String.CASE_INSENSITIVE_ORDER);
        
        smackConnectionListener = new SmackConnectionListener();
    }
    
    @Override
    public String toString() {
        return org.limewire.util.StringUtils.toString(this, configuration, connection);
    }

    @Override
    public boolean supportsMode() {
        return true;
    }

    public ListeningFuture<Void> setMode(final FriendPresence.Mode mode) {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                setModeImpl(mode);
                return null;
            }
        });   
    }    

    void setModeImpl(FriendPresence.Mode mode) throws FriendException {
        synchronized (this) {
            try {
                checkLoggedIn();
                connection.sendPacket(getPresenceForMode(mode));
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new FriendException(e);
            } 
        }
    }

    private Packet getPresenceForMode(FriendPresence.Mode mode) {
        org.jivesoftware.smack.packet.Presence presence = new org.jivesoftware.smack.packet.Presence(
                org.jivesoftware.smack.packet.Presence.Type.available);
        presence.setMode(org.jivesoftware.smack.packet.Presence.Mode.valueOf(mode.name()));
        if (jabberSettings.advertiseLimeWireStatus()) {
            presence.setStatus("on LimeWire");
        }
        return presence;
    }

    public FriendConnectionConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public ListeningFuture<Void> login() {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                loginImpl();
                return null;
            }
        });   
    }

    void loginImpl() throws FriendException {
        synchronized (this) {
            try {
                loggingIn.set(true);
                connectionMulticaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECTING));        
                org.jivesoftware.smack.XMPPConnection.addConnectionCreationListener(smackConnectionListener);
                org.jivesoftware.smack.XMPPConnection.DEBUG_ENABLED = configuration.isDebugEnabled();
                connect();
                LOG.infof("connected.");
                LOG.infof("logging in {0} with resource: {1} ...", configuration.getUserInputLocalID(), configuration.getResource());
                connection.login(configuration.getUserInputLocalID(), configuration.getPassword(), configuration.getResource());
                LOG.infof("logged in.");
                loggedIn.set(true);
                loggingIn.set(false);
                connectionMulticaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECTED));
            } catch (org.jivesoftware.smack.XMPPException e) {
                handleLoginError(e);
                throw new FriendException(e);
            } catch (RuntimeException e) {
                handleLoginError(e);
                throw e;
            } 
        }
    }
    
    /**
     * Unwind upon login error - broadcast login failed, remove conn creation
     * listener from smack, set conn to null, disconnect if need be, etc
     *
     * @param e Exception which occurred during login
     */
    private synchronized void handleLoginError(Exception e) {
        loggingIn.set(false);
        connectionMulticaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECT_FAILED, e));
        if (connection != null && connection.isConnected()) {
            connection.disconnect();
        }
        org.jivesoftware.smack.XMPPConnection.removeConnectionCreationListener(smackConnectionListener);
        connection = null;
    }
    
    private void connect() throws org.jivesoftware.smack.XMPPException {
        for(ConnectionConfigurationFactory factory : connectionConfigurationFactories) {
            try {
                connectUsingFactory(factory);
                return;
            } catch (FriendException e) {
                LOG.debug(e.getMessage(), e);
            }
        }
    }

    private void connectUsingFactory(ConnectionConfigurationFactory factory) throws FriendException {
        ConnectionConfigurationFactory.RequestContext requestContext = new ConnectionConfigurationFactory.RequestContext();
        while(factory.hasMore(configuration, requestContext)) {
            ConnectionConfiguration connectionConfig = factory.getConnectionConfiguration(configuration, requestContext);
            connection = new XMPPConnection(connectionConfig);
            connection.addRosterListener(new RosterListenerImpl());
            LOG.infof("connecting to {0} at {1}:{2} ...", connectionConfig.getServiceName(), connectionConfig.getHost(), connectionConfig.getPort());
            try {
                connection.connect();
                return;
            } catch (org.jivesoftware.smack.XMPPException e) {
                LOG.debug(e.getMessage(), e);
                requestContext.incrementRequests();
            }            
        }
        throw new FriendException("couldn't connect using " + factory);
    }

    @Override
    public boolean isLoggingIn() {
        return loggingIn.get();
    }

    @Override
    public ListeningFuture<Void> logout() {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logoutImpl(null);
                return null;
            }
        }); 
    }

    /**
     * 
     * @param error null if connection is closed by user
     */
    void logoutImpl(Exception error) {
        synchronized (this) {
            if(isLoggedIn()) {
                loggedIn.set(false);
                LOG.infof("disconnecting from {0} at {1}:{2} ...", connection.getServiceName(), connection.getHost(), connection.getPort());
                connection.disconnect();
                synchronized (friends) {
                    friends.clear();
                }
                XMPPConnection.removeConnectionCreationListener(smackConnectionListener);
                connection = null;
                LOG.info("disconnected.");
                connectionMulticaster.broadcast(new FriendConnectionEvent(XMPPFriendConnectionImpl.this, FriendConnectionEvent.Type.DISCONNECTED, error));
                ChatStateManager.remove(connection);
                if(discoInfoListener != null) {
                    discoInfoListener.cleanup();
                }
                if (noSaveFeatureInitializer != null) {
                    noSaveFeatureInitializer.cleanup();
                }
                if (idleStatusMonitor != null) {
                    idleStatusMonitor.stop();
                }
                if (xmppActivityListener != null) {
                    xmppActivitySupport.removeListener(xmppActivityListener);
                }
                featureRegistry.deregisterInitializer(NoSaveFeature.ID);
            } 
        }
    }
    
    public boolean isLoggedIn() {
        return loggedIn.get();
    }
    
    private void checkLoggedIn() throws FriendException {
        synchronized (this) {
            if(!isLoggedIn()) {
                throw new FriendException("not connected");
            }
        }
    }

    private class RosterListenerImpl implements org.jivesoftware.smack.RosterListener {

        public void entriesAdded(Collection<String> addedIds) {
            try {
                synchronized (XMPPFriendConnectionImpl.this) {
                    checkLoggedIn();
                    synchronized (friends) {
                        Roster roster = connection.getRoster();
                        if(roster != null) {
                            Map<String, XMPPFriendImpl> newFriends = new HashMap<String, XMPPFriendImpl>();
                            for(String id : addedIds) {
                                RosterEntry rosterEntry = roster.getEntry(id);
                                XMPPFriendImpl friend = new XMPPFriendImpl(id, rosterEntry, configuration, connection, featureRegistry);
                                LOG.debugf("user {0} added", friend);
                                newFriends.put(id, friend);
                            }
                            friends.putAll(newFriends);
                            rosterListeners.broadcast(new RosterEvent(new ArrayList<Friend>(newFriends.values()), RosterEvent.Type.FRIENDS_ADDED));
                        }
                    }
                }
            } catch (org.jivesoftware.smack.XMPPException e) {
                LOG.debugf(e, "error getting roster");    
            } catch (FriendException e) {
                LOG.debugf(e, "error getting roster");    
            }
        }

        public void entriesUpdated(Collection<String> updatedIds) {
            try {
                synchronized (XMPPFriendConnectionImpl.this) {
                    checkLoggedIn();
                    synchronized (friends) {                 
                        Roster roster = connection.getRoster();
                        if(roster != null) {
                            List<Friend> updatedFriends = new ArrayList<Friend>();
                            for(String id : updatedIds) {
                                RosterEntry rosterEntry = roster.getEntry(id);
                                XMPPFriendImpl friend = friends.get(id);
                                if(friend == null) {
                                    // should never happen ?
                                    friend = new XMPPFriendImpl(id, rosterEntry, configuration, connection, featureRegistry);
                                    friends.put(id, friend);
                                } else {
                                    friend.setRosterEntry(rosterEntry);
                                }
                                updatedFriends.add(friend);
                                LOG.debugf("user {0} updated", friend);
                            }
                            rosterListeners.broadcast(new RosterEvent(updatedFriends, RosterEvent.Type.FRIENDS_UPDATED));
                        }
                    }
                }
            } catch (org.jivesoftware.smack.XMPPException e) {
                LOG.debugf(e, "error getting roster");    
            } catch (FriendException e) {
                LOG.debugf(e, "error getting roster");    
            }
        }

        public void entriesDeleted(Collection<String> removedIds) {
            synchronized (friends) {
                List<Friend> deletedFriends = new ArrayList<Friend>();
                for(String id : removedIds) {
                    XMPPFriendImpl friend = friends.remove(id);
                    if(friend != null) {
                        deletedFriends.add(friend);
                        LOG.debugf("user {0} removed", friend);
                    }
                }
                rosterListeners.broadcast(new RosterEvent(deletedFriends, RosterEvent.Type.FRIENDS_DELETED));
            }
        }

        @Override
        public void presenceChanged(final org.jivesoftware.smack.packet.Presence presence) {
            String localJID;
            try {
                localJID = getLocalJid();
            } catch (FriendException e) {
                localJID = null;
            }
            if(!presence.getFrom().equals(localJID)) {
                XMPPFriendImpl friend = getFriend(presence);
                if(friend != null) {
                    LOG.debugf("presence from {0} changed to {1}", presence.getFrom(), presence.getType());
                    // synchronize to avoid updates or presences from being lost
                    // better to replace that with a lock object private to this
                    // connection class
                    synchronized (friend) {
                        if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.available)) {
                            if(!friend.getPresences().containsKey(presence.getFrom())) {
                                addNewPresence(friend, presence);
                            } else {
                                updatePresence(friend, presence);
                            }
                        } else if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.unavailable)) {
                            PresenceImpl p = (PresenceImpl)friend.getPresence(presence.getFrom());
                            if(p != null) {
                                p.update(presence);
                                friend.removePresense(p);
                            }
                        }
                    }
                } else {
                    LOG.debugf("no friend for presence {0}", presence.getFrom());    
                }
            }
        }

        private XMPPFriendImpl getFriend(org.jivesoftware.smack.packet.Presence presence) {
            XMPPFriendImpl friend;
            synchronized (friends) {
                friend = friends.get(PresenceUtils.parseBareAddress(presence.getFrom()));
            }
            return friend;
        }

        private void addNewPresence(final XMPPFriendImpl friend, final org.jivesoftware.smack.packet.Presence presence) {
            final PresenceImpl presenceImpl = new PresenceImpl(presence, friend, featureSupport);
            presenceImpl.addTransport(AddressFeature.class, addressIQListener);
            presenceImpl.addTransport(AuthTokenFeature.class, authTokenIQListener);
            presenceImpl.addTransport(ConnectBackRequestFeature.class, connectRequestIQListener);
            presenceImpl.addTransport(LibraryChangedNotifierFeature.class, libraryChangedIQListener);
            presenceImpl.addTransport(FileOfferFeature.class, fileTransferIQListener);
            friend.addPresense(presenceImpl);
        }

        private void updatePresence(XMPPFriendImpl friend, org.jivesoftware.smack.packet.Presence presence) {
            PresenceImpl currentPresence = (PresenceImpl)friend.getPresences().get(presence.getFrom());
            currentPresence.update(presence);
            friend.updatePresence(currentPresence);
        }
    }

    @Override
    public boolean supportsAddRemoveFriend() {
        return true;
    }

    @Override
    public ListeningFuture<Void> addNewFriend(final String id, final String name) {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                addFriendImpl(id, name);
                return null;
            }
        }); 
    }

    void addFriendImpl(String id, String name) throws FriendException {
        synchronized (this) {
            try {
                checkLoggedIn();
                Roster roster = connection.getRoster();
                if(roster != null) {
                    // TODO smack enhancement
                    // TODO to support notifications when
                    // TODO the Roster is created
                    roster.createEntry(id, name, null);
                }
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new FriendException(e);
            } 
        }
    }

    @Override
    public ListeningFuture<Void> removeFriend(final String id) {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                removeFriendImpl(id);
                return null;
            }
        }); 
    }

    private void removeFriendImpl(String id) throws FriendException {
        synchronized (this) {
            try {
                checkLoggedIn();
                Roster roster = connection.getRoster();
                if(roster != null) {
                    // TODO smack enhancement
                    // TODO to support notifications when
                    // TODO the Roster is created
    
                    RosterEntry entry = roster.getEntry(id);
                    if(entry!= null) {
                        roster.removeEntry(entry);    
                    }
                }
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new FriendException(e);
            }
        }
    }

    @Override
    public XMPPFriendImpl getFriend(String id) {
        id = PresenceUtils.parseBareAddress(id);
        synchronized (friends) { 
            return friends.get(id);
        }
    }

    @Override
    public Collection<Friend> getFriends() {
        synchronized (friends) { 
            return new ArrayList<Friend>(friends.values());
        }
    }

    public void sendPacket(Packet packet) throws FriendException {
        synchronized (this) {
            try {
                checkLoggedIn();
                connection.sendPacket(packet);
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new FriendException(e);
            } 
        }
    }

    public String getLocalJid() throws FriendException {
        synchronized (this) {
            checkLoggedIn();
            return connection.getUser();
        }
    }
    
    /**
     * Sets the connection mode to idle (extended away) after receiving activity
     * events triggered by periods of inactivity.
     */
    private class XmppActivityEventListener implements EventListener<XmppActivityEvent> {

        @Override
        public void handleEvent(XmppActivityEvent event) {
            switch (event.getSource()) {
                case Idle:
                    try {
                        setModeImpl(FriendPresence.Mode.xa);
                    } catch (FriendException e) {
                        LOG.debugf(e, "couldn't set mode based on {0}", event);
                    }
                    break;
                case Active:
                    try {
                        setModeImpl(jabberSettings.isDoNotDisturbSet() ? 
                                FriendPresence.Mode.dnd : FriendPresence.Mode.available);
                    } catch (FriendException e) {
                        LOG.debugf(e, "couldn't set mode based on {0}", event);
                    }
            }
        }
    }
    
    private class SmackConnectionListener implements ConnectionListener, ConnectionCreationListener {
        @Override
        public void connectionCreated(XMPPConnection connection) {
            if(XMPPFriendConnectionImpl.this.connection != connection) {
                return;
            }

            if(LOG.isDebugEnabled()) {
                LOG.debug("connection created for "+ connection.toString());
            }
            connection.addConnectionListener(this);

            synchronized (ProviderManager.getInstance()) {
                if(ProviderManager.getInstance().getIQProvider("address", "jabber:iq:lw-address") == null) {
                    ProviderManager.getInstance().addIQProvider("address", "jabber:iq:lw-address", new AddressIQProvider(addressFactory));
                }
                if(ProviderManager.getInstance().getIQProvider("file-transfer", "jabber:iq:lw-file-transfer") == null) {
                    ProviderManager.getInstance().addIQProvider("file-transfer", "jabber:iq:lw-file-transfer", FileTransferIQ.getIQProvider());
                }
                if(ProviderManager.getInstance().getIQProvider("auth-token", "jabber:iq:lw-auth-token") == null) {
                    ProviderManager.getInstance().addIQProvider("auth-token", "jabber:iq:lw-auth-token", new AuthTokenIQProvider());
                }
                if(ProviderManager.getInstance().getIQProvider("library-changed", "jabber:iq:lw-lib-change") == null) {
                    ProviderManager.getInstance().addIQProvider("library-changed", "jabber:iq:lw-lib-change", LibraryChangedIQ.getIQProvider());
                }
                if (ProviderManager.getInstance().getIQProvider(ConnectBackRequestIQ.ELEMENT_NAME, ConnectBackRequestIQ.NAME_SPACE) == null) {
                    ProviderManager.getInstance().addIQProvider(ConnectBackRequestIQ.ELEMENT_NAME, ConnectBackRequestIQ.NAME_SPACE, new ConnectBackRequestIQProvider());
                }
                if (ProviderManager.getInstance().getIQProvider(NoSaveIQ.ELEMENT_NAME, NoSaveIQ.NAME_SPACE) == null) {
                    ProviderManager.getInstance().addIQProvider(NoSaveIQ.ELEMENT_NAME, NoSaveIQ.NAME_SPACE, NoSaveIQ.getIQProvider());    
                }
            }

            ChatStateManager.getInstance(connection);

            discoInfoListener = new DiscoInfoListener(XMPPFriendConnectionImpl.this, connection, featureRegistry);
            discoInfoListener.addListeners(connectionMulticaster, friendPresenceSupport);
            
            addressIQListener = addressIQListenerFactory.create(XMPPFriendConnectionImpl.this, addressFactory);
            connection.addPacketListener(addressIQListener, addressIQListener.getPacketFilter());

            fileTransferIQListener = fileTransferIQListenerFactory.create(XMPPFriendConnectionImpl.this);
            connection.addPacketListener(fileTransferIQListener, fileTransferIQListener.getPacketFilter());

            authTokenIQListener = authTokenIQListenerFactory.create(XMPPFriendConnectionImpl.this);
            connection.addPacketListener(authTokenIQListener, authTokenIQListener.getPacketFilter());

            libraryChangedIQListener = libraryChangedIQListenerFactory.create(XMPPFriendConnectionImpl.this);
            connection.addPacketListener(libraryChangedIQListener, libraryChangedIQListener.getPacketFilter());

            connectRequestIQListener = connectBackRequestIQListenerFactory.create(XMPPFriendConnectionImpl.this);
            connection.addPacketListener(connectRequestIQListener, connectRequestIQListener.getPacketFilter());
            
            new LimewireFeatureInitializer().register(featureRegistry);
            
            noSaveFeatureInitializer = new NoSaveFeatureInitializer(connection, XMPPFriendConnectionImpl.this, 
                    rosterListeners, friendPresenceSupport);
            noSaveFeatureInitializer.register(featureRegistry);
            
            SubscriptionListener sub = new SubscriptionListener(connection,
                                                friendRequestBroadcaster);
            connection.addPacketListener(sub, sub);
            
            for(URI feature : featureRegistry.getPublicFeatureUris()) {
                ServiceDiscoveryManager.getInstanceFor(connection).addFeature(feature.toASCIIString());
            }
            if (xmppActivityListener == null) {
                xmppActivityListener = new XmppActivityEventListener();
            }
            xmppActivitySupport.addListener(xmppActivityListener);
            
            if (idleStatusMonitor == null) {
                idleStatusMonitor = idleStatusMonitorFactory.create();    
            }
            idleStatusMonitor.start();
        }

        @Override
        public void connectionClosed() {
            LOG.debug("smack connection closed");
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            LOG.debug("smack connection closed with error", e);
            logoutImpl(e);
        }

        @Override
        public void reconnectingIn(int seconds) {
        }

        @Override
        public void reconnectionFailed(Exception e) {
        }

        @Override
        public void reconnectionSuccessful() {
        }
     }
}