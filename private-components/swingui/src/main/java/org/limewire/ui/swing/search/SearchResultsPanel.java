package org.limewire.ui.swing.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.browse.BrowseStatus;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.filter.AdvancedFilterPanel;
import org.limewire.ui.swing.filter.AdvancedFilterPanelFactory;
import org.limewire.ui.swing.filter.AdvancedFilterPanel.CategoryListener;
import org.limewire.ui.swing.friends.refresh.AllFriendsRefreshManager;
import org.limewire.ui.swing.search.SearchResultsMessagePanel.MessageType;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.BaseResultPanel.ListViewTable;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * This is the top-level container for the search results display.  
 * SearchResultsPanel contains several UI components, including the category
 * tab items, sort and filter panel, sponsored results panel, and search
 * results tables.
 */
public class SearchResultsPanel extends JXPanel implements Disposable {
    
    /**
     * The type of overlay which should be placed over the search results.

     * NONE indicates that no overlay should be shown.
     * AWAITING_CONNECTIONS indicates that an overlay with a busy icon and a "LimeWire is connecting..." message should be shown.
     * NO_FRIENDS_ON_LIMEWIRE indicates that friends' files cannot be shown b/c no friends are logged on
     */
    public enum OverlayType {
        NONE, 
        AWAITING_CONNECTIONS,
        NO_FRIENDS_ON_LIMEWIRE
    }

    /** Decorator used to set the appearance of the header bar. */
    private final HeaderBarDecorator headerBarDecorator;
    
    /** Icon manager for categories. */
    private final CategoryIconManager categoryIconManager;
    
    /** Label that displays the search title. */
    private final JLabel searchTitleLabel = new JLabel();
    
    /** Panel containing filter components. */
    private final AdvancedFilterPanel<VisualSearchResult> filterPanel;
    
    /**
     * This is the subpanel that displays the actual search results.
     */
    private final ResultsContainer resultsContainer;
       
    /**
     * This is the subpanel that appears in the upper-right corner
     * of each search results tab.
     */
    private final SortAndFilterPanel sortAndFilterPanel;
    
    /** The scroll pane embedding the search results & sponsored results. */
    private JScrollPane scrollPane;
    
    /** The ScrollablePanel that the scroll pane is embedding. */
    private ScrollablePanel scrollablePanel;
       
    /** The panel for showing messages and a pointer to the classic search results view button. */
    private final SearchResultsMessagePanel messagePanel;
    
    /** This is the white gap which appears between the message panel and the search results */
    private Component messagePanelsGap;
    
    /** Listener for changes in the view type. */
    private final SettingListener viewTypeListener;
    
    /** Listener for updates to the result count. */
    private final ListEventListener<VisualSearchResult> resultCountListener;
    
    /** Search results data model. */
    private final SearchResultsModel searchResultsModel;
    
    @Resource private Color tabHighlightTopGradientColor;
    @Resource private Color tabHighlightBottomGradientColor;

    private boolean lifeCycleComplete = true;

    private boolean fullyConnected = true;

    private boolean receivedSponsoredResults = false;

    private boolean receivedSearchResults = false;
    
    private BrowseStatus browseStatus = null;
    
    /** Shows status of failed browses and refresh button.
     */
    private BrowseStatusPanel browseStatusPanel;
    
    /** Title when browsing friends; null for search results. */
    private String browseTitle;

    private final BrowseFailedMessagePanel browseFailedPanel;

    /** This class has a JXLayer as its sole component. The JXLayer has the search results components
        as its main panel and sometimes places overlays over these components. */
    private final JXLayer jxlayer;
    
    /** This is the active OverlayType for the JXLayer */
    private OverlayType overlayType = OverlayType.NONE;
    
    private SettingListener messagePanelGapHider;
    
    /**
     * Constructs a SearchResultsPanel with the specified components.
     */
    @Inject
    public SearchResultsPanel(
            @Assisted SearchResultsModel searchResultsModel,
            ResultsContainerFactory containerFactory,
            SortAndFilterPanelFactory sortAndFilterFactory,
            AdvancedFilterPanelFactory<VisualSearchResult> filterPanelFactory,
            HeaderBarDecorator headerBarDecorator,
            CategoryIconManager categoryIconManager, 
            BrowseFailedMessagePanelFactory browseFailedMessagePanelFactory,
            AllFriendsRefreshManager allFriendsRefreshManager) {
        super(new BorderLayout());
        
        GuiUtils.assignResources(this);
        
        this.searchResultsModel = searchResultsModel;
        this.headerBarDecorator = headerBarDecorator; 
        this.categoryIconManager = categoryIconManager;
        
        this.browseFailedPanel = browseFailedMessagePanelFactory.create(searchResultsModel);
        
        // Create sort and filter components.
        sortAndFilterPanel = sortAndFilterFactory.create(searchResultsModel);
        
        filterPanel = filterPanelFactory.create(searchResultsModel, searchResultsModel.getSearchType());
        
        scrollPane = new JScrollPane();
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollablePanel = new ScrollablePanel();
        configureEnclosingScrollPane();
        
        // Create results container with tables.
        resultsContainer = containerFactory.create(searchResultsModel);
        
        viewTypeListener = new SettingListener() {
            int oldSearchViewTypeId = SwingUiSettings.SEARCH_VIEW_TYPE_ID.getValue();
            @Override
            public void settingChanged(SettingEvent evt) {
               SwingUtilities.invokeLater(new Runnable() {
                   @Override
                   public void run() {
                       int newSearchViewTypeId = SwingUiSettings.SEARCH_VIEW_TYPE_ID.getValue();
                       if(newSearchViewTypeId != oldSearchViewTypeId) {
                           SearchViewType newSearchViewType = SearchViewType.forId(newSearchViewTypeId);
                           resultsContainer.setViewType(newSearchViewType);
                           syncScrollPieces();
                           oldSearchViewTypeId = newSearchViewTypeId;
                       }
                   }               
               });
            } 
        };
        SwingUiSettings.SEARCH_VIEW_TYPE_ID.addSettingListener(viewTypeListener);
        
        // Initialize header label.
        updateTitle();
        
        // Install listener to update header label.
        resultCountListener = new ListEventListener<VisualSearchResult>() {
            @Override
            public void listChanged(ListEvent<VisualSearchResult> listChanges) {
                // these updates are coming in on the AWT thread. so, it's safe to make GUI updates here.
                updateTitle();

                receivedSearchResults = (SearchResultsPanel.this.searchResultsModel.getUnfilteredList().size() > 0);

                updateMessages();
            }
        };
        searchResultsModel.getUnfilteredList().addListEventListener(resultCountListener);
        searchResultsModel.getFilteredList().addListEventListener(resultCountListener);
        
        // Configure sort panel and results container.
        sortAndFilterPanel.setSearchCategory(searchResultsModel.getSearchCategory());
        resultsContainer.showCategory(searchResultsModel.getSearchCategory());
        syncScrollPieces();
        
        // Configure advanced filters.
        filterPanel.setSearchCategory(searchResultsModel.getSearchCategory());
        filterPanel.addCategoryListener(new CategoryListener() {
            @Override
            public void categorySelected(SearchCategory displayCategory) {
                sortAndFilterPanel.setSearchCategory(displayCategory);
                resultsContainer.showCategory(displayCategory);
                syncScrollPieces();
                updateTitle();
            }
        });

        browseStatusPanel = new BrowseStatusPanel(searchResultsModel, allFriendsRefreshManager);
        
        messagePanel = new SearchResultsMessagePanel();
        
        // if the user closes the hint showing where the classic search view is by clicking on the hint's close button,
        // we need to hear about that in this class so that we can make the gap separating the message box from
        // the search results disappear
        if (messagePanel.isShowClassicSearchResultsHint()) {
            messagePanelGapHider = new SettingListener() {
                @Override
                public void settingChanged(SettingEvent evt) {
                    if (!SwingUiSettings.SHOW_CLASSIC_REMINDER.getValue()) {
                        messagePanelsGap.setVisible(false);
                    }
                }
            };
            SwingUiSettings.SHOW_CLASSIC_REMINDER.addSettingListener(messagePanelGapHider);
        }
        
        jxlayer = new JXLayer<JComponent>(createSearchResultsPanel());
        jxlayer.getGlassPane().setLayout(new BorderLayout());
        
        add(jxlayer, BorderLayout.CENTER);
    }
    
    /**
     * This method controls whether an overlay should be shown and if so, which type of overlay.
     * 
     * @param overlayType -- the overlay type. See the type definition for more info.
     */
    void setOverlayType(OverlayType overlayType) {
        // this method can be called multiple times for the same overlay type.
        // so, let's check to make sure that the overlay actually needs to change
        // before installing or uninstalling an overlay panel.
        if (this.overlayType != overlayType) {
            this.overlayType = overlayType;
            switch (overlayType) {
            case AWAITING_CONNECTIONS:
                installOverlay(new AwaitingConnectionsPanel(headerBarDecorator));
                break;
                
            case NO_FRIENDS_ON_LIMEWIRE:
                installOverlay(browseFailedPanel);
                break;
                
            case NONE:
                uninstallOverlay();
                break;
                
            default:
                throw new IllegalStateException("invalid type: " + overlayType); 
            }
        }
    }
          
    private void installOverlay(JComponent component) {
        jxlayer.getGlassPane().setVisible(false);
        jxlayer.getGlassPane().removeAll();
        jxlayer.getGlassPane().add(component);
        jxlayer.getGlassPane().setVisible(true);
    }
    
    private void uninstallOverlay() {
        jxlayer.getGlassPane().setVisible(false);
        jxlayer.getGlassPane().removeAll();
    }
    

    /**
     * Disposes of resources used by the container.  This method is called when 
     * the search is closed.
     */
    @Override
    public void dispose() {
        SwingUiSettings.SEARCH_VIEW_TYPE_ID.removeSettingListener(viewTypeListener);
        if (messagePanelGapHider != null) SwingUiSettings.SHOW_CLASSIC_REMINDER.removeSettingListener(messagePanelGapHider);
        searchResultsModel.getFilteredList().removeListEventListener(resultCountListener);
        searchResultsModel.getUnfilteredList().removeListEventListener(resultCountListener);
        sortAndFilterPanel.dispose();
        filterPanel.dispose();
        messagePanel.dispose();
        browseFailedPanel.dispose();
        searchResultsModel.dispose();
        browseStatusPanel.dispose();
    }
    
    /**
     * @return the SearchResultsModel of the SearchResultsPanel.
     */
    public SearchResultsModel getModel(){
        return searchResultsModel;
    }

    /**
     * Fills in the top right corner if a scrollbar appears with an empty table
     * header.
     */
    protected void configureEnclosingScrollPane() {
        JTableHeader th = new JTableHeader();
        th.setDefaultRenderer(new TableCellHeaderRenderer());
        // Put a dummy header in the upper-right corner.
        final Component renderer = th.getDefaultRenderer().getTableCellRendererComponent(null, "", false, false, -1, -1);
        JPanel cornerComponent = new JPanel(new BorderLayout());
        cornerComponent.add(renderer, BorderLayout.CENTER);
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerComponent);
    }
    
    /**
     * Sets the browse title in the container.  When not null, the browse title
     * is displayed at the top of the panel.  When null, the container displays 
     * the search title from the data model.
     */
    public void setBrowseTitle(String title) {
        browseTitle = title;
        updateTitle();
    }
    
    /**
     * Updates the title icon and text in the container.  For search results, 
     * the title includes the category name, search title, and result counts. 
     */
    private void updateTitle() {
        // Get result counts.
        int total = searchResultsModel.getUnfilteredList().size();
        int actual = searchResultsModel.getFilteredList().size();
        
        if (browseTitle != null) {
            // Set browse title.
            searchTitleLabel.setText((actual == total) ?
                    // {0}: browse title, {1}: total count
                    I18n.tr("Browse {0} ({1})", browseTitle, total) :
                    // {0}: browse title, {1}: actual count, {2}: total count 
                    I18n.tr("Browse {0} - Showing {1} of {2}", browseTitle, actual, total));
            
        } else {
            // Get search category and title.
            SearchCategory displayCategory = searchResultsModel.getSelectedCategory();
            String title = searchResultsModel.getSearchTitle();
            
            // Set title icon based on category.
            Icon icon = (displayCategory == SearchCategory.ALL) ? null :
                categoryIconManager.getIcon(displayCategory.getCategory());
            searchTitleLabel.setIcon(icon);

            // Set title text.
            switch (displayCategory) {
            case ALL:
                searchTitleLabel.setText((actual == total) ?
                        // {0}: search title, {1}: total count
                        I18n.tr("All results for {0} ({1})", title, total) :
                        // {0}: search title, {1}: actual count, {2}: total count 
                        I18n.tr("All results for {0} - Showing {1} of {2}", title, actual, total));
                break;
            case AUDIO:
                searchTitleLabel.setText((actual == total) ?
                        // {0}: search title, {1}: total count
                        I18n.tr("Audio results for {0} ({1})", title, total) :
                        // {0}: search title, {1}: actual count, {2}: total count 
                        I18n.tr("Audio results for {0} - Showing {1} of {2}", title, actual, total));
                break;
            case VIDEO:
                searchTitleLabel.setText((actual == total) ?
                        // {0}: search title, {1}: total count
                        I18n.tr("Video results for {0} ({1})", title, total) :
                        // {0}: search title, {1}: actual count, {2}: total count 
                        I18n.tr("Video results for {0} - Showing {1} of {2}", title, actual, total));
                break;
            case IMAGE:
                searchTitleLabel.setText((actual == total) ?
                        // {0}: search title, {1}: total count
                        I18n.tr("Image results for {0} ({1})", title, total) :
                        // {0}: search title, {1}: actual count, {2}: total count 
                        I18n.tr("Image results for {0} - Showing {1} of {2}", title, actual, total));
                break;
            case DOCUMENT:
                searchTitleLabel.setText((actual == total) ?
                        // {0}: search title, {1}: total count
                        I18n.tr("Document results for {0} ({1})", title, total) :
                        // {0}: search title, {1}: actual count, {2}: total count 
                        I18n.tr("Document results for {0} - Showing {1} of {2}", title, actual, total));
                break;
            case PROGRAM:
                searchTitleLabel.setText((actual == total) ?
                        // {0}: search title, {1}: total count
                        I18n.tr("Program results for {0} ({1})", title, total) :
                        // {0}: search title, {1}: actual count, {2}: total count 
                        I18n.tr("Program results for {0} - Showing {1} of {2}", title, actual, total));
                break;
            case OTHER:
                searchTitleLabel.setText((actual == total) ?
                        // {0}: search title, {1}: total count
                        I18n.tr("Other results for {0} ({1})", title, total) :
                        // {0}: search title, {1}: actual count, {2}: total count 
                        I18n.tr("Other results for {0} - Showing {1} of {2}", title, actual, total));
                break;
            case TORRENT:
                searchTitleLabel.setText((actual == total) ?
                        // {0}: search title, {1}: total count
                        I18n.tr("Torrent results for {0} ({1})", title, total) :
                        // {0}: search title, {1}: actual count, {2}: total count 
                        I18n.tr("Torrent results for {0} - Showing {1} of {2}", title, actual, total));
                break;
            default:
                throw new IllegalStateException("Invalid search category " + displayCategory);
            }
        }
    }
    
    /**
     * Updates the column header component in the scroll pane.  This depends on
     * the current results view and whether the sponsored results are visible. 
     */
    private void syncColumnHeader() {
        Component resultHeader = resultsContainer.getScrollPaneHeader();
        scrollPane.setColumnHeaderView(resultHeader);
        
        scrollPane.validate();
        
        // Resize and repaint table header.  This eliminates visual issues due
        // to a change in the table format, which can result in an incorrect
        // header height or header flickering when a category is selected.
        if (resultHeader instanceof JTableHeader) {
            ((JTableHeader) resultHeader).resizeAndRepaint();
        }
    }
    
    /**
     * Initializes the components and adds them to the container.  Called by
     * the constructor.  
     */
    private JPanel createSearchResultsPanel() {
        JPanel searchResultsComponentsPanel = new JPanel( new MigLayout("hidemode 2, insets 0 0 0 0, gap 0!, novisualpadding", 
                                                                        "[][grow]",        // col constraints
                                                                        "[][][][grow]") ); // row constraints
        
        searchResultsComponentsPanel.setMinimumSize(new Dimension(searchResultsComponentsPanel.getPreferredSize().width, 33));
        
        RectanglePainter tabHighlight = new RectanglePainter();
        tabHighlight.setFillPaint(new GradientPaint(20.0f, 0.0f, tabHighlightTopGradientColor, 
                                                    20.0f, 33.0f, tabHighlightBottomGradientColor));

        tabHighlight.setInsets(new Insets(0,0,1,0));
        tabHighlight.setBorderPaint(null);
        
        HeaderBar header = new HeaderBar(searchTitleLabel);
        header.setLayout(new MigLayout("insets 0, gap 0!, novisualpadding, alignx 100%, aligny 50%"));
        header.add(browseStatusPanel, "alignx 0%, growx, pushx");
        headerBarDecorator.decorateBasic(header);
        
        sortAndFilterPanel.layoutComponents(header);
        
        searchResultsComponentsPanel.add(header, "spanx 2, growx, growy, wrap");
        searchResultsComponentsPanel.add(filterPanel, "grow, spany 3");
        searchResultsComponentsPanel.add(messagePanel, "spanx 1, growx, wrap");
        messagePanelsGap = Box.createVerticalStrut(6);
        searchResultsComponentsPanel.add(messagePanelsGap, "hidemode 3, spanx 1, growx, wrap");
        searchResultsComponentsPanel.add(scrollPane , "hidemode 3, grow, spany");

        scrollablePanel.setScrollableTracksViewportHeight(false);

        scrollablePanel.setLayout(new BorderLayout());
        scrollablePanel.add(resultsContainer, BorderLayout.CENTER);
        scrollPane.setViewportView(scrollablePanel);
        
        syncScrollPieces();
        
        return searchResultsComponentsPanel;
    }
    
    /**
     * Updates the view components in the scroll pane. 
     */
    private void syncScrollPieces() {
        scrollablePanel.setScrollable(resultsContainer.getScrollable());
        syncColumnHeader();
    }
    
    /**
     * Panel used as the viewport view in the scroll pane.  This contains the
     * results table and sponsored results panel in a single, scrollable area.
     */
    private class ScrollablePanel extends JXPanel {
        private Scrollable scrollable;

        public void setScrollable(Scrollable scrollable) {
            this.scrollable = scrollable;
        }
        
        @Override
        public Dimension getPreferredSize() {
            if(scrollable == null) {
                return super.getPreferredSize();
            } else {
                int width = super.getPreferredSize().width;
                int height = ((JComponent)scrollable).getPreferredSize().height;
                
                // the list view has some weird rendering sometimes (double space after last result)
                // so don't fill full screen on list view
                if(! (scrollable instanceof ListViewTable)) {
                    int headerHeight = 0;
                    
                    //the table headers aren't being set on the scrollpane, so if its visible check its
                    // height and subtract it from the viewport size
                    JTableHeader header = ((JTable)scrollable).getTableHeader();
                    if(header != null && header.isShowing()) {
                        headerHeight = header.getHeight();
                    }
                    
                    // if the height of table is less than the scrollPane height, set preferred height
                    // to same size as scrollPane
                    if(height < scrollPane.getSize().height - headerHeight) {
                        height = scrollPane.getSize().height - headerHeight;
                    }
                }
                return new Dimension(width, height);
            }
        }
        
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            if(scrollable == null) {
                return super.getScrollableUnitIncrement(visibleRect, orientation, direction);
            } else {
                return scrollable.getScrollableUnitIncrement(visibleRect, orientation, direction);
            }
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            if(scrollable == null) {
                return super.getScrollableBlockIncrement(visibleRect, orientation, direction);
            } else {
                return scrollable.getScrollableBlockIncrement(visibleRect, orientation, direction);
            }
        }
    }

    /**
     * Sets an indicator to determine whether the application has finished
     * loading, and updates the user message.
     */
    public void setLifeCycleComplete(boolean lifeCycleComplete) {
        this.lifeCycleComplete = lifeCycleComplete;
        updateMessages();
    }

    /**
     * Sets an indicator to determine whether the application is fully 
     * connected to the P2P Network, and updates the user message.
     */
    public void setFullyConnected(boolean fullyConnected) {
        this.fullyConnected = fullyConnected;
        updateMessages();        
    }
    
    public void setBrowseStatus(BrowseStatus browseStatus){
        this.browseStatus = browseStatus;
        updateMessages();        
    }
    
    /**
     * Updates the user message based on the current state of the application. 
     */
    private void updateMessages() {
        browseStatusPanel.setBrowseStatus(browseStatus);

        // let's check whether we need to show the user any messages
        if (!fullyConnected && (receivedSearchResults || receivedSponsoredResults)) {
            messagePanel.setMessageType(MessageType.CONNECTING_TO_ULTRAPEERS);
            messagePanelsGap.setVisible(true);
        } else if (fullyConnected && receivedSearchResults && messagePanel.isShowClassicSearchResultsHint()) {
            messagePanel.setMessageType(MessageType.CLASSIC_SEARCH_RESULTS_HINT);            
            messagePanelsGap.setVisible(true);
        } else {
            messagePanel.setMessageType(MessageType.NONE);
            messagePanelsGap.setVisible(false);
        }
       
        // let's check whether we need to put an overlay over the search results panel
        if ( (!lifeCycleComplete || !fullyConnected ) && (!receivedSearchResults && !receivedSponsoredResults) ) {
            setOverlayType(OverlayType.AWAITING_CONNECTIONS);
        } else if (browseStatus != null && !browseStatus.getState().isOK()) {
            browseFailedPanel.update(browseStatus.getState(), browseStatus.getBrowseSearch(), browseStatus.getFailedFriends());
            setOverlayType(OverlayType.NO_FRIENDS_ON_LIMEWIRE);           
        } else {
            setOverlayType(OverlayType.NONE);            
        }
    }
}
