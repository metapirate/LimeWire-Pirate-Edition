/**
 * 
 */
package org.limewire.ui.swing.downloads.table;

import org.limewire.core.api.download.DownloadItem;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.DefaultEventTableModel;



public class DownloadTableModel extends DefaultEventTableModel<DownloadItem> {
	
	private static final long serialVersionUID = 4079559883623594683L;
	private EventList<DownloadItem> downloadItems;

	public DownloadTableModel(EventList<DownloadItem> downloadItems) {
		super(downloadItems, new DownloadTableFormat());
		this.downloadItems = downloadItems;
	}


	public DownloadItem getDownloadItem(int index) {
		return downloadItems.get(index);
	}

}
