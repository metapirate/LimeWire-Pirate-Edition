package org.limewire.ui.swing.friends;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.GuiUtils;

import static org.limewire.ui.swing.util.I18n.tr;
import org.limewire.util.Objects;
import org.limewire.friend.api.FriendConnection;

import net.miginfocom.swing.MigLayout;

/**
 * Displays a dialog for adding a friend to the roster of an XMPP account.
 */
public class AddFriendDialog extends LimeJDialog {

    public AddFriendDialog(final FriendConnection connection) {
        super(GuiUtils.getMainFrame(), tr("Add Friend"));
        // The dialog can only be popped up when the user is signed in, so
        // connection should never be null
        Objects.nonNull(connection, "connection");
        setModalityType(ModalityType.MODELESS);
        setLayout(new MigLayout());
        setResizable(false);

        JLabel usernameLabel = new JLabel(tr("Username:"));
        JLabel nicknameLabel = new JLabel(tr("Nickname:"));
        final JTextField usernameField = new JTextField(18);
        final JTextField nicknameField = new JTextField(18);
        
        final Action okAction = new AbstractAction(tr("Add Friend")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!isEnabled()) {
                    return;
                }
                
                // If the user didn't enter a domain, use the service name
                String user = usernameField.getText().trim();
                if(user.indexOf('@') == -1)
                    user += "@" + connection.getConfiguration().getServiceName();
                final String username = user;
                // If the user didn't enter a nickname, use the username
                String nick = nicknameField.getText().trim();
                if(nick.equals(""))
                    nick = usernameField.getText();
                final String nickname = nick;
                setVisible(false);
                dispose();
                connection.addNewFriend(username, nickname);
            }
        };

        JButton ok = new JButton(okAction);
        okAction.setEnabled(false); // Disable until a username is entered
        usernameField.addActionListener(okAction);
        nicknameField.addActionListener(okAction);

        final JButton cancel = new JButton(tr("Cancel"));
        cancel.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });

        // Disable the OK button when the username field is empty
        usernameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                checkIfEmpty();
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                checkIfEmpty();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                checkIfEmpty();
            }
            
            private void checkIfEmpty() {
                okAction.setEnabled(!usernameField.getText().trim().equals(""));
            }
        });

        add(usernameLabel);
        add(usernameField, "span, wrap");
        add(nicknameLabel);
        add(nicknameField, "span, wrap");
        add(ok, "cell 1 2");
        add(cancel, "cell 2 2");
        
        

        pack();
        setLocationRelativeTo(GuiUtils.getMainFrame());
        setVisible(true);
    }
}