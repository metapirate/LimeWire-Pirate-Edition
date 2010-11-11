package org.limewire.listener;

import com.google.inject.Inject;

/**
 * An extension to <code>EventListener</code> for listeners that want to register at
 * injection time.
 */
public interface RegisteringEventListener<E> extends EventListener<E> {
    /**
     * An injection time method that allows an <code>EventListener</code>
     * to register with its corresponding <code>ListenSupport</code>.
     * <p>
     * NOTE: implementors must annotate themselves with the
     * <code>@Inject</code> annotation; the <code>interface</code>
     * level annotation only serves as documentation.
     */
    @Inject
    public void register(ListenerSupport<E> listenerSupport);
}
