package com.limegroup.gnutella.metadata.audio.reader;

import java.io.File;
import java.io.IOException;

import com.limegroup.gnutella.metadata.ASFParser;
import com.limegroup.gnutella.metadata.MetaReader;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;


/**
 * Sets WMA metadata using the ASF parser.
 */
public class WMAReader implements MetaReader {
    
    @Override
    public AudioMetaData parse(File f) throws IOException {
        return parse(new ASFParser(f));
    }
    
    public AudioMetaData parse(ASFParser parser) throws IOException {
        AudioMetaData audioData = new AudioMetaData();
        set(audioData, parser);
        return audioData;
    }
    
    /** Sets data based on an ASF Parser. */
    private void set(AudioMetaData audioData, ASFParser data) throws IOException {
        if(data.hasVideo())
            throw new IOException("use WMV instead!");
        if(!data.hasAudio())
            throw new IOException("no audio data!");
            
        audioData.setTitle(data.getTitle());
        audioData.setAlbum(data.getAlbum());
        audioData.setArtist(data.getArtist());
        audioData.setYear(data.getYear());
        audioData.setComment(data.getComment());
        audioData.setTrack(String.valueOf(data.getTrack()));
        audioData.setBitrate(data.getBitrate());
        audioData.setLength(data.getLength());
        audioData.setGenre(data.getGenre());
        audioData.setLicense(data.getCopyright());
        
        if(data.getLicenseInfo() != null)
            audioData.setLicenseType(data.getLicenseInfo());
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "wma" };
    }

}