package org.limewire.ui.swing;

import org.limewire.core.impl.CoreGlueModule;
import org.limewire.ui.mojito.visual.LimeWireMojitoVisualModule;
import org.limewire.ui.support.LimeWireIntegratedUiSupportModule;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.LimeWireCoreModule;

public class LimeWireModule extends AbstractModule {
    
    @Override
    protected void configure() {
        install(new LimeWireCoreModule());
        install(new CoreGlueModule());
        install(new LimeWireIntegratedUiSupportModule());
        install(new LimeWireMojitoVisualModule());
    }
    

}
