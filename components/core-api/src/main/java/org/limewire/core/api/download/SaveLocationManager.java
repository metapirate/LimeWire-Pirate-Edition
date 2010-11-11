package org.limewire.core.api.download;

import java.io.File;

public interface SaveLocationManager {

    boolean isSaveLocationTaken(File candidateFile);

}
