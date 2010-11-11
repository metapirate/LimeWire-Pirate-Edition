package org.limewire.xmpp.activity;

import org.limewire.listener.DefaultSourcedEvent;

public class XmppActivityEvent extends DefaultSourcedEvent<XmppActivityEvent.ActivityState>{
    public static enum ActivityState {
        Active, Idle
    }

    public XmppActivityEvent(ActivityState source) {
        super(source);
    }
}
