package org.limewire.collection.glazedlists;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ca.odell.glazedlists.CollectionList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.FreezableList;
import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.GroupingList;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.PopularityList;
import ca.odell.glazedlists.RangeList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.ThresholdList;
import ca.odell.glazedlists.TransactionList;
import ca.odell.glazedlists.UniqueList;
import ca.odell.glazedlists.CollectionList.Model;
import ca.odell.glazedlists.FunctionList.Function;
import ca.odell.glazedlists.ObservableElementList.Connector;
import ca.odell.glazedlists.ThresholdList.Evaluator;
import ca.odell.glazedlists.impl.ReadOnlyList;
import ca.odell.glazedlists.impl.SubEventList;
import ca.odell.glazedlists.impl.ThreadSafeList;
import ca.odell.glazedlists.impl.swing.SwingThreadProxyEventList;
import ca.odell.glazedlists.io.CachingList;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.util.concurrent.Lock;

/**
 * A factory for creating all kinds of GlazedLists.
 * This is necessary when creating any list with a source,
 * in a multi-threaded environment.
 */
public class GlazedListsFactory {
    
    /** Returns a snapshot copy of the list. */
    public static <E> List<E> copyList(EventList<? extends E> source) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {
            return new ArrayList<E>(source);
        } finally {
            lock.unlock();
        }
    }
    
    public static CachingList cachingList(EventList source, int maxSize) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new CachingList(source, maxSize);
        } finally {
            lock.unlock();
        }
    }
    
    public static <S, E> CollectionList<S, E> collectionList(EventList<S> source, Model<S, E> model) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new CollectionList<S, E>(source, model);
        } finally {
            lock.unlock();
        }
    }
    
    
    public static <E> FilterList<E> filterList(EventList<E> source) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new FilterList<E>(source);
        } finally {
            lock.unlock();
        }
    }

    public static <E> FilterList<E> filterList(EventList<E> source, Matcher<? super E> matcher) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new FilterList<E>(source, matcher);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> FilterList<E> filterList(EventList<E> source, MatcherEditor<? super E> matcherEditor) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new FilterList<E>(source, matcherEditor);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> FreezableList<E> freezableList(EventList<E> source) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new FreezableList<E>(source);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E, S> FunctionList<S, E> functionList(EventList<S> source, Function<S,E> forward) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new FunctionList<S, E>(source, forward);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E, S> FunctionList<S, E> functionList(EventList<S> source, Function<S,E> forward, Function<E,S> reverse) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new FunctionList<S, E>(source, forward, reverse);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E, S> SimpleFunctionList<S, E> simpleFunctionList(EventList<S> source, Function<S,E> forward) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new SimpleFunctionList<S, E>(source, forward);
        } finally {
            lock.unlock();
        }
    } 
    
    public static <E> GroupingList<E> groupingList(EventList<E> source) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new GroupingList<E>(source);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> GroupingList<E> groupingList(EventList<E> source, Comparator<E> comparator) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new GroupingList<E>(source, comparator);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> ObservableElementList<E> observableElementList(EventList<E> source, Connector<E> elementConnector) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new ObservableElementList<E>(source, elementConnector);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> PopularityList<E> popularityList(EventList<E> source, Comparator<E> uniqueComparator) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new PopularityList<E>(source, uniqueComparator);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> PopularityList<E> popularityList(EventList<E> source) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new PopularityList<E>(source);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> RangeList<E> rangeList(EventList<E> source) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new RangeList<E>(source);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> ThresholdList<E> thresholdList(EventList<E> source, String propertyName) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new ThresholdList<E>(source, propertyName);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> ThresholdList<E> thresholdList(EventList<E> source, Evaluator<E> evaluator) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new ThresholdList<E>(source, evaluator);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> ReadOnlyList<E> readOnlyList(EventList<E> source) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new ReadOnlyList<E>(source);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> SortedList<E> sortedList(EventList<E> source) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new SortedList<E>(source);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> SortedList<E> sortedList(EventList<E> source, Comparator<? super E> comparator) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new SortedList<E>(source, comparator);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> SubEventList<E> subEventList(EventList<E> source, int startIndex, int endIndex, boolean automaticallyRemove) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new SubEventList<E>(source, startIndex, endIndex, automaticallyRemove);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> SwingThreadProxyEventList<E> swingThreadProxyEventList(EventList<E> source) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new SwingThreadProxyEventList<E>(source);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> ThreadSafeList<E> threadSafeList(EventList<E> source) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new ThreadSafeList<E>(source);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> UniqueList<E> uniqueList(EventList<E> source) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new UniqueList<E>(source);
        } finally {
            lock.unlock();
        }
    }
 
    public static <E> UniqueList<E> uniqueList(EventList<E> source, Comparator<E> comparator) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new UniqueList<E>(source, comparator);
        } finally {
            lock.unlock();
        }
    }
    
    public static <E> TransactionList<E> transactionList(EventList<E> source) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {            
            return new TransactionList<E>(source);
        } finally {
            lock.unlock();
        }
    }
    
}
