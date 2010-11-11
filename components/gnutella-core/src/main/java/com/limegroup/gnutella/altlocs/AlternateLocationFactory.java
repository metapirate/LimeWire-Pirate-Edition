package com.limegroup.gnutella.altlocs;

import java.io.IOException;

import org.limewire.io.IpPort;

import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;

public interface AlternateLocationFactory {

    /**
     * Creates a new <tt>AlternateLocation</tt> for a file stored locally 
     * with the specified <tt>URN</tt>.
     * <p>
     * Note: the altloc created this way does not know the name of the file.
     *
     * @param urn the <tt>URN</tt> of the locally stored file
     */
    public AlternateLocation create(URN urn);

    /**
     * Creates a new <tt>AlternateLocation</tt> for the data stored in
     * a <tt>RemoteFileDesc</tt>.
     *
     * @param rfd the <tt>RemoteFileDesc</tt> to use in creating the 
     *  <tt>AlternateLocation</tt>
     * @return a new <tt>AlternateLocation</tt>
     * @throws <tt>IOException</tt> if the <tt>rfd</tt> does not contain
     *  a valid urn or if it's a private address
     * @throws <tt>NullPointerException</tt> if the <tt>rfd</tt> is 
     *  <tt>null</tt>
     * @throws <tt>IOException</tt> if the port is invalid
     */
    public AlternateLocation create(final RemoteFileDesc rfd)
            throws IOException;

    /**
     * Creates a new push AlternateLocation.
     */
    public AlternateLocation createPushAltLoc(PushEndpoint pe, URN urn);
            

    /**
     * Creates a new direct AlternateLocation from information that was
     * found in the DHT.
     */
    public AlternateLocation createDirectDHTAltLoc(IpPort ipp, URN urn,
            long fileSize, byte[] ttroot) throws IOException;

    /**
     * Creates a new direct AlternateLocation.
     */
    public AlternateLocation createDirectAltLoc(IpPort ipp, URN urn)
            throws IOException;

    /**
     * Constructs a new <tt>AlternateLocation</tt> instance based on the
     * specified string argument and URN.  The location created this way
     * assumes the name "ALT" for the file.
     *
     * @param location a string containing one of the following:
     *  "http://my.address.com:port#/uri-res/N2R?urn:sha:SHA1LETTERS" or
     *  "1.2.3.4[:6346]" or
     *  http representation of a PushEndpoint.
     * <p>
     * If the first is given, then the SHA1 in the string MUST match
     * the SHA1 given.
     * 
     * @param URN the urn to use when the location doesn't contain a URN
     * @param tlsCapable if the alternate location is capable of receiving
     *                   TLS connections.  valid only if the location was
     *                   not a full URL.
     *
     * @throws <tt>IOException</tt> if there is any problem constructing
     *  the new instance.
     */
    public AlternateLocation create(String location, URN urn, boolean tlsCapable)
            throws IOException;

    /**
     * Constructs a new <tt>AlternateLocation</tt> instance based on the
     * specified string argument and URN.  The location created this way
     * assumes the name "ALT" for the file.
     *
     * @param location a string containing one of the following:
     *  "http://my.address.com:port#/uri-res/N2R?urn:sha:SHA1LETTERS" or
     *  "1.2.3.4[:6346]" or
     *  http representation of a PushEndpoint.
     * <p>
     * If the first is given, then the SHA1 in the string MUST match
     * the SHA1 given.
     * 
     * @param URN the urn to use when the location doesn't contain a URN
     *
     * @throws <tt>IOException</tt> if there is any problem constructing
     *  the new instance.
     */
    public AlternateLocation create(final String location, final URN urn)
            throws IOException;

}