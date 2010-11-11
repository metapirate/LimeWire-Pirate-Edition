package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.ButtonGroup;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;

/** 
 * A base container for {@link FancyTab FancyTab} objects.  Each tab is
 * represented by a {@link TabActionMap TabActionMap} that defines the actions
 * taken when a tab is selected.  Concrete subclasses should specify their own
 * layout managers and implement {@link #layoutTabs() layoutTabs()} to add 
 * visible tabs to the container.
 * 
 * @see FancyTabList
 * @see FlexibleTabList
 */
public abstract class AbstractTabList extends JXPanel {
    /** Change type indicator used for animating tab layouts. */
    public enum ChangeType {
        NONE, ADDED, REMOVED, SELECTED;
    }
    
    private final List<FancyTab> tabs = new ArrayList<FancyTab>();
    private final ButtonGroup tabGroup = new ButtonGroup();
    
    private final FancyTabProperties props = new FancyTabProperties();    


    /**
     * Constructs an empty AbstractTabList.
     */
    protected AbstractTabList() {
    }
    
    /**
     * Sets a new list of tabs using the specified collection of action maps.
     * Each action map in the collection is assigned to a separate tab.  The
     * method calls <code>layoutTabs()</code> to display the visible tabs.
     */
    public void setTabActionMaps(Iterable<? extends TabActionMap> newActionMaps) {
        // Remove existing tabs.
        for (FancyTab tab : tabs) {
            tab.removeFromGroup(tabGroup);
        }
        tabs.clear();

        // Add new tabs.
        for (TabActionMap actions : newActionMaps) {
            FancyTab tab = createAndPrepareTab(actions);
            tabs.add(tab);
        }
        
        // Layout tabs in container.
        layoutTabs(ChangeType.NONE);
    }
    
    /**
     * Creates a new tab with the specified action map.
     */
    protected FancyTab createAndPrepareTab(TabActionMap actionMap) {
        // Create tab.
        final FancyTab tab = new FancyTab(actionMap, tabGroup, props);
        
        // Add listener to remove tab.
        tab.addRemoveActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeTab(tab);
            }
        });
        
        // Add listener to update layout when tab selected.
        actionMap.getMainAction().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                    if (evt.getNewValue().equals(Boolean.TRUE)) {
                        layoutTabs(ChangeType.SELECTED);
                    }
                }
            }
        });
        
        return tab;
    }

    /**
     * Updates the layout to display the visible tabs. 
     */
    protected abstract void layoutTabs();

    /**
     * Updates the layout to display the visible tabs with the specified
     * change type.  By default, the layout is not animated.  Subclasses
     * may override this method to enable animated transitions.
     */
    protected void layoutTabs(ChangeType changeType) {
        layoutTabs();
    }
    
    /**
     * Adds the specified tab to the list at the specified index.  This method
     * also calls <code>layoutTabs()</code> to update the visible tabs.
     */
    protected void addTab(FancyTab tab, int i) {
        tabs.add(i, tab);
        layoutTabs(ChangeType.ADDED);
    }
    
    /**
     * Removes the specified tab from the container.  This does not trigger 
     * any listeners on the tab's removal.  This method may call 
     * <code>layoutTabs()</code> to update the visible tabs.
     */
    protected void removeTab(FancyTab tab) {
        boolean selected = tab.isSelected();
        int idx = tabs.indexOf(tab);
        if (idx < 0) return;
        tabs.remove(tab);
        tab.removeFromGroup(tabGroup);
        
        // Shift the selection to the tab to the left (or right, if idx==0)
        if (selected && !tabs.isEmpty()) {
            // Selecting a tab will trigger a layout.
            if (idx == 0 && tabs.size() > 0) {
                tabs.get(0).getTabActionMap().getMainAction().putValue(Action.SELECTED_KEY, true);
            } else if (idx > 0 && tabs.size() > 0) {
                tabs.get(idx - 1).getTabActionMap().getMainAction().putValue(Action.SELECTED_KEY, true);
            } // else empty, no need to layout.
        } else {
            layoutTabs(ChangeType.REMOVED);
        }
    }
    
    /**
     * Removes the tab associated with the specified action map.  This will not
     * trigger an action from the {@link TabActionMap#getRemoveAction()} action.
     */
    public void removeTabActionMap(TabActionMap actionMap) {
        for (Iterator<FancyTab> iter = tabs.iterator(); iter.hasNext(); ) {
            FancyTab tab = iter.next();
            if (tab.getTabActionMap().equals(actionMap)) {
                tab.removeFromGroup(tabGroup);
                iter.remove();
                break;
            }
        }
        layoutTabs(ChangeType.REMOVED);
    }
    
    /**
     * Returns the currently selected tab.
     */
    public FancyTab getSelectedTab() {
        for (FancyTab tab : tabs) {
            if (tab.isSelected()) {
                return tab;
            }
        }
        return null;
    }
    
    /**
     * Returns the tab properties that apply to the tab list.
     */
    public FancyTabProperties getTabProperties() {
        return props;
    }

    /**
     * Returns an unmodifiable list of all tabs.
     */
    public List<FancyTab> getTabs() {
        return Collections.unmodifiableList(tabs);
    }

    /**
     * Sets the painter to be used when the tab is rolled over.
     */
    public void setHighlightPainter(Painter<?> highlightPainter) {
        for (FancyTab tab : tabs) {
            if (tab.isHighlighted()) {
                tab.setBackgroundPainter(highlightPainter);
            }
        }
        props.setHighlightPainter(highlightPainter);
    }

    /**
     * Sets the painter to be used when the tab is in the normal state.
     */
    public void setNormalPainter(Painter<?> normalPainter) {
        for (FancyTab tab : tabs) {
            if (!tab.isHighlighted() && !tab.isSelected()) {
                tab.setBackgroundPainter(normalPainter);
            }
        }
        props.setNormalPainter(normalPainter);
    }
    
    /**
     * Sets the painter to be used when the tab is selected. 
     */
    public void setSelectionPainter(Painter<?> selectedPainter) {
        for (FancyTab tab : tabs) {
            if (tab.isSelected()) {
                tab.setBackgroundPainter(selectedPainter);
            }
        }
        props.setSelectedPainter(selectedPainter);
    }

    /**
     * Sets the color used to render the tab's text when it is not selected. 
     */
    public void setTabTextColor(Color normalColor) {
        for (FancyTab tab : tabs) {
            if (!tab.isSelected()) {
                tab.setButtonForeground(normalColor);
            }
        }
        props.setNormalColor(normalColor);
    }
    
    
    /** 
     * Sets the color used to render the tab's text when it is selected. 
     */
    public void setTabTextSelectedColor(Color selectionColor) {
        for (FancyTab tab : tabs) {
            if (tab.isSelected()) {
                tab.setButtonForeground(selectionColor);
            }
        }
        props.setSelectionColor(selectionColor);
    }
    
    /** 
     * Sets the font used to render the tab's text. 
     */
    public void setTextFont(Font font) {
        for (FancyTab tab : tabs) {
            tab.setTextFont(font);
        }
        props.setTextFont(font);
    }
}
