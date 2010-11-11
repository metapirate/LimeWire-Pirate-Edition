package com.limegroup.gnutella.hashing;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.ogg.OggVorbisTagReader;
import org.jaudiotagger.audio.ogg.OggVorbisTagReader.OggVorbisHeaderSizes;
import org.limewire.io.IOUtils;

/**
 * Locates the beginning and end of the audio portion of an OGG file. This 
 * checks for Codec, Comment and Setup Headers at the begining of the file. 
 * Per OGG spec, no tags are ever located at the end of the file so we 
 * assume EOF is end-of-audio. 
 */
class OGGNonMetaDataHasher extends NonMetaDataHasher {

    private final File file;
    
    OGGNonMetaDataHasher(File file) {
        this.file = file;
    }

    /**
     * Returns the start Frame of an Ogg file. An Ogg file must contain three
     * Header values: a Codec, a Comment and a Setup Header. The Ogg stream 
     * begins after these three headers. This returns the position of the first frame
     * after all three header values. 
     */
    @Override
    public long getStartPosition() throws IOException {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            OggVorbisTagReader reader  = new OggVorbisTagReader();
            OggVorbisHeaderSizes tag = reader.readOggVorbisHeaderSizes(raf);

            return tag.getCommentHeaderStartPosition() + tag.getCommentHeaderSize() + tag.getSetupHeaderSize() + tag.getExtraPacketDataSize();
        } catch (CannotReadException e) {
            throw new IOException(e);
        } finally {
            IOUtils.close(raf);
        }
    }
    
    /**
     * Returns the position of the end of the last frame of audio in this file. For
     * OGG files this is equal to EOF.
     */
    @Override
    public long getEndPosition() throws IOException {
        return file.length();
    }
}
