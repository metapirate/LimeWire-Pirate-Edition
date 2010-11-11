package org.limewire.ui.swing.table;

import java.awt.Component;

import javax.swing.JTable;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Displays the quality of a file in a table cell. 
 */
@LazySingleton
public class QualityRenderer extends DefaultLimeTableCellRenderer {

    @Inject
    public QualityRenderer(){
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {        
        String text;
        
        if(value == null) {
            text = "";
        } else if(value instanceof VisualSearchResult){            
            VisualSearchResult result = (VisualSearchResult)value;
            if(result.isSpam()) {
                text = I18n.tr("Spam");
            } else {
                if(!(result.getProperty(FilePropertyKey.QUALITY) instanceof Number))
                    text = "";
                else {
                    Number num = ((Number)result.getProperty(FilePropertyKey.QUALITY));
                    text = GuiUtils.toQualityStringShort(num.longValue()) + getQualityDetails(result); 
                }
            }
        } else if(value instanceof LocalFileItem) {
            LocalFileItem item = (LocalFileItem) value;
            if(!(item.getProperty(FilePropertyKey.QUALITY) instanceof Number))
                text = "";
            else {
                Number num = ((Number)item.getProperty(FilePropertyKey.QUALITY));
                text = GuiUtils.toQualityStringShort(num.longValue()) + getQualityDetails(item); 
            }
        } else {
            text = "";
        }
        
        super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
        return this;
    }
    
    private String getQualityDetails(PropertiableFile propertiable){
        if (propertiable.getCategory() == Category.AUDIO){
            Object bitRate = propertiable.getProperty(FilePropertyKey.BITRATE);
            if (bitRate != null) {
                return " (" + bitRate + ")";
            }
        }
        return "";
    }
}
