package org.limewire.bittorrent;

/**
 * A mechanism that can be used to retrieve the state of each piece in a download.
 */
public interface TorrentPiecesInfo {

    /**
     * @return the state of the indexed piece.
     */
    public TorrentPieceState getPieceState(int piece);

    /**
     * @return the total number of pieces in the download.
     */
    public int getNumPieces();
    
    /**
     * @return the number of completed (downloaded) pieces in the torrent.
     */
    public int getNumPiecesCompleted();

}