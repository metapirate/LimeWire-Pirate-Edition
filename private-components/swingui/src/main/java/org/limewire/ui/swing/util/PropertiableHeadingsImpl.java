package org.limewire.ui.swing.util;

import java.text.SimpleDateFormat;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.inject.LazySingleton;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
class PropertiableHeadingsImpl implements PropertiableHeadings {
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");
    private final Provider<IconManager> iconManager;
    
    @Inject
    public PropertiableHeadingsImpl(Provider<IconManager> iconManager) {
        this.iconManager = iconManager;
    }
    
    @Override
    public String getHeading(PropertiableFile propertiable) {
        Object property = propertiable.getProperty(FilePropertyKey.NAME);
        String name = property == null ? "" : property.toString();
        String renderName = "";
        switch (propertiable.getCategory()) {
        case AUDIO:
            String artist = propertiable.getPropertyString(FilePropertyKey.AUTHOR);
            String title = propertiable.getPropertyString(FilePropertyKey.TITLE);
            if (!StringUtils.isEmpty(artist) && !StringUtils.isEmpty(title)) {
                renderName = artist + " - " + title;
            } else {
                renderName = name;
            }
            break;
        case VIDEO:
        case IMAGE:
        case DOCUMENT:
        case PROGRAM:
        case OTHER:
        default:
            renderName = name + "." + getFileExtension(propertiable);
        }
        return renderName.trim();
    }

    private String getFileExtension(PropertiableFile propertiableFile) {
        return FileUtils.getFileExtension(propertiableFile.getFileName());
    }

    @Override
    public String getSubHeading(PropertiableFile propertiable) {
        //TODO: Unit test this class (then refactor)!!! So many conditions :-(
        String subheading = "";

        switch (propertiable.getCategory()) {
        case AUDIO: {
            String albumTitle = propertiable.getPropertyString(FilePropertyKey.ALBUM);
            Long qualityScore = (Long)propertiable.getProperty(FilePropertyKey.QUALITY);
            Long length = (Long)propertiable.getProperty(FilePropertyKey.LENGTH);

            boolean insertHyphen = false;
            if (!StringUtils.isEmpty(albumTitle)) {
                subheading += albumTitle;
                insertHyphen = true;
            }

            if (qualityScore != null) {
                if (insertHyphen) {
                    subheading += " - ";
                }
                subheading += GuiUtils.toQualityString(qualityScore);
                Long bitRate = (Long)propertiable.getProperty(FilePropertyKey.BITRATE);
                if (bitRate != null) {
                    subheading += " (" + bitRate+ ")";
                }
                insertHyphen = true;
            }

            if (length != null) {
                subheading = addLength(subheading, length, insertHyphen);
            } else {
                Long fileSize = (Long)propertiable.getProperty(FilePropertyKey.FILE_SIZE);
                subheading = addFileSize(subheading, fileSize, insertHyphen);
            }
        }
            break;
        case VIDEO: {
            Long qualityScore = (Long)propertiable.getProperty(FilePropertyKey.QUALITY);
            Long length = (Long)propertiable.getProperty(FilePropertyKey.LENGTH);
            Long fileSize = (Long)propertiable.getProperty(FilePropertyKey.FILE_SIZE);

            boolean insertHyphen = false;
            if (qualityScore != null) {
                subheading += GuiUtils.toQualityString(qualityScore);
                insertHyphen = true;
            }

            subheading = addLength(subheading, length, insertHyphen);
            
            subheading = addFileSize(subheading, fileSize, insertHyphen);
            
        }
            break;
        case IMAGE: {
            Long fileSize = (Long)propertiable.getProperty(FilePropertyKey.FILE_SIZE);
            
            boolean insertHyphen = false;
            Object time = propertiable.getProperty(FilePropertyKey.DATE_CREATED);
            if (time != null  && time instanceof Long) {
                subheading = DATE_FORMAT.format(new java.util.Date((Long) time));
                insertHyphen = true;
            }
            
            subheading = addFileSize(subheading, fileSize, insertHyphen);
        }
            break;
        case PROGRAM: {
            subheading = getFileSize(propertiable);
        }
            break;
        case DOCUMENT:
        case OTHER:
        default: {
             subheading = iconManager.get().getMIMEDescription(propertiable);
             subheading = subheading == null ? "" : subheading;
            // TODO add name of program used to open this file, not included in
            // 5.0
            Long fileSize = (Long)propertiable.getProperty(FilePropertyKey.FILE_SIZE);
            subheading = addFileSize(subheading, fileSize, !"".equals(subheading));
        }
        }
        return subheading == null ? "" : subheading;

    }

    private String addLength(String subheading, Long length, boolean insertHyphen) {
        if (length != null) {
            if (insertHyphen) {
                subheading += " - ";
            }
            subheading += CommonUtils.seconds2time(length);
        }
        return subheading;
    }

    private String addFileSize(String subheading, Long fileSize, boolean insertHyphen) {
        if (fileSize != null) {
            if (insertHyphen) {
                subheading += " - ";
            }
            subheading += GuiUtils.formatUnitFromBytes(fileSize);
        }
        return subheading;
    }
    
    @Override
    public String getFileSize(PropertiableFile propertiable) {
        Long fileSize = (Long)propertiable.getProperty(FilePropertyKey.FILE_SIZE);
        if (fileSize != null) {
            return GuiUtils.formatUnitFromBytes(fileSize) + "  (" + GuiUtils.formatBytes(fileSize) + ")";
        }
        return "";
    }
}
