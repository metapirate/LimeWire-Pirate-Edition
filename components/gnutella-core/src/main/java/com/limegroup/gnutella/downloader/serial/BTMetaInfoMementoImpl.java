package com.limegroup.gnutella.downloader.serial;

import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BTMetaInfoMementoImpl implements BTMetaInfoMemento, Serializable {
    private static final long serialVersionUID = 4808344749443143000L;

    private Map<String, Object> serialObjects = new HashMap<String, Object>();

    @SuppressWarnings("unchecked")
    public List<byte[]> getHashes() {
        return (List<byte[]>)serialObjects.get("hashes");    
    }

    public void setHashes(List<byte[]> hashes) {
        serialObjects.put("hashes", hashes);
    }

    public int getPieceLength() {
        Integer i = (Integer)serialObjects.get("pieceLength"); 
        if(i != null) {
            return i;
        } else {
            return 0;
        }
    }

    public void setPieceLength(int pieceLength) {
        serialObjects.put("pieceLength", pieceLength);
    }

    public TorrentFileSystemMemento getFileSystem() {
        return (TorrentFileSystemMemento)serialObjects.get("fileSystem");    
    }

    public void setFileSystem(TorrentFileSystemMemento fileSystem) {
        serialObjects.put("fileSystem", fileSystem);
    }

    public byte[] getInfoHash() {
        return (byte [])serialObjects.get("infoHash");    
    }

    public void setInfoHash(byte[] infoHash) {
        serialObjects.put("infoHash", infoHash);
    }

    public float getRatio() {
        Float f = (Float)serialObjects.get("ratio"); 
        return f != null ? f : 0f;
    }

    public void setRatio(float ratio) {
        serialObjects.put("ratio", ratio);
    }

    public BTDiskManagerMemento getFolderData() {
        return (BTDiskManagerMemento)serialObjects.get("folderData");    
    }

    public void setFolderData(BTDiskManagerMemento folderData) {
        serialObjects.put("folderData", folderData);
    }

    public URI[] getTrackers() {
        return (URI [])serialObjects.get("trackers");    
    }

    public void setTrackers(URI[] trackers) {
        serialObjects.put("trackers", trackers);
    }

    public boolean isPrivate() {
        Boolean b =  (Boolean)serialObjects.get("private"); 
        return b != null ? b : false;
    }

    public void setPrivate(boolean aPrivate) {
        serialObjects.put("private", aPrivate);
    }

    @SuppressWarnings("unchecked")
    public URI[] getWebSeeds() {
        List<URI> uris = (List<URI>) serialObjects.get("url-list");
        return uris == null ? null : uris.toArray(new URI[uris.size()]);
    }

    public void setWebSeeds(URI[] webSeeds) {
        if (webSeeds != null) {
            List<URI> uris = Arrays.asList(webSeeds);
            serialObjects.put("url-list", uris);
        } else {
            serialObjects.put("url-list", new URI[0]);
        }
    }
}
