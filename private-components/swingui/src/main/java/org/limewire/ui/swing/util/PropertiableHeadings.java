package org.limewire.ui.swing.util;

import org.limewire.core.api.library.PropertiableFile;

/**
 * Returns common property values for PropertiableFiles, taking into account
 * file categories (Video, Audio, etc).
 */
public interface PropertiableHeadings {

    String getHeading(PropertiableFile propertiable);
    
    String getSubHeading(PropertiableFile propertiable);

    String getFileSize(PropertiableFile propertiable);
}
