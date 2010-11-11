package org.limewire.ui.swing.library.table;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.settings.TablesHandler;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.PropertiableFileUtils;

import com.google.inject.Inject;

public class AllTableFormat <T extends LocalFileItem> extends AbstractLibraryFormat<T> {

    static final int PLAY_INDEX = 0;
    static final int NAME_INDEX = 1;
    static final int TYPE_INDEX = 2;
    static final int SIZE_INDEX = 3;
    static final int HIT_INDEX = 4;
    static final int UPLOADS_INDEX = 5;
    static final int UPLOAD_ATTEMPTS_INDEX = 6;
    static final int PATH_INDEX = 7;
    static final int ACTION_INDEX = 8;
    
    /** Icon manager used to find native file type information. */
    private IconManager iconManager;
    
    @Inject
    public AllTableFormat(IconManager iconManager) {
        super(ACTION_INDEX, "LIBRARY_ALL_TABLE", NAME_INDEX, true, new ColumnStateInfo[] {
                new ColumnStateInfo(PLAY_INDEX, "LIBRARY_ALL_PLAY", "", 16, 16, true, false), 
                new ColumnStateInfo(NAME_INDEX, "LIBRARY_ALL_NAME", I18n.tr("Name"), 480, true, true), 
                new ColumnStateInfo(TYPE_INDEX, "LIBRARY_ALL_TYPE", I18n.tr("Type"), 230, true, true),     
                new ColumnStateInfo(SIZE_INDEX, "LIBRARY_ALL_SIZE", I18n.tr("Size"), 60, false, true),
                new ColumnStateInfo(HIT_INDEX, "LIBRARY_ALL_HITS", I18n.tr("Hits"), 100, false, true), 
                new ColumnStateInfo(UPLOADS_INDEX, "LIBRARY_ALL_UPLOADS", I18n.tr("Uploads"), 100, false, true), 
                new ColumnStateInfo(UPLOAD_ATTEMPTS_INDEX, "LIBRARY_ALL_UPLOAD_ATTEMPTS", I18n.tr("Upload attempts"), 200, false, true),
                new ColumnStateInfo(PATH_INDEX, "LIBRARY_ALL_PATH", I18n.tr("Location"), 200, false, true),
                new ColumnStateInfo(ACTION_INDEX, "LIBRARY_ALL_ACTION", I18n.tr(" "), 22, 22, true, false)
        });     
        
        this.iconManager = iconManager;
    }
    
    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
        case PLAY_INDEX: return baseObject;
        case NAME_INDEX: return baseObject;
        case SIZE_INDEX: return baseObject.getSize();
        case TYPE_INDEX:
            if(baseObject.getCategory() == Category.DOCUMENT || baseObject.getCategory() == Category.OTHER) {
                String mime = iconManager.getMIMEDescription(baseObject);
                if(mime != null) {
                    return I18n.tr(baseObject.getCategory().getSingularName()) + " (" + mime + ")";
                } else {
                    return I18n.tr(baseObject.getCategory().getSingularName());
                }
            } else {
                return I18n.tr(baseObject.getCategory().getSingularName());
            }
        case HIT_INDEX: return baseObject.getNumHits();
        case UPLOAD_ATTEMPTS_INDEX: return baseObject.getNumUploadAttempts();
        case UPLOADS_INDEX: return baseObject.getNumUploads();
        case PATH_INDEX: return baseObject.getProperty(FilePropertyKey.LOCATION);
        case ACTION_INDEX: return baseObject;
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }
    
    @Override
    public Class getColumnClass(int column) {
        switch(column) {
        case PLAY_INDEX:
        case NAME_INDEX:
            return FileItem.class;
        }
        return super.getColumnClass(column);
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
        return Collections.emptyList();
    }
    
    @Override
    public Comparator getColumnComparator(int column) {
        switch(column) {
            case NAME_INDEX: return new NameComparator();
        }
        return super.getColumnComparator(column);
    }
    
    /**
     * Compares the title field in the NAME_COLUMN.
     */
    private class NameComparator implements Comparator<FileItem> {
        @Override
        public int compare(FileItem o1, FileItem o2) {               
            String title1 = getDisplayText((LocalFileItem) o1);
            String title2 = getDisplayText((LocalFileItem) o2);
            
            return title1.toLowerCase(Locale.US).compareTo(title2.toLowerCase(Locale.US));
        }
        
        private String getDisplayText(LocalFileItem item) {
            if(item.getCategory() == Category.AUDIO) {
                return PropertiableFileUtils.getNameProperty(item, true);
            } else {
                return item.getFileName();
            }
        }
    }
}
