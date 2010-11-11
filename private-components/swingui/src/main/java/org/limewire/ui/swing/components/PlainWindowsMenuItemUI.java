package org.limewire.ui.swing.components;

import java.awt.Color;

import com.sun.java.swing.plaf.windows.WindowsMenuItemUI;

@SuppressWarnings("restriction")
public class PlainWindowsMenuItemUI extends WindowsMenuItemUI {
    private static Color originalSelectionBackground;
    private static Color originalSelectionForeground;

    public static void overrideDefaults(Color selectionForeground, Color selectionBackground) {
        originalSelectionForeground = selectionForeground;
        originalSelectionBackground = selectionBackground;
    }
    
    public PlainWindowsMenuItemUI() {
    }

    @Override
    public void installDefaults() {
        super.installDefaults();
        selectionForeground = originalSelectionForeground;
        selectionBackground = originalSelectionBackground;
    }
}
