package com.limegroup.gnutella.licenses;

import java.net.URI;
import java.net.URL;

import org.limewire.http.httpclient.LimeHttpClient;

import com.limegroup.gnutella.URN;

/**
 * A bad Creative Commons license (unverifiable).
 */
public class BadCCLicense implements MutableLicense {
    
    private String license;
    private String name;
    
    public BadCCLicense(String license) {
        this.license = license;
    }
    
    /** Sets the license name. */
    public void setLicenseName(String name) { this.name = name; }
    
    /** Attempts to guess what the license URI is from the license text. */    
    private URL guessLicenseDeed() {
        return CCConstants.guessLicenseDeed(license);
    }    
    
    public boolean isVerified() { return true; }
    public boolean isVerifying() { return false; }
    public boolean isValid(URN urn) { return false; }
    public String getLicenseDescription(URN urn) { return "Permissions unknown."; }
    public URI getLicenseURI() { return null; }
    public URL getLicenseDeed(URN urn) { return guessLicenseDeed(); }
    public String getLicense() { return license; }
    public void verify(LicenseCache licenseCache, LimeHttpClient httpClient) {}
    public long getLastVerifiedTime() { return 0; }
    public String getLicenseName() { return name; }
    
    public License copy(String license, URI licenseURI) {
        throw new UnsupportedOperationException("no copies allowed.");
    }    
}