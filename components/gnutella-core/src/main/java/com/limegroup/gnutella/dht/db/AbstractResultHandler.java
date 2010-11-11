package com.limegroup.gnutella.dht.db;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.listener.EventListener;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.util.ExceptionUtils;

import com.limegroup.gnutella.dht.DHTManager;

/**
 * An abstract implementation of DHTFutureAdapter to handle AltLocValues
 * and PushAltLocValues.
 * 
 * Iterates over entity keys and retrieves their values from the node and
 * calls {@link #handleDHTValueEntity(DHTValueEntity)} for them.
 */
abstract class AbstractResultHandler extends DHTFutureAdapter<FindValueResult> {
    
    private static final Log LOG = LogFactory.getLog(AbstractResultHandler.class);
    
    /**
     * Result type to denote different results of {@link AbstractResultHandler#handleDHTValueEntity(DHTValueEntity)}.
     */
    enum Result {
        /** An alternate location has been found */
        FOUND(true),
        /** An alternate location has not been found and will not be found */
        NOT_FOUND(false),
        /** An alternate location has not yet been found but could still be found */
        NOT_YET_FOUND(false) {
            @Override
            public boolean isFound() {
                throw new UnsupportedOperationException("Should not have been called on " + this);
            }  
        };
        
        private final boolean value;
        
        private Result(boolean value) {
            this.value = value;
        }
        
        public boolean isFound() {
            return value;
        }
    };
    
    private final List<EntityKey> entityKeys = new ArrayList<EntityKey>();
    
    protected final DHTManager dhtManager;
    
    protected final KUID key;
    
    private final SearchListener listener;
    
    protected final DHTValueType valueType;
    
    private volatile Result outcome = Result.NOT_FOUND;
    
    AbstractResultHandler(DHTManager dhtManager, KUID key, 
            SearchListener listener, DHTValueType valueType) {
        
        if (listener == null) {
            throw new NullPointerException("listener should not be null");
        }
        
        this.dhtManager = dhtManager;
        this.key = key;
        this.listener = listener;
        this.valueType = valueType;
    }
    
    @Override
    protected void operationComplete(FutureEvent<FindValueResult> event) {
        switch (event.getType()) {
            case SUCCESS:
                handleFutureSuccess(event.getResult());
                break;
            case EXCEPTION:
                handleExecutionException(event.getException());
                break;
            case CANCELLED:
                handleCancellation();
                break;
        }
    }

    private void handleFutureSuccess(FindValueResult result) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("result: " + result);
        }
        
        outcome = Result.NOT_FOUND;
        
        try {
            if (result.isSuccess()) {
                for (DHTValueEntity entity : result.getEntities()) {
                    outcome = updateResult(outcome, handleDHTValueEntity(entity)); 
                }
                
                entityKeys.addAll(result.getEntityKeys());
            }
            doNext();
            
        } catch (Throwable t) {
            uncaughtException(t);
        }
    }
    
    private void doNext() {
        if (entityKeys.isEmpty()) {
            if (outcome == Result.NOT_FOUND) {
                listener.searchFailed();
            }
            return;
        }
        
        EntityKey entityKey = entityKeys.remove(0);
        DHTFuture<FindValueResult> future 
            = dhtManager.get(entityKey);
        
        future.addFutureListener(new EventListener<FutureEvent<FindValueResult>>() {
            @Override
            public void handleEvent(FutureEvent<FindValueResult> event) {
                try {
                    if (event.getType() == Type.SUCCESS) {
                        FindValueResult result = event.getResult();
                        if (result.isSuccess()) {
                            for (DHTValueEntity entity : result.getEntities()) {
                                outcome = updateResult(outcome, handleDHTValueEntity(entity));
                            }
                        }
                    }
                    doNext();
                } catch (Throwable t) {
                    uncaughtException(t);
                }
            }
        });
    }
    
    private void uncaughtException(Throwable t) {
        ExceptionUtils.reportOrReturn(t);
        listener.searchFailed();
    }
    
    /**
     * Updates the result from the old value to the new value if the new value
     * doesn't already say the value was found. 
     *
     * FOUND => goes nowhere, the value has been found
     * NOT_YET_FOUND  => can go to FOUND
     * NOT_FOUND => can go to NOT_YET_FOUND, or FOUND, or stay with FOUND
     */
    private Result updateResult(Result oldValue, Result possibleValue) {
        if (oldValue == Result.FOUND || possibleValue == Result.FOUND) {
            return Result.FOUND;
        } else if (oldValue == Result.NOT_YET_FOUND || possibleValue == Result.NOT_YET_FOUND) {
            return Result.NOT_YET_FOUND;
        } else {
            return possibleValue;
        }
    }
    
    /**
     * Handles a DHTValueEntity (turns it into some Gnutella Object)
     * and returns true on success
     */
    protected abstract Result handleDHTValueEntity(DHTValueEntity entity);
    
    private void handleCancellation() {
        LOG.error("Cancelled");
        listener.searchFailed();
    }

    private void handleExecutionException(ExecutionException e) {
        LOG.error("ExecutionException", e);
        listener.searchFailed();
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof AbstractResultHandler)) {
            return false;
        }
        
        AbstractResultHandler other = (AbstractResultHandler)o;
        return key.equals(other.key)
                && valueType.equals(other.valueType);
    }
}