package org.limewire.ui.swing.tray;

import java.util.EventObject;

import org.limewire.ui.swing.util.I18n;

class GrowlNotifier implements TrayNotifier {
    private static final String NOTIFY_USER = "NotifyUser";
    private GrowlWrapper wrapper;
    
    public GrowlNotifier() {
        wrapper = new GrowlWrapper(I18n.tr("LimeWire"), null, new String[] {NOTIFY_USER}, new String[] {NOTIFY_USER});
    }
    
    public boolean supportsSystemTray() {
        return false;
    }

    public boolean showTrayIcon() {
        return false;
    }

    public void hideTrayIcon() {
        
    }

    public void showMessage(Notification notification) {
        String title = notification.getTitle() != null ? notification.getTitle() : I18n.tr("LimeWire 5"); 
        wrapper.notify(NOTIFY_USER, title, notification.getMessage());
    }
    
    public void hideMessage(Notification notification) {
        
    }

    public void updateUI() {
        
    }
    
    @Override
    public boolean isExitEvent(EventObject event) {
        return false;
    }
}
