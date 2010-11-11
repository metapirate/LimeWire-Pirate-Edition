package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Set;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.VerifyingFile;

/** A collection of {@link IncompleteFileDesc}s */
public interface IncompleteFileCollection extends FileCollection {
    
    /**
     * Adds an incomplete file to be used for partial file sharing.
     * 
     * @modifies this
     * @param incompleteFile the incomplete file.
     * @param urns the set of all known URNs for this incomplete file
     * @param name the completed name of this incomplete file
     * @param size the completed size of this incomplete file
     * @param vf the VerifyingFile containing the ranges for this inc. file
     */
    public void addIncompleteFile(File incompleteFile,
            Set<? extends URN> urns,
            String name,
            long size,
            VerifyingFile vf);

}
