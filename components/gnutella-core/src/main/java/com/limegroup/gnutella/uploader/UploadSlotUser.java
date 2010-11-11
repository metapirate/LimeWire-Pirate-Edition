package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.BandwidthTracker;

/**
 * Something that uses up upload slot. 
 */
public interface UploadSlotUser extends BandwidthTracker {
	
	/** 
	 * @return the remote host that this user is or will be uploading to.
	 */
	public String getHost();
	
	/**
	 * request that this releases the upload slot it may be using.
	 */
	public void releaseSlot();
}
