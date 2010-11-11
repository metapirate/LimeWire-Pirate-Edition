package org.limewire.libtorrent;

import org.limewire.bittorrent.TorrentPieceState;
import org.limewire.bittorrent.TorrentPiecesInfo;

/**
 * Lightweight impl of TorrentPiecesInfo that copies its working data from
 *  a LibTorrentPiecesInfoContainer returned from the JNA.
 */
class LibTorrentPiecesInfo implements TorrentPiecesInfo {

    private static final char PIECE_DOWNLOADED = 'x';
    private static final char PIECE_PARTIAL = 'p';
    private static final char PIECE_PENDING = '0';
    private static final char PIECE_ACTIVE = 'a';
    private static final char PIECE_UNAVAILABLE = 'U';
    private static final char PIECE_QUEUED = 'q';
    private static final char PIECE_UNAVAILABLE_PARTIAL = 'u';
    
    private final String stateInfo;
    private final int numPiecesCompleted;
    
    /**
     * Generates a working instance of {@link TorrentPiecesInfo} from a 
     *  {@link LibTorrentPiecesInfoContainer} returned from libtorrent through
     *  JNA.
     */
    LibTorrentPiecesInfo(LibTorrentPiecesInfoContainer piecesInfoContainer) {
        stateInfo = piecesInfoContainer.getStateInfo();
        numPiecesCompleted = piecesInfoContainer.getNumPiecesCompleted();
    }
    
    @Override
    public int getNumPieces() {
        return stateInfo.length();
    }

    @Override
    public int getNumPiecesCompleted() {
        return numPiecesCompleted;
    }

    @Override
    public TorrentPieceState getPieceState(int piece) {
        return getPieceState(stateInfo.charAt(piece));
    }
    
    private static TorrentPieceState getPieceState(char c) {
        switch (c) {
            case PIECE_DOWNLOADED :
                return TorrentPieceState.DOWNLOADED;
            case PIECE_PARTIAL :
                return TorrentPieceState.PARTIAL;
            case PIECE_PENDING :
            case PIECE_QUEUED :
                return TorrentPieceState.AVAILABLE;
            case PIECE_ACTIVE :
                return TorrentPieceState.ACTIVE;
            case PIECE_UNAVAILABLE :
            case PIECE_UNAVAILABLE_PARTIAL :
                return TorrentPieceState.UNAVAILABLE;
            default :
                throw new IllegalArgumentException("Unknown Piece Descriptor: " + c);
        }
    }
}
