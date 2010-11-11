package com.limegroup.gnutella.library;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * The list of all known files. This creates and maintains a list of 
 * directories and FileDescs. It also creates a set of FileLists which 
 * may contain subsets of all FileDescs. Files can be added to just the 
 * FileManager or loaded into both the FileManager and a specified FileList
 * once the FileDesc has been created. <p>
 *
 * This class is thread-safe.
 */
@EagerSingleton 
class FileManagerImpl implements FileManager, Service {
    
    private final LibraryImpl library;
    
    private Saver saver;
    
    /**
     * Whether the FileManager has been shutdown.
     */
    private volatile boolean shutdown;
    
    /** The background executor. */
    private final ScheduledExecutorService backgroundExecutor;
    
    private final Provider<LibraryFileData> fileData;
    private final Provider<LibraryConverter> libraryConverter;
    
    private final FileCollectionManagerImpl fileCollectionManagerImpl;

    /**
     * Creates a new <tt>FileManager</tt> instance.
     */
    @Inject
    public FileManagerImpl(LibraryImpl managedFileList,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<LibraryFileData> libraryFileData,
            Provider<LibraryConverter> libraryConverter,
            FileCollectionManagerImpl collectionManager) {        
        this.backgroundExecutor = backgroundExecutor;
        this.library = managedFileList;
        this.fileData = libraryFileData;
        this.fileCollectionManagerImpl = collectionManager;
        this.libraryConverter = libraryConverter;
    }

    @Override
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Shared Files");
    }

    @Override
    public void initialize() {
        library.initialize();
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }

    @Override
    public void start() {
        LibraryConverter converter = libraryConverter.get();
        if(converter.isOutOfDate()) {
            library.fireLoading();
            converter.convert(fileData.get());
        }
        
        fileCollectionManagerImpl.loadStoredCollections();
        library.loadManagedFiles();
        
        synchronized (this) {
            if (saver == null) {
                this.saver = new Saver();
                backgroundExecutor.scheduleWithFixedDelay(saver, 1, 1, TimeUnit.MINUTES);
            }
        }
    }

    @Override
    public void stop() {
        library.save();
        shutdown = true;
    }
    
    private class Saver implements Runnable {
        public void run() {
            if (!shutdown && library.isLoadFinished()) {
                library.save();
            }
        }
    }
}