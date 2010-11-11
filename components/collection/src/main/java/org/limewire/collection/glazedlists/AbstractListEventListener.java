package org.limewire.collection.glazedlists;

import java.util.ArrayList;
import java.util.List;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.util.concurrent.Lock;

/**
 * An abstract ListEventListener for use with glazed lists when you really need
 * the deleted object. Use this class sparingly, as it will cache the contents
 * of the list.
 * <p>
 * To use, subclass {@link AbstractListEventListener} and {@link #install(EventList)}
 * it on the list you want to listen to.
 */
public abstract class AbstractListEventListener<E> {
    private final List<E> cache = new ArrayList<E>();

    /**
     * Adds this {@link AbstractListEventListener} as a listener on the source
     * {@link EventList}.  Installation this way is required in order to
     * guarantee that the {@link ListEventListener} has a cache of the current
     * contents of the list so it can call {@link #itemRemoved(Object, int, EventList)}
     * with the correct items.
     */
    public void install(EventList<E> source) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {
            cache.addAll(source);
            source.addListEventListener(new ListEventListener<E>() {
                @Override
                public void listChanged(ListEvent<E> changes) {
                    while (changes.next()) {
                        int type = changes.getType();
                        int idx = changes.getIndex();
                        EventList<E> source = changes.getSourceList();
                        switch (type) {
                        case ListEvent.INSERT:
                            cache.add(idx, changes.getSourceList().get(idx));
                            itemAdded(cache.get(idx), idx, source);
                            break;
                        case ListEvent.DELETE:
                            E removed = cache.remove(idx);
                            itemRemoved(removed, idx, source);
                            break;
                        case ListEvent.UPDATE:
                            E prior = cache.get(idx);
                            cache.set(idx, changes.getSourceList().get(idx));
                            itemUpdated(cache.get(idx), prior, idx, source);
                            break;
                        }
                    }
                }
            });
        } finally {
            lock.unlock();
        }
    }

    /** Notification that an item was added. */
    protected abstract void itemAdded(E item, int idx, EventList<E> source);

    /** Notification that an item was removed. */
    protected abstract void itemRemoved(E item, int idx, EventList<E> source);

    /** Notification that an item was replaced. */
    protected abstract void itemUpdated(E item, E priorItem, int idx, EventList<E> source);

}
