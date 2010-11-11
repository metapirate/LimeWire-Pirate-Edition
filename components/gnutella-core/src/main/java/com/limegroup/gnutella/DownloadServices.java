package com.limegroup.gnutella;

import java.io.File;
import java.util.List;

import org.limewire.core.api.download.DownloadException;
import org.limewire.io.GUID;

import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.CantResumeException;

public interface DownloadServices {

    /**
     * Starts a resume download for the given incomplete file.
     * @exception CantResumeException incompleteFile is not a valid 
     *  incomplete file
     * @throws DownloadException 
     */
    public Downloader download(File incompleteFile) throws CantResumeException,
            DownloadException;

    /**
     * Creates a downloader for a magnet using the given additional options.
     *
     * @param magnet provides the information of the  file to download, must be
     *  valid
     * @param overwrite whether an existing file a the final file location 
     * should be overwritten
     * @param saveDir can be null, then the save directory from the settings
     * is used
     * @param fileName the final filename of the download, can be
     * <code>null</code>
     * @return
     * @throws DownloadException
     * @throws IllegalArgumentException if the magnet is not
     * {@link MagnetOptions#isGnutellaDownloadable() downloadable}.
     */
    public Downloader download(MagnetOptions magnet, boolean overwrite,
            File saveDir, String fileName) throws DownloadException;

    /**
     * Creates a downloader for a magnet.
     * @param magnetprovides the information of the  file to download, must be
     *  valid
     * @param overwrite whether an existing file a the final file location 
     * should be overwritten
     * @return
     * @throws DownloadException
     * @throws IllegalArgumentException if the magnet is not 
     * {@link MagnetOptions#isGnutellaDownloadable() valid}.
     */
    public Downloader download(MagnetOptions magnet, boolean overwrite)
            throws DownloadException;

    public Downloader download(RemoteFileDesc[] files, boolean overwrite,
            GUID queryGUID) throws DownloadException;

    /**
     * Stub for calling download(RemoteFileDesc[], DataUtils.EMPTY_LIST, boolean)
     * @throws DownloadException 
     */
    public Downloader download(RemoteFileDesc[] files, GUID queryGUID,
            boolean overwrite, File saveDir, String fileName)
            throws DownloadException;

    public Downloader download(RemoteFileDesc[] files,
            List<? extends RemoteFileDesc> alts, GUID queryGUID,
            boolean overwrite) throws DownloadException;

    /** 
     * Tries to "smart download" <b>any</b> [sic] of the given files.<p>  
     *
     * If any of the files already being downloaded (or queued for downloaded)
     * has the same temporary name as any of the files in 'files', throws
     * DownloadException.  Note, however, that this doesn't guarantee
     * that a successfully downloaded file can be moved to the library.<p>
     *
     * If overwrite==false, then if any of the files already exists in the
     * download directory, DownloadException is thrown and no files are
     * modified.  If overwrite==true, the files may be overwritten.<p>
     * 
     * Otherwise returns a Downloader that allows you to stop and resume this
     * download.  The ActivityCallback will also be notified of this download,
     * so the return value can usually be ignored.  The download begins
     * immediately, unless it is queued.  It stops after any of the files
     * succeeds.  
     *
     * @param files a group of "similar" files to smart download
     * @param alts a List of secondary RFDs to use for other sources
     * @param queryGUID guid of the query that returned the results (i.e. files)
     * @param overwrite true iff the download should proceedewithout
     *  checking if it's on disk
     * @param saveDir can be null, then the save directory from the settings
     * is used
     * @param fileName can be null, then one of the filenames of the 
     * <code>files</code> array is used
     * array is used
     * @return the download object you can use to start and resume the download
     * @throws DownloadException if there is an error when setting the final
     * file location of the download 
     * @see DownloadManager#getFiles(RemoteFileDesc[], boolean)
     */
    public Downloader download(RemoteFileDesc[] files,
            List<? extends RemoteFileDesc> alts, GUID queryGUID,
            boolean overwrite, File saveDir, String fileName)
            throws DownloadException;

    /**
     * Returns whether there are any active internet (non-multicast) transfers
     * going at speed greater than 0.
     */
    public boolean hasActiveDownloads();
    
    /**
     * Returns the number of active downloads.
     */
    public int getNumActiveDownloads();

    /**
     * Returns the number of downloads in progress.
     */
    public int getNumDownloads();

}