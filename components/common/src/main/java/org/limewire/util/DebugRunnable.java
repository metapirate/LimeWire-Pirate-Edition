package org.limewire.util;

/**
 * A Runnable that keeps the stack trace from when
 * it was created, so that an exception thrown while
 * running from another thread can keep the created stack trace.
 */
public class DebugRunnable implements Runnable {
    
    private final Exception creationTime;
    private final Runnable delegate;
    
    public DebugRunnable(Runnable runner) {
        this.creationTime = new Exception("Debug Exception Creation");
        this.delegate = runner;
        // Drop this line from the creationTime trace..
        StackTraceElement[] trace = creationTime.getStackTrace();
        StackTraceElement[] newTrace = new StackTraceElement[trace.length - 1];
        System.arraycopy(trace, 1, newTrace, 0, newTrace.length);
        creationTime.setStackTrace(newTrace);
    }
    
    public final void run() {
        boolean setStackTrace = true;
        try {
            delegate.run();
        } catch(Throwable t) {
            if(t.getCause() == null) {
                try { 
                    t.initCause(creationTime);
                    setStackTrace = false;
                } catch (IllegalStateException ise) {
                    // thrown if throwable was initialized with null cause for some reason
                }
            }
            if (setStackTrace) {
                // If it already had a cause, all we can do is manipulate the StackTraceElement[]
                StackTraceElement[] trace = t.getStackTrace();
                StackTraceElement[] createdTrace = creationTime.getStackTrace();
                StackTraceElement[] combinedTrace = new StackTraceElement[trace.length + createdTrace.length];
                System.arraycopy(trace, 0, combinedTrace, 0, trace.length);
                System.arraycopy(createdTrace, 0, combinedTrace, trace.length, createdTrace.length);
                t.setStackTrace(combinedTrace);
            }
            
            if(t instanceof Error)
                throw (Error)t;
            else if(t instanceof RuntimeException)
                throw (RuntimeException)t;
            else
                throw new RuntimeException(t);
        }
    }

}
