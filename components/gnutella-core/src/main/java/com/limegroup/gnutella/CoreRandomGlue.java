package com.limegroup.gnutella;

import java.io.File;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.lifecycle.ServiceStage;

import com.google.inject.Inject;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.DownloaderGuidAlternateLocationFinder;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.db.PushProxiesPublisher;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.licenses.LicenseFactory;
import com.limegroup.gnutella.tigertree.HashTreeCache;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;

/** Some glue that installs listeners & things. TODO: Figure out a better way to do this. */
@EagerSingleton
class CoreRandomGlue {

    private final Library library;
    private final ConnectionManager connectionManager;
    private final DHTManager dhtManager;
    private final PushProxiesPublisher pushProxiesPublisher;
    private final ConnectionServices connectionServices;
    private final DownloadManager downloadManager;
    private final DownloaderGuidAlternateLocationFinder downloaderGuidAlternateLocationFinder;
    private final SpamServices spamServices;
    private final HashTreeCache hashTreeCache;
    private final LicenseFactory licenseFactory;
    private final AltLocManager altLocManager;
    
    @Inject
    CoreRandomGlue(Library library,
            ConnectionManager connectionManager, DHTManager dhtManager,
            PushProxiesPublisher pushProxiesPublisher,
            ConnectionServices connectionServices,
            DownloadManager downloadManager, 
            DownloaderGuidAlternateLocationFinder downloaderGuidAlternateLocationFinder,
            SpamServices spamServices,
            LicenseFactory licenseFactory,
            HashTreeCache hashTreeCache,
            SchemaReplyCollectionMapper schemaMapper,
            AltLocManager altLocManager) {
        this.library = library;
        this.connectionManager = connectionManager;
        this.dhtManager = dhtManager;
        this.pushProxiesPublisher = pushProxiesPublisher;
        this.connectionServices = connectionServices;
        this.downloadManager = downloadManager;
        this.downloaderGuidAlternateLocationFinder = downloaderGuidAlternateLocationFinder;
        this.spamServices = spamServices;
        this.hashTreeCache = hashTreeCache;
        this.licenseFactory = licenseFactory;
        this.altLocManager = altLocManager;
    }
    
    @SuppressWarnings({"unused", "UnusedDeclaration"})
    @Inject private void register(ServiceRegistry registry) {
        registry.register(new Service() {            
            public void initialize() {
                //TODO: find a better way to do this
                library.addListener(altLocManager);
                
                connectionManager.addEventListener(dhtManager);
                dhtManager.addEventListener(pushProxiesPublisher);
                downloadManager.addListener(downloaderGuidAlternateLocationFinder);
            }
            
            public void start() {
                spamServices.reloadSpamFilters();
            }
            
            public void stop() {
                downloadManager.removeListener(downloaderGuidAlternateLocationFinder);
            }
            
            public String getServiceName() {
                return I18nMarker.marktr("Various Core Services");
            }
        }).in("EarlyBackground");
        registry.register(new Service() {
            public String getServiceName() {
                return org.limewire.i18n.I18nMarker.marktr("Gnutella Connections");
            }
            public void initialize() {
            }
            public void start() {                
                if(ConnectionSettings.CONNECT_ON_STARTUP.getValue()) {
                    // Make sure connections come up ultra-fast (beyond default keepAlive)      
                    int outgoing = ConnectionSettings.NUM_CONNECTIONS.getValue();
                    if ( outgoing > 0 ) {
                        connectionServices.connect();
                    }
                }
            }
            public void stop() {
                hashTreeCache.persistCache(library, downloadManager);
                licenseFactory.persistCache();
                
                cleanupPreviewFiles();                
                cleanupTorrentMetadataFiles();
            }            
        }).in(ServiceStage.LATE);
    }
    
    
    private void cleanupTorrentMetadataFiles() {
    }

    /** Deletes all preview files. */
    private void cleanupPreviewFiles() {
        //Cleanup any preview files.  Note that these will not be deleted if
        //your previewer is still open.
        File incompleteDir = SharingSettings.INCOMPLETE_DIRECTORY.get();
        if (incompleteDir == null)
            return; // if we could not get the incomplete directory, simply return.
        
        
        File[] files = incompleteDir.listFiles();
        if(files == null)
            return;
        
        for (int i=0; i<files.length; i++) {
            String name = files[i].getName();
            if (name.startsWith(IncompleteFileManager.PREVIEW_PREFIX))
                files[i].delete();  //May or may not work; ignore return code.
        }
    }
    
    
}
