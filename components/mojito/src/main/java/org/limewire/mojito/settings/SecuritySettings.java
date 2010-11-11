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

import org.limewire.mojito.security.SecurityTokenHelper;
import org.limewire.security.SecurityToken;
import org.limewire.security.SecurityToken.TokenData;
import org.limewire.setting.BooleanSetting;

/**
 * Various Mojito security Settings.
 */
public class SecuritySettings extends MojitoProps {

    private SecuritySettings() {}
    
    /**
     * Settings for whether not the Port number should be substituted
     * in the {@link SecurityToken} and {@link TokenData} if a Node
     * says it's firewalled. Some NAT boxes keep changing the Port
     * number with each outgoing UDP packet and break therefore the
     * whole thing. See {@link SecurityTokenHelper} for more info!
     */
    public static final BooleanSetting SUBSTITUTE_TOKEN_PORT
        = FACTORY.createRemoteBooleanSetting("SUBSTITUTE_TOKEN_PORT", 
                true);
}
