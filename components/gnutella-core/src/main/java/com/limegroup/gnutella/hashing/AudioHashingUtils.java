package com.limegroup.gnutella.hashing;

import java.io.File;
import java.io.IOException;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.URN;

/**
 * Utility methods for locating the start/end of the audio portion of
 * a file and calculating the non-metadata hash. 
 */
public class AudioHashingUtils {
    
    private static final Log LOG = LogFactory.getLog(AudioHashingUtils.class);
    
    /**
     * Attempts to locate the beginning and end of the audio portion of a 
     * file. If they can be located, returns a URN with a SHA1 of the non-audio
     * portion of the file, otherwise returns null.
     */
    public static URN generateNonMetaDataSHA1FromFile(File file) throws InterruptedException {
        if(!canCreateNonMetaDataSHA1(file))
            return null;
        try {
            NonMetaDataHasher hasher = getHasher(file);
            long startPosition = hasher.getStartPosition();
            long length = hasher.getEndPosition();

            length = length - startPosition;

            return URN.generateNMS1FromFile(file, startPosition, length);            
        } catch (IOException e) {
            LOG.error("IOException reading file: " + file.getName(), e);
        } catch (NumberFormatException e) {
            LOG.error("Illegal value while parsing tag size: " + file.getName(), e);
        }
        return null;
    }
    
    /**
     * Returns the appropriate NonMetaDataHasher class for the
     * given file. If no hasher exists for this file type than
     * an exception is thrown.
     */
    public static NonMetaDataHasher getHasher(File file) {
        String ext = FileUtils.getFileExtension(file);
        if(ext.equalsIgnoreCase("mp3")) {
            return new MP3NonMetaDataHasher(file);
        } else if(ext.equalsIgnoreCase("flac")) {
            return new FLACNonMetaDataHasher(file);
        } else if(ext.equalsIgnoreCase("ogg")) {
            return new OGGNonMetaDataHasher(file);
        } else {
            LOG.error("Attempted to create a Hasher for an unsupported file type: " +  file.getName());
            throw new IllegalArgumentException(ext + " not supported nmsha1 format");
        }
    }
    
    /**
     * Returns true if a non-metadata hash can successfully be
     * created from this file, false otherwise.
     */
    public static boolean canCreateNonMetaDataSHA1(File file) {
        String ext = FileUtils.getFileExtension(file);
        return ext.equalsIgnoreCase("mp3") || ext.equalsIgnoreCase("flac") || ext.equalsIgnoreCase("ogg");
    }
    

}
