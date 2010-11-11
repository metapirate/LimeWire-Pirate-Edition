package org.limewire.nio.timeout;

import java.net.SocketException;

/**
 * Defines the interface to get the socket timeout value in milliseconds. 
 */

public interface SoTimeout {

    /**
     * @return 0 implies the timeout option is disabled (i.e.. timeout of 
     * infinity). A negative return implies an error.
     */
    public int getSoTimeout() throws SocketException;
}
