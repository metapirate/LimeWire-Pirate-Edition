package com.limegroup.gnutella.downloader;

import java.io.IOException;

public class QueuedException extends IOException {
    //All package access. 
    private int minPollTime = 45000;  //45 secs default
    private int maxPollTime = 120000; //120 secs default
    private int queuePos = -1; //position in the queue
    
    public QueuedException(int minPoll, int maxPoll, int pos) {
        this.minPollTime = minPoll;
        this.maxPollTime = maxPoll;
        this.queuePos = pos;
    }
    
    //package access accessor methods
    public int getMinPollTime() {
        return minPollTime;
    }

    public int getMaxPollTime() {
        return maxPollTime;
    }
    
    public int getQueuePosition() {
        return queuePos;
    }
}
    
