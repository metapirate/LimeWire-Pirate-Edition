package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXTable;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.friend.api.Friend;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.table.GlazedJXTable;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.WritableTableFormat;
import ca.odell.glazedlists.impl.swing.SwingThreadProxyEventList;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@LazySingleton
class LibrarySharingEditablePanel {
    
    @Resource private Color borderColor;
    @Resource private Font sharingLabelFont;
    @Resource private Color sharingLabelColor;
    @Resource private Font selectFont;
    @Resource private Color selectColor;
    
    private final JPanel component;
    private final PromptTextField filterTextField;
    private final HyperlinkButton allButton;
    private final HyperlinkButton noneButton;
    private final JXButton applyButton;
    private final HyperlinkButton cancelButton;
    
    private final Map<String, Friend> knownFriends;
    
    private final EventList<EditableSharingData> baseEventList;
    private final EventList<EditableSharingData> filteredList;
    private final JXTable friendTable;
    
    @Inject
    public LibrarySharingEditablePanel(ApplySharingAction applyAction,
            CancelSharingAction cancelAction, LibrarySharingAction libraryAction, TextFieldDecorator textFieldDecorator,
            ButtonDecorator buttonDecorator, @Named("known") Map<String, Friend> knownFriends) {
        GuiUtils.assignResources(this);
        this.knownFriends = knownFriends;
        
        component = new JPanel(new MigLayout("insets 0, gap 0, fillx", "[134!]", ""));        
        component.setOpaque(false);
        
        JLabel shareLabel = new JLabel(I18n.tr("Sharing list with..."));
        shareLabel.setFont(sharingLabelFont);
        shareLabel.setForeground(sharingLabelColor);
        component.add(shareLabel, "gapleft 5, gaptop 5, wrap");        
        
        filterTextField = new PromptTextField(I18n.tr("Filter..."));
        textFieldDecorator.decorateClearablePromptField(filterTextField, AccentType.NONE);
        component.add(filterTextField, "gapleft 5, gaptop 4, gapright 5, wmax 124, wrap");

        JLabel selectLabel = new JLabel(I18n.tr("Select"));
        selectLabel.setFont(selectFont);
        selectLabel.setForeground(selectColor);
        component.add(selectLabel, "gapleft 5, gaptop 2, split 3");
        
        allButton = new HyperlinkButton(new AbstractAction(I18n.tr("all")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSelectedStateForAll(true);
            }
        });
        allButton.setFont(selectFont);
        noneButton = new HyperlinkButton(new AbstractAction(I18n.tr("none")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSelectedStateForAll(false);
            }
        });
        noneButton.setFont(selectFont);
        component.add(allButton, "gapleft 6, gaptop 2");
        component.add(noneButton, "gapleft 6, gaptop 2, wrap");
                
        baseEventList = new BasicEventList<EditableSharingData>();
        SortedList<EditableSharingData> sortedList = GlazedListsFactory.sortedList(baseEventList, new FriendComparator());        
        MatcherEditor<EditableSharingData> matcher = new TextComponentMatcherEditor<EditableSharingData>(filterTextField, new FriendFilterator());
        filteredList = GlazedListsFactory.filterList(sortedList, matcher);
        SwingThreadProxyEventList<EditableSharingData> stpl = GlazedListsFactory.swingThreadProxyEventList(filteredList);
        friendTable = new GlazedJXTable(new DefaultEventTableModel<EditableSharingData>(stpl, new EditTableFormat())) {
            @Override
            public void editingStopped(ChangeEvent e) {
                TableCellEditor editor = getCellEditor();
                if(editor != null) {
                    removeEditor();
                }
            }
            
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                return new Dimension(super.getPreferredScrollableViewportSize().width, getModel().getRowCount() * getRowHeight());
            }
        };
        // the table is strictly the size of the number of rows or full screen with a scrollbar
        // if it surpasses available space. When adding/removing friends or filtering need to
        // revalidate the size to correctly update the panel and table sizing.
        filteredList.addListEventListener(new ListEventListener<EditableSharingData>(){
            @Override
            public void listChanged(ListEvent<EditableSharingData> listChanges) {
                if(LibrarySharingEditablePanel.this.getComponent().isShowing()) {
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            component.revalidate();
                        }
                    });
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(friendTable);
        scrollPane.setMinimumSize(new Dimension(0,0));
        scrollPane.setBorder(BorderFactory.createMatteBorder(1,0,1,0, borderColor)); 
        friendTable.getColumnExt(0).setCellRenderer(new LibrarySharingEditableRendererEditor());
        friendTable.getColumnExt(0).setCellEditor(new LibrarySharingEditableRendererEditor());
        friendTable.setTableHeader(null);
        friendTable.setShowGrid(false, false);
        friendTable.setRowHeight(20);
                
        component.add(scrollPane, "growx, gaptop 3, wrap");
        
        applyButton = new JXButton(applyAction);
        applyButton.setFont(selectFont);
        applyButton.addActionListener(libraryAction);
        buttonDecorator.decorateDarkFullButton(applyButton, AccentType.NONE);
        cancelButton = new HyperlinkButton(cancelAction);
        cancelButton.setFont(selectFont);
        cancelButton.addActionListener(libraryAction);
        
        component.add(applyButton, "split 2, gaptop 5, gapbottom 5, gapright unrelated, alignx center");
        component.add(cancelButton, "gaptop 5, gapbottom 5, wrap");
    }
    
    public JComponent getComponent() {
        return component;
    }
    
    void clear() {
        baseEventList.clear();
    }
    
    void refreshSelectedList() {
        Set<String> selectedIds = new HashSet<String>();
        for(EditableSharingData data : baseEventList) {
            if(data.isSelected()) {
                selectedIds.addAll(data.getIds());
            }
        }
        addItemsToList(selectedIds);
    }
    
    void editWithSelectedIds(List<String> selectedIds) {
        filterTextField.setText("");
        addItemsToList(new HashSet<String>(selectedIds));
    }
    
    private void addItemsToList(Set<String> setOfIds) {
        baseEventList.clear();
        for(Friend friend : knownFriends.values()) {
            baseEventList.add(new EditableSharingData(friend, setOfIds.remove(friend.getId())));
        }
        if(!setOfIds.isEmpty()) {
            baseEventList.add(new EditableSharingData(new ArrayList<String>(setOfIds), true));
        }
        component.revalidate();
    }
    
    /**
     * Returns a list of Friends who this list is shared with.
     */
    List<String> getSelectedFriendIds() {
        List<String> friends = new ArrayList<String>();
        for(EditableSharingData data : baseEventList) {
            if(data.isSelected()) {
                friends.addAll(data.getIds());
            }
        }
        return friends;
    }
    
    private void setSelectedStateForAll(boolean isSelected) {
        for(EditableSharingData data : filteredList) {
            data.setSelected(isSelected);
        }
        friendTable.repaint();
    }
    
    /**
     * Filters on the displayed name of a Friend.
     */
    private static class FriendFilterator implements TextFilterator<EditableSharingData> {
        @Override
        public void getFilterStrings(List<String> baseList, EditableSharingData data) {
            Friend friend = data.getFriend();
            if(friend != null) {
                if(friend.getName() != null) {
                    baseList.add(friend.getName());
                }
                baseList.add(friend.getId());
            }
        }
    }
    
    private static class FriendComparator implements Comparator<EditableSharingData> {
        @Override
        public int compare(EditableSharingData data1, EditableSharingData data2) {
            Friend friend1 = data1.getFriend();
            Friend friend2 = data2.getFriend();
            if(friend1 == friend2)
                return 0;
            if(friend1 == null || friend2 == null) {
                if(friend1 == null && friend2 == null)
                    return 0;
                else if(friend1 == null)
                    return 1;
                else
                    return -1;
            } else
                return friend1.getRenderName().compareToIgnoreCase(friend2.getRenderName());
        }
    }
    
    private static class EditTableFormat implements WritableTableFormat<EditableSharingData> {
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            return "";
        }

        @Override
        public Object getColumnValue(EditableSharingData baseObject, int column) {
            return baseObject;
        }
        
        @Override
        public boolean isEditable(EditableSharingData baseObject, int column) {
            return true;
        }
        
        @Override
        public EditableSharingData setColumnValue(EditableSharingData baseObject,
                Object editedValue, int column) {
            return baseObject;
        }
        
    }
}
