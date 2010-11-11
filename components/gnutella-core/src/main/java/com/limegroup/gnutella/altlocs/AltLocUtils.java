package com.limegroup.gnutella.altlocs;

import java.io.IOException;
import java.util.StringTokenizer;

import org.limewire.collection.BitNumbers;
import org.limewire.collection.Function;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPUtils;

/** 
 * Provides utility methods relating to {@link AlternateLocation} objects. 
 */
public class AltLocUtils {
    
    private AltLocUtils() {}   
    
    /**
     * Parses an http string of alternate locations, passing each parsed
     * location to the given function.
     * If either sha1 or locations are null, nothing is done.
     * @param sha1 The expected sha1 of each location.  If mismatched, an AssertionError is thrown.
     * @param locations The comma-separated string of alternate locations.
     * @param allowTLS Whether or not a tls=# index is allowed.
     * @param function The closure-like function that each location is passed to.
     */
    public static void parseAlternateLocations(URN sha1, String locations, boolean allowTLS, AlternateLocationFactory alternateLocationFactory, 
            Function<AlternateLocation, Void> function) {
        parseAlternateLocations(sha1, locations, allowTLS, alternateLocationFactory, function, false);
    }
    
    public static void parseAlternateLocations(URN sha1, String locations, boolean allowTLS,  AlternateLocationFactory alternateLocationFactory, 
                                               Function<AlternateLocation, Void> function, boolean allowMe) {
        if(locations == null)
            return;
        if(sha1 == null)
            return;

        BitNumbers tlsIdx = null;
        StringTokenizer st = new StringTokenizer(locations, ",");
        int idx = 0;
        while(st.hasMoreTokens()) {
            String token = st.nextToken().trim();
            if(allowTLS && tlsIdx == null && token.startsWith(DirectAltLoc.TLS_IDX)) {
                tlsIdx = BitNumbers.EMPTY_BN;
                try {
                    String value = HTTPUtils.parseValue(token);
                    if(value != null) {
                        try {
                            tlsIdx = new BitNumbers(value);
                        } catch(IllegalArgumentException ignored) {}
                    }
                } catch(IOException invalid) {}
                continue;
            }
            
            // if we didn't set a BitNumbers above, stop us from ever doing it again.
            if(tlsIdx == null) {
                tlsIdx = BitNumbers.EMPTY_BN;
            } 

            try {
                AlternateLocation al = alternateLocationFactory.create(token, sha1, tlsIdx.isSet(idx));
                idx++;
                
                assert al.getSHA1Urn().equals(sha1) : "sha1 mismatch!";
                if (al.isMe() && !allowMe) 
                    continue;
                
                function.apply(al);
            } catch(IOException e) {
                tlsIdx = BitNumbers.EMPTY_BN; // prevent us from reading future alt-locs as tls-capable
            }
        }
        
        
    }

}
