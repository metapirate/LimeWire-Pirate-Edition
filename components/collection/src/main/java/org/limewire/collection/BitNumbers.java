package org.limewire.collection;

import java.util.Locale;
import java.util.Arrays;



/** 
 * Allows storage & retrieval of numbers based on the index of an
 * on or off bit in a byte[] or a hexadecimal String representation
 * of that byte[].
 */
public class BitNumbers {
    
    private static final byte[] EMPTY = new byte[0];

    /** A set of masks for finding if the bit is set at the right index. */
    private static final byte[] MASKS = new byte[] {
        (byte)0x80, 0x40, 0x20, 0x10, 0x8, 0x4, 0x2, 0x1
    };
    
    /** The bits that are stored. */
    private byte[] data;
    
    /** The total size of this bitNumbers. */
    private final int size;
    
    /** A convenient shared immutable empty BitNumbers. */
    public static final BitNumbers EMPTY_BN = new ImmutableBitNumbers();
    
    /** Constructs a BitNumbers backed by the given byte[]. */
    public BitNumbers(byte[] data) {
        this.data = data;
        this.size = data.length * 8;
    }
    
    /** Constructs a BitNumbers large enough to store numbers up to size. */
    public BitNumbers(int size) {
        this.size = size;
    }
    
    /**
     * Constructs a BitNumbers based on the given hex string.
     * This accepts a nibble for the last element,
     * thus: 
     * <pre>
     *    FF   corresponds to elements 0 through 8 being on
     *    FFF  corresponds to elements 0 through 12 being on (implies below)
     *    FFF0 corresponds to elements 0 through 12 being on also
     * </pre>
     */
    public BitNumbers(String hexString) throws IllegalArgumentException {
        this.data = new byte[(int)Math.ceil(hexString.length() / 2d)];
        this.size  = data.length * 8;
        
        // Now fill up data, decoding the string...
        for(int i = 0; i < hexString.length(); i+=2) {
            // Will throw NFE (which extends IAE) if data is invalid
            boolean nibble = i == hexString.length() - 1;
            int j = Integer.parseInt(hexString.substring(i, nibble ? i+1 : i+2), 16);
            if(nibble) // the last element may just be a nibble
                j <<= 4;
            assert j <= 0xFF;
            data[i/2] = (byte)j;
        }
    }
    
    /** Returns true if the correct bit is set. */
    public boolean isSet(int idx) {
        if(idx >= size)
            return false;
        
        int index = (int)Math.floor(idx / 8d);
        int offset = idx % 8;
        return data != null && index < data.length && (data[index] & MASKS[offset]) != 0;
    }
    
    /** Returns the maximum number that can be stored in this BitNumbers. */
    public int getMax() {
        return size;
    }
    
    /** Sets the bit corresponding to the index. */
    public void set(int idx) {
        if(idx >= size)
            throw new IndexOutOfBoundsException("idx: " + idx + ", max: " + size);
        
        int index = (int)Math.floor(idx / 8d);
        int offset = idx % 8;
        if(data == null)
            data = new byte[(int)Math.ceil(size / 8d)];
        data[index] |= MASKS[offset];
    }
    
    /** Returns the byte array that BitNumbers is backed off of. */
    public byte[] toByteArray() {
        if(data == null) {
            return EMPTY;
        } else {
            int lastNonZero = getLastNonZeroIndex();
            if(lastNonZero == -1) { // completely empty
                return EMPTY;
            } else if(lastNonZero == data.length - 1) { // uses full width
                return data;
            } else { // must strip out the extra bytes.
                byte[] shortened = new byte[lastNonZero+1];
                System.arraycopy(data, 0, shortened, 0, lastNonZero+1);
                return shortened;
            }
        }
    }
    
    /** Returns true if no set bits exist. */
    public boolean isEmpty() {
        if(data == null)
            return true;

        for (byte b : data)
            if (b != 0)
                return false;
        return true;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BitNumbers) {
            BitNumbers other = (BitNumbers)obj;
            return Arrays.equals(toByteArray(), other.toByteArray());
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(toByteArray());
    }
    
    /** A hexadecimal representation of the byte[]. */
    public String toHexString() {
        if(isEmpty()) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder(data.length * 2);
            int lastNonZero = 0;
            for(int i = 0; i < data.length; i++) {
                if(data[i] != 0)
                    lastNonZero = i;
                String hex = Integer.toHexString(data[i] & 0x00FF);
                if(hex.length() == 1)
                    sb.append("0");
                sb.append(hex.toUpperCase(Locale.US));
            }
            sb.setLength(lastNonZero * 2 + 2); // erase empty fields.
            if(sb.length() > 1 && sb.charAt(sb.length()-1) == '0')
                sb.setLength(sb.length()-1);
            return sb.toString();
        }
    }
    
    /** Returns the last non-empty index. */
    private int getLastNonZeroIndex() {
        for(int i = data.length - 1; i >= 0; i--) {
            if(data[i] != 0)
                return i;
        }
        return -1;
    }
    
    @Override
    public String toString() {
        return toHexString();
    }
    
    /** A BitNumbers that is empty and non-settable. */
    private static class ImmutableBitNumbers extends BitNumbers {
        @Override
        public void set(int idx) {
            throw new UnsupportedOperationException("immutable!");
        }

        ImmutableBitNumbers() {
            super(0);
        }
        
    }
    
    public static BitNumbers synchronizedBitNumbers(BitNumbers delegate) {
        return new SynchronizedBitNumbers(delegate);
    }

    private static class SynchronizedBitNumbers extends BitNumbers {

        private final BitNumbers delegate;
        private SynchronizedBitNumbers(BitNumbers delegate) {
            super(0);
            this.delegate = delegate;
        }

        @Override
        public synchronized boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public synchronized int getMax() {
            return delegate.getMax();
        }

        @Override
        public synchronized boolean isSet(int idx) {
            return delegate.isSet(idx);
        }

        @Override
        public synchronized void set(int idx) {
            delegate.set(idx);
        }

        @Override
        public synchronized byte[] toByteArray() {
            return delegate.toByteArray();
        }

        @Override
        public synchronized String toHexString() {
            return delegate.toHexString();
        }

        @Override
        public synchronized String toString() {
            return delegate.toString();
        }

    }

}
