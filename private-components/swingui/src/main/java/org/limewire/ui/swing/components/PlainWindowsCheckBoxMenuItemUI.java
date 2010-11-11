package org.limewire.ui.swing.components;

import java.awt.Color;

import com.sun.java.swing.plaf.windows.WindowsCheckBoxMenuItemUI;

@SuppressWarnings("restriction")
public class PlainWindowsCheckBoxMenuItemUI extends WindowsCheckBoxMenuItemUI {
    private static Color originalSelectionBackground;
    private static Color originalSelectionForeground;

    public static void overrideDefaults(Color selectionForeground, Color selectionBackground) {
        originalSelectionForeground = selectionForeground;
        originalSelectionBackground = selectionBackground;
    }
    
    public PlainWindowsCheckBoxMenuItemUI() {
    }

    @Override
    public void installDefaults() {
        super.installDefaults();
        selectionForeground = originalSelectionForeground;
        selectionBackground = originalSelectionBackground;
    }
}
