package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.io.IOException;
import java.util.List;


public interface OldDownloadConverter {
    
    public List<DownloadMemento> readAndConvertOldDownloads(File inputFile) throws IOException;

}
