package com.limegroup.gnutella.http;

/**
 * This interface for classes that contain values that can be accessed
 * as HTTP header values for writing HTTP headers.
 */
public interface HTTPHeaderValue {

	/**
	 * Returns a string representation of the HTTP header value for this
	 * class.
	 *
	 * @return a string representation of the HTTP header value for this
	 *  class
	 */
	public String httpStringValue();
}
