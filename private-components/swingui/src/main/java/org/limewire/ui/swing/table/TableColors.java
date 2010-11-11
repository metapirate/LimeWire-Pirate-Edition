/**
 * 
 */
package org.limewire.ui.swing.table;

import java.awt.Color;
import java.awt.Font;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.ui.swing.util.GuiUtils;

public class TableColors {
    /**
     * These consider the first element even (zero based).
     */
    @Resource
    public Color evenColor;
    @Resource
    public Color evenForeground;
    @Resource
    public Color oddColor;
    @Resource
    public Color oddForeground;
    @Resource
    public Color menuRowColor;
    @Resource
    public Color menuRowForeground;    
    @Resource
    public Color selectionColor;
    @Resource
    public Color selectionForeground;
    @Resource
    private Color disabledForegroundColor;
    @Resource
    private Color gridColor;
    @Resource
    private Font tableFont;
    
    private ColorHighlighter evenHighlighter;
    
    private ColorHighlighter oddHighlighter;
    
    public TableColors() {
        GuiUtils.assignResources(this);
        
        evenHighlighter = new ColorHighlighter(HighlightPredicate.EVEN, evenColor, evenForeground, selectionColor, selectionForeground);
        oddHighlighter = new ColorHighlighter(HighlightPredicate.ODD, oddColor, oddForeground, selectionColor, selectionForeground);
    }
    
    public ColorHighlighter getEvenHighlighter() {
        return evenHighlighter;
    }
    
    public ColorHighlighter getOddHighlighter() {
        return oddHighlighter;
    }
    
    public Color getDisabledForegroundColor() {
        return disabledForegroundColor;
    }
    
    public Color getGridColor() {
        return gridColor;
    }
    
    public Font getTableFont() {
        return tableFont;
    }
}