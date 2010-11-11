package org.limewire.xmpp.client.impl.messages.nosave;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.FriendPresenceEvent;
import org.limewire.friend.impl.feature.NoSave;
import org.limewire.friend.impl.feature.NoSaveFeature;
import org.limewire.friend.impl.feature.NoSaveStatus;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.xmpp.client.impl.XMPPFriendConnectionImpl;

/**
 * This class is responsible for:
 *<pre>
 *
 * 1. Listening for google:nosave result packets.
 *    This class is a PacketListener, and is added to a jabber xmpp connection.
 *
 * 2. Keeping all presences in the currently logged in connection up to date
 *    with their nosave statuses by adding the nosave feature to the presence.
 *    This class tracks the nosave status for every friend.
 *
 * Sample google:nosave packet:
 *
 *  <iq id="58jE1-6" to="limebuddytest1@gmail.com/vN/X2IGD1QFDF73EA8" type="result">
 *      <query xmlns="google:nosave">
 *          <item xmlns="google:nosave" jid="limebuddytest2@gmail.com" value="disabled"/>
 *          <item xmlns="google:nosave" jid="limebuddytest3@gmail.com" value="enabled"/>
 *          ...
 *
 * </pre>
 */
public class NoSaveIQListener implements PacketListener {

    private final XMPPFriendConnectionImpl connection;
    private Map<String, NoSave> noSaveMap = new HashMap<String, NoSave>();

    private ListenerSupport<FriendPresenceEvent> friendPresenceSupport;
    private EventListener<FriendPresenceEvent> friendPresenceListener;

    private NoSaveIQListener(XMPPFriendConnectionImpl connection) {
        this.connection = connection;
    }

    /**
     * Factory method to create NoSaveIQListener.  Purpose is separate creation via
     * constructor from adding a FriendPresenceEvent listener
     *
     * @param connection XMPPConnectionImpl
     * @param friendPresenceSupport FriendPresenceEvent listener manager
     * @return NoSaveIQListener object
     */
    public static NoSaveIQListener createNoSaveIQListener(XMPPFriendConnectionImpl connection,
                                                          ListenerSupport<FriendPresenceEvent> friendPresenceSupport) {
        NoSaveIQListener noSaveIQListener = new NoSaveIQListener(connection);
        noSaveIQListener.register(friendPresenceSupport);
        return noSaveIQListener;
    }

    /**
     * 1. Update this object's nosave state (nosave status for all friends)
     * 2. For each friend's presence, add nosave feature to presence if necessary
     *    {@link #shouldAddFeature}
     *
     * @param packet NoSaveIQ packet
     */
    @Override
    public void processPacket(Packet packet) {
        NoSaveIQ noSave = (NoSaveIQ)packet;

        // go thru the nosave IQ packet, and go thru each item and update the nosaveStatus
        Map<String, NoSave> friends = noSave.getNoSaveUsers();
        updateNoSaveStatusMap(friends);

        for (String friendName : friends.keySet()) {
            Friend noSaveFriend = connection.getFriend(friendName);

            // If friend is not yet known to the connection, skip it.
            // This can occur upon login if the nosave IQ packet is processed before the roster packet is processed.
            // It is ok to skip this friend name, because when the friend presence arrives it will automatically
            // trigger the adding of the nosave feature if applicable.
            if (noSaveFriend != null) {
                // add feature (if necessary) to all of this friend's presences
                for (FriendPresence presence : noSaveFriend.getPresences().values()) {
                    addNoSaveFeatureIfNecessary(presence, friends.get(friendName));
                }
            }
        }
    }

    /**
     * In adding jabber connection listener,
     * used to specify to jabber connection that
     * only nosave packets are processed.
     *
     * @return PacketFilter
     */
    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            @Override
            public boolean accept(Packet packet) {
                return packet instanceof NoSaveIQ;
            }
        };
    }

    /**
     * Called when this class is no longer relevant (such as when
     * the end user signs off)
     */
    public void cleanup() {
        this.friendPresenceSupport.removeListener(friendPresenceListener);
    }


    private void register(ListenerSupport<FriendPresenceEvent> friendPresenceSupport) {
        this.friendPresenceSupport = friendPresenceSupport;
        this.friendPresenceListener = new FriendPresenceListener();
        this.friendPresenceSupport.addListener(friendPresenceListener);
    }

    private void updateNoSaveStatusMap(Map<String, NoSave> noSaveMap) {
        synchronized (this) {
            this.noSaveMap = new HashMap<String, NoSave>(noSaveMap);
        }
    }

    private void updateNewPresenceNoSave(FriendPresence presence) {
        String friendId = presence.getFriend().getId();
        NoSave noSaveStatus;

        synchronized (this) {
            noSaveStatus = noSaveMap.get(friendId);
        }
        if (noSaveStatus != null) {
            addNoSaveFeatureIfNecessary(presence, noSaveStatus);
        }
    }

    private void addNoSaveFeatureIfNecessary(FriendPresence presence, NoSave nosave) {
        if (shouldAddFeature(presence, nosave)) {
            presence.addFeature(new NoSaveFeature(new NoSaveStatusImpl(nosave, presence.getFriend().getId())));
        }
    }

    /**
     * Add feature to presence only if the presence currently doesn't have the feature
     * or if the presence's current nosave feature status value is different than the correct one
     * (the value most recently parsed from the nosave packet).
     */
    private boolean shouldAddFeature(FriendPresence presence, NoSave nosave) {
        if (presence.hasFeatures(NoSaveFeature.ID) &&
            nosave == ((NoSaveFeature)presence.getFeature(NoSaveFeature.ID)).getFeature().getStatus()) {
            return false;
        } else {
            return true;
        }

    }

    /**
     * This FriendPresenceEvent listener class is responsible for keeping new
     * presences up to date with current nosave data.
     *
     */
    private class FriendPresenceListener implements EventListener<FriendPresenceEvent> {
        @Override
        public void handleEvent(FriendPresenceEvent event) {
            // update all new presences with nosave data
            if (event.getType() == FriendPresenceEvent.Type.ADDED) {
                updateNewPresenceNoSave(event.getData());
            }
        }
    }

    /**
     * Inner class impl of {@link NoSaveStatus} which uses its
     * containing XMPPConnectionImpl member to send a nosave set
     * packet in the toggleStatus method.
     */
    private class NoSaveStatusImpl implements NoSaveStatus {

        private final NoSave noSave;
        private final String userName;

        NoSaveStatusImpl(NoSave noSave, String userName) {
            this.noSave = noSave;
            this.userName = userName;
        }

        @Override
        public NoSave getStatus() {
            return noSave;
        }

        @Override
        public void toggleStatus() throws FriendException {
            NoSaveIQ noSaveMsg = NoSaveIQ.getNoSaveSetMessage(
                    userName, noSave == NoSave.ENABLED ? NoSave.DISABLED : NoSave.ENABLED);
            NoSaveIQListener.this.connection.sendPacket(noSaveMsg);
        }
    }
}
