package com.limegroup.gnutella;

import java.util.Set;

import org.limewire.io.GUID;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.dht.db.PushEndpointService;

public interface PushEndpointCache extends PushEndpointService {

    void clear();
    
    /**
     * Should only be used internally by {@link PushEndpoint} implementations.
     * <p>
     * For retrieving a value from the cache use {@link #getPushEndpoint()}.
     * </p>
     */
    PushEndpoint getCached(GUID guid);    
    
    /**
     * Overwrites the current known push proxies for the host specified
     * by the GUID, using the the proxies as written in the httpString.
     * 
     * @param guid the guid whose proxies to overwrite
     * @param httpString comma-separated list of proxies and possible proxy features
     */
    public void overwriteProxies(byte [] guid, String httpString);

    /**
     * Overwrites any stored proxies for the host specified by the guid.
     * 
     * @param guid the guid whose proxies to overwrite
     * @param newSet the proxies to overwrite with
     */
    /**
     * Sets a new set of proxies overwriting the exiting one. 
     */
    public void overwriteProxies(byte[] guid, Set<? extends IpPort> newSet);

    /**
     * updates the external address of all PushEndpoints for the given guid
     */
    public void setAddr(byte [] guid, IpPort addr);

    /**
     * Sets the fwt version supported for all PEs pointing to the
     * given client guid.
     */
    public void setFWTVersionSupported(byte[] guid, int version);

    /**
     * Adds or removes the given set of ip ports depending on <code>valid</code>.
     * 
     * @param valid if false removes <code>proxies</code> otherwise adds them
     * 
     * @return the guid of the push endpoint a client should hold onto to keep
     * the values in the cache 
     */
    public GUID updateProxiesFor(GUID guid, PushEndpoint pushEndpoint, boolean valid);

    /**
     * Removes the push proxies from the set of push proxies of the push endpoint
     * in the cache.

     * @param bytes the bytes of the guid of the push endpoint
     * @param pushProxy the push proxy to remove
     */
    void removePushProxy(byte[] bytes, IpPort pushProxy);

}