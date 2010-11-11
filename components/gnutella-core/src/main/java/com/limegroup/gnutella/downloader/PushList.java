package com.limegroup.gnutella.downloader;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.GUID;
import org.limewire.io.NetworkInstanceUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.RemoteFileDesc;

/** Keeps track of who needs a push, and who should be notified of of success or failure. */
public class PushList {
    
    private static final Log LOG = LogFactory.getLog(PushList.class);

    /** Map of clientGUID -> List of potential pushes with that GUID. */
    private final TreeMap<byte[], List<Push>> pushers = new TreeMap<byte[], List<Push>>(GUID.GUID_BYTE_COMPARATOR);
    
    private final NetworkInstanceUtils networkInstanceUtils;
    
    @Inject
    public PushList(NetworkInstanceUtils networkInstanceUtils) {
        this.networkInstanceUtils = networkInstanceUtils;
    }
    
    /** Adds a host that wants to be notified of a push. */
    public void addPushHost(PushDetails details, HTTPConnectObserver observer) {
        if(LOG.isDebugEnabled())
            LOG.debug("Adding observer for details: " + details);
        synchronized(pushers) {
            byte[] clientGUID = details.getClientGUID();
            List<Push> perGUID = pushers.get(clientGUID);
            if(perGUID == null) {
                perGUID = new LinkedList<Push>();
                pushers.put(clientGUID, perGUID);
            }
            perGUID.add(new Push(details, observer));
        }
    }

    /** Returns the exact observer that was added with this PushDetails. */
    public HTTPConnectObserver getExactHostFor(PushDetails details) {
        if(LOG.isDebugEnabled())
            LOG.debug("Retrieving exact match for details: " + details);
        
        synchronized(pushers) {
            byte[] clientGUID = details.getClientGUID();
            List<Push> perGUID = pushers.get(clientGUID);
            if(perGUID == null) {
                LOG.debug("No pushes waiting on those exact details.");
                return null;
            } else {
                Push best = getExactHost(perGUID, details);
                if(perGUID.isEmpty())
                    pushers.remove(clientGUID);
                if(best != null)
                    return best.observer;
                else
                    return null;
            }
        }
    }
    
    /** Returns an HTTPConnectObserver for the given clientGUID & Socket. */
    public HTTPConnectObserver getHostFor(byte[] clientGUID, String address) {
        if(LOG.isDebugEnabled())
            LOG.debug("Retrieving best match for address: " + address + ", guid: " + new GUID(clientGUID));
        
        synchronized(pushers) {
            List<Push> perGUID = pushers.get(clientGUID);
            if(perGUID == null) {
                LOG.debug("No pushes waiting on that GUID.");
                return null;
            } else {
                Push best = getBestHost(perGUID, address);
                if(perGUID.isEmpty())
                    pushers.remove(clientGUID);
                if(best != null)
                    return best.observer;
                else
                    return null;
            }
        }
    }
    
    /** Returns all existing HTTPConnectObservers and clears the list. */
    public List<HTTPConnectObserver> getAllAndClear() {
        List<HTTPConnectObserver> allConnectors = new LinkedList<HTTPConnectObserver>();
        synchronized(pushers) {
            for(List<Push> list : pushers.values()) {
                if(list != null) {
                    for(Push next : list) {
                        allConnectors.add(next.observer);
                    }
                }
            }
            pushers.clear();
        }
        return allConnectors;
    }
    
    /** Returns the first matching Push in the list, or a random one if none match. */
    private Push getBestHost(List<? extends Push> hosts, String address) {
        if(hosts.isEmpty())
            return null;
        
        // First try to find one that exactly matches the IP address.
        for(Iterator<? extends Push> i = hosts.iterator(); i.hasNext(); ) {
            Push next = i.next();
            if(next.details.getAddress().equals(address)) {
                LOG.debug("Found an exact match!");
                i.remove();
                return next;
            }
        }
        
        // Then try and find the first private address.
        LOG.debug("No exact match, using first private|bogus address.");
        for(Iterator<? extends Push> i = hosts.iterator(); i.hasNext();) {
            Push next = i.next();
            if(networkInstanceUtils.isPrivateAddress(next.details.getAddress()) ||
               next.details.getAddress().equals(RemoteFileDesc.BOGUS_IP)) {
                i.remove();
                return next;
            }   
        }
        
        LOG.debug("No private address to use!");
        return null;
    }
    
    /** Returns the exact Push in the list. */
    private Push getExactHost(List<? extends Push> hosts, PushDetails details) {
        if(hosts.isEmpty())
            return null;
        
        // First try to find one that exactly matches the IP address.
        for(Iterator<? extends Push> i = hosts.iterator(); i.hasNext(); ) {
            Push next = i.next();
            if(next.details.equals(details)) {
                i.remove();
                return next;
            }
        }
        
        LOG.debug("No exact match!");
        return null;
    }    
    
    /** A push-type struct. */
    private static class Push {
        private final PushDetails details;
        private final HTTPConnectObserver observer;
        
        Push(PushDetails details, HTTPConnectObserver observer) {
            this.details = details;
            this.observer = observer;
        }
        
    }
}
