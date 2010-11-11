package org.limewire.ui.swing.library.table;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.player.PlayerMediator;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
class IsPlayingRenderer extends DefaultTableCellRenderer {

    @Resource Icon playingIcon;
    
    private final Provider<PlayerMediator> playerMediator;
    private final Border emptyBorder;
    
    @Inject
    public IsPlayingRenderer(Provider<PlayerMediator> playerMediator) {
        GuiUtils.assignResources(this);
        
        this.playerMediator = playerMediator;
        emptyBorder = BorderFactory.createEmptyBorder(0,3,0,3);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    
        setText("");
        setBorder(emptyBorder);
        
        if(value instanceof LocalFileItem) {
            if(playerMediator.get().isPlaying(((LocalFileItem)value).getFile()) || playerMediator.get().isPaused(((LocalFileItem)value).getFile()))
                setIcon(playingIcon);
            else
                setIcon(null);
        } else {
            setIcon(null);
        }
        
        return this;
    }
}
