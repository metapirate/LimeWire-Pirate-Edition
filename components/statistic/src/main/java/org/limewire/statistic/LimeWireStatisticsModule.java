package org.limewire.statistic;

import com.google.inject.AbstractModule;


public class LimeWireStatisticsModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(StatisticAccumulator.class).to(StatisticsAccumulatorImpl.class);
    }
    
}
