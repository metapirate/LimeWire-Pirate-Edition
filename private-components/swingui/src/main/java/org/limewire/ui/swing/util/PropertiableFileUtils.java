package org.limewire.ui.swing.util;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.util.StringUtils;

public class PropertiableFileUtils {
    
    public static String getNameProperty(PropertiableFile file, boolean useAudioArtist) {
        String name = file.getPropertyString(FilePropertyKey.NAME);
        
        // For audio files, use non-blank title, prefixed by non-blank artist.
        if (file.getCategory().equals(Category.AUDIO)) {
            String title = file.getPropertyString(FilePropertyKey.TITLE);
            if(!StringUtils.isEmpty(title)) {
                String artist = file.getPropertyString(FilePropertyKey.AUTHOR);
                if (useAudioArtist && !StringUtils.isEmpty(artist)) {
                    name = artist + " - " + title;
                } else {
                    name = title;
                }
            }
        }
        
        // Return result.
        return name;
    }

}
