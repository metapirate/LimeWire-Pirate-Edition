package com.limegroup.gnutella.statistics;

import org.limewire.statistic.Statistic;

public interface TcpBandwidthStatistics {
    
    public static enum StatisticType {
        HTTP_HEADER_DOWNSTREAM, HTTP_HEADER_INNETWORK_DOWNSTREAM,
        HTTP_HEADER_UPSTREAM, HTTP_HEADER_INNETWORK_UPSTREAM,
        HTTP_BODY_DOWNSTREAM, HTTP_BODY_INNETWORK_DOWNSTREAM,
        HTTP_BODY_UPSTREAM, HTTP_BODY_INNETWORK_UPSTREAM;
    }
    
    public long getTotalUpstream();

    public long getTotalDownstream();

    public double getAverageHttpUpstream();
    
    public Statistic getStatistic(StatisticType statisticType);

}
