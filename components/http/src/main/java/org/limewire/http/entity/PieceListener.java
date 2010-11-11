package org.limewire.http.entity;

import java.io.IOException;

/**
 * Interface for classes interested in {@link PieceReader} events.
 */
public interface PieceListener {

    /**
     * Invoked when a new {@link Piece} is available that can be retrieved
     * through PieceReader#next().
     */
    void readSuccessful();
    
    /**
     * Invoked when reading of a piece failed. The {@link PieceReader} has been
     * shutdown and will not sent further events.
     */
    void readFailed(IOException e);
    
}
