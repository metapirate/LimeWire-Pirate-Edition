package org.limewire.collection;

/**
 * Something that generates a tree node such as Tiger hash. 
 */
public interface NodeGenerator {
    byte [] generate(byte [] left, byte [] right);
    
    static class NullGenerator implements NodeGenerator {
        public byte [] generate(byte [] left, byte [] right) {
            return null;
        }
    }
}