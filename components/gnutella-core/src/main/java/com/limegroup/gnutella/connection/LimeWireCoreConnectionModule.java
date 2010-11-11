package com.limegroup.gnutella.connection;

import org.limewire.inject.AbstractModule;

public class LimeWireCoreConnectionModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(RoutedConnectionFactory.class).to(RoutedConnectionFactoryImpl.class);
        bind(ConnectionCheckerManager.class).to(ConnectionCheckerManagerImpl.class);
        bind(MessageReaderFactory.class).to(MessageReaderFactoryImpl.class);
        bind(UDPConnectionChecker.class).to(UDPConnectionCheckerImpl.class);
        bind(ConnectionCapabilities.class).to(ConnectionCapabilitiesImpl.class);
        bind(ConnectionBandwidthStatistics.class).to(ConnectionBandwidthStatisticsImpl.class);
    }
}
