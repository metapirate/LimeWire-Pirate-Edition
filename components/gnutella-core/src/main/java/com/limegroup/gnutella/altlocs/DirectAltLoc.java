
package com.limegroup.gnutella.altlocs;


import java.io.IOException;
import java.util.Set;

import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.util.DataUtils;

/**
 * An alternate location that is directly reachable, i.e. not firewalled.
 */
public class DirectAltLoc extends AbstractAlternateLocation {

    /**
     * A constant prepending a list of direct locations that is used
     * to define which locations following it support TLS.
     */
    public static final String TLS_IDX = "tls=";
    
	/** The node that backs this location. */
	private final IpPort _node;
	
    /**
     * Remembers if this AltLoc ever failed, if it did _demoted is set. If this
     * succeeds, it may be promoted again resetting the value of _demoted.  The
     * _count attribute does does take into account the case of a good alternate
     * location with a high count, which has recently failed. 
     * <p> 
     * Note that demotion in not intrinsic to the use of this class, some
     * modules like the download may not want to demote an AlternatLocation, 
     * other like the uploader may rely on it.
     */
    protected volatile boolean _demoted = false;
    
    private final NetworkInstanceUtils networkInstanceUtils;
    
    protected DirectAltLoc(IpPort address, URN sha1, NetworkInstanceUtils networkInstanceUtils) throws IOException {
        super(sha1);
        this.networkInstanceUtils = networkInstanceUtils;
        if (!networkInstanceUtils.isValidExternalIpPort(address))
		    throw new IOException("not a valid external address:port in direct altloc "+address);
		
		_node=address;
	}
	
	@Override
    protected String generateHTTPString() {
		String ret = _node.getInetAddress().getHostAddress();
		if (_node.getPort()!=6346)
			ret = ret+":"+_node.getPort();
		return ret;
	}
	
	@Override
    public RemoteFileDesc createRemoteFileDesc(long size, RemoteFileDescFactory remoteFileDescFactory) {
		Set<URN> urnSet = new UrnSet(getSHA1Urn());
        int quality = 3;
		RemoteFileDesc ret = remoteFileDescFactory.createRemoteFileDesc(_node instanceof Connectable ? (Connectable)_node : new ConnectableImpl(_node, false), 
		        0, HTTPConstants.URI_RES_N2R + SHA1_URN, size, DataUtils.EMPTY_GUID,
                1000, quality, false, null, urnSet, false, ALT_VENDOR,  -1);
        return ret;
	}
	
	@Override
    public synchronized AlternateLocation createClone() {
        DirectAltLoc ret = null;
        try {
        	ret = new DirectAltLoc(_node, this.SHA1_URN, networkInstanceUtils);

        } catch(IOException ioe) {
            ErrorService.error(ioe);
            return null;
        }
        ret._demoted = this._demoted;
        ret._count = this._count;
        return ret;
    }
	
	@Override
    public boolean isMe(){
	    return networkInstanceUtils.isMe(_node);
	}
	
	/**
	 * Returns the host/port of this alternate location as an endpoint.
	 */
	public IpPort getHost() {
		return _node;
	}

	
	@Override
    public boolean equals(Object o){
		if (o==null || !(o instanceof DirectAltLoc))
			return false;
		
		
		if (!super.equals(o))
			return false;
		
		DirectAltLoc other = (DirectAltLoc)o;
		
		if (_node == other._node)
			return true;
		
		return (_node.getInetAddress().equals(other._node.getInetAddress()) &&
		        _node.getPort() == other._node.getPort());
		
	}
	
	@Override
    synchronized void demote() { _demoted = true;}
	
	@Override
    synchronized void promote() { _demoted = false; }
	
	@Override
    public synchronized boolean isDemoted() { return _demoted; }

	@Override
    public int compareTo(AlternateLocation o) {
	    
        if (this==o) //equal
            return 0;
	    
	    int ret = super.compareTo(o);
	    
	    // if comparing to PushLocs count is all we need.
	    // if that is the same, compare by hashCode()
	    if (!(o instanceof DirectAltLoc)) { 
	        if (ret!=0) 
	            return ret;
	        else
	            return -1;
	    }
		
		DirectAltLoc other = (DirectAltLoc)o;
		
		// if I'm demoted, I'm bigger. Otherwise I'm smaller
        if (_demoted != other._demoted) {
            if (_demoted)
                return 1;
            return -1;
        }

        // ret still holds the count difference
        if (ret != 0)
            return ret;

        // if we are both altlocs for myself
        if (_node == other._node)
        	return 0;
        
        ret = _node.getAddress().compareTo(other._node.getAddress());
        if (ret != 0)
            return ret;
        
        ret = (_node.getPort() - other._node.getPort());
        
        // if we got here and ret is still 0, we are the same as the other 
        // DirectLoc.
        return ret;

	}
	
	@Override
    public int hashCode() {
		if (hashCode ==0) {
			int result = super.hashCode();
			result = (37* result)+_node.getAddress().hashCode();
			result = (37* result)+_node.getPort();
			hashCode=result;
		}
		return hashCode;
	}
	
	/**
	 * Overrides toString to return a string representation of this 
	 * <tt>AlternateLocation</tt>, namely the url and the date.
	 *
	 * @return the string representation of this alternate location
	 */
	@Override
    public String toString() {
		return _node+","+_count+","+_demoted;
	}
}
