package org.limewire.bittorrent.bencoding;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * A Bencode element (http://en.wikipedia.org/wiki/Bencoding) that can be 
 * used for Lists and Maps. This works for reading Maps because they're read
 * as a list of key/value pairs.
 */
public abstract class BEAbstractCollection<T> extends Token<T> {

    /** Whether the token has been parsed completely */
    protected boolean done;
    
    /** The current element being parsed. */
    private Token<?> currentElement;
    
    public BEAbstractCollection(ReadableByteChannel chan) {
        super(chan);
        result = createCollection();
    }
    
    /** Creates the underlying collection to store the result. */
    protected abstract T createCollection();
    
    /** Adds a newly parsed item into the underlying collection. */
    protected abstract void add(Object o);
    
    /** Parses an item from the stream. */
    protected abstract Token<?> getNewElement() throws IOException;
    
    /** Determines if all parsing is finished for this token. */
    @Override
    protected boolean isDone() {
        return done;
    }
    
    /** Reads a single item from the stream. */
    @Override
    public void handleRead() throws IOException {
        if (isDone())
            throw new IllegalStateException("token is done, don't read to it");
        while(true) {
            if (currentElement == null) 
                currentElement = getNewElement();
            
            if (currentElement == null)
                return;
            
            if (currentElement.getResult() == Token.TERMINATOR) {
                done = true;
                return;
            }
                        
            currentElement.handleRead();
            Object result = currentElement.getResult();
            if (result == null)
                return;
            
            if (result == Token.TERMINATOR) {
                done = true;
                return;
            }
            
            add(result);
            currentElement = null;
        }
    }
    
    

}