package org.limewire.ui.swing.dock;

import com.google.inject.AbstractModule;

public class LimeWireUiDockModule extends AbstractModule {
    
    @Override
    protected void configure() {
        
        bind(DockIconFactory.class).to(DockIconFactoryImpl.class);   
        
    }
}
