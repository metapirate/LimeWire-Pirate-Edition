package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.util.URIUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.DownloaderType;

public class LibTorrentBTDownloadMementoImpl implements LibTorrentBTDownloadMemento, Serializable {

    private static final long serialVersionUID = 1160492348504657012L;

    private final Map<String, Object> serialObjects = new HashMap<String, Object>();

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAttributes() {
        return (Map<String, Object>) serialObjects.get("attributes");
    }

    @Override
    public String getDefaultFileName() {
        String defaultFileName = (String) serialObjects.get("defaultFileName");
        //Returning getName if defaultFileName is null. this is kind of backwards
        //but we were not serializing defaultFileName in the past so this is needed
        //to remain backwards compatible.
        return defaultFileName != null ? defaultFileName : getName();
    }

    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.BTDOWNLOADER;
    }

    @Override
    public File getSaveFile() {
        return (File) serialObjects.get("saveFile");
    }

    @Override
    public String getName() {
        return (String) serialObjects.get("name");
    }

    public File getIncompleteFile() {
        return (File)serialObjects.get("incompleteFile");
    }
    
    @Override
    public URN getSha1Urn() {
        URN sha1URN = null;
        String sha1URNString = (String) serialObjects.get("sha1Urn");
        if (sha1URNString != null) {
            try {
                sha1URN = URN.createSHA1Urn(sha1URNString);
            } catch (IOException e) {
                // invalid urn
            }
        }
        return sha1URN;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<URI> getTrackers() {
        Object trackersValue = serialObjects.get("trackers");
        if (trackersValue != null) {
            return (List<URI>) trackersValue;
        }
        
        String tracker = (String) serialObjects.get("trackerURL");

        try {
            return Collections.singletonList(URIUtils.toURI(tracker));
        } catch (URISyntaxException e) {
            // The tracker was invalid, have no other trackers
            return null;
        }
    }

    @Override
    public String getFastResumePath() {
        return (String) serialObjects.get("fastResumePath");
    }

    @Override
    public String getTorrentPath() {
        return (String) serialObjects.get("torrentPath");
    }

    @Override
    public void setAttributes(Map<String, Object> attributes) {
        serialObjects.put("attributes", attributes);
    }

    @Override
    public void setDefaultFileName(String defaultFileName) {
        serialObjects.put("defaultFileName", defaultFileName);
    }

    @Override
    public void setSaveFile(File saveFile) {
        serialObjects.put("saveFile", saveFile);
    }

    @Override
    public void setName(String name) {
        serialObjects.put("name", name);
    }

    @Override
    public void setIncompleteFile(File incompleteFile) {
        serialObjects.put("incompleteFile", incompleteFile);
    }
    
    @Override
    public void setSha1Urn(URN sha1Urn) {
        String sha1URNString = sha1Urn != null ? sha1Urn.toString() : null;
        serialObjects.put("sha1Urn", sha1URNString);
    }

    @Override
    public void setTrackers(List<URI> url) {
        serialObjects.put("trackers", url);
    }

    @Override
    public void setFastResumePath(String data) {
        serialObjects.put("fastResumePath", data);
    }

    @Override
    public void setTorrentPath(String torrentPath) {
        serialObjects.put("torrentPath", torrentPath);
    }

    @Override
    public void setDownloadType(DownloaderType downloaderType) {
        // not needed getDownloadType is overridden to always return
        // BTDownloader.
    }

    @Override
    public Boolean isPrivate() {
        Boolean isPublic = (Boolean)serialObjects.get("private");
        return isPublic;
    }

    @Override
    public void setPrivate(Boolean isPrivate) {
        serialObjects.put("private", isPrivate);
    }
}
