package com.limegroup.gnutella.xml;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileDescChangeEvent;
import com.limegroup.gnutella.library.FileViewChangeEvent;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.library.LibraryStatusEvent;


/** 
 * Used to map schema URIs to Reply Collections.
 * 
 * @author Sumeet Thadani
 */
@EagerSingleton
public class SchemaReplyCollectionMapper implements XmlController {
    
    private final Map<String, LimeXMLReplyCollection> mapper;
    
    protected final Provider<LimeXMLReplyCollectionFactory> limeXMLReplyCollectionFactory;
    protected final Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository;
    
    @Inject SchemaReplyCollectionMapper(Provider<LimeXMLReplyCollectionFactory> limeXMLReplyCollectionFactory,
            Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository) {
        this.limeXMLReplyCollectionFactory = limeXMLReplyCollectionFactory;
        this.limeXMLSchemaRepository = limeXMLSchemaRepository;
        
        mapper = new ConcurrentHashMap<String, LimeXMLReplyCollection>();
    }


    /**
     * Adds the SchemaURI to a HashMap with the replyCollection.
     * <p>
     * Warning/Note:If the schemaURI already corresponds to a ReplyCollection
     * this method will replace the old reply collection with the new one. 
     * The old collection will be lost!
     */
    public void add(String schemaURI, LimeXMLReplyCollection replyCollection) {
        mapper.put(schemaURI, replyCollection);
    }
    
    /**
     * Looks up and returns the <tt>LimeXMLReplyCollection</tt> value for the
     * supplied schemaURI key.
     * 
     * @ return the <tt>LimeXMLReplyCollection</tt> for the given schema URI,
     * or <tt>null</tt> if we the requested mapping does not exist
     */
    public LimeXMLReplyCollection getReplyCollection(String schemaURI) {
        return mapper.get(schemaURI);
    }
    
    /**
     * Returns a collection of all available LimeXMLReplyCollections.
     */
    public Collection<LimeXMLReplyCollection> getCollections() {
        return mapper.values();
    }
    
    @Inject void register(ServiceRegistry registry, final Library managedList,
            final ListenerSupport<FileDescChangeEvent> fileDescSupport) {
        registry.register(new Service() {
            @Override
            public String getServiceName() {
                return I18nMarker.marktr("Metadata Loader");
            }
            @Override
            public void initialize() {
                loadSchemas();
                
                managedList.addListener(new EventListener<FileViewChangeEvent>() {
                    @Override
                    public void handleEvent(FileViewChangeEvent event) {
                        switch(event.getType()) {
                        case FILE_REMOVED:
                            removeFileDesc(event.getFileDesc());
                            break;
                        case FILE_CHANGED:
                            removeFileDesc(event.getOldValue());
                            break; 
                        case FILES_CLEARED:
                            loadSchemas();
                            break;
                        }
                    }
                });
                
                managedList.addManagedListStatusListener(new EventListener<LibraryStatusEvent>() {
                    @Override
                    public void handleEvent(LibraryStatusEvent event) {
                        switch (event.getType()) {
                        case LOAD_FINISHING:
                            finishLoading();
                            break;
                        case SAVE:
                            save(event);
                            break;

                        }
                    }
                });
            }
            @Override
            public void start() {
                // TODO Auto-generated method stub
                
            }
            @Override
            public void stop() {
                // TODO Auto-generated method stub
                
            }
        });
    }
    
    private void removeFileDesc(FileDesc fd) {
        // Get the schema URI of each document and remove from the collection
        // We must remember the schemas and then remove the doc, or we will
        // get a concurrent mod exception because removing the doc also
        // removes it from the FileDesc.
        List<LimeXMLDocument> xmlDocs = fd.getLimeXMLDocuments();
        List<String> schemas = new LinkedList<String>();
        for (LimeXMLDocument doc : xmlDocs)
            schemas.add(doc.getSchemaURI());
        for (String uri : schemas) {
            LimeXMLReplyCollection col = getReplyCollection(uri);
            if (col != null)
                col.removeDoc(fd);
        }
    }
    
    /**
     * Notifies all the LimeXMLReplyCollections that the initial loading
     * has completed.
     */
    private void finishLoading() {
        Collection<LimeXMLReplyCollection> replies = getCollections();
        for (LimeXMLReplyCollection col : replies)
            col.loadFinished();
    }
    
    /**
     * Serializes the current LimeXMLReplyCollection to disk.
     */
    private void save(LibraryStatusEvent event) {
        if (event.getLibrary().isLoadFinished()) {
            Collection<LimeXMLReplyCollection> replies = getCollections();
            for (LimeXMLReplyCollection col : replies)
                col.writeMapToDisk();
        }
    }
    
    @Override
    public boolean canConstructXml(FileDesc fd) {
        Collection<LimeXMLReplyCollection> replies = getCollections();
        for (LimeXMLReplyCollection col : replies) {
            if(col.canCreateDocument(fd.getFile())) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean loadCachedXml(FileDesc fd, Collection<? extends LimeXMLDocument> prebuilt) {
        Collection<LimeXMLReplyCollection> replies = getCollections();
        boolean loaded = false;
        for (LimeXMLReplyCollection col : replies) {
            LimeXMLDocument doc = col.initialize(fd, prebuilt);
            if(doc != null) {
                loaded = true;
            }
        }
        return loaded;
    }
    
    /**
     * Loads the map with the LimeXMLDocument for a given FileDesc. If no LimeXMLDocument
     * exists for the FileDesc, one is created for it.
     */
    @Override
    public boolean loadXml(FileDesc fd) {
        Collection<LimeXMLReplyCollection> replies = getCollections();
        boolean loaded = false;
        for (LimeXMLReplyCollection col : replies) {
            LimeXMLDocument doc = col.createIfNecessary(fd);
            if(doc != null) {
                loaded = true;
            }
        }
        return loaded;
    }
    
    /**
     * Loads all the SchemaURI to a HashMap with the replyCollection. 
     */
    private void loadSchemas() {
        String[] schemas = limeXMLSchemaRepository.get().getAvailableSchemaURIs();
        for(String schema : schemas) {
            add(schema, limeXMLReplyCollectionFactory.get().createLimeXMLReplyCollection(schema));
        }
    }
}
