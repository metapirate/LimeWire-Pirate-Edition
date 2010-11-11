package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;

/**
 * Factory for returning readers and writers of metadata. 
 */
public interface MetaDataFactory {
    
    /**
     * Returns an editor for a file if one exists or null if LimeWire
     * does not support editing the file type meta data.
     */
    public MetaWriter getEditorForFile(String name);
    
    /**
     * Returns true if this factory contains an editor for this file type,
     * false otherwise.
     */
    public boolean containsEditor(String name);
    
    /**
     * Reads the meta data from the file if the file type is supported
     * or return null if reading the file meta data if not supported.
     */
    public MetaData parse(File f) throws IOException;
    
    /**
     * Returns true if this factory contains a reader for this file type,
     * false otherwise.
     */
    public boolean containsReader(File f);
    
    /**
     * Returns true if this factory contains an audio reader for this file type,
     * false otherwise.
     */
    public boolean containsAudioReader(File f);
    
    /**
     * Returns true if this factory contains a video reader for this file type,
     * false otherwise.
     */
    public boolean containsVideoReader(File f);
    
    /**
     * Registers a reader factory for a number of file extensions.
     * @throws IllegalArgumentException if another factory is already registered
     * for one of the given extensions
     */
    void registerReader(MetaReader reader);
}
