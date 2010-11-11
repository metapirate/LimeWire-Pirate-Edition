package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Thrown when the slots are filled, and the client should
 * try again later, ie an HTTP 503.
 */
public class TryAgainLaterException extends IOException {
    public TryAgainLaterException() { super("Try Again Later"); }
    public TryAgainLaterException(String msg) { super(msg); }
}
