package com.limegroup.gnutella.dht.db;

import java.io.Serializable;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.io.IpPort;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.routing.Contact;

import com.limegroup.gnutella.PushEndpoint;

/**
 * The representation of a {@link PushEndpoint} in the DHT. This can also 
 * represent non-firewalled clients. In that case the publisher's address
 * will match the single push proxy value in {@link #getPushProxies()}.
 * <p>
 * Does not contain external address information since it can be retrieved
 * from the {@link Contact} that created the value. This should be passed on
 * to the external address info of a {@link PushEndpoint} created from it.
 * <p>
 * Implementations should provide a value based {@link #equals(Object)} method.
 */
public interface PushProxiesValue extends DHTValue, Serializable  {

    /**
     * The Client ID of the Gnutella Node.
     */
    public byte[] getGUID();

    /**
     * The Port number of the Gnutella Node.
     */
    public int getPort();

    /**
     * The supported features of the Gnutella Node.
     */
    public byte getFeatures();

    /**
     * The version of the firewalls-to-firewall 
     * transfer protocol.
     */
    public int getFwtVersion();

    /**
     * A Set of Push Proxies of the Gnutella Node.
     */
    public Set<? extends IpPort> getPushProxies();

    /**
     * @return BitNumbers for TLS status of push proxies,
     * or empty Bit numbers
     */
    public BitNumbers getTLSInfo();
    
}