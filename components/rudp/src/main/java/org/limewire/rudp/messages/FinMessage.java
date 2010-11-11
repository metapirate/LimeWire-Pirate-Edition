package org.limewire.rudp.messages;

/**
 * Defines an interface to signal the data transport finished. Additionally,
 * the interface includes constants to describe the cause when the transport
 * completed.
 */
public interface FinMessage extends RUDPMessage {

    public static final byte REASON_NORMAL_CLOSE = 0x0;

    public static final byte REASON_YOU_CLOSED = 0x1;

    public static final byte REASON_TIMEOUT = 0x2;

    public static final byte REASON_LARGE_PACKET = 0x3;

    public static final byte REASON_TOO_MANY_RESENDS = 0x4;

    public static final byte REASON_SEND_EXCEPTION = 0x5;

}