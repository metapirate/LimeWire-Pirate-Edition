package org.limewire.ui.swing.search.advanced;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.advanced.AbstractTabItem;
import org.limewire.ui.swing.advanced.TabButton;
import org.limewire.ui.swing.components.PopupWindow;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.options.TabItemListener;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.search.KeywordAssistedSearchBuilder;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Display container for the Advanced Search popup dialog.  This is shown when
 * the user selects the Advanced search category.
 */
public class AdvancedPopupPanel extends JXPanel {
    /** Defines the tabs for the popup. */
    private enum TabId {
        AUDIO(I18n.tr("Audio"), SearchCategory.AUDIO), 
        VIDEO(I18n.tr("Video"), SearchCategory.VIDEO);
        
        private final String name;
        private final SearchCategory category;
        
        TabId(String name, SearchCategory category) {
            this.name = name;
            this.category = category;
        }
        
        public SearchCategory getCategory() {
            return category;
        }
        
        public String getName() {
            return name;
        }
    }
    
    @Resource private Color background;
    @Resource private Color borderColor;
    @Resource private Color tabTopColor;
    @Resource private Color tabBottomColor;
    @Resource private Color tabForeground;
    @Resource private Font tabFont;
    @Resource private Icon audioTabIcon;
    @Resource private Icon videoTabIcon;
    
    private final AdvancedPanelFactory advancedPanelFactory;
    private final ButtonDecorator buttonDecorator;
    private final Provider<KeywordAssistedSearchBuilder> advancedSearchBuilder;
    private final SearchHandler searchHandler;
    
    private final Map<TabId, AdvancedSearchTabItem> tabItemMap = 
        new EnumMap<TabId, AdvancedSearchTabItem>(TabId.class);
    
    private Action nextTabAction = new NextTabAction();
    private Action prevTabAction = new PrevTabAction();
    private Action searchAction = new SearchAction();
    
    private AdvancedSearchTabItem selectedItem;
    
    private JXPanel headerPanel;
    private JPanel cardPanel;
    private CardLayout cardLayout;
    
    private JPanel buttonPanel;
    private JXButton searchButton;
    
    @Inject
    public AdvancedPopupPanel(BarPainterFactory barPainterFactory,
            AdvancedPanelFactory advancedPanelFactory,
            ButtonDecorator buttonDecorator,
            Provider<KeywordAssistedSearchBuilder> advancedSearchBuilder,
            SearchHandler searchHandler) {
        this.advancedPanelFactory = advancedPanelFactory;
        this.buttonDecorator = buttonDecorator;
        this.advancedSearchBuilder = advancedSearchBuilder;
        this.searchHandler = searchHandler;
        
        // Inject annotated resource values.
        GuiUtils.assignResources(this);

        // Initialize components.
        initComponents(barPainterFactory);
        
        // Add tabs to popup.
        addTab(TabId.AUDIO, audioTabIcon);
        addTab(TabId.VIDEO, videoTabIcon);
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initComponents(BarPainterFactory barPainterFactory) {
        setBorder(BorderFactory.createLineBorder(borderColor));
        setLayout(new BorderLayout());
        
        // Create header panel to hold tab buttons.
        headerPanel = new JXPanel();
        headerPanel.setBackgroundPainter(barPainterFactory.createTopBarPainter());
        headerPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0",
                "",                  // col constraints
                "align top,fill"));  // row constraints
        
        // Create panel to hold tab content panels.
        cardLayout = new CardLayout();
        cardPanel = new JPanel();
        cardPanel.setBackground(background);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 6));
        cardPanel.setLayout(cardLayout);
        cardPanel.setOpaque(true);
        
        // Create search button.
        searchButton = new JXButton(searchAction);
        buttonDecorator.decorateGreenFullButton(searchButton);
        
        buttonPanel = new JPanel();
        buttonPanel.setBackground(background);
        buttonPanel.setLayout(new MigLayout("insets 0 12 12 12, gap 0"));
        buttonPanel.setOpaque(true);
        buttonPanel.add(searchButton, "pushx, alignx right");
        
        add(headerPanel, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Adds a tab to the dialog using the specified tab identifier and icon.
     */
    private void addTab(TabId tabId, Icon icon) {
        // Create tab item and add to map.
        AdvancedSearchTabItem tabItem = new AdvancedSearchTabItem(tabId);
        tabItemMap.put(tabId, tabItem);
        
        // Add button to header.
        headerPanel.add(createButton(tabItem, icon));
    }
    
    /**
     * Creates a tab button for the specified tab item and icon.
     */
    private JButton createButton(AdvancedSearchTabItem tabItem, Icon icon) {
        TabButton button = new TabButton(new TabAction(tabItem, icon));
        
        // Adjust button size.
        button.setPreferredSize(new Dimension(54, 60));
        button.setBorder(BorderFactory.createEmptyBorder());
        
        // Set gradient colors.
        button.setGradients(tabTopColor, tabBottomColor);
        button.setForeground(tabForeground);
        button.setFont(tabFont);
        
        // Add inputs and action to select previous tab.
        button.getActionMap().put(PrevTabAction.KEY, prevTabAction);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), PrevTabAction.KEY);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), PrevTabAction.KEY);
        
        // Add inputs and action to select next tab.
        button.getActionMap().put(NextTabAction.KEY, nextTabAction);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NextTabAction.KEY);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), NextTabAction.KEY);
        
        return button;
    }
    
    /**
     * Creates a new popup window to display the container.
     */
    private PopupWindow createPopup(JComponent invoker, int x, int y) {
        // Determine popup location on screen.
        Point invokerLoc = invoker.getLocationOnScreen();
        Point location = new Point(invokerLoc.x + x, invokerLoc.y + y);
        
        // Create popup containing this panel.
        return PopupWindow.createPopupWindow(invoker, this, location);
    }
    
    /**
     * Closes the popup window.
     */
    private void disposePopup() {
        Container ancestor = getTopLevelAncestor();
        if (ancestor instanceof Window) {
            ((Window) ancestor).setVisible(false);
        }
    }
    
    /**
     * Returns the TabId of the next tab after or before the specified tab.
     * The <code>forward</code> argument specifies direction: true for next
     * tab, false for previous tab.
     */
    private TabId findNextTab(TabId tabId, boolean forward) {
        // Get index for tab item.
        int index = tabId.ordinal();
        
        // Get array of TabId values.
        TabId[] tabIds = TabId.values();
        
        // Get normalized index of next/previous tab.
        int nextTab = (index + (forward ? 1 : -1) + tabIds.length) % tabIds.length;
        
        // Return TabId.
        return tabIds[nextTab];
    }
    
    /**
     * Selects the tab item with the specified identifier.
     * @param tabId the identifier of the tab
     */
    private void select(TabId tabId) {
        // De-select current item.
        if (selectedItem != null) {
            selectedItem.fireSelected(false);
        }
        
        // Set new selected item.
        selectedItem = tabItemMap.get(tabId);

        // Get tab panel, and add to container if necessary.
        final AdvancedPanel tabPanel = selectedItem.getTabPanel();
        if (!cardPanel.isAncestorOf(tabPanel)) {
            tabPanel.setBackground(background);
            cardPanel.add(tabPanel, tabId.getName());
        }

        // Display selected tab panel.
        cardLayout.show(cardPanel, tabId.getName());
        
        // Fire event to select tab.
        selectedItem.fireSelected(true);
        
        // Post an event to request focus on first input field.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tabPanel.requestFocusFirstComponent();
            }
        });
    }
    
    /**
     * Displays this panel in a popup window at the (x, y) position relative
     * to the specified invoker.
     */
    public void showPopup(JComponent invoker, int x, int y, int width) {
        
        // Select tab.
        if (selectedItem == null) {
            select(TabId.AUDIO);
        }
        
        // Adjust container to desired width.
        setPreferredSize(new Dimension(width, getPreferredSize().height));
        
        // Create popup and display.
        createPopup(invoker, x, y).setVisible(true);
    }
    
    /**
     * A tab item for the Advanced Search popup. 
     */
    private class AdvancedSearchTabItem extends AbstractTabItem {
        private final TabId tabId;
        private AdvancedPanel tabPanel;

        public AdvancedSearchTabItem(TabId tabId) {
            this.tabId = tabId;
        }
        
        @Override
        public String getId() {
            return tabId.getName();
        }

        @Override
        public void select() {
            AdvancedPopupPanel.this.select(tabId);
        }
        
        public TabId getTabId() {
            return tabId;
        }
        
        public AdvancedPanel getTabPanel() {
            if (tabPanel == null) {
                tabPanel = advancedPanelFactory.create(tabId.getCategory(), searchAction);
            }
            return tabPanel;
        }
    }
    
    /**
     * An Action associated with a tab button.  This updates the "selected" 
     * value when its associated tab item is selected or de-selected.
     */
    private class TabAction extends AbstractAction {
        private final TabId tabId;

        public TabAction(AdvancedSearchTabItem tabItem, Icon icon) {
            super(tabItem.getTabId().getName(), icon);
            
            // Store tab identifier and action command.
            tabId = tabItem.getTabId();
            putValue(ACTION_COMMAND_KEY, tabId.getName());

            // Install listener to handle tab item selection. 
            tabItem.addTabItemListener(new TabItemListener() {
                @Override
                public void itemSelected(boolean selected) {
                    putValue(SELECTED_KEY, selected);
                }
            });
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            select(tabId);
        }
    }
    
    /**
     * An Action that handles events to select the next tab.
     */
    private class NextTabAction extends AbstractAction {
        final static String KEY = "NEXT";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            select(findNextTab(selectedItem.getTabId(), true));
        }
    }
    
    /**
     * An Action that handles events to select the previous tab.
     */
    private class PrevTabAction extends AbstractAction {
        final static String KEY = "PREV";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            select(findNextTab(selectedItem.getTabId(), false));
        }
    }
    
    /**
     * An Action that handles events to select the previous tab.
     */
    private class SearchAction extends AbstractAction {
        
        public SearchAction() {
            super(I18n.tr("Search"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get advanced search values.
            AdvancedPanel visiblePanel = selectedItem.getTabPanel();
            Map<FilePropertyKey, String> searchData = visiblePanel.getSearchData();
            
            // Close popup.
            disposePopup();
            
            // Start search if any fields filled in.
            if (searchData != null) {
                SearchInfo info = advancedSearchBuilder.get().createAdvancedSearch(
                        searchData, visiblePanel.getSearchCategory());
                searchHandler.doSearch(info);
            }
        }
    }
}
