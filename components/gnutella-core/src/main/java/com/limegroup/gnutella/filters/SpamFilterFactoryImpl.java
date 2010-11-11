package com.limegroup.gnutella.filters;

import java.util.ArrayList;

import org.limewire.core.settings.FilterSettings;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
class SpamFilterFactoryImpl implements SpamFilterFactory {

    private final Provider<HostileFilter> hostileFilter;
    private final Provider<LocalIPFilter> ipFilter;
    private final Provider<URNFilter> urnFilter;
    private final Provider<RepetitiveQueryFilter> repetitiveQueryFilter;
    private final Provider<SameAltLocsFilter> sameAltLocsFilter;

    @Inject
    public SpamFilterFactoryImpl(Provider<HostileFilter> hostileFilter,
            Provider<LocalIPFilter> ipFilter,
            Provider<URNFilter> urnFilter,
            Provider<RepetitiveQueryFilter> repetitiveQueryFilter,
            Provider<SameAltLocsFilter> sameAltLocsFilter) {
        this.hostileFilter = hostileFilter;
        this.ipFilter = ipFilter;
        this.urnFilter = urnFilter;
        this.repetitiveQueryFilter = repetitiveQueryFilter;
        this.sameAltLocsFilter = sameAltLocsFilter;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.filters.SpamFilterFactory#createPersonalFilter()
     */
    public SpamFilter createPersonalFilter() {

        ArrayList<SpamFilter> buf = new ArrayList<SpamFilter>();

        //1. Hostile and locally blacklisted IP addresses.
        LocalIPFilter ipFilter = this.ipFilter.get();
        if(ipFilter.hasBlacklistedHosts())
            buf.add(ipFilter);

        //2. Queries matching banned keywords (responses are handled by the
        //   ResponseFilter pipeline to avoid dropping messages with a mixture
        //   of matching and non-matching responses).
        buf.add(new KeywordFilter());
        buf.add(new PhraseFilter());

        //3. Spammy replies. (TODO: do these still exist?)
        buf.add(new SpamReplyFilter());

        //4. URN filter.
        if(FilterSettings.FILTER_URNS.getValue())
            buf.add(urnFilter.get());

        //5. Query replies in which every response has similar alt-locs.
        buf.add(sameAltLocsFilter.get());

        return compose(buf);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.filters.SpamFilterFactory#createRouteFilter()
     */
    public SpamFilter createRouteFilter() {
        //Assemble spam filters. Order matters a little bit.

        ArrayList<SpamFilter> buf = new ArrayList<SpamFilter>();

        //1. Eliminate old LimeWire requeries. (TODO: do these still exist?)
        buf.add(new RequeryFilter());

        //1b. Eliminate runaway Qtrax queries. (TODO: do these still exist?)
        buf.add(new GUIDFilter());

        //2. Duplicate-based techniques.
        if (FilterSettings.FILTER_DUPLICATES.getValue()) {
            buf.add(new DuplicateFilter());
            buf.add(repetitiveQueryFilter.get());
        }

        //3. Greedy queries.  Yes, this is a route filter issue.
        if (FilterSettings.FILTER_GREEDY_QUERIES.getValue())
            buf.add(new GreedyQueryFilter());

        //4. Queries containing hash urns.
        if (FilterSettings.FILTER_HASH_QUERIES.getValue())
            buf.add(new HashFilter());

        //5. Hostile IP addresses.
        buf.add(hostileFilter.get());

        //6. Query replies with suspicious GUIDs.
        if (FilterSettings.CLIENT_GUID_FILTER.getValue())
            buf.add(new ClientGuidFilter());
        
        return compose(buf);
    }

    /**
     * Returns a composite filter of the given filters.
     * @param filters a ArrayList of SpamFilter.
     */
    private static SpamFilter compose(ArrayList<? extends SpamFilter> filters) {
        //As a minor optimization, we avoid a few method calls in
        //special cases.
        if (filters.size()==0)
            return new AllowFilter();
        else if (filters.size()==1)
            return filters.get(0);
        else {
            SpamFilter[] delegates = new SpamFilter[filters.size()];
            return new CompositeFilter(filters.toArray(delegates));
        }
    }
}
