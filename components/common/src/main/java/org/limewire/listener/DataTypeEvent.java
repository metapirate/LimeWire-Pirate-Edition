package org.limewire.listener;

/**
 * An event for a given data and type.
 */
public interface DataTypeEvent<D, T> extends DataEvent<D>, TypedEvent<T> {

}
