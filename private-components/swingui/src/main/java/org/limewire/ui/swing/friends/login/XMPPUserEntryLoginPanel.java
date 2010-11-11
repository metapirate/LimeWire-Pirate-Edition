package org.limewire.ui.swing.friends.login;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.settings.FriendSettings;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.PromptPasswordField;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.friends.settings.FriendAccountConfiguration;
import org.limewire.ui.swing.friends.settings.FriendAccountConfigurationManager;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class XMPPUserEntryLoginPanel extends JPanel implements Disposable {
    
    private static final String CUSTOM_SERVICE_NAME = "Jabber";
    
    // Used for hack to show email/username depending on if the service is gmail or not
    private static final String GMAIL_SERVICE_NAME = "gmail.com";
    
    @Resource private Font headerTextFont;
    @Resource private Color headerTextForeground;
    @Resource private Font descriptionTextFont;
    @Resource private Color descriptionTextForeground;
    @Resource private Font inputTextFont;
    @Resource private Color inputTextForeground;
    @Resource private Font goBackTextFont;
    
    @Resource(key="XMPPUserEntryLoginPanel.authFailedLabel.foreground")
    private Color warningForeground;
    @Resource(key="XMPPUserEntryLoginPanel.authFailedLabel.font")
    private Font warningFont;
    @Resource(key="XMPPUserEntryLoginPanel.authFailedLabel.icon")
    private Icon warningIcon;
    
    @Resource(key="XMPPUserEntryLoginPanel.signInButton.foreground")
    private Color signInButtonForeground;
    @Resource(key="XMPPUserEntryLoginPanel.signInButton.font") 
    private Font signInButtonFont;
    @Resource(key="XMPPUserEntryLoginPanel.autoLoginCheckBox.icon") 
    private Icon autoLoginCheckBoxIcon;
    @Resource(key="XMPPUserEntryLoginPanel.autoLoginCheckBox.selectedIcon")
    private Icon autoLoginCheckBoxSelectedIcon;
    @Resource(key="XMPPUserEntryLoginPanel.autoLoginCheckBox.font") 
    private Font autoLoginCheckBoxFont;
    
    private static final String SIGNIN_ENABLED_TEXT = tr("Sign In");
    private static final String SIGNIN_DISABLED_TEXT = tr("Signing in...");
    private static final String AUTHENTICATION_ERROR = tr("Incorrect username or password.");
    private static final String NETWORK_ERROR = tr("Network error.");
    
    private PromptTextField serviceField;
    private PromptTextField usernameField;
    private PromptPasswordField passwordField;
    private JCheckBox autoLoginCheckBox;
    private JLabel authFailedLabel;
    private JXButton signInButton;
    private final SignInAction signinAction = new SignInAction();
    
    private ListenerSupport<FriendConnectionEvent> connectionSupport = null;
    
    private final FriendAccountConfiguration accountConfig;
    private final LoginPopupPanel parent;
    private final FriendAccountConfigurationManager accountManager;
    private EventListener<FriendConnectionEvent> connectionListener;
    private JLabel serviceLabel;
    private JComponent serviceRecenter;
    
    private boolean connectionHasBeenInitiated = false;
    private final FriendConnectionFactory friendConnectionFactory;
    
    @Inject
    public XMPPUserEntryLoginPanel(@Assisted FriendAccountConfiguration accountConfig, LoginPopupPanel parent,
            FriendAccountConfigurationManager accountManager,
            ButtonDecorator buttonDecorator,
            TextFieldDecorator textFieldDecorator,
            BarPainterFactory barPainterFactory,
            FriendConnectionFactory friendConnectionFactory) {
    
        super(new BorderLayout());
    
        this.accountConfig = accountConfig;
        this.parent = parent;
        this.accountManager = accountManager;
        this.friendConnectionFactory = friendConnectionFactory;
        
        GuiUtils.assignResources(this);
        
        JXPanel headerPanel = new JXPanel(new MigLayout("gap 10, insets 15 20 15 20"));
        headerPanel.setBackgroundPainter(barPainterFactory.createPopUpBarPainter());
        
        JLabel headerLabel = new JLabel(I18n.tr("Sign in with {0}",accountConfig.getLabel()));
        JLabel headerIcon = new JLabel(accountConfig.getLargeIcon());
        headerLabel.setFont(headerTextFont);
        headerLabel.setForeground(headerTextForeground);
        headerPanel.add(headerIcon);
        headerPanel.add(headerLabel);
        
        add(headerPanel, BorderLayout.NORTH);
        
        initComponents(buttonDecorator, textFieldDecorator);
        initServiceField();
        setSignInComponentsEnabled(true);
    }
    
    @Inject
    void registerListener(ListenerSupport<FriendConnectionEvent> connectionSupport) {
        
        this.connectionSupport = connectionSupport;
        
        connectionListener = new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTING:
                    connecting();
                    break;
                case CONNECTED:
                    connected();
                    break;
                case CONNECT_FAILED:
                    
                    // Do not show connect failed messages from before a
                    //  connection has been attempted
                    if (!connectionHasBeenInitiated) {
                        break;
                    }
                    
                    // Ignore duplicate events caused by authentication
                    // errors and events caused by deliberately signing
                    // out or switching user
                    Exception reason = event.getException();
                    if(reason != null) {
                        disconnected(reason);
                    }
                }
            }
        };
        
        connectionSupport.addListener(connectionListener);
    }
   
    private void initComponents(ButtonDecorator buttonDecorator,
            TextFieldDecorator textFieldDecorator) {
        
        serviceRecenter = new JPanel();
        ResizeUtils.forceSize(serviceRecenter, new Dimension(30,30));
        
        serviceLabel = new JLabel(tr("Domain"));
        serviceLabel.setFont(descriptionTextFont);
        serviceLabel.setForeground(descriptionTextForeground);
        JLabel usernameLabel = new JLabel(GMAIL_SERVICE_NAME.equals(accountConfig.getNetworkName()) ? tr("Email") : tr("Username"));
        usernameLabel.setFont(descriptionTextFont);
        usernameLabel.setForeground(descriptionTextForeground);
        JLabel passwordLabel = new JLabel(tr("Password"));
        passwordLabel.setFont(descriptionTextFont);
        passwordLabel.setForeground(descriptionTextForeground);
       
        /*
         * Populate the input fields on initialize. Otherwise, if the auto-login
         * option is selected in application settings and the user exits out of
         * the login panel with the auto-login option unchecked, settings are
         * cleared from application settings.
         */
        serviceField = new PromptTextField();
        textFieldDecorator.decoratePromptField(serviceField, AccentType.NONE);
        serviceField.setFont(inputTextFont);
        serviceField.setForeground(inputTextForeground);
        usernameField = new PromptTextField();
        textFieldDecorator.decoratePromptField(usernameField, AccentType.NONE);
        usernameField.setFont(inputTextFont);
        usernameField.setForeground(inputTextForeground);
        usernameField.setText(accountConfig.getUserInputLocalID());

        passwordField = new PromptPasswordField();
        textFieldDecorator.decoratePromptField(passwordField, AccentType.NONE);
        passwordField.setFont(inputTextFont);
        passwordField.setForeground(inputTextForeground);
        passwordField.setAction(signinAction);
        passwordField.setText(accountConfig.getPassword());

        ResizeUtils.forceSize(serviceField, new Dimension(224, 26));
        ResizeUtils.forceSize(usernameField, new Dimension(224, 26));
        ResizeUtils.forceSize(passwordField, new Dimension(224, 26));

        autoLoginCheckBox = new JCheckBox(tr("Sign in when I start LimeWire"), true);
        autoLoginCheckBox.setFont(autoLoginCheckBoxFont);
        autoLoginCheckBox.setIcon(autoLoginCheckBoxIcon);
        autoLoginCheckBox.setSelectedIcon(autoLoginCheckBoxSelectedIcon);
        autoLoginCheckBox.setSelected(accountManager.getAutoLoginConfig() != null);
        
        autoLoginCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                // When the user clears the auto-login checkbox,
                // forget the auto-login config
                if(!autoLoginCheckBox.isSelected()) {
                    accountManager.setAutoLoginConfig(null);
                }
                SwingUiSettings.REMEMBER_ME_CHECKED.setValue(autoLoginCheckBox.isSelected());
            }
        });
        autoLoginCheckBox.setOpaque(false);

        // Hack to get the enter key to start login for JCheckBox
        autoLoginCheckBox.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
            }
            @Override
            public void keyReleased(KeyEvent e) {
            }
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    login();
                }
            }
            
        });

        signInButton = new JXButton(signinAction);
        buttonDecorator.decorateGreenFullButton(signInButton);
        signInButton.setFont(signInButtonFont);
        signInButton.setForeground(signInButtonForeground);
        signInButton.setBorder(BorderFactory.createEmptyBorder(0,15,2,15));
        ResizeUtils.looseForceHeight(signInButton, 32);
        
        authFailedLabel = new JLabel();
        authFailedLabel.setVisible(false);
        authFailedLabel.setForeground(warningForeground);
        authFailedLabel.setFont(warningFont);
        authFailedLabel.setIcon(warningIcon);

        HyperlinkButton goBackButton = new HyperlinkButton(new AbstractAction(I18n.tr("Choose another account")) {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                parent.restart();                
            }
        });
        goBackButton.setFont(goBackTextFont);
        
        JPanel contentPanel = new JPanel(new MigLayout("gap 0, insets 10, align center"));
        
        contentPanel.add(authFailedLabel, "dock north, gapleft 2, hidemode 3, gapbottom 3, wrap");
        
        contentPanel.add(serviceLabel, "hidemode 3, wrap");
        contentPanel.add(serviceField, "gapbottom 10, hidemode 3, grow, wrap");
        contentPanel.add(usernameLabel, "wrap");
        contentPanel.add(usernameField, "gapbottom 10, grow, wrap");
        contentPanel.add(passwordLabel, "wrap");
        contentPanel.add(passwordField, "gapbottom 6, grow, wrap");
        contentPanel.add(autoLoginCheckBox, "gapbottom 10, wmin 0, wrap");
        contentPanel.add(signInButton, "wrap");
        contentPanel.add(serviceRecenter, "hidemode 3, wrap");
        
        JPanel bottomPanel = new JPanel(new MigLayout("insets 0, gap 0, align center"));
        bottomPanel.add(goBackButton, "gapbottom 10");
        
        add(contentPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void setSignInComponentsEnabled(boolean isEnabled) {
        signinAction.setEnabled(isEnabled);
        signinAction.putValue(Action.NAME, isEnabled ? SIGNIN_ENABLED_TEXT : SIGNIN_DISABLED_TEXT);
        serviceField.setEnabled(isEnabled);
        usernameField.setEnabled(isEnabled);
        passwordField.setEnabled(isEnabled);
        autoLoginCheckBox.setEnabled(isEnabled);
    }
    
    private void initServiceField() {
        if(accountConfig.getLabel().equals(CUSTOM_SERVICE_NAME)) {
            serviceLabel.setVisible(true);
            serviceField.setVisible(true);
            serviceRecenter.setVisible(false);
        } else {
            serviceLabel.setVisible(false);
            serviceField.setVisible(false);
            serviceRecenter.setVisible(true);
        }
    }
    
    private void login() {
        FriendSettings.EVER_TRIED_TO_SIGN_IN.setValue(true);
        String user = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if(user.equals("") || password.equals("")) {
            return;
        }            
        if(accountConfig.getLabel().equals("Jabber")) {
            String service = serviceField.getText().trim();
            if(service.equals(""))
                return;
            accountConfig.setServiceName(service);
        }
        accountConfig.setUsername(user);
        accountConfig.setPassword(password);
        if(autoLoginCheckBox.isSelected()) {
            // Set this as the auto-login account
            accountManager.setAutoLoginConfig(accountConfig);
        } else {
            // If there was previously an auto-login account, delete it
            accountManager.setAutoLoginConfig(null);
        }
        
        authFailedLabel.setVisible(false);
        validate();
        repaint();
        friendConnectionFactory.login(accountConfig);         
    }

    void connected() {
        FriendSettings.EVER_SIGNED_IN.setValue(true);
        parent.finished();
    }

    void disconnected(Exception reason) {
        setSignInComponentsEnabled(true);
        if(reason !=null && reason.getMessage() != null && reason.getMessage().toLowerCase(Locale.US).contains("auth")) {
            authFailedLabel.setText(AUTHENTICATION_ERROR);
            passwordField.setText("");
        } else {
            authFailedLabel.setText(NETWORK_ERROR);
        }
        authFailedLabel.setVisible(true);
    }
    
    public void connecting() {
        connectionHasBeenInitiated = true;
        setSignInComponentsEnabled(false);
    }
    
    @Override
    public boolean requestFocusInWindow() {
        if (serviceField.isVisible()) {
            return serviceField.requestFocusInWindow();
        }
        else {
            return usernameField.requestFocusInWindow();
        }
    }
    
    class SignInAction extends AbstractAction {
        public SignInAction() {
            super();
        }

        public void actionPerformed(ActionEvent e) {
            login();
        }
    }

    @Override
    public void dispose() {
        connectionSupport.removeListener(connectionListener);
    }
}
