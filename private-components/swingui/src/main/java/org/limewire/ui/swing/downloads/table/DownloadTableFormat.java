package org.limewire.ui.swing.downloads.table;

import java.util.Comparator;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.table.AbstractAdvancedTableFormat;

class DownloadTableFormat extends AbstractAdvancedTableFormat<DownloadItem> {
    public static final int TITLE = 0;
    public static final int TITLE_GAP = 1;
    public static final int PROGRESS = 2;
    public static final int PROGRESS_GAP = 3;
    public static final int MESSAGE = 4;
    public static final int MESSAGE_GAP = 5;
    public static final int ACTION = 6;
    public static final int ACTION_GAP = 7;
    public static final int CANCEL = 8;
    public DownloadTableFormat(){
        super("title", "titleGap", "progress", "progress gap", "message", "message gap", 
                "action", "action gap", "cancel");
    }

    @Override
    public Class getColumnClass(int column) {
        return DownloadItem.class;
    }

    @Override
    public Comparator getColumnComparator(int column) {
        return null;
    }

    @Override
    public Object getColumnValue(DownloadItem baseObject, int column) {
        return baseObject;
    }

   
}
