package org.limewire.util;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Provides utilities to retrieve XML documents.
 */
public class XMLUtils {

    private static Log LOG = LogFactory.getLog(XMLUtils.class);
    
    private XMLUtils() {}
    
    /**
     * Returns a newly created document from the given XML.
     * Any errors while parsing will be printed to the XMLUtils.class log. 
     */
    public static Document getDocument(String xml) throws IOException {
        return getDocument(xml, null, new LogErrorHandler(LOG));
    }
    
    /**
     * Returns a newly created document from the given XML.
     * Any errors while parsing will be printed to the given log.
     */
    public static Document getDocument(String xml, Log log) throws IOException {
        return getDocument(xml, null, new LogErrorHandler(log));
    }

    /**
     * Returns a newly created document from the given XML.
     * Any errors while parsing will be printed to the errorHandler.
     */
    public static Document getDocument(String xml, ErrorHandler errorHandler) throws IOException {
        return getDocument(xml, null, errorHandler);
    }
    
    /**
     * Returns a newly created document from the given XML.
     * Any errors while parsing will be printed to the errorHandler.
     * The given EntityResolver will be used if it is non-null.
     */
    public static Document getDocument(String xml, EntityResolver resolver, 
                                       ErrorHandler errorHandler) throws IOException {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            if(resolver != null)
                builder.setEntityResolver(resolver);
            builder.setErrorHandler(errorHandler);
            return builder.parse(is);
        } catch(IOException ioe) {
            if(LOG.isErrorEnabled())
                LOG.error("Unable to parse: " + xml, ioe);
            throw ioe;
        } catch(SAXException sax) {
            if(LOG.isErrorEnabled())
                LOG.error("Unable to parse: " + xml, sax);
            throw (IOException)new IOException().initCause(sax);
        } catch (ParserConfigurationException bad) {
            LOG.error("Unable to create parser", bad);
            throw (IOException)new IOException().initCause(bad);
        }
    }
    
   /**
     * Provides a default implementation of <code>ErrorHandler</code>.
     * <code>LogErrorHandler</code> prints warnings, errors and fatal
     * errors to a log.
     */
    public static class LogErrorHandler implements ErrorHandler {
        private final Log log;
        
        public LogErrorHandler(Log log) {
            this.log = log;
        }
        
        public void error(SAXParseException exception) throws SAXException {
            log.error("Parse error", exception);
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            log.error("Parse fatal error", exception);
            throw exception;
        }

        public void warning(SAXParseException exception) throws SAXException {
            log.error("Parse warning", exception);
        }
    }
}
