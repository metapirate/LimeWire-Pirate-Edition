package org.limewire.xmpp.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.friend.api.FileOfferEvent;
import org.limewire.friend.api.FriendRequestEvent;
import org.limewire.friend.api.RosterEvent;
import org.limewire.friend.impl.LimeWireFriendXmppModule;
import org.limewire.listener.AsynchronousMulticasterImpl;
import org.limewire.listener.BroadcastPolicy;
import org.limewire.listener.CachingEventMulticasterImpl;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.AsynchronousEventBroadcaster;
import org.limewire.xmpp.activity.XmppActivityEvent;
import org.limewire.xmpp.api.client.JabberSettings;
import org.limewire.xmpp.client.impl.ConnectionConfigurationFactory;
import org.limewire.xmpp.client.impl.DNSConnectionConfigurationFactory;
import org.limewire.xmpp.client.impl.FallbackConnectionConfigurationFactory;
import org.limewire.xmpp.client.impl.XMPPConnectionFactoryImpl;
import org.limewire.xmpp.client.impl.XMPPConnectionImplFactory;
import org.limewire.xmpp.client.impl.XMPPFriendConnectionImpl;
import org.limewire.xmpp.client.impl.IdleTime;
import org.limewire.xmpp.client.impl.IdleTimeImpl;
import org.limewire.xmpp.client.impl.IdleStatusMonitor;
import org.limewire.xmpp.client.impl.IdleStatusMonitorFactory;
import org.limewire.xmpp.client.impl.messages.address.AddressIQListener;
import org.limewire.xmpp.client.impl.messages.address.AddressIQListenerFactory;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQListener;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQListenerFactory;
import org.limewire.xmpp.client.impl.messages.connectrequest.ConnectBackRequestIQListener;
import org.limewire.xmpp.client.impl.messages.connectrequest.ConnectBackRequestIQListenerFactory;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQListener;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQListenerFactory;
import org.limewire.xmpp.client.impl.messages.library.LibraryChangedIQListener;
import org.limewire.xmpp.client.impl.messages.library.LibraryChangedIQListenerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireXMPPModule extends AbstractModule {
    private final Class<? extends JabberSettings> jabberSettingsClass;
    
    public LimeWireXMPPModule(Class<? extends JabberSettings> jabberSettingsClass) {
        this.jabberSettingsClass = jabberSettingsClass;
    }
    
    @Override
    protected void configure() {
        install(new LimeWireFriendXmppModule());
        
        if(jabberSettingsClass != null) {
            bind(JabberSettings.class).to(jabberSettingsClass);
        }
        bind(XMPPConnectionFactoryImpl.class);

        Executor executor = ExecutorsHelper.newProcessingQueue("XMPPEventThread");
        
        AsynchronousMulticasterImpl<RosterEvent> rosterMulticaster = new AsynchronousMulticasterImpl<RosterEvent>(executor); 
        bind(new TypeLiteral<AsynchronousEventBroadcaster<RosterEvent>>(){}).toInstance(rosterMulticaster);
        bind(new TypeLiteral<ListenerSupport<RosterEvent>>(){}).toInstance(rosterMulticaster);

        EventMulticaster<FileOfferEvent> fileOfferMulticaster = new EventMulticasterImpl<FileOfferEvent>(); 
        bind(new TypeLiteral<EventBroadcaster<FileOfferEvent>>(){}).toInstance(fileOfferMulticaster);
        bind(new TypeLiteral<ListenerSupport<FileOfferEvent>>(){}).toInstance(fileOfferMulticaster);

        EventMulticaster<FriendRequestEvent> friendRequestMulticaster = new EventMulticasterImpl<FriendRequestEvent>(); 
        bind(new TypeLiteral<EventBroadcaster<FriendRequestEvent>>(){}).toInstance(friendRequestMulticaster);
        bind(new TypeLiteral<ListenerSupport<FriendRequestEvent>>(){}).toInstance(friendRequestMulticaster);

        EventMulticaster<XmppActivityEvent> activityMulticaster = new CachingEventMulticasterImpl<XmppActivityEvent>(BroadcastPolicy.IF_NOT_EQUALS); 
        bind(new TypeLiteral<EventBroadcaster<XmppActivityEvent>>(){}).toInstance(activityMulticaster);
        bind(new TypeLiteral<ListenerSupport<XmppActivityEvent>>(){}).toInstance(activityMulticaster);

        List<ConnectionConfigurationFactory> connectionConfigurationFactories = new ArrayList<ConnectionConfigurationFactory>(2);
        connectionConfigurationFactories.add(new DNSConnectionConfigurationFactory());
        connectionConfigurationFactories.add(new FallbackConnectionConfigurationFactory());
        bind(new TypeLiteral<List<ConnectionConfigurationFactory>>(){}).toInstance(connectionConfigurationFactories);
        
        bind(XMPPConnectionImplFactory.class).toProvider(FactoryProvider.newFactory(XMPPConnectionImplFactory.class, XMPPFriendConnectionImpl.class));
        
        bind(AddressIQListenerFactory.class).toProvider(FactoryProvider.newFactory(AddressIQListenerFactory.class, AddressIQListener.class));
        bind(AuthTokenIQListenerFactory.class).toProvider(FactoryProvider.newFactory(AuthTokenIQListenerFactory.class, AuthTokenIQListener.class));
        bind(ConnectBackRequestIQListenerFactory.class).toProvider(FactoryProvider.newFactory(ConnectBackRequestIQListenerFactory.class, ConnectBackRequestIQListener.class));
        bind(LibraryChangedIQListenerFactory.class).toProvider(FactoryProvider.newFactory(LibraryChangedIQListenerFactory.class, LibraryChangedIQListener.class));
        bind(FileTransferIQListenerFactory.class).toProvider(FactoryProvider.newFactory(FileTransferIQListenerFactory.class, FileTransferIQListener.class));
        
                
        bind(IdleTime.class).to(IdleTimeImpl.class);
        
        bind(IdleStatusMonitorFactory.class).toProvider(
                FactoryProvider.newFactory(
                        IdleStatusMonitorFactory.class, IdleStatusMonitor.class));
    }
}
