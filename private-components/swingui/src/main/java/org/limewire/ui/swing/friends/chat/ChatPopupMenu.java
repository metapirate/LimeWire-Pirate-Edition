package org.limewire.ui.swing.friends.chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.library.ShareListIcons;
import org.limewire.ui.swing.search.FriendPresenceActions;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Popup menu for a friend in the chat window. 
 */
class ChatPopupMenu extends JPopupMenu {
    private final Provider<FriendPresenceActions> remoteHostActions;
    private final Provider<SharedFileListManager> sharedFileListManager;
    private final Provider<LibraryMediator> library;
    private final Provider<ChatFrame> chatFrame;
    private final ChatFriend chatFriend;
    
    @Inject    
    public ChatPopupMenu(Provider<ChatFriendList> chatFriendList,
            Provider<FriendPresenceActions> remoteHostActions,
            Provider<SharedFileListManager> sharedFileListManager,
            Provider<LibraryMediator> library, Provider<ChatFrame> chatFrame) {
        this.remoteHostActions = remoteHostActions;
        this.sharedFileListManager = sharedFileListManager;
        this.library = library;
        this.chatFrame = chatFrame;
        
        chatFriend = chatFriendList.get().getSelectedFriend();
        
        init();
    }
    
    private void init() {        
        if (chatFriend.isChatting()) {
            add(createShareListSubMenu());
            addSeparator();                
            add(new BrowseFiles());
            addSeparator();
            add(new CloseChat());
        } else {
            add(new OpenChat());
            addSeparator();
            add(createShareListSubMenu());
            addSeparator();                
            add(new BrowseFiles());
        }
    }
    
    /**
     * Performs a browse host on the selected friend. This is
     * only enabled if the friend is on LW and is browseable.
     */
    class BrowseFiles extends AbstractAction {
         public BrowseFiles() {
            super(I18n.tr("Browse Files"));
            setEnabled(chatFriend.isSignedInToLimewire());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            remoteHostActions.get().viewFriendLibrary(chatFriend.getFriend());
        }
    }
    
    /**
     * Closes the current chat with the selected friend.
     */
    private class CloseChat extends AbstractAction {
        public CloseChat() {
            super(I18n.tr("Close Conversation"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            chatFrame.get().closeConversation(chatFriend);
        }
    }
    
    /**
     * Starts a new chat with the selected friend.
     */
    private class OpenChat extends AbstractAction {  
        public OpenChat() {
            super(I18n.tr("Open Conversation"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            chatFrame.get().selectOrStartConversation(chatFriend);
        }
    }   
    
    /**
	 * Creates a new Share List in the Library and adds this friend to that list.
	 */
    private class ShareNewList extends  AbstractAction {
        public ShareNewList() {
            super(I18n.tr("Share New List"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final int id = sharedFileListManager.get().createNewSharedFileList(I18n.tr("Untitled"));
            for(SharedFileList list : sharedFileListManager.get().getModel()) {
                if(list.getId() == id) {
                    list.addFriend(chatFriend.getID());
                    library.get().selectAndRenameSharedList(list);
                }
            }            
        }
    } 
    
    private JMenu createShareListSubMenu() {
        JMenu shareListMenu = new JMenu(I18n.tr("Share List"));
        for (SharedFileList list : sharedFileListManager.get().getModel()) {
            if(list.isPublic()){//skip the public share list
                continue;
            }
            shareListMenu.add(new AddRemoveCheckBoxMenuItem(chatFriend.getID(), list));
        }
        shareListMenu.addSeparator();
        shareListMenu.add(new ShareNewList());

        return shareListMenu;
    }
    
    /**
	 * Check box in the Share List menu, if the list is shared with the selected friend,
	 * this will be checked.
	 */
    private class AddRemoveCheckBoxMenuItem extends JCheckBoxMenuItem {
        public AddRemoveCheckBoxMenuItem(final String friendID, final SharedFileList shareList) {            
            super(shareList.getCollectionName(), new ShareListIcons().getListIcon(shareList));
      
            setSelected(shareList.getFriendIds().contains(friendID));
      
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                    
                    if(isSelected()){
                        shareList.addFriend(friendID);
                    } else {
                        shareList.removeFriend(friendID);
                    }
                  
                    setIcon(new ShareListIcons().getListIcon(shareList));
                }
            });
        }
    }
}
