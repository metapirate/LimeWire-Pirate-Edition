package org.limewire.ui.swing.downloads.table;

import org.limewire.core.api.download.DownloadItem;

import ca.odell.glazedlists.EventList;

public interface DownloadTableFactory {
    DownloadTable create(EventList<DownloadItem> downloadItems);
}
