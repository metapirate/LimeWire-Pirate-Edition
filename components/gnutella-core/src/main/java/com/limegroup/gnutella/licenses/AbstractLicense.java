package com.limegroup.gnutella.licenses;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.limegroup.gnutella.util.LimeWireUtils;


/**
 * A base license class, implementing common functionality.
 */
public abstract class AbstractLicense implements MutableLicense, Serializable, Cloneable {
    
    private static final Log LOG = LogFactory.getLog(AbstractLicense.class);
    
    private static final long serialVersionUID = 6508972367931096578L;
    
    /** Whether or not this license has been verified. */
    protected transient int verified = UNVERIFIED;
    
    /** The URI where verification will be performed. */
    protected transient URI licenseLocation;
    
    /** The license name. */
    private transient String licenseName;
    
    /** The last time this license was verified. */
    private long lastVerifiedTime;

    /** Constructs a new AbstractLicense. */
    AbstractLicense(URI uri) {
        this.licenseLocation = uri;
    }
    
    public void setLicenseName(String name) { this.licenseName = name; }
    
    public boolean isVerifying() { return verified == VERIFYING; }
    public boolean isVerified() { return verified == VERIFIED; }
    public String getLicenseName() { return licenseName; }
    public URI getLicenseURI() { return licenseLocation; }
    public long getLastVerifiedTime() { return lastVerifiedTime; }
    
    void setVerified(int verified) {
        this.verified = verified;
    }
    
    void setLastVerifiedTime(long lastVerifiedTime) {
        this.lastVerifiedTime = lastVerifiedTime;
    }
    
    /**
     * Assume that all serialized licenses were verified.
     * (Otherwise they wouldn't have been serialized.
     */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        verified = VERIFIED;
    }
    
    /**
     * Clears all internal state that could be set while verifying.
     */
    protected abstract void clear();
    
    /**
     * Retrieves the body of a URL from a webserver.
     *
     * Returns null if the page could not be found.
     */
    protected String getBody(String url, LimeHttpClient httpClient) {
        return getBodyFromURL(url, httpClient);
    }
    
    /**
     * Contacts the given URL and downloads returns the body of the
     * HTTP request.
     */
    protected String getBodyFromURL(String url, LimeHttpClient httpClient) {
        if (LOG.isTraceEnabled())
            LOG.trace("Contacting: " + url);
        HttpResponse response = null;
        try {
            HttpGet get = new HttpGet(url);
            get.addHeader("User-Agent", LimeWireUtils.getHttpServer());
            response = httpClient.execute(get);
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            return result;
        } catch (IOException e) {
            LOG.warn("Can't contact license server: " + url, e);
        } finally {
            httpClient.releaseConnection(response);
        }
        return null;
    }
    
    /**
	 * Parses the document node of the XML. 
	 */
    protected abstract void parseDocumentNode(Node node, LicenseCache licenseCache, LimeHttpClient httpClient);
    
    /**
     * Attempts to parse the given XML.
     * The actual handling of the XML is sent to parseDocumentNode,
     * which subclasses can implement as they see fit.
     *
     * If this is a request directly from our Verifier, 'liveData' is true.
     * Subclasses may use this to know where the XML data is coming from.
     */
    protected void parseXML(String xml, LicenseCache licenseCache, LimeHttpClient httpClient) {
        if(xml == null)
            return;
        
        if(LOG.isTraceEnabled())
            LOG.trace("Attempting to parse: " + xml);

        // TODO propagate exceptions and handle in LicenseVerifier
        Document d;
        try {
        	DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        	InputSource is = new InputSource(new StringReader(xml));
            d = parser.parse(is);
        } catch (IOException ioe) {
            LOG.debug("IOX parsing XML\n" + xml, ioe);
            return;
        } catch (SAXException saxe) {
            LOG.debug("SAX parsing XML\n" + xml, saxe);
            return;
        } catch (ParserConfigurationException bad) {
        	LOG.debug("couldn't instantiate parser", bad);
        	return;
        }
        
        parseDocumentNode(d.getDocumentElement(), licenseCache, httpClient);
    }

    public void verify(LicenseCache licenseCache, LimeHttpClient httpClient) {
        setVerified(AbstractLicense.VERIFYING);
        clear();

        String body = getBody(getLicenseURI().toString(), httpClient);
        parseXML(body, licenseCache, httpClient);
        setLastVerifiedTime(System.currentTimeMillis());
        setVerified(AbstractLicense.VERIFIED);
        
        licenseCache.addVerifiedLicense(this);
    }
    
}