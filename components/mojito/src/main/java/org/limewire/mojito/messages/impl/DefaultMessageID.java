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

package org.limewire.mojito.messages.impl;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Random;

import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.util.ArrayUtils;
import org.limewire.security.AbstractSecurityToken;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.InvalidSecurityTokenException;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;

/**
 * An 128bit, pseudo random implementation of MessageID with support
 * for tagging.
 * <p>
 * This class is immutable!
 */
public class DefaultMessageID implements MessageID, Comparable<DefaultMessageID> {

    private static final long serialVersionUID = -1477232241287654597L;

    public static final int LENGTH = 16;

    private static final Random GENERATOR = new Random();

    /**
     * A random pad we're using to obfuscate the actual
     * AddressSecurityToken. Nodes must do a lookup to get the QK!
     */
    private static final byte[] RANDOM_PAD = new byte[4];

    static {
        GENERATOR.nextBytes(RANDOM_PAD);
    }

    private final byte[] messageId;

    private final int hashCode;
    
    private final MACCalculatorRepositoryManager macManager;

    private DefaultMessageID(byte[] messageId, MACCalculatorRepositoryManager macManager) {
        if (messageId == null) {
            throw new NullPointerException("messageId cannot be null");
        }
        
        if (messageId.length != LENGTH) {
            throw new IllegalArgumentException("MessageID must be " 
                    + LENGTH + " bytes long: " + messageId.length);
        }
        
        this.macManager = macManager;
        this.messageId = messageId;
        this.hashCode = Arrays.hashCode(messageId);
    }

    /**
     * Creates a MessageID from the given InputStream.
     */
    public static DefaultMessageID createWithInputStream(InputStream in, MACCalculatorRepositoryManager manager) throws IOException {
        byte[] messageId = new byte[LENGTH];
        
        int len = -1;
        int r = 0;
        while(r < messageId.length) {
            len = in.read(messageId, r, messageId.length-r);
            if (len < 0) {
                throw new EOFException();
            }
            r += len;
        }
        
        return new DefaultMessageID(messageId, manager);
    }
    
    /**
     * Creates and returns a MessageID from the given byte Array.
     */
    public static DefaultMessageID createWithBytes(byte[] messageId) {
        byte[] copy = new byte[messageId.length];
        System.arraycopy(messageId, 0, copy, 0, messageId.length);
        return new DefaultMessageID(copy, null);
    }

    /**
     * Creates a pseudo random MessageID and tags it with the given 
     * SocketAddress. 
     */
    public static DefaultMessageID createWithSocketAddress(SocketAddress dst, MACCalculatorRepositoryManager macManager) {
        byte[] messageId = new byte[LENGTH];
        GENERATOR.nextBytes(messageId);

        if (dst instanceof InetSocketAddress) {
            byte[] token = (new MessageSecurityToken(new DHTTokenData(dst), macManager)).getBytes();
            System.arraycopy(token, 0, messageId, 0, 4);
        }

        return new DefaultMessageID(messageId, macManager);
    }

    /**
     * Creates and returns a MessageID from the given
     * hex encoded String.
     */
    public static DefaultMessageID createWithHexString(String messageId) {
        return new DefaultMessageID(ArrayUtils.parseHexString(messageId), null);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.messages.MessageID#write(java.io.OutputStream)
     */
    public void write(OutputStream os) throws IOException {
        os.write(messageId, 0, messageId.length);
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.messages.MessageID#getLength()
     */
    public int getLength() {
        return LENGTH;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.messages.MessageID#getBytes()
     */
    public byte[] getBytes() {
        return getBytes(0, new byte[messageId.length], 0, messageId.length);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.messages.MessageID#getBytes(int, byte[], int, int)
     */
    public byte[] getBytes(int srcPos, byte[] dest, int destPos, int length) {
        System.arraycopy(messageId, srcPos, dest, destPos, length);
        return dest;
    }
    
    /**
     * Extracts and returns the AddressSecurityToken from the GUID.
     * @throws InvalidSecurityTokenException 
     */
    private SecurityToken getSecurityToken() throws InvalidSecurityTokenException {
        byte[] token = new byte[4];
        System.arraycopy(messageId, 0, token, 0, token.length);
        return new MessageSecurityToken(token, macManager);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.messages.MessageID#isTaggingSupported()
     */
    public boolean isTaggingSupported() {
        return true;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.messages.MessageID#verifySecurityToken(java.net.SocketAddress)
     */
    public boolean isFor(SocketAddress src) {
        if (!(src instanceof InetSocketAddress)) {
            return false;
        }
        
        try {
            SecurityToken token = getSecurityToken();
            DHTTokenData data = new DHTTokenData(src);
            return token.isFor(data); 
        } catch (InvalidSecurityTokenException iste) {
            return false;
        }
    }
    
    public int compareTo(DefaultMessageID o) {
        int d = 0;
        for(int i = 0; i < messageId.length; i++) {
            d = (messageId[i] & 0xFF) - (o.messageId[i] & 0xFF);
            if (d < 0) {
                return -1;
            } else if (d > 0) {
                return 1;
            }
        }

        return 0;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof DefaultMessageID)) {
            return false;
        }

        return Arrays.equals(messageId, ((DefaultMessageID)o).messageId);
    }
    
    /**
     * Returns this MessageID as a hex String.
     */
    public String toHexString() {
        return ArrayUtils.toHexString(messageId);
    }

    /**
     * Returns this MessageID as a binary String.
     */
    public String toBinString() {
        return ArrayUtils.toBinString(messageId);
    }

    @Override
    public String toString() {
        return "MessageID: " + toHexString();
    }
    
    public static class DHTTokenData extends AddressSecurityToken.AddressTokenData {
        
        private final SocketAddress addr;
        public DHTTokenData(SocketAddress addr) {
            super(addr);
            this.addr = addr;
            for (int i = 0; i < RANDOM_PAD.length; i++)
                data[i] ^= RANDOM_PAD[i];
        }
        
        @Override
        public String toString() {
            return "DHTTokenData: " + ArrayUtils.toHexString(getData())+ " for "+addr;
        }
    }
   
    public static class MessageSecurityToken extends AbstractSecurityToken {
        
        public MessageSecurityToken(byte[] network, MACCalculatorRepositoryManager manager) throws InvalidSecurityTokenException {
            super(network, manager);
        }

        public MessageSecurityToken(DHTTokenData data, MACCalculatorRepositoryManager manager) {
            super(data, manager);
        }

        @Override
        protected byte[] getFromMAC(byte[] mac, TokenData ignored) {
            return mac;
        }
        
        @Override
        public String toString() {
            return "MessageSecurityToken: " + ArrayUtils.toHexString(getBytes());
        }
    }
}
