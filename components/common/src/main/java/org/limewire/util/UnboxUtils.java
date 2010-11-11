package org.limewire.util;

/** A utillity for boxing & unboxing, so that null values can be unboxed to be 0 or false. */
public class UnboxUtils {
    
    private UnboxUtils() {}

    public static int toInt(Integer obj) {
        if(obj == null) {
            return 0;
        } else {
            return obj;
        }
    }

    public static long toLong(Long obj) {
        if(obj == null) {
            return 0;
        } else {
            return obj;
        }
    }
    
    /**
     * Safely unboxes the given Long object. If the reference is null then the 
     * defaultValue is returned instead of the unboxed value. 
     */
    public static long toLong(Long obj, long defaultValue) {
        if(obj == null) {
            return defaultValue;
        } else {
            return obj;
        }
    }

    public static boolean toBoolean(Boolean obj) {
        if(obj == null) {
            return false;
        } else {
            return obj;
        }
    }
    
    

}
