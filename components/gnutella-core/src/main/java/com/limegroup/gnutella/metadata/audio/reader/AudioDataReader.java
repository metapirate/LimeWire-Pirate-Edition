package com.limegroup.gnutella.metadata.audio.reader;

import java.io.File;
import java.io.IOException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;

import com.limegroup.gnutella.metadata.MetaReader;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;

/**
 *  Handles the reading of most audio files. All file types supported by 
 *  jAudioTagger can use this class to read their meta data.
 */
public class AudioDataReader implements MetaReader {

    public static final String ISO_LATIN_1 = "8859_1";
    public static final String UNICODE = "Unicode";
    public static final String MAGIC_KEY = "NOT CLEARED";
    public static final String SHAREABLE = "SHAREABLE";
   
    /**
     * Reads header information about the file. All audio formats contain
     * some sort of header information to describe how the audio file is encoded.
     * This typically includes sample rate, bit rate, length, encoding scheme, etc.
     */
    private void readHeader(AudioMetaData audioData, AudioHeader header) {
        audioData.setVBR(header.isVariableBitRate());
        audioData.setSampleRate(header.getSampleRateAsNumber());
        audioData.setBitrate((int)header.getBitRateAsNumber());
        audioData.setLength(header.getTrackLength());
    }
    
    /**
     * Reads any metadata the user may have added to this audio format. Each audio
     * type has its own format for describing the audio file. 
     */
    protected void readTag(AudioMetaData audioData, AudioFile audioFile, Tag tag){
        audioData.setTitle(tag.getFirstTitle());
        audioData.setArtist(tag.getFirstArtist());
        audioData.setAlbum(tag.getFirstAlbum());
        audioData.setYear(tag.getFirstYear());
        audioData.setComment(tag.getFirstComment());
        audioData.setTrack(tag.getFirstTrack());
        audioData.setGenre(tag.getFirstGenre());
        
        //if an ogg or flac file, try reading the license
        if(tag instanceof VorbisCommentTag) {
            audioData.setLicense(tag.getFirst("LICENSE"));
        }
    }
    
    /**
     * Handles the reading and parsing of this file.
     * @param file file to read
     * @throws IOException - thrown if the file can't be read, is corrupted, etc.
     */
    @Override
    public AudioMetaData parse(File file) throws IOException { 
        try {
            AudioMetaData audioData = new AudioMetaData();
            AudioFile audioFile = AudioFileIO.read(file);
            readHeader(audioData, audioFile.getAudioHeader());          
            readTag(audioData, audioFile, audioFile.getTag());
            return audioData;
        } catch (CannotReadException e) { 
            throw (IOException)new IOException().initCause(e);
        } catch (TagException e) {
            throw (IOException)new IOException().initCause(e);
        } catch (ReadOnlyFileException e) {
            throw (IOException)new IOException().initCause(e);
        } catch (InvalidAudioFrameException e) {
            throw (IOException)new IOException().initCause(e);
        }
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "fla", "flac", "m4a", "m4p", "ogg", "wav", "ra", "ram"};
    }

}
