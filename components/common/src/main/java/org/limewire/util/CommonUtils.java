package org.limewire.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * Provides convenience functionality ranging from getting user information,
 * copying files to getting the stack traces of all current threads.
 * <DL>
 * <DT>User Information
 * <DD>Get a username, a user home directory, etc.
 * 
 * <DT>File Operation
 * <DD>Copy resource files, get the current directory, set, get and validate the 
 * directory to store user settings. Also, you can use convertFileName to replace 
 * operating system specific illegal characters.
 * 
 * <DT>Threads
 * <DD>Get the stack traces of all current threads.
 * 
 * <DT>Time
 * <DD>Convert an integer value representing the seconds into an appropriate days, 
 * hour, minutes and seconds format (d:hh:mm:ss). 
 * 
 * <DT>Decode
 * <DD>Decode a URL encoded from a string.
 * 
 * <DT>Resources
 * <DD>Retrieve a resource file and a stream.
 * </DL>
 */
public class CommonUtils {

    /**
     * Several arrays of illegal characters on various operating systems. Used
     * by convertFileName
     */
    private static final char[] ILLEGAL_CHARS_ANY_OS = { '/', '\n', '\r', '\t', '\0', '\f' };

    private static final char[] ILLEGAL_CHARS_UNIX = { '`' };

    private static final char[] ILLEGAL_CHARS_WINDOWS = { '?', '*', '\\', '<', '>', '|', '\"', ':' };

    private static final char[] ILLEGAL_CHARS_MACOS = { ':' };

    /** The location where settings are stored. */
    private static volatile File settingsDirectory = null;

    /**
     * Returns the user home directory.
     * 
     * @return the <tt>File</tt> instance denoting the abstract pathname of the
     *         user's home directory, or <tt>null</tt> if the home directory
     *         does not exist
     */
    public static File getUserHomeDir() {
        return new File(System.getProperty("user.home"));
    }

    /**
     * Return the user's name.
     * 
     * @return the <tt>String</tt> denoting the user's name.
     */
    public static String getUserName() {
        return System.getProperty("user.name");
    }

    /**
     * Gets an InputStream from a resource file.
     * 
     * @param location the location of the resource in the resource file
     * @return an <tt>InputStream</tt> for the resource
     * @throws IOException if the resource could not be located or there was
     *         another IO error accessing the resource
     */
    public static InputStream getResourceStream(String location) throws IOException {
        ClassLoader cl = CommonUtils.class.getClassLoader();
        URL resource = null;

        if (cl == null) {
            resource = ClassLoader.getSystemResource(location);
        } else {
            resource = cl.getResource(location);
        }

        if (resource == null)
            throw new IOException("null resource: " + location);
        else
            return resource.openStream();
    }

    /**
     * Copied from URLDecoder.java.
     */
    public static String decode(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '+':
                sb.append(' ');
                break;
            case '%':
                try {
                    sb.append((char) Integer.parseInt(s.substring(i + 1, i + 3), 16));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(s);
                }
                i += 2;
                break;
            default:
                sb.append(c);
                break;
            }
        }
        // Undo conversion to external encoding
        String result = sb.toString();
        try {
            byte[] inputBytes = result.getBytes("8859_1");
            result = new String(inputBytes);
        } catch (UnsupportedEncodingException e) {
            // The system should always have 8859_1
        }
        return result;
    }

    /**
     * Copies the specified resource file into the current directory from the
     * jar file. If the file already exists, no copy is performed.
     * 
     * @param fileName the name of the file to copy, relative to the jar file --
     *        such as "org/limewire/gui/images/image.gif"
     * @param newFile the new <tt>File</tt> instance where the resource file
     *        will be copied to -- if this argument is null, the file will be
     *        copied to the current directory
     * @param forceOverwrite specifies whether or not to overwrite the file if
     *        it already exists
     */
    public static void copyResourceFile(String fileName, File newFile, boolean forceOverwrite)
            throws IOException {
        if (newFile == null)
            newFile = new File(".", fileName);

        // return quickly if the file is already there, no copy necessary
        if (!forceOverwrite && newFile.exists())
            return;

        String parentString = newFile.getParent();
        if (parentString == null)
            return;

        File parentFile = new File(parentString);
        if (!parentFile.isDirectory())
            parentFile.mkdirs();

        ClassLoader cl = CommonUtils.class.getClassLoader();
        // load resource using my class loader or system class loader
        // Can happen if Launcher loaded by system class loader
        URL resource = cl != null ? cl.getResource(fileName) : ClassLoader
                .getSystemResource(fileName);

        if (resource == null)
            throw new IOException("resource: " + fileName + " doesn't exist.");

        saveStream(resource.openStream(), newFile);
    }

    /**
     * Copies the src file to the destination file. This will always overwrite
     * the destination.
     */
    public static void copyFile(File src, File dst) throws IOException {
        saveStream(new FileInputStream(src), dst);
    }

    /**
     * Saves all data from the stream into the destination file. This will
     * always overwrite the file.
     */
    public static void saveStream(InputStream inStream, File newFile) throws IOException {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            // buffer the streams to improve I/O performance
            final int bufferSize = 2048;
            bis = new BufferedInputStream(inStream, bufferSize);
            bos = new BufferedOutputStream(new FileOutputStream(newFile), bufferSize);
            byte[] buffer = new byte[bufferSize];
            int c = 0;

            do { // read and write in chunks of buffer size until EOF reached
                c = bis.read(buffer, 0, bufferSize);
                if (c > 0)
                    bos.write(buffer, 0, c);
            } while (c == bufferSize); // (# of bytes read)c will = bufferSize
                                       // until EOF

            bos.flush();
        } catch (IOException e) {
            // if there is any error, delete any portion of file that did write
            newFile.delete();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException ignored) {
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
    
    /**
     * Converts a value in seconds to: "d:hh:mm:ss" where d=days, hh=hours,
     * mm=minutes, ss=seconds, or "h:mm:ss" where h=hours<24, mm=minutes,
     * ss=seconds, or "m:ss" where m=minutes<60, ss=seconds.
     */
    public static String seconds2time(long seconds) {
        long minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        long hours = minutes / 60;
        minutes = minutes - hours * 60;
        long days = hours / 24;
        hours = hours - days * 24;
        // build the numbers into a string
        StringBuilder time = new StringBuilder();
        if (days != 0) {
            time.append(Long.toString(days));
            time.append(":");
            if (hours < 10)
                time.append("0");
        }
        if (days != 0 || hours != 0) {
            time.append(Long.toString(hours));
            time.append(":");
            if (minutes < 10)
                time.append("0");
        }
        time.append(Long.toString(minutes));
        time.append(":");
        if (seconds < 10)
            time.append("0");
        time.append(Long.toString(seconds));
        return time.toString();
    }

    /**
     * Returns a normalized and shortened valid file name taking the length of
     * the path of the parent directory into account.
     * <p>
     * The name is cleared from illegal file system characters and it is ensured
     * that the maximum path system on the system is not exceeded unless the
     * parent directory path has already the maximum path length.
     * 
     * @throws IOException if the parent directory's path takes up
     *         {@link OSUtils#getMaxPathLength()}.
     */
    public static String convertFileName(File parentDir, String name) throws IOException {
        int parentLength = FileUtils.getCanonicalFile(parentDir).getAbsolutePath().getBytes(Charset.defaultCharset().name()).length;
        if (parentLength >= OSUtils.getMaxPathLength() - 1 /*
                                                            * for the separator
                                                            * char
                                                            */) {
            throw new IOException("Path too long");
        }
        return convertFileName(name, Math.min(OSUtils.getMaxPathLength() - parentLength - 1, 180));
    }

    /**
     * Cleans up the filename and truncates it to length of 180 bytes by calling
     * {@link #convertFileName(String, int) convertFileName(String, 180)}.
     */
    public static String convertFileName(String name) {
        return convertFileName(name, 180);
    }

    /**
     * Cleans up the filename from illegal characters and truncates it to the
     * length of bytes specified.
     * 
     * @param name the filename to clean up
     * @param maxBytes the maximum number of bytes the cleaned up file name can
     *        take up
     * @return the cleaned up file name
     */
    public static String convertFileName(String name, int maxBytes) {
        // use default encoding which is also used for files judging from the
        // property name "file.encoding"
        try {
            return convertFileName(name, maxBytes, Charset.defaultCharset());
        } catch (CharacterCodingException cce) {
            try {
                // UTF-8 should always be available
                return convertFileName(name, maxBytes, Charset.forName("UTF-8"));
            } catch (CharacterCodingException e) {
                // should not happen, UTF-8 can encode unicode and gives us a
                // good length estimate
                throw new RuntimeException("UTF-8 should have encoded: " + name, e);
            }
        }
    }

    /**
     * Replaces OS specific illegal characters from any filename with '_',
     * including ( / \n \r \t ) on all operating systems, ( ? * \ < > | " ) on
     * Windows, ( ` ) on Unix.
     * 
     * @param name the filename to check for illegal characters
     * @param maxBytes the maximum number of bytes for the resulting file name,
     *        must be > 0
     * @return String containing the cleaned filename
     * 
     * @throws CharacterCodingException if the charset could not encode the
     *         characters in <code>name</code>
     * @throws IllegalArgumentException if maxBytes <= 0
     */
    public static String convertFileName(String name, int maxBytes, Charset charSet)
            throws CharacterCodingException {

        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be > 0");
        }

        // ensure that block-characters aren't in the filename.
        name = I18NConvert.instance().compose(name);

        // if the name is too long, reduce it. We don't go all the way
        // up to 255 because we don't know how long the directory name is
        // We want to keep the extension, though.
        if (name.length() > maxBytes || name.getBytes().length > maxBytes) {
            int extStart = name.lastIndexOf('.');
            if (extStart == -1) { // no extension, weird, but possible
                name = getPrefixWithMaxBytes(name, maxBytes, charSet);
            } else {
                // if extension is greater than 11, we truncate it.
                // ( 11 = '.' + 10 extension bytes )
                int extLength = name.length() - extStart;
                int extEnd = extLength > 11 ? extStart + 11 : name.length();
                byte[] extension = getMaxBytes(name.substring(extStart, extEnd), 16, charSet);
                try {
                    // disregard extension if we lose too much of the name
                    // since the name is also used for searching
                    if (extension.length >= maxBytes - 10) {
                        name = getPrefixWithMaxBytes(name, maxBytes, charSet);
                    } else {
                        name = getPrefixWithMaxBytes(name, maxBytes - extension.length, charSet)
                                + new String(extension, charSet.name());
                    }
                } catch (UnsupportedEncodingException uee) {
                    throw new RuntimeException("Could not handle string", uee);
                }
            }
        }
        for (char aILLEGAL_CHARS_ANY_OS : ILLEGAL_CHARS_ANY_OS) {
            name = name.replace(aILLEGAL_CHARS_ANY_OS, '_');
        }

        if (OSUtils.isWindows() || OSUtils.isOS2()) {
            for (char aILLEGAL_CHARS_WINDOWS : ILLEGAL_CHARS_WINDOWS) {
                name = name.replace(aILLEGAL_CHARS_WINDOWS, '_');
            }
        } else if (OSUtils.isLinux() || OSUtils.isSolaris()) {
            for (char aILLEGAL_CHARS_UNIX : ILLEGAL_CHARS_UNIX) {
                name = name.replace(aILLEGAL_CHARS_UNIX, '_');
            }
        } else if (OSUtils.isMacOSX()) {
            for (char aILLEGAL_CHARS_MACOS : ILLEGAL_CHARS_MACOS) {
                name = name.replace(aILLEGAL_CHARS_MACOS, '_');
            }
        }

        return name;
    }

    /**
     * Sanitizes a folder name. Folder names can contain illegal characters that
     * are valid within a filename. 
     * http://msdn.microsoft.com/en-us/library/aa365247(VS.85).aspx
     * 
     * @param name String to sanitize
     * @return sanitized String
     * @throws IOException if no valid characters remain within this file name.
     */
    public static String sanitizeFolderName(String name) throws IOException {
        String result = santizeString(name).trim();
        
        int index = result.length();
        while(result.charAt(index-1) == '.') {
            index -= 1;
            if(index <= 0)
                throw new IOException("folder does not contain valid characters");
        }
        
        if(index == result.length())
            return result;
        else                
            return result.substring(0, index);
    }
    
    /**
     * Sanitizes a String for use in a directory and file name and removes any
     * illegal characters from it.
     * 
     * @param name String to check
     * @return sanitized String
     */
    public static String santizeString(String name) {
        for (char aILLEGAL_CHARS_ANY_OS : ILLEGAL_CHARS_ANY_OS) {
            name = name.replace(aILLEGAL_CHARS_ANY_OS, '_');
        }

        if (OSUtils.isWindows() || OSUtils.isOS2()) {
            for (char aILLEGAL_CHARS_WINDOWS : ILLEGAL_CHARS_WINDOWS) {
                name = name.replace(aILLEGAL_CHARS_WINDOWS, '_');
            }
        } else if (OSUtils.isLinux() || OSUtils.isSolaris()) {
            for (char aILLEGAL_CHARS_UNIX : ILLEGAL_CHARS_UNIX) {
                name = name.replace(aILLEGAL_CHARS_UNIX, '_');
            }
        } else if (OSUtils.isMacOSX()) {
            for (char aILLEGAL_CHARS_MACOS : ILLEGAL_CHARS_MACOS) {
                name = name.replace(aILLEGAL_CHARS_MACOS, '_');
            }
        }
        return name;
    }

    /**
     * Returns the prefix of <code>string</code> which takes up a maximum of
     * <code>maxBytes</code>.
     * 
     * @throws CharacterCodingException
     */
    static String getPrefixWithMaxBytes(String string, int maxBytes, Charset charSet)
            throws CharacterCodingException {
        try {
            return new String(getMaxBytes(string, maxBytes, charSet), charSet.name());
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("Could not recreate string", uee);
        }
    }

    /**
     * Returns the first <code>maxBytes</code> of <code>string</code> encoded
     * using the encoder of <code>charSet</code>
     * 
     * @param string whose prefix bytes to return
     * @param maxBytes the maximum number of bytes to return
     * @param charSet the char set used for encoding the characters into bytes
     * @return the array of bytes of length <= maxBytes
     * @throws CharacterCodingException if the char set's encoder could not
     *         handle the characters in the string
     */
    static byte[] getMaxBytes(String string, int maxBytes, Charset charSet)
            throws CharacterCodingException {
        byte[] bytes = new byte[maxBytes];
        ByteBuffer out = ByteBuffer.wrap(bytes);
        CharBuffer in = CharBuffer.wrap(string.toCharArray());
        CharsetEncoder encoder = charSet.newEncoder();
        CoderResult cr = encoder.encode(in, out, true);
        encoder.flush(out);
        if (cr.isError()) {
            cr.throwException();
        }
        byte[] result = new byte[out.position()];
        System.arraycopy(bytes, 0, result, 0, result.length);
        return result;
    }

    /**
     * Returns the user's current working directory as a <tt>File</tt> instance,
     * or <tt>null</tt> if the property is not set.
     * 
     * @return the user's current working directory as a <tt>File</tt> instance,
     *         or <tt>null</tt> if the property is not set
     */
    public static File getCurrentDirectory() {
        return new File(System.getProperty("user.dir"));
    }

    /**
     * Validates a potential settings directory. This returns the validated
     * directory, or throws an IOException if it can't be validated.
     */
    public static File validateSettingsDirectory(File dir) throws IOException {
        dir = dir.getAbsoluteFile();
        if (!dir.isDirectory()) {
            dir.delete(); // delete whatever it may have been
            if (!dir.mkdirs())
                throw new IOException("could not create preferences directory: " + dir);
        }

        if (!FileUtils.canWrite(dir))
            throw new IOException("settings dir not writable: " + dir);

        if (!dir.canRead())
            throw new IOException("settings dir not readable: " + dir);

        // Validate that you can write a file into settings directory.
        // catches vista problem where if settings directory is
        // locked canRead and canWrite still return true
        File file = File.createTempFile("test", "test", dir);
        if (!file.exists())
            throw new IOException("can't write test file in directory: " + dir);
        file.delete();

        return dir;
    }

    /**
     * Sets the new settings directory. The settings directory cannot be set
     * more than once.
     * <p>
     * If the directory can't be set (because it isn't a folder, can't be made
     * into a folder, or isn't readable and writable), an IOException is thrown.
     * 
     * @throws IOException
     */
    public static void setUserSettingsDir(File settingsDir) throws IOException {
        if (settingsDirectory != null)
            throw new IllegalStateException("settings directory already set!");
        settingsDirectory = validateSettingsDirectory(settingsDir);
    }

    /**
     * Returns the directory where all user settings should be stored. This is
     * where all application data should be stored. If the directory is not set,
     * this returns the user's home directory.
     */
    public synchronized static File getUserSettingsDir() {
        if (settingsDirectory != null)
            return settingsDirectory;
        else
            return getUserHomeDir();
    }

    /**
     * Parses a long from the given string swallowing any exceptions. null is
     * returned if there is an error parsing the string.
     */
    public static Long parseLongNoException(String str) {
        Long num = null;
        if (str != null) {
            try {
                num = Long.valueOf(str);
            } catch (NumberFormatException e) {
                // continue; null is returned
            }
        }
        return num;
    }
}
