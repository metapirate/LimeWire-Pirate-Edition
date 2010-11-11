package org.limewire.core.api.properties;

import java.util.List;

/**
 * Lookup for schema-backed property enumerations.
 */
public interface PropertyDictionary {
    
    List<String> getAudioGenres();
    
    List<String> getVideoGenres();
    
    List<String> getVideoRatings();
    
    List<String> getApplicationPlatforms();
}
