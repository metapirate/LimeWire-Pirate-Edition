package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.DownloadSerializeSettings;
import com.limegroup.gnutella.downloader.serial.DownloadSerializer;
import com.limegroup.gnutella.downloader.serial.OldDownloadConverter;

public class DownloadUpgradeTask {
    
    private static final Log LOG = LogFactory.getLog(DownloadUpgradeTask.class);
    
    private final OldDownloadConverter oldDownloadConverter;
    private final DownloadSerializeSettings oldDownloadSettings;
    private final DownloadSerializeSettings newSettings;
    private final DownloadSerializer downloadSerializer;
    
    @Inject
    public DownloadUpgradeTask(OldDownloadConverter oldDownloadConverter,
                               @Named("oldDownloadSettings") DownloadSerializeSettings oldDownloadSettings,
                               DownloadSerializeSettings newSettings, 
                               DownloadSerializer downloadSerializer) {
        this.oldDownloadConverter = oldDownloadConverter;
        this.oldDownloadSettings = oldDownloadSettings;
        this.newSettings = newSettings;
        this.downloadSerializer = downloadSerializer;
    }
    
    public void upgrade() {
        File newSaveBackup = newSettings.getBackupFile();
        File newSave = newSettings.getSaveFile();
        if(!newSaveBackup.exists() && !newSave.exists()) {
            try {
                List<DownloadMemento> mementos = readAndConvertOldFormat();
                if(downloadSerializer.writeToDisk(mementos)) {
                    // Success! Now delete the old files.
                    oldDownloadSettings.getSaveFile().delete();
                    oldDownloadSettings.getBackupFile().delete();
                }   
            } catch(IOException iox) {
                LOG.warn("Unable to read old file or write to backup!", iox);
            }
        }
    }


    /** Converts the old serialized format to new mementos. */
    private List<DownloadMemento> readAndConvertOldFormat() throws IOException {
        try {
            return oldDownloadConverter.readAndConvertOldDownloads(oldDownloadSettings.getSaveFile());
        } catch(Throwable ignored) {
            LOG.warn("Error trying to convert old normal file.", ignored);
        }
        
        try {
            return oldDownloadConverter.readAndConvertOldDownloads(oldDownloadSettings.getBackupFile());
        } catch(Throwable ignored) {
            LOG.warn("Error trying to convert old normal file.", ignored);
        }
        
        throw new IOException("Unable to read old files!");
    }
    
}
