package com.limegroup.gnutella.settings;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.net.SocketBindingSettings;

import com.google.inject.Singleton;

/**
 * An implementation of {@link SocketBindingSettings} that is based on
 * LimeWire's settings from {@link ConnectionSettings}.
 */
@Singleton
public class SettingsBackedSocketBindingSettings implements SocketBindingSettings {

    public void bindingFailed() {
        ConnectionSettings.CUSTOM_NETWORK_INTERFACE.setValue(false);
    }

    public String getAddressToBindTo() {
        return ConnectionSettings.CUSTOM_INETADRESS.get();
    }

    public boolean isSocketBindingRequired() {
        return ConnectionSettings.CUSTOM_NETWORK_INTERFACE.getValue();
    }

}
