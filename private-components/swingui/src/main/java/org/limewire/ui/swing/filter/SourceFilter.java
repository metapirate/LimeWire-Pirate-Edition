package org.limewire.ui.swing.filter;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.friend.api.Friend;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.RolloverCursorListener;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.Objects;

import ca.odell.glazedlists.CollectionList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.UniqueList;
import ca.odell.glazedlists.FunctionList.Function;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;

/**
 * Filter component to select items according to their sources.
 */
class SourceFilter<E extends FilterableItem> extends AbstractFilter<E> {

    private final JPanel panel = new JPanel();
    private final JLabel label = new JLabel();
    private final JXList list = new JXList();
    private final HyperlinkButton moreButton = new HyperlinkButton();
    private final List<FriendListener> friendListenerList = new ArrayList<FriendListener>();
    
    private EventList<SourceItem> sourceList;
    private UniqueListFactory<SourceItem> uniqueSourceListFactory;
    private UniqueList<SourceItem> uniqueSourceList;
    
    private EventList<SourceItem> friendList;
    private UniqueListFactory<SourceItem> uniqueFriendListFactory;
    private UniqueList<SourceItem> uniqueFriendList;
    
    private UniqueList<SourceItem> currentUniqueList;
    private SortedList<SourceItem> sortedList;
    private DefaultEventListModel<SourceItem> listModel;
    private DefaultEventSelectionModel<SourceItem> selectionModel;
    private DefaultEventSelectionModel<SourceItem> popupSelectionModel;
    private SelectionListener selectionListener;
    private FilterPopupPanel filterPopupPanel;

    private boolean anyFriendFound;
    
    /**
     * Constructs a SourceFilter using the specified results list.
     */
    public SourceFilter(EventList<E> resultsList) {
        FilterResources resources = getResources();
        
        // Set up visual components.
        panel.setLayout(new MigLayout("insets 0 0 0 0, gap 0!", 
                "[left,grow]", ""));
        panel.setOpaque(false);
        
        label.setFont(resources.getHeaderFont());
        label.setForeground(resources.getHeaderColor());
        label.setText(I18n.tr("From"));
        
        list.setCellRenderer(new SourceCellRenderer(resources.getBackground(), 
                BorderFactory.createEmptyBorder(1, 7, 0, 7), false));
        list.setFont(resources.getRowFont());
        list.setForeground(resources.getRowColor());
        list.setOpaque(false);
        list.setRolloverEnabled(true);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add highlighter for rollover.
        list.setHighlighters(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW,
                resources.getHighlightBackground(), resources.getHighlightForeground()));
        
        // Add listener to show cursor on mouse over.
        new RolloverCursorListener().install(list);
        
        moreButton.setAction(new MoreAction());
        moreButton.setBorder(BorderFactory.createEmptyBorder(0, 1, 1, 1));
        moreButton.setContentAreaFilled(false);
        moreButton.setFocusPainted(false);
        moreButton.setFont(resources.getRowFont());
        moreButton.setHorizontalTextPosition(JButton.LEADING);
        
        // Add listener to set popup trigger indicator.  This activates logic
        // so that pressing "more" a second time closes an open popup.
        moreButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (filterPopupPanel != null) {
                    filterPopupPanel.setPopupTriggered(true);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (filterPopupPanel != null) {
                    filterPopupPanel.setPopupTriggered(false);
                }
            }
        });
        
        // Apply results list to filter.
        initialize(resultsList);
        
        // Calculate max list height.
        list.setPrototypeCellValue("Type");
        int listHeight = 3 * list.getFixedCellHeight();
        
        panel.add(label     , "gap 6 6, wrap");
        panel.add(list      , "hmax " + listHeight + ", grow, wrap");
        panel.add(moreButton, "gap 6 6");
    }
    
    /**
     * Initializes the filter using the specified list of search results.
     */
    private void initialize(EventList<E> resultsList) {
        // Create list of unique source values.
        sourceList = createSourceList(resultsList);
        uniqueSourceListFactory = new UniqueListFactory<SourceItem>(sourceList, new SourceItemComparator());
        uniqueSourceListFactory.setName(I18n.tr("Sources"));
        uniqueSourceList = uniqueSourceListFactory.getUniqueList();
        
        // Create list of unique friend values.
        friendList = createFriendList(resultsList);
        uniqueFriendListFactory = new UniqueListFactory<SourceItem>(friendList, new SourceItemComparator());
        uniqueFriendListFactory.setName(I18n.tr("Friends"));
        uniqueFriendList = uniqueFriendListFactory.getUniqueList();
        
        // Add listener to update unique list in use.
        uniqueSourceList.addListEventListener(new ListEventListener<SourceItem>() {
            @Override
            public void listChanged(ListEvent<SourceItem> listChanges) {
                updateAnonymousFound();
                updateAnyFriendFound();
                updateMoreVisibility();
            }
        });
        
        // Add listener to display "more" button when needed.
        uniqueFriendList.addListEventListener(new ListEventListener<SourceItem>() {
            @Override
            public void listChanged(ListEvent<SourceItem> listChanges) {
                updateMoreVisibility();
            }
        });
        
        // Set unique list for filter.
        currentUniqueList = uniqueFriendList;
        updateMoreVisibility();
        updateFilterList();
    }
    
    /**
     * Updates the filter list using the current unique list.
     */
    private void updateFilterList() {
        // Dispose of old models and lists.
        if (selectionListener != null) list.removeListSelectionListener(selectionListener);
        if (listModel != null) listModel.dispose();
        if (selectionModel != null) selectionModel.dispose();
        if (sortedList != null) sortedList.dispose();
        
        // Create sorted list using current unique list.
        sortedList = GlazedListsFactory.sortedList(currentUniqueList, new SourceItemCountComparator());
        
        // Create list and selection models.
        listModel = new DefaultEventListModel<SourceItem>(sortedList);
        selectionModel = new DefaultEventSelectionModel<SourceItem>(sortedList);
        list.setSelectionModel(selectionModel);
        list.setModel(listModel);
        
        // Add selection listener to update filter.
        selectionListener = new SelectionListener(selectionModel);
        list.addListSelectionListener(selectionListener);
    }
    
    /**
     * Adds the specified listener to the list that is notified when the
     * <code>anyFriendFound</code> state changes.
     */
    public void addFriendListener(FriendListener listener) {
        friendListenerList.add(listener);
    }
    
    /**
     * Removes the specified listener from the list that is notified when the
     * <code>anyFriendFound</code> state changes.
     */
    public void removeFriendListener(FriendListener listener) {
        friendListenerList.remove(listener);
    }
    
    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void reset() {
        // Clear selections.
        if (selectionModel != null) {
            selectionModel.clearSelection();
        }
        if (popupSelectionModel != null) {
            popupSelectionModel.clearSelection();
        }
        // Deactivate filter.
        deactivate();
    }
    
    @Override
    public void dispose() {
        // Dispose of source and friend lists.
        uniqueSourceListFactory.dispose();
        uniqueFriendListFactory.dispose();
        ((TransformedList) sourceList).dispose();
        ((TransformedList) friendList).dispose();
    }
    
    /**
     * Activates the filter using the specified text description and matcher.
     * This method also hides the filter component.
     */
    @Override
    protected void activate(String activeText, Matcher<E> matcher) {
        super.activate(activeText, matcher);
        getComponent().setVisible(false);
    }
    
    /**
     * Deactivates the filter by clearing the text description and matcher.
     * This method also displays the filter component.
     */
    @Override
    protected void deactivate() {
        super.deactivate();
        getComponent().setVisible(anyFriendFound);
    }
    
    /**
     * Returns a text description of the filter state.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        
        buf.append(getClass().getSimpleName()).append("[");
        buf.append("uniqueItems=").append(currentUniqueList.size());
        buf.append(", active=").append(isActive());
        EventList<SourceItem> selectedList = selectionModel.getSelected();
        buf.append(", selection=").append((selectedList.size() > 0) ? selectedList.get(0) : "null");
        buf.append("]");
        
        return buf.toString();
    }
    
    /**
     * Returns a list of SourceItem objects that represent the sources for the 
     * elements in the specified results list.
     */
    private EventList<SourceItem> createSourceList(EventList<E> resultsList) {
        // Create collection list model.
        CollectionList.Model<E, SourceItem> model = new CollectionList.Model<E, SourceItem>() {
            @Override
            public List<SourceItem> getChildren(E parent) {
                List<SourceItem> list = new ArrayList<SourceItem>();
                if (parent.isAnonymous()) {
                    list.add(SourceItem.ANONYMOUS_SOURCE);
                }
                if (parent.getFriends().size() > 0) {
                    list.add(SourceItem.ANY_FRIEND_SOURCE);
                }
                return list;
            }
        };
        
        // Create collection list.
        return GlazedListsFactory.collectionList(resultsList, model);
    }
    
    /**
     * Returns a list of SourceItem objects that represent the friends for the 
     * elements in the specified results list.
     */
    private EventList<SourceItem> createFriendList(EventList<E> resultsList) {
        // Create collection list model for friends.
        CollectionList.Model<E, Friend> model = new CollectionList.Model<E, Friend>() {
            @Override
            public List<Friend> getChildren(E parent) {
                Collection<Friend> friends = parent.getFriends();
                return new ArrayList<Friend>(friends);
            }
        };
        
        // Create collection list for friends.
        CollectionList<E, Friend> collectionList = GlazedListsFactory.collectionList(resultsList, model);
        
        // Create function list.
        return GlazedListsFactory.simpleFunctionList(collectionList, new SourceItemFriendFunction());
    }
    
    /**
     * Determines whether anonymous sources are found, and updates the current
     * unique list being displayed.  When anonymous sources are found, the
     * P2P Network/Any Friends list is used; otherwise, the friends-only list
     * is used.
     */
    private void updateAnonymousFound() {
        // Determine if anonymous sources are found.
        boolean found = uniqueSourceList.contains(SourceItem.ANONYMOUS_SOURCE);
        
        // Update current unique list only if changed.
        UniqueList<SourceItem> newList = found ? uniqueSourceList : uniqueFriendList;
        if (currentUniqueList == newList) return;
        currentUniqueList = newList;
        
        // Update button label.
        moreButton.getAction().putValue(Action.NAME, found ? I18n.tr("friends") : I18n.tr("more"));
        
        // Post Runnable on event queue to update list.  Because this method is
        // handling a listChanged event, we need to post a new event to modify
        // the list.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateFilterList();
            }
        });
    }
    
    /**
     * Determines whether any friend sources are found, and notifies listeners
     * when the state changes.
     */
    private void updateAnyFriendFound() {
        // Determine if any friend sources are found.
        boolean found = uniqueSourceList.contains(SourceItem.ANY_FRIEND_SOURCE);
        
        // Update indicator if necessary.
        if (anyFriendFound == found) return;
        anyFriendFound = found;
        
        // Notify listeners about state change.
        for (int i = 0, size = friendListenerList.size(); i < size; i++) {
            friendListenerList.get(i).friendFound(found);
        }
    }
    
    /**
     * Updates the visibility of the more button.  For the P2P Network/Any 
     * Friends list, the button is always displayed; for the Friends-only list,
     * the button is displayed when there are more than three friends.  
     */
    private void updateMoreVisibility() {
        boolean visible = (currentUniqueList == uniqueSourceList) || (uniqueFriendList.size() > 3);
        
        if (!moreButton.isVisible() && visible) {
            moreButton.setVisible(true);
        } else if (moreButton.isVisible() && !visible) {
            moreButton.setVisible(false);
        }
    }
    
    /**
     * Creates a popup to display the complete list of friends.
     */
    private FilterPopupPanel createFriendPopup() {
        FilterPopupPanel popupPanel = new FilterPopupPanel(getResources(), I18n.tr("Friends"));
        
        // Set list cell renderer.
        popupPanel.setListCellRenderer(new SourceCellRenderer(popupPanel.getBackground(), 
                BorderFactory.createEmptyBorder(1, 4, 0, 1), true));
        
        // Set list and selection models.  We use the unique list directly
        // to display values alphabetically.
        DefaultEventListModel<SourceItem> listModel = new DefaultEventListModel<SourceItem>(uniqueFriendList);
        popupSelectionModel = new DefaultEventSelectionModel<SourceItem>(uniqueFriendList);
        popupPanel.setListModel(listModel);
        popupPanel.setListSelectionModel(popupSelectionModel);
        
        // Add selection listener to update filter.
        popupSelectionModel.addListSelectionListener(new SelectionListener(popupSelectionModel));
        
        return popupPanel;
    }
    
    /**
     * Displays the "friend" popup that lists all friends.
     */
    private void showFriendPopup() {
        if (filterPopupPanel == null) {
            filterPopupPanel = createFriendPopup();
        }
        filterPopupPanel.showPopup(moreButton, list.getWidth() - 12, label.getY() - moreButton.getY());
    }
    
    /**
     * Hides the "friend" popup that lists all friends.
     */
    private void hideFriendPopup() {
        if (filterPopupPanel != null) {
            filterPopupPanel.hidePopup();
        }
    }
    
    /**
     * Action to display list of all friends in popup window.
     */
    private class MoreAction extends AbstractAction {

        public MoreAction() {
            super(I18n.tr("more"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (filterPopupPanel == null) {
                showFriendPopup();
            } else if (filterPopupPanel.isPopupReady()) {
                showFriendPopup();
            } else {
                filterPopupPanel.setPopupReady(true);
            }
        }
    }
    
    /**
     * Listener to handle selection changes to update the matcher editor.  
     */
    private class SelectionListener implements ListSelectionListener {
        private final DefaultEventSelectionModel<SourceItem> selectionModel;
        
        public SelectionListener(DefaultEventSelectionModel<SourceItem> selectionModel) {
            this.selectionModel = selectionModel;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            // Skip selection change if filter is active.
            if (isActive()) {
                return;
            }
            
            // Get list of selected values.
            EventList<SourceItem> selectedList = selectionModel.getSelected();
            if (selectedList.size() > 0) {
                SourceItem value = selectedList.get(0);
                // Create new matcher and activate.
                Matcher<E> newMatcher = new SourceMatcher<E>(value);
                activate(value.toString(), newMatcher);
                
            } else {
                // Deactivate to clear matcher.
                deactivate();
            }
            
            // Hide popup if showing.
            hideFriendPopup();
            
            // Notify filter listeners.
            fireFilterChanged(SourceFilter.this);
        }
    }
    
    /**
     * Cell renderer for SourceItem values.  Note that the filter list uses the
     * current unique list, while the popup list always uses the unique friend
     * list.
     */
    private class SourceCellRenderer extends DefaultListCellRenderer {
        private final Color background;
        private final Border border;
        private final boolean useFriend;
        
        public SourceCellRenderer(Color background, Border border, boolean useFriend) {
            this.background = background;
            this.border = border;
            this.useFriend = useFriend;
        }
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            Component renderer = super.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);
            
            if ((renderer instanceof JLabel) && (value instanceof SourceItem)) {
                // Get count.
                int count = useFriend ? uniqueFriendList.getCount((SourceItem) value) :
                    currentUniqueList.getCount((SourceItem) value);
                
                // Set text.
                StringBuilder buf = new StringBuilder();
                buf.append(((SourceItem) value).getName());
                buf.append(" (").append(count).append(")");
                ((JLabel) renderer).setText(buf.toString());
                
                // Set appearance.
                ((JLabel) renderer).setBackground(background);
                ((JLabel) renderer).setBorder(border);
            }

            return renderer;
        }
    }
    
    /**
     * A Comparator for SourceItem values.
     */
    private static class SourceItemComparator implements Comparator<SourceItem> {

        @Override
        public int compare(SourceItem item1, SourceItem item2) {
            if (item1.getType() == item2.getType()) {
                String name1 = item1.getName();
                String name2 = item2.getName();
                return Objects.compareToNullIgnoreCase(name1, name2, false);
                
            } else if (item1.getType() == SourceItem.Type.ANONYMOUS) {
                return 1;
            } else if (item2.getType() == SourceItem.Type.ANONYMOUS) {
                return -1;
            } else if (item1.getType() == SourceItem.Type.ANY_FRIEND) {
                return 1;
            } else {
                return -1;
            }
        }
    }
    
    /**
     * A Comparator to sort SourceItem values by their result count.
     */
    private class SourceItemCountComparator implements Comparator<SourceItem> {

        @Override
        public int compare(SourceItem item1, SourceItem item2) {
            int count1 = currentUniqueList.getCount(item1);
            int count2 = currentUniqueList.getCount(item2);
            // Return inverse value to sort in descending order.
            return (count1 < count2) ? 1 : ((count1 > count2) ? -1 : 0);
        }
    }
    
    /**
     * A function to transform a list of friends into a list of source 
     * items.
     */
    private static class SourceItemFriendFunction implements Function<Friend, SourceItem> {

        @Override
        public SourceItem evaluate(Friend sourceValue) {
            return new SourceItem(SourceItem.Type.FRIEND, sourceValue.getRenderName());
        }
    }
    
    /**
     * Defines a listener for friend found events. 
     */
    public static interface FriendListener {
        
        void friendFound(boolean found);
        
    }
}
