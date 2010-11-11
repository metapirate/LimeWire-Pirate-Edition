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
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;

/**
 * Miscellaneous Network settings.
 */
public final class NetworkSettings extends MojitoProps {
    
    private NetworkSettings() {}
    
    /**
     * The amount of time we're waiting for a response
     * before giving up.
     */
    public static final LongSetting DEFAULT_TIMEOUT
        = FACTORY.createRemoteLongSetting("DEFAULT_TIMEOUT", 
                10L*1000L);
    
    /**
     * A multiplication factor for the RTT.
     */
    public static final IntSetting MIN_TIMEOUT_RTT_FACTOR
        = FACTORY.createRemoteIntSetting("MIN_TIMEOUT_RTT_FACTOR", 2);
    
    /**
     * A multiplication factor for the RTT.
     */
    public static final LongSetting MIN_TIMEOUT_RTT
        = FACTORY.createRemoteLongSetting("MIN_TIMEOUT_RTT", 1000L);
    
    /**
     * The maximum number of errors (timeouts) that may occur 
     * before we're giving up to re-send requests.
     */
    public static final IntSetting MAX_ERRORS
        = FACTORY.createRemoteIntSetting("MAX_ERRORS", 
                2);
    
    /**
     * The maximum size of a serialized message.
     */
    public static final IntSetting MAX_MESSAGE_SIZE
        = FACTORY.createIntSetting("MAX_MESSAGE_SIZE", 1492);
    
    /**
     * The cleanup rate for Receipts.
     */
    public static final LongSetting CLEANUP_RECEIPTS_DELAY
        = FACTORY.createLongSetting("CLEANUP_RECEIPTS_DELAY", 50L);
    
    /**
     * The buffer size for incoming messages.
     */
    public static final IntSetting RECEIVE_BUFFER_SIZE
        = FACTORY.createIntSetting("RECEIVE_BUFFER_SIZE", 64*1024);
    
    /**
     * The buffer size for outgoing messages.
     */
    public static final IntSetting SEND_BUFFER_SIZE
        = FACTORY.createIntSetting("SEND_BUFFER_SIZE", 64*1024);
    
    /**
     * Whether or not we're accepting forced addresses.
     */
    public static final BooleanSetting ACCEPT_FORCED_ADDRESS
        = FACTORY.createBooleanSetting("ACCEPT_FORCED_ADDRESS", false);
    
    /**
     * Whether or not a new ByteBuffer should be allocated for
     * every message we're receiving.
     */
    public static final BooleanSetting ALLOCATE_NEW_BUFFER
        = FACTORY.createBooleanSetting("ALLOCATE_NEW_BUFFER", false);
    
    /**
     * Setting for whether or not private IP Addresses are
     * considered private.
     * <p>
     * NOTE: If you're planning to run the DHT on a Local Area 
     * Network (LAN) you want to set LOCAL_IS_PRIVATE to false!
     */
    public static final BooleanSetting LOCAL_IS_PRIVATE
        = FACTORY.createBooleanSetting("LOCAL_IS_PRIVATE", true);
    
    /**
     * Setting for whether or not IPs from the same Class C
     * Network should be filtered.
     * <p>
     * NOTE: If you're planning to run the DHT on a Local Area 
     * Network (LAN) you want to set FILTER_CLASS_C to false!
     */
    public static final BooleanSetting FILTER_CLASS_C
        = FACTORY.createRemoteBooleanSetting("FILTER_CLASS_C", 
                true);
    
    /**
     * Setting for whether or not RESPONSE messages should be dropped if 
     * the SENDER (remote Node) is firewalled.
     * <p>
     * Warning: Changing this Setting may cause weird effects!
     */
    public static final BooleanSetting DROP_RESPONE_IF_FIREWALLED
        = FACTORY.createBooleanSetting("DROP_RESPONE_IF_FIREWALLED", true);
    
    /**
     * Setting for whether or not REQUEST messages should be dropped is
     * the RECEIVER (local Node) is firewalled.
     * <p>
     * Warning: Changing this Setting may cause weird effects!
     */
    public static final BooleanSetting DROP_REQUEST_IF_FIREWALLED
        = FACTORY.createBooleanSetting("DROP_REQUEST_IF_FIREWALLED", true);
    
    /**
     * Setting for whether or not IPv4-compatible addresses should be 
     * dropped that have a public address.
     */
    public static final BooleanSetting DROP_PUBLIC_IPV4_COMPATIBLE_ADDRESSES
        = FACTORY.createRemoteBooleanSetting("DROP_PUBLIC_IPV4_COMPATIBLE_ADDRESSES", 
                true);
    
    /**
     * Setting for whether or not IPv4-compatible addresses should be 
     * dropped that have a private address.
     */
    public static final BooleanSetting DROP_PRIVATE_IPV4_COMPATIBLE_ADDRESSES
        = FACTORY.createRemoteBooleanSetting("DROP_PRIVATE_IPV4_COMPATIBLE_ADDRESSES", 
                true);
}
