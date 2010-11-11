package org.limewire.ui.swing.friends.chat;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.ChatState;
import org.limewire.friend.api.FriendEvent;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.inject.LazySingleton;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Contains all the ConversationPanes. Each friend that has an open conversation
 * will have a unique ConversationPane. Only one ConversationPane can be open at 
 * a given time. 
 */
@LazySingleton
class ConversationPanel {
    private final ConversationPaneFactory conversationFactory;
    private final JPanel component;
    /**  Map of friendId's to the conversation pane. */
    private final Map<String, ConversationPane> chats;
    
    /** 
      * Used to coordinate removal from friends list when chat is no longer needed. 
      * When friend is signed off and afterwards we close the conversation, 
      * the friend should be removed from the friends list.
      */
    private final ChatModel chatModel;
    
    /**
     * Friend who's conversation is currently displayed, null if no conversation is
     * being displayed.
     */
    private ChatFriend selectedConversation = null;

    /**
     * Listeners for chat related events targeted for specific conversation panes 
     */
    private final ListenerSupport<ChatMessageEvent> messageListenerManager;
    private final ListenerSupport<ChatStateEvent> chatStateListenerManager;
    private final ListenerSupport<FeatureEvent> featureListenerManager;
    private final ListenerSupport<FriendEvent> friendListenerManager;
    private final EventListener<ChatMessageEvent> messageEventListener;
    private final EventListener<ChatStateEvent> chatStateEventListener;
    private final EventListener<FeatureEvent> featureEventListener;
    private final EventListener<FriendEvent> friendEventListener;
    
    @Inject
    public ConversationPanel(ConversationPaneFactory conversationFactory, 
                             ListenerSupport<ChatMessageEvent> messageListenerManager,
                             ListenerSupport<ChatStateEvent> chatStateListenerManager,
                             @Named("available")ListenerSupport<FriendEvent> friendListenerManager,
                             ListenerSupport<FeatureEvent> featureListenerManager,
                             ChatModel chatModel) {       
        this.conversationFactory = conversationFactory;
        this.chatModel = chatModel;
        component = new JPanel(new BorderLayout());
        this.chats = new HashMap<String, ConversationPane>();
        this.messageListenerManager = messageListenerManager;
        this.chatStateListenerManager = chatStateListenerManager;
        this.friendListenerManager = friendListenerManager;
        this.featureListenerManager = featureListenerManager;
        this.messageEventListener = new EventListener<ChatMessageEvent>() {
            @SwingEDTEvent
            @Override
            public void handleEvent(ChatMessageEvent event) {
                Message message = event.getData();
                String friendId = message.getFriendID();
                if (chats.containsKey(friendId)) {
                    chats.get(friendId).newChatMessage(message);
                }
            }
        };
        this.chatStateEventListener = new EventListener<ChatStateEvent>() {
            @SwingEDTEvent
            @Override
            public void handleEvent(ChatStateEvent event) {
                ChatState chatState = event.getType();
                String friendId = event.getSource();
                if (chats.containsKey(friendId)) {
                    chats.get(friendId).newChatState(chatState);    
                }
            }
        };
        this.featureEventListener = new EventListener<FeatureEvent>() {
            @SwingEDTEvent
            @Override
            public void handleEvent(FeatureEvent event) {
                String friendId = event.getSource().getFriend().getId();
                if (chats.containsKey(friendId)) {
                    chats.get(friendId).featureUpdate(event.getData(), event.getType());
                }
            }
        };
        this.friendEventListener = new EventListener<FriendEvent>() {
            @SwingEDTEvent
            @Override
            public void handleEvent(FriendEvent event) {
                String friendId = event.getData().getId();
                if (chats.containsKey(friendId)) {
                    chats.get(friendId).friendAvailableUpdate(event.getType());
                }
            }
        };
    }
    
    public void registerListeners() {
        messageListenerManager.addListener(messageEventListener);
        chatStateListenerManager.addListener(chatStateEventListener);
        friendListenerManager.addListener(friendEventListener);
        featureListenerManager.addListener(featureEventListener);
    }
    
    public void unregisterListeners() {
        messageListenerManager.removeListener(messageEventListener);
        chatStateListenerManager.removeListener(chatStateEventListener);
        friendListenerManager.removeListener(friendEventListener);
        featureListenerManager.removeListener(featureEventListener);
    }

    /**
     * Returns the panel containing the conversations.
     */
    public JComponent getComponent() {
        return component;
    }
    
    private void setConversationPanel(JComponent chatComponent) {
        component.removeAll();
        component.add(chatComponent, BorderLayout.CENTER);
        component.repaint();
    }
    
    /**
     * Displays the conversation with the given ChatFriend.
     */
    public void displayConverstaion(ChatFriend chatFriend) {
        ConversationPane chatPane = chats.get(chatFriend.getID());
        selectedConversation = chatFriend;
        selectedConversation.setHasUnviewedMessages(false);
        setConversationPanel(chatPane.asComponent());
        chatPane.handleDisplay();
    }
    
    /**
     * Returns the ChatFriend whose conversation is currently 
     * displayed. If no conversation is selected returns null.
     */
    public ChatFriend getCurrentConversationFriend() {
        return selectedConversation;
    }
    
    /**
     * Returns true if a conversation already exists with the given friend,
     * false otherwise.
     */
    public boolean hasConversation(ChatFriend chatFriend) {
        return chats.containsKey(chatFriend.getID());
    }
    
    /**
     * Destroys a conversation with a given friend.
     */
    public void removeConversation(ChatFriend chatFriend) {
        if(chatFriend.equals(selectedConversation)) {
            selectedConversation = null;
            setConversationPanel(new JPanel());
        }        
        if(hasConversation(chatFriend)) {
            chatFriend.stopChat();
            Conversation conversation = chats.remove(chatFriend.getID());
            conversation.dispose();
            chatModel.removeFriendIfNecessary(chatFriend);
            if (chats.isEmpty()) {
                unregisterListeners();
            }
        }
    }
    
    /**
     * Destroys all conversations with all friends.
     */
    public void removeAllConversations() {
        selectedConversation = null;
        setConversationPanel(new JPanel());
        
        for(String key : chats.keySet()) {
            chats.get(key).dispose();
        }
        chats.clear();
        unregisterListeners();
    }
    
    /**
     * Starts a new chat with the given friend.
     */
    public void startNewChat(ChatFriend chatFriend, MessageWriter messageWriter) {
        // if there are currently no chats, register the listeners
        if (chats.isEmpty()) {
            registerListeners();
        }
        ConversationPane chatPane = conversationFactory.create(messageWriter, chatFriend);
        chats.put(chatFriend.getID(), chatPane);
        selectedConversation = chatFriend;
        selectedConversation.setHasUnviewedMessages(false);
        setConversationPanel(chatPane);
        
        chatPane.handleDisplay();
    }
}
