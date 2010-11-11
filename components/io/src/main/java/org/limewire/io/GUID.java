package org.limewire.io;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Random;

import org.limewire.util.ByteUtils;


/**
 * A 16-byte globally unique ID.  Immutable.<p>
 *
 * Let the bytes of a GUID G be labelled G[0]..G[15].  All bytes are unsigned.
 * Let a "short" be a 2 byte little-endian** unsigned number.  Let AB be the
 * short formed by concatenating bytes A and B, with B being the most
 * significant byte.  LimeWire GUID's have the following properties:
 *
 * <ol>
 * <li>G[15]=0x00.  This is reserved for future use.
 * <li>G[9][10]= tag(G[4][5], G[6][7]).  This is LimeWire's "secret" 
 *  proprietary marking. 
 * </ol>
 *
 * Here tag(A, B)=OxFFFF & ((A+2)*(B+3) >> 8).  In other words, the result is
 * obtained by first taking pair of two byte values and adding "secret"
 * constants.  These two byte values are then multiplied together to form a 4
 * byte product.  The middle two bytes of this product are the tag.  <b>Sign IS
 * considered during this process, since Java does that by default.</b><p>
 *
 * As of 9/2004, LimeWire GUIDs used to be marked as such:
 * <li>G[8]==0xFF.  This serves to identify "new GUIDs", e.g. from BearShare.
 * This marking was deprecated.
 *
 * In addition, LimeWire GUIDs may be marked as follows:
 * <ol>
 * <li>G[13][14]=tag(G[0]G[1], G[9][10]).  This was used by LimeWire 2.2.0-2.2.3
 * to mark automatic requeries.  Unfortunately these versions inadvertently sent
 * requeries when cancelling uploads or when sometimes encountering a group of
 * busy hosts. VERSION 0
 * </ol>
 * <li>G[13][14]=tag(G[0][1], G[2][3]).  This marks requeries from versions of
 *  LimeWire that have fixed the requery bug, e.g., 2.2.4 and all 2.3s.  VERSION
 * 1
 * </ol>
 * <li>G[13][14]=tag(G[0][1], G[11][12]).  This marks requeries from versions of
 * LimeWire that have much reduced the amount of requeries that can be sent by
 * an individual client.  a client can only send 32 requeries amongst ALL
 * requeries a day.  VERSION 2
 * </ol>
 *
 * Note that this still leaves 10-12 bytes for randomness.  That's plenty of
 * distinct GUID's.  And there's only a 1 in 65000 chance of mistakenly
 * identifying a LimeWire.
 *
 * Furthermore, LimeWire GUIDs may be 'marked' by containing address info.  In
 * particular:
 * <ol>
 * <li>G[0][3] = 4-octet IP address.  G[13][14] = 2-byte port (little endian).
 * </ol>
 * Note that there is no way to tell from a guid if it has been marked in this
 * manner.  You need to have some indication external to the guid (i.e. for
 * queries the minSpeed field might have a bit set to indicate this).  Also,
 * this reduces the amount of guids per IP to 2^48 - plenty since IP and port
 * combos are themselves unique.
 *  
 */
public class GUID implements Comparable<GUID> {
    /** The size of a GUID. */
    private static final int SZ=16;
    /** Used to generated new GUID's. */
    private static Random rand=new Random();

    /** The contents of the GUID.  INVARIANT: bytes.length==SZ. */
    private byte[] bytes;

    /**
     * Creates a new Globally Unique Identifier (GUID).
     */
    public GUID() {
        this(makeGuid());
    }

    /**
     * Creates a new <tt>GUID</tt> instance with the specified array
     * of unique bytes.
     *
     * @param bytes the array of unique bytes
     */
    public GUID(byte[] bytes) {
        assert(bytes.length==SZ);
        this.bytes=bytes;
    }
    
    /**
     * Creates a GUID from a hex string.
     * 
     * @throws IllegalArgumentException if the string is invalid, see {@link #fromHexString(String)}.
     */
    public GUID(String hexString) {
        this(GUID.fromHexString(hexString));
    }

    /** See the class header description for more details.
     * @param first the first index of the first two bytes involved in the
     * marking.
     * @param second the first index of the second two bytes involved in the
     * marking.
     * @param markPoint the first index of the two bytes where to put the 
     * marking.
     */
    private static void tagGuid(byte[] guid,
                                int first,
                                int second,
                                int markPoint) {
        // You could probably avoid calls to ByteOrder as an optimization.
        short a=ByteUtils.leb2short(guid, first);
        short b=ByteUtils.leb2short(guid, second);
        short tag=tag(a, b);
        ByteUtils.short2leb(tag, guid, markPoint);        
    }

    /** Returns the bytes for a new GUID. */
    public static byte[] makeGuid() {
        //Start with random bytes.  You could avoid filling them all in,
        //but it's not worth it.
        byte[] ret=new byte[16];
        rand.nextBytes(ret);

        //Apply common tags.
        ret[15]=(byte)0x00;   //Version number is 0.

        //Apply LimeWire's marking.
        tagGuid(ret, 4, 6, 9);
        return ret;
    }

    /** 
     * generate a new GUID using md5 of a public Key and the multiInstallationMark
     */
    public static byte[] makeSecureGuid(byte[] publicKeyMd5Value, int multiInstallationMark) {     
        assert(publicKeyMd5Value.length == 16);
        byte[] ret = publicKeyMd5Value.clone();
        // combine with multiInstallationMark
        ByteUtils.int2beb(multiInstallationMark, ret, 12);
        return ret;
    }

    /** 
     * verify if the secureID is generated by the public key 
     */
    public static boolean isSecureGuid(byte[] publicKeyMd5Value, GUID secureId) {
        assert(publicKeyMd5Value.length == 16);
        byte[] secureIdBytes = secureId.bytes();
        // 12, 13, 14, 15th bytes are from multiInstallationMark
        for(int i=0; i<12; i++){ 
            if(publicKeyMd5Value[i]!=secureIdBytes[i]){
                return false;
            }
        }
        return true;
    }

    /** @return the bytes for a new GUID flagged to be a requery made by LW. 
     */
    public static byte[] makeGuidRequery() {
        byte[] ret = makeGuid();

        //Apply LimeWire's marking.
        tagGuid(ret, 0, 11, 13);

        return ret;
    }
    
    /**
     * Encodes the current time in seconds in the guid.  This takes away
     * 3 bytes of entropy because it overwrites byte 16.  
     * If used together with address encoding, we're
     * left with 4 bytes of entropy (~ 4 billion)
     * (4 ip, 2 port, 4 timestamp, 2 lime mark).
     */
    public static void timeStampGuid(byte [] guid) {
        int now = (int)(System.currentTimeMillis() / 1000); // good for another 30 years
        guid[4] = (byte) (now >> 24);
        guid[5] = (byte) (now >> 16);
        guid[6] = (byte) (now >> 8);
        guid[15] = (byte) now;
    }
    
    /**
     * @return the encoded time in the guid
     */
    public static long readTimeStamp(byte [] guid) {
        return ((guid[4] << 24) |
         ((guid[5] & 0xFF) << 16) |
         ((guid[6] & 0xFF) << 8) |
         (guid[15] & 0xFF) ) * 1000l; // round off last second
    }

    /** Create a guid with an IP and port encoded within.
     *  @exception IllegalArgumentException thrown if ip.length != 4 or if the
     *  port is not a valid value.
     */
    public static byte[] makeAddressEncodedGuid(byte[] ip, int port) 
        throws IllegalArgumentException {
        return addressEncodeGuid(makeGuid(), ip, port);
    }

    /** Modifies the input guid by address encoding it with the ip and port.
     *  @exception IllegalArgumentException thrown if ip.length != 4 or if the
     *  port is not a valid value or if the size of the input guid is not 16.
     *  @returns the input guid, now modified as appropriate.  note that since
     *  you have a handle to the original bytes you don't need the return value.
     */
    public static byte[] addressEncodeGuid(byte[] ret, byte[] ip, int port) 
        throws IllegalArgumentException {
        
        if (ret.length != SZ)
            throw new IllegalArgumentException("Input byte array wrong length.");
        if (!NetworkUtils.isValidAddress(ip))
            throw new IllegalArgumentException("IP is invalid!");
        if (!NetworkUtils.isValidPort(port))
            throw new IllegalArgumentException("Port is invalid: " + port);

        // put the IP in there....
        for (int i = 0; i < 4; i++)
            ret[i] = ip[i];

        // put the port in there....
        ByteUtils.short2leb((short) port, ret, 13);
        
        return ret;
    }

    /** Returns LimeWire's secret tag described above. */
    static short tag(short a, short b) {
        int product=(a+2)*(b+3);
        //No need to actually do the AND since the downcast does that.
        short productMiddle=(short)(product >> 8);
        return productMiddle;
    }
    

    /** Same as isLimeGUID(this.bytes) */
    public boolean isLimeGUID() {    
        return isLimeGUID(this.bytes);
    }
    

    /** Same as isLimeRequeryGUID(this.bytes, version) */
    public boolean isLimeRequeryGUID(int version) {
        return isLimeRequeryGUID(this.bytes, version);
    }
    

    /** Same is isLimeRequeryGUID(this.bytes) */
    public boolean isLimeRequeryGUID() {
        return isLimeRequeryGUID(this.bytes);
    }


    /** Same as addressesMatch(this.bytes, ....) */
    public boolean addressesMatch(byte[] ip, int port)
        throws IllegalArgumentException {
        return addressesMatch(this.bytes, ip, port);
    }

    /** Same as getIP(this.bytes) */
    public String getIP() {
        return getIP(this.bytes);
    }

    /** Same as matchesIP(this.bytes) */
    public boolean matchesIP(byte[] bytes) {
        return matchesIP(bytes, this.bytes);
    }

    /** Same as getPort(this.bytes) */
    public int getPort() {
        return getPort(this.bytes);
    }


    
    private static boolean checkMatching(byte[] bytes, 
                                         int first,
                                         int second,
                                         int found) {
        short a = ByteUtils.leb2short(bytes, first);
        short b = ByteUtils.leb2short(bytes, second);
        short foundTag = ByteUtils.leb2short(bytes, found);
        short expectedTag = tag(a, b); 
        return foundTag == expectedTag;
    }


    /** Returns true if this is a specially marked LimeWire GUID.
     *  This does NOT mean that it's a new GUID as well; the caller
     *  will probably want to check that. */
    public static boolean isLimeGUID(byte[] bytes) {    
        return checkMatching(bytes, 4, 6, 9);
    }    

    /** Returns true if this is a specially marked Requery GUID from any version
     *  of LimeWire.  This does NOT mean that it's a new GUID as well; the
     *  caller will probably want to check that.
     */
    public static boolean isLimeRequeryGUID(byte[] bytes) {    
        return isLimeRequeryGUID(bytes, 0) || 
        isLimeRequeryGUID(bytes, 1) || isLimeRequeryGUID(bytes, 2);
    }    

    /** Returns true if this is a specially marked LimeWire Requery GUID.
     *  This does NOT mean that it's a new GUID as well; the caller
     *  will probably want to check that. 
     *
     *  @param version the version of RequeryGUID you want to test for.  0 for
     *  requeries up to 2.2.4, 1 for requeries between 2.2.4 and all 2.3s, and 2
     *  for current requeries....
     */
    public static boolean isLimeRequeryGUID(byte[] bytes, int version) {    
        if (version == 0)
            return checkMatching(bytes, 0, 9, 13);
        else if (version == 1)
            return checkMatching(bytes, 0, 2, 13);
        else
            return checkMatching(bytes, 0, 11, 13);
    }    
    
    /** @return true if the input IP and port match the one encoded in the guid.
     *  @exception IllegalArgumentException thrown if ip.length != 4 or if the
     *  port is not a valid value.     
     */
    public static boolean addressesMatch(byte[] guidBytes, byte[] ip, int port) 
        throws IllegalArgumentException {

        if (ip.length != 4)
            throw new IllegalArgumentException("IP address too big!");
        if (!NetworkUtils.isValidPort(port))
            return false;
        if (!NetworkUtils.isValidAddress(ip))
            return false;

        byte[] portBytes = new byte[2];
        ByteUtils.short2leb((short) port, portBytes, 0);

        return ((guidBytes[0] == ip[0]) &&
                (guidBytes[1] == ip[1]) &&
                (guidBytes[2] == ip[2]) &&
                (guidBytes[3] == ip[3]) &&
                (guidBytes[13] == portBytes[0]) &&
                (guidBytes[14] == portBytes[1]));
    }

    /** Gets bytes 0-4 as a dotted IP address.
     */
    public static String getIP(byte[] guidBytes) {
        return NetworkUtils.ip2string(guidBytes);
    }

    /** Gets bytes 0-4 as a dotted IP address.
     */
    public static boolean matchesIP(byte[] ipBytes, byte[] guidBytes) {
        if (ipBytes.length != 4)
            throw new IllegalArgumentException("Bad byte[] length = " +
                                               ipBytes.length);
        for (int i = 0; i < ipBytes.length; i++)
            if (ipBytes[i] != guidBytes[i]) return false;
        return true;
    }

    /** Gets bytes 13-14 as a port.
     */
    public static int getPort(byte[] guidBytes) {
        return ByteUtils.ushort2int(ByteUtils.leb2short(guidBytes, 13));
    }

    /** 
     * Compares this GUID to o, lexically.
     */
    public int compareTo(GUID o) {
        if (this == o)
            return 0;
        else
            return compare(this.bytes(), o.bytes());
    }
    
    public static final Comparator<GUID> GUID_COMPARATOR = new GUIDComparator();
    public static final Comparator<byte[]> GUID_BYTE_COMPARATOR = new GUIDByteComparator();

    /** Compares GUID's lexically. */
    public static class GUIDComparator implements Comparator<GUID> {
        public int compare(GUID a, GUID b) {
            return GUID.compare(a.bytes, b.bytes);
        }
    }

    /** Compares 16-byte arrays (raw GUIDs) lexically. */
    public static class GUIDByteComparator implements Comparator<byte[]> {
        public int compare(byte[] a, byte[] b) {
            return GUID.compare(a, b);
        }
    }

    /** Compares guid and guid2 lexically, which MUST be 16-byte guids. */
    private static final int compare(byte[] guid, byte[] guid2) {
        for (int i=0; i<SZ; i++) {
            int diff=guid[i]-guid2[i];
            if (diff!=0)
                return diff;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) {
            return true;
        } else if(o instanceof GUID) {
            byte[] bytes2=((GUID)o).bytes();
            return Arrays.equals(bytes, bytes2);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        //Glum bytes 0..3, 4..7, etc. together into 32-bit numbers.
        byte[] ba=bytes;
        final int M1=0x000000FF;
        final int M2=0x0000FF00;
        final int M3=0x00FF0000;

        int a=(M1&ba[0])|(M2&ba[1]<<8)|(M3&ba[2]<<16)|(ba[3]<<24);
        int b=(M1&ba[4])|(M2&ba[5]<<8)|(M3&ba[6]<<16)|(ba[7]<<24);
        int c=(M1&ba[8])|(M2&ba[9]<<8)|(M3&ba[10]<<16)|(ba[11]<<24);
        int d=(M1&ba[12])|(M2&ba[13]<<8)|(M3&ba[14]<<16)|(ba[15]<<24);

        //XOR together to yield new 32-bit number.
        return a^b^c^d;
    }

    /** Warning: this exposes the rep!  Do not modify returned value. */
    public byte[] bytes() {
        return bytes;
    }

    @Override
    public String toString() {
        return toHexString();
    }

    /**
     *  Create a hex version of a GUID for compact display and storage
     *  Note that the client guid should be read in with the
     *  Integer.parseByte(String s, int radix)  call like this in reverse
     */
    public String toHexString() {
        return toHexString(bytes);
    }
    
    /**
     * Returns a hex string of the guid bytes. 
     */
    public static String toHexString(byte[] guid) {
        StringBuilder buf=new StringBuilder();
        String       str;
        int val;
        for (int i=0; i<SZ; i++) {
            //Treating each byte as an unsigned value ensures
            //that we don't str doesn't equal things like 0xFFFF...
            val = ByteUtils.ubyte2int(guid[i]);
            str = Integer.toHexString(val);
            while ( str.length() < 2 )
            str = "0" + str;
            buf.append( str );
        }
        return buf.toString().toUpperCase(Locale.US);
    }
    
    /**
     *  Create a GUID bytes from a hex string version.
     *  
     *  @throws IllegalArgumentException if sguid is
     *  not of the proper format.
     */
    public static byte[] fromHexString(String sguid)
            throws IllegalArgumentException {
        byte bytes[] = new byte[SZ];
        try {
            for (int i=0; i<SZ; i++) {
                bytes[i] =
                    (byte)Integer.parseInt(sguid.substring(i*2,(i*2)+2), 16);
            }
            return bytes;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(sguid);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(sguid);
        }
    }

    /** Simply couples a GUID with a timestamp.  Needed for expiration of
     *  QueryReplies waiting for out-of-band delivery, expiration of proxied
     *  GUIDs, etc.
     */
    public static class TimedGUID {
        private final long MAX_LIFE;
        private final GUID _guid;
        public GUID getGUID() { return _guid; }
        private final long _creationTime;
        
        /**
         * Constructs a TimedGUID that can be used to lookup
         * other TimedGUIDs from HashMaps.  This shouldn't be
         * used as an expiring GUID.
         *  
         */
        public TimedGUID(GUID guid) {
            _guid = guid;
            MAX_LIFE = -1;
            _creationTime = -1;
        }

        /**
         * @param guid the GUID to 'time'.
         * @param maxLife the max lifetime of this GUID.
         */
        public TimedGUID(GUID guid, long maxLife) {
            _guid = guid;
            MAX_LIFE = maxLife;
            _creationTime = System.currentTimeMillis();
        }

        /** @return true if other is a GUID that is the same as the GUID
         *  in this bundle.
         */
        @Override
        public boolean equals(Object other) {
            if (other == this) return true;
            if (other instanceof TimedGUID) 
                return _guid.equals(((TimedGUID) other)._guid);
            return false;
        }

        /** Since guids will be all we have when we do a lookup in a hashtable,
         *  we want the hash code to be the same as the GUID. 
         */
        @Override
        public int hashCode() {
            return _guid.hashCode();
        }

        /** @return true if this bundle is greater than MAX_LIFE seconds old.
         */
        public boolean shouldExpire(long now) {
            return now - _creationTime >= MAX_LIFE;
        }
        
        @Override
        public String toString() {
            return _guid.toString();
        }
        
    }


    //Unit test: in the tests project.
}
