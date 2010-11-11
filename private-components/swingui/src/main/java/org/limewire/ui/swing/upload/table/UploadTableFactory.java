package org.limewire.ui.swing.upload.table;

import org.limewire.ui.swing.upload.UploadMediator;

/**
 * Factory for creating an UploadTable.
 */
public interface UploadTableFactory {

    /**
     * Creates an UploadTable using the specified mediator.
     */
    UploadTable create(UploadMediator uploadMediator);
}
