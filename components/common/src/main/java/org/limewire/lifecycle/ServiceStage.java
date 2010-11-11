package org.limewire.lifecycle;

public enum ServiceStage {

    /**
     * gnutella message routing
     */
    EARLY,

    /**
     * The default stage for all Services
     */
    NORMAL,

    /**
     * Registering http request handlers and connecting to Gnutella
     */
    LATE,

    /**
     * Things that can be safely done as extra tasks after the most important 
     * Services are started to support core functionality - i.e., search and download. 
     */
    VERY_LATE
    
}
