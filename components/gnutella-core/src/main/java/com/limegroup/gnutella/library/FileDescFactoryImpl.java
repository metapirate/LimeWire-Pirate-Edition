package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Set;

import org.limewire.listener.SourcedEventMulticaster;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.licenses.LicenseFactory;

@Singleton
class FileDescFactoryImpl implements FileDescFactory {
    
    private final RareFileStrategy rareFileStrategy;
    private final SourcedEventMulticaster<FileDescChangeEvent, FileDesc> multicaster;
    private final LicenseFactory licenseFactory;
    
    @Inject
    public FileDescFactoryImpl(RareFileStrategy rareFileStrategy,
            SourcedEventMulticaster<FileDescChangeEvent, FileDesc> multicaster,
            LicenseFactory licenseFactory) {
        this.rareFileStrategy = rareFileStrategy;
        this.multicaster = multicaster;
        this.licenseFactory = licenseFactory;
    }

    @Override
    public FileDesc createFileDesc(File file, Set<? extends URN> urns, int index) {
        return new FileDescImpl(rareFileStrategy, licenseFactory, multicaster, file, urns, index);
    }
    
    @Override
    public IncompleteFileDesc createIncompleteFileDesc(File file, Set<? extends URN> urns,
            int index, String completedName, long completedSize, VerifyingFile vf) {
        return new IncompleteFileDescImpl(rareFileStrategy, licenseFactory, multicaster, file, urns,
                index, completedName, completedSize, vf);
    }

}
