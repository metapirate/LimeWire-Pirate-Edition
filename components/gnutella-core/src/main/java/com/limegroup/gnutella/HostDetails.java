package com.limegroup.gnutella;


public interface HostDetails {
    /** 
     * Accessor for HTTP11.
     *
     * @return Whether or not we think this host supports HTTP11.
     */
    boolean isHTTP11();

    /**
     * Mutator for HTTP11.  Should be set after connecting.
     */
    void setHTTP11(boolean http11);

    /**
     * Accessor for the client guid for this file, which can be <tt>null</tt>.
     *
     * @return the client guid for this file, which can be <tt>null</tt>
     */
    byte[] getClientGUID();

    /**
     * Accessor for the speed of the host with this file, which can be 
     * <tt>null</tt>.
     *
     * @return the speed of the host with this file, which can be 
     *  <tt>null</tt>
     */
    int getSpeed();

    String getVendor();

    boolean isBrowseHostEnabled();

    /**
     * Returns the "quality" of the remote file in terms of firewalled status,
     * whether or not the remote host has open slots, etc.
     * 
     * @return the current "quality" of the remote file in terms of the 
     *  determined likelihood of the request succeeding
     */
    int getQuality();

    /**
     * Determines whether or not this RFD was a reply to a multicast query.
     *
     * @return <tt>true</tt> if this RFD was in reply to a multicast query,
     *  otherwise <tt>false</tt>
     */
    boolean isReplyToMulticast();

}
