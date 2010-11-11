package org.limewire.ui.swing.friends.chat;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.Comparator;
import java.util.List;

import javax.swing.JToolTip;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;

import org.jdesktop.application.Resource;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.Network;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.table.GlazedJXTable;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.Objects;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * A Table that represents all of the friends that are online. 
 */
@LazySingleton
class ChatFriendList extends GlazedJXTable {

    @Resource private Color backgroundColor;
    @Resource private Color tooltipBackground;
    
    private TablePopupHandler popupHandler;
    
    final FilterList<ChatFriend> filter;
    private final Provider<ChatFrame> chatFrame;
    
    @Inject
    public ChatFriendList(ChatModel chatModel, ConversationPanel conversationPanel,
            Provider<ChatFrame> chatFrame) {
        this.chatFrame = chatFrame;
        
        GuiUtils.assignResources(this);
          
        initialize();
        
        ObservableElementList<ChatFriend> observableList = GlazedListsFactory.observableElementList(chatModel.getChatFriendList(), GlazedLists.beanConnector(ChatFriend.class));
        SortedList<ChatFriend> sortedFriends = GlazedListsFactory.sortedList(observableList,  new AvailableComparator());
        final TextMatcherEditor<ChatFriend> editor = new TextMatcherEditor<ChatFriend>(new FriendFilterator());
        filter = GlazedListsFactory.filterList(sortedFriends, editor);

        setModel(new DefaultEventTableModel<ChatFriend>(filter, new ChatTableFormat()));
        
        TableColumnModel columnModel = getColumnModel();
        columnModel.getColumn(0).setCellRenderer(new ChatFriendRenderer(conversationPanel));
    }
    
    private void initialize() {
        setBackground(backgroundColor);
        setFillsViewportHeight(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setShowGrid(false, false);
        setTableHeader(null);
        setRowHeight(18);
    }
    
    @Inject
    void register() {
        CloseListener listener = new CloseListener();
        addMouseListener(listener);
        addMouseMotionListener(listener);
        addMouseListener(new MouseAdapter() {            
            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger() && popupHandler != null) {
                    int col = columnAtPoint(e.getPoint());
                    int row = rowAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0) {
                        getSelectionModel().setSelectionInterval(row, row);
                        popupHandler.maybeShowPopup(e.getComponent(), e.getX(), e.getY());
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
    public JToolTip createToolTip() {
        JToolTip tooltip = super.createToolTip();
        tooltip.setBackground(tooltipBackground);
        return tooltip;
    }
    
    /**
     * Custom tooltip text to display information about the friend.
     */
    @Override
    public String getToolTipText(MouseEvent event) {
        Point mousePoint = event.getPoint();
        int row = rowAtPoint(mousePoint);
        if (row == -1) {
            return null;
        }

        ChatFriend chatFriend = filter.get(row);
        
        if (chatFriend.isChatting() && isOverClose(mousePoint)) {
            return tr("Close Conversation");
        }
        
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html>")
            .append("<head>")
            .append("<style>body { margin: 2px 10px 2px 4px;}</style>")
            .append("</head>")
            .append("<body>")
            .append("<img src=\"")
            .append(getIconURL(chatFriend.getMode()))
            .append("\"/>&nbsp;")
            .append("<b>").append(chatFriend.getName());
        
        if(chatFriend.getFriend().getNetwork().getType() != Network.Type.FACEBOOK) {
            tooltip.append(" &lt;").append(chatFriend.getID())
                .append("&gt;");
        }
            
        tooltip.append("</b><br/>");
        String status = chatFriend.getStatus();
        if (status != null && status.length() > 0) {
            tooltip.append("<div>").append("<b>" + I18n.tr("Status: ") + "</b>").append(status).append("</div>");
        }
        tooltip.append("</body>")
            .append("</html>");
        return tooltip.toString();
    }
    
    /**
     * Returns the friend that is currently selected in the list,
     * if no friend is selected or the list is empty returns null.
     */
    public ChatFriend getSelectedFriend() {
        int selectedIndex = getSelectedRow();
        if(selectedIndex >= 0 && selectedIndex < getRowCount()) {
            return filter.get(selectedIndex);
        }
        return null;
    }
    
    /**
     * Sets a popupHandler for displaying a popup menu when 
     * the table is right clicked with the mouse.
     */
    public void setPopupHandler(TablePopupHandler popupHandler) {
        this.popupHandler = popupHandler;
    }
    
    private static class ChatTableFormat implements TableFormat<ChatFriend> {
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            return "";
        }

        @Override
        public Object getColumnValue(ChatFriend chatFriend, int column) {
            return chatFriend;
        }       
    }
    
    /**
     * Comparator to sort friends in the list. Lists are sorted alphabetically
     * by those who have conversations started then alphabetically by the state
     * of the friend presence.
     */
    private class AvailableComparator implements Comparator<ChatFriend> {
        @Override
        public int compare(ChatFriend friend1, ChatFriend friend2) {
            boolean friend1IsChatting = friend1.isChatting() || friend1.hasUnviewedMessages();
            boolean friend2IsChatting = friend2.isChatting() || friend2.hasUnviewedMessages();
            
            if(friend1IsChatting && friend2IsChatting) {
                return friend1.getChatStartTime() < friend2.getChatStartTime() ? -1 : friend1.getChatStartTime() == friend2.getChatStartTime() ? 0 : 1;
            } else if(friend1IsChatting) {
                return -1;
            } else if (friend2IsChatting) {
                return 1;
            } 
            
            int friend1ModeIndex = friend1.getMode().getOrder();
            int friend2ModeIndex = friend2.getMode().getOrder();
            
            if (friend1ModeIndex > friend2ModeIndex) {
                return 1;
            } else if (friend2ModeIndex > friend1ModeIndex) {
                return -1;
            }

            return Objects.compareToNullIgnoreCase(friend1.getName(), friend2.getName(), false);
        }
    }
    
    private class FriendFilterator implements TextFilterator<ChatFriend> {
        @Override
        public void getFilterStrings(List<String> baseList, ChatFriend element) {
            baseList.add(element.getName());
        }
    }
    
    /**
     * Returns true if the mouse is over the position where the icon
     * is displayed.
     */
    private boolean isOverClose(Point mousePosition) {
        return mousePosition.x > 2 && mousePosition.x < 10;
    }
    
    /**
     * Listens for mouse actions to close an active chat from within the table.
     * If a conversation is active, and the mouse is over the icon, converts the
     * mouse to a hand and will close the chat if clicked.
     */
    private class CloseListener extends MouseAdapter {

        @Override
        public void mouseMoved(MouseEvent e) {
            int row = rowAtPoint(e.getPoint());
            if(row >= 0) {
                ChatFriend friend = filter.get(row);
                if(friend.isChatting() && isOverClose(e.getPoint())) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    setCursor(Cursor.getDefaultCursor());
                }
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            int row = rowAtPoint(e.getPoint());
            if(row >= 0) {
                ChatFriend friend = filter.get(row);
                if(friend.isChatting() && isOverClose(e.getPoint()) && e.getClickCount() == 1 &&
                        e.getButton() == MouseEvent.BUTTON1) {
                    chatFrame.get().closeConversation(friend);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }

        // ensure that the cursor is returned to the default when exiting 
        // the table.
        @Override
        public void mouseExited(MouseEvent e) {
            setCursor(Cursor.getDefaultCursor());
        }       
    }
    
    /**
     * Returns the URL of an icon for displaying the status icon in the tooltip.
     */
    private String getIconURL(FriendPresence.Mode mode) {
        switch(mode) {
        case available:
            return getURL("/org/limewire/ui/swing/mainframe/resources/icons/friends/available.png");
        case dnd:
            return getURL("/org/limewire/ui/swing/mainframe/resources/icons/friends/doNotDisturb.png");
        }
        return getURL("/org/limewire/ui/swing/mainframe/resources/icons/friends/away.png");
    }
    
    private String getURL(String path) {
        URL resource = ChatFriendList.class.getResource(path);
        return resource != null ? resource.toExternalForm() : "";
    }
}
