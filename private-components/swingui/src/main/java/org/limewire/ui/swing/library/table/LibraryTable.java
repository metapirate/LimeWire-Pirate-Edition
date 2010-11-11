package org.limewire.ui.swing.library.table;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.jdesktop.swingx.table.TableColumnExt;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.table.ColumnStateHandler;
import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.IconLabelRenderer;
import org.limewire.ui.swing.table.IconLabelRendererFactory;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.MultilineTooltipRenderer;
import org.limewire.ui.swing.table.NameRenderer;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.table.TableColumnSelector;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.EventListJXTableSorting;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class LibraryTable extends MouseableTable {

    private final int rowHeight = 20;

    private LibraryTableModel cachedLibraryTableModel;
    private DefaultEventSelectionModel<LocalFileItem> cachedEventSelectionModel;
    private EventListJXTableSorting cachedTableSorting;
    private SortedList<LocalFileItem> cachedSortedList;
    
    private AbstractLibraryFormat<LocalFileItem> fileItemFormat;
    private ColumnStateHandler columnStateHandler;
    private MousePopupListener mousePopupListener;
    
    private Provider<DefaultLimeTableCellRenderer> defaultCellRenderer;
    private final Provider<NameCategoryRenderer> nameCategoryRenderer;
    private final Provider<TimeRenderer> timeRenderer;
    private final Provider<FileSizeRenderer> fileSizeRenderer;
    private final Provider<NameRenderer> nameRenderer;
    private final Provider<RemoveRenderer> removeRenderer;
    private final Provider<IsPlayingRenderer> isPlayingRenderer;
    private final Provider<LaunchFileAction> launchAction;
    private final IconLabelRenderer iconLabelRenderer;
    private final RemoveEditor removeEditor;
    private final CategoryManager categoryManager;
    
    @Inject
    public LibraryTable(Provider<DefaultLimeTableCellRenderer> defaultCellRenderer,
            Provider<NameCategoryRenderer> nameCategoryRenderer,
            Provider<TimeRenderer> timeRenderer,
            Provider<FileSizeRenderer> fileSizeRenderer,
            Provider<NameRenderer> nameRenderer,
            Provider<LibraryPopupMenu> libraryPopupMenu,
            Provider<RemoveRenderer> removeRenderer,
            Provider<IsPlayingRenderer> isPlayingRenderer,
            Provider<LaunchFileAction> launchAction,
            IconLabelRendererFactory iconLabelRendererFactory,
            RemoveEditor removeEditor,
            DeletionKeyListener deletionKeyListener,
            CategoryManager categoryManager) {
        this.defaultCellRenderer = defaultCellRenderer;
        this.nameCategoryRenderer = nameCategoryRenderer;
        this.timeRenderer = timeRenderer;
        this.fileSizeRenderer = fileSizeRenderer;
        this.nameRenderer = nameRenderer;
        this.removeRenderer = removeRenderer;
        this.isPlayingRenderer = isPlayingRenderer;
        this.launchAction = launchAction;
        this.iconLabelRenderer = iconLabelRendererFactory.createIconRenderer(false);
        this.removeEditor = removeEditor;
        this.categoryManager = categoryManager;

        initTable();
        
        //TODO: anything below here should be initialized outside of the constructor
        mousePopupListener = new MousePopupListener() {
            @Override
            public void handlePopupMouseEvent(MouseEvent e) {
                showHeaderPopupMenu(e.getPoint());
            }
        }; 

        setPopupHandler(new LibraryPopupHandler(this, libraryPopupMenu));
        
        this.addKeyListener(deletionKeyListener);
    }
    
    private void initTable() {
        setShowHorizontalLines(false);
        setShowGrid(false, true);
        
        setFillsViewportHeight(true);
        setDragEnabled(true);
        setRowHeight(rowHeight);
        setDropMode(DropMode.ON);
        
        setDoubleClickHandler(new DoubleClickHandler());
        setEnterKeyAction(launchAction.get());
    }
    
    private void uninstallListeners() {
        getTableHeader().removeMouseListener(mousePopupListener);
        
        if(columnStateHandler != null) {
            columnStateHandler.removeListeners();
        }
    }
    
    private void installListeners() {
        // Add mouse listener to display popup menu on column header.  We use 
        // MousePopupListener to detect the popup trigger, which differs on 
        // Windows, Mac, and Linux.
        JTableHeader header = getTableHeader();
        header.addMouseListener(mousePopupListener);
        
        // Install column state handler.
        columnStateHandler = new ColumnStateHandler(this, fileItemFormat);
    }
    
    public void showHeaderPopupMenu(Point p) {
        JPopupMenu menu = new TableColumnSelector(this, fileItemFormat).getPopupMenu();
        menu.show(getTableHeader(), p.x, p.y);
    }
    
    public LibraryTableModel getLibraryTableModel() {
        return cachedLibraryTableModel;
    }
    
    public LocalFileItem getSelectedItem() {
        if(getSelectedRow() >= 0)
            return cachedLibraryTableModel.getElementAt(getSelectedRow());
        else           
            return null;
    }
    
    public void setEventList(EventList<LocalFileItem> eventList, AbstractLibraryFormat<LocalFileItem> tableFormat) {
        uninstallListeners();
        
        fileItemFormat = tableFormat;
        
        SortedList<LocalFileItem> newSortedList = GlazedListsFactory.sortedList(eventList, null);
        LibraryTableModel newLibraryTableModel = new LibraryTableModel(newSortedList, tableFormat);
        DefaultEventSelectionModel<LocalFileItem> newEventSelectionModel = new DefaultEventSelectionModel<LocalFileItem>(newSortedList);
  
        if(cachedTableSorting != null) {
            cachedTableSorting.uninstall();
        }

		// setting the selection model first to ensure table doesn't
		// try to select non-existent rows
        setSelectionModel(newEventSelectionModel);
        setModel(newLibraryTableModel);
        newEventSelectionModel.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        
        if(cachedLibraryTableModel != null) {
            cachedEventSelectionModel.dispose();
            cachedLibraryTableModel.dispose();
            cachedSortedList.dispose();
        }
        
        EventListJXTableSorting newTableSorting = EventListJXTableSorting.install(this, newSortedList, tableFormat);
        
        cachedSortedList = newSortedList;
        cachedLibraryTableModel = newLibraryTableModel;
        cachedEventSelectionModel = newEventSelectionModel;
        cachedTableSorting = newTableSorting;
        
        installListeners();
    }
    
    /**
     * Creates a list of playable file items using the specified source list.
     */
    private FilterList<LocalFileItem> createPlayableList(EventList<LocalFileItem> sourceList) {
        // Return filter list for playable files.
        return GlazedListsFactory.filterList(sourceList, new Matcher<LocalFileItem>() {
            @Override
            public boolean matches(LocalFileItem item) {
                Category category = categoryManager.getCategoryForFile(item.getFile());
                return category == Category.AUDIO;
            }
        });
    }
    
    /**
     * Returns the list of playable file items.
     */
    public EventList<LocalFileItem> getPlayableList() {
        return createPlayableList(((LibraryTableModel)getModel()).getAllItems());
    }
    
    public boolean isRowDisabled(int row) {
        FileItem item = getLibraryTableModel().getFileItem(convertRowIndexToModel(row));
        if (item instanceof LocalFileItem) {
            return ((LocalFileItem) item).isIncomplete();
        }
        return item == null;
    }
    
    public void selectAndScrollTo(File file) {
    	//ensure table has focus to recieve keyboard events
        requestFocus();
        LibraryTableModel model = getLibraryTableModel();
        for(int y=0; y < model.getRowCount(); y++) {
            FileItem fileItem = model.getElementAt(y);
            if(fileItem instanceof LocalFileItem) {
                if(file.equals(((LocalFileItem)fileItem).getFile())) {
                    getSelectionModel().setSelectionInterval(y, y);
                    break;
                }
            }
        }
        ensureRowVisible(getSelectedRow());
    }
    
    public void selectAndScrollTo(URN urn) {
        //ensure table has focus to recieve keyboard events
        requestFocus();
        LibraryTableModel model = getLibraryTableModel();
        for(int y=0; y < model.getRowCount(); y++) {
            FileItem fileItem = model.getElementAt(y);
            if(urn.equals(fileItem.getUrn())) {
                getSelectionModel().setSelectionInterval(y, y);
                break;
            }
        }
        ensureRowVisible(getSelectedRow());
    }
        
    /**
     * Loads the saved state of the columns. 
     *
     * <p>NOTE: This method must be called after the renderers and editors
     * have been loaded.  The settings must be applied in this order:
     * width/visibility/order.</p>
     */
    public void applySavedColumnSettings(){
        if (columnStateHandler != null) {
            columnStateHandler.setupColumnWidths();
            columnStateHandler.setupColumnVisibility(false);
            columnStateHandler.setupColumnOrder();
        }
    }
    
    public void setupCellRenderers(Category category, AbstractLibraryFormat format) {
        for (int i = 0; i < format.getColumnCount(); i++) {
            Class clazz = format.getColumnClass(i);
            if (clazz == String.class) {
                setCellRenderer(i, defaultCellRenderer.get());
            } 
        }   
        
        if(category != null) {
            switch(category) {
            case AUDIO:
                setHeaderRenderer(AudioTableFormat.LENGTH_INDEX, new TableCellHeaderRenderer(JLabel.TRAILING));
                setCellRenderer(AudioTableFormat.PLAY_INDEX, isPlayingRenderer.get());
                setUnsortable(AudioTableFormat.PLAY_INDEX);
                setCellRenderer(AudioTableFormat.SIZE_INDEX, fileSizeRenderer.get());
                setCellRenderer(AudioTableFormat.LENGTH_INDEX, timeRenderer.get());
                setCellRenderer(AudioTableFormat.TITLE_INDEX, nameRenderer.get());
                setCellRenderer(AudioTableFormat.ACTION_INDEX, removeRenderer.get());
                setCellEditor(AudioTableFormat.ACTION_INDEX, removeEditor);
                setUnsortable(AudioTableFormat.ACTION_INDEX);
                break;
            case VIDEO:
                setHeaderRenderer(VideoTableFormat.LENGTH_INDEX, new TableCellHeaderRenderer(JLabel.TRAILING));
                setCellRenderer(VideoTableFormat.LENGTH_INDEX, timeRenderer.get());
                setCellRenderer(VideoTableFormat.SIZE_INDEX, fileSizeRenderer.get());
                setCellRenderer(VideoTableFormat.NAME_INDEX, nameRenderer.get());
                setCellRenderer(VideoTableFormat.ACTION_INDEX, removeRenderer.get());
                setCellEditor(VideoTableFormat.ACTION_INDEX, removeEditor);
                setUnsortable(VideoTableFormat.ACTION_INDEX);
                break;
            case IMAGE:
                setCellRenderer(ImageTableFormat.SIZE_INDEX, fileSizeRenderer.get());
                setCellRenderer(ImageTableFormat.ACTION_INDEX, removeRenderer.get());
                setCellEditor(ImageTableFormat.ACTION_INDEX, removeEditor);
                setUnsortable(ImageTableFormat.ACTION_INDEX);
                break;
            case DOCUMENT:
                setCellRenderer(DocumentTableFormat.NAME_INDEX, iconLabelRenderer);
                setCellRenderer(DocumentTableFormat.SIZE_INDEX, fileSizeRenderer.get());
                setCellRenderer(DocumentTableFormat.ACTION_INDEX, removeRenderer.get());
                setCellEditor(DocumentTableFormat.ACTION_INDEX, removeEditor);
                setUnsortable(DocumentTableFormat.ACTION_INDEX);
                break;
            case PROGRAM:
                setCellRenderer(ProgramTableFormat.NAME_INDEX, iconLabelRenderer);
                setCellRenderer(ProgramTableFormat.SIZE_INDEX, fileSizeRenderer.get());
                setCellRenderer(ProgramTableFormat.ACTION_INDEX, removeRenderer.get());
                setCellEditor(ProgramTableFormat.ACTION_INDEX, removeEditor);
                setUnsortable(ProgramTableFormat.ACTION_INDEX);
                break;
            case OTHER:
                setCellRenderer(OtherTableFormat.NAME_INDEX, iconLabelRenderer);
                setCellRenderer(OtherTableFormat.SIZE_INDEX, fileSizeRenderer.get());
                setCellRenderer(OtherTableFormat.ACTION_INDEX, removeRenderer.get());
                setCellEditor(OtherTableFormat.ACTION_INDEX, removeEditor);
                setUnsortable(OtherTableFormat.ACTION_INDEX);
                break;
            case TORRENT:
                
                MultilineTooltipRenderer renderer = new MultilineTooltipRenderer();
                setCellRenderer(TorrentTableFormat.TRACKERS_INDEX, renderer);
                setCellRenderer(TorrentTableFormat.FILES_INDEX, renderer);
                
                setCellRenderer(TorrentTableFormat.NAME_INDEX, iconLabelRenderer);
                setCellRenderer(TorrentTableFormat.SIZE_INDEX, fileSizeRenderer.get());
                setCellRenderer(TorrentTableFormat.ACTION_INDEX, removeRenderer.get());
                setCellEditor(TorrentTableFormat.ACTION_INDEX, removeEditor);
                
                setUnsortable(TorrentTableFormat.ACTION_INDEX);
                setUnsortable(TorrentTableFormat.TRACKERS_INDEX);
                setUnsortable(TorrentTableFormat.FILES_INDEX);
                break;
            default:
                throw new IllegalArgumentException("Unknown category:" + category);
            }
        } else {
            setCellRenderer(AllTableFormat.PLAY_INDEX, isPlayingRenderer.get());
            setUnsortable(AllTableFormat.PLAY_INDEX);
            setCellRenderer(AllTableFormat.NAME_INDEX, nameCategoryRenderer.get());
            setCellRenderer(AllTableFormat.SIZE_INDEX, fileSizeRenderer.get());
            setCellRenderer(AllTableFormat.ACTION_INDEX, removeRenderer.get());
            setCellEditor(AllTableFormat.ACTION_INDEX, removeEditor);
            setUnsortable(AllTableFormat.ACTION_INDEX);
        }
    }
    
    /**
     * Assigns the specified header renderer to the specified column in the 
     * Table view column model.   
     */
    protected void setHeaderRenderer(int column, TableCellRenderer headerRenderer) {
        TableColumnModel tcm = getColumnModel();
        TableColumn tc = tcm.getColumn(column);
        tc.setHeaderRenderer(headerRenderer);
    }
    
    private void setCellRenderer(int column, TableCellRenderer renderer) {
        getColumnModel().getColumn(column).setCellRenderer(renderer);
    }
    
    private void setCellEditor(int column, TableCellEditor editor) {
        getColumnModel().getColumn(column).setCellEditor(editor);
    }
    
    /** Prevents the given column from being sortable. */
    private void setUnsortable(int column) {
        ((TableColumnExt)getColumnModel().getColumn(column)).setSortable(false);
    }

    /** Returns all currently selected LocalFileItems. */
    public List<LocalFileItem> getSelection() {
        return cachedEventSelectionModel.getSelected();
    }
    
    /**
     * Handles double clicking a row in the library table.
     */
    private class DoubleClickHandler implements TableDoubleClickHandler{
        @Override
        public void handleDoubleClick(int row) {
            if (getSelectedItem() == null) { 
                getSelectionModel().setSelectionInterval(row, row);
            }
            launchAction.get().actionPerformed(null);
        }
    }
    
    @Override
    protected String getToolTipText(int row, int col) {
        Object value = getValueAt(row, col);
        if(value != null && value instanceof LocalFileItem) {
            LocalFileItem localFileItem = (LocalFileItem) value;
            if(!localFileItem.isLoaded()) {
                return I18n.tr("This file is still processing.");
            } else if(!localFileItem.isShareable()) {
                return I18n.tr("This file cannot be shared.");
            }
        }  
        return super.getToolTipText(row, col);
    }
}
