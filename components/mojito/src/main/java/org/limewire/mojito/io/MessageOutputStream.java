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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;

import org.limewire.mojito.KUID;
import org.limewire.mojito.StatusCode;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.messages.DHTMessage.OpCode;
import org.limewire.mojito.messages.StatsRequest.StatisticType;
import org.limewire.mojito.messages.StoreResponse.StoreStatusCode;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.SecurityToken;


/**
 * The MessageOutputStream class writes a DHTMessage (serializes)
 * to a given OutputStream.
 * <p>
 * <b>NOTE</b>: This class is specific to Mojito's Gnutella backed
 * Message format. You may or may not be able to use parts of this
 * class for alternative message formats!
 */
public class MessageOutputStream extends DataOutputStream {
    
    public MessageOutputStream(OutputStream out) {
        super(out);
    }
	
    /**
     * Writes the given KUID to the OutputStream.
     */
    public void writeKUID(KUID kuid) throws IOException {
        if (kuid == null) {
            throw new NullPointerException("KUID cannot be null");
        }
        
        kuid.write(this);
    }
    
    /**
     * Writes the given MessageID to the OutputStream.
     */
    public void writeMessageID(MessageID messageId) throws IOException {
        if (messageId == null) {
            throw new NullPointerException("MessageID cannot be null");
        }
        
        messageId.write(this);
    }
    
    /**
     * Writes the given BigInteger to the OutputStream.
     */
    public void writeDHTSize(BigInteger estimatedSize) throws IOException {
        byte[] data = estimatedSize.toByteArray();
        if (data.length > KUID.LENGTH) { // Can't be more than 2**160 bit
            throw new IOException("Illegal length: " + data.length + "/" + estimatedSize);
        }
        writeByte(data.length);
        write(data, 0, data.length);
    }
    
    /**
     * Writes the given DHTValue to the OutputStream.
     */
    public void writeDHTValueEntity(DHTValueEntity entity) throws IOException {
        writeContact(entity.getCreator());
        entity.getPrimaryKey().write(this);
        writeDHTValue(entity.getValue());
    }
    
    /**
     * 
     */
    private void writeDHTValue(DHTValue value) throws IOException {
        writeDHTValueType(value.getValueType());
        writeVersion(value.getVersion());
        byte[] data = value.getValue();
        writeShort(data.length);
        write(data, 0, data.length);
    }
    
    /**
     * Writes the given Collection of KUIDs to the OutputStream.
     */
    public void writeKUIDs(Collection<KUID> keys) throws IOException {
        writeCollectionSize(keys);
        for (KUID k : keys) {
            k.write(this);
        }
    }
    
    /**
     * Writes the given Collection of DHTValues to the OutputStream.
     */
    public void writeDHTValueEntities(Collection<? extends DHTValueEntity> values) throws IOException {
        writeCollectionSize(values);
        for(DHTValueEntity entity : values) {
            writeDHTValueEntity(entity);
        }
    }
    
    /**
     * Writes the given Signature to the OutputStream.
     */
    public void writeSignature(byte[] signature) throws IOException {
        if (signature != null && signature.length > 0) {
            writeByte(signature.length);
            write(signature, 0, signature.length);
        } else {
            writeByte(0);
        }
    }
    
    /**
     * Writes the given Contact to the OutputStream.
     */
    public void writeContact(Contact node) throws IOException {
        writeVendor(node.getVendor());
        writeVersion(node.getVersion());
        writeKUID(node.getNodeID());
        writeSocketAddress(node.getContactAddress());
    }
    
    /**
     * Writes the given Collection of Contact to the OutputStream.
     */
    public void writeContacts(Collection<? extends Contact> nodes) throws IOException {
        writeCollectionSize(nodes);
        for(Contact node : nodes) {
            writeContact(node);
        }
    }
    
    /**
     * Writes the given InetAddress to the OutputStream.
     */
    public void writeInetAddress(InetAddress addr) throws IOException {
        byte[] address = addr.getAddress();
        writeByte(address.length);
        write(address, 0, address.length);
    }
    
    /**
     * Writes the given Port number to the OutputStream.
     */
    public void writePort(int port) throws IOException {
        writeShort(port);
    }
    
    /**
     * Writes the given SocketAddress to the OutputStream.
     */
    public void writeSocketAddress(SocketAddress addr) throws IOException {
        if (addr instanceof InetSocketAddress
                && !((InetSocketAddress)addr).isUnresolved()) {
            InetSocketAddress iaddr = (InetSocketAddress)addr;
            
            writeInetAddress(iaddr.getAddress());
            writePort(iaddr.getPort());
        } else {
            writeByte(0);
        }
    }
    
    /**
     * Writes the given AddressSecurityToken to the OutputStream.
     */
    public void writeSecurityToken(SecurityToken securityToken) throws IOException {
        if (securityToken != null) {
            assert (securityToken instanceof AddressSecurityToken);
            byte[] qk = securityToken.getBytes();
            writeByte(qk.length);
            write(qk, 0, qk.length);
        } else {
            writeByte(0);
        }
    }
    
    /**
     * Writes an encoded Statistics payload to the OutputStream.
     */
    public void writeStatistics(byte[] statistics) throws IOException {
        if (statistics != null) {
            writeShort(statistics.length);
            write(statistics);
        } else {
            writeShort(0);
        }
    }
    
    /**
     * Writes the given OpCode to the OutputStream.
     */
    public void writeOpCode(OpCode opcode) throws IOException {
        writeByte(opcode.toByte());
    }
    
    /**
     * Writes the given Type to the OutputStream.
     */
    public void writeStatisticType(StatisticType type) throws IOException {
        writeByte(type.toByte());
    }
    
    /**
     * Writes the given StatusCode to the OutputStream.
     */
    public void writeStatusCode(StatusCode statusCode) throws IOException {
        writeShort(statusCode.shortValue());
        writeDHTString(statusCode.getDescription());
    }
    
    /**
     * Writes the given String to the OutputStream. This is different
     * from writeUTF(String) which writes the String in the so called
     * Modified-UTF format!
     *
     *  @param str must not be null
     */
    void writeDHTString(String str) throws IOException {
        byte[] b = str.getBytes("UTF-8");
        if (b.length > 0xFFFF) {
            throw new IOException("String is too big");
        }
        writeShort(b.length);
        write(b);
    }
    
    /**
     * Writes the given Collection of StoreStatusCode(s) to the OutputStream.
     */
    public void writeStoreStatusCodes(Collection<StoreStatusCode> statusCodes) throws IOException {
        writeCollectionSize(statusCodes);
        for (StoreStatusCode statusCode : statusCodes) {
            writeKUID(statusCode.getPrimaryKey());
            writeKUID(statusCode.getSecondaryKey());
            writeStatusCode(statusCode.getStatusCode());
        }
    }
    
    /**
     * Writes the size of the given Collection to the OutputStream.
     */
    private void writeCollectionSize(Collection c) throws IOException {
        int size = c.size();
        if (size > 0xFF) {
            throw new IOException("Too many elements: " + size);
        }
        
        writeByte(size);
    }
    
    /**
     * Writes the DHTValueType to the OutputStream.
     */
    public void writeDHTValueType(DHTValueType type) throws IOException {
        writeInt(type.toInt());
    }
    
    /**
     * Writes the Vendor to the OutputStream.
     */
    public void writeVendor(Vendor vendor) throws IOException {
        writeInt(vendor.intValue());
    }
    
    /**
     * Writes the Version to the OutputStream.
     */
    public void writeVersion(Version version) throws IOException {
        writeShort(version.shortValue());
    }
}
