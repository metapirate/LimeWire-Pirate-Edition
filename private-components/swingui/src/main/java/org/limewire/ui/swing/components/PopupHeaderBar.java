package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.ui.swing.util.ResizeUtils;

public class PopupHeaderBar extends JPanel {

    @Resource private Font font;
    @Resource private Color background = PainterUtils.TRANSPARENT;
    
    private final JLabel titleBarLabel;
    
    public PopupHeaderBar(String title, Action closeAction) {
        super(new MigLayout("gap 0, insets 0, fill"));
        
        GuiUtils.assignResources(this);
        
        ResizeUtils.forceHeight(this, 21);
        
        setBackground(background);
        
        titleBarLabel = new JLabel(title);
        titleBarLabel.setOpaque(false);
        titleBarLabel.setForeground(Color.WHITE);
        titleBarLabel.setFont(font);
        
        add(titleBarLabel, "gapleft 4, gaptop 1, dock west");
        
        if (closeAction != null) {
            IconButton closeButton = new PopupCloseButton();
            closeButton.addActionListener(closeAction);
            add(closeButton, "gapright 3, gaptop 1, dock east");
        }
    }
    
    public void setIcon(Icon icon) {
        titleBarLabel.setIcon(icon);
    }
    
    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (titleBarLabel != null) {
            titleBarLabel.setFont(font);
        }
    }
}
