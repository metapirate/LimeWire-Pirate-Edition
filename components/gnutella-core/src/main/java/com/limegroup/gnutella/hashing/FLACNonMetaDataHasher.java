package com.limegroup.gnutella.hashing;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.flac.FlacStreamReader;
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockHeader;
import org.limewire.io.IOUtils;

/**
 * Locates the beginning and end of the audio portion of a FLAC file. This 
 * checks for ID3v2.x tags, and FLAC Headers at the begining of the file. 
 * Per FLAC spec, no tags are ever located at the end of the file so we 
 * assume EOF is end-of-audio. 
 */
class FLACNonMetaDataHasher extends NonMetaDataHasher {

    private final File file;
    
    FLACNonMetaDataHasher(File file) {
        this.file = file;
    }

    /**
     * Returns the start frame of this FLAC file. The FLAC audio 
     * stream may be preceded by an ID3 tag and a series of MetaData
     * Block Headers including an OGG_Comment header. This returns the
     * position of the first frame within the FLAC_Audio stream.
     */
    @Override
    public long getStartPosition() throws IOException {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            FlacStreamReader flacStream = new FlacStreamReader(raf);
            try {
                // attempt to locate the start of the FLAC file. ID3 tags
                // or other tags may be added before the FLAC Headers
                flacStream.findStream();
            } catch(CannotReadException e) {
                throw new IOException(e);
            }
            
            boolean isEndOfHeader = false;
            while(!isEndOfHeader) {
                MetadataBlockHeader mbh = MetadataBlockHeader.readHeader(raf);
                
                raf.seek(raf.getFilePointer() + mbh.getDataLength());
    
                isEndOfHeader = mbh.isLastBlock();
            }
            return raf.getFilePointer();
        } finally {
            IOUtils.close(raf);
        }
    }

    /**
     * Returns the position of the end of the last frame of audio in this file. For
     * FLAC files this is equal to EOF.
     */
    @Override
    public long getEndPosition() throws IOException {
        return file.length();
    }
}
