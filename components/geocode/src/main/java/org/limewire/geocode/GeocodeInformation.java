package org.limewire.geocode;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

/**
 * Defines a class for geographic information created from a
 * {@link org.limewire.geocode.Geocoder} about the client. This is
 * basically a map from property names to values. Access to these values is
 * through the method {@link #getProperty(GeocodeInformation.Property)}.
 */
public class GeocodeInformation {

    private final Map<Property, String> names2values = new EnumMap<Property, String>(Property.class);

    /**
     * Maps {@link Property} names to values so that we can turn {@link String}s
     * into {@link Property Properties}. In order to use these without
     * specifically naming one you must make some reference to one of the enum
     * members. For example:
     * 
     * <pre>
     * Property.AreaCode.getValue();
     * </pre>
     * 
     * in {@link GeocodeInformation}.
     */
    private final static Map<String, Property> STRINGS2PROPERTIES = new HashMap<String, Property>();
    
    /**
     * Immutable empty geo code information.
     */
    public static final GeocodeInformation EMPTY_GEO_INFO = new GeocodeInformation() {
        @Override
        public void setProperty(Property arg0, String arg1) {
            throw new UnsupportedOperationException();
        }
        @Override
        public void setProperty(String arg0, String arg1) {
            throw new UnsupportedOperationException();
        }
    };

    /**
     * The various values.
     * 
     */
    public enum Property {

        /**
         * IP address -- e.g. <code>12.12.12.12</code>.
         */
        Ip("Ip"),

        /**
         * 2-letter country code -- e.g. <code>US</code>.
         */
        CountryCode("CountryCode"),

        /**
         * 3-letter country code -- e.g. <code>USs</code>.
         */
        CountryCode3("CountryCode3"),

        /**
         * Full country name -- e.g. <code>United States</code>.
         */
        CountryName("CountryName"),

        /**
         * Short region name -- e.g. <code>NY</code>.
         */
        Region("Region"),

        /**
         * Full region name -- e.g. <code>New York</code>.
         */
        Region2("Region2"),

        /**
         * Full city name -- e.g. <code>New York</code>.
         */
        City("City"),

        /**
         * Country-specific postal code -- e.g. <code>10004</code>.
         */
        PostalCode("PostalCode"),

        /**
         * Latitude in decimal degrees -- e.g. <code>40.6888</code>.
         */
        Latitude("Latitude"),

        /**
         * Longitude in decimal degrees -- e.g. <code>40.6888</code>.
         */
        Longitude("Longitude"),

        /**
         * Designated market area code (<a
         * href="http://en.wikipedia.org/wiki/Media_market">wikipedia</a>) --
         * e.g.<code>501</code>.
         */
        DmaCode("DmaCode"),

        /**
         * Country-specific area code -- e.g. <code>212</code>.
         */
        AreaCode("AreaCode");

        private final String s;

        Property(String s) {
            this.s = s;
            getStrings2Properties().put(s, this);
        }

        public String getValue() {
            return s;
        }
    }

    static {
        // We have to make reference to one so they resolve, in case we're
        // creating them only from GeocodeInformation.STRINGS2PROPERTIES
        Property.AreaCode.getValue();
    }

    /**
     * Returns the String value for {@link Property} or <code>null</code>.
     * 
     * @param p key value
     * @return the String value for {@link Property} or <code>null</code>
     */
    public String getProperty(Property p) {
        return names2values.get(p);
    }

    /**
     * @return whether or not there is no information in this object
     */
    public boolean isEmpty() {
        return names2values.isEmpty();
    }
    
    public void setProperty(String name, String value) {
        Property prop = getStrings2Properties().get(name);
        if (prop != null) {
            setProperty(prop, value);
        }
    }

    public void setProperty(Property p, String value) {
        names2values.put(p, value);
    }

    @Override
    public String toString() {
        return String.valueOf(names2values);
    }

    public static Map<String, Property> getStrings2Properties() {
        return STRINGS2PROPERTIES;
    }
    
    /**
     * Flattens out the keys and values to an array of strings.
     */
    public Properties toProperties() {
       Properties props = new Properties();
       for (Entry<Property, String> entry : names2values.entrySet()) {
           props.setProperty(entry.getKey().getValue(), entry.getValue());
       }
       return props;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GeocodeInformation) {
            return names2values.equals(((GeocodeInformation)obj).names2values);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return names2values.hashCode();
    }
    
    /**
     * @return empty geo code information if <code>props</code> are empty
     */
    public static GeocodeInformation fromProperties(Properties props) {
        GeocodeInformation info = new GeocodeInformation();
        for (Property property : Property.values()) {
            String value = props.getProperty(property.getValue());
            if (value != null) {
                info.setProperty(property, value);
            }
        }
        return info;
    }
}


