package org.limewire.http.entity;

import java.io.EOFException;

/**
 * Implementors read chunks of bytes from a data source.
 */
public interface PieceReader {

    /**
     * Returns the next piece. Pieces will always be returned in the order they
     * are stored in the data source.
     * <p>
     * When the caller has finished processing the returned piece
     * {@link #release(Piece)} must be called.
     * 
     * @return null, if next piece is not yet available or all pieces have been
     *         read
     * @throws EOFException thrown if all pieces have already been returned
     */
    Piece next() throws EOFException;
    
    /**
     * Releases resources used by <code>piece</code>.
     * 
     * @see #next()
     */
    void release(Piece piece);
    
}
