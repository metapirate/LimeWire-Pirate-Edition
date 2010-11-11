package org.limewire.xmpp.client.impl.messages.discoinfo;

import java.net.URI;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.FriendPresenceEvent;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.client.impl.XMPPFriendConnectionImpl;


/**
 * sends disco info messages (http://jabber.org/protocol/disco#info) to newly available
 * presences and then calls the appropriate FeatureInitializer for each of the
 * features that come back in the response.
 */
public class DiscoInfoListener implements PacketListener {

    private static final Log LOG = LogFactory.getLog(DiscoInfoListener.class);

    private final FriendConnection connection;
    private final org.jivesoftware.smack.XMPPConnection smackConnection;

    private final XMPPConnectionListener connectionListener = new XMPPConnectionListener();
    private ListenerSupport<FriendConnectionEvent> connectionSupport;
    private ListenerSupport<FriendPresenceEvent> friendPresenceSupport;
    private final FriendPresenceListener friendPresenceListener = new FriendPresenceListener();
    private final PacketFilter packetFilter = new DiscoPacketFilter();

    private final FeatureRegistry featureRegistry;
    
    public DiscoInfoListener(FriendConnection connection,
                             org.jivesoftware.smack.XMPPConnection smackConnection,
                             FeatureRegistry featureRegistry) {
        this.connection = connection;
        this.smackConnection = smackConnection;
        this.featureRegistry = featureRegistry;
    }

    public void addListeners(ListenerSupport<FriendConnectionEvent> connectionSupport,
                             ListenerSupport<FriendPresenceEvent> friendPresenceSupport) {
        this.connectionSupport = connectionSupport;
        this.friendPresenceSupport = friendPresenceSupport;

        connectionSupport.addListener(connectionListener);
        friendPresenceSupport.addListener(friendPresenceListener);
        smackConnection.addPacketListener(this, packetFilter);
    }

    @Override
    public void processPacket(Packet packet) {
        DiscoverInfo discoverInfo = (DiscoverInfo) packet;
        String from = discoverInfo.getFrom();

        if (from == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debugf("null from field: {0}", discoverInfo.getChildElementXML());
        }
            return;
    }

        FriendPresence friendPresence = matchValidPresence(from);
        if (friendPresence == null && !isForThisConnection(from)) {
            LOG.debugf("no presence found for and not for this connection: {0}", from);
            return;
    }

        String featureInitializer = friendPresence != null ? friendPresence.getPresenceId() : from;
        for (URI uri : featureRegistry.getAllFeatureUris()) {
                if (discoverInfo.containsFeature(uri.toASCIIString())) {
                    LOG.debugf("initializing feature {0} for {1}", uri.toASCIIString(), featureInitializer);
                featureRegistry.get(uri).initializeFeature(friendPresence);
            }
        }
    }

    public void cleanup() {
        if (connectionListener != null) {
            connectionSupport.removeListener(connectionListener);
        }
        if (friendPresenceSupport != null) {
            friendPresenceSupport.removeListener(friendPresenceListener);
        }
        smackConnection.removePacketListener(this);
    }

    /**
     * Blockingly discovers features of an xmpp entity.
     *
     * @param entityName name of entity (can be anything, such as a
     *                   presence id, an xmpp server name, etc)
     */
    private void discoverFeatures(String entityName) {
        try {
            ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(smackConnection);

            // check for null due to race condition between whoever is doing feature discovery
            // and smack connection shutting down.  if shut down, no features worth discovering.
            if (serviceDiscoveryManager != null) {
                LOG.debugf("discovering presence: {0}", entityName);
                serviceDiscoveryManager.discoverInfo(entityName);
            } else {
                LOG.debug("no service discovery manager");
            }
        } catch (org.jivesoftware.smack.XMPPException exception) {
            LOG.info(exception.getMessage(), exception);
            if (exception.getXMPPError() != null &&
                    !exception.getXMPPError().getCondition().
                            equals(XMPPError.Condition.feature_not_implemented.toString())) {
            }
        }
    }


    private boolean isForThisConnection(String from) {
        return connection.getConfiguration().getServiceName().equals(from);
    }

    /**
     * @param from address (e.g. loginName@serviceName.com/resourceInfo)
     * @return the intended presence of the announced feature based on
     *         what is in the disco info packet.
     *         <p/>
     *         Returns NULL if there is no presence for the announced feature
     */
    private FriendPresence matchValidPresence(String from) {

        // does the from string match a presence
        Friend friend = connection.getFriend(StringUtils.parseBareAddress(from));

        if (friend != null) {
            FriendPresence presence = friend.getPresences().get(from);
            if (presence != null) {
                return presence;
            }
        }

        // gets here if packet contains a from string indicating
        // a presence we don't know about.  Or there is an unknown problem.
        return null;
    }


    // listen for new presences in order to discover presence features
    private class FriendPresenceListener implements EventListener<FriendPresenceEvent> {
        @BlockingEvent(queueName = "feature discovery")
        @Override
        public void handleEvent(final FriendPresenceEvent event) {
            if (event.getType() == FriendPresenceEvent.Type.ADDED) {
                discoverFeatures(event.getData().getPresenceId());
            }
        }
    }

    // listen for new connections in order to discover server features
    private class XMPPConnectionListener implements EventListener<FriendConnectionEvent> {
        @BlockingEvent(queueName = "feature discovery")
        @Override
        public void handleEvent(FriendConnectionEvent event) {
            if (!(event.getSource() instanceof XMPPFriendConnectionImpl)) {
                return;
            }
            if (event.getType() == FriendConnectionEvent.Type.CONNECTED) {
                discoverFeatures(connection.getConfiguration().getServiceName());
            }
        }
    }

    private static class DiscoPacketFilter implements PacketFilter {
        @Override
        public boolean accept(Packet packet) {
            return packet instanceof DiscoverInfo &&
                    (((DiscoverInfo) packet).getType() == IQ.Type.SET
                            || ((DiscoverInfo) packet).getType() == IQ.Type.RESULT);
        }
    }

}
