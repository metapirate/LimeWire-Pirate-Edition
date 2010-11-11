package org.limewire.xmpp.client.impl;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;

import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventBroadcaster;
import org.limewire.xmpp.activity.XmppActivityEvent;
import org.limewire.xmpp.activity.XmppActivityEvent.ActivityState;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class IdleStatusMonitor {
    private static final int TWENTY_MINUTES_IN_MILLIS = 1200000;
    private final IdleTime idleTime;
    private final ScheduledExecutorService backgroundExecutor;
    private final EventBroadcaster<XmppActivityEvent> activityBroadcaster;
    private EventBean<FriendConnectionEvent> connectionEvent;
    private Future task;

    @Inject
    public IdleStatusMonitor(@Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor, 
            IdleTime idleTime, EventBroadcaster<XmppActivityEvent> activityBroadcaster,
            EventBean<FriendConnectionEvent> connectionSupport) {
        this.backgroundExecutor = backgroundExecutor;
        this.idleTime = idleTime;
        this.activityBroadcaster = activityBroadcaster;
        this.connectionEvent = connectionSupport;
    }
    
    public void start() {
        task = backgroundExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                FriendConnectionEvent lastEvent = connectionEvent.getLastEvent();
                if (idleTime.supportsIdleTime() && lastEvent != null && lastEvent.getType().equals(FriendConnectionEvent.Type.CONNECTED)) {
                    if (idleTime.getIdleTime() > TWENTY_MINUTES_IN_MILLIS) {
                        activityBroadcaster.broadcast(new XmppActivityEvent(ActivityState.Idle));
                    } else {
                        activityBroadcaster.broadcast(new XmppActivityEvent(ActivityState.Active));
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    
    public void stop() {
        if (task != null) {
            task.cancel(false);
        }
    }
}
