package com.limegroup.gnutella.rudp;

import java.io.IOException;
import java.nio.channels.SelectableChannel;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.listener.AsynchronousEventBroadcaster;
import org.limewire.listener.AsynchronousMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.nio.NIODispatcher;
import org.limewire.rudp.RUDPContext;
import org.limewire.rudp.RUDPSettings;
import org.limewire.rudp.UDPMultiplexor;
import org.limewire.rudp.UDPSelectorProvider;
import org.limewire.rudp.UDPService;
import org.limewire.rudp.UDPSocketChannelConnectionEvent;
import org.limewire.rudp.messages.RUDPMessageFactory;
import org.limewire.rudp.messages.impl.DefaultMessageFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.limegroup.gnutella.rudp.messages.LimeWireGnutellaRudpMessageModule;


public class LimeWireGnutellaRudpModule extends AbstractModule {
    
    @Override
    protected void configure() {
        binder().install(new LimeWireGnutellaRudpMessageModule());

        ListeningExecutorService executorService = ExecutorsHelper.newProcessingQueue(UDPSocketChannelConnectionEvent.class.getSimpleName());
        AsynchronousMulticasterImpl<UDPSocketChannelConnectionEvent> connectionEventMulticaster = new AsynchronousMulticasterImpl<UDPSocketChannelConnectionEvent>(executorService); 
        bind(new TypeLiteral<AsynchronousEventBroadcaster<UDPSocketChannelConnectionEvent>>(){}).toInstance(connectionEventMulticaster);
        bind(new TypeLiteral<ListenerSupport<UDPSocketChannelConnectionEvent>>(){}).toInstance(connectionEventMulticaster);
        bind(RUDPContext.class).to(LimeRUDPContext.class);
        bind(UDPMultiplexor.class).toProvider(UDPMultiplexorProvider.class);
        bind(UDPService.class).to(LimeUDPService.class);
        bind(RUDPSettings.class).to(LimeRUDPSettings.class);
        bind(RUDPMessageFactory.class).annotatedWith(Names.named("delegate")).to(DefaultMessageFactory.class);        
    }
    
    @Singleton
    private static class UDPMultiplexorProvider extends AbstractLazySingletonProvider<UDPMultiplexor> {
        private final UDPSelectorProvider provider;
        private final NIODispatcher nioDispatcher;
        
        @Inject
        @SuppressWarnings("unused") public UDPMultiplexorProvider(UDPSelectorProvider provider,
                NIODispatcher nioDispatcher) {
            this.provider = provider;
            this.nioDispatcher = nioDispatcher;
        }
        
        @Override
        protected UDPMultiplexor createObject() {
            UDPMultiplexor multiplexor = provider.openSelector();
            SelectableChannel socketChannel = provider.openSocketChannel();
            try {
                socketChannel.close();
            } catch(IOException ignored) {}
            nioDispatcher.registerSelector(multiplexor, socketChannel.getClass());
            return multiplexor;
        }
    }

}
