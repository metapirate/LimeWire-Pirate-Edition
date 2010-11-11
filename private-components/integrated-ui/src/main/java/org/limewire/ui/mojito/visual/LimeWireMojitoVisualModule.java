package org.limewire.ui.mojito.visual;

import org.limewire.inject.AbstractModule;
import org.limewire.ui.swing.plugin.SwingUiPlugin;

import com.google.inject.name.Names;

public class LimeWireMojitoVisualModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SwingUiPlugin.class).annotatedWith(Names.named("MojitoArcsPlugin")).to(ArcsPlugin.class);        
    }

}
