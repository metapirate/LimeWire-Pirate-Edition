package org.limewire.rudp.messages;

import org.limewire.io.InvalidDataException;

/**
 * Thrown when a message isn't properly formatted. For example, the message
 * might not be the proper length, not enough data in the header or an
 * inconsistent message size.
 */
public class MessageFormatException extends InvalidDataException {

    public MessageFormatException() {
        super();
    }

    public MessageFormatException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public MessageFormatException(String msg) {
        super(msg);
    }

    public MessageFormatException(Throwable cause) {
        super(cause);
    }

}
