package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.limegroup.bittorrent.TorrentFile;

public class TorrentFileSystemMementoImpl implements TorrentFileSystemMemento, Serializable {
    
    private Map<String, Object> serialObjects = new HashMap<String, Object>();

    public File getCompleteFile() {
        return (File)serialObjects.get("completeFile");
    }

    @SuppressWarnings("unchecked")
    public List<TorrentFile> getFiles() {
        return (List<TorrentFile>)serialObjects.get("files");
    }

    @SuppressWarnings("unchecked")
    public Collection<File> getFolders() {
        return (Collection<File>)serialObjects.get("folders");
    }

    public File getIncompleteFile() {
        return (File)serialObjects.get("incompleteFile");
    }

    public String getName() {
        return (String)serialObjects.get("name");
    }

    public long getTotalSize() {
        Long l = (Long)serialObjects.get("totalSize");
        if(l == null)
            return 0;
        else
            return l;
    }

    public void setCompleteFile(File file) {
        serialObjects.put("completeFile", file);
    }

    public void setFiles(List<TorrentFile> files) {
        serialObjects.put("files", files);
    }

    public void setFolders(Collection<File> folders) {
        serialObjects.put("folders", folders);
    }

    public void setIncompleteFile(File file) {
        serialObjects.put("incompleteFile", file);
    }

    public void setName(String name) {
        serialObjects.put("name", name);
    }

    public void setTotalSize(long size) {
        serialObjects.put("totalSize", size);
    }

}
