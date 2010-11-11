package com.limegroup.gnutella.licenses;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A collection of constants & utilities for Creative Commons licenses.
 */
public final class CCConstants {
    
    private static final Log LOG = LogFactory.getLog(CCLicense.class);    
    
    /** 
     * The string that is inserted into QRP & goes out in license queries
     * when searching for Creative Commons licenses.
     * <p>
     * THIS CAN NEVER EVER CHANGE.
     * (And, if you really do change it for some reason, make sure
     *  that you update the value in the various .xsd files.)
     */
    public static final String CC_URI_PREFIX = "creativecommons.org/licenses/";
    
    /**
     * The string that indicates all subsequent information is the URL where the
     * CC license is stored.
     */
    public static final String URL_INDICATOR = "verify at";
    
    /** The header to include in RDF documents. */
    public static final String CC_RDF_HEADER = "<!-- <rdf:RDF xmlns=\"http://web.resource.org/cc/\"" +
            " xmlns:dc=\"http://purl.org/dc/elements/1.1/\"" +
            " xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">";
    
    /** The footer of the RDF block. */
    public static final String CC_RDF_FOOTER = "</rdf:RDF> -->";
    
    /** various types of licenses and combinations of permitted/prohibited uses. */
    public static final int ATTRIBUTION = 0;
    public static final int ATTRIBUTION_NO_DERIVS = 0x1;
    public static final int ATTRIBUTION_NON_COMMERCIAL = 0x2;
    public static final int ATTRIBUTION_NON_COMMERCIAL_NO_DERIVS = ATTRIBUTION_NON_COMMERCIAL | ATTRIBUTION_NO_DERIVS;
    public static final int ATTRIBUTION_SHARE = 0x4;
    public static final int ATTRIBUTION_SHARE_NON_COMMERCIAL = ATTRIBUTION_SHARE | ATTRIBUTION_NON_COMMERCIAL;
    
    /** URI's for each type of license. */
    public static final String ATTRIBUTION_URI = "http://creativecommons.org/licenses/by/2.5/";
    public static final String ATTRIBUTION_NO_DERIVS_URI = "http://creativecommons.org/licenses/by-nd/2.5/";
    public static final String ATTRIBUTION_NON_COMMERCIAL_URI = "http://creativecommons.org/licenses/by-nc/2.5/";
    public static final String ATTRIBUTION_NON_COMMERCIAL_NO_DERIVS_URI = "http://creativecommons.org/licenses/by-nc-nd/2.5/";
    public static final String ATTRIBUTION_SHARE_URI = "http://creativecommons.org/licenses/by-sa/2.5/";
    public static final String ATTRIBUTION_SHARE_NON_COMMERCIAL_URI = "http://creativecommons.org/licenses/by-nc-sa/2.5/";
    
    private static final Map<Integer, String> LICENSE_URI_MAP;
    static {
        LICENSE_URI_MAP = new HashMap<Integer, String>();
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION),ATTRIBUTION_URI);
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION_NO_DERIVS),ATTRIBUTION_NO_DERIVS_URI);
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION_NON_COMMERCIAL),ATTRIBUTION_NON_COMMERCIAL_URI);
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION_NON_COMMERCIAL_NO_DERIVS),ATTRIBUTION_NON_COMMERCIAL_NO_DERIVS_URI);
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION_SHARE),ATTRIBUTION_SHARE_URI);
        LICENSE_URI_MAP.put(new Integer(ATTRIBUTION_SHARE_NON_COMMERCIAL),ATTRIBUTION_SHARE_NON_COMMERCIAL_URI);
    }
    
    public static String getLicenseURI(int licenseType) {
        return LICENSE_URI_MAP.get(new Integer(licenseType));
    }
    
    public static String getLicenseElement(int licenseType) {
        Integer licenseTypeI = new Integer(licenseType);
        assert(LICENSE_URI_MAP.containsKey(licenseTypeI));
        
        StringBuilder ret = new StringBuilder();
        // header - the description of the license
        ret.append("<License rdf:about=\"").append(LICENSE_URI_MAP.get(licenseTypeI)).append("\">");
        
        // all licenses require attribution and permit reproduction and distribution
        ret.append("<requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />");
        ret.append("<permits rdf:resource=\"http://web.resource.org/cc/Reproduction\" />");
        ret.append("<permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />");
        
        // are derivative works allowed?
        if ((licenseType & ATTRIBUTION_NO_DERIVS) == 0)
            ret.append("<permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />");
        
        // is commercial use prohibited?
        if ((licenseType & ATTRIBUTION_NON_COMMERCIAL) != 0)
            ret.append("<prohibits rdf:resource=\"http://web.resource.org/cc/CommercialUse\" />");
        
        // is share-alike required?
        if ((licenseType & ATTRIBUTION_SHARE) != 0)
            ret.append("<requires rdf:resource=\"http://web.resource.org/cc/ShareAlike\" />");
        
        // all license require a notice
        ret.append("<requires rdf:resource=\"http://web.resource.org/cc/Notice\" />");
        ret.append("</License>");
        return ret.toString();
    }
    
    /**
     * Guesses a license deed URL from a license string.
     */
    static URL guessLicenseDeed(String license) {
        if(license == null)
            return null;
        
        // find where "creativecommons.org/licenses/" is.
        int idx = license.indexOf(CCConstants.CC_URI_PREFIX);
        if(idx == -1)
            return null;
        // find the "http://" before it.
        int httpIdx = license.lastIndexOf("http://", idx);
        if(httpIdx == -1)
            return null;
        // make sure that there's a space before it or it's the start.
        if(httpIdx != 0 && license.charAt(httpIdx-1) != ' ')
            return null;

        // find where the first space is after the http://.
        // if it's before the creativecommons.org part, that's bad.
        int spaceIdx = license.indexOf(" ", httpIdx);
        if(spaceIdx == -1)
            spaceIdx = license.length();
        else if(spaceIdx < idx)
            return null;
     
        try {       
            return new URL(license.substring(httpIdx, spaceIdx));
        } catch(MalformedURLException bad) {
            LOG.warn("Unable to create URL from license: " + license, bad);
            return null;
        }
    }
}
