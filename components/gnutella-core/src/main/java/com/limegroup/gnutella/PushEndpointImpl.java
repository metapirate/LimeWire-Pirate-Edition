package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;

import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.service.ErrorService;

public class PushEndpointImpl extends AbstractPushEndpoint {
    /**
	 * the client guid of the endpoint
	 */
	private final byte [] _clientGUID;
	
	/**
	 * the guid as an object to avoid recreating
	 * If there are other PushEnpoint objects, they all will ultimately
	 * point to the same GUID object.  This ensures that as long as
	 * there is at least one PE object for a remote host, the set of
	 * proxies will not be gc-ed.
	 */
	private GUID _guid;
	
	/**
	 * the various features this PE supports.
	 */
	private final int _features;
	
	/**
	 * the version of firewall to firewall transfer protocol
	 * this endpoint supports.  
	 */
	private final int _fwtVersion;
	
	/**
	 * the set of proxies this has immediately after creating the endpoint
	 * cleared after registering in the map.  This is used only to 
	 * hold the parsed proxies until they are put in the map.
	 */
	private Set<? extends IpPort> _proxies;
	
	/**
	 * the external address of this PE.  Needed for firewall-to-firewall
	 * transfers, but can be null.
	 */
	private final IpPort _externalAddr;
    
    private final PushEndpointCache pushEndpointCache;

    private final NetworkInstanceUtils networkInstanceUtils;

    public PushEndpointImpl(byte[] guid, Set<? extends IpPort> proxies, byte features,
            int fwtVersion, IpPort addr, PushEndpointCache pushEndpointCache, NetworkInstanceUtils networkInstanceUtils) {
        this.pushEndpointCache = pushEndpointCache;
        this.networkInstanceUtils = networkInstanceUtils;
        
		_features = ((features & FEATURES_MASK) | (fwtVersion << 3));
		_fwtVersion=fwtVersion;
		_clientGUID=guid;
		_guid = new GUID(_clientGUID);
		if (proxies != null) {
            if (proxies instanceof IpPortSet)
                _proxies = Collections.unmodifiableSet(proxies);
            else
                _proxies = Collections.unmodifiableSet(new IpPortSet(proxies));
        } else
            _proxies = Collections.emptySet();
		_externalAddr = addr;
		
		if(addr != null && addr.getAddress().equals(RemoteFileDesc.BOGUS_IP))
		    ErrorService.error(new IllegalStateException("constructing PEI w/ bogus IP!"));
	}

	/**
	 * 
	 * @return an IpPort representing our valid external
	 * address, or null if we don't have such.
	 */
	public IpPort getValidExternalAddress() {
        IpPort ret = getIpPort();
	    if (ret == null || !networkInstanceUtils.isValidExternalIpPort(ret))
	        return null;
	    
	    // This shouldn't be possible... but we can workaround it.
	    if(ret.getAddress().equals(RemoteFileDesc.BOGUS_IP))
	        return null;
	    
        
	    return ret;
	}
	
	public byte [] getClientGUID() {
		return _clientGUID;
	}
	
	public Set<? extends IpPort> getProxies() {

	    synchronized(this) {
	    	if (_proxies!=null)
	        	return _proxies;
	    }

	    PushEndpoint current = pushEndpointCache.getCached(_guid);
	    if (current == null)
            return Collections.emptySet();        
	    return current.getProxies();
	}
	
	
	public int getFWTVersion() {
		PushEndpoint current = pushEndpointCache.getCached(_guid);
		int currentVersion = current == null ? 
				_fwtVersion : current.getFWTVersion();
		return currentVersion;
	}
	
	public byte getFeatures() {
		PushEndpoint current = pushEndpointCache.getCached(_guid);
		int currentFeatures = current==null ? _features : current.getFeatures();
		return (byte)(currentFeatures & FEATURES_MASK);
	}

	private IpPort getIpPort() {
        PushEndpoint current = pushEndpointCache.getCached(_guid);
        return current == null || current.getValidExternalAddress() == null ?
                _externalAddr :         current.getValidExternalAddress();
    }
    
	/**
     * Implements the IpPort interface, returning a bogus ip if we don't know
     * it.
     * 
     * @return the external address if known otherwise {@link RemoteFileDesc#BOGUS_IP}
     */
    public String getAddress() {
        IpPort addr = getIpPort();
        return addr != null ? addr.getAddress() : RemoteFileDesc.BOGUS_IP;
    }
    
    public InetAddress getInetAddress() {
        IpPort addr = getIpPort();
        return addr != null ? addr.getInetAddress() : null;
    }
    
    /**
     * Implements the IpPort interface, returning a bogus port if we don't know it
     * 
     * @return the port of the external address if known otherwise 6346
     */
    public int getPort() {
        IpPort addr = getIpPort();
        return addr != null ? addr.getPort() : 6346;
    }
    
    public InetSocketAddress getInetSocketAddress() {
        IpPort addr = getIpPort();
        return addr != null ? addr.getInetSocketAddress() : null;
    }
    
    @Override
    public String getAddressDescription() {
        IpPort addr = getIpPort();
        return addr != null ? addr.getAddress() : null;
    }
    
    public boolean isLocal() {
        return false;
    }
	
	public synchronized void updateProxies(boolean good) {
        _guid = pushEndpointCache.updateProxiesFor(_guid, this, good);
        _proxies = null;
    }
    
    public PushEndpoint createClone() {
        return new PushEndpointImpl(_guid.bytes(), getProxies(), getFeatures(), getFWTVersion(), getIpPort(), pushEndpointCache, networkInstanceUtils);
    }
	
}
