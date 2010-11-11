package com.limegroup.gnutella.licenses;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URI;
import java.net.URL;

import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.service.ErrorService;
import org.limewire.util.URIUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * A concrete implementation of a License, for Weed licenses.
 */
class WeedLicense extends AbstractLicense {
    
    //private static final Log LOG = LogFactory.getLog(WeedLicense.class);
    
    private static final long serialVersionUID = 1230497157539025753L;
    
    /** The site to contact for verification (non-final for testing). */
    private static       String URI = "http://www.weedshare.com/license/verify_usage_rights.aspx";
    /** The versionid attribute. */
    private static final String VID = "versionid";
    /** The contentid attribute. */
    private static final String CID = "contentid";
    
    /** The artist. */
    private String artist;
    
    /** The title. */
    private String title;
    
    /** The price. */
    private String price;
    
    /** Whether or not the license is valid. */
    private boolean valid;
    
    /** Builds the URI from the given cid & vid. */
    public static URI buildURI(String cid, String vid) {
        try {
            return URIUtils.toURI((URI + "?" + VID + "=" + vid + "&" + CID + "=" + cid));
        } catch(URISyntaxException bad) {
            return null;
        }  
    }
    
    /**
     * Constructs a new WeedLicense.
     */
    WeedLicense(URI uri) {
        super(uri);
    }
    
    /** There is no explicit license text for Weed files. */
    public String getLicense() { return null; }
    
    /**
     * Retrieves the license deed for the given URN.
     */
    public URL getLicenseDeed(URN urn) {
        try {
            return new URL("http://weedshare.com/company/policies/summary_usage_rights.aspx");
        } catch(MalformedURLException murl) {
            return null;
        }
    }
        
    /**
     * Determines if the Weed License is valid.
     */
    public boolean isValid(URN urn) {
        return valid;
    }
    
    /**
     * Returns a new WeedLicense with a different URI.
     */
    public License copy(String license, URI licenseURI) {
        WeedLicense newL = null;
        try {
            newL = (WeedLicense)clone();
            newL.licenseLocation = licenseURI;
        } catch(CloneNotSupportedException error) {
            ErrorService.error(error);
        }
        return newL;
    }
    
    /**
     * Builds a description of this license based on what is permitted,
     * probibited, and required.
     */
    public String getLicenseDescription(URN urn) {
        if(artist == null && title == null && price == null) {
            return "Details unknown.";
        } else {
            StringBuilder sb = new StringBuilder();
            if(artist != null)
                sb.append("Artist: " + artist + "\n");
            if(title != null)
                sb.append("Title: " + title + "\n");
            if(price != null)
                sb.append("Price: " + price);
            return sb.toString();
        }
    }

    /** Clears prior validation information. */    
    @Override
    protected void clear() {
        valid = false;
        artist = null;
        title = null;
        price = null;
    }

    /**
     * Overriden to retrieve the body of data from a special URI.
     */
    @Override
    protected String getBody(String url, LimeHttpClient httpClient) {
        return super.getBody(url + "&data=1", httpClient);
    }
        
    /**
     * Parses the XML sent back from the Weed server.
     * The XML should look like:
     *  <WeedVerifyData>
	 *       <Status>Verified</Status>
	 *       <Artist>Roger Joseph Manning, Jr.</Artist>
	 *       <Title>What You Don't Know About the Girl</Title>
	 *       <Price>1.2500</Price>
     *  </WeedVerifyData>
     */
    @Override
    protected void parseDocumentNode(Node doc, LicenseCache licenseCache, LimeHttpClient httpClient) {
        if(!doc.getNodeName().equals("WeedVerifyData"))
            return;
        
        NodeList children = doc.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            String value = LimeXMLUtils.getTextContent(child);
            if(name == null || value == null)
                continue;

            value = value.trim();
            if(value.equals(""))
                continue;
                
            if(name.equals("Status"))
                valid = value.equals("Verified");
            else if(name.equals("Artist"))
                artist = value;
            else if(name.equals("Title"))
                title = value;
            else if(name.equals("Price"))
                price = value;
        }
    }
}
