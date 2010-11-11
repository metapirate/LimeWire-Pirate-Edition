package com.limegroup.gnutella.util;

import java.util.Locale;
import java.util.Random;

/**
 * A simple UUID.
 */
public final class UUID {
    
    /**
     * The characters to generate this UUID with.
     */
    private static final char[] HEX = new char[] {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f'
    };
    
    /**
     * Index into the UUID to set its type.
     */
    private static final byte INDEX_TYPE = 6;
    
    /**
     * Index into the UUID to set its variation.
     */
    private static final byte INDEX_VARIATION = 8;
    
    /**
     * The specific type of the UUID.
     */
    private static final byte TYPE_RANDOM_BASED = 4;
    
    /**
     * The rnd generator.
     */
    private static final Random RANDOM = new Random();
    
    /**
     * The string representing this UUID.
     */
    private final String uuid;
    
    /**
     * Constructs a new UUID with the specified bytes.
     */
    private UUID(byte[] bytes) {
       this.uuid = genString(bytes);
    }
    
    /**
     * Constructs a new UUID with the specified string.
     *
     * The size of the string must be 32, but no other characters
     * are checked.
     */
    public UUID(String uuid) {
        this.uuid = uuid.toLowerCase(Locale.US);
        if(uuid.length() != 36)
            throw new IllegalArgumentException();
    }
    
    /**
     * Returns the next UUID.
     */
    public static UUID nextUUID() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        bytes[INDEX_TYPE] &= (byte) 0x0F;
        bytes[INDEX_TYPE] |= (byte) (TYPE_RANDOM_BASED << 4);
        bytes[INDEX_VARIATION] &= (byte) 0x3F;
        bytes[INDEX_VARIATION] |= (byte) 0x80;
        return new UUID(bytes);
    }
    
    /**
     * Creates the string of this UUID.
     */
    private static String genString(byte[] info) {
        StringBuilder sb = new StringBuilder(32);
        for(int i = 0; i < 16; i++) {
            if (i==4 || i==6 || i==8 || i==10)
                sb.append('-');
            int hex = info[i] & 0xFF;
            sb.append(HEX[hex >> 4]);
            sb.append(HEX[hex & 0x0F]);
        }
        return sb.toString();      
    }  
    
    /**
     * Generates the string of this UUID.
     */
    @Override
    public String toString() {
        return uuid;
    }
    
    /**
     * Determines if this UUID is the same as another.
     */
    @Override
    public boolean equals(Object o) {
        if(o == this)
            return true;
        else if (o instanceof UUID) {
            UUID other = (UUID)o;
            return uuid.equals(other.uuid);
        }
        return false;
    }
    
    /**
     * The hashCode of this UUID.
     */
    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
   