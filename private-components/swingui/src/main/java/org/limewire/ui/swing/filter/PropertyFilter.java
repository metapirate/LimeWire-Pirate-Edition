package org.limewire.ui.swing.filter;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.RolloverCursorListener;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.Objects;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.UniqueList;
import ca.odell.glazedlists.FunctionList.Function;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;

import com.google.inject.Provider;

/**
 * Filter component to select items according to a collection of property 
 * values.
 */
class PropertyFilter<E extends FilterableItem> extends AbstractFilter<E> {

    private final FilterType filterType;
    private final FilePropertyKey propertyKey;
    private final Provider<IconManager> iconManager;
    
    private final JPanel panel = new JPanel();
    private final JLabel propertyLabel = new JLabel();
    private final JXList list = new JXList();
    private final HyperlinkButton moreButton = new HyperlinkButton();
    
    private EventList<Object> propertyList;
    private FilterList<Object> nonNullList;
    private UniqueListFactory<Object> uniqueListFactory;
    private UniqueList<Object> uniqueList;
    private DefaultEventSelectionModel<Object> selectionModel;
    private DefaultEventSelectionModel<Object> popupSelectionModel;
    private FilterPopupPanel morePopupPanel;
    
    /**
     * Constructs a PropertyFilter using the specified results list,
     * filter type, property key, and icon manager.
     */
    public PropertyFilter(EventList<E> resultsList,
            FilterType filterType, FilePropertyKey propertyKey, 
            Provider<IconManager> iconManager) {
        
        if ((filterType == FilterType.PROPERTY) && (propertyKey == null)) {
            throw new IllegalArgumentException("Property filter cannot use null key");
        }
        
        this.filterType = filterType;
        this.propertyKey = propertyKey;
        this.iconManager = iconManager;
        
        FilterResources resources = getResources();
        
        panel.setLayout(new MigLayout("insets 0 0 0 0, gap 0!, hidemode 3", 
                "[left,grow]", ""));
        panel.setOpaque(false);
        
        propertyLabel.setFont(resources.getHeaderFont());
        propertyLabel.setForeground(resources.getHeaderColor());
        propertyLabel.setText(getPropertyText());
        
        list.setCellRenderer(new PropertyCellRenderer(resources.getBackground(),
                BorderFactory.createEmptyBorder(1, 7, 0, 7)));
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
                if (morePopupPanel != null) {
                    morePopupPanel.setPopupTriggered(true);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (morePopupPanel != null) {
                    morePopupPanel.setPopupTriggered(false);
                }
            }
        });
        
        // Apply results list to filter.
        initialize(resultsList);
        
        // Calculate max list height.
        list.setPrototypeCellValue("Type");
        int listHeight = 3 * list.getFixedCellHeight();
        
        panel.add(propertyLabel, "gap 6 6, wrap");
        panel.add(list         , "hmax " + listHeight + ", grow, wrap");
        panel.add(moreButton   , "gap 6 6");
    }
    
    /**
     * Initializes the filter using the specified list of items.
     */
    private void initialize(EventList<E> resultsList) {
        // Create list of unique property values.
        propertyList = createPropertyList(resultsList);
        nonNullList = createNonNullList(propertyList);
        uniqueListFactory = new UniqueListFactory<Object>(nonNullList, new PropertyComparator(filterType, propertyKey));
        uniqueListFactory.setName(getPropertyText());
        uniqueList = uniqueListFactory.getUniqueList();
        
        // Initialize label and button visibility.
        propertyLabel.setVisible(uniqueList.size() > 0);
        moreButton.setVisible(uniqueList.size() > 3);

        // Add listener to display label and button when needed.
        uniqueList.addListEventListener(new ListEventListener<Object>() {
            @Override
            public void listChanged(ListEvent listChanges) {
                // Update label visibility.
                if (!propertyLabel.isVisible() && (uniqueList.size() > 0)) {
                    propertyLabel.setVisible(true);
                } else if (propertyLabel.isVisible() && (uniqueList.size() < 1)) {
                    propertyLabel.setVisible(false);
                }
                // Update "more" button visibility.
                if (!moreButton.isVisible() && (uniqueList.size() > 3)) {
                    moreButton.setVisible(true);
                } else if (moreButton.isVisible() && (uniqueList.size() < 4)) {
                    moreButton.setVisible(false);
                }
            }
        });
        
        // Create sorted list to display most popular values.
        EventList<Object> sortedList = GlazedListsFactory.sortedList(uniqueList, new PropertyCountComparator());
        
        // Create list and selection models.
        DefaultEventListModel<Object> listModel = new DefaultEventListModel<Object>(sortedList);
        selectionModel = new DefaultEventSelectionModel<Object>(sortedList);
        list.setModel(listModel);
        list.setSelectionModel(selectionModel);
        
        // Add selection listener to update filter.
        selectionModel.addListSelectionListener(new SelectionListener(selectionModel));
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
        // Dispose of property list.
        uniqueListFactory.dispose();
        propertyList.dispose();
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
        getComponent().setVisible(true);
    }
    
    /**
     * Returns a text description of the filter state.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        
        buf.append(getClass().getSimpleName()).append("[");
        buf.append("type=").append(filterType);
        buf.append(", property=").append(propertyKey);
        buf.append(", uniqueItems=").append(uniqueList.size());
        buf.append(", active=").append(isActive());
        EventList<Object> selectedList = selectionModel.getSelected();
        buf.append(", selection=").append((selectedList.size() > 0) ? selectedList.get(0) : "null");
        buf.append("]");
        
        return buf.toString();
    }
    
    /**
     * Returns the text string describing the property for the current filter 
     * type and property key. 
     */
    private String getPropertyText() {
        switch (filterType) {
        case EXTENSION:
            return I18n.tr("Extensions");
            
        case PROPERTY:
            switch (propertyKey) {
            case AUTHOR:
                return I18n.tr("Artists");
            case ALBUM:
                return I18n.tr("Albums");
            case GENRE:
                return I18n.tr("Genres");
            default:
                return propertyKey.toString();
            }
            
        case FILE_TYPE:
            return I18n.tr("Types");
            
        default:
            throw new IllegalStateException("Unknown filter type " + filterType);
        }
    }
    
    /**
     * Returns a list of property values in the specified list of items.
     */
    private EventList<Object> createPropertyList(EventList<E> resultsList) {
        switch (filterType) {
        case EXTENSION:
        case PROPERTY:
        case FILE_TYPE:
            // Create list of property values.
            return GlazedListsFactory.simpleFunctionList(resultsList, 
                    new PropertyFunction(filterType, propertyKey));
            
        default:
            throw new IllegalArgumentException("Invalid filter type " + filterType);
        }
    }

    /**
     * Returns a list of non-null values in the specified list of property 
     * values.
     */
    private FilterList<Object> createNonNullList(EventList<Object> propertyList) {
        // Create list of non-null values.
        return GlazedListsFactory.filterList(propertyList, 
            new Matcher<Object>() {
                @Override
                public boolean matches(Object item) {
                    return (item != null);
                }
            }
        );
    }
    
    /**
     * Creates a popup to display the complete list of property values.
     */
    private FilterPopupPanel createMorePopup() {
        FilterPopupPanel popupPanel = new FilterPopupPanel(getResources(), getPropertyText());
        
        // Set list cell renderer.
        popupPanel.setListCellRenderer(new PropertyCellRenderer(popupPanel.getBackground(),
                BorderFactory.createEmptyBorder(1, 4, 0, 1)));
        
        // Set list and selection models.  We use the unique list directly
        // to display values alphabetically.
        DefaultEventListModel<Object> listModel = new DefaultEventListModel<Object>(uniqueList);
        popupSelectionModel = new DefaultEventSelectionModel<Object>(uniqueList);
        popupPanel.setListModel(listModel);
        popupPanel.setListSelectionModel(popupSelectionModel);
        
        // Add selection listener to update filter.
        popupSelectionModel.addListSelectionListener(new SelectionListener(popupSelectionModel));
        
        return popupPanel;
    }
    
    /**
     * Displays the "more" popup that lists all property values.
     */
    private void showMorePopup() {
        if (morePopupPanel == null) {
            morePopupPanel = createMorePopup();
        }
        morePopupPanel.showPopup(moreButton, list.getWidth() - 12, propertyLabel.getY() - moreButton.getY());
    }
    
    /**
     * Hides the "more" popup that lists all property values.
     */
    private void hideMorePopup() {
        if (morePopupPanel != null) {
            morePopupPanel.hidePopup();
        }
    }
    
    /**
     * Action to display list of all property values in popup window.
     */
    private class MoreAction extends AbstractAction {

        public MoreAction() {
            super(I18n.tr("more"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (morePopupPanel == null) {
                showMorePopup();
            } else if (morePopupPanel.isPopupReady()) {
                showMorePopup();
            } else {
                morePopupPanel.setPopupReady(true);
            }
        }
    }
    
    /**
     * Listener to handle selection changes to update the matcher editor.  
     */
    private class SelectionListener implements ListSelectionListener {
        private final DefaultEventSelectionModel<Object> selectionModel;

        public SelectionListener(DefaultEventSelectionModel<Object> selectionModel) {
            this.selectionModel = selectionModel;
        }
        
        @Override
        public void valueChanged(ListSelectionEvent e) {
            // Skip selection change if filter is active.
            if (isActive()) {
                return;
            }
            
            // Get list of selected values.
            EventList<Object> selectedList = selectionModel.getSelected();
            if (selectedList.size() > 0) {
                // Create new matcher and activate.
                Matcher<E> newMatcher = 
                    new PropertyMatcher<E>(filterType, propertyKey, iconManager, selectedList);
                activate(selectedList.get(0).toString(), newMatcher);
                
            } else {
                // Deactivate to clear matcher.
                deactivate();
            }
            
            // Hide popup if showing.
            hideMorePopup();
            
            // Notify filter listeners.
            fireFilterChanged(PropertyFilter.this);
        }
    }
    
    /**
     * Cell renderer for property values.
     */
    private class PropertyCellRenderer extends DefaultListCellRenderer {
        private final Color background;
        private final Border border;
        
        public PropertyCellRenderer(Color background, Border border) {
            this.background = background;
            this.border = border;
        }
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            Component renderer = super.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);
            
            if ((renderer instanceof JLabel) && (value != null)) {
                // Get count for property value.
                int count = uniqueList.getCount(value);
                
                // Display count in cell.
                StringBuilder buf = new StringBuilder();
                buf.append(value.toString()).append(" (").append(count).append(")");
                ((JLabel) renderer).setText(buf.toString());
                
                // Set appearance.
                ((JLabel) renderer).setBackground(background);
                ((JLabel) renderer).setBorder(border);
            }
            
            return renderer;
        }
    }
    
    /**
     * Comparator to sort property values by their result count.
     */
    private class PropertyCountComparator implements Comparator<Object> {

        @Override
        public int compare(Object o1, Object o2) {
            int count1 = uniqueList.getCount(o1);
            int count2 = uniqueList.getCount(o2);
            // Return inverse value to sort in descending order.
            return (count1 < count2) ? 1 : ((count1 > count2) ? -1 : 0);
        }
    }
    
    /**
     * A Comparator for values in a specific property.  This is used to create
     * a list of unique property values.
     */
    private static class PropertyComparator implements Comparator<Object> {
        private final FilterType filterType;
        private final FilePropertyKey propertyKey;

        public PropertyComparator(FilterType filterType, FilePropertyKey propertyKey) {
            this.filterType = filterType;
            this.propertyKey = propertyKey;
        }

        @Override
        public int compare(Object o1, Object o2) {
            if ((filterType == FilterType.PROPERTY) && FilePropertyKey.isLong(propertyKey)) {
                Long long1 = (Long) o1;
                Long long2 = (Long) o2;
                return Objects.compareToNull(long1, long2, false);
                
            } else {
                String s1 = (String) o1;
                String s2 = (String) o2;
                return Objects.compareToNullIgnoreCase(s1, s2, false);
            }
        }
    }
    
    /**
     * A function to transform a list of filterable items into a list of
     * specific property values.
     */
    private class PropertyFunction implements Function<E, Object> {
        private final FilterType filterType;
        private final FilePropertyKey propertyKey;

        public PropertyFunction(FilterType filterType, FilePropertyKey propertyKey) {
            this.filterType = filterType;
            this.propertyKey = propertyKey;
        }
        
        @Override
        public Object evaluate(E item) {
            switch (filterType) {
            case EXTENSION:
                return item.getFileExtension().toLowerCase();
            case PROPERTY:
                return item.getProperty(propertyKey);
            case FILE_TYPE:
                return iconManager.get().getMIMEDescription(item.getFileExtension());
            default:
                return null;
            }
        }
    }
}
