package org.limewire.core.api.browse;

/**
 * Interface for browsing a host. The browse is initiated with a
 * {@link BrowseListener}. As files are read from the host the listener will
 * receive the updates. The Browse can be halted by calling the {@link #stop()}
 * method.
 */
public interface Browse {
    /** Starts the browse. */
    void start(BrowseListener searchListener);

    /** Stops the browse. */
    void stop();
}
