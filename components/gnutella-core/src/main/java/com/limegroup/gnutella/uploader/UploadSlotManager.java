package com.limegroup.gnutella.uploader;


import com.limegroup.gnutella.BandwidthTracker;
import com.limegroup.gnutella.InsufficientDataException;


/**
 * Defines an interface for the logic of managing BT uploads and HTTP Uploads.
 * More information available see  
 * http://wiki.limewire.org/index.php?title=Upload_Slots_And_BT
 */
public interface UploadSlotManager extends BandwidthTracker {

    /**
	 * Polls for an available upload slot. (HTTP-style)
	 * 
	 * @param user the user that will use the upload slot
	 * @param queue if the user can enter the queue 
     * @param highPriority if the user needs an upload slot now or never 
	 * @return the position in the queue if queued, -1 if rejected,
	 * 0 if it can proceed immediately
	 */
	public int pollForSlot(UploadSlotUser user, boolean queue, boolean highPriority);
		
	/**
	 * Requests an upload slot. (BT-style)
	 * 
	 * @param listener the listener that should be notified when a slot
	 * becomes available
	 * @param highPriority if the user needs an upload slot now or never
	 * @return the position of the upload if queued, -1 if rejected, 0 if 
	 * it can proceed immediately.
	 */
	public int requestSlot(UploadSlotListener listener, boolean highPriority);
	
	public int positionInQueue(UploadSlotUser user);

	/**
	 * @return whether there is a free slot for an HTTP uploader.
	 */
	public boolean hasHTTPSlot(int current);
	
	/**
	 * @return true if there may be a free slot for an HTTP uploader.
	 */
	public boolean hasHTTPSlotForMeta(int current);
	
	public void measureBandwidth();
	
	public float getMeasuredBandwidth() throws InsufficientDataException;
	
	public float getAverageBandwidth();

	/**
	 * Cancels the request issued by the upload slot user.
	 */
	public void cancelRequest(UploadSlotUser user);

	/**
	 * Notification that the UploadSlotUser is done with its request.
	 */
	public void requestDone(UploadSlotUser user);
	
	public int getNumActive();
	
	public int getNumQueued();
	
	public int getNumQueuedResumable();
	
	public int getNumUsersForHost(String host);
    
    public void cleanup();
}
