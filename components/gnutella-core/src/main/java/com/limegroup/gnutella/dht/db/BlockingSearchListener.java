package com.limegroup.gnutella.dht.db;

import org.limewire.concurrent.OnewayExchanger;

/**
 * Blocking implementation of {@link SearchListener}, clients instantiate it
 * pass it in to the API call and call the blocking {@link #getResult()}. 
 */
public class BlockingSearchListener<Result> implements SearchListener<Result> {

    private final OnewayExchanger<Result, RuntimeException> exchanger = new OnewayExchanger<Result, RuntimeException>();
    
    public void handleResult(Result result) {
        exchanger.setValue(result);
    }

    public void searchFailed() {
        exchanger.setValue(null);
    }
    
    public Result getResult() {
        try {
            return exchanger.get();
        } catch (InterruptedException ie) {
            return null;
        }
    }
}
