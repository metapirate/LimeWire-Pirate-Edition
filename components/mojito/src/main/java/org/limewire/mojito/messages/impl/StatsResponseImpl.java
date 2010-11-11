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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.limewire.mojito.Context;
import org.limewire.mojito.io.MessageInputStream;
import org.limewire.mojito.io.MessageOutputStream;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.messages.StatsResponse;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;


/**
 * An implementation of StatsResponse.
 */
public class StatsResponseImpl extends AbstractResponseMessage
        implements StatsResponse {

    private final byte[] statistics;

    public StatsResponseImpl(Context context, 
            Contact contact, MessageID messageId, byte[] statistics) {
        super(context, OpCode.STATS_RESPONSE, contact, messageId, Version.ZERO);

        this.statistics = statistics;
    }

    public StatsResponseImpl(Context context, SocketAddress src, 
            MessageID messageId, Version msgVersion, MessageInputStream in) throws IOException {
        super(context, OpCode.STATS_RESPONSE, src, messageId, msgVersion, in);
        
        byte[] s = in.readStatistics();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(s);
        GZIPInputStream gz = new GZIPInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(s.length * 2);
        
        byte[] b = new byte[2048];
        int len = -1;
        while((len = gz.read(b)) != -1) {
            baos.write(b, 0, len);
        }
        gz.close();
        baos.close();
        
        this.statistics = baos.toByteArray();
    }
    
    public byte[] getStatistics() {
        return statistics;
    }

    @Override
    protected void writeBody(MessageOutputStream out) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gz = new GZIPOutputStream(baos);
        gz.write(statistics);
        gz.close();
        byte[] s = baos.toByteArray();
        
        out.writeStatistics(s);
    }
    
    @Override
    public String toString() {
        try {
            return "StatsResponse: " + new String(statistics, "ISO-8859-1");
        } catch (UnsupportedEncodingException err) {
            throw new RuntimeException(err);
        }
    }
}
