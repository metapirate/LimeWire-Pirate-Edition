package org.limewire.ui.swing.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import javax.swing.JOptionPane;

import org.limewire.core.settings.SharingSettings;
import org.limewire.io.IOUtils;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;


/** Handles prompting the user to enter a valid save directory. */
public final class SaveDirectoryHandler {    

    private SaveDirectoryHandler() {} 

    /**
     * Ensures that the current save directory is valid,
     * prompting for a new one if it isn't.
     */
    public static void validateSaveDirectoryAndPromptForNewOne() {    
        File saveDir = SharingSettings.getSaveDirectory();
        if(!isDirectoryValid(saveDir))
            promptAndSetNewSaveDirectory();
    }
    
    public static boolean isDirectoryValid(File dir){
        return isSaveDirectoryValid(dir)  && showVistaWarningIfNeeded(dir);
    }


    /**
     * Constructs a new window that prompts the user to enter a valid save
     * directory.
     *
     * This doesn't return until the user has chosen a valid directory.
     */
    private static void promptAndSetNewSaveDirectory() {
        File dir = null;
        while(!isSaveDirectoryValid(dir) || !showVistaWarningIfNeeded(dir)) {
            FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), I18n.tr("Your save folder is not valid. It may have been deleted, you may not have permissions to write to it, or there may be another problem. Please choose a different folder."),
                    I18n.tr("Invalid Folder"), JOptionPane.WARNING_MESSAGE);
            
            dir = showChooser();
            if(dir == null)
                continue;
            FileUtils.setWriteable(dir);
        }
    }
    
    

    /**
     * Shows the chooser & sets the save directory setting, adding the save
     * directory as shared, also.
     *
     * @return the selected <tt>File</tt>, or <tt>null</tt> if there were
     *  any problems
     */
    private static File showChooser() { 
        File dir = FileChooser.getInputDirectory(GuiUtils.getMainFrame());
        if(dir != null) {
            try {
                // updates Incomplete directory etc... 
                SharingSettings.setSaveDirectory(dir);
                //SharingSettings.DIRECTORIES_TO_SHARE.add(dir);
                return dir;
            } catch(IOException ignored) {}
        }
        return null;
    }
    
    /**
     * Utility method for checking whether or not the save directory is valid.
     * 
     * @param saveDir the save directory to check for validity
     * @return <tt>true</tt> if the save directory is valid, otherwise 
     *  <tt>false</tt>
     */
    private static boolean isSaveDirectoryValid(File saveDir) {
        if(saveDir == null || saveDir.isFile()) 
            return false;

        if(!saveDir.exists())
            saveDir.mkdirs();
        
        if(!saveDir.isDirectory())
            return false;
        
        FileUtils.setWriteable(saveDir);
        
        Random generator = new Random();
        File testFile = null;
        for(int i = 0; i < 10 && testFile == null; i++) {
            StringBuilder name = new StringBuilder();
            for(int j = 0; j < 8; j++) {
                name.append((char)('a' + generator.nextInt('z'-'a')));
            }
            name.append(".tmp");
            
            testFile = new File(saveDir, name.toString());
            if (testFile.exists()) {
                testFile = null; // try again!
            }
        }
        
        if (testFile == null) {
            return false;
        }
        
        RandomAccessFile testRAFile = null;
        try {
            testRAFile = new RandomAccessFile(testFile, "rw");         
            // Try to write something just to make extra sure we're OK.
            testRAFile.write(7);
            testRAFile.close();
        } catch (FileNotFoundException e) {
            // If we could not open the file, then we can't write to that 
            // directory.
            return false;
        } catch(IOException e) {
            // The directory is invalid if there was an error writing to it.
            return false;
        } finally {
            // Delete our test file.
            testFile.delete();
            IOUtils.close(testRAFile);
        }
        
        return FileUtils.canWrite(saveDir);
    }
    
    private static boolean isGoodVistaDirectory(File f) {
        if (!OSUtils.isWindowsVista())
            return true;
        try {
            return FileUtils.isReallyInParentPath(CommonUtils.getUserHomeDir(), f);
        } catch (IOException iox) {
            return true; // probably bad, but not vista-specific
        }
    }
    
    /**
     * @param f the directory the user wants to save to
     * @return true if its ok to use that directory
     */
    private static boolean showVistaWarningIfNeeded(File f) {
        if (isGoodVistaDirectory(f))
            return true;
        
        if(SwingUiSettings.VISTA_WARN_DIRECTORIES.contains(f)) {
            return true;
        }
        
        int ret = FocusJOptionPane
                .showOptionDialog(
                        GuiUtils.getMainFrame(),
                        I18n.tr("Saving downloads to {0} may not function correctly.\nTo be sure downloads are saved properly you should save them to a sub-folder of\n{1}.\nWould you like to choose another location?",
                                f, CommonUtils.getUserHomeDir()),
                        I18n.tr("Folder Warning"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        null,
                        JOptionPane.YES_OPTION);
        
        if(ret == JOptionPane.NO_OPTION) {
            SwingUiSettings.VISTA_WARN_DIRECTORIES.add(f);
        }
        
        return ret == JOptionPane.NO_OPTION;
    }
}

