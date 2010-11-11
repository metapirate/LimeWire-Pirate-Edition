package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.effect.LayerEffect;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableColumnExt;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.inject.LazySingleton;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.dnd.LocalFileListTransferHandler;
import org.limewire.ui.swing.library.LibraryFilterPanel.LibraryCategoryListener;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.library.sharing.LibrarySharingAction;
import org.limewire.ui.swing.library.sharing.LibrarySharingPanel;
import org.limewire.ui.swing.library.table.AbstractLibraryFormat;
import org.limewire.ui.swing.library.table.LibraryImageTable;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.player.PlayerControlPanelFactory;
import org.limewire.ui.swing.player.PlayerMediator;
import org.limewire.ui.swing.player.PlayerMediatorListener;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingHacks;
import org.limewire.util.OSUtils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class LibraryPanel extends JPanel {

    @Resource private Icon plusIcon;
    @Resource private Font fileCountFont;
    @Resource private Color fileCountColor;
    @Resource private Color tableOverlayColor;
    
    private static final String TABLE = "TABLE";
    private static final String LIST = "LIST";
    
    private final HeaderBar headerBar = new HeaderBar();
    private final LockableUI lockableUI;
    private final LibraryTable libraryTable;
    private final LibraryNavigatorPanel libraryNavigatorPanel;
    private final LibrarySharingPanel librarySharingPanel;
    private final PublicSharedFeedbackPanel publicSharedFeedbackPanel;
    private final LibraryFilterPanel libraryFilterPanel;
    private final ButtonDecorator buttonDecorator;
    private final LocalFileListTransferHandler transferHandler;
    private final Provider<LibraryImageTable> libraryImagePanelProvider;
    private LibraryImageTable libraryImagePanel;
    
    private JPanel tableListPanel;
    private JScrollPane libraryScrollPane;
    private CardLayout tableListLayout;
    
    private JXButton addFilesButton;
    private JLabel fileCountLabel;
    private JXButton filterToggleButton;
    
    private Category selectedCategory;
    private EventList<LocalFileItem> eventList;
    private FilterList<LocalFileItem> filteredList;
    private FilterList<LocalFileItem> textFilterList;
    private FileCountListener fileCountListener;
    
    private LibraryNavItem selectedNavItem;
    
    @Inject
    public LibraryPanel(LibraryNavigatorPanel navPanel, HeaderBarDecorator headerBarDecorator, LibraryTable libraryTable,
            LibrarySharingPanel sharingPanel, LibraryFilterPanel libraryFilterPanel,
            PublicSharedFeedbackPanel publicSharedFeedbackPanel, PlayerControlPanelFactory playerPanel, AddFileAction addFileAction,
            ButtonDecorator buttonDecorator, LibraryTransferHandler transferHandler,
            Provider<LibraryImageTable> libraryImagePanelProvider,
            LibrarySharingAction libraryAction) {
        super(new MigLayout("insets 0, gap 0, fill"));
        
        this.libraryNavigatorPanel = navPanel;
        this.libraryTable = libraryTable;
        this.librarySharingPanel = sharingPanel;
        this.libraryFilterPanel = libraryFilterPanel;
        this.publicSharedFeedbackPanel = publicSharedFeedbackPanel;
        this.buttonDecorator = buttonDecorator;
        this.transferHandler = transferHandler;
        this.libraryImagePanelProvider = libraryImagePanelProvider;
        this.fileCountListener = new FileCountListener();
        
        GuiUtils.assignResources(this);
        this.lockableUI = new LockedUI();
        
        layoutComponents(headerBarDecorator, playerPanel.createAudioControlPanel(), addFileAction, libraryAction);

        eventList = new BasicEventList<LocalFileItem>();
        selectTable(libraryFilterPanel.getSelectedTableFormat(), libraryFilterPanel.getSelectedCategory());
    }
    
    private void layoutComponents(HeaderBarDecorator headerBarDecorator, JComponent playerPanel, AddFileAction addFileAction, LibrarySharingAction libraryAction) {
        headerBarDecorator.decorateBasic(headerBar);
        
        createAddFilesButton(addFileAction, libraryAction);
        createFilterToggleButton();
        
        fileCountLabel = new JLabel();
        fileCountLabel.setForeground(fileCountColor);
        fileCountLabel.setFont(fileCountFont);

        headerBar.setLayout(new MigLayout("insets 0 5 1 5, gap 0, fill"));
        headerBar.add(addFilesButton, "push");
        headerBar.add(playerPanel, "pos 0.5al 0.5al");
        headerBar.add(fileCountLabel, "gapafter 5, pad -2 0 0 0");
        headerBar.add(filterToggleButton);
                
        tableListLayout = new CardLayout();
        tableListPanel = new JPanel(tableListLayout);
        
        libraryTable.setTransferHandler(transferHandler);
        SwingHacks.fixDnDforKDE(libraryTable);
        
        libraryScrollPane = new JScrollPane(libraryTable);
        libraryScrollPane.setBorder(BorderFactory.createEmptyBorder());  
        configureEnclosingScrollPane(libraryScrollPane);

        JXLayer<JComponent> layer = new JXLayer<JComponent>(libraryScrollPane, lockableUI);
        tableListPanel.add(layer, TABLE);
        
        setupHighlighters();
        
        add(libraryNavigatorPanel, "dock west, growy");
        add(headerBar, "dock north, growx");
        add(libraryFilterPanel.getComponent(), "dock north, growx, hidemode 3");
        add(librarySharingPanel.getComponent(), "dock west, growy, hidemode 3");
        add(tableListPanel, "grow");
        add(publicSharedFeedbackPanel.getComponent(), "dock south, growx, hidemode 3");    
    }
    
    /**
     * Fills in the top right corner if a scrollbar appears with an empty table
     * header.
     */
    protected void configureEnclosingScrollPane(JScrollPane scrollPane) {
        JTableHeader th = new JTableHeader();
        th.setDefaultRenderer(new TableCellHeaderRenderer());
        // Put a dummy header in the upper-right corner.
        final Component renderer = th.getDefaultRenderer().getTableCellRendererComponent(null, "", false, false, -1, -1);
        JPanel cornerComponent = new JPanel(new BorderLayout());
        cornerComponent.add(renderer, BorderLayout.CENTER);
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerComponent);
    }
    
    @Inject
    void register(LibraryManager libraryManager, final PlayerMediator playerMediator) {        
        //Loads the Library after Component has been realized.
        final LibraryFileList libraryList = libraryManager.getLibraryManagedList();
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                selectedNavItem = libraryNavigatorPanel.getSelectedNavItem();
                eventList = libraryList.getSwingModel();
                selectTable(libraryFilterPanel.getSelectedTableFormat(), libraryFilterPanel.getSelectedCategory());
                configureEnclosingScrollPane(libraryScrollPane);
            }
        });        
        // listen for selection changes in the combo box and filter the table
        // replacing the table header as you do.
        libraryFilterPanel.addSearchTabListener(new LibraryCategoryListener(){

            @Override
            public void categorySelected(Category category) {
            	// only update the table if the category has changed
                if(category != getSelectedCategory()) {
                    selectTable(libraryFilterPanel.getSelectedTableFormat(), libraryFilterPanel.getSelectedCategory());
                }
            }
        });
        libraryNavigatorPanel.addTableSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if(!e.getValueIsAdjusting()) {
                	// save the state of the filter Text field before we swap out tables.
                    selectedNavItem.setFilteredText(libraryFilterPanel.getFilterField().getText());
                    // save the state of the selected category to the NavItem before swapping.
                    selectedNavItem.setSelectedCategory(libraryFilterPanel.getSelectedCategory());
                    
                    LibraryNavItem navItem = libraryNavigatorPanel.getSelectedNavItem();
                    selectedNavItem = navItem;
                    // update the filter Panel to display the saved category/filter text of the new NavItem
                    libraryFilterPanel.setSelectedCategory(navItem.getSelectedCategory(), navItem.getFilteredText());
                    setPublicSharedComponentVisible(navItem);
                    eventList = navItem.getLocalFileList().getSwingModel();
                    selectSharing(navItem);
                    selectTable(libraryFilterPanel.getSelectedTableFormat(), libraryFilterPanel.getSelectedCategory());
                }
            }
        });
        
        // Add player listener to repaint table when song changes or player stops.
        playerMediator.addMediatorListener(new PlayerMediatorListener(){
            @Override
            public void progressUpdated(float progress) {
            }

            @Override
            public void mediaChanged(String name) {
                if(libraryTable.isVisible() && isPlayable(libraryFilterPanel.getSelectedCategory())) {
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            libraryTable.repaint();     
                        }
                    });
                }
            }

            @Override
            public void stateChanged(final PlayerState state) {
                if ((state == PlayerState.STOPPED || state == PlayerState.EOM || state == PlayerState.UNKNOWN || state == PlayerState.NO_SOUND_DEVICE) 
                        && libraryTable.isVisible() 
                        && isPlayable(libraryFilterPanel.getSelectedCategory())) {
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            libraryTable.repaint();
                            
                            // gives feedback to Windows 7 users when headphone/speaker jack is not plugged in.
                            if(state == PlayerState.NO_SOUND_DEVICE && OSUtils.isWindows7()) {
                                FocusJOptionPane.showConfirmDialog(null, I18n.tr("LimeWire could not play this file. There may not be a sound device installed on your computer or your speakers may not be plugged in."), I18n.tr("Problem Playing File"), JOptionPane.OK_CANCEL_OPTION); 
                            }
                        }
                    });
                }
            }
        });
        
        librarySharingPanel.getComponent().addPropertyChangeListener(new PropertyChangeListener(){
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals(LibrarySharingPanel.EDIT_MODE)) {
                    setEditSharingModeEnabled((Boolean)evt.getNewValue());
                }
            }
        });
    }
    
    /**
     * When editing users, disables components.
     */
    private void setEditSharingModeEnabled(boolean value) {
        lockableUI.setLocked(value);
        addFilesButton.setEnabled(!value);
    }
    
    /**
     * Returns a Rectangle contains the location and size of the table within
     * this container.
     */
    public Rectangle getTableListRect() {
        Point location = tableListPanel.getLocation();
        Dimension size = tableListPanel.getSize();
        return new Rectangle(location.x, location.y, size.width, size.height);
    }
    
    public void selectLocalFileList(LocalFileList localFileList) {
        libraryNavigatorPanel.selectLocalFileList(localFileList);
    }
    
    public void selectAndScrollTo(File file) {
        if(file != null) {
            libraryFilterPanel.clearFilters();
            libraryTable.selectAndScrollTo(file);
        }
    }
    
    public void selectAndScrollTo(URN urn) {
        if(urn != null) {
            libraryFilterPanel.clearFilters();
            libraryTable.selectAndScrollTo(urn);
        }
    }
    
    private void selectSharing(LibraryNavItem navItem) {
        if(navItem != null && navItem.getLocalFileList() instanceof SharedFileList) {
            librarySharingPanel.setSharedFileList((SharedFileList)navItem.getLocalFileList());
        }
        librarySharingPanel.getComponent().setVisible(navItem != null && navItem.getType() == NavType.LIST);
        // if the sharing panel isn't visible, ensure everything is enabled.
        if(!librarySharingPanel.getComponent().isVisible())
            setEditSharingModeEnabled(false);
    }
    
    private void createImageList() {
        libraryImagePanel = libraryImagePanelProvider.get();
        libraryImagePanel.setTransferHandler(transferHandler);
        // setTransferHandler() is overriden so kde hack not needed
        tableListPanel.add(libraryImagePanel, LIST); 
    }
    
    /**
     * Returns true if the specified category is playable.
     */
    public static boolean isPlayable(Category category) {
        //null is the all category
        return (category == null) || (category == Category.AUDIO);
    }
    
    /**
     * Returns the current list of playable file items.
     */
    public EventList<LocalFileItem> getPlayableList() {
        return libraryTable.getPlayableList();
    }
    
    /**
     * Returns the selected display category.
     */
    public Category getSelectedCategory() {
        return selectedCategory;
    }
    
    /**
     * Returns the selected library item.
     */
    public LibraryNavItem getSelectedNavItem() {
        return libraryNavigatorPanel.getSelectedNavItem();
    }
    
    List<File> getSelectedFiles() {
        List<LocalFileItem> selected;
        if(selectedCategory == Category.IMAGE) {
            selected = libraryImagePanel.getSelection();
        } else {
            selected = libraryTable.getSelection();
        }
        
        List<File> files = new ArrayList<File>(selected.size());
        for(LocalFileItem item : selected) {
            files.add(item.getFile());
        }
        return files;
    }
    
    public List<LocalFileItem> getSelectedItems() {
        List<LocalFileItem> selected;
        if(selectedCategory == Category.IMAGE) {
            selected = libraryImagePanel.getSelection();
        } else {
            selected = libraryTable.getSelection();
        }
        return selected;
    }
    
    private void selectTable(AbstractLibraryFormat<LocalFileItem> libraryTableFormat, Category category) {       
        selectedCategory = category;

        if(category != Category.IMAGE) {
            setEventListOnTable(eventList);
            libraryTable.setupCellRenderers(category, libraryTableFormat);
            libraryTable.applySavedColumnSettings();
            
            // hide the remove button for Library Tables
            TableColumnExt column = libraryTable.getColumnExt(libraryTableFormat.getColumnName(libraryTableFormat.getActionColumn()));
            if(column != null) {
                column.setVisible(libraryNavigatorPanel.getSelectedNavItem().getType() != NavType.LIBRARY);
            }
            tableListLayout.show(tableListPanel, TABLE);
        } else {
            if(libraryImagePanel == null) {
                createImageList();
            }
            setEventListOnImages(eventList);
            // hide remove button for library
            libraryImagePanel.setShowButtons(libraryNavigatorPanel.getSelectedNavItem().getType() != NavType.LIBRARY);
            tableListLayout.show(tableListPanel, LIST);
        }
    }
    
    private void createAddFilesButton(AddFileAction addFileAction, LibrarySharingAction libraryAction) {
        addFilesButton = new JXButton(addFileAction);
        addFilesButton.setIcon(plusIcon);
        addFilesButton.setRolloverIcon(plusIcon);
        addFilesButton.setPressedIcon(plusIcon);
        addFilesButton.addActionListener(libraryAction);
        addFilesButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        buttonDecorator.decorateDarkFullImageButton(addFilesButton, AccentType.SHADOW);
    }
    
    
    private void createFilterToggleButton() {
        filterToggleButton = new JXButton(I18n.tr("Filter"));
        filterToggleButton.setSelected(SwingUiSettings.SHOW_LIBRARY_FILTERS.getValue());
        filterToggleButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                filterToggleButton.setSelected(!filterToggleButton.isSelected());
                libraryFilterPanel.getComponent().setVisible(filterToggleButton.isSelected());
                libraryFilterPanel.clearFilters();
                libraryFilterPanel.getFilterField().requestFocusInWindow();
                SwingUiSettings.SHOW_LIBRARY_FILTERS.setValue(filterToggleButton.isSelected());
            }
        });
        filterToggleButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        buttonDecorator.decorateDarkFullImageButton(filterToggleButton, AccentType.SHADOW);
    }
    
    private EventList<LocalFileItem> recreateFilterList(final EventList<LocalFileItem> eventList) {
        if(filteredList != null) {
            filteredList.dispose();
            filteredList = null;
        }
        if(textFilterList != null) {
            textFilterList.removeListEventListener(fileCountListener);
            textFilterList.dispose();
        }
        if(selectedCategory != null) {
            final Category category = selectedCategory;
            filteredList = GlazedListsFactory.filterList(eventList, new Matcher<LocalFileItem>() {
                @Override
                public boolean matches(LocalFileItem item) {
                    return item.getCategory().equals(category);
                }
            });
        }
        MatcherEditor<LocalFileItem> textMatcherEditor = new TextComponentMatcherEditor<LocalFileItem>(libraryFilterPanel.getFilterField(), new LocalFileItemFilterator(selectedCategory) );
        textFilterList = GlazedListsFactory.filterList(filteredList == null ? eventList : filteredList, textMatcherEditor);
        setFileCount(textFilterList.size(), eventList.size());
        textFilterList.addListEventListener(fileCountListener);
        
        return textFilterList;
    }
    
    private void setEventListOnTable(EventList<LocalFileItem> eventList) {
        libraryTable.setEventList(recreateFilterList(eventList), libraryFilterPanel.getSelectedTableFormat());
    }
    
    private void setEventListOnImages(EventList<LocalFileItem> eventList) {
        libraryImagePanel.setEventList(recreateFilterList(eventList));
    }
    
    private void setPublicSharedComponentVisible(LibraryNavItem navItem) {
        publicSharedFeedbackPanel.getComponent().setVisible(navItem != null && navItem.getType() == NavType.PUBLIC_SHARED);
    }
    
    private void setupHighlighters() {
        TableColors tableColors = new TableColors();
        ColorHighlighter storeHighlighter = new ColorHighlighter(new GrayHighlightPredicate(), 
                null, tableColors.getDisabledForegroundColor(), 
                null, tableColors.getDisabledForegroundColor());
        
        libraryTable.addHighlighter(storeHighlighter);
    }
    
    private void setFileCount(int filterSize, int totalCount) {
        if(selectedCategory == null && filterSize == totalCount) {
            fileCountLabel.setText(I18n.tr("{0} files", totalCount));
        } else { 
            fileCountLabel.setText(I18n.tr("{0} of {1} files", filterSize, totalCount));
        }
    }
    
    private class GrayHighlightPredicate implements HighlightPredicate {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            LibraryNavItem navItem = libraryNavigatorPanel.getSelectedNavItem();
            LocalFileItem item = libraryTable.getLibraryTableModel().getElementAt(adapter.row);
            if( navItem.getType() == NavType.PUBLIC_SHARED || (navItem.getType() == NavType.LIST && ((SharedFileList)navItem.getLocalFileList()).getFriendIds().size() > 0))
                return !item.isShareable(); 

            return !item.isLoaded();
        }
    }
    
    /**
	 * Clears any active filters on the library.
	 */
    public void clearFilters() {
        libraryFilterPanel.clearFilters();
    }

    /**
     * Selects the specified SharedFileList in the library nav and starts editing on its name.
     * @param sharedFileList can not be the public shared list
     */
    public void editSharedListName(SharedFileList sharedFileList) {
        libraryNavigatorPanel.editSharedListName(sharedFileList);     
    }
    
    /**
     * If the sharing panel is visible, try to show the edit mode.
     */
    public void showEditMode() {
    	if(librarySharingPanel.getComponent().isVisible())
        	librarySharingPanel.showEditMode();
    }
    
    /**
     * Listens for changes on the current list and updates the 
     * file count label.
     */
    private class FileCountListener implements ListEventListener<LocalFileItem> {
        @Override
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            setFileCount(textFilterList.size(), filteredList == null ? eventList.size() : filteredList.size());
        }
    }
    
    /**
     * Creates a locked layer over a table. This layer prevents the user from
     * interacting with the contents underneath it.
     */
    private class LockedUI extends LockableUI {
        private JXPanel panel;
        
        public LockedUI(LayerEffect... lockedEffects) {
            super(lockedEffects);
            
            panel = new JXPanel();
            panel.setBackground(tableOverlayColor);
            panel.setVisible(false);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            JXLayer<JComponent> l = (JXLayer<JComponent>) c;
            l.getGlassPane().setLayout(new BorderLayout());
            l.getGlassPane().add(panel, BorderLayout.CENTER);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public void uninstallUI(JComponent c) {
            super.uninstallUI(c);
            JXLayer<JComponent> l = (JXLayer<JComponent>) c;
            l.getGlassPane().setLayout(new FlowLayout());
            l.getGlassPane().remove(panel);
        }
        
        @Override
        public void setLocked(boolean isLocked) {
            super.setLocked(isLocked);
            panel.setVisible(isLocked);
        }
        
        @Override
        public Cursor getLockedCursor() {
            return Cursor.getDefaultCursor();
        }
    }
}
