package org.limewire.ui.swing.tray;

import java.util.EventObject;

import javax.swing.ActionMap;

import org.jdesktop.application.Application;
import org.jdesktop.application.Application.ExitListener;

public class TrayExitListener implements ExitListener {
    
    private final TrayNotifier trayNotifier;
    
    public TrayExitListener(TrayNotifier trayNotifier) {
        this.trayNotifier = trayNotifier;
    }

    @Override
    public boolean canExit(EventObject event) {
        if(!trayNotifier.supportsSystemTray() || trayNotifier.isExitEvent(event)) {
            return true;
        } else {
            ActionMap map = Application.getInstance().getContext().getActionMap();
            map.get("minimizeToTray").actionPerformed(null);
            return false;
        }
    }

    @Override
    public void willExit(EventObject event) {
    }
    

}
