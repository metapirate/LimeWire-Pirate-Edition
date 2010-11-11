package com.limegroup.gnutella;

import java.io.File;
import java.util.List;

import org.limewire.core.api.download.DownloadException;
import org.limewire.io.GUID;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.CantResumeException;

@Singleton
public class DownloadServicesImpl implements DownloadServices {
    
    private final Provider<DownloadManager> downloadManager;
    
    @Inject
    public DownloadServicesImpl(Provider<DownloadManager> downloadManager) {
        this.downloadManager = downloadManager;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadServices#download(java.io.File)
     */ 
    public Downloader download(File incompleteFile)
            throws CantResumeException, DownloadException {
        return downloadManager.get().download(incompleteFile);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadServices#download(com.limegroup.gnutella.browser.MagnetOptions, boolean, java.io.File, java.lang.String)
     */
    public Downloader download(MagnetOptions magnet, boolean overwrite,
    		File saveDir, String fileName) throws DownloadException {
    	return downloadManager.get().download(magnet, overwrite, saveDir, fileName);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadServices#download(com.limegroup.gnutella.browser.MagnetOptions, boolean)
     */
    public Downloader download(MagnetOptions magnet, boolean overwrite) 
    	throws DownloadException {
    	if (!magnet.isGnutellaDownloadable()) {
    		throw new IllegalArgumentException("invalid magnet: not have enough information for downloading");
    	}
    	return downloadManager.get().download(magnet, overwrite, null, magnet.getDisplayName());
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadServices#download(com.limegroup.gnutella.RemoteFileDesc[], boolean, com.limegroup.gnutella.GUID)
     */
    public Downloader download(RemoteFileDesc[] files,
    								  boolean overwrite, GUID queryGUID) 
    	throws DownloadException {
    	return download(files, queryGUID, overwrite, null, null);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadServices#download(com.limegroup.gnutella.RemoteFileDesc[], com.limegroup.gnutella.GUID, boolean, java.io.File, java.lang.String)
     */
    public Downloader download(RemoteFileDesc[] files,
                                      GUID queryGUID, 
                                      boolean overwrite, File saveDir, String fileName)
    	throws DownloadException {
    	return download(files, RemoteFileDesc.EMPTY_LIST, queryGUID,
    			overwrite, saveDir, fileName);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadServices#download(com.limegroup.gnutella.RemoteFileDesc[], java.util.List, com.limegroup.gnutella.GUID, boolean)
     */
    public Downloader download(RemoteFileDesc[] files, 
    								  List<? extends RemoteFileDesc> alts,
    								  GUID queryGUID,
    								  boolean overwrite)
    	throws DownloadException {
    	return download(files, alts, queryGUID, overwrite, null, null);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadServices#download(com.limegroup.gnutella.RemoteFileDesc[], java.util.List, com.limegroup.gnutella.GUID, boolean, java.io.File, java.lang.String)
     */
    public Downloader download(RemoteFileDesc[] files, 
                                      List<? extends RemoteFileDesc> alts, GUID queryGUID,
                                      boolean overwrite, File saveDir,
    								  String fileName)
    	throws DownloadException {
    	return downloadManager.get().download(files, alts, queryGUID, overwrite, saveDir,
    							   fileName);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadServices#getNumActiveDownloads()
     */
    public int getNumActiveDownloads() {
        return downloadManager.get().getNumActiveDownloads();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadServices#getNumDownloads()
     */
    public int getNumDownloads() {
        return downloadManager.get().downloadsInProgress();
    }

    public boolean hasActiveDownloads() {
        downloadManager.get().measureBandwidth();
        return downloadManager.get().getMeasuredBandwidth() != 0;
    }

}
