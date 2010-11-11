package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseStatus.BrowseState;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendConnectionEvent.Type;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.components.decorators.MessageDecorator;
import org.limewire.ui.swing.friends.chat.ChatMediator;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

/**
 * Replaces the search results when a browse fails.  Must be disposed.
 */
public class BrowseFailedMessagePanel extends JPanel implements Disposable{

    @Resource private Font chatFont;
    @Resource private Color chatForeground;

    private final SearchResultsModel searchResultsModel;
    private final Provider<ChatMediator> chatMediator;
    private final EventBean<FriendConnectionEvent> connectionEventBean;
    private EventListener<FriendConnectionEvent> connectionListener;
    private ListenerSupport<FriendConnectionEvent> connectionSupport;
    private final RemoteLibraryManager remoteLibraryManager;
    private final Provider<MessageDecorator> messageDecoratorProvider;
    private final HeaderBarDecorator headerBarDecorator;
    
    private BrowseSearch browseSearch;
    
    private boolean isInitialized = false;

    private BrowseState state;

    private List<Friend> friends;

    @Inject
    public BrowseFailedMessagePanel(EventBean<FriendConnectionEvent> connectionEventBean, Provider<ChatMediator> chatMediator, RemoteLibraryManager remoteLibraryManager,
            Provider<MessageDecorator> messageDecoratorProvider, HeaderBarDecorator headerBarDecorator,
            @Assisted SearchResultsModel searchResultsModel) {
        GuiUtils.assignResources(this);
        this.connectionEventBean = connectionEventBean;
        this.chatMediator = chatMediator;
        this.searchResultsModel = searchResultsModel;
        this.remoteLibraryManager = remoteLibraryManager;
        this.messageDecoratorProvider = messageDecoratorProvider;
        this.headerBarDecorator = headerBarDecorator;
    }
    
    public void update(BrowseState state, BrowseSearch browseSearch, List<Friend> friends){
        this.state = state;
        this.browseSearch = browseSearch;
        this.friends = friends;
        if(!isInitialized){
            isInitialized = true;
            initialize();
        }
        updateLabel();
    }
    
    @Inject
    void registerListener(ListenerSupport<FriendConnectionEvent> connectionSupport) {
        
        this.connectionSupport = connectionSupport;
        //TODO: this should probably be handled in the BrowseSearch models eventually
        connectionListener = new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                if (isInitialized) {
                    updateLabel();
                    repaint();
                }
            }
        };
        
        connectionSupport.addListener(connectionListener);
    }
  
    private void initialize() {
        setLayout(new MigLayout("insets 0, gap 0, fill", "[]", "[][grow][grow]"));
    }
    
    private void updateLabel(){
        removeAll();
        add(createMessageComponent(getLabelText()), "pos 0.50al 0.4al");
        add(createBottomComponent(), "pos 1al 1al");
    }

    /**
     * Floating message in the FriendLibrary. This displays feedback to the user
     * as to what state their friend is in when no files are displayed.
     */
    private JComponent createMessageComponent(String text) {
        
        HeaderBar header = new HeaderBar(new JLabel(""));
        header.setLayout(new MigLayout("insets 0, gap 0!, novisualpadding, alignx 100%, aligny 100%"));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerBarDecorator.decorateBasic(header);
        add(header, "growx, growy, wrap");       
        
        MessageComponent messageComponent = new MessageComponent();
        messageDecoratorProvider.get().decorateGrayMessage(messageComponent);

        JLabel message = new JLabel(text);
        messageComponent.decorateHeaderLabel(message);
        messageComponent.addComponent(message, hasRefresh() ? "" : "wrap");
       

        if (hasRefresh()) {
            HyperlinkButton refresh = new HyperlinkButton(I18n.tr("Retry"));
            refresh.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    BrowseFailedMessagePanel.this.setVisible(false);
                    new DefaultSearchRepeater(browseSearch, searchResultsModel).refresh();
                }
            });
            messageComponent.decorateHeaderLink(refresh);
            messageComponent.addComponent(refresh, "gapleft 5, wrap");
        }
        
        if(state == BrowseState.NO_FRIENDS_SHARING || state == BrowseState.OFFLINE){
            String subText = null;
            if (isUserOffline()) {
                if(state == BrowseState.NO_FRIENDS_SHARING){
                subText = I18n.tr("When you sign on to LimeWire, your friends' files will appear here.");
                } else {//BrowseState.OFFLINE
                    subText = I18n.tr("When you sign on to LimeWire, your friend's files will appear here.");
                }
            } else {
                subText = I18n.tr("When they sign on to LimeWire and share with you, their files will appear here.");
            }
                
            JLabel subMessage = new JLabel(subText);
            messageComponent.decorateSubLabel(subMessage);            
            messageComponent.addComponent(subMessage, "");
        }
        
        return messageComponent;
    }
    
    private boolean hasRefresh(){
        return state != BrowseState.OFFLINE && state != BrowseState.NO_FRIENDS_SHARING;
    }
    
    private JComponent createBottomComponent(){
        if(state == BrowseState.NO_FRIENDS_SHARING && !isUserOffline()){  
            String text;
            if(areFriendsSignedOnToLimeWire()){
                text = I18n.tr("<HTML><A href=\"\">Chat</A> and tell them to share.</HTML>");
            } else {
                text = I18n.tr("<HTML><A href=\"\">Chat</A> and tell them to sign on.</HTML>");
            }
            
            HTMLLabel message = new HTMLLabel(text);           
            message.setHtmlFont(chatFont);
            message.setHtmlForeground(chatForeground);
            message.addHyperlinkListener(new HyperlinkListener(){
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        chatMediator.get().setVisible(true);
                    }
                }
            });
            
            JPanel panel = new JPanel(new MigLayout("insets 0, gap 0, novisualpadding"));
            panel.add(message, "gapright 5");
            return panel;
        }
        return new JLabel();
    }


    private String getLabelText() {
        if (isUserOffline()
                && (state == BrowseState.NO_FRIENDS_SHARING || state == BrowseState.OFFLINE)) {
            return getUserOfflineText();
        }

        if (state == BrowseState.NO_FRIENDS_SHARING) {
            if (areFriendsSignedOnToLimeWire()){
                return I18n.tr("No friends are sharing with you");
            } 
            return I18n.tr("No friends are on LimeWire");
        }
        
        if (state == BrowseState.OFFLINE) {
            if (isSingleBrowse()) {
                return I18n.tr("{0} is not signed on to LimeWire.", getSingleFriendName());
            } else {
                return I18n.tr("These people are not signed on to LimeWire.");
            }
        }

        if (isSingleBrowse()) {
            return I18n.tr("There was a problem browsing {0}.", getSingleFriendName());
        } else {
            return I18n.tr("There was a problem viewing these people.");
        }

    }

    private boolean isUserOffline(){
        return connectionEventBean.getLastEvent() == null || connectionEventBean.getLastEvent().getType() != Type.CONNECTED;
    }
    
    private boolean isSingleBrowse(){
        return friends.size() == 1;
    }   
    
    public boolean areFriendsSignedOnToLimeWire(){
        return remoteLibraryManager.getFriendLibraryList().size() != 0;
    }
    
    
    private String getSingleFriendName(){
        return friends.get(0).getRenderName();
    }
    
    private String getUserOfflineText(){
        return I18n.tr("You are offline.");
    }

    public void dispose() {
        if (connectionSupport != null) {
            connectionSupport.removeListener(connectionListener);
        }
    }

}
