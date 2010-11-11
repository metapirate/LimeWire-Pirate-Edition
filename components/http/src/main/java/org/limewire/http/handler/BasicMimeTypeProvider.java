package org.limewire.http.handler;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.protocol.HTTP;

/**
 * A simple mime type provider that determines the mime type by file extension.
 */
public class BasicMimeTypeProvider implements MimeTypeProvider {

    private final Map<String, String> mimeTypeByExtension = Collections
            .synchronizedMap(new HashMap<String, String>());

    private final String defaultMimeType;
    
    /**
     * Constructs a provider with a default set of mappings.
     * 
     * @param defaultMimeType the default to use when a file extension does not
     *        map to any mime type
     */
    public BasicMimeTypeProvider(String defaultMimeType) {
        if (defaultMimeType == null) {
            throw new IllegalArgumentException();
        }
        
        this.defaultMimeType = defaultMimeType;

        addMimeTypeByExtension("css", "text/css");
        addMimeTypeByExtension("gif", "image/gif");
        addMimeTypeByExtension("ico", "image/x-icon");
        addMimeTypeByExtension("jpg", "image/jpg");
        addMimeTypeByExtension("png", "image/png");
        addMimeTypeByExtension("htm", "text/html");
        addMimeTypeByExtension("html", "text/html");
        addMimeTypeByExtension("img", "image/gif");
        addMimeTypeByExtension("js", "application/x-javascript");
        addMimeTypeByExtension("mp3", "audio/mpeg");
    }

    /**
     * Constructs a provider with a default set of mappings and
     * {@link HTTP#OCTET_STREAM_TYPE} as the default mime type.
     */
    public BasicMimeTypeProvider() {
        this(HTTP.OCTET_STREAM_TYPE);
    }

    public void addMimeTypeByExtension(String extension, String mimeType) {
        mimeTypeByExtension.put(extension, mimeType);
    }

    public void removeMimeTypeByExtension(String extension) {
        mimeTypeByExtension.remove(extension);
    }

    public String getMimeType(File file) {
        String mimeType = mimeTypeByExtension.get(getExtension(file));
        return (mimeType != null) ? mimeType : defaultMimeType;
    }

    private String getExtension(File file) {
        String name = file.getName();
        int i = name.lastIndexOf(".");
        return (i != -1) ? name.substring(i + 1) : name; 
    }

}
