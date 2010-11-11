package org.limewire.util;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/** Utilities relating to threads. */
public class ThreadUtils {
    
    /** Returns all stack traces, including lock info. */
    public static String getAllStackTraces() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        boolean monitor = threadMXBean.isObjectMonitorUsageSupported();
        boolean sync = threadMXBean.isSynchronizerUsageSupported();
        ThreadInfo[] allThreadInfo = threadMXBean.dumpAllThreads(monitor, sync);
        StringBuilder sb = new StringBuilder("Stack Trace Report:\n");
        buildTrace(allThreadInfo, sb);
        return sb.toString();
    }
    
    /** Builds the stack traces of all the given threadIDs in the buffer.  Returns the stack trace of the first thread. */
    public static StackTraceElement[] buildStackTraces(long[] threadIds, StringBuilder buffer) {
        ThreadInfo[] allThreadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(threadIds, true, true);
        return buildTrace(allThreadInfo, buffer);
    }
    
    /** Builds the stack traces of all the given ThreadInfos in the buffer.  Returns the stack trace of the first thread. */
    private static StackTraceElement[] buildTrace(ThreadInfo[] allThreadInfo, StringBuilder buffer) {
        StackTraceElement[] firstStackTrace = null;
        for (ThreadInfo info : allThreadInfo) {
            buffer.append("\"" + info.getThreadName() + "\" (id=" + info.getThreadId() + ")");
            buffer.append(" " + info.getThreadState() + " on " + info.getLockName() + " owned by ");
            buffer.append("\"" + info.getLockOwnerName() + "\" (id=" + info.getLockOwnerId() + ")");
            if (info.isSuspended())
                buffer.append(" (suspended)");
            if (info.isInNative())
                buffer.append(" (in native)");
            buffer.append("\n");
            StackTraceElement[] trace = info.getStackTrace();
            if(firstStackTrace == null)
                firstStackTrace = trace;
            for(int i = 0; i < trace.length; i++) {
                buffer.append("\tat " + trace[i].toString() + "\n");
                if(i == 0)
                    addLockInfo(info, buffer);
                addMonitorInfo(info, buffer, i);
            }
            
            addLockedSynchronizers(info, buffer);
            
            buffer.append("\n");
        }
        return firstStackTrace;
    }
    
    /** Returns the thread id of all potentially locked threads. */
    public static long[] findDeadlockedThreads() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        if(threadMXBean.isSynchronizerUsageSupported()) {
            return threadMXBean.findDeadlockedThreads();
        } else {
            return threadMXBean.findMonitorDeadlockedThreads();
        }
    }

    /** Add locked synchronizers data. */
    private static void addLockedSynchronizers(ThreadInfo info, StringBuilder sb) {
        LockInfo[] lockInfo = info.getLockedSynchronizers();
        if(lockInfo.length > 0) {
            sb.append("\n\tNumber of locked synchronizers = " + lockInfo.length + "\n");
            for(int i = 0; i < lockInfo.length; i++) {
                sb.append("\t- " + lockInfo[i] + "\n");
            }
        }
    }
    
    /** Add more specific locking details. */
    private static void addMonitorInfo(ThreadInfo info, StringBuilder sb, int stackDepth) {
        MonitorInfo[] monitorInfos = info.getLockedMonitors();
        for(int i = 0; i < monitorInfos.length; i++) {
            MonitorInfo mi = monitorInfos[i];
            int depth = mi.getLockedStackDepth();
            if(depth == stackDepth) {
                sb.append("\t-  locked " + mi + "\n");
            }
        }
    }
    
    /** Add the LockInfo data to the report. */
    private static void addLockInfo(ThreadInfo info, StringBuilder sb) {
        Thread.State ts = info.getThreadState();
        switch (ts) {
            case BLOCKED: 
                sb.append("\t-  blocked on " + info.getLockInfo() + "\n");
                break;
            case WAITING:
            case TIMED_WAITING:
                sb.append("\t-  waiting on " + info.getLockInfo() + "\n");
                break;
            default:
        }
    }

}
