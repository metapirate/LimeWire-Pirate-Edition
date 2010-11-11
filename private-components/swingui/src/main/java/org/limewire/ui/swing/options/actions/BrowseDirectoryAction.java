package org.limewire.ui.swing.options.actions;

import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.limewire.i18n.I18nMarker;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.LabelTextField;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

public class BrowseDirectoryAction extends AbstractAction {

    private enum FolderErrors {
        NOT_A_FOLDER (I18nMarker.marktr("{0} is not a folder")),
        CANNOT_WRITE (I18nMarker.marktr("Cannot write to {0}")),
        CANT_FIND (I18nMarker.marktr("Cannot find {0}")), 
        SUCCESS ("");
        
        private String errorMessage;
        
        FolderErrors(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    };
    
    private Container parent;

    private LabelTextField currentDirectoryTextField;

    public BrowseDirectoryAction(Container parent, LabelTextField currentDirectoryTextField) {
        this.parent = parent;
        this.currentDirectoryTextField = currentDirectoryTextField;

        putValue(Action.NAME, I18n.tr("Browse..."));
        putValue(Action.SHORT_DESCRIPTION, I18n.tr("Choose a different Save Location"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String oldDirectory = currentDirectoryTextField.getText();

        Window window = FocusJOptionPane.getWindowForComponent(parent);
        File directory = FileChooser.getInputDirectory(window, new File(oldDirectory));

        // no directory was selected
        if (directory == null) {
            return;
        }

        FolderErrors error = isSaveDirectoryValid(directory);
        if (error == FolderErrors.SUCCESS) {
            try {
                String newDirectory = directory.getCanonicalPath();
                currentDirectoryTextField.setText(newDirectory);
            } catch (IOException ioe) {
                FocusJOptionPane.showMessageDialog(window, 
                        I18n.tr(error.getErrorMessage(), directory),
                        I18n.tr("Save Folder Error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            FocusJOptionPane.showMessageDialog(window, 
                    I18n.tr(error.getErrorMessage(), directory),
                    I18n.tr("Save Folder Error"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Utility method for checking whether or not the save directory is valid.
     * 
     * @param saveDir the save directory to check for validity
     * @return <tt>true</tt> if the save directory is valid, otherwise
     *         <tt>false</tt>
     */
    private static FolderErrors isSaveDirectoryValid(File saveDir) {
        if (saveDir == null) 
            return FolderErrors.CANT_FIND;
        if(saveDir.isFile())
            return FolderErrors.NOT_A_FOLDER;
        
        
        if (!saveDir.exists())
            saveDir.mkdirs();

        if (!saveDir.isDirectory())
            return FolderErrors.NOT_A_FOLDER;

        FileUtils.setWriteable(saveDir);

        Random generator = new Random();
        File testFile = null;
        for (int i = 0; i < 10 && testFile == null; i++) {
            StringBuilder name = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                name.append((char) ('a' + generator.nextInt('z' - 'a')));
            }
            name.append(".tmp");

            testFile = new File(saveDir, name.toString());
            if (testFile.exists()) {
                testFile = null; // try again!
            }
        }

        if (testFile == null) {
            return FolderErrors.CANNOT_WRITE;
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
            return FolderErrors.CANNOT_WRITE;
        } catch (IOException e) {
            // The directory is invalid if there was an error writing to it.
            return FolderErrors.CANNOT_WRITE;
        } finally {
            // Delete our test file.
            testFile.delete();
            try {
                if (testRAFile != null)
                    testRAFile.close();
            } catch (IOException ignored) {
            }
        }

        if(FileUtils.canWrite(saveDir))
            return FolderErrors.SUCCESS;
        else
            return FolderErrors.CANNOT_WRITE;
    }
}
