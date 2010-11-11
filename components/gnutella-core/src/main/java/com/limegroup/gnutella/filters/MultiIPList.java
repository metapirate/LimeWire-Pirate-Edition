package com.limegroup.gnutella.filters;

import org.limewire.io.IP;
import org.limewire.io.NetworkInstanceUtils;

/**
 * A view over several IPLists.
 */
public class MultiIPList extends IPList {

    /** The lists that this view represents */
    private final IPList [] lists;
    
    public MultiIPList(IPList... lists) {
        this.lists = lists;
    }
    
    @Override
    public synchronized void add(IP ip) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public synchronized boolean contains(IP lookup) {
        for (IPList ipl : lists) {
            if (ipl.contains(lookup))
                return true;
        }
        return false;
    }
    
    @Override
    public synchronized boolean isEmpty() {
        for (IPList ipl : lists) {
            if (!ipl.isEmpty())
                return false;
        }
        return true;
    }
    
    @Override
    public synchronized boolean isValidFilter(boolean allowPrivateIPs, NetworkInstanceUtils networkInstanceUtils) {
        for (IPList ipl : lists) {
            if (!ipl.isValidFilter(allowPrivateIPs, networkInstanceUtils))
                return false;
        }
        return true;
    }
    
    @Override
    public synchronized int minDistanceTo(IP lookup) {
        int ret = Integer.MAX_VALUE;
        for (IPList ipl : lists) 
            ret = Math.min(ret, ipl.minDistanceTo(lookup));
        return ret;
    }
    
    @Override
    public synchronized int size() {
        int ret = 0;
        for (IPList ipl : lists) 
            ret += ipl.size();
        return ret;
    }
}
