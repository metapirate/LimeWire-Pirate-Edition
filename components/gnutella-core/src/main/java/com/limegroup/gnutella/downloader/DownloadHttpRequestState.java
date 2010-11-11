package com.limegroup.gnutella.downloader;


class DownloadHttpRequestState {
    
    static enum State {
        BEGIN,
        REQUESTING_THEX,
        DOWNLOADING_THEX,
        CONSUMING_BODY,
        REQUESTING_HTTP,
        QUEUED,
        DOWNLOADING;        
    }
    
    private volatile State state;
    private volatile boolean http11;
    
    DownloadHttpRequestState() {
        this.state = State.BEGIN;
        this.http11 = true;
    }
    
    State getCurrentState() {
        return state;
    }
    
    void setState(State state) {
        this.state = state;
    }
    
    boolean isHttp11() {
        return http11;
    }
    
    void setHttp11(boolean http11) {
        this.http11 = http11;
    }
    
    @Override
    public String toString() {
        if(state != null) {
            return state.toString();
        } else {
            return "no state";
        }
    }

}
