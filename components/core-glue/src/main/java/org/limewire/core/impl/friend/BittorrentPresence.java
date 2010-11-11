package org.limewire.core.impl.friend;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureTransport;

import com.limegroup.gnutella.Uploader;

/**
 * Allows the bittorrent uploader to be converted into a FriendPresence that
 * does no support any standard friend/gnutella features.
 */
public class BittorrentPresence implements FriendPresence {

    private final String id;
    private final Friend friend;

    public BittorrentPresence(Uploader uploader) {
        this.id = uploader.getUrn().toString();
        this.friend = new BittorrentFriend(id, this);
    }

    @Override
    public void addFeature(Feature feature) {

    }

    @Override
    public <D, F extends Feature<D>> void addTransport(Class<F> clazz, FeatureTransport<D> transport) {

    }

    @Override
    public Feature getFeature(URI id) {
        return null;
    }

    @Override
    public Collection<Feature> getFeatures() {
        return Collections.emptyList();
    }

    @Override
    public Friend getFriend() {
        return friend;
    }

    @Override
    public Mode getMode() {
        return Mode.available;
    }

    @Override
    public String getPresenceId() {
        return id;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public String getStatus() {
        return "";
    }

    @Override
    public <F extends Feature<D>, D> FeatureTransport<D> getTransport(Class<F> feature) {
        return null;
    }

    @Override
    public Type getType() {
        return Type.available;
    }

    @Override
    public boolean hasFeatures(URI... id) {
        return false;
    }

    @Override
    public void removeFeature(URI id) {
    }

}
