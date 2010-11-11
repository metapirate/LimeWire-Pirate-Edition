package org.limewire.ui.swing.components;

import java.awt.Color;

import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;

public class PlainCheckBoxMenuItemUI extends BasicCheckBoxMenuItemUI {

    private static Color originalSelectionBackground;
    private static Color originalSelectionForeground;

    public static void overrideDefaults(Color selectionForeground, Color selectionBackground) {
        originalSelectionForeground = selectionForeground;
        originalSelectionBackground = selectionBackground;
    }
    
    public PlainCheckBoxMenuItemUI() {
    }

    @Override
    public void installDefaults() {
        super.installDefaults();
        selectionForeground = originalSelectionForeground;
        selectionBackground = originalSelectionBackground;
    }
}
