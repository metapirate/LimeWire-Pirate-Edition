package com.limegroup.gnutella.metadata.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.limewire.util.NameValue;

import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 *  A composite of audio data for marshalling it to and from disk. When loading
 *  meta-data from audio files to LimeXMLDocuments and when writing LimeXMLDocument
 *  updates to disk.
 */
public class AudioMetaData implements MetaData {

    private String title;
    private String artist;
    private String album;
    private String year;
    private String comment;
    private String track;
    private String genre;
    private int bitrate = -1;
    private int length = -1;
    private short totalTracks =-1;
    private short disk=-1;
    private short totalDisks=-1;
    private String license;
    private String licensetype;
    private String channels;
    private int sampleRate = -1;
    private boolean isVBR = false;
    
    /**
     * @return the XML schema this data represents.
     */
    public String getSchemaURI() {
        return LimeXMLNames.AUDIO_SCHEMA;
    }

    public void populate(LimeXMLDocument doc) {
        title   = (doc.getValue(LimeXMLNames.AUDIO_TITLE) == null ) ? "" : doc.getValue(LimeXMLNames.AUDIO_TITLE);
        artist  = (doc.getValue(LimeXMLNames.AUDIO_ARTIST) == null) ? "" : doc.getValue(LimeXMLNames.AUDIO_ARTIST);
        album   = (doc.getValue(LimeXMLNames.AUDIO_ALBUM) == null) ? "" : doc.getValue(LimeXMLNames.AUDIO_ALBUM);
        year    = (doc.getValue(LimeXMLNames.AUDIO_YEAR) == null) ? "" : doc.getValue(LimeXMLNames.AUDIO_YEAR);
        track   = (doc.getValue(LimeXMLNames.AUDIO_TRACK) == null) ? "" : doc.getValue(LimeXMLNames.AUDIO_TRACK);
        comment = (doc.getValue(LimeXMLNames.AUDIO_COMMENTS) == null) ? "" : doc.getValue(LimeXMLNames.AUDIO_COMMENTS);
        genre   = (doc.getValue(LimeXMLNames.AUDIO_GENRE) == null) ? "" : doc.getValue(LimeXMLNames.AUDIO_GENRE);
        license = (doc.getValue(LimeXMLNames.AUDIO_LICENSE) == null) ? "" : doc.getValue(LimeXMLNames.AUDIO_LICENSE);
    }

    public List<NameValue<String>> toNameValueList() {
        List<NameValue<String>> list = new ArrayList<NameValue<String>>();
        add(list, title, LimeXMLNames.AUDIO_TITLE);
        add(list, artist, LimeXMLNames.AUDIO_ARTIST);
        add(list, album, LimeXMLNames.AUDIO_ALBUM);
        add(list, year, LimeXMLNames.AUDIO_YEAR);
        add(list, comment, LimeXMLNames.AUDIO_COMMENTS);
        add(list, track, LimeXMLNames.AUDIO_TRACK);
        add(list, genre, LimeXMLNames.AUDIO_GENRE);
        add(list, bitrate, LimeXMLNames.AUDIO_BITRATE);
        add(list, length, LimeXMLNames.AUDIO_SECONDS);
        add(list, license, LimeXMLNames.AUDIO_LICENSE);
        add(list, licensetype, LimeXMLNames.AUDIO_LICENSETYPE);
        return list;
    }
    
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getYear() { return year; }
    public String getComment()  { return comment; }
    public String getTrack() { return track; }
    public short getTotalTracks() {return totalTracks;}
    public short getDisk() {return disk;}
    public short getTotalDisks() {return totalDisks;}
    public String getGenre() { return genre; }
    public int getBitrate() { return bitrate; }
    public int getLength() { return length; }
    public String getLicense() { return license; }
    public String getLicenseType() { return licensetype; }
    public String getNumChannels(){ return channels; }
    public int getSampleRate(){ return sampleRate; }
    public boolean isVBR(){ return isVBR; }
    
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }    
    public void setAlbum(String album) { this.album = album; }
    public void setYear(String year) { this.year = year; }
    public void setComment(String comment) { this.comment = comment; }    
    public void setTrack(String track) { this.track = track; }    
    public void setTotalTracks(short total) { totalTracks = total; }    
    public void setDisk(short disk) { this.disk =disk; }
    public void setTotalDisks(short total) { totalDisks=total; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setBitrate(int bitrate) { this.bitrate = bitrate; }    
    public void setLength(int length) { this.length = length; }    
    public void setLicense(String license) { this.license = license; }
    public void setLicenseType(String licensetype) { this.licensetype = licensetype; }
    public void setNumChannels(String channels) { this.channels = channels; }
    public void setSampleRate(int sampleRate) { this.sampleRate = sampleRate; }
    public void setVBR(boolean isVBR) { this.isVBR = isVBR; }

    private void add(List<NameValue<String>> list, String value, String key) {
        if(isValid(value))
            list.add(new NameValue<String>(key, value.trim()));
    }
    
    private void add(List<NameValue<String>> list, int value, String key) {
        if(isValid(value))
            list.add(new NameValue<String>(key, "" + value));
    }
    
    private boolean isValid(String s) {
        return s != null && !s.trim().equals("");
    }
    
    private boolean isValid(int i) {
        return i >= 0;
    }
    
    
    /**
     * Determines whether a LimeXMLDocument was corrupted by
     * ID3Editor in the past.
     */
    public static boolean isCorrupted(LimeXMLDocument doc) {
        if(!LimeXMLNames.AUDIO_SCHEMA.equals(doc.getSchemaURI()))
            return false;

        for(Map.Entry<String, String> entry : doc.getNameValueSet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            // album & artist were the corrupted fields ...
            if( name.equals(LimeXMLNames.AUDIO_ALBUM) || name.equals(LimeXMLNames.AUDIO_ARTIST) ) {
                if( value.length() == 30 ) {
                    // if there is a value in the 29th char, but not
                    // in the 28th, it's corrupted. 
                    if( value.charAt(29) != ' ' && value.charAt(28) == ' ' )
                        return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Creates a new LimeXMLDocument without corruption.
     */
    public static LimeXMLDocument fixCorruption(LimeXMLDocument oldDoc, LimeXMLDocumentFactory limeXMLDocumentFactory) {
        List<NameValue<String>> info = new ArrayList<NameValue<String>>(oldDoc.getNumFields());
        for(Map.Entry<String, String> entry : oldDoc.getNameValueSet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            // album & artist were the corrupted fields ...
            if( name.equals(LimeXMLNames.AUDIO_ALBUM) || name.equals(LimeXMLNames.AUDIO_ARTIST) ) {
                if( value.length() == 30 ) {
                    // if there is a value in the 29th char, but not
                    // in the 28th, it's corrupted erase & trim.
                    if( value.charAt(29) != ' ' && value.charAt(28) == ' ' )
                        value = value.substring(0, 29).trim();
                }
            }
            info.add(new NameValue<String>(name, value));
        }
        return limeXMLDocumentFactory.createLimeXMLDocument(info, oldDoc.getSchemaURI());
    }

    public static boolean isNonLimeAudioField(String fieldName) {
        return !fieldName.equals(LimeXMLNames.AUDIO_TRACK) &&
        !fieldName.equals(LimeXMLNames.AUDIO_ARTIST) &&
        !fieldName.equals(LimeXMLNames.AUDIO_ALBUM) &&
        !fieldName.equals(LimeXMLNames.AUDIO_TITLE) &&
        !fieldName.equals(LimeXMLNames.AUDIO_GENRE) &&
        !fieldName.equals(LimeXMLNames.AUDIO_YEAR) &&
        !fieldName.equals(LimeXMLNames.AUDIO_COMMENTS) &&
        !fieldName.equals(LimeXMLNames.AUDIO_BITRATE) &&
        !fieldName.equals(LimeXMLNames.AUDIO_SECONDS) &&
        !fieldName.equals(LimeXMLNames.AUDIO_LICENSE) &&
        !fieldName.equals(LimeXMLNames.AUDIO_PRICE) &&
        !fieldName.equals(LimeXMLNames.AUDIO_LICENSETYPE)
        ;
    }

}
