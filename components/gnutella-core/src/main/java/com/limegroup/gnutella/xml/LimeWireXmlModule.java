package com.limegroup.gnutella.xml;

import com.google.inject.AbstractModule;

public class LimeWireXmlModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(XmlController.class).to(SchemaReplyCollectionMapper.class);
        bind(LimeXMLReplyCollectionFactory.class).to(LimeXMLReplyCollectionFactoryImpl.class);
        bind(LimeXMLDocumentFactory.class).to(LimeXMLDocumentFactoryImpl.class);
        
    }

}
