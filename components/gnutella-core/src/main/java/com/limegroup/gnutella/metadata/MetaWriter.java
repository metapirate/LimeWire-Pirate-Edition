package com.limegroup.gnutella.metadata;

import com.limegroup.gnutella.metadata.audio.AudioMetaData;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection.MetaDataState;

/**
 * Writes metadata to disk, each type of media editor must implement
 * this interface.
 */
public interface MetaWriter {

	/**
     * Writes the meta data contained in AudioMetaData to the file.
     */
    public MetaDataState commitMetaData(String filename, AudioMetaData audioData);

    /**
     * Returns an array of extensions whose meta data can be written.
     */
    public String[] getSupportedExtensions();
}
