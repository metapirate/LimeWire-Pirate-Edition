package com.limegroup.gnutella.downloader;

import java.net.Socket;
import java.util.Collection;

import org.limewire.io.GUID;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.altlocs.AlternateLocation;

/**Defines an interface for controlled downloads. */
public interface ManagedDownloader extends MeshHandler, CoreDownloader {

    /** Adds any default sources, and a default filename to use. */
    public void addInitialSources(Collection<RemoteFileDesc> rfds, String defaultFileName);
    
    /** Sets the query GUID that was used for searching for this download, if any. */
    public void setQueryGuid(GUID queryGuid);

    /**
     * Determines if this is in an 'active' downloading state.
     */
    public boolean isActive();

    /**
     * notifies this downloader that an alternate location has been added.
     */
    public void locationAdded(AlternateLocation loc);

    /** 
     * Attempts to add the given location to this.  If rfd is accepted, this
     * will terminate after downloading rfd or any of the other locations in
     * this.  This may swarm some file from rfd and other locations.<p>
     * 
     * This method only adds rfd if allowAddition(rfd).  Subclasses may
     * wish to override this protected method to control the behavior.
     * 
     * @param rfd a new download candidate.  Typically rfd will be similar or
     *  same to some entry in this, but that is not required. 
     * @param cache if true, add the <code>RemoteFileDesc</code> to a set of 
     *  cached files  
     * @return true if rfd has been added.  In this case, the caller should
     *  not offer rfd to another ManagedDownloaders.
     */
    public boolean addDownload(RemoteFileDesc rfd, boolean cache);

    public boolean addDownload(Collection<? extends RemoteFileDesc> c, boolean cache);

    /**
     * Returns true if we have received more possible source since the last
     * time we went inactive.
     */
    public boolean hasNewSources();

    /**
     * Accepts a push download.  If this chooses to download the given file
     * (with given index and clientGUID) from socket, returns true.  In this
     * case, the caller may not make any modifications to the socket.  If this
     * rejects the given file, returns false without modifying this or socket.
     * Non-blocking.
     *     @modifies this, socket
     *     @requires GIV string (and nothing else) has been read from socket
     */
    public boolean acceptDownload(String file, Socket socket, int index, byte[] clientGUID);

    /**
     * Determines if this download was cancelled.
     */
    public boolean isCancelled();

    public int getNumDownloaders();

}