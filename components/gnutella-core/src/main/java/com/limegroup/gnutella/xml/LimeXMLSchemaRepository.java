/*
 * LimeXMLSchemaRepository.java
 *
 * Created on April 12, 2001, 4:00 PM
 */

package com.limegroup.gnutella.xml;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Stores and provides access to various XML schemas that me might have.
 * Singleton class.
 * 
 * @author asingla
 */
@Singleton
public class LimeXMLSchemaRepository {

    /**
     * Mapping from URI (string) to an instance of XMLSchema.
     */
    private Map<String, LimeXMLSchema> _uriSchemaMap = new HashMap<String, LimeXMLSchema>();

    /** Creates new LimeXMLSchemaRepository. */
    @Inject
    LimeXMLSchemaRepository(LimeXMLProperties limeXMLProperties) {
        URL[] schemaUrls = limeXMLProperties.getAllXmlSchemaUrls();
        
        // create schema objects and put them in the _uriSchemaMap
        for (int i = 0; i < schemaUrls.length; i++) {
            if(schemaUrls[i] != null) {
                try {
                    LimeXMLSchema limeXmlSchema= new LimeXMLSchema(schemaUrls[i]);
                    _uriSchemaMap.put(limeXmlSchema.getSchemaURI(), limeXmlSchema);
                } catch (IOException ioe) {}
            }
        }
    }

    /**
     * Returns the schema corresponding to the given URI.
     * 
     * @param uri The URI which identifies the schema to be returned.
     * @return The schema corresponding to the given uri. If no mapping exists,
     *         returns null.
     */
    public LimeXMLSchema getSchema(String uri) {
        synchronized (_uriSchemaMap) {
            return _uriSchemaMap.get(uri);
        }
    }

    /**
     * Returns all available schemas.
     */
    public Collection<LimeXMLSchema> getAvailableSchemas() {
        return Collections.unmodifiableCollection(_uriSchemaMap.values());
    }

    /**
     * Returns the URIs corresponding to the schemas that we have.
     * 
     * @return sorted array of URIs corresponding to the schemas that we have
     */
    public String[] getAvailableSchemaURIs() {
        String[] schemaURIs;
        synchronized (_uriSchemaMap) {
            schemaURIs = _uriSchemaMap.keySet().toArray(new String[0]);
        }
        Arrays.sort(schemaURIs);
        return schemaURIs;
    }
    
}
