package org.limewire.ui.swing.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class YesNoCheckBoxDialog extends LimeJDialog {
    public static final String YES_COMMAND = "YES";

    public static final String NO_COMMAND = "NO";

    private JButton yesButton = null;

    private JButton noButton = null;

    private JCheckBox checkBox;
    
    private String selectedCommand;

    public YesNoCheckBoxDialog(String message, String checkBoxMessage, boolean checked) {
        this(message, checkBoxMessage, checked,I18n.tr("Yes"), I18n.tr("No"));
    }

    public YesNoCheckBoxDialog(String message, String checkBoxMessage, boolean checked,
            String yesLabel, String noLabel) {
        this("", message, checkBoxMessage, checked, yesLabel, noLabel);
    }
    
    public YesNoCheckBoxDialog(String title, String message, String checkBoxMessage, boolean checked,
            String yesLabel, String noLabel) {
        super(GuiUtils.getMainFrame(), title);
        setModalityType(ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel();
        MultiLineLabel messageLabel = new MultiLineLabel(message, 350);

        checkBox = new JCheckBox(checkBoxMessage);
        checkBox.setSelected(checked);

        yesButton = new JButton(yesLabel);
        yesButton.setActionCommand(YES_COMMAND);
        yesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                selectedCommand = YES_COMMAND;
                YesNoCheckBoxDialog.this.dispose();
            }
        });
        noButton = new JButton(noLabel);
        noButton.setActionCommand(NO_COMMAND);
        noButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedCommand = NO_COMMAND;
                YesNoCheckBoxDialog.this.dispose();
            }
        });
        panel.setLayout(new MigLayout("", "", ""));
        panel.add(messageLabel, "wrap");
        panel.add(checkBox, "wrap");
        panel.add(yesButton, "alignx right");
        panel.add(noButton, "alignx right");

        setContentPane(panel);
        pack();

    }

    public void addActionListener(ActionListener actionListener) {
        yesButton.addActionListener(actionListener);
        noButton.addActionListener(actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        yesButton.removeActionListener(actionListener);
        noButton.removeActionListener(actionListener);
    }

    public synchronized boolean isCheckBoxSelected() {
        return checkBox.isSelected();
    }
    
    /**
     * @return true if yes was clicked
     */
    public boolean isConfirmed(){
        return selectedCommand == YES_COMMAND;
    }
}
