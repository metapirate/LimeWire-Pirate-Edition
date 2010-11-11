package com.limegroup.gnutella.metadata.audio.reader;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * An encapsulation of the XML that describes Windows Media's
 * extended content encryption object.
 * <p>
 * Construction will always succeed, but the object may be invalid.
 * Consult WRMXML.isValid() to see if the given XML was valid.
 */
public class WRMXML {
    
    private static final Log LOG = LogFactory.getLog(WRMXML.class);
    
    public static final String PROTECTED = "licensed: ";
    
    // The XML should look something like:
    //<WRMHEADER>
    //    <DATA>
    //        <SECURITYVERSION>XXXX</SECURITYVERSION>
    //        <CID>XXXX</CID>
    //        <LAINFO>XXXX</LAINFO>
    //        <KID>XXXX</KID>
    //        <CHECKSUM>XXXX</CHECKSUM>
    //    </DATA>
    //    <SIGNATURE>
    //        <HASHALGORITHM type="XXXX"></HASHALGORITHM>
    //        <SIGNALGORITHM type="XXXX"></SIGNALGORITHM>
    //        <VALUE>XXXX</VALUE>
    //    </SIGNATURE>
    //</WRMHEADER> 
    
    protected String _securityversion, _cid, _lainfo, _kid, _checksum;
    protected String _hashalgorithm, _signalgorithm, _signatureValue;
    protected Node _documentNode;
    
    /**
     * Parses the given XML & constructs a WRMXML object out of it.
     */
    public WRMXML(String xml) {
        parse(xml);
    }
    
    /**
     * Constructs a WRMXML object out of the given document.
     */
    WRMXML(Node documentNode) {
        parseDocument(documentNode);
    }
    
    /**
     * Determines is this WRMXML is well formed.
     * If it is not, no other methods are considered valid.
     */
    public boolean isValid() {
        return _documentNode != null &&
               _lainfo != null &&
               _hashalgorithm != null &&
               _signalgorithm != null &&
               _signatureValue != null;
    }
    
    public String getSecurityVersion() { return _securityversion; }
    public String getCID() { return _cid; }
    public String getLAInfo() { return _lainfo; }
    public String getKID() { return _kid; }
    public String getHashAlgorithm() { return _hashalgorithm; }
    public String getSignAlgorithm() { return _signalgorithm; }
    public String getSignatureValue() { return _signatureValue; }
    public String getChecksum() { return _checksum; }
    
    
    /** Parses the content encryption XML. */
    protected void parse(String xml) {
    	Document d;
        try {
        	d = XMLUtils.getDocument(xml, LOG);
        } catch (IOException ioe) {
            return;
        }
        
        parseDocument(d.getDocumentElement());
    }
    
    /**
     * Parses through the given document node, handing each child
     * node to parseNode.
     */
    protected void parseDocument(Node node) {
        _documentNode = node;
        if(!_documentNode.getNodeName().equals("WRMHEADER"))
            return;
        
        NodeList children = _documentNode.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            parseNode(child.getNodeName(), child);
        }
    }
    
    /**
     * Parses a node.
     * 'nodeName' is the parent node's name.
     * All child elements of this node are sent to parseChild, and all
     * attributes are parsed via parseAttributes.
     */
    protected void parseNode(String nodeName, Node data) {
        NodeList children = data.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            parseAttributes(nodeName, child);
                        
            String name = child.getNodeName();            
            String value = LimeXMLUtils.getTextContent(child);
            if(value == null)
                continue;
            value = value.trim();
            if(value.equals(""))
                continue;
                
            parseChild(nodeName, name, null, value);
        }
    }
    
    /**
     * Parses the attributes of a given node.
     * 'parentNodeName' is the parent node of this child, and child is the node
     * which the attributes are part of.
     * Attributes are sent to parseChild for parsing.
     */
    protected void parseAttributes(String parentNodeName, Node child) {
        NamedNodeMap nnm = child.getAttributes();
        String name = child.getNodeName();
        for(int i = 0; i < nnm.getLength(); i++) {
            Node attribute = nnm.item(i);
            String attrName = attribute.getNodeName();
            String attrValue = attribute.getNodeValue();
            if(attrValue == null)
                continue;
            attrValue = attrValue.trim();
            if(attrValue.equals(""))
                continue;
            parseChild(parentNodeName, name, attrName, attrValue);
        }
    }
    
    
    /**
     * Parses a child of the data node.
     * @param nodeName the parent node's name
     * @param name the name of this node
     * @param attribute the attribute's name, or null if not an attribute.
     * @param value the value of the node's text content (or the attribute)
     */
    protected void parseChild(String nodeName, String name, String attribute, String value) {
        if(nodeName.equals("DATA")) {
            if(attribute != null)
                return;            
            if(name.equals("SECURITYVERSION"))
                _securityversion = value;
            else if(name.equals("CID"))
                _cid = value;
            else if(name.equals("LAINFO"))
                _lainfo = value;
            else if(name.equals("KID"))
                _kid = value;
            else if(name.equals("CHECKSUM"))
                _checksum = value;
        } else if(nodeName.equals("SIGNATURE")) {
            if(name.equals("HASHALGORITHM") && "type".equals(attribute))
                _hashalgorithm = value;
            else if(name.equals("SIGNALGORITHM") && "type".equals(attribute))
                _signalgorithm = value;
            else if(name.equals("VALUE") && attribute == null)
                _signatureValue = value;
        }
    }
}