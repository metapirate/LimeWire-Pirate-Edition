package com.limegroup.gnutella.altlocs;

import java.io.IOException;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IP;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortForSelf;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;

@Singleton
public class AlternateLocationFactoryImpl implements AlternateLocationFactory {
    
    private final NetworkManager networkManager;
    private final PushEndpointFactory pushEndpointFactory;
    private final ApplicationServices applicationServices;
    private final ConnectionServices connectionServices;
    private final NetworkInstanceUtils networkInstanceUtils;
    private final IpPortForSelf ipPortForSelf;
    
    @Inject
    public AlternateLocationFactoryImpl(NetworkManager networkManager,
            PushEndpointFactory pushEndpointFactory,
            ApplicationServices applicationServices,
            ConnectionServices connectionServices,
            NetworkInstanceUtils networkInstanceUtils, 
            IpPortForSelf ipPortForSelf) {
        this.networkManager = networkManager;
        this.pushEndpointFactory = pushEndpointFactory;
        this.applicationServices = applicationServices;
        this.connectionServices = connectionServices;
        this.networkInstanceUtils = networkInstanceUtils;
        this.ipPortForSelf = ipPortForSelf;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.altlocs.AlternateLocationFactory#create(com.limegroup.gnutella.URN)
     */
    public AlternateLocation create(URN urn) {
    	if(urn == null) throw new NullPointerException("null sha1");
        
    	try {
    	    
    	    // We try to guess whether we are firewalled or not.  If the node
    	    // has just started up and has not yet received an incoming connection
    	    // our best bet is to see if we have received a connection in the past.
    	    //
    	    // However it is entirely possible that we have received connection in 
    	    // the past but are firewalled this session, so if we are connected
    	    // we see if we received a conn this session only.
    	    
    	    boolean open;
    	    
    	    if (connectionServices.isConnected())
    	        open = networkManager.acceptedIncomingConnection();
    	    else
    	        open = ConnectionSettings.EVER_ACCEPTED_INCOMING.getValue();
    	    
    	    
    		if (open && networkInstanceUtils.isValidExternalIpPort(ipPortForSelf)) {
    		    return new DirectAltLoc(new ConnectableImpl(
    		                NetworkUtils.ip2string(networkManager.getAddress()),
    		                networkManager.getPort(),
    		                networkManager.isIncomingTLSEnabled())
    		            , urn, networkInstanceUtils);
    		} else { 
    			return new PushAltLoc(pushEndpointFactory.createForSelf(), urn, applicationServices);
    		}
    		
    	}catch(IOException bad) {
    		ErrorService.error(bad);
    		return null;
    	}
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.altlocs.AlternateLocationFactory#create(com.limegroup.gnutella.RemoteFileDesc)
     */
    public AlternateLocation create(final RemoteFileDesc rfd) 
    	                                                    throws IOException {
    	if(rfd == null)
    		throw new NullPointerException("cannot accept null RFD");
    
    	URN urn = rfd.getSHA1Urn();
    	if(urn == null)
    	    throw new NullPointerException("cannot accept null URN");
    
    	Address address = rfd.getAddress();
    	if (address instanceof Connectable) {
            return new DirectAltLoc((Connectable)address, urn, networkInstanceUtils);
        } else {
            PushEndpoint copy;
            if (address instanceof PushEndpoint) {
                copy = (PushEndpoint)address;
            } else  {
                throw new IllegalArgumentException(address.getClass() + " should not have become an alternate location: " + rfd.getCreationTime());
                // this is the old code, that would fail silently
                // copy = pushEndpointFactory.createPushEndpoint(rfd.getClientGUID(), IpPort.EMPTY_SET, PushEndpoint.PLAIN, 0, null);
            }
    	    return new PushAltLoc(copy,urn, applicationServices);
    	} 
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.altlocs.AlternateLocationFactory#createPushAltLoc(com.limegroup.gnutella.PushEndpoint, com.limegroup.gnutella.URN)
     */
    public AlternateLocation createPushAltLoc(PushEndpoint pe, URN urn) {
        return new PushAltLoc(pe, urn, applicationServices);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.altlocs.AlternateLocationFactory#createDirectDHTAltLoc(org.limewire.io.IpPort, com.limegroup.gnutella.URN, long, byte[])
     */
    public AlternateLocation createDirectDHTAltLoc(IpPort ipp, URN urn, 
            long fileSize, byte[] ttroot) throws IOException {
        return new DirectDHTAltLoc(ipp, urn, fileSize, ttroot, networkInstanceUtils);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.altlocs.AlternateLocationFactory#createDirectAltLoc(org.limewire.io.IpPort, com.limegroup.gnutella.URN)
     */
    public AlternateLocation createDirectAltLoc(IpPort ipp, URN urn) throws IOException {
        return new DirectAltLoc(ipp, urn, networkInstanceUtils);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.altlocs.AlternateLocationFactory#create(java.lang.String, com.limegroup.gnutella.URN, boolean)
     */
    public AlternateLocation create(String location,
                                           URN urn,
                                           boolean tlsCapable) throws IOException {
        if(location == null || location.equals(""))
            throw new IOException("null or empty location");
        if(urn == null)
            throw new IOException("null URN.");
         
        // Case 1. Direct Alt Loc
        if (location.indexOf(";")==-1) {
        	IpPort addr = createUrlFromMini(location, urn, tlsCapable);
    		return new DirectAltLoc(addr, urn, networkInstanceUtils);
        }
        
        //Case 2. Push Alt loc
        PushEndpoint pe = pushEndpointFactory.createPushEndpoint(location);
        return new PushAltLoc(pe,urn, applicationServices);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.altlocs.AlternateLocationFactory#create(java.lang.String, com.limegroup.gnutella.URN)
     */
    public AlternateLocation create(final String location,
                                           final URN urn) throws IOException {
        return create(location, urn, false);
    }

    /**
     * Creates a new <tt>URL</tt> based on the IP and port in the location
     * The location MUST be a dotted IP address.
     */
    private IpPort createUrlFromMini(final String location, URN urn, boolean tlsCapable)
            throws IOException {
        int port = location.indexOf(':');
        final String loc =
            (port == -1 ? location : location.substring(0, port));
        //Use the IP class as a quick test to make sure it numeric
        try {
            new IP(loc);
        } catch(IllegalArgumentException iae) {
            throw new IOException("invalid location: " + location);
        }
        //But, IP still could have passed if it thought there was a submask
        if( loc.indexOf('/') != -1 )
            throw new IOException("invalid location: " + location);
    
        //Then make sure it's a valid IP addr.
        if(!NetworkUtils.isValidAddress(loc))
            throw new IOException("invalid location: " + location);
        
        if( port == -1 )
            port = 6346; // default port if not included.
        else {
            // Not enough room for a port.
            if(location.length() < port+1)
                throw new IOException("invalid location: " + location);
            try {
                port = Integer.parseInt(location.substring(port+1));
            } catch(NumberFormatException nfe) {
                throw new IOException("invalid location: " + location);
            }
        }
        
        if(!NetworkUtils.isValidPort(port))
            throw new IOException("invalid port: " + port);
        
        return new ConnectableImpl(loc,port, tlsCapable);
    }

}
