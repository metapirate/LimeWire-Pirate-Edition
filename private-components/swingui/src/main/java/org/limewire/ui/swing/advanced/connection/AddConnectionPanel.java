package org.limewire.ui.swing.advanced.connection;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.io.NetworkUtils;
import org.limewire.ui.swing.components.DropDownListAutoCompleteControl;
import org.limewire.ui.swing.components.NumericTextField;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Display panel for the Add Connection function.
 */
public class AddConnectionPanel extends JPanel {

    /** Manager instance for connection data. */
    private GnutellaConnectionManager gnutellaConnectionManager;
    
    /** Action to add a connection. */
    private Action addAction = new AddAction();
    
    private JLabel titleLabel = new JLabel();
    private JLabel hostLabel = new JLabel();
    private JLabel portLabel = new JLabel();
    private JTextField hostTextField = new JTextField(20);
    private NumericTextField portTextField = new NumericTextField(4, 1, 0xFFFF);
    private JCheckBox tlsCheckBox = new JCheckBox();
    private JButton addButton = new JButton();
    
    /**
     * Constructs the AddConnectionPanel to display the Add Connection control.
     */
    @Inject
    public AddConnectionPanel(GnutellaConnectionManager gnutellaConnectionManager) {
        this.gnutellaConnectionManager = gnutellaConnectionManager;
        
        setBorder(BorderFactory.createTitledBorder(""));
        setLayout(new MigLayout("insets 0 0 0 0",
                "[left][left]",                      // col constraints
                "[top]12[top]6[top][top][bottom]")); // row constraints
        setOpaque(false);
        
        titleLabel.setText(I18n.tr("Add a Connection"));
        
        hostLabel.setText(I18n.tr("Host"));
        hostTextField.setMinimumSize(new Dimension(105, 21));
        hostTextField.setPreferredSize(new Dimension(105, 21));
        hostTextField.addActionListener(addAction);
        hostTextField.getDocument().addDocumentListener(new HostNameListener());
        
        portLabel.setText(I18n.tr("Port"));
        portTextField.setMinimumSize(new Dimension(60, 21));
        portTextField.setPreferredSize(new Dimension(60, 21));
        portTextField.setValue(6346);
        portTextField.addActionListener(addAction);

        // Install auto-complete list on host field.
        DropDownListAutoCompleteControl.install(hostTextField);
        
        // Install clipboard actions on text fields.
        TextFieldClipboardControl.install(hostTextField);
        TextFieldClipboardControl.install(portTextField);
        
        tlsCheckBox.setText(I18n.tr("Use TLS"));
        tlsCheckBox.setOpaque(false);

        addButton.setText(I18n.tr("Add"));
        addButton.setEnabled(false);
        addButton.addActionListener(addAction);
        
        add(titleLabel   , "cell 0 0 2 1");
        add(hostLabel    , "cell 0 1,gaptop 3");
        add(hostTextField, "cell 1 1");
        add(portLabel    , "cell 0 2,gaptop 3");
        add(portTextField, "cell 1 2");
        add(tlsCheckBox  , "cell 0 3 2 1");
        add(addButton    , "cell 1 4,right");
    }
    
    /**
     * Resets the input fields.
     */
    public void resetInput() {
        hostTextField.setText(null);
        portTextField.setText(null);
        tlsCheckBox.setSelected(false);
    }
    
    /**
     * Action to add a connection. 
     */
    private class AddAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            String hostnamestr = hostTextField.getText();

            // Look for the port in the host.
            int idx = hostnamestr.lastIndexOf(':');
            // If it exists, rewrite the host & port.
            if (idx != -1) {
                portTextField.setText(hostnamestr.substring(idx + 1));
                hostTextField.setText(hostnamestr.substring(0, idx));
                hostnamestr = hostTextField.getText();
            }

            // Convert port number to int.
            int portnum = portTextField.getValue(6346);

            // Verify port number is valid.
            if (!NetworkUtils.isValidPort(portnum)) {
                portnum = 6346;
            }

            // Update port number.
            portTextField.setValue(portnum);

            if (hostnamestr.trim().length() > 0) {
                // Establish connection to host and port.
                gnutellaConnectionManager.tryConnection(hostnamestr, portnum, 
                        tlsCheckBox.isSelected());
                // Reset input fields.
                resetInput();
                
            } else {
                hostTextField.requestFocusInWindow();
            }
        }
    }

    /**
     * Document listener for host name changes.
     */
    private class HostNameListener implements DocumentListener {

        @Override
        public void changedUpdate(DocumentEvent e) {
            addButton.setEnabled(e.getDocument().getLength() > 0);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            addButton.setEnabled(e.getDocument().getLength() > 0);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            addButton.setEnabled(e.getDocument().getLength() > 0);
        }
    }
}
