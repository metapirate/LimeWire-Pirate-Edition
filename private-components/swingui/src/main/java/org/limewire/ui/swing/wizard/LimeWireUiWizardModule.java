package org.limewire.ui.swing.wizard;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiWizardModule extends AbstractModule {

    @Override
    protected void configure() {
        
        bind(SetupComponentDecoratorFactory.class).toProvider(
                FactoryProvider.newFactory(SetupComponentDecoratorFactory.class, 
                        SetupComponentDecorator.class));  
    }

}
