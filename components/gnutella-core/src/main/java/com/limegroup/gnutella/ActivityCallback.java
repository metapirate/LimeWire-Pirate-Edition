package com.limegroup.gnutella;

import java.io.File;
import java.util.Set;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.i18n.I18nMarker;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 *  Defines the interface of a callback to notify about asynchronous backend 
 *  events. The methods in this interface fall into the following categories:
 *
 *  <ul>
 *  <li>Query replies (for displaying results) and query strings 
 *     (for the monitor)</li>
 *  <li>Update in shared file statistics</li>
 *  <li>Change of connection state</li>
 *  <li>New or dead uploads or downloads</li>
 *  <li>New chat requests and chat messages</li>
 *  <li>Error messages</li>
 *  </ul>
 */
public interface ActivityCallback extends DownloadCallback
{
    
    /**
     * Notifies the UI that a new query result has come in to the backend.
     * 
     * @param rfd the descriptor for the remote file
     * @param queryReply
     * @param locs the <tt>Set</tt> of alternate locations for the file
     */
	public void handleQueryResult(RemoteFileDesc rfd, QueryReply queryReply, Set<? extends IpPort> locs);

    /**
     * Add a query string to the monitor screen
     */
    public void handleQuery(QueryRequest query, String address, int port);

    /** Add an uploader to the upload window */
    public void addUpload(Uploader u);

    /** Remove an uploader from the upload window. */
    public void uploadComplete(Uploader u);    

    public void handleSharedFileUpdate(File file);
    
    /** 
     * Notifies that all active uploads have been completed.
     */  
    public void uploadsComplete();

	/**
	 *  Tell to deiconify.
	 */
	public void restoreApplication();

    /**
     * @return true If the <code>guid</code> that maps to a query result screen 
     * is still available/viewable to the user.
     */
    public boolean isQueryAlive(GUID guid);
    
    /** Notification that installation may be corrupted. */
    public void installationCorrupted();
	
	/**
	 * The core passes parsed magnets to the callback and asks it if it wants
	 * to handle them itself.
	 * <p>
	 * If this is the case the callback should return <code>true</code>, otherwise
	 * the core starts the downloads itself.
	 * @param magnets Array of magnet information to handle
	 */
	public void handleMagnets(MagnetOptions[] magnets);

    /** Try to download the torrent file */
	public void handleTorrent(File torrentFile);

    /**
     * Translate a String taking into account Locale.
     * 
     * String literals that should be translated must still be marked for
     * translation using {@link I18nMarker#marktr(String)}.
     * 
     * @param s The String to translate
     * @return the translated String
     */
    public String translate(String s);
    
    /**
     * Handles the supplied DownloadException, for example by prompting the
     * user for a new save location or whether to overwrite the file. 
     */
    void handleDownloadException(DownloadAction downLoadAction, DownloadException e, boolean supportsNewSaveDir);

    /**
     * Asks the user whether to continue with a torrent if scanning the torrent file failed.
     * @return true if the download should continue.
     */
    boolean promptAboutTorrentDownloadWithFailedScan();
    
}
