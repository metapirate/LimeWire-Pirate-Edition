package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.Set;

/**
 * Provides utility methods like checking set intersection etc.
 * @author Anurag Singla
 */
public class Utilities 
{
    /**
     * Determines if two sets have non-void intersection.
     * @param set1 first set
     * @param set2 second set
     * @return true, if two sets have non-void intersection, false otherwise
     */
    public static boolean hasIntersection(Set set1, Set set2)
    {
        //Iterate over the first set, and check the value in the second set
        for(Iterator iterator = set1.iterator(); iterator.hasNext();)
        {
            //if second set contains the entry, return true
            if(set2.contains(iterator.next()))
                return true;
        }
        //if no match found, return true
        return false;
    }

    /**
     * An optimized replacement for Arrays.fill that takes advantage of System.arraycopy.
     * On my Windows machine, this is over nearly twice as fast for arrays greater than
     * 1000 elements.  The savings decrease somewhat over 32000 elements.
     *
     * @param array the array to fill
     * @param start the starting offset, inclusive
     * @param stop the stop offset+1.  <b>MUST be greater than start; this differs
     *  from Arrays.fill.</b>
     * @param value the value to write into the array
     */
    public static void fill(int array[], int start, int stop, int value) {
        array[start] = value;
        int span=1;
        for (int i=start+1; i<stop; ) {
            System.arraycopy(array, start, array, i, Math.min(span, stop-i));
            i+=span;
            span=span*2;
        }
    }
    
    /**
     * An optimized replacement for Arrays.fill that takes advantage of System.arraycopy.
     * On someone's Windows machine, this is over nearly twice as fast for arrays greater
     * than 1000 elements.  The savings decrease somewhat over 32000 elements.
     *
     * @param array the array to fill
     * @param start the starting offset, inclusive
     * @param stop the stop offset+1.  <b>MUST be greater than start; this differs
     *  from Arrays.fill.</b>
     * @param value the value to write into the array
     */
    public static void fill(byte array[], int start, int stop, byte value) {
        array[start] = value;
        int span=1;
        for (int i=start+1; i<stop; ) {
            System.arraycopy(array, start, array, i, Math.min(span, stop-i));
            i+=span;
            span=span*2;
        }
    }    


    /**
     * Returns the based 2 logarithm of num.
     * @param num MUST be a power of 2
     */
    public static byte log2(int num) {
        //Binary search submitted by Philippe Verdy uses 5 comparisons.
        //Previously "return (byte)(Math.log(num) / Math.log(2))", but that had
        //potential rounding errors.
        if      (num <             0x10000)
            if      (num <           0x100)
                if      (num <        0x10)
                    if      (num <     0x4)
                        if  (num <     0x2) return  0; else return  1;
                    else if (num <     0x8) return  2; else return  3;
                else if (num <        0x40)
                    if      (num <    0x20) return  4; else return  5;
                else if (num <        0x80) return  6; else return  7;
            else if (num <          0x1000)
                if      (num <       0x400)
                    if      (num <   0x200) return  8; else return  9;
                else if (num <       0x800) return 10; else return 11;
            else if (num <          0x4000)
                if       (num <     0x2000) return 12; else return 13;
            else if (num <          0x8000) return 14; else return 15;
        else if (num <           0x1000000)
            if      (num <        0x100000)
                if      (num <     0x40000)
                    if      (num < 0x20000) return 16; else return 17;
                else if (num <     0x80000) return 18; else return 19;
            else if (num <        0x400000)
                if      (num <    0x200000) return 20; else return 21;
            else if (num <        0x800000) return 22; else return 23;
        else if (num <          0x10000000)
            if      (num <       0x4000000)
                if      (num <   0x2000000) return 24; else return 25;
            else if (num <       0x8000000) return 26; else return 27;
        else if (num <          0x40000000)
            if      (num <      0x20000000) return 28; else return 29;
        else/*if(num <          0x80000000)*/return 30;/*else return 31;*/
    }
    
    /** Returns true iff the argument is a power of two */
    public static boolean isPowerOf2(int x) {
        if (x<=0)
            return false;
        else
            return (x&(x - 1)) == 0;
    }
}
