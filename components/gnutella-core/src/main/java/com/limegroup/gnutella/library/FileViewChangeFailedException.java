package com.limegroup.gnutella.library;

import java.io.File;

import com.limegroup.gnutella.library.FileViewChangeEvent.Type;

/** An event that's triggered when adding to a file view failed for some reason. */
public class FileViewChangeFailedException extends Exception {
    
    private final File file;
    private final Type type;
    private final Reason reason;
    
    public static enum Reason {
        ERROR_LOADING_URNS,
        CANT_CANONICALIZE,
        ALREADY_MANAGED,
        NOT_MANAGEABLE,
        FILE_TYPE_NOT_ALLOWED,
        INVALID_URN,
        OLD_WASNT_MANAGED,
        CANT_ADD_TO_LIST,
        DANGEROUS_FILE
    }

    /** Constructs the event with a particular reason. */
    public FileViewChangeFailedException(File file, Type type, Reason reason) {
        super("File: " + file + ", Type: " + type + ", Reason: " + reason);
        this.file = file;
        this.type = type;
        this.reason = reason;
    }
    
    /** Constructs the event with a reason & a Throwable as a cause. */
    public FileViewChangeFailedException(File file, Type type, Reason reason, Throwable cause) {
        super("File: " + file + ", Type: " + type + ", Reason: " + reason, cause);
        this.file = file;
        this.type = type;
        this.reason = reason;
    }
    
    /** Returns the type of change-event this represents. */
    public FileViewChangeEvent.Type getType() {
        return type;
    }
    
    public File getFile() {
        return file;
    }
    
    public Reason getReason() {
        return reason;
    }
    
}
