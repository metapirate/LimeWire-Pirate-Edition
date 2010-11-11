package org.limewire.ui.swing.util;


import org.limewire.inject.LazyBinder;

import com.google.inject.AbstractModule;

public class LimeWireUiUtilModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(NonBlockFileIconController.class).to(BasicFileIconController.class);
        bind(PropertiableHeadings.class).toProvider(LazyBinder.newLazyProvider(
                PropertiableHeadings.class, PropertiableHeadingsImpl.class));
        bind(DownloadExceptionHandler.class).to(DownloadExceptionHandlerImpl.class);
        bind(MagnetHandler.class).toProvider(LazyBinder.newLazyProvider(
                MagnetHandler.class, MagnetHandlerImpl.class));
    }
}