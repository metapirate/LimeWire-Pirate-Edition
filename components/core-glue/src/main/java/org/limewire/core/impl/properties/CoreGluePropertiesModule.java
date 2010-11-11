package org.limewire.core.impl.properties;

import org.limewire.core.api.properties.PropertyDictionary;

import com.google.inject.AbstractModule;

public class CoreGluePropertiesModule extends AbstractModule {
        
    @Override
    protected void configure() {
        bind(PropertyDictionary.class).to(PropertyDictionaryImpl.class);
    }
}
