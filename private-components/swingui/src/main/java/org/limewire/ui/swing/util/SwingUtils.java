package org.limewire.ui.swing.util;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.limewire.util.ExceptionUtils;


public class SwingUtils {
    
    private SwingUtils() {}
    
    /**
     * Calls {@link SwingUtilities#invokeAndWait(Runnable)}
     * only if this is not currently on the Swing thread.
     */
    public static void invokeNowOrWaitWithInterrupted(Runnable runnable) throws InterruptedException {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
        } else {
            try {
                EventQueue.invokeAndWait(runnable);
            } catch (InvocationTargetException ite) {
                ExceptionUtils.rethrow(ite.getCause());
            }
        }
    }

    /**
     * Calls {@link SwingUtilities#invokeAndWait(Runnable)}
     * only if this is not currently on the Swing thread.
     */
    public static void invokeNowOrWait(Runnable runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
        } else {
            try {
                EventQueue.invokeAndWait(runnable);
            } catch (InvocationTargetException ite) {
                ExceptionUtils.rethrow(ite.getCause());
            } catch(InterruptedException ignored) {
                throw new RuntimeException(ignored);
            }
        }
    }
    
    /**
     * Calls {@link SwingUtilities#invokeLater(Runnable)} only
     * if this is not currently on the Swing thread.
     */
    public static void invokeNowOrLater(Runnable runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
        } else {
            EventQueue.invokeLater(runnable);
        }
    }

}
