package com.limegroup.gnutella.xml;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.StringUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.licenses.CCConstants;
import com.limegroup.gnutella.licenses.LicenseType;

@Singleton
class LimeXMLDocumentFactoryImpl implements LimeXMLDocumentFactory {
    
    private static final Log LOG = LogFactory.getLog(LimeXMLDocumentFactoryImpl.class);

    private static final String XML_ID_ATTRIBUTE = "identifier__";
    private static final String XML_ACTION_ATTRIBUTE = "action__";
//    private static final String XML_ACTION_INFO = "addactiondetail__";
            static final String XML_INDEX_ATTRIBUTE = "index__";
    private static final String XML_VERSION_ATTRIBUTE = GenericXmlDocument.VERSION_STRING + "__";
    
    private final Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository;

    @Inject
    public LimeXMLDocumentFactoryImpl(Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository) {
        this.limeXMLSchemaRepository = limeXMLSchemaRepository;
    }
    
    public LimeXMLDocument createLimeXMLDocument(String xml)
            throws SAXException, SchemaNotFoundException, IOException {
        if (xml == null || xml.equals("")) {
            throw new SAXException("null or empty string");
        }

        InputSource doc = new InputSource(new StringReader(xml));
        XMLParsingUtils.ParseResult result = XMLParsingUtils.parse(doc);
        if (result.isEmpty()) {
            throw new IOException("No element present");
        }
        if (result.schemaURI == null) {
            throw new SchemaNotFoundException("no schema");
        }

        Map<String, String> map = result.get(0);
        DocInfo docInfo = setFields(result.canonicalKeyPrefix, map);        
        LimeXMLSchema schema = limeXMLSchemaRepository.get().getSchema(result.schemaURI);        
        return createDocument(schema, map, docInfo);
    }

    public LimeXMLDocument createLimeXMLDocument(
            Map<String, String> map, String schemaUri, String keyPrefix)
            throws IOException {        
        if(map.isEmpty()) {
            throw new IllegalArgumentException("empty map");
        }
        if(schemaUri == null) {
            throw new IOException("null schemaUri");
        }
        
        map.remove(keyPrefix + XML_ID_ATTRIBUTE); // remove id.
        DocInfo docInfo = setFields(keyPrefix, map);        
        LimeXMLSchema schema = limeXMLSchemaRepository.get().getSchema(schemaUri);        
        return createDocument(schema, map, docInfo);
    }

    public LimeXMLDocument createLimeXMLDocument(
            Collection<? extends Entry<String, String>> nameValueList,
            String schemaURI) {
        if(nameValueList.isEmpty()) {
            throw new IllegalArgumentException("empty list");
        }
        if(schemaURI == null) {
            throw new IllegalArgumentException("null schemaUri not allowed");
        }

        Map<String, String> map = new HashMap<String, String>(nameValueList.size());
        //iterate over the passed list of field names & values
        for(Map.Entry<String, String> next : nameValueList) {
            String key = next.getKey() == null ? null : next.getKey().trim().intern();
            String value = next.getValue() == null ? null : next.getValue().trim().intern();
            if(!StringUtils.isEmpty(key) && value != null) {
                map.put(key, value);
            }
        }
        
        // scan for action/id/etc..
        DocInfo docInfo = scanFields(map);
        if(docInfo == null) {
            throw new IllegalArgumentException("invalid nameValueList: " + nameValueList);
        }
        
        LimeXMLSchema schema = limeXMLSchemaRepository.get().getSchema(schemaURI);
        try {
            return createDocument(schema, map, docInfo);
        } catch(IOException iox) {
            // Rethrow as IllegalArgument, because this should not be happening
            // from user-supplied NameValue pairs.
            throw new IllegalArgumentException(iox);
        }
    }
    
    private LimeXMLDocument createDocument(LimeXMLSchema schema, Map<String, String> map, DocInfo docInfo) throws IOException {
        if (!isValid(schema, map)) {
            throw new IOException("invalid doc! "+map+" \nschema: " + (schema != null ? schema.getSchemaURI() : "null schema"));
        }
        
        return new GenericXmlDocument(schema, map, docInfo.version, docInfo.action, docInfo.licenseType);
    }
    
    /** Returns false if no valid document can be constructed with this information. */
    private boolean isValid(LimeXMLSchema schema, Map<String, String> map) {
        if(schema == null) {
            return false;
        }
        
        List<String> fNames = schema.getCanonicalizedFieldNames();
        for(String fieldName : fNames) {
            if(map.get(fieldName) != null) {
                // If atleast one field exists, it's valid.
                return true;
            }            
        }
        
        return false;
    }

    /**
     * Stores whether or not an action or CC license are in this LimeXMLDocument.
     */
    private DocInfo setFields(String prefix, Map<String, String> inputMap) {
        DocInfo docInfo = new DocInfo();
        
        // store action.
        docInfo.action = inputMap.get(prefix + XML_ACTION_ATTRIBUTE);
//        docInfo.actionDetail = inputMap.get(prefix + LimeXMLDocument.XML_ACTION_INFO);

        // deal with updating license_type based on the license
        String license = inputMap.get(prefix + GenericXmlDocument.XML_LICENSE_ATTRIBUTE);
        String type = inputMap.get(prefix + GenericXmlDocument.XML_LICENSE_TYPE_ATTRIBUTE);
        
        if(LOG.isDebugEnabled())
            LOG.debug("type: " + type);
        
        // Do specific stuff on licenseType for various licenses.
        // CC licenses require that the 'license' field has the CC_URI_PREFIX & CC_URL_INDICATOR
        // somewhere.  Weed licenses require that the 'license type' field has WeedInfo.LINFO,
        // a content id & a version id.
        docInfo.licenseType = LicenseType.determineLicenseType(license, type);        
        if (docInfo.licenseType == LicenseType.CC_LICENSE) {
            inputMap.put(prefix + GenericXmlDocument.XML_LICENSE_TYPE_ATTRIBUTE, CCConstants.CC_URI_PREFIX);
        } else if (docInfo.licenseType == LicenseType.LIMEWIRE_STORE_PURCHASE) {
            inputMap.put(prefix + GenericXmlDocument.XML_LICENSE_TYPE_ATTRIBUTE, LicenseType.LIMEWIRE_STORE_PURCHASE.toString());
        } else if (docInfo.licenseType == LicenseType.LIMEWIRE_STORE_RESHAREABLE) {
            inputMap.put(prefix + GenericXmlDocument.XML_LICENSE_TYPE_ATTRIBUTE, LicenseType.LIMEWIRE_STORE_RESHAREABLE.toString());
        }
        
        // Grab the version, if it exists.
        String versionString = inputMap.get(prefix + XML_VERSION_ATTRIBUTE);
        if(versionString != null) {
            try {
                docInfo.version = Integer.parseInt(versionString);
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Set version to: " + docInfo.version);
                }
            } catch(NumberFormatException nfe) {
                LOG.warn("Unable to set version", nfe);
                docInfo.version = GenericXmlDocument.CURRENT_VERSION;
            }
        } else {
            docInfo.version = GenericXmlDocument.CURRENT_VERSION;
        }
        inputMap.remove(prefix + XML_VERSION_ATTRIBUTE);
        
        if(LOG.isDebugEnabled())
            LOG.debug("Fields after setting: " + inputMap);
        
        return docInfo;
    }

    /**
     * Looks in the fields for the ACTION, IDENTIFIER, and INDEX, and a license.
     * Action is stored, index & identifier are removed.
     */
    private DocInfo scanFields(Map<String, String> inputMap) {
        String canonicalKey = getCanonicalKey(inputMap.entrySet());
        if(canonicalKey == null) {
            return null;
        } else {
            DocInfo docInfo = setFields(canonicalKey, inputMap);
            inputMap.remove(canonicalKey + XML_INDEX_ATTRIBUTE);
            inputMap.remove(canonicalKey + XML_ID_ATTRIBUTE);
            return docInfo;
        }
    }
    
    /** Derives a canonicalKey from a collection of Map.Entry's. */
    private String getCanonicalKey(Collection<? extends Map.Entry<String, String>> entries) {
        if(entries.isEmpty()) {
            return null;
        }
        Map.Entry<String, String> firstEntry = entries.iterator().next();
        String firstKey = firstEntry.getKey();
        
        // The canonicalKey is always going to be x__x__<other stuff here>
        int idx = firstKey.indexOf(XMLStringUtils.DELIMITER);
        idx = firstKey.indexOf(XMLStringUtils.DELIMITER, idx+1);
        // not two delimiters? can't find the canonicalKey
        if(idx == -1) {
            return null;
        }
            
        // 2 == XMLStringUtils.DELIMITER.length()
        return firstKey.substring(0, idx + 2);
    }
    
    private static class DocInfo {
        private LicenseType licenseType;
        private int version;
        private String action;
//        private String actionDetail;
    }    
}
