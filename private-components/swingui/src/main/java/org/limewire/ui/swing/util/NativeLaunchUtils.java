package org.limewire.ui.swing.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import javax.swing.JOptionPane;

import org.limewire.concurrent.ManagedThread;
import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.util.OSUtils;
import org.limewire.util.SystemUtils;



/**
 * This class launches files in their associated applications and opens 
 * urls in the default browser for different operating systems.  This
 * really only works meaningfully for the Mac and Windows.<p>
 *
 * Acknowledgement goes to Eric Albert for demonstrating the general 
 * technique for loading the MRJ classes in his frequently-used
 * "BrowserLauncher" code.
 * <p>
 * This code is Copyright 1999-2001 by Eric Albert (ejalbert@cs.stanford.edu) 
 * and may be redistributed or modified in any form without restrictions as
 * long as the portion of this comment from this paragraph through the end of  
 * the comment is not removed.  The author requests that he be notified of any 
 * application, applet, or other binary that makes use of this code, but that's 
 * more out of curiosity than anything and is not required.  This software
 * includes no warranty.  The author is not repsonsible for any loss of data 
 * or functionality or any adverse or unexpected effects of using this software.
 * <p>
 * Credits:
 * <br>Steven Spencer, JavaWorld magazine 
 * (<a href="http://www.javaworld.com/javaworld/javatips/jw-javatip66.html">Java Tip 66</a>)
 * <br>Thanks also to Ron B. Yeh, Eric Shapiro, Ben Engber, Paul Teitlebaum, 
 * Andrea Cantatore, Larry Barowski, Trevor Bedzek, Frank Miedrich, and Ron 
 * Rabakukk
 *
 * @author Eric Albert 
 *  (<a href="mailto:ejalbert@cs.stanford.edu">ejalbert@cs.stanford.edu</a>)
 * @version 1.4b1 (Released June 20, 2001)
 */
public final class NativeLaunchUtils {
    private static final Log LOG = LogFactory.getLog(NativeLaunchUtils.class);

    /** 
     * This class should be never be instantiated; this just ensures so. 
     */
    private NativeLaunchUtils() {}
    
    /**
     * Opens the specified url in a browser. 
     *
     * <p>A browser will only be opened if the underlying operating system 
     * recognizes the url as one that should be opened in a browser, 
     * namely a url that ends in .htm or .html.
     *
     * @param url the url to open
     */
    public static void openURL(final String url) {
        ManagedThread managedThread = new ManagedThread( new Runnable() {
            @Override
            public void run() {
                try {
                    if (OSUtils.isWindows()) {
                        openURLWindows(url);
                    } else if (OSUtils.isMacOSX()) {
                        openURLMac(url);
                    } else {
                        openURLLinux(url);
                    }
                } catch (IOException iox) {
                    // Desktop.browse has various problems on different OSs. Trying
                    // native calls and falling back to this if an error occurs.
                    try {
                        Desktop.getDesktop().browse(new URI(url));
                    } catch (Throwable t) {
                        logException(I18n.tr("Unable to open URL"), I18n.tr("Open URL"), new Exception(t));
                    } 
                }
            }
        });
        managedThread.start();
    }

    /**
     * Trys to open the url with the default browser on linux, passing it the specified
     * url.
     * 
     * @param url the url to open in the browser
     */
    private static Process openURLLinux(String url) throws IOException {
        return exec("xdg-open", url);
    }

    /**
     * Opens the default web browser on windows, passing it the specified
     * url.
     *
     * @param url the url to open in the browser
     */
    private static void openURLWindows(String url) throws IOException {
        SystemUtils.openURL(url);
    }

    /**
     * Opens the specified url in the default browser on the Mac.
     * This previously made use of the dynamically-loaded MRJ classes.
     * This library is no longer being used, however, and now, 
     * it's using the JDK 6 method Desktop.browse(URI)
     *
     * @param url the url to load
     *
     * @throws <tt>IOException</tt> if the necessary Mac classes were not
     *         loaded successfully or if another exception was
     *         throws -- it wraps these exceptions in an <tt>IOException</tt>
     */
    private static void openURLMac(String url) throws IOException {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (URISyntaxException e) {
            throw new IOException();
        }
    }

    /**
     * Launches the specified file.  If the file's Category is PROGRAM or OTHER, this delegates to 
     * <code>launchExplorer(file)</code>
     */
    public static void safeLaunchFile(File file, CategoryManager categoryManager){
        Category category = categoryManager.getCategoryForFile(file);
        if(category == Category.PROGRAM || category == Category.OTHER){
            launchExplorer(file);
        } else {
            launchFile(file);
        }
    }

    /**
     * Launches the file whose abstract path is specified in the <tt>File</tt>
     * parameter. This method will not launch any file with .exe, .vbs, .lnk,
     * .bat, .sys, or .com extensions, displaying an error if one of the file is
     * of one of these types.
     * 
     * This is run on its own thread to prevent ui calls from blocking.
     *
     * @param file the file to launch
     * @return an object for accessing the launch process; null, if the process
     *         can be represented (e.g. the file was launched through a native
     *         call)
     */
    private static void launchFile(final File file) {
        ManagedThread managedThread = new ManagedThread( new Runnable() {
            @Override
            public void run() {
                try {
                    launchFileImpl(file);
                } catch (LaunchException lex) {
                    logException(I18n.tr("Unable to open file: {0}", file.getName()),
                            I18n.tr("Open File"), lex);
                } catch (IOException iox) {
                    logException(I18n.tr("Unable to open file: {0}", file.getName()),
                            I18n.tr("Open File"), iox);
                } catch (SecurityException ex) {
                    logException(I18n.tr("Unable to open file: {0}", file.getName()),
                            I18n.tr("Open File"), ex);
                }
            }
        });
        managedThread.start();
        
    }
    
    private static void launchFileImpl(File file) throws IOException, SecurityException {
        String path = file.getCanonicalPath();
        String extCheckString = path.toLowerCase(Locale.US);

        if(!extCheckString.endsWith(".exe") &&
                    !extCheckString.endsWith(".vbs") &&
                    !extCheckString.endsWith(".lnk") &&
                    !extCheckString.endsWith(".bat") &&
                    !extCheckString.endsWith(".sys") &&
                    !extCheckString.endsWith(".com")) {
            openFile(file);
        } else {
            throw new SecurityException();
        }
    }

    private static void openFile(File file) throws IOException {
        String path = file.getCanonicalPath();

        try {
            if (OSUtils.isWindows()) {
                launchFileWindows(path);
            } else if (OSUtils.isMacOSX()) {
                launchFileMacOSX(path);
            } else {
                launchFileLinux(path);
            }
        } catch(IOException e) {
            // Desktop.open has various problems on different OSs. Trying
            // native calls and falling back to this if an error occurs.
            try {
                Desktop.getDesktop().open(file);
            } catch(Throwable t) {
                throw new IOException(t);
            }
        }
    }

    /**
     * Launches the Explorer/Finder and highlights the file.
     * 
     * @param file the file to show in explorer
     * @return null, if not supported by platform; the launched process otherwise
     * @see #safeLaunchFile(File)
     */
    public static Process launchExplorer(File file) {
        try {
            return launchExplorerImpl(file);
        } catch (LaunchException lex) {
            logException(I18n.tr("Unable to locate file: {0}", file.getName()),
                    I18n.tr("Locate File"), lex);
            return null;
        } catch (SecurityException ex) {
            logException(I18n.tr("Unable to locate file: {0}", file.getName()),
                    I18n.tr("Locate File"), ex);
            return null;
        } catch (IOException iox) {
            logException(I18n.tr("Unable to locate file: {0}", file.getName()),
                    I18n.tr("Locate File"), iox);
            return null;
        }
    }
    
    private static Process launchExplorerImpl(File file) throws IOException, SecurityException {
        if (OSUtils.isWindows()) {
            String explorePath = file.getPath(); 
            try { 
                explorePath = file.getCanonicalPath(); 
            } catch (IOException ignored) {
            } 
            
            // the file path is quoted to escape any commas
            // that may be located within the path since the openFile
            // command is comma delimited
            explorePath = "\"" + explorePath + "\"";
            
            if(file.isDirectory()) {
                SystemUtils.openFile("explorer", explorePath);
                
            } else {
                // launches explorer and highlights the file
                SystemUtils.openFile("explorer", "/select," + explorePath);
            }
            
        } else if (OSUtils.isMacOSX()) {
            // launches the Finder and highlights the file
            return exec(selectFileCommand(file));
        } else if (OSUtils.isLinux()) {
            // launches the Finder and highlights the file
            return exec(selectFileCommandLinux(file));
        }
        return null;
    }
    
    private static String[] selectFileCommandLinux(File file) {
        String path = null;
        File parentDir = file.isDirectory() ? file : file.getParentFile();
        try {
            path = parentDir.getCanonicalPath();
        } catch (IOException err) {
            path = parentDir.getAbsolutePath();
        }
        return new String[] {"xdg-open", path};        
    }

    /**
     * Launches the given file on Linux.
     *
     * @param path the path of the file to launch
     *
     * @return Process which was used to open the file. In this case the xdg-open. 
     * The actual file will be opened in another process.
     */
    private static Process launchFileLinux(String path) throws IOException {
        return exec("xdg-open", path);
    }
    
    /**
     * Launches the given file on Windows.
     *
     * @param path the path of the file to launch
     *
     * @return an int for the exit code of the native method
     */
    private static int launchFileWindows(String path) throws IOException {
        return SystemUtils.openFile(path);
    }

    /**
     * Launches a file on OSX, appending the full path of the file to the
     * "open" command that opens files in their associated applications
     * on OSX.
     *
     * @param filename the <tt>File</tt> instance denoting the abstract pathname
     *  of the file to launch
     * @throws IOException if an I/O error occurs in making the runtime.exec()
     *  call or in getting the canonical path of the file
     */
    private static Process launchFileMacOSX(final String filename) throws IOException {
        Process process = exec(new String[]{"open", filename});
        try {
            process.waitFor();
            // If the exit value is 1, then no handler could be found for the given file.
            // Let's show a pop-up explaining that the file could not be opened.
            if (process.exitValue() == 1) {
                String message = I18n.tr("The file ") + new File(filename).getName() + I18n.tr(" could not be opened! There are no registered applications on your system for this file type.");
                String title = I18n.tr("File cannot be opened");
                JOptionPane.showMessageDialog(GuiUtils.getMainFrame(), message, title, JOptionPane.ERROR_MESSAGE); 
            }
        } catch (InterruptedException e) {
        }

        return process;
    }
    
    /**
     * Launches the Finder and selects the given File.
     */
    private static String[] selectFileCommand(File file) {
        String path = null;
        try {
            path = file.getCanonicalPath();
        } catch (IOException err) {
            path = file.getAbsolutePath();
        }
        
        String[] command = new String[] { 
                "osascript", 
                "-e", "set unixPath to \"" + path + "\"",
                "-e", "set hfsPath to POSIX file unixPath",
                "-e", "tell application \"Finder\"", 
                "-e",    "activate", 
                "-e",    "select hfsPath",
                "-e", "end tell" 
        };
        
        return command;
    }
    
    private static Process exec(String... commands) throws LaunchException {
        ProcessBuilder pb = new ProcessBuilder(commands);
        try {
            return pb.start();
        } catch (IOException e) {
            throw new LaunchException(e, commands);
        }
    }
    
    /**
     * Logs the specified exception, and displays the specified user message
     * if the current thread is the UI thread.
     */
    private static void logException(final String userMessage, final String title, Exception ex) {
        // Report exception to logger.
        LOG.error(userMessage, ex);
        
        // Display user message
        SwingUtils.invokeNowOrLater( new Runnable() {
            @Override
            public void run() {
                FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(),
                        userMessage, title, JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
    
    public static class LaunchException extends IOException {
        
        private final String[] command;

        /**
         * @param cause the exception that occurred during execution of command
         * @param command the executed command
         */
        public LaunchException(IOException cause, String... command) {
            this.command = command;
            
            initCause(cause);
        }

        /**
         * @param command the executed command.
         */
        public LaunchException(String... command) {
            this.command = command;
        }

        public String[] getCommand() {
            return command;
        }
    }
    
}
