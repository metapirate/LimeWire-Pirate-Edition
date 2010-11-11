package org.limewire.ui.swing.util;

import java.awt.FileDialog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.filechooser.FileFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.service.ErrorService;
import org.limewire.ui.swing.components.LimeJFrame;
import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;

import foxtrot.Job;
import foxtrot.Worker;

/**
 * A collection of utility methods for OSX.
 * These methods should only be called if run from OSX,
 * otherwise ClassNotFoundErrors may occur.
 * 
 * Clients may use the method isNativeLibraryLoadedCorrectly() 
 * to check whether the native library loaded correctly.
 * If not, they may choose to disable certain user interface
 * features to reflect this state.
 * 
 * <p>
 * To determine if the Cocoa Foundation classes are present,
 * use the method CommonUtils.isCocoaFoundationAvailable().
 */
public class MacOSXUtils {
    
    /**
     * The application bundle identifier for the LimeWire application that is packed into its Info.plist config file.
     */
    public static final String LIMEWIRE_APPLICATION_BUNDLE_IDENTIFIER = "com.limegroup.gnutella";
    
    /**
     * The name of the app that launches.
     */
    private static final String APP_NAME = "LimeWire.app";

    private static boolean nativeLibraryLoadedCorrectly = false;
    
    private static final Log LOG = LogFactory.getLog(MacOSXUtils.class);
    
    static {
        if (OSUtils.isMacOSX()) {
            try {
                System.loadLibrary("MacOSXUtils");
                nativeLibraryLoadedCorrectly = true;
            } catch (UnsatisfiedLinkError err) {
                ErrorService.error(err, "java.library.path=" + System.getProperty("java.library.path") + "\n\n" + "trace dependencies=" + MacOSXUtils.traceLibraryDependencies("MacOSXUtils.jnilib"));
            }
        }
    }
    
    /**
     * This returns a boolean indicating whether an exception occurred when loading the native library.
     * @return true if the native library loaded without any errors.
     */
    public static boolean isNativeLibraryLoadedCorrectly() {
        return nativeLibraryLoadedCorrectly;
    }

    private MacOSXUtils() {}
    
    /**
    * If a given library is not loading for some users on OS-X, this method
    * can be used to trace what other libraries this library is dependent
    * on and whether those libraries are present on the user's system.
    */
    public static String traceLibraryDependencies(String libraryName) {       
        StringBuffer traceResultsBuffer = new StringBuffer("ls command output: ");
        String lsCommand = "ls " + System.getProperty("user.dir");
        traceResultsBuffer.append("(").append(lsCommand).append(") "); 
        traceResultsBuffer.append( getCommandOutput(lsCommand) );
        traceResultsBuffer.append( "\n" );

        String otoolCommand = "otool -L " + System.getProperty("user.dir") + "/" + libraryName;
        traceResultsBuffer.append("otool command output: ");
        traceResultsBuffer.append( getCommandOutput(otoolCommand) );
        traceResultsBuffer.append( "\n" );
               
        return traceResultsBuffer.toString();
    }
    
    /**
    * This method runs a system command and returns the command's output as a string.
    *
    */
    private static String getCommandOutput(String command) {
        StringBuffer outputBuffer = new StringBuffer("");

        try {
            // start the command running
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(command);
    
            // put a BufferedReader on the command output
            InputStream inputstream = process.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
    
            // read the command output   
            String line;
            while ((line = bufferedreader.readLine()) != null) {
                outputBuffer.append(line).append("\n");
            }
        
            // check for command failure
            try {
                if (process.waitFor() != 0) {
                    outputBuffer.append("exit value = ");
                    outputBuffer.append(process.exitValue());
                }
            }
            catch (InterruptedException e) {
            }
        } catch (IOException exc) {
        }
        
        return outputBuffer.toString();
    }

    
    /**
     * Modifies the loginwindow.plist file to either include or exclude
     * starting up LimeWire.
     */
    public static void setLoginStatus(boolean allow) {
        try {
            SetLoginStatusNative(allow);
        } catch(UnsatisfiedLinkError ule) {
            LOG.error("UnsatisfiedLinkError for MacOSXUtils", ule);
        }
    }
    
    /**
     * Gets the full user's name.
     */
    public static String getUserName() {
        try {
            return GetCurrentFullUserName();
        } catch(UnsatisfiedLinkError ule) {
            // No big deal, just return user name.
            LOG.error("UnsatisfiedLinkError for MacOSXUtils", ule);
            
            return CommonUtils.getUserName();
        }
    }
    
    /**
     * Retrieves the app directory & name.
     * If the user is not running from the bundled app as we named it,
     * defaults to /Applications/LimeWire/ as the directory of the app.
     */
    public static String getAppDir() {
        String appDir = "/Applications/LimeWire/";
        String path = CommonUtils.getCurrentDirectory().getPath();
        int app = path.indexOf("LimeWire.app");
        if(app != -1)
            appDir = path.substring(0, app);
        return appDir + APP_NAME;
    }

    /**
     * This sets LimeWire as the default handler for this file type.
     * @param fileType -- the file extension for the file. this will be used to look up the file type's UTI (universal type identifier)
     */
    public static void setLimewireAsDefaultFileTypeHandler(String fileType) {
        try {
            SetDefaultFileTypeHandler(fileType, LIMEWIRE_APPLICATION_BUNDLE_IDENTIFIER);
        } catch(UnsatisfiedLinkError ule) {
            LOG.error("UnsatisfiedLinkError for MacOSXUtils", ule); 
        }
    }

    /**
     * This sets LimeWire as the default handler for this URL scheme.
     * @param url scheme -- the designator for the protocol, e.g. magnet
     */
    public static void setLimewireAsDefaultURLSchemeHandler(String urlScheme) {
        try {
            SetDefaultURLSchemeHandler(urlScheme, LIMEWIRE_APPLICATION_BUNDLE_IDENTIFIER);
        } catch(UnsatisfiedLinkError ule) {
            LOG.error("UnsatisfiedLinkError for MacOSXUtils", ule); 
        }
    }

    /**
     * This checks whether LimeWire is the default handler for this file type.
     * @param fileType -- the file extension for the file. this will be used to look up the file type's UTI (universal type identifier)
     * @return true if LimeWire is the default handler for this file type
     */
    public static boolean isLimewireDefaultFileTypeHandler(String fileType) {
        try {
            return IsApplicationTheDefaultFileTypeHandler(fileType, LIMEWIRE_APPLICATION_BUNDLE_IDENTIFIER);
        } catch(UnsatisfiedLinkError ule) {
            LOG.error("UnsatisfiedLinkError for MacOSXUtils", ule); 
        }
        
        return true;
    }

    /**
     * This checks whether LimeWire is the default handler for this URL scheme.
     * @param urlScheme -- the designator for the protocol, e.g. magnet
     * @return true if LimeWire is the default handler for this URL scheme
     */
    public static boolean isLimewireDefaultURLSchemeHandler(String urlScheme) {
        try {
            return IsApplicationTheDefaultURLSchemeHandler(urlScheme, LIMEWIRE_APPLICATION_BUNDLE_IDENTIFIER);
        } catch(UnsatisfiedLinkError ule) {
            LOG.error("UnsatisfiedLinkError for MacOSXUtils", ule); 
        }
        
        return true;
    }
    
    /**
     * This checks whether any applications are registered as handlers for this fileType in the OS-X
     * launch services database.
     * 
     * @param fileType -- the file extension for the file. this will be used to look up the file type's UTI (universal type identifier)
     * @return true if any application is registered as a handler for this file type
     */
    public static boolean isFileTypeHandled(String fileType) {
        try {
            return IsFileTypeHandled(fileType);
        } catch(UnsatisfiedLinkError ule) {
            LOG.error("UnsatisfiedLinkError for MacOSXUtils", ule); 
        }
        
        return true;
    }

    /**
     * This checks whether any applications are registered as handlers for this URL scheme in the OS-X
     * launch services database.
     * 
     * @param urlScheme -- the designator for the protocol, e.g. magnet
     * @return true if any application is registered as a handler for this URL scheme
     */
    public static boolean isURLSchemeHandled(String urlScheme) {
        try {
            return IsURLSchemeHandled(urlScheme);
        } catch(UnsatisfiedLinkError ule) {
            LOG.error("UnsatisfiedLinkError for MacOSXUtils", ule); 
        }
        
        return true;
    }

    /**
     * This tries to change the file type handler for the given file type from LimeWire to another application.
     * Basically, it just changes the default handler application to the first application in the list
     * returned by launch services that isn't LimeWire. It might fail if no other handlers are registered for this file type.  
     * The list of handlers that are used internally in this method should not be shown to users as they are probably not understandable
     * by users.  For example LimeWire is represented by the application bundle identifier com.limegroup.gnutella.
     * 
     * @param fileType -- the file extension for the file. this will be used to look up the file type's UTI (universal type identifier)
     */
    public static void tryChangingDefaultFileTypeHandler(String fileType) {
        try {
            String[] handlers = GetAllHandlersForFileType(fileType);
            if (handlers != null) {
                for (String handler : handlers) {
                    if (!handler.equals(LIMEWIRE_APPLICATION_BUNDLE_IDENTIFIER)) {
                        SetDefaultFileTypeHandler(fileType, handler);
                        break;
                    }
                }
            }
        } catch(UnsatisfiedLinkError ule) {
            LOG.error("UnsatisfiedLinkError for MacOSXUtils", ule); 
        }
    }

    /**
     * This method returns true if there are any other applications on the user's system that have
     * registered themselves as handlers for the given file type.
     * 
     * @param fileType -- the file extension for the file. this will be used to look up the file type's UTI (universal type identifier)
     */

    public static boolean canChangeDefaultFileTypeHandler(String fileType) {
        try {
            String[] handlers = GetAllHandlersForFileType(fileType);
            if (handlers != null) {
                for (String handler : handlers) {
                    if (!handler.equals(LIMEWIRE_APPLICATION_BUNDLE_IDENTIFIER)) {
                        return true;
                    }
                }
            }
        } catch(UnsatisfiedLinkError ule) {
            LOG.error("UnsatisfiedLinkError for MacOSXUtils", ule); 
        }
        
        return false;
    }
    
    /**
     * This tries to change the URL scheme handler for the given file type from LimeWire to another application.
     * Basically, it just changes the default handler application to the first application in the list
     * returned by launch services that isn't LimeWire. It might fail if no other handlers are registered for this URL scheme.  
     * The list of handlers that are used internally in this method should not be shown to users as they are probably not understandable
     * by users.  For example LimeWire is represented by the application bundle identifier com.limegroup.gnutella.
     * 
     * @param urlScheme -- the designator for the protocol, e.g. magnet
     */
    public static void tryChangingDefaultURLSchemeHandler(String urlScheme) {
        try {
            String[] handlers = GetAllHandlersForURLScheme(urlScheme);
            if (handlers != null) {
                for (String handler : handlers) {
                    if (!handler.equals(LIMEWIRE_APPLICATION_BUNDLE_IDENTIFIER)) {
                        SetDefaultURLSchemeHandler(urlScheme, handler);
                        break;
                    }
                }
            }
        } catch(UnsatisfiedLinkError ule) {
            LOG.error("UnsatisfiedLinkError for MacOSXUtils", ule); 
        }
    }

    /**
     * This method returns true if there are any other applications on the user's system that have
     * registered themselves as handlers for the given URL scheme.
     * 
     * @param urlScheme -- the designator for the protocol, e.g. magnet
     */
    public static boolean canChangeDefaultURLSchemeHandler(String urlScheme) {
        try {
            String[] handlers = GetAllHandlersForURLScheme(urlScheme);
            if (handlers != null) {
                for (String handler : handlers) {
                    if (!handler.equals(LIMEWIRE_APPLICATION_BUNDLE_IDENTIFIER)) {
                        return true;
                    }
                }
            }
        } catch(UnsatisfiedLinkError ule) {
            LOG.error("UnsatisfiedLinkError for MacOSXUtils", ule); 
        }
        
        return false;
    }
    
    /**
     * This method opens up a native OS-X file dialog. These native dialogs are better than the FileDialog and JFileChooser
     * currently available in jdk6, because they have the native look and feel and navigation features of a FileDialog, but
     * they also allow for multiple file selections as JFileChoosers do.  If a native file dialog cannot be opened
     * however because the native library cannot be found, then this shows a Java based dialog instead.
     *
     * @param dialogTitle - the title to be shown in the dialog. this should already have been translated.
     * @param directory - the directory that the file dialog should open to
     * @param canChooseFiles - whether files can be selected
     * @param canChooseDirectories - whether directories can be selected
     * @param allowMultipleSelections - whether multiple files or directories can be selected
     * @return an array of file objects that were selected by the user or null if the user canceled the operation
     */
    public static List<File> openNativeFileDialog(final String dialogTitle, final File directory, final boolean canChooseFiles, 
                                                  final boolean canChooseDirectories, final boolean allowMultipleSelections,
                                                  final FileFilter filter) {
        try {
            final String directoryAbsolutePath = (directory != null) ? directory.getAbsolutePath() : null;

            String[] filePaths = (String[]) Worker.post(new Job()
            {
                @Override
                public Object run()
                {
                    String[] filePaths = OpenNativeFileDialog(dialogTitle, directoryAbsolutePath, canChooseFiles, canChooseDirectories, allowMultipleSelections); 
                    return filePaths;
                }
            });
            
            if (filePaths == null) {
                return null;
            } else {
                List<File> selectedFileList = new ArrayList<File>();
                for (String filePath : filePaths) {
                    File selectedFile = new File(filePath);
                    // since we couldn't pass the file filter over to the native dialog, 
                    // let's filter the files here if the filter is not null...
                    if (filter != null) {
                        if (filter.accept(selectedFile)) {
                            selectedFileList.add(selectedFile);
                        }
                    } else {
                        selectedFileList.add(selectedFile);                       
                    }
                }
                             
                return selectedFileList;
            }
        } catch(UnsatisfiedLinkError ule) {
            // If we can't open up a native file dialog, then let's open a Java dialog in the same way we did before we
            // started using native dialogs
            return openJavaFileDialog(dialogTitle, directory, canChooseFiles, canChooseDirectories, allowMultipleSelections, filter);
        }
    }

    /**
     * This opens up a Java file dialog that's been fine tuned for OS-X for selecting files or directories.
     * Java based file dialogs (as of JDK6) do not allow users to select multiple files or directories.
     * Clients should prefer to use the openNativeFileDialog() method rather than this one,
     * and for this reason this method is currently private.
     * This method is intended to serve only as a fallback if the native library for opening
     * native file dialogs cannot be loaded.
     * 
     * @param dialogTitle - the title to be shown in the dialog. this should already have been translated.
     * @param directory - the directory that the file dialog should open to
     * @param canChooseFiles - whether files can be selected
     * @param canChooseDirectories - whether directories can be selected
     * @param allowMultipleSelections - whether multiple files or directories can be selected. (This is ignored 
     *                                  when using a Java based dialog.)
     * @param filter - a file filter for disallowing users to select certain files
     * @return an array of file objects that were selected by the user or null if the user canceled the operation
     */
    private static List<File> openJavaFileDialog(String dialogTitle, File directory, boolean canChooseFiles, 
                                                boolean canChooseDirectories, boolean allowMultipleSelections,
                                                final FileFilter filter) {        
        FileDialog dialog;
        if(canChooseDirectories && !canChooseFiles) {
            dialog = MacUtils.getFolderDialog(null);
        } else {
            dialog = new FileDialog(new LimeJFrame(), "");
        }
        
        dialog.setTitle(dialogTitle);
        
        if(filter != null) {
            FilenameFilter f = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return filter.accept(new File(dir, name));
                }
            };
            dialog.setFilenameFilter(f);
        }
        
        dialog.setVisible(true);
        String dirStr = dialog.getDirectory();
        String fileStr = dialog.getFile();
        if((dirStr==null) || (fileStr==null))
            return null;

        // if the filter didn't work, pretend that the person picked
        // nothing
        File f = new File(dirStr, fileStr);
        if(filter != null && !filter.accept(f))
            return null;
        
        return Collections.singletonList(f);        
    }
    
    /**
     * Uses OS-X's launch services API to check whether any application has registered itself
     * as a handler for this file type. 
     */
    private static final native boolean IsFileTypeHandled(String fileType);

    /**
     * Uses OS-X's launch services API to check whether any application has registered itself
     * as a handler for this URL scheme. 
     */
    private static final native boolean IsURLSchemeHandled(String urlScheme);

    /**
     * Uses OS-X's launch services API to check whether the given application is the default handler for this
     * file type.
     */
    private static final native boolean IsApplicationTheDefaultFileTypeHandler(String fileType, String applicationBundleIdentifier);

    /**
     * Uses OS-X's launch services API to check whether the given application is the default handler for this
     * URL scheme.
     */
    private static final native boolean IsApplicationTheDefaultURLSchemeHandler(String urlScheme, String applicationBundleIdentifier);

    /**
     * Uses OS-X's launch services API to set the given application as the default handler for this file type.
     */
    private static final native int SetDefaultFileTypeHandler(String fileType, String applicationBundleIdentifier);

    /**
     * Uses OS-X's launch services API to set the given application as the default handler for this URL scheme.
     */
    private static final native int SetDefaultURLSchemeHandler(String urlScheme, String applicationBundleIdentifier);
    
    /**
     * Uses OS-X's launch services API to get all the handlers for this file type.
     */
    private static final native String[] GetAllHandlersForFileType(String fileType); 

    /**
     * Uses OS-X's launch services API to get all the handlers for this URL scheme.
     */
    private static final native String[] GetAllHandlersForURLScheme(String ulrScheme); 
    
    /**
     * Open a native file dialog for selecting files and folders.
     * Native dialogs have the advantage of preserving the look and feel of
     * the platform while still allowing for multiple file selections.
     */
    private static final native String[] OpenNativeFileDialog(String title, String directoryPath, boolean canChooseFiles, 
                                                              boolean canChooseDirectories, boolean allowMultipleSelections);

    /**
     * Gets the full user's name.
     */
    private static final native String GetCurrentFullUserName();
    
    /**
     * [Un]registers LimeWire from the startup items list.
     */
    private static final native void SetLoginStatusNative(boolean allow);
}