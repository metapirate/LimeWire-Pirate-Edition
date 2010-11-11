package org.limewire.collection.glazedlists;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.FunctionList.Function;
import ca.odell.glazedlists.event.ListEvent;

/**
 * A simpler FunctionList that does not actually keep a map of old -> new, 
 * but just does a transformation in-place of old -> new.
 * 
 * This should use less memory than FunctionList, at the expense of CPU
 * and possibly garbage collection.
 */
public class SimpleFunctionList<S, E> extends TransformedList<S, E> {
    private final Function<S, E> function;

    public SimpleFunctionList(EventList<S> source, Function<S, E> function) {
        super(source);
        this.function = function;
        source.addListEventListener(this);
    }

    @Override
    public E get(int index) {
        final S elem = source.get(index);
        return function.evaluate(elem);
    }

    @Override
    public void listChanged(ListEvent<S> listChanges) {
        updates.forwardEvent(listChanges);
    }

    @Override
    protected boolean isWritable() {
        return false;
    }
}