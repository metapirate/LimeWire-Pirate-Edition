package org.limewire.logging;

/**
 * Extends {@link org.apache.commons.logging.Log} and defines convenience methods 
 * to allow conditional message formatting. 
 */
public interface Log extends org.apache.commons.logging.Log {

    /**
     * Logs <code>message</code> as debug message and formats message
     * with <code>args</code> if {@link #isDebugEnabled()} is true.
     */
    void debugf(Throwable t, String message, Object...args);
    
    void debugf(String message, Object...args);
    
    void debugf(String message, Object args);
    
    void debugf(String message, Object arg1, Object arg2);
    
    void debugf(String message, Object arg1, Object arg2, Object arg3);
    /**
     * Logs <code>message</code> as trace message and formats message
     * with <code>args</code> if {@link #isTraceEnabled()} is true.
     */
    void tracef(Throwable t, String message, Object...args);
    
    void tracef(String message, Object...args);

    void tracef(String message, Object args);
    
    void tracef(String message, Object arg1, Object arg2);
    
    void tracef(String message, Object arg1, Object arg2, Object arg3);
    /**
     * Logs <code>message</code> as info message and formats message
     * with <code>args</code> if {@link #isInfoEnabled()} is true.
     */
    void infof(Throwable t, String message, Object...args);
    
    void infof(String message, Object...args);
    
    void infof(String message, Object args);
    
    void infof(String message, Object arg1, Object arg2);
    
    void infof(String message, Object arg1, Object arg2, Object arg3);
    /**
     * Logs <code>message</code> as warn message and formats message
     * with <code>args</code> if {@link #isWarnEnabled()} is true.
     */
    void warnf(Throwable t, String message, Object...args);
    
    void warnf(String message, Object...args);
    
    void warnf(String message, Object args);
    
    void warnf(String message, Object arg1, Object arg2);
    
    void warnf(String message, Object arg1, Object arg2, Object arg3);
    
    /**
     * Logs <code>message</code> as error message and formats message
     * with <code>args</code> if {@link #isErrorEnabled()} is true.
     */
    void errorf(Throwable t, String message, Object...args);
    
    void errorf(String message, Object...args);
    
    void errorf(String message, Object args);
    
    void errorf(String message, Object arg1, Object arg2);
    
    void errorf(String message, Object arg1, Object arg2, Object arg3);
}
