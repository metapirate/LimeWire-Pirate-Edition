package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendEvent;
import org.limewire.inject.LazySingleton;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.table.GlazedJXTable;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.impl.swing.SwingThreadProxyEventList;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@LazySingleton
class LibrarySharingFriendListPanel {

    @Resource private Font labelFont;
    @Resource private Color labelColor;
    @Resource private Font linkFont;
    @Resource private Color backgroundColor;
    
    private final JPanel component;
    private final JLabel headerLabel;
    private final HyperlinkButton editButton;
    private final Map<String, Friend> knownFriends;
    private final List<String> sharedIds;
    
    private final LibrarySharingFriendListRenderer renderer;
    private final EventList<String> eventList;
    private final JXTable friendList;
    private final JScrollPane scrollPane;

    @Inject
    public LibrarySharingFriendListPanel(EditSharingAction sharingAction, @Named("known") Map<String, Friend> knownFriends) {
        GuiUtils.assignResources(this);
        this.knownFriends = knownFriends;        
        this.friendList = new GlazedJXTable();
        this.sharedIds = new ArrayList<String>();
        
        eventList = new BasicEventList<String>();
        SortedList<String> sortedList = GlazedListsFactory.sortedList(eventList, new FriendComparator());
        SwingThreadProxyEventList<String> stpl = GlazedListsFactory.swingThreadProxyEventList(sortedList);
        friendList.setModel(new DefaultEventTableModel<String>(stpl, new FriendTableFormat()));
        
        component = new JPanel(new MigLayout("insets 0, gap 0, fillx", "134!", ""));        
        component.setOpaque(false);
        
        headerLabel = new JLabel();
        headerLabel.setFont(labelFont);
        headerLabel.setForeground(labelColor);
        component.add(headerLabel, "aligny top, gaptop 8, gapleft 6, gapbottom 6, wrap");

        friendList.setTableHeader(null);
        friendList.setShowGrid(false, false);
        friendList.setFocusable(false);
        friendList.setBackground(backgroundColor);
        
        scrollPane = new JScrollPane(friendList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setMinimumSize(new Dimension(0,0));
        scrollPane.setBorder(BorderFactory.createEmptyBorder(1,0,1,0));
        scrollPane.setBackground(backgroundColor);
        
        renderer = new LibrarySharingFriendListRenderer(scrollPane);
        friendList.getColumnExt(0).setCellRenderer(renderer);
       
        component.add(scrollPane, "grow, wrap");
        
        editButton = new HyperlinkButton(I18n.tr("Edit Sharing"), sharingAction);
        editButton.setFont(linkFont);
        component.add(editButton, "aligny top, gaptop 5, gapleft 6, gapbottom 5, wrap");
        
        scrollPane.getVerticalScrollBar().addComponentListener(new ComponentListener(){
            @Override
            public void componentHidden(ComponentEvent e) {
                scrollPane.setBorder(BorderFactory.createEmptyBorder(1,0,1,0));
            }

            @Override
            public void componentShown(ComponentEvent e) {
                scrollPane.setBorder(BorderFactory.createMatteBorder(1,0,1,0, Color.BLACK));
            }
            
            @Override
            public void componentMoved(ComponentEvent e) {}
            @Override
            public void componentResized(ComponentEvent e) {}
        });
    }
    
    @Inject void register(@Named("known") ListenerSupport<FriendEvent> friendSupport) {
        friendSupport.addListener(new EventListener<FriendEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendEvent event) {
                switch(event.getType()) {
                case ADDED:
                    setSharedFriendIds(sharedIds);
                    break;
                }
            }
        });
    }
    
    void clear() {
        eventList.clear();
    }
    
    /** Sets the list of IDs this is shared with. */
    void setSharedFriendIds(List<String> newFriendIds) {
        if(!newFriendIds.isEmpty()) {
            headerLabel.setText(I18n.tr("Sharing list with..."));
        } else {
            headerLabel.setText(I18n.tr("Not Shared"));
        }
        
        // Only set if we're not refreshing..
        if(newFriendIds != sharedIds) {
            sharedIds.clear();
            sharedIds.addAll(new ArrayList<String>(newFriendIds));
        }
        
        List<String> newModel = new ArrayList<String>();
        int unknown = 0;
        for(String id : sharedIds) {
            Friend friend = knownFriends.get(id);
            if(friend != null) {
                newModel.add(friend.getRenderName());
            } else {
                unknown++;
            }
        }
        if(unknown > 0) {
            newModel.add(I18n.tr("{0} friends from other accounts", unknown));
            // TODO: Do something about row sizes.
            friendList.setRowHeightEnabled(true);            
        } else {
            friendList.setRowHeightEnabled(false);
        }
        eventList.clear();
        eventList.addAll(newModel);
        friendList.setVisibleRowCount(newModel.size());
        component.revalidate();
    }
    
    JComponent getComponent() {
        return component;
    }
    
    private static class FriendComparator implements Comparator<String> {
        @Override
        public int compare(String name1, String name2) {
            return name1.compareToIgnoreCase(name2);
        }
    }
    
    private static class FriendTableFormat implements TableFormat<String> {
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            return "";
        }

        @Override
        public Object getColumnValue(String baseObject, int column) {
            return baseObject;
        }       
    }
}
