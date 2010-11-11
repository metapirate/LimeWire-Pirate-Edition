package org.limewire.ui.swing.components;

import static org.limewire.ui.swing.util.I18n.tr;
import static org.limewire.ui.swing.util.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.limewire.collection.MultiIterable;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.feature.ReferrerFeature;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.search.BlockUserMenuFactory;
import org.limewire.ui.swing.search.FriendPresenceActions;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

/**
 * A Widget for displaying RemoteHosts associated with a piece of data. 
 * A drop down menu is displayed that may be actions associated with a 
 * single RemoteHost, or a list of RemoteHosts each with an action
 * associated with each Host.
 */
public class RemoteHostWidget extends JPanel {

    public static enum RemoteWidgetType {
        SEARCH_LIST, TABLE, UPLOAD
    }
    
    private final LimeComboBox comboBox;
    private final JPopupMenu comboBoxMenu;
    
    private final Provider<FriendPresenceActions> fromActions;
    private final Provider<BlockUserMenuFactory> blockUserMenuFactory;
    
    private List<RemoteHost> people = new ArrayList<RemoteHost>();
    private List<RemoteHost> poppedUpPeople = Collections.emptyList();
    
    private final RemoteWidgetType type;
    private FriendPresenceActions remoteHostActions;
    
    @Inject
    RemoteHostWidget(ComboBoxDecorator comboBoxDecorator,
                           Provider<FriendPresenceActions> fromActions,
                           Provider<BlockUserMenuFactory> blockUserMenuFactory,
                           @Assisted RemoteWidgetType type) {
        
        this.fromActions = fromActions;
        this.blockUserMenuFactory = blockUserMenuFactory;
        this.type = type;
        
        comboBox = new LimeComboBox();
        comboBoxDecorator.decorateMiniComboBox(comboBox);
        
        this.comboBoxMenu = new JPopupMenu();
        this.comboBox.overrideMenu(this.comboBoxMenu);
        comboBoxMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                if(!poppedUpPeople.equals(people)) {
                    comboBox.setClickForcesVisible(true);
                }
            }
            
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if(!poppedUpPeople.equals(people)) {
                    comboBox.setClickForcesVisible(true);
                }
            }
            
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                comboBox.setClickForcesVisible(false);
                poppedUpPeople = people;
                updateMenus();
            }
        });
        
        this.layoutComponents();
        this.setOpaque(false);
    }
    
    /** A name for actions when they're the only action available on a host. */
    private static final String SINGULAR_ACTION_NAME = "singularActionName";
    
    private Action getChatAction(final RemoteHost person) {
        return new AbstractAction(tr("Chat")) {
            {
                putValue(SINGULAR_ACTION_NAME, tr("Chat with {0}", person.getFriendPresence().getFriend().getRenderName()));
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                getRemoteHostAction().chatWith(person.getFriendPresence().getFriend());
            }
        };
    }

    private Action getLibraryAction(final RemoteHost person) {
        return new AbstractAction(tr("Browse Files")) {
            {
                putValue(SINGULAR_ACTION_NAME, tr("Browse Files of {0}", person.getFriendPresence().getFriend().getRenderName()));
            }
        
            @Override
            public void actionPerformed(ActionEvent e) {
                getRemoteHostAction().viewLibrariesOf(Collections.singleton(person.getFriendPresence()));
            }
        };
    }
    
    /**
     * Returns an Action to block the specified remote host.
     */
    private Action getBlockUserAction(RemoteHost person) {
        Action blockAction = blockUserMenuFactory.get().createBlockUserAction(
                tr("Block User"), person.getFriendPresence().getFriend());
        
        blockAction.putValue(SINGULAR_ACTION_NAME, tr("Block User {0}", person.getFriendPresence().getFriend().getRenderName()));
        
        return blockAction;
    }
    
    private FriendPresenceActions getRemoteHostAction() {
        if(remoteHostActions == null)
            remoteHostActions = fromActions.get();
        return remoteHostActions;
    }

    private void layoutComponents() {
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

        add(this.comboBox, BorderLayout.WEST);
    }

    public void setPerson(RemoteHost remoteHost) {
        this.people.clear();
        this.people.add(remoteHost);
        this.comboBox.setText(getFromText());
    }
    
    public void setPeople(Collection<RemoteHost> people) {
        this.people.clear();
        this.people.addAll(people);
        this.comboBox.setText(getFromText());
    }

    private String getFromText() {
        if(people.size() == 0) {
            return tr("nobody");
        } else if(people.size() == 1 && type == RemoteWidgetType.UPLOAD) { 
            return people.get(0).getFriendPresence().getFriend().getRenderName();
        } else {
            boolean foundFriend = false;
            boolean foundAnon = false;
            for(RemoteHost host : people) {
                if(host.getFriendPresence().getFriend().isAnonymous()) {
                    foundAnon = true;
                } else {
                    foundFriend = true;
                }
                
                if(foundAnon && foundFriend) {
                    // no need to search anymore.
                    break;
                }
            }
            if(foundFriend && foundAnon) {
                if(type == RemoteWidgetType.TABLE) {
                    return trn("{0} Person", "{0} People", people.size());
                } else {
                    return trn("Person", "People", people.size());
                }
            } else if(foundFriend) {
                if(type == RemoteWidgetType.TABLE) {
                    return (people.size() == 1) ? people.get(0).getFriendPresence().getFriend().getRenderName() : tr("{0} Friends", people.size());
                } else {
                    return (people.size() == 1) ? people.get(0).getFriendPresence().getFriend().getRenderName() : tr("Friends");
                }
            } else { // foundAnon
                if (isTorrentWebSearch(people)) {
                    return tr("Web Search");
                } else if(type == RemoteWidgetType.TABLE) {
                    return trn("{0} P2P User", "{0} P2P Users", people.size());
                } else {
                    return trn("P2P User", "P2P Users", people.size());
                }
            }
        }
    }
    
    private static boolean isTorrentWebSearch(List<RemoteHost> people) {
        if (people.size() == 0) {
            return false;
        }
        
        RemoteHost host = people.get(0);
        
        if (host == null) {
            return false;
        }
        
        FriendPresence presence = host.getFriendPresence();
        
        if (presence == null) {
            return false;
        }
        
        Friend friend = presence.getFriend();
        
        if (friend == null) {
            return false;
        }
        
        Network network = friend.getNetwork();
        
        if (network == null) {
            return false;
        }
        
        return network.getType() == Network.Type.WEBSEARCH; 
    }

    private JMenuItem createItem(Action a) {
        JMenuItem item = new JMenuItem(a);        
        comboBox.decorateMenuComponent(item);        
        return item;
    }
    
    private void updateMenus() {        
        comboBoxMenu.removeAll();        
        if (people.size() == 0) {
            return; // menu has no items
        }

        if(people.size() == 1 && type == RemoteWidgetType.UPLOAD) {
            RemoteHost person = people.get(0);
            if (person.isChatEnabled()) {
                comboBoxMenu.add(createItem(getChatAction(person)));
            }
            // always show the browse host, disable if this action isn't available
            JMenuItem item = createItem(getLibraryAction(person));
            item.setEnabled(person.isBrowseHostEnabled());
            comboBoxMenu.add(item);
 
        } else {
            List<JMenuItem> friends = new ArrayList<JMenuItem>();
            List<JMenuItem> friendsDisabled = new ArrayList<JMenuItem>();
            List<JMenuItem> p2pUsers = new ArrayList<JMenuItem>();
            List<JMenuItem> p2pUsersDisabled = new ArrayList<JMenuItem>();
            List<RemoteHost> browsableHosts = new ArrayList<RemoteHost>();
            
            if (isTorrentWebSearch(people)) {
                URI referrerURI = (URI) people.get(0).getFriendPresence().getFeature(ReferrerFeature.ID).getFeature();
                comboBoxMenu.add(createItem(new UrlAction(I18n.tr("Locate"), referrerURI.toASCIIString())));
            } else {
                for (RemoteHost person : people) {
                    JMenu submenu = new JMenu(person.getFriendPresence().getFriend().getRenderName());
                    comboBox.decorateMenuComponent(submenu);

                    if (person.isChatEnabled()) {
                        submenu.add(createItem(getChatAction(person)));
                    }
                    if (person.isBrowseHostEnabled()) {
                        submenu.add(createItem(getLibraryAction(person)));
                    }
                    // Add Block User action for P2P users.
                    if (person.getFriendPresence().getFriend().isAnonymous()) {
                        submenu.add(createItem(getBlockUserAction(person)));
                    }

                    JMenuItem itemToAdd = submenu;
                    // If we only added one item, remove the parent menu and make this it.
                    if (submenu.getMenuComponentCount() == 1) {
                        itemToAdd = (JMenuItem) submenu.getMenuComponent(0);
                        Action action = itemToAdd.getAction();
                        // Replace the name with the singular name.
                        action.putValue(Action.NAME, action.getValue(SINGULAR_ACTION_NAME));
                    } else if (submenu.getMenuComponentCount() == 0) {
                        itemToAdd = new JMenuItem(submenu.getText());
                        comboBox.decorateMenuComponent(itemToAdd);
                        itemToAdd.setEnabled(false);
                    }

                    if(itemToAdd.isEnabled()){
                        browsableHosts.add(person);
                    }

                    if (person.getFriendPresence().getFriend().isAnonymous()) {
                        if (itemToAdd.isEnabled()) {
                            p2pUsers.add(itemToAdd);
                        } else {
                            p2pUsersDisabled.add(itemToAdd);
                        }
                    } else {
                        if (itemToAdd.isEnabled()) {
                            friends.add(itemToAdd);
                        } else {
                            friendsDisabled.add(itemToAdd);
                        }
                    }
                }

                // Now go back through our submenus & add them in.
                if (friends.size() + friendsDisabled.size() > 0 &&
                        p2pUsers.size() + p2pUsersDisabled.size() > 0) {
                    // Add friends to menu.
                    for (JMenuItem friend : new MultiIterable<JMenuItem>(friends, friendsDisabled)) {
                        comboBoxMenu.add(friend);
                    }
                    // Add P2P users to menu.
                    for (JMenuItem p2pUser : new MultiIterable<JMenuItem>(p2pUsers, p2pUsersDisabled)) {
                        comboBoxMenu.add(p2pUser);
                    }
                } else if (friends.size() + friendsDisabled.size() == 1) {
                    // Only one friend so add sub-menu items directly to menu.
                    for (JMenuItem friend : new MultiIterable<JMenuItem>(friends, friendsDisabled)) {
                        if (friend instanceof JMenu) {
                            Component[] components = ((JMenu) friend).getMenuComponents();
                            for (Component component : components) {
                                comboBoxMenu.add(component);
                            }
                        } else {
                            comboBoxMenu.add(friend);
                        }
                    }
                } else if (friends.size() + friendsDisabled.size() > 1) {
                    for (JMenuItem friend : new MultiIterable<JMenuItem>(friends, friendsDisabled)) {
                        comboBoxMenu.add(friend);
                    }
                } else if (p2pUsers.size() + p2pUsersDisabled.size() > 0) {
                    for (JMenuItem p2pUser : new MultiIterable<JMenuItem>(p2pUsers, p2pUsersDisabled)) {
                        comboBoxMenu.add(p2pUser);
                    }
                }            
            }
        }
    }
    
    @Override
    public String getToolTipText(){
        return comboBox.getText();
    }
}
