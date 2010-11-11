package org.limewire.ui.swing.nav;

import com.google.inject.AbstractModule;


public class LimeWireUiNavModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(Navigator.class).to(NavigatorImpl.class);
    }

    
}
