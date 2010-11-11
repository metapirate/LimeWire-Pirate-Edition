package org.limewire.core.api.download;

import java.io.File;
import java.io.IOException;

/**
 * IOException which can be thrown from when setting the save location on a
 * downloader.
 */
public class DownloadException extends IOException {

    public static enum ErrorCode {
        /**
         * Attempt to change save location that violates security rules, such as
         * attempting directory traversal .
         */
        SECURITY_VIOLATION,
        /** Attempt to change save location too late to save file in new place. */
        FILE_ALREADY_SAVED,
        /**
         * Attempt to change save location to a directory where files cannot be
         * created.
         */
        DIRECTORY_NOT_WRITEABLE,
        /** Attempt to change save location to a non-existent directory. */
        DIRECTORY_DOES_NOT_EXIST,
        /** Attempt to change save location to a File that already exists. */
        FILE_ALREADY_EXISTS,
        /**
         * Attempt to change save location to a file which is already reserved
         * by another download.
         */
        FILE_IS_ALREADY_DOWNLOADED_TO,
        /**
         * Attempt to change save location to a pre-existing file that isn't a
         * regular file (such as a directory or device file).
         */
        FILE_NOT_REGULAR,
        /**
         * Attempt to change save directory to a "directory" that exists, but is
         * not a directory.
         */
        NOT_A_DIRECTORY,
        /** IOException or other file system error while setting save location. */
        FILESYSTEM_ERROR,
        /**
         * Attempt to download the exact same file (urn, filename, size) while
         * it is already being downloaded.
         */
        FILE_ALREADY_DOWNLOADING,
        /**
         * Thrown when the directory to save in already exceeds the maximum path
         * name on the OS.
         */
        PATH_NAME_TOO_LONG,

        /**
         * Thrown when trying to open a torrent file that is too large.
         */
        TORRENT_FILE_TOO_LARGE,

        /**
         * Throw when trying to register a torrent if the torrent manager
         * is not loaded.
         */
        NO_TORRENT_MANAGER, 
        
        /**
         * Throw when trying to start a new download before resume downloaders have finished running.
         */
        FILES_STILL_RESUMING, 
        
        /**
         * Thrown when trying to escape the add download logic because the user cancelled the download.
         */
        DOWNLOAD_CANCELLED, 
        /**
         * Thrown when the user tries to download an already uploading torrent file. 
         */
        FILE_ALREADY_UPLOADING
    }

    /**
     * The error code of this exception.
     */
    private final ErrorCode errorCode;

    /**
     * Handle to the file that caused the exception.
     */
    private final File file;

    /**
     * Constructs a DownloadException with the specified cause and file.
     */
    public DownloadException(IOException cause, File file) {
        super(cause);
        this.errorCode = ErrorCode.FILESYSTEM_ERROR;
        this.file = file;
    }

    public DownloadException(ErrorCode errorCode, File file) {
        super("error code " + errorCode + ", file " + file);
        this.errorCode = errorCode;
        this.file = file;
    }

    /**
     * Constructs a DownloadException for the specified error code.
     * 
     * @param message optional more detailed message for debugging purposes
     */
    public DownloadException(ErrorCode errorCode, File file, String message) {
        super(message);
        this.errorCode = errorCode;
        this.file = file;
    }

    /**
     * Returns the error code of this exception.
     * 
     * @return
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public File getFile() {
        return file;
    }

}
