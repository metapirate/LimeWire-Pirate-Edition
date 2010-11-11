package org.limewire.ui.swing.friends.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.FriendPresenceEvent;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.inject.LazySingleton;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

/**
 * Keeps a list of friends and their presences. As friends sign on and off,
 * this list is automatically updated. This also listens for incoming chat
 * messages from a given presence. 
 */
@LazySingleton
class ChatModel {
    /** List of friends to chat with */
    private final EventList<ChatFriend> chatFriends;
    /** Mapping of friendId to ChatFriend */
    private final Map<String, ChatFriend> idToFriendMap;
    
    private final ListenerSupport<FriendPresenceEvent> presenceSupport;
    private PresenceListener presenceEvent;
    
    /**
     * Listener for incoming chat events. 
     */
    private final List<IncomingListener> incomingListeners = new CopyOnWriteArrayList<IncomingListener>();
    
    private final EventBroadcaster<ChatMessageEvent> chatMessageList;
    private final EventBroadcaster<ChatStateEvent> chatStateList;
    
    @Inject
    public ChatModel(ListenerSupport<FriendPresenceEvent> presenceSupport,
                     EventBroadcaster<ChatMessageEvent> chatMessageList,
                     EventBroadcaster<ChatStateEvent> chatStateList) {
        this.presenceSupport = presenceSupport;
        this.chatFriends = new BasicEventList<ChatFriend>();
        this.idToFriendMap = new HashMap<String, ChatFriend>();
        this.chatStateList = chatStateList;
        this.chatMessageList = chatMessageList;
    }
    
    /**
     * Returns an EventList of chatFriends.
     */
    public EventList<ChatFriend> getChatFriendList() {
        return chatFriends;
    }
    
    /** 
     * Returns the ChatFriend associated with this friendId. 
     * Returns null if no ChatFriend exists for this friendId.
     */
    public ChatFriend getChatFriend(String friendId) {
        return idToFriendMap.get(friendId);
    }
    
    public void addIncomingListener(IncomingListener listener) {
        incomingListeners.add(listener);
    }
    
    public void removeIncomingListener(IncomingListener listener) {
        incomingListeners.remove(listener);
    }
    
    /**
     * Registers presence listeners.
     */
    public void registerListeners() {
        // listen for presence sign on/off changes
        if(presenceEvent == null)
            presenceEvent = new PresenceListener();
        presenceSupport.addListener(presenceEvent);
    }
    
    /**
     * Removes presence listeners and clears the list of friends.
     */
    public void unregisterListeners() {
        if(presenceEvent != null)
            presenceSupport.removeListener(presenceEvent);
        idToFriendMap.clear();
        chatFriends.clear();
    }
    
    /**
     * Updates the list of ChatFriends as presences sign on and off.
     */
    void handlePresenceEvent(FriendPresenceEvent event) {
        final FriendPresence presence = event.getData();
        final Friend friend = presence.getFriend();
        ChatFriend chatFriend = idToFriendMap.get(friend.getId());
        switch(event.getType()) {
        case ADDED:
            addFriend(chatFriend, presence);
            break;
        case UPDATE:
            if (chatFriend != null) {
                chatFriend.update();
            }
            break;
        case REMOVED:
            if (chatFriend != null) {
                removeFriendIfNecessary(chatFriend);
                chatFriend.update();
            }
            break;
        }
    }

    /**
     * Remove friend from the friends list if necessary (not chatting, and no presences
     * are signed in anymore).
     * 
     * @param chatFriend friend to remove from friends list.
     */
    public void removeFriendIfNecessary(ChatFriend chatFriend) {
        if (shouldRemoveFromFriendsList(chatFriend)) {
            Friend friend = chatFriend.getFriend();
            chatFriends.remove(idToFriendMap.remove(friend.getId()));
            friend.removeChatListener();
        }    
    }
    
    /**
     * Remove from the friends list only when:
     * <pre>
     * 1. The user (buddy) associated with the chatfriend is no longer signed in, AND
     * 2. The chat has been closed (by clicking on the "x" on the friend in the friend's list)
     * </pre>
     * @param chatFriend the ChatFriend to decide whether to remove (no null check)
     * @return true if chatFriend should be removed.
     */
    private boolean shouldRemoveFromFriendsList(ChatFriend chatFriend) {
        return (!chatFriend.isChatting()) && (!chatFriend.isSignedIn());
    }
    
    /**
     * Adds a friend to the list of friends that can be chatted with. Also
     * adds a listener to this friend presence that listens for incoming messages
     * from this presence. 
     * <p>
     * This listener ensures that the ChatPanel has been created prior to 
     * firing a ConversationEvent.
     */
    private void addFriend(ChatFriend chatFriend, final FriendPresence presence) {
        if(chatFriend == null) {
            chatFriend = new ChatFriendImpl(presence);
            chatFriends.add(chatFriend);
            idToFriendMap.put(presence.getFriend().getId(), chatFriend);
        }

        final ChatFriend friend = chatFriend;
        IncomingChatListener incomingChatListener = new IncomingChatListener() {
            public MessageReader incomingChat(final MessageWriter writer) {
                SwingUtils.invokeNowOrWait(new Runnable() {
                    @Override
                    public void run() {
                        MessageWriter writerWrapper = new MessageWriterImpl(friend, writer, chatMessageList);
                        fireIncomingEvent(friend, writerWrapper);
                    }
                });
                return new MessageReaderImpl(friend, chatMessageList, chatStateList);
            }
        };
        presence.getFriend().setChatListenerIfNecessary(incomingChatListener);
        chatFriend.update();
    }
    
    private void fireIncomingEvent(ChatFriend chatFriend, MessageWriter messageWriter) {
        for(IncomingListener listener : incomingListeners) {
            listener.incomingChat(chatFriend, messageWriter);
        }
    }
    
    /**
     * Listens for changes in friend presences.
     */
    private class PresenceListener implements EventListener<FriendPresenceEvent> {
        @Override
        @SwingEDTEvent
        public void handleEvent(FriendPresenceEvent event) {
            handlePresenceEvent(event);
        }
    }
}
