package com.limegroup.gnutella;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.ConnectionSettings;

import com.google.inject.Singleton;

@Singleton
public class UPnPManagerConfigurationImpl implements UPnPManagerConfiguration {
    public boolean isEnabled() {
        return !ConnectionSettings.DISABLE_UPNP.getValue();
    }

    public void setEnabled(boolean enabled) {
        ConnectionSettings.DISABLE_UPNP.setValue(!enabled);
    }

    public String getClientID() {
        return ApplicationSettings.CLIENT_ID.get().substring(0,10);
    }
}
