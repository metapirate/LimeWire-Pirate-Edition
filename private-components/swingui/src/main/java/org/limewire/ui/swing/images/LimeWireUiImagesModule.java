package org.limewire.ui.swing.images;

import org.limewire.inject.LazyBinder;

import com.google.inject.AbstractModule;

public class LimeWireUiImagesModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ThumbnailManager.class).toProvider(LazyBinder.newLazyProvider(
                ThumbnailManager.class, ThumbnailManagerImpl.class));
    }
}
