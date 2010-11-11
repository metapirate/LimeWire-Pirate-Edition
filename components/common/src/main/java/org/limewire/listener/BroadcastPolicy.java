package org.limewire.listener;

/**
 * Lists the broadcast policies.
 * @see org.limewire.listener.CachingEventMulticasterImpl
 */
public enum BroadcastPolicy {
    /**
     * Broadcast every event.
     */
    ALWAYS,

    /**
     * Only broadcast events if the are not equal
     * to the previous event.
     */
    IF_NOT_EQUALS
}
