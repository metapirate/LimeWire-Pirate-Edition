package org.limewire.core.impl.friend;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.AddressFeature;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;

/**
 * An implementation of FriendPresence for a Gnutella address.  For example,
 * a GnutellaPresence can be created for a Connection, which is supplied to
 * the RemoteLibraryManager to add and browse the presence.
 * 
 * To construct a Gnutella presence, choose one of the subclasses that best
 * describes the ID of the presence.
 */
public abstract class GnutellaPresence implements FriendPresence {
    
    private final GnutellaFriend friend;
    
    /** Constructs a presence with the given Address. */
    GnutellaPresence(Address address) {
        this.friend = new GnutellaFriend(address, this);
    }
    
    @Override
    public Friend getFriend() {
        return friend;
    }

    @Override
    public abstract String getPresenceId();

    @Override
    public Type getType() {
        return Type.available;
    }

    @Override
    public String getStatus() {
        return "";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public Mode getMode() {
        return Mode.available;
    }
    
    @Override
    public String toString() {
        return "GnutellaPresence for: " + friend;
    }
    
    @Override
    public void addFeature(Feature feature) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public <D, F extends Feature<D>> void addTransport(Class<F> clazz, FeatureTransport<D> transport) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Feature getFeature(URI id) {
        if(id.equals(AddressFeature.ID)) {
            return new AddressFeature(friend.getAddress());
        } else {
            return null;
        }
    }
    
    @Override
    public Collection<Feature> getFeatures() {
        return Collections.<Feature>singleton(getFeature(AddressFeature.ID));
    }
    
    @Override
    public <F extends Feature<D>, D> FeatureTransport<D> getTransport(Class<F> feature) {
        return null;
    }
    
    @Override
    public boolean hasFeatures(URI... ids) {
        for(URI uri : ids) {
            return uri.equals(AddressFeature.ID);
        }
        return false;
    }
    
    @Override
    public void removeFeature(URI id) {
        throw new UnsupportedOperationException();
    }
    
    /// NOTE: These subclasses exist for memory optimizations,
    // so that results from Gnutella do not have to allocate extra memory
    // to calculate an ID for the presence.
    
    /** A Gnutella presence whose id is based off a String. */
    public static class GnutellaPresenceWithString extends GnutellaPresence {
        private final String id;
        
        /** Constructs a presence with the given Address & id string. */
        public GnutellaPresenceWithString(Address address, String id) {
            super(address);
            this.id = id;
        }
        
        @Override
        public String getPresenceId() {
            return id;
        }
    }
    
    /** A Gnutella presence whose id is based off the GUID. */
    public static class GnutellaPresenceWithGuid extends GnutellaPresence {
        private final byte[] id;
        
        /** Constructs a presence with the given Address & byte[] as an id. */
        public GnutellaPresenceWithGuid(Address address, byte[] id) {
            super(address);
            this.id = id;
        }
        
        @Override
        public String getPresenceId() {
            return GUID.toHexString(id);
        }
    }
    
    /** A gnutella presence whose id is based off the connectable's InetSocketAddress. */
    public static class GnutellaPresenceWithConnectable extends GnutellaPresence {
        private final Connectable connectable;
        
        /** Constructs a presence with the given Connectable as the id & address. */
        public GnutellaPresenceWithConnectable(Connectable address) {
            super(address);
            this.connectable = address;
        }
        
        @Override
        public String getPresenceId() {
            return connectable.getInetSocketAddress().toString();
        }
    }
}
