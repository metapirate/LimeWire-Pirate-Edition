package com.limegroup.gnutella.dht;

/**
 * Defines the interface to create DHT bootstrappers from a DHT controller.
 */
public interface DHTBootstrapperFactory {

    DHTBootstrapper createBootstrapper(DHTController dhtController);

}
