package org.limewire.io;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashSet;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Returns the current port and address for the local system. A <a 
 * href="http://en.wikipedia.org/wiki/Singleton_pattern"> Singleton</a> 
 * pattern, <code>IpPortForSelf</code> keeps a static reference which can be
 * reused. 
 * <p>
 * It's ok to put <code>IpPortForSelf</code> in {@link HashSet HashSets}.<br>
 * It's not ok to put <code>IpPortForSelf</code> in {@link IpPortSet IpPortSets}.<br>
 * It's not ok to put <code>DirectAltLoc</code>s using this in 
 * <code>AlternateLocationCollections</code>.<br>
 * It's not ok to use <code>IpPortForSelf</code> in objects whose hashCode or 
 * equals will depend on the values returned by any of the getters.  
 */
@Singleton
public class IpPortForSelf implements IpPort, Connectable {
	
	private final InetAddress localhost;
	
	private final LocalSocketAddressProvider localSocketAddressProvider;
	
	@Inject
	IpPortForSelf(LocalSocketAddressProvider localSocketAddressProvider) {
        this.localSocketAddressProvider = localSocketAddressProvider;

        byte [] b = new byte[] {(byte)127,(byte)0,(byte)0,(byte)1};
        InetAddress addr = null;
        try {
            addr = InetAddress.getByAddress(b);
        } catch (UnknownHostException impossible) {
            throw new RuntimeException(impossible);
        }
        localhost = addr;
    }
	
    public byte[] getAddressAsBytes() {
        return localSocketAddressProvider.getLocalAddress();
    }

	public String getAddress() {
		return getInetAddress().getHostName();
	}

	public InetAddress getInetAddress() {
		try {
			return InetAddress.getByAddress(localSocketAddressProvider.getLocalAddress());
		} catch (UnknownHostException bad) {
			return localhost;
		}
	}

	public int getPort() {
		return localSocketAddressProvider.getLocalPort();
	}
    
    public InetSocketAddress getInetSocketAddress() {
        return new InetSocketAddress(getInetAddress(), getPort());
    }
    
    @Override
    public String getAddressDescription() {
        return getInetSocketAddress().toString();
    }
	
	@Override
    public String toString() {
		return getAddress() +":"+getPort();
	}
    public boolean isTLSCapable() {
        return localSocketAddressProvider.isTLSCapable();
    }
}
