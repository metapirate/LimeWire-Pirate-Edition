package org.limewire.ui.swing.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.GroupLayout.Group;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.icon.EmptyIcon;
import org.limewire.ui.swing.util.GuiUtils;

class FancyTabMoreButton extends LimeComboBox {
    
    @Resource private Icon selectedIcon;
    private Icon unselectedIcon;    
    
    private static volatile int moreTabsButtonClicked = 0;
    
    private JPopupMenu menu = new JPopupMenu();
    
    public FancyTabMoreButton(List<FancyTab> tabs) {
        super(null);        
        GuiUtils.assignResources(this);
        unselectedIcon = new EmptyIcon(selectedIcon.getIconWidth(), selectedIcon.getIconHeight());
        overrideMenu(menu);
        MoreListener listener = new MoreListener(tabs);
        menu.addPopupMenuListener(listener);
        setFocusable(false);
    }
    
    private JComponent createMenuItemFor(
            final JPopupMenu menu, final FancyTab tab) {

        JXPanel jp = new JXPanel();
        jp.setOpaque(false);
        jp.setBackground(UIManager.getColor("MenuItem.selectionBackground"));
        
        final AbstractButton selectButton = tab.createMainButton();
        selectButton.setOpaque(false);
        selectButton.setHorizontalAlignment(SwingConstants.LEADING);
        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                menu.setVisible(false);
            }
        });
        
        JLabel selectionLabel = new JLabel(tab.isSelected() ? selectedIcon : unselectedIcon);
        selectionLabel.setOpaque(false);
        JLabel busyLabel = tab.createBusyLabel();
        busyLabel.setOpaque(false);
        
        GroupLayout layout = new GroupLayout(jp);
        jp.setLayout(layout);
        
        SequentialGroup horGroup = layout.createSequentialGroup();
        layout.setHorizontalGroup(horGroup);
        
        Group verGroup = layout.createParallelGroup(GroupLayout.Alignment.CENTER);
        layout.setVerticalGroup(verGroup);
        
        layout.setAutoCreateGaps(true);
        
        horGroup.addGap(5)
                .addComponent(selectionLabel)
                .addComponent(busyLabel)
                .addComponent(selectButton, 0, 120, 120);
        
        verGroup.addComponent(selectionLabel)
                .addComponent(busyLabel)
                .addComponent(selectButton);
        
        layout.setHonorsVisibility(busyLabel, false);
        
        Highlighter highlighter = new Highlighter(jp, selectButton);
        jp.addMouseListener(highlighter);
        selectButton.addMouseListener(highlighter);
        
        return jp;
    }
    
    private static class Highlighter extends MouseAdapter {
        private final JXPanel panel;
        private final AbstractButton selectButton;
        
        public Highlighter(JXPanel panel, AbstractButton selectButton) {
            this.panel = panel;
            this.selectButton = selectButton;
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            panel.setOpaque(true);
            panel.repaint();
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            panel.setOpaque(false);
            panel.repaint();
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            // Forward the click to selection if it wasn't already a button
            if(!(e.getSource() instanceof AbstractButton) && SwingUtilities.isLeftMouseButton(e)) {
                selectButton.doClick(0);
            }
        }
    }
    
    private class MoreListener implements PopupMenuListener {
        private final List<FancyTab> tabs;
        
        public MoreListener(List<FancyTab> tabs) {
            this.tabs = tabs;
        }
        
        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {}
        
        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            // Reset selected state.
            setSelected(false);
        }
        
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            menu.removeAll();            
            for (FancyTab tab : tabs) {
                menu.add(createMenuItemFor(menu, tab));
            }
            // Set selected state to display selected icon.
            setSelected(true);

            moreTabsButtonClicked++;
        }
    }
}
