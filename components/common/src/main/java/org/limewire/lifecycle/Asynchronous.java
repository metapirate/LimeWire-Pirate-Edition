package org.limewire.lifecycle;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented

/**
 * Used to mark methods on <code>Service</code> impls as wanting to run asynchronously
 */
public @interface Asynchronous {

    /**
     * @return How an asynchronous task should be join()'ed.  Default is <code>Join.INFINITE</code>.
     */
    Join join() default Join.INFINITE;
    
    /**
     * @return how long to wait for this asynchronous task to complete.  
     * Units are in seconds. Use only in conjunction with <code>Join.TIMEOUT</code>. 
     */
    int timeout() default -1;

    /**
     * @return whether the asynchronous task should run as a daemon or not
     */
    boolean daemon() default true;

}
