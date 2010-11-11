package org.limewire.ui.swing.properties;

import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;

/**
 * Creates a FileInfo Dialog and displays it.
 */
public interface FileInfoDialogFactory {

    public FileInfoDialog createFileInfoDialog(PropertiableFile propertiableFile, FileInfoType type);
}
