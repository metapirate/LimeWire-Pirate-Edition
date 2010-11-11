package com.limegroup.gnutella.altlocs;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.http.HTTPHeaderValue;

/**
 * This interface defines classes that encapsulate the data for an alternate resource location, as 
 * specified in HUGE v0.93.  This also provides utility methods for such 
 * operations as comparing alternate locations based on the date they were 
 * stored.
 * 
 * Firewalled hosts can also be alternate locations, although the format is
 * slightly different.
 */
public interface AlternateLocation extends HTTPHeaderValue, Comparable<AlternateLocation> {

    /**
     * The vendor to use.
     */
    public static final String ALT_VENDOR = "ALT";

    /**
     * The three types of medium altlocs travel through.
     */
    public static final int MESH_PING = 0;

    public static final int MESH_LEGACY = 1;

    public static final int MESH_RESPONSE = 2;

    /**
     * Accessor for the SHA1 urn for this <tt>AlternateLocation</tt>.
     * <p>
     * @return the SHA1 urn for the this <tt>AlternateLocation</tt>
     */
    URN getSHA1Urn();

    /**
     * Accessor to find if this has been demoted.
     */
    int getCount();

    /**
     * package access, accessor to the value of _demoted.
     */
    boolean isDemoted();

    /**
     * Creates a new <tt>RemoteFileDesc</tt> from this AlternateLocation.
     *
     * @param size the size of the file for the new <tt>RemoteFileDesc</tt> 
     *  -- this is necessary to make sure the download bucketing works 
     *  correctly
     * @return new <tt>RemoteFileDesc</tt> based off of this, or 
     *  <tt>null</tt> if the <tt>RemoteFileDesc</tt> could not be created
     */
    RemoteFileDesc createRemoteFileDesc(long size, RemoteFileDescFactory remoteFileDescFactory);

    /**
     * 
     * @return whether this is an alternate location pointing to myself.
     */
    boolean isMe();

    /**
     * Increment the count.
     * @see demote
     */
    void increment();

    /**
     * Could return null.
     */
    AlternateLocation createClone();

    void send(long now, int meshType);

    boolean canBeSent(int meshType);

    boolean canBeSentAny();

}