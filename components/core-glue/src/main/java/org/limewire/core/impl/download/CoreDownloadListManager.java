package org.limewire.core.impl.download;

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.DownloadItem.DownloadItemType;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.impl.download.listener.ItunesDownloadListenerFactory;
import org.limewire.core.impl.download.listener.RecentDownloadListener;
import org.limewire.core.impl.download.listener.TorrentDownloadListenerFactory;
import org.limewire.core.impl.magnet.MagnetLinkImpl;
import org.limewire.core.impl.search.CoreSearch;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.core.settings.SharingSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.lifecycle.ServiceScheduler;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.setting.FileSetting;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.impl.ThreadSafeList;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.malware.VirusDefinitionDownloader;

@EagerSingleton
public class CoreDownloadListManager implements DownloadListManager {
    
	private final EventList<DownloadItem> observableDownloadItems;
	private EventList<DownloadItem> swingThreadDownloadItems;
	private final DownloadManager downloadManager;
	private final RemoteFileDescFactory remoteFileDescFactory;
    private final SpamManager spamManager;
    private final ItunesDownloadListenerFactory itunesDownloadListenerFactory;
    private final TorrentDownloadListenerFactory torrentDownloadListenerFactory;
    private final CoreDownloadItem.Factory coreDownloadItemFactory;
    
    private final PropertyChangeSupport changeSupport = new SwingSafePropertyChangeSupport(this);
    /**the base list - all removing and adding must be done from here.*/
    private final ThreadSafeList<DownloadItem> threadSafeDownloadItems;
    
    private static final int PERIOD = 1000;
    
    private Map<org.limewire.core.api.URN, DownloadItem> urnMap = Collections.synchronizedMap(new HashMap<org.limewire.core.api.URN, DownloadItem>());
    private final DownloadItemFactoryRegistry downloadItemFactoryRegistry;
    private final CategoryManager categoryManager;
	
	@Inject
	public CoreDownloadListManager(DownloadManager downloadManager,
            RemoteFileDescFactory remoteFileDescFactory, SpamManager spamManager, 
            ItunesDownloadListenerFactory itunesDownloadListenerFactory, 
            TorrentDownloadListenerFactory torrentDownloadListenerFactory,
            CoreDownloadItem.Factory coreDownloadItemFactory,
            DownloadItemFactoryRegistry downloadItemFactoryRegistry,
            CategoryManager categoryManager) {
	    
	    this.downloadManager = downloadManager;
	    this.remoteFileDescFactory = remoteFileDescFactory;
        this.spamManager = spamManager;
        this.itunesDownloadListenerFactory = itunesDownloadListenerFactory;
        this.torrentDownloadListenerFactory = torrentDownloadListenerFactory;
        this.coreDownloadItemFactory = coreDownloadItemFactory;
        this.downloadItemFactoryRegistry = downloadItemFactoryRegistry;
        this.categoryManager = categoryManager;
        
        threadSafeDownloadItems = GlazedListsFactory.threadSafeList(new BasicEventList<DownloadItem>());
	    ObservableElementList.Connector<DownloadItem> downloadConnector = GlazedLists.beanConnector(DownloadItem.class);
	    observableDownloadItems = GlazedListsFactory.observableElementList(threadSafeDownloadItems, downloadConnector);
	}
	
	@Inject 
	void registerDownloadListener(DownloadListenerList listenerList) {
	    
	    listenerList.addDownloadListener(new CoreDownloadListener(threadSafeDownloadItems,
                new QueueTimeCalculator(observableDownloadItems)));
	    
	}
	
	@Inject 
    void registerService(ServiceScheduler scheduler, @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {

	      Runnable command = new Runnable() {
              @Override
              public void run() {
                  update();
              }
          };
     
          scheduler.scheduleWithFixedDelay("UI Download Status Monitor", command, PERIOD*2, PERIOD, TimeUnit.MILLISECONDS, backgroundExecutor);
	}

    // forces refresh
    private void update() {
        observableDownloadItems.getReadWriteLock().writeLock().lock();
        try {
            // TODO use TransactionList for these for performance (requires using GlazedLists from head)
            for (DownloadItem item : observableDownloadItems) {
                if ((!item.getState().isFinished()) && item instanceof CoreDownloadItem)
                    ((CoreDownloadItem) item).fireDataChanged();
            }
        } finally {
            observableDownloadItems.getReadWriteLock().writeLock().unlock();
        }
    }
    
	@Override
	public EventList<DownloadItem> getDownloads() {
		return observableDownloadItems;
	}
	
	@Override
	public EventList<DownloadItem> getSwingThreadSafeDownloads() {
	    assert EventQueue.isDispatchThread();
	    if(swingThreadDownloadItems == null) {
	        swingThreadDownloadItems = GlazedListsFactory.swingThreadProxyEventList(observableDownloadItems);
	    }
	    return swingThreadDownloadItems;
	}

	@Override
	public DownloadItem addDownload(Search search, List<? extends SearchResult> searchResults) throws DownloadException {
	   return addDownload(search, searchResults, null, false);
	}
	

    @Override
    public DownloadItem addDownload(Search search, List<? extends SearchResult> searchResults,
            File saveFile, boolean overwrite) throws DownloadException {

        // do it before spam marking, since those search results might not support spam marking
        DownloadItem downloadItem = downloadItemFactoryRegistry.create(search, searchResults, saveFile, overwrite);
        if (downloadItem != null) {
            return downloadItem;
        }

        // Train the spam filter even if the results weren't rated as spam
        spamManager.handleUserMarkedGood(searchResults);
        
        RemoteFileDesc[] files;
        List<RemoteFileDesc> alts = new ArrayList<RemoteFileDesc>();
        files = createRfdsAndAltsFromSearchResults(searchResults, alts);
        
        GUID queryGUID = null;
        if(search != null && search instanceof CoreSearch) {
            queryGUID = ((CoreSearch)search).getQueryGuid();
        }
        
        Category category = searchResults.iterator().next().getCategory();
        return createDownloader(files, alts, queryGUID, saveFile, overwrite, category);
    }
	
	private DownloadItem createDownloader(RemoteFileDesc[] files, List<RemoteFileDesc> alts,
                                          GUID queryGuid, File saveFile, boolean overwrite, Category category)
            throws DownloadException {
        
        File saveDir = null;
        String fileName = null;
        
        if(saveFile != null) {
            if(saveFile.isDirectory()) {
                saveDir = saveFile;
            } else {
                saveDir = saveFile.getParentFile();
                fileName = saveFile.getName();
            }
        }

        // determine per mediatype directory if saveLocation == null
        // and only pass it through if directory is different from default
        // save directory == !isDefault()
        //if (saveDir == null &&) {
            FileSetting fs = SharingSettings.getFileSettingForCategory(category);
            if (!fs.isDefault()) {
                saveDir = fs.get();
            }
       // }
        Downloader downloader = downloadManager.download(files, alts, queryGuid, overwrite, saveDir, fileName);
        // This should have been funneled through our addDownload callback method, which
        // should have set the CoreDownloadItem.
        return (DownloadItem)downloader.getAttribute(DownloadItem.DOWNLOAD_ITEM);
    }

	private RemoteFileDesc[] createRfdsAndAltsFromSearchResults(
            List<? extends SearchResult> searchResults, List<RemoteFileDesc> altList) {
	    List<RemoteFileDesc> rfds = new ArrayList<RemoteFileDesc>(searchResults.size());
        Set<IpPort> alts = new IpPortSet();
        
        // size of searchResults can change, so iterate over list and make no
        // assumptions about its size
        for (SearchResult result : searchResults) {
            RemoteFileDescAdapter rfdAdapter = (RemoteFileDescAdapter) result;
            rfds.add(rfdAdapter.getRfd());
            alts.addAll(rfdAdapter.getAlts());
        }        
        
        // Iterate through RFDs and remove matching alts.
        // Also store the first SHA1 capable RFD for collecting alts.
        RemoteFileDesc sha1RFD = null;
        for(RemoteFileDesc next : rfds) {
            if(next.getSHA1Urn() != null)
                sha1RFD = next;
            Address address = next.getAddress();
            // response alts are always only ip ports and no kind of other address
            // so it suffices to compare ip port instances of rfd addresses with the alt set
            // since other address types won't match anyways
            if (address instanceof IpPort) 
                alts.remove(address); // Removes an alt that matches the IpPort of the RFD
        }

        // If no SHA1 rfd, just use the first.
        if(sha1RFD == null)
            sha1RFD = rfds.get(0);
        
        // Now iterate through alts & add more rfds.
        for(IpPort next : alts) {
            altList.add(remoteFileDescFactory.createRemoteFileDesc(sha1RFD, next));
        }
        
        return rfds.toArray(new RemoteFileDesc[rfds.size()]);
    }

	/**
	 * Adds the specified listener to the list that is notified when a 
	 * property value changes.  Listeners added from the Swing UI thread will 
	 * always receive notification events on the Swing UI thread. 
	 */
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes the specified listener from the list that is notified when a 
     * property value changes.
     */
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    /**
     * Checks for downloads in progress, and fires a property change event if
     * all downloads are completed.
     */
    @Override
    public void updateDownloadsCompleted() {
        if (downloadManager.downloadsInProgress() == 0) {
            changeSupport.firePropertyChange(DOWNLOADS_COMPLETED, false, true);
        }
    }

    class CoreDownloadListener implements DownloadListener {
	    
	    private final List<DownloadItem> list;
	    private final QueueTimeCalculator queueTimeCalculator;

        public CoreDownloadListener(List<DownloadItem> list, QueueTimeCalculator queueTimeCalculator){
	        this.list = list;
	        this.queueTimeCalculator = queueTimeCalculator;
	    }

        @Override
        public void downloadAdded(Downloader downloader) {
            // don't show currentDefinitionAVGCheck
            if(isVirusScannerCurrentInfoDownload(downloader))
                return;
            //Save the starting time if it hasn't been set
            if(downloader.getAttribute(DownloadItem.DOWNLOAD_START_DATE)== null){
                downloader.setAttribute(DownloadItem.DOWNLOAD_START_DATE, new Date(), true);
            }
            DownloadItem item = coreDownloadItemFactory.create(downloader, queueTimeCalculator);
            downloader.setAttribute(DownloadItem.DOWNLOAD_ITEM, item, false);
            
            if (categoryManager.getCategoryForFile(downloader.getFile()) == Category.TORRENT) {
                downloader.addListener(torrentDownloadListenerFactory.createListener(downloader, list));
            }            
            
            downloader.addListener(new RecentDownloadListener(downloader));
            downloader.addListener(itunesDownloadListenerFactory.createListener(downloader));
            threadSafeDownloadItems.add(item);
            changeSupport.firePropertyChange(DOWNLOAD_ADDED, false, true);
            URN urn = item.getUrn();
            if(urn != null) {
                //the bittorrent File Downloader can have a null urn
                urnMap.put(item.getUrn(), item);
            }
        }

        @Override
        public void downloadRemoved(Downloader downloader) {
            // don't show currentDefinitionAVGCheck
            if(isVirusScannerCurrentInfoDownload(downloader))
                return;
            DownloadItem item = getDownloadItem(downloader);            
            DownloadState state = item.getState();            
            if (state.isFinished()) {
                changeSupport.firePropertyChange(DOWNLOAD_COMPLETED, null, item);
            }
            
            // Always remove anti-virus update item.  For all others, remove
            // from list UNLESS (a) download is in error state, (b) download
            // is removed for protection, or (c) download is finished and 
            // auto-clear is not set.
            if (item.getDownloadItemType() == DownloadItemType.ANTIVIRUS) {
                remove(item);
            } else if (state != DownloadState.ERROR && 
                    state != DownloadState.DANGEROUS &&
                    state != DownloadState.THREAT_FOUND &&
                    (SharingSettings.CLEAR_DOWNLOAD.getValue() || !state.isFinished())) {
                remove(item);
            }
        }
        
        @Override
        public void downloadsCompleted() {
            changeSupport.firePropertyChange(DOWNLOADS_COMPLETED, false, true);
        }

        DownloadItem getDownloadItem(Downloader downloader) {
            DownloadItem item = (DownloadItem)downloader.getAttribute(DownloadItem.DOWNLOAD_ITEM);
            return item;
        }
                
        /**
         * Returns true if this is a VirusDefinitionCheck download, false otherwise. 
         * This download checks if Virus Definitions are up to date. No need 
         * to show this to the user as this will usually return true. If this download
         * returns false, the user will see an AVG update download.
         */
        private boolean isVirusScannerCurrentInfoDownload(Downloader downloader) {
            if(downloader instanceof VirusDefinitionDownloader) {
                VirusDefinitionDownloader virusDownload = (VirusDefinitionDownloader) downloader;
                if(virusDownload.getFile().getName().equals("current.nfo"))
                    return true;
            }
            return false;
        }
    }

    @Override
    public DownloadItem addTorrentDownload(URI uri, boolean overwrite) throws DownloadException {
        Downloader downloader = downloadManager.downloadTorrent(uri, overwrite);
        return (DownloadItem)downloader.getAttribute(DownloadItem.DOWNLOAD_ITEM);
    }
    
    @Override
    public DownloadItem addTorrentDownload(String name, URN sha1, List<URI> trackers) throws DownloadException {
        Downloader downloader = downloadManager.downloadTorrent(name, 
                (com.limegroup.gnutella.URN)sha1,
                trackers);
        
        return (DownloadItem)downloader.getAttribute(DownloadItem.DOWNLOAD_ITEM);
    }
    
    @Override
    public DownloadItem addDownload(MagnetLink magnet, File saveFile, boolean overwrite) throws DownloadException {
        File saveDir = null;
        String fileName = null;
        
        if(saveFile != null) {
            if(saveFile.isDirectory()) {
                saveDir = saveFile;
            } else {
                saveDir = saveFile.getParentFile();
                fileName = saveFile.getName();
            }
        }
        MagnetOptions magnetOptions = ((MagnetLinkImpl)magnet).getMagnetOptions();
        Downloader downloader = downloadManager.download(magnetOptions, overwrite, saveDir, fileName);
        DownloadItem downloadItem = (DownloadItem)downloader.getAttribute(DownloadItem.DOWNLOAD_ITEM);
        return downloadItem;
    }

    @Override
    public DownloadItem addTorrentDownload(File file, File saveDirectory, boolean overwrite)
            throws DownloadException {
        Downloader downloader = downloadManager.downloadTorrent(file, saveDirectory, overwrite);
		return (DownloadItem)downloader.getAttribute(DownloadItem.DOWNLOAD_ITEM);
    }

    @Override
    public boolean contains(org.limewire.core.api.URN urn) {
        return urnMap.containsKey(urn);
    }
    
    @Override
    public DownloadItem getDownloadItem(org.limewire.core.api.URN urn) {
        return urnMap.get(urn);
    }

    @Override
    public void clearFinished() {
        final List<DownloadItem> finishedItems = new ArrayList<DownloadItem>();
        threadSafeDownloadItems.getReadWriteLock().writeLock().lock();
        try {
            for (DownloadItem item : threadSafeDownloadItems) {
                DownloadState state = item.getState();
                if (state.isFinished()) {
                    finishedItems.add(item);
                }
            }

            for(DownloadItem item : finishedItems) {
                remove(item);
            }
        } finally {
            threadSafeDownloadItems.getReadWriteLock().writeLock().unlock();
        }
    }

    @Override
    public void remove(DownloadItem item) {
        urnMap.remove(item.getUrn());
        threadSafeDownloadItems.remove(item);
    }

}