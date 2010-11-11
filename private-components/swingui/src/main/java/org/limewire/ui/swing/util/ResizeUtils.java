package org.limewire.ui.swing.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Common methods for adjusting the size of various components.
 */
public class ResizeUtils {
    
    /**
     * Forces a component to a specific height without changing any of the 
     *  width defaults.
     */
    public static void forceHeight(Component comp, int height) {
        comp.setMinimumSize(new Dimension((int)comp.getMinimumSize().getWidth(), height));
        comp.setMaximumSize(new Dimension((int)comp.getMaximumSize().getWidth(), height));
        comp.setPreferredSize(new Dimension((int)comp.getPreferredSize().getWidth(), height));
        comp.setSize(new Dimension((int)comp.getSize().getWidth(), height));
    }

    /**
     * Forces a component to a specific width without changing any of the 
     *  height defaults.
     */
    public static void forceWidth(Component comp, int width) {
        comp.setMinimumSize(new Dimension(width, (int)comp.getMinimumSize().getHeight()));
        comp.setMaximumSize(new Dimension(width, (int)comp.getMaximumSize().getHeight()));
        comp.setPreferredSize(new Dimension(width, (int)comp.getPreferredSize().getHeight()));
        comp.setSize(new Dimension(width, (int)comp.getSize().getHeight()));
    }
    
    public static void forceSize(Component comp, Dimension d) {
        comp.setMaximumSize(d);
        comp.setMaximumSize(d);
        comp.setPreferredSize(d);
        comp.setSize(d);
    }
    
    /**
     * Attempts to set sane maximum and minimum size values for a component
     *  without modifying preferred and set size.  Minimally invasive and
     *  usually leaves things in a state where the component width grows with contents.
     */
    public static void looseForceHeight(Component comp, int height) {
        comp.setMinimumSize(new Dimension(10, height));
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }
    
    public static void looseForceWidth(Component comp, int width) {
        comp.setMinimumSize(new Dimension(width, 10));
        comp.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
    }
    
    /**
     * Updates the size of the button to match either the explicit text of the
     * button, or the largest item in the menu.
     */
    public static void updateSize(JButton comp,  List<Action> items) {        
        if (comp.getText() == null && (items == null || items.isEmpty())) {
            return;
        }
        
        Font font = comp.getFont();
        FontMetrics fm = comp.getFontMetrics(font);
        Rectangle largest = new Rectangle();
        Rectangle iconR = new Rectangle();
        Rectangle textR = new Rectangle();
        Rectangle viewR = new Rectangle(Short.MAX_VALUE, Short.MAX_VALUE);
        
        // If text is explicitly set, layout that text.
        if(comp.getText() != null && !comp.getText().isEmpty()) {
            SwingUtilities.layoutCompoundLabel(
                    comp, fm, comp.getText(), comp.getIcon(),
                    comp.getVerticalAlignment(), comp.getHorizontalAlignment(),
                    comp.getVerticalTextPosition(), comp.getHorizontalTextPosition(),
                    viewR, iconR, textR, comp.getIconTextGap()
            );
            Rectangle r = iconR.union(textR);
            largest = r;
        } else {
            // Otherwise, find the largest layout area of all the menu items.
            for(Action action : items) {
                Icon icon = (Icon)action.getValue(Action.SMALL_ICON);
                String text = (String)action.getValue(Action.NAME);            
                
                iconR.height = iconR.width = iconR.x = iconR.y = 0;
                textR.height = textR.width = textR.x = textR.y = 0;
                viewR.x = viewR.y = 0;
                viewR.height = viewR.width = Short.MAX_VALUE;
                
                SwingUtilities.layoutCompoundLabel(
                        comp, fm, text, icon,
                        SwingConstants.CENTER, SwingConstants.CENTER,
                        SwingConstants.CENTER, SwingConstants.TRAILING,
                        viewR, iconR, textR, (text == null ? 0 : 4)
                );
                Rectangle r = iconR.union(textR);                
                largest.height = Math.max(r.height, largest.height);
                largest.width = Math.max(r.width, largest.width);
            }
        }
        
        Insets insets = comp.getInsets();
        largest.width += insets.left + insets.right;
        largest.height += insets.top + insets.bottom;
        largest.height = Math.max(comp.getMinimumSize().height, largest.height);
        
        comp.setMaximumSize(new Dimension(200, 100));
        comp.setMinimumSize(largest.getSize());
        comp.setPreferredSize(largest.getSize());
        comp.setSize(largest.getSize());
        
        comp.revalidate();
        comp.repaint();
    }
}
