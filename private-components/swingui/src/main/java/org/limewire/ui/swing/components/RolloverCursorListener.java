package org.limewire.ui.swing.components;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A mouse listener that displays a rollover or hover cursor when the mouse
 * enters the component on which the listener has been installed.
 */
public class RolloverCursorListener extends MouseAdapter {

    private final Cursor rolloverCursor;
    
    /**
     * Constructs a RolloverCursorListener with the default rollover cursor.
     * The default rollover cursor is Cursor.HAND_CURSOR.
     */
    public RolloverCursorListener() {
        this(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    
    /**
     * Constructs a RolloverCursorListener with the specified rollover cursor.
     */
    public RolloverCursorListener(Cursor rolloverCursor) {
        this.rolloverCursor = rolloverCursor;
    }
    
    /**
     * Installs the listener on the specified component.
     */
    public void install(Component comp) {
        comp.addMouseListener(this);
        comp.addMouseMotionListener(this);
    }
    
    /**
     * Uninstalls the listener on the specified component.
     */
    public void uninstall(Component comp) {
        comp.removeMouseListener(this);
        comp.removeMouseMotionListener(this);
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        e.getComponent().setCursor(rolloverCursor);
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        e.getComponent().setCursor(Cursor.getDefaultCursor());
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        e.getComponent().setCursor(rolloverCursor);
    }
}
