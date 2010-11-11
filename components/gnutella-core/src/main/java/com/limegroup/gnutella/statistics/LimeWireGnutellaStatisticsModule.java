package com.limegroup.gnutella.statistics;

import com.google.inject.AbstractModule;

public class LimeWireGnutellaStatisticsModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(OutOfBandStatistics.class).to(OutOfBandStatisticsImpl.class);
        bind(TcpBandwidthStatistics.class).to(TcpBandwidthStatisticsImpl.class);       
        bind(UptimeStatTimer.class);
    }
}
