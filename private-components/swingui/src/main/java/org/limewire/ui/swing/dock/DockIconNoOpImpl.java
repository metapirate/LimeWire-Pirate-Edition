package org.limewire.ui.swing.dock;

import java.awt.Component;
import java.awt.Graphics;

/**
 * For platforms that are not Mac OS X. I've heard
 * rumors of their existence, but I honestly don't
 * believe that sort of thing.
 *
 */
class DockIconNoOpImpl implements DockIcon {

    public int getIconHeight() { return 0; }
    public int getIconWidth() { return 0; }
    public void paintIcon(Component c, Graphics g, int x, int y) {}
    public void draw (int complete) { }
    
}
