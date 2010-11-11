package com.limegroup.gnutella.downloader.serial.conversion;

import java.net.URL;

public class SerialUrlRemoteFileDesc extends SerialRemoteFileDesc4x16 {

    private static final long serialVersionUID = 820347987014466054L;
    
    private URL _url;
    
    public URL getUrl() {
        return _url;
    }
    
}
