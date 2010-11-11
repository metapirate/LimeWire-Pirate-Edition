package org.limewire.core.api.library;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.URN;

import com.google.common.base.Predicate;

/** A list of FileItems that are locally stored on disk. */
public interface LocalFileList extends FileList<LocalFileItem> {

    public static final String FILE_ITEM_PROPERTY = "limewire.fileitem";
    
    /**
     * Adds the given file to the list.
     * <p>
     * Returns a {@link ListeningFuture} that will notify
     * when a LocalFileItem has been created out of this file.
     */
    ListeningFuture<LocalFileItem> addFile(File file);

    /** Removes the given file from the list. */
    void removeFile(File file);
    
    /**
     * Adds all the files in the folder to the list that can pass
     * through the FileFilter.
     * <p>
     * Returns a {@link ListeningFuture} that will notify
     * when a List of potential {@link LocalFileItem LocalFileItems}
     * have been created from this folder.
     */
    ListeningFuture<List<ListeningFuture<LocalFileItem>>> addFolder(File folder, FileFilter fileFilter);
       
    /** Returns true if the list contains this file. */
    boolean contains(File file);

    /** Returns true if the list contains a file with this URN. */
    boolean contains(URN urn);
    
    /**
     * Returns the of FileItem for the given file, or null if it is not in this list.
     * This may return null if the library has not finished loading.
     */
    LocalFileItem getFileItem(File file);
    
    /**
     * Returns the of FileItem for the given urn, or null if it is not in this list.
     * This may return null if the library has not finished loading or if URN
     * cannot be cast to a com.limegroup.gnutella.URN.
     */
    LocalFileItem getFileItem(URN urn);
    
    /** Returns true if the file is addable to the list.  Always false for a directory. */
    boolean isFileAllowed(File file);
    
    /** Returns true if this directory is allowed to be added to the list. Always false for a non-directory. */
    boolean isDirectoryAllowed(File folder);
    
    /** Removes all files from the list that match the specified filter. */
    public void removeFiles(Predicate<LocalFileItem> fileFilter);

    /** Removes all files from the list */
    void clear();

}
