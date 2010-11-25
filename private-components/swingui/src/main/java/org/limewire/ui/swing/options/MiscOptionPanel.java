package org.limewire.ui.swing.options;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Application;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.LanguageComboBox;
import org.limewire.ui.swing.components.NonNullJComboBox;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.friends.settings.FriendAccountConfiguration;
import org.limewire.ui.swing.friends.settings.FriendAccountConfigurationManager;
import org.limewire.ui.swing.settings.QuestionsHandler;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.LanguageUtils;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Misc Option View.
 */
public class MiscOptionPanel extends OptionPanel {

    private final Provider<FriendAccountConfigurationManager> accountManager;

    private NotificationsPanel notificationsPanel;
    private FriendsChatPanel friendsChatPanel;
    
    private Locale currentLanguage;
    private JLabel comboLabel;
    private final JComboBox languageDropDown;

    @Inject
    public MiscOptionPanel(Provider<FriendAccountConfigurationManager> accountManager, Application application) {
        this.accountManager = accountManager;
        
        GuiUtils.assignResources(this);

        setLayout(new MigLayout("nogrid, insets 15 15 15 15, fillx, gap 4"));

        comboLabel = new JLabel(I18n.tr("Language:"));
        languageDropDown = new LanguageComboBox();
        
        add(comboLabel);
        add(languageDropDown, "wrap");
        
        add(getNotificationsPanel(), "growx, wrap");
        add(getFriendChatPanel());
    }

    private OptionPanel getNotificationsPanel() {
        if(notificationsPanel == null) {
            notificationsPanel = new NotificationsPanel();
        }
        
        return notificationsPanel;
    }

    private OptionPanel getFriendChatPanel() {
        if(friendsChatPanel == null) {
            friendsChatPanel = new FriendsChatPanel();
        }
        
        return friendsChatPanel;
    }

    @Override
    ApplyOptionResult applyOptions() {
        
        Locale selectedLocale = (Locale) languageDropDown.getSelectedItem();
        
        ApplyOptionResult result = getNotificationsPanel().applyOptions();
        if (result.isSuccessful())
            result.applyResult(getFriendChatPanel().applyOptions());
                
        // if the language changed, always notify about a required restart
        if(selectedLocale != null && !currentLanguage.equals(selectedLocale) && result.isSuccessful()) {
            currentLanguage = selectedLocale;
            LanguageUtils.setLocale(selectedLocale);
            result.updateRestart(true);
        }
        return result;
    }

    @Override
    void setOptionTabItem(OptionTabItem tab) {
        super.setOptionTabItem(tab);
        getFriendChatPanel().setOptionTabItem(tab);
        getNotificationsPanel().setOptionTabItem(tab);
    }

    @Override
    boolean hasChanged() {
        Locale selectedLocale = (Locale) languageDropDown.getSelectedItem();
        
        return getNotificationsPanel().hasChanged() ||
            getFriendChatPanel().hasChanged() ||
            selectedLocale != currentLanguage;
    }

    @Override
    public void initOptions() {
        getNotificationsPanel().initOptions();
        getFriendChatPanel().initOptions();
        currentLanguage = LanguageUtils.getCurrentLocale();
        // if language got corrupted somehow, resave it
        // this shouldn't be possible but somehow currentLanguage can be 
        // null on OSX.
        if(currentLanguage == null) {
            LanguageUtils.setLocale(Locale.ENGLISH);
            currentLanguage = Locale.ENGLISH;
        }
        languageDropDown.setSelectedItem(currentLanguage);
    }

    private class NotificationsPanel extends OptionPanel {

        private JCheckBox showNotificationsCheckBox;
        private JButton resetWarningsButton;

        public NotificationsPanel() {
            super(tr("Notifications and Warnings"));

            showNotificationsCheckBox = new JCheckBox(tr("Show popup system notifications"));
            showNotificationsCheckBox.setContentAreaFilled(false);
            resetWarningsButton = new JButton(new AbstractAction(I18n.tr("Reset")){
                @Override
                public void actionPerformed(ActionEvent e) {
                    resetWarnings();
                }
            });
            
            add(showNotificationsCheckBox, "wrap");
            add(new JLabel(I18n.tr("Reset warning messages")));
            add(resetWarningsButton, "wrap");
        }

        @Override
        ApplyOptionResult applyOptions() {
            SwingUiSettings.SHOW_NOTIFICATIONS.setValue(showNotificationsCheckBox.isSelected());
            return new ApplyOptionResult(false, true);
        }

        @Override
        boolean hasChanged() {
            return showNotificationsCheckBox.isSelected() != SwingUiSettings.SHOW_NOTIFICATIONS.getValue();
        }

        @Override
        public void initOptions() {
            showNotificationsCheckBox.setSelected(SwingUiSettings.SHOW_NOTIFICATIONS.getValue());
        }
    }

    private class FriendsChatPanel extends OptionPanel implements SettingListener {

        private JCheckBox autoLoginCheckBox;
        private JComboBox serviceComboBox;
        private JLabel serviceLabel;
        private JTextField serviceField;
        private JTextField usernameField;
        private JPasswordField passwordField;

        public FriendsChatPanel() {
            super(tr("Friends and Chat"));
            
            SwingUiSettings.XMPP_AUTO_LOGIN.addSettingListener(this);

            autoLoginCheckBox = new JCheckBox(tr("Sign into Friends when LimeWire starts"));            
            autoLoginCheckBox.setContentAreaFilled(false);
            autoLoginCheckBox.addItemListener(new ItemListener(){
                @Override
                public void itemStateChanged(ItemEvent e) {
                    setComponentsEnabled(autoLoginCheckBox.isSelected());
                    if (autoLoginCheckBox.isSelected())
                        usernameField.requestFocusInWindow();
                }
            });

            serviceComboBox = new NonNullJComboBox();
            for(String label : accountManager.get().getLabels()) {
                if(!label.equals("Facebook")) {
                    serviceComboBox.addItem(label);
                }
            }
            serviceComboBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    populateInputs();
                }
            });
            serviceComboBox.setRenderer(new Renderer());
            serviceLabel = new JLabel(tr("Jabber Server:"));
            serviceField = new JTextField(18);
            usernameField = new JTextField(18);
            passwordField = new JPasswordField(18);
            
            TextFieldClipboardControl.install(serviceField);
            TextFieldClipboardControl.install(usernameField);
            TextFieldClipboardControl.install(passwordField);
            
            add(autoLoginCheckBox, "wrap");
            
            JPanel servicePanel = new JPanel(new MigLayout("insets 0, fill"));
            servicePanel.setOpaque(false);
            
            servicePanel.add(new JLabel(tr("Using:")), "gapleft 25");
            servicePanel.add(serviceComboBox, "wrap");

            servicePanel.add(serviceLabel, "gapleft 25, hidemode 3");
            servicePanel.add(serviceField, "hidemode 3, wrap");

            servicePanel.add(new JLabel(tr("Username:")), "gapleft 25");
            servicePanel.add(usernameField, "wrap");

            servicePanel.add(new JLabel(tr("Password:")), "gapleft 25");
            servicePanel.add(passwordField, "wrap");
            
            add(servicePanel);
            
        }

        private void populateInputs() {
            String label = (String)serviceComboBox.getSelectedItem();
            if(label.equals("Jabber")) {
                serviceLabel.setVisible(true);
                serviceField.setVisible(true);
            } else {
                serviceLabel.setVisible(false);
                serviceField.setVisible(false);
            }
            FriendAccountConfiguration config = accountManager.get().getConfig(label);
            if(config == accountManager.get().getAutoLoginConfig()) {
                serviceField.setText(config.getServiceName());
                usernameField.setText(config.getUserInputLocalID());
                passwordField.setText(config.getPassword());
            } else {
                serviceField.setText("");
                usernameField.setText("");
                passwordField.setText("");
            }
        }

        private void setComponentsEnabled(boolean enabled) {
            serviceComboBox.setEnabled(enabled);
            serviceField.setEnabled(enabled);
            usernameField.setEnabled(enabled);
            passwordField.setEnabled(enabled);
            if(!enabled) {
                serviceField.setText("");
                usernameField.setText("");
                passwordField.setText("");
            }
        }

        @Override
        ApplyOptionResult applyOptions() {
            if(hasChanged()) {
                if(autoLoginCheckBox.isSelected()) {
                    // Set this as the auto-login account
                    String user = usernameField.getText().trim();
                    String password = new String(passwordField.getPassword());
                    if (user.length() == 0) {
                        getOptionTabItem().select();
                        usernameField.requestFocusInWindow();
                        FocusJOptionPane.showMessageDialog(this, tr("Username cannot be blank."),
                                tr("Username"), JOptionPane.ERROR_MESSAGE);
                        return new ApplyOptionResult(false, false);
                    }
                    
                    if (password.length() == 0) {
                        getOptionTabItem().select();
                        passwordField.requestFocusInWindow();
                        FocusJOptionPane.showMessageDialog(this, tr("Password cannot be blank."),
                                tr("Password"), JOptionPane.ERROR_MESSAGE);
                        
                        return new ApplyOptionResult(false, false);
                    }
                    String label = (String)serviceComboBox.getSelectedItem();
                    FriendAccountConfiguration config = accountManager.get().getConfig(label);
                    if(label.equals("Jabber")) {
                        String service = serviceField.getText().trim();
                        if(service.isEmpty()) {
                            getOptionTabItem().select();
                            serviceField.requestFocusInWindow();
                            FocusJOptionPane.showMessageDialog(this, tr("Service cannot be blank."),
                                    tr("Service"), JOptionPane.ERROR_MESSAGE);
                            return new ApplyOptionResult(false, false);
                        }
                        config.setServiceName(service);
                    }
                    config.setUsername(user);
                    config.setPassword(password);
                    accountManager.get().setAutoLoginConfig(config);
                } else {
                    accountManager.get().setAutoLoginConfig(null);
                }
            }
            return new ApplyOptionResult(false, true);
        }

        @Override
        boolean hasChanged() {
            FriendAccountConfiguration auto = accountManager.get().getAutoLoginConfig();

            if(auto == null) {
                return autoLoginCheckBox.isSelected();
            } else {
                if(!autoLoginCheckBox.isSelected())
                    return true;
                String label = (String)serviceComboBox.getSelectedItem();
                if(!label.equals(auto.getLabel()))
                    return true;
                String serviceName = serviceField.getText().trim();
                if(!serviceName.equals(auto.getServiceName()))
                    return true;
                String username = usernameField.getText().trim();
                if(!username.equals(auto.getUserInputLocalID()))
                    return true;
                String password = new String(passwordField.getPassword());
                if(!password.equals(auto.getPassword()))
                    return true;
            }
            return false;
        }

        @Override
        public void initOptions() {
            FriendAccountConfiguration auto = accountManager.get().getAutoLoginConfig();
            if(auto == null) {
                serviceComboBox.setSelectedItem("Gmail");
                setComponentsEnabled(false);
                autoLoginCheckBox.setSelected(false);
            } else {
                serviceComboBox.setSelectedItem(auto.getLabel());
                setComponentsEnabled(true);
                autoLoginCheckBox.setSelected(true);
            }
            populateInputs();
        }
        
        @Override
        public void settingChanged(SettingEvent evt) {
            SwingUtils.invokeNowOrLater(new Runnable() {
                @Override
                public void run() {
                    initOptions();
                }
            });
        }

    }

    private class Renderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            FriendAccountConfiguration config = accountManager.get().getConfig(value.toString());
            if(config != null) {
                setIcon(config.getIcon());
            } else {
                setIcon(null);
            }
            return this;
        }
    }
    
    private static void resetWarnings() {
        QuestionsHandler.WARN_TORRENT_SEED_MORE.revertToDefault();
        QuestionsHandler.CONFIRM_BLOCK_HOST.revertToDefault();
        SwingUiSettings.WARN_DOWNLOAD_DANGEROUS.revertToDefault();
        SwingUiSettings.WARN_DOWNLOAD_SCAN_FAILED.revertToDefault();
        SwingUiSettings.WARN_DOWNLOAD_THREAT_FOUND.revertToDefault();
    }
}
