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
 * Setting for Kademlia lookups.
 */
public class LookupSettings extends MojitoProps {
    
    private LookupSettings() {}
    
    /**
     * Use FIND_NODE (default) or FIND_VALUE to get the SecurityToken of
     * a remote Node. 
     */
    public static final BooleanSetting FIND_NODE_FOR_SECURITY_TOKEN
        = FACTORY.createBooleanSetting("FIND_NODE_FOR_SECURITY_TOKEN", true);

    /**
     * Whether or not the (k+1)-closest Contact should be
     * removed from the response Set.
     */
    public static final BooleanSetting DELETE_FURTHEST_CONTACT
        = FACTORY.createBooleanSetting("DELETE_FURTHEST_CONTACT", true);
    
    /**
     * The FIND_NODE lookup timeout.
     */
    public static final LongSetting FIND_NODE_LOOKUP_TIMEOUT
        = FACTORY.createRemoteLongSetting("FIND_NODE_LOOKUP_TIMEOUT", 
                90L*1000L);

    /**
     * The FIND_VALUE lookup timeout.
     */
    public static final LongSetting FIND_VALUE_LOOKUP_TIMEOUT
        = FACTORY.createRemoteLongSetting("FIND_VALUE_LOOKUP_TIMEOUT", 
                90L*1000L);

    /**
     * Whether or not a value lookup is exhaustive.
     */
    public static final BooleanSetting EXHAUSTIVE_VALUE_LOOKUP
        = FACTORY.createBooleanSetting("EXHAUSTIVE_VALUE_LOOKUP", false);

    /**
     * The number of parallel FIND_VALUE lookups.
     */
    public static final IntSetting FIND_VALUE_PARALLEL_LOOKUPS
        = FACTORY.createIntSetting("FIND_VALUE_PARALLEL_LOOKUPS_2", 5);

    /**
     * The number of parallel FIND_NODE lookups.
     */
    public static final IntSetting FIND_NODE_PARALLEL_LOOKUPS
        = FACTORY.createRemoteIntSetting("FIND_NODE_PARALLEL_LOOKUPS", 5);

    /**
     * Bootstrapping Node return an empty Collection of Contacts
     * for our FIND_NODE requests. This Setting controls whether or 
     * not such Nodes should be added to the lookup response path.
     */
    public static final BooleanSetting ACCEPT_EMPTY_FIND_NODE_RESPONSES
        = FACTORY.createRemoteBooleanSetting("ACCEPT_EMPTY_FIND_NODE_RESPONSES", 
                true);
    
    /**
     * Setting for what percentage of all Contacts in a FIND_NODE
     * response must be valid before the entire response is considered
     * valid.
     */
    public static final FloatSetting CONTACTS_SCRUBBER_REQUIRED_RATIO
        = FACTORY.createRemoteFloatSetting("CONTACTS_SCRUBBER_REQUIRED_RATIO", 
                0.0f);
    
    /**
     * Returns the lock timeout for a lookup process.
     * 
     * @param findNode whether it's a FIND_NODE or FIND_VALUE operation
     */
    public static long getWaitOnLock(boolean findNode) {
        long waitOnLock = 0L;
        
        if (findNode) {
            waitOnLock += ContextSettings.getWaitOnLock(
                    LookupSettings.FIND_NODE_LOOKUP_TIMEOUT.getValue());
        } else {
            waitOnLock += ContextSettings.getWaitOnLock(
                    LookupSettings.FIND_VALUE_LOOKUP_TIMEOUT.getValue());
        }
        
        return waitOnLock;
    }
}
