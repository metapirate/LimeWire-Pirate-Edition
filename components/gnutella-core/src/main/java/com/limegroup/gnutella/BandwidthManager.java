package com.limegroup.gnutella;

import java.net.Socket;

import org.limewire.nio.Throttle;

/**
 * Defines the interface to control the upload and download rate.
 */
public interface BandwidthManager {

	public void applyRate();

	public void applyUploadRate();
	
	public Throttle getReadThrottle();
        
    public Throttle getWriteThrottle();
    
    public Throttle getWriteThrottle(Socket socket);

}
