package org.limewire.ui.swing.components;

import java.awt.Color;

import com.sun.java.swing.plaf.windows.WindowsMenuUI;

@SuppressWarnings("restriction")
public class PlainWindowsMenuUI extends WindowsMenuUI {
    private static Color originalSelectionBackground;
    private static Color originalSelectionForeground;

    public static void overrideDefaults(Color selectionForeground, Color selectionBackground) {
        originalSelectionForeground = selectionForeground;
        originalSelectionBackground = selectionBackground;
    }
    
    public PlainWindowsMenuUI() {
    }

    @Override
    public void installDefaults() {
        super.installDefaults();
        selectionForeground = originalSelectionForeground;
        selectionBackground = originalSelectionBackground;
    }
}
