package org.limewire.ui.swing.components;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JToggleButton;

/**
 * A listener that extends toggle capability to a peer component of a check box or radio button.
 *  Useful when using alternate components such as multi-line labels to display text for
 *  those toggle components.
 */
public class ToggleExtenderListener implements MouseListener {

    private final JToggleButton linkComponent;
    
    public ToggleExtenderListener(JToggleButton linkComponent) {
        this.linkComponent = linkComponent;
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            linkComponent.doClick();
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
    }
    @Override
    public void mouseExited(MouseEvent arg0) {
    }
    @Override
    public void mousePressed(MouseEvent arg0) {
    }
    @Override
    public void mouseReleased(MouseEvent arg0) {
    }
}

