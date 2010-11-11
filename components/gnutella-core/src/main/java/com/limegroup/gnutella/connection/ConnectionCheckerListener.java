package com.limegroup.gnutella.connection;

public interface ConnectionCheckerListener {

    /**
     * Invoked when an Internet connection was detected.
     */
    void connected();
    
    /**
     * Invoked when no Internet connection could be detected.
     */
    void noInternetConnection();

}
