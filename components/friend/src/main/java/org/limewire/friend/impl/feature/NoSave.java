package org.limewire.friend.impl.feature;

/**
 * Types of nosave statuses.
 */
public enum NoSave {

    // Each enum takes an identifier String, which is used to
    // parse each individual status in the nosave result packet
    ENABLED("enabled"),
    DISABLED("disabled");

    private final String statusName;

    NoSave(String statusName) {
        this.statusName = statusName;
    }

    /**
     * Used during parsing of google:nosave IQ packets.
     *
     * @return String corresponding to what the nosave
     * iq parser looks for ("enabled" or "disabled")
     */
    public String getPacketIdentifier() {
        return statusName;
    }
}
