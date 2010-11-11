package com.limegroup.gnutella.metadata.video;

import java.util.ArrayList;
import java.util.List;

import org.limewire.util.NameValue;

import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 *  A composite of video data for marshalling it to and from disk. When loading
 *  meta-data from video files to LimeXMLDocuments and when writing LimeXMLDocument
 *  updates to disk.
 */
public class VideoMetaData implements MetaData {

    private String title;
    private String year;
    private int length = -1;
    private String comment;
    private String language;
    private String license;
    private int width = -1;
    private int height = -1;
    private String licensetype;
        
    public String getTitle() { return title; }
    public String getYear() { return year; }
    public int getLength() { return length; }
    public String getComment()  { return comment; }
    public String getLanguage() { return language; }
    public String getLicense() { return license; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getLicenseType() { return licensetype; }

    public void setTitle(String title) { this.title = title; }
    public void setYear(String year) { this.year = year; }
    public void setLength(int length) { this.length = length; } 
    public void setComment(String comment) { this.comment = comment; }
    public void setLanguage(String language) { this.language = language; }
    public void setLicense(String license) { this.license = license; }
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    public void setLicenseType(String licensetype) { this.licensetype = licensetype; }
    
    /**
     * The XML schema this data represents.
     */
    public String getSchemaURI() {
        return LimeXMLNames.VIDEO_SCHEMA;
    }

    /**
     * Populates the fields with the values from the LimeXMLDocument.
     */
    public void populate(LimeXMLDocument doc) {
        title   = doc.getValue(LimeXMLNames.VIDEO_TITLE);
        year    = doc.getValue(LimeXMLNames.VIDEO_YEAR);
        comment = doc.getValue(LimeXMLNames.VIDEO_COMMENTS);
        language = doc.getValue(LimeXMLNames.VIDEO_LANGUAGE);
        license = doc.getValue(LimeXMLNames.VIDEO_LICENSE);
    }

    /**
     * @return the values as a Name Value List representation
     */
    public List<NameValue<String>> toNameValueList() {
        List<NameValue<String>> list = new ArrayList<NameValue<String>>();
        add(list, title, LimeXMLNames.VIDEO_TITLE);
        add(list, year, LimeXMLNames.VIDEO_YEAR);
        add(list, length, LimeXMLNames.VIDEO_LENGTH);
        add(list, comment, LimeXMLNames.VIDEO_COMMENTS);
        add(list, language, LimeXMLNames.VIDEO_LANGUAGE);
        add(list, license, LimeXMLNames.VIDEO_LICENSE);
        add(list, width, LimeXMLNames.VIDEO_WIDTH);
        add(list, height, LimeXMLNames.VIDEO_HEIGHT);
        add(list, licensetype, LimeXMLNames.VIDEO_LICENSETYPE);
        return list;
    }
    
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
}
