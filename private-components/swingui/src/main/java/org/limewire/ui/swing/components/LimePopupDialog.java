package org.limewire.ui.swing.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

public class LimePopupDialog {     
   
    private final JPopupMenu popup = new JPopupMenu();

    @Resource private Color titleForeground;
    @Resource private Color titleBackground;
    @Resource private Font titleFont;
    @Resource private Icon closeIcon;
    @Resource private Icon closeIconRollover;
    @Resource private Icon closeIconPressed;
    
    public LimePopupDialog(String title, JComponent component){
        GuiUtils.assignResources(this);
        
        JPanel mainPanel = new JPanel();
        
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(titleBackground);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        titlePanel.setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel();
        titleLabel.setForeground(titleForeground);
        titleLabel.setFont(titleFont);
        titleLabel.setText(title);
        
        JButton closeButton = new IconButton(closeIcon, closeIconRollover, closeIconPressed);
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 0));
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popup.setVisible(false);
            }
        });
        
        popup.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        
        titlePanel.setLayout(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(closeButton, BorderLayout.EAST);
        
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(component, BorderLayout.CENTER);
        
        popup.add(mainPanel);
    }

    /**
     * Displays the popup window at the x,y position relative to
     * the specified invoker.
     */
    public void showPopup(Component invoker, int x, int y){
        popup.show(invoker, x, y);
    }
    
    /**
     * Hides the popup window.
     */
    public void hidePopup() {
        popup.setVisible(false);
    }
}
