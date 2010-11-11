package org.limewire.ui.swing.properties;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.CommonUtils;

/**
 * Common conversions PropertiableFiles.
 */
public class FileInfoUtils {
    
    /**
     * Returns a String version of the DataCreated or empty String if no 
     * Date is available or there were problems parsing the date.
     */
    public static String convertDate(PropertiableFile propertiable) {
        Object time = propertiable.getProperty(FilePropertyKey.DATE_CREATED);
        if (time instanceof Long) {
            return GuiUtils.msec2DateTime((Long) time);
        }
        return "";
    }
    
    /**
     * Returns the length of a playable file in hr:min:sec format or
     * empty String if no length is available.
     */
    public static String getLength(PropertiableFile propertiableFile) {
        Long length = (Long)propertiableFile.getProperty(FilePropertyKey.LENGTH);
        return length != null ? CommonUtils.seconds2time(length) : null;
    }
    
    /**
     * Returns the file size in bytes/KB/MB/GB (bytes) format or empty string
     * if no size exists.
     */
    public static String getFileSize(PropertiableFile propertiable) {
        Long fileSize = getFileSizeLong(propertiable);
        if (fileSize != null) {
            return GuiUtils.formatUnitFromBytes(fileSize);
        }
        return "";
    }
    
    /**
     * Returns the exact file size in bytes format or empty string
     * if no size exists.
     */
    public static String getFileSizeBytes(PropertiableFile propertiable) {
        Long fileSize = getFileSizeLong(propertiable);
        if (fileSize != null) {
            return GuiUtils.formatBytes(fileSize);
        }
        return "";
    }
    
    private static Long getFileSizeLong(PropertiableFile propertiable) {
        return (Long)propertiable.getProperty(FilePropertyKey.FILE_SIZE);
    }
    
    /**
     * Returns the quality of this file.
     */
    public static String getQuality(PropertiableFile propertiableFile) {
        Long qualityScore = getQualityScoreLong(propertiableFile);
        return qualityScore != null ? GuiUtils.toQualityString(qualityScore) : null;
    }
    
    private static Long getQualityScoreLong(PropertiableFile propertiable) {
        return (Long)propertiable.getProperty(FilePropertyKey.QUALITY);
    }
}
