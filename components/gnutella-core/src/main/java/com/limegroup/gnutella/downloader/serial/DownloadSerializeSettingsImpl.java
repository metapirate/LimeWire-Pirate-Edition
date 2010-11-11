package com.limegroup.gnutella.downloader.serial;

import java.io.File;

import org.limewire.util.CommonUtils;

public class DownloadSerializeSettingsImpl implements DownloadSerializeSettings {
    
    public File getBackupFile() {
        return new File(CommonUtils.getUserSettingsDir(), "downloads.bak");
    }
    
    public File getSaveFile() {
        return new File(CommonUtils.getUserSettingsDir(), "downloads.dat");
    }

}
