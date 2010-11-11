package org.limewire.ui.swing;

import org.limewire.ui.swing.mainframe.AppFrame;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;


/** A module that lists all LW modules and exists solely for the purpose of inspection bootstrapping. */
public class AllLimeWireModules__DO_NOT_USE extends AbstractModule {
    
    @Override
    protected void configure() {
        install(new LimeWireModule());
        install(new LimeWireSwingUiModule());
        bind(AppFrame.class).in(Scopes.SINGLETON);
    }

}
