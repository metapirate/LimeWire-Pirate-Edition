package com.limegroup.gnutella;

import org.limewire.io.Address;
import org.limewire.io.GUID;

import com.limegroup.gnutella.BrowseHostHandler.PushRequestDetails;
import com.limegroup.gnutella.downloader.PushedSocketHandler;

public interface BrowseHostHandlerManager extends PushedSocketHandler {

	public BrowseHostHandler createBrowseHostHandler(GUID guid, GUID serventID);

    public interface BrowseHostCallback {
        void putInfo(GUID _serventid, PushRequestDetails details);
    }

    public void initialize();

    /**
     * Creates a browse host handler with a session guid <code>browseGuid</code>.
     * 
     * Used for browses on {@link Address} objects. Call 
     * {@link BrowseHostHandler#browseHost(Address)} on the created browse
     * host handler.
     */
    public BrowseHostHandler createBrowseHostHandler(GUID browseGuid);

}