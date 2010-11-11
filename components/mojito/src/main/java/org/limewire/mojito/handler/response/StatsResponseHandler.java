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
 
package org.limewire.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.exceptions.DHTBackendException;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.messages.StatsResponse;
import org.limewire.mojito.result.StatsResult;


/**
 * The StatsResponseHandler handles responses for Stats requests.
 */
public class StatsResponseHandler extends AbstractResponseHandler<StatsResult> {

    private static final Log LOG = LogFactory.getLog(StatsResponseHandler.class);
    
    public StatsResponseHandler(Context context) {
        super(context);
    }
    
    @Override
    protected void start() throws DHTException {
    }

    @Override
    protected void response(ResponseMessage message, long time) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Stats request to " + message.getContact() + " succeeded");
        }
        
        StatsResponse response = (StatsResponse)message;
        setReturnValue(new StatsResult(response.getStatistics()));
    }

    @Override
    protected void timeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException {
        fireTimeoutException(nodeId, dst, message, time);
    }
    
    @Override
    protected void error(KUID nodeId, SocketAddress dst, RequestMessage message, IOException e) {
        setException(new DHTBackendException(nodeId, dst, message, e));
    }
}
