package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.limewire.util.GenericsUtils;

class SerialManagedDownloaderImpl extends SerialRoot implements SerialManagedDownloader {
    private static final long serialVersionUID = 2772570805975885257L;

    private transient SerialRemoteFileDesc defaultRFD;

    private transient Set<SerialRemoteFileDesc> remoteFileDescs;

    private transient SerialIncompleteFileManager incompleteFileManager;

    private transient Map<String, Serializable> properties;
    
    protected SerialManagedDownloaderImpl() {
    }
    
    private void writeObject(ObjectOutputStream output) throws IOException {}

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {        
        Object next = stream.readObject();
        if (next instanceof SerialRemoteFileDesc[]) {
            SerialRemoteFileDesc[] rfds = (SerialRemoteFileDesc[]) next;
            if (rfds.length > 0)
                defaultRFD = rfds[0];
            remoteFileDescs = new HashSet<SerialRemoteFileDesc>(Arrays.asList(rfds));
        } else if (next instanceof Set) { // new format
            remoteFileDescs = GenericsUtils.scanForSet(next, SerialRemoteFileDesc.class,
                    GenericsUtils.ScanMode.REMOVE);
            if (remoteFileDescs.size() > 0) {
                defaultRFD = remoteFileDescs.iterator().next();
            }
        }

        incompleteFileManager = (SerialIncompleteFileManager) stream.readObject();

        Object map = stream.readObject();
        if (map instanceof Map) {
            properties = GenericsUtils.scanForMap(map, String.class, Serializable.class,
                    GenericsUtils.ScanMode.REMOVE);
        } else {
            properties = new HashMap<String, Serializable>();
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialManagedDownloader#getDefaultRFD()
     */
    public SerialRemoteFileDesc getDefaultRFD() {
        return defaultRFD;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialManagedDownloader#getRemoteFileDescs()
     */
    public Set<SerialRemoteFileDesc> getRemoteFileDescs() {
        return remoteFileDescs;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialManagedDownloader#getIncompleteFileManager()
     */
    public SerialIncompleteFileManager getIncompleteFileManager() {
        return incompleteFileManager;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialManagedDownloader#getProperties()
     */
    public Map<String, Serializable> getProperties() {
        return properties;
    }

}
