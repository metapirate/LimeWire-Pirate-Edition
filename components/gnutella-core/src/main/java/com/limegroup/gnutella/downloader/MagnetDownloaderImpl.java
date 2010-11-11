package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.network.BandwidthCollector;
import org.limewire.io.InvalidDataException;
import org.limewire.net.SocketsManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.MagnetDownloadMemento;
import com.limegroup.gnutella.downloader.serial.MagnetDownloadMementoImpl;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.library.UrnCache;
import com.limegroup.gnutella.malware.DangerousFileChecker;
import com.limegroup.gnutella.malware.VirusScanner;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.spam.SpamManager;
import com.limegroup.gnutella.tigertree.HashTreeCache;
import com.limegroup.gnutella.util.QueryUtils;

/**
 * A ManagedDownloader for MAGNET URIs.  Unlike a ManagedDownloader, a
 * MagnetDownloader needs not have an initial RemoteFileDesc.  Instead it can be
 * started with various combinations of the following:
 * <ul>
 * <li>initial URL (exact source)
 * <li>hash/URN (exact topic)
 * <li>file name (display name)
 * <li>search keywords (keyword topic)
 * </ul>
 * Names in parentheses are those given by the MAGNET specification at
 * http://magnet-uri.sourceforge.net/magnet-draft-overview.txt
 * <p>
 * Implementation note: this uses ManagedDownloader to try the initial download
 * location.  Unfortunately ManagedDownloader requires RemoteFileDesc's.  We can
 * fake up most of the RFD fields, but size presents problems.
 * ManagedDownloader depends on size for swarming purposes.  It is possible to
 * redesign the swarming algorithm to work around the lack of size, but this is
 * complex, especially with regard to HTTP/1.1 swarming.  For this reason, we
 * simply make a HEAD request to get the content length before starting the
 * download.  
 */
class MagnetDownloaderImpl extends ManagedDownloaderImpl implements MagnetDownloader {
    
    private static final Log LOG = LogFactory.getLog(MagnetDownloaderImpl.class);
        
	private MagnetOptions magnet;
	
	/**
	 * Boolean to keep track of whether default urls were already tried. Ensures
	 * that they are only tried once per session.
	 */
	private final AtomicBoolean triedDefaultUrls = new AtomicBoolean(false);

	/**
     * Creates a new MAGNET downloader.  Immediately tries to download from
     * <tt>defaultURLs</tt>, if specified. If that fails, or if defaultURLs does
     * not provide alternate locations, issues a requery with <tt>textQuery</tt>
     * and </tt>urn</tt>, as provided.  (Note that at least one must be
     * non-null.)  If <tt>filename</tt> is specified, it will be used as the
     * name of the complete file; otherwise it will be taken from any search
     * results or guessed from <tt>defaultURLs</tt>.
     * @param magnet contains all the information for the download, must be
     * {@link MagnetOptions#isGnutellaDownloadable() downloadable}.
     * @param overwrite whether file at download location should be overwritten
     * @param saveDir can be null, then the default save directory is used
     * @param fileName the final file name, can be <code>null</code>
     *
     * @throws DownloadException if there was an error setting the downloads
     * final file location 
     */
	@Inject
	MagnetDownloaderImpl(SaveLocationManager saveLocationManager,
	        DownloadManager downloadManager,
	        @GnutellaFiles FileCollection gnutellaFileCollection,
	        IncompleteFileManager incompleteFileManager,
	        DownloadCallback downloadCallback,
	        NetworkManager networkManager,
	        AlternateLocationFactory alternateLocationFactory,
	        RequeryManagerFactory requeryManagerFactory,
	        QueryRequestFactory queryRequestFactory,
	        OnDemandUnicaster onDemandUnicaster,
	        DownloadWorkerFactory downloadWorkerFactory,
	        AltLocManager altLocManager,
	        SourceRankerFactory sourceRankerFactory,
	        UrnCache urnCache,
	        VerifyingFileFactory verifyingFileFactory,
	        DiskController diskController, 
	        IPFilter ipFilter,
	        @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
	        Provider<MessageRouter> messageRouter,
	        Provider<HashTreeCache> tigerTreeCache,
	        ApplicationServices applicationServices,
	        RemoteFileDescFactory remoteFileDescFactory,
	        Provider<PushList> pushListProvider,
	        SocketsManager socketsManager, 
	        @Named("downloadStateProcessingQueue") ListeningExecutorService downloadStateProcessingQueue,
	        DangerousFileChecker dangerousFileChecker,
	        VirusScanner virusScanner,
            SpamManager spamManager,
            Library library,
            CategoryManager categoryManager,
            BandwidthCollector bandwidthCollector) {
	    super(saveLocationManager, downloadManager, gnutellaFileCollection,
	            incompleteFileManager, downloadCallback, networkManager,
	            alternateLocationFactory, requeryManagerFactory,
	            queryRequestFactory, onDemandUnicaster, downloadWorkerFactory,
	            altLocManager, sourceRankerFactory, urnCache, 
	            verifyingFileFactory, diskController, ipFilter,
	            backgroundExecutor, messageRouter, tigerTreeCache,
	            applicationServices, remoteFileDescFactory, pushListProvider,
	            socketsManager, downloadStateProcessingQueue,
	            dangerousFileChecker, virusScanner, spamManager, library,
	            categoryManager, bandwidthCollector);
    }
    
    @Override
    public void initialize() {
        MagnetOptions magnet = getMagnet();
        assert (magnet != null);
        URN sha1 = magnet.getSHA1Urn();
        if(sha1 != null)
            setSha1Urn(sha1);
        long size = magnet.getFileSize();
        if (size != -1) {
            setContentLength(size);
        }
        super.initialize();
        // activate requery manager to get urn lookups for magnet downloads
        requeryManager.activate();
    }

	public synchronized MagnetOptions getMagnet() {
	    return magnet;
	}
	
	public synchronized void setMagnet(MagnetOptions magnet) {
        if(getMagnet() != null)
            throw new IllegalStateException("magnet already set!");
        this.magnet = magnet;
	}
    
    /**
     * overrides ManagedDownloader to ensure that we issue requests to the known
     * locations until we find out enough information to start the download 
     */
    @Override
    protected DownloadState initializeDownload() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Initializing magnet download for: " + magnet);
        }
        
        // ask ranker since the alt loc manager might have added extra alt locs
        // which were added to the ranker then
        SourceRanker ranker = getSourceRanker();
        boolean hasMore = ranker != null && ranker.hasMore();
		if (!hasRFD() && !hasMore && triedDefaultUrls.compareAndSet(false, true)) {
			MagnetOptions magnet = getMagnet();
			String[] defaultURLs = magnet.getDefaultURLs();
			
			boolean foundSource = false;
			long fileSize = magnet.getFileSize();
			for (int i = 0; i < defaultURLs.length; i++) {
				try {
				    RemoteFileDesc rfd = createRemoteFileDesc(defaultURLs[i],
													 getSaveFile().getName(), magnet.getSHA1Urn(), fileSize);
				    // update size in case it was -1, to save HEAD requests
				    // for the following urls
				    fileSize = rfd.getSize();
					initPropertiesMap(rfd);
					addDownloadForced(rfd, true);
					foundSource = true;
				} catch (IOException e) {
				    LOG.warn("error", e);
				} catch (URISyntaxException e) {
				    LOG.warn("error", e);
				}
            }
        
			// if all locations included in the magnet URI fail we can't do much
			if (!foundSource)
				return DownloadState.GAVE_UP;
		}
        return super.initializeDownload();
    }
    
    /** 
     * Creates a faked-up RemoteFileDesc to pass to ManagedDownloader.  If a URL
     * is provided and fileSize is -1, it issues a HEAD request to get the file size.  If this fails,
     * returns null.
     * <p>
     * Protected and non-static so it can be overridden in tests.
     * </p>
     * <p>
     * NOTE: this calls HTTPUtils.contentLength which opens a URL and calls Head on the
     * link to determine the file length. This is a blocking call!
     * </p>
     */
    private RemoteFileDesc createRemoteFileDesc(String defaultURL,
        String filename, URN urn, long fileSize)
            throws IOException, URISyntaxException {
        return remoteFileDescFactory.createUrlRemoteFileDesc(new URL(defaultURL), filename, urn, fileSize);
    } 

    ////////////////////////////// Requery Logic ///////////////////////////

    /** 
     * Overrides ManagedDownloader to use the query words 
     * specified by the MAGNET URI.
     */
    @Override
    public QueryRequest newRequery()
        throws CantResumeException {
        MagnetOptions magnet = getMagnet();
		String textQuery = magnet.getQueryString();
        if (textQuery != null) {
            String q = QueryUtils.createQueryString(textQuery);
            return queryRequestFactory.createQuery(q);
        }
        else {
            String q = QueryUtils.createQueryString(getSaveFile().getName());
            return queryRequestFactory.createQuery(q);
        }
    }

    /** 
     * Overrides ManagedDownloader to allow any files with the right
     * hash even if this doesn't currently have any download
     * locations.  
     * <p>
     * We only allow for additions if the download has a sha1.  
     */
    @Override
    protected boolean allowAddition(RemoteFileDesc other) {        
        // Allow if we have a hash and other matches it.
		URN otherSHA1 = other.getSHA1Urn();
		if (getSha1Urn() != null && otherSHA1 != null) {
			return getSha1Urn().equals(otherSHA1);
        }
        return false;
    }

    /**
	 * Overridden for internal purposes, returns result from super method
	 * call.
     */
	@Override
    protected synchronized boolean addDownloadForced(RemoteFileDesc rfd,
													 boolean cache) {
		if (!hasRFD())
			initPropertiesMap(rfd);
		return super.addDownloadForced(rfd, cache);
	}

    /**
	 * Only allow requeries when <code>downloadSHA1</code> is not null.
     */
	@Override
    protected boolean canSendRequeryNow() {
		return getSha1Urn() != null ? super.canSendRequeryNow() : false;
	}

	/**
	 * Overridden to make sure it calls the super method only if 
	 * the filesize is known.
	 */
	@Override
    protected void initializeIncompleteFile() throws IOException {
		if (getContentLength() != -1) {
			super.initializeIncompleteFile();
		}
	}
    
    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.MAGNET;
    }
    
    @Override
    public synchronized void initFromMemento(DownloadMemento memento) throws InvalidDataException {
        super.initFromMemento(memento);
        MagnetDownloadMemento mmem = (MagnetDownloadMemento)memento;
        setMagnet(mmem.getMagnet());
        
    }
    
    @Override
    protected void fillInMemento(DownloadMemento memento) {
        super.fillInMemento(memento);
        MagnetDownloadMemento mmem = (MagnetDownloadMemento)memento;
        mmem.setMagnet(getMagnet());
    }
    
    @Override
    protected DownloadMemento createMemento() {
        return new MagnetDownloadMementoImpl();
    }
}
