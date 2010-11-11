package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Thrown when no 'HTTP OK' or the equivalent is not received.
 */
public class NoHTTPOKException extends IOException {
	public NoHTTPOKException() { super("No HTTP OK"); }
	public NoHTTPOKException(String msg) { super(msg); }
}
