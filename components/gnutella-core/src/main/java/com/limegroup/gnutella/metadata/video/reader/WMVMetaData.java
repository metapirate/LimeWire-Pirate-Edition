package com.limegroup.gnutella.metadata.video.reader;

import java.io.File;
import java.io.IOException;

import com.limegroup.gnutella.metadata.ASFParser;
import com.limegroup.gnutella.metadata.MetaReader;
import com.limegroup.gnutella.metadata.video.VideoMetaData;



/**
 * Sets WMV metadata using the ASF parser.
 */
public class WMVMetaData implements MetaReader {
    
    /** Parse using the ASF Parser. */
    @Override
    public VideoMetaData parse(File f) throws IOException {
        return parse(new ASFParser(f));
    }
    
    public VideoMetaData parse(ASFParser parser) throws IOException {
        VideoMetaData videoData = new VideoMetaData();
        set(videoData, parser);
        return videoData;
    }
    
    /** Sets data based on an ASF Parser. */
    private void set(VideoMetaData videoData, ASFParser data) throws IOException {
        if(!data.hasVideo())
            throw new IOException("no video data!");
            
        videoData.setTitle(data.getTitle());
        videoData.setYear(data.getYear());
        videoData.setComment(data.getComment());
        videoData.setLength(data.getLength());
        videoData.setWidth(data.getWidth());
        videoData.setHeight(data.getHeight());
        
        if(data.getLicenseInfo() != null)
            videoData.setLicenseType(data.getLicenseInfo());
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "wmv" };
    }

}
