package org.limewire.ui.swing.table;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * There is a bug in Apple's implementation of JTable, where CTRL clicking on a table to simulate a 
 * right mouse click causes selected items in the table to be deselected.
 * This proxy mouse listener wraps Apple's Aqua mouse listener and it doesn't pass
 * it CTRL click events (or more exactly it doesn't pass it any events which are
 * popup triggers).  This prevents Apple's implementation of JTable from improperly
 * processing these events.
 */
class AquaMouseListenerProxy implements MouseListener
{
    private final static String AQUA_MOUSE_LISTENER = "com.apple.laf.AquaTableUI$MouseInputHandler";

    private final MouseListener mouseListener;
   
    /**
     * This method tests whether the given mouse listener is an Aqua listener.
     * 
     * @param argMouseListener - the listener to be tested
     * @return true if the listener has the class type "com.apple.laf.AquaTableUI$MouseInputHandler".
     */
    public static boolean isAquaMouseListener(MouseListener argMouseListener) {
        return argMouseListener.getClass().getName().equals(AQUA_MOUSE_LISTENER);
    }
    
    public AquaMouseListenerProxy(MouseListener argMouseListener) {
        mouseListener = argMouseListener;
    }
    
    public MouseListener getPeer() {
        return mouseListener;
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger()) {                
            return;
        }
        
        mouseListener.mouseClicked(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mouseListener.mouseEntered(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseListener.mouseExited(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {                
            return;
        }

        mouseListener.mousePressed(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {                
            return;
        }

        mouseListener.mouseReleased(e);
    }       
}
