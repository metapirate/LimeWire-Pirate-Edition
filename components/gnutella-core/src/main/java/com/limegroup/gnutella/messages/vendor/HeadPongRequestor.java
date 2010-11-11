package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.URN;

/** Controls how HeadPongs are constructed. */
interface HeadPongRequestor {

    /** Returns the URN the request is for. */
    public URN getUrn();

    /** True if this requests wants ranges. */
    public boolean requestsRanges();

    /** True if this is requesting direct alternate locations. */
    public boolean requestsAltlocs();

    /** True if this is requesting push alternate locations. */
    public boolean requestsPushLocs();

    /** True if this is requesting FWT-capable alternate locations. */
    public boolean requestsFWTOnlyPushLocs();

    /**
     * Returns the features this request supports.
     * 
     * Note: This is only used by HeadPong v1, which mirrored most
     *       of the requested features in the reply.
     *       This field should not be used for v2+.
     *       Instead, use the specific requestsXXX methods.
     */
    public byte getFeatures();
    
    /** Determines if the Pong should be constructed with GGEP or Binary. */
    public boolean isPongGGEPCapable();
    
    /** Returns the GUID that should be used for the Pong. */
    public byte[] getGUID();

}