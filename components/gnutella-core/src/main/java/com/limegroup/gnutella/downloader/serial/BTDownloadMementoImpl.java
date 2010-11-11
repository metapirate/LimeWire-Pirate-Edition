package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.limegroup.gnutella.downloader.DownloaderType;

public class BTDownloadMementoImpl implements BTDownloadMemento, Serializable {

    private static final long serialVersionUID = -1116043348504657012L;
    
    private Map<String, Object> serialObjects = new HashMap<String, Object>();
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAttributes() {
        return (Map<String, Object>)serialObjects.get("attributes");
    }
    
    public String getDefaultFileName() {
        return (String)serialObjects.get("defaultFileName");
    }
    
    public DownloaderType getDownloadType() {
        return (DownloaderType)serialObjects.get("downloadType");
    }
    
    public File getSaveFile() {
        return (File)serialObjects.get("saveFile");
    }
    
    public BTMetaInfoMemento getBtMetaInfoMemento() {
        return (BTMetaInfoMemento)serialObjects.get("btMetaInfo");
    }
    
    public void setAttributes(Map<String, Object> attributes) {
        serialObjects.put("attributes", attributes);
    }
    
    public void setBtMetaInfoMemento(BTMetaInfoMemento btMetaInfo) {
        serialObjects.put("btMetaInfo", btMetaInfo);
    }
    
    public void setDefaultFileName(String defaultFileName) {
        serialObjects.put("defaultFileName", defaultFileName);
    }
    
    public void setDownloadType(DownloaderType downloaderType) {
        serialObjects.put("downloadType", downloaderType);
    }
    
    public void setSaveFile(File saveFile) {
        serialObjects.put("saveFile", saveFile);
    }
    



}
