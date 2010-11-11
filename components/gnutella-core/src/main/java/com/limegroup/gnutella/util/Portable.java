package com.limegroup.gnutella.util;

import java.io.File;

/** Holds runtime settings for Portable LimeWire. */
public interface Portable {

    /** true if there are portable settings, even if they're bad. */
    public boolean isPortable();
    
    /** Absolute path to the settings directory as portable settings define, null if no settings or bad settings. */
    public File getSettingsLocation();
}
