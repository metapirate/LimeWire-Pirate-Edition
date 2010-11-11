/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package org.limewire.mojito.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;

/**
 * Settings for the Database, DHTValue and for the DHTValueManager.
 */
public final class DatabaseSettings extends MojitoProps {
    
    private DatabaseSettings() {}
    
    /**
     * The maximum number of Keys a single Node can store.
     */
    public static final IntSetting MAX_DATABASE_SIZE
        = FACTORY.createRemoteIntSetting("MAX_DATABASE_SIZE", 16384);
    
    /**
     * The maximum number of Values per Key a single Node can store.
     */
    public static final IntSetting MAX_VALUES_PER_KEY
        = FACTORY.createRemoteIntSetting("MAX_VALUES_PER_KEY", 5);
    
    /**
     * The maximum number of keys a single IP can store in the DHT.
     * (assuming random distribution of keys).
     * <pre>
     * v = total number of values for one IP
     * n = DHT size
     * k = replication param (default 20)
     * 
     * x = number of key stored per DHT node per IP
     * 
     * x = (v*k)/n  ==> x = (v*20)/100000 = (1/5000)*v
     * 
     * --> with x = 5, v = 25'000
     * </pre>
     * Considering even NAT'd addresses, this should be enough
     */
    // LimeWire 4.13.3
    //public static final IntSetting MAX_KEYS_PER_IP
    //    = FACTORY.createRemoteIntSetting("MAX_KEYS_PER_IP", 5, 
    //            "Mojito.MaxKeysPerIP", 1, Integer.MAX_VALUE - 1);
    
    /**
     * The limit after which the host gets banned. 
     * @see MAX_KEY_PER_IP
     */
    // LimeWire 4.13.3
    //public static final IntSetting MAX_KEYS_PER_IP_BAN_LIMIT
    //    = FACTORY.createRemoteIntSetting("MAX_KEYS_PER_IP_BAN_LIMIT", 50, 
    //        "Mojito.MaxKeysPerIPBanLimit", 1, Integer.MAX_VALUE - 1);
    
    /**
     * The time after a non-local value expires.
     */
    public static final LongSetting VALUE_EXPIRATION_TIME
        = FACTORY.createRemoteLongSetting("VALUE_EXPIRATION_TIME", 60L*60L*1000L);
    
    /**
     * The lower bound republishing interval for a DHTValue. That
     * means a DHTValue cannot be republished more often than this
     * interval.
     */
    public static final LongSetting MIN_VALUE_REPUBLISH_INTERVAL
        = FACTORY.createLongSetting("MIN_VALUE_REPUBLISH_INTERVAL", 2L*60L*1000L);
    
    /**
     * The republishing interval in milliseconds.
     */
    public static final LongSetting VALUE_REPUBLISH_INTERVAL
        = FACTORY.createLongSetting("VALUE_REPUBLISH_INTERVAL_2", 3300000);
    
    /**
     * The period of the StorablePublisher.
     */
    public static final LongSetting STORABLE_PUBLISHER_PERIOD
        = FACTORY.createLongSetting("STORABLE_PUBLISHER_PERIOD_2", 1860000);
    
    /**
     * The period of the DatabaseCleaner.
     */
    public static final LongSetting DATABASE_CLEANER_PERIOD
        = FACTORY.createRemoteLongSetting("DATABASE_CLEANER_PERIOD", 5L*60L*1000L);
    
    /**
     * The *alpha* factor for the Exponentially Moving Average (EMA) 
     * computation of the value request load.
     */
    public static final FloatSetting VALUE_REQUEST_LOAD_SMOOTHING_FACTOR 
        = FACTORY.createFloatSetting("VALUE_REQUEST_LOAD_SMOOTHING_FACTOR", 0.25f);
    
    /**
     * The delay (in sec) after which we null back the value request load.
     */
    // 1 minute
    public static final IntSetting VALUE_REQUEST_LOAD_NULLING_DELAY
        = FACTORY.createIntSetting("VALUE_REQUEST_LOAD_NULLING_DELAY", 60); 
    
    /**
     * Whether or not to delete a DHTValue from the Database if we're
     * the furthest of the k closest Nodes and a new Node comes along
     * that is nearer.
     */
    public static final BooleanSetting DELETE_VALUE_IF_FURTHEST_NODE
        = FACTORY.createRemoteBooleanSetting("DELETE_VALUE_IF_FURTHEST_NODE", 
                false);
    
    /**
     * Whether or not we limit the number of values a certain Class C
     * Network can store at the local Node.
     */
    public static final BooleanSetting LIMIT_VALUES_PER_NETWORK
        = FACTORY.createRemoteBooleanSetting("LIMIT_VALUES_PER_NETWORK", 
                true);
    
    /**
     * The maximum number of values a certain Class C Network can
     * store at the local Node.
     */
    public static final IntSetting MAX_VALUES_PER_NETWORK
        = FACTORY.createRemoteIntSetting("MAX_VALUES_PER_NETWORK", 
            100);
    
    /**
     * Whether or not we limit the number of values a single IP address
     * can store at the local Node.
     */
    public static final BooleanSetting LIMIT_VALUES_PER_ADDRESS
        = FACTORY.createRemoteBooleanSetting("LIMIT_VALUES_PER_ADDRESS", 
                true);
    
    /**
     * The maximum number of keys a single IP can store in the DHT.
     * (assuming random distribution of keys).
     * <pre>
     * v = total number of values for one IP
     * n = DHT size
     * k = replication param (default 20)
     * 
     * x = number of key stored per DHT node per IP
     * 
     * x = (v*k)/n  ==> x = (v*20)/100000 = (1/5000)*v
     * 
     * --> with x = 5, v = 25'000
     * </pre>
     * Considering even NAT'd addresses, this should be enough.
     */
    public static final IntSetting MAX_VALUES_PER_ADDRESS
        = FACTORY.createRemoteIntSetting("MAX_VALUES_PER_ADDRESS", 
            5);
    
    /**
     * Whether or not we validate the creator of a value.
     */
    public static final BooleanSetting VALIDATE_VALUE_CREATOR
        = FACTORY.createRemoteBooleanSetting("VALIDATE_VALUE_CREATOR", 
                false);
}
