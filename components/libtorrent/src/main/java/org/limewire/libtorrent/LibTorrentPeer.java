package org.limewire.libtorrent;

import org.limewire.bittorrent.TorrentPeer;

import com.sun.jna.Structure;
import com.sun.jna.WString;

public class LibTorrentPeer extends Structure implements Structure.ByReference, TorrentPeer {
    public static final short source_tracker = 0x1;
    public static final short source_dht = 0x2;
    public static final short source_pex = 0x4;
    public static final short source_lsd = 0x8;

    public static final int rc4_encrypted = 0x100000;
    public static final int plaintext_encrypted = 0x200000;
    
    public int status_flags;
    public String peer_id;
    public String ip;
    public int source;
    public float up_speed;
    public float down_speed;
    public float payload_up_speed;
    public float payload_down_speed;
    public float progress;
    public String country;
    public WString clientName;

    @Override
    public String getCountry() {
        return country;
    }

    @Override
    public String getClientName() {
        if (clientName != null) {
            return clientName.toString();
        } 
        else {
            return "";
        }
        
    }
    
    @Override
    public float getDownloadSpeed() {
        return down_speed;
    }

    @Override
    public String getIPAddress() {
        return ip;
    }

    @Override
    public float getPayloadDownloadSpeed() {
        return payload_down_speed;
    }

    @Override
    public float getPayloadUploadSpeed() {
        return payload_up_speed;
    }

    @Override
    public String getPeerId() {
        return peer_id;
    }

    @Override
    public float getProgress() {
        return progress;
    }

    @Override
    public float getUploadSpeed() {
        return up_speed;
    }

    @Override
    public boolean isFromTracker() {
        return (source_tracker & source) > 0;
    }

    @Override
    public boolean isFromDHT() {
        return (source_dht & source) > 0;
    }

    @Override
    public boolean isFromPEX() {
        return (source_pex & source) > 0;
    }

    @Override
    public boolean isFromLSD() {
        return (source_lsd & source) > 0;
    }

    @Override 
    public boolean isEncrypted() {
        return (rc4_encrypted & status_flags) > 0 
            || (plaintext_encrypted & status_flags) > 0;
    }
}
