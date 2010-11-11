package org.limewire.ui.swing.listener;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

public class ActionHandListener extends MouseActionListener {

    public ActionHandListener(ActionListener actionListener) {
        super(actionListener);
    }
    
    public ActionHandListener() {
        this(null);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        JComponent comp = (JComponent) e.getComponent();
        if (!Boolean.TRUE.equals(comp.getClientProperty("limewire.actionHand.disabled"))) {
            Container topLevelContainer = comp.getTopLevelAncestor();
            if(topLevelContainer != null) {
                topLevelContainer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                comp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        JComponent comp = (JComponent) e.getComponent();
        Container topLevelContainer = comp.getTopLevelAncestor();
        if(topLevelContainer != null) {
            topLevelContainer.setCursor(Cursor.getDefaultCursor());
        } else {
            comp.setCursor(Cursor.getDefaultCursor());
        }
    }
    
    /** Resets cursor to default for specified component. */
    public static void resetDefaultCursor(JComponent comp) {
        Container topLevelContainer = comp.getTopLevelAncestor();
        if(topLevelContainer != null) {
            topLevelContainer.setCursor(Cursor.getDefaultCursor());
        } else {
            comp.setCursor(Cursor.getDefaultCursor());
        }
    }   
    
    /** Disables or re-enables the action hand from drawing. */
    public static void setActionHandDrawingDisabled(JComponent component, boolean disabled) {
        component.putClientProperty("limewire.actionHand.disabled", disabled);
    }   
}
