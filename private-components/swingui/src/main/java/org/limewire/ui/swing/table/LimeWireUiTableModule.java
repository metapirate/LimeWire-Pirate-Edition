package org.limewire.ui.swing.table;

import org.limewire.inject.AbstractModule;

import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiTableModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IconLabelRendererFactory.class).toProvider(FactoryProvider.newFactory(
                IconLabelRendererFactory.class, IconLabelRenderer.class));
    }
}
