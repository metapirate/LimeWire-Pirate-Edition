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

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.Signature;
import java.security.SignatureException;

import org.limewire.io.ByteBufferOutputStream;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.io.MessageInputStream;
import org.limewire.mojito.io.MessageOutputStream;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;


/**
 * An abstract implementation of DHTMessage
 */
abstract class AbstractDHTMessage implements DHTMessage {
    
    protected final Context context;
    
    private final OpCode opcode;
    
    private final Contact contact;
    
    private final MessageID messageId;
    
    private final Version msgVersion;
    
    private byte[] payload;
    
    public AbstractDHTMessage(Context context, 
            OpCode opcode, Contact contact, MessageID messageId, Version msgVersion) {

        if (opcode == null) {
            throw new NullPointerException("OpCode is null");
        }
        
        if (contact == null) {
            throw new NullPointerException("Contact is null");
        }

        if (messageId == null) {
            throw new NullPointerException("MessageID is null");
        }
        
        if (msgVersion == null) {
            throw new NullPointerException("Version is null");
        }
        
        this.context = context;
        this.opcode = opcode;
        this.contact = contact;
        this.messageId = messageId;
        this.msgVersion = msgVersion;
    }

    public AbstractDHTMessage(Context context, OpCode opcode, SocketAddress src, 
            MessageID messageId, Version msgVersion, MessageInputStream in) throws IOException {
        
        if (opcode == null) {
            throw new NullPointerException("OpCode is null");
        }
        
        if (messageId == null) {
            throw new NullPointerException("MessageID is null");
        }
        
        if (msgVersion == null) {
            throw new NullPointerException("Version is null");
        }
        
        this.context = context;
        this.opcode = opcode;
        this.messageId = messageId;
        this.msgVersion = msgVersion;
        
        Vendor vendor = in.readVendor();
        Version version = in.readVersion();
        KUID nodeId = in.readKUID();
        SocketAddress contactAddress = in.readSocketAddress();
        
        if (contactAddress == null) {
            throw new UnknownHostException("Contact Address is null");
        }
        
        int instanceId = in.readUnsignedByte();
        int flags = in.readUnsignedByte();
        
        this.contact = createContact(src, vendor, version, 
                nodeId, contactAddress, instanceId, flags);
        
        int extensionsLength = in.readUnsignedShort();
        in.skip(extensionsLength);
    }
    
    protected abstract Contact createContact(SocketAddress src, Vendor vendor, Version version,
            KUID nodeId, SocketAddress contactAddress, int instanceId, int flags);
    
    public Context getContext() {
        return context;
    }
    
    public OpCode getOpCode() {
        return opcode;
    }

    public Contact getContact() {
        return contact;
    }
    
    public MessageID getMessageID() {
        return messageId;
    }
    
    public Version getMessageVersion() {
        return msgVersion;
    }
    
    public void write(OutputStream os) throws IOException {
        serialize();
        
        MessageOutputStream out = new MessageOutputStream(os);
        
        // --- GNUTELLA HEADER ---
        
        messageId.write(out); // 0-15
        out.writeByte(DHTMessage.F_DHT_MESSAGE); // 16
        out.writeVersion(getMessageVersion()); //17-18
        
        // Length is in Little-Endian!
        out.write((payload.length      ) & 0xFF); // 19-22
        out.write((payload.length >>  8) & 0xFF);
        out.write((payload.length >> 16) & 0xFF);
        out.write((payload.length >> 24) & 0xFF);
        
        // --- GNUTELLA PAYLOAD ---
        out.write(payload, 0, payload.length); // 23-n
    }
    
    private synchronized void serialize() throws IOException {
        if (payload != null) {
            return;
        }
        
        ByteBufferOutputStream baos = new ByteBufferOutputStream(640);
        MessageOutputStream out = new MessageOutputStream(baos);
        
        // --- MOJITO HEADER CONINUED ---
        writeHeader(out);
        
        // --- MOJITO BODY ---
        writeBody(out);
        
        out.close();
        payload = baos.toByteArray();
    }
    
    protected void writeHeader(MessageOutputStream out) throws IOException {
        out.writeOpCode(getOpCode()); // 0
        out.writeVendor(getContact().getVendor()); // 1-3
        out.writeVersion(getContact().getVersion()); // 4-5
        out.writeKUID(getContact().getNodeID()); // 6-25
        out.writeSocketAddress(getContact().getContactAddress()); // 26-33
        out.writeByte(getContact().getInstanceID()); // 34
        out.writeByte(getContact().getFlags()); // 35
        
        // Write the extended header
        writeExtendedHeader(out); // 36-
    }
    
    private void writeExtendedHeader(MessageOutputStream out) throws IOException {
        out.writeShort(0); // 36-37
    }
    
    protected abstract void writeBody(MessageOutputStream out) throws IOException;
    
    protected void initSignature(Signature signature) 
            throws SignatureException {
        try {
            // Destination
            SocketAddress myExternalAddress = context.getContactAddress();
            signature.update(NetworkUtils.getBytes(myExternalAddress, java.nio.ByteOrder.BIG_ENDIAN));

            // Source
            SocketAddress contactAddress = getContact().getContactAddress();
            signature.update(NetworkUtils.getBytes(contactAddress, java.nio.ByteOrder.BIG_ENDIAN));
        } catch (UnknownHostException err) {
            throw new SignatureException(err);
        }
    }
    
    public final int getLength() {
        try {
            serialize();
            return payload.length;
        } catch (IOException impossible) {
            return -1;
        }
    }
}
