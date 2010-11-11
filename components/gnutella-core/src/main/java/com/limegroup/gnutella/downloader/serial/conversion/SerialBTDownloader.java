package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

import org.limewire.util.GenericsUtils;

class SerialBTDownloader extends SerialRoot {
    

    private static final long serialVersionUID = -7785186190441081641L;

    private transient Map<String, Serializable> properties;

    private void writeObject(ObjectOutputStream output) throws IOException {}
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        Object read = in.readObject();
        properties = GenericsUtils.scanForMap(read, String.class, Serializable.class,
                GenericsUtils.ScanMode.EXCEPTION);
    }
    
    Map<String, Serializable> getProperties() {
        return properties;
    }

    
}
