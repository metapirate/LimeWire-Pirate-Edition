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

package org.limewire.mojito.manager;

import java.net.SocketAddress;
import java.util.Set;

import org.limewire.mojito.Context;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureTask;
import org.limewire.mojito.concurrent.DHTTask;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.routing.Contact;

/**
 * Manages the bootstrap process and determines
 * whether or not the local Node is bootstrapped.
 */
public class BootstrapManager extends AbstractManager<BootstrapResult> {
    
    /**
     * The BootstrapFuture object that will contain the result
     * of our bootstrap.
     * LOCKING: this
     */
    private BootstrapFuture future = null;
    
    /** 
     * A flag for whether or not we're bootstrapped 
     */
    private boolean bootstrapped = false;
    
    public BootstrapManager(Context context) {
        super(context);
    }
    
    /**
     * Returns true if this Node has bootstrapped successfully.
     */
    public synchronized boolean isBootstrapped() {
        return bootstrapped;
    }
    
    /**
     * An internal method to set the bootstrapped flag.
     * Meant for internal use only.
     */
    public synchronized void setBootstrapped(boolean bootstrapped) {
        this.bootstrapped = bootstrapped;
    }
    
    /**
     * Returns true if this Node is currently bootstrapping.
     */
    public synchronized boolean isBootstrapping() {
        return future != null;
    }
    
    public void stop() {
        BootstrapFuture f;
        synchronized(this) {
            f = future;
            future = null;
        }
        if (f != null) 
            f.cancel(true);
    }
    
    /**
     * Tries to bootstrap the local Node from the given Contact.
     */
    public DHTFuture<BootstrapResult> bootstrap(Contact node) {
        if (node == null) {
            throw new NullPointerException("Contact is null");
        }
        
        if (node.equals(context.getLocalNode())) {
            throw new IllegalArgumentException("Cannot bootstrap from local Node");
        }
        
        // Make sure there is only one bootstrap process active!
        // Having parallel bootstrap processes is too expensive!
        stop();
        
        // Bootstrap...
        BootstrapProcess process = new BootstrapProcess(context, this, node);
        BootstrapFuture future = new BootstrapFuture(process);
        synchronized (this) {
            this.future = future;
        }
        context.getDHTExecutorService().execute(future);

        return future;
    }
    
    /**
     * Tries to bootstrap the local Node from any of the given SocketAddresses.
     */
    public DHTFuture<BootstrapResult> bootstrap(Set<? extends SocketAddress> dst) {
        if (dst == null) {
            throw new NullPointerException("Set<SocketAddress> is null");
        }
        
        // Make sure there is only one bootstrap process active!
        // Having parallel bootstrap processes is too expensive!
        stop();
        
        // Bootstrap...
        BootstrapProcess process = new BootstrapProcess(context, this, dst);
        BootstrapFuture future = new BootstrapFuture(process);
        synchronized (this) {
            this.future = future;
        }
        context.getDHTExecutorService().execute(future);

        return future;
    }
    
    private class BootstrapFuture extends DHTFutureTask<BootstrapResult> {

        public BootstrapFuture(DHTTask<BootstrapResult> task) {
            super(context, task);
        }

        @Override
        protected void done0() {
            stop();
        }
    }
}
