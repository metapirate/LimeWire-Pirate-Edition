package org.limewire.core.impl.mozilla;

import com.google.inject.AbstractModule;

public class CoreGlueMozillaModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(LimeMozillaDownloadManagerListenerImpl.class);
        bind(LimeMozillaOverrides.class);
    }
}
