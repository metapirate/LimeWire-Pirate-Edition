package com.limegroup.gnutella.routing;

import java.util.Iterator;

import org.limewire.collection.SparseIntSet;

/**
 * A QRTTableStorage that uses a SparseIntSet to store
 * the list of routing table entries. 
 */
class SparseSetStorage implements QRTTableStorage {

    private final SparseIntSet set; 
    private final int length;
    
    SparseSetStorage(int length) {
        this(new SparseIntSet(), length);
    }
    
    private SparseSetStorage(SparseIntSet set, int length) {
        this.set = set;
        this.length = length;
    }
    
    public void clear(int hash) {
        set.remove(hash);
    }

    public void compact() {
        set.compact();
    }

    public double getPercentFull() {
        return set.size() * 100.0 / length;
    }

    public int getUnitsInUse() {
        // this is to approximate what a BitSetStorage would return
        return set.getActualMemoryUsed() / 8;
    }

    public int getUnusedUnits() {
        return 0;
    }

    public int numUnitsWithLoad(int load) {
        return -1;
    }

    public void or(QRTTableStorage other) {
        if (other instanceof SparseSetStorage) {
            SparseSetStorage optimized = (SparseSetStorage)other;
            set.addAll(optimized.set);
        } else {
            for (int i : other) 
                set.add(i);
        }
    }

    public QRTTableStorage resize(int newSize) {
        if (newSize == length)
            return this;
        SparseIntSet s = new SparseIntSet();
        for (int i : set) {
            final int firstSet = (int)(((long)i * newSize) / length);
            i = nextClearBit(i+1);
            final int lastNotSet = (int)(((long)i * newSize - 1) / length + 1);
            for (int j = firstSet; j < lastNotSet; j++)
                s.add(j);
        }
        return new SparseSetStorage(s,newSize);
    }

    public void set(int hash) {
        set.add(hash);
    }

    public void xor(QRTTableStorage other) {
        // probably slow
        for (int i = 0; i < length; i++) {
            if (get(i) != other.get(i))
                set(i);
            else
                clear(i);
        }
    }

    public int cardinality() {
        return set.size();
    }

    public boolean get(int i) {
        return set.contains(i);
    }

    public int maxSize() {
        return length;
    }

    public int nextClearBit(int i) {
        // slow
        for (int j = i; j < length; j++) {
            if (!set.contains(j))
                return j;
        }
        return -1;
    }

    public int nextSetBit(int i) {
        // fast
        return set.nextSetBit(i);
    }
    
    @Override
    public QRTTableStorage clone() throws CloneNotSupportedException {
        SparseIntSet copy = new SparseIntSet();
        copy.addAll(set);
        return new SparseSetStorage(copy, length);
    }
    
    @Override
    public boolean equals(Object o) {
        if (! (o instanceof QRTTableStorage))
            return false;
        
        // check max sizes
        QRTTableStorage bf = (QRTTableStorage)o;
        if (bf.maxSize() != maxSize())
            return false;
        
        // try fast first
        if (o instanceof SparseSetStorage) {
            SparseSetStorage sss = (SparseSetStorage)o;
            return set.containsAll(sss.set) && set.size() == sss.set.size();
        }
        
        if (bf.cardinality() != cardinality())
            return false;
        
        // otherwise iterate
        Iterator<Integer> it = bf.iterator();
        for (int i : this)
            if (i != it.next())
                return false;
        
        return true;
    }

    public Iterator<Integer> iterator() {
        return set.iterator();
    }
    
    @Override
    public String toString() {
        return "SparseSetStorage: " + set;
    }
}
