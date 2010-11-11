package org.limewire.ui.swing;

import java.util.ArrayList;
import java.util.List;

import org.limewire.service.ErrorService;
import com.sun.jna.Callback;

/**
 * This class serves as the error handler for both java threads
 * as well as errors occurring in JNA callbacks.  
 */
public class UncaughtExceptionHandlerImpl implements Thread.UncaughtExceptionHandler, 
                                                     Callback.UncaughtExceptionHandler {
    private final List<StackTraceElement> notReported;

    public UncaughtExceptionHandlerImpl() {
        notReported = new ArrayList<StackTraceElement>();
        notReported.add(new StackTraceElement("javax.jmdns.DNSRecord", "suppressedBy", null, -1));
        // add more unreported stacktraces here.
    }


    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        handleUncaughtException(thread.getName(), throwable);
    }
    
    
    @Override
    public void uncaughtException(Callback c, Throwable e) {
        handleUncaughtException(Thread.currentThread().getName(), e);
    }
    
    private void handleUncaughtException(String name, Throwable throwable) {
        StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            if (matchesUnreported(stackTraceElement)) {
                throwable.printStackTrace();
                return;
            }
        }
        ErrorService.error(throwable, "Uncaught thread error: " + name);
    }

    /**
     * Checks to see if the give stack trace matches any of the stacktraces
     * which we will NOT report.
     */
    private boolean matchesUnreported(StackTraceElement stackTraceElement) {
        for (StackTraceElement notReportedStackTrace : notReported) {
            if (matches(notReportedStackTrace, stackTraceElement)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if a given stack trace element matches against a given
     * filter. For a match to be successful, either the ClassName methodName and
     * line number must match. Or the class name method name can match and the
     * filter line number can be a wild card by having a negative value.
     */
    private boolean matches(StackTraceElement filter, StackTraceElement element) {
        return filter.getClassName().equals(element.getClassName())
                && filter.getMethodName().equals(element.getMethodName())
                && (filter.getLineNumber() < 0 || filter.getLineNumber() == element.getLineNumber());
    }
}
