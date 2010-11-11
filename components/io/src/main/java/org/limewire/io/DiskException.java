
package org.limewire.io;

import java.io.IOException;

/**
 * Thrown upon problems reading from and writing to disk.
 *
 */
public class DiskException extends IOException {
    public DiskException(String str) {
        super(str);
    }
	public DiskException(IOException cause) {
	    initCause(cause);
	}
}
