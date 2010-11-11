package org.limewire.ui.swing.tray;

import java.util.EventObject;

/**
 * Interface the outlines the basic functionality of any native desktop
 * notification mechanism, such as the "system tray" on Windows.
 */
public interface TrayNotifier {
    
    /** Returns true if this supports showing in the system tray. */
    public boolean supportsSystemTray();

    /**
     * Shows the tray icon, if possible.
     * Returns true if this was successfully able to add a the tray.
     */
    public boolean showTrayIcon();

    /**
     * Removes the tray icon, if it's visible.
     */
    public void hideTrayIcon();

    /** Shows a message if possible. */
    public void showMessage(Notification notification);

    /** Hides a message. Does nothing if message is not displayed. */
	public void hideMessage(Notification notification);

	/** Returns true if this notifier originated the event. */
    public boolean isExitEvent(EventObject event);
}
