package org.limewire.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.limewire.io.IOUtils;

/**
 * A ConnectionDispatcher that blocks while reading.
 */
public class BlockingConnectionDispatcher implements Runnable {
    
    private final ConnectionDispatcher dispatcher;
    private final Socket socket;
    private final String allowedWord;
    
    public BlockingConnectionDispatcher(ConnectionDispatcher dispatcher, Socket socket, String allowedWord) {
        if (dispatcher == null) {
            throw new IllegalArgumentException();
        }
        if (socket == null) {
            throw new IllegalArgumentException();
        }
 
        this.dispatcher = dispatcher;
        this.socket = socket;
        this.allowedWord = allowedWord;
    }

    protected void shutdown() {
    }
    
    /** Reads a word and sends it off to the ConnectionDispatcher for dispatching. */
    public void run() {
        try {
            //The try-catch below is a work-around for JDK bug 4091706.
            InputStream in=null;
            try {
                in=socket.getInputStream(); 
            } catch (IOException e) {
                shutdown();
                throw e;
            } catch(NullPointerException e) {
                // This should only happen extremely rarely.
                // JDK bug 4091706
                throw new IOException(e.getMessage());
            }
            
            String word = IOUtils.readLargestWord(in, dispatcher.getMaximumWordSize());
            if(allowedWord != null && !allowedWord.equals(word))
                throw new IOException("wrong word!");
            dispatcher.dispatch(word, socket, false);
        } catch (IOException iox) {
            shutdown();
            IOUtils.close(socket);
        }
    }
}    
