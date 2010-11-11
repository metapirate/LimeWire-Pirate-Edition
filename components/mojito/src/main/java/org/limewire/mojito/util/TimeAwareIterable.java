/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.limewire.mojito.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.limewire.collection.CollectionUtils;

/**
 * TimeAwareIterable has a maximum time for which it's valid
 * and it takes the average time between next() and hasNext()
 * calls respectively into consideration to select and return 
 * elements. The hasNext() method returns false if all elements
 * have been returned or if there's not enough time remaining
 * (i.e. if the average time between next()/hasNext() calls is
 * higher than the remaining time).
 * <p>
 * The selection of elements looks about this. The gap between
 * selected elements gets bigger and bigger as we're approaching
 * the maximum time for which this Iterable is valid.
 * <pre>
 * a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t-u-v-w-x-y-z
 * ^ ^   ^     ^       ^         ^           ^
 * 0 ------------------- time --------------------> max
 * </pre>
 * Implementation detail: Unlike with regular Iterators where you 
 * may call next() multiple times in a row without checking whether 
 * or not it has more elements you must check on every iteration
 * if there're more elements left.
 */
public class TimeAwareIterable<E> implements Iterable<E> {
    
    private final int sampleSize;
    
    private final long maxTime;

    private final List<? extends E> elements;
    
    /**
     * Creates a TimeAwareIterable with a default sample size of 10.
     * 
     * @param maxTime the maximum time this Iterable is valid
     * @param elements the elements to process
     */
    public TimeAwareIterable(long maxTime, Collection<? extends E> elements) {
        this(10, maxTime, elements);
    }
    
    /**
     * Creates a TimeAwareIterable.
     * 
     * @param sampleSize the sample size to compute the average time between calls
     * @param maxTime the maximum time this Iterable is valid
     * @param elements the elements to process
     */
    public TimeAwareIterable(int sampleSize, long maxTime, 
            Collection<? extends E> elements) {
        this.sampleSize = sampleSize;
        this.maxTime = maxTime;
        this.elements = CollectionUtils.toList(elements);
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<E> iterator() {
        return new TimeAwareIterator();
    }

    /**
     * The actual Iterator.
     */
    private class TimeAwareIterator implements Iterator<E> {
        
        private final long[] samples = new long[sampleSize];
        
        private int sampleCount = 0;
        
        private int sampleIndex = 0;
        
        private long startTime = -1L;
        
        private long lastTime = -1L;
        
        private int currentIndex = -1;
        
        private int nextIndex = 0;
        
        public boolean hasNext() {
            if (currentIndex == -1 && nextIndex < elements.size()) {
                long currentTime = System.currentTimeMillis();
                long average = 0L;
                
                if (startTime == -1L) {
                    startTime = currentTime;
                }
                
                if (lastTime != -1L) {
                    average = addSample(currentTime - lastTime);
                }
                lastTime = currentTime;
                
                // If there's not enough time left then exit
                long timeRemaining = maxTime - (currentTime - startTime);
                if (timeRemaining >= average) {
                    currentIndex = nextIndex;
                    nextIndex++;
                    int elementsRemaining = elements.size() - nextIndex;
                    assert (elementsRemaining >= 0);
                    nextIndex += (int)(average*elementsRemaining/timeRemaining);
                }
            }
            
            return currentIndex != -1;
        }
        
        public E next() {
            if (startTime == -1L || currentIndex == -1) {
                throw new NoSuchElementException();
            }
            
            int index = currentIndex;
            currentIndex = -1;
            return elements.get(index);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        private long addSample(long time) {
            if (time >= 0L) {
                samples[sampleIndex] = time;
                sampleIndex = (sampleIndex + 1) % samples.length;
                
                if (sampleCount < samples.length) {
                    sampleCount++;
                }
            }
            
            return getAverage();
        }
        
        private long getAverage() {
            if (sampleCount == 0) {
                return 0L;
            }
            
            long sum = 0L;
            for (long time : samples) {
                sum += time;
            }
            return sum/sampleCount;
        }
    }
}
