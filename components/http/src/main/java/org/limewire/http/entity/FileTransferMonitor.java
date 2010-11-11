package org.limewire.http.entity;

import java.io.IOException;

/**
 * Defines the requirements for classes that control a file transfer.
 */
public interface FileTransferMonitor {

    /**
     * Invoked when data has been transfered.
     * 
     * @param written the number of bytes written since the last invocation of
     *        this method
     */
    void addAmountUploaded(int written);

    /**
     * Invoked when the transfer fails. This method needs to close the underlying connection.
     * 
     * @param e the exception that describes the cause of the failure
     */
    void failed(IOException e);
    
    /**
     * Invoked when the transfer is initialized. 
     */
    void start();

    /**
     * Invoked when the transfer timesout. This method needs to close the underlying connection. 
     */
    void timeout();

}
