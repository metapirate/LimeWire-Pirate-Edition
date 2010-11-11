package org.limewire.ui.swing.components;

import javax.swing.Action;

import org.jdesktop.swingx.JXButton;

/**
 * Simple class to extend a JXButton with selectability based on
 *  its Action.  Code ported from {@link IconButton}.
 */
public class SelectableJXButton extends JXButton {
    public SelectableJXButton(Action action) {
        super(action);
    }
    
    @Override
    protected void actionPropertyChanged(Action action, String propertyName) {
        super.actionPropertyChanged(action, propertyName);
        
        if (propertyName == Action.SELECTED_KEY && hasSelectedKey(action)) {
            setSelectedFromAction(action);
        }
    }
    
    /** Sets the selected state of the button from the action. */
    private void setSelectedFromAction(Action a) {
        boolean selected = false;
        if (a != null) {
            selected =  Boolean.TRUE.equals(a.getValue(Action.SELECTED_KEY));
        }
        
        if (selected != isSelected()) {
            setSelected(selected);
        }
    }
    
    private static boolean hasSelectedKey(Action a) {
        return (a != null && a.getValue(Action.SELECTED_KEY) != null);
    }
    
}
