package org.limewire.ui.swing;

import java.awt.IllegalComponentStateException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.service.ErrorService;


/**
 * @author jum
 *
 * Implement a generic error handler that catches all errors thrown
 * by ActionListeners in the AWT event dispatcher thread.
 */
public class DefaultErrorCatcher {
    
    private static volatile boolean storeCaughtBugs = false;
    private static final List<Throwable> storedBugs = new CopyOnWriteArrayList<Throwable>(); 

	static void install() {
	    System.setProperty("sun.awt.exception.handler",
	                       DefaultErrorCatcher.class.getName());
    }

	/** Sets whether or not bugs are captured or reported to ErrorService. */
    static void storeCaughtBugs() {
        storeCaughtBugs = true;
    }
    
    /** Gets all captured bugs & unsets storing. */
    static List<Throwable> getAndResetStoredBugs() {
        storeCaughtBugs = false;
        List<Throwable> list = new ArrayList<Throwable>(storedBugs);
        storedBugs.clear();
        return list;
    }
	
	public void handle(Throwable ex) {
	    StringWriter sw = new StringWriter();
	    PrintWriter pw = new PrintWriter(sw);
	    ex.printStackTrace(pw);
	    pw.flush();
	    String bug = sw.toString();
	    
	    if(!isIgnorable(ex, bug)) {
	        if(storeCaughtBugs) {
	            storedBugs.add(ex);
	        } else {
	            ErrorService.error(ex, "Uncaught event-thread error.");
	        }
	    } else {
		    System.err.println("Ignoring error:");
		    ex.printStackTrace();
        }
	}
	
	/**
	 * Determines if the message can be ignored.
	 */
	private boolean isIgnorable(Throwable bug, String msg) {
	    // ignore all overflows,
	    // since they'll give us absolutely no debugging information
	    if(bug instanceof StackOverflowError)
	        return true;
	        
        // no bug?  kinda impossible, but shouldn't report.
	    if(msg == null) {
	        return true;
	    }
	        
        // frickin' repaint manager stinks.
        if(msg.indexOf("javax.swing.RepaintManager") != -1)
            return true;
        if(msg.indexOf("sun.awt.RepaintArea.paint") != -1)
            return true;
         
        // display manager on OSX goes out of whack   
        if(bug instanceof ArrayIndexOutOfBoundsException) {
            if(msg.indexOf("apple.awt.CWindow.displayChanged") != -1)
                return true;
            if(msg.indexOf("javax.swing.plaf.basic.BasicTabbedPaneUI.getTabBounds") != -1)
                return true;
        }
        
        // system clipboard can be held, preventing us from getting.
        // throws a RuntimeException through stuff we don't control...
        if(bug instanceof IllegalStateException) {
            if(msg.indexOf("cannot open system clipboard") != -1)
                return true;
        }
        
        // odd component exception
        if(bug instanceof IllegalComponentStateException) {
            if(msg.indexOf("component must be showing on the screen to determine its location") != -1)
                return true;
        }
	        
        // various NPEs we can ignore:
        if(bug instanceof NullPointerException) {
            if(msg.indexOf("MetalFileChooserUI") != -1)
                return true;
            if(msg.indexOf("WindowsFileChooserUI") != -1)
                return true;
            if(msg.indexOf("AquaDirectoryModel") != -1)
                return true;
            if(msg.indexOf("SizeRequirements.calculateAlignedPositions") != -1)
                return true;
            if(msg.indexOf("BasicTextUI.damageRange") != -1)
                return true;
            if(msg.indexOf("null pData") != -1)
                return true;
            if(msg.indexOf("disposed component") != -1)
                return true;
        }
        
        // various InternalErrors we can ignore.
        if(bug instanceof InternalError) {
            if(msg.indexOf("getGraphics not implemented for this component") != -1)
                return true;
        }
	    
	    // if we're not somewhere in the bug, ignore it.
	    // no need for us to debug sun's internal errors.
	    if(msg.indexOf("com.limegroup.gnutella") == -1 && msg.indexOf("org.limewire") == -1) {
	        return true;
	    }
	    
	    StackTraceElement[] stack = bug.getStackTrace();
	    if(stack != null) {
	        // Internal errors with Swing's FilePane -- we can't do anything about it
	        if(bug instanceof IndexOutOfBoundsException && stack.length > 2) {
	            if(stack[0].getClassName().equals("javax.swing.DefaultRowSorter") && stack[1].getClassName().equals("sun.swing.FilePane$SortableListModel")) {
	                return true;
	            }
	        }
	        if(bug instanceof NullPointerException && stack.length > 2) {
	            if(stack[0].getClassName().equals("javax.swing.JComponent") && stack[1].getClassName().equals("sun.swing.FilePane$2")) {
	                return true;
	            }
	        }	        
	    }
	        
        return false;
    }
}
