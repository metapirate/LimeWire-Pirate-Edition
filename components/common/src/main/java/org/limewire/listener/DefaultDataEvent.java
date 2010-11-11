package org.limewire.listener;

import org.limewire.util.Objects;

public class DefaultDataEvent<D> implements DataEvent<D> {
    
    private final D data;
    
    public DefaultDataEvent(D data) {
        this.data = Objects.nonNull(data, "data");
    }
    
    @Override
    public D getData() {
        return data;
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof DataEvent)) {
            return false;
        }
        return data.equals(((DataEvent)obj).getData());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " - data: " + data;
    }
}
