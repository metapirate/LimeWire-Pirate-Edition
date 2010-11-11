package org.limewire.ui.swing.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.ui.swing.components.NumericTextField;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Proxy Option View.
 */
public class ProxyOptionPanel extends OptionPanel {

    private final Provider<TorrentManager> torrentManager;
    private final TorrentManagerSettings torrentSettings;
    
    private JRadioButton noProxyRadioButton;
    private JRadioButton socksV4RadionButton;
    private JRadioButton socksV5RadioButton;
    private JRadioButton httpRadioButton;
    
    private ButtonGroup buttonGroup;
    
    private JTextField proxyTextField;
    private NumericTextField portTextField;
    
    private JCheckBox authenticationCheckBox;
    private JTextField userNameTextField;
    private JPasswordField passwordField;
    
    private JLabel proxyLabel;
    private JLabel portLabel;
    private JLabel enableLabel;
    private JLabel userNameLabel;
    private JLabel passwordLabel;
    
    @Inject
    public ProxyOptionPanel(Provider<TorrentManager> torrentManager, @TorrentSettingsAnnotation TorrentManagerSettings torrentSettings) {
        super();
        this.torrentManager = torrentManager;
        this.torrentSettings = torrentSettings;
        
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        setOpaque(false);
        
        add(getProxyPanel(), "pushx, growx");
    }
    
    private JPanel getProxyPanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(""));
        p.setLayout(new MigLayout("gapy 10"));
        p.setOpaque(false);
        
        noProxyRadioButton = new JRadioButton(I18n.tr("No Proxy"));
        noProxyRadioButton.addItemListener(new ProxyButtonListener());
        socksV4RadionButton = new JRadioButton(I18n.tr("Socks v4"));
        socksV4RadionButton.addItemListener(new ProxyButtonListener());
        socksV5RadioButton = new JRadioButton(I18n.tr("Socks v5"));
        socksV5RadioButton.addItemListener(new ProxyButtonListener());
        httpRadioButton = new JRadioButton(I18n.tr("HTTP"));
        httpRadioButton.addItemListener(new ProxyButtonListener());
        
        noProxyRadioButton.setOpaque(false);
        socksV4RadionButton.setOpaque(false);
        socksV5RadioButton.setOpaque(false);
        httpRadioButton.setOpaque(false);
        
        buttonGroup = new ButtonGroup();
        buttonGroup.add(noProxyRadioButton);
        buttonGroup.add(socksV4RadionButton);
        buttonGroup.add(socksV5RadioButton);
        buttonGroup.add(httpRadioButton);
        
        proxyTextField = new JTextField(15);
        TextFieldClipboardControl.install(proxyTextField);
        portTextField = new NumericTextField(5, 0, 0xFFFF);
        authenticationCheckBox = new JCheckBox();
        authenticationCheckBox.setOpaque(false);
        authenticationCheckBox.addItemListener(new ProxyButtonListener());
        userNameTextField = new JTextField(15);
        TextFieldClipboardControl.install(userNameTextField);
        passwordField = new JPasswordField(15);
        TextFieldClipboardControl.install(passwordField);
        
        proxyLabel = new JLabel(I18n.tr("Proxy:"));
        portLabel = new JLabel(I18n.tr("Port:"));
        enableLabel = new JLabel(I18n.tr("Enable Authentication"));
        userNameLabel = new JLabel(I18n.tr("Username:"));
        passwordLabel = new JLabel(I18n.tr("Password:"));
        
        p.add(noProxyRadioButton, "split, wrap");
        
        p.add(socksV4RadionButton, "split, wrap");
        
        p.add(socksV5RadioButton, "split, wrap");
        
        p.add(httpRadioButton, "split, wrap");
        
        p.add(proxyLabel, "split");
        p.add(proxyTextField, "gap unrelated");
        p.add(portLabel);
        p.add(portTextField, "wrap");
        
        p.add(authenticationCheckBox, "gapleft 25, split");
        p.add(enableLabel, "wrap");
        
        p.add(userNameLabel, "gapleft 25, split");
        p.add(userNameTextField, "wrap");
        p.add(passwordLabel, "gapleft 25, split");
        p.add(passwordField);
        
        return p;
    }
    
    @Override
    ApplyOptionResult applyOptions() {
        int connectionMethod = ConnectionSettings.C_NO_PROXY;

        if (socksV4RadionButton.isSelected())
            connectionMethod = ConnectionSettings.C_SOCKS4_PROXY;
        else if (socksV5RadioButton.isSelected())
            connectionMethod = ConnectionSettings.C_SOCKS5_PROXY;
        else if (httpRadioButton.isSelected())
            connectionMethod = ConnectionSettings.C_HTTP_PROXY;

        int oldProxyPort = ConnectionSettings.PROXY_PORT.getValue();
        final int proxyPort = portTextField.getValue(oldProxyPort);
        final String proxy = proxyTextField.getText();

        ConnectionSettings.PROXY_PORT.setValue(proxyPort);
        ConnectionSettings.CONNECTION_METHOD.setValue(connectionMethod);
        ConnectionSettings.PROXY_HOST.set(proxy);
        
        ConnectionSettings.PROXY_USERNAME.set(userNameTextField.getText());
        ConnectionSettings.PROXY_PASS.set(new String(passwordField.getPassword()));
        ConnectionSettings.PROXY_AUTHENTICATE.setValue(authenticationCheckBox.isSelected());
        
        if(torrentManager.get().isInitialized() && torrentManager.get().isValid()) {
            BackgroundExecutorService.execute(new Runnable() {
               @Override
                public void run() {
                   torrentManager.get().setTorrentManagerSettings(torrentSettings);
                } 
            });
        }
        return new ApplyOptionResult(false, true);
    }

    @Override
    boolean hasChanged() {
        int oldProxyPort = ConnectionSettings.PROXY_PORT.getValue();
        if(oldProxyPort != portTextField.getValue(oldProxyPort))
            return true;
        if(!ConnectionSettings.PROXY_HOST.get().equals(proxyTextField.getText()))
            return true;
        
        // authentication:
        if(ConnectionSettings.PROXY_AUTHENTICATE.getValue() != authenticationCheckBox.isSelected())
            return true;
        if(!ConnectionSettings.PROXY_USERNAME.get().equals(userNameTextField.getText()))
            return true;
        if(!ConnectionSettings.PROXY_PASS.get().equals(passwordField.getPassword().toString()))
            return true;
        
        // connection style
        switch(ConnectionSettings.CONNECTION_METHOD.getValue()) {
            case ConnectionSettings.C_SOCKS4_PROXY:
                return !socksV4RadionButton.isSelected();
            case ConnectionSettings.C_SOCKS5_PROXY:
                return !socksV5RadioButton.isSelected();
            case ConnectionSettings.C_HTTP_PROXY:
                return !httpRadioButton.isSelected();
            case ConnectionSettings.C_NO_PROXY:
                return !noProxyRadioButton.isSelected();
            default:
                return true;
        }
    }

    @Override
    public void initOptions() {
        int connectionMethod = ConnectionSettings.CONNECTION_METHOD.getValue();
        String proxy = ConnectionSettings.PROXY_HOST.get();
        int proxyPort = ConnectionSettings.PROXY_PORT.getValue();

        noProxyRadioButton.setSelected(connectionMethod == ConnectionSettings.C_NO_PROXY);
        socksV4RadionButton.setSelected(connectionMethod == ConnectionSettings.C_SOCKS4_PROXY);
        socksV5RadioButton.setSelected(connectionMethod == ConnectionSettings.C_SOCKS5_PROXY);
        httpRadioButton.setSelected(connectionMethod == ConnectionSettings.C_HTTP_PROXY);
        
        proxyTextField.setText(proxy);
        portTextField.setValue(proxyPort);
        
        //authentication
        authenticationCheckBox.setSelected(ConnectionSettings.PROXY_AUTHENTICATE.getValue());
        userNameTextField.setText(ConnectionSettings.PROXY_USERNAME.get());
        passwordField.setText(ConnectionSettings.PROXY_PASS.get());
        
        updateState();
    }
    
    private void updateState() {
        if(noProxyRadioButton.isSelected()) {
            updateProxy(false);
            updateAuthentication(false);
        } else {
            updateProxy(true);
            if(httpRadioButton.isSelected()) {
                updateAuthentication(false);
            } else {
                updateAuthentication(true);
            }
        }
    }
    
    private void updateProxy(boolean value) {
        proxyTextField.setEnabled(value);
        portTextField.setEnabled(value);
        
        proxyLabel.setVisible(value);
        proxyTextField.setVisible(value);
        portLabel.setVisible(value);
        portTextField.setVisible(value);
    }
    
    private void updateAuthentication(boolean value) {
        authenticationCheckBox.setEnabled(value);
        authenticationCheckBox.setVisible(value);
        enableLabel.setVisible(value);
        
        if(authenticationCheckBox.isSelected() && authenticationCheckBox.isEnabled()) {
            userNameTextField.setEnabled(value);
            passwordField.setEnabled(value);
            
            userNameLabel.setVisible(value);
            userNameTextField.setVisible(value);
            if(socksV4RadionButton.isSelected()) {
                passwordLabel.setVisible(false);
                passwordField.setVisible(false);
            } else {
                passwordLabel.setVisible(value);
                passwordField.setVisible(value);
            }
        } else {
            userNameTextField.setEnabled(false);
            passwordField.setEnabled(false);
            
            userNameLabel.setVisible(false);
            userNameTextField.setVisible(false);
            passwordLabel.setVisible(false);
            passwordField.setVisible(false);
        }
    }
    
    private class ProxyButtonListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            updateState();
        }
    }
}
