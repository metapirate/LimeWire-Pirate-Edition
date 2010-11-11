package org.limewire.net;

import java.net.Socket;

/**
 * Objects of this type can be notified whenever a new
 * connection is established and the first word on the wire
 * is read.  
 * 
 * The objects are responsible for registering themselves
 * with the ConnectionDispatcher.
 */
public interface ConnectionAcceptor {
	
    /**
	 * Notification that a new incoming socket has been
	 * opened.
	 * @param word first word that arrived on the wire
	 * @param s the newly opened socket.
	 */
	void acceptConnection(String word, Socket s);
	
	/**
     * Returns true, if {@link #acceptConnection(String, Socket)} needs to be
     * invoked in a separate thread.
     */
	boolean isBlocking();
	
}
