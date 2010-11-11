package com.limegroup.gnutella.downloader.serial;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.util.ConverterObjectInputStream;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DownloadSerializerImpl implements DownloadSerializer {
    
    private static final Log LOG = LogFactory.getLog(DownloadSerializerImpl.class);
    
    private final DownloadSerializeSettings downloadSerializeSettings;
    
    @Inject
    public DownloadSerializerImpl(DownloadSerializeSettings downloadSerializeSettings) {
        this.downloadSerializeSettings = downloadSerializeSettings;
    }
    
    /**
     * Reads all saved downloads from disk.
     * <p>
     * This works by first attempting to read from the save file described in the settings,
     * and then attempting to read from the backup file if there were any errors while
     * reading the normal file.  If both files fail, this returns an empty list.
     */
    public List<DownloadMemento> readFromDisk() throws IOException {
        if(!downloadSerializeSettings.getSaveFile().exists() && !downloadSerializeSettings.getSaveFile().exists())
            return Collections.emptyList();
        
        Throwable exception;
        ObjectInputStream in = null;
        try {
            in = new ConverterObjectInputStream(new BufferedInputStream(new FileInputStream(downloadSerializeSettings.getSaveFile())));
            return GenericsUtils.scanForList(in.readObject(), DownloadMemento.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable ignored) {
            exception = ignored;
            LOG.warn("Error reading normal file.", ignored);
        } finally {
            IOUtils.close(in);
        }
        
        // Falls through to here only on error with normal file.
        
        try {
            in = new ConverterObjectInputStream(new BufferedInputStream(new FileInputStream(downloadSerializeSettings.getBackupFile())));
            return GenericsUtils.scanForList(in.readObject(), DownloadMemento.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable ignored) {
            LOG.warn("Error reading normal file.", ignored);
        } finally {
            IOUtils.close(in);
        }
        
        if(exception instanceof IOException)
            throw (IOException)exception;
        else
            throw (IOException)new IOException().initCause(exception);
    }

    /**
     * Writes the mementos to disk. This works by first writing to the backup
     * file and then renaming the backup file to the save file. If the backup
     * file cannot be written, this fails.
     */
    // synchronized to prevent more than one person at a time from possibly writing
    public synchronized boolean writeToDisk(List<? extends DownloadMemento> mementos) {
        return FileUtils.writeWithBackupFile(mementos, downloadSerializeSettings.getBackupFile(),
                downloadSerializeSettings.getSaveFile(), LOG);

    }
}
