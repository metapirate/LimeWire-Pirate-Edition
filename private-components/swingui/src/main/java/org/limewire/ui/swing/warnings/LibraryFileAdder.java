package org.limewire.ui.swing.warnings;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorTable;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Clears any filters that may be on the library, then handles logic around
 * looping through a list of files and calling addFile or addFoler as necessary
 * on the provided LocalFileList.
 */
class LibraryFileAdder {

    private final Provider<LibraryMediator> libraryMediator;
    private final Provider<LibraryNavigatorTable> libraryNavigatorTable;
    private final CategoryManager categoryManager;

    @Inject
    public LibraryFileAdder(Provider<LibraryMediator> libraryMediator, Provider<LibraryNavigatorTable> libraryNavigatorTable,
            CategoryManager categoryManager) {
        this.libraryMediator = libraryMediator;
        this.libraryNavigatorTable = libraryNavigatorTable;
        this.categoryManager = categoryManager;
    }
    
    /**
     * Adds all files to the given list. If a file in files is a directory, then
     * the FileFilter is applied to any files within that directory to see if
     * they should be added also.
     */
    void addFilesInner(final LocalFileList fileList, final List<File> files, final FileFilter fileFilter) {
        //only clear the filters if the library has been initialized
        if(libraryMediator.get().isInitialized()) {
            //only clear the filters if we are adding files to the same list that is being shown
            LibraryNavItem libraryNavItem = libraryNavigatorTable.get().getSelectedItem();
            if(libraryNavItem != null && libraryNavItem.getLocalFileList() == fileList && shouldClearFilter(files)) {
                libraryMediator.get().clearFilters();
            }
        }
        
        List<File> rejectedFiles = new ArrayList<File>();
        for(File file : files) {
            if(fileList.isDirectoryAllowed(file)) {
                // add as directory if it's a directory
                fileList.addFolder(file, fileFilter);
            } else if(fileList.isFileAllowed(file)) {
                // add as file if it's a file.
                fileList.addFile(file);
            } else {
                // otherwise it was rejected!
            	rejectedFiles.add(file);
            }
        }
        
        // if anything was rejected notify user
        if(rejectedFiles.size() > 0) {
            FocusJOptionPane.showMessageDialog(null, getRejectedMessage(rejectedFiles), 
                    I18n.trn("File/Folder not added", "File(s)/Folder(s) not added", rejectedFiles.size()), JOptionPane.INFORMATION_MESSAGE);
        }     
    }
    
    /**
     * Returns true if this list of files contains directories, one or more files 
     * that are not part of the selected category.
     */
    private boolean shouldClearFilter(List<File> files) {
        Category category = libraryMediator.get().getComponent().getSelectedCategory();
        boolean shouldClear = false;
        for(File file : files) {
            if(file.isDirectory()) {
                shouldClear = true;
                break;
            }
            
            Category fileCategory = categoryManager.getCategoryForFile(file);
            if(fileCategory != category) {
                shouldClear = true;
                break;
            }
        }
        return shouldClear;
    }
    
    /**
     * Returns message to show when one or more files/folders were
     * rejected.
     */
    private String getRejectedMessage(List<File> rejectedFiles) {
        // display name if it was only 1 folder/file
        if(rejectedFiles.size() == 1) {
            return I18n.tr("{0} could not be added to the Library.", rejectedFiles.get(0).getName());
        } else {
            // display the number of files/folders that were rejected.
            int folderCount = 0;
            int fileCount = 0;
            for(File file : rejectedFiles) {
                if(file.isDirectory())
                    folderCount += 1;
                else
                    fileCount += 1;
            }
            if(folderCount > 0 && fileCount > 0)
                return I18n.tr("{0} folders and {1} files could not be added to the Library.", folderCount, fileCount);
            else if(folderCount > 0)
                return I18n.tr("{0} folders could not be added to the Library.", folderCount);
            else 
                return I18n.tr("{0} files could not be added to the Library.", fileCount);
             
        }
    }
}
