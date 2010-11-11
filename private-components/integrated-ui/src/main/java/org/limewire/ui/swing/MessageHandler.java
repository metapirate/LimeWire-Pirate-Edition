package org.limewire.ui.swing;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.limewire.service.MessageCallback;
import org.limewire.service.Switch;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;


/**
 * Displays messages to the user using the standard LimeWire messaging service
 * classes.
 */
public class MessageHandler implements MessageCallback {

    @Override
    public void showError(final String error) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), getLabel(I18n
                        .tr(error)), I18n.tr("Error"), JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @Override
    public void showError(final String error, final Switch ignore) {
        if (!ignore.getValue()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), getLabel(I18n
                            .tr(error)), I18n.tr("Error"), JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    @Override
    public void showMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), getLabel(I18n
                        .tr(message)), I18n.tr("Message"), JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    @Override
    public void showMessage(final String message, final Switch ignore) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), getLabel(I18n
                        .tr(message)), I18n.tr("Message"), JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    @Override
    public void showFormattedError(final String error, final Object... args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), getLabel(I18n.tr(
                        error, args)), I18n.tr("Error"), JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @Override
    public void showFormattedError(final String error, final Switch ignore, final Object... args) {
        if (!ignore.getValue()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(),
                            doNotDisplayAgainLabel(I18n.tr(error, args), ignore), I18n
                                    .tr("Error"), JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    @Override
    public void showFormattedMessage(final String message, final Object... args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), getLabel(I18n.tr(
                        message, args)), I18n.tr("Message"), JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    @Override
    public void showFormattedMessage(final String message, final Switch ignore,
            final Object... args) {
        if (!ignore.getValue()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(),
                            doNotDisplayAgainLabel(I18n.tr(message, args), ignore), I18n
                                    .tr("Message"), JOptionPane.INFORMATION_MESSAGE);
                }
            });
        }
    }

    private final JComponent doNotDisplayAgainLabel(final String message, final Switch setting) {
        JPanel thePanel = new JPanel(new BorderLayout(0, 15));
        JCheckBox option = new JCheckBox(I18n.tr("Do not display this message again"));
        JComponent lbl = getLabel(message);
        thePanel.add(lbl, BorderLayout.NORTH);
        thePanel.add(option, BorderLayout.WEST);
        option.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                setting.setValue(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        return thePanel;
    }

    private JComponent getLabel(String message) {
        if (message.startsWith("<html")) {
            HTMLLabel label = new HTMLLabel(message);
            label.setMargin(new Insets(5, 5, 5, 5));
            return label;
        } else { 
            return new MultiLineLabel(message, 400);
        }
    }

}
