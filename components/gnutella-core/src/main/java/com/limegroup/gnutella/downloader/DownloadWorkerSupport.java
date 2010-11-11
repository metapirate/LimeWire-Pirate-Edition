package com.limegroup.gnutella.downloader;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.tigertree.HashTree;

/**
 * Defines the contract by which a download can be signaled & controlled from a
 * DownloadWorker.
 */
interface DownloadWorkerSupport extends ManagedDownloader {

    void addToRanker(RemoteFileDescContext rfdContext);

    void forgetRFD(RemoteFileDesc _rfd);

    List<DownloadWorker> getActiveWorkers();

    List<DownloadWorker> getAllWorkers();

    /**
     * @return The alternate locations we have failed to downloaded from
     */
    Set<AlternateLocation> getInvalidAlts();

    Map<DownloadWorker, Integer> getQueuedWorkers();

    /**
     * @return The alternate locations we have successfully downloaded from
     */
    Set<AlternateLocation> getValidAlts();

    void hashTreeRead(HashTree newTree);

    /** Same as setState(newState, Integer.MAX_VALUE). */
    void setState(DownloadState connecting);

    void incrementTriedHostsCount();

    void removeQueuedWorker(DownloadWorker downloadWorker);

    boolean killQueuedIfNecessary(DownloadWorker downloadWorker, int i);

    void cancelCorruptDownload();

    QueryRequest newRequery() throws CantResumeException;
    
    /**
     * Registers a new ConnectObserver that is waiting for a socket from the given MRFD.
     */
    void registerPushObserver(HTTPConnectObserver observer, PushDetails details);

    boolean removeActiveWorker(DownloadWorker downloadWorker);

    /**
     * Unregisters a ConnectObserver that was waiting for the given MRFD.  If shutdown
     * is true and the observer was still registered, calls shutdown on that observer.
     */
    void unregisterPushObserver(PushDetails details, boolean b);

    void workerFailed(DownloadWorker downloadWorker);

    /**
     * Callback that the specified worker has finished.
     */
    void workerFinished(DownloadWorker downloadWorker);

    void workerStarted(DownloadWorker downloadWorker);

    
}
