package com.limegroup.gnutella.licenses;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.URIUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.metadata.audio.reader.WRMXML;
import com.limegroup.gnutella.metadata.audio.reader.WeedInfo;


/**
 * A factory for constructing Licenses based on licenses.
 */
@Singleton
public final class LicenseFactoryImpl implements LicenseFactory {
    
    private static final Log LOG = LogFactory.getLog(LicenseFactoryImpl.class);
    
    private final Provider<LicenseCache> licenseCache;
    
    @Inject
    public LicenseFactoryImpl(Provider<LicenseCache> licenseCache) {
        this.licenseCache = licenseCache;
    }
    
    public boolean isVerifiedAndValid(URN urn, String licenseString) {
        URI uri = getLicenseURI(licenseString);
        return uri != null && licenseCache.get().isVerifiedAndValid(urn, uri);
    }
    
    public String getLicenseName(String licenseString) {
        if(isCCLicense(licenseString))
            return CC_NAME;
        else if(isWeedLicense(licenseString))
            return WEED_NAME;
        else if(isUnknownLicense(licenseString))
            return UNKNOWN_NAME;
        else
            return null;
    }
    
    public License create(String licenseString) {
        if(licenseString == null)
            return null;
        
        if(LOG.isTraceEnabled())
            LOG.trace("Attempting to create license from: " + licenseString);
        
        License license = null;
        URI uri = getLicenseURI(licenseString);
        
        // Try to get a cached version, first.
        if(uri != null)
            license = licenseCache.get().getLicense(licenseString, uri);
        
        // If the cached version didn't exist, try to make one.
        if(license == null) {
            if(isCCLicense(licenseString)) {
                if(uri != null)
                    license = new CCLicense(licenseString, uri);
                else
                    license = new BadCCLicense(licenseString);
            } else if(isWeedLicense(licenseString) && uri != null) {
                license = new WeedLicense(uri);
            } else if(isUnknownLicense(licenseString)) {
                license = new UnknownLicense();
            }
        }
        
        // set additional properties
        if (license instanceof MutableLicense) {
            ((MutableLicense)license).setLicenseName(getLicenseName(licenseString));
        }
        
        return license;
    }
    
    /** Determines if the given string can be a CC license. */
    private static boolean isCCLicense(String s) {
        return s.toLowerCase(Locale.US).indexOf(CCConstants.URL_INDICATOR) != -1;
    }
    
    /** Determines if the given string can be a Weed license. */
    private static boolean isWeedLicense(String s) {
        return s.startsWith(WeedInfo.LAINFO);
    }
    
    /** Determines if the given string can be an Unknown license. */
    private static boolean isUnknownLicense(String s) {
        return s.startsWith(WRMXML.PROTECTED);
    }
    
    /**
     * Persists the cache.
     */
    public void persistCache() {
        licenseCache.get().persistCache();
    }
    
    /**
     * Determines the URI to verify this license at from the license string.
     */
    static URI getLicenseURI(String license) {
        if(license == null)
            return null;
            
        // Look for CC first.
        URI uri = getCCLicenseURI(license);
        
        // Then Weed.
        if(uri == null)
            uri = getWeedLicenseURI(license);
            
        // ADD MORE LICENSES IN THE FORM OF
        // if( uri == null)
        //      uri = getXXXLicenseURI(license)
        // AS WE UNDERSTAND MORE...
        
        return uri;
    }
        
    /** Gets a CC license URI from the given license string. */
    private static URI getCCLicenseURI(String license) {
        license = license.toLowerCase(Locale.US);
        
        // find where the URL should begin.
        int verifyAt = license.indexOf(CCConstants.URL_INDICATOR);
        if(verifyAt == -1)
            return null;
            
        int urlStart = verifyAt + CCConstants.URL_INDICATOR.length();
        if(urlStart >= license.length())
            return null;
            
        String url = license.substring(urlStart).trim();
        url = url.split(" ")[0];
        
        URI uri = null;
        try {
            uri = URIUtils.toURI(url);

            // Make sure the scheme is HTTP.
            String scheme = uri.getScheme();
            if(scheme == null || !scheme.equalsIgnoreCase("http"))
                throw new URISyntaxException(uri.toString(), "Invalid scheme: " + scheme);
            // Make sure the scheme has some authority.
            String authority = uri.getAuthority();
            if(authority == null || authority.equals("") || authority.indexOf(' ') != -1)
                throw new URISyntaxException(uri.toString(), "Invalid authority: " + authority);
            
        } catch(URISyntaxException e) {
            //URIUtils.error(e);
            uri = null;
            LOG.error("Unable to create URI", e);
        }
        
        return uri;
    }
    
    /** Gets a Weed license URI from the given license string. */
    private static URI getWeedLicenseURI(String license) {
        int lainfo = license.indexOf(WeedInfo.LAINFO);
        if(lainfo == -1)
            return null;
            
        int cidx = license.indexOf(WeedInfo.CID);
        int vidx = license.indexOf(WeedInfo.VID);
        
        // If no cid or vid, exit.
        if(cidx == -1 || vidx == -1) {
            LOG.debug("No cid or vid, bailing.");
            return null;
        }
            
        cidx += WeedInfo.CID.length();;
        vidx += WeedInfo.VID.length();;
            
        int cend = license.indexOf(" ", cidx);
        int vend = license.indexOf(" ", vidx);
        // If there's no ending space for BOTH, exit.
        // (it's okay if one is at the end, but both can't be)
        if(cend == -1 && vend == -1) {
            LOG.debug("No endings for both cid & vid, bailing");
            return null;
        }
        if(cend == -1)
            cend = license.length();
        if(vend == -1)
            vend = license.length();
        
        // If the cid or vid are empty, exit.
        String cid = license.substring(cidx, cend).trim();
        String vid = license.substring(vidx, vend).trim();
        if(cid.length() == 0 || vid.length() == 0) {
            LOG.debug("cid or vid is empty, bailing");
            return null;
        }
        
        if(cid.startsWith(WeedInfo.VID.trim()) || vid.startsWith(WeedInfo.CID.trim())) {
            LOG.debug("cid starts with vid, or vice versa, bailing.");
            return null;
        }
        
        return WeedLicense.buildURI(cid, vid);
    }
}
  