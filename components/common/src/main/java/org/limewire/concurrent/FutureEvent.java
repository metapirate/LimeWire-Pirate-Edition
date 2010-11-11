package org.limewire.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/** An Event about a {@link Future}'s result. */
public class FutureEvent<V> {
    public static enum Type { SUCCESS, EXCEPTION, CANCELLED }
    
    private final FutureEvent.Type type;
    private final V result;
    private final ExecutionException exception;
    
    private FutureEvent(FutureEvent.Type type, V result, ExecutionException exception) {
        this.type = type;
        this.result = result;
        this.exception = exception;            
    }
    
    /** Creates an event out of the future. */
    public static <V> FutureEvent<V> createEvent(Future<V> future) {
        assert future.isDone();
        
        boolean cancelled = future.isCancelled();
        V result = null;
        ExecutionException ee = null;
        if(!cancelled) {
            try {
                result = future.get();
            } catch(ExecutionException exception) {
                ee = exception;
            } catch(InterruptedException ie) {
                throw new IllegalStateException(ie);
            }
        }
        
        if(cancelled) {
            return FutureEvent.createCancelled(); 
        } else if(ee != null) {
            return FutureEvent.createException(ee);
        } else {
            return FutureEvent.createSuccess(result);
        }
    }
    
    /** Creates a cancelled FutureEvent. */
    public static <V> FutureEvent<V> createCancelled() {
        return new FutureEvent<V>(Type.CANCELLED, null, null);
    }
    
    /** Creates a successful FutureEvent. */
    public static <V> FutureEvent<V> createSuccess(V result) {
        return new FutureEvent<V>(Type.SUCCESS, result, null);
    }
    
    /** Creates an exception FutureEvent. */
    public static <V> FutureEvent<V> createException(ExecutionException ee) {
        return new FutureEvent<V>(Type.EXCEPTION, null, ee);
    }
    
    /** Gets the kind of event. */
    public FutureEvent.Type getType() {
        return type;
    }
    
    /** Gets the result, if any.  Null if none. */
    public V getResult() {
        return result;
    }
    
    /** Gets the exception, if any.  Null if none. */
    public ExecutionException getException() {
        return exception;
    }
}