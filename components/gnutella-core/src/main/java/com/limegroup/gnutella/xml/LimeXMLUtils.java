/*
 * XMLUtils.java
 *
 * Created on April 30, 2001, 4:51 PM
 */
package com.limegroup.gnutella.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.limewire.io.IOUtils;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.I18NConvert;
import org.limewire.util.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Contains utility methods.
 * @author  asingla
 */
public class LimeXMLUtils {
    
    private static final Log LOG = LogFactory.getLog(LimeXMLUtils.class);

    private static final double MATCHING_RATE = .9;

    private static final String C_HEADER_BEGIN = "{";
    private static final String C_HEADER_END   = "}";
    private static final String C_HEADER_NONE_VAL = "plaintext";
    private static final String C_HEADER_ZLIB_VAL = "deflate";
    private static final String C_HEADER_GZIP_VAL = "gzip";
    
    private static final String COMPRESS_HEADER_ZLIB = 
        C_HEADER_BEGIN + C_HEADER_ZLIB_VAL + C_HEADER_END;
    private static final String COMPRESS_HEADER_GZIP = 
        C_HEADER_BEGIN + C_HEADER_GZIP_VAL + C_HEADER_END;
    private static final String COMPRESS_HEADER_NONE = 
        C_HEADER_BEGIN + C_HEADER_END;

    
    private static final int NONE = 0;
    private static final int GZIP = 1;
    private static final int ZLIB = 2;

    
    /**
     * Gets the text content of the child nodes.
     * This is the same as Node.getTextContent(), but exists on all
     * JDKs.
     */
    public static String getTextContent(Node node) {
        return getText(node.getChildNodes());
    }
    
    /**
     * Collapses a list of CDATASection, Text, and predefined EntityReference
     * nodes into a single string.  If the list contains other types of nodes,
     * those other nodes are ignored.
     */
    public static String getText(NodeList nodeList) {
        StringBuilder buffer = new StringBuilder();
        for(int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            switch(node.getNodeType()) {
                case Node.CDATA_SECTION_NODE :
                case Node.TEXT_NODE :
                    buffer.append(node.getNodeValue());
                    break;
                case Node.ENTITY_REFERENCE_NODE :
                    if(node.getNodeName().equals("amp"))
                        buffer.append('&');
                    else if(node.getNodeName().equals("lt"))
                        buffer.append('<');
                    else if(node.getNodeName().equals("gt"))
                        buffer.append('>');
                    else if(node.getNodeName().equals("apos"))
                        buffer.append('\'');
                    else if(node.getNodeName().equals("quot"))
                        buffer.append('"');
                    // Any other entity references are ignored
                    break;
                default :
                    // All other nodes are ignored
             }
         }
         return buffer.toString();
    }

    /**
     * Writes <CODE>string</CODE> into writer, escaping &, ', ", <, and >
     * with the XML escape strings.
     */
    public static void writeEscapedString(Writer writer, String string)
        throws IOException {
        for(int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if(c == '<')
                writer.write("&lt;");
            else if(c == '>')
                writer.write("&gt;");
            else if(c == '&')
                writer.write("&amp;");
            else if(c == '\'')
                writer.write("&apos;");
            else if(c == '"')
                writer.write("&quot;");
            else
		writer.write(c);
        }
    }
    
    /**
     * Reads all the bytes from the passed input stream till end of stream
     * reached.
     * @param in the input stream to read from
     * @return array of bytes read
     * @exception IOException If any I/O exception occurs while reading data
     */
    public static byte[] readFully(InputStream in) throws IOException {
        //create a new byte array stream to store the read data
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        
        //read the bytes till EOF
        byte[] buffer = new byte[1024];
        int bytesRead;
        while((bytesRead = in.read(buffer)) != -1)
        {
            //append the bytes read to the byteArray buffer
            byteArray.write(buffer,0,bytesRead);
        }
        
        //return the bytes read
        return byteArray.toByteArray();
    }
    
    
    /**
     * Compares the queryDoc with the replyDoc and finds out if the
     * replyDoc is a match for the queryDoc.
     * @param replyDoc potential reply Document
     * @param queryDoc the query Document
     * @return true if the replyDoc is a match for the queryDoc, false
     * otherwise
     */
    public static boolean match(LimeXMLDocument replyDoc,
                                LimeXMLDocument queryDoc,
                                boolean allowAllNulls) {
        if(queryDoc == null || replyDoc == null)
            throw new NullPointerException("querying with null doc.");

        //First find the names of all the fields in the query
        Collection<Map.Entry<String, String>> queryNameValues = queryDoc.getNameValueSet();
        int size = queryNameValues.size();
        int matchCount = 0; // number of matches
        int nullCount = 0; // number of fields in query not in replyDoc.
        boolean matchedBitrate = false;
        for(Map.Entry<String, String> entry : queryNameValues) {
            String currFieldName = entry.getKey();
            String queryValue = entry.getValue();
            assert queryValue != null : "null value";
            if (queryValue.equals(""))
                continue; // "" matches everything!!
            String replyDocValue = replyDoc.getValue(currFieldName);
            
            if (currFieldName.endsWith("license_type__") && queryValue.length() > 0) {
                if (replyDocValue == null || !replyDocValue.startsWith(queryValue))
                    return false;
                }

            if (replyDocValue == null || replyDocValue.equals(""))
                nullCount++;
            else {
                try {  
                    // if this is a parse-able numeric value, doing a prefix
                    // matching doesn't make sense.  cast it to a double and do
                    // a straight equals comparison
                    double rDVD = Double.parseDouble(replyDocValue);
                    double qVD  = Double.parseDouble(queryValue);
                    if (rDVD == qVD) {
                        matchCount++;
                        if (currFieldName.equals(LimeXMLNames.AUDIO_BITRATE))
                            matchedBitrate = true;
                    }
                    continue;
                } catch (NumberFormatException nfe) {
                    // just roll through and try to do a normal test...
                } 
                // we used to do a .equalsIgnoreCase, but that is a little too
                // rigid.  so do a ignore case prefix match.
                String queryValueLC = queryValue.toLowerCase(Locale.US);
                String replyDocValueLC = I18NConvert.instance().getNorm(replyDocValue);
                if (replyDocValueLC.startsWith(queryValueLC) ||
                        replyDocValueLC.indexOf(" " + queryValueLC) >= 0)
                    matchCount++;
            }
        }
        // The metric of a correct match is that whatever fields are specified
        // in the query must have prefix match* with the fields in the reply
        // unless the reply has a null for that field, in which case we are OK 
        // with letting it slide.  also, %MATCHING_RATE of the fields must
        // either be a prefix match or null.
        // We make an exception for queries of size 1 field. In this case, there
        // must be a 100% match (which is trivially >= %MATCHING_RATE)
        // * prefix match assumes a string; for numerics just do an equality test
        double sizeD = size;
        double matchCountD = matchCount;
        double nullCountD = nullCount;

        if (size > 1) {
            if (matchedBitrate) {
                // discount a bitrate match.  matching bitrate's shouldn't
                // influence the logic because where size is 2, a matching
                // bitrate will result in a lot of irrelevant results.
                sizeD--;
                matchCountD--;
                matchCount--;
            }
            if (((nullCountD + matchCountD)/sizeD) < MATCHING_RATE)
                return false;
            // ok, it passed rate test, now make sure it had SOME matches...
            if (allowAllNulls || matchCount > 0)
                return true;
            else
                return false;
        }
        else if (size == 1) {
            if(allowAllNulls && nullCount == 1)
                return true;
            if(matchCountD/sizeD < 1)
                return false;
            return true;
        }
        //this should never happen - size >0
        return false;
    }
    
    /**
     * Scans over the given String and returns a new String that contains
     * no invalid whitespace XML characters if any exist.  If none exist
     * the original string is returned.
     * <p>
     * This DOES NOT CONVERT entities such as & or <, it will only remove
     * invalid characters such as \u0002, \u0004, etc...
     */
    public static String scanForBadCharacters(String input) {
        if(input == null)
            return null;
        
        int length = input.length();
        //lazily create the buffer so that we can scan & return the string
        //itself w/o recreating it if we didn't have to.
        StringBuilder buffer = null;
        for (int i = 0; i < length; ) {
            int c = input.codePointAt(i);
            // TODO: do other types need to be blanked out?
            if(Character.getType(c) == Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE) {
                if(buffer == null)
                    buffer = createBuffer(input, i);
                buffer.append(' ');
            } else {
                if(buffer != null)
                    buffer.appendCodePoint(c);
            }
            
            i += Character.charCount(c);
        }
        
        if(buffer == null)
            return input;
        else
            return buffer.toString();
    }
    
    /**
     * Attempts to unencode any leftover encoded entities in the XML.
     * This is generally caused by poor ID3 writers that write "&amp;" instead of "&".
     */
    public static String unencodeXML(String input) {
        //return null, if null is passed as argument
        if(input == null)
            return null;
        
        int length = input.length();        
        
        //lazily create the buffer so that we can scan & return the string
        //itself w/o recreating it if we didn't have to.
        StringBuilder buffer = null;

        for (int i = 0; i < length; ) {
            int c = input.codePointAt(i);
            if(c == '&') {
                if(input.regionMatches(i+1, "amp;", 0, 4)) {
                    if(buffer == null)
                        buffer = createBuffer(input, i);
                    buffer.append("&");
                    i += 4;
                } else if(input.regionMatches(i+1, "lt;", 0, 3)) {
                    if(buffer == null)
                        buffer = createBuffer(input, i);
                    buffer.append("<");
                    i += 3;
                } else if(input.regionMatches(i+1, "gt;", 0, 3)) {
                    if(buffer == null)
                        buffer = createBuffer(input, i);
                    buffer.append(">");
                    i += 3;
                } else if(input.regionMatches(i+1, "quot;", 0, 5)) {
                    if(buffer == null)
                        buffer = createBuffer(input, i);
                    buffer.append("\"");
                    i += 5;
                } else if(input.regionMatches(i+1, "apos;", 0, 5)) {
                    if(buffer == null)
                        buffer = createBuffer(input, i);
                    buffer.append("'");
                    i += 5;
                } else {
                    if(buffer != null)
                        buffer.appendCodePoint(c);
                }
            } else { 
                if(buffer != null)
                    buffer.appendCodePoint(c);
            }
            
            i += Character.charCount(c);
        }
        
        // If we never created the buffer, return the string itself.
        if(buffer == null)
            return input;
        else
            return buffer.toString();       
    }
    
    /**
     * Parses the passed string, and encodes the special characters (used in
     * xml for special purposes) with the appropriate codes.
     * e.g. '<' is changed to '&lt;'
     * @return the encoded string. Returns null, if null is passed as argument
     */
    public static String encodeXML(String input) {
        //return null, if null is passed as argument
        if(input == null)
            return null;
        
        int length = input.length();        
        
        //lazily create the buffer so that we can scan & return the string
        //itself w/o recreating it if we didn't have to.
        StringBuilder buffer = null;

        for (int i = 0; i < length; ) {
            int c = input.codePointAt(i);
            // TODO: do other types need to be blanked out?
            if(Character.getType(c) == Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE) {
                if(buffer == null)
                    buffer = createBuffer(input, i);
                buffer.append(' ');
            } else {
                switch (c) {
                case '&':
                    if(buffer == null)
                        buffer = createBuffer(input, i);
                    buffer.append("&amp;");
                    break;
                case '<':
                    if(buffer == null)
                        buffer = createBuffer(input, i);
                    buffer.append("&lt;");
                    break;
                case '>':
                    if(buffer == null)
                        buffer = createBuffer(input, i);
                    buffer.append("&gt;");
                    break;
                case '\"':
                    if(buffer == null)
                        buffer = createBuffer(input, i);
                    buffer.append("&quot;");
                    break;
                case '\'':
                    if(buffer == null)
                        buffer = createBuffer(input, i);
                    buffer.append("&apos;");
                    break;
                default:
                    if(buffer != null)
                        buffer.appendCodePoint(c);
                }
            }
            
            i += Character.charCount(c);
        }
        
        // If we never created the buffer, return the string itself.
        if(buffer == null)
            return input;
        else
            return buffer.toString();
    }
    
    /** Creates a StringBuilder from the given data, up to the right length. */
    private static StringBuilder createBuffer(String data, int upTo) {
        StringBuilder sb = new StringBuilder(data.length() * 2);
        sb.append(data, 0, upTo);
        return sb;
    }

    /** @return A properly formatted version of the input data.
     */
    public static byte[] compress(byte[] data) {

        byte[] compressedData = null;
        if (shouldCompress(data)) 
                compressedData = compressZLIB(data);
        
        byte[] retBytes = null;
        if (compressedData != null) {
            retBytes = new byte[COMPRESS_HEADER_ZLIB.length() +
                               compressedData.length];
            System.arraycopy(StringUtils.toAsciiBytes(COMPRESS_HEADER_ZLIB),
                             0,
                             retBytes,
                             0,
                             COMPRESS_HEADER_ZLIB.length());
            System.arraycopy(compressedData, 0,
                             retBytes, COMPRESS_HEADER_ZLIB.length(),
                             compressedData.length);
        }
        else {  // essentially compress failed, just send prefixed raw data....
            retBytes = new byte[COMPRESS_HEADER_NONE.length() +
                                data.length];
            System.arraycopy(StringUtils.toAsciiBytes(COMPRESS_HEADER_NONE),
                             0,
                             retBytes,
                             0,
                             COMPRESS_HEADER_NONE.length());
            System.arraycopy(data, 0,
                             retBytes, COMPRESS_HEADER_NONE.length(),
                             data.length);

        }

        return retBytes;
    }


    /** Currently, all data is compressed.  In the future, this will handle
     *  heuristics about whether data should be compressed or not.
     */
    private static boolean shouldCompress(byte[] data) {
        if (data.length >= 1000)
            return true;
        else
            return false;
    }

    /** Returns a ZLIB'ed version of data. */
    private static byte[] compressZLIB(byte[] data) {
        DeflaterOutputStream gos = null;
        Deflater def = null;
        try {
            def = new Deflater();
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            gos=new DeflaterOutputStream(baos, def);
            gos.write(data, 0, data.length);
            gos.flush();
            gos.close(); // required to flush data  -- flush doesn't do it.
            //            System.out.println("compression savings: " + ((1-((double)baos.toByteArray().length/(double)data.length))*100) + "%");
            return baos.toByteArray();
        } catch (IOException e) {
            //This should REALLY never happen because no devices are involved.
            //But could we propagate it up.
            assert false : "Couldn't write to byte stream";
            return null;
        } finally {
            IOUtils.close(gos);
            IOUtils.close(def);
        }
    }


    /** Returns a GZIP'ed version of data. */
    /*
    private static byte[] compressGZIP(byte[] data) {
        try {
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            DeflaterOutputStream gos=new GZIPOutputStream(baos);
            gos.write(data, 0, data.length);
            gos.flush();
            gos.close();                      //flushes bytes
            //            System.out.println("compression savings: " + ((1-((double)baos.toByteArray().length/(double)data.length))*100) + "%");
            return baos.toByteArray();
        } catch (IOException e) {
            //This should REALLY never happen because no devices are involved.
            //But could we propogate it up.
            Assert.that(false, "Couldn't write to byte stream");
            return null;
        }
    } */

    /** @return Correctly uncompressed data (according to Content-Type header) 
     *  May return a byte[] of length 0 if something bad happens. 
     */
    public static byte[] uncompress(byte[] data) throws IOException {
        byte[] retBytes = new byte[0];
        String headerFragment = StringUtils.getASCIIString(data, 0, C_HEADER_BEGIN.length());
        if (headerFragment.equals(C_HEADER_BEGIN)) {
            // we have well formed input (so far)
            boolean found = false;
            int i=0;
            for(; i<data.length && !found; i++)
                if(data[i]==(byte)125)
                    found = true;
            //We know know that "{" is at 1 because we are in this if block
            headerFragment = StringUtils.getASCIIString(data,1,i-1-1);
            int comp = getCompressionType(headerFragment);
            if (comp == NONE) {
                retBytes = new byte[data.length-(headerFragment.length()+2)];
                System.arraycopy(data,
                                 i,
                                 retBytes,
                                 0,
                                 data.length-(headerFragment.length()+2));
            }
            else if (comp == GZIP) {
                retBytes = new byte[data.length-COMPRESS_HEADER_GZIP.length()];
                System.arraycopy(data,
                                 COMPRESS_HEADER_GZIP.length(),
                                 retBytes,
                                 0,
                                 data.length-COMPRESS_HEADER_GZIP.length());
                retBytes = uncompressGZIP(retBytes);                
            }
            else if (comp == ZLIB) {
                retBytes = new byte[data.length-COMPRESS_HEADER_ZLIB.length()];
                System.arraycopy(data,
                                 COMPRESS_HEADER_ZLIB.length(),
                                 retBytes,
                                 0,
                                 data.length-COMPRESS_HEADER_ZLIB.length());
                retBytes = uncompressZLIB(retBytes);                
            }
            else
                ; // uncompressible XML, just drop it on the floor....
        }
        else
            return data;  // the Content-Type header is optional, assumes PT
        return retBytes;
    }

    private static int getCompressionType(String header) {
        String s = header.trim();
        if(s.equals("") || s.equalsIgnoreCase(C_HEADER_NONE_VAL))
            return NONE;
        else if(s.equalsIgnoreCase(C_HEADER_GZIP_VAL))
            return GZIP;
        else if(s.equalsIgnoreCase(C_HEADER_ZLIB_VAL))
            return ZLIB;
        else
            return -1;
        
    }
    

    /** Returns the uncompressed version of the given ZLIB'ed bytes.  Throws
     *  IOException if the data is corrupt. */
    private static byte[] uncompressGZIP(byte[] data) throws IOException {
        ByteArrayInputStream bais=new ByteArrayInputStream(data);
        InflaterInputStream gis = null;
        try {
            gis =new GZIPInputStream(bais);
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            while (true) {
                int b=gis.read();
                if (b==-1)
                    break;
                baos.write(b);
            }
            return baos.toByteArray();
        } finally {
            IOUtils.close(gis);
        }
    }

        

    /** Returns the uncompressed version of the given ZLIB'ed bytes.  Throws
     *  IOException if the data is corrupt. */
    private static byte[] uncompressZLIB(byte[] data) throws IOException {
        ByteArrayInputStream bais=new ByteArrayInputStream(data);
        InflaterInputStream gis = null;
        Inflater inf = null;
        try {
            inf = new Inflater();
            gis =new InflaterInputStream(bais, inf);
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            while (true) {
                int b=gis.read();
                if (b==-1)
                    break;
                baos.write(b);
            }
            return baos.toByteArray();
        } finally {
            IOUtils.close(gis);
            IOUtils.close(inf);
        }
    }


    private static final int NUM_BYTES_TO_HASH = 100;
    private static final int NUM_TOTAL_HASH    = NUM_BYTES_TO_HASH*3;
    private static void clearHashBytes(byte[] hashBytes) {
        for (int i = 0; i < NUM_BYTES_TO_HASH; i++)
            hashBytes[i] = (byte)0;
    }

    /**
     * Hashes the file using bits and pieces of the file.
     * 
     * @return the SHA hash bytes of the input bytes.
     * @throws IOException if hashing failed for any reason.
     */
    public static byte[] hashFile(File toHash) throws IOException {
        byte[] retBytes = null;
        FileInputStream fis = null;
        byte[] hashBytes = new byte[NUM_BYTES_TO_HASH];
        
        try {        

            // setup
            fis = new FileInputStream(toHash);
            MessageDigest md = null;
           
            try {
                md = MessageDigest.getInstance("SHA");
            } catch(NoSuchAlgorithmException nsae) {
                throw new IllegalStateException(nsae);
            }

            long fileLength = toHash.length();            
            if (fileLength < NUM_TOTAL_HASH) {
                int numRead = 0;
                do {
                    clearHashBytes(hashBytes);
                    numRead = fis.read(hashBytes);
                    md.update(hashBytes);
                    // if the file changed underneath me, throw away...
                    if (toHash.length() != fileLength)
                        throw new IOException("invalid length");
                } while (numRead == NUM_BYTES_TO_HASH);
            }
            else { // need to do some mathy stuff.......

                long thirds = fileLength / 3;

                // beginning input....
                clearHashBytes(hashBytes);
                fis.read(hashBytes);
                md.update(hashBytes);

                // if the file changed underneath me, throw away...
                if (toHash.length() != fileLength)
                    throw new IOException("invalid length");

                // middle input...
                clearHashBytes(hashBytes);
                fis.skip(thirds - NUM_BYTES_TO_HASH);
                fis.read(hashBytes);
                md.update(hashBytes);

                // if the file changed underneath me, throw away...
                if (toHash.length() != fileLength)
                    throw new IOException("invalid length");
                
                // ending input....
                clearHashBytes(hashBytes);
                fis.skip(toHash.length() - 
                         (thirds + NUM_BYTES_TO_HASH) -
                         NUM_BYTES_TO_HASH);
                fis.read(hashBytes);
                md.update(hashBytes);

                // if the file changed underneath me, throw away...
                if (toHash.length() != fileLength)
                    throw new IOException("invalid length");

            }
                
            retBytes = md.digest();
        } finally {
            if (fis != null)
                fis.close();
        }
        return retBytes;
    }

    /**
     * Tries to parse <code>integer</code> to an int. If it fails, returns
     * <code>defaultValue</code>.
     */
    public static int parseInteger(String integer, int defaultValue) {
        try {
            return Integer.parseInt(integer);
        } catch(NumberFormatException nfx) {
            LOG.error("Unable to parse number: " + integer, nfx);
            return defaultValue;
        }
    }

    /**
     * Removes <pre><elementName>.*</elementName></pre> from <code>input</code>.
     * 
     * @return <code>input</code> if element not found in the input
     */
    public static String stripElement(String input, String elementName) {
        return input.replaceAll("<" + elementName + ">[^<]*</" + elementName +">", "");
    }
}
