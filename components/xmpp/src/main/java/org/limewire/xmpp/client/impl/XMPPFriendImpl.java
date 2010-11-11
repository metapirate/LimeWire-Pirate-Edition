package org.limewire.xmpp.client.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import net.jcip.annotations.GuardedBy;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.ChatStateListener;
import org.jivesoftware.smackx.ChatStateManager;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.friend.api.ChatState;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.impl.AbstractFriend;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.DebugRunnable;
import org.limewire.util.StringUtils;


public class XMPPFriendImpl extends AbstractFriend {
   
    private static final Log LOG = LogFactory.getLog(XMPPFriendImpl.class);

    private final String id;
    private final FeatureRegistry featureRegistry;
    private final String idNoService;
    private AtomicReference<RosterEntry> rosterEntry;
    private final org.jivesoftware.smack.XMPPConnection connection;
    private final Network network;

    // -----------------------------------------------------------------
    // presences map (presences of this user who are signed in)
    // and the active presence JID (the presence lw is chatting with)
    // represent the Presence State
    private final Object presenceLock;
    
    @GuardedBy("presenceLock")
    private final Map<String, FriendPresence> presences;

    @GuardedBy("presenceLock")
    private String activePresenceJid;
    // -----------------------------------------------------------------


    // -----------------------------------------------------------------
    private final Object chatListenerLock;

    @GuardedBy("chatListenerLock")
    private volatile IncomingChatListenerAdapter listenerAdapter;
    // -----------------------------------------------------------------


    XMPPFriendImpl(String id, RosterEntry rosterEntry, Network network,
             org.jivesoftware.smack.XMPPConnection connection,
             FeatureRegistry featureRegistry) {
        this.id = id;
        this.featureRegistry = featureRegistry;
        this.idNoService = stripService(id, network.getNetworkName());
        this.network = network;
        this.rosterEntry = new AtomicReference<RosterEntry>(rosterEntry);
        this.presences = new HashMap<String, FriendPresence>();
        this.activePresenceJid = null;
        this.connection = connection;
        this.presenceLock = new Object();
        this.chatListenerLock = new Object();
    }
    
    private static String stripService(String id, String service) {
        int idx = id.lastIndexOf("@" + service);
        if(idx == -1) {
            return id;
        } else {
            return id.substring(0, idx);    
        }
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        String name = rosterEntry.get().getName();
        if(name != null) {
            String service = network.getNetworkName();
            int idx = name.lastIndexOf("@" + service);
            if(idx == -1) {
                return name;
            } else {
                return name.substring(0, idx);
            }
        } else {
            return null;
        }
    }

    @Override
    public String getRenderName() {
        String visualName = getName();
        if(visualName == null) {
            return idNoService;
        } else {
            return visualName;
        }
    }
    
    @Override
    public String getFirstName() {
        String visualName = getName();
        if(visualName == null) {
            return idNoService;
        } else {
            String[] subStrings = visualName.split(" ");
            return subStrings[0];
        }
    }

    void setRosterEntry(RosterEntry rosterEntry) {
        this.rosterEntry.set(rosterEntry);
    }

    @Override
    public void setName(final String name) {
        Thread t = ThreadExecutor.newManagedThread(new DebugRunnable(new Runnable() {
            public void run() {
                try {
                    XMPPFriendImpl.this.rosterEntry.get().setName(name);
                } catch (org.jivesoftware.smack.XMPPException e) {
                    LOG.debugf("set name failed", e);
                }
            }
        }), "set-name-thread-" + toString());
        t.start();
    }

    @Override
    public Map<String, FriendPresence> getPresences() {
        synchronized (presenceLock) {
            return Collections.unmodifiableMap(new HashMap<String, FriendPresence>(presences));
        }
    }
    
    @Override
    public boolean isSubscribed() {
        switch(rosterEntry.get().getType()) {
            case both:
            case to:
                return true;
            default:
                return false;
        }
    }

    void addPresense(FriendPresence presence) {
        if(LOG.isDebugEnabled()) {
            LOG.debugf("adding presence {0}", presence.getPresenceId());
        }
        synchronized (presenceLock) {
            presences.put(presence.getPresenceId(), presence);
        }
        firePresenceEvent(new PresenceEvent(presence, PresenceEvent.Type.PRESENCE_NEW));
    }

    void removePresense(FriendPresence presence) {
        if(LOG.isDebugEnabled()) {
            LOG.debugf("removing presence {0}", presence.getPresenceId());
        }
        Collection<Feature> features = presence.getFeatures();
        for(Feature feature : features) {
            LOG.debugf("removing feature {0} for {1}", feature, presence);
            featureRegistry.get(feature.getID()).removeFeature(presence);
        }

        synchronized (presenceLock) {
            presences.remove(presence.getPresenceId());

            // if the presence being removed is the same presence as the active presence, set the
            // active presence to null so the next outgoing message goes to all of this user's presences
            if (presence.getPresenceId().equals(activePresenceJid)) {
                activePresenceJid = null;
            }

            if (!isSignedIn()) {
                removeChatListener();
            }
        }

        firePresenceEvent(new PresenceEvent(presence, PresenceEvent.Type.PRESENCE_UPDATE));
    }

    @Override
    public String toString() {
        return StringUtils.toString(this, id, getName());
    }

    void updatePresence(FriendPresence updatedPresence) {
        if(LOG.isDebugEnabled()) {
            LOG.debugf("updating presence {0}", updatedPresence.getPresenceId());
        }
        synchronized (presenceLock) {
            presences.put(updatedPresence.getPresenceId(), updatedPresence);
        }
        firePresenceEvent(new PresenceEvent(updatedPresence, PresenceEvent.Type.PRESENCE_UPDATE));
    }

    FriendPresence getPresence(String jid) {
        synchronized (presenceLock) {
            return presences.get(jid);
        }
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    private String getChatParticipantId() {
        synchronized (presenceLock) {
            return (activePresenceJid == null) ? id : activePresenceJid;
        }
    }

    private void setActivePresence(String presenceId) {
        synchronized (presenceLock) {
            activePresenceJid = presenceId;
        }
    }

    @Override
    public FriendPresence getActivePresence() {
        synchronized (presenceLock) {
            return presences.get(activePresenceJid);
        }
    }

    @Override
    public boolean hasActivePresence() {
        synchronized (presenceLock) {
            return (activePresenceJid != null);
        }
    }

    @Override
    public boolean isSignedIn() {
        synchronized (presenceLock) {
            return !(presences.isEmpty());
        }
    }

    @Override
    public MessageWriter createChat(final MessageReader reader) {
        if(LOG.isInfoEnabled()) {
            LOG.info("new chat with " + getChatParticipantId());
        }
        final Chat chat = connection.getChatManager().createChat(getChatParticipantId(),
                new DefaultChatStateListener(reader));
        return new DefaultMessageWriter(chat);
    }

    @Override
    public void setChatListenerIfNecessary(final IncomingChatListener listener) {
        synchronized (chatListenerLock) {
            if (listenerAdapter == null) {
                listenerAdapter = new IncomingChatListenerAdapter(listener);
                connection.getChatManager().addChatListener(listenerAdapter);
            }
        }
    }

    @Override
    public void removeChatListener() {
        synchronized (chatListenerLock) {
            if(listenerAdapter != null) {
                connection.getChatManager().removeChatListener(listenerAdapter);
                listenerAdapter = null;
            }
        }
    }

    private class IncomingChatListenerAdapter implements ChatManagerListener {
        private final IncomingChatListener listener;

        public IncomingChatListenerAdapter(IncomingChatListener listener) {
            this.listener = listener;
        }

        @Override
        public void chatCreated(final Chat chat, boolean createdLocally) {
            String chatParticipant = chat.getParticipant();
            if (!createdLocally && isForThisUser(chatParticipant)) {
                if (!chatParticipant.equals(getChatParticipantId())) {
                    setActivePresence(chatParticipant);
                }
                if (LOG.isInfoEnabled()) {
                    LOG.info("new incoming chat with " + getChatParticipantId());
                }
                DefaultMessageWriter writer = new DefaultMessageWriter(chat);
                MessageReader reader = listener.incomingChat(writer);
                chat.addMessageListener(new DefaultChatStateListener(reader));
            }
        }

        private boolean isForThisUser(String incomingMsgJid) {
            return incomingMsgJid.startsWith(id);
        }

    }

    /**
     * This class encapsulates the actual writing of messages in the smack API.
     *                                                                                                    
     * The message writer must take into account which presence is the current
     * active presence and set the chat participant appropriately
     * (set to jid identifying the presence, or the user id if no active presence)
     *
     */
    private class DefaultMessageWriter implements MessageWriter {

        private Chat chat;

        DefaultMessageWriter(Chat chat) {
            this.chat = chat;
        }

        @Override
        public void writeMessage(String message) throws FriendException {
            try {
                chat.setParticipant(getChatParticipantId());
                chat.sendMessage(message);
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new FriendException(e);
            }
        }

        @Override
        public void setChatState(ChatState chatState) throws FriendException {
            try {
                ChatStateManager.getInstance(connection).setCurrentState(org.jivesoftware.smackx.ChatState.valueOf(chatState.toString()), chat);
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new FriendException(e);
            }
        }
    }

    /**
     * Acts as an adapter between the smack message/chat state listener and our
     * {@link MessageReader} class.
     *
     * Note: If a message is received which comes from a different presence
     * than the presence LW is currently chatting with, processMessage will
     * set the active presence to the presence from which it received the message.
     *  
     */
    private class DefaultChatStateListener implements ChatStateListener {

        private final MessageReader reader;

        DefaultChatStateListener(MessageReader reader) {
            this.reader = reader;
        }

        @Override
        public void processMessage(Chat chat, Message message) {
            String msgFromJid = message.getFrom();
            if (!(getChatParticipantId().equals(msgFromJid))) {
                setActivePresence(msgFromJid);
            }
            
            if (message.getType() == Message.Type.error) {
                String errorMsg = parseError(message);
                if (errorMsg != null) {
                    reader.error(parseError(message));
                }
            } else {
                reader.readMessage(message.getBody());
            }
        }

        @Override
        public void stateChanged(Chat chat, org.jivesoftware.smackx.ChatState state) {
            if (isSignedIn()) {
                reader.newChatState(ChatState.valueOf(state.toString()));
            }
        }
        
        private String parseError(Message errorMessage) {
            XMPPError error = errorMessage.getError();
            if (error != null) {
                String body = errorMessage.getBody();
                if (body != null) {
                    return "Error sending message: '" + body + "' : " + error.toString();
                }
            }
            return null;
        }
    }
}
