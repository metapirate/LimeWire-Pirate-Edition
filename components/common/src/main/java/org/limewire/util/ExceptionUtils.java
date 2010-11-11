package org.limewire.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.UndeclaredThrowableException;

public class ExceptionUtils {

    private ExceptionUtils() {
    }

    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }
    
    /**
     * Rethrows a Throwable as either a {@link RuntimeException}, {@link Error},
     * or an {@link UndeclaredThrowableException}.
     */
    public static void rethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } else {
            throw new UndeclaredThrowableException(t);
        }
    }
    
    /**
     * Reports an exception to the current thread's
     * {@link UncaughtExceptionHandler}, or if that is null reports to
     * {@link Thread#getDefaultUncaughtExceptionHandler()}, or if that is null,
     * returns the exception.
     * 
     */
    public static Throwable reportOrReturn(Throwable t) {
        UncaughtExceptionHandler handler = Thread.currentThread().getUncaughtExceptionHandler();
        if (handler == null) {
            handler = Thread.getDefaultUncaughtExceptionHandler();
        }
        
        if (handler != null) {
            handler.uncaughtException(Thread.currentThread(), t);
            return null;
        } else {
            return t;
        }
    }

    /**
     * Reports an exception to the current thread's
     * {@link UncaughtExceptionHandler}, or if that is null reports to
     * {@link Thread#getDefaultUncaughtExceptionHandler()}, or if that is null,
     * rethrows the exception.
     * 
     */
    public static void reportOrRethrow(Throwable t) {
        UncaughtExceptionHandler handler = Thread.currentThread().getUncaughtExceptionHandler();
        if (handler == null) {
            handler = Thread.getDefaultUncaughtExceptionHandler();
        }
        
        if (handler != null) {
            handler.uncaughtException(Thread.currentThread(), t);
        } else {
            rethrow(t);
        }
    }
}
