package com.limegroup.gnutella.downloader;

import java.util.Comparator;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningExecutorService;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.limegroup.bittorrent.BTDownloader;
import com.limegroup.bittorrent.BTDownloaderImpl;
import com.limegroup.bittorrent.BTTorrentFileDownloader;
import com.limegroup.bittorrent.BTTorrentFileDownloaderImpl;
import com.limegroup.gnutella.downloader.serial.DownloadSerializeSettings;
import com.limegroup.gnutella.downloader.serial.DownloadSerializeSettingsImpl;
import com.limegroup.gnutella.downloader.serial.DownloadSerializer;
import com.limegroup.gnutella.downloader.serial.DownloadSerializerImpl;
import com.limegroup.gnutella.downloader.serial.OldDownloadConverter;
import com.limegroup.gnutella.downloader.serial.conversion.OldDownloadConverterImpl;
import com.limegroup.gnutella.downloader.serial.conversion.OldDownloadSettings;

public class LimeWireDownloadModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(ManagedDownloader.class).to(ManagedDownloaderImpl.class);
        bind(MagnetDownloader.class).to(MagnetDownloaderImpl.class);
        bind(ResumeDownloader.class).to(ResumeDownloaderImpl.class);
        bind(BTDownloader.class).to(BTDownloaderImpl.class);
        bind(BTTorrentFileDownloader.class).to(BTTorrentFileDownloaderImpl.class);
        
        bind(RemoteFileDescFactory.class).to(RemoteFileDescFactoryImpl.class);
        bind(DownloadWorkerFactory.class).to(DownloadWorkerFactoryImpl.class);
        bind(HTTPDownloaderFactory.class).to(HTTPDownloaderFactoryImpl.class);
        bind(RequeryManagerFactory.class).to(RequeryManagerFactoryImpl.class);
        bind(PushedSocketHandlerRegistry.class).to(PushDownloadManager.class);
        bind(CoreDownloaderFactory.class).to(CoreDownloaderFactoryImpl.class);
        bind(DownloadSerializer.class).to(DownloadSerializerImpl.class);
        bind(DownloadSerializeSettings.class).to(DownloadSerializeSettingsImpl.class);
        bind(OldDownloadConverter.class).to(OldDownloadConverterImpl.class);
        bind(DownloadSerializeSettings.class).annotatedWith(Names.named("oldDownloadSettings")).to(OldDownloadSettings.class);
        bind(DownloadStatsTracker.class).to(DownloadStatsTrackerImpl.class);
        bind(new TypeLiteral<Comparator<RemoteFileDescContext>>(){}).to(PingedRemoteFileDescComparator.class);
    }
    
    @Provides @Singleton @Named("downloadStateProcessingQueue") ListeningExecutorService downloadStateProcessingQueue() {
        return ExecutorsHelper.newProcessingQueue("downloadStateProcessingQueue");
    }

}
