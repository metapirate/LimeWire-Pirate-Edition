package com.limegroup.gnutella.uploader;

import java.io.File;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.BandwidthManager;

@Singleton
public class FileResponseEntityFactoryImpl implements FileResponseEntityFactory {

    private final Provider<BandwidthManager> bandwidthManager;

    @Inject
    public FileResponseEntityFactoryImpl(Provider<BandwidthManager> bandwidthManager) {
        this.bandwidthManager = bandwidthManager;
        
    }
    
    public FileResponseEntity createFileResponseEntity(
            HTTPUploader uploader, File file) {
        return new FileResponseEntity(uploader, file, bandwidthManager);
    }

}
