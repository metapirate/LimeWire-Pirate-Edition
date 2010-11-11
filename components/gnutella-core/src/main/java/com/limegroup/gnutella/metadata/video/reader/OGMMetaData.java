package com.limegroup.gnutella.metadata.video.reader;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.limewire.io.IOUtils;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.metadata.MetaReader;
import com.limegroup.gnutella.metadata.video.VideoMetaData;

/**
 * Reads MetaData from Ogg Media Formats.
 */
public class OGMMetaData implements MetaReader {

    public static final String TITLE_TAG = "title";

    public static final String COMMENT_TAG = "comment";

    public static final String LICENSE_TAG = "license";

    private static final String DATE_TAG = "date";

    private static final String LANGUAGE_TAG = "language";

    @Override
    public VideoMetaData parse(File file) throws IOException {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(is);
            Set<String> set = readMetaData(dis);
            VideoMetaData videoData = new VideoMetaData();
            parseMetaData(videoData, set);
            return videoData;
        } finally {
            IOUtils.close(is);
        }
    }

    /**
     * Reads the first pages of the Ogg container, extracts all Vorbis comments.
     * 
     * @param dis a DataInputStream
     * @return Set of String containing Vorbis comments
     * @throws IOException
     */
    private Set<String> readMetaData(DataInputStream dis) throws IOException {
        Set<String> set = new HashSet<String>();
        boolean shouldStop = false;
        do {
            int pageSize = readHeader(dis);
            shouldStop = parseCommentBlock(pageSize, dis, set);
        } while (!shouldStop);
        return set;
    }

    /**
     * Reads the header of an Ogg page.
     * 
     * @param dis the DataInputStream to read from
     * @return size of the rest of the page.
     * @throws IOException
     */
    private int readHeader(DataInputStream dis) throws IOException {
        // read pageHeader
        if (dis.readByte() != 'O')
            throw new IOException("not an ogg file");
        if (dis.readByte() != 'g')
            throw new IOException("not an ogg file");
        if (dis.readByte() != 'g')
            throw new IOException("not an ogg file");
        if (dis.readByte() != 'S')
            throw new IOException("not an ogg file");

        // boring data
        IOUtils.ensureSkip(dis, 22);

        // number of page segments
        int segments = dis.readUnsignedByte();
        int size = 0;
        for (int i = 0; i < segments; i++) {
            size += dis.readUnsignedByte();
        }

        return size;
    }

    /*
     * parse what we hope is a comment block. If that's not the case, we mostly
     * skip the data.
     */
    private boolean parseCommentBlock(int pageSize, DataInputStream dis, Set<String> comments)
            throws IOException {
        int type = dis.readByte();
        pageSize--;

        if ((type & 1) != 1) {
            // we are reading a data block, stop.
            IOUtils.ensureSkip(dis, pageSize);
            return true;
        } else if (type != 3) {
            IOUtils.ensureSkip(dis, pageSize);
            // reading some header block
            return false;
        }

        byte[] vorbis = new byte[6];
        dis.readFully(vorbis);
        pageSize -= 6;

        if (vorbis[0] != 'v' || vorbis[1] != 'o' || vorbis[2] != 'r' || vorbis[3] != 'b'
                || vorbis[4] != 'i' || vorbis[5] != 's') {
            // not a vorbis comment
            IOUtils.ensureSkip(dis, pageSize);
            return true;
        }

        // read size of vendor string
        byte[] dword = new byte[4];
        dis.readFully(dword);
        int vendorStringSize = ByteUtils.leb2int(dword, 0);

        // read vendor string
        byte[] vendorString = new byte[vendorStringSize];
        dis.readFully(vendorString);

        // read number of comments
        dis.readFully(dword);
        int numComments = ByteUtils.leb2int(dword, 0);

        // read comments
        for (int i = 0; i < numComments; i++) {
            dis.readFully(dword);
            int commentSize = ByteUtils.leb2int(dword, 0);
            byte[] comment = new byte[commentSize];
            dis.readFully(comment);
            comments.add(new String(comment, "UTF-8"));
        }
        // last bit marker missing -> error
        if ((dis.readByte() & 1) != 1)
            return true;
        return false;
    }

    /**
     * Extracts usable information from a Set of Vorbis comments.
     * 
     * @param data a Set of String containing Vorbis comments
     */
    private void parseMetaData(VideoMetaData videoData, Set<String> data) {
        for (String comment : data) {
            int index = comment.indexOf('=');
            if (index <= 0)
                continue;
            String key = comment.substring(0, index);
            String value = comment.substring(index + 1);

            if (key.equalsIgnoreCase(COMMENT_TAG)) {
                if (videoData.getComment() != null)
                    videoData.setComment(videoData.getComment() + "\n" + value);
                else
                    videoData.setComment(value);
            } else if (key.equalsIgnoreCase(LANGUAGE_TAG)) {
                if (videoData.getLanguage() != null)
                    videoData.setLanguage(videoData.getLanguage() + ";" + value);
                else
                    videoData.setLanguage(value);
            } else if (key.equalsIgnoreCase(LICENSE_TAG)) {
                videoData.setLicense(value);
            } else if (key.equalsIgnoreCase(TITLE_TAG)) {
                videoData.setTitle(value);
            } else if (key.equalsIgnoreCase(DATE_TAG)) {
                videoData.setYear(value);
            }
        }
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "ogm" };
    }

}
