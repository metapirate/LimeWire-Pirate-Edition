package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.limegroup.bittorrent.TorrentFile;

public class SerialTorrentFileSystem implements Serializable {
    
    private static final long serialVersionUID = 6006838744525690869L;
    
    private String _name;
    private long _totalSize;
    private List<TorrentFile> _files;
    private Collection<File> _folders = new HashSet<File>();
    private File _incompleteFile;
    private File _completeFile;
 

    public String getName() {
        return _name;
    }
    public long getTotalSize() {
        return _totalSize;
    }
    public List<TorrentFile> getFiles() {
        return _files;
    }
    
    public Collection<File> getFolders() {
        return _folders;
    }
    public File getIncompleteFile() {
        return _incompleteFile;
    }
    public File getCompleteFile() {
        return _completeFile;
    }
    
    
}
