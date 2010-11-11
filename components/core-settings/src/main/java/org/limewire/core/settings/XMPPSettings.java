package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;

/**
 * Settings for XMPP: a list of servers and the label and username of the
 * auto-login account, if there is one (the password can be retrieved from
 * PasswordManager).
 */
public class XMPPSettings extends LimeProps {

    private XMPPSettings() {}

    /**
     * Remote setting whether to set the xmpp status text automatically or not.
     */
    public static final BooleanSetting XMPP_ADVERTISE_LIMEWIRE_STATUS =
        FACTORY.createRemoteBooleanSetting("XMPP_ADVERTISE_LIMEWIRE_STATUS", true);
}
