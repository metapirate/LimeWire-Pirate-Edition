package org.limewire.io;

import org.limewire.util.ByteUtils;

/**
 * Represents an Internet Protocol (IP) address with operations to parse, convert 
 * and compare an IP address. More precisely, <code>IP</code> is a set of IP 
 * addresses specified by a regular expression (e.g., "18.239.*.*") or a submask 
 * (e.g., "18.239.0.0/255.255.0.0"). 
 * <p>
 * Note, <code>IP</code> class is not currently 
 * <a href="http://www.ipv6.org/">IPv6</a> compliant.
 */ 
 /* This class is heavily optimized since IP 
 * objects are constructed for every PingReply, QueryReply, PushRequest, and 
 * internally or externally generated connection.
 */

//the generic usefulness of this class is questionable.
public class IP {
    private static final String MSG = "Could not parse: ";

    public final int addr;
    public final int mask;
    
    /**
     * Creates an IP object out of a provided byte array and offset.
     * @throws IllegalArgumentException if there are less than 4 bytes between
     * offset and end of array.
     */
    public IP(byte [] b, int offset) throws IllegalArgumentException {
        if( b.length < offset + 4 )
            throw new IllegalArgumentException(MSG);
        this.addr = bytesToInt(b, offset);
        this.mask = -1; /* 255.255.255.255 == 0xFFFFFFFF */
    }
    

    public IP(int address, int mask) {
        this.addr = address;
        this.mask = mask;
    }
    
    /**
     * Creates an IP object out of a four byte array of the IP in
     * BIG ENDIAN format (most significant byte first).
     */
    public IP(byte[] ip_bytes) throws IllegalArgumentException {
        this(ip_bytes,0);
        if (ip_bytes.length != 4)
            throw new IllegalArgumentException(MSG);
    }

    /**
     * Creates an IP object out of a String in the format
     * "0.0.0.0", "0.0.0.0/0.0.0.0" or "0.0.0.0/0"
     *
     * @param ip_str a String of the format "0.0.0.0", "0.0.0.0/0.0.0.0",
     *               or "0.0.0.0/0" as an argument.
     */
    public IP(final String ip_str) throws IllegalArgumentException {
        int slash = ip_str.indexOf('/');
        if (slash == -1) { //assume a simple IP "0.0.*.*"
            this.mask = createNetmaskFromWildChars(ip_str);
            this.addr = stringToInt(ip_str);
        }
        // ensure that it isn't a malformed address like
        // 0.0.0.0/0/0
        else if (ip_str.lastIndexOf('/') == slash) {
            // maybe an IP of the form "0.0.0.0/0.0.0.0" or "0.0.0.0/0"
            this.mask = parseNetmask(ip_str.substring(slash + 1));
            this.addr = stringToInt(ip_str.substring(0, slash)) & this.mask;
        } else
            throw new IllegalArgumentException(MSG + ip_str);
    }

    /**
     * Convert String containing an netmask to long variable containing
     * a bitmask
     * @param mask String containing a netmask of the format "0.0.0.0" or
     *          containing an unsigned integer < 32 if this netmask uses the
     *          simplified BSD syntax.
     * @return int containing the subnetmask
     */
    private static int parseNetmask(final String mask)
        throws IllegalArgumentException {
        if (mask.indexOf('.') != -1)
            return stringToInt(mask);
        // assume simple syntax, an integer in [0..32]
        try {
            // if the String contains the number k, we should return a
            // mask of k ones followed by (32 - k) zeroes
            int k = Integer.parseInt(mask);
            if (k >= 0 && k <= 32)
                // note: the >>> operator on 32-bit ints only considers the
                // five lowest bits of the shift count, so 32 shifts would
                // actually perform 0 shift!
                return (k == 32) ? -1 : ~(-1 >>> k);
        } catch (NumberFormatException e) {
        }
        throw new IllegalArgumentException(MSG + mask);
    }
    
    /**
     * Converts a four byte array into a 32-bit int.
     */
    private static int bytesToInt(byte[] ip_bytes, int offset) {
        return ByteUtils.beb2int(ip_bytes, offset);
    }

    /**
     * Convert String containing an ip_address or subnetmask to long
     * containing a bitmask.
     * @param String of the format "0.0.0..." presenting ip_address or
     * subnetmask.  A '*' will be converted to a '0'.
     * @return long containing a bit representation of the ip address
     */
    private static int stringToInt(final String ip_str)
        throws IllegalArgumentException {
        int ip = 0;
        int numOctets = 0;
        int length = ip_str.length();
        // loop over each octet
        for (int i = 0; i < length; i++, numOctets++) {
            int octet = 0;
            // loop over each character making the octet
            for (int j = 0; i < length; i++, j++) {
                char c = ip_str.charAt(i);
                if (c == '.') { // finished octet?
                    // can't be first in octet, and not ending 4th octet.
                    if (j != 0 && numOctets < 3)
                        break; // loop to next octet
                } else if (c == '*') { // convert wildcard.
                    // wildcard be the only character making an octet
                    if (j == 0) // functionality equivalent to setting c to be '0'
                        continue;
                } else if (c >= '0' && c <= '9') {
                    // check we read no more than 3 digits
                    if (j <= 2) {
                        octet = octet * 10 + c - '0';
                        // check it's not a faulty addr.
                        if (octet <= 255)
                            continue;
                   }
                }
                throw new IllegalArgumentException(MSG + ip_str);
            }
            ip = (ip << 8) | octet;
        }
        // if the address had less than 4 octets, push the ip over suitably.
        if (numOctets < 4)
            ip <<= (4 - numOctets) * 8;
        return ip;
    }

    /**
     * Create new subnet mask from IP-address of the format "0.*.*.0".
     * @param ip_str String of the format "W.X.Y.Z", W, X, Y and Z can be
     *               numbers or '*'.
     * @return a 32-bit int with a subnet mask.
     */
    private static int createNetmaskFromWildChars(final String ip_str)
        throws IllegalArgumentException {
        int mask = 0;
        int numOctets = 0;
        int length = ip_str.length();
        // loop over each octet
        for (int i = 0; i < length; i++, numOctets++) {
            int submask = 255;
            // loop over each character in the octet
            // if we encounter a single non '*', mask it off.
            for (int j = 0; i < length; i++, j++) {
                char c = ip_str.charAt(i);
                if (c == '.') {
                    // can't be first in octet, and not ending 4th octet.
                    if (j != 0 && numOctets < 3)
                        break; // loop to next octet
                } else if (c == '*') {
                    // wildcard be the only character making an octet
                    if (j == 0) { // functionality equivalent to setting c to be '0'
                        submask = 0;
                        continue;
                    }
                } else if (c >= '0' && c <= '9') {
                    // can't accept more than three characters.
                    if (j <= 2)
                        continue;
                }
                throw new IllegalArgumentException(MSG + ip_str);
            }
            mask = (mask << 8) | submask;
        }
        // if the address had less than 4 octets, push the mask over suitably.
        if (numOctets < 4)
            mask <<= (4 - numOctets) * 8;
        return mask;
    }
    
    /**
     * Computes the minimum distance between any two IPv4 addresses within two
     * IPv4 address ranges.  Uses xor as the distance metric.
     * 
     * @param ip a 32-bit IPv4 address range, represented as an IP object
     * @return the distance between ipV4Addr and the nearest ip address
     *   represented by the range using the xor metric, a 32-bit unsigned
     *   integer value returned as a 32-bit signed int.
     */
    public int getDistanceTo(IP ip) {
        return (ip.addr ^ this.addr) & ip.mask & this.mask;
    }
    
    /**
     * Returns the stuff as a string.
     */
    @Override
    public String toString() {
        return toString(addr) + "/" + toString(mask); 
    }
    
    private String toString(int i) {
        return ((i >> 24) & 0xFF) + "." +
               ((i >> 16) & 0xFF) + "." + 
               ((i >>  8) & 0xFF) + "." + 
               ( i        & 0xFF);
    }
    
    /**
     * Returns if ip is contained in this.
     * @param ip a singleton IP set, e.g., one representing a single address
     */
    public boolean contains(IP ip) {
        //Example: let this=1.1.1.*=1.1.1.0/255.255.255.0
        //         let ip  =1.1.1.2=1.1.1.2/255.255.255.255
        //      => ip.addr&this.mask=1.1.1.0   (equals this.addr)
        //      => this.addr&this.mask=1.1.1.0 (equals this.mask)
        return (ip.addr & this.mask) == (this.addr /* & this.mask*/) &&
               (ip.mask & this.mask) == this.mask;
    }

    /**
     * Returns true if other is an IP with the same address and mask.  Note that
     * "1.1.1.1/0.0.0.0" DOES equal "2.2.2.2/0.0.0.0", because they
     * denote the same sets of addresses.  But:<ul>
     * <li>"1.1.1.1/255.255.255.255" DOES NOT equal "2.2.2.2/255.255.255.255"
     * (disjoint sets of addresses, intersection and difference is empty).</li>
     * <li>"1.1.1.1/255.255.255.240" DOES NOT equal "1.1.1.1/255.255.255.255"
     * (intersection is not empty, but difference is not empty)</li>
     * </ul>
     * To be equal, the two compared sets must have the same netmask, and their
     * start address (computed from the ip and netmask) must be equal.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof IP) {
            IP ip = (IP)other;
            return this.mask == ip.mask &&
                   (this.addr & this.mask) == (ip.addr & ip.mask);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return addr^mask;
    }
}
