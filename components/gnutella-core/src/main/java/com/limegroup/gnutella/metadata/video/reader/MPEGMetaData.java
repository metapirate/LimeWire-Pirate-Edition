package com.limegroup.gnutella.metadata.video.reader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;

import com.limegroup.gnutella.metadata.MetaReader;
import com.limegroup.gnutella.metadata.video.VideoMetaData;




/**
 * Constructs metadata for an MPEG 1 & 2 video file.
 * 
 * This is based off the work of XNap, at: 
 * http://xnap.sourceforge.net/xref/org/xnap/plugin/viewer/videoinfo/VideoFile.html
 */
public class MPEGMetaData implements MetaReader {
    
    private static final Log LOG = LogFactory.getLog(MPEGMetaData.class);
    
    private static final int PACK_START_CODE = 0x000001BA;
    private static final int SEQ_START_CODE = 0x000001B3;
    private static final int MAX_FORWARD_READ_LENGTH = 50000;
    private static final int MAX_BACKWARD_READ_LENGTH = 3000000;


    @Override
    public VideoMetaData parse(File f) throws IOException {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "r");
            VideoMetaData videoData = new VideoMetaData();
            parseMPEG(videoData, raf);
            return videoData;
        } finally {
            IOUtils.close(raf);
        }
    }

    private void parseMPEG(VideoMetaData videoData, RandomAccessFile raf) throws IOException {
        boolean firstGOP = false;
        boolean firstSEQ = false;
        boolean lastGOP = false;
        
        long initialHMS = -1;
        
        // MPEG is structured a series of codes.
        // GOP (group of picture) contains hour/minute/second of each frame
        // SEQ (sequence) contains height/width of the frame.
        
        // The height & width of the first frame are assumed to be the
        // height/width of every frame.        
        // The duration is calculated by subtracting the HMS of the first frame
        // from the HMS of the last frame.        
        
        while(true) {
            LOG.debug("Advancing to next code...");
            nextStartCode(raf);
            int code = raf.readInt();
            if(code == PACK_START_CODE && !firstGOP) {
                LOG.debug("Found GOP code");
                firstGOP = true;
                byte[] b = new byte[6];
                raf.readFully(b);
                if ((b[0] & 0xF0) == 0x20) {
                    initialHMS = getMPEGHMS(b);
                } else if ((b[0] & 0xC0) == 0x40) {
                    initialHMS = getMPEG2HMS(b);
                }
            } else if(code == SEQ_START_CODE && !firstSEQ) {
                LOG.debug("Found SEQ code");
                firstSEQ = true;
                byte[] b = new byte[3];
                raf.readFully(b);
                videoData.setWidth(((b[0] & 0xff) << 4) | (b[1] & 0xf0));
                videoData.setHeight(((b[1] & 0x0f) << 8) | (b[2] & 0xff));
            }
            
            if(firstSEQ && firstGOP)
                break;
        }
        
        // If we couldn't get the initial HMS, we don't need get the last.
        if(initialHMS != -1) {
            raf.seek(raf.length());
            while(true) {
                LOG.debug("Rewinding to prior code...");
                previousStartCode(raf);
                if(raf.readInt() == PACK_START_CODE) {
                    LOG.debug("Found GOP code");
                    lastGOP = true;
                    break;
                }
                // pretend we didn't read that int.
                raf.seek(raf.getFilePointer() - 4);
            }
            
            if (lastGOP) {
                byte[] b = new byte[6];
                long lastHMS = -1;
                raf.readFully(b);
                if ((b[0] & 0xF0) == 0x20) {
                    lastHMS = getMPEGHMS(b);
                } else if ((b[0] & 0xC0) == 0x40) {
                    lastHMS = getMPEG2HMS(b);
                }
                    
                if(lastHMS != -1)
                    videoData.setLength((int)(lastHMS - initialHMS));
            }
        }
    }
    
    /** Advances the RAF to the next code. */
    private void nextStartCode(RandomAccessFile raf) throws IOException {
        byte[] b = new byte[1024];
        int available;

        for (int i = 0; i < MAX_FORWARD_READ_LENGTH; i += available) {
            available = raf.read(b);
            if (available > 0) {
                i += available;
                for (int offset = 0; offset < available - 2; offset++) {
                    if (b[offset] == 0 && b[offset + 1] == 0 && b[offset + 2] == 1) {
                        raf.seek(raf.getFilePointer() - (available - offset));
                        return;
                    }
                }
            } else {
                throw new IOException("no start code");
            }
        }
        
        throw new IOException("no start code");
    }
    
    /** Rewinds the RAF to the prior code. */
    private void previousStartCode(RandomAccessFile raf) throws IOException {
        byte[] b = new byte[8024];

        for (int i = 0; i < MAX_BACKWARD_READ_LENGTH; i += b.length) {
            long fp = raf.getFilePointer() - b.length;
            if (fp < 0) {
                if (fp <= b.length) {
                    break;
                }
                fp = 0;
            }
            raf.seek(fp);
            raf.readFully(b);
            for (int offset = b.length - 1; offset > 1; offset--) {
                if (b[offset - 2] == 0 && b[offset - 1] == 0 && b[offset] == 1) {
                    raf.seek(raf.getFilePointer() - (b.length - offset) - 2);
                    return;
                }
            }
            
            raf.seek(raf.getFilePointer() - b.length);
        }
        
        throw new IOException("no prior start code");
    }
    
    /** Gets the hour/minute/second in seconds of MPEG-1. */
    protected long getMPEGHMS(byte[] b) {
       long low4Bytes = (((b[0] & 0xff) >> 1) & 0x03) << 30 | (b[1] & 0xff) << 22 | ((b[2] & 0xff) >> 1) << 15
                | (b[3] & 0xff) << 7 | (b[4] & 0xff) >> 1;

       return low4Bytes / 90000;
    }
    
    /** Gets the hour/minute/second in seconds of MPEG-2. */
    protected long getMPEG2HMS(byte[] b) {
        long low4Bytes = ((b[0] & 0x18) >> 3) << 30 | (b[0] & 0x03) << 28 | (b[1] & 0xff) << 20
                | ((b[2] & 0xF8) >> 1) << 15 | (b[2] & 0x03) << 13 | (b[3] & 0xff) << 5 | (b[4] & 0xff) >> 3;

        int sys_clock_extension = (b[4] & 0x3) << 7 | ((b[5] & 0xff) >> 1);

        if (sys_clock_extension == 0) {
            return low4Bytes / 90000;
        } else {
            return -1;
        }
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "mpg", "mpeg" };
    }


}
