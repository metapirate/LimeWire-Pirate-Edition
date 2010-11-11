package com.limegroup.gnutella.util;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

import org.limewire.util.FileUtils;

/** Holds runtime settings for Portable LimeWire. */
public class PortableImpl implements Portable {

    /** File name of portable settings next to where we're running. */
    static final String PORTABLE_FILE = "portable.props";
    
    /** File name of program settings in the settings folder. */
    static final String PROGRAM_FILE = "limewire.props";
    
    /** Property key for source to copy from a CD-R. */
    static final String SOURCE_KEY = "SOURCE";
    
    /** Property key for portable settings location. */
    static final String SETTINGS_KEY = "SETTINGS";
    
    /** Default property value for portable settings location. */
    static final String DEFAULT_SETTINGS_VALUE = "Settings";
    
    /** Property key prefix for portable paths to resolve and inject. */
    static final String KEY_PREFIX = "PORTABLE_PATH_";

    /** Absolute path to settings directory, or null if no portable settings. */
    private File path;

    /** null if we loaded good portable settings without error. */
    private IOException exception;

    /** Reads settings where we're running for Portable LimeWire. */
    public PortableImpl() {
        try {
            
            // Read portable settings and find out where program settings should be
            File portablePath = new File(PORTABLE_FILE);
            if (!portablePath.exists())
                return; // No portable settings, we're running normally
            Properties portableProperties = FileUtils.readProperties(portablePath);
            path = FileUtils.resolveSpecialPath(portableProperties.getProperty(SETTINGS_KEY));
            
            // Copy in settings from a read-only location
            if (portableProperties.containsKey(SOURCE_KEY)) {
                File source = FileUtils.resolveSpecialPath(portableProperties.getProperty(SOURCE_KEY));
                if (!path.exists())
                    FileUtils.copyDirectory(source, path);
            }
            
            // Open the program's settings file
            Properties programProperties = new Properties();
            File programPath = new File(path, PROGRAM_FILE);
            if (programPath.exists())
                programProperties = FileUtils.readProperties(programPath);
            
            // Loop through portable settings that we need to set in the program's settings
            boolean save = false;
            Iterator i = portableProperties.keySet().iterator();
            while (i.hasNext()) {
                String portableKey = (String)i.next();
                if (portableKey.startsWith(KEY_PREFIX)) {
                    
                    // Turn portable "PORTABLE_PATH_NAME=special path" into program "NAME=absolute path"
                    String key = portableKey.substring(KEY_PREFIX.length(), portableKey.length()).trim();
                    if (key.length() != 0) {
                        String value = FileUtils.resolveSpecialPath(portableProperties.getProperty(portableKey)).getPath();
                        programProperties.setProperty(key, value);
                        save = true;
                    }
                }
            }
            
            // Save the edits we made to the program's settings file
            if (save) {
                FileUtils.makeFolder(path);
                FileUtils.writeProperties(programPath, programProperties);
            }

        // Catch and save the exception to record that settings were bad or there was an error
        } catch (IOException e) {
            exception = e;
        }
    }

    /** true if there are portable settings, even if they're bad. */
    public boolean isPortable() {
        return path != null || exception != null;
    }
    
    /** Absolute path to the settings directory as portable settings define, null if no settings or bad settings. */
    public File getSettingsLocation() {
        return path;
    }

    /** Default absolute path to portable settings directory. */
    public static File getDefaultSettingsLocation() throws IOException {
        return FileUtils.resolveSpecialPath(PortableImpl.DEFAULT_SETTINGS_VALUE);
    }
}
