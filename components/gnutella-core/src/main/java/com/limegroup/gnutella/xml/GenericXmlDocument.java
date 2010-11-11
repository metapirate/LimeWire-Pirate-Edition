package com.limegroup.gnutella.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.limewire.util.NameValue;
import org.limewire.util.Objects;
import org.limewire.util.RPNParser.StringLookup;

import com.google.common.collect.ImmutableList;
import com.limegroup.gnutella.licenses.LicenseType;

/**
 * A generic implementation of LimeXMLDocument.
 */
class GenericXmlDocument implements StringLookup, LimeXMLDocument {

    static final String XML_LICENSE_ATTRIBUTE = "license__";
    static final String XML_LICENSE_TYPE_ATTRIBUTE = "licensetype__";
    static final String VERSION_STRING = "internal_version";
    
    /**
     * The current version of LimeXMLDocuments.
     *
     * Increment this number as features are added which require
     * reparsing documents on disk.
     */
    static final int CURRENT_VERSION = 3;
    
    /**
     * Cached list of keywords.  Because keywords are only filled up
     * upon construction, they can be cached upon retrieval.
     */
    private volatile List<String> cachedKeywords = null;
    
    /** The kind of license this has. */
    private final LicenseType licenseType;
    /** The schema for this document. */
    private final LimeXMLSchema schema;
    /** The actual data. */
    private final Map<String, String> data;
    /** The action that this doc has. */
    private final String action;
    
    /** The file this is related to.  Can be null if pure meta-data. */
    private File fileId;    
    
    /**  The version of this LimeXMLDocument. */
    private volatile int version;    
    /** Cached hashcode. */
    private volatile int hashCode = 0;

    GenericXmlDocument(LimeXMLSchema schema, Map<String, String> map, int version, String action, LicenseType licenseType) {
        this.version = version;
        this.action = action == null ? "" : action;
        this.licenseType = Objects.nonNull(licenseType, "licenseType");
        this.schema = Objects.nonNull(schema, "schema");
        this.data = new HashMap<String, String>(Objects.nonNull(map, "map")); // not unmodifable for memory optimization
    }

    @Override
    public Set<Entry<String, String>> getNameValueSet() {
        return data.entrySet();
    }

    @Override
    public String getValue(String fieldName) {
        return data.get(fieldName);
    }

    @Override
    public List<String> getKeyWords() {
        if( cachedKeywords != null ) {
            return cachedKeywords;
        }

        List<String> retList = new ArrayList<String>();
        for(Map.Entry<String, String> entry : getNameValueSet()) {
            String currKey = entry.getKey();
            String val = entry.getValue();
            if(val != null && !val.equals("") && !isIndivisible(currKey) && isIndexableKey(currKey)) {
                try {
                    Double.parseDouble(val); // will trigger NFE.
                } catch(NumberFormatException ignored) {
                    retList.add(val);
                }
            }
        }
        
        cachedKeywords = ImmutableList.copyOf(retList);
        return retList;
    }
    
    @Override
    public int getNumFields() {
        return getNameValueSet().size();
    }

    @Override
    public List<String> getKeyWordsIndivisible() {
        return licenseType.getIndivisibleKeywords();
    }

    /**
     * Determines if this keyword & value is indivisible
     * (thus making QRP not split it).
     */
    private boolean isIndivisible(String currKey) {
        //the license-type is always indivisible.
        //note that for weed licenses, this works because getKeyWordsIndivisible
        //is returning a list of only 'WeedInfo.LAINFO'.  the content-id & version-id
        //are essentially lost & ignored.
        return currKey.endsWith(XML_LICENSE_TYPE_ATTRIBUTE);
    }
    
    private boolean isIndexableKey(String key) {
        return !key.equals(LimeXMLNames.TORRENT_FILE_SIZES) && !key.equals(LimeXMLNames.TORRENT_INFO_HASH) && !key.equals(LimeXMLNames.TORRENT_TRACKERS);
    }

    @Override
    public String getSchemaURI() {
        return getSchema().getSchemaURI();
    }

    @Override
    public LimeXMLSchema getSchema() {
        return schema;
    }

    @Override
    public File getIdentifier() {
        return fileId;
    }

    @Override
    public void initIdentifier(File id) {
        fileId = id;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override    
    public boolean isLicenseAvailable() {
        return licenseType != LicenseType.NO_LICENSE;
    }

    @Override
    public String getLicenseString() {
        if(isLicenseAvailable()) {
            String licenseStringSuffix = getVerifiableLicenseElement(licenseType);
            if (licenseStringSuffix == null) {
                return null;
            }
            for(Map.Entry<String, String> next : getNameValueSet()) {
                String key = next.getKey();
                if (key.endsWith(licenseStringSuffix))
                    return next.getValue();
            }
        }
        return null;
    }

    /** Returns the end of the key value where the license string can be found. */
    protected String getVerifiableLicenseElement(LicenseType type) {
        if (type == LicenseType.CC_LICENSE)
            return XML_LICENSE_ATTRIBUTE;
        if (type.isDRMLicense())
            return XML_LICENSE_TYPE_ATTRIBUTE;
        return null;
    }

    @Override
    public List<NameValue<String>> getOrderedNameValueList() {
        if(getSchema() == null) {
            return Collections.emptyList();
        }
        
        List<String> fNames = getSchema().getCanonicalizedFieldNames();
        List<NameValue<String>> retList = new ArrayList<NameValue<String>>(fNames.size());
        for (String fieldName : fNames) {
            String value = getValue(fieldName);
            if (value != null) {
                retList.add(new NameValue<String>(fieldName, value));
            }
        }
            
        return retList;
    }

    @Override
    public String getXMLString() {
        StringBuilder fullXML = new StringBuilder();
        LimeXMLDocumentHelper.buildXML(fullXML, getSchema(), getAttributeString() + "/>");
        return fullXML.toString();
    }

    @Override
    public String getAttributeStringWithIndex(int i) {
        String attributes = getAttributeString();
        return attributes + " index=\"" + i + "\"/>";
    }
    
    /**
     * Returns the attribute string. THIS IS NOT A FULL XML ELEMENT.
     * It is purposely left unclosed so an index can easily be inserted.
     */
    private String getAttributeString() {
        return constructAttributeString();
    }

    /**
     * Retrieves the XML of this, with the version embedded. This is useful for
     * serializing the XML.
     */
    String getXmlWithVersion() {
        StringBuilder fullXML = new StringBuilder();
        LimeXMLDocumentHelper.buildXML(fullXML, getSchema(), getAttributeString() + " " + VERSION_STRING + "=\"" + version + "\"/>");
        return fullXML.toString();
    }
    
    /** Determines if this XML was built with the current version. */
    boolean isCurrent() { return version == CURRENT_VERSION; }
    
    /** Sets this XML to be current. */
    void setCurrent() { version = CURRENT_VERSION; }
    
    /**
     * Constructs the open-ended XML that contains the attributes.
     * This is purposely open-ended so that an index can easily be
     * inserted.
     * If no attributes exist, this returns an empty string,
     * to easily be marked as invalid.
     */
    private String constructAttributeString() {
        List<NameValue<String>> attributes = getOrderedNameValueList();
        if(attributes.isEmpty())
            return ""; // invalid.
            
        StringBuilder tag = new StringBuilder();
        String root = getSchema().getRootXMLName();
        String type = getSchema().getInnerXMLName();
        String canonicalKey = root + "__" + type + "__";
        tag.append("<");
        tag.append(type);

        for(NameValue<String> nv : attributes) {
            String name = XMLStringUtils.getLastField(canonicalKey, nv.getName());
            if(name == null)
                continue;
            // Construct: ' attribute="value"'
            tag.append(" ");
            tag.append(name);
            tag.append("=\"");
            tag.append(LimeXMLUtils.encodeXML(nv.getValue()));
            tag.append("\"");
        }
        return tag.toString();
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) {
            return true;
        } else if(!(o instanceof GenericXmlDocument)) {
            return false;
        } else {        
            GenericXmlDocument xmlDoc = (GenericXmlDocument) o;
            return action.equals(xmlDoc.getAction())
               && version == xmlDoc.version
               && schema.equals(xmlDoc.schema)
               && data.equals(xmlDoc.data);
        }
    }


    @Override
    public int hashCode() {
        if(hashCode == 0) {
            int result = 17;
            result = 37 * result + action.hashCode();
            result = 37 * result + schema.hashCode();
            result = 37 * result + data.hashCode();
            hashCode = result;
        } 
        return hashCode;
    }

    /**
     * Returns the XML identifier for the string.
     */
    @Override
    public String toString() {
        return getXMLString();
    }

    @Override
    public String lookup(String key) {
        if (key == null)
            return null;
        if ("schema".equals(key))
            return getSchema().getDescription();
        if ("numKWords".equals(key))
            return String.valueOf(getKeyWords().size());
        if ("numKWordsI".equals(key))
            return String.valueOf(getKeyWordsIndivisible().size());
        if ("numFields".equals(key))
            return String.valueOf(getNumFields());
        if ("ver".equals(key))
            return String.valueOf(version);
        if (key.startsWith("field_")) {
            key = key.substring(6,key.length());
            return getValue(key);
        }
        return null;
    }
}

