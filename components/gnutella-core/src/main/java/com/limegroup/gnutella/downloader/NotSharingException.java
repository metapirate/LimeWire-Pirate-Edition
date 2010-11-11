package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * HTTP 410 "Gone" error, aka, "BearShare Not Sharing". 
 */
public class NotSharingException extends IOException {
	public NotSharingException() { super("BearShare Not Sharing"); }
	public NotSharingException(String msg) { super(msg); }
}
