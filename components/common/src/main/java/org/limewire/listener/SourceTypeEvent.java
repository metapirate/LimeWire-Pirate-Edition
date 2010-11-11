package org.limewire.listener;

/**
 * An event for a given source and type.
 */
public interface SourceTypeEvent<S, T> extends SourcedEvent<S>, TypedEvent<T> {

}
