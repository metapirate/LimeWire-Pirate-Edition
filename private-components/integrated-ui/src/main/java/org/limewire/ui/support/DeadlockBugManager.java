package org.limewire.ui.support;

public class DeadlockBugManager {

   private DeadlockBugManager() {}
    
    /** Handles a deadlock bug. */
    public static void handleDeadlock(DeadlockException bug, String threadName, String message) {
        bug.printStackTrace();
        System.err.println("Detail: " + message);
    }
}
