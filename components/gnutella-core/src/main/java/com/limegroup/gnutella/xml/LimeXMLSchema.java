/*
 * LimeXMLSchema.java
 *
 * Created on April 12, 2001, 4:03 PM
 */

package com.limegroup.gnutella.xml;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.limewire.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Stores a XML schema, and provides access to various components
 * of schema.
 * @author asingla
 */
public class LimeXMLSchema {
    /**
     * List of fields (in canonicalized form to preserve the structural information)
     */
    private final List<SchemaFieldInfo> _canonicalizedFields;
    
    /**
     * The URI for this schema
     */
    private final String _schemaURI;
    
    /**
     * The description for this schema.
     */
    private final String _description;
    
    /**
     * The outer-XML name for this schema.
     * IE: 'things', for the 'thing' schema.
     */
    private final String _rootXMLName;

    private List<String> canonicalFieldNames;
    

    /** 
     * Creates new LimeXMLSchema .
     * @param schemaUrl the URL from where to read the schema definition
     * @exception IOException If the specified schemaFile doesn't exist, or isn't
     * a valid schema file
     */
    public LimeXMLSchema(URL schemaUrl) throws IOException {
        this(new InputSource(schemaUrl.openStream()));
    }
    
    /** 
     * Creates new LimeXMLSchema.
     * @param inputSource The source representing the XML schema definition
     * to be parsed
     * @exception IOException If the specified schemaFile doesn't exist, or isn't
     * a valid schema file
     */
    public LimeXMLSchema(InputSource inputSource) throws IOException {
        //initialize schema
        Document document = getDocument(inputSource);
        IOUtils.close(inputSource.getByteStream()); // make sure we closed the input source.
        _canonicalizedFields = Collections.unmodifiableList(new LimeXMLSchemaFieldExtractor().getFields(document));
        canonicalFieldNames = createCanonicalFieldNames(_canonicalizedFields);
        _schemaURI = retrieveSchemaURI(document);
        _rootXMLName = getRootXMLName(document);
        _description = getDisplayString(_schemaURI);
    }
    
    /**
     * Initializes the schema after parsing it from the input source
     * @param schemaInputSource The source representing the XML schema definition
     * to be parsed
     */
    private Document getDocument(InputSource schemaInputSource)
        throws IOException {
        //get an instance of DocumentBuilderFactory
        DocumentBuilderFactory documentBuilderFactory =
            DocumentBuilderFactory.newInstance();
        //set validating, and namespace awareness
        //documentBuilderFactory.setValidating(true);
        //documentBuilderFactory.setNamespaceAware(true);
            
        //get the document builder from factory    
        DocumentBuilder documentBuilder=null;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch(ParserConfigurationException e) {
            throw new IOException(e.getMessage());
        }
        // Set an entity resolver to resolve the schema
        documentBuilder.setEntityResolver(new Resolver(schemaInputSource));

        // Parse the schema and create a  document
        Document document=null;  
        try {
            document = documentBuilder.parse(schemaInputSource);
        } catch(SAXException e) {
            throw new IOException(e.getMessage());
        }

        return document;
    }
    
    /**
     * Returns the URI of the schema represented in the passed document.
     * @param document the document representing the XML Schema whose URI is to
     * be retrieved
     * @return the schema URI
     * @requires the document be a parsed form of valid xml schema
     */
    private static String retrieveSchemaURI(Document document) {
        //get the root element which should be "xsd:schema" element (provided
        //document represents valid schema)
        Element root = document.getDocumentElement();
        //get attributes
        NamedNodeMap  nnm = root.getAttributes();
        //get the targetNameSpaceAttribute
        Node targetNameSpaceAttribute = nnm.getNamedItem("targetNamespace");

        if(targetNameSpaceAttribute != null) {
            //return the specified target name space as schema URI
            return targetNameSpaceAttribute.getNodeValue();
        } else {
            //return an empty string otherwise
            return "";
        }
    }
    
    /**
     * Retrieves the name of the root tag name for XML generated
     * with this schema.
     */
    private static String getRootXMLName(Document document) {
        Element root = document.getDocumentElement();
        // Get the children elements.
        NodeList children = root.getElementsByTagName("element");
        if(children.getLength() == 0)
            return "";
        
        Node element = children.item(0);
        NamedNodeMap map = element.getAttributes();
        Node name = map.getNamedItem("name");
        if(name != null)
            return name.getNodeValue();
        else
            return "";
    }
    
    /**
     * Returns the unique identifier which identifies this particular schema.
     */
    public String getSchemaURI() {
        return _schemaURI;
    }
    
    /**
     * Retrieves the name to use when constructing XML docs under this schema.
     */
    public String getRootXMLName() {
        return _rootXMLName;
    }
    
    /**
     * Retrieves the name to use for inner elements when constructing docs under this schema.
     */
    public String getInnerXMLName() {
        return _description;
    }
    /**
     * Returns all the fields(placeholders) in this schema.
     * The field names are canonicalized as mentioned below:
     * <p>
     * So as to preserve the structure, Structure.Field will be represented as
     * Structure__Field (Double Underscore is being used as a delimiter to 
     * represent the structure).
     *<p>
     * In case of multiple structured values with same name, 
     * as might occur while using + or * in the regular expressions in schema,
     * those should be represented as using the array index using the __ 
     * notation (without the square brackets).
     * <p>
     * For e.g. myarray[0].name ==> myarray__0__name
     * <p>
     * Attribute names for an element in the XML schema should be postfixed 
     * with __ (double underscore).
     * <p>
     * So element.attribute ==> element__attribute__
     *
     * @return unmodifiable list (of SchemaFieldInfo) of all the fields 
     * in this schema.
     */
    public List<SchemaFieldInfo> getCanonicalizedFields() {
        return _canonicalizedFields;
    }
    
    
    /**
     * Returns only those fields which are of enumeration type.
     */
    public List<SchemaFieldInfo> getEnumerationFields() {
        //create a new list
        List<SchemaFieldInfo> enumerationFields = new LinkedList<SchemaFieldInfo>();
        
        //iterate over canonicalized fields, and add only those which are 
        //of enumerative type
        for(SchemaFieldInfo schemaFieldInfo : _canonicalizedFields) {
            //if enumerative type, add to the list of enumeration fields
            if(schemaFieldInfo.getEnumerationList() != null)
                enumerationFields.add(schemaFieldInfo);
        }
        
        //return the list of enumeration fields
        return enumerationFields;
    }
    
    
    private static List<String> createCanonicalFieldNames(List<SchemaFieldInfo> fields) {
        List<String> fieldNames = new ArrayList<String>(fields.size());
        for (SchemaFieldInfo field : fields) {
            fieldNames.add(field.getCanonicalizedFieldName());
        }
        return Collections.unmodifiableList(fieldNames);
    }
    
    /**
     * Returns all the fields(placeholders) names in this schema.
     * The field names are canonicalized as mentioned below:
     * <p>
     * So as to preserve the structure, Structure.Field will be represented as
     * Structure__Field (Double Underscore is being used as a delimiter to 
     * represent the structure).
     *<p>
     * In case of multiple structured values with same name, 
     * as might occur while using + or * in the regular expressions in schema,
     * those should be represented as using the array index using the __ 
     * notation (without the square brackets).
     * <p>
     * For e.g. myarray[0].name ==> myarray__0__name
     * <p>   
     * Attribute names for an element in the XML schema should be postfixed 
     * with __ (double underscore).
     * <p>
     * So element.attribute ==> element__attribute__
     *
     * @return list (Strings) of all the field names in this schema.
     */
    public List<String> getCanonicalizedFieldNames() {
        return canonicalFieldNames;
    }
    
    private static final class Resolver implements EntityResolver
    {
        private InputSource schema;
        
        public Resolver(InputSource s)
        {
            schema = s;
        }
        
        public InputSource resolveEntity(String publicId, String systemId)
        {
            return schema;
            
            //String Id = systemId+publicId;
            //String schemaId = schema.getSystemId()+schema.getPublicId();
            //if (Id.equals(schemaId))
            //    return schema;
            //else
            //    return null;
        }
    }//end of private innner class
    
    /**
     * Returns the display name of this schema.
     */
    public String getDescription() {
        return _description;
    }

    /**
     * Utility method to be used in the GUI to display schemas.
     */
    public static String getDisplayString(String schemaURI)
    {
        int start = schemaURI.lastIndexOf("/");
        //TODO3: Are we sure that / is the correct delimiter???
        int end = schemaURI.lastIndexOf(".");
        String schemaStr;
        if(start == -1 || end == -1)
            schemaStr = schemaURI;
        else
            schemaStr= schemaURI.substring(start+1,end);
        return schemaStr;
    }
    
    @Override
    public boolean equals(Object o) {
        if( o == this )
            return true;
        if( o == null )
            return false;
        return _schemaURI.equals(((LimeXMLSchema)o)._schemaURI);
    }
    
    @Override
    public int hashCode() {
        return _schemaURI.hashCode();
    }
    
    @Override
    public String toString() {
        return "LimeXMLSchema[description=" + _description + ", rootXMLName=" + _rootXMLName + ", schemaURI=" + _schemaURI + "]";
    }
}
