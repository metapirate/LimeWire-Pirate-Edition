package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.util.Map;

import com.limegroup.gnutella.downloader.DownloaderType;

/**
 * Defines an interface from which downloads can be saved and recreated over
 * different sessions.
 */
public interface DownloadMemento {
    
    public void setDownloadType(DownloaderType downloaderType);
    
    public DownloaderType getDownloadType();
    
    public void setSaveFile(File saveFile);
    
    public void setDefaultFileName(String defaultFileName);
    
    public String getDefaultFileName();
    
    public File getSaveFile();
    
    public Map<String, Object> getAttributes();
    
    public void setAttributes(Map<String, Object> attributes);
}
