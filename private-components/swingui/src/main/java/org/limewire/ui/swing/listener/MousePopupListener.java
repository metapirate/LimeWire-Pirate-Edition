package org.limewire.ui.swing.listener;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Responds to mouse pressed, released, and clicked events, checking for
 * a popup trigger from the MouseEvent and then calling a handler method to 
 * respond to the popup event. 
 */
public abstract class MousePopupListener extends MouseAdapter {

    @Override
    public void mouseClicked(MouseEvent e) {
        verifyPopupTrigger(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        verifyPopupTrigger(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        verifyPopupTrigger(e);
    }

    private void verifyPopupTrigger(MouseEvent e) {
        if (e.isPopupTrigger()) {
            handlePopupMouseEvent(e);
        }
    }
    
    public abstract void handlePopupMouseEvent(MouseEvent e);
}
