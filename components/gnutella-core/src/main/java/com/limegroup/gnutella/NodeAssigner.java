package com.limegroup.gnutella;

public interface NodeAssigner {

    /**
     * Schedules a timer event to continually updates the upload and download
     * bandwidth used, and assign this node accordingly.  Non-blocking.
     * Router provides the schedule(..) method for the timing
     */
    public void start();

    public void stop();

    /**
     * Accessor for whether or not this machine has settings that are too good
     * to pass up for Ultrapeer election.
     *
     * @return <tt>true</tt> if this node has extremely good Ultrapeer settings,
     *  otherwise <tt>false</tt>
     */
    public boolean isTooGoodUltrapeerToPassUp();

}