package org.limewire.core.impl.upload;

import org.limewire.core.api.upload.UploadListManager;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class CoreGlueUploadModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(UploadListManager.class).to(CoreUploadListManager.class);
        bind(CoreUploadItem.Factory.class).toProvider(FactoryProvider.newFactory(CoreUploadItem.Factory.class, CoreUploadItem.class));
    }
}
