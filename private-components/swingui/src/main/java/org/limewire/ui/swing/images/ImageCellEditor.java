package org.limewire.ui.swing.images;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.library.table.RemoveButton;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class ImageCellEditor extends JPanel {

    @Resource
    private int width;
    @Resource
    private int height;
    
    private final RemoveButton removeButton;
    
    @Inject
    public ImageCellEditor(RemoveButton removeButton) {
        super(new MigLayout("insets 0, gap 0, fill"));
        
        this.removeButton = removeButton;
        this.removeButton.setVisible(false);
        
        GuiUtils.assignResources(this);
        
        setOpaque(false);
        setPreferredSize(new Dimension(width, height));
        setBounds(0, 0, width, height);

        add(removeButton, "alignx right, aligny top, wrap, gaptop 6, gapright 6");
    }
    
    public void setShowButtons(boolean value) {
        removeButton.setVisible(value);
    }
    
    public JButton getRemoveButton() {
        return removeButton;
    }
}
