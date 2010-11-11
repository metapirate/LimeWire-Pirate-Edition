package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.NameValue;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;

/**
 * Utility class that creates a <tt>LimeXMLDocument</tt> from a file.
 */
@Singleton
public class MetaDataReader {

    private static final Log LOG = LogFactory.getLog(MetaDataReader.class);
    
    private final LimeXMLDocumentFactory limeXMLDocumentFactory;
    private final LimeXMLSchemaRepository limeXMLSchemaRepository;
    private final Provider<MetaDataFactory> metaDataFactory;

    @Inject
    MetaDataReader(LimeXMLDocumentFactory limeXMLDocumentFactory,
            LimeXMLSchemaRepository limeXMLSchemaRepository,
            Provider<MetaDataFactory> metaDataFactory) {
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.limeXMLSchemaRepository = limeXMLSchemaRepository;
        this.metaDataFactory = metaDataFactory;
    }

    /**
     * Generates a LimeXMLDocument from this file, only parsing it if it's the
     * given schemaURI.
     */
    public LimeXMLDocument readDocument(File file) throws IOException {
        MetaData data = metaDataFactory.get().parse(file);
        if (data == null)
            throw new IOException("unable to parse file");

        List<NameValue<String>> nameValList = data.toNameValueList();
        if (nameValList.isEmpty())
            throw new IOException("invalid/no data.");

        String uri = data.getSchemaURI();
        LimeXMLSchema schema = limeXMLSchemaRepository.getSchema(uri);
        if (schema == null || schema.getCanonicalizedFields().isEmpty())
            throw new IOException("schema: " + uri + " doesn't exist");
        
        try {
            return limeXMLDocumentFactory.createLimeXMLDocument(nameValList, uri);
        } catch(IllegalArgumentException iae) {
            LOG.warn("Error creating document", iae);
            // See: LWC-1150.  It's sometimes the case that people had
            // old schemas that don't contain all the newer fields, and
            // some docs are setup with only newer fields, leading the
            // attributeString to be empty, causing the doc to not validate.
            // Is worked-around as much as possible.
            throw (IOException)new IOException().initCause(iae);
        }
    }

}
