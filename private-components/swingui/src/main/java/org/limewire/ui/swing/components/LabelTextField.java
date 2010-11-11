package org.limewire.ui.swing.components;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Provider;

/**
 * Behaves like a TextField. Adds an icon within the textField that
 * is always displayed to the left of the textfield regardless of how much
 * space there is. 
 * 
 * An action can be added to the textfield that will be called on mouse clicks.
 * 
 * This textfield is currently assumed to be non-editable. Border color and background
 * assume this. If this is to be used in a non-editable fashion, more work will need
 * to be put into the L&F of this.
 */
public class LabelTextField extends JPanel {

    @Resource
    private Icon folderIcon;
    
    private JLabel label;
    private JTextField textField;
    
    private MouseListener mouseListener;
    
    private Provider<IconManager> iconManager;
    
    public LabelTextField(Provider<IconManager> iconManager) {
        
        GuiUtils.assignResources(this);
        
        this.iconManager = iconManager;
        
        label = new JLabel();
        label.setOpaque(false);
        textField = new JTextField();
        textField.setEditable(false);
        textField.setBorder(BorderFactory.createEmptyBorder(0,2,0,0));
        textField.setOpaque(false);
        
        setLayout(new BorderLayout());
        add(label, BorderLayout.WEST);
        add(textField, BorderLayout.CENTER);
        
        // use the colors from the textfield to paint the panel
        setBackground(UIManager.getColor("TextField.disabledBackground"));
        
        
        setBorder(BorderFactory.createCompoundBorder(UIManager.getBorder("TextField.border"),
                BorderFactory.createEmptyBorder(2,2,2,2)));
    }
    
    @Override
    public void setEnabled(boolean value) {
        textField.setEnabled(value);
    }
    
    public void setEditable(boolean value) {
        textField.setEditable(value);
    }
    
    public String getText() {
        return textField.getText();
    }
    
    public void setText(String text) {
        textField.setText(text);
        textField.setCaretPosition(0);
        try {
            Icon icon = iconManager.get().getIconForFile(new File(text));
            if(icon != null)
                label.setIcon(icon);
            else
                label.setIcon(folderIcon);
        } catch(Exception e) {
            label.setIcon(folderIcon);
        }
    }
    
    public void addMouseListener(final Action action) {
        if(mouseListener != null) {
            textField.removeMouseListener(mouseListener);
            mouseListener = null;
        }
        mouseListener = new MouseListener(){
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 1) {
                    action.actionPerformed(null);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
        };
        textField.addMouseListener(mouseListener);
    }
    
    public void removeMouseListener() {
        if(mouseListener != null) {
            textField.removeMouseListener(mouseListener);
            mouseListener = null;
        }
    }
}
