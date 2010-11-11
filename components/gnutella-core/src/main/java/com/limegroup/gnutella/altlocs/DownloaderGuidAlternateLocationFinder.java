package com.limegroup.gnutella.altlocs;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.listener.EventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.DownloadManagerEvent;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.dht.db.PushEndpointService;
import com.limegroup.gnutella.dht.db.SearchListener;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStateEvent;
import com.limegroup.gnutella.downloader.MagnetDownloader;

/**
 * Listens for {@link DownloadManagerEvent} and registers itself as an event listener
 * on {@link MagnetDownloader}. After registration and when a downloader goes into
 * {@link DownloadState#QUEUED}, it will peform a search for alternate locations
 * asking its {@link PushEndpointManagerImpl} for endpoints.
 * <p>
 * Push endpoints where the external address and port equal its only push proxy
 * are assumed to be non-firewalled hosts and a {@link DirectAltLoc} is created
 * for them. 
 */
@Singleton
public class DownloaderGuidAlternateLocationFinder implements EventListener<DownloadManagerEvent> {

    private static final Log LOG = LogFactory.getLog(DownloaderGuidAlternateLocationFinder.class);
    
    private final PushEndpointService pushEndpointManager;
    private final AlternateLocationFactory alternateLocationFactory;
    private final AltLocManager altLocManager;
    
    /**
     * Package access for testing.
     */
    final EventListener<DownloadStateEvent> downloadStatusListener = new EventListener<DownloadStateEvent>() { 
        public void handleEvent(DownloadStateEvent event) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("per download event received: " + event);
            }
            handleStatusEvent(event);
        }
    };
    
    @Inject
    public DownloaderGuidAlternateLocationFinder(@Named("pushEndpointManager") PushEndpointService pushEndpointManager, 
            AlternateLocationFactory alternateLocationFactory,
            AltLocManager altLocManager) {
        this.pushEndpointManager = pushEndpointManager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.altLocManager = altLocManager;
    }

    public void handleEvent(DownloadManagerEvent event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("event received: " + event);
        }
        switch (event.getType()) {
        case ADDED:
            CoreDownloader downloader = event.getData();
            if (downloader instanceof MagnetDownloader) {
                MagnetDownloader magnetDownloader = (MagnetDownloader)downloader;
                // subscribe for status events so we can search when waiting for user
                magnetDownloader.addListener(downloadStatusListener);
            }
            break;
        case REMOVED:
            downloader = event.getData();
            if (downloader instanceof MagnetDownloader) {
                downloader.removeListener(downloadStatusListener);
            }
            break;
        } 
    }
    
    private long getSize(MagnetDownloader downloader) {
        long size = downloader.getMagnet().getFileSize();
        if (size == -1) {
            size = downloader.getContentLength();
        }
        return size;
    }
    
    private void searchForPushEndpoints(MagnetDownloader magnetDownloader) {
        MagnetOptions magnet = magnetDownloader.getMagnet();
        URN sha1Urn = magnet.getSHA1Urn();
        if (sha1Urn == null) {
            LOG.debug("no sha1 urn");
            return;
        }
        long size = getSize(magnetDownloader);
        if (size == -1) {
            LOG.debug("no file size");
            return;
        }
        searchForPushEndpoints(sha1Urn, magnet.getGUIDUrns());
    }
    
    void handleStatusEvent(DownloadStateEvent event) {
        switch (event.getType()) {
        case GAVE_UP:
        case WAITING_FOR_USER:
            CoreDownloader downloader = event.getSource();
            if (downloader instanceof MagnetDownloader) {
                MagnetDownloader magnetDownloader = (MagnetDownloader)downloader;
                searchForPushEndpoints(magnetDownloader);
            }
			break;
        }
    }
    
    void searchForPushEndpoints(URN sha1Urn, Set<URN> guidUrns) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching for guid urns: " + guidUrns);
        }
        for (URN guidUrn : guidUrns) {
            try {
                GUID guid = new GUID(guidUrn.getNamespaceSpecificString());
                pushEndpointManager.findPushEndpoint(guid, new PushendpointHandler(sha1Urn));
            } catch (IllegalArgumentException iae) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("invalid hex string of guid", iae);
                }
            }
        }
    }

    private class PushendpointHandler implements SearchListener<PushEndpoint> {
        
        private final URN sha1;

        public PushendpointHandler(URN sha1Urn) {
            this.sha1 = sha1Urn;
        }

        public void handleResult(PushEndpoint result) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("endpoint found: " + result);
            }
            // TODO instantly create alternate locations for all sha1s that have the same guid urn as a source
            
            IpPort ipPort = result.getValidExternalAddress();
            // if the external address is the same as the push proxy, it's a non-firewalled source
            if (ipPort != null && result.getProxies().size() == 1 && result.getProxies().contains(ipPort)) {
                try {
                    LOG.debug("creating direct altloc");
                    AlternateLocation alternateLocation = alternateLocationFactory.createDirectAltLoc(ipPort, sha1);
                    // adding to alt loc manager will notify the downloader of the new alternate location
                    altLocManager.add(alternateLocation, null);
                } catch (IOException ie) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("error creating direct alt loc from " + ipPort, ie);
                    }
                }
            } else {
                LOG.debug("creating push altloc");
                AlternateLocation  alternateLocation = alternateLocationFactory.createPushAltLoc(result, sha1);
                // adding to alt loc manager will notify the downloader of the new alternate location
                altLocManager.add(alternateLocation, null);
            }
        }

        public void searchFailed() {
            // ignoring unsuccessful searches
        }
        
    }
}
