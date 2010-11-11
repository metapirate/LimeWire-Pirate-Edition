package org.limewire.ui.swing.search.resultpanel;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiSearchResultPanelModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SearchResultMenuFactory.class).toProvider(FactoryProvider.newFactory(
                SearchResultMenuFactory.class, SearchResultMenu.class));
    }

}
