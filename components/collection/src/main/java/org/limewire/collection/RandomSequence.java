package org.limewire.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * Generates all the integers from 0 to a certain limit in a 
 * random fashion where each number is generated only once per cycle.
 * <p>
 * <code>RandomSequence</code> also implements the <code>Iterable</code> 
 * interface that iterates over a single cycle.
 * <p>
 * For more information regarding the logic, see <a href=
 * "http://en.wikipedia.org/wiki/Linear_congruential_generator">Linear 
 * congruential generator</a>.
 * <p>
 * Thanks to Cyclonus for the pow2 hint.

 <pre>
    for(Integer o : new RandomSequence(10))
        System.out.println(o);

    Random Output:
        4
        7
        2
        5
        0
        3
        1
        8
        6
        9
</pre>
 */

public class RandomSequence implements Iterable<Integer> {
    private final int end;
    private final long pow2, a;
    private long seed;
    
    public RandomSequence(int end) {
        this.end = end;
        
        // empty or single-element sequence
        if (end <= 1) {
            pow2 = 0; 
            a = 0;
            return;
        }
        
        long pow = 1;
        while (pow < end)
            pow <<= 1;
        pow2 = pow - 1;
        a = (((int)(Math.random() * (pow2 >> 2))) << 2) + 1;
        seed = (int)(Math.random() * end);
    }
    
    public int nextInt() {
        if (end < 1)
            throw new NoSuchElementException();
        
        if (end == 1)
            return 0;
        
        do {
            seed = (a * seed + 3 ) & pow2;
        } while (seed >= end || seed < 0);
        return (int)seed;
    }
    
    public Iterator<Integer> iterator() {
        return new RandomIterator();
    }
    
    private class RandomIterator extends  UnmodifiableIterator<Integer> {
        private int given;
        public boolean hasNext() {
            return given < end;
        }
        
        public Integer next() {
            if (!hasNext())
                throw new NoSuchElementException();
            given++;
            return nextInt();
        }
    }
}
