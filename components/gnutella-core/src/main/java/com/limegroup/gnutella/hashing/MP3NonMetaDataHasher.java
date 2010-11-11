package com.limegroup.gnutella.hashing;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.TagException;
import org.limewire.util.StringUtils;

/**
 * Locates the beginning and end of the audio portion of an mp3 file. This 
 * checks for ID3v1.0-ID3v2.4 tags, LYRICS3 tags, and APE tags. Tags
 * located at the end of the audio stream are explicitely checked for. As
 * a result, any padding added to the end of the audio stream is considered
 * part of the audio portion of the file. 
 */
class MP3NonMetaDataHasher extends NonMetaDataHasher {
    
    /** Begining String of a LYRICS3 tag. */
    private static final String LYRICSBEGIN = "LYRICSBEGIN";
    
    /** Ending String of a LYRICS3v1.0 tag. */
    private static final String LYRICSEND_V1 = "LYRICSEND";
    
    /** Ending String of a LYRICS3v2.0 tag. */
    private static final String LYRICSEND_V2 = "LYRICS200";
    
    /** Begining/Ending String of a APE tag. */
    private static final String APETAG = "APETAGEX";
    
    private final File file;
    
    MP3NonMetaDataHasher(File file) { 
        this.file = file;
    }
    
    /**
     * Returns the start position of the audio portion of this mp3. 
     */
    @Override
    public long getStartPosition() throws IOException {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            if(!(audioFile instanceof MP3File)) {
                throw new IOException("Cannot cast to a MP3File");
            }
            MP3File mp3File = (MP3File) audioFile;
            return mp3File.getMP3StartByte(mp3File.getFile());
        } catch (InvalidAudioFrameException e) {
            throw new IOException(e);
        } catch (CannotReadException e) {
            throw new IOException(e);
        } catch (TagException e) {
            throw new IOException(e);
        } catch (ReadOnlyFileException e) {
            throw new IOException(e);
        } 
    }

    /**
     * Checks the end of an Mp3File for various metadata tags. These
     * include ID3v1.0, ID3v1.1, LYRIC3V1, LYRIC3V2, and APE tags. Other
     * metadata may exist there but this is the most common.
     * 
     * ID3v1.x tags are always checked first and if they exist will always appear
     * last within the file. LYRIC3 tag or APE tags may exist before an ID3v1.x
     * tag or if an ID3v1.x tag doesn't exist, at the end of the file. We assume
     * LYRIC3 and APE tags can never coexist. Both are extremely rare and will
     * hardly ever exist to begin with. 
     *
     * Returns the position within the file where the audio or padding preceding 
     * any of these tags. If no tags are located, returns the length of the audio file. 
     */
    @Override
    public long getEndPosition() throws IOException {
        AudioFile audioFile;
        try {
            audioFile = AudioFileIO.read(file);
        } catch (CannotReadException e) {
            throw new IOException(e);
        } catch (TagException e) {
            throw new IOException(e);
        } catch (ReadOnlyFileException e) {
            throw new IOException(e);
        } catch (InvalidAudioFrameException e) {
            throw new IOException(e);
        }
        if(!(audioFile instanceof MP3File)) {
            throw new IOException("Cannot cast to a MP3File");
        }
        MP3File mp3File = (MP3File) audioFile;
        
        long fileLength = file.length();
        
        int offset = 0;
        // id3 tag v1.0 and v1.1 will always will be last and is always 128 bytes.
        if(mp3File.hasID3v1Tag()) {
            LOG.debug("found ID3v1 tag");
            offset = 128;
            fileLength -= 128;
        }
        
        // This buffer is large enough to locate the existense of all footer
        // tags. Extra IO may be needed if an APE or LYRICS3 footer tag is
        // located but both of these tags are extremely rare.
        ByteBuffer buffer = ByteBuffer.allocate(32);
        fillBuffer(buffer, mp3File.getFile(), offset);
        
        if(buffer.limit() < 32) {
            throw new IOException("Couldn't fill buffer while parsing footers");
        }
        
        // check for the LYRICSv1.0 & LYRICSv2.0 tag
        // these tags will precede ID3v1.x tag or EOF
        String lyricsTag = getLyricsFooterTag(buffer);
        if(LYRICSEND_V1.equals(lyricsTag)) {
            LOG.debug("found LYRICS3 footer tag");
            // 5100 is the maximum length of a LYRICSv1.0 tag, whether the 9 byte footer 
            // tag is included in this in undefined
            ByteBuffer newByteBuffer = ByteBuffer.allocate(5100 + 9);
            fillBuffer(newByteBuffer, mp3File.getFile(), offset);
            fileLength -= getSizeLyricsTagV1(newByteBuffer);
        } else if(LYRICSEND_V2.equals(lyricsTag)) {
            LOG.debug("found LYRICS3v2 footer tag");
            fileLength -= getSizeLyricsTagV2(buffer, offset, mp3File);
        }
        
        // check for APE tag, this will precede EOF, or ID3v1.x
        if(containsAPETag(buffer)) {
            LOG.debug("found APE footer tag");
            fileLength -= getSizeAPETag(buffer, offset, mp3File);
        }
        
        return fileLength;
    }
    
    /**
     * LYRICS3 tags is an all text descriptor that contains lyrical information
     * about an mp3. LYRICS3 tags are located at the end of an mp3 or if an
     * ID3v1.x tag exists, immediately preceding the ID3v1.x tag.
     * 
     * There are currently two different version of LRYICS3 tags, v1 and v2. The
     * end of a LYRICS3 tag will contain a 9 byte string. LYRICS3v1.0 footer tags
     * can be identified with the String "LYRICSEND". LYRICS3v2.0 footer tags
     * can be identified with the String "LYRICS200". 
     *
     * This returns the String comprised of the 9 bytes immediately preceding 
     * EOF or if an ID3v1.x tag exists, the 9 bytes preceding that tag. 
     * 
     * http://www.id3.org/Lyrics3
     */
    private static String getLyricsFooterTag(ByteBuffer buffer) {
        byte[] bytes = new byte[9];

        buffer.position(buffer.limit() - 9);
        buffer.get(bytes);
        return StringUtils.getASCIIString(bytes);
    }
    
    /**
     * Lyrics3v1 tags are located at the end of an mp3 file or right before
     * an ID3v1.x tag. The last 9 bytes are LRYICSEND. If this tag is found,
     * search back 5100 bytes and begin scanning for the LYRICSBEGIN tag. 
     * LYRICSBEGIN is considered the start of the Lyrics3v1 tag.
     * 
     * http://www.id3.org/Lyrics3
     */
    private static long getSizeLyricsTagV1(ByteBuffer buffer) throws IOException {
        byte[] bytes = new byte[11];
               
        for(int i = 0; i < buffer.limit() - bytes.length; i++) {
            buffer.position(i);
            buffer.get(bytes);
            if(LYRICSBEGIN.equals(StringUtils.getASCIIString(bytes))) {
                LOG.debug("found LYRICS3 header tag");
                // tag size is the start of the LYRICS3 tag within the buffer to the 
                // offset location of the last calculated tag
                return buffer.limit() - i;
            }
        }
        // start tag was not found
        throw new IOException("Could not find BEGIN LYRICS3v1 tag");
    }

    /**
     * Attempts to locate the beginning on a LYRICS3v2.0 tag. This assumes that 
     * a LYRICS3v2.0 footer tag has already been located. To locate the 
     * beginning of a LYRICS3v2.0 tag, the 6 bytes preceding the footer tag 
     * should be read and converted to a number. This number will tell the 
     * length of the LYRICS3v2.0 tag NOT including the size of the footer tag or
     * the 6 bytes describing the Tag size. 
     * 
     * After locating the beginning of the LYRICS3 tag, the first 9 bytes must 
     * read the LYRICS3 starting String "LYRICSBEGIN".
     * 
     * http://www.id3.org/Lyrics3v2
     */
    private static long getSizeLyricsTagV2(ByteBuffer buffer, int offset, MP3File mp3File) throws NumberFormatException, IOException {
        buffer.position(buffer.limit() - 9 - 6);
        
        byte[] bytes = new byte[6];
        buffer.get(bytes);
        
        long startOfTag = Long.parseLong(StringUtils.getASCIIString(bytes));
        
        //sanity check on the tag size
        if(startOfTag <= 0 || startOfTag + 9 + 6 + offset > mp3File.getFile().length())
            throw new IOException("LYRICS3v2 tag size too large");

         //create a new buffer and read the bytes where head of tag is located.
         ByteBuffer newByteBuffer = ByteBuffer.allocate(11);
         fillBuffer(newByteBuffer, mp3File.getFile(), (int)(offset + startOfTag + 6 + 9 - newByteBuffer.capacity()));
            
         String beginTag = StringUtils.getASCIIString(newByteBuffer.array());
        
        if(LYRICSBEGIN.equals(beginTag)) {
            LOG.debug("found LYRICS3v2 header tag");
            // return the stated size of the tag plus the 6 bytes describing the size
            // plus the 9 bytes of the footer tag
            return 9 + 6 + startOfTag;
        } else {
            //this should never happen, LRYIC BEGIN/END tags should
            //always be matching. 
            throw new IOException("Could not locate BEGIN LYRICS3v2 tag");
        }
    }

    /**
     * Returns true if an APE tag is located. APE tags are located at the
     * end of a file. APEv1 and APEv2 contain a 32 byte footer. The APE footer
     * tag will precede an ID3v1.x tag or be located at the end of the file.
     * 
     * The first 8 bytes of an APE tag footer will contain the String "APETAGEX".
     * 
     * http://wiki.hydrogenaudio.org/index.php?title=APEv2_specification
     */
    private static boolean containsAPETag(ByteBuffer buffer) {
        byte[] bytes = new byte[8];
        buffer.position(buffer.limit() - 32);
        buffer.get(bytes);
        
        String footerTag = StringUtils.getASCIIString(bytes);
        return APETAG.equals(footerTag);
    }
    
    /**
     * The APETAG Footer looks like this:
     * 8 bytes - "APETAGEX"
     * 4 bytes - version number
     * 4 bytes - size of tag, including the footer, but NOT including any header
     * 4 bytes - number of items in the tag
     * 4 bytes - global flags
     * 8 bytes - unused (all 0s)
     *
     * After determining the length of the APETAG, scan back the length of the tag
     * plus 32 bytes. APEv2.0 tags will have a matching 32 byte header not specified 
     * in the length of the tag, APEv1.0 tags will NOT have a header. Check if a 
     * APETAG header is located at the start of the tag, if so a APETAGv2.0 tag has been
     * found, otherwise we assume a APETAGv1.0 tag is being used if a valid footer is located.
     */
    private static long getSizeAPETag(ByteBuffer buffer, int offset, MP3File mp3File) throws IOException {
        // read the footer of the tag again
        byte[] bytes = new byte[32]; 
        buffer.position(buffer.limit() - 32);
        buffer.get(bytes);
        
        // the footer of the tag is 32 bytes long, the first 96 bits contains the
        // Preabmle String and the version number. The next 32 bits contains a 
        // little endian Integer that represents the size of the entire tag.
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int tagSize = buffer.getInt(12);
        
        //sanity check on this value
        if(tagSize <= 0 || offset + tagSize + 32 > mp3File.getFile().length())
            throw new IOException("APE tagsize too large");
        
        // locate the start of the tag in a new buffer and check for the TagHeader
        ByteBuffer newByteBuffer = ByteBuffer.allocate(8);
        fillBuffer(newByteBuffer, mp3File.getFile(), offset + tagSize + 32 - newByteBuffer.capacity());
        String headerTag = StringUtils.getASCIIString(newByteBuffer.array());
        
        LOG.debug("found APE header tag");
        // if a tag header is found, v2.0 otherwise assume its v1.0
        if(APETAG.equals(headerTag)) {
            return tagSize + 32;  //APEv2.0 tag
        } else {
            return tagSize;       //APEv1.0
        }
    }
}
