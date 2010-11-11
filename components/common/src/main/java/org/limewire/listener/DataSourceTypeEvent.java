package org.limewire.listener;

/**
 * An event for a given data, source and type.
 */
public interface DataSourceTypeEvent<D, S, T> extends SourcedEvent<S>, TypedEvent<T>, DataEvent<D> {

}
