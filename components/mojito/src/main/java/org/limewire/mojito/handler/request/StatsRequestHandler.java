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
 
package org.limewire.mojito.handler.request;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.StatsRequest;
import org.limewire.mojito.messages.StatsResponse;

/**
 * Handles incoming Stats requests.
 */
public class StatsRequestHandler extends AbstractRequestHandler {

    private static final Log LOG = LogFactory.getLog(StatsRequestHandler.class);
    
    public StatsRequestHandler(Context context) {
        super(context);
    }

    @Override
    public void request(RequestMessage message) throws IOException {
        
        StatsRequest request = (StatsRequest) message;
        
        if (!request.isSecure()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(message.getContact() + " sent us an invalid Stats Request");
            }
            return;
        }
        
        StringWriter writer = new StringWriter();
        
        switch(request.getType()) {
            case STATISTICS:
                break;
            case DATABASE:
                writer.write(context.getDatabase().toString());
                break;
            case ROUTETABLE:
                writer.write(context.getRouteTable().toString());
                break;
            default:
                if (LOG.isErrorEnabled()) {
                    LOG.error("Unknown stats request: " + request.getType());
                }
                return;
                
        }
        
        StatsResponse response = context.getMessageHelper()
            .createStatsResponse(message, writer.toString().getBytes("ISO-8859-1"));
        
        context.getMessageDispatcher().send(message.getContact(), response);
    }
}
