package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;

/**
 *  Reads meta-data from a file. Each type of media type reader
 *  must implement this interface
 */
public interface MetaReader {
    public MetaData parse(File file) throws IOException;
    
    public String[] getSupportedExtensions();
}
