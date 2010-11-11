package com.limegroup.gnutella.downloader;


import java.net.URL;
import java.util.Set;

import org.limewire.core.settings.SpeedConstants;
import org.limewire.io.Address;
import org.limewire.net.address.AddressFactory;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.util.DataUtils;

/**
 * A RemoteFileDesc augmented with a URL, which might be different from the
 * standard '/get/<index>/<name>'.  Overrides the getUrl() method of
 * RemoteFileDesc.  
 */
class UrlRemoteFileDescImpl extends RemoteFileDescImpl implements RemoteFileDesc {

    /** The return value for getUrl */
    private URL _url;

    /**
     * Constructs a new RemoteFileDescImpl.
     * 
     * @param url the url
     */
    UrlRemoteFileDescImpl(Address address, String filename, long size,
            Set<? extends URN> urns, URL url, AddressFactory addressFactory) {
        super(address, 1, filename, size, DataUtils.EMPTY_GUID, SpeedConstants.T3_SPEED_INT, 3, false, null, 
                urns, false, "", -1, false, addressFactory, null);
        this._url = url;
    }

    /**
     * Returns the URL specified at construction time, which might be totally
     * independent of getName()/getIndex().
     */
    @Override
    public String getUrlPath() {
        return _url.getFile();
    }
    
    @Override
    public RemoteHostMemento toMemento() {
        RemoteHostMemento memento = super.toMemento();
        memento.setCustomUrl(_url);
        return memento;
    }
}
