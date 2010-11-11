package org.limewire.ui.swing.nav;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;

public class NavigatorUtils {
    
    public static final String NAV_ITEM = "limewire.navigator.NAV_ITEM";

    private NavigatorUtils() {
    }

    /**
     * Returns an {@link Action} that is synchronized with the {@link NavItem}.
     * When the {@link NavItem} is selected, the button's SELECTED_KEY property
     * is set to true (and when unselected, it is set to false). When the
     * action's SELECTED_KEY is set to true or
     * {@link ActionListener#actionPerformed(ActionEvent)} is called, the item
     * is selected.
     */
    public static Action getNavAction(final NavItem item) {
        return new AbstractAction() {
            {
                item.addNavItemListener(new NavItemListener() {
                    public void itemRemoved(boolean wasSelected) {
                    }

                    public void itemSelected(boolean selected) {
                        putValue(SELECTED_KEY, selected);
                    }
                });

                addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(java.beans.PropertyChangeEvent evt) {
                        if (evt.getPropertyName().equals(SELECTED_KEY)) {
                            if (evt.getNewValue().equals(Boolean.TRUE)) {
                                item.select();
                            }
                        }
                    }
                });
                
                putValue(NAV_ITEM, item);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                item.select();
            }
        };
    }

}
