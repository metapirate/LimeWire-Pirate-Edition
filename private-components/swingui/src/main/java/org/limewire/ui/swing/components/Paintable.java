package org.limewire.ui.swing.components;

import java.awt.Insets;

/**
 * Defines a painted component that is drawn using custom painters.
 */
public interface Paintable {

    /**
     * Returns the effective insets rendered by a custom painter.  This can be
     * used to determine the actual dimensions as drawn on the screen.
     */
    Insets getPaintedInsets();
    
}
