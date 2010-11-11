package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.File;

import com.limegroup.gnutella.URN;

class SerialResumeDownloader4x11 extends SerialManagedDownloaderImpl implements SerialResumeDownloader {
    private static final long serialVersionUID = -4535935715006098724L;

    private File _incompleteFile;

    private String _name;

    private int _size;

    private URN _hash;
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialResumeDownloader#getIncompleteFile()
     */
    public File getIncompleteFile() {
        return _incompleteFile;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialResumeDownloader#getName()
     */
    public String getName() {
        return _name;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialResumeDownloader#getSize()
     */
    public long getSize() {
        return _size;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialResumeDownloader#getUrn()
     */
    public URN getUrn() {
        return _hash;
    }

}
