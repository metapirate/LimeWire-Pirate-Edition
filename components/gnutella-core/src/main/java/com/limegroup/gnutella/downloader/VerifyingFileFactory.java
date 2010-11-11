package com.limegroup.gnutella.downloader;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/** A factory for creating VerifyingFiles. */
@Singleton
public class VerifyingFileFactory {
    
    private final Provider<DiskController> diskController;
    
    /** Constructs a VerifyingFileFactory that uses the given DiskController when constructing VerifyingFiles. */
    @Inject
    public VerifyingFileFactory(Provider<DiskController> diskController) {
        this.diskController = diskController;
    }

    /** Constructs a verifying file with the given completed size. */
    public VerifyingFile createVerifyingFile(long completedSize) {
        return new VerifyingFile(completedSize, diskController);
    }

    /** Constructs a verifying file for testing. */
    public VerifyingFile createVerifyingFile() {
        return new VerifyingFile(-1, diskController);
    }
    
}
