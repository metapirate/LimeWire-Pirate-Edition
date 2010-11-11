package org.limewire.ui.swing.search;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.search.SearchDetails.SearchType;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseSearchFactory;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.inject.LazySingleton;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.friends.chat.ChatMediator;
import org.limewire.ui.swing.friends.refresh.AllFriendsRefreshManager;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
class FriendPresenceActionsImpl implements FriendPresenceActions {
    private static final Log LOG = LogFactory.getLog(FriendPresenceActionsImpl.class);
    private volatile int numBrowseAll = 0;
    private volatile int numBrowseFriend = 0;
    private volatile int numBrowseHost = 0;


    private final Provider<ChatMediator> chatMediator;

    //Provider prevents circular dependency
    private final Provider<SearchNavigator> searchNavigator;


    private final Provider<BrowseSearchFactory> browseSearchFactory;


    private final BrowsePanelFactory browsePanelFactory;
    
    private final Map<String,SearchNavItem> browseNavItemCache = new HashMap<String, SearchNavItem>();
    private final AllFriendsRefreshManager allFriendsRefreshManager;
    
    private static final String ALL_FRIENDS_KEY = "ALL_FRIENDS";


    @Inject
    public FriendPresenceActionsImpl(Provider<ChatMediator> chatMediator,  
            BrowsePanelFactory browsePanelFactory, Provider<SearchNavigator> searchNavigator,
            Navigator navigator, Provider<BrowseSearchFactory> browseSearchFactory, AllFriendsRefreshManager allFriendsRefreshManager) {
        this.chatMediator = chatMediator;
        this.browsePanelFactory = browsePanelFactory;
        this.searchNavigator = searchNavigator;
        this.browseSearchFactory = browseSearchFactory;
        this.allFriendsRefreshManager = allFriendsRefreshManager;
    }
 
    @Override
    public void chatWith(Friend friend) {
        LOG.debugf("chatWith: {0}", friend);
        chatMediator.get().startOrSelectConversation(friend.getId());
    }
    
    @Override
    public void viewFriendLibrary(Friend friend) {
        numBrowseFriend++;
        assert(friend != null && !friend.isAnonymous());
        LOG.debugf("viewLibraryOf: {0}", friend);
        
        if(navigateIfTabExists(friend.getId())) {
            return;
        }
        
        browse(browseSearchFactory.get().createFriendBrowseSearch(friend), 
                DefaultSearchInfo.createBrowseSearch(SearchType.SINGLE_BROWSE),
                friend.getRenderName(), friend.getId());
    }

    @Override
    public void viewLibrariesOf(Collection<FriendPresence> people) {
        numBrowseHost += people.size();
        if(people.size() == 1) {
            FriendPresence person = people.iterator().next();
            if (!navigateIfTabExists(person.getFriend().getId())) {
                browse(browseSearchFactory.get().createBrowseSearch(person), 
                        DefaultSearchInfo.createBrowseSearch(SearchType.SINGLE_BROWSE),
                        person.getFriend().getRenderName(), person.getFriend().getId());
            }
        } else if(!people.isEmpty()) {
            browse(browseSearchFactory.get().createBrowseSearch(people), 
                    DefaultSearchInfo.createBrowseSearch(SearchType.MULTIPLE_BROWSE),
                    getTabTitle(people),
                    null);
        }
    }
    
    @Override
    public void browseAllFriends(boolean forceRefresh) {
        numBrowseAll++;
        if(navigateIfTabExists(ALL_FRIENDS_KEY)){
            if (forceRefresh) {
                allFriendsRefreshManager.refresh();
            }
            return;
        }
        
        browse(browseSearchFactory.get().createAllFriendsBrowseSearch(),
                DefaultSearchInfo.createBrowseSearch(SearchType.ALL_FRIENDS_BROWSE), 
                I18n.tr("All Friends"), ALL_FRIENDS_KEY);
    }
    
    private String getTabTitle(Collection<FriendPresence> people){
        boolean hasP2P = hasP2P(people);
        boolean hasFriends = hasFriend(people);
        
        if (hasP2P && hasFriends){
            return I18n.trn("{0} Person", "{0} People", people.size());
        } else if (hasP2P){
            return I18n.trn("{0} P2P User", "{0} P2P Users", people.size());            
        } else if (hasFriends){
            return I18n.trn("{0} Friend", "{0} Friends", people.size());            
        }
        
        //empty list
        return "";
    }

    private boolean hasP2P(Collection<FriendPresence> people) {
        for (FriendPresence host : people) {
            if (host.getFriend().isAnonymous()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFriend(Collection<FriendPresence> people) {
        for (FriendPresence host : people) {
            if (!host.getFriend().isAnonymous()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @param key - null to not cache browse
     */
    private void browse(BrowseSearch search, SearchInfo searchInfo, String title, String key) {  
        
        SearchResultsPanel searchPanel = browsePanelFactory.createBrowsePanel(search, searchInfo);
        // Add search results display to the UI, and select its navigation item.
        SearchNavItem item = searchNavigator.get().addSearch(title, searchPanel, search, searchPanel.getModel());
        item.select();
        searchPanel.getModel().start(new SwingSearchListener(searchPanel.getModel(), item));
        searchPanel.setBrowseTitle(title);
        
        if (key != null) {
            browseNavItemCache.put(key, item);
            item.addNavItemListener(new ItemRemovalListener(key));
        }
        
        if(key == ALL_FRIENDS_KEY){
            allFriendsRefreshManager.registerBrowseSearch(search, searchPanel.getModel());
        }
        
    }
    
    /** 
     * Selects a tab in search results if there's already an open browse
     * for the key.  Returns true if an existing tab was selected.
     */
    private boolean navigateIfTabExists(String key){
        if (key != null && browseNavItemCache.get(key) != null) {
            browseNavItemCache.get(key).select();
            return true;
        }
        
        return false;
    }
    
    private class ItemRemovalListener implements NavItemListener {
        private String key;

        public ItemRemovalListener(String key){
            this.key = key;
        }

        @Override
        public void itemRemoved(boolean wasSelected) {
            browseNavItemCache.remove(key);
            if (key == ALL_FRIENDS_KEY) {
                allFriendsRefreshManager.clearBrowseSearch();
            }
        }

        @Override
        public void itemSelected(boolean selected) {
            //do nothing            
        }
        
    }

}
