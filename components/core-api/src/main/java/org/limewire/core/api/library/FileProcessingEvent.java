package org.limewire.core.api.library;

import java.io.File;

import org.limewire.listener.DefaultSourceTypeEvent;

public class FileProcessingEvent extends DefaultSourceTypeEvent<File, FileProcessingEvent.Type> {

    public static enum Type {
        QUEUED, PROCESSING, FINISHED;
    }

    public FileProcessingEvent(Type type, File file) {
        super(file, type);
    }
}
