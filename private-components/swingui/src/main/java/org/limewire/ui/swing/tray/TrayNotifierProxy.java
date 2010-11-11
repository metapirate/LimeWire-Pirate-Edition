package org.limewire.ui.swing.tray;

import java.util.EventObject;

import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/** This class acts as a proxy for a platform-specific user notification class. */
@Singleton
class TrayNotifierProxy implements TrayNotifier {

    /** The NotifyUser object that this class is serving as a proxy for. */
    private TrayNotifier notifier;

    /** Flag for whether or not the application is currently in the tray. */
    private boolean inTray;

    @Inject
    TrayNotifierProxy() {
        if(OSUtils.isMacOSX()) {
            notifier = new GrowlNotifier();            
        } else {
            notifier = new SystemTrayNotifier();
            if(!showTrayIcon()) {
                // If it failed, revert.
                notifier = new BasicNotifier();
            }
        }        
    }
    
    public boolean supportsSystemTray() {
        return notifier.supportsSystemTray();
    }

    public boolean showTrayIcon() {
        if (inTray)
            return true;
        boolean notify = notifier.showTrayIcon();
        inTray = notify;
        return notify;
    }

    public void hideTrayIcon() {
        if (!inTray)
            return;
        notifier.hideTrayIcon();
        inTray = false;
    }
    
    public void hideMessage(Notification notification) {
        notifier.hideMessage(notification);
    }

    public void showMessage(Notification notification) {
        if (!SwingUiSettings.SHOW_NOTIFICATIONS.getValue()) {
            return;
        }
        
        notifier.showMessage(notification);
    }
    
    @Override
    public boolean isExitEvent(EventObject event) {
        return notifier.isExitEvent(event);
    }

}
