/**
 * 
 */
package org.limewire.ui.swing.tray;

class WindowDisposedEvent {
    private final NotificationWindow notWindow;
    public WindowDisposedEvent(NotificationWindow notWindow) {
        this.notWindow = notWindow;
    }
    public NotificationWindow getNotWindow() {
        return notWindow;
    }
}
