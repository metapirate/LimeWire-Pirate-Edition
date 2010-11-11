package com.limegroup.gnutella.settings;

import org.limewire.core.settings.FriendSettings;
import org.limewire.core.settings.XMPPSettings;
import org.limewire.xmpp.api.client.JabberSettings;

import com.google.inject.Singleton;

@Singleton
public class SettingsBackedJabberSettings implements JabberSettings {

    @Override
    public boolean isDoNotDisturbSet() {
        return FriendSettings.DO_NOT_DISTURB.getValue();
    }

    @Override
    public boolean advertiseLimeWireStatus() {
        return XMPPSettings.XMPP_ADVERTISE_LIMEWIRE_STATUS.getValue();
    }

}
