package com.limegroup.gnutella.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.limewire.collection.PatriciaTrie;
import org.limewire.collection.Trie;
import org.limewire.collection.PatriciaTrie.KeyAnalyzer;
import org.limewire.collection.Trie.Cursor;
import org.limewire.io.IP;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.util.ByteUtils;


/**
 * A mutable list of IP addresses.  More specifically, a list of sets of
 * addresses, like "18.239.0.*".  Provides fast operations to find if an address
 * is in the list.  Used to implement IPFilter.  Not synchronized. 
 * <p>
 * This class is optimized by the use of a PATRICIA Trie to store the ranges.
 * Many of the optimizations work because of two key properties that we use
 * when inserting items into the Trie.
 * <pre>
 *   1) If the item to be inserted is within a range already in the Trie,
 *      the item is not inserted.
 *   2) If the item to be inserted contains any items that are within the
 *      Trie, those items are removed.
 * </pre>     
 * Maintaining these properties allows certain necessary optimizations, such as
 * looking at only the closest node when performing a lookup.  If these
 * optimizations were not done, then certain items would appear closer within
 * the Trie, despite there being a range further away that encompassed a given IP.
 * <p>
 * Using a PATRICIA allows an intelligent traversal to be done, so that at most
 * 32 comparisons (the number of bits in an address) are performed regardless 
 * of the number of items inserted into the Trie.  It also allows very efficient
 * means of calculating the minimum distance (using an xor metric), because the
 * Trie orders the IPs by distance.  
 */
public class IPList {
    
    /** A null IP, to use as a comparison when adding. */
    private static final IP NULL_IP = new IP("*.*.*.*");
    
    /** The list of IPs. */
    private Trie<IP, IP> ips = new PatriciaTrie<IP, IP>(new IPKeyAnalyzer());

    public IPList () {}
    
    /**
     * Determines if any hosts exist in this list.
     */
    public synchronized boolean isEmpty() {
        return ips.isEmpty();
    }
    
    /** Gets the number of addresses loaded. */
    public synchronized int size() {
        return ips.size();
    }
    
    /**
     * Parses a string and adds it to the IPList if it represents a valid IP.
     * @return true if the string is valid, otherwise false.
     */
    public boolean add(String ipStr) {
        IP ip;
        try {
            ip = new IP(ipStr);
        } catch (IllegalArgumentException e) {
            return false;
        }
        add(ip);
        return true;
    }
    
    /** 
     * Adds a certain IP to the IPList.
     */
    public synchronized void add(IP ip) {
        // SPECIAL-CASE:
        // If the IP we're adding is the 'null' key (0.0.0.0/0.0.0.0)
        // then we must clear the trie.  The AddFilter trick will not
        // work in this case.
        if(ip.equals(NULL_IP)) {
            ips.clear();
            ips.put(ip, ip);
            return;
        }
        
        if(!NetworkUtils.isValidAddress(ip)) {
            return;
        }
                
        // If we already had it (or an address that contained it),
        // then don't include.  Also remove any IPs we encountered
        // that are contained by this new IP.
        // These two properties are necessary to allow the optimization
        // in Lookup to exit when the distance is greater than 1.
        AddFilter filter = new AddFilter(ip);
        Map.Entry<IP, IP> entry = ips.select(ip, filter);
        if(entry != null) {
            if(!entry.getKey().contains(ip)) {
                for(IP obsolete : filter.getContained()) {
                    ips.remove(obsolete);
                }
                ips.put(ip, ip);
            }
        } else {
            ips.put(ip, ip);
        }
    }

    /**
     * @returns true if ip_address is contained somewhere in the list of IPs
     */
    public synchronized boolean contains(IP lookup) {
        IP ip = ips.select(lookup);        
        return ip != null && ip.contains(lookup);
    }
    
    /**
     * Determines if this filter is valid.  If private IPs are not allowed,
     * NetworkInstanceUtils must be non-null in order to check if an address
     * is considered private.  If allowPrivateIPs is true, networkInstanceUtils
     * can be null.
     */
    public synchronized boolean isValidFilter(boolean allowPrivateIPs, NetworkInstanceUtils networkInstanceUtils) {
        ValidFilter filter = new ValidFilter(allowPrivateIPs, networkInstanceUtils);
        ips.traverse(filter);
        return filter.isValid();
    }
    
    /**
     * Calculates the first set bit in the distance between an IPv4 address and
     * the ranges represented by this list.
     * <p>
     * This is equivalent to floor(log2(distance)) + 1.
     *  
     * @param ip an IPv4 address, represented as an IP object with a /32 netmask.
     * @return an int on the interval [0,31].
     */
    public int logMinDistanceTo(IP ip) {
        int distance = minDistanceTo(ip);
        int logDistance = 0;
        int testMask = -1; // All bits set
        // Guaranteed to terminate since testMask will reach zero
        while ((distance & testMask) != 0) {
            testMask <<= 1;
            ++logDistance;
        }
        return logDistance;
    }
    
    
    /**
     * Calculates the minimum distance between an IPv4 address and this list of IPv4 address ranges.
     * 
     * @param lookup an IPv4 address, represented as an IP object with a /32 netmask.
     * @return 32-bit unsigned distance (using xor metric), represented as Java int
     */
    public synchronized int minDistanceTo(IP lookup) {
        if (lookup.mask != -1) {
            throw new IllegalArgumentException("Expected single IP, not an IP range.");
        }
        
        // The nature of the PATRICIA Trie & the distance (using an xor metric)
        // work well in that the closest item within the trie is also the shortest
        // distance.
        IP ip = ips.select(lookup);
        if(ip == null)
            return Integer.MAX_VALUE;
        else
            return ip.getDistanceTo(lookup);
    }
    
    /**
     *  A trie cursor that determines if the IP list contained in the
     *  trie is valid or not. 
     *  A list is considered invalid if :
     *  <pre>
     *  1) It contains a private IP
     *  2) It contains an invalid IP
     *  3) It spans a range of hosts larger than the MAX_LIST_SPACE constant
     * </pre>
     */
    private static class ValidFilter implements Trie.Cursor<IP, IP> {
        
        /** The space covered by private addresses */
        private static final int INVALID_SPACE = 60882944;
        
        /** The total IP space available */
        private static final long TOTAL_SPACE = (long)Math.pow(2,32) - INVALID_SPACE;
        
        /** The maximum IP space (in percent) for this IPList to be valid */
        private static final float MAX_LIST_SPACE = 0.025f;
        
        private boolean isInvalid;
        private long counter;
        
        private final boolean allowPrivateIPs;
        private final NetworkInstanceUtils networkInstanceUtils;
        
        public boolean isValid() {
            return !isInvalid && ((counter/(float)TOTAL_SPACE) < MAX_LIST_SPACE) ;
        }
        
        public ValidFilter(boolean allowPrivateIPs, NetworkInstanceUtils networkInstanceUtils) {
            this.allowPrivateIPs = allowPrivateIPs;
            this.networkInstanceUtils = networkInstanceUtils;
        }
        
        public SelectStatus select(Entry<? extends IP, ? extends IP> entry) {
            IP key = entry.getKey();
            byte[] buf = new byte[4];
            ByteUtils.int2beb(key.addr,buf,0);
            
            if(!allowPrivateIPs && networkInstanceUtils.isPrivateAddress(buf)) {
                isInvalid = true;
                return SelectStatus.EXIT;
            }
            
            counter += Math.pow(2,countBits(~key.mask));
            return SelectStatus.CONTINUE;
        }
        
        /**
         * Counts number of 1 bits in a 32 bit unsigned number.
         *
         * @param x unsigned 32 bit number whose bits you wish to count.
         *
         * @return number of 1 bits in x.
         * @author Roedy Green
         */
        private int countBits( int x ) {
           // collapsing partial parallel sums method
           // collapse 32x1 bit counts to 16x2 bit counts, mask 01010101
           x = (x >>> 1 & 0x55555555) + (x & 0x55555555);
           // collapse 16x2 bit counts to 8x4 bit counts, mask 00110011
           x = (x >>> 2 & 0x33333333) + (x & 0x33333333);
           // collapse 8x4 bit counts to 4x8 bit counts, mask 00001111
           x = (x >>> 4 & 0x0f0f0f0f) + (x & 0x0f0f0f0f);
           // collapse 4x8 bit counts to 2x16 bit counts
           x = (x >>> 8 & 0x00ff00ff) + (x & 0x00ff00ff);
           // collapse 2x16 bit counts to 1x32 bit count
           return(x >>> 16) + (x & 0x0000ffff);
       }
        
    }
    
    /**
     * A filter for adding IPs -- stores IPs we encountered that
     * are contained by the to-be-added IP, so they can later
     * be removed.
     */
    private static class AddFilter implements Trie.Cursor<IP, IP> {
        private final IP lookup;
        private List<IP> contained;
        
        AddFilter(IP lookup) {
            this.lookup = lookup;
        }
        
        /**
         * Returns all the IPs we encountered while selecting
         * that were contained by the IP being added.
         */
        public List<IP> getContained() {
            if(contained == null)
                return Collections.emptyList();
            else
                return contained;
        }
        
        public Cursor.SelectStatus select(Map.Entry<? extends IP, ? extends IP> entry) {
            IP compare = entry.getKey();
            if (compare.contains(lookup)) {
                return Cursor.SelectStatus.EXIT; // Terminate
            }
            
            if(lookup.contains(compare)) {
                if(contained == null)
                    contained = new ArrayList<IP>();
                contained.add(compare);
                return SelectStatus.CONTINUE;
            } else {
                // Because select traverses in XOR closeness,
                // the first time we encounter an item that's
                // not contained, we know we've exhausted all
                // possible containing values.
                return SelectStatus.EXIT;
            }
        }
    };
    
    private static class IPKeyAnalyzer implements KeyAnalyzer<IP> {

        private static final int[] createIntBitMask(final int bitCount) {
            int[] bits = new int[bitCount];
            for(int i = 0; i < bitCount; i++) {
                bits[i] = 1 << (bitCount - i - 1);
            }
            return bits;
        }
        
        private static final int[] BITS = createIntBitMask(32);
        
        public int length(IP key) {
            return 32;
        }

        public boolean isBitSet(IP key, int keyLength, int bitIndex) {
            int maddr = key.addr & key.mask;
            return (maddr & BITS[bitIndex]) != 0;
        }
        
        public int bitIndex(IP key,   int keyOff, int keyLength,
                            IP found, int foundOff, int foundKeyLength) {
            int maddr1 = key.addr & key.mask;
            int maddr2 = (found != null) ? found.addr & found.mask : 0;
            
            if(keyOff != 0 || foundOff != 0)
                throw new IllegalArgumentException("offsets must be 0 for fixed-size keys");
            
            int length = Math.max(keyLength, foundKeyLength);
            
            boolean allNull = true;
            for (int i = 0; i < length; i++) {
                int a = maddr1 & BITS[i];
                int b = maddr2 & BITS[i];
                
                if (allNull && a != 0) {
                    allNull = false;
                }
                
                if (a != b) {
                    return i;
                }
            }
            
            if (allNull) {
                return KeyAnalyzer.NULL_BIT_KEY;
            }
            
            return KeyAnalyzer.EQUAL_BIT_KEY;
        }

        public int compare(IP o1, IP o2) {
            int addr1 = o1.addr & o1.mask;
            int addr2 = o2.addr & o2.mask;
            if(addr1 > addr2)
                return 1;
            else if(addr1 < addr2)
                return -1;
            else
                return 0;
                
        }

        // This method is generally intended for variable length keys.
        // Fixed-length keys, such as an IP address (32 bits) tend to
        // look at each element as a bit, thus 1 element == 1 bit.
        public int bitsPerElement() {
            return 1;
        }

        public boolean isPrefix(IP prefix, int offset, int length, IP key) {
            int addr1 = prefix.addr & prefix.mask;
            int addr2 = key.addr & key.mask;
            addr1 = addr1 << offset;
            
            int mask = 0;
            for(int i = 0; i < length; i++) {
                mask |= (0x1 << i);
            }
            
            addr1 &= mask;
            addr2 &= mask;
            
            return addr1 == addr2;
        }
    }
}
