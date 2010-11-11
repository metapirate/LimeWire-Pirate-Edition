package com.limegroup.gnutella.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.StringArraySetting;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;


/**
 * Converts from old-style library settings (shared files)
 * to new-style library settings (managed files)
 */
@SuppressWarnings("deprecation")
class LibraryConverter {
    
    private final CategoryManager categoryManager;
    
    @Inject public LibraryConverter(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }
    
    boolean isOutOfDate() {
        return LibrarySettings.VERSION.get() == LibrarySettings.LibraryVersion.FOUR_X.name();
    }
    
    void convert(final LibraryFileData newData) {
        newData.revertToDefault();
        
        List<File> sharedFolders = new ArrayList<File>();
        List<File> excludedFolders = new ArrayList<File>();
        List<File> excludedFiles = new ArrayList<File>();
        List<String> extensions = new ArrayList<String>();
        
        OldLibraryData oldData = new OldLibraryData(); // load if necessary
        for(File folder : OldLibrarySettings.DIRECTORIES_TO_SHARE.get()) {
            if(!LibraryUtils.isSensitiveDirectory(folder) || oldData.SENSITIVE_DIRECTORIES_VALIDATED.contains(folder)) {
                folder = FileUtils.canonicalize(folder);
                sharedFolders.add(folder);
            }
        }
        
        excludedFolders.addAll(oldData.DIRECTORIES_NOT_TO_SHARE);
        
        for(File file : oldData.SPECIAL_FILES_TO_SHARE) {
            file = FileUtils.canonicalize(file);            
            if(addManagedFile(newData, file)) {
                newData.setFileInCollection(file, LibraryFileData.DEFAULT_SHARED_COLLECTION_ID, true);
            }
        }
        
        for(File file : oldData.FILES_NOT_TO_SHARE) {
            file = FileUtils.canonicalize(file);
            excludedFiles.add(file);
        }
        
        // Set the new managed extensions.
        extensions.addAll(Arrays.asList(OldLibrarySettings.getDefaultExtensions()));
        extensions.removeAll(Arrays.asList(StringArraySetting.decode(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.get())));
        extensions.addAll(Arrays.asList(StringArraySetting.decode(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.get())));        
//        newData.setManagedExtensions(extensions);
        
        Set<File> convertedDirectories = new HashSet<File>();
        // Here's the bulk of the conversion -- loop through, recursively, previously 
        // shared directories & mark all potential files as shareable.
        convertSharedDirectories(sharedFolders, excludedFolders, excludedFiles, extensions, convertedDirectories, newData);
        
        
        for(File file : oldData.SPECIAL_STORE_FILES) {
            file = FileUtils.canonicalize(file);
            addManagedFile(newData, file);
        }
        
        //add save directory contents to library
        LibraryConverterHelper helper = new LibraryConverterHelper(new LibraryConverterHelper.FileAdder() {
            @Override
            public void addFile(File file) {
                addManagedFile(newData, file);
            }
        }, categoryManager);
        helper.convertSaveDirectories(excludedFolders, excludedFiles, convertedDirectories);

        LibrarySettings.VERSION.set(LibrarySettings.LibraryVersion.FIVE_0_0.name());
        
        oldData.revertToDefault();
        OldLibrarySettings.DIRECTORIES_TO_SHARE.revertToDefault();
        OldLibrarySettings.DISABLE_SENSITIVE.revertToDefault();
        OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.revertToDefault();
        OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.revertToDefault();
        OldLibrarySettings.EXTENSIONS_TO_SHARE.revertToDefault();
    }

    /**
     * Checks to make sure that the file is not a document before adding file to managed file list.
     * Returns true if the files was not a document, false otherwise. 
     */
    private boolean addManagedFile(LibraryFileData newData, File file) {
        Category category = categoryManager.getCategoryForFile(file);
        if(category != Category.DOCUMENT && category != Category.PROGRAM) {
            newData.addManagedFile(file);
            return true;
        }
        return false;
    }
    
    private void convertSharedDirectories(List<File> sharedFolders, List<File> excludedFolders,
            List<File> excludedFiles, List<String> extensions, Set<File> convertedDirectories, final LibraryFileData data) {
        
        LibraryConverterHelper helper = new LibraryConverterHelper(new LibraryConverterHelper.FileAdder() {
           @Override
            public void addFile(File file) {
               if(addManagedFile(data, file)) {
                   data.setFileInCollection(file, LibraryFileData.DEFAULT_SHARED_COLLECTION_ID, true);
               }
            } 
        }, categoryManager);
        
        for (File directory : sharedFolders) {
            helper.convertDirectory(directory, extensions, excludedFolders, excludedFiles, convertedDirectories, true);
        }
    }
}
