package org.limewire.lifecycle;

/**
     * How an asynchronous task should be join()'ed
 */
public enum Join {
    /**
     * the <code>join()</code> method on <code>Thread</code> is to be used
     */
    INFINITE {
        @Override
        void join(Thread t, int timeout)  throws InterruptedException {
            if(timeout > -1) {
                throw new IllegalArgumentException("timeout");
            }
            t.join();    
        }
    },

    /**
     * the <code>join(long timeout)</code> method on <code>Thread</code> is to be used
     */
    TIMEOUT {
        @Override
        void join(Thread t, int timeout)  throws InterruptedException {
            if(timeout < 0) {
                throw new IllegalArgumentException("timeout");
            }
            t.join(1000 * timeout);
        }},

    /**
     * No <code>join()</code>'ing is to be done
     */
    NONE {
        @Override
        void join(Thread t, int timeout)  throws InterruptedException {
            if(timeout > -1) {
                throw new IllegalArgumentException("timeout");
            }
            // don't join
        }};
    
    abstract void join(Thread t, int timeout) throws InterruptedException;
}
