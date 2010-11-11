package org.limewire.ui.swing.library.table;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.settings.TablesHandler;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Table format for the Other Table when it is in My Library.
 */
public class OtherTableFormat<T extends LocalFileItem> extends AbstractLibraryFormat<T> {

    static final int NAME_INDEX = 0;
    static final int TYPE_INDEX = 1;
    static final int SIZE_INDEX = 2;
    static final int HIT_INDEX = 3;
    static final int UPLOADS_INDEX = 4;
    static final int UPLOAD_ATTEMPTS_INDEX = 5;
    static final int PATH_INDEX = 6;
    static final int ACTION_INDEX = 7;
    
    private Provider<IconManager> iconManager;
    
    @Inject
    public OtherTableFormat(Provider<IconManager> iconManager) {
        super(ACTION_INDEX, "LIBRARY_OTHER_TALBE", NAME_INDEX, true, new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "LIBRARY_OTHER_NAME", I18n.tr("Name"), 480, true, true), 
                new ColumnStateInfo(TYPE_INDEX, "LIBRARY_OTHER_TYPE", I18n.tr("Type"), 230, true, true),     
                new ColumnStateInfo(SIZE_INDEX, "LIBRARY_OTHER_SIZE", I18n.tr("Size"), 60, false, true),
                new ColumnStateInfo(HIT_INDEX, "LIBRARY_OTHER_HITS", I18n.tr("Hits"), 100, false, true), 
                new ColumnStateInfo(UPLOADS_INDEX, "LIBRARY_OTHER_UPLOADS", I18n.tr("Uploads"), 100, false, true), 
                new ColumnStateInfo(UPLOAD_ATTEMPTS_INDEX, "LIBRARY_OTHER_UPLOAD_ATTEMPTS", I18n.tr("Upload attempts"), 200, false, true),
                new ColumnStateInfo(PATH_INDEX, "LIBRARY_OTHER_PATH", I18n.tr("Location"), 200, false, true),
                new ColumnStateInfo(ACTION_INDEX, "LIBRARY_OTHER_ACTION", I18n.tr(" "), 22, 22, true, false)
        });
        this.iconManager = iconManager;
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
        case NAME_INDEX: return baseObject;
        case TYPE_INDEX: 
            // Use icon manager to return MIME description.
            return (iconManager != null) ?
                iconManager.get().getMIMEDescription(baseObject) : 
                baseObject.getProperty(FilePropertyKey.DESCRIPTION);
        case SIZE_INDEX: return baseObject.getSize();
        case HIT_INDEX: return baseObject.getNumHits();
        case UPLOAD_ATTEMPTS_INDEX: return baseObject.getNumUploadAttempts();
        case UPLOADS_INDEX: return baseObject.getNumUploads();
        case PATH_INDEX: return baseObject.getProperty(FilePropertyKey.LOCATION);
        case ACTION_INDEX: return baseObject;
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public List<SortKey> getDefaultSortKeys() {
        if(TablesHandler.getSortedColumn(getSortOrderID(), getSortedColumn()).getValue() == getSortedColumn() &&
                TablesHandler.getSortedOrder(getSortOrderID(), getSortOrder()).getValue() == getSortOrder())
            return Arrays.asList(
                    new SortKey(SortOrder.ASCENDING, NAME_INDEX),
                    new SortKey(SortOrder.ASCENDING, TYPE_INDEX),
                    new SortKey(SortOrder.ASCENDING, SIZE_INDEX));
        else
            return super.getDefaultSortKeys();
    }

    @Override
    public List<Integer> getSecondarySortColumns(int column) {
        switch (column) {
        case NAME_INDEX:
            return Arrays.asList(TYPE_INDEX, SIZE_INDEX);
        case TYPE_INDEX:
            return Arrays.asList(NAME_INDEX, SIZE_INDEX);
        case SIZE_INDEX:
            return Arrays.asList(NAME_INDEX, TYPE_INDEX);
        default:
            return Collections.emptyList();
        }
    }
}
