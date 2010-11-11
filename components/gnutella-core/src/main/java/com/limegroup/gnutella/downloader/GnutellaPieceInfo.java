package com.limegroup.gnutella.downloader;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.core.api.download.DownloadPiecesInfo;

class GnutellaPieceInfo implements DownloadPiecesInfo {
    
    private final IntervalSet written;
    private final IntervalSet active;
    private final IntervalSet available;
    private final long pieceSize;
    private final int pieceCount;
    private final int piecesCompletedCount;
    private final long length;
    
    public GnutellaPieceInfo(IntervalSet written, IntervalSet active, IntervalSet available, long pieceSize, long length) {
        this.written = written;
        this.active = active;
        this.available = available;
        this.pieceSize = pieceSize;
        this.length = length;
        this.piecesCompletedCount = aproximatePiecesCompleted();
        
        if(length <= 0) {
            this.pieceCount = 0;
        } else {
            this.pieceCount = (int)Math.min(Integer.MAX_VALUE, Math.ceil((double)length / pieceSize));
        }
    }

    @Override
    public int getNumPieces() {
        return pieceCount;
    }

    @Override
    public PieceState getPieceState(int piece) {
        long pieceStart = piece * pieceSize;
        long pieceEnd = Math.max(0, Math.min(pieceStart + pieceSize, length)-1);
        Range range = Range.createRange(pieceStart, pieceEnd);
        PieceState state;
        
        if(written.contains(range)) {
            state = PieceState.DOWNLOADED;
        } else if(active.containsAny(range)) {
            state = PieceState.ACTIVE;
        } else if(available.contains(range)) {
            if(written.containsAny(range)) {
                state = PieceState.PARTIAL;
            } else {
                state = PieceState.AVAILABLE;
            }
        } else {
            state = PieceState.UNAVAILABLE;
        }
        
        return state;
        
    }

    @Override
    public long getPieceSize() {
        return pieceSize;
    }

    @Override
    public int getNumPiecesCompleted() {
        return piecesCompletedCount;
    }
    
    /**
     * @return an approximation of the pieces currently completed.  Calculated
     *  by the number of bytes completed over the number of bytes in a piece.
     */
    private int aproximatePiecesCompleted() {
        return (int)Math.floor((double)written.getSize() / pieceSize);
    }

}
