package org.limewire.jmx;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.MXBean;

/**
 * A JMX Bean for {@link AtomicLong}s
 */
@MXBean
public interface AtomicLongBean {

    /**
     * Returns the current value
     */
    public long getValue();
    
    /**
     * Sets the current value
     */
    public void setValue(long value);
    
    /**
     * Returns true if this {@link AtomicLong} is a read-only value
     */
    public boolean isReadOnly();
    
    /**
     * The default implementation of {@link AtomicLongBean}
     */
    public static class Impl implements AtomicLongBean {
        
        private final AtomicLong atomic;
        
        private final boolean readOnly;
        
        /**
         * Creates an {@link AtomicLongBean}
         */
        public Impl(AtomicLong atomic) {
            this(atomic, false);
        }
        
        /**
         * Creates an {@link AtomicLongBean}
         */
        public Impl(AtomicLong atomic, boolean readOnly) {
            this.atomic = atomic;
            this.readOnly = readOnly;
        }

        @Override
        public long getValue() {
            return atomic.get();
        }

        @Override
        public boolean isReadOnly() {
            return readOnly;
        }

        @Override
        public void setValue(long value) {
            if (readOnly) {
                throw new IllegalStateException();
            }
            
            atomic.set(value);
        }
    }
}
