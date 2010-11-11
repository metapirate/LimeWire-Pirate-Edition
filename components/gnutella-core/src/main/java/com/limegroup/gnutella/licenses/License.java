package com.limegroup.gnutella.licenses;

import java.net.URI;
import java.net.URL;

import org.limewire.http.httpclient.LimeHttpClient;

import com.limegroup.gnutella.URN;

/**
 * Contains methods related to verification.
 * <p>
 * It is possible that the License is a bulk license and contains
 * information related to multiple works.  This license is encapsulated
 * so that it contains information unique to a single verification location.
 * Methods that retrieve information specific to a particular work should
 * provide a URN to identify that work.  If the provided URN is null,
 * information will be given on a best-guess basis.
 */
public interface License {
    
    static final int NO_LICENSE = -1;
    static final int UNVERIFIED = 0;
    static final int VERIFYING = 1;
    static final int VERIFIED = 2;
    
    /**
     * True if this license has been externally verified.
     * <p>
     * This does NOT indicate whether or not the license was valid.
     */
    public boolean isVerified();
    
    /**
     * True if this license is currently being or in queue for verification.
     */
    public boolean isVerifying();
    
    /**
     * True if this license was verified and is valid & matches the given URN.
     * <p>
     * If the provided URN is null, this will return true as long as at least
     * one work in this license is valid.  If the license provided no URNs
     * for a work, this will also return true.  If URNs were provided for
     * all works and a URN is given here, this will only return true if the
     * URNs match.
     */
    public boolean isValid(URN urn);
    
    /**
     * Returns a description of this license.
     * <p>
     * Retrieves the description for the particular URN.  If no URN is given,
     * a best-guess is used to extract the correct description.
     */
    public String getLicenseDescription(URN urn);
    
    /**
     * Returns a URI that the user can visit to manually verify.
     */
    public URI getLicenseURI();
    
    /**
     * Returns the location of the deed for this license.
     * <p>
     * Retrieves the deed for the work with the given URN.  If no URN is given,
     * a best-guess is used to extract the correct license deed.
     */
    public URL getLicenseDeed(URN urn);
    
    /**
     * Returns the license, in human readable form.
     */
    public String getLicense();
    
    /**
     * Verifies the license. The results of the verification can be retrieved by
     * invoking {@link #isValid(URN)}.
     */
    public void verify(LicenseCache licenseCache, LimeHttpClient httpClient);
    
    /**
     * Returns the last time this license was verified.
     */
    public long getLastVerifiedTime();
    
    /**
     * Returns a copy of this license with a new 'license' string and URI.
     */
    public License copy(String license, URI licenseURI);
    
    /**
     * Gets the name of this license.
     * For example, "Creative Commons License", or "Weed License".
     */
    public String getLicenseName();
}