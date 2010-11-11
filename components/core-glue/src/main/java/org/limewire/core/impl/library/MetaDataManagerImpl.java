package org.limewire.core.impl.library;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MetaDataException;
import org.limewire.core.api.library.MetaDataManager;
import org.limewire.core.impl.util.FilePropertyKeyPopulator;
import org.limewire.util.NameValue;
import org.xml.sax.SAXException;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.metadata.MetaDataFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection;
import com.limegroup.gnutella.xml.SchemaNotFoundException;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection.MetaDataState;

@Singleton
public class MetaDataManagerImpl implements MetaDataManager {
    private final SchemaReplyCollectionMapper schemaReplyCollectionMapper;

    private final LimeXMLDocumentFactory limeXMLDocumentFactory;
    private final Provider<MetaDataFactory> metaDataFactory;

    @Inject
    public MetaDataManagerImpl(LimeXMLDocumentFactory limeXMLDocumentFactory,
            SchemaReplyCollectionMapper schemaReplyCollectionMapper,
            Provider<MetaDataFactory> metaDataFactory) {
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.schemaReplyCollectionMapper = schemaReplyCollectionMapper;
        this.metaDataFactory = metaDataFactory;
    }

    @Override
    public void save(LocalFileItem localFileItem, Map<FilePropertyKey, Object> newData) throws MetaDataException {
        if (localFileItem instanceof CoreLocalFileItem) {
            CoreLocalFileItem coreLocalFileItem = (CoreLocalFileItem) localFileItem;
            saveMetaData(coreLocalFileItem, newData);
        }
    }

    private void saveMetaData(CoreLocalFileItem coreLocalFileItem, Map<FilePropertyKey, Object> newData) throws MetaDataException {
        FileDesc fileDesc = coreLocalFileItem.getFileDesc();
        Category category = coreLocalFileItem.getCategory();

        String limeXMLSchemaUri = FilePropertyKeyPopulator.getLimeXmlSchemaUri(category);
        LimeXMLDocument oldDocument = fileDesc.getXMLDocument(limeXMLSchemaUri);

        String input = buildInput(fileDesc, limeXMLSchemaUri, coreLocalFileItem, newData);

        if (oldDocument != null && (input == null || input.trim().length() == 0)) {
            removeMeta(fileDesc, limeXMLSchemaUri);
            return;
        } else if (input == null || input.trim().length() == 0) {
            return;
        }

        LimeXMLDocument newDoc = null;

        try {
            newDoc = limeXMLDocumentFactory.createLimeXMLDocument(input);
        } catch (SAXException e) {
            throw new MetaDataException("Internal Document Error. Data could not be saved.", e);
        } catch (SchemaNotFoundException e) {
            throw new MetaDataException("Internal Document Error. Data could not be saved.", e);
        } catch (IOException e) {
            throw new MetaDataException("Internal Document Error. Data could not be saved.", e);
        }

        String schemaURI = newDoc.getSchemaURI();
        LimeXMLReplyCollection collection = schemaReplyCollectionMapper
                .getReplyCollection(schemaURI);

        LimeXMLDocument result = null;

        if (oldDocument != null) {
            result = merge(oldDocument, newDoc);
            oldDocument = collection.replaceDoc(fileDesc, result);
        } else {
            result = newDoc;
            collection.addReply(fileDesc, result);
        }

        if(metaDataFactory.get().containsReader(fileDesc.getFile())) {
            final MetaDataState committed = collection.mediaFileToDisk(fileDesc, result);
            if (committed != MetaDataState.NORMAL && committed != MetaDataState.UNCHANGED) {
                throw new MetaDataException("Internal Document Error. Data could not be saved.");
            }
        } else if (!collection.writeMapToDisk()) {
            throw new MetaDataException("Internal Document Error. Data could not be saved.");
        }
    }

    /**
     * Merge the current and new doc.
     */
    public LimeXMLDocument merge(LimeXMLDocument currentDoc, LimeXMLDocument newDoc) {
        if (!currentDoc.getSchemaURI().equalsIgnoreCase(newDoc.getSchemaURI())) {
            throw new IllegalArgumentException(
                    "Current XML document and new XML document must be of the same type!");
        }

        Map<String, Map.Entry<String, String>> map = new HashMap<String, Map.Entry<String, String>>();

        // Initialize the Map with the current fields
        for (Map.Entry<String, String> entry : currentDoc.getNameValueSet())
            map.put(entry.getKey(), entry);

        // And overwrite everything with the new fields
        for (Map.Entry<String, String> entry : newDoc.getNameValueSet())
            map.put(entry.getKey(), entry);

        return limeXMLDocumentFactory.createLimeXMLDocument(map.values(), currentDoc.getSchemaURI());
    }

    private String buildInput(FileDesc fileDesc, String limeXMLSchemaUri, LocalFileItem localFileItem, Map<FilePropertyKey, Object> newData) {
        List<NameValue<String>> nameValueList = new ArrayList<NameValue<String>>();
        Category category = localFileItem.getCategory();
        
        // cycle through the list of editable fields that were displayed
        for(FilePropertyKey filePropertyKey : newData.keySet()) {
            String limeXmlName = FilePropertyKeyPopulator.getLimeXmlName(category, filePropertyKey);
            if (limeXmlName != null) {
                Object value = newData.get(filePropertyKey);
                if(value != null)
                    value = FilePropertyKeyPopulator.sanitizeValue(filePropertyKey, value);
                
                // must set null fields to empty string otherwise when merging new and old XML
                // docs, this will be replaced by the old value
                if(value == null)
                    value = "";
                
                nameValueList.add(new NameValue<String>(limeXmlName, value.toString()));
            }
        }

        // if there are no fields, don't try to create an XML doc
        if(nameValueList.size() == 0)
            return null;

        return limeXMLDocumentFactory.createLimeXMLDocument(nameValueList, limeXMLSchemaUri).getXMLString();
    }

    private void removeMeta(FileDesc fileDesc, String limeXMLSchemaUri) throws MetaDataException {

        LimeXMLReplyCollection collection = schemaReplyCollectionMapper.getReplyCollection(limeXMLSchemaUri);

        if (!collection.removeDoc(fileDesc)) {
            throw new MetaDataException("Internal Document Error. Data could not be saved.");
        }
    }

}
