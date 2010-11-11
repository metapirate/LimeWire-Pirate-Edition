package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.ssl.SSLUtils;

import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.LibraryUtils;

/**
 * Provides an implementation of the {@link Uploader} interface.
 */
public abstract class AbstractUploader implements Uploader {

    private static final Log LOG = LogFactory.getLog(AbstractUploader.class);

    private final HTTPUploadSession session;

    /** 
     * The number of bytes that were transferred in previous sessions. 
     * <p>
     * Note: Obtain {@link #bwLock} before accessing.
     */
    private long totalAmountUploadedBefore;

    /**
     * The number of bytes transfered by all requests represented by this
     * instance.
     * <p>
     * Note: Obtain {@link #bwLock} before accessing.
     */
    private long totalAmountUploaded;

    /** 
     * The number of bytes transfered for the current request. 
     * <p>
     * Note: Obtain {@link #bwLock} before accessing.
     */
    private long amountUploaded;

    /**
     * Lock for bandwidth tracking related fields. 
     */
    private final Object bwLock = new Object();

    private boolean ignoreTotalAmountUploaded;

    private long fileSize;

    private String userAgent;

    private final String filename;

    private UploadStatus state = UploadStatus.CONNECTING;

    private UploadStatus lastTransferState;

    private boolean firstReply;

    private boolean browseHostEnabled;

    /**
     * True if this is a forcibly shared network file.
     */
    private boolean forcedShare = false;

    /**
     * True if this is an uploader with high priority.
     */
    private boolean priorityShare = false;

    /** The descriptor of the file being uploaded. */
    private FileDesc fileDesc;

    private int index;

    private String host;

    private int port = -1;

    /** The upload type of this uploader. */
    private UploadType uploadType;
    
    public AbstractUploader(String fileName, HTTPUploadSession session) {
        this.session = session;
        this.filename = fileName;
        this.firstReply = true;
    }

    /**
     * Reinitializes this uploader for a new request.
     */
    public void reinitialize() {
        setState(UploadStatus.CONNECTING);
        host = null;
        port = -1;
        synchronized (bwLock) {
            totalAmountUploadedBefore = 0;
            if (!ignoreTotalAmountUploaded) {
                totalAmountUploaded += amountUploaded;
            }
            ignoreTotalAmountUploaded = false;
            amountUploaded = 0;
        }
        firstReply = false;
    }

    /**
     * Sets the file that is being uploaded.
     * 
     * @param fd the file being uploaded
     * @throws IOException if the file cannot be read from the disk.
     */
    public void setFileDesc(FileDesc fd) {
        if (LOG.isDebugEnabled())
            LOG.debug("Setting file description for " + this + ": " + fd);
        this.fileDesc = fd;
        this.forcedShare = LibraryUtils.isForcedShare(fd);
        this.priorityShare = false;
        this.index = fd.getIndex();
        setFileSize(fd.getFileSize());
    }

    public void setState(UploadStatus state) {
        if (this.state == state) {
            IllegalStateException ise = new IllegalStateException();
//            ise.initCause(previousStateSetter);
            throw ise;
        }
        this.lastTransferState = this.state;
        this.state = state;
//        this.previousStateSetter = new Exception();
    }

    /**
     * Returns the queued position if queued.
     */
    public int getQueuePosition() {
        if (lastTransferState != UploadStatus.QUEUED || state == UploadStatus.INTERRUPTED)
            return -1;
        else
            return session.positionInQueue();
    }

    /**
     * Sets the number of bytes that have been uploaded for this upload. This is
     * expected to restart from 0 for each chunk of an HTTP/1.1 transfer.
     * 
     * @param amount the number of bytes that have been uploaded
     */
    void setAmountUploaded(long amount) {
        synchronized (bwLock) {
            addAmountUploaded((int) (amount - amountUploaded));
        }
    }

    /**
     * Increases the amount of uploaded bytes.
     * 
     * @param written number of bytes transferred
     */
    public void addAmountUploaded(int written) {
        assert written >= 0;

        synchronized (bwLock) {
            amountUploaded += written;
        }
    }

    /**
     * Returns whether or not this upload is in what is considered an "inactive"
     * state, such as completed or aborted.
     * 
     * @return <tt>true</tt> if this upload is in an inactive state,
     *         <tt>false</tt> otherwise
     */
    public boolean isInactive() {
        switch (state) {
        case COMPLETE:
        case INTERRUPTED:
            return true;
        default:
            return false;
        }
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getIndex() {
        return index;
    }

    public String getFileName() {
        return this.filename;
    }

    public UploadStatus getState() {
        return state;
    }

    public UploadStatus getLastTransferState() {
        return lastTransferState;
    }

    public String getHost() {
        return (host != null) ? host : session.getHost();
    }

    public boolean isBrowseHostEnabled() {
        return browseHostEnabled;
    }

    public int getGnutellaPort() {
        return port;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public boolean isForcedShare() {
        return forcedShare;
    }

    public boolean isPriorityShare() {
        return priorityShare;
    }

    /**
     * Returns true, if this is this uploader represents the first request.
     */
    protected boolean isFirstReply() {
        return firstReply;
    }

    public long amountUploaded() {
        synchronized (bwLock) {
            return amountUploaded;
        }
    }

    public long getTotalAmountUploaded() {
        synchronized (bwLock) {
            if (ignoreTotalAmountUploaded)
                return amountUploaded;
            else if (totalAmountUploadedBefore > 0)
                return totalAmountUploadedBefore + amountUploaded;
            else
                return totalAmountUploaded + amountUploaded;
        }
    }

    public FileDesc getFileDesc() {
        return fileDesc;
    }

    public void measureBandwidth() {
        // FIXME type conversion
        int written;
        synchronized (bwLock) {
            written = (int) (totalAmountUploaded + amountUploaded);
        }
        session.measureBandwidth(written);
    }

    public float getMeasuredBandwidth() throws InsufficientDataException {
        return session.getMeasuredBandwidth();
    }

    public float getAverageBandwidth() {
        return session.getAverageBandwidth();
    }

    public String getCustomIconDescriptor() {
        return null;
    }

    public UploadType getUploadType() {
        return uploadType;
    }

    /**
     * Sets the type returned by {@link #getUploadType()}.
     */
    public void setUploadType(UploadType type) {
        uploadType = type;
    }

    /**
     * Sets the flag returned by {@link #isBrowseHostEnabled()}.
     */
    public void setBrowseHostEnabled(boolean browseHostEnabled) {
        this.browseHostEnabled = browseHostEnabled;
    }

    /**
     * Sets the port returned by {@link #getGnutellaPort()}.
     */
    public void setGnutellaPort(int port) {
        this.port = port;
    }

    /**
     * Sets the amount uploaded in previous sessions. If
     * <code>totalAmountReadBefore</code> is != 0,
     * {@link #getTotalAmountUploaded()} will use that value to calculate the
     * total amount uploaded instead of relying on the value maintained by this
     * uploader.
     */
    public void setTotalAmountUploadedBefore(int totalAmountReadBefore) {
        synchronized (bwLock) {
            this.totalAmountUploadedBefore = totalAmountReadBefore;
        }
    }

    /**
     * Sets the user agent returned by {@link #getUserAgent()}.
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[host=" + getHost() + ",index=" + index
                + ",filename=" + filename + ",state=" + state
                + ",lastTransferState=" + lastTransferState + "]";
    }

    /**
     * Returns the upload session that is associated with the connection.
     */
    public HTTPUploadSession getSession() {
        return session;
    }

    /**
     * Returns the file size returned by {@link #getFileSize()}.
     * 
     * @param fileSize must be greater than 0
     */
    public void setFileSize(long fileSize) {
        if (fileSize <= 0) {
            throw new IllegalArgumentException("illegal file size: " + fileSize);
        }
        this.fileSize = fileSize;
    }

    /**
     * Returns true, if the amount uploaded in previous sessions is not returned
     * by {@link #getTotalAmountUploaded()}.
     * 
     * @see #setIgnoreTotalAmountUploaded(boolean)
     */
    public boolean getIgnoreTotalAmountUploaded() {
        return ignoreTotalAmountUploaded;
    }

    /**
     * If set to true, the amount uploaded in previous sessions is not returned
     * by {@link #getTotalAmountUploaded()}.
     * <p>
     * Note: This is reset to <code>false</code> by {@link #reinitialize()}.
     * 
     * @see #getIgnoreTotalAmountUploaded()
     */
    public void setIgnoreTotalAmountUploaded(boolean ignoreTotalAmountUploaded) {
        this.ignoreTotalAmountUploaded = ignoreTotalAmountUploaded;
    }

    public void setHost(String host) {
        this.host = host;
    }
 
    /** Returns true if the socket this is on is currently using TLS. */
    public boolean isTLSCapable() {
        return SSLUtils.isTLSEnabled(getSession().getIOSession().getSocket());
    }

    /** Returns the address of the host that initiated this upload. */
    public String getAddress() {
        return getSession().getHost();
    }

   /** Returns the address of the host that initiated this session. */
    public InetAddress getInetAddress() {
        return getSession().getIOSession().getSocket().getInetAddress();
    }

    /** Returns a combination of getInetAddress and getPort. */
    public InetSocketAddress getInetSocketAddress() {
        return new InetSocketAddress(getInetAddress(), getPort());
    }
    
    @Override
    public String getAddressDescription() {
        return getInetSocketAddress().toString();
    }

    /** Returns the Gnutella Port, if one was provided.  Otherwise, the remote port from the socket. */
    public int getPort() {
        int gnutellaPort = getGnutellaPort();
        if(gnutellaPort != -1)
            return gnutellaPort;
        else
            return ((InetSocketAddress)getSession().getIOSession().getRemoteAddress()).getPort();
    }
    
    @Override
    public void pause() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void resume() {
        throw new UnsupportedOperationException();  
    }
}
