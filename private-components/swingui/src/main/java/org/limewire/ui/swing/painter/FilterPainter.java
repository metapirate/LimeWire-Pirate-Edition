package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * An area painter intended for use on the filter input field.  When text is
 * entered, FilterPainter paints a background color in the field and displays
 * a "reset" icon on the right edge.  Clicking on the reset icon deletes all
 * text in the field.
 * 
 * <p>FilterPainter is installed on an instance of LimePromptTextField by 
 * calling the method <code>install(LimePromptTextField)</code>.</p>
 */
public class FilterPainter<T> extends RectanglePainter<T> 
    implements DocumentListener {

    @Resource private Color fillColor;
    @Resource private Icon activeResetIcon;
    @Resource private Icon inactiveResetIcon;
    
    private final MouseAdapter resetListener;
    
    private Cursor defaultCursor;
    private Icon resetIcon;
    private boolean showIcon;

    /**
     * Constructs a FilterPainter with the specified arc width and arc height.
     */
    public FilterPainter(int arcWidth, int arcHeight) {
        GuiUtils.assignResources(this);
        
        this.resetListener = new ResetMouseListener();
        
        // Set painter properties.
        setRounded(true);
        setFillPaint(Color.WHITE);
        setRoundWidth(arcWidth);
        setRoundHeight(arcHeight);
        setInsets(new Insets(2,2,2,2));
        setBorderPaint(null);
        setFillVertical(true);
        setFillHorizontal(true);
        setAntialiasing(true);
        setCacheable(true);
        setResetIcon(inactiveResetIcon);
    }
    
    /**
     * Installs this painter on the specified text field. 
     */
    public void install(PromptTextField textField) {
        // Set default cursor.
        defaultCursor = textField.getCursor();
        
        // Set border to increase right margin.
        textField.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 
                12 + resetIcon.getIconWidth()));
        
        // Install mouse listener.
        textField.addMouseListener(resetListener);
        textField.addMouseMotionListener(resetListener);
        
        // Install document listener to update fill color.
        textField.getDocument().addDocumentListener(this);
    }
    
    @Override
    public void doPaint(Graphics2D g, T component, int width, int height) {
        super.doPaint(g, component, width, height);
        
        // Draw reset icon if enabled.
        if (showIcon) {
            Point iconLocation = getIconLocation(width, height);
            resetIcon.paintIcon((Component) component, g, iconLocation.x, iconLocation.y);
        }
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        // Do nothing.
    }

    /**
     * Handles insert text event to update fill color and reset icon.
     */
    @Override
    public void insertUpdate(DocumentEvent e) {
        updatePainter(e.getDocument());
    }

    /**
     * Handles remove text event to update fill color and reset icon.
     */
    @Override
    public void removeUpdate(DocumentEvent e) {
        updatePainter(e.getDocument());
    }
    
    /**
     * Returns the position of the reset icon for the specified text field
     * width and height.
     */
    private Point getIconLocation(int width, int height) {
        // Get icon size.
        int iconWidth = resetIcon.getIconWidth();
        int iconHeight = resetIcon.getIconHeight();
        
        // Return icon position.  This is indented from the right-side of the
        // text field, and vertically centered.
        return new Point(width - iconWidth - 6, (height - iconHeight) / 2);
    }

    /**
     * Returns true if the specified mouse event is positioned over the reset
     * icon.
     */
    private boolean isIconLocation(MouseEvent e) {
        // Get mouse and icon locations.
        Point mouseLocation = e.getPoint();
        Point iconLocation = getIconLocation(e.getComponent().getWidth(), e.getComponent().getHeight());
        int iconWidth = resetIcon.getIconWidth();
        int iconHeight = resetIcon.getIconHeight();
        
        // Return true if mouse is within icon boundaries.
        if ((mouseLocation.x >= iconLocation.x) && 
            (mouseLocation.x <= iconLocation.x + iconWidth) && 
            (mouseLocation.y >= iconLocation.y) &&
            (mouseLocation.y <= iconLocation.y + iconHeight)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the reset icon.  If the icon is changed, then the painter is
     * marked dirty so it will be repainted.
     */
    private void setResetIcon(Icon resetIcon) {
        if (this.resetIcon != resetIcon) {
            this.resetIcon = resetIcon;
            setDirty(true);
        }
    }

    /**
     * Updates the painter based on the content in the specified document.
     */
    private void updatePainter(Document document) {
        setFillPaint((document.getLength() == 0) ? Color.WHITE : fillColor);
        showIcon = (document.getLength() != 0);
        
        // Restore inactive icon when text cleared.
        if (!showIcon) {
            setResetIcon(inactiveResetIcon);
        }
    }
    
    /**
     * Listener to handle mouse events on the text field.
     */
    private class ResetMouseListener extends MouseAdapter {
        /**
         * Handles mouse moved event to update the mouse cursor and reset icon. 
         */
        @Override
        public void mouseMoved(MouseEvent e) {
            JTextField textField = (JTextField) e.getSource();
            
            if (showIcon) {
                // Update cursor and icon based on mouse position.
                if (isIconLocation(e)) {
                    textField.setCursor(Cursor.getDefaultCursor());
                    setResetIcon(activeResetIcon);
                } else {
                    textField.setCursor(defaultCursor);
                    setResetIcon(inactiveResetIcon);
                }
                
                // Request repaint to draw icon.
                textField.repaint();
                
            } else if (textField.getCursor() != defaultCursor) {
                textField.setCursor(defaultCursor);
            }
        }

        /**
         * Handles mouse clicked event to clear filter text.
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            if (showIcon && isIconLocation(e)) {
                ((JTextField) e.getSource()).setText(null);
            }
        }
    }
}
