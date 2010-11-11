package org.limewire.core.api.connection;

/**
 * The various reasons why FWT status is either true or false.
 */
public enum FWTStatusReason {
    UNKNOWN,
    NO_SOLICITED_INCOMING_MESSAGES,
    REUSING_STATUS_FROM_PREVIOUS_SESSION,
    INVALID_EXTERNAL_ADDRESS,
    PORT_UNSTABLE,
}
