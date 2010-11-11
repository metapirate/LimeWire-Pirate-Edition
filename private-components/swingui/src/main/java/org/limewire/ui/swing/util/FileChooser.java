package org.limewire.ui.swing.util;

import java.awt.Component;
import java.awt.FileDialog;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import org.limewire.i18n.I18nMarker;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;

/**
 * This is a utility class that displays a file chooser dialog to the user. 
 */
public final class FileChooser {
    
    private FileChooser() {}
    
    /**
     * Returns the last directory that was used in a FileChooser.
     * <p>
     * If no last directory can be found, the users home directory is returned.
     * If that cannot be found the current directory is returned.
     */
    public static File getLastInputDirectory() {
        File dir = SwingUiSettings.LAST_FILECHOOSER_DIRECTORY.get();
        if(dir == null || dir.getPath().equals("") || !dir.exists() || !dir.isDirectory()) {
            return getDefaultLastFileChooserDir();
        }
        else { 
            return dir;
        }
    }
    
    /**
     * Returns the default directory for the file chooser.
     * Defaults to the users home directory if it exists,
     * otherwise the current directory is used.
     * <p>
     * Logic is currently duplicated from ApplicationSettings getDefaultLastFileChooserDir.
     */
    private static File getDefaultLastFileChooserDir() {
        File defaultDirectory = CommonUtils.getUserHomeDir();
        if(defaultDirectory == null || !defaultDirectory.exists()) {
            defaultDirectory = CommonUtils.getCurrentDirectory();
        }
        return defaultDirectory;
    }
   
    public static File getInputDirectory(Component parent) {
        return getInputDirectory(parent, 
                I18nMarker.marktr("Select Folder"), 
                I18nMarker.marktr("Select"),
                null,
                null);
    }   

    /**
     * Same as <tt>getInputFile</tt> that takes no arguments, except this
     * allows the caller to specify the parent component of the chooser as well
     * as other options.
     * 
     * @param parent the <tt>Component</tt> that should be the dialog's parent
     * @param directory the directory to open the dialog to
     * 
     * @return the selected <tt>File</tt> instance, or <tt>null</tt> if a
     *         file was not selected correctly
     */
    public static File getInputDirectory(Component parent, 
                                         File directory) {
        return getInputDirectory(parent, 
                                 I18nMarker.marktr("Select Folder"), 
                                 I18nMarker.marktr("Select"),
                                 directory);
    }
    
    /**
     * Same as <tt>getInputFile</tt> that takes no arguments, except this
     * allows the caller to specify the parent component of the chooser as well
     * as other options.
     * 
     * @param parent the <tt>Component</tt> that should be the dialog's parent
     * @param titleKey the key for the locale-specific string to use for the
     *        file dialog title
     * @param approveKey the key for the locale-specific string to use for the
     *        approve button text
     * @param directory the directory to open the dialog to
     * 
     * @return the selected <tt>File</tt> instance, or <tt>null</tt> if a
     *         file was not selected correctly
     */
    public static File getInputDirectory(Component parent, String titleKey,
                                         String approveKey, File directory) {
        return getInputDirectory(parent, 
                                 titleKey, 
                                 approveKey,
                                 directory,
                                 null);
    }
    
    /**
     * Same as <tt>getInputFile</tt> that takes no arguments, except this
     * allows the caller to specify the parent component of the chooser as well
     * as other options.
     * 
     * @param parent the <tt>Component</tt> that should be the dialog's parent
     * @param titleKey the key for the locale-specific string to use for the
     *        file dialog title
     * @param approveKey the key for the locale-specific string to use for the
     *        approve button text
     * @param directory the directory to open the dialog to
     * @param filter the <tt>FileFilter</tt> instance for customizing the
     *        files that are displayed -- if this is null, no filter is used
     * 
     * @return the selected <tt>File</tt> instance, or <tt>null</tt> if a
     *         file was not selected correctly
     */
    public static File getInputDirectory(Component parent, String titleKey,
            String approveKey, File directory, FileFilter filter) {
        
        List<File> dirs = getInput(parent, titleKey, approveKey, directory,
                JFileChooser.DIRECTORIES_ONLY, JFileChooser.APPROVE_OPTION,
                false, filter);
        
        assert (dirs == null || dirs.size() <= 1) 
            : "selected more than one folder: " + dirs;
            
        if (dirs != null && dirs.size() == 1) {
            return dirs.get(0);
        } else {
            return null;
        }
    }
    
    /**
     * Same as <tt>getInputFile</tt> that takes no arguments, except this
     * allows the caller to specify the parent component of the chooser.
     * 
     * @param parent the <tt>Component</tt> that should be the dialog's parent
     * @param titleKey the key for the locale-specific string to use for the
     *        file dialog title
     * @param directory the directory to open the dialog to
     * @param filter the <tt>FileFilter</tt> instance for customizing the
     *        files that are displayed -- if this is null, no filter is used
     * 
     * @return the selected <tt>File</tt> instance, or <tt>null</tt> if a
     *         file was not selected correctly
     */
    public static File getInputFile(Component parent, String titleKey, String approveKey,
                                    File directory, FileFilter filter) {
        List<File> files = getInput(parent, titleKey, approveKey, directory,
                JFileChooser.FILES_ONLY, JFileChooser.APPROVE_OPTION, false,
                filter);
        
        assert (files == null || files.size() <= 1) 
        : "selected more than one directory: " + files;

        if (files != null && files.size() == 1) {
            return files.get(0);
        } else {
            return null;
        }
    }
    
    /**
     * The implementation that the other methods delegate to. This provides the
     * caller with all available options for customizing the
     * <tt>JFileChooser</tt> instance. If a <tt>FileDialog</tt> is displayed
     * instead of a <tt>JFileChooser</tt> (on OS X, for example), most or all
     * of these options have no effect.
     * 
     * @param parent the <tt>Component</tt> that should be the dialog's parent
     * @param titleKey the key for the locale-specific string to use for the
     *        file dialog title
     * @param approveKey the key for the locale-specific string to use for the
     *        approve button text
     * @param directory the directory to open the dialog to
     * @param mode the "mode" to open the <tt>JFileChooser</tt> in from the
     *        <tt>JFileChooser</tt> class, such as
     *        <tt>JFileChooser.DIRECTORIES_ONLY</tt>
     * @param option the option to look for in the return code, such as
     *        <tt>JFileChooser.APPROVE_OPTION</tt>
     * @param allowMultiSelect true if the chooser allows multiple files to be
     *        chosen
     * @param filter the <tt>FileFilter</tt> instance for customizing the
     *        files that are displayed -- if this is null, no filter is used
     * 
     * @return the selected <tt>File</tt> instance, or <tt>null</tt> if a
     *         file was not selected correctly
     */
    public static List<File> getInput(Component parent, String titleKey,
                                String approveKey,
                                File directory,
                                int mode,
                                int option,
                                boolean allowMultiSelect,
                                final FileFilter filter) {
            if(!OSUtils.isMacOSX()) {
                
                if(OSUtils.isWindows() && mode == JFileChooser.DIRECTORIES_ONLY && !allowMultiSelect) {
                   
                    // attempt to get the folder using the native widget, if jna fails,
                    // fallback to the Swing FileChooser
                    try {
                        String oldPath = directory == null ? null : directory.getAbsolutePath();
                        WindowsFolderChooser folder = new WindowsFolderChooser(parent, titleKey, false, true, oldPath);
                        String path = folder.showWidget();
                        if(path != null && path.length() > 0) {
                            File file = new File(path);
                            setLastInputDirectory(file);
                            return Collections.singletonList(file);
                        } else {
                            return null;
                        }
                    } catch(UnsatisfiedLinkError ule) {
                        return getFileChooser(parent, titleKey, approveKey, directory, mode, option, allowMultiSelect, filter);
                    }
                } else {
                    return getFileChooser(parent, titleKey, approveKey, directory, mode, option, allowMultiSelect, filter);
                }
            } else {
                // Okay, we're on Mac OS-X...  Let's open up a native file dialog so that we can get the OS-X
                // navigation features and allow multiple selections of files and directories as well...
                boolean canChooseFiles = (mode == JFileChooser.FILES_ONLY || mode == JFileChooser.FILES_AND_DIRECTORIES);
                boolean canChooseDirectories = (mode == JFileChooser.DIRECTORIES_ONLY || mode == JFileChooser.FILES_AND_DIRECTORIES);
                List<File> selectedFiles = MacOSXUtils.openNativeFileDialog(I18n.tr(titleKey), directory, canChooseFiles, canChooseDirectories, allowMultiSelect, filter);
                if ( selectedFiles == null) {
                    return null;
                } else {                   
                    // Users can only select multiple files that are in the same directory.  So, taking the directory 
                    // from the first file in the list is okay, because all the files in the list should be from that same directory...
                    // If it's a list of directories, then we'll take the parent directory rather than the first directory in the list.
                    if(selectedFiles.size() > 0) {
                        if (selectedFiles.get(0).isFile()) {
                            setLastInputDirectory(selectedFiles.get(0));
                        } else {
                            if (selectedFiles.get(0).getParent() != null)
                                setLastInputDirectory(new File(selectedFiles.get(0).getParent()));
                            else
                                setLastInputDirectory(selectedFiles.get(0));
                        }
                    } 

                    return selectedFiles;                    
                }
            }       
    }
    
    /**
     * Uses the Swing FileChooser to return a List of Files.
     */
    private static List<File> getFileChooser(Component parent, String titleKey, String approveKey, File directory,
            int mode, int option, boolean allowMultiSelect, final FileFilter filter) {
        JFileChooser fileChooser = getDirectoryChooser(titleKey, approveKey, directory, mode, filter, false);
        fileChooser.setMultiSelectionEnabled(allowMultiSelect);
        boolean dispose = false;
        if(parent == null) {
            dispose = true;
            parent = FocusJOptionPane.createFocusComponent();
        }
        try {
            if(fileChooser.showOpenDialog(parent) != option)
                return null;
        } catch(NullPointerException npe) {
            // ignore NPE.  can't do anything with it ...
            return null;
        } finally {
            if(dispose)
                ((JFrame)parent).dispose();
        }
        
        if(allowMultiSelect) {
            File[] chosen = fileChooser.getSelectedFiles();
            if(chosen.length > 0)
                setLastInputDirectory(chosen[0]);
            return Arrays.asList(chosen);
        } else {
            File chosen = fileChooser.getSelectedFile();
            setLastInputDirectory(chosen);
            return Collections.singletonList(chosen);
        }
    }
    
    /** Sets the last directory that was used for the FileChooser. */
    private static void setLastInputDirectory(File file) {
        if(file != null) {
            if(!file.exists() || !file.isDirectory())
                file = file.getParentFile();
            if(file != null) {
                if(file.exists() && file.isDirectory())
                    SwingUiSettings.LAST_FILECHOOSER_DIRECTORY.set(file);
            }
        }
    }
    
    /**
     * Returns a new <tt>JFileChooser</tt> instance for selecting directories
     * and with internationalized strings for the caption and the selection
     * button.
     *
     * @param titleKey dialog title
     * @param approveKey can be <code>null</code>
     * @param directory can be <code>null</code>
     * @param mode file selection mode
     * @param filter can be <code>null</code>
     * @param promptToOverwrite true if Save dialog prompts user to overwrite
     * @return a new <tt>JFileChooser</tt> instance for selecting directories.
     */
    private static JFileChooser getDirectoryChooser(String titleKey,
            String approveKey, File directory, int mode, FileFilter filter, 
            boolean promptToOverwrite) {
        
        JFileChooser chooser = null;
        if (directory == null)
            directory = getLastInputDirectory();
        

        if(directory == null) {
            chooser = createFileChooser(null, promptToOverwrite);
        } else {
            try {
                chooser = createFileChooser(directory, promptToOverwrite);
            } catch (NullPointerException e) {
                // Workaround for JRE bug 4711700. A NullPointer is thrown
                // sometimes on the first construction under XP look and feel,
                // but construction succeeds on successive attempts.
                try {
                    chooser = createFileChooser(directory, promptToOverwrite);
                } catch (NullPointerException npe) {
                    // ok, now we use the metal file chooser, takes a long time to load
                    // but the user can still use the program
                    UIManager.getDefaults().put("FileChooserUI", "javax.swing.plaf.metal.MetalFileChooserUI");
                    chooser = createFileChooser(directory, promptToOverwrite);
                }
            } catch (ArrayIndexOutOfBoundsException ie) {
                // workaround for Windows XP, not sure if second try succeeds
                // then
                chooser = createFileChooser(directory, promptToOverwrite);
            } catch (RuntimeException re) {
                // see: LWC-2690
                // happens only on windows, try again and if it still happening
                // us Metal L&F file chooser
                if (re.getCause() instanceof IOException && OSUtils.isWindows()) {
                    // but construction succeeds on successive attempts.
                    try {
                        chooser = createFileChooser(directory, promptToOverwrite);
                    } catch (RuntimeException r) {
                        // ok, now we use the metal file chooser, takes a long time to load
                        // but the user can still use the program
                        UIManager.getDefaults().put("FileChooserUI", "javax.swing.plaf.metal.MetalFileChooserUI");
                        chooser = createFileChooser(directory, promptToOverwrite);
                    }
                } else {
                    // rethrow if other OS or exception type
                    throw re;
                }
            }
        }
        if (filter != null) {
            chooser.setFileFilter(filter);
        } else {
            if (mode == JFileChooser.DIRECTORIES_ONLY) {
                chooser.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return true;
                    }
                    @Override
                    public String getDescription() {
                        return I18n.tr("All Folders");
                    }
                });
            }
        }
        chooser.setFileSelectionMode(mode);
        String title = I18n.tr(titleKey);
        chooser.setDialogTitle(title);

        if (approveKey != null) {
            String approveButtonText = I18n.tr(approveKey);
            chooser.setApproveButtonText(approveButtonText);
        }
        return chooser;
    }
    
    /**
     * Creates a new file chooser with the specified current directory and
     * <code>promptToOverwrite</code> behavior.
     */
    private static JFileChooser createFileChooser(File currentDirectory, boolean promptToOverwrite) {
        LimeFileChooser fileChooser = new LimeFileChooser(currentDirectory);
        fileChooser.setPromptToOverwrite(promptToOverwrite);
        return fileChooser;
    }

    /**
     * Opens a dialog asking the user to choose a file which is used for
     * saving to.  If an existing file is selected, the user is automatically
     * prompted to overwrite the file or cancel the selection.
     * 
     * @param parent the parent component the dialog is centered on
     * @param titleKey the key for the locale-specific string to use for the
     *        file dialog title
     * @param suggestedFile the suggested file for saving
     * @return the file or <code>null</code> when the user cancelled the
     *         dialog
     */
    public static File getSaveAsFile(Component parent, String titleKey, File suggestedFile) {
        return getSaveAsFile(parent, titleKey, suggestedFile, null);
    }
    
    /**
     * Opens a dialog asking the user to choose a file which is used for
     * saving to.  If an existing file is selected, the user is automatically
     * prompted to overwrite the file or cancel the selection.
     * 
     * @param parent the parent component the dialog is centered on
     * @param titleKey the key for the locale-specific string to use for the
     *        file dialog title
     * @param suggestedFile the suggested file for saving
     * @param the filter to use for what's shown.
     * @return the file or <code>null</code> when the user cancelled the
     *         dialog
     */
    public static File getSaveAsFile(Component parent, String titleKey,
                                     File suggestedFile, final FileFilter filter) {
        if (OSUtils.isMacOSX()) {
            FileDialog dialog = new FileDialog(GuiUtils.getParentFrame(parent),
                                               I18n.tr(titleKey),
                                               FileDialog.SAVE);
            dialog.setDirectory(suggestedFile.getParent());
            dialog.setFile(suggestedFile.getName()); 
            if (filter != null) {
                FilenameFilter f = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return filter.accept(new File(dir, name));
                    }
                };
                dialog.setFilenameFilter(f);
            }

            dialog.setVisible(true);
            String dir = dialog.getDirectory();
            if(dir != null) {
                setLastInputDirectory(new File(dir));
            }
            String file = dialog.getFile();
            if ((dir != null) && (file != null)) {
                File f = new File(dir, file);
                if ((filter != null) && !filter.accept(f)) {
                    return null;
                } else {
                    return f;
                }
            } else {
                return null;
            }
            
        } else {
            JFileChooser chooser = getDirectoryChooser(titleKey, null, null, 
                    JFileChooser.FILES_ONLY, filter, true);
            chooser.setSelectedFile(suggestedFile);
            int ret = chooser.showSaveDialog(parent);
            File file = chooser.getSelectedFile();
            setLastInputDirectory(file);
            return (ret != JFileChooser.APPROVE_OPTION) ? null : file;
        }
    }

    /**
     * An extension of JFileChooser that implements an option for file 
     * validation.  When the Save dialog is displayed, the chooser may prompt 
     * the user to overwrite an existing file.
     */
    private static class LimeFileChooser extends JFileChooser {
        
        private boolean promptToOverwrite = false;
        
        /**
         * Constructs a LimeFileChooser using the specified current directory.
         */
        public LimeFileChooser(File currentDirectory) {
            super(currentDirectory);
        }

        /**
         * Overrides the superclass method to validate the selected file before
         * approving the selection.
         */
        @Override
        public void approveSelection() {
            // Validate selection based on dialog type.
            switch (getDialogType()) {
            case SAVE_DIALOG:
                if (promptToOverwrite) {
                    File selectedFile = getSelectedFile();
                    if ((selectedFile != null) && selectedFile.exists()) {
                        // Prompt user to overwrite existing file.
                        int answer = FocusJOptionPane.showConfirmDialog(this,
                                selectedFile.getPath() + "\n" +
                                I18n.tr("File already exists.  Do you want to replace it?"), 
                                getDialogTitle(), JOptionPane.YES_NO_OPTION, 
                                JOptionPane.WARNING_MESSAGE);

                        // Exit if answer is not yes.
                        if (answer != JOptionPane.YES_OPTION) {
                            return;
                        }
                    }
                }
                
            default:
                break;
            }
            
            // Call superclass method to close dialog and return value.
            super.approveSelection();
        }
        
        /**
         * Sets an indicator that determines whether the Save dialog prompts
         * the user to overwrite an existing file.
         */
        public void setPromptToOverwrite(boolean promptToOverwrite) {
            this.promptToOverwrite = promptToOverwrite;
        }
    }
}
