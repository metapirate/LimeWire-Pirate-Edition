package com.limegroup.gnutella;

import java.net.Socket;

/**
 * A processor that handles new incoming connections.
 * A SocketProcessor can be handed a socket and told to process it,
 * or handed a socket and told to only process it if the protocol
 * the socket requests is a specific protocol.
 */
public interface SocketProcessor {

	/** Accepts the given socket. */
	public void processSocket(Socket client);

	/**
	 * Accepts the given incoming socket, allowing only the given protocol.
	 * If allowedProtocol is null, all are allowed.
	 */
	public void processSocket(Socket client, String allowedProtocol);

}