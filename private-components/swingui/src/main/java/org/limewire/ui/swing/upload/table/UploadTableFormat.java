package org.limewire.ui.swing.upload.table;

import java.util.Comparator;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.ui.swing.table.AbstractAdvancedTableFormat;

/**
 * Table format for the Uploads table.
 */
public class UploadTableFormat extends AbstractAdvancedTableFormat<UploadItem> {
    public static final int TITLE_COL = 0;
    public static final int TITLE_GAP = 1;
    public static final int PROGRESS_COL = 2;
    public static final int PROGRESS_GAP = 3;
    public static final int MESSAGE_COL = 4;
    public static final int MESSAGE_GAP = 5;
    public static final int ACTION_COL = 6;
    public static final int ACTION_GAP = 7;
    public static final int CANCEL_COL = 8;

    public UploadTableFormat() {
        super("title", "titleGap", "progress", "progress gap", "message", 
                "message gap", "action", "action gap", "cancel");
    }

    @Override
    public Class getColumnClass(int column) {
        return UploadItem.class;
    }

    @Override
    public Comparator getColumnComparator(int column) {
        return null;
    }

    @Override
    public Object getColumnValue(UploadItem baseObject, int column) {
        return baseObject;
    }
}
