package org.limewire.geocode;

import org.limewire.inject.AbstractModule;

/**
 * Main module for the geocoder component.
 */
public class LimewireGeocodeModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Geocoder.class).to(GeocoderImpl.class);
        bind(GeocodeInformation.class).toProvider(CachedGeoLocationImpl.class);
    }
}
