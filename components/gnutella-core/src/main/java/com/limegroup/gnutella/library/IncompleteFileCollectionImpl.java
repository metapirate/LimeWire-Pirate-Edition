package com.limegroup.gnutella.library;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.Set;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.listener.SourcedEventMulticaster;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/** A collection of IncompleteFileDescs. */
@Singleton
class IncompleteFileCollectionImpl extends AbstractFileCollection implements IncompleteFileCollection {
    
    private LibraryImpl managedList;

    @Inject
    public IncompleteFileCollectionImpl(LibraryImpl managedList, 
            SourcedEventMulticaster<FileViewChangeEvent, FileView> multicaster) {
        super(managedList, multicaster);
        this.managedList = managedList;
    }
    
    @Override
    public String getName() {
        return "Incomplete Collection";
    }

    public void addIncompleteFile(File incompleteFile, Set<? extends URN> urns, String name,
            long size, VerifyingFile vf) {
        managedList.addIncompleteFile(incompleteFile, urns, name, size, vf);
    }
    
    @Override
    public ListeningFuture<FileDesc> add(File file) {
        throw new UnsupportedOperationException("cannot add from here");
    }
    
    @Override
    public ListeningFuture<FileDesc> add(File file, List<? extends LimeXMLDocument> documents) {
        throw new UnsupportedOperationException("cannot add from here");
    }
    
    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> addFolder(File folder, FileFilter fileFilter) {
        throw new UnsupportedOperationException("cannot add from here");
    }
    
    @Override
    protected boolean isFileDescAllowed(FileDesc fileDesc) {
        return fileDesc instanceof IncompleteFileDesc;
    }

    @Override
    protected boolean isPending(File file, FileDesc fd) {
        return fd instanceof IncompleteFileDesc;
    }
    
    @Override
    protected void saveChange(File file, boolean added) {
        // Don't save incomplete status.
    }

    @Override
    public boolean isFileAllowed(File file) {
        return true;
    }
    
    @Override
    public boolean isDirectoryAllowed(File folder) {
        return false;
    }
    
    
}
