package org.limewire.xmpp.client.impl;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.limewire.friend.api.FriendRequest;
import org.limewire.friend.api.FriendRequestDecisionHandler;
import org.limewire.friend.api.FriendRequestEvent;
import org.limewire.friend.impl.util.PresenceUtils;
import org.limewire.listener.EventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

/**
 * Handles presence subscriptions and unsubscriptions on an XMPP connection
 * (see RFC 3921 section 8). <code>FriendRequestEvent</code>s that require
 * decisions from the user are broadcast for interception by the UI, and the
 * results are passed back asynchronously through the
 * <code>FriendRequestDecisionHandler</code> interface. 
 */
class SubscriptionListener
implements PacketListener, PacketFilter, FriendRequestDecisionHandler {

    private static final Log LOG = LogFactory.getLog(SubscriptionListener.class);

    private final XMPPConnection connection;
    private final EventBroadcaster<FriendRequestEvent> friendRequestBroadcaster;

    SubscriptionListener(XMPPConnection connection,
            EventBroadcaster<FriendRequestEvent> friendRequestBroadcaster) {
        this.connection = connection;
        this.friendRequestBroadcaster = friendRequestBroadcaster;
    }

    @Override
    public void processPacket(Packet packet) {
        try {
            Presence presence = (Presence)packet;
            String friendUsername = PresenceUtils.parseBareAddress(packet.getFrom());
            if(presence.getType() == Type.subscribe) {
                LOG.debugf("subscribe from {0}", friendUsername);
                // If this is a new friend request, ask the user what to do
                Roster roster = connection.getRoster();
                if(roster != null) {
                    RosterEntry entry = roster.getEntry(friendUsername);
                    if(entry == null) {
                        LOG.debug("it's a new subscription");
                        // Ask the user
                        friendRequestBroadcaster.broadcast(new FriendRequestEvent(
                                new FriendRequest(friendUsername, this),
                                FriendRequestEvent.Type.REQUESTED));
                    } else {
                        LOG.debug("it's a response to our subscription");
                        // Acknowledge the subscription
                        Presence subbed = new Presence(Presence.Type.subscribed);
                        subbed.setTo(friendUsername);
                        connection.sendPacket(subbed);
                    }
                }
            } else if(presence.getType() == Type.subscribed) {
                LOG.debugf("subscribed from {0}", friendUsername);
            } else if(presence.getType() == Type.unsubscribe) {
                LOG.debugf("unsubscribe from {0}", friendUsername);
                // Acknowledge the unsubscription
                Presence unsubbed = new Presence(Presence.Type.unsubscribed);
                unsubbed.setTo(friendUsername);
                connection.sendPacket(unsubbed);
                // If this is a response, don't respond again
                Roster roster = connection.getRoster();
                if(roster != null) {
                    RosterEntry entry = roster.getEntry(friendUsername);
                    if(entry == null) {
                        LOG.debug("it's a response to our unsubscription");
                    } else {
                        LOG.debug("it's a new unsubscription");
                        // Unsubscribe from the friend
                        Presence unsub = new Presence(Presence.Type.unsubscribe);
                        unsub.setTo(friendUsername);
                        connection.sendPacket(unsub);
                        // Remove the friend from the roster
                        try {
                            roster.removeEntry(entry);
                        } catch(XMPPException x) {
                            LOG.debug(x);
                        }
                    }
                }
            } else if(presence.getType() == Type.unsubscribed) {
                LOG.debugf("unsubscribed from {0}", friendUsername);
            }
        } catch (XMPPException e) {
            LOG.debug("processPacket failed", e);
        }
    }

    @Override
    public boolean accept(Packet packet) {
        if(packet instanceof Presence) {
            Presence presence = (Presence)packet;
            return presence.getType() == Type.subscribe
            || presence.getType() == Type.subscribed
            || presence.getType() == Type.unsubscribe
            || presence.getType() == Type.unsubscribed;
        }
        return false;
    }

    @Override
    public void handleDecision(String friendUsername, boolean accepted) {
        try {
            if(!connection.isConnected())
                return;
            if(accepted) {
                LOG.debug("user accepted");
                // Acknowledge the subscription
                Presence subbed = new Presence(Presence.Type.subscribed);
                subbed.setTo(friendUsername);
                connection.sendPacket(subbed);
                // Add the friend to the roster (this will subscribe to the friend)
                Roster roster = connection.getRoster();
                if(roster != null) {
                    roster.createEntry(friendUsername, friendUsername, null);
                }
            } else {
                LOG.debug("user declined");
                // Refuse the subscription
                Presence unsubbed = new Presence(Presence.Type.unsubscribed);
                unsubbed.setTo(friendUsername);
                connection.sendPacket(unsubbed);
            }
        } catch (XMPPException e) {
            LOG.debug("handleDecision failed", e);    
        }
    }
}