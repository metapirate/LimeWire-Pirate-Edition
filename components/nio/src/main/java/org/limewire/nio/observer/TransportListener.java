package org.limewire.nio.observer;

/**
 * Defines an interface that notifies when events 
 * are generated by a transport layer.
 * <p>
 * For example, a transport listener notifies the NIO thread when a selector 
 * has a pending event.
 */
public interface TransportListener {
	public void eventPending();
}
