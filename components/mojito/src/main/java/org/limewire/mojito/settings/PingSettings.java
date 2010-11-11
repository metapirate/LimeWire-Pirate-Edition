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

import org.limewire.setting.IntSetting;

/**
 * Settings for Mojito PINGs.
 */
public class PingSettings extends MojitoProps {

    private PingSettings() {}
    
    /**
     * The number of pings to send in parallel.
     */
    public static final IntSetting PARALLEL_PINGS
        = FACTORY.createRemoteIntSetting("PARALLEL_PINGS", 15);
    /**
     * The maximum number of ping failures before pinging is
     * given up.
     */
    public static final IntSetting MAX_PARALLEL_PING_FAILURES
        = FACTORY.createIntSetting("MAX_PARALLEL_PING_FAILURES", 40);
    
    /**
     * Returns the lock timeout for pings.
     */
    public static long getWaitOnLock() {
        return ContextSettings.getWaitOnLock(
                NetworkSettings.DEFAULT_TIMEOUT.getValue());
    }
}
