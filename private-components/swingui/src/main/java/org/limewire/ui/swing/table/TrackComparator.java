package org.limewire.ui.swing.table;

import java.util.Comparator;

import org.limewire.util.CommonUtils;

/**
 * Compares two track meta data inputs.  Sorts so that null or empty values go to the bottom,
 *  pure string "gibberish" values go second, and normal numbers or X/Y numbers go first.
 *  
 * <p>Gibberish values are string compared, normal track values are simplified before comparing
 *  if in complex form where if meta data track is in form X/Y track = X.
 *  
 */
public class TrackComparator implements Comparator<String> {

    /**
     * An implementation of compare that groups and orders track descriptors
     *  according to the rules outlined in this class.
     */
    @Override
    public int compare(String t1, String t2) {
       
        Object track1 = t1;
        Object track2 = t2;        

        track1 = parseTrackSimple((String)track1);
        track2 = parseTrackSimple((String)track2);
        
        if (track1 == null) {
            if (track2 == null) {
                return 0;
            } else {
                return -1;
            }
        } 
        else {
            if (track2 == null) {
                return 1;
            } else {
                return compareConverted(track1, track2);
            }
        }        
    }

    /**
     * Compares non null track data of the simplified Long structure or
     *  a gibberish string.
     *  
     * <p>Gibberish goes to the bottom and is String sorted.
     * <p>Longs go to the top and are Long sorted.
     *  
     * <p>NOTE: track1 and track2 must be instances of String or Long
     */
    private static int compareConverted(Object track1, Object track2) {
        if (track1 instanceof Long) {
            if (track2 instanceof Long) {
                return ((Long)track1).compareTo((Long) track2);
            } else if (track2 instanceof String) {
                return 1;
            } else {
                throw new IllegalArgumentException("track2 must be an instance of String or Long");
            }
        } else if (track1 instanceof String) {
            if (track2 instanceof Long) {
                return -1;
            } else if (track2 instanceof String) {
                return ((String)track1).compareTo((String) track2);
            } else {
                throw new IllegalArgumentException("track2 must be an instance of String or Long");
            }
        } else {
            throw new IllegalArgumentException("track1 must be an instance of String or Long");
        }
    }
    
    /**
     * Takes a raw track descriptor string and attempts to simplify it.
     * 
     * <p>If the String is empty it will return null.
     * <p>If the String is numeric returns a Long type containing the value.
     * <p>If the String is in the format X/Y return a Long type of the numerator X. 
     * <p>Otherwise simply return the original <b>unmodified</b> String.
     */
    private static Object parseTrackSimple(String rawTrack) {
        if (rawTrack == null) {
            return null;
        }
        
        String track = rawTrack.trim();
        if ("".equals(track)) {
            return null;
        }
        
        Long trackSimple = CommonUtils.parseLongNoException(track); 
        
        if (trackSimple != null) {
            return trackSimple;
        }
            
        int slashIndex = track.indexOf('/');
        if (slashIndex > -1) {
            Long simpleTrack = CommonUtils.parseLongNoException(track.substring(0, slashIndex));
            if (simpleTrack != null) {
                return simpleTrack;
            }
        }
        
        return rawTrack;
    }
}
