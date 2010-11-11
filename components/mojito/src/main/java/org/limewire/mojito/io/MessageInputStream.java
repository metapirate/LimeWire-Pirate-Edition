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
 
package org.limewire.mojito.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.limewire.io.NetworkUtils;
import org.limewire.mojito.KUID;
import org.limewire.mojito.StatusCode;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueFactoryManager;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.messages.DHTMessage.OpCode;
import org.limewire.mojito.messages.StatsRequest.StatisticType;
import org.limewire.mojito.messages.StoreResponse.StoreStatusCode;
import org.limewire.mojito.messages.impl.DefaultMessageID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;


/**
 * The MessageInputStream reads (parses) a <code>DHTMessage</code>
 * from a given InputStream.
 * <p>
 * <strong>NOTE</strong>: This class is specific to Mojito's Gnutella backed
 * Message format. You may or may not be able to use parts of this
 * class for alternative message formats!
 */
public class MessageInputStream extends DataInputStream {
    
    private final MACCalculatorRepositoryManager MACCalculatorRepositoryManager;
    public MessageInputStream(InputStream in, MACCalculatorRepositoryManager MACCalculatorRepositoryManager) {
        super(in);
        this.MACCalculatorRepositoryManager = MACCalculatorRepositoryManager;
    }
    
    /**
     * Reads the given number of bytes.
     */
    public byte[] readBytes(int bytes) throws IOException {
        byte[] buf = new byte[bytes];
        readFully(buf);
        return buf;
    }
    
    /**
     * Reads a KUID from the InputStream.
     */
    public KUID readKUID() throws IOException {
        return KUID.createWithInputStream(this);
    }
    
    /**
     * Reads a MessageID from the InputStream.
     */
    public MessageID readMessageID() throws IOException {
        return DefaultMessageID.createWithInputStream(this, MACCalculatorRepositoryManager);
    }
    
    /**
     * Reads a BigInteger from the InputStream.
     */
    public BigInteger readDHTSize() throws IOException {
        int length = readUnsignedByte();
        if (length > KUID.LENGTH) { // can't be more than 2**160 bit
            throw new IOException("Illegal length: " + length);
        }
        
        byte[] num = readBytes(length);
        return new BigInteger(1 /* unsigned */, num);
    }
    
    /**
     * Reads a DHTValueEntity from the InputStream.
     * 
     * @param sender the Contact that send us the DHTValue
     */
    public DHTValueEntity readDHTValueEntity(Contact sender, DHTValueFactoryManager factoryManager) throws IOException {
        Contact creator = readContact();
        KUID primaryKey = readKUID();
        DHTValue value = readDHTValue(factoryManager);
        
        // if the creator has the same KUID as the sender, use the sender as we have its external addr.
        if (creator.getNodeID().equals(sender.getNodeID()))
            creator = sender;
        return DHTValueEntity.createFromRemote(creator, sender, primaryKey, value);
    }
    
    /**
     * Reads a DHTValue from the InputStream.
     */
    private DHTValue readDHTValue(DHTValueFactoryManager factoryManager) throws IOException {
        DHTValueType valueType = readValueType();
        Version version = readVersion();
        
        byte[] data = null;
        int length = readUnsignedShort();
        if (length > 0) {
            data = new byte[length];
            readFully(data);
        }
        
        return factoryManager.createDHTValue(valueType, version, data);
    }
    
    /**
     * Reads multiple DHTValues from the InputStream.
     */
    public List<DHTValueEntity> readDHTValueEntities(Contact sender, DHTValueFactoryManager factoryManager) throws IOException {
        int size = readUnsignedByte();
        if (size == 0) {
            return Collections.emptyList();
        }
        
        DHTValueEntity[] entities = new DHTValueEntity[size];
        for(int i = 0; i < entities.length; i++) {
            entities[i] = readDHTValueEntity(sender, factoryManager);
        }
        return Arrays.asList(entities);
    }
    
    /**
     * Reads multiple KUIDs from the InputStream.
     */
    public Collection<KUID> readKUIDs() throws IOException {
        int size = readUnsignedByte();
        if (size == 0) {
            return Collections.emptySet();
        }
        
        KUID[] keys = new KUID[size];
        for (int i = 0; i < size; i++) {
            keys[i] = readKUID();
        }
        
        return Arrays.asList(keys);
    }
    
    /**
     * Reads a Signature from the InputStream.
     */
    public byte[] readSignature() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] signature = new byte[length];
        readFully(signature, 0, signature.length);
        return signature;
    }
	
    /**
     * Reads a Contact from the InputStream.
     */
    public Contact readContact() throws IOException {
        Vendor vendor = readVendor();
        Version version = readVersion();
        KUID nodeId = readKUID();
        SocketAddress addr = readSocketAddress();
        
        if (addr == null) {
            throw new UnknownHostException("SocketAddress is "+addr);
        }
        
        return ContactFactory.createUnknownContact(vendor, version, nodeId, addr);
    }
    
    /**
     * Reads multiple Contacts from the InputStream.
     */
    public Collection<Contact> readContacts() throws IOException {
        int size = readUnsignedByte();
        if (size == 0) {
            return Collections.emptyList();
        }
        
        Contact[] nodes = new Contact[size];
        for(int i = 0; i < nodes.length; i++) {
            nodes[i] = readContact();
        }
        return Arrays.asList(nodes);
    }

    /**
     * Reads an InetAddress from the InputStream.
     */
    public InetAddress readInetAddress() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] address = new byte[length];
        readFully(address);
        
        return InetAddress.getByAddress(address);
    }
    
    /**
     * Reads a Port number from the InputStream.
     */
    public int readPort() throws IOException {
        return readUnsignedShort();
    }
    
    /**
     * Reads a SocketAddress from the InputStream.
     */
    public InetSocketAddress readSocketAddress() throws IOException {
        InetAddress addr = readInetAddress();
        if (addr == null || !NetworkUtils.isValidAddress(addr)) {
            return null;
        }
        
        int port = readPort();
        return new InetSocketAddress(addr, port);
    }
    
    /**
     * Reads a AddressSecurityToken from the InputStream.
     */
    public SecurityToken readSecurityToken() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] securityToken = new byte[length];
        readFully(securityToken, 0, securityToken.length);
        return new AddressSecurityToken(securityToken, MACCalculatorRepositoryManager);
    }
    
    /**
     * Reads an encoded Statistics.
     */
    public byte[] readStatistics() throws IOException {
        int length = readUnsignedShort();
        if (length == 0) {
            return null;
        }
        
        byte[] statistics = new byte[length];
        readFully(statistics);
        return statistics;
    }
    
    /**
     * Reads an OpCode from the InputStream.
     */
    public OpCode readOpCode() throws IOException {
        return OpCode.valueOf(readUnsignedByte());
    }
    
    /**
     * Reads an Type from the InputStream.
     */
    public StatisticType readStatisticType() throws IOException {
        return StatisticType.valueOf(readUnsignedByte());
    }
    
    /**
     * Reads a StatusCode from the InputStream.
     */
    public StatusCode readStatusCode() throws IOException {
        return StatusCode.valueOf(readUnsignedShort(), readDHTString());
    }
    
    /**
     * Reads a String from the InputStream. See 
     * MessageOutputStrea.writeDHTString().
     * 
     * @returns empty string if the length of the string is 0
     */
    private String readDHTString() throws IOException {
        int length = readUnsignedShort();
        if (length == 0) {
            return "";
        }
        
        byte[] b = new byte[length];
        readFully(b);
        return new String(b, "UTF-8");
    }
    
    /**
     * Reads and returns a Collection of StoreStatusCode(s).
     */
    public Collection<StoreStatusCode> readStoreStatusCodes() throws IOException {
        int size = readUnsignedByte();
        if (size == 0) {
            return Collections.emptySet();
        }
        
        StoreStatusCode[] status = new StoreStatusCode[size];
        for (int i = 0; i < size; i++) {
            KUID primaryKey = readKUID();
            KUID secondaryKey = readKUID();
            StatusCode statusCode = readStatusCode();
            status[i] = new StoreStatusCode(primaryKey, secondaryKey, statusCode);
        }
        
        return Arrays.asList(status);
    }
    
    /**
     * Reads a DHTValueType from the InputStream.
     */
    public DHTValueType readValueType() throws IOException {
        return DHTValueType.valueOf(readInt());
    }
    
    /**
     * Reads a Vendor from the InputStream.
     */
    public Vendor readVendor() throws IOException {
        return Vendor.valueOf(readInt());
    }
    
    /**
     * Reads a Version from the InputStream.
     */
    public Version readVersion() throws IOException {
        return Version.valueOf(readUnsignedShort());
    }
}
