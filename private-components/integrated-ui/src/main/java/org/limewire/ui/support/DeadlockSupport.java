package org.limewire.ui.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.util.ThreadUtils;

/** Simple class to help monitor deadlocking. */
public class DeadlockSupport {
    
    private static Log LOG = LogFactory.getLog(DeadlockSupport.class);
    
    /** 
     * How often to check for deadlocks. 
     * 
     * This class doubles as a workaround for bug_id=6435126,
     * so it doesn't use a multiple of 10 for the sleep interval.
     */
    private static final int DEADLOCK_CHECK_INTERVAL = 3001;

    public static void startDeadlockMonitoring() {
        Thread t = ThreadExecutor.newManagedThread(new Runnable() {
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(DEADLOCK_CHECK_INTERVAL);
                    } catch (InterruptedException ignored) {}
                    LOG.trace("deadlock check start");
                    long [] ids = ThreadUtils.findDeadlockedThreads();
                    
                    if (ids == null) {
                        LOG.trace("no deadlocks found");
                        continue;
                    }
                    
                    StringBuilder sb = new StringBuilder("Deadlock Report:\n");
                    StackTraceElement[] firstStackTrace = ThreadUtils.buildStackTraces(ids, sb);                    
                    DeadlockException deadlock = new DeadlockException();
                    // Redirect the stack trace to separate deadlock reports.
                    if(firstStackTrace != null)
                        deadlock.setStackTrace(firstStackTrace);
                    
                    DeadlockBugManager.handleDeadlock(deadlock, Thread.currentThread().getName(), sb.toString());
                    return;
                }
            }
        });
        t.setDaemon(true);
        t.setName("Deadlock Detection Thread");
        t.start();
    }
}
