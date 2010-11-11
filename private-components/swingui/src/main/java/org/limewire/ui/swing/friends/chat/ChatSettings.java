package org.limewire.ui.swing.friends.chat;

import org.limewire.core.settings.LimeProps;
import org.limewire.setting.StringSetting;
import org.limewire.ui.swing.util.I18n;

public class ChatSettings extends LimeProps {
    
    private static String titleText = "<H1>" + I18n.tr("Facebook chat is not supported in this version of LimeWire.") + "</H1>";
    private static String paragraph1 = "<p>" + I18n.tr("We at Lime Wire are actively looking into remedies.  In the meantime, you will still be able to browse friends.") + "</p>";
    private static String paragraph2 = "<p>" + I18n.tr("Thank you for your patience.") + "</p>";
    
    private ChatSettings() {
        super();
    }
    
    // LWC-4069
    public static final StringSetting FACEBOOK_CHAT_DISABLED_TEXT =
        FACTORY.createRemoteStringSetting("FACEBOOK_CHAT_DISABLED_TEXT", titleText + paragraph1 + paragraph2);
}
