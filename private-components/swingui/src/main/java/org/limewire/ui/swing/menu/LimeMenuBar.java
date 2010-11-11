package org.limewire.ui.swing.menu;

import java.awt.Color;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.action.DelayedMenuItemCreator;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class LimeMenuBar extends JMenuBar {
    
    @Resource private Color backgroundColor;
    private final MenuListener menuListener;
    
    @Inject
    LimeMenuBar(FileMenu fileMenu, FriendsMenu friendMenu, ViewMenu viewMenu, HelpMenu helpMenu,
            ToolsMenu toolsMenu) {
        
        GuiUtils.assignResources(this);
        
        setBackground(backgroundColor);
        
        menuListener = new MenuListener() {
            @Override
            public void menuCanceled(MenuEvent e) {
                ((JMenu)e.getSource()).removeAll();
            }
            @Override
            public void menuDeselected(MenuEvent e) {
                ((JMenu)e.getSource()).removeAll();                
            }
            @Override
            public void menuSelected(MenuEvent e) {
                ((DelayedMenuItemCreator)e.getSource()).createMenuItems();
            }
        };
        
        addMenu(0, fileMenu);
        addMenu(1, viewMenu);
        addMenu(2, friendMenu);
        addMenu(3, toolsMenu);
        addMenu(4, helpMenu);
    }
    
    private void addMenu(int idx, JMenu  menu) {
        add(menu, idx);
        menu.setBackground(backgroundColor);
        menu.addMenuListener(menuListener);
    }
    
}
