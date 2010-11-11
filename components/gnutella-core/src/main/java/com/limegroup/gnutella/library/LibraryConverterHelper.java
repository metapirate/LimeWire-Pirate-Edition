package com.limegroup.gnutella.library;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.settings.SharingSettings;
import org.limewire.util.FileUtils;

/**
 * Helper class used for library conversion.
 * 
 * Since crawling directories and adding files are done similarly for both a
 * 4.18 upgrade and a 5.0/5.1 upgrade this class is holding the common logic.
 */
class LibraryConverterHelper {
    static interface FileAdder {
        public void addFile(File file);
    }

    private final FileAdder fileAdder;
    private final CategoryManager categoryManager;

    LibraryConverterHelper(FileAdder fileAdder, CategoryManager categoryManager) {
        this.fileAdder = fileAdder;
        this.categoryManager = categoryManager;
    }

    /**
     * Calls the fileAdd method with files matching the given filters on all
     * save directories that should be brought over during the upgrade. Save
     * directories are not search recursively for files. They are only searched
     * 1 level deep.
     * 
     * Under the hood convertDirectory is called for each directory that will be
     * converted.
     * 
     * @param excludedFolders The folders to not include in the crawl
     * @param excludedFiles The files to exclude from calling addFile
     * @param convertedDirectories A list of already converted directories so we
     *        can stop converting the same directory twice.
     */
    @SuppressWarnings("deprecation")
    void convertSaveDirectories(List<File> excludedFolders, List<File> excludedFiles,
            Set<File> convertedDirectories) {
        convertDirectory(SharingSettings.getSaveDirectory(), null, excludedFolders, excludedFiles,
                convertedDirectories, false);
        convertDirectory(SharingSettings.getFileSettingForCategory(Category.AUDIO)
                .get(), null, excludedFolders, excludedFiles, convertedDirectories, false);
        convertDirectory(SharingSettings.getFileSettingForCategory(Category.VIDEO)
                .get(), null, excludedFolders, excludedFiles, convertedDirectories, false);
        convertDirectory(SharingSettings.getFileSettingForCategory(Category.IMAGE)
                .get(), null, excludedFolders, excludedFiles, convertedDirectories, false);
        convertDirectory(SharingSettings.getFileSettingForCategory(Category.OTHER)
                .get(), null, excludedFolders, excludedFiles, convertedDirectories, false);
        convertDirectory(SharingSettings.getFileSettingForCategory(Category.PROGRAM)
                .get(), null, excludedFolders, excludedFiles, convertedDirectories, false);
        convertDirectory(SharingSettings.getFileSettingForCategory(Category.DOCUMENT)
                .get(), null, excludedFolders, excludedFiles, convertedDirectories, false);
        convertDirectory(OldLibrarySettings.DIRECTORY_FOR_SAVING_LWS_FILES.get(), Collections.singletonList("mp3"),
                excludedFolders, excludedFiles, convertedDirectories, true);
    }

    /**
     * Calls the FileAdder.addFile method for each file matching the supplied
     * filter.
     * 
     * @param extensions The extensions to filter files against
     * @param excludedFolders The folders to not include in the crawl
     * @param excludedFiles The files to exclude from calling addFile
     * @param convertedDirectories A list of already converted directories so we
     *        can stop converting the same directory twice.
     * @param recurse whether or not to recursivley scan the directory
     */
    void convertDirectory(File directory, final List<String> extensions,
            final List<File> excludedFolders, final List<File> excludedFiles,
            final Set<File> convertedDirectories, final boolean recurse) {

        // If we already converted this directory, or it is suppsoed to be
        // excluded.
        if (!directory.exists() || convertedDirectories.contains(directory) || excludedFolders.contains(directory)) {
            return;
        }

        convertedDirectories.add(directory);

        File[] fileList = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return LibraryUtils.isFileManagable(file, categoryManager)
                        && (extensions == null || extensions.contains(FileUtils.getFileExtension(
                                file).toLowerCase(Locale.US))) && !excludedFiles.contains(file);
            }
        });

        if (fileList != null) {
            for (File file : fileList) {
                file = FileUtils.canonicalize(file);
                fileAdder.addFile(file);
            }
        }

        if (!LibraryUtils.isForcedShareDirectory(directory) && recurse) {
            File[] dirList = directory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File folder) {
                    return folder.isDirectory() && folder.canRead()
                            && !isIncompleteDirectory(folder)
                            && !LibraryUtils.isFolderBanned(folder)
                            && !excludedFolders.contains(folder);
                }
            });

            if (dirList != null) {
                for (File subdir : dirList) {
                    convertDirectory(subdir, extensions, excludedFolders, excludedFiles,
                            convertedDirectories, recurse);
                }
            }
        }
    }

    private static boolean isIncompleteDirectory(File folder) {
        return FileUtils.canonicalize(SharingSettings.INCOMPLETE_DIRECTORY.get()).equals(folder);
    }
}
