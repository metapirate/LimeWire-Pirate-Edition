package org.limewire.xmpp.client.impl.features;


import org.jivesoftware.smack.XMPPConnection;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.FriendPresenceEvent;
import org.limewire.friend.api.RosterEvent;
import org.limewire.friend.api.feature.FeatureInitializer;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.impl.feature.NoSaveFeature;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.client.impl.XMPPFriendConnectionImpl;
import org.limewire.xmpp.client.impl.messages.nosave.NoSaveIQ;
import org.limewire.xmpp.client.impl.messages.nosave.NoSaveIQListener;

/**
 * Initializer for {@link NoSaveFeature}.
 */
public class NoSaveFeatureInitializer implements FeatureInitializer {

    private static final Log LOG = LogFactory.getLog(NoSaveFeatureInitializer.class);

    private final XMPPFriendConnectionImpl connection;
    private final XMPPConnection jabberConnection;
    private final ListenerSupport<RosterEvent> rosterSupport;
    private final ListenerSupport<FriendPresenceEvent> friendPresenceSupport;
    private RosterListener rosterListener;
    private NoSaveIQListener noSaveListener;

    // The feature can only be fully initialized (meaning that the NoSaveIQ
    // packet is sent, and the feature added to the xmpp connection)
    // after both of the following preconditions are met:
    //
    // 1. Roster received           (RosterEvent is used to determine this)
    // 2. NoSaveFeature  supported  (We know it is when initializeFeature is called)
    //
    private boolean isRosterReceived;
    private boolean isFeatureSupported;

    public NoSaveFeatureInitializer(XMPPConnection jabberConnection, XMPPFriendConnectionImpl limeConnection,
                                    ListenerSupport<RosterEvent> rosterSupport,
                                    ListenerSupport<FriendPresenceEvent> friendPresenceSupport) {
        this.connection = limeConnection;
        this.jabberConnection = jabberConnection;
        this.rosterSupport = rosterSupport;
        this.friendPresenceSupport = friendPresenceSupport;
        this.rosterListener = new RosterListener();
        this.noSaveListener = null;
    }

    @Override
    public void register(FeatureRegistry registry) {
        rosterSupport.addListener(rosterListener);
        registry.registerPrivateInitializer(NoSaveFeature.ID, this);
    }

    @Override
    public void initializeFeature(FriendPresence notUsedInFeatureInit) {
        setFeatureSupported(true);
        sendNoSaveRequest();
    }

    @Override
    public void removeFeature(FriendPresence friendPresence) {
        friendPresence.removeFeature(NoSaveFeature.ID);
    }

    public void cleanup() {
        rosterSupport.removeListener(rosterListener);

        if (noSaveListener != null) {
            jabberConnection.removePacketListener(noSaveListener);
            noSaveListener.cleanup();
        }
    }

    /**
     * Only initialize the feature if both pre-conditions have been met
     * <pre>
     * 1. Roster has been received
     * 2. The feature {@link NoSaveFeature} is supported
     * </pre>
     */
    private void sendNoSaveRequest() {

        if (canSendNoSaveRequest()) {
            // send message to entity server explicitly requesting nosave state
            // make sure noSaveIQListener is ready for the reply messages
            if (noSaveListener == null) {
                noSaveListener = NoSaveIQListener.createNoSaveIQListener(connection, friendPresenceSupport);
                jabberConnection.addPacketListener(noSaveListener, noSaveListener.getPacketFilter());
            }
            NoSaveIQ requestNoSaveSettingsPacket = NoSaveIQ.getNoSaveStatesMessage();

            try {
                connection.sendPacket(requestNoSaveSettingsPacket);
            } catch (FriendException e) {
                LOG.warn("couldn't request google:nosave settings", e);
            }
        }
    }

    private synchronized void setFeatureSupported(boolean isFeatureSupported) {
        this.isFeatureSupported = isFeatureSupported;
    }

    private synchronized void setRosterReceived(boolean isRosterReceived) {
        this.isRosterReceived = isRosterReceived;
    }

    private synchronized boolean canSendNoSaveRequest() {
        return isFeatureSupported && isRosterReceived;
    }

    /** Every time roster changes, (re)send nosave request packet.
     *  Event handler code is annotated @BlockingEvent because we are
     *  potentially over the network.
     **/
    private class RosterListener implements EventListener<RosterEvent> {
        @BlockingEvent
        @Override
        public void handleEvent(RosterEvent event) {
            setRosterReceived(true);
            sendNoSaveRequest();
        }
    }
}
