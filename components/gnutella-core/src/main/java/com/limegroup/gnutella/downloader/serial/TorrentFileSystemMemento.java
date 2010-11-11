package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.limegroup.bittorrent.TorrentFile;

public interface TorrentFileSystemMemento {

    String getName();

    long getTotalSize();

    List<TorrentFile> getFiles();

    Collection<File> getFolders();

    File getIncompleteFile();

    File getCompleteFile();

    void setCompleteFile(File file);

    void setFiles(List<TorrentFile> _files);

    void setFolders(Collection<File> _folders);

    void setIncompleteFile(File file);

    void setName(String _name);

    void setTotalSize(long size);

}
