package org.limewire.net;

import java.net.Socket;

/**
 * Defines the requirements for classes that forward incoming connections to
 * {@link ConnectionAcceptor}s.
 */
public interface ConnectionDispatcher {

    /**
     * Retrieves the maximum size a word can have.
     */
    public abstract int getMaximumWordSize();

    /**
     * Associates the given SocketAcceptor with the given words.
     * If localOnly is true, non-local-host sockets will be closed
     * when using the word.  Otherwise, localhost sockets will be
     * forbidden from using the word.
     * If blocking is true, a new thread will be spawned when calling
     * acceptor.acceptConnection.
     * 
     * @param acceptor the SocketAcceptor to call acceptConnection on
     * @param localOnly true if localhost connections are required, false if none allowed
     * @param words the list of words to associate with this SocketAcceptor
     */
    public abstract void addConnectionAcceptor(ConnectionAcceptor acceptor,
            boolean localOnly, String... words);

    /** Removes any ConnectionAcceptors from being associated with the given words. */
    public abstract void removeConnectionAcceptor(String... words);

    /** Determines if the word is valid for the understood protocols. */
    public abstract boolean isValidProtocolWord(String word);

    /**
     * Dispatches this incoming connection to the appropriate manager, depending
     * on the word that was read.
     * 
     * @param word the word associated with this Socket
     * @param client socket to use for dispatch
     * @param newThread whether or not a new thread is necessary when dispatching
     *                  to a blocking protocol.
     */
    public abstract void dispatch(final String word, final Socket client,
            boolean newThread);

}