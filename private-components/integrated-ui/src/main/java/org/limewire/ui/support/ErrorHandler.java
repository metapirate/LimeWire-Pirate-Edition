package org.limewire.ui.support;

import javax.swing.SwingUtilities;

import org.limewire.service.ErrorCallback;

/** Forwards error messages to the BugManager on the Swing thread. */
public final class ErrorHandler implements ErrorCallback {
    private final BugManager bugManager;
    
    public ErrorHandler(BugManager bugManager) {
        this.bugManager = bugManager;
    }
    
    /** Displays the error to the user. */
    public void error(Throwable problem) {
        error(problem, null);
    }

    /** Displays the error to the user with a specific message. */
	public void error(Throwable problem, String msg) {
        // ThreadDeath must NOT be caught, or a thread will be left zombied     
        if(problem instanceof ThreadDeath)
            throw (ThreadDeath)problem;
        else {
            // force to end of queue, so we don't continue holding
            // any locks if this was already called from the UI thread.
            SwingUtilities.invokeLater(new Error(problem, msg));
        }
    }

    /** This class handles error callbacks. */
    private class Error implements Runnable {
        /** The stack trace of the error. */
        private final Throwable PROBLEM;
        /** An extra message associated with the error. */
        private final String MESSAGE;
        /** The name of the thread the error occurred in. */
        private final String CURRENT_THREAD_NAME;
        
        private Error(Throwable problem, String msg) {
            PROBLEM = problem;
            MESSAGE = msg;
            CURRENT_THREAD_NAME = Thread.currentThread().getName();
        }

        public void run() {
            try {
                // TODO: Deal with startup dialogs?
                //GUIMediator.closeStartupDialogs();
                bugManager.handleBug(PROBLEM, CURRENT_THREAD_NAME, MESSAGE);
            } catch(Throwable ignored) {
                ignored.printStackTrace();
            }
        }
    }
}
