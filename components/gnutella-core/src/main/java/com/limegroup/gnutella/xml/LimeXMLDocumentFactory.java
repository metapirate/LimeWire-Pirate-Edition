package com.limegroup.gnutella.xml;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.xml.sax.SAXException;

public interface LimeXMLDocumentFactory {

    public LimeXMLDocument createLimeXMLDocument(String xml)
            throws SAXException, SchemaNotFoundException, IOException;

    public LimeXMLDocument createLimeXMLDocument(Map<String, String> map,
            String schemaURI, String keyPrefix) throws IOException;

    public LimeXMLDocument createLimeXMLDocument(
            Collection<? extends Entry<String, String>> nameValueList,
            String schemaURI);

}