package org.limewire.listener;

import org.limewire.util.Objects;

/**
 * A default, simple implementation of Event.
 */
public class DefaultDataTypeEvent<D, T> extends DefaultDataEvent<D> implements DataTypeEvent<D, T> {
    
    private final T type;
    
    public DefaultDataTypeEvent(D data, T type) {
        super(data);
        this.type = Objects.nonNull(type, "type");
    }

    @Override
    public T getType() {
        return type;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + type.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) {
            return false;
        }
        if(!(obj instanceof DataTypeEvent)) {
            return false;
        }
        return type.equals(((DataTypeEvent)obj).getType());
    }

    @Override
    public String toString() {
        return super.toString() + ", type: " + type;
    }
}
