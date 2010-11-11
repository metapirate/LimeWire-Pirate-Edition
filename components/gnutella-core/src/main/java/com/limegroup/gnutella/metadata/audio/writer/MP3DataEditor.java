
package com.limegroup.gnutella.metadata.audio.writer;


import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.ID3v11Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v23Tag;

import com.limegroup.gnutella.metadata.audio.AudioMetaData;

/**
 * Returns the correct ID3 tag for the mp3. ID3v2 tags are considered better than
 * version ID3v1 tags. There for if both tags exists, the ID3v2 tag is updated. 
 * If no tag exists yet, creates and returns an ID3v2.3. If we happen across a 
 * ID3v1.0 tag, we update it to a ID3v1.1b tag which adds comments, track and genre. 
 */
public class MP3DataEditor extends AudioDataEditor {
    
    @Override
    protected Tag createTag(AudioFile audioFile, AudioMetaData audioData) {
        
        if( audioFile.getTag() == null )
            return new ID3v23Tag();
        MP3File mp3File = (MP3File)audioFile;
        // if v2 tag is available, use that one
        if(mp3File.hasID3v2Tag()) { 
            return mp3File.getID3v2Tag();
        } else if( mp3File.hasID3v1Tag()) { 
            ID3v1Tag tag = mp3File.getID3v1Tag();
            // if we try to write a copyright to file, must be ID3v2 tag
            if( audioData.getLicense() != null && !audioData.getLicense().equals("")) {
                return new ID3v23Tag(tag);
            }
            else if( tag instanceof ID3v11Tag ) { 
                return tag;
            } else {
                // v1.0 tags don't support track or genres. Being that its used so rarely, just update
                // the tag to v1.1b to not break our implementation
                return new ID3v11Tag(tag);
            }
        } else { // this should never happen but just in case
            return new ID3v23Tag();
        }
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "mp3" };
    }
}
