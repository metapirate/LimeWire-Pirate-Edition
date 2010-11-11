package com.limegroup.gnutella;

import java.io.File;
import java.util.Set;

import org.limewire.bittorrent.Torrent;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;

import com.google.inject.Singleton;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * Provides a default implementation of <code>ActivityCallback</code> where
 * all the methods are either empty or return <code>false</code>. You can extend 
 * this class when you are only need specific methods.
 */
@Singleton
public class ActivityCallbackAdapter implements ActivityCallback {

    @Override
    public void addUpload(Uploader u) {
        
    }

    @Override
    public void handleMagnets(MagnetOptions[] magnets) {

    }

    @Override
    public void handleQueryResult(RemoteFileDesc rfd, QueryReply queryReply,
            Set<? extends IpPort> locs) {
        
    }

    @Override
    public void handleQuery(QueryRequest query, String address, int port) {
        
    }

    @Override
    public void handleSharedFileUpdate(File file) {
        
    }

    @Override
    public void handleTorrent(File torrentFile) {
        
    }

    @Override
    public void installationCorrupted() {
        
    }

    @Override
    public boolean isQueryAlive(GUID guid) {
        return false;
    }

    @Override
    public void uploadComplete(Uploader u) {
        
    }

    @Override
    public void restoreApplication() {
        
    }

    @Override
    public void uploadsComplete() {
        
    }

    @Override
    public void addDownload(Downloader d) {
        
    }

    @Override
    public void downloadsComplete() {
        
    }

    @Override
    public void promptAboutUnscannedPreview(Downloader dloader) {
        dloader.discardUnscannedPreview(false);
    }
    
    @Override
    public void removeDownload(Downloader d) {
        
    }

    @Override
    public String translate(String s) {
        return s;
    }

    @Override
    public void handleDownloadException(DownloadAction downLoadAction,
            DownloadException e, boolean supportsNewSaveDir) {
    }

    @Override
    public boolean promptTorrentFilePriorities(Torrent torrent) {
        return true;
    }

    @Override
    public boolean promptAboutTorrentWithBannedExtensions(Torrent torrent, Set<String> bannedExtensions) {
        return true;
    }

    @Override
    public boolean promptAboutTorrentDownloadWithFailedScan() {
        return true;
    }
}
