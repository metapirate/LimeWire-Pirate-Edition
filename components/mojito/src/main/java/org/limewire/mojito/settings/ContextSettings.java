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

import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.util.ArrayUtils;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;
import org.limewire.setting.StringSetting;

/**
 * Miscellaneous context settings.
 */
public class ContextSettings extends MojitoProps {
    
    private ContextSettings() {}
    
    /**
     * A multiplier that is used to determinate the number
     * of Nodes to where we're sending shutdown messages.
     */
    public static final IntSetting SHUTDOWN_MESSAGES_MULTIPLIER
        = FACTORY.createRemoteIntSetting("SHUTDOWN_MESSAGES_MULTIPLIER", 2);
    
    /**
     * The time interval to compute the locally estimated Network size.
     */
    public static final LongSetting ESTIMATE_NETWORK_SIZE_EVERY
        = FACTORY.createRemoteLongSetting("ESTIMATE_NETWORK_SIZE_EVERY", 60L*1000L);
    
    /**
     * The time interval in which the estimated Network size can be updated.
     */
    public static final LongSetting UPDATE_NETWORK_SIZE_EVERY
        = FACTORY.createRemoteLongSetting("UPDATE_NETWORK_SIZE_EVERY", 5L*1000L);
    
    /**
     * The maximum number of locally estimated Network sizes to
     * keep in Memory and to use as basis for the local estimation.
     */
    public static final IntSetting MAX_LOCAL_HISTORY_SIZE
        = FACTORY.createRemoteIntSetting("MAX_LOCAL_HISTORY_SIZE", 20);
    
    /**
     * Whether or not only live Nodes should be used to estimate the
     * DHT size.
     */
    public static final BooleanSetting ESTIMATE_WITH_LIVE_NODES_ONLY
        = FACTORY.createRemoteBooleanSetting("ESTIMATE_WITH_LIVE_NODES_ONLY", true);
    
    /**
     * The maximum number of remotely estimated Network sizes to
     * keep in Memory and to use as basis for the local estimation.
     */
    public static final IntSetting MAX_REMOTE_HISTORY_SIZE
        = FACTORY.createRemoteIntSetting("MAX_REMOTE_HISTORY_SIZE", 10);
    
    /**
     * Whether or not to estimate the Network size.
     */
    public static final BooleanSetting COUNT_REMOTE_SIZE
        = FACTORY.createRemoteBooleanSetting("COUNT_REMOTE_SIZE", 
                true);
    
    /**
     * The number smallest and biggest DHT size estimates to skip.
     */
    public static final IntSetting SKIP_REMOTE_ESTIMATES
        = FACTORY.createRemoteIntSetting("SKIP_REMOTE_ESTIMATES", 1);
    
    /**
     * The name of the master key file.
     */
    public static final StringSetting MASTER_KEY
        = FACTORY.createStringSetting("MASTER_KEY", "public.key");
    
    /**
     * The maximum time to wait on an Object.
     */
    public static final LongSetting WAIT_ON_LOCK
        = FACTORY.createLongSetting("WAIT_ON_LOCK", 3L*60L*1000L);
    
    /**
     * Whether or not assertion is enabled for collision pings.
     * This is used for testing! Default should be always true!
     */
    public static final BooleanSetting ASSERT_COLLISION_PING
        = FACTORY.createBooleanSetting("ASSERT_COLLISION_PING", true);
    
    /**
     * This Node's Vendor code.
     */
    public static final IntSetting VENDOR
        = FACTORY.createIntSetting("VENDOR", ArrayUtils.toInteger("LIME"));
    
    /**
     * This Node's Version.
     */
    public static final IntSetting VERSION
        = FACTORY.createIntSetting("VERSION", 0);
    
    /**
     * Returns the local Node's Vendor ID.
     */
    public static Vendor getVendor() {
        return Vendor.valueOf(ContextSettings.VENDOR.getValue());
    }
    
    /**
     * Returns the local Node's Version number.
     */
    public static Version getVersion() {
        return Version.valueOf(ContextSettings.VERSION.getValue());
    }
    
    /**
     * Whether or not we're sending a shutdown message.
     */
    public static final BooleanSetting SEND_SHUTDOWN_MESSAGE
        = FACTORY.createBooleanSetting("SEND_SHUTDOWN_MESSAGE", true);
    
    /**
     * Whether or not certain operations throw a NotBootstrappedException
     * if Mojito is not bootstrapped. This setting is meant for testing!
     * Do not change w/o having a good reason!
     */
    public static final BooleanSetting THROW_EXCEPTION_IF_NOT_BOOTSTRAPPED
        = FACTORY.createBooleanSetting("THROW_EXCEPTION_IF_NOT_BOOTSTRAPPED", true);
    
    /**
     * Returns the lock timeout.
     */
    public static long getWaitOnLock(long timeout) {
        return Math.max((long)(timeout * 1.5f), 
                ContextSettings.WAIT_ON_LOCK.getValue());
    }
}
