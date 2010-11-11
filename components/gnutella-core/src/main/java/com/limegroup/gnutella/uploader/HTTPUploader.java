package com.limegroup.gnutella.uploader;

import java.io.File;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpResponse;
import org.limewire.collection.Range;
import org.limewire.core.api.transfer.SourceInfo;

import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.http.AltLocTracker;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.IncompleteFileDesc;

/**
 * Maintains state for an HTTP upload.
 */
public class HTTPUploader extends AbstractUploader implements Uploader {

	/**
	 * The URN specified in the X-Gnutella-Content-URN header, if any.
	 */
    private URN requestedURN;
    
    private boolean supportsQueueing = false;
    
    private AltLocTracker altLocTracker;
    
    private long uploadBegin;

    private long uploadEnd;
    
    private boolean containedRangeRequest;

    private long startTime = -1;
    
    private boolean visible;
        
    private String method;
	
    private volatile String friendId;
    
    private HttpResponse lastResponse;

    private PushEndpoint pushEndpoint;

    public HTTPUploader(String fileName, HTTPUploadSession session) {
        super(fileName, session);
	}
    
    @Override
    public void reinitialize() {
        super.reinitialize();
    
        requestedURN = null;
        uploadBegin = 0;
        uploadEnd = 0;
        containedRangeRequest = false;
        method = null;
    }
		
    @Override
    public void setFileDesc(FileDesc fd) {
        super.setFileDesc(fd);
	
        setUploadBegin(0);
        setUploadEnd(getFileSize());
    }
    
    public void setFile(File file) {
        setFileSize(file.length());
        setUploadBegin(0);
        setUploadEnd(getFileSize());
    }
    
    public void setFriendId(String id) {
        this.friendId = id;
    }
    
    public InetAddress getConnectedHost() {
        return getSession().getConnectedHost();
    }    
    
    public void stop() {
        // for testing: if the uploader was not initialized from a real
        // connection it does not have an IO session
        if (getSession().getIOSession() != null) {
            getSession().getIOSession().shutdown();
        }
	}
	
	/**
     * Returns the index of the first byte of the file to upload.
	 */
    public long getUploadBegin() {
        return this.uploadBegin;
	}
	 
    /**
     * Returns the exclusive index of the last byte to upload.
     */
    public long getUploadEnd() {
        return this.uploadEnd;
    }
    
    public boolean containedRangeRequest() {
        return containedRangeRequest;
    }
    
    /**
     * Validates the byte range to upload. Shrinks the range to a valid range if
     * it was explicitly requested.
	 *
     * @return true, if the upload end and upload begin are set to valid values
	 */
    public boolean validateRange() {
        long first = getUploadBegin();
        long last = getUploadEnd();

        if (first >= last) {
            return false;
        }
        if (getFileDesc() instanceof IncompleteFileDesc) {
            // If we are allowing, see if we have the range.
            IncompleteFileDesc ifd = (IncompleteFileDesc) getFileDesc();
            // If the request contained a 'Range:' header, then we can
            // shrink the request to what we have available.
            if (containedRangeRequest()) {
                Range request = ifd.getAvailableSubRange( first,
                        last - 1);
                if (request == null) {
                    return false;
                }
                setUploadBegin(request.getLow());
                setUploadEnd(request.getHigh() + 1);
            } else {
                if (!ifd.isRangeSatisfiable(first, last - 1)) {
                    return false;
                } 
            }
        } else if (first < 0 || last > getFileSize()) {
            return false;
        }

        return true;
    }

    /**
     * Returns the content URN that the client asked for.
     */
    public URN getRequestedURN() {
        return requestedURN;
		}
		
    public void setRequestedURN(URN requestedURN) {
        this.requestedURN = requestedURN;
    }
    
    public boolean supportsQueueing() {
        return supportsQueueing && isValidQueueingAgent();
	}
	
	/**
     * Blocks certain vendors from being queued, because of buggy downloading
     * implementations on their side.
	 */
    private boolean isValidQueueingAgent() {
        if (getUserAgent() == null)
            return true;

        return !getUserAgent().startsWith("Morpheus 3.0.2");
    }
    
    public void setSupportsQueueing(boolean supportsQueueing) {
        this.supportsQueueing = supportsQueueing;
    }

    public AltLocTracker getAltLocTracker() {
        if (altLocTracker == null) {
            altLocTracker = new AltLocTracker(getFileDesc().getSHA1Urn());
        }
        return altLocTracker;
    }	

    public void setUploadBegin(long uploadBegin) {
        this.uploadBegin = uploadBegin;
    }

    public void setUploadEnd(long uploadEnd) {
        this.uploadEnd = uploadEnd;
    }
            
    public void setContainedRangeRequest(boolean containedRangeRequest) {
        this.containedRangeRequest = containedRangeRequest;
    }	

	/**
     * Returns the time when the upload of content was started.
	 */
    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
                
    public boolean isPartial() {
        return getUploadEnd() - getUploadBegin() < getFileSize();
	}

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public void setLastResponse(HttpResponse lastResponse) {
        this.lastResponse = lastResponse;
    }
    
    public HttpResponse getLastResponse() {
        return lastResponse;
    }
    
    @Override
    public boolean isBrowseHostEnabled() {
        return super.isBrowseHostEnabled() && (getGnutellaPort() != -1 || pushEndpoint != null);
    }

    public void setPushEndpoint(PushEndpoint pushEndpoint) {
        this.pushEndpoint = pushEndpoint;
    }

    public PushEndpoint getPushEndpoint() {
        return pushEndpoint;
    }

    @Override
    public File getFile() {
        FileDesc fileDesc = getFileDesc();
        return fileDesc != null ? fileDesc.getFile() : null;
    }
    
    @Override
    public URN getUrn() {
     return getFileDesc().getSHA1Urn();
    }

    @Override
    public int getNumUploadConnections() {
        return 1;
    }

    @Override
    public String getPresenceId() {
        return friendId;
    }

    @Override
    public float getSeedRatio() {
        //not applicable
        return -1;
    }

    @Override
    public List<SourceInfo> getTransferDetails() {
        return Collections.emptyList();
    }
}
