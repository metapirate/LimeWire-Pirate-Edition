package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

interface SerialManagedDownloader {

    public SerialRemoteFileDesc getDefaultRFD();

    public Set<SerialRemoteFileDesc> getRemoteFileDescs();

    public SerialIncompleteFileManager getIncompleteFileManager();

    public Map<String, Serializable> getProperties();

}