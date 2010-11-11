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
 * Miscellaneous RouteTable related settings.
 */
public final class RouteTableSettings extends MojitoProps {

    private RouteTableSettings() {}
    
    /**
     * The maximum number of Contacts we're keeping in the
     * Bucket replacement cache.
     */
    public static final IntSetting MAX_CACHE_SIZE
        = FACTORY.createRemoteIntSetting("MAX_CACHE_SIZE", 16);
    
    /**
     * The maximum number of failures a node may have before being completely
     * evicted from the routing table. This also serves as a basis for the 
     * probability of a node to be included in the list of k closest nodes.
     */
    public static final IntSetting MAX_ACCEPT_NODE_FAILURES 
        = FACTORY.createRemoteIntSetting("MAX_ACCEPT_NODE_FAILURES", 20);
    
    /**
     * The maximum number of errors that may occur before an
     * alive Contact is considered as dead.
     */
    public static final IntSetting MAX_ALIVE_NODE_FAILURES
        = FACTORY.createRemoteIntSetting("MAX_ALIVE_NODE_FAILURES", 4);
   
    /**
     * The maximum number of errors that may occur before an
     * unknown Contact is considered as dead.
     */
    public static final IntSetting MAX_UNKNOWN_NODE_FAILURES
        = FACTORY.createRemoteIntSetting("MAX_UNKNOWN_NODE_FAILURES", 2);
    
    /**
     * The minimum time that must pass since the last successful contact 
     * before we're contacting a Node for RouteTable maintenance reasons.
     */
    public static final LongSetting MIN_RECONNECTION_TIME
        = FACTORY.createRemoteLongSetting("MIN_RECONNECTION_TIME", 30L*1000L);
    
    /**
     * The symbol size, i.e. the number of bits improved at each step.
     * Also known as parameter B.
     */
    public static final IntSetting DEPTH_LIMIT
        = FACTORY.createRemoteIntSetting("DEPTH_LIMIT", 
                4);
    
    /**
     * The period of the Bucket freshness.
     */
    public static final LongSetting BUCKET_REFRESH_PERIOD
        = FACTORY.createRemoteLongSetting("BUCKET_REFRESH_PERIOD", 30L*60L*1000L);
    
    /**
     * A minimum time (in sec) to pass before pinging the least recently
     * seen node of a bucket again.
     */
    public static final LongSetting BUCKET_PING_LIMIT
        = FACTORY.createRemoteLongSetting("BUCKET_PING_LIMIT", 30L*1000L);
    
    /**
     * The maximum number of consecutive failures that may occur
     * in a row before we're suspending all maintenance operations
     * (we're maybe no longer connected to the Internet and we'd
     * kill our RouteTable).
     */
    public static final IntSetting MAX_CONSECUTIVE_FAILURES
        = FACTORY.createRemoteIntSetting("MAX_CONSECUTIVE_FAILURES", 100);
    
    /**
     * The maximum percentage of Contacts a Bucket can hold that are from
     * the same Class C Network.
     */
    public static final FloatSetting MAX_CONTACTS_PER_NETWORK_CLASS_RATIO
        = FACTORY.createFloatSetting("MAX_CONTACTS_PER_NETWORK_CLASS_RATIO_2",
                0.1f);
    
    /**
     * True if contacts created from incoming requests should be considered UNKNOWN
     * instead of LIVE.
     */
    public static final BooleanSetting INCOMING_REQUESTS_UNKNOWN =
        FACTORY.createRemoteBooleanSetting("INCOMING_REQUESTS_UNKNOWN", true);
}
