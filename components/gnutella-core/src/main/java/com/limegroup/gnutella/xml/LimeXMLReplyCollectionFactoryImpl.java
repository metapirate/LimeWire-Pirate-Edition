package com.limegroup.gnutella.xml;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.metadata.MetaDataFactory;
import com.limegroup.gnutella.metadata.MetaDataReader;

@Singleton
class LimeXMLReplyCollectionFactoryImpl implements LimeXMLReplyCollectionFactory {

    private final Provider<LimeXMLProperties> limeXMLProperties;

    private final Provider<Library> library;

    private final Provider<LimeXMLDocumentFactory> limeXMLDocumentFactory;

    private final Provider<MetaDataReader> metaDataReader;
    
    private final Provider<MetaDataFactory> metaDataFactory;
    
    @Inject
    public LimeXMLReplyCollectionFactoryImpl(
            Provider<LimeXMLProperties> limeXMLProperties, Provider<Library> library,
            Provider<LimeXMLDocumentFactory> limeXMLDocumentFactory, Provider<MetaDataReader> metaDataReader,
            Provider<MetaDataFactory> metaDataFactory) {
        this.limeXMLProperties = limeXMLProperties;
        this.library = library;
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.metaDataReader = metaDataReader;
        this.metaDataFactory = metaDataFactory;
    }

    public LimeXMLReplyCollection createLimeXMLReplyCollection(String URI) {
        return new LimeXMLReplyCollection(URI, limeXMLProperties.get().getXMLDocsDir(), library, limeXMLDocumentFactory, metaDataReader, metaDataFactory);
    }

}
