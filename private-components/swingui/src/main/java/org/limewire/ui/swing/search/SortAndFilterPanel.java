package org.limewire.ui.swing.search;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.LimeComboBox.SelectionListener;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.painter.ButtonBackgroundPainter.DrawMode;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.SortOption;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * This class manages the UI components for filtering and sorting
 * search results.
 * 
 * @see org.limewire.ui.swing.search.SearchResultsPanel
 */
public class SortAndFilterPanel implements Disposable {

    private final ButtonDecorator buttonDecorator;

    /** Search results data model. */
    private final SearchResultsModel searchResultsModel;
    
    /** Map of sort options and actions. */ 
    private final Map<SortOption, Action> actionMap = new EnumMap<SortOption, Action>(SortOption.class); 

    @Resource private Icon listViewIcon;
    @Resource private Icon tableViewIcon;
    
    @Resource private Font sortLabelFont;

    private final LimeComboBox sortCombo;
    
    private final JLabel sortLabel = new JLabel(tr("Sort by:"));
    private final JXButton listViewToggleButton = new JXButton();
    private final JXButton tableViewToggleButton = new JXButton();
    
    private SortOption sortBy;
    
    private boolean repopulatingCombo;
    private SettingListener viewTypeListener;

    /**
     * Constructs a SortAndFilterPanel with the specified search results data
     * model and UI decorators.
     */
    @Inject
    SortAndFilterPanel(
            @Assisted SearchResultsModel searchResultsModel,
            ComboBoxDecorator comboBoxDecorator, 
            ButtonDecorator buttonDecorator) {
        
        GuiUtils.assignResources(this);
        
        this.buttonDecorator = buttonDecorator;
        this.searchResultsModel = searchResultsModel;
        
        // Initialize sort actions.
        populateActionList();
        
        // Initialize sort components.
        sortLabel.setFont(sortLabelFont);
        sortLabel.setForeground(Color.WHITE);
        
        sortCombo = new LimeComboBox();
        comboBoxDecorator.decorateDarkFullComboBox(sortCombo);
        sizeSortCombo();

        // Initialize components to select view type.
        listViewToggleButton.setModel(new JToggleButton.ToggleButtonModel());
        tableViewToggleButton.setModel(new JToggleButton.ToggleButtonModel());
        setSearchCategory(searchResultsModel.getSearchCategory());
        configureViewButtons();
        
        // Initialize sorting and filtering.
        configureSortFilter();
    }

    /**
     * Removes listeners to external resources. 
     */
    @Override
    public void dispose() {
        SwingUiSettings.SEARCH_VIEW_TYPE_ID.removeSettingListener(viewTypeListener);
    }

    /**
     * Updates the size of the sort combobox based on the display length of the
     * current sort options.
     */
    private void sizeSortCombo() {
        int widestActionText = 0;
        FontMetrics sortFontMetrics = sortCombo.getFontMetrics(sortCombo.getFont());
        
        for (SortOption sortOption : actionMap.keySet()) {
            widestActionText = Math.max(widestActionText, 
                    sortFontMetrics.stringWidth(getDisplayName(sortOption)));
        }
        
        //Width of text plus padding for the whitespace around the text and the drop down icon
        Dimension sortComboDimensions = new Dimension(widestActionText + 30, sortCombo.getPreferredSize().height);
        sortCombo.setPreferredSize(sortComboDimensions);
        sortCombo.setMinimumSize(sortComboDimensions);
        sortCombo.setMaximumSize(sortComboDimensions);
    }

    /**
     * Initializes the collection of sort actions. 
     */
    private void populateActionList() {
        for (SortOption sortOption : SortOption.values()) {
            actionMap.put(sortOption, new SortAction(sortOption));
        }
    }

    /**
     * Configures the UI components used to select the view type.
     */
    private void configureViewButtons() {

        buttonDecorator.decorateDarkFullImageButton(listViewToggleButton, DrawMode.LEFT_ROUNDED);
        listViewToggleButton.setIcon(listViewIcon);
        listViewToggleButton.setPressedIcon(listViewIcon);
        listViewToggleButton.setToolTipText(tr("List view"));
        listViewToggleButton.setMargin(new Insets(0, 10, 0, 6));

        listViewToggleButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    SwingUiSettings.SEARCH_VIEW_TYPE_ID.setValue(SearchViewType.LIST.getId());
                    if (searchResultsModel.getSearchCategory() == SearchCategory.TORRENT) {
                        SwingUiSettings.TORRENT_SEARCH_VIEW_TYPE_ID.setValue(SearchViewType.LIST.getId());
                    }
                    selectListView();
                }
            }
        });

        buttonDecorator.decorateDarkFullImageButton(tableViewToggleButton, DrawMode.RIGHT_ROUNDED);
        tableViewToggleButton.setIcon(tableViewIcon);
        tableViewToggleButton.setPressedIcon(tableViewIcon);
        tableViewToggleButton.setToolTipText(tr("Classic view"));
        tableViewToggleButton.setMargin(new Insets(0, 6, 0, 10));
        
        tableViewToggleButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    SwingUiSettings.SEARCH_VIEW_TYPE_ID.setValue(SearchViewType.TABLE.getId());
                    SwingUiSettings.SHOW_CLASSIC_REMINDER.setValue(false);
                    if (searchResultsModel.getSearchCategory() == SearchCategory.TORRENT) {
                        SwingUiSettings.TORRENT_SEARCH_VIEW_TYPE_ID.setValue(SearchViewType.TABLE.getId());
                    }
                    selectTableView();
                }
            }
        });

        viewTypeListener = new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        int newViewTypeId = SwingUiSettings.SEARCH_VIEW_TYPE_ID.getValue();
                        SearchViewType newSearchViewType = SearchViewType.forId(newViewTypeId);
                        updateView(newSearchViewType);                        
                    }
                });
            }
        };
        SwingUiSettings.SEARCH_VIEW_TYPE_ID.addSettingListener(viewTypeListener);
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(listViewToggleButton);
        viewGroup.add(tableViewToggleButton);
        
        updateView(SearchViewType.getSearchViewType(searchResultsModel.getSearchCategory()));
    }

    /**
     * Updates the UI components based on the specified view type.
     */
    private void updateView(SearchViewType newSearchViewType) {
        switch (newSearchViewType) {
        case LIST:
            selectListView();
            break;
        case TABLE:
            selectTableView();
            break;
        }
    }
    
    /**
     * Updates the UI components when the List view is selected. 
     */
    private void selectListView() {
        tableViewToggleButton.setSelected(false);
        listViewToggleButton.setSelected(true);
        sortLabel.setVisible(true);
        sortCombo.setVisible(true);
    }

    /**
     * Updates the UI components when the Table view is selected. 
     */
    private void selectTableView() {
        tableViewToggleButton.setSelected(true);
        listViewToggleButton.setSelected(false);
        sortLabel.setVisible(false);
        sortCombo.setVisible(false);
    }

    /**
     * Configures the sort and filter components to work with the data model.
     */
    private void configureSortFilter() {
        // Initialize sort option in data model.
        searchResultsModel.setSortOption(SortOption.getDefault());
        
        // Install combobox listener to update sort order.
        SelectionListener listener = new SelectionListener() {
            @Override
            public void selectionChanged(Action action) {
                SortOption option = ((SortAction) action).getSortOption();
                if (!repopulatingCombo && !option.equals(sortBy)) {
                    // changing sort order
                    searchResultsModel.setSortOption(option);
                    sortBy = option;
                }
            }
        };
        sortCombo.addSelectionListener(listener);
        
        // Trigger the initial sort.
        sortCombo.setSelectedAction(actionMap.get(SortOption.getDefault()));
    }
    
    /**
     * Adds the sorting and filtering components to the specified panel.
     */
    public void layoutComponents(JPanel panel) {        
        panel.add(sortLabel, "gapafter 5, gapbottom 2, hidemode 0");
        panel.add(sortCombo, "gapafter 10, hidemode 0");
        panel.add(listViewToggleButton, "right");
        panel.add(tableViewToggleButton, "gapafter 5");
    }

    /**
     * Clears the filter text field.
     *
    public void clearFilterBox() {
        filterBox.setText("");
    }*/
    
    /**
     * Sets the state of the view toggle buttons.
     * @param mode the current mode ... LIST or TABLE
     */
    public void setMode(SearchViewType mode) {
        if (mode == SearchViewType.LIST) {
            listViewToggleButton.setSelected(true);
            tableViewToggleButton.setSelected(false);
        } else if (mode == SearchViewType.TABLE) {
            listViewToggleButton.setSelected(false);
            tableViewToggleButton.setSelected(true);
        }
    }

    /**
     * Sets the search category, and updates the sort combobox with the 
     * appropriate list of sort selections.
     */
    public void setSearchCategory(SearchCategory category) {
        Action currentItem = sortCombo.getSelectedAction();
        boolean currentValid = false;
        
        repopulatingCombo = true;
        sortCombo.removeAllActions();
        
        // Get sort options for category.
        SortOption[] options = SortOption.getSortOptions(category);

        // Create list of sort actions.  We also determine if the current
        // sort action is valid for the new category.
        List<Action> actionList = new LinkedList<Action>();
        for (SortOption option : options) {
            actionList.add(actionMap.get(option));
            if (actionMap.get(option).equals(currentItem)) {
                currentValid = true;
            }
        }
        
        sortCombo.addActions(actionList);
        
        sizeSortCombo();

        repopulatingCombo = false;

        // Set combobox to current action if valid.  Otherwise, set search 
        // model to use first sort option.
        if (currentValid) {
            sortCombo.setSelectedAction(currentItem);
        } else {
            searchResultsModel.setSortOption(options[0]);
            sortBy = options[0];
        }
    }

    /**
     * Returns the display name for the specified SortOption.
     */
    private String getDisplayName(SortOption sortOption) {
        switch (sortOption) {
        case COMPANY: return tr("Company");
        case PLATFORM: return tr("Platform");
        case TYPE: return tr("Type");
        case DATE_CREATED: return tr("Date Created");
        case QUALITY: return tr("Quality");
        case YEAR: return tr("Year");
        case FILE_EXTENSION: return tr("File Extension");
        case TITLE: return tr("Title");
        case LENGTH: return tr("Length");
        case ALBUM: return tr("Album");
        case ARTIST: return tr("Artist");
        case SIZE_LOW_TO_HIGH: return tr("Size (low to high)");
        case SIZE_HIGH_TO_LOW: return tr("Size (high to low)");
        case CATEGORY: return tr("Category");
        case NAME: return tr("Name");
        case RELEVANCE_ITEM: return tr("Relevance");
        default: return sortOption.name();
        }
    }

    /**
     * An Action implementation for a sort option.
     */
    private class SortAction extends AbstractAction {
        private final SortOption sortOption;
        
        public SortAction(SortOption sortOption) {
            super(getDisplayName(sortOption));
            this.sortOption = sortOption;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        }

        public SortOption getSortOption() {
            return sortOption;
        }
    }
}
