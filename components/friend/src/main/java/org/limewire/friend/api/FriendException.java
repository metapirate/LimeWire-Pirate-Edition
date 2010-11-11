package org.limewire.friend.api;

public class FriendException extends Exception{
    public FriendException(Throwable cause) {
        super(cause);
    }

    public FriendException(String message) {
        super(message);
    }

    public FriendException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
