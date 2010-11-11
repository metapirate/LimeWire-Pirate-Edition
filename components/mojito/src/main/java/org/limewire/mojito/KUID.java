/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package org.limewire.mojito;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.Map.Entry;

import org.limewire.collection.PatriciaTrie.KeyAnalyzer;
import org.limewire.mojito.util.ArrayUtils;
import org.limewire.security.SecurityUtils;



/**
 * KUID stands for Kademlia Unique Identifier and represents 
 * a 160-bit integer.
 * <p>
 * This class is immutable!
 */
public class KUID implements Comparable<KUID>, Serializable {
    
    private static final long serialVersionUID = 633717248208386374L;
    
    private static final Random GENERATOR = SecurityUtils.createSecureRandomNoBlock();
    
    public static final int LENGTH = 20;
    
    public static final int LENGTH_IN_BITS = LENGTH * 8; // 160-bit
    
    /** Bits from Most Significant Bits (MSB) to Least Significant Bits (LSB) */
    private static final int[] BITS = {
        0x80,
        0x40,
        0x20,
        0x10,
        0x8,
        0x4,
        0x2,
        0x1
    };
    
    /** All 160 bits are 0. */
    public static final KUID MINIMUM;
    
    /** All 160 bits are 1. */
    public static final KUID MAXIMUM;
                                           
    static {
        byte[] min = new byte[LENGTH];
        
        byte[] max = new byte[LENGTH];
        Arrays.fill(max, (byte)0xFF);
        
        MINIMUM = new KUID(min);
        MAXIMUM = new KUID(max);
    }
    
    /** The id. */
    private final byte[] id;
    
    /** The hashCode of this Object. */
    private final int hashCode;
    
    protected KUID(byte[] id) {
        if (id == null) {
            throw new NullPointerException("ID is null");
        }
        
        if (id.length != LENGTH) {
            throw new IllegalArgumentException("ID must be " + LENGTH + " bytes long");
        }
        
        this.id = id;
        this.hashCode = Arrays.hashCode(id);
    }
    
    /**
     * Writes the ID to the OutputStream.
     */
    public void write(OutputStream out) throws IOException {
        out.write(id, 0, id.length);
    }
    
    /**
     * Returns whether or not the 'bitIndex' th bit is set.
     */
    public boolean isBitSet(int bitIndex) {
        // Take advantage of rounding errors!
        int index = (bitIndex / BITS.length);
        int bit = (bitIndex - index * BITS.length);
        return (id[index] & BITS[bit]) != 0;
    }
    
    /**
     * Sets the specified bit to 1 and returns a new
     * KUID instance.
     */
    public KUID set(int bit) {
        return set(bit, true);
    }
    
    /**
     * Sets the specified bit to 0 and returns a new
     * KUID instance.
     */
    public KUID unset(int bit) {
        return set(bit, false);
    }
    
    /**
     * Flips the specified bit from 0 to 1 or vice versa
     * and returns a new KUID instance.
     */
    public KUID flip(int bit) {
        return set(bit, !isBitSet(bit));
    }
    
    /**
     * Sets or unsets the 'bitIndex' th bit.
     */
    private KUID set(int bitIndex, boolean set) {
        // Take advantage of rounding errors!
        int index = (bitIndex / BITS.length);
        int bit = (bitIndex - index * BITS.length);
        boolean isBitSet = (id[index] & BITS[bit]) != 0;
        
        // Don't create a new Object if nothing is
        // gonna change
        if (isBitSet != set) {
            byte[] id = getBytes();
            id[index] ^= BITS[bit];
            return new KUID(id);
        } else {
            return this;
        }
    }
    
    /**
     * Returns the number of bits that are 1.
     */
    public int bits() {
        int bits = 0;
        for(int i = 0; i < LENGTH_IN_BITS; i++) {
            if (isBitSet(i)) {
                bits++;
            }
        }
        return bits;
    }
    
    /**
     * Returns the first bit that differs in this KUID
     * and the given KUID or KeyAnalyzer.NULL_BIT_KEY
     * if all 160 bits are zero or KeyAnalyzer.EQUAL_BIT_KEY
     * if both KUIDs are equal.
     */
    public int bitIndex(KUID nodeId) {
        boolean allNull = true;
        
        int bitIndex = 0;
        for(int i = 0; i < id.length; i++) {
            if (allNull && id[i] != 0) {
                allNull = false;
            }
            
            if (id[i] != nodeId.id[i]) {
                for(int j = 0; j < BITS.length; j++) {
                    if ((id[i] & BITS[j]) 
                            != (nodeId.id[i] & BITS[j])) {
                        break;
                    }
                    bitIndex++;
                }
                break;
            }
            bitIndex += BITS.length;
        }
        
        if (allNull) {
            return KeyAnalyzer.NULL_BIT_KEY;
        }
        
        if (bitIndex == LENGTH_IN_BITS) {
            return KeyAnalyzer.EQUAL_BIT_KEY;
        }
        
        return bitIndex;
    }
    
    /**
     * Returns the XOR distance between the current and given KUID.
     */
    public KUID xor(KUID nodeId) {
        byte[] result = new byte[id.length];
        for(int i = 0; i < result.length; i++) {
            result[i] = (byte)(id[i] ^ nodeId.id[i]);
        }
        
        return new KUID(result);
    }
    
    /**
     * Inverts all bits of the current KUID.
     */
    public KUID invert() {
        byte[] result = new byte[id.length];
        for(int i = 0; i < result.length; i++) {
            result[i] = (byte)~id[i];
        }
        return new KUID(result);
    }
    
    /**
     * 
     * 
     * Returns true if the distance from this KUID to <tt>targetId</tt>, is 
     * smaller than the distance from <tt>otherId</tt> to <tt>targetId</tt>.
     * 
     * <pre>
     *  KUID thisKUID = KUID.createWithHexString("0000000000000000000000000000000000000000");
     *  KUID targetID = KUID.createWithHexString("0000000000000000000000000000000000000001");
     *  KUID otherID  = KUID.createWithHexString("1000000000000000000000000000000000000000");
     *        
     *  System.out.println("Distance thisKUID to targetID: " + thisKUID.xor(targetID));
     *  System.out.println("Distance otherID  to targetID: " + otherID.xor(targetID));
     *  
     *  System.out.println("thisKUID to targetID is closer than otherID to targetID: " 
     *                     + thisKUID.isNearerTo(targetID, otherID));
     * 
     * Output:
     * Distance thisKUID to targetID: 0000000000000000000000000000000000000001
     * Distance otherID  to targetID: 1000000000000000000000000000000000000001
     * thisKUID to targetID is closer than otherID to targetID: true
     * </pre>
     * @param targetId the target ID
     * @param otherId the other KUID to compare to
     * 
     * @return true if this KUID is nearer to targetID, false otherwise
     */
    public boolean isNearerTo(KUID targetId, KUID otherId) {
        int xorToSelf = 0;
        int xorToOther = 0;
        
        for (int i = 0; i < id.length; i++){
            xorToSelf = (id[i] ^ targetId.id[i]) & 0xFF;
            xorToOther = (otherId.id[i] ^ targetId.id[i]) & 0xFF;

            if (xorToSelf < xorToOther) {
                return true;
            } else if (xorToSelf > xorToOther) {
                return false;
            } // else continue
        }
        
        return false;
    }
    
    /**
     * Returns the raw bytes of the current KUID. The
     * returned byte[] array is a copy and modifications
     * are not reflected to this KUID.
     */
    public byte[] getBytes() {
        return getBytes(0, new byte[id.length], 0, id.length);
    }
    
    /**
     * Returns the raw bytes of the current KUID from the specified interval.
     */
    public byte[] getBytes(int srcPos, byte[] dest, int destPos, int length) {
        System.arraycopy(id, srcPos, dest, destPos, length);
        return dest;
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }
    
    public int compareTo(KUID o) {
        int d = 0;
        for(int i = 0; i < id.length; i++) {
            d = (id[i] & 0xFF) - (o.id[i] & 0xFF);
            if (d < 0) {
                return -1;
            } else if (d > 0) {
                return 1;
            }
        }
        
        return 0;
    }
    
    /**
     * Returns whether or not both KUIDs are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } if (!(o instanceof KUID)) {
            return false;
        } else {
            return Arrays.equals(id, ((KUID)o).id);
        }
    }

    /**
     * Returns the current KUID as hex String.
     */
    public String toHexString() {
        return ArrayUtils.toHexString(id);
    }
    
    /**
     * Returns the current KUID as bin String.
     */
    public String toBinString() {
        return ArrayUtils.toBinString(id);
    }
    
    /**
     * Returns the current KUID as BigInteger.
     */
    public BigInteger toBigInteger() {    
        return new BigInteger(1 /* unsigned! */, id);
    }
    
    /**
     * Returns the approximate log2. See BigInteger.bitLength()
     * for more info!
     */
    public int log2() {
        return toBigInteger().bitLength();
    }
    
    @Override
    public String toString() {
        return toHexString();
    }
    
    /**
     * Compute common prefix length of two KUIDs.
     */   
    public int getCommonPrefixLength(KUID other) {
        int commonPrefixLength = 0;
        for (int i = 0; i < id.length; i++) {
            byte xorValue = (byte)(id[i] ^ other.id[i]);
            if (xorValue == 0) {
                commonPrefixLength += BITS.length;
            } else {
                for (int j = 0; j < BITS.length; j++) {
                    if ((xorValue & BITS[j]) == 0) {
                        commonPrefixLength++;
                    } else {
                        return commonPrefixLength;
                    }
                }
            }
        }   
        return commonPrefixLength;
    }
    
    /**
     * Creates and returns a random ID that is hopefully
     * globally unique.
     */
    public static KUID createRandomID() {
        
        /*
         * Random Numbers.
         */
        MessageDigestInput randomNumbers = new MessageDigestInput() {
            @Override
            public void update(MessageDigest md) {
                byte[] random = new byte[LENGTH * 2];
                GENERATOR.nextBytes(random);
                md.update(random);
            }
        };
        
        /*
         * System Properties. Many of them are not unique but
         * properties like user.name, user.home or os.arch will
         * add some randomness.
         */
        MessageDigestInput properties = new MessageDigestInput() {
            @Override
            public void update(MessageDigest md) {
                Properties props = System.getProperties();
                try {
                    for (Entry entry : props.entrySet()) {
                        String key = (String)entry.getKey();
                        String value = (String)entry.getValue();
                        
                        md.update(key.getBytes("UTF-8"));
                        md.update(value.getBytes("UTF-8"));
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        
        /*
         * System time in milliseconds (GMT). Many computer clocks
         * are off. Should be a good source for randomness.
         */
        MessageDigestInput millis = new MessageDigestInput() {
            @Override
            public void update(MessageDigest md) {
                long millis = System.currentTimeMillis();
                md.update((byte)((millis >> 56L) & 0xFFL));
                md.update((byte)((millis >> 48L) & 0xFFL));
                md.update((byte)((millis >> 40L) & 0xFFL));
                md.update((byte)((millis >> 32L) & 0xFFL));
                md.update((byte)((millis >> 24L) & 0xFFL));
                md.update((byte)((millis >> 16L) & 0xFFL));
                md.update((byte)((millis >>  8L) & 0xFFL));
                md.update((byte)((millis       ) & 0xFFL));
            }
        };
        
        /*
         * VM/machine dependent pseudo time.
         */
        MessageDigestInput nanos = new MessageDigestInput() {
            @Override
            public void update(MessageDigest md) {
                long nanos = System.nanoTime();
                md.update((byte)((nanos >> 56L) & 0xFFL));
                md.update((byte)((nanos >> 48L) & 0xFFL));
                md.update((byte)((nanos >> 40L) & 0xFFL));
                md.update((byte)((nanos >> 32L) & 0xFFL));
                md.update((byte)((nanos >> 24L) & 0xFFL));
                md.update((byte)((nanos >> 16L) & 0xFFL));
                md.update((byte)((nanos >>  8L) & 0xFFL));
                md.update((byte)((nanos       ) & 0xFFL));
            }
        };
        
        /*
         * Sort the MessageDigestInput(s) by their random
         * index (i.e. shuffle the array).
         */
        MessageDigestInput[] input = { 
            properties, 
            randomNumbers, 
            millis, 
            nanos
        };
        
        Arrays.sort(input);
        
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            
            // Get the SHA1...
            for(MessageDigestInput mdi : input) {
                mdi.update(md);
                
                // Hash also the identity hash code
                int hashCode = System.identityHashCode(mdi);
                md.update((byte)((hashCode >> 24) & 0xFF));
                md.update((byte)((hashCode >> 16) & 0xFF));
                md.update((byte)((hashCode >>  8) & 0xFF));
                md.update((byte)((hashCode      ) & 0xFF));
                
                // and the random index
                md.update((byte)((mdi.rnd >> 24) & 0xFF));
                md.update((byte)((mdi.rnd >> 16) & 0xFF));
                md.update((byte)((mdi.rnd >>  8) & 0xFF));
                md.update((byte)((mdi.rnd      ) & 0xFF));
            }
            
            return new KUID(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    private abstract static class MessageDigestInput 
            implements Comparable<MessageDigestInput> {
        
        private int rnd = GENERATOR.nextInt();
        
        public abstract void update(MessageDigest md);
        
        public int compareTo(MessageDigestInput o) {
            return rnd - o.rnd;
        }
    }
    
    /**
     * Creates and returns a KUID from a byte array.
     */
    public static KUID createWithBytes(byte[] id) {
        byte[] dst = new byte[id.length];
        System.arraycopy(id, 0, dst, 0, id.length);
        return new KUID(dst);
    }
    
    /**
     * Creates and returns a KUID from a hex encoded String.
     */
    public static KUID createWithHexString(String id) {
        return new KUID(ArrayUtils.parseHexString(id));
    }
    
    /**
     * Creates a KUID from the given InputStream.
     */
    public static KUID createWithInputStream(InputStream in) throws IOException {
        byte[] id = new byte[LENGTH];
        
        int len = -1;
        int r = 0;
        while(r < id.length) {
            len = in.read(id, r, id.length-r);
            if (len < 0) {
                throw new EOFException();
            }
            r += len;
        }
        
        return new KUID(id);
    }
    
    /**
     * Creates a random ID with the specified byte prefix.
     * 
     * @param prefix the fixed prefix bytes
     * @param depth of the Bucket in the Trie
     * @return a random KUID starting with the given prefix
     */
    public static KUID createPrefxNodeID(KUID prefix, int depth) {
        byte[] random = new byte[LENGTH];
        GENERATOR.nextBytes(random);
        return createPrefxNodeID(prefix, depth, random);
    }
    
    /**
     * Creates a random ID with the specified byte prefix.
     * 
     * @param prefix the fixed prefix bytes
     * @param depth of the Bucket in the Trie
     * @param random random bytes
     * @return a random KUID starting with the given prefix
     */
    private static KUID createPrefxNodeID(KUID prefix, int depth, byte[] random) {
        depth++;
        int length = depth/8;
        System.arraycopy(prefix.id, 0, random, 0, length);
        
        int bitsToCopy = depth % 8;
        if (bitsToCopy != 0) {
            // Mask has the low-order (8-bits) bits set
            int mask = (1 << (8-bitsToCopy)) - 1;
            int prefixByte = prefix.id[length];
            int randByte   = random[length];
            random[length] = (byte) ((prefixByte & ~mask) | (randByte & mask));
        }
        
        return new KUID(random);
    }
    
    /**
     * The default KeyAnalyzer for KUIDs.
     */
    public static final KeyAnalyzer<KUID> KEY_ANALYZER = new KUIDKeyAnalyzer();
    
    /**
     * A <code>PatriciaTrie</code> <code>KeyAnalyzer</code> for <code>KUIDs</code>.
     */
    private static class KUIDKeyAnalyzer implements KeyAnalyzer<KUID> {
        
        private static final long serialVersionUID = 6412279289438108492L;

        public int bitIndex(KUID key, int keyStart, int keyLength, KUID found, int foundStart, int foundLength) {
            if (found == null) {
                found = KUID.MINIMUM;
            }
            
            return key.bitIndex(found);
        }

        public int bitsPerElement() {
            return 1;
        }

        public boolean isBitSet(KUID key, int keyLength, int bitIndex) {
            return key.isBitSet(bitIndex);
        }

        public boolean isPrefix(KUID prefix, int offset, int length, KUID key) {
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                if (prefix.isBitSet(i) != key.isBitSet(i)) {
                    return false;
                }
            }
            
            return true;
        }

        public int length(KUID key) {
            return KUID.LENGTH;
        }

        public int compare(KUID o1, KUID o2) {
            return o1.compareTo(o2);
        }
    }
}
