package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;

import com.limegroup.gnutella.URN;

class SerialMagnetDownloader extends SerialManagedDownloaderImpl {
    private static final long serialVersionUID = 9092913030585214105L;
    
    private transient String _textQuery;
    private transient URN _urn;
    private transient String _filename;
    private transient String[] _defaultURLs;
    
    String getTextQuery() {
        return _textQuery;
    }
    
    URN getUrn() {
        return _urn;
    }
    
    String getFilename() {
        return _filename;
    }
    
    String[] getDefaultUrls() {
        return _defaultURLs;
    }
    

    private synchronized void readObject(ObjectInputStream stream) throws IOException,
            ClassNotFoundException {
        GetField gets = stream.readFields();
        try { _textQuery = (String)gets.get("_textQuery", null); } catch(IllegalArgumentException iae) {}
        try {_urn = (URN) gets.get("_urn", null);} catch(IllegalArgumentException iae) {}
        try { _filename = (String) gets.get("_filename", null);} catch(IllegalArgumentException iae) {}
        try { _defaultURLs = (String[]) gets.get("_defaultURLs", null);} catch(IllegalArgumentException iae) {}
    }
}
