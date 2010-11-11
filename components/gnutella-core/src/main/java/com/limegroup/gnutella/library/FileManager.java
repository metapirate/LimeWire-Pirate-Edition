package com.limegroup.gnutella.library;

/** A central hub to start/stop all file management related services. */
public interface FileManager {

    /** Asynchronously loads all files by calling loadSettings. */
    void start();

    /** Stops saving the library changes to disk. */
    void stop();
}