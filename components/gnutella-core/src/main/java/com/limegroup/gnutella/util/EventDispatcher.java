package com.limegroup.gnutella.util;

import java.util.EventListener;
import java.util.EventObject;

/**
 * Defines an interface to add or remove listeners and to dispatch events to 
 * the listeners.
 *
 * @param <T> the event to dispatch
 * @param <Y> listener which dispatch events
 */
public interface EventDispatcher<T extends EventObject, Y extends EventListener> {
	public void dispatchEvent(T event);
	public void addEventListener(Y listener);
	public void removeEventListener(Y listener);
}
