package org.limewire.core.impl.upload;


public interface UploadListenerList {

    void addUploadListener(UploadListener listener);

    void removeUploadListener(UploadListener listener);

}
