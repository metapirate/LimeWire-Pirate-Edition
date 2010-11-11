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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.handler.RequestHandler;
import org.limewire.mojito.messages.RequestMessage;


/**
 * An abstract base class for <code>RequestHandler</code>s.
 */
abstract class AbstractRequestHandler implements RequestHandler {
    
    private static final Log LOG = LogFactory.getLog(AbstractRequestHandler.class);
    
    /** A handle to Context */
    protected final Context context;
    
    public AbstractRequestHandler(Context context) {
        this.context = context;
    }

    /**
     * See handleRequest()
     */
    protected abstract void request(RequestMessage message) throws IOException;
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.handler.RequestHandler#handleRequest(com.limegroup.mojito.messages.RequestMessage)
     */
    public void handleRequest(RequestMessage message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(message.getContact() + " is requesting " + message);
        }
        
        request(message);
    }
}
