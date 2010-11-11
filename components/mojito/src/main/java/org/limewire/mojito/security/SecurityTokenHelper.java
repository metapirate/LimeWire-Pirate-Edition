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

package org.limewire.mojito.security;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.limewire.mojito.Context;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.SecuritySettings;
import org.limewire.security.SecurityToken;
import org.limewire.security.SecurityToken.TokenData;

/**
 * A helper class to create {@link SecurityToken}s and {@link TokenData}
 * objects.
 */
public class SecurityTokenHelper {
    
    private static final int SUBSTITUTE_PORT = 1024;
    
    private final Context context;
    
    public SecurityTokenHelper(Context context) {
        this.context = context;
    }
    
    /**
     * Returns a SocketAddress that can be used to generate a
     * {@link SecurityToken} or a {@link TokenData} object.
     * <p>
     * The reasoning behind this method is the following: Some
     * NAT boxes keep changing the Port for each outgoing UDP
     * packet. So if we respond to a FIND_NODE with the SecurityToken
     * and the Node actually tries to do something (e.g. STORE)
     * it will fail because the new request is coming from a different
     * Port.
     */
    private SocketAddress getTokenSocketAddress(Contact node) {
        if (!node.isFirewalled()
                || !SecuritySettings.SUBSTITUTE_TOKEN_PORT.getValue()) {
            return node.getContactAddress();
        }
        
        InetSocketAddress isa = (InetSocketAddress)node.getContactAddress();
        InetAddress addr = isa.getAddress();
        return new InetSocketAddress(addr, SUBSTITUTE_PORT);
    }
    
    /**
     * Creates and returns a {@link SecurityToken} for the given destination {@link Contact}.
     */
    public SecurityToken createSecurityToken(Contact dst) {
        return context.getSecurityTokenProvider().getSecurityToken(getTokenSocketAddress(dst));
    }
    
    /**
     * Creates and returns a {@link TokenData} for the given source {@link Contact}.
     */
    public TokenData createTokenData(Contact src) {
        return context.getSecurityTokenProvider().getTokenData(getTokenSocketAddress(src));
    }
}
