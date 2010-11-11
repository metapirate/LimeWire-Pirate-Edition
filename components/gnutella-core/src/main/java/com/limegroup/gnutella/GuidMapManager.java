package com.limegroup.gnutella;

public interface GuidMapManager {

    /**
     * Constructs a new GuidMap.
     * Returned GuidMaps will expire within 4 minutes of the expected expiry time.
     */
    public GuidMap getMap();

    /**
     * Removes a map from our accounting.
     * @param expiree
     */
    public void removeMap(GuidMap expiree);

}