package org.limewire.ui.swing.friends.chat;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.JComponent;
import javax.swing.text.html.HTMLEditorKit;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.friend.api.ChatState;
import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendEvent;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.api.feature.FileOfferFeature;
import org.limewire.friend.api.feature.LimewireFeature;
import org.limewire.friend.impl.feature.NoSave;
import org.limewire.friend.impl.feature.NoSaveFeature;
import org.limewire.friend.impl.feature.NoSaveStatus;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.action.CopyAction;
import org.limewire.ui.swing.action.CopyAllAction;
import org.limewire.ui.swing.action.PopupUtil;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.friends.chat.Message.Type;
import org.limewire.ui.swing.painter.GenericBarPainter;
import org.limewire.ui.swing.search.FriendPresenceActions;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;

/**
 * Class representing the chat window.
 */
public class ConversationPane extends JPanel implements Displayable, Conversation {
    private static final int PADDING = 5;
    private static final Log LOG = LogFactory.getLog(ConversationPane.class);
    private static final Color DEFAULT_BACKGROUND = new Color(224, 224, 224);
    private static final Color BACKGROUND_COLOR = Color.WHITE;

    private final List<Message> messages = new ArrayList<Message>();
    private final Map<String, MessageFileOffer> idToMessageWithFileOffer =
            new ConcurrentHashMap<String, MessageFileOffer>();

    private final JEditorPane editor;
    private final String conversationName;
    private final String friendId;
    private final MessageWriter writer;
    private final ChatFriend chatFriend;
    private final Provider<IconManager> iconManager;
    private final FriendPresenceActions remoteHostActions;
    private HyperlinkButton downloadlink;
    private HyperlinkButton nosaveLink;
    private JXPanel toolbar;
    private ResizingInputPanel inputPanel;
    private ChatState currentChatState;
    private NoSave noSaveState;

    private final EventBroadcaster<ChatMessageEvent> messageBroadcaster;
    
    @Resource(key="ChatConversation.toolbarTopColor") private Color toolbarTopColor;
    @Resource(key="ChatConversation.toolbarBottomColor") private Color toolbarBottomColor;
    @Resource(key="ChatConversation.toolbarBorderColor") private Color toolbarBorderColor;
    @Resource(key="ChatConversation.linkFont") private Font linkFont;
    @Resource private Icon endChat;

    private final JScrollPane conversationScroll;
    private final JPanel chatWrapper;

    @Inject
    public ConversationPane(@Assisted MessageWriter writer, final @Assisted ChatFriend chatFriend,
                            Provider<IconManager> iconManager,
                            ChatHyperlinkListenerFactory chatHyperlinkListenerFactory,
                            CloseChatMessage closeChatMessage,
                            FriendPresenceActions remoteHostActions,
                            @Named("backgroundExecutor") ScheduledExecutorService schedExecService,
                            EventBroadcaster<ChatMessageEvent> messageBroadcaster) {
        this.writer = writer;
        this.chatFriend = chatFriend;
        this.conversationName = chatFriend.getName();
        this.friendId = chatFriend.getID();
        this.iconManager = iconManager;
        this.noSaveState = null;
        this.remoteHostActions = remoteHostActions;
        this.messageBroadcaster = messageBroadcaster;
        
        GuiUtils.assignResources(this);

        setLayout(new BorderLayout());
        
        editor = new JEditorPane();
        editor.setEditable(false);
        editor.setContentType("text/html");
        editor.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        HTMLEditorKit editorKit = (HTMLEditorKit) editor.getEditorKit();
        editorKit.setAutoFormSubmission(false);

        conversationScroll = new JScrollPane(editor, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        conversationScroll.setOpaque(false);
        conversationScroll.setBorder(BorderFactory.createEmptyBorder());
        
        
        final JButton closeConversation = new IconButton(closeChatMessage);
        closeConversation.setIcon(endChat);
        
        chatWrapper = new JPanel();
        chatWrapper.setLayout(new OverlayLayout(chatWrapper));
        
        JPanel closePanel = new JPanel();
        closePanel.setLayout(null);
        closePanel.setOpaque(false);
        final Rectangle closeBounds = new Rectangle(268,5,6,6);
        final Rectangle closeBoundsSlider = new Rectangle(250,5,6,6);
        closeConversation.setBounds(closeBounds);
        closePanel.add(closeConversation);
        
        JPanel conversationPanel = new JPanel(new BorderLayout());
        conversationPanel.setOpaque(false);
        conversationPanel.add(conversationScroll, BorderLayout.CENTER);
        
        chatWrapper.add(closePanel);
        chatWrapper.add(conversationPanel);
        
        conversationScroll.getVerticalScrollBar().addComponentListener(new ComponentListener() {
            @Override
            public void componentHidden(ComponentEvent e) {
                closeConversation.setBounds(closeBounds);
            }
            @Override
            public void componentMoved(ComponentEvent e) {
            }
            @Override
            public void componentResized(ComponentEvent e) {
            }
            @Override
            public void componentShown(ComponentEvent e) {
                closeConversation.setBounds(closeBoundsSlider);
            }
        });
        
        AdjustmentListener adjustmentListener = new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                chatWrapper.repaint();
            }
        };
        
        conversationScroll.getVerticalScrollBar().addAdjustmentListener(adjustmentListener);
        conversationScroll.getHorizontalScrollBar().addAdjustmentListener(adjustmentListener);
        
        add(chatWrapper, BorderLayout.CENTER);

        PopupUtil.addPopupMenus(editor, new CopyAction(editor), new CopyAllAction());

        add(footerPanel(writer, chatFriend, schedExecService), BorderLayout.SOUTH);

        setBackground(DEFAULT_BACKGROUND);

        editor.addHyperlinkListener(chatHyperlinkListenerFactory.create(this));
    }

    /**
     * Add a new {@link Message} to this conversation.
     * 
     * @param message being added
     */
    public void newChatMessage(Message message) {
        // TODO: Refactor this,ChatDocumentBuilder, etc into cleaner/clearer, way to display msgs
        LOG.debugf("Message: from {0} text: {1}", message.getSenderName(), message.toString());
        messages.add(message);
        Type type = message.getType();

        if (type != Type.SENT) {
            currentChatState = ChatState.active;
        }

        if (message instanceof MessageFileOffer) {
            MessageFileOffer msgWithFileOffer = (MessageFileOffer)message;
            addFileOfferMessage(msgWithFileOffer);
        } else if (message instanceof NoSaveStatusMessage) {
            updateNoSaveLink(((NoSaveStatusMessage)message).getStatus());
        }

        displayMessages();
    }


    private void addFileOfferMessage(MessageFileOffer msgWithFileOffer) {
        String fileOfferID = msgWithFileOffer.getFileOffer().getId();
        idToMessageWithFileOffer.put(fileOfferID, msgWithFileOffer);
    }

    private void updateNoSaveLink(NoSave noSave) {
        noSaveState = noSave;
        nosaveLink.setText("<html><u>" + (noSaveState == NoSave.ENABLED ? I18n.tr("On the Record") :
                I18n.tr("Off the Record")) + "</u></html>");
    }

    /**
     * Called to indicate a new chat state in this conversation.
     * 
     * @param chatState being added.
     */
    public void newChatState(ChatState chatState) {
        LOG.debugf("Chat state update for {0} to {1}", chatFriend.getName(), chatState);
        if (currentChatState != chatState) {
            currentChatState = chatState;
            displayMessages();
        }
    }

    /**
     * Update availability of friend associated with this conversation.
     * 
     * @param update type of update.
     */
    public void friendAvailableUpdate(FriendEvent.Type update) {
        switch(update) {
        case ADDED:
            currentChatState = ChatState.active;
            displayMessages(false);
            if ((nosaveLink != null) && hasFeature(NoSaveFeature.ID)) {
                nosaveLink.setVisible(true);
            }
            break;
        case REMOVED:
            displayMessages(true);
            if (nosaveLink != null) {
                nosaveLink.setVisible(false);
            }
            break;
        }
    }

    /**
     * Called to indicate a new feature addition/removal.
     * 
     * @param feature the feature being updated
     * @param action whether the feature is added or removed.
     */
    public void featureUpdate(Feature feature, FeatureEvent.Type action) {
        if (feature.getID().equals(LimewireFeature.ID)) {
            if (action == FeatureEvent.Type.ADDED) {
                downloadlink.setEnabled(true);
            } else if (action == FeatureEvent.Type.REMOVED) {
                downloadlink.setEnabled(chatFriend.isSignedInToLimewire());
            }
        } else if (feature.getID().equals(NoSaveFeature.ID)) {
            if (action == FeatureEvent.Type.ADDED) {
                ensureNoSaveLinkExists();
                NoSave status = ((NoSaveStatus)feature.getFeature()).getStatus();
                if (status != noSaveState) {
                    NoSaveStatusMessage msg = new NoSaveStatusMessage(friendId,
                            Message.Type.SERVER, status);
                    messageBroadcaster.broadcast(new ChatMessageEvent(msg));
                }
            }
        }
    }

    /**
     * Performs necessary cleanup for this conversation.
     */
    @Override
    public void dispose() {
        inputPanel.dispose();
    }

    @Override
    public void displayMessages() {
        displayMessages(!chatFriend.isSignedIn());
    }

    @Override
    public ChatFriend getChatFriend() {
        return chatFriend;
    }
    
    @Override
    public JComponent asComponent() {
        return this;
    }

    /**
     * @return Map of file offer IDs to file offer messages ({@link MessageFileOffer}).
     */
    public Map<String, MessageFileOffer> getFileOfferMessages() {
        return Collections.unmodifiableMap(new HashMap<String, MessageFileOffer>(idToMessageWithFileOffer));
    }

    private void displayMessages(boolean friendSignedOff) {
        String chatDoc = ChatDocumentBuilder.buildChatText(messages, currentChatState, conversationName, friendSignedOff);
        LOG.debugf("Chat doc: {0}", chatDoc);
        final JScrollBar verticalScrollBar = conversationScroll.getVerticalScrollBar();
        final int scrollValue = verticalScrollBar.getValue();
        editor.setText(chatDoc);
        
        //LWC-2262: If the scroll bar was moved above the bottom of the scrollpane, reset the value of
        //the bar to where it was before the text was updated.  This needs to be issued to the end of the
        //queue because the actual repainting/resizing of the scrollbar happens later in a 
        //task added to the EDT by the plaf listener of the editor's document.
        //A better fix for this behavior may be possible
        if (verticalScrollBar.getMaximum() > (scrollValue + verticalScrollBar.getVisibleAmount() + PADDING)) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    verticalScrollBar.setValue(scrollValue);
                }
            });
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    verticalScrollBar.setValue(verticalScrollBar.getMaximum());
                }
            });
        }
        
        decorateFileOfferButtons();
        
        chatWrapper.repaint();
    }

    private void decorateFileOfferButtons() {
        //This is a hack to set the file mime-type icon for file offer buttons that may appear in the conversation
        recursiveButtonSearch(editor);
    }

    private void recursiveButtonSearch(Container container) {
        for(Component component : container.getComponents()) {
            if (component instanceof Container) {
                recursiveButtonSearch((Container)component);
            }
            if (component instanceof JButton) {
                JButton button = (JButton)component;
                String buttonText = button.getText();

                // Using end of button text to determine whether button shouild be disabled
                // then disable it.  This is because JEditorPane does not disable buttons
                // disabled in the form html
                if (buttonText.endsWith(":disabled")) {
                    buttonText = buttonText.substring(0, buttonText.lastIndexOf(":disabled"));
                    button.setText(buttonText);
                    button.setEnabled(false);
                }

                String extension = FileUtils.getFileExtension(buttonText);
                if (!extension.isEmpty()) {
                    Icon icon = iconManager.get().getIconForExtension(extension);
                    button.setIcon(icon);
                }

            }
        }
    }

    private JPanel footerPanel(MessageWriter writer, ChatFriend chatFriend,
                               ScheduledExecutorService schedExecService) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        
        downloadlink = new HyperlinkButton(new DownloadFromFriendLibraryAction());
        downloadlink.setFont(linkFont);
        downloadlink.setEnabled(chatFriend.isSignedInToLimewire());

        toolbar = new JXPanel(new MigLayout("insets 0 0 0 5, gap 10, alignx right, aligny 50%"));
        ResizeUtils.forceHeight(toolbar, 22);
        
        toolbar.setBackgroundPainter(new GenericBarPainter<JXPanel>(
                new GradientPaint(0, 0, toolbarTopColor, 0, 1, toolbarBottomColor),
                toolbarBorderColor, PainterUtils.TRANSPARENT,
                toolbarBorderColor, PainterUtils.TRANSPARENT));

        if (hasFeature(NoSaveFeature.ID)) {
            ensureNoSaveLinkExists();
        }

        toolbar.add(downloadlink);

        inputPanel = new ResizingInputPanel(writer, schedExecService);
        inputPanel.setBorder(BorderFactory.createEmptyBorder());
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);

        return panel;
    }

    
    private boolean hasFeature(URI feature) {
        return getFeature(feature) != null;
    }

    /**
     * Returns the feature identified by the method parameter. The
     * feature can be in any of the presences of the friend.
     * <p>
     * This was done to get around implementing adding/removing features on
     * objects (such as xmpp connections and friends) other than FriendPresences
     *
     * @param feature desired feature's URI
     * @return {@link Feature}
     */
    private Feature getFeature(URI feature) {
        for (FriendPresence presence : chatFriend.getFriend().getPresences().values()) {
            if (presence.hasFeatures(feature)) {
                return presence.getFeature(feature);
            }
        }
        return null;
    }

    private void ensureNoSaveLinkExists() {
        if (nosaveLink == null) {
            NoSaveToggleAction action = new NoSaveToggleAction();
            nosaveLink = new HyperlinkButton(action);
            nosaveLink.setFont(linkFont);

            // initialize nosave state
            NoSaveFeature nosaveFeature = (NoSaveFeature)getFeature(NoSaveFeature.ID);
            updateNoSaveLink(nosaveFeature.getFeature().getStatus());

            toolbar.add(nosaveLink, 0);
        }
    }

    @Override
    public void handleDisplay() {
        invalidate();
        repaint();
        inputPanel.handleDisplay();
    }

    private class DownloadFromFriendLibraryAction extends AbstractAction {
        public DownloadFromFriendLibraryAction() {
            super("<html><u>" + I18n.tr("Browse Files") + "</u></html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            remoteHostActions.viewFriendLibrary(chatFriend.getFriend());
        }
    }

    /**
     * Sets the nosave state to be the opposite of what it currently is.
     * Sends a nosave IQ msg to the server.
     * 
     */
    private class NoSaveToggleAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            NoSaveStatus status = ((NoSaveFeature)getFeature(NoSaveFeature.ID)).getFeature();

            try {
                status.toggleStatus();
            } catch (FriendException e1) {
                LOG.warn("Error occurred while toggling nosave status", e1);
            }
        }
    }

    public void offerFolder(ListeningFuture<List<ListeningFuture<LocalFileItem>>> future) {
        // TODO: Change this to show event immediately & update as status changes.
        future.addFutureListener(new EventListener<FutureEvent<List<ListeningFuture<LocalFileItem>>>>() {
            @Override
            public void handleEvent(FutureEvent<List<ListeningFuture<LocalFileItem>>> event) {
                if(event.getResult() != null) {
                    for(ListeningFuture<LocalFileItem> future : event.getResult()) {
                        offerFile(future);
                    }
                }
            }
        });
    }

    public void offerFile(ListeningFuture<LocalFileItem> future) {
        // TODO: Change this to show event immediately & update as status changes.
        future.addFutureListener(new EventListener<FutureEvent<LocalFileItem>>() {
            @SwingEDTEvent
            @Override
            public void handleEvent(FutureEvent<LocalFileItem> event) {
               if(event.getResult() != null) {
                   FileMetaData metadata = event.getResult().toMetadata();
                   boolean sentFileOffer = false;
                   Friend friend = chatFriend.getFriend();
                   
                   // do not send file offer if user not signed in.
                   if (!friend.isSignedIn()) {
                       String errorMsg = I18n.tr("File offer not sent because friend signed off.");
                       Message msg = new MessageFileOfferImpl(I18n.tr("me"), friendId, 
                           Message.Type.SENT, metadata, null);
                       messageBroadcaster.broadcast(
                           new ChatMessageEvent(new ErrorMessage(errorMsg, msg)));
                       return;
                   }

                   // if active presence exists, send file offer to it,
                   // otherwise broadcast to every presence with FileOfferFeature.ID feature
                   FriendPresence activePresence = friend.getActivePresence();
                   if ((activePresence != null) && activePresence.hasFeatures(FileOfferFeature.ID)) {
                        sentFileOffer = performFileOffer(metadata, activePresence);
                   } else {
                       for (FriendPresence presence : friend.getPresences().values()) {
                            sentFileOffer |= performFileOffer(metadata, presence);
                       }
                   }

                   if (sentFileOffer) {
                        messageBroadcaster.broadcast(new ChatMessageEvent(
                            new MessageFileOfferImpl(I18n.tr("me"), friendId, 
                                Message.Type.SENT, metadata, null)));
                   } else {
                       // TODO: handle file offer sending failures (consider using ErrorMessage)
                   }
               }
            }

            private boolean performFileOffer(FileMetaData metadata, FriendPresence presence) {
                Feature fileOfferFeature = presence.getFeature(FileOfferFeature.ID);
                boolean fileOfferSent = false;
                if (fileOfferFeature != null) {
                    try {
                        writer.setChatState(ChatState.active);
                        FeatureTransport<FileMetaData> fileOfferer = presence.getTransport(FileOfferFeature.class);
                        fileOfferer.sendFeature(presence, metadata);
                        fileOfferSent = true;
                    } catch (FriendException e) {
                        LOG.debug("File offer failed", e);
                    }
                }
                return fileOfferSent;
            }
        });
    }
}
