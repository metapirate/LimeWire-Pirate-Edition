package org.limewire.xmpp.client.impl;

import java.util.concurrent.Callable;

import org.jivesoftware.smack.Roster;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.friend.api.FriendConnectionFactoryRegistry;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.Network;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Service;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventUtils;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.JabberSettings;

import com.google.inject.Inject;


@EagerSingleton
public class XMPPConnectionFactoryImpl implements Service, FriendConnectionFactory {

    private static final Log LOG = LogFactory.getLog(XMPPConnectionFactoryImpl.class);

    private final XMPPConnectionImplFactory connectionImplFactory;
    private final JabberSettings jabberSettings;
    private final ListeningExecutorService executorService;

    private final EventBean<FriendConnectionEvent> friendConnectionEventBean;

    @Inject
    public XMPPConnectionFactoryImpl(XMPPConnectionImplFactory connectionImplFactory,
            EventMulticaster<FriendConnectionEvent> connectionBroadcaster,
            EventBean<FriendConnectionEvent> friendConnectionEventBean,
            JabberSettings jabberSettings) {
        this.connectionImplFactory = connectionImplFactory;
        this.friendConnectionEventBean = friendConnectionEventBean;
        this.jabberSettings = jabberSettings;

        connectionBroadcaster.addListener(new ReconnectionManager(this));
        // We'll install our own subscription listeners
        Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.manual);
        executorService = ExecutorsHelper.newSingleThreadExecutor(ExecutorsHelper.daemonThreadFactory("XMPPServiceImpl"));
    }

    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }

    @Override
    public void initialize() {
    }

    @Override
    public void start() {
    }

    /**
     * Logs out all existing connections.
     */
    @Asynchronous
    @Override
    public void stop() {
        XMPPFriendConnectionImpl connection = getActiveConnection();
        if (connection != null) {
            connection.logoutImpl(null);
        }
    }

    @Override
    public String getServiceName() {
        return "XMPP";
    }

    @Override
    public ListeningFuture<FriendConnection> login(final FriendConnectionConfiguration configuration) {
        return executorService.submit(new Callable<FriendConnection>() {
            @Override
            public FriendConnection call() throws Exception {
                return loginImpl(configuration);
            }
        }); 
    }

    @Override
    @Inject
    public void register(FriendConnectionFactoryRegistry registry) {
        registry.register(Network.Type.XMPP, this);
    }

    FriendConnection loginImpl(FriendConnectionConfiguration configuration) throws FriendException {
        return loginImpl(configuration, false);
    }

    FriendConnection loginImpl(FriendConnectionConfiguration configuration, boolean isReconnect) throws FriendException {
        synchronized (this) {
            XMPPFriendConnectionImpl activeConnection = getActiveConnection();
            if(isReconnect) {
                if(activeConnection != null) {
                    return activeConnection;
                }
            } else {
                if(activeConnection != null) {
                    if (activeConnection.getConfiguration().equals(configuration)) {
                        return activeConnection;
                    } else {
                        // logout synchronously
                        activeConnection.logoutImpl(null);
                    }
                }    
            }                
            
            XMPPFriendConnectionImpl connection = connectionImplFactory.createConnection(
                    configuration, executorService);
            try {
                connection.loginImpl();
                //maintain the last set login state available or do not disturb
                connection.setModeImpl(jabberSettings.isDoNotDisturbSet() ? FriendPresence.Mode.dnd : FriendPresence.Mode.available);
                return connection;
            } catch(FriendException e) {
                LOG.debug(e.getMessage(), e);
                throw new FriendException(e);
            }
        }
    }

    XMPPFriendConnectionImpl getActiveConnection() {
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        if (friendConnection instanceof XMPPFriendConnectionImpl && friendConnection.isLoggedIn()) {
            return (XMPPFriendConnectionImpl)friendConnection;
        }
        return null;
    }

    @SuppressWarnings("unused")
    private void setModeImpl(FriendPresence.Mode mode) throws FriendException {
        XMPPFriendConnectionImpl connection = getActiveConnection();
        if (connection != null) {
            connection.setModeImpl(mode);
        }
    }

    @Override
    public ListeningFuture<String> requestLoginUrl(FriendConnectionConfiguration configuration) {
        return null;
    }

    
}
