package org.limewire.ui.swing.components;

import java.util.Arrays;
import java.util.List;

import net.miginfocom.swing.MigLayout;

/** 
 * A horizontal container for {@link FancyTab FancyTab} objects.  FancyTabList 
 * arranges its tabs without regard for the available space - the container is
 * assumed to be able to display the tabs.  To create a tab list that adjusts
 * the number of visible tabs depending on the container size, use
 * {@link FlexibleTabList FlexibleTabList}.
 * 
 * <p>FancyTabList is used to display the category tabs within the search
 * results header.</p>
 */
public class FancyTabList extends AbstractTabList {
    
    /**
     * Constructs a FancyTabList containing a tab for each element in the 
     * specified collection of action maps.
     */
    public FancyTabList(Iterable<? extends TabActionMap> actionMaps) {
        setOpaque(false);
        setLayout(new MigLayout("insets 0, gap 0, filly, hidemode 2"));  
        
        setTabActionMaps(actionMaps);
    }
    
    /**
     * Constructs a FancyTabList containing a tab for each element in the 
     * specified array of action maps.
     */
    public FancyTabList(TabActionMap... actionMaps) {
        this(Arrays.asList(actionMaps));
    }
    
    /**
     * Set the visibility of all the tabs.
     * @param visible true to make visible; false otherwise
     */
    public void setTabsVisible(boolean visible) {
        List<FancyTab> tabs = getTabs();
        for (FancyTab tab : tabs) {
            tab.setVisible(visible);
        }
    }

    /** 
     * Updates the container layout by removing all tabs and adding them back
     * to the container. 
     */
    @Override
    protected void layoutTabs() {
        removeAll();      
        for (FancyTab tab : getTabs()) {
            add(tab, "growy, gaptop 2, gapright 2, gapleft 2, gapbottom 3");
        }        

        revalidate();
        repaint();
    }

    /**
     * Sets an indicator to enable underlined text in the tabs.
     */
    public void setUnderlineEnabled(boolean enabled) {
        List<FancyTab> tabs = getTabs();
        for (FancyTab tab : tabs) {
            tab.setUnderlineEnabled(enabled);
        }
        getTabProperties().setUnderlineEnabled(enabled);
    }
}
