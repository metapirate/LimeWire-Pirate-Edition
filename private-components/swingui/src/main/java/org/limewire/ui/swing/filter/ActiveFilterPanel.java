package org.limewire.ui.swing.filter;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Display panel for an active filter.
 */
class ActiveFilterPanel extends JXPanel {
    private static final int PREF_HEIGHT = 18;
    private static final int HORIZ_INSET = 12;

    @Resource(key="AdvancedFilter.filterWidth") private int filterWidth;
    @Resource private Color backgroundColor;
    @Resource private Color borderColor;
    @Resource private Color textColor;
    @Resource private Font textFont;
    @Resource private Icon removeDefaultIcon;
    @Resource private Icon removeHoverIcon;
    
    private final JLabel label = new JLabel();
    private final JButton removeButton = new JButton();

    /**
     * Constructs an ActiveFilterPanel with the specified remove action.
     */
    public ActiveFilterPanel(Action removeAction) {
        
        GuiUtils.assignResources(this);
        
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        setLayout(new MigLayout("insets 0 0 0 0, gap 0!", 
                "[left][right]", "[center]"));
        setOpaque(false);
        setBackgroundPainter(new RectanglePainter(0, 0, 0, 0, 16, 16, true,
                backgroundColor, 1.0f, borderColor));
        
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 3));
        label.setFont(textFont);
        label.setForeground(textColor);
        label.setHorizontalAlignment(JLabel.LEADING);
        label.setVerticalAlignment(JLabel.CENTER);
        label.setText((String) removeAction.getValue(Action.NAME));
        label.setPreferredSize(new Dimension(label.getPreferredSize().width, PREF_HEIGHT));
        
        removeButton.setAction(removeAction);
        removeButton.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
        removeButton.setContentAreaFilled(false);
        removeButton.setFocusPainted(false);
        removeButton.setIcon(removeDefaultIcon);
        removeButton.setText(null);
        
        // Add listener to show cursor on mouse over.
        removeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                removeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                removeButton.setIcon(removeHoverIcon);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                removeButton.setCursor(Cursor.getDefaultCursor());
                removeButton.setIcon(removeDefaultIcon);
            }
        });
        
        // Determine max label width.
        int maxWidth = filterWidth - removeButton.getPreferredSize().width - HORIZ_INSET;
        
        add(removeButton, "gap 0 0");
        add(label       , "wmax " + maxWidth + ", hmin " + PREF_HEIGHT);
    }
    
}
