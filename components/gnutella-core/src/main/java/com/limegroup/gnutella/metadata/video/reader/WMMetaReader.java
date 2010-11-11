package com.limegroup.gnutella.metadata.video.reader;

import java.io.File;
import java.io.IOException;

import com.limegroup.gnutella.metadata.ASFParser;
import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.metadata.MetaReader;
import com.limegroup.gnutella.metadata.audio.reader.WMAReader;

public class WMMetaReader implements MetaReader {

    private final WMVMetaData wmvMetaData = new WMVMetaData();
    
    private final WMAReader wmaReader= new WMAReader();
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "asf", "wm" };
    }
    
    @Override
    public MetaData parse(File file) throws IOException {
        ASFParser p = new ASFParser(file);
        if (p.hasVideo())
            return wmvMetaData.parse(p);
        else if(p.hasAudio())
            return wmaReader.parse(p);
        else 
            throw new IOException("could not parse file");
    }
}
