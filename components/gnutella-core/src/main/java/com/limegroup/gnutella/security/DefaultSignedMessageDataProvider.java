package com.limegroup.gnutella.security;

/**
 * Interface used by simpp and update to provide hardcoded default messages
 * that are loaded at startup if there is no newer message saved to disk.
 */
public interface DefaultSignedMessageDataProvider {

    /**
     * @return the payload of the default signed message
     */
    byte[] getDefaultSignedMessageData();

    /**
     * @return the paylod of the old signed message that is sent to peers who
     * still have disabled keys
     */
    byte[] getDisabledKeysSignedMessageData();

}
