package org.limewire.core.api.download;

/**
 * A mechanism that can be used to retrieve the state of each piece in a download.
 */
public interface DownloadPiecesInfo {

    /**
     * The possible states of any piece.
     */
    public enum PieceState {
        /**
         * The piece has been downloaded in its entirety.
         */
        DOWNLOADED,
        
        /**
         * Some of the data for the piece has been downloaded.
         */
        PARTIAL, 
        
        /**
         * Nothing has been downloaded for the piece yet, however it is available.
         */
        AVAILABLE, 
        
        /**
         * The piece is currently being downloaded or written to.
         */
        ACTIVE, 
        
        /**
         * The piece is set to be downloaded next and no progress has been made yet.
         * 
         * <p>NOTE:Currently does not happen often in BT and unimplemented in Gnutella
         *          Will consider removal. 
         */
        QUEUED,
        
        /**
         * Data for the piece is currently not available with the given peer set.
         */
        UNAVAILABLE,
        
        /**
         * Download has started on the piece but there is no additional data available. 
         */
        UNAVAILABLE_PARTIAL;
    }
  
    /**
     * @return the state of the piece at the given index.
     */
    public PieceState getPieceState(int piece);
  
    /**
     * @return the number of pieces represented.
     */
    public int getNumPieces();
  
    /**
     * @return the on disk size of each piece, except for possibly the last, in bytes.
     */
    public long getPieceSize();
    
    /**
     * @return the number of pieces that have been fully downloaded thus far.
     */
    public int getNumPiecesCompleted();
}
