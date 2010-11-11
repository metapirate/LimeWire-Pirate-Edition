package org.limewire.ui.swing.library.navigator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;

import javax.swing.DropMode;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import org.jdesktop.application.Resource;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.table.GlazedJXTable;
import org.limewire.ui.swing.table.SingleColumnTableFormat;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingHacks;

import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.impl.swing.SwingThreadProxyEventList;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class LibraryNavigatorTable extends GlazedJXTable {

    @Resource private Color backgroundColor;    
    private TablePopupHandler popupHandler;
    private final Provider<Navigator> navigatorProvider;
    private SwingThreadProxyEventList<LibraryNavItem> stpl;
    
    @Inject
    public LibraryNavigatorTable(LibraryNavTransferHandler libraryNavTransferHandler,
            LibraryManager libraryManager, SharedFileListManager sharedFileListManager,
            Provider<Navigator> navigatorProvider) {
        this.navigatorProvider = navigatorProvider;
        GuiUtils.assignResources(this);

        initialize();

        EventList<SharedFileList> privateLists = sharedFileListManager.getModel();
        FunctionList<SharedFileList, LibraryNavItem> shareToNav = GlazedListsFactory.functionList(privateLists, new FunctionList.AdvancedFunction<SharedFileList, LibraryNavItem>() {
            @Override
            public void dispose(SharedFileList sourceValue, LibraryNavItem transformedValue) {
                // TODO: sourceValue.dispose necessary?
            }
            
            @Override
            public LibraryNavItem evaluate(SharedFileList sourceValue) {
                return new LibraryNavItem(sourceValue);
            }
            @Override
            public LibraryNavItem reevaluate(SharedFileList sourceValue, LibraryNavItem transformedValue) {
                return transformedValue;
            }            
        });
        
        CompositeList<LibraryNavItem> compositeList = new CompositeList<LibraryNavItem>(privateLists.getPublisher(), privateLists.getReadWriteLock());
        compositeList.getReadWriteLock().writeLock().lock();
        try {
            // Create a fake EventList to hold the library list & add it to the composite list.
            EventList<LibraryNavItem> libraryList = compositeList.createMemberList();
            libraryList.add(new LibraryNavItem(libraryManager.getLibraryManagedList()));
            compositeList.addMemberList(libraryList);
            compositeList.addMemberList(shareToNav);
        } finally {
            compositeList.getReadWriteLock().writeLock().unlock();
        }
        
        SortedList<LibraryNavItem> sortedList = GlazedListsFactory.sortedList(compositeList, new LibraryNavItemComparator());    
        stpl = GlazedListsFactory.swingThreadProxyEventList(sortedList);
        
        setModel(new DefaultEventTableModel<LibraryNavItem>(stpl, new SingleColumnTableFormat<LibraryNavItem>("")));
        setDropMode(DropMode.ON);
        setTransferHandler(libraryNavTransferHandler);
        SwingHacks.fixDnDforKDE(this);
        setEditable(false);
    }
    
    public void selectLibraryNavItem(int id) {
        showNavTableIfHidden();
        for(int i = 0; i < getModel().getRowCount(); i++) {
            Object value = getModel().getValueAt(i, 0);
            if(value instanceof LibraryNavItem) {
                if( ((LibraryNavItem) value).getId() == id) {
                    getSelectionModel().setSelectionInterval(i,i);
                    break;
                }
            }
        }
    }
    
    public void selectLibraryNavItem(LocalFileList sharedFileList) {
        showNavTableIfHidden();
        for(int i = 0; i < getModel().getRowCount(); i++) {
            Object value = getModel().getValueAt(i, 0);
            if(value instanceof LibraryNavItem) {
                if( ((LibraryNavItem) value).getLocalFileList() == sharedFileList) {
                    getSelectionModel().setSelectionInterval(i,i);
                    break;
                }
            }
        }
    }
    
    private void showNavTableIfHidden(){
        if (!isShowing()) {
            NavItem item = navigatorProvider.get().getNavItem(NavCategory.LIBRARY,
                    LibraryMediator.NAME);
            item.select();
        }
    }
    
    public LibraryNavItem getSelectedItem() {
        int selected = getSelectedRow();
        if(selected < 0) {
            getSelectionModel().setSelectionInterval(0, 0);
            selected = 0;
        }
        return (LibraryNavItem)getModel().getValueAt(selected, 0);
    }
    
    private void initialize() {
        setFillsViewportHeight(true);
        setBackground(backgroundColor);
        setShowGrid(false, false);
        setTableHeader(null);
        setRowHeight(24);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {//adding this to editor messes up popups
//
//                int col = columnAtPoint(e.getPoint());
//                int row = rowAtPoint(e.getPoint());
//
//                if (row >= 0 && col >= 0) {
//                    if (rowDoubleClickHandler != null || columnDoubleClickHandler != null) {
//                        Component component = e.getComponent();
//                        //launch file on double click unless the click is on a button
//                        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)
//                                && !(component.getComponentAt(e.getPoint()) instanceof JButton)) {
//                            if (rowDoubleClickHandler != null) {
//                                rowDoubleClickHandler.handleDoubleClick(row);
//                            }
//                            if (columnDoubleClickHandler != null) {
//                                columnDoubleClickHandler.handleDoubleClick(col);
//                            }
//                        }
//                    }                   
//                }
            }
            

            @Override
            public void mouseExited(MouseEvent e) {
//                maybeCancelEditing();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
//                int col = columnAtPoint(e.getPoint());
//                int row = rowAtPoint(e.getPoint());
//                if (isEditing() && isCellEditable(row, col)) { 
//                    TableCellEditor editor = getCellEditor(row, col);
//                    if (editor != null) {
//                        // force update editor colors
//                        prepareEditor(editor, row, col);
//                    }                        
//                }
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger() && popupHandler != null) {
                    int col = columnAtPoint(e.getPoint());
                    int row = rowAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0) {
                        popupHandler.maybeShowPopup(
                            e.getComponent(), e.getX(), e.getY());
                        TableCellEditor editor = getCellEditor();
                        if (editor != null) {
                            editor.cancelCellEditing();
                        }
                    }
                }
            }
        });
    }
    
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(super.getPreferredScrollableViewportSize().width, getModel().getRowCount() * getRowHeight());
    }
    
    public void setPopupHandler(TablePopupHandler popupHandler) {
        this.popupHandler = popupHandler;
    }
    
    @Override
    public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {    
            Object value = editor.getCellEditorValue();
            if(value instanceof String) {
                handleRename((String)value);
            }
            removeEditor();
        }
        setEditable(false);
    }
    
    /**
     * Renames the selected LibraryNavItem if the new Name
     * is a valid String.
     */
    private void handleRename(String newName) {
        if(newName == null || newName.length() == 0)
            return;
        LibraryNavItem item = getSelectedItem();
        LocalFileList fileList = item.getLocalFileList();
        if(fileList instanceof SharedFileList) {
            SharedFileList sharedFileList = (SharedFileList) fileList;
            if(sharedFileList.isNameChangeAllowed() && !sharedFileList.getCollectionName().equals(newName)) {
                sharedFileList.setCollectionName(newName);
            }
        }
    }
    
    @Override
    public boolean isCellEditable(int row, int col) {
        if (!isEditable() || row >= getRowCount() || col >= getColumnCount() || row < 2 || col < 0) {
            return false;
        }
        return true;
    }
    
    @Override
    public boolean editCellAt(int row, int column) {
        if(super.editCellAt(row, column)) {
            LibraryNavTableEditor editor = (LibraryNavTableEditor)getCellEditor();
            editor.prepareForEditing();
            return true;
        } else {
            return false;
        }
    }
    
    public int getPrivateSharedLibraryCount() {
        int numberOfPrivateSharedLibraries = 0;
        for ( LibraryNavItem item  : stpl ) {
            if ( item.getType() == LibraryNavItem.NavType.LIST ) {
                numberOfPrivateSharedLibraries++;
            }
        }
        return numberOfPrivateSharedLibraries;
    }
    
    /**
     * Technically shouldn't be necessary but ensures that Library and Public Shared
     * always appear first and second in the table. The other items appear as they are
     * loaded.
     */
    private static class LibraryNavItemComparator implements Comparator<LibraryNavItem> {

        @Override
        public int compare(LibraryNavItem nav1, LibraryNavItem nav2) {
            NavType type1 = nav1.getType();
            NavType type2 = nav2.getType();
            if(type1 == NavType.LIBRARY)
                return -1;
            else if(type2 == NavType.LIBRARY)
                return 1;
            else if(type1 == NavType.PUBLIC_SHARED)
                return -1;
            else if(type2 == NavType.PUBLIC_SHARED)
                return 1;
            return 0;
        }
    }
}
