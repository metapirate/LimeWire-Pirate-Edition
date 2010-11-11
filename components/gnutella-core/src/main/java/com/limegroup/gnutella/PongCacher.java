package com.limegroup.gnutella;

import java.util.List;

import com.limegroup.gnutella.messages.PingReply;

public interface PongCacher {

    /**
     * Constant for the number of pongs to store per hop.  Public to make
     * testing easier.
     */
    public static final int NUM_PONGS_PER_HOP = 1;

    /**
     * Constant for the number of hops to keep track of in our pong cache.
     */
    public static final int NUM_HOPS = 6;

    /**
     * Constant for the number of seconds to wait before expiring cached pongs.
     */
    public static final int EXPIRE_TIME = 6000;

    /**
     * Constant for expiring locale specific pongs
     */
    public static final int EXPIRE_TIME_LOC = 15 * EXPIRE_TIME;

    /**
     * Accessor for the <tt>Set</tt> of cached pongs.  This <tt>List</tt>
     * is unmodifiable and will throw <tt>IllegalOperationException</tt> if
     * it is modified.
     *
     * @return the <tt>List</tt> of cached pongs -- continually updated
     */
    List<PingReply> getBestPongs(String loc);

    /**
     * Adds the specified <tt>PingReply</tt> instance to the cache of pongs.
     *
     * @param pr the <tt>PingReply</tt> to add
     */
    void addPong(PingReply pr);

}