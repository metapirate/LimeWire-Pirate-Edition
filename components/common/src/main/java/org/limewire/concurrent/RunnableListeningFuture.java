package org.limewire.concurrent;

import java.util.concurrent.RunnableFuture;

/** An extension of {@link RunnableFuture} for {@link ListeningFuture}. */
public interface RunnableListeningFuture<V> extends RunnableFuture<V>, ListeningFuture<V> {

}
