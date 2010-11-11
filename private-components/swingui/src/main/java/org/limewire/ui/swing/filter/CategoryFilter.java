package org.limewire.ui.swing.filter;

import java.awt.Color;
import java.awt.Component;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
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
import org.limewire.core.api.Category;
import org.limewire.ui.swing.components.RolloverCursorListener;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.Objects;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.UniqueList;
import ca.odell.glazedlists.FunctionList.Function;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;

/**
 * Filter component to select search results according to their categories.
 */
class CategoryFilter<E extends FilterableItem> extends AbstractFilter<E> {

    private final JPanel panel = new JPanel();
    private final JLabel categoryLabel = new JLabel();
    private final JXList list = new JXList();
    
    private Category selectedCategory;
    private EventList<Category> categoryList;
    private UniqueListFactory<Category> uniqueListFactory;
    private UniqueList<Category> uniqueList;
    private DefaultEventListModel<Category> listModel;
    private DefaultEventSelectionModel<Category> selectionModel;

    /**
     * Constructs a CategoryFilter using the specified results list.
     */
    public CategoryFilter(EventList<E> resultsList) {
        FilterResources resources = getResources();
        
        panel.setLayout(new MigLayout("insets 0 0 0 0, gap 0!", 
                "[left,grow]", ""));
        panel.setOpaque(false);

        categoryLabel.setFont(resources.getHeaderFont());
        categoryLabel.setForeground(resources.getHeaderColor());
        categoryLabel.setText(I18n.tr("Categories"));
        
        list.setCellRenderer(new CategoryCellRenderer());
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
        
        panel.add(categoryLabel, "gap 6 6, wrap");
        panel.add(list         , "grow");

        // Apply results list to filter.
        initialize(resultsList);
    }

    /**
     * Initializes the filter using the specified list of search results.
     */
    private void initialize(EventList<E> resultsList) {
        // Create list of unique category values.
        categoryList = createCategoryList(resultsList);
        uniqueListFactory = new UniqueListFactory<Category>(categoryList, new CategoryComparator());
        uniqueListFactory.setName(I18n.tr("Categories"));
        uniqueList = uniqueListFactory.getUniqueList();
        
        // Initialize label visibility.
        categoryLabel.setVisible(uniqueList.size() > 0);
        
        // Add listener to display label when needed.
        uniqueList.addListEventListener(new ListEventListener<Object>() {
            @Override
            public void listChanged(ListEvent<Object> listChanges) {
                if (!categoryLabel.isVisible() && (uniqueList.size() > 0)) {
                    categoryLabel.setVisible(true);
                } else if (categoryLabel.isVisible() && (uniqueList.size() < 1)) {
                    categoryLabel.setVisible(false);
                }
            }
        });
        
        // Create list and selection models.
        listModel = new DefaultEventListModel<Category>(uniqueList);
        selectionModel = new DefaultEventSelectionModel<Category>(uniqueList);
        list.setModel(listModel);
        list.setSelectionModel(selectionModel);
        
        // Add selection listener to update filter.
        selectionModel.addListSelectionListener(new SelectionListener());
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void reset() {
        selectionModel.clearSelection();
        selectedCategory = null;
        // Deactivate filter.
        deactivate();
    }
    
    @Override
    public void dispose() {
        // Dispose of category list.
        uniqueListFactory.dispose();
        categoryList.dispose();
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
        buf.append("uniqueItems=").append(uniqueList.size());
        buf.append(", active=").append(isActive());
        EventList<Category> selectedList = selectionModel.getSelected();
        buf.append(", selection=").append((selectedList.size() > 0) ? selectedList.get(0) : "null");
        buf.append("]");
        
        return buf.toString();
    }
    
    /**
     * Returns the number of unique categories.
     */
    public int getCategoryCount() {
        return uniqueList.size();
    }
    
    /**
     * Returns the default category.  The default category is defined as the 
     * one containing the most search results.
     */
    public Category getDefaultCategory() {
        Category defaultCategory = null;
        int defaultCount = 0;
        
        // Find category with most results.
        for (Category category : uniqueList) {
            int count = uniqueList.getCount(category);
            if (count > defaultCount) {
                defaultCount = count;
                defaultCategory = category;
            }
        }
        
        return defaultCategory;
    }
    
    /**
     * Returns the currently selected category.  The method returns null if 
     * no category is selected.
     */
    public Category getSelectedCategory() {
        return selectedCategory;
    }
    
    /**
     * Returns a list of category values in the specified list of search 
     * results.
     */
    private EventList<Category> createCategoryList(EventList<E> resultsList) {
        // Create list of category values.
        return GlazedListsFactory.simpleFunctionList(resultsList, new CategoryFunction<E>());
    }
    
    /**
     * Listener to handle selection changes to update the matcher editor.  
     */
    private class SelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            // Skip selection change if filter is active.
            if (isActive()) {
                return;
            }
            
            // Get list of selected values.
            EventList<Category> selectedList = selectionModel.getSelected();
            if (selectedList.size() > 0) {
                selectedCategory = selectedList.get(0);
                // Create new matcher and activate.
                Matcher<E> newMatcher = new CategoryMatcher<E>(selectedCategory);
                activate(selectedCategory.toString(), newMatcher);
                
            } else {
                selectedCategory = null;
                // Deactivate to clear matcher.
                deactivate();
            }
            
            // Notify filter listeners.
            fireFilterChanged(CategoryFilter.this);
        }
    }
    
    /**
     * Cell renderer for category values.
     */
    private class CategoryCellRenderer extends DefaultListCellRenderer {
        private final Color background = getResources().getBackground();
        private final Border border = BorderFactory.createEmptyBorder(1, 7, 0, 7);
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            Component renderer = super.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);
            
            if (renderer instanceof JLabel) {
                // Get count for category.
                int count = uniqueList.getCount((Category) value);

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
     * A Comparator for category values.
     */
    private static class CategoryComparator implements Comparator<Category> {

        @Override
        public int compare(Category cat1, Category cat2) {
            return Objects.compareToNullIgnoreCase(cat1.toString(), cat2.toString(), false);
        }
    }

    /**
     * A function to transform a list of visual search results into a list of
     * specific category values.
     */
    private static class CategoryFunction<E extends FilterableItem> implements Function<E, Category> {

        @Override
        public Category evaluate(E item) {
            return item.getCategory();
        }
    }
}
