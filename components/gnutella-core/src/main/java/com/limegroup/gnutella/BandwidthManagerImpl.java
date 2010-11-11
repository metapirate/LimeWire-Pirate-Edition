package com.limegroup.gnutella;

import java.net.Socket;

import org.limewire.core.settings.DownloadSettings;
import org.limewire.nio.NBThrottle;
import org.limewire.nio.Throttle;
import org.limewire.rudp.RUDPSocket;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BandwidthManagerImpl implements BandwidthManager {

    private final Throttle UP_TCP, DOWN_TCP, UP_UDP;
    
    private final UploadServices uploadServices;
    
    @Inject
    public BandwidthManagerImpl(UploadServices uploadServices) {
        this.uploadServices = uploadServices;
        
        UP_TCP = new NBThrottle(true,0);
        DOWN_TCP = new NBThrottle(false,0);
        UP_UDP = new NBThrottle(true, 0);
    }
    
    public void applyRate() {
        applyDownloadRate();
        applyUploadRate();
    }
    
    private void applyDownloadRate() {
        float downloadRate = DownloadSettings.MAX_DOWNLOAD_SPEED.getValue();

        if ( !DownloadSettings.LIMIT_MAX_DOWNLOAD_SPEED.getValue() ) {
            downloadRate = Float.MAX_VALUE;
        }
        DOWN_TCP.setRate(downloadRate);
    }
    
    public void applyUploadRate() {
        float uploadRate = uploadServices.getRequestedUploadSpeed(); 
        UP_TCP.setRate(uploadRate);
        UP_UDP.setRate(uploadRate);
    }
    
    public Throttle getReadThrottle() {
        applyDownloadRate();
        return DOWN_TCP;
    }
    
    public Throttle getWriteThrottle() {
        applyUploadRate();
        return UP_TCP;
    }
    
    public Throttle getWriteThrottle(Socket socket) {
        applyUploadRate();
        return (socket instanceof RUDPSocket) ? UP_UDP : UP_TCP;
    }

}
