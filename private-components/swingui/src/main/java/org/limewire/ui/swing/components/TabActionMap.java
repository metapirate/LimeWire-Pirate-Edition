package org.limewire.ui.swing.components;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * A collection of actions for use with {@link FancyTabList}.
 */
public class TabActionMap {
    
    /** An action command to be used for the select action. */
    public static final String SELECT_COMMAND = "tab.select";
    /** An action command to be used for the remove action. */
    public static final String REMOVE_COMMAND = "tab.remove";
    
    /** A property in the main action that can indicate business. */
    public static final String BUSY_KEY = "busy.indicator";
    
    /** A property in the main action that can indicate 'newness'. */
    public static final String NEW_HINT = "new.indicator";
    
    /**A placeholder indicating that a separator should be put in the menu */
    public static final Action SEPARATOR = new AbstractAction(){
        @Override
        public void actionPerformed(ActionEvent e) {}        
    }; 
    
    private final Action main;
    private final Action remove;
    private final Action moreText;
    private final List<? extends Action> rightClick;
    
    private Action removeOthers;
    private Action removeAll;
        
    public TabActionMap(Action mainAction, Action removeAction,
            Action moreTextAction, List<? extends Action> rightClickActions) {
        this.main = mainAction;
        this.remove = removeAction;
        this.moreText = moreTextAction;
        if (rightClickActions == null) {
            this.rightClick = Collections.emptyList();
        } else {
            this.rightClick = rightClickActions;
        }
    }
    
    public Action getRemoveOthers() {
        return removeOthers;
    }
    
    void setRemoveOthers(Action removeOthers) {
        this.removeOthers = removeOthers;
    }

    public Action getRemoveAll() {
        return removeAll;
    }

    void setRemoveAll(Action removeAll) {
        this.removeAll = removeAll;
    }

    public Action getMainAction() {
        return main;
    }

    public Action getRemoveAction() {
        return remove;
    }
    
    public Action getMoreTextAction() {
        return moreText;
    }
    
    public List<? extends Action> getRightClickActions() {
        return rightClick;
    }
    
    /**
     * Wraps the given {@link Action} for use within a list {@link TabActionMap},
     * suitable for constructing a {@link FancyTabList}.
     */
    public static List<TabActionMap> createMapForMainActions(
            Action... selectActions) {
        return createMapForMainActions(Arrays.asList(selectActions));
    }
    
    /**
     * Wraps the given {@link Action} for use within a list {@link TabActionMap},
     * suitable for constructing a {@link FancyTabList}.
     */
    public static List<TabActionMap> createMapForMainActions(
            Collection<? extends Action> mainActions) {
        
        List<TabActionMap> maps = new ArrayList<TabActionMap>();
        for(Action action : mainActions) {
            maps.add(new TabActionMap(action, null, null, null));
        }
        return maps;
    }
}
