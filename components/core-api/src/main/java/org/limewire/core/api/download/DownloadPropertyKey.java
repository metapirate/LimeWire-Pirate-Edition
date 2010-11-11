package org.limewire.core.api.download;

/**
 * Used in the getDownloadProperty method in DownloadItem to allow getting
 * different properties from the different downloaders available.
 */
public enum DownloadPropertyKey {
    /** The AntivirusUpdateType*/
    ANTIVIRUS_UPDATE_TYPE, 
    /**The index of the update (if the update is incremental)*/
    ANTIVIRUS_INCREMENT_INDEX, 
    /** The total number of increments (if the update is incremental)*/
    ANTIVIRUS_INCREMENT_COUNT,
    /** A hint for the reason of a scan failure. */
    ANTIVIRUS_FAIL_HINT,
}
