package org.limewire.ui.swing.tray;

import com.google.inject.AbstractModule;


public class LimeWireUiTrayModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(TrayNotifier.class).to(TrayNotifierProxy.class);
    }

}
