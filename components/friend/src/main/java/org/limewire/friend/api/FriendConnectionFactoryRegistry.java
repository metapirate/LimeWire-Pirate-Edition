package org.limewire.friend.api;


/**
 * Each <code>Network.Type</code> has a different <code>FriendConnectionFactory</code>
 * impl. This registry is the place where those <code>FriendConnectionFactory</code> impls
 * make themselves available.
 */
public interface FriendConnectionFactoryRegistry {

    /**
     * Adds a <code>FriendConnectionFactory</code> for a <code>Network.Type</code>
     * @param type
     * @param factory
     */
    public void register(Network.Type type, FriendConnectionFactory factory);
}
