package com.limegroup.gnutella;

import java.util.Collections;
import java.util.List;

import org.apache.http.auth.Credentials;
import org.limewire.io.Address;
import org.limewire.security.SecureMessage.Status;

import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;

/**
 * A reference to a single file on a remote machine.  In this respect
 * RemoteFileDesc is similar to a URL, but it contains Gnutella-
 * specific data as well, such as the server's 16-byte GUID.<p>
 */
public interface RemoteFileDesc extends RemoteFileDetails {

    /** bogus IP we assign to RFDs whose real ip is unknown */
    public static final String BOGUS_IP = "1.1.1.1";

    /** Typed reference to an empty list of RemoteFileDescs. */
    public static final List<RemoteFileDesc> EMPTY_LIST = Collections.emptyList();

    /**
     * @return whether this rfd points to myself.
     */
    public boolean isMe(byte[] myClientGUID);

    /**
     * Determines whether or not this RemoteFileDesc was created
     * from an alternate location.
     */
    public boolean isFromAlternateLocation();

    /**
     * Returns the url encoded HTTP request path.
     */
    public String getUrlPath();
    
    /**
     * Returns credentials needed for downloading this file from the remote
     * side
     * @return null if no credentials are needed
     */
    public Credentials getCredentials();

    /**
     * @return true if I am not a multicast host and have a hash.
     * also, if I am firewalled I must have at least one push proxy,
     * otherwise my port and address need to be valid.
     */
    public boolean isAltLocCapable();

    public void setSpamRating(float rating);

    public float getSpamRating();

    public Status getSecureStatus();

    public void setSecureStatus(Status secureStatus);

    /**
     * Returns a memento that can be used for serializing this object.
     */
    public RemoteHostMemento toMemento();
    
    public boolean isSpam();

    public Address getAddress();

    /**
     * Returns true if this RemoteFileDesc has XML metadata or its filename
     * matches the specified query.
     */
    public boolean matchesQuery(String query);

    /**
     * Returns the query GUID if this RemoteFileDesc was constructed from a
     * query reply, otherwise null.
     */
    public byte[] getQueryGUID();
}