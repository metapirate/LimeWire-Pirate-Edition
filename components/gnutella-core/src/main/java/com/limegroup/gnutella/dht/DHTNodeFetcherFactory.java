package com.limegroup.gnutella.dht;

/**
 * Defines an interface to create a {@link DHTNodeFetcher}.
 */
public interface DHTNodeFetcherFactory {

    DHTNodeFetcher createNodeFetcher(DHTBootstrapper dhtBootstrapper);

}
