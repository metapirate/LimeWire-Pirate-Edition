package org.limewire.geocode;



/**
 * Defines the interface for the finder and retriever or
 * {@link GeocodeInformationImpl}.
 * <p>
 * The portion of instances of this interface that implements
 * {@link SuccessOrFailureCallback<String>} should process Strings of the form:
 * 
 * <pre>
 * S    ::= T N Line*
 * T    ::= String
 * N    ::= String
 * Line ::= String(key) T String(value) N
 * </pre>
 *  producing a map.  Here is such an example:
 * 
 * <pre>
 *  CountryCode  US
 *  CountryCode3    USA
 *  CountryName United States
 *  Region  NY
 *  Region2 New York
 *  City    New York
 *  PostalCode  10004
 *  Latitude    40.6888
 *  Longitude   -74.0203
 *  DmaCode 501
 *  AreaCode    212
 * </pre>
 */
public interface Geocoder {

    /**
     * Returns geo code information.
     * 
     * @return empty {@link GeocodeInformation} if there was an error
     */
    GeocodeInformation getGeocodeInformation();

}
