package org.limewire.core.impl.friend;

import java.util.Collections;
import java.util.Map;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.listener.EventListener;

class BittorrentFriend implements Friend {

    private final String id;
    private final FriendPresence presence;

    public BittorrentFriend(String id, BittorrentPresence bittorrentPresence) {
        this.id = id;
        this.presence = bittorrentPresence;
    }

    @Override
    public void addPresenceListener(EventListener<PresenceEvent> presenceListener) {

    }

    @Override
    public MessageWriter createChat(MessageReader reader) {
        return null;
    }

    @Override
    public FriendPresence getActivePresence() {
        return null;
    }

    @Override
    public String getFirstName() {
        return getId();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return getId();
    }

    @Override
    public Network getNetwork() {
        return null;
    }

    @Override
    public Map<String, FriendPresence> getPresences() {
        return Collections.singletonMap(id, presence);
    }

    @Override
    public String getRenderName() {
        return getName();
    }

    @Override
    public boolean hasActivePresence() {
        return false;
    }

    @Override
    public boolean isAnonymous() {
        return true;
    }

    @Override
    public boolean isSignedIn() {
        return false;
    }

    @Override
    public boolean isSubscribed() {
        return false;
    }

    @Override
    public void removeChatListener() {

    }

    @Override
    public void setChatListenerIfNecessary(IncomingChatListener listener) {

    }

    @Override
    public void setName(String name) {

    }

}
