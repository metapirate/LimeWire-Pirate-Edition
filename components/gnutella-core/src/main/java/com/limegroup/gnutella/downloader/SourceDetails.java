package com.limegroup.gnutella.downloader;

import org.limewire.core.api.transfer.SourceInfo;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.InsufficientDataException;

class SourceDetails implements SourceInfo {
    
    private final String name;
    private final float speed;
    private final String addr;
    private final boolean encrypted;
    
    public SourceDetails(DownloadWorker worker) {
        HTTPDownloader downloader = worker.getDownloader();
        if(downloader != null) {
            String agent = downloader.getServer();
            if(!StringUtils.isEmpty(agent)) {
                name = agent;
            } else {
                name = worker.getRFD().getVendor();
            }
            encrypted = downloader.isEncrypted();
            float measured = 0;
            try {
                measured = downloader.getMeasuredBandwidth()*1024;
            } catch(InsufficientDataException id) {}
            speed = measured;
        } else {
            speed = 0;
            encrypted = false;
            name = worker.getRFD().getVendor();
        }
        addr = worker.getRFD().getAddress().getAddressDescription();
        
    }

    @Override
    public String getClientName() {
        return name;
    }

    @Override
    public float getDownloadSpeed() {
        return speed;
    }

    @Override
    public String getIPAddress() {
        return addr;
    }

    @Override
    public float getUploadSpeed() {
        return 0;
    }

    @Override
    public boolean isEncyrpted() {
        return encrypted;
    }

}
