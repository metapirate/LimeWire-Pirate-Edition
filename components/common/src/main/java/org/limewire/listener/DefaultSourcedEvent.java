package org.limewire.listener;

import org.limewire.util.Objects;

public class DefaultSourcedEvent<S> implements SourcedEvent<S> {
    
    private final S source;
    
    public DefaultSourcedEvent(S source) {
        this.source = Objects.nonNull(source, "source");
    }
    
    public S getSource() {
        return source;
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SourcedEvent)) {
            return false;
        }
        return source.equals(((SourcedEvent)obj).getSource());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " - source: " + source;
    }
}
