package org.limewire.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.EnumMap;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.limewire.i18n.I18nMarker;
import org.limewire.service.ErrorService;
import org.limewire.service.MessageService;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;


/**
 * Provides utility input and output related methods. <code>IOUtils</code> 
 * includes methods to read and skip over data, and handle exceptions. Furthermore,
 * this class lets you compress and uncompress data and to close {@link Closeable}
 * objects, {@link Socket Sockets} and {@link ServerSocket ServerSockets}.
 */
public class IOUtils {
    
    public static enum ErrorType {
        GENERIC, DOWNLOAD;
    }
    
    private static enum DetailErrorType {
        DISK_FULL, FILE_LOCKED, NO_PRIVS, BAD_CHARS;
    }
    
    private static final EnumMap<ErrorType, EnumMap<DetailErrorType, String>> errorDescs;
    
    static {
        errorDescs = new EnumMap<ErrorType, EnumMap<DetailErrorType,String>>(ErrorType.class);
        for(ErrorType type : ErrorType.values())
            errorDescs.put(type, new EnumMap<DetailErrorType, String>(DetailErrorType.class));
        
        errorDescs.get(ErrorType.GENERIC).put(DetailErrorType.DISK_FULL, 
            I18nMarker.marktr("LimeWire was unable to write a necessary file because your hard drive is full. To continue using LimeWire you must free up space on your hard drive."));
        errorDescs.get(ErrorType.GENERIC).put(DetailErrorType.FILE_LOCKED,
            I18nMarker.marktr("LimeWire was unable to open a necessary file because another program has locked the file. LimeWire may act unexpectedly until this file is released."));
        errorDescs.get(ErrorType.GENERIC).put(DetailErrorType.NO_PRIVS,
            I18nMarker.marktr("LimeWire was unable to write a necessary file because you do not have the necessary permissions. Your preferences may not be maintained the next time you start LimeWire, or LimeWire may behave in unexpected ways."));
        errorDescs.get(ErrorType.GENERIC).put(DetailErrorType.BAD_CHARS,
            I18nMarker.marktr("LimeWire cannot open a necessary file because the filename contains characters which are not supported by your operating system. LimeWire may behave in unexpected ways."));        


        errorDescs.get(ErrorType.DOWNLOAD).put(DetailErrorType.DISK_FULL,
            I18nMarker.marktr("LimeWire cannot download the selected file because your hard drive is full. To download more files, you must free up space on your hard drive."));
        errorDescs.get(ErrorType.DOWNLOAD).put(DetailErrorType.FILE_LOCKED,
            I18nMarker.marktr("LimeWire was unable to download the selected file because another program is using the file. Please close the other program and retry the download."));
        errorDescs.get(ErrorType.DOWNLOAD).put(DetailErrorType.NO_PRIVS,
            I18nMarker.marktr("LimeWire was unable to create or continue writing an incomplete file for the selected download because you do not have permission to write files to the incomplete folder. To continue using LimeWire, please choose a different Save Folder."));
        errorDescs.get(ErrorType.DOWNLOAD).put(DetailErrorType.BAD_CHARS,
            I18nMarker.marktr("LimeWire was unable to open the incomplete file for the selected download because the filename contains characters which are not supported by your operating system."));
        
        // just verify it was all setup right.
        for(ErrorType type : ErrorType.values()) {
            assert errorDescs.get(type) != null;
            assert errorDescs.get(type).size() == DetailErrorType.values().length;
        }
     
    }

    /**
     * Attempts to handle an IOException. If we know expect the problem,
     * we can either ignore it or display a friendly error (both returning
     * true, for handled) or expect the outer-world to handle it (and
     * return false).
     *
     * @return true if we could handle the error.
     */
    public static boolean handleException(IOException ioe, ErrorType errorType) {
        Throwable e = ioe;
        
        while(e != null) {
            String msg = e.getMessage();
            
            if(msg != null) {
                msg = msg.toLowerCase();
                DetailErrorType detailType = null;
                // If the user's disk is full, let them know.
                if(StringUtils.contains(msg, "no space left") || 
                   StringUtils.contains(msg, "not enough space")) {
                    detailType = DetailErrorType.DISK_FULL;
                }
                // If the file is locked, let them know.
                else if(StringUtils.contains(msg, "being used by another process") ||
                   StringUtils.contains(msg, "with a user-mapped section open")) {
                    detailType = DetailErrorType.FILE_LOCKED;
                }
                // If we don't have permissions to write, let them know.
                else if(StringUtils.contains(msg, "access is denied") || 
                   StringUtils.contains(msg, "permission denied") ) {
                    detailType = DetailErrorType.NO_PRIVS;
                }
                // If character set is faulty...
                else if(StringUtils.contains(msg, "invalid argument")) {
                    detailType = DetailErrorType.BAD_CHARS;
                }
                
                if(detailType != null) {
                    MessageService.showError(errorDescs.get(errorType).get(detailType));
                    return true;
                }
            }
            
            e = e.getCause();
        }

        // dunno what to do, let the outer world handle it.
        return false;
    }       

    /** Convenience method to create a new IOException with initCause set. */
    public static IOException getIOException(String message, Throwable cause) {
        IOException ioException = new IOException(message);
        ioException.initCause(cause);
        return ioException;
    }

   /**
     * Returns the first word of specified maximum size up to the first space
     * and returns it. This does not read up to the first whitespace
     * character -- it only looks for a single space. This is particularly
     * useful for reading HTTP requests, as the request method, the URI, and
     * the HTTP version must all be separated by a single space.
     * Note that only one extra character is read from the stream in the case of
     * success (the white space character after the word).
     * <p>
     * Note: Doesn't handle multi-byte words correctly, each byte as it comes in
     * will be treated as one character.
     *
     * @param in the input stream from where to read the word
     * @param maxSize the maximum size of the word.
     * @return the first word (i.e., no whitespace) of specified maximum size
     * @exception IOException if the word of specified maxSize couldn't be read,
     * either due to stream errors, or timeouts
     */
    public static String readWord(InputStream in, int maxSize)
      throws IOException {
        final char[] buf = new char[maxSize];
        int i = 0;
        //iterate till maxSize + 1 (for white space)
        while (true) {
            int got;
            try {
                got = in.read();
                if (got >= 0) { // not EOF
                    if ((char)got != ' ') { //didn't get word. Exclude space.
                        if (i < maxSize) { //We don't store the last letter
                            buf[i++] = (char)got;
                            continue;
                        }
                        //if word of size upto maxsize not found, throw an
                        //IOException. (Fixes bug 26 in 'core' project)
                        throw new IOException("could not read word");
                    }
                    return new String(buf, 0, i);
                }
                throw new IOException("unexpected end of file");
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                // thrown in strange circumstances of in.read(), consider IOX.
                throw new IOException("unexpected aioobe");
            }
        }
    }
    
    /**
     * Reads a word, but if the connection closes, returns the largest word read
     * instead of throwing an IOX.
     */
    public static String readLargestWord(InputStream in, int maxSize)
      throws IOException {
        final char[] buf = new char[maxSize];
        int i = 0;
        //iterate till maxSize + 1 (for white space)
        while (true) {
            int got;
            try {
                got = in.read();
                if(got == -1) {
                    if(i == 0)
                        throw new IOException("could not read any word.");
                    else
                        return new String(buf, 0, i);
                } else if (got >= 0) {
                    if ((char)got != ' ') { //didn't get word. Exclude space.
                        if (i < maxSize) { //We don't store the last letter
                            buf[i++] = (char)got;
                            continue;
                        }
                        //if word of size upto maxsize not found, throw an
                        //IOException. (Fixes bug 26 in 'core' project)
                        throw new IOException("could not read word");
                    }
                    return new String(buf, 0, i);
                }
                throw new IOException("unknown got amount");
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                // thrown in strange circumstances of in.read(), consider IOX.
                throw new IOException("unexpected aioobe");
            }
        }
    }
    
    public static long ensureSkip(InputStream in, long length) throws IOException {
        long skipped = 0;
        while(skipped < length) {
            long current = in.skip(length - skipped);
            if(current == -1 || current == 0)
                throw new EOFException("eof");
            else
                skipped += current;
        }
        return skipped;
    }

    /**
     * A utility method to close Closeable objects (Readers, Writers, 
     * Input- and OutputStreams and RandomAccessFiles).
     */
    public static void close(Closeable closeable) {
        FileUtils.close(closeable);
    }
    
    /**
     * Closes a collection of closeables. Also handles a null argument gracefully. 
     */
    public static void close(Iterable<? extends Closeable> closeables) {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                close(closeable);
            }
        }
    }
    
    /**
     * A utility method to flush Flushable objects (Readers, Writers, 
     * Input- and OutputStreams and RandomAccessFiles).
     */
    public static void flush(Flushable flushable) {
        FileUtils.flush(flushable);
    }
    
    /** Closes the socket if it is not null. */
    public static void close(DatagramSocket s) {
        if(s != null) {
            s.close();
        }
    }
    
    /**
     * A utility method to close Sockets.
     */
    public static void close(Socket s) {
        if(s != null) {
            try {
                s.close();
            } catch(IOException ignored) {}
            
            try {
                close(s.getInputStream());
            } catch(IOException ignored) {}

            try {
                close(s.getOutputStream());
            } catch(IOException ignored) {}
        }
    }
    
    /**
     * A utility method to close ServerSockets.
     */
    public static void close(ServerSocket s) {
        if(s != null) {
            try {
                s.close();
            } catch(IOException ignored) {}
        }
    }
    
    public static void close(Deflater deflater) {
        if(deflater != null) {
            deflater.end();
        }
    }
    
    public static void close(Inflater inflater) {
        if(inflater != null) {
            inflater.end();
        }
    }
    
    /**
     * Deflates (compresses) the data.
     */
    public static byte[] deflate(byte[] data) {
        OutputStream dos = null;
        Deflater def = null;
        try {
            def = new Deflater();
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            dos = new DeflaterOutputStream(baos, def);
            dos.write(data, 0, data.length);
            dos.close();                      //flushes bytes
            return baos.toByteArray();
        } catch(IOException impossible) {
            ErrorService.error(impossible);
            return null;
        } finally {
            close(dos);
            close(def);
        }
    }
    
    /**
     * Inflates (uncompresses) the data.
     */
    public static byte[] inflate(byte[] data) throws IOException {
        InputStream in = null;
        Inflater inf = null;
        try {
            inf = new Inflater();
            in = new InflaterInputStream(new ByteArrayInputStream(data), inf);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[64];
            while(true) {
                int read = in.read(buf, 0, buf.length);
                if(read == -1)
                    break;
                out.write(buf, 0, read);
            }
            return out.toByteArray();
        } catch(OutOfMemoryError oome) {
            throw new IOException(oome.getMessage());
        } finally {
            close(in);
            close(inf);
        }
    } 
    
    public static byte [] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream bos = null;
        try {
            bos = new ByteArrayOutputStream();
            byte [] buffer = new byte[1024];
            int read;
            while((read = in.read(buffer)) != -1) {
                bos.write(buffer, 0, read);        
            }
            return bos.toByteArray();
        } finally {
            close(bos);
            close(in);
        }
    }

    /**
     * Reads a byte from input stream and throws {@link EOFException} if
     * the end of the stream was reached. 
     */
    public static int readByte(InputStream is) throws IOException{
        int ret = is.read();
        if (ret == -1)
            throw new EOFException();
        return ret;
    }

    /**
     * Fills array with bytes from input stream and throws {@link EOFException}
     * if it couldn't be fully read. 
     */
    public static void readFully(InputStream in, byte[] array) throws IOException {
        int offset = 0;
        while (offset < array.length) {
            int read = in.read(array, offset, array.length - offset);
            if (read == -1) {
                throw new EOFException();
            }
            offset += read;
        }
    }
}
