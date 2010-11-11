package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.limewire.util.GenericsUtils;

import com.limegroup.gnutella.URN;

public class SerialBTMetaInfo implements Serializable {
    
    private static final long serialVersionUID = -2693983731217045071L;
	
	private static final ObjectStreamField[] serialPersistentFields = 
    	ObjectStreamClass.NO_FIELDS;
    private List<byte[]> hashes;
    private SerialTorrentFileSystem fileSystem;
    private byte[] infoHash;
    private URN infoHashURN;
    private SerialOldURI[] trackers;
    private SerialDiskManagerData diskManagerData;
    private int pieceLength;
    private boolean isPrivate;
    private float historicRatio;

    // keys used between read/write object.
    static enum SerialKeys {
        HASHES, PIECE_LENGTH, FILE_SYSTEM, INFO_HASH, TRACKERS, RATIO, FOLDER_DATA, PRIVATE;
    }
    
    private void writeObject(ObjectOutputStream out) {};
    
    /**
	 * Overrides serialization method to initialize the VerifyingFolder
	 */
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		Object read = in.readObject();
		Map<SerialKeys, Serializable> toRead;
		toRead = GenericsUtils.scanForMap(read, 
						SerialKeys.class, Serializable.class, 
						GenericsUtils.ScanMode.EXCEPTION);
		
		hashes =  GenericsUtils.scanForList(toRead.get(SerialKeys.HASHES),
                                             byte[].class,
                                             GenericsUtils.ScanMode.EXCEPTION);
		Integer pieceLength = (Integer)toRead.get(SerialKeys.PIECE_LENGTH);
		fileSystem = (SerialTorrentFileSystem) toRead.get(SerialKeys.FILE_SYSTEM);
		infoHash = (byte []) toRead.get(SerialKeys.INFO_HASH);
		infoHashURN = URN.createSHA1UrnFromBytes(infoHash);
		trackers = (SerialOldURI[]) toRead.get(SerialKeys.TRACKERS);
		Float ratio = (Float)toRead.get(SerialKeys.RATIO);
        diskManagerData = (SerialDiskManagerData)toRead.get(SerialKeys.FOLDER_DATA);         
		historicRatio = ratio.floatValue();
		this.pieceLength = pieceLength.intValue();        
        if (toRead.containsKey(SerialKeys.PRIVATE))
            isPrivate = true;
	}

    public List<byte[]> getHashes() {
        return hashes;
    }

    public SerialTorrentFileSystem getFileSystem() {
        return fileSystem;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public URN getInfoHashURN() {
        return infoHashURN;
    }

    public URI[] getTrackers() throws URISyntaxException {
        URI [] uris = new URI[trackers.length];
        for(int i = 0; i < trackers.length; i++) {
            uris[i] = trackers[i].toURI();
        }
        return uris;
    }

    public SerialDiskManagerData getDiskManagerData() {
        return diskManagerData;
    }

    public int getPieceLength() {
        return pieceLength;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public float getHistoricRatio() {
        return historicRatio;
    }
}
