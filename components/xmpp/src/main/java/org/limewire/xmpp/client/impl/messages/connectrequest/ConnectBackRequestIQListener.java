package org.limewire.xmpp.client.impl.messages.connectrequest;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.ConnectBackRequestFeature;
import org.limewire.friend.api.feature.FeatureInitializer;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectBackRequest;
import org.limewire.net.ConnectBackRequestedEvent;
import org.limewire.xmpp.client.impl.XMPPFriendConnectionImpl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Listens for {@link ConnectBackRequestIQ connect back request iqs} and fires
 * a {@link ConnectBackRequestedEvent}.
 */
public class ConnectBackRequestIQListener implements PacketListener, FeatureTransport<ConnectBackRequest> {

    private static final Log LOG = LogFactory.getLog(ConnectBackRequestIQListener.class);
    
    private final XMPPFriendConnectionImpl connection;
    private final Handler<ConnectBackRequest> connectBackRequestHandler;

    @Inject
    public ConnectBackRequestIQListener(@Assisted XMPPFriendConnectionImpl connection,
                                        FeatureTransport.Handler<ConnectBackRequest> connectBackRequestHandler,
                                    FeatureRegistry featureRegistry) {
        this.connection = connection;
        this.connectBackRequestHandler = connectBackRequestHandler;
        new ConnectBackRequestIQFeatureInitializer().register(featureRegistry);
    }
    
    @Override
    public void processPacket(Packet packet) {
        ConnectBackRequestIQ connectRequest = (ConnectBackRequestIQ)packet;
        LOG.debugf("processing connect request: {0}", connectRequest);
        connectBackRequestHandler.featureReceived(packet.getFrom(), connectRequest.getConnectBackRequest());
    }
    
    @Override
    public void sendFeature(FriendPresence presence, ConnectBackRequest connectBackRequest)
            throws FriendException {
        ConnectBackRequestIQ connectRequest = new ConnectBackRequestIQ(connectBackRequest);
        connectRequest.setTo(presence.getPresenceId());
        connectRequest.setFrom(connection.getLocalJid());
        LOG.debugf("sending request: {0}", connectRequest);
        connection.sendPacket(connectRequest);
    }
    
    public PacketFilter getPacketFilter() {
        return new PacketFilter() {
            @Override
            public boolean accept(Packet packet) {
                return packet instanceof ConnectBackRequestIQ;
            }
        };
    }
    
    private static class ConnectBackRequestIQFeatureInitializer implements FeatureInitializer {
        @Override
        public void register(FeatureRegistry registry) {
            registry.registerPublicInitializer(ConnectBackRequestFeature.ID, this);
        }

        @Override
        public void initializeFeature(FriendPresence friendPresence) {
            friendPresence.addFeature(new ConnectBackRequestFeature());
        }

        @Override
        public void removeFeature(FriendPresence friendPresence) {
            friendPresence.removeFeature(ConnectBackRequestFeature.ID);
        }
    }

}
