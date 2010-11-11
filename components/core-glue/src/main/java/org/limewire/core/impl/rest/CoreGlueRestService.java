package org.limewire.core.impl.rest;

import static org.limewire.rest.AuthorizationInterceptor.REMOTE_PREFIX;

import java.io.IOException;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.rest.AuthorizationInterceptor;
import org.limewire.rest.AuthorizationInterceptorFactory;
import org.limewire.rest.RestAuthority;
import org.limewire.rest.RestAuthorityFactory;
import org.limewire.rest.RestPrefix;
import org.limewire.rest.RestRequestHandlerFactory;
import org.limewire.rest.RestUtils;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.browser.LocalAcceptor;
import com.limegroup.gnutella.browser.LocalHTTPAcceptor;

/**
 * REST API service for the live core.
 */
@EagerSingleton
public class CoreGlueRestService implements Service {
    private static final Log LOG = LogFactory.getLog(CoreGlueRestService.class);

    private final Provider<LocalHTTPAcceptor> localHttpAcceptorFactory;
    private final Provider<LocalAcceptor> localAcceptorFactory;
    private final AuthorizationInterceptorFactory authorizationInterceptorFactory;
    private final RestAuthorityFactory restAuthorityFactory;
    private final RestRequestHandlerFactory restRequestHandlerFactory;
    
    private SettingListener localSettingListener;
    private AuthorizationInterceptor localAuthorizationInterceptor;
    
    @Inject
    public CoreGlueRestService(Provider<LocalHTTPAcceptor> localHttpAcceptorFactory,
            Provider<LocalAcceptor> localAcceptorFactory,
            AuthorizationInterceptorFactory authorizationInterceptorFactory,
            RestAuthorityFactory restAuthorityFactory,
            RestRequestHandlerFactory restRequestHandlerFactory) {
        this.localHttpAcceptorFactory = localHttpAcceptorFactory;
        this.localAcceptorFactory = localAcceptorFactory;
        this.authorizationInterceptorFactory = authorizationInterceptorFactory;
        this.restAuthorityFactory = restAuthorityFactory;
        this.restRequestHandlerFactory = restRequestHandlerFactory;
    }
    
    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Override
    public String getServiceName() {
        return I18nMarker.marktr("REST Service");
    }

    @Override
    public void initialize() {
    }

    @Override
    public void start() {
        // Update REST access secret.
        RestUtils.updateAccessSecret();
        
        // Install setting listener.
        if (localSettingListener == null) {
            localSettingListener = new SettingListener() {
                @Override
                public void settingChanged(SettingEvent evt) {
                    if (((BooleanSetting) evt.getSetting()).getValue()) {
                        registerLocalHandlers();
                    } else {
                        unregisterLocalHandlers();
                    }
                }
            };
            ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.addSettingListener(localSettingListener);
        }
        
        // Register request handlers if enabled.
        if (ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.getValue()) {
            registerLocalHandlers();
        }
    }

    @Override
    public void stop() {
        // Uninstall setting listener.
        if (localSettingListener != null) {
            ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.removeSettingListener(localSettingListener);
            localSettingListener = null;
        }
        
        unregisterLocalHandlers();
    }

    /**
     * Registers local handlers for all REST targets.
     */
    private void registerLocalHandlers() {
        try {
            // Create interceptor for authorization.
            if (localAuthorizationInterceptor == null) {
                String localUrl = "http://localhost";
                int port = localAcceptorFactory.get().getPort();
                String secret = RestUtils.getAccessSecret();
                RestAuthority localAuthority = restAuthorityFactory.create(localUrl, port, secret);
                localAuthorizationInterceptor = authorizationInterceptorFactory.create(localAuthority);
            }
            localHttpAcceptorFactory.get().addRequestInterceptor(localAuthorizationInterceptor);

            // Register REST handlers.
            for (RestPrefix restPrefix : RestPrefix.values()) {
                localHttpAcceptorFactory.get().registerHandler(createPattern(restPrefix.pattern()),
                        restRequestHandlerFactory.createRequestHandler(restPrefix));
            }
            
        } catch (IOException ex) {
            LOG.debugf(ex, "Unable to register REST handlers {0}", ex.getMessage());
        }
    }
    
    /**
     * Unregisters local handlers for all REST targets.
     */
    private void unregisterLocalHandlers() {
        // Unregister REST handlers.
        for (RestPrefix restPrefix : RestPrefix.values()) {
            localHttpAcceptorFactory.get().unregisterHandler(createPattern(restPrefix.pattern()));
        }
        
        // Remove authorization interceptor.
        localHttpAcceptorFactory.get().removeRequestInterceptor(localAuthorizationInterceptor);
        localAuthorizationInterceptor = null;
    }
    
    /**
     * Creates the remote access URI pattern for the specified target.
     */
    private String createPattern(String target) {
        return REMOTE_PREFIX + target + "*";
    }
}
