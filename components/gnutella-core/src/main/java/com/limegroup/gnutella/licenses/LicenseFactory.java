package com.limegroup.gnutella.licenses;

import com.limegroup.gnutella.URN;

public interface LicenseFactory {

    public static final String WEED_NAME = "Weed License";
    public static final String CC_NAME = "Creative Commons License";
    public static final String UNKNOWN_NAME = "Unknown License";
    
    /**
     * Checks if the specified license-URI is valid for the given URN
     * without doing any expensive lookups.
     * <p>
     * The URI must have been retrieved via getLicenseURI.
     *
     */
    public boolean isVerifiedAndValid(URN urn, String licenseString);

    /**
     * Gets the name associated with this license string.
     */
    public String getLicenseName(String licenseString);

    /**
     * Returns a License for the given license string, if one
     * can be constructed.  If no License exists to validate
     * the license, returns null.
     */
    public License create(String licenseString);

    /**
     * Persists the cache.
     */
    public void persistCache();

}