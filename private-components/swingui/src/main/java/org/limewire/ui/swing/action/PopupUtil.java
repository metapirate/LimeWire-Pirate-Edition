package org.limewire.ui.swing.action;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class PopupUtil {

    public static JPopupMenu addPopupMenus(JComponent component, Action... actions) {
        return addPopupMenus(component, AlwaysShowPopup.get(), actions);
    }

    public  static JPopupMenu addPopupMenus(JComponent component, PopupDecider decider, Action... actions) {
        JPopupMenu menu = new JPopupMenu();
        for(Action action : actions) {
            menu.add(action);
        }
        PopupListener popupListener = new PopupListener(menu, decider);
        component.addMouseListener(popupListener);
        menu.addPopupMenuListener(popupListener);
        return menu;
    }
    
    private static class AlwaysShowPopup implements PopupDecider {
        private static AlwaysShowPopup INSTANCE = new AlwaysShowPopup();
        
        private AlwaysShowPopup(){}
        
        public static PopupDecider get() { return INSTANCE; }
        
        @Override
        public boolean shouldDisplay(MouseEvent e) {
            return true;
        }
    }


    /**
     * Handles displaying the popup.  Also sets the enablement of each menuitem in a JPopupMenu
     * based on its action's enablement. 
     */
    private static class PopupListener extends MouseAdapter implements PopupMenuListener {
        private final JPopupMenu popup;
        private final PopupDecider decider;
        
        public PopupListener(JPopupMenu popup, PopupDecider decider) {
            this.popup = popup;
            this.decider = decider;
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger() && decider.shouldDisplay(e)) {
                popup.show(e.getComponent(),
                           e.getX(), e.getY());
            }
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            //no-op
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            //no-op
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            for(Component comp : popup.getComponents()) {
                if (comp instanceof JMenuItem) {
                    JMenuItem item = (JMenuItem)comp;
                    Action action = item.getAction();
                    if (action != null) {
                        item.setEnabled(action.isEnabled());
                    }
                    //This is a hack to let the action modify aspects about the item before display
                    if (action instanceof ItemNotifyable) {
                        ItemNotifyable notifyable =  (ItemNotifyable)action;
                        notifyable.notifyItem(item);
                    }
                }
            }
        }
    }
}
