package com.limegroup.bittorrent;

import java.io.File;

/**
 * Holds the length and the path of a file.
 */
public class TorrentFile extends File {
	private static final long serialVersionUID = 4051327846800962608L;

	private final long length;
	
	private final String torrentPath;

	/** 
	 * The indices of the first and last blocks 
	 * of the torrent this file occupies
	 */
	private int begin, end;
	
	private long startByte, endByte;

	TorrentFile(long length, String path, String torrentPath) {
		super(path);
		this.length = length;
		this.torrentPath = torrentPath;
		begin = -1; //these need to be initialized.
		end = -1; 
		startByte = -1;
		endByte = -1;
	}
	
	@Override
    public long length() {
		return length;
	}
	
	/**
	 * Sets the beginning piece index for this torrent file.
	 */
	public void setBeginPiece(int begin) {
		this.begin = begin;
	}
	
	/**
	 * Gets the beginning piece index for this torrent file.
	 */
	public int getBeginPiece() {
		return begin;
	}
	
	/**
	 * Sets the end piece index for this torrent file.
	 */
	public void setEndPiece(int end) {
		this.end = end;
	}
	
	/**
	 * Gets the end piece index for this torrent file.
	 * @return
	 */
	public int getEndPiece() {
		return end;
	}

	/**
	 * Gets the start byte of the file in the torrent fileSystem.
	 */
    public long getStartByte() {
        return startByte;
    }

    /**
     * Sets the start byte of the file in the torrent fileSystem.
     */
    public void setStartByte(long startByte) {
        this.startByte = startByte;
    }

    /**
     * Gets the end byte of the file in the torrent fileSystem.
     */
    public long getEndByte() {
        return endByte;
    }

    /**
    * Sets the end byte of the file in the torrent fileSystem.
    */
    public void setEndByte(long endByte) {
        this.endByte = endByte;
    }

    /**
     * Returns the path to the torrent in the torrent file system.
     * This path is always returned as a unix based path. path/to/file
     */
    public String getTorrentPath() {
        return torrentPath;
    }

}
