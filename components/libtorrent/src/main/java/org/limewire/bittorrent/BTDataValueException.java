package org.limewire.bittorrent;

import java.io.IOException;

/**
 * Exception to throw in case received bencoded data does not 
 * match the expected structure.
 */
public class BTDataValueException extends IOException {
	private static final long serialVersionUID = 3990038438042291913L;

	public BTDataValueException() {
		super();
	}

	public BTDataValueException(String arg0) {
		super(arg0);
	}

}
