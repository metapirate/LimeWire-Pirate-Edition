package com.limegroup.gnutella.downloader;

/** 
 * Thrown if we can't resume to a file, i.e., because it's not a valid
 * incomplete file. 
 */
public class CantResumeException extends Exception {
    private String _file;

    /**@param cause the cause of the Exception 
     * @param file the name of the file that couldn't be resumed */
    public CantResumeException(Throwable cause, String file) {
        super(cause);
        this._file=file;
    }
    
    /**@param message the cause of the Exception 
     * @param file the name of the file that couldn't be resumed */
    public CantResumeException(String message, String file) {
        super(message);
        this._file=file;
    }
    
    /** @param file the name of the file that couldn't be resumed */
    public CantResumeException(String file) {
        this._file=file;
    }

    /** Returns the name of the file that couldn't be resumed. */
    public String getFilename() {
        return _file;
    }
}
