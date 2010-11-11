package org.limewire.friend.api;

import java.util.concurrent.Executor;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.friend.impl.address.FriendAddressRegistry;
import org.limewire.friend.impl.address.FriendAddressResolver;
import org.limewire.friend.impl.address.FriendAddressSerializer;
import org.limewire.listener.AsynchronousEventBroadcaster;
import org.limewire.listener.AsynchronousCachingEventMulticasterImpl;
import org.limewire.listener.AsynchronousEventMulticaster;
import org.limewire.listener.BroadcastPolicy;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.LogFactory;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class LimeWireFriendModule extends AbstractModule {

    @Override
    protected void configure() {
        
        EventMulticaster<FriendEvent> knownMulticaster = new EventMulticasterImpl<FriendEvent>(FriendEvent.class);
        EventMulticaster<FriendEvent> availMulticaster = new EventMulticasterImpl<FriendEvent>();
        EventMulticaster<FriendPresenceEvent> presenceMulticaster = new EventMulticasterImpl<FriendPresenceEvent>();
        EventMulticaster<FeatureEvent> featureMulticaster = new EventMulticasterImpl<FeatureEvent>();

        bind(new TypeLiteral<ListenerSupport<FriendEvent>>(){}).annotatedWith(Names.named("known")).toInstance(knownMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FriendEvent>>(){}).annotatedWith(Names.named("known")).toInstance(knownMulticaster);

        bind(new TypeLiteral<ListenerSupport<FriendEvent>>(){}).annotatedWith(Names.named("available")).toInstance(availMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FriendEvent>>(){}).annotatedWith(Names.named("available")).toInstance(availMulticaster);

        bind(new TypeLiteral<ListenerSupport<FriendPresenceEvent>>(){}).toInstance(presenceMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FriendPresenceEvent>>(){}).toInstance(presenceMulticaster);

        bind(new TypeLiteral<ListenerSupport<FeatureEvent>>(){}).toInstance(featureMulticaster);
        bind(new TypeLiteral<EventMulticaster<FeatureEvent>>(){}).toInstance(featureMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FeatureEvent>>(){}).toInstance(featureMulticaster);

        Executor executor = ExecutorsHelper.newProcessingQueue("FriendConnectionEventThread");

        AsynchronousCachingEventMulticasterImpl<FriendConnectionEvent> asyncConnectionMulticaster =
            new AsynchronousCachingEventMulticasterImpl<FriendConnectionEvent>(executor, BroadcastPolicy.IF_NOT_EQUALS, LogFactory.getLog(FriendConnectionEvent.class));
        bind(new TypeLiteral<EventBean<FriendConnectionEvent>>(){}).toInstance(asyncConnectionMulticaster);
        bind(new TypeLiteral<EventMulticaster<FriendConnectionEvent>>(){}).toInstance(asyncConnectionMulticaster);
        bind(new TypeLiteral<AsynchronousEventMulticaster<FriendConnectionEvent>>(){}).toInstance(asyncConnectionMulticaster);
        bind(new TypeLiteral<AsynchronousEventBroadcaster<FriendConnectionEvent>>(){}).toInstance(asyncConnectionMulticaster);
        bind(new TypeLiteral<ListenerSupport<FriendConnectionEvent>>(){}).toInstance(asyncConnectionMulticaster);

        EventMulticaster<LibraryChangedEvent> libraryChangedMulticaster = new EventMulticasterImpl<LibraryChangedEvent>();
        bind(new TypeLiteral<EventBroadcaster<LibraryChangedEvent>>(){}).toInstance(libraryChangedMulticaster);
        bind(new TypeLiteral<ListenerSupport<LibraryChangedEvent>>(){}).toInstance(libraryChangedMulticaster);

        // bind eagerly, so it registers itself with SocketsManager
        bind(FriendAddressResolver.class).asEagerSingleton();
        // ditto
        bind(FriendAddressSerializer.class).asEagerSingleton();

        bind(FriendAddressRegistry.class);
    }
}
