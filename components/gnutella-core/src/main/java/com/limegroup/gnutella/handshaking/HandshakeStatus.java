package com.limegroup.gnutella.handshaking;

/**
 * A description of status that handshaking can end with.
 */
public enum HandshakeStatus {

    /* The only good status. */
    OK("OK", true),
    /* All bad statuses. */
    NO_X_ULTRAPEER("No X-Ultrapeer"),
    DISCONNECTED("I'm Disconnected"),
    WE_ARE_LEAVES("We're Leaves"),
    NOT_GOOD_UP("Not Good Ultrapeer"),
    IDLE_LIMEWIRE("Idle, Need LimeWire"),
    STARTING_LIMEWIRE("Starting, Need LimeWire"),
    TOO_MANY_UPS("No Ultrapeer Slots"),
    NOT_ALLOWED_LEAF("Leaf Connection Failed"),
    NOT_GOOD_LEAF("Not Good Leaf"),
    TOO_MANY_LEAF("No Leaf Slots"),
    NOT_ALLOWED_UP("Ultrapeer Connection Failed"),
    NON_LIME_RATIO("Non-LimeWire Slots Full"),
    NO_LIME_SLOTS("No LimeWire Slots"),
    NO_HEADERS("No Headers Received"),
    UNKNOWN("Unknown Handshake Failure");
    
    /** The message the handshake should use. */
    private final String msg;
    /** Whether or not the handshake can continue. */
    private final boolean ok;
    
    /**
     * Constructs a HandshakeStatus.
     * If 'ok' is true, the handshake is acceptable.
     * 
     * @param msg
     * @param ok
     */
    private HandshakeStatus(String msg, boolean ok) {
        this.msg = msg;
        this.ok = ok;
    }
    
    /** Constructs a HandshakeStatus that is not acceptable. */
    private HandshakeStatus(String msg) {
        this(msg, false);
    }
    
    /** Describes the message the handshake should use. */
    public String getMessage() {
        return msg;
    }
    
    /** Determines if the handshake can continue. */
    public boolean isAcceptable() {
        return ok;
    }
    

}
