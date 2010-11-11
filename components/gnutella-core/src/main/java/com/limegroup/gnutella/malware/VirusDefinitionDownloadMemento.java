package com.limegroup.gnutella.malware;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.malware.AntivirusUpdateType;
import org.limewire.util.GenericsUtils;
import org.limewire.util.StringUtils;
import org.limewire.util.UnboxUtils;
import org.limewire.util.GenericsUtils.ScanMode;

import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;

class VirusDefinitionDownloadMemento implements DownloadMemento, Serializable {

    private static final long serialVersionUID = 1653891283169958921L;
    
    private final Map<String, Object> serialObjects = new HashMap<String, Object>();

    @Override
    public Map<String, Object> getAttributes() {
        return GenericsUtils.scanForMap(serialObjects.get("attributes"),
                String.class, Object.class, ScanMode.REMOVE);
    }

    @Override
    public void setAttributes(Map<String, Object> attributes) {
        serialObjects.put("attributes", attributes);
    }

    @Override
    public String getDefaultFileName() {
        return (String)serialObjects.get("defaultFileName");
    }

    @Override
    public void setDefaultFileName(String defaultFileName) {
        serialObjects.put("defaultFileName", defaultFileName);
    }

    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.ANTIVIRUS;
    }

    @Override
    public void setDownloadType(DownloaderType downloaderType) {
        // Always DownloaderType.ANTIVIRUS
    }

    @Override
    public File getSaveFile() {
        return (File)serialObjects.get("saveFile");
    }

    @Override
    public void setSaveFile(File saveFile) {
        serialObjects.put("saveFile", saveFile);
    }
    
    public void setUri(URI uri) {
        serialObjects.put("uri", uri);
    }
    
    public URI getUri() {
        return (URI)serialObjects.get("uri");
    }

    public void setAmountWritten(long amountWritten) {
        serialObjects.put("amountWritten", amountWritten);
    }
    
    public long getAmountWritten() {
        return UnboxUtils.toLong((Long)serialObjects.get("amountWritten"));
    }
    
    public AntivirusUpdateType getAntivirusUpdateType() {
        String handler = (String)serialObjects.get("antivirusUpdateType");
        if(handler != null) {
            try {
                return AntivirusUpdateType.valueOf(handler);
            } catch(IllegalArgumentException ignored) {}
        }
        return null;
    }
    
    public void setAntivirusUpdateType(AntivirusUpdateType avType) {
        serialObjects.put("antivirusUpdateType", avType.name());
    }

    public File getIncompleteFile() {
        String path = (String)serialObjects.get("incompleteFile");
        if(path != null) {
            return new File(path);
        } else {
            return null;
        }
    }
    
    public void setIncompleteFile(File file) {
        serialObjects.put("incompleteFile", file.getPath());
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}
