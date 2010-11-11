package com.limegroup.gnutella.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.Response;

@Singleton
public final class LimeXMLDocumentHelper{

    private static final Log LOG = LogFactory.getLog(LimeXMLDocumentHelper.class);
    
    public static final String XML_HEADER = "<?xml version=\"1.0\"?>";
    public static final String XML_NAMESPACE =
        "xsi:noNamespaceSchemaLocation=\"";

    private final LimeXMLDocumentFactory limeXMLDocumentFactory;

    /**
     * Private constructor to ensure that this class can never be instantiated.
     */
    @Inject
    public LimeXMLDocumentHelper(LimeXMLDocumentFactory limeXMLDocumentFactory) {
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
    }

    /**
     * To be used when a Query Reply comes with a chunk of meta-data
     * we want to get LimeXMLDocuments out of it.
     */
    public List<LimeXMLDocument[]> getDocuments(String aggregatedXML, int totalResponseCount) {
        if(aggregatedXML==null || aggregatedXML.equals("") || totalResponseCount <= 0)
            return Collections.emptyList();
        
        List<LimeXMLDocument[]> results = new ArrayList<LimeXMLDocument[]>();
        
        for(String xmlDocument : XMLParsingUtils.split(aggregatedXML)) {
            XMLParsingUtils.ParseResult parsingResult;
            try {
                parsingResult = XMLParsingUtils.parse(xmlDocument,totalResponseCount);
            } catch (SAXException sax) {
                LOG.warn("SAX while parsing: " + xmlDocument, sax);
                continue;// bad xml, ignore
            } catch (IOException bad) {
                LOG.warn("IOX while parsing: " + aggregatedXML, bad);
                return Collections.emptyList(); // abort
            }
            
            final String indexKey = parsingResult.canonicalKeyPrefix +
                                    LimeXMLDocumentFactoryImpl.XML_INDEX_ATTRIBUTE;
            LimeXMLDocument[] documents = new LimeXMLDocument[totalResponseCount];
            for(Map<String, String> attributes : parsingResult) {
                String sindex = attributes.remove(indexKey);
                if (sindex == null)
                    return Collections.emptyList();
                
                int index = -1;
                try {
                    index = Integer.parseInt(sindex);
                } catch(NumberFormatException bad) { //invalid document
                    LOG.warn("NFE while parsing", bad);
                    return Collections.emptyList();
                }
                
                if (index >= documents.length || index < 0)
                    return Collections.emptyList(); // malicious document, can't trust it.
                
                if(!attributes.isEmpty()) {
                    try {
                        documents[index] = limeXMLDocumentFactory.createLimeXMLDocument(
                                attributes, parsingResult.schemaURI, parsingResult.canonicalKeyPrefix);
                    } catch(IOException ignored) {
                        LOG.debug("",ignored);
                    }
                }
            }
            results.add(documents);
        }
        return results;
    }
    
    /**
     * Builds an XML string out of all the responses.
     * If no responses have XML, an empty string is returned.
     */
    public static String getAggregateString(Response... responses) {
        Map<LimeXMLSchema, StringBuilder> allXML = new HashMap<LimeXMLSchema, StringBuilder>();
        for(int i = 0; i < responses.length; i++) {
            LimeXMLDocument doc = responses[i].getDocument();
            if(doc != null) {
                LimeXMLSchema schema = doc.getSchema();
                StringBuilder built = allXML.get(schema);
                if(built == null) {
                    built = new StringBuilder();
                    allXML.put(schema, built);
                }
                built.append(doc.getAttributeStringWithIndex(i));
            }
        }
     
        // Iterate through each schema and build a string containing
        // a bunch of XML docs, each beginning with XML_HEADER.   
        StringBuilder fullXML = new StringBuilder();
        for(Map.Entry<LimeXMLSchema, StringBuilder> entry : allXML.entrySet())
            buildXML(fullXML, entry.getKey(), entry.getValue().toString());
        
        return fullXML.toString();
    }
    
    /**
     * Wraps the inner element around the root tags, with the correct
     * XML headers.
     */
    public static void buildXML(StringBuilder buffer, LimeXMLSchema schema, String inner) {
        buffer.append(XML_HEADER);
        buffer.append("<");
        buffer.append(schema.getRootXMLName());
        buffer.append(" ");
        buffer.append(XML_NAMESPACE);
        buffer.append(schema.getSchemaURI());
        buffer.append("\">");
        buffer.append(inner);
        buffer.append("</");
        buffer.append(schema.getRootXMLName());
        buffer.append(">");
    }
}
