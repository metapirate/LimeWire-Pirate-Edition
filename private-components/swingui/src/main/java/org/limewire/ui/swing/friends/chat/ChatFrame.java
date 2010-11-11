package org.limewire.ui.swing.friends.chat;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.friend.api.MessageWriter;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.listener.EventBroadcaster;

import com.google.inject.Inject;

/**
 * Main Panel for the Chat window. Creates and manages all of the other
 * components within the Chat window.
 */
@LazySingleton
class ChatFrame extends JPanel {

    @Resource private Color borderColor;
    @Resource private Color dividerColor;
    
    private final ChatHeader chatHeader;
    private final ChatFriendList chatList;
    private final ConversationPanel conversationPanel;
    private final ChatMediator chatMediator;
    
    private final EventBroadcaster<ChatMessageEvent> chatMessageList;
    private final EventBroadcaster<ChatStateEvent> chatStateList;
    
    @Inject
    public ChatFrame(ChatHeader chatHeader, ChatFriendList chatList, 
            ConversationPanel conversationPanel, ChatPopupHandler popupHandler,
            ChatMediator chatMediator,
            EventBroadcaster<ChatMessageEvent> chatMessageList,
            EventBroadcaster<ChatStateEvent> chatStateList) {
        super(new MigLayout("gap 0, insets 0, fill"));
        
        this.chatHeader = chatHeader;
        this.chatList = chatList;
        this.conversationPanel = conversationPanel;
        this.chatMediator = chatMediator;           
        this.chatMessageList = chatMessageList;
        this.chatStateList = chatStateList;
        
        GuiUtils.assignResources(this);
        
        setBorder(BorderFactory.createMatteBorder(1,1,0,1, borderColor));
        
        add(chatHeader.getComponent(), "dock north");
        
        JScrollPane scrollPane = new JScrollPane(chatList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, dividerColor));
        ResizeUtils.forceWidth(scrollPane, 122);
        chatList.setPopupHandler(popupHandler);
        add(scrollPane, "dock west");
        
        add(conversationPanel.getComponent(), "dock center");
    }
    
    @Inject
    public void register() {
        // listen for selection of a friend in the ChatList. Update the shown conversation and
        // ChatFrame header when appropriate.
        chatList.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if(chatList.getSelectedRow() >= 0 && chatList.getSelectedRow() < chatList.getRowCount()) {
                    ChatFriend chatFriend = (ChatFriend) chatList.getModel().getValueAt(chatList.getSelectedRow(), 0);
                    if(conversationPanel.hasConversation(chatFriend)) {
                        chatHeader.setFriend(chatFriend);
                        conversationPanel.displayConverstaion(chatFriend);
                    } 
                }
            }
        });
        // double click listener
        chatList.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    int index = chatList.rowAtPoint(e.getPoint());
                    if(index >= 0 && index < chatList.getRowCount()) {
                        ChatFriend chatFriend = (ChatFriend) chatList.getModel().getValueAt(chatList.getSelectedRow(), 0);
                        selectOrStartConversation(chatFriend);
                    }
                }
            }
        });
    }
    
    /**
     * Starts or selects a conversation with the given ChatFriend.
     */
    public void selectOrStartConversation(ChatFriend chatFriend) {
        if(!conversationPanel.hasConversation(chatFriend)) {
            startConversation(chatFriend, createMessageWriter(chatFriend));
        } else {
            conversationPanel.displayConverstaion(chatFriend);
            chatHeader.setFriend(chatFriend);        
            // underline the currently selected friend, need to repaint all the chats
            chatList.repaint();
        }
        
        chatMediator.getChatButton().repaint();
    }
    
    /**
     * Creates a new MessageWriter for this ChatFriend if one doesn't already exist.
     */
    private MessageWriter createMessageWriter(ChatFriend chatFriend) {
        MessageWriter writer = chatFriend.createChat(new MessageReaderImpl(chatFriend, chatMessageList, chatStateList));
        MessageWriter messageWriter = new MessageWriterImpl(chatFriend, writer, chatMessageList);
        return messageWriter;
    }
    
    /**
     * Starts a conversation with the given ChatFriend using the given MessageWriter.
     */
    public void startConversation(ChatFriend chatFriend, MessageWriter messageWriter) {
        if(!conversationPanel.hasConversation(chatFriend)) {
            chatFriend.startChat();
            conversationPanel.startNewChat(chatFriend, messageWriter);
            chatHeader.setFriend(chatFriend);        
            // underline the currently selected friend, need to repaint all the chats
            chatList.repaint();
        }
    }
    
    /**
     * Returns the ChatFriend whose conversation is currently selected, 
     * or null if no ChatFriend exists.
     */
    public ChatFriend getSelectedConversation() {
        return conversationPanel.getCurrentConversationFriend();
    }
    
    /**
     * Closes all Conversations with all friends.
     */
    public void closeAllChats() {
        chatHeader.clearFriend();
        conversationPanel.removeAllConversations();
        chatList.repaint();
        chatMediator.getChatButton().repaint();
    }
    
    /**
     * Closes a Conversation with a given Friend.
     */
    public void closeConversation(ChatFriend chatFriend) {
        chatHeader.clearFriend();
        conversationPanel.removeConversation(chatFriend);
        chatList.repaint();
        chatMediator.getChatButton().repaint();
    }
}
