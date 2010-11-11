package org.limewire.ui.swing.downloads.table.renderer;

import org.limewire.ui.swing.downloads.table.DownloadActionHandler;

/**
 * Defines a API for a factory to create a message renderer/editor component
 * for the downloads table.
 */
public interface DownloadMessageRendererEditorFactory {

    DownloadMessageRendererEditor create(DownloadActionHandler actionHandler);
}
