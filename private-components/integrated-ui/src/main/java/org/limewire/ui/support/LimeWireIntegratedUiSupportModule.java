package org.limewire.ui.support;

import com.google.inject.AbstractModule;

public class LimeWireIntegratedUiSupportModule extends AbstractModule {
    
    @Override
    protected void configure() {
        requestStaticInjection(FatalBugManager.class);
        requestStaticInjection(DeadlockBugManager.class);
        requestStaticInjection(ServletAccessor.class);
        requestStaticInjection(BugManager.class);
    }

}
