package org.limewire.ui.swing.downloads.table;

import org.limewire.ui.swing.downloads.table.renderer.DownloadMessageRendererEditor;
import org.limewire.ui.swing.downloads.table.renderer.DownloadMessageRendererEditorFactory;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiDownloadsTableModule extends AbstractModule {

    @Override
    protected void configure() {        

        bind(DownloadTableFactory.class).toProvider(
                FactoryProvider.newFactory(
                        DownloadTableFactory.class, DownloadTable.class));
        bind(DownloadTableMenuFactory.class).toProvider(
                FactoryProvider.newFactory(
                        DownloadTableMenuFactory.class, DownloadTableMenu.class));
        bind(DownloadPopupHandlerFactory.class).toProvider(
                FactoryProvider.newFactory(
                        DownloadPopupHandlerFactory.class, DownloadPopupHandler.class));
        bind(DownloadMessageRendererEditorFactory.class).toProvider(
                FactoryProvider.newFactory(
                        DownloadMessageRendererEditorFactory.class, DownloadMessageRendererEditor.class));
    }

}
