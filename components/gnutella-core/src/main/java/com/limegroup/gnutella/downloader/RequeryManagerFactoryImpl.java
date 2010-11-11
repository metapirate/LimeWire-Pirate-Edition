package com.limegroup.gnutella.downloader;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.db.AltLocFinder;

@Singleton
public class RequeryManagerFactoryImpl implements RequeryManagerFactory {
    
    private final Provider<DownloadManager> downloadManager;
    private final Provider<AltLocFinder> altLocFinder;
    private final Provider<DHTManager> dhtManager;
    private final ConnectionServices connectionServices;

    @Inject
    public RequeryManagerFactoryImpl(Provider<DownloadManager> downloadManager,
            Provider<AltLocFinder> altLocFinder,
            Provider<DHTManager> dhtManager,
            ConnectionServices connectionServices) {
        this.downloadManager = downloadManager;
        this.altLocFinder = altLocFinder;
        this.dhtManager = dhtManager;
        this.connectionServices = connectionServices;
    }    

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.RequeryManagerFactory#createRequeryManager(com.limegroup.gnutella.downloader.ManagedDownloader)
     */
    public RequeryManager createRequeryManager(
            RequeryListener requeryListener) {
        return new RequeryManager(requeryListener, downloadManager.get(),
                altLocFinder.get(), dhtManager.get(), connectionServices);
    }
}
