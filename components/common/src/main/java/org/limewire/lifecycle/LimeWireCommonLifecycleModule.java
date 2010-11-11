package org.limewire.lifecycle;

import com.google.inject.AbstractModule;

public class LimeWireCommonLifecycleModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ServiceRegistry.class).to(ServiceRegistryImpl.class); 
        bind(ServiceScheduler.class).to(ServiceSchedulerImpl.class);
    }

}
