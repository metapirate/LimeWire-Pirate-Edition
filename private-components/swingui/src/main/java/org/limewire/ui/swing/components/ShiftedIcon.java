package org.limewire.ui.swing.components;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

public class ShiftedIcon implements Icon {
    
    private final int shiftWidth;
    private final int shiftHeight;
    private final Icon icon;

    public ShiftedIcon(int shiftWidth, int shiftHeight, Icon icon) {
        this.shiftWidth = shiftWidth;
        this.shiftHeight = shiftHeight;
        this.icon = icon;
    }

    @Override
    public int getIconHeight() {
        return icon.getIconHeight() + shiftHeight;
    }

    @Override
    public int getIconWidth() {
        return icon.getIconWidth() + shiftWidth;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        icon.paintIcon(c, g, shiftWidth+x, shiftHeight+y);
    }

}
