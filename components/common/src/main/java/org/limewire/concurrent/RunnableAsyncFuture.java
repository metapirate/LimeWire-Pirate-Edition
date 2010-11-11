package org.limewire.concurrent;

import java.util.concurrent.RunnableFuture;

/** 
 * An extension of {@link RunnableFuture} for {@link AsyncFuture}.
 */
public interface RunnableAsyncFuture<V> extends RunnableListeningFuture<V> {

}
