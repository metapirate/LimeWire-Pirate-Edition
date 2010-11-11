package org.limewire.xmpp.client.impl;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.friend.impl.AbstractFriend;
import org.limewire.friend.impl.AbstractFriendPresence;
import org.limewire.listener.EventBroadcaster;

class PresenceImpl extends AbstractFriendPresence implements FriendPresence {

    private final AbstractFriend friend;
    private final String jid;
    
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    private Type type;
    private String status;
    private int priority;
    private Mode mode;
    

    PresenceImpl(org.jivesoftware.smack.packet.Presence presence,
                 AbstractFriend friend, EventBroadcaster<FeatureEvent> featureBroadcaster) {
        super(featureBroadcaster);
        this.friend = friend;
        this.jid = presence.getFrom();
        update(presence);
    }

    public void update(org.jivesoftware.smack.packet.Presence presence) {
        rwLock.writeLock().lock();
        try {
            this.type = Type.valueOf(presence.getType().toString());
            this.status = presence.getStatus();
            this.priority = presence.getPriority();
            this.mode = presence.getMode() != null ? Mode.valueOf(presence.getMode().toString()) : Mode.available;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public String getPresenceId() {
        return jid;
    }

    @Override
    public Type getType() {
        rwLock.readLock().lock();
        try {
            return type;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public String getStatus() {
        rwLock.readLock().lock();
        try {
            return status;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public int getPriority() {
        rwLock.readLock().lock();
        try {
            return priority;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Mode getMode() {
        rwLock.readLock().lock();
        try {
            return mode;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public String toString() {
        return getPresenceId() + " for " + friend.toString();
    }

    @Override
    public AbstractFriend getFriend() {
        return friend;
    }
}
