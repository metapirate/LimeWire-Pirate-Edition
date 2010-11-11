package com.limegroup.gnutella.util;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.util.ByteUtils;
import org.limewire.util.StringUtils;

/**
 * Utility class keeps track of masked ip ranges and an associated count.
 */
// TODO rename class to reflect that it is more flexible than class c networks
public class ClassCNetworks {
    
    private final Map<Integer, Integer> counts = new HashMap<Integer,Integer>();
    
    /** Utility comparator to use for sorting class C networks */
    static final Comparator<Map.Entry<Integer,Integer>> CLASS_C_COMPARATOR =
        new Comparator<Map.Entry<Integer, Integer>>() {
        public int compare(Map.Entry<Integer, Integer> a, Map.Entry<Integer, Integer> b) {
            return b.getValue().compareTo(a.getValue());
        }
    };
    
    private final int mask;
    
    public ClassCNetworks() {
        this(24);
    }
    
    public ClassCNetworks(int mask) {
        this.mask = NetworkUtils.getHexMask(mask);
    }
    
    public void addAll(Collection<? extends IpPort> c) {
        for (IpPort ip : c)
            add(ip.getInetAddress(), 1);
    }
    
    public void add(InetAddress addr, int count) {
        add(NetworkUtils.getMaskedIP(addr, mask), count);
    }
    
    public void add(int masked, int count) {
        masked &= mask;
        Integer num = counts.get(masked);
        if (num == null) {
            num = Integer.valueOf(0);
        }
        num = Integer.valueOf(num.intValue() + count);
        counts.put(masked, num);
    }
    
    public List<Map.Entry<Integer, Integer>> getTop() {
        List<Map.Entry<Integer, Integer>> ret = 
            new ArrayList<Map.Entry<Integer,Integer>>(counts.size());
        ret.addAll(counts.entrySet());
        Collections.sort(ret, CLASS_C_COMPARATOR);
        return ret;
    }
    
    /** Returns the top n class C networks in easy to bencode format.*/ 
    public byte [] getTopInspectable(int number) {
        List<Map.Entry<Integer, Integer>> top = getTop();
        number = Math.min(top.size(), number);
        byte [] ret = new byte[8 * number];
        int i = 0;
        for (Map.Entry<Integer, Integer> entry : top) {
            if (i == number)
                break;
            ByteUtils.int2beb(entry.getKey(), ret, i * 8);
            ByteUtils.int2beb(entry.getValue(), ret, i * 8 + 4);
            i++;
        }
        return ret;
    }
    
    /**
     * @return exposes the map.  
     */
    public Map<Integer,Integer> getMap() {
        return counts;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this, mask, counts);
    }
    
    /**
     * Combines the provided class C networks into this one.
     */
    public void addAll(ClassCNetworks... other) {
        for(ClassCNetworks c : other) {
            for (int classC : c.getMap().keySet())
                add(classC,c.getMap().get(classC));
        }
    }
}