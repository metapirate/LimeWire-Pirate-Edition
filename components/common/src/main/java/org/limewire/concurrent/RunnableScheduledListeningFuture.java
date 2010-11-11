package org.limewire.concurrent;

import java.util.concurrent.RunnableScheduledFuture;

/** An extension of {@link RunnableScheduledFuture} for use with {@link ListeningFuture}. */
public interface RunnableScheduledListeningFuture<V> extends RunnableScheduledFuture<V>, ListeningFuture<V> {

}
