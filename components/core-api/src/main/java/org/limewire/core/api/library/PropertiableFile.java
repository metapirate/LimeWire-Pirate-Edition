package org.limewire.core.api.library;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;

public interface PropertiableFile {
    
    /** Returns the complete file name (including extensions) for this file. */
    String getFileName();
    
    /** Returns the category this file belongs in. */
    Category getCategory();

    /** Returns xml data about this fileItem. */
    Object getProperty(FilePropertyKey key);

    /** Returns a string representation of this items xml data. */
    String getPropertyString(FilePropertyKey filePropertyKey);
    
    /** Returns a unique identifier for this file. */
    URN getUrn();
}
