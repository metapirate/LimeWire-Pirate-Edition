package org.limewire.ui.swing.components;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

/**
 * Module to configure Guice bindings for the UI components.
 */
public class LimeWireUiComponentsModule extends AbstractModule {
    
    /**
     * Configures the bindings for the UI components.
     */
    @Override
    protected void configure() {
        bind(FlexibleTabListFactory.class).toProvider(
                FactoryProvider.newFactory(FlexibleTabListFactory.class, FlexibleTabList.class));   
        
//        bind(ShapeDialog.class);
        
        bind(RemoteHostWidgetFactory.class).toProvider(
                FactoryProvider.newFactory(RemoteHostWidgetFactory.class, RemoteHostWidget.class)); 
    }
}
