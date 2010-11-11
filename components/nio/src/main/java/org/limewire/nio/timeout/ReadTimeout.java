package org.limewire.nio.timeout;

/**
 * Defines the interface to get the read timeout value in milliseconds. 
 */
public interface ReadTimeout {

    /**
     * @return 0 implies the timeout option is disabled (i.e.. timeout of 
     * infinity). A negative return implies an error.
     */
    public long getReadTimeout();
}
