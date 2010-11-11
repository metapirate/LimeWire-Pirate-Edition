package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Thrown when a file is not found, for example an HTTP 404. 
 */

public class FileNotFoundException extends IOException {
	public FileNotFoundException() { super("File Not Found"); }
	public FileNotFoundException(String msg) { super(msg); }
}

