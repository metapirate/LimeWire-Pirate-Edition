package org.limewire.listener;

import org.limewire.util.Objects;

/**
 * Default class for events that can carry data in addition to a source
 * and an event type. 
 */
public class DefaultDataSourceTypeEvent<S, T, D> extends DefaultSourceTypeEvent<S, T> implements DataSourceTypeEvent<D, S, T> {

    private final D data;

    public DefaultDataSourceTypeEvent(S source, T event, D data) {
        super(source, event);
        this.data = Objects.nonNull(data, "data");
    }
    
    public D getData() {
        return data;
    }
    
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + data.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DataSourceTypeEvent)) {
            return false;
        }
        if(!super.equals(obj)) {
            return false;
        }
        return data.equals(((DataSourceTypeEvent)obj).getData());
    }
    
    @Override
    public String toString() {
        return super.toString() + ", data: " + data;
    }

}
