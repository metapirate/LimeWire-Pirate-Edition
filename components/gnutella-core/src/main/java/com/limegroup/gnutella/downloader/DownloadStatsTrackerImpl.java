package com.limegroup.gnutella.downloader;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Singleton;

@Singleton
public class DownloadStatsTrackerImpl implements DownloadStatsTracker {

    private HashMap<PushReason, AtomicInteger> pushReasonStats = new HashMap<PushReason, AtomicInteger>();
    
    private AtomicInteger directConnectSuccess;
    
    private AtomicInteger directConnectFail;
    
    private AtomicInteger pushConnectSuccess;
    
    private AtomicInteger pushConnectFail;                
    
    public DownloadStatsTrackerImpl() {
        directConnectSuccess = new AtomicInteger(0);
        directConnectFail = new AtomicInteger(0);
        pushConnectSuccess = new AtomicInteger(0);
        pushConnectFail = new AtomicInteger(0);
        for(PushReason reason : PushReason.values()) {
            pushReasonStats.put(reason, new AtomicInteger(0));        
        }
    }

    public void successfulDirectConnect() {
        directConnectSuccess.incrementAndGet();
    }

    public void failedDirectConnect() {
        directConnectFail.incrementAndGet();
    }

    public void successfulPushConnect() {
        pushConnectSuccess.incrementAndGet();
    }

    public void failedPushConnect() {
        pushConnectFail.incrementAndGet();
    }

    public void increment(PushReason reason) {
        if(reason != null) {
            pushReasonStats.get(reason).incrementAndGet();       
        }    
    }
}
