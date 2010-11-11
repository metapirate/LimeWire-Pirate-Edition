package com.limegroup.gnutella.metadata;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.CountingInputStream;
import org.limewire.io.IOUtils;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.metadata.audio.reader.WRMXML;
import com.limegroup.gnutella.metadata.audio.reader.WeedInfo;


/**
 * A parser for reading ASF files.
 * Everything we understand is stored.
 * <p>
 * This is initially based  off the work of Reed Esau, in his excellent ptarmigan package,
 * from http://ptarmigan.sourceforge.net/ .  This was also based off of the work
 * in the XNap project, from
 *  http://xnap.sourceforge.net/xref/org/xnap/plugin/viewer/videoinfo/VideoFile.html ,
 * which in turn was based off the work from the avifile project, at 
 *  http://avifile.sourceforge.net/ .
 */
public class ASFParser {
    
    private static final Log LOG = LogFactory.getLog(ASFParser.class); 
    
    // data types we know about in the extended content description.
    // THESE ARE WRONG (but close enough for now)
    private static final int TYPE_STRING = 0;
    private static final int TYPE_BINARY = 1;
    private static final int TYPE_BOOLEAN = 2;
    private static final int TYPE_INT = 3;
    private static final int TYPE_LONG = 4;
    
    private String _album, _artist, _title, _year, _copyright,
                   _rating, _genre, _comment, _drmType;
    private short _track = -1;
    private int _bitrate = -1, _length = -1, _width = -1, _height = -1;
    private boolean _hasAudio, _hasVideo;
    private WeedInfo _weed;
    private WRMXML _wrmdata;
    
    public String getAlbum() { return _album; }
    public String getArtist() { return _artist; }
    public String getTitle() { return _title; }
    public String getYear() { return _year; }
    public String getCopyright() { return _copyright; }
    public String getRating() { return _rating; }
    public String getGenre() { return _genre; }
    public String getComment() { return _comment; }
    public short getTrack() { return _track; }
    public int getBitrate() { return _bitrate; }
    public int getLength() { return _length; }
    public int getWidth() { return _width; }
    public int getHeight() { return _height; }
    
    public WeedInfo getWeedInfo() { return _weed; }
    public WRMXML getWRMXML() { return _wrmdata; }
    
    public boolean hasAudio() { return _hasAudio; }
    public boolean hasVideo() { return _hasVideo; }
    
    public String getLicenseInfo() {
        if(_weed != null)
            return _weed.getLicenseInfo();
        else if(_wrmdata != null && _drmType != null)
            return WRMXML.PROTECTED + _drmType;
        else
            return null;
    }        
    
    /**
     * Constructs a new ASFParser based off the given file, parsing all the known properties.
     */
    public ASFParser(File f) throws IOException {
        parseFile(f);
    }

    /**
     * Parses the given file for metadata we understand.
     */
    protected void parseFile(File f) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Parsing file: " + f);
        
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(f));
            parse(is);
        } catch(IOException iox) {
            LOG.warn("IOX while parsing", iox);
            throw iox;
        } finally {
            IOUtils.close(is);
        }
    }
    
    /**
     * Parses a ASF input stream's metadata.
     * This first checks that the marker (16 bytes) is correct, reads the data offset & object count,
     * and then iterates through the objects, reading them.
     * Each object is stored in the format:
     * <xmp>
     *   ObjectID (16 bytes)
     *   Object Size (4 bytes)
     *   Object (Object Size bytes)
     * </xmp>
     */
    private void parse(InputStream is) throws IOException {
        CountingInputStream counter = new CountingInputStream(is);
        DataInputStream ds = new DataInputStream(counter);
        
        byte[] marker = new byte[IDs.HEADER_ID.length];
        ds.readFully(marker);
        if(!Arrays.equals(marker, IDs.HEADER_ID))
            throw new IOException("not an ASF file");
       
        long dataOffset = ByteUtils.leb2long(ds);
        int objectCount = ByteUtils.leb2int(ds);
        IOUtils.ensureSkip(ds, 2);
        
        if(LOG.isDebugEnabled())
            LOG.debug("Data Offset: " + dataOffset + ", objectCount: " + objectCount);
        
        if (dataOffset < 0)
            throw new IOException("ASF file is corrupt. Data offset negative:"
                    +dataOffset);
        if (objectCount < 0)
            throw new IOException("ASF file is corrupt. Object count unreasonable:"
                    + ByteUtils.uint2long(objectCount));
        if(objectCount > 100)
            throw new IOException("object count very high: " + objectCount);
            
        byte[] object = new byte[16];
        for(int i = 0; i < objectCount; i++) {
            if(LOG.isDebugEnabled())
                LOG.debug("Parsing object[" + i + "]");
                
            ds.readFully(object);
            long size = ByteUtils.leb2long(ds) - 24;
            if (size < 0)
                throw new IOException("ASF file is corrupt.  Object size < 0 :"+size);
            counter.clearAmountRead();
            readObject(ds, object, size);
            int read = counter.getAmountRead();
            
            if(read > size)
                throw new IOException("read (" + read + ") more than size (" + size + ")");
            else if(read != size) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Skipping to next object.  Read: " + read + ", size: " + size);
                IOUtils.ensureSkip(ds, size - read);
            }
        }
    }
    
    /**
     * Reads a single object from a ASF metadata stream.
     * The objectID has already been read.  Each object is stored differently.
     */
    private void readObject(DataInputStream ds, byte[] id, long size) throws IOException {
        if(Arrays.equals(id, IDs.FILE_PROPERTIES_ID))
            parseFileProperties(ds);
        else if(Arrays.equals(id, IDs.STREAM_PROPERTIES_ID)) 
            parseStreamProperties(ds);
        else if(Arrays.equals(id, IDs.EXTENDED_STREAM_PROPERTIES_ID))
            parseExtendedStreamProperties(ds);
        else if(Arrays.equals(id, IDs.CONTENT_DESCRIPTION_ID))
            parseContentDescription(ds);
        else if(Arrays.equals(id, IDs.EXTENDED_CONTENT_DESCRIPTION_ID))
            parseExtendedContentDescription(ds);
        else if(Arrays.equals(id, IDs.CONTENT_ENCRYPTION_ID))
            parseContentEncryption(ds);
        else if(Arrays.equals(id, IDs.EXTENDED_CONTENT_ENCRYPTION_ID))
            parseExtendedContentEncryption(ds);
        else {
            LOG.debug("Unknown Object, ignoring.");
            // for debugging.
            //byte[] temp = new byte[size];
            //ds.readFully(temp);
            //LOG.debug("id: " + string(id) + ", data: " + string(temp));
        }
        
    }

    /** Parses known information out of the file properties object. */
    private void parseFileProperties(DataInputStream ds) throws IOException {
        LOG.debug("Parsing file properties");
        IOUtils.ensureSkip(ds, 48);
        
        int duration = (int)(ByteUtils.leb2long(ds) / 10000000);
        if (duration < 0)
            throw new IOException("ASF file corrupt.  Duration < 0:"+duration);
        _length = duration;
        IOUtils.ensureSkip(ds, 20);
        int maxBR = ByteUtils.leb2int(ds);
        if (maxBR < 0)
            throw new IOException("ASF file corrupt.  Max bitrate > 2 Gb/s:"+
                    ByteUtils.uint2long(maxBR));
        if(LOG.isDebugEnabled())
            LOG.debug("maxBitrate: " + maxBR);
        _bitrate = maxBR / 1000;
    }
    
    /** Parses stream properties to see if we have audio or video data. */
    private void parseStreamProperties(DataInputStream ds) throws IOException {
        LOG.debug("Parsing stream properties");
        byte[] streamID = new byte[16];
        ds.readFully(streamID);
        
        if(Arrays.equals(streamID, IDs.AUDIO_STREAM_ID)) {
            _hasAudio = true;
        } else if(Arrays.equals(streamID, IDs.VIDEO_STREAM_ID)) {
            _hasVideo = true;
            IOUtils.ensureSkip(ds, 38);
            _width = ByteUtils.leb2int(ds);
            if (_width < 0)
                throw new IOException("ASF file corrupt.  Video width excessive:"+
                        ByteUtils.uint2long(_width));
            _height = ByteUtils.leb2int(ds);
            if (_height < 0)
                throw new IOException("ASF file corrupt.  Video height excessive:"+
                        ByteUtils.uint2long(_height));
        }
        
        // we aren't reading everything, but we'll skip over just fine.
    }
    
    /** Parses known information out of the extended stream properties object. */
    private void parseExtendedStreamProperties(DataInputStream ds) throws IOException {
        LOG.debug("Parsing extended stream properties");
        
        IOUtils.ensureSkip(ds, 56);
        int channels = ByteUtils.ushort2int(ByteUtils.leb2short(ds));
        int sampleRate = ByteUtils.leb2int(ds);
        if (sampleRate < 0)
            throw new IOException("ASF file corrupt.  Sample rate excessive:"+
                    ByteUtils.uint2long(sampleRate));
        int byteRate = ByteUtils.leb2int(ds);
        if (byteRate < 0)
            throw new IOException("ASF file corrupt.  Byte rate excessive:"+
                    ByteUtils.uint2long(byteRate));
        if(_bitrate == -1)
            _bitrate = byteRate * 8 / 1000;
        if(LOG.isDebugEnabled())
            LOG.debug("channels: " + channels + ", sampleRate: " + sampleRate + ", byteRate: " + byteRate + ", bitRate: " + _bitrate);
    }
    
    /**
     * Parses the content encryption object, to determine if the file is protected.
     * We parse through it all, even though we don't use all of it, to ensure
     * that the object is well-formed.
     */
    private void parseContentEncryption(DataInputStream ds) throws IOException {
        LOG.debug("Parsing content encryption");
        long skipSize = ByteUtils.uint2long(ByteUtils.leb2int(ds)); // data
        IOUtils.ensureSkip(ds, skipSize);
        
        int typeSize = ByteUtils.leb2int(ds); // type
        if (typeSize < 0)
            throw new IOException("ASF file is corrupt.  Type size < 0: "+typeSize);
        byte[] b = new byte[typeSize];
        ds.readFully(b);
        _drmType = new String(b).trim();
        
        skipSize = ByteUtils.uint2long(ByteUtils.leb2int(ds)); // data
        IOUtils.ensureSkip(ds, skipSize);
        
        skipSize = ByteUtils.uint2long(ByteUtils.leb2int(ds)); // url
        IOUtils.ensureSkip(ds, skipSize);
    }   
    
    /**
     * Parses the extended content encryption object, looking for encryption's
     * we know about.
     * Currently, this is Weed.
     */
    private void parseExtendedContentEncryption(DataInputStream ds) throws IOException {
        LOG.debug("Parsing extended content encryption");
        int size = ByteUtils.leb2int(ds);
        if (size < 0)
            throw new IOException("ASF file reports excessive length of encryption data:"
                    +ByteUtils.uint2long(size));
        byte[] b = new byte[size];
        ds.readFully(b);
        String xml = new String(b, "UTF-16").trim();
        WRMXML wrmdata = new WRMXML(xml);
        if(!wrmdata.isValid()) {
            LOG.debug("WRM Data is invalid.");
            return;
        }

        _wrmdata = wrmdata;
        
        WeedInfo weed = new WeedInfo(wrmdata);
        if(weed.isValid()) {
            LOG.debug("Parsed weed data.");
            _weed = weed;
            _wrmdata = weed;
            if(_weed.getAuthor() != null)
                _artist = _weed.getAuthor();
            if(_weed.getTitle() != null)
                _title = _weed.getTitle();
            if(_weed.getDescription() != null)
                _comment = _weed.getDescription();
            if(_weed.getCollection() != null)
                _album = _weed.getCollection();
            if(_weed.getCopyright() != null)
                _copyright = _weed.getCopyright();
            return;
        }
    }
    
    /**
     * Parses known information out of the Content Description object.
     * <p>
     * The data is stored as:
     *   10 bytes of sizes (2 bytes for each size).
     *   The data corresponding to each size.  
     *   <p>
     *   The data is stored in order of:
     *   Title, Author, Copyright, Description, Rating.
     */
    private void parseContentDescription(DataInputStream ds) throws IOException {
        LOG.debug("Parsing Content Description");
        int[] sizes = { -1, -1, -1, -1, -1 };
        
        for(int i = 0; i < sizes.length; i++)
            sizes[i] = ByteUtils.ushort2int(ByteUtils.leb2short(ds));
        
        byte[][] info = new byte[5][];
        for(int i = 0; i < sizes.length; i++)
            info[i] = new byte[sizes[i]];
                
        for(int i = 0; i < info.length; i++)
            ds.readFully(info[i]);
        
        _title = string(info[0]);
        _artist = string(info[1]);
        _copyright = string(info[2]);
        _comment = string(info[3]);
        _rating = string(info[4]);
            
        if(LOG.isDebugEnabled())
            LOG.debug("Standard Tag Values.  Title: " + _title + ", Author: " + _artist + ", Copyright: " + _copyright
                         + ", Description: " + _comment + ", Rating: " + _rating);
    }
    
    /**
     * Reads the extended Content Description object.
     * The extended tag has an arbitrary number of fields.  
     * The number of fields is stored first, as:
     *      Field Count (2 bytes)
     *<p>
     * Each field is stored as:
     * <pre>
     *      Field Size (2 bytes)
     *      Field      (Field Size bytes)
     *      Data Type  (2 bytes)
     *      Data Size  (2 bytes)
     *      Data       (Data Size bytes)
     * </pre>
     */
    private void parseExtendedContentDescription(DataInputStream ds) throws IOException {
        LOG.debug("Parsing extended content description");
        int fieldCount = ByteUtils.ushort2int(ByteUtils.leb2short(ds));
        
        if(LOG.isDebugEnabled())
            LOG.debug("Extended fieldCount: " + fieldCount);
        
        for(int i = 0; i < fieldCount; i++) {
            int fieldSize = ByteUtils.ushort2int(ByteUtils.leb2short(ds));
            byte[] field = new byte[fieldSize];
            ds.readFully(field);
            String fieldName = string(field);
            int dataType = ByteUtils.ushort2int(ByteUtils.leb2short(ds));
            int dataSize = ByteUtils.ushort2int(ByteUtils.leb2short(ds));
            
            switch(dataType) {
            case TYPE_STRING:
                parseExtendedString(fieldName, dataSize, ds);
                break;
            case TYPE_BINARY:
                parseExtendedBinary(fieldName, dataSize, ds);
                break;
            case TYPE_BOOLEAN:
                parseExtendedBoolean(fieldName, dataSize, ds);
                break;
            case TYPE_INT:
                parseExtendedInt(fieldName, dataSize, ds);
                break;
            case TYPE_LONG:
                parseExtendedInt(fieldName, dataSize, ds);
                break;
            default: 
                if(LOG.isDebugEnabled())
                    LOG.debug("Unknown dataType: " + dataType + " for field: " + fieldName);
                IOUtils.ensureSkip(ds, dataSize);
            }
        }
    }
    
    /**
     * Parses a value from an extended tag, assuming the value is of the 'string' dataType.
     */
    private void parseExtendedString(String field, int size, DataInputStream ds) throws IOException {
        byte[] data = new byte[Math.min(250, size)];
        ds.readFully(data);
        int leftover = Math.max(0, size - 250);
        IOUtils.ensureSkip(ds, leftover);
        String info = string(data);
        
        if(LOG.isDebugEnabled())
            LOG.debug("Parsing extended String.  field: " + field + ", Value: " + info);
        
        if(Extended.WM_TITLE.equals(field)) {
            if(_title == null)
                _title = info;
        } else if(Extended.WM_AUTHOR.equals(field)) {
            if(_artist == null)
                _artist = info;
        } else if(Extended.WM_ALBUMTITLE.equals(field)) {
            if(_album == null)
                _album = info;
        } else if(Extended.WM_TRACK_NUMBER.equals(field)) {
            if(_track == -1)
                _track = toShort(info);
        } else if(Extended.WM_YEAR.equals(field)) {
            if(_year == null)
                _year = info;
        } else if(Extended.WM_GENRE.equals(field)) {
            if(_genre == null)
                _genre = info;
        } else if(Extended.WM_DESCRIPTION.equals(field)) {
            if(_comment == null)
                _comment = info;
        }
    }
    
    /**
     * Parses a value from an extended tag, assuming the value is of the 'boolean' dataType.
     */
    private void parseExtendedBoolean(String field, int size, DataInputStream ds) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Ignoring boolean field: " + field + ", size: " + size);
            
        IOUtils.ensureSkip(ds, size);
    }
    
    /**
     * Parses a value from an extended tag, assuming the value is of the 'int' dataType.
     */
    private void parseExtendedInt(String field, int size, DataInputStream ds) throws IOException {
        if(size != 4) {
            if(LOG.isDebugEnabled())
                LOG.debug("Int field size != 4, ignoring.   Field: " + field + ", size: " + size);
            IOUtils.ensureSkip(ds, size);
            return;
        }
        
        int value = ByteUtils.leb2int(ds);
        if(LOG.isDebugEnabled())
            LOG.debug("Parsing extended int, field: " + field + ", size: " + size + ", value: " + value);
            
        if(Extended.WM_TRACK_NUMBER.equals(field)) {
            if(_track == -1) {
                short shortValue = (short)value;
                if (shortValue < 0)
                    throw new IOException("ASF file reports negative track number "+shortValue);
                _track = shortValue;
            }
        }
    }
    
    /**
     * Parses a value from an extended tag, assuming the value is of the 'binary' dataType.
     */
    private void parseExtendedBinary(String field, int size, DataInputStream ds) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Ignoring binary field: " + field + ", size: " + size);        
            
        IOUtils.ensureSkip(ds, size);
    }
    
    /**
     * Parses a value from an extended tag, assuming the value is of the 'long' dataType.
     */
    @SuppressWarnings("unused")
    private void parseExtendedLong(String field, int size, DataInputStream ds) throws IOException {
        if(size != 8) {
            if(LOG.isDebugEnabled())
                LOG.debug("Long field size != 8, ignoring.   Field: " + field + ", size: " + size);
            IOUtils.ensureSkip(ds, size);
            return;
        }
        
        long value = ByteUtils.leb2long(ds);
        if(LOG.isDebugEnabled())
            LOG.debug("Ignoring long field: " + field + ", size: " + size + ", value: " + value);
    }
    
    /** Converts a String to a short, if it can. */
    private short toShort(String x) {
        try {
            return Short.parseShort(x);
        } catch(NumberFormatException nfe) {
            return -1;
        }
    }
    
    /**
     * Returns a String uses ASF's encoding (WCHAR: UTF-16 little endian).
     * If we don't support that encoding for whatever, hack out the zeros.
     */
    private String string(byte[] x) throws IOException {
        if(x == null)
            return null;
            
        try {
            return new String(x, "UTF-16LE").trim();
        } catch(UnsupportedEncodingException uee) {
            // hack.
            int pos = 0;
            for(int i = 0; i < x.length; i++) {
                if(x[i] != 0)
                    x[pos++] = x[i];
            }
            return new String(x, 0, pos, "UTF-8");
        }
    }
    
    private static class IDs {
        private static final byte HEADER_ID[] =
            { (byte)0x30, (byte)0x26, (byte)0xB2, (byte)0x75, (byte)0x8E, (byte)0x66, (byte)0xCF, (byte)0x11,
              (byte)0xA6, (byte)0xD9, (byte)0x00, (byte)0xAA, (byte)0x00, (byte)0x62, (byte)0xCE, (byte)0x6C };
            
        private static final byte FILE_PROPERTIES_ID[] =
            { (byte)0xA1, (byte)0xDC, (byte)0xAB, (byte)0x8C, (byte)0x47, (byte)0xA9, (byte)0xCF, (byte)0x11,
              (byte)0x8E, (byte)0xE4, (byte)0x00, (byte)0xC0, (byte)0x0C, (byte)0x20, (byte)0x53, (byte)0x65 };
              
        private static final byte STREAM_PROPERTIES_ID[] =
            { (byte)0x91, (byte)0x07, (byte)0xDC, (byte)0xB7, (byte)0xB7, (byte)0xA9, (byte)0xCF, (byte)0x11,
              (byte)0x8E, (byte)0xE6, (byte)0x00, (byte)0xC0, (byte)0x0C, (byte)0x20, (byte)0x53, (byte)0x65 };
            
        private static final byte EXTENDED_STREAM_PROPERTIES_ID[] =
            { (byte)0xCB, (byte)0xA5, (byte)0xE6, (byte)0x14, (byte)0x72, (byte)0xC6, (byte)0x32, (byte)0x43,
              (byte)0x83, (byte)0x99, (byte)0xA9, (byte)0x69, (byte)0x52, (byte)0x06, (byte)0x5B, (byte)0x5A };
            
        private static final byte CONTENT_DESCRIPTION_ID[] =
            { (byte)0x33, (byte)0x26, (byte)0xB2, (byte)0x75, (byte)0x8E, (byte)0x66, (byte)0xCF, (byte)0x11,
              (byte)0xA6, (byte)0xD9, (byte)0x00, (byte)0xAA, (byte)0x00, (byte)0x62, (byte)0xCE, (byte)0x6C };
            
        private static final byte EXTENDED_CONTENT_DESCRIPTION_ID[] =
            { (byte)0x40, (byte)0xA4, (byte)0xD0, (byte)0xD2, (byte)0x07, (byte)0xE3, (byte)0xD2, (byte)0x11,
              (byte)0x97, (byte)0xF0, (byte)0x00, (byte)0xA0, (byte)0xC9, (byte)0x5E, (byte)0xA8, (byte)0x50 };
            
        private static final byte CONTENT_ENCRYPTION_ID[] =
            { (byte)0xFB, (byte)0xB3, (byte)0x11, (byte)0x22, (byte)0x23, (byte)0xBD, (byte)0xD2, (byte)0x11,
              (byte)0xB4, (byte)0xB7, (byte)0x00, (byte)0xA0, (byte)0xC9, (byte)0x55, (byte)0xFC, (byte)0x6E };
            
        private static final byte EXTENDED_CONTENT_ENCRYPTION_ID[] =
            { (byte)0x14, (byte)0xE6, (byte)0x8A, (byte)0x29, (byte)0x22, (byte)0x26, (byte)0x17, (byte)0x4C,
              (byte)0xB9, (byte)0x35, (byte)0xDA, (byte)0xE0, (byte)0x7E, (byte)0xE9, (byte)0x28, (byte)0x9C };
            
        @SuppressWarnings("unused")
        private static final byte CODEC_LIST_ID[] =
            { (byte)0x40, (byte)0x52, (byte)0xD1, (byte)0x86, (byte)0x1D, (byte)0x31, (byte)0xD0, (byte)0x11,
              (byte)0xA3, (byte)0xA4, (byte)0x00, (byte)0xA0, (byte)0xC9, (byte)0x03, (byte)0x48, (byte)0xF6 };
              
        private static final byte AUDIO_STREAM_ID[] =
            { (byte)0x40, (byte)0x9E, (byte)0x69, (byte)0xF8, (byte)0x4D, (byte)0x5B, (byte)0xCF, (byte)0x11, 
              (byte)0xA8, (byte)0xFD, (byte)0x00, (byte)0x80, (byte)0x5F, (byte)0x5C, (byte)0x44, (byte)0x2B };
              
        private static final byte VIDEO_STREAM_ID[] = 
           { (byte)0xC0, (byte)0xEF, (byte)0x19, (byte)0xBC, (byte)0x4D, (byte)0x5B, (byte)0xCF, (byte)0x11, 
             (byte)0xA8, (byte)0xFD, (byte)0x00, (byte)0x80, (byte)0x5F, (byte)0x5C, (byte)0x44, (byte)0x2B };
    }
    
    
    private static class Extended {
        /** the title of the file */
        private static final String WM_TITLE = "WM/Title";
        
        /** the author of the file */
        private static final String WM_AUTHOR = "WM/Author";
        
        /** the title of the album the file is on */
        private static final String WM_ALBUMTITLE = "WM/AlbumTitle";
        
        /** the zero-based track of the song */
        @SuppressWarnings("unused")
        private static final String WM_TRACK = "WM/Track";
        
        /** the one-based track of the song */
        private static final String WM_TRACK_NUMBER = "WM/TrackNumber";
        
        /** the year the song was made */
        private static final String WM_YEAR = "WM/Year";
        
        /** the genre of the song */
        private static final String WM_GENRE = "WM/Genre";
        
        /** the description of the song */
        private static final String WM_DESCRIPTION = "WM/Description";
        
        /** the lyrics of the song */
        @SuppressWarnings("unused")
        private static final String WM_LYRICS = "WM/Lyrics";
        
        /** whether or not this is encoded in VBR */
        @SuppressWarnings("unused")
        private static final String VBR = "IsVBR";
        
        /** the unique file identifier of this song */
        @SuppressWarnings("unused")
        private static final String WM_UNIQUE_FILE_IDENTIFIER = "WM/UniqueFileIdentifier";
        
        /** the artist of the album as a whole */
        @SuppressWarnings("unused")
        private static final String WM_ALBUMARTIST = "WM/AlbumArtist";
        
        /** the encapsulated ID3 info */
        @SuppressWarnings("unused")
        private static final String ID3 = "ID3";
        
        /** the provider of the song */
        @SuppressWarnings("unused")
        private static final String WM_PROVIDER = "WM/Provider";
        
        /** the rating the provider gave this song */
        @SuppressWarnings("unused")
        private static final String WM_PROVIDER_RATING = "WM/ProviderRating";
        
        /** the publisher */
        @SuppressWarnings("unused")
        private static final String WM_PUBLISHER = "WM/Publisher";
        
        /** the composer */
        @SuppressWarnings("unused")
        private static final String WM_COMPOSER = "WM/Composer";
        
        /** the time the song was encoded */
        @SuppressWarnings("unused")
        private static final String WM_ENCODING_TIME = "WM/EncodingTime";
        
    }
}
