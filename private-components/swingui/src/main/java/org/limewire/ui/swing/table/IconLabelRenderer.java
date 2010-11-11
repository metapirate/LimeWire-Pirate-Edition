package org.limewire.ui.swing.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JTable;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

/**
 * Renders a table cell with a string and the system icon representing that
 * file type.
 */
public class IconLabelRenderer extends DefaultLimeTableCellRenderer {

    private final Provider<IconManager> iconManager;
    private final CategoryIconManager categoryIconManager;
    private final boolean showAudioArtist;
    
    @Resource private Icon spamIcon;
    @Resource private Icon downloadingIcon;
    @Resource private Icon libraryIcon;
    @Resource private Icon warningIcon;
    @Resource private Color disabledForegroundColor;
    @Resource private Font font;
    
    @Inject
    public IconLabelRenderer(Provider<IconManager> iconManager, 
            CategoryIconManager categoryIconManager,
            @Assisted boolean showAudioArtist) {
        GuiUtils.assignResources(this);
        
        this.iconManager = iconManager;
        this.categoryIconManager = categoryIconManager;
        this.showAudioArtist = showAudioArtist;
        
        setIconTextGap(5);
        setFont(font);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        Color background;
        Color foreground;
        Icon icon;
        String text;
        
        if (table.getSelectedRow() == row) {
            background = table.getSelectionBackground();
            foreground = table.getSelectionForeground();
        } else {
            background = table.getBackground();
            foreground = table.getForeground();
        }
        
        if (value instanceof FileItem) {            
            FileItem item = (FileItem) value;            
            if(item instanceof LocalFileItem) {
                icon = iconManager.get().getIconForFile(((LocalFileItem) item).getFile());
                LocalFileItem localFileItem = (LocalFileItem) item;
                if(localFileItem.isIncomplete()) {
                    text = I18n.tr("{0} (downloading)", item.getFileName());
                } else {
                    text = item.getFileName();
                }
            } else {
                icon = null;
                text = item.getFileName();
            }            
        } else if (value instanceof VisualSearchResult){            
            VisualSearchResult vsr = (VisualSearchResult)value;            
            text = vsr.getNameProperty(showAudioArtist);
            icon = getIcon(vsr);
            if(vsr.isSpam()) {
                foreground = disabledForegroundColor;
            } else {
                foreground = table.getForeground();
            }
        } else if (value != null) {
            throw new IllegalArgumentException(value + " must be a FileItem or VisualSearchResult, not a " + value.getClass().getCanonicalName());
        } else {
            icon = null;
            text = "";
        }
        
        setBackground(background);
        setForeground(foreground);
        setIcon(icon);
        setText(text);
        
        return this;
    }
    
    @Override
    protected void setValue(Object value) {
        // make this a noop, so value.toString() is not called
        // text is set explicitly in this class here
    }
    
    @Override
    public String getToolTipText(){
        return getText();
    }
    
    private Icon getIcon(VisualSearchResult vsr) {
        if (vsr.isSpam()) {
            return spamIcon;
        } 
        switch (vsr.getDownloadState()) {
        case DOWNLOADING:
            return downloadingIcon;
        case DOWNLOADED:
        case LIBRARY:
            return libraryIcon;
        case REMOVED:
            return warningIcon;
        }
        return categoryIconManager.getIcon(vsr);
    }
}
