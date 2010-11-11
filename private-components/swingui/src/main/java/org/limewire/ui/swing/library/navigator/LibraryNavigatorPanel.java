package org.limewire.ui.swing.library.navigator;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.inject.LazySingleton;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

@LazySingleton
public class LibraryNavigatorPanel extends JXPanel {

    @Resource private Color backgroundColor;
    @Resource private Color borderColor;
    @Resource private Font createListFont;
    
    private final LibraryNavigatorTable table;
    private final CreateListAction createAction;
    private final SharedFileListManager sharedFileListManager;
    
    private HyperlinkButton createListButton;
    
    @Inject
    public LibraryNavigatorPanel(LibraryNavigatorTable table, LibraryNavTableRenderer renderer,
            LibraryNavTableEditor editor,
            LibraryNavPopupHandler popupHandler, 
            CreateListAction createAction,
            SharedFileListManager sharedFileListManager, GhostDragGlassPane ghostGlassPane) {
        super(new MigLayout("insets 0, gap 0, fillx", "[150!]", ""));
        
        this.table = table;
        this.sharedFileListManager = sharedFileListManager;
        this.createAction = createAction;
        
        GuiUtils.assignResources(this);
        setBackground(backgroundColor);
        
        setBorder(BorderFactory.createMatteBorder(0,0,0,1, borderColor));
        
        JPanel panel = new JPanel(new MigLayout("fill, gap 0, insets 0"));
        panel.setBackground(backgroundColor);
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); 
        panel.add(table, "grow, wrap");
        
        table.getColumnModel().getColumn(0).setCellRenderer(renderer);
        table.getColumnModel().getColumn(0).setCellEditor(editor);
        table.setPopupHandler(popupHandler);
        
        add(scrollPane, "growx, growy, wrap");

        createCreateListButton();
        panel.add(createListButton, "aligny top, gapbottom 5, gapleft 30");

        initData();
    }
    
    @Inject
    void register(ListenerSupport<FriendConnectionEvent> connectionEvent) {
        // listen for a friend sign on and create the private shared list
        // if one doesn't exist on sign on.
        connectionEvent.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                if(event.getType() == FriendConnectionEvent.Type.CONNECTED) {
                    EventList<SharedFileList> listsModel = sharedFileListManager.getModel();
                    if(listsModel.size() == 1) {
                        createPrivateShareList();
                    }
                }
            }
        });
        
        // when editing stops, reenable the Create List button
        table.getColumnModel().getColumn(0).getCellEditor().addCellEditorListener(new CellEditorListener(){
            @Override
            public void editingCanceled(ChangeEvent e) {
                createListButton.setEnabled(true);
            }

            @Override
            public void editingStopped(ChangeEvent e) {
                createListButton.setEnabled(true);
            }
        });
    }
    
    /**
     * If we haven't created the Private Shared list yet, create it
     */
    private void createPrivateShareList() {
        sharedFileListManager.createNewSharedFileList(I18n.tr("Private Shared"));
    }
    
    private void initData() {
        table.getSelectionModel().setSelectionInterval(0, 0);
    }
    
    private void createCreateListButton() {
        createListButton = new HyperlinkButton(I18n.tr("Create List"), createAction);
        createListButton.setFont(createListFont);
    }
    
    public void selectLocalFileList(LocalFileList localFileList) {
        table.selectLibraryNavItem(localFileList);
    }
    
    public void addTableSelectionListener(ListSelectionListener listener) {
        table.getSelectionModel().addListSelectionListener(listener);
    }
    
    public int getSelectedRow() {
        return table.getSelectedRow();
    }
    
    public LibraryNavItem getSelectedNavItem() {
        return table.getSelectedItem();
    }

    /**
     * Selects the specified SharedFileList in the library nav and starts editing on its name.
     * @param sharedFileList can not be the public shared list
     */
    public void editSharedListName(SharedFileList sharedFileList) {
        assert(!sharedFileList.isPublic());
        selectLocalFileList(sharedFileList);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                table.setEditable(true);
                table.editCellAt(getSelectedRow(), 0);
                createListButton.setEnabled(false);
            }
        });
    }
    
    public int getPrivateListCount() {
        return table.getPrivateSharedLibraryCount();
    }
}


