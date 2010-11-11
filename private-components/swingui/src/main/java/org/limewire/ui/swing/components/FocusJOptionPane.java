package org.limewire.ui.swing.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;


/**
 * A simple wrapper around {@link JOptionPane} methods that
 * forces the parent component to be a visible frame,
 * allowing it to blink in the task bar.
 */
public class FocusJOptionPane {
    
    /**
     * Contains a string representation of the text of all of the currently 
     * visible message dialogs. Presently this is only applicable to dialogs
     * displayed via showMessageDialog.
     */
    private static final List<String> visibleDialogs = new ArrayList<String>();
    
    public static Component createFocusComponent() {
        JFrame frame = new LimeJFrame("LimeWire");
        frame.setUndecorated(true);
        frame.setSize(0, 0);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(screenSize.width / 2, screenSize.height / 2);
        frame.setVisible(true);
        return frame;
     }
    
    /**
     * @see JOptionPane#showMessageDialog(Component, Object, String, int)
     */
    public static void showMessageDialog(Component parentComponent, Object message, String title,
            int messageType) throws HeadlessException {
        String messageString = message.toString();
        // if the message text is already present in a visible message dialog,
        // then we needn't show it again. just ignore it and return.
        //
        // later, when the dialog is closed, we'll remove its text from this
        // list.
        if (visibleDialogs.contains(messageString))
            return;
            
        visibleDialogs.add(messageString);
        
        boolean dispose = false;
        parentComponent = getWindowForComponent(parentComponent);
        if(parentComponent == null) {
            parentComponent = createFocusComponent();
            dispose = true;
        }
        
        Color oldOptionColor = UIManager.getColor("OptionPane.background");
        Color oldPanelColor = UIManager.getColor("Panel.background");
        if(GuiUtils.getMainFrame() != null) {
            UIManager.put("OptionPane.background", GuiUtils.getMainFrame().getBackground());
            UIManager.put("Panel.background", GuiUtils.getMainFrame().getBackground());
        }
        try {
            JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
        } finally {
            visibleDialogs.remove(messageString);
            
            if (dispose)
                ((JFrame)parentComponent).dispose();
            UIManager.put("OptionPane.background", oldOptionColor);
            UIManager.put("Panel.background", oldPanelColor);
        }
    }

    /**
     * @see JOptionPane#showConfirmDialog(Component, Object, String, int)
     */
    public static int showConfirmDialog(Component parentComponent, Object message, String title,
            int optionType) throws HeadlessException {
        boolean dispose = false;
        parentComponent = getWindowForComponent(parentComponent);
        if(parentComponent == null) {
            parentComponent = createFocusComponent();
            dispose = true;
        }
        return showConfirmationDialog(parentComponent, message, title, optionType, JOptionPane.QUESTION_MESSAGE, null, null, dispose);
    }
    
    /**
     * @see JOptionPane#showConfirmDialog(Component, Object, String, int)
     */
    public static int showConfirmDialog(Component parentComponent, Object message, String title,
            int optionType, int messageType) throws HeadlessException {
        boolean dispose = false;
        parentComponent = getWindowForComponent(parentComponent);
        if(parentComponent == null) {
            parentComponent = createFocusComponent();
            dispose = true;
        }
        return showConfirmationDialog(parentComponent, message, title, optionType, messageType, null, null, dispose); 
    }

    /**
     * @see JOptionPane#showOptionDialog(Component, Object, String, int, int,
     *      Icon, Object[], Object)
     */
    public static int showOptionDialog(Component parentComponent, Object message, String title,
            int optionType, int messageType, Icon icon, Object[] options, Object initialValue)
            throws HeadlessException {
        boolean dispose = false;
        parentComponent = getWindowForComponent(parentComponent);
        if(parentComponent == null) {
            parentComponent = createFocusComponent();
            dispose = true;
        }
        return showConfirmationDialog(parentComponent, message, title, optionType, messageType, options, initialValue, 
                dispose);
    }
    
    /**
     * Ensures that the colors for the dialog are set properlly prior to being shown.
     */
    private static int showConfirmationDialog(Component parentComponent, Object message, String title,
            int optionType, int messageType, Object[] options, Object initialValue, boolean dispose) {
        Color oldOptionColor = UIManager.getColor("OptionPane.background");
        Color oldPanelColor = UIManager.getColor("Panel.background");
        UIManager.put("OptionPane.background", GuiUtils.getMainFrame().getBackground());
        UIManager.put("Panel.background", GuiUtils.getMainFrame().getBackground());
        
        try {
            return JOptionPane.showOptionDialog(parentComponent, message, title, optionType, messageType,
                    null, options, initialValue);
        } finally {
            if(dispose)
                ((JFrame)parentComponent).dispose();
            UIManager.put("OptionPane.background", oldOptionColor);
            UIManager.put("Panel.background", oldPanelColor);
        }
    }
    
    public static JDialog createDialog(String title, JComponent parent, JComponent contentPane) {
        JDialog dialog;
        Window window = FocusJOptionPane.getWindowForComponent(parent);
        if (window instanceof Frame) {
            dialog = new LimeJDialog((Frame)window, title, true);   
        } else {
            dialog = new LimeJDialog((Dialog)window, title, true);
        }
        
        Container frameContentPane = dialog.getContentPane();
        frameContentPane.setLayout(new BorderLayout());
        frameContentPane.add(contentPane, BorderLayout.CENTER);
        contentPane.setOpaque(false);
        frameContentPane.setBackground(GuiUtils.getMainFrame().getBackground());

        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parent);
        dialog.pack();
        
        return dialog;
    }
    
    public static int showYesNoMessage(String message, String title, int defaultOption, JComponent parent) {
        final String[] options = {I18n.tr("Yes"), I18n.tr("No")};
        
        int option;
        Color oldOptionColor = UIManager.getColor("OptionPane.background");
        Color oldPanelColor = UIManager.getColor("Panel.background");
        UIManager.put("OptionPane.background", GuiUtils.getMainFrame().getBackground());
        UIManager.put("Panel.background", GuiUtils.getMainFrame().getBackground());
        try {
            option = FocusJOptionPane.showOptionDialog(getWindowForComponent(parent), 
                         getLabel(message), 
                         title,
                         JOptionPane.YES_NO_OPTION, 
                         JOptionPane.WARNING_MESSAGE, null,
                         options, defaultOption);
        } catch(InternalError ie) {
            option = JOptionPane.NO_OPTION;
        } finally {
            UIManager.put("OptionPane.background", oldOptionColor);
            UIManager.put("Panel.background", oldPanelColor);
        }
        
        return option;
    }
    
    private static JComponent getLabel(String message) {
        if(message.startsWith("<html")) {
            HTMLLabel label = new HTMLLabel(message);
            label.setMargin(new Insets(5, 5, 5, 5));
            return label;
        } else {
            return new MultiLineLabel(message, 400);
        }
    }
    
    public static Window getWindowForComponent(Component parentComponent) throws HeadlessException {
        if (parentComponent == null) {
            return GuiUtils.getMainFrame();
        }
        
        if (parentComponent instanceof Frame || parentComponent instanceof Dialog) {
            return (Window) parentComponent;
        }
        
        return FocusJOptionPane.getWindowForComponent(parentComponent.getParent());
    }
}
