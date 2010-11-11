package org.limewire.friend.impl.address;

import org.limewire.io.PermanentAddress;
import org.limewire.util.Objects;

/**
 * Provides a permanent address for a full jabber id including
 * its resource.
 * 
 * An {@link FriendAddress} is equal to another one if the full
 * id matches up to the first 5 characters in the resource part.
 */
public class FriendAddress implements PermanentAddress {

    private final String id;
    
    /**
     * Id prefix used for equals and hashcode semantics.
     */
    private final String idPrefix;

    public static String parseIdPrefix(String id) {
        int slash = id.indexOf('/');
        if (slash == -1) {
            return id;
        }
        int endPrefix = Math.min(id.length(), slash + 6);
        return id.substring(0, endPrefix);
    }

    /**
     * 
     * @param id the full jabber id including resource
     */
    public FriendAddress(String id) {
        this(Objects.nonNull(id, "id"), parseIdPrefix(id));
    }
    
    FriendAddress(String id, String idPrefix) {
        this.id = id;
        this.idPrefix = idPrefix;
    }
    
    /**
     * Returns the full jabber id including resource. 
     */
    public String getFullId() {
        return id;
    }
    
    @Override
    public String getAddressDescription() {
        return id;
    }
    
    /**
     * Returns the jabber id email address without resource. 
     */
    public String getId() {
        return parseBareAddress(id);
    }

    @Override
    public String toString() {
        return org.limewire.util.StringUtils.toString(this);
    }
    
    /**
     * Returns the XMPP address with any resource information removed. For example,
     * for the address "matt@jivesoftware.com/Smack", "matt@jivesoftware.com" would
     * be returned.
     *
     * @param XMPPAddress the XMPP address.
     * @return the bare XMPP address without resource information.
     */
    public static String parseBareAddress(String XMPPAddress) {
        if (XMPPAddress == null) {
            return null;
        }
        int slashIndex = XMPPAddress.indexOf("/");
        if (slashIndex < 0) {
            return XMPPAddress;
        }
        else if (slashIndex == 0) {
            return "";
        }
        else {
            return XMPPAddress.substring(0, slashIndex);
        }
    }

    @Override
    public int hashCode() {
        return idPrefix.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof FriendAddress)) {
            return false;
        }
        FriendAddress other = (FriendAddress)obj;
        return idPrefix.equals(other.idPrefix);
    }
}
