package org.limewire.player.api;


import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 *  A wrapper for the source of an audio file that is currently playing.
 */
public interface AudioSource {
    
    public File getFile();
    
    public InputStream getStream();

    public URL getURL();
}
