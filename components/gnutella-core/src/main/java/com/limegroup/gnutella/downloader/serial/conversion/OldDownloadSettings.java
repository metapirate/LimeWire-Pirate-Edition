package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.File;

import org.limewire.core.settings.SharingSettings;

import com.google.inject.Singleton;
import com.limegroup.gnutella.downloader.serial.DownloadSerializeSettings;

@Singleton
public class OldDownloadSettings implements DownloadSerializeSettings {
    
    public File getBackupFile() {
        return SharingSettings.OLD_DOWNLOAD_SNAPSHOT_BACKUP_FILE.get();
    }

    public File getSaveFile() {
        return SharingSettings.OLD_DOWNLOAD_SNAPSHOT_FILE.get();
    }

}
