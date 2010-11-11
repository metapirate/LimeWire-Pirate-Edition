package org.limewire.jmx;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MXBean;

/**
 * A JMX Bean for {@link AtomicBoolean}s
 */
@MXBean
public interface AtomicBooleanBean {

    /**
     * Returns the current value
     */
    public boolean getValue();
    
    /**
     * Sets the current value
     */
    public void setValue(boolean value);
    
    /**
     * Returns true if this {@link AtomicBoolean} is a read-only value
     */
    public boolean isReadOnly();
    
    /**
     * The default implementation of {@link AtomicBooleanBean}
     */
    public static class Impl implements AtomicBooleanBean {
        
        private final AtomicBoolean atomic;
        
        private final boolean readOnly;
        
        /**
         * Creates an {@link AtomicBooleanBean}
         */
        public Impl(AtomicBoolean atomic) {
            this(atomic, false);
        }
        
        /**
         * Creates an {@link AtomicBooleanBean}
         */
        public Impl(AtomicBoolean atomic, boolean readOnly) {
            this.atomic = atomic;
            this.readOnly = readOnly;
        }

        @Override
        public boolean getValue() {
            return atomic.get();
        }

        @Override
        public boolean isReadOnly() {
            return readOnly;
        }

        @Override
        public void setValue(boolean value) {
            if (readOnly) {
                throw new IllegalStateException();
            }
            
            atomic.set(value);
        }
    }
}
