package org.limewire.jmx;

import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MXBean;

/**
 * A JMX Bean for {@link AtomicInteger}s
 */
@MXBean
public interface AtomicIntegerBean {

    /**
     * Returns the current value
     */
    public int getValue();
    
    /**
     * Sets the current value
     */
    public void setValue(int value);
    
    /**
     * Returns true if this {@link AtomicInteger} is a read-only value
     */
    public boolean isReadOnly();
    
    /**
     * The default implementation of {@link AtomicIntegerBean}
     */
    public static class Impl implements AtomicIntegerBean {
        
        private final AtomicInteger atomic;
        
        private final boolean readOnly;
        
        /**
         * Creates an {@link AtomicIntegerBean}
         */
        public Impl(AtomicInteger atomic) {
            this(atomic, false);
        }
        
        /**
         * Creates an {@link AtomicIntegerBean}
         */
        public Impl(AtomicInteger atomic, boolean readOnly) {
            this.atomic = atomic;
            this.readOnly = readOnly;
        }

        @Override
        public int getValue() {
            return atomic.get();
        }

        @Override
        public boolean isReadOnly() {
            return readOnly;
        }

        @Override
        public void setValue(int value) {
            if (readOnly) {
                throw new IllegalStateException();
            }
            
            atomic.set(value);
        }
    }
}
