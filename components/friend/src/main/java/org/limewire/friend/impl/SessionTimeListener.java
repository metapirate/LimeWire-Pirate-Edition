package org.limewire.friend.impl;

import java.util.concurrent.atomic.AtomicLong;

import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;

import com.google.inject.Inject;

@EagerSingleton
public class SessionTimeListener implements EventListener<FriendConnectionEvent> {
    
    private long connectedTime;
    private final AtomicLong previousSessionTimes = new AtomicLong();
    
    @Inject
    void register(ListenerSupport<FriendConnectionEvent> listenerSupport){
        listenerSupport.addListener(this);
    }
    
    @Override
    public void handleEvent(FriendConnectionEvent event) {
        synchronized (previousSessionTimes) {
            switch (event.getType()) {
                case CONNECTED:
                    connectedTime = System.currentTimeMillis();
                    break;
                case DISCONNECTED: 
                    previousSessionTimes.addAndGet(System.currentTimeMillis() - connectedTime);
                    connectedTime = 0;
                    break;
            }
        }
    }
}
