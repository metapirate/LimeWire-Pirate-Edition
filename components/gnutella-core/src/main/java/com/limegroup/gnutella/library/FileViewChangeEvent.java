package com.limegroup.gnutella.library;

import java.io.File;

import org.limewire.listener.SourcedEvent;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;


/** An event that's triggered when a file view is changed. */
public class FileViewChangeEvent implements SourcedEvent<FileView> {
    
    public static enum Type {
        /** Notification a new FileDesc was added. */
        FILE_ADDED, 
        /** Notification a FileDesc was removed. */
        FILE_REMOVED, 
        /** Notification a File has changed from one FileDesc to another. */
        FILE_CHANGED, 
        /** Notification a FileDesc's metadata (such as URN, XML) has changed. */
        FILE_META_CHANGED,
        /** Notification an add failed. */
        FILE_ADD_FAILED,
        /** Notification that a change failed. */
        FILE_CHANGE_FAILED, 
        /** Notification that all files in the view were cleared. */
        FILES_CLEARED;
    }
    
    private final Type type;
    private final FileView list;
    private final FileDesc newValue;
    private final FileDesc oldValue;
    private final File oldFile;
    private final File newFile;
    private boolean isShared = false;
    private boolean libraryCleared = false;
    
    public FileViewChangeEvent(FileView list, Type type, boolean libraryCleared) {
        assert type == Type.FILES_CLEARED;
        this.type = Objects.nonNull(type, "type");
        this.list = Objects.nonNull(list, "list");
        this.oldValue = null;
        this.newFile = null;
        this.oldFile = null;
        this.newValue = null;
        this.libraryCleared = libraryCleared;
    }
    
    public FileViewChangeEvent(FileView list, Type type, File file) {
        assert type == Type.FILE_ADD_FAILED;
        this.type = Objects.nonNull(type, "type");
        this.list = Objects.nonNull(list, "list");
        this.oldValue = null;
        this.newFile = Objects.nonNull(file, "file");
        this.oldFile = null;
        this.newValue = null;
    }
    
    public FileViewChangeEvent(FileView list, Type type, File oldFile, FileDesc oldValue, File newValue) {
        assert type == Type.FILE_CHANGE_FAILED;
        this.type = Objects.nonNull(type, "type");
        this.list = Objects.nonNull(list, "list");
        this.oldValue = oldValue; // May be null!
        this.newValue = null;
        this.oldFile = Objects.nonNull(oldFile, "oldFile");
        this.newFile = Objects.nonNull(newValue, "file");
    }
    
    public FileViewChangeEvent(FileView list, Type type, FileDesc value) {
        assert type == Type.FILE_ADDED || type == Type.FILE_REMOVED || type == Type.FILE_META_CHANGED;
        this.type = Objects.nonNull(type, "type");
        this.list = Objects.nonNull(list, "list");
        this.oldValue = null;
        this.newValue = Objects.nonNull(value, "value");
        this.oldFile = null;
        this.newFile = Objects.nonNull(newValue.getFile(), "value.getFile()");
    }
    
    public FileViewChangeEvent(FileView list, Type type, FileDesc oldValue, FileDesc newValue) {
        assert type == Type.FILE_CHANGED;
        this.type = Objects.nonNull(type, "type");
        this.list = Objects.nonNull(list, "list");
        this.oldValue = Objects.nonNull(oldValue, "oldValue");
        this.newValue = Objects.nonNull(newValue, "newValue");
        this.oldFile = Objects.nonNull(oldValue.getFile(), "oldValue.getFile()");
        this.newFile = Objects.nonNull(newValue.getFile(), "newValue.getFile()");
    }
    
    @Override
    public FileView getSource() {
        return list;
    }
    
    public File getOldFile() {
        return oldFile;
    }
    
    public File getFile() {
        return newFile;
    }
    
    public Type getType() {
        return type;
    }
    
    public FileView getFileView() {
        return list;
    }
    
    public FileDesc getFileDesc() {
        return newValue;
    }
    
    public FileDesc getOldValue() {
        return oldValue;
    }
    
    public boolean isShared() {
        return isShared;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
    
    public boolean isLibraryClear() {
        return libraryCleared;
    }

}
