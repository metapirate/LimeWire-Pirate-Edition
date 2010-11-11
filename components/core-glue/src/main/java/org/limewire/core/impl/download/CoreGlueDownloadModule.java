package org.limewire.core.impl.download;


import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.ResultDownloader;
import org.limewire.core.impl.download.listener.ItunesDownloadListener;
import org.limewire.core.impl.download.listener.ItunesDownloadListenerFactory;
import org.limewire.core.impl.download.listener.TorrentDownloadListener;
import org.limewire.core.impl.download.listener.TorrentDownloadListenerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class CoreGlueDownloadModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(DownloadListManager.class).to(CoreDownloadListManager.class);
        bind(ResultDownloader.class).to(CoreDownloadListManager.class);
        bind(ItunesDownloadListenerFactory.class).toProvider(FactoryProvider.newFactory(ItunesDownloadListenerFactory.class, ItunesDownloadListener.class));
        bind(TorrentDownloadListenerFactory.class).toProvider(FactoryProvider.newFactory(TorrentDownloadListenerFactory.class, TorrentDownloadListener.class));
        bind(CoreDownloadItem.Factory.class).toProvider(FactoryProvider.newFactory(CoreDownloadItem.Factory.class, CoreDownloadItem.class));
    }
}
