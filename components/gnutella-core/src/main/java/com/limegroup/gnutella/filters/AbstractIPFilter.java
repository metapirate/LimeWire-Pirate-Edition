package com.limegroup.gnutella.filters;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.IP;
import org.limewire.mojito.messages.DHTMessage;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

public abstract class AbstractIPFilter implements IPFilter {

    private static final Log LOG = LogFactory.getLog(AbstractIPFilter.class);
    
    /**
     * Marker object for IPs which are allowed.
     */
    private static final IP ALLOWED = new IP(new byte[4]);
    
    protected IP extractAddress(Message m) {
        if (m instanceof PingReply) {
            PingReply pr = (PingReply)m;
            return getIP(pr.getAddress());
        } else if (m instanceof QueryReply) {
            QueryReply qr = (QueryReply)m;
            return getIP(qr.getIPBytes());
        } else if (m instanceof PushRequest) {
            PushRequest push=(PushRequest)m;
            return getIP(push.getIP());
        } else if (m instanceof QueryRequest) {
            QueryRequest query = (QueryRequest)m;
            if (query.desiresOutOfBandReplies())
                return getIP(query.getGUID());
            else
                return ALLOWED;
        }
        else if (m instanceof DHTMessage){
            DHTMessage message = (DHTMessage)m;
            InetSocketAddress addr = 
                (InetSocketAddress) message.getContact().getContactAddress();
            if (addr != null && addr.getAddress() instanceof Inet4Address)
                return getIP(addr.getAddress().getAddress());
            // dht messages do not require contact address.
            return ALLOWED;
        } else {
            // we dont want to block other kinds of messages
            return ALLOWED;
        }
    }
    
    private IP getIP(byte [] host) {
        IP ip = null;
        try {
            ip = new IP(host, 0);
        } catch(IllegalArgumentException badHost) {
        }
        return ip;
    }
    
    private IP getIP(String host) {
        IP ip = null;
        try {
            ip = new IP(host);
        } catch (IllegalArgumentException badHost) {
            try {
                if (LOG.isDebugEnabled())
                    LOG.debug("doing dns lookup for "+host);
                InetAddress lookUp = InetAddress.getByName(host);
                host = lookUp.getHostAddress();
                ip = new IP(host);
            } catch(UnknownHostException unknownHost) {
                // could not look up this host.
            } catch(IllegalArgumentException stillBadHost) {
                // couldn't construct IP still.
            }
        }        
        return ip;
    }
    
    /** 
     * Checks if a given Message's host is banned.
     * @return true if this Message's host is allowed, false if it is banned
     *  or we are unable to create correct IP addr out of it.
     */
    public boolean allow(Message m) {
        return allow(extractAddress(m));
    }
    

    public boolean allow(SocketAddress addr) {
        if(!(addr instanceof InetSocketAddress)) {
            return false;
        }
        return allow(((InetSocketAddress)addr).getAddress().getAddress());
    }
    
    /**
     * Checks to see if a given host is banned.
     * @param host the host's IP in byte form
     */
    public boolean allow(byte[] host) {
        return allow(getIP(host));
    }
    
    public boolean allow(String host) {
        return allow(getIP(host));
    }
    
    public boolean allow(IP ip) {
        return allowAndLog(ip);
    }
    
    private boolean allowAndLog(IP ip) {
        if (ip == ALLOWED) {
            LOG.debug("Allowing non-checkable IP");
            return true;
        }
        
        if (ip == null) {
            LOG.debug("Not allowing invalid IP");
            return false;
        }
        
        boolean yes = allowImpl(ip);
        
        if (LOG.isDebugEnabled()) {
            if (yes)
                LOG.debug(getClass().getSimpleName() + " allowing " + ip);
            else
                LOG.debug(getClass().getSimpleName() + " not allowing " + ip);
        }
        
        return yes;
    }
    
    protected abstract boolean allowImpl(IP ip);
    
    @Override
    public boolean allow(Address address) {
        if (address instanceof Connectable) {
            return allow(((Connectable)address).getInetAddress().getAddress());
        } 
        return true;
    }
}
