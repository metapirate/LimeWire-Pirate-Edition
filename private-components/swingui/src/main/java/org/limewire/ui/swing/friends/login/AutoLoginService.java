package org.limewire.ui.swing.friends.login;

import java.util.concurrent.ExecutionException;

import javax.swing.Action;
import javax.swing.SwingUtilities;

import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.friend.api.FriendException;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.friends.settings.FriendAccountConfiguration;
import org.limewire.ui.swing.friends.settings.FriendAccountConfigurationManager;

import com.google.inject.Inject;
import com.google.inject.Provider;

@EagerSingleton
public class AutoLoginService implements Service {
    
    private static final Log LOG = LogFactory.getLog(AutoLoginService.class);
    
    private final FriendAccountConfigurationManager accountManager;
    private volatile boolean hasAttemptedLogin = false;
    private final FriendConnectionFactory friendConnectionFactory;
    private final Provider<LoginPopupPanel> friendsSignInPanel;

    @Inject
    public AutoLoginService(FriendAccountConfigurationManager accountManager,
                            FriendConnectionFactory friendConnectionFactory,
                            Provider<LoginPopupPanel> friendsSignInPanel) {
        this.accountManager = accountManager;
        this.friendConnectionFactory = friendConnectionFactory;
        this.friendsSignInPanel = friendsSignInPanel;
    }
    
    /**
     * Used to identify whether or not this service will attempt to automatically login.
     */
    public boolean hasLoginConfig() {
        return accountManager.getAutoLoginConfig() != null;
    }
    
    /**
     * Whether or not the service has attempted a login yet.
     */
    public boolean hasAttemptedLogin() {
        return hasAttemptedLogin;
    }
    
    /**
     * If an auto login is in process.
     */
    public boolean isAttemptingLogin() {
        return !hasAttemptedLogin() && hasLoginConfig(); 
    }
    
    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Override
    public String getServiceName() {
        return "Auto-Login Serivce";
    }
    @Override
    public void initialize() {
    }
    
    @Override
    public void start() {
        // If there's an auto-login account, select it and log in
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if(hasLoginConfig()) {
                    final FriendAccountConfiguration config = accountManager.getAutoLoginConfig();
                    ListeningFuture<FriendConnection> connectionListenerFuture = friendConnectionFactory.login(config);
                    connectionListenerFuture.addFutureListener(new EventListener<FutureEvent<FriendConnection>>() {
                        @SwingEDTEvent
                        @Override
                        public void handleEvent(FutureEvent<FriendConnection> event) {
                            if(event.getType() == FutureEvent.Type.EXCEPTION) {
                                ExecutionException exception = event.getException();
                                if(exception.getCause() instanceof FriendException) {
                                    LoginPopupPanel loginPanel = friendsSignInPanel.get();
                                    loginPanel.setVisible(true);
                                    Action login = loginPanel.getServiceSelectionLoginPanel().getLoginActions().get(config);
                                    login.actionPerformed(null);
                                } else {
                                    LOG.debug("auto-login failed", exception);    
                                }
                            }
                        }
                    });   
                }
                hasAttemptedLogin = true;
            }
        });
    }
    
    @Override
    public void stop() {
    }
}
