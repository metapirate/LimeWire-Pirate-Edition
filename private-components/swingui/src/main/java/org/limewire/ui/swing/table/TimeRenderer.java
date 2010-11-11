package org.limewire.ui.swing.table;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;

import org.limewire.inject.LazySingleton;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;

/**
 * Display the length of an audio/video file in hours:minutes:seconds.
 */
@LazySingleton
public class TimeRenderer extends DefaultLimeTableCellRenderer {
    
    @Inject
    public TimeRenderer(){
        setHorizontalAlignment(SwingConstants.RIGHT);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if (value != null && value instanceof Long) {
            setText(CommonUtils.seconds2time((Long)value)); 
        } else {
            setText("");
        }
        return this;
    }
}
