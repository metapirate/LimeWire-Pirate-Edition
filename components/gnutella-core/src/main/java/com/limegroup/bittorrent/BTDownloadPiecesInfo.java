package com.limegroup.bittorrent;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentInfo;
import org.limewire.bittorrent.TorrentPieceState;
import org.limewire.bittorrent.TorrentPiecesInfo;
import org.limewire.core.api.download.DownloadPiecesInfo;

class BTDownloadPiecesInfo implements DownloadPiecesInfo {

    private final TorrentPiecesInfo piecesInfo;
    private final Torrent torrent;
    
    BTDownloadPiecesInfo(Torrent torrent) {
        this.torrent = torrent;
        
        piecesInfo = torrent.getPiecesInfo();
    }
    
    @Override
    public PieceState getPieceState(int piece) {
        return convertPieceState(piecesInfo.getPieceState(piece));
    }

    @Override
    public int getNumPieces() {
        return piecesInfo.getNumPieces();
    }

    @Override
    public long getPieceSize() {
        TorrentInfo info = torrent.getTorrentInfo();
        
        if (info == null) {
            return -1;
        }
        
        return torrent.getTorrentInfo().getPieceLength();
    }  
    
    @Override
    public int getNumPiecesCompleted() {
        return piecesInfo.getNumPiecesCompleted();
    }
    
    private static PieceState convertPieceState(TorrentPieceState state) {
        switch(state) {
            case ACTIVE :
                return PieceState.ACTIVE;
            case DOWNLOADED :
                return PieceState.DOWNLOADED;
            case PARTIAL :
                return PieceState.PARTIAL;
            case AVAILABLE :
                return PieceState.AVAILABLE;
            case UNAVAILABLE :
                return PieceState.UNAVAILABLE;
            default:
                throw new IllegalArgumentException("Unknown TorrentPieceState: " + state);
        }
    }
}
