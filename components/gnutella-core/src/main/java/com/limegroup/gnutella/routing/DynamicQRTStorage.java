package com.limegroup.gnutella.routing;

import java.util.Iterator;

/**
 * A delegating QRTTableStorage that switches between
 * SparseSetStorage and BitSetStorage implementation depending
 * on the number of entries. 
 */
class DynamicQRTStorage implements QRTTableStorage {

    /**
     * A treshold at which a 128kbit BitSetStorage will use less memory
     * than a SparseSetStorage.  Determined experimentally.
     */
    private final float TRESHOLD = 2.5f;
    
    /**
     * The current storage we use.
     */
    private QRTTableStorage storage;
    
    DynamicQRTStorage(int length) {
        this(new SparseSetStorage(length));
    }
    
    private DynamicQRTStorage(QRTTableStorage storage) {
        this.storage = storage;
    }
    
    public void clear(int hash) {
        storage.clear(hash);
    }

    public void compact() {
        changeStorage();
        storage.compact();
    }

    /**
     * changes the storage implementation if necessary.
     */
    private void changeStorage() {
        if (storage.getPercentFull() > TRESHOLD) {
            if (storage instanceof SparseSetStorage) {
                QRTTableStorage bitSet = new BitSetQRTTableStorage(storage.maxSize());
                bitSet.or(storage);
                storage = bitSet;
            }
        } else if (storage instanceof BitSetQRTTableStorage) {
            QRTTableStorage sparse = new SparseSetStorage(storage.maxSize());
            sparse.or(storage);
            storage = sparse;
        }
    }
    
    public double getPercentFull() {
        return storage.getPercentFull();
    }

    public int getUnitsInUse() {
        return storage.getUnitsInUse();
    }

    public int getUnusedUnits() {
        return storage.getUnusedUnits();
    }

    public int numUnitsWithLoad(int load) {
        return storage.numUnitsWithLoad(load);
    }

    public void or(QRTTableStorage other) {
        if (other instanceof DynamicQRTStorage) 
            other = ((DynamicQRTStorage)other).storage; // so we can optimize
        storage.or(other);
    }

    public QRTTableStorage resize(int newSize) {
        return storage.resize(newSize);
    }

    public void set(int hash) {
        storage.set(hash);
    }

    public void xor(QRTTableStorage other) {
        if (other instanceof DynamicQRTStorage) 
            other = ((DynamicQRTStorage)other).storage; // so we can optimize
        storage.xor(other);
    }

    public int cardinality() {
        return storage.cardinality();
    }

    public boolean get(int i) {
        return storage.get(i);
    }

    public int maxSize() {
        return storage.maxSize();
    }

    public int nextClearBit(int i) {
        return storage.nextClearBit(i);
    }

    public int nextSetBit(int i) {
        return storage.nextSetBit(i);
    }

    @Override
    public DynamicQRTStorage clone() throws CloneNotSupportedException {
        return new DynamicQRTStorage(storage.clone());
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof DynamicQRTStorage)
            o = ((DynamicQRTStorage)o).storage;
        return storage.equals(o);
    }
    
    public Iterator<Integer> iterator() {
        return storage.iterator();
    }
    
    @Override
    public String toString() {
        return "DynamicQRTStorage: " + storage.toString();
    }
}
