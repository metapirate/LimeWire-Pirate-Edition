package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Thrown when the requested range is not available.
 */
public class RangeNotAvailableException extends IOException {
    public RangeNotAvailableException() { super("Range not available"); }
    public RangeNotAvailableException(String msg) { super(msg); }
}
