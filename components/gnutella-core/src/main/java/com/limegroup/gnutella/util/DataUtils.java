package com.limegroup.gnutella.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import org.limewire.util.ByteUtils;


/**
 * Utility class that supplies commonly used data sets that each
 * class should not have to create on its own.  These data sets
 * are immutable objects, so any class and any thread may access them
 * whenever they like.
 */
public final class DataUtils {
    
    /**
     * Ensure that this class cannot be constructed.
     */
    private DataUtils() {}
    
    /**
     * Constant empty byte array for any class to use -- immutable.
     */
    public static byte[] EMPTY_BYTE_ARRAY = new byte[0];
    
    /**
     * An empty byte array length 1.
     */
    public static byte[] BYTE_ARRAY_ONE = new byte[1];
    
    /**
     * An empty byte array length 2.
     */
    public static byte[] BYTE_ARRAY_TWO = new byte[2];
    
    /**
     * An empty byte array length 3.
     */
    public static byte[] BYTE_ARRAY_THREE = new byte[3];
    
    static {
        BYTE_ARRAY_ONE[0] = 0;
        BYTE_ARRAY_TWO[0] = 0;
        BYTE_ARRAY_TWO[1] = 0;
        BYTE_ARRAY_THREE[0] = 0;
        BYTE_ARRAY_THREE[1] = 0;
        BYTE_ARRAY_THREE[2] = 0;
    }
    
    /**
     * Constant empty string array for any class to use -- immutable.
     */
    public static String[] EMPTY_STRING_ARRAY = new String[0];
        
    /**
     * An 16-length empty byte array, for GUIDs.
     */
    public static final byte[] EMPTY_GUID = new byte[16];
    
    /**
     * The amount of milliseconds in a week.
     */
    public static final long ONE_WEEK = 7 * 24 * 60 * 60 * 1000;
    
    /**
     * Determines whether or not the the child Set contains any elements
     * that are in the parent's set.
     */
    public static boolean containsAny(Collection<?> parent, Collection<?> children) {
        for(Iterator<?> i = children.iterator(); i.hasNext(); )
            if(parent.contains(i.next()))
                return true;
        return false;
    }    
    
    /**
     * Utility function to write out the toString contents
     * of a URN.
     */
    public static String listSet(Set<?> s) {
        StringBuilder sb = new StringBuilder();
        for(Iterator<?> i = s.iterator(); i.hasNext();)
            sb.append(i.next().toString());
        return sb.toString();
    }

    /**
     * Prints out the contents of the input array as a hex string.
     */
    public static String toHexString(byte[] bytes) {
        StringBuilder buf=new StringBuilder();
        String str;
        int val;
        for (int i=0; i<bytes.length; i++) {
            //Treating each byte as an unsigned value ensures
            //that we don't str doesn't equal things like 0xFFFF...
            val = ByteUtils.ubyte2int(bytes[i]);
            str = Integer.toHexString(val);
            while ( str.length() < 2 )
            str = "0" + str;
            buf.append( str );
        }
        return buf.toString().toUpperCase(Locale.US);
    }

}
