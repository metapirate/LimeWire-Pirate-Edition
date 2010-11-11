package com.limegroup.gnutella.statistics;

import org.limewire.statistic.BasicKilobytesStatistic;
import org.limewire.statistic.Statistic;
import org.limewire.statistic.StatisticAccumulator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class TcpBandwidthStatisticsImpl implements TcpBandwidthStatistics {

    private final StatisticAccumulator statisticAccumulator;

    private final Statistic UPSTREAM;
    private final Statistic DOWNSTREAM;
    private final Statistic HTTP_DOWNSTREAM;
    private final Statistic HTTP_UPSTREAM;
    private final Statistic HTTP_DOWNSTREAM_INNETWORK;
    private final Statistic HTTP_UPSTREAM_INNETWORK;
    private final Statistic HTTP_HEADER_DOWNSTREAM;
    private final Statistic HTTP_HEADER_DOWNSTREAM_INNETWORK;
    private final Statistic HTTP_BODY_DOWNSTREAM;
    private final Statistic HTTP_BODY_DOWNSTREAM_INNETWORK;
    private final Statistic HTTP_HEADER_UPSTREAM;
    private final Statistic HTTP_HEADER_UPSTREAM_INNETWORK;
    private final Statistic HTTP_BODY_UPSTREAM;
    private final Statistic HTTP_BODY_UPSTREAM_INNETWORK;

    @Inject
    TcpBandwidthStatisticsImpl(StatisticAccumulator statisticAccumulator) {
        this.statisticAccumulator = statisticAccumulator;
        UPSTREAM = new BandwidthStat();
        DOWNSTREAM = new BandwidthStat();
        HTTP_DOWNSTREAM = new DelegateStat(DOWNSTREAM);
        HTTP_UPSTREAM = new DelegateStat(UPSTREAM);

        // In-network stats purposely don't add into the global HTTP,
        // otherwise the stats in the UI would look very strange showing
        // bandwidth w/o a visible download
        HTTP_DOWNSTREAM_INNETWORK = new DelegateStat(DOWNSTREAM);
        HTTP_UPSTREAM_INNETWORK = new DelegateStat(UPSTREAM);
        HTTP_HEADER_DOWNSTREAM_INNETWORK = new DelegateStat(HTTP_DOWNSTREAM_INNETWORK);
        HTTP_BODY_DOWNSTREAM_INNETWORK = new DelegateStat(HTTP_DOWNSTREAM_INNETWORK);
        HTTP_HEADER_UPSTREAM_INNETWORK = new DelegateStat(HTTP_UPSTREAM_INNETWORK);
        HTTP_BODY_UPSTREAM_INNETWORK = new DelegateStat(HTTP_UPSTREAM_INNETWORK);

        HTTP_HEADER_DOWNSTREAM = new DelegateStat(HTTP_DOWNSTREAM);
        HTTP_BODY_DOWNSTREAM = new DelegateStat(HTTP_DOWNSTREAM);
        HTTP_HEADER_UPSTREAM = new DelegateStat(HTTP_UPSTREAM);
        HTTP_BODY_UPSTREAM = new DelegateStat(HTTP_UPSTREAM);
    }

    public Statistic getStatistic(StatisticType statisticType) {
        switch (statisticType) {
        case HTTP_BODY_DOWNSTREAM:
            return HTTP_BODY_DOWNSTREAM;
        case HTTP_BODY_INNETWORK_DOWNSTREAM:
            return HTTP_BODY_DOWNSTREAM_INNETWORK;
        case HTTP_BODY_INNETWORK_UPSTREAM:
            return HTTP_BODY_UPSTREAM_INNETWORK;
        case HTTP_BODY_UPSTREAM:
            return HTTP_BODY_UPSTREAM;
        case HTTP_HEADER_DOWNSTREAM:
            return HTTP_HEADER_DOWNSTREAM;
        case HTTP_HEADER_INNETWORK_DOWNSTREAM:
            return HTTP_HEADER_DOWNSTREAM_INNETWORK;
        case HTTP_HEADER_INNETWORK_UPSTREAM:
            return HTTP_HEADER_UPSTREAM_INNETWORK;
        case HTTP_HEADER_UPSTREAM:
            return HTTP_HEADER_UPSTREAM;
        default:
            throw new IllegalArgumentException("invalid statistic type: " + statisticType);
        }
    }

    public long getTotalDownstream() {
        return (long) DOWNSTREAM.getTotal();
    }

    public long getTotalUpstream() {
        return (long) UPSTREAM.getTotal();
    }

    public double getAverageHttpUpstream() {
        return HTTP_UPSTREAM.getAverage();
    }

    private class BandwidthStat extends BasicKilobytesStatistic {
        public BandwidthStat() {
            super(statisticAccumulator);
        }
    }

    private class DelegateStat extends BandwidthStat {
        private final Statistic delegate;

        DelegateStat(Statistic delegate) {
            this.delegate = delegate;
        }

        @Override
        public void addData(int data) {
            super.addData(data);
            delegate.addData(data);
        }
    }

}
