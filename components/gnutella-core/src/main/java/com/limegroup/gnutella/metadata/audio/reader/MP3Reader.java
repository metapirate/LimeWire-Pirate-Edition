package com.limegroup.gnutella.metadata.audio.reader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.AbstractDataType;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v24Frames;

import com.limegroup.gnutella.metadata.audio.AudioMetaData;

/**
 *  Reads MetaData from MP3 files. This extends AudioDataReader which also
 *  handles this format. However, store files need to get checked and parsed
 *  correctly so we do that here. 
 */
public class MP3Reader extends AudioDataReader {
	
    @Override
    protected void readTag(AudioMetaData audioData, AudioFile audioFile, Tag tag) {
        MP3File mp3File = ((MP3File)audioFile);
        mp3File.getID3v1Tag();
        
        AbstractID3v2Tag v2Tag = mp3File.getID3v2Tag();
        ID3v1Tag v1Tag = mp3File.getID3v1Tag();

        // check v2 tags first if they exist
        if( v2Tag != null )
            readV2Tag(audioData, v2Tag);

        // check v1 tags next
        if( v1Tag != null )
            readV1Tag(audioData, v1Tag); 
    }
    
    /**
     * Reads v1 tags from the mp3. Only writes the field to the AudioData if
     * it has not been filled in by v2 tags
     */
    private void readV1Tag(AudioMetaData audioData, ID3v1Tag tag){
        if( audioData.getTitle() == null || audioData.getTitle().length() == 0)
            audioData.setTitle(tag.getFirstTitle());
        if( audioData.getArtist() == null || audioData.getArtist().length() == 0)
            audioData.setArtist(tag.getFirstArtist());
        if( audioData.getAlbum() == null || audioData.getAlbum().length() == 0)
            audioData.setAlbum(tag.getFirstAlbum());
        if( audioData.getYear() == null || audioData.getYear().length() == 0)
            audioData.setYear(tag.getFirstYear()); 
        if( audioData.getComment() == null || audioData.getComment().length() == 0)
            audioData.setComment(tag.getFirstComment());
        if( audioData.getGenre() == null || audioData.getGenre().length() == 0)
            audioData.setGenre(tag.getFirstGenre()); 
        if( audioData.getTrack() == null || audioData.getTrack().length() == 0) {
            try {
                audioData.setTrack(tag.getFirstTrack());
            }
            catch(UnsupportedOperationException e) {
                // id3v1.0 tags dont have tracks
            }
        }
    }
    
    /**
     * Reads v2 tags from the mp3. 
     */
    private void readV2Tag(AudioMetaData audioData, AbstractID3v2Tag tag) {
        audioData.setTitle(tag.getFirstTitle());
        audioData.setArtist(tag.getFirstArtist());
        audioData.setAlbum(tag.getFirstAlbum());
        audioData.setYear(tag.getFirstYear()); 
        audioData.setComment(tag.getFirstComment());
        audioData.setGenre(parseGenre(tag.getFirstGenre()));
        audioData.setTrack(tag.getFirstTrack());
        audioData.setLicense(tag.getFirst(ID3v24Frames.FRAME_ID_COPYRIGHTINFO));
               
        // read the PRIV tag if it exists
        Object privateTag = tag.getFrame(ID3v24Frames.FRAME_ID_PRIVATE);
        if(privateTag != null) {
            readPrivateTag(privateTag, audioData);
        }
    }
    
    /**
     * Reads the PRIV tag.
     */
    private void readPrivateTag(Object frame, AudioMetaData audioData) {
        Map<String, byte[]> map = new HashMap<String, byte[]>(5);
        
        if(frame instanceof AbstractID3v2Frame) {
            parseTagDataType((AbstractID3v2Frame) frame, map);
        } else if(frame instanceof List) {
            for(Object o : (List)frame) {
                if(o instanceof AbstractID3v2Frame) {
                    parseTagDataType((AbstractID3v2Frame) o, map);
                }
            }
        }
    }
    
    /**
     * This looks for Key Value pairs within a given ID3 Frame and adds any
     * matching pair to the map.
     */
    // This suppressWarnings is for the iterator cast. The internal list is
    // generified to AbstractDataType but it doesn't expose that in the API.
    @SuppressWarnings("unchecked")
    private void parseTagDataType(AbstractID3v2Frame frame, Map<String,byte[]> map) {
        Iterator<AbstractDataType> iterator = frame.getBody().iterator();
        while (iterator.hasNext()) {
            AbstractDataType dataType = iterator.next();
            // if we have a String value, check if the next DataType is 
            // a byte[]. If so add them to the map, otherwise ignore them
            if(dataType.getValue() instanceof String) {
                String key = (String)dataType.getValue();
                if(iterator.hasNext()) {
                    dataType = iterator.next();
                    if(dataType.getValue() instanceof byte[]) {
                        map.put(key, (byte[]) dataType.getValue());
                    }
                }
            }
        }
    }
       
    /**
     * Some genres in ID3v2 tags are displaying (XXX) numbers along side the genre.
     * If this exists it hides the number from the user
     */
    private String parseGenre(String genre){
        if( genre == null || genre.length() <= 0) 
            return genre;
        String cleanGenre = genre;
        if( genre.charAt(0) == '(') {
            int startIndex = 0;
            for(int i = 0; i < genre.length(); i++) {
                if( genre.charAt(i) == ')') {
                    startIndex = i + 1;
                }
            }            
            cleanGenre = genre.substring(startIndex);
        }
        return cleanGenre;
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "mp3" };
    }
}
