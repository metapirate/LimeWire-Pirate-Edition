package org.limewire.common;

import org.limewire.inject.LimeWireInjectModule;
import org.limewire.lifecycle.LimeWireCommonLifecycleModule;
import org.limewire.util.Clock;
import org.limewire.util.ClockImpl;

import com.google.inject.AbstractModule;

public class LimeWireCommonModule extends AbstractModule {
    
    @Override
    protected void configure() {
        install(new LimeWireInjectModule());
        install(new LimeWireCommonLifecycleModule());
        bind(Clock.class).to(ClockImpl.class);
    }

}
