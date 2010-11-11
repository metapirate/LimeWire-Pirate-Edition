package org.limewire.ui.swing.action;

import java.awt.event.MouseEvent;

/**
 * I'm the decider.  I decide whether the popup should display.
 */
public interface PopupDecider {
    boolean shouldDisplay(MouseEvent e);
}
