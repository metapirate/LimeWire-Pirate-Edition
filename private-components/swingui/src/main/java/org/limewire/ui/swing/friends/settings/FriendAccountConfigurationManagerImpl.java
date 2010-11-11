package org.limewire.ui.swing.friends.settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.core.api.xmpp.XMPPResourceFactory;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.PasswordManager;
import org.limewire.inject.LazySingleton;
import org.limewire.io.UnresolvedIpPort;
import org.limewire.io.UnresolvedIpPortImpl;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

@LazySingleton
public class FriendAccountConfigurationManagerImpl implements FriendAccountConfigurationManager {
    
    private final PasswordManager passwordManager;
    private final Map<String, FriendAccountConfiguration> configs; // Indexed by label
    private final String resource;
    
    private FriendAccountConfiguration autoLoginConfig = null;
    
    /**
     * If the login configs have been loaded yet.
     */
    private boolean loaded = false;
    
    @Resource private Icon gmailIconLarge;
    @Resource private Icon gmailIconSmall;
    @Resource private Icon ljIconLarge;
    @Resource private Icon ljIconSmall;
    @Resource private Icon otherIconLarge;
    @Resource private Icon otherIconSmall;
    
    @Inject
    public FriendAccountConfigurationManagerImpl(PasswordManager passwordManager,
            XMPPResourceFactory xmppResourceFactory) {
        
        GuiUtils.assignResources(this);
        this.passwordManager = passwordManager;
        configs = new HashMap<String, FriendAccountConfiguration>();
        resource = xmppResourceFactory.getResource();
    }

    /**
     * Loads the configs for the servers on demand.
     */
    private void init() {
        loadWellKnownServers();
        loadCustomServer();
        loadAutoLoginAccount();
        loaded = true;
    }
    
    /**
     * Used to get the config map, loading it if necessary.
     */
    private Map<String, FriendAccountConfiguration> getRawConfigs() {
        if (!loaded) {
            init();
        }
        
        return configs;
    }
    
    private void loadCustomServer() {
        String custom = SwingUiSettings.USER_DEFINED_JABBER_SERVICENAME.get();
        FriendAccountConfigurationImpl customConfig =
            new FriendAccountConfigurationImpl(custom, "Jabber", resource, Network.Type.XMPP, otherIconSmall, otherIconLarge);
        configs.put(customConfig.getLabel(), customConfig);
    }

    private void loadAutoLoginAccount() {
        String autoLogin = SwingUiSettings.XMPP_AUTO_LOGIN.get();
        if(!autoLogin.equals("")) {
            int comma = autoLogin.indexOf(',');
            try {
                String label = autoLogin.substring(0, comma);
                String username = autoLogin.substring(comma + 1);
                FriendAccountConfiguration config = configs.get(label);                        
                if(config != null) {
                    config.setUsername(username);
                    if(config.storePassword()) {
                        String password = passwordManager.loadPassword(username); 
                        config.setPassword(password);
                    }                    
                    autoLoginConfig = config;
                }
            } catch(IndexOutOfBoundsException ignored) {
                // Malformed string - no soup for you!
            } catch(IllegalArgumentException ignored) {
                // Empty username - no soup for you!
            } catch(IOException ignored) {
                // Error decrypting password - no soup for you!
            }
        }
    }

    private void loadWellKnownServers() {
        FriendAccountConfiguration gmail =
            new FriendAccountConfigurationImpl(true, "gmail.com", "Gmail", gmailIconSmall, gmailIconLarge, resource, getGTalkServers(), Network.Type.XMPP);
        configs.put(gmail.getLabel(), gmail);
        FriendAccountConfiguration livejournal =
            new FriendAccountConfigurationImpl(false, "livejournal.com", "LiveJournal", ljIconSmall, ljIconLarge, resource, getLiveJournalServers(), Network.Type.XMPP);
        configs.put(livejournal.getLabel(), livejournal);
    }

    private List<UnresolvedIpPort> getLiveJournalServers() {
        List<UnresolvedIpPort> defaultServers = new ArrayList<UnresolvedIpPort>(1);
        defaultServers.add(new UnresolvedIpPortImpl("xmpp.services.livejournal.com", 5222));
        return defaultServers;
    }

    private List<UnresolvedIpPort> getGTalkServers() {
        List<UnresolvedIpPort> defaultServers = new ArrayList<UnresolvedIpPort>(5);
        defaultServers.add(new UnresolvedIpPortImpl("talk.1.google.com", 5222));
        defaultServers.add(new UnresolvedIpPortImpl("talk1.1.google.com", 5222));
        defaultServers.add(new UnresolvedIpPortImpl("talk2.1.google.com", 5222));
        defaultServers.add(new UnresolvedIpPortImpl("talk3.1.google.com", 5222));
        defaultServers.add(new UnresolvedIpPortImpl("talk4.1.google.com", 5222));
        return defaultServers;
    }

    @Override
    public FriendAccountConfiguration getConfig(String label) {
        return getRawConfigs().get(label);
    }
    
    @Override
    public List<FriendAccountConfiguration> getConfigurations() {
        ArrayList<FriendAccountConfiguration> configurations = new ArrayList<FriendAccountConfiguration>(getRawConfigs().values());
        Collections.sort(configurations, new Comparator<FriendAccountConfiguration>() {
            @Override
            public int compare(FriendAccountConfiguration o1, FriendAccountConfiguration o2) {
                return o1.getLabel().compareToIgnoreCase(o2.getLabel());
            }
        });
        return configurations;
    }    
    
    @Override
    public List<String> getLabels() {
        ArrayList<String> labels = new ArrayList<String>();
        for(FriendAccountConfiguration config : getRawConfigs().values())
            labels.add(config.getLabel());
        Collections.sort(labels);
        return labels;
    }
    
    @Override
    public FriendAccountConfiguration getAutoLoginConfig() {
        if (!loaded) {
            init();
        }
        
        return autoLoginConfig;
    }
    
    @Override
    public void setAutoLoginConfig(FriendAccountConfiguration config) {
        // Remove the old configuration, if there is one
        if(autoLoginConfig != null) {
            passwordManager.removePassword(autoLoginConfig.getUserInputLocalID());
            SwingUiSettings.XMPP_AUTO_LOGIN.set("");
            SwingUiSettings.USER_DEFINED_JABBER_SERVICENAME.set("");
            autoLoginConfig = null;
        }
        // Store the new configuration, if there is one
        if(config != null) {
            try {
                if(config.storePassword()) {
                    passwordManager.storePassword(config.getUserInputLocalID(), config.getPassword());
                }
                SwingUiSettings.XMPP_AUTO_LOGIN.set(config.getLabel() + "," + config.getUserInputLocalID());
                if(config.getLabel().equals("Jabber"))
                    SwingUiSettings.USER_DEFINED_JABBER_SERVICENAME.set(config.getServiceName());
                autoLoginConfig = config;
            } catch (IllegalArgumentException ignored) {
                // Empty username or password - no soup for you!
            } catch (IOException ignored) {
                // Error encrypting password - no more Soup Nazi jokes for you!
            }
        }
    }
}
