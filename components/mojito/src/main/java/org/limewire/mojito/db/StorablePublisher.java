package org.limewire.mojito.db;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.mojito.Context;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.DatabaseSettings;

/**
 * Publishes {@link Storable} values in the DHT.
 */
public class StorablePublisher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(StorablePublisher.class);
    
    private final Context context;
    
    private ScheduledFuture future;
    
    private final PublishTask publishTask = new PublishTask();
    
    public StorablePublisher(Context context) {
        this.context = context;
    }
    
    /**
     * Starts the <code>DHTValuePublisher</code>.
     */
    public void start() {
        synchronized (publishTask) {
            if (future == null) {
                long delay = DatabaseSettings.STORABLE_PUBLISHER_PERIOD.getValue();
                long initialDelay = delay;
                
                future = context.getDHTExecutorService()
                    .scheduleWithFixedDelay(this, initialDelay, delay, TimeUnit.MILLISECONDS);
            }
        }
    }
    
    /**
     * Stops the <code>DHTValuePublisher</code>.
     */
    public void stop() {
        synchronized (publishTask) {
            if (future != null) {
                future.cancel(true);
                future = null;
                
                publishTask.stop();
            }
        }
    }
    
    public void run() {
        
        // Do not publish values if we're not bootstrapped!
        if (context.isBootstrapped() 
                && !context.isBootstrapping()) {
            if (publishTask.isDone()) {
                
                if (LOG.isInfoEnabled()) {
                    LOG.info(context.getName() + " begins with publishing");
                }
                
                publishTask.start();
            }
        } else {
            
            if (LOG.isInfoEnabled()) {
                LOG.info(context.getName() + " is not bootstrapped");
            }
            
            publishTask.stop();
        }
    }
    
    /**
     * Publishes DHTValue(s) one-by-one by going
     * through a List of DHTValues. Every time a store finishes, it
     * continues with the next DHTValue until all DHTValues have
     * been republished
     */
    private class PublishTask {
        
        private Iterator<Storable> values = null;
        
        private DHTFuture<StoreResult> future = null;
        
        /**
         * Stops the PublishTask
         */
        public synchronized void stop() {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
            
            values = null;
        }
        
        /**
         * Returns whether or not the PublishTask is done
         */
        public synchronized boolean isDone() {
            return values == null || !values.hasNext();
        }
        
        /**
         * Starts the PublishTask.
         */
        public synchronized void start() {
            assert (isDone());
            
            StorableModelManager modelManager = context.getStorableModelManager();
            Collection<Storable> valuesToPublish = modelManager.getStorables();
            if (valuesToPublish == null) {
                valuesToPublish = Collections.emptyList();
            }
            
            if (LOG.isInfoEnabled()) {
                LOG.info(context.getName() + " has " 
                        + valuesToPublish.size() + " DHTValues to process");
            }
            
            values = valuesToPublish.iterator();
            next();
        }
        
        /**
         * Publishes the next <code>DHTValue</code>.
         */
        private synchronized boolean next() {
            if (isDone()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(context.getName() + " is done with publishing");
                }
                return false;
            }
            
            while(values.hasNext()) {
                Storable storable = values.next();
                if (publish(storable)) {
                    return true;
                }
            }
            
            return false;
        }
        
        /**
         * Publishes or expires the given <code>DHTValue</code>.
         */
        private boolean publish(Storable storable) {
            
            // Check if value is still in DB because we're
            // working with a copy of the Collection.
            future = context.store(DHTValueEntity.createFromStorable(context, storable));
            future.addFutureListener(new StoreResultHandler(storable));
            return true;
        }
    }
    
    private class StoreResultHandler extends DHTFutureAdapter<StoreResult> {
        
        private final Storable storable;
        
        private StoreResultHandler(Storable storable) {
            this.storable = storable;
        }
        
        @Override
        protected void operationComplete(FutureEvent<StoreResult> event) {
            FutureEvent.Type type = event.getType();
            
            if (type == Type.SUCCESS) {
                handleSuccess(event.getResult());
            } else {
                if (!publishTask.next() 
                        || type == Type.CANCELLED) {
                    publishTask.stop();
                }
            }
        }
        
        private void handleSuccess(final StoreResult result) {
            if (LOG.isInfoEnabled()) {
                Collection<? extends Contact> locations = result.getLocations();
                if (!locations.isEmpty()) {
                    LOG.info(result);
                } else {
                    LOG.info("Failed to store " + result.getValues());
                }
            }
            
            storable.handleStoreResult(result);
            
            context.getDHTExecutorService().execute(new Runnable() {
                public void run() {
                    context.getStorableModelManager().handleStoreResult(storable, result);
                }
            });
            
            if (!publishTask.next()) {
                publishTask.stop();
            }
        }
    }
}
