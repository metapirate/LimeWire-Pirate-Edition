package org.limewire.ui.swing.friends.chat;

import org.limewire.friend.impl.feature.NoSave;

import static org.limewire.ui.swing.util.I18n.tr;

/**
 * A status message pertaining to google:nosave updates.
 */
class NoSaveStatusMessage extends AbstractMessageImpl {

    static final String SENDER_NAME = "chat server";

    private final NoSave status;

    public NoSaveStatusMessage(String friendId, Type type, NoSave status) {
        super(SENDER_NAME, friendId, type);
        this.status = status;
    }

    @Override
    public String format() {
        return "<br/><b>" + getForDisplay() + "</b><br/>";
    }

    @Override
    public String toString() {
        return getForDisplay();
    }

    private String getForDisplay() {
        return (status == NoSave.ENABLED) ? tr("Chat is now off the record") :
                tr("Chat is now on the record");
    }

    public NoSave getStatus() {
        return status;
    }
}
