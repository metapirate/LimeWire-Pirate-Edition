package com.limegroup.gnutella.xml;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.limewire.util.NameValue;
import org.limewire.util.RPNParser.StringLookup;

public interface LimeXMLDocument extends StringLookup {
    
    public static final List<LimeXMLDocument> EMPTY_LIST = Collections.emptyList();

    /**
     * Returns the number of fields this document has.
     */
    int getNumFields();

    /**
     * Returns all the non-numeric fields in this.  These are
     * not necessarily QRP keywords.  For example, one of the
     * elements of the returned list may be "Some comment-blah".
     * QRP code may want to split this into the QRP keywords
     * "Some", "comment", and "blah".
     * <p>
     * Indivisible keywords are not returned.  To retrieve those,
     * use getIndivisibleKeywords().  Indivisible keywords are
     * those which QRP will not split up.
     */
    List<String> getKeyWords();

    /**
     * Returns all the indivisible keywords for entry into QRP tables.
     */
    List<String> getKeyWordsIndivisible();

    /**
     * Returns the unique identifier which identifies the schema this XML
     * document conforms to.
     */
    String getSchemaURI();

    /**
     * Returns the LimeXMLSchema associated with this XML document.
     */
    LimeXMLSchema getSchema();

    /**
     * Returns the name of the file that the data in this XML document 
     * corresponds to. If the meta-data does not correspond to any file
     * in the file system, this method will return a null.
     */
    File getIdentifier();

    /**
     * Sets the identifier.
     */
    void initIdentifier(File id);

    /**
     * Returns the action corresponding with this LimeXMLDocument.
     */
    String getAction();

    /**
     * Returns a Set of Map.Entry, where each key-value corresponds to a
     * Canonicalized field name (placeholder), and its corresponding value in
     * the XML Document.
     * <p>
     * Canonicalization:
     * <p>
     * So as to preserve the structure, Structure.Field will be represented as
     * Structure__Field (Double Underscore is being used as a delimiter to
     * represent the structure).
     *<p>
     * In case of multiple structured values with same name,
     * as might occur while using + or * in the regular expressions in schema,
     * those should be represented as using the array index using the __
     * notation (without the square brackets)
     * <p>
     * For e.g. myarray[0].name ==> myarray__0__name
     * <p>
     * Attribute names for an element in the XML schema should be postfixed 
     * with __ (double underscore).
     * <p>
     * So element.attribute ==> element__attribute__
     *
     * @return a Set of Map.Entry, where each key-value corresponds to a
     * canonicalized field name (placeholder), and its corresponding value in
     * the XML Document.
     */
    Collection<Map.Entry<String, String>> getNameValueSet();

    /**
     * Determines if a license exists that this LimeXMLDocument knows about.
     */
    boolean isLicenseAvailable();

    /**
     * Returns a string that can be used to verify if this license is valid.
     */
    String getLicenseString();

    /**
     * Returns a list of attributes and their values in the same order
     * as is in the schema.
     */
    List<NameValue<String>> getOrderedNameValueList();

    /**
     * Returns the value associated with this canonicalized fieldname.
     */
    String getValue(String fieldName);

    /**
     * Constructs an XML string from this document.
     */
    String getXMLString();

    /**
     * Returns the attribute string with the given index.
     * <p>
     * For example, this will return:
     *   <thing att1="value1" att2="value2" att3="value3" index="4"/>
     */
    String getAttributeStringWithIndex(int i);
}