package org.limewire.ui.swing.options;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiOptionsModule extends AbstractModule {

    @Override
    protected void configure() {
        
        bind(ManageSaveFoldersOptionPanelFactory.class).toProvider(
                FactoryProvider.newFactory(
                        ManageSaveFoldersOptionPanelFactory.class, ManageSaveFoldersOptionPanel.class));   
    }
}
