package org.limewire.ui.swing.filter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * Base implementation of Filter that provides listener support.
 */
abstract class AbstractFilter<E extends FilterableItem> implements Filter<E> {

    /** List of listeners notified when filter is changed. */
    private final List<FilterListener<E>> listenerList = new ArrayList<FilterListener<E>>();
    
    /** Matcher/editor used to filter items. */
    private final FilterMatcherEditor<E> editor;
    
    /** Resources for filters. */
    private final FilterResources resources = new FilterResources();
    
    /** Indicator that determines whether the filter is active. */
    private boolean active;
    
    /** Description for active filter. */
    private String activeText;
    
    /**
     * Constructs an AbstractFilter.
     */
    public AbstractFilter() {
        // Create matcher editor for filtering.
        editor = new FilterMatcherEditor<E>();
    }
    
    /**
     * Adds the specified listener to the list that is notified when the 
     * filter changes.
     */
    @Override
    public void addFilterListener(FilterListener<E> listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    /**
     * Removes the specified listener from the list that is notified when the 
     * filter changes.
     */
    @Override
    public void removeFilterListener(FilterListener listener) {
        listenerList.remove(listener);
    }
    
    /**
     * Notifies all registered listeners that the filter has changed for the
     * specified filter component.
     */
    protected void fireFilterChanged(Filter<E> filter) {
        for (int i = 0, size = listenerList.size(); i < size; i++) {
            listenerList.get(i).filterChanged(filter);
        }
    }
    
    /**
     * Returns an indicator that determines whether the filter is in use.
     */
    @Override
    public boolean isActive() {
        return active;
    }
    
    /**
     * Returns the display text for an active filter.
     */
    @Override
    public String getActiveText() {
        return activeText;
    }
    
    /**
     * Returns the matcher/editor used to filter items.
     */
    @Override
    public MatcherEditor<E> getMatcherEditor() {
        return editor;
    }

    /**
     * Resets the filter.  The default implementation calls 
     * <code>deactivate()</code>.  Subclasses may override this method to 
     * update the Swing component when the filter is reset.
     */
    @Override
    public void reset() {
        deactivate();
    }
    
    /**
     * Activates the filter using the specified text description and matcher.
     */
    protected void activate(String activeText, Matcher<E> matcher) {
        this.activeText = activeText;
        editor.setMatcher(matcher);
        active = true;
    }

    /**
     * Deactivates the filter by clearing the text description and matcher.
     */
    protected void deactivate() {
        editor.setMatcher(null);
        activeText = null;
        active = false;
    }
    
    /**
     * Returns the resource container for the filter.
     */
    protected FilterResources getResources() {
        return resources;
    }
    
    /**
     * Resource container for filters.
     */
    public static class FilterResources {
        @Resource(key="AdvancedFilter.filterWidth")
        private int filterWidth;
        @Resource(key="AdvancedFilter.background")
        private Color background;
        @Resource(key="AdvancedFilter.headerColor")
        private Color headerColor;
        @Resource(key="AdvancedFilter.headerFont")
        private Font headerFont;
        @Resource(key="AdvancedFilter.highlightBackground")
        private Color highlightBackground;
        @Resource(key="AdvancedFilter.highlightForeground")
        private Color highlightForeground;
        @Resource(key="AdvancedFilter.rowColor")
        private Color rowColor;
        @Resource(key="AdvancedFilter.rowFont")
        private Font rowFont;
        @Resource(key="AdvancedFilter.popupBorderColor")
        private Color popupBorderColor;
        // TODO create icon resource
        private Icon popupCloseIcon = new CloseIcon(Color.WHITE, 6);
        @Resource(key="AdvancedFilter.popupHeaderFont")
        private Font popupHeaderFont;
        @Resource(key="AdvancedFilter.popupHeaderBackground")
        private Color popupHeaderBackground;
        @Resource(key="AdvancedFilter.popupHeaderForeground")
        private Color popupHeaderForeground;
        
        /**
         * Constructs a FilterResources object. 
         */
        FilterResources() {
            GuiUtils.assignResources(this);
        }
        
        /**
         * Returns the filter width.
         */
        public int getFilterWidth() {
            return filterWidth;
        }
        
        /**
         * Returns the background color for the filter.
         */
        public Color getBackground() {
            return background;
        }
        
        /**
         * Returns the text color for the filter header.
         */
        public Color getHeaderColor() {
            return headerColor;
        }
        
        /**
         * Returns the text font for the filter header.
         */
        public Font getHeaderFont() {
            return headerFont;
        }
        
        /**
         * Returns the background color for highlighted filter rows.
         */
        public Color getHighlightBackground() {
            return highlightBackground;
        }
        
        /**
         * Returns the foreground color for highlighted filter rows.
         */
        public Color getHighlightForeground() {
            return highlightForeground;
        }
        
        /**
         * Returns the border color for filter popup.
         */
        public Color getPopupBorderColor() {
            return popupBorderColor;
        }
        
        /**
         * Returns the close icon for filter popup.
         */
        public Icon getPopupCloseIcon() {
            return popupCloseIcon;
        }
        
        /**
         * Returns the header font for filter popup.
         */
        public Font getPopupHeaderFont() {
            return popupHeaderFont;
        }
        
        /**
         * Returns the header background for the filter popup.
         */
        public Color getPopupHeaderBackground() {
            return popupHeaderBackground;
        }
        
        /**
         * Returns the header foreground for the filter popup.
         */
        public Color getPopupHeaderForeground() {
            return popupHeaderForeground;
        }
        
        /**
         * Returns the text color for filter rows.
         */
        public Color getRowColor() {
            return rowColor;
        }
        
        /**
         * Returns the text font for filter rows.
         */
        public Font getRowFont() {
            return rowFont;
        }
    }
    
    /**
     * Button icon to close filter popup.
     */
    private static class CloseIcon implements Icon {
        private static final float SIZE_TO_THICKNESS = 4.0f;

        private final Color color;
        private final int size;
        
        public CloseIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }
        
        @Override
        public int getIconHeight() {
            return this.size;
        }

        @Override
        public int getIconWidth() {
            return this.size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            // Create graphics.
            Graphics2D g2d = (Graphics2D) g.create();
            
            // Set graphics to use anti-aliasing for smoothness.
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Set line color and thickness.
            float thickness = Math.max(this.size / SIZE_TO_THICKNESS, 1.0f);
            g2d.setColor(this.color);
            g2d.setStroke(new BasicStroke(thickness));

            // Create shape.
            Shape backSlash = new Line2D.Double(0, 0, this.size, this.size);
            Shape slash = new Line2D.Double(0, this.size, this.size, 0);
            
            // Draw shape at specified position.
            g2d.translate(x, y);
            g2d.draw(backSlash);
            g2d.draw(slash);

            // Dispose graphics.
            g2d.dispose();
        }
    }
}
