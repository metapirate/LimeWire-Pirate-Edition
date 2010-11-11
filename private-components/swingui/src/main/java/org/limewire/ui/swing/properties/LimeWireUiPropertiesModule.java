package org.limewire.ui.swing.properties;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;


public class LimeWireUiPropertiesModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FileInfoDialogFactory.class).toProvider(
                FactoryProvider.newFactory(FileInfoDialogFactory.class, FileInfoDialog.class));
                
        bind(FileInfoPanelFactory.class).to(FileInfoPanelFactoryImpl.class);
    }
}
