package org.limewire.ui.swing.upload.table;

import java.util.List;

import org.limewire.core.api.upload.UploadItem;

/**
 * Defines a factory for creating instances of UploadPopupMenu.
 */
public interface UploadPopupMenuFactory {

    UploadPopupMenu create(UploadTable table, List<UploadItem> uploadItems);
}
