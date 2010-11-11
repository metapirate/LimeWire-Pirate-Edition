package org.limewire.ui.swing.components.decorators;

import java.awt.Component;

import javax.swing.table.JTableHeader;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.table.TableColors;

public class TableDecorator {

    public void decorate(JXTable table) {
        TableColors colors = new TableColors();
        
        table.setFont(colors.getTableFont());

        table.setHighlighters(colors.getEvenHighlighter(), 
                colors.getOddHighlighter(),
                new ColorHighlighter(new HighlightPredicate() {
                    @Override
                    public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
                        return false;
                    }
                }, colors.menuRowColor,  colors.menuRowForeground, colors.menuRowColor, colors.menuRowForeground));

        table.setGridColor(colors.getGridColor());
        
        JTableHeader th = table.getTableHeader();
        th.setDefaultRenderer(new TableCellHeaderRenderer());
        
        table.setDefaultRenderer(Object.class, new DefaultLimeTableCellRenderer());
    }
    
}
