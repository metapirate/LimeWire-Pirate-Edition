package org.limewire.ui.swing.mainframe;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchDetails.SearchType;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.FlexibleTabList;
import org.limewire.ui.swing.components.FlexibleTabListFactory;
import org.limewire.ui.swing.components.NoOpAction;
import org.limewire.ui.swing.components.SelectableJXButton;
import org.limewire.ui.swing.components.TabActionMap;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.friends.refresh.AllFriendsRefreshManager;
import org.limewire.ui.swing.home.HomeMediator;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.NavMediator;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.NavigationListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.painter.factories.SearchTabPainterFactory;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.DefaultSearchRepeater;
import org.limewire.ui.swing.search.KeywordAssistedSearchBuilder;
import org.limewire.ui.swing.search.SearchBar;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.SearchNavItem;
import org.limewire.ui.swing.search.SearchNavigator;
import org.limewire.ui.swing.search.SearchResultMediator;
import org.limewire.ui.swing.search.KeywordAssistedSearchBuilder.CategoryOverride;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
class TopPanel extends JXPanel implements SearchNavigator {
    
    @Resource private Icon browseIcon;
    
    private final JXButton friendButton;
    private final SearchBar searchBar;    

    private final FlexibleTabList searchList;
    private final Navigator navigator;
    private final NavItem homeNav;
    private final NavItem libraryNav;
    private final Provider<KeywordAssistedSearchBuilder> keywordAssistedSearchBuilder;
    private final HomeMediator homeMediator;

    private final String repeatSearchTitle = I18n.tr("Repeat Search");
    private final String refreshBrowseTitle = I18n.tr("Refresh");
    
    /** Maximum number of tabs open in this session*/
    private volatile long maxTabCount = 0;

    private final AllFriendsRefreshManager allFriendsRefreshManager;    
    
    @Inject
    public TopPanel(SearchHandler searchHandler,
                    Navigator navigator,
                    final HomeMediator homeMediator,
                    SearchBar searchBar,
                    FlexibleTabListFactory tabListFactory,
                    BarPainterFactory barPainterFactory,
                    SearchTabPainterFactory tabPainterFactory,
                    final LibraryMediator myLibraryMediator,
                    Provider<KeywordAssistedSearchBuilder> keywordAssistedSearchBuilder,
                    Provider<FriendsButton> friendsButtonProvider,
                    AllFriendsRefreshManager allFriendsRefreshManager,
                    ButtonDecorator buttonDecorator) {        
        GuiUtils.assignResources(this);
        this.searchBar = searchBar;
        this.navigator = navigator;
        this.searchBar.addSearchActionListener(new Searcher(searchHandler));        
        this.keywordAssistedSearchBuilder = keywordAssistedSearchBuilder;
        this.homeMediator = homeMediator;
        this.allFriendsRefreshManager = allFriendsRefreshManager;        
        setName("WireframeTop");
        
        setBackgroundPainter(barPainterFactory.createTopBarPainter());
        
        homeNav = navigator.createNavItem(NavCategory.LIMEWIRE, HomeMediator.NAME, homeMediator);
        
        libraryNav = navigator.createNavItem(NavCategory.LIBRARY, LibraryMediator.NAME, myLibraryMediator);
        JXButton libraryButton = new SelectableJXButton(NavigatorUtils.getNavAction(libraryNav));
        
        libraryButton.setName("WireframeTop.libraryButton");
        
        libraryButton.setText(I18n.tr("My Files"));
        buttonDecorator.decorateBasicHeaderButton(libraryButton);
                
        friendButton = friendsButtonProvider.get();
        friendButton.setName("WireframeTop.friendsButton");
               
        searchList = tabListFactory.create();
        searchList.setName("WireframeTop.SearchList");
        searchList.setCloseAllText(I18n.tr("Close All Searches"));
        searchList.setCloseOneText(I18n.tr("Close Search"));
        searchList.setCloseOtherText(I18n.tr("Close Other Searches"));
        searchList.setRemovable(true);
        searchList.setSelectionPainter(tabPainterFactory.createSelectionPainter());
        searchList.setHighlightPainter(tabPainterFactory.createHighlightPainter());
        searchList.setNormalPainter(tabPainterFactory.createNormalPainter());
        searchList.setTabInsets(new Insets(0,10,2,10));
        
        setLayout(new MigLayout("gap 0, insets 0, fill, alignx leading"));
        add(libraryButton, "gapleft 5, gapbottom 2, gaptop 0");
        add(friendButton, "gapleft 3, gapbottom 2, gaptop 0");

        add(searchBar, "gapleft 11, gapbottom 2, gaptop 1");
        add(searchList.getComponent(), "gapleft 10, gaptop 4, gapbottom 0, grow, push");
        
        navigator.addNavigationListener(new NavigationListener() {
            @Override
            public void categoryRemoved(NavCategory category, boolean wasSelected) {
                if(wasSelected && category == NavCategory.SEARCH_RESULTS) {
                    goHome();
                }
            }
            
            @Override public void categoryAdded(NavCategory category) {}
            @Override public void itemAdded(NavCategory category, NavItem navItem) {}
            @Override public void itemRemoved(NavCategory category, NavItem navItem, boolean wasSelected) {}
            
            @Override public void itemSelected(NavCategory category, NavItem navItem, NavSelectable selectable, NavMediator navMediator) {
                if(category != NavCategory.SEARCH_RESULTS) {
                    TopPanel.this.searchBar.setText("");
                }
            }
      });
    }
    
    @Override
    public boolean requestFocusInWindow() {
        return searchBar.requestFocusInWindow();
    }
    

    @Override
    public SearchNavItem addSearch(String title, final JComponent searchPanel, final BrowseSearch search, SearchResultsModel model) {
        return addSearch(title, searchPanel, search, model, createBrowseActions(search, model), browseIcon, false);
    }

    @Override
    public SearchNavItem addSearch(String title, final JComponent searchPanel, final Search search, SearchResultsModel model) {
        return addSearch(title, searchPanel, search, model, createSearchActions(search, model), null, true);
    }
    
    @Override
    public void openAdvancedSearch() {
        searchBar.selectAdvancedSearch();
    }
    
    private SearchNavItem addSearch(String title, final JComponent searchPanel, final Search search, SearchResultsModel model, 
            List<Action> contextActions, Icon icon, boolean stopSpinnerAfter50Results) {
        NavItem item = navigator.createNavItem(NavCategory.SEARCH_RESULTS, title, new SearchResultMediator(searchPanel));
        SearchAction action = new SearchAction(item);
        action.putValue(Action.LARGE_ICON_KEY, icon);
        search.addSearchListener(action);

        Action moreTextAction = new NoOpAction();
        action.putValue(Action.LONG_DESCRIPTION, action.getValue(Action.NAME));      

        TabActionMap actionMap = new TabActionMap(action, action, moreTextAction, contextActions);       
        searchList.addTabActionMapAt(actionMap, 0);
        maybeIncrementMaxTabCount();
        
        String searchText = "";
        if(model.getSearchType() == SearchType.KEYWORD) {
            searchText = title;
        }
        item.addNavItemListener(new SearchTabNavItemListener(action, actionMap, (Disposable)searchPanel, searchText, search.getCategory()));
        
        return new SearchNavItemImpl(item, action, stopSpinnerAfter50Results);
    }

    public void goHome() {
        homeNav.select();
        homeMediator.getComponent().loadDefaultUrl();
    }

    private List<Action> createSearchActions(Search search, SearchResultsModel model){
        final Action moreResults = new MoreResultsAction(search).register();
        final Action repeatSearch = new RepeatSearchAction(repeatSearchTitle, search, model).register();
        final Action stopSearch = new StopSearchAction(search).register();
        return Arrays.asList(stopSearch, repeatSearch, TabActionMap.SEPARATOR, moreResults);
    }
    
    private List<Action> createBrowseActions(Search search, SearchResultsModel model){
        final Action refresh = new RepeatSearchAction(refreshBrowseTitle, search, model).register();
        return Arrays.asList(refresh);
        
    }
    
    /** Keeps track of the maximum number of tabs open in this session for inspections*/
    private void maybeIncrementMaxTabCount() {
        if(searchList.getTabs().size() > maxTabCount){
            maxTabCount = searchList.getTabs().size();
        }
    }
    
    /**
     * Specialized NavItemListener that handles disposing the SearchPanel after a search is closed, 
     * removes the panel from the search list. and updates the search bar text and category when a 
     * new search tab is selected. 
     */
    private class SearchTabNavItemListener implements NavItemListener {
        private final SearchAction action;
        private final TabActionMap actionMap;
        private final String searchText;
        private final Disposable panel;
        private final SearchCategory category;

        private SearchTabNavItemListener(SearchAction action, TabActionMap actionMap, Disposable panel, String searchText, SearchCategory category) {
            this.action = action;
            this.actionMap = actionMap;
            this.searchText = searchText;
            this.panel = panel;
            this.category = category;
        }

        @Override
        public void itemRemoved(boolean wasSelected) {
            searchList.removeTabActionMap(actionMap);
            panel.dispose();
            if(searchList.getTabs().size() == 0){
                searchBar.setText(null);
            }
        }

        @Override
        public void itemSelected(boolean selected) {
            searchBar.setText(searchText);
            searchBar.setCategory(category);
            action.putValue(Action.SELECTED_KEY, selected);
        }
    }

    private final static class SearchNavItemImpl implements SearchNavItem {
        private final NavItem item;
        private final SearchAction action;
        private final boolean stopSpinnerAfter50Results;

        private SearchNavItemImpl(NavItem item, SearchAction action, boolean stopSpinnerAfter50Results) {
            this.item = item;
            this.action = action;
            this.stopSpinnerAfter50Results = stopSpinnerAfter50Results;
        }

        @Override
        public void sourceCountUpdated(int newSourceCount) {
            if(!item.isSelected()) {
                action.putValue(TabActionMap.NEW_HINT, true);
            }

            if (stopSpinnerAfter50Results && newSourceCount >= 50) {
                action.killBusy();
            }
        }

        @Override
        public String getId() {
            return item.getId();
        }

        @Override
        public void remove() {
            item.remove();
        }

        @Override
        public void select() {
            select(null);
        }

        @Override
        public void select(NavSelectable selectable) {
            item.select();
        }

        @Override
        public void addNavItemListener(NavItemListener listener) {
            item.addNavItemListener(listener);
        }

        @Override
        public void removeNavItemListener(NavItemListener listener) {
            item.removeNavItemListener(listener);
        }

        @Override
        public boolean isSelected() {
            return item.isSelected();
        }
    }

    private class Searcher implements ActionListener {
        private final SearchHandler searchHandler;
        
        Searcher(SearchHandler searchHandler) {
            this.searchHandler = searchHandler;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get search text, and do search if non-empty.
            String searchText = searchBar.getSearchText();
            if (!searchText.isEmpty()) {
                
                SearchCategory category = searchBar.getCategory();
                String query = searchText;
                
                // Check if the category was overridden by a keyword 
                CategoryOverride categoryOverride = keywordAssistedSearchBuilder.get().parseCategoryOverride(query);
                
                // Do not allow searches in the Other category
                if (categoryOverride != null && categoryOverride.getCategory() != SearchCategory.OTHER) {
                    
                    // Set new category and trim the category text out of the search string
                    //  for the actual search
                    category = categoryOverride.getCategory();
                    query = categoryOverride.getCutQuery();

                    // Update the UI with the new category
                    searchBar.setCategory(category);
                }
                    
                // Attempt to parse an advanced search from the search query
                SearchInfo search = keywordAssistedSearchBuilder.get().attemptToCreateAdvancedSearch(
                        query, category);
                
                // Fall back on the normal search
                if (search == null) {
                    search = DefaultSearchInfo.createKeywordSearch(query, category);
                } 
                
                if(searchHandler.doSearch(search)) {
                    searchBar.selectAllSearchText();
                }
            }
        }
    }
    
    private class SearchAction extends AbstractAction implements SearchListener {
        private final NavItem item;
        private Timer busyTimer;
        
        public SearchAction(NavItem item) {
            super(item.getId());
            this.item = item;

            // Make sure this syncs up with any changes in selection.
            addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        if (evt.getNewValue().equals(Boolean.TRUE)) {
                            SearchAction.this.item.select();
                            putValue(TabActionMap.NEW_HINT, null);
                        }
                    }
                }
            });
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals(TabActionMap.SELECT_COMMAND)) {
                item.select();
                if(!StringUtils.isEmpty(searchBar.getSearchText())) {
                        searchBar.requestSearchFocus();
                }
            } else if (e.getActionCommand().equals(TabActionMap.REMOVE_COMMAND)) {
                item.remove();
            }
        }

        @Override
        public void handleSearchResult(Search search, SearchResult searchResult) {
        }
        
        @Override
        public void handleSearchResults(Search search, Collection<? extends SearchResult> searchResults) {
        }
        
        void killBusy() {
            if (busyTimer != null && busyTimer.isRunning()) {
                busyTimer.stop();
            }
            putValue(TabActionMap.BUSY_KEY, Boolean.FALSE);
        }
        
        @Override
        public void searchStarted(Search search) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    busyTimer = new Timer(60000, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            putValue(TabActionMap.BUSY_KEY, Boolean.FALSE);
                        }
                    });
                    busyTimer.setRepeats(false);
                    busyTimer.start();
                    putValue(TabActionMap.BUSY_KEY, Boolean.TRUE);
                }
            });
        }
        
        @Override
        public void searchStopped(Search search) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    killBusy();
                }
            });
        }
    }
    
    private static abstract class SearchListenerAction extends AbstractAction implements SearchListener{
        protected final Search search;

        public SearchListenerAction(String tr, Search search) {
            super(tr);
            this.search = search;
        }        
    
        /**
         * Registers the SearchListenerAction as a SearchListener on search.
         * 
         * @return this
         */
        public SearchListenerAction register(){
            search.addSearchListener(this);
            return this;
        }
        
        @Override
        public void handleSearchResult(Search search, SearchResult searchResult) {}
        @Override
        public void handleSearchResults(Search search, Collection<? extends SearchResult> searchResults) {}
    }

    /**
     * Action which repeats a search.  Since it makes no sense to
     * repeat an unstarted search, this class only enables searches to be
     * repeated if they have already started.
     */
    private static class MoreResultsAction extends SearchListenerAction {

        MoreResultsAction(Search search) {
            super(I18n.tr("Find More Results"), search);
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            search.repeat();
        }

        @Override
        public void searchStarted(Search search) {
            setEnabled(true);
        }

        @Override public void searchStopped(Search search) { }
    }
    
    /**
     * Clears the current search results and repeats the search
     */
    private class RepeatSearchAction extends SearchListenerAction {
        private SearchResultsModel model;

        RepeatSearchAction(String title, Search search, SearchResultsModel model) {
            super(title, search);
            this.model = model;
            setEnabled(false);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if(model.getSearchType() == SearchType.ALL_FRIENDS_BROWSE){
                allFriendsRefreshManager.refresh();
            } else {            
                new DefaultSearchRepeater(search, model).refresh();
            }
        }
        
        @Override
        public void searchStopped(Search search) {
        }

        @Override
        public void searchStarted(Search search) {
            setEnabled(true);            
        }
        
    }
    
    /**Stops the search*/
    private static class StopSearchAction extends SearchListenerAction {
        StopSearchAction(Search search) {
            super(I18n.tr("Stop Search"), search);
            setEnabled(false);
        }
                
        @Override
        public void actionPerformed(ActionEvent e) {
            search.stop();
        }          

        @Override
        public void searchStarted(Search search) {
            setEnabled(true);
        }

        @Override
        public void searchStopped(Search search) {
            setEnabled(false);
        } 
    }
    

}
